/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.externalSystem.test;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.DisposeAwareProjectChange;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.jetbrains.sbt.project.settings.SbtProjectSettings;

import java.io.IOException;
import java.util.*;

/**
 * @author Vladislav.Soroka
 * @since 6/30/2014
 */
public abstract class ExternalSystemImportingTestCase extends ExternalSystemTestCase {

  @Override
  protected void setUpInWriteAction() throws Exception {
    super.setUpInWriteAction();
  }

  protected void assertModules(String... expectedNames) {
    Module[] actual = ModuleManager.getInstance(myProject).getModules();
    List<String> actualNames = new ArrayList<String>();

    for (Module m : actual) {
      actualNames.add(m.getName());
    }

    assertUnorderedElementsAreEqual(actualNames, expectedNames);
  }

  protected void assertContentRoots(String moduleName, String... expectedRoots) {
    List<String> actual = new ArrayList<String>();
    for (ContentEntry e : getContentRoots(moduleName)) {
      actual.add(e.getUrl());
    }

    for (int i = 0; i < expectedRoots.length; i++) {
      expectedRoots[i] = VfsUtilCore.pathToUrl(expectedRoots[i]);
    }

    assertUnorderedPathsAreEqual(actual, Arrays.asList(expectedRoots));
  }

  protected void assertSources(String moduleName, String... expectedSources) {
    doAssertContentFolders(moduleName, JavaSourceRootType.SOURCE, expectedSources);
  }

  protected void assertGeneratedSources(String moduleName, String... expectedSources) {
    ContentEntry contentRoot = getContentRoot(moduleName);
    List<ContentFolder> folders = new ArrayList<ContentFolder>();
    for (SourceFolder folder : contentRoot.getSourceFolders(JavaSourceRootType.SOURCE)) {
      JavaSourceRootProperties properties = folder.getJpsElement().getProperties(JavaSourceRootType.SOURCE);
      assertNotNull(properties);
      if (properties.isForGeneratedSources()) {
        folders.add(folder);
      }
    }
    doAssertContentFolders(contentRoot, folders, expectedSources);
  }

  protected void assertResources(String moduleName, String... expectedSources) {
    doAssertContentFolders(moduleName, JavaResourceRootType.RESOURCE, expectedSources);
  }

  protected void assertTestSources(String moduleName, String... expectedSources) {
    doAssertContentFolders(moduleName, JavaSourceRootType.TEST_SOURCE, expectedSources);
  }

  protected void assertTestResources(String moduleName, String... expectedSources) {
    doAssertContentFolders(moduleName, JavaResourceRootType.TEST_RESOURCE, expectedSources);
  }

  protected void assertExcludes(String moduleName, String... expectedExcludes) {
    ContentEntry contentRoot = getContentRoot(moduleName);
    doAssertContentFolders(contentRoot, Arrays.asList(contentRoot.getExcludeFolders()), expectedExcludes);
  }

  protected void assertContentRootExcludes(String moduleName, String contentRoot, String... expectedExcudes) {
    ContentEntry root = getContentRoot(moduleName, contentRoot);
    doAssertContentFolders(root, Arrays.asList(root.getExcludeFolders()), expectedExcudes);
  }

  private void doAssertContentFolders(String moduleName, @NotNull JpsModuleSourceRootType<?> rootType, String... expected) {
    ContentEntry contentRoot = getContentRoot(moduleName);
    doAssertContentFolders(contentRoot, contentRoot.getSourceFolders(rootType), expected);
  }

  private static void doAssertContentFolders(ContentEntry e, final List<? extends ContentFolder> folders, String... expected) {
    List<String> actual = new ArrayList<String>();
    for (ContentFolder f : folders) {
      String rootUrl = e.getUrl();
      String folderUrl = f.getUrl();

      if (folderUrl.startsWith(rootUrl)) {
        int length = rootUrl.length() + 1;
        folderUrl = folderUrl.substring(Math.min(length, folderUrl.length()));
      }

      actual.add(folderUrl);
    }

    assertOrderedElementsAreEqual(actual, Arrays.asList(expected));
  }

