package org.jetbrains.plugins.scala.packagesearch

import com.intellij.buildsystem.model.unified.UnifiedDependency
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.{Navigatable, NavigatableAdapter}
import com.jetbrains.packagesearch.intellij.plugin.extensibility.{ProjectModule, ProjectModuleProvider}
import com.jetbrains.packagesearch.intellij.plugin.ui.toolwindow.models.PackageVersion
import kotlin.sequences.Sequence
import org.jetbrains.plugins.scala.packagesearch.utils.{SbtCommon, SbtDependencyUtils, SbtProjectModuleType}
import org.jetbrains.sbt.{RichFile, Sbt, SbtUtil}

import java.io.File
import scala.jdk.CollectionConverters.IteratorHasAsJava

class SbtProjectModuleProvider extends ProjectModuleProvider {

  def findModulePaths(module: Module): Array[File] = {
    if (!SbtUtil.isSbtModule(module)) return null
    val contentRoots = ModuleRootManager.getInstance(module).getContentRoots
    if (contentRoots.length < 1) return null
    contentRoots.map(virtualFile => {
      new File(virtualFile.getPath)
    })
  }

  def createNavigatableDependencyCallback(project: Project,
                                          module: Module): (String, String, PackageVersion) =>
    Navigatable = (groupId: String, artifactId: String, packageVersion: PackageVersion) => {
    
    val targetedLibDep = SbtDependencyUtils.findLibraryDependency(
      project,
      module,
      new UnifiedDependency(groupId, artifactId, packageVersion.toString, SbtCommon.defaultLibScope),
      configurationRequired = false
    )

    new NavigatableAdapter() {
      override def navigate(requestFocus: Boolean): Unit = {
        PsiNavigationSupport.getInstance.createNavigatable(
          project,
          targetedLibDep.getContainingFile.getVirtualFile,
          targetedLibDep.getTextOffset
        ).navigate(requestFocus)
      }
    }
  }

  def obtainProjectModulesFor(project: Project, module: Module):ProjectModule = try {
    val sbtFileOpt = SbtDependencyUtils.getSbtFileOpt(module)
    sbtFileOpt match {
      case Some(buildFile: VirtualFile) =>
          val projectModule = new ProjectModule(
          module.getName,
          module,
          null,
            buildFile,
          SbtCommon.buildSystemType,
          SbtProjectModuleType
        )
        ScalaHelper.setNavigatableDependency(projectModule, createNavigatableDependencyCallback(project, module))
        projectModule
      case _ => null
    }

  } catch {
    case exception: Exception =>
      throw(exception)
    case x: Throwable => throw(x)
  }

  override def obtainAllProjectModulesFor(project: Project): Sequence[ProjectModule] = {
    val projectModules = ModuleManager.getInstance(project).getModules
      .map(module => obtainProjectModulesFor(project, module))
      .filter(_ != null)
      .distinct
    val res = ScalaHelper.toKotlinSequence(projectModules.iterator.asJava)
    res
  }
}
