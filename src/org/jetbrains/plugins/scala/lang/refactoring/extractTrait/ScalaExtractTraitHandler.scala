package org.jetbrains.plugins.scala
package lang.refactoring.extractTrait

import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.openapi.project.Project
import com.intellij.psi._
import com.intellij.openapi.actionSystem.{CommonDataKeys, DataContext}
import com.intellij.openapi.editor.{ScrollType, Editor}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef._
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.lang.refactoring.memberPullUp.ScalaPullUpProcessor
import scala.collection.mutable
import org.jetbrains.plugins.scala.lang.psi.api.ScalaRecursiveElementVisitor
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScNewTemplateDefinition, ScSuperReference, ScReferenceExpression}
import scala.collection.JavaConverters._
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import com.intellij.util.containers.MultiMap
import com.intellij.openapi.ui.DialogWrapper
import java.util
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReferenceElement
import com.intellij.refactoring.extractSuperclass.ExtractSuperClassUtil
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaDirectoryService

/**
 * Nikolay.Tropin
 * 2014-05-20
 */
class ScalaExtractTraitHandler extends RefactoringActionHandler {

  val REFACTORING_NAME = ScalaBundle.message("extract.trait.title")

  override def invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext) = {
    val offset: Int = editor.getCaretModel.getOffset
    editor.getScrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    val element: PsiElement = file.findElementAt(offset)
    val clazz = PsiTreeUtil.getParentOfType(element, classOf[ScTemplateDefinition])
    invokeOnClass(clazz, project, editor)
  }

  override def invoke(project: Project, elements: Array[PsiElement], dataContext: DataContext) = {
    val clazz = elements match {
      case Array(clazz: ScTemplateDefinition) => clazz
      case _ =>
        val parent = PsiTreeUtil.findCommonParent(elements: _*)
        PsiTreeUtil.getParentOfType(parent, classOf[ScTemplateDefinition], false)
    }

    if (dataContext != null) {
      val editor: Editor = CommonDataKeys.EDITOR.getData(dataContext)
      if (editor != null && clazz != null) {
        invokeOnClass(clazz, project, editor)
      }
    }
  }

  private def invokeOnClass(clazz: ScTemplateDefinition, project: Project, editor: Editor) {
    if (clazz == null) return

    val dialog = new ScalaExtractTraitDialog(project, clazz)
    dialog.show()
    val memberInfos = dialog.getSelectedMembers.asScala
    if (memberInfos.isEmpty) return
    val extractInfo = new ExtractInfo(clazz, memberInfos)
    extractInfo.collect()

    val isOk = ExtractSuperClassUtil.showConflicts(dialog, extractInfo.conflicts, clazz.getProject)
    if (!isOk) return

    val name = dialog.getTraitName
    val packName = dialog.getPackageName

    val trt = inWriteCommandAction(project, "Extract trait") {
      val newTrait = createTraitFromTemplate(name, packName, clazz)
      addSelfType(newTrait, extractInfo.selfTypeText)
      ExtractSuperUtil.addExtendsTo(clazz, newTrait)
      newTrait
    }
    val pullUpProcessor = new ScalaPullUpProcessor(project, clazz, trt, memberInfos)
    pullUpProcessor.moveMembersToBase()
  }

  private def addSelfType(trt: ScTrait, selfTypeText: Option[String]) {
    selfTypeText match {
      case None =>
      case Some(selfTpe) =>
        val traitText = s"trait ${trt.name} {\n$selfTpe\n}"
        val dummyTrait = ScalaPsiElementFactory.createTemplateDefinitionFromText(traitText, trt.getParent, trt)
        val selfTypeElem = dummyTrait.extendsBlock.selfTypeElement.get
        val extendsBlock = trt.extendsBlock
        val templateBody = extendsBlock.templateBody match {
          case Some(tb) => tb
          case None => extendsBlock.add(ScalaPsiElementFactory.createTemplateBody(trt.getManager))
        }

        val lBrace = templateBody.getFirstChild
        val ste = templateBody.addAfter(selfTypeElem, lBrace)
        templateBody.addAfter(ScalaPsiElementFactory.createNewLine(trt.getManager), lBrace)
        ScalaPsiUtil.adjustTypes(ste)
    }
  }

  private def createTraitFromTemplate(name: String, packageName: String, clazz: ScTemplateDefinition): ScTrait = {
    val currentPackageName = ExtractSuperUtil.packageName(clazz)
    val dir =
      if (packageName == currentPackageName) clazz.getContainingFile.getContainingDirectory
      else {
        val pckg = JavaPsiFacade.getInstance(clazz.getProject).findPackage(packageName)
        if (pckg == null || pckg.getDirectories.isEmpty) throw new IllegalArgumentException("Cannot find directory for new trait")
        else pckg.getDirectories()(0)
      }
    ScalaDirectoryService.createClassFromTemplate(dir, name, "Scala Trait", askToDefineVariables = false).asInstanceOf[ScTrait]
  }

  def checkConflicts(dialog: DialogWrapper, clazz: ScTemplateDefinition, selectedMembers: util.List[ScalaExtractMemberInfo]) = {
    import scala.collection.convert.wrapAll._

    val conflicts: MultiMap[PsiElement, String] = new MultiMap[PsiElement, String]
    var currentMemberName: String = null

    val visitor = new ScalaRecursiveElementVisitor() {
      override def visitReference(ref: ScReferenceElement) {
        val resolve = ref.resolve()
        resolve match {
          case named: PsiNamedElement =>
            ScalaPsiUtil.nameContext(named) match {
              case m: ScMember if m.containingClass == clazz && m.isPrivate && !selectedMembers.map(_.getMember).contains(m) =>
                conflicts.putValue(m, s"Private member ${named.name} cannot be used in the extracted member $currentMemberName")
              case m: ScMember if clazz.isInstanceOf[ScNewTemplateDefinition] && m.containingClass == clazz =>
                conflicts.putValue(m, s"Member ${named.name} of an anonymous class cannot be used in the extracted member $currentMemberName")
              case m: ScMember if ref.qualifier.isInstanceOf[ScSuperReference] && clazz.isInheritor(m.containingClass, deep = true) =>
                conflicts.putValue(m, s"Extracted member $currentMemberName has reference to super, but extracted trait won't have a base class.")
              case _ =>
            }
          case _ =>
        }
      }
    }

    for {
      info <- selectedMembers
      if !info.isToAbstract
      m = info.getMember
    } {
      currentMemberName = info.getDisplayName
      m.accept(visitor)
    }

    ExtractSuperClassUtil.showConflicts(dialog, conflicts, clazz.getProject)
  }

  private class ExtractInfo(clazz: ScTemplateDefinition, selectedMemberInfos: Seq[ScalaExtractMemberInfo]) {
    private val classesForSelfType = mutable.Set[PsiClass]()
    private val selected = selectedMemberInfos.map(_.getMember)
    private var currentMemberName: String = null
    val conflicts: MultiMap[PsiElement, String] = new MultiMap[PsiElement, String]

    private def forMember[T](elem: PsiElement)(action: PsiMember => Option[T]): Option[T] = {
      elem match {
        case ScalaPsiUtil.inNameContext(m: ScMember) => action(m)
        case m: PsiMember => action(m)
        case _ => None
      }
    }

    private object inSameClassOrAncestor {
      def unapply(elem: PsiElement): Option[(PsiMember, PsiClass)] = {
        forMember(elem) {
          m => m.containingClass match {
            case null => None
            case `clazz` => Some(m, clazz)
            case c if clazz.isInheritor(c, deep = true) => Some(m, c)
            case _ => None
          }
        }
      }
    }

    private object inSelfType {
      def unapply(elem: PsiElement): Option[PsiClass] = {
        val selfTypeOfClazz = clazz.extendsBlock.selfType
        if (selfTypeOfClazz.isEmpty) return None

        forMember(elem) {
          m => m.containingClass match {
            case null => None
            case c if selfTypeOfClazz.get.conforms(ScType.designator(c)) => Some(c)
            case _ => None
          }
        }
      }
    }

    private def addToClassesForSelfType(cl: PsiClass) {
      if (!classesForSelfType.contains(cl) && !classesForSelfType.exists(_.isInheritor(cl, true))) {
        classesForSelfType --= classesForSelfType.filter(cl.isInheritor(_, true))
        classesForSelfType += cl
      }
    }

    private def collectForSelfType(resolve: PsiElement) {
      resolve match {
        case inSameClassOrAncestor(m, cl: PsiClass) if !selected.contains(m) => addToClassesForSelfType(cl)
        case inSelfType(cl) => addToClassesForSelfType(cl)
        case _ =>
      }
    }

    private def collectConflicts(ref: ScReferenceExpression, resolve: PsiElement) {
      resolve match {
        case named: PsiNamedElement =>
          ScalaPsiUtil.nameContext(named) match {
            case m: ScMember if m.containingClass == clazz && m.isPrivate && !selected.contains(m) =>
              conflicts.putValue(m, s"Private member ${named.name} cannot be used in the extracted member $currentMemberName")
            case m: ScMember if clazz.isInstanceOf[ScNewTemplateDefinition] && m.containingClass == clazz =>
              conflicts.putValue(m, s"Member ${named.name} of an anonymous class cannot be used in the extracted member $currentMemberName")
            case m: PsiMember
              if m.containingClass != null && ref.qualifier.isInstanceOf[ScSuperReference] && clazz.isInheritor(m.containingClass, deep = true) =>
              conflicts.putValue(m, s"Extracted member $currentMemberName has reference to super, but extracted trait won't have a base class.")
            case _ =>
          }
        case _ =>
      }
    }

    def collect() {
      val visitor = new ScalaRecursiveElementVisitor {
        override def visitReferenceExpression(ref: ScReferenceExpression) {
          val resolve = ref.resolve()
          collectForSelfType(resolve)
          collectConflicts(ref, resolve)
        }
      }

      for {
        info <- selectedMemberInfos
        member = info.getMember
      } {
        currentMemberName = info.getDisplayName
        member.accept(visitor)
      }

      classesForSelfType.foreach {
        case cl: PsiClass if cl.getTypeParameters.nonEmpty =>
          conflicts.putValue(cl,
            s"Extracted trait will have ${cl.name} as a self type, but identification of it's type parameters is not supported")
        case _ =>
      }
    }

    def selfTypeText: Option[String] = {
      val alias = clazz.extendsBlock.selfTypeElement.fold("this")(_.name)

      val typeText = classesForSelfType.map {
        case obj: ScObject => s"${obj.qualifiedName}.type"
        case cl: ScTypeDefinition => cl.qualifiedName
        case cl: PsiClass => cl.getQualifiedName
      }.mkString(" with ")

      if (classesForSelfType.nonEmpty) Some(s"$alias: $typeText =>") else None
    }
  }

}