  protected void assertModuleOutput(String moduleName, String output, String testOutput) {
    CompilerModuleExtension e = getCompilerExtension(moduleName);

    assertFalse(e.isCompilerOutputPathInherited());
    assertEquals(output, getAbsolutePath(e.getCompilerOutputUrl()));
    assertEquals(testOutput, getAbsolutePath(e.getCompilerOutputUrlForTests()));
  }

  protected void assertModuleInheritedOutput(String moduleName) {
    CompilerModuleExtension e = getCompilerExtension(moduleName);
    assertTrue(e.isCompilerOutputPathInherited());
  }

  private static String getAbsolutePath(String path) {
    path = VfsUtil.urlToPath(path);
    path = FileUtil.toCanonicalPath(path);
    return FileUtil.toSystemIndependentName(path);
  }

  protected void assertProjectOutput(String module) {
    assertTrue(getCompilerExtension(module).isCompilerOutputPathInherited());
  }

  protected CompilerModuleExtension getCompilerExtension(String module) {
    return CompilerModuleExtension.getInstance(getModule(module));
  }

  protected void assertModuleLibDep(String moduleName, String depName) {
    assertModuleLibDep(moduleName, depName, null);
  }

  protected void assertModuleLibDep(String moduleName, String depName, String classesPath) {
    assertModuleLibDep(moduleName, depName, classesPath, null, null);
  }

  protected void assertModuleLibDep(String moduleName, String depName, String classesPath, String sourcePath, String javadocPath) {
    LibraryOrderEntry lib = ContainerUtil.getFirstItem(getModuleLibDeps(moduleName, depName));

    assertModuleLibDepPath(lib, OrderRootType.CLASSES, classesPath == null ? null : Collections.singletonList(classesPath));
    assertModuleLibDepPath(lib, OrderRootType.SOURCES, sourcePath == null ? null : Collections.singletonList(sourcePath));
    assertModuleLibDepPath(lib, JavadocOrderRootType.getInstance(), javadocPath == null ? null : Collections.singletonList(javadocPath));
  }

  protected void assertModuleLibDep(String moduleName,
                                    String depName,
                                    List<String> classesPaths,
                                    List<String> sourcePaths,
                                    List<String> javadocPaths) {
    LibraryOrderEntry lib = ContainerUtil.getFirstItem(getModuleLibDeps(moduleName, depName));

    assertModuleLibDepPath(lib, OrderRootType.CLASSES, classesPaths);
    assertModuleLibDepPath(lib, OrderRootType.SOURCES, sourcePaths);
    assertModuleLibDepPath(lib, JavadocOrderRootType.getInstance(), javadocPaths);
  }

  private static void assertModuleLibDepPath(LibraryOrderEntry lib, OrderRootType type, List<String> paths) {
    if (paths == null) return;
    assertUnorderedPathsAreEqual(Arrays.asList(lib.getRootUrls(type)), paths);
    // also check the library because it may contain slight different set of urls (e.g. with duplicates)
    final Library library = lib.getLibrary();
    assertNotNull(library);
    assertUnorderedPathsAreEqual(Arrays.asList(library.getUrls(type)), paths);
  }

  protected void assertModuleLibDepScope(String moduleName, String depName, DependencyScope scopes) {
    List<LibraryOrderEntry> deps = getModuleLibDeps(moduleName, depName);
    assertUnorderedElementsAreEqual(ContainerUtil.map2Array(deps, new Function<LibraryOrderEntry, Object>() {
      @Override
      public Object fun(LibraryOrderEntry entry) {
        return entry.getScope();
      }
    }), scopes);
  }

  private List<LibraryOrderEntry> getModuleLibDeps(String moduleName, String depName) {
    return getModuleDep(moduleName, depName, LibraryOrderEntry.class);
  }

  protected void assertModuleLibDeps(String moduleName, String... expectedDeps) {
    assertModuleDeps(moduleName, LibraryOrderEntry.class, expectedDeps);
  }

  protected void assertExportedDeps(String moduleName, String... expectedDeps) {
    final List<String> actual = new ArrayList<String>();

    getRootManager(moduleName).orderEntries().withoutSdk().withoutModuleSourceEntries().exportedOnly().process(new RootPolicy<Object>() {
      @Override
      public Object visitModuleOrderEntry(ModuleOrderEntry e, Object value) {
        actual.add(e.getModuleName());
        return null;
      }

      @Override
      public Object visitLibraryOrderEntry(LibraryOrderEntry e, Object value) {
        actual.add(e.getLibraryName());
        return null;
      }
    }, null);

    assertOrderedElementsAreEqual(actual, expectedDeps);
  }

  protected void assertModuleModuleDeps(String moduleName, String... expectedDeps) {
    assertModuleDeps(moduleName, ModuleOrderEntry.class, expectedDeps);
  }

  private void assertModuleDeps(String moduleName, Class clazz, String... expectedDeps) {
    assertOrderedElementsAreEqual(collectModuleDepsNames(moduleName, clazz), expectedDeps);
  }

  protected void assertModuleModuleDepScope(String moduleName, String depName, DependencyScope... scopes) {
    List<ModuleOrderEntry> deps = getModuleModuleDeps(moduleName, depName);
    assertUnorderedElementsAreEqual(ContainerUtil.map2Array(deps, new Function<ModuleOrderEntry, Object>() {
      @Override
      public Object fun(ModuleOrderEntry entry) {
        return entry.getScope();
      }
    }), new Object[]{scopes});
  }

  @NotNull
  private List<ModuleOrderEntry> getModuleModuleDeps(@NotNull String moduleName, @NotNull String depName) {
    return getModuleDep(moduleName, depName, ModuleOrderEntry.class);
  }

  private List<String> collectModuleDepsNames(String moduleName, Class clazz) {
    List<String> actual = new ArrayList<String>();

    for (OrderEntry e : getRootManager(moduleName).getOrderEntries()) {
      if (clazz.isInstance(e)) {
        actual.add(e.getPresentableName());
      }
    }
    return actual;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private <T> List<T> getModuleDep(@NotNull String moduleName, @NotNull String depName, @NotNull Class<T> clazz) {
    List<T> deps = new ArrayList<>();

    for (OrderEntry e : getRootManager(moduleName).getOrderEntries()) {
      if (clazz.isInstance(e) && e.getPresentableName().equals(depName)) {
        deps.add((T)e);
      }
    }
    assertNotNull("Dependency not found: " + depName + "\namong: " + collectModuleDepsNames(moduleName, clazz), deps);
    return deps;
  }

  public void assertProjectLibraries(String... expectedNames) {
    List<String> actualNames = new ArrayList<String>();
    for (Library each : LibraryTablesRegistrar.getInstance().getLibraryTable(myProject).getLibraries()) {
      String name = each.getName();
      actualNames.add(name == null ? "<unnamed>" : name);
    }
    assertUnorderedElementsAreEqual(actualNames, expectedNames);
  }

  protected void assertModuleGroupPath(String moduleName, String... expected) {
    String[] path = ModuleManager.getInstance(myProject).getModuleGroupPath(getModule(moduleName));

    if (expected.length == 0) {
      assertNull(path);
    }
    else {
      assertNotNull(path);
      assertOrderedElementsAreEqual(Arrays.asList(path), expected);
    }
  }

  private ContentEntry getContentRoot(String moduleName) {
    ContentEntry[] ee = getContentRoots(moduleName);
    List<String> roots = new ArrayList<String>();
    for (ContentEntry e : ee) {
      roots.add(e.getUrl());
    }

    String message = "Several content roots found: [" + StringUtil.join(roots, ", ") + "]";
    assertEquals(message, 1, ee.length);

    return ee[0];
  }

  private ContentEntry getContentRoot(String moduleName, String path) {
    for (ContentEntry e : getContentRoots(moduleName)) {
      if (e.getUrl().equals(VfsUtilCore.pathToUrl(path))) return e;
    }
    throw new AssertionError("content root not found");
  }

  public ContentEntry[] getContentRoots(String moduleName) {
    return getRootManager(moduleName).getContentEntries();
  }

  private ModuleRootManager getRootManager(String module) {
    return ModuleRootManager.getInstance(getModule(module));
  }

  protected void importProject(@NonNls String config) throws IOException {
    createProjectConfig(config);
    importProject();
  }

  protected void importProject() {
    doImportProject();
  }
  protected void importProject(Sdk sdk) {
    doImportProject(sdk);
  }

  private void doImportProject() {
    doImportProject(null);
  }

  @SuppressWarnings("unchecked")
  private void doImportProject(final Sdk sdk) {
    AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(myProject, getExternalSystemId());
    final ExternalProjectSettings projectSettings = getCurrentExternalProjectSettings();
    projectSettings.setExternalProjectPath(getProjectPath());
    if (sdk != null) {
      ((SbtProjectSettings) projectSettings).jdk_$eq(sdk.getName());
    }
    Set<ExternalProjectSettings> settings = new HashSet<ExternalProjectSettings>(systemSettings.getLinkedProjectsSettings());
    settings.remove(projectSettings);
    settings.add(projectSettings);
    systemSettings.setLinkedProjectsSettings(settings);

    final Ref<Couple<String>> error = Ref.create();
    ExternalSystemUtil.refreshProjects(
      new ImportSpecBuilder(myProject, getExternalSystemId())
        .use(ProgressExecutionMode.MODAL_SYNC)
        .callback(new ExternalProjectRefreshCallback() {
          @Override
          public void onSuccess(@Nullable final DataNode<ProjectData> externalProject) {
            if (externalProject == null) {
              fail("Got null External project after import");
              return;
            }
            ExternalSystemApiUtil.executeProjectChangeAction(true, new DisposeAwareProjectChange(myProject) {
              @Override
              public void execute() {
                ProjectRootManagerEx.getInstanceEx(myProject).mergeRootsChangesDuring(new Runnable() {
                  @Override
                  public void run() {
                    ProjectDataManager ProjectDataManager = ApplicationManager.getApplication().getService(ProjectDataManager.class);
                    ProjectDataManager.importData(Collections.singleton(externalProject), myProject, true);
                  }
                });
              }
            });
            System.out.println("External project was successfully imported");
          }

          @Override
          public void onFailure(@NotNull String errorMessage, @Nullable String errorDetails) {
            error.set(Couple.of(errorMessage, errorDetails));
          }
        })
    );

    if (!error.isNull()) {
      String failureMsg = "Import failed: " + error.get().first;
      if (StringUtil.isNotEmpty(error.get().second)) {
        failureMsg += "\nError details: \n" + error.get().second;
      }
      fail(failureMsg);
    }
  }

  protected abstract ExternalProjectSettings getCurrentExternalProjectSettings();

  protected abstract ProjectSystemId getExternalSystemId();

  protected static Sdk createJdk(String versionName) {
    return IdeaTestUtil.getMockJdk17(versionName);
  }

  //protected void assertProblems(String... expectedProblems) {
  //  final List<String> actualProblems = new ArrayList<String>();
  //  UIUtil.invokeAndWaitIfNeeded(new Runnable() {
  //    @Override
  //    public void run() {
  //      final NewErrorTreeViewPanel messagesView = ExternalSystemNotificationManager.getInstance(myProject)
  //        .prepareMessagesView(getExternalSystemId(), NotificationSource.PROJECT_SYNC, false);
  //      final ErrorViewStructure treeStructure = messagesView.getErrorViewStructure();
  //
  //      ErrorTreeElement[] elements = treeStructure.getChildElements(treeStructure.getRootElement());
  //      for (ErrorTreeElement element : elements) {
  //        if (element.getKind() == ErrorTreeElementKind.ERROR ||
  //            element.getKind() == ErrorTreeElementKind.WARNING) {
  //          actualProblems.add(StringUtil.join(element.getText(), "\n"));
  //        }
  //      }
  //    }
  //  });
  //
  //  assertOrderedElementsAreEqual(actualProblems, expectedProblems);
  //}
}
