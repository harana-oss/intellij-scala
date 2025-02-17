package org.jetbrains.plugins.scala
package debugger
package exactBreakpoints

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.SourcePositionHighlighter
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.DocumentUtil
import com.intellij.xdebugger.XDebuggerUtil
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.junit.Assert
import org.junit.experimental.categories.Category

import scala.jdk.CollectionConverters._

abstract class ExactBreakpointTestBase extends ScalaDebuggerTestCase {
  case class Breakpoint(line: Int, ordinal: Integer, fileName: String = null) {
    override def toString: String = s"line = $line, ordinal=$ordinal"
  }

  private def addBreakpoint(b: Breakpoint): Unit = addBreakpoint(b.line, Option(b.fileName).getOrElse(mainFileName), b.ordinal)

  protected def checkVariants(lineNumber: Int, variants: String*): Unit = {
    inReadAction {
      val xSourcePosition = XDebuggerUtil.getInstance().createPosition(getVirtualFile(getFileInSrc(mainFileName)), lineNumber)
      val foundVariants = scalaLineBreakpointType.computeVariants(getProject, xSourcePosition).asScala.map(_.getText)
      Assert.assertEquals("Wrong set of variants found: ", variants, foundVariants)
    }
  }

  protected def checkStoppedAtBreakpointAt(breakpoints: Breakpoint*)(sourcePositionText: String): Unit = {
    checkStopResumeSeveralTimes(breakpoints: _*)(sourcePositionText)
  }

  protected def checkStopResumeSeveralTimes(breakpoints: Breakpoint*)(sourcePositions: String*): Unit = {
    def message(expected: String, actual: String) = {
      s"Wrong source position. Expected: $expected, actual: $actual"
    }
    clearBreakpoints()
    breakpoints.foreach(addBreakpoint)
    runDebugger() {
      for (expected <- sourcePositions) {
        waitForBreakpoint()
        val location = currentLocation()
        inReadAction {
          val sourcePosition = positionManager.getSourcePosition(location)
          val text: String = highlightedText(sourcePosition)
          Assert.assertTrue(message(expected, text), text.startsWith(expected.stripSuffix("...")))
        }
        resume()
      }
    }
  }

  private def highlightedText(sourcePosition: SourcePosition): String = {
    val elemRange = SourcePositionHighlighter.getHighlightRangeFor(sourcePosition)
    val document = PsiDocumentManager.getInstance(getProject).getDocument(sourcePosition.getFile)
    val lineRange = DocumentUtil.getLineTextRange(document, sourcePosition.getLine)
    val textRange = if (elemRange != null) elemRange.intersection(lineRange) else lineRange
    document.getText(textRange).trim
  }

  protected def checkNotStoppedAtBreakpointAt(breakpoint: Breakpoint, mainClass: String = mainClassName): Unit = {
    clearBreakpoints()
    addBreakpoint(breakpoint)
    runDebugger(mainClass, shouldStopAtBreakpoint = false) {
      Assert.assertTrue(s"Stopped at breakpoint: $breakpoint", processTerminatedNoBreakpoints())
    }
  }
}

@Category(Array(classOf[DebuggerTests]))
class ExactBreakpointTest_2_11 extends ExactBreakpointTest {
  override protected def supportedIn(version: ScalaVersion) = version <= LatestScalaVersions.Scala_2_11
}

@Category(Array(classOf[DebuggerTests]))
class ExactBreakpointTest_2_12 extends ExactBreakpointTest {
  override protected def supportedIn(version: ScalaVersion) = 
    version >= LatestScalaVersions.Scala_2_12 && version <= LatestScalaVersions.Scala_2_13
  
  addSourceFile("SamAbstractClass.scala",
    """object SamAbstractClass  {
      |
      |  def main(args: Array[String]): Unit = {
      |    val test: Parser[String] = (in: String) => {
      |      println()
      |      in
      |    }
      |
      |    test.parse(string)
      |
      |    parse(string)(firstChar)
      |  }
      |
      |  def parse[T](s: String)(p: Parser[T]) = p.parse(s)
      |
      |  def firstChar(s: String): Option[Char] = s.headOption
      |
      |  val string = "string"
      |
      |  abstract class Parser[T] {
      |    def parse(s: String): T
      |  }
      |}
    """.stripMargin)
  def testSamAbstractClass(): Unit = {
    val printlnBp       = Breakpoint(4, null)
    val testParseBp     = Breakpoint(8, null)
    val firstCharLambda = Breakpoint(10, 0)
    val firstCharMethod = Breakpoint(15, null)

    checkStopResumeSeveralTimes(printlnBp, testParseBp, firstCharLambda, firstCharMethod)(
      "test.parse(string)",
      "println()",
      "firstChar",
      "def firstChar..."
    )
  }
}

@Category(Array(classOf[DebuggerTests]))
class ExactBreakpointTest_3_0 extends ExactBreakpointTest_2_12 {
  override protected def supportedIn(version: ScalaVersion) = version >= LatestScalaVersions.Scala_3_0

  removeSourceFile("EarlyDefAndTemplateBody.scala")
  override def testEarlyDefAndTemplateBody(): Unit = {}

  addSourceFile("one.scala", "def one() = 1")
  addSourceFile("a/two.scala",
  """package a
    |
    |def two() = 2
    |""".stripMargin)
  addSourceFile("a/b/three.scala",
    """package a.b
      |
      |def three =
      |  3
      |""".stripMargin)
  addSourceFile("topLevel.scala",
    """import a.two
      |import a.b.three
      |
      |@main
      |def topLevelMain(): Unit =
      |  println(0)
      |  println(one())
      |  println(two())
      |  println(three)
      |
      |""".stripMargin)
  def testtopLevelMain(): Unit = {
    val zeroBp = Breakpoint(5, null, "topLevel.scala")
    val oneBp = Breakpoint(0, null, "one.scala")
    val twoBp = Breakpoint(2, null, "a/two.scala")
    val threeBp = Breakpoint(3, null, "a/b/three.scala")

    checkStopResumeSeveralTimes(zeroBp, oneBp, twoBp, threeBp) (
      "println(0)", "def one() = 1", "def two() = 2", "3"
    )
  }

  addSourceFile("MainAnnotation.scala",
    """object MainAnnotation {
      |  object Inner {
      |    @main
      |    def mainInInnerObject(): Unit =
      |      println(42)
      |  }
      |}
      |""".stripMargin)
  def testmainInInnerObject(): Unit = {
    checkStoppedAtBreakpointAt(Breakpoint(4, null, "MainAnnotation.scala"))("println(42)")
  }


  //todo fix breakpoints in lambdas
  override def testLikeDefaultArgName(): Unit = {}
  override def testPartialFunctionArg(): Unit = {}
  override def testConstructorAndClassParam(): Unit = {}
  override def testEither(): Unit = {}
  override def testSeveralLines(): Unit = {}
  override def testNestedLambdas(): Unit = {}
  override def testNestedLambdas2(): Unit = {}
}

@Category(Array(classOf[DebuggerTests]))
abstract class ExactBreakpointTest extends ExactBreakpointTestBase {
  override protected def supportedIn(version: ScalaVersion): Boolean = version == LatestScalaVersions.Scala_2_11

  addSourceFile("OneLine.scala",
    """object OneLine {
      |  def main(args: Array[String]): Unit = {
      |    Seq(1).map(x => x + 1).filter(_ > 10).foreach(println)
      |  }
      |}""".stripMargin.trim
  )
  def testOneLine(): Unit = {
    checkVariants(lineNumber = 2, "All", "line in function main", "x => x + 1", "_ > 10", "println")

    checkStopResumeSeveralTimes(Breakpoint(2, null))("Seq(1).map(...", "x => x + 1", "_ > 10")
    checkStoppedAtBreakpointAt(Breakpoint(2, -1))("Seq(1).map(...")
    checkStoppedAtBreakpointAt(Breakpoint(2, 0))("x => x + 1")
    checkStoppedAtBreakpointAt(Breakpoint(2, 1))("_ > 10")
    checkNotStoppedAtBreakpointAt(Breakpoint(2, 2))
  }

  addSourceFile("Either.scala",
    """object Either {
      |  def main(args: Array[String]): Unit = {
      |    val x: Either[String, Int] = Right(1)
      |    val y: Either[String, Int] = Left("aaa")
      |
      |    x.fold(_.substring(1), _ + 1)
      |    y.fold(_.substring(2), _ + 2)
      |  }
      |}""".stripMargin.trim
  )
  def testEither(): Unit = {
    checkVariants(lineNumber = 5, "All", "line in function main", "_.substring(1)", "_ + 1")
    checkStopResumeSeveralTimes(Breakpoint(5, null), Breakpoint(6, null))("x.fold(...", "_ + 1", "y.fold(...", "_.substring(2)")
    checkStoppedAtBreakpointAt(Breakpoint(5, 1))("_ + 1")
    checkStoppedAtBreakpointAt(Breakpoint(6, 0))("_.substring(2)")
    checkNotStoppedAtBreakpointAt(Breakpoint(5, 0))
    checkNotStoppedAtBreakpointAt(Breakpoint(6, 1))
  }

  addSourceFile("SeveralLines.scala",
    """object SeveralLines {
      |  def main(args: Array[String]): Unit = {
      |    Option("aaa").flatMap(_.headOption)
      |      .find(c => c.isDigit).getOrElse('0')
      |  }
      |}""".stripMargin.trim
  )
  def testSeveralLines(): Unit = {
    checkVariants(2, "All", "line in function main", "_.headOption")
    checkVariants(3, "All", "line in function main", "c => c.isDigit", "'0'")

    checkStopResumeSeveralTimes(Breakpoint(2, null), Breakpoint(3, null))("Option(\"aaa\")...", "_.headOption", ".find(...", "c => c.isDigit", "'0'")
    checkStopResumeSeveralTimes(Breakpoint(2, -1), Breakpoint(3, -1))("Option(...", ".find(...")
    checkStopResumeSeveralTimes(Breakpoint(2, 0), Breakpoint(3, 0))("_.headOption", "c => c.isDigit")
  }

  addSourceFile("NestedLambdas.scala",
    """object NestedLambdas {
      |  def main(args: Array[String]): Unit = {
      |    Seq("a").flatMap(x => x.find(_ == 'a').getOrElse('a').toString).foreach(c => println(Some(c).filter(_ == 'a').getOrElse('b')))
      |  }
      |}""".stripMargin.trim
  )
  def testNestedLambdas(): Unit = {
    checkVariants(2,
      "All",
      "line in function main",
      "x => x.find(_ == 'a').getOrElse('a').toString",
      "_ == 'a'",
      "'a'",
      "c => println(Some(c).filter(_ == 'a').getOrElse('b'))",
      "_ == 'a'",
      "'b'")

    checkStopResumeSeveralTimes(Breakpoint(2, null))("Seq(\"a\").flatMap(...", "x => x.find(...", "_ == 'a'", "c => println...", "_ == 'a'")
    checkNotStoppedAtBreakpointAt(Breakpoint(2, 2))
    checkNotStoppedAtBreakpointAt(Breakpoint(2, 5))
  }

  addSourceFile("NestedLambdas2.scala",
    """object NestedLambdas2 {
      |  def main(args: Array[String]): Unit = {
      |    Seq("b").flatMap(x => x.find(_ == 'a').getOrElse('a').toString).foreach(c => println(Some(c).filter(_ == 'b').getOrElse('a')))
      |  }
      |}""".stripMargin.trim
  )
  def testNestedLambdas2(): Unit = {
    checkVariants(2,
      "All",
      "line in function main",
      "x => x.find(_ == 'a').getOrElse('a').toString",
      "_ == 'a'",
      "'a'",
      "c => println(Some(c).filter(_ == 'b').getOrElse('a'))",
      "_ == 'b'",
      "'a'")

    checkStopResumeSeveralTimes(Breakpoint(2, null))("Seq(\"b\").flatMap(...", "x => x.find(...", "_ == 'a'", "'a'", "c => println...", "_ == 'b'", "'a'")
    checkStoppedAtBreakpointAt(Breakpoint(2, 1))("_ == 'a'")
    checkStoppedAtBreakpointAt(Breakpoint(2, 2))("'a'")
    checkStoppedAtBreakpointAt(Breakpoint(2, 4))("_ == 'b'")
    checkStoppedAtBreakpointAt(Breakpoint(2, 5))("'a'")
  }

  addSourceFile("ConstructorAndClassParam.scala",
    """object ConstructorAndClassParam {
      |  def main(args: Array[String]): Unit = {
      |    new BBB()
      |  }
      |}
      |
      |class BBB extends AAA("a3".filter(_.isDigit)) {
      |  Seq(1).map(x => x + 1).filter(_ > 10)
      |}
      |
      |class AAA(s: String)""".stripMargin.trim
  )
  def testConstructorAndClassParam(): Unit = {
    checkVariants(6, "All", "constructor of BBB", "_.isDigit")
    checkStopResumeSeveralTimes(Breakpoint(6, null), Breakpoint(10, null))("class BBB ...", "_.isDigit", "_.isDigit", "class AAA(...")
  }

  addSourceFile("EarlyDefAndTemplateBody.scala",
    """object EarlyDefAndTemplateBody {
      |  def main(args: Array[String]): Unit = {
      |    new CCC()
      |  }
      |}
      |
      |class CCC extends {
      |  val x = None.getOrElse(Seq(1)).filter(_ > 0)
      |} with DDD("") {
      |  Seq(1).map(x => x + 1).filter(_ > 10)
      |}
      |
      |class DDD(s: String)""".stripMargin.trim
  )
  def testEarlyDefAndTemplateBody(): Unit = {
    checkVariants(7, "All", "early definitions of CCC", "Seq(1)", "_ > 0")
    checkVariants(9, "All", "line in containing block", "x => x + 1", "_ > 10")

    checkStopResumeSeveralTimes(Breakpoint(7, null), Breakpoint(9, null))("val x = ...", "Seq(1)", "_ > 0", "Seq(1).map...", "x => x + 1", "_ > 10")
  }

  addSourceFile("NewTemplateDefinitionAsLambda.scala",
    """object NewTemplateDefinitionAsLambda {
      |  def main(args: Array[String]): Unit = {
      |    Seq("a").map(new ZZZ(_)).filter(_ => false).headOption.getOrElse(new ZZZ("1"))
      |  }
      |}
      |
      |class ZZZ(s: String)""".stripMargin.trim
  )
  def testNewTemplateDefinitionAsLambda(): Unit = {
    checkVariants(2, "All", "line in function main", "new ZZZ(_)", "_ => false", "new ZZZ(\"1\")")
    checkStopResumeSeveralTimes(Breakpoint(2, null))("Seq(\"a\")...", "new ZZZ(_)", "_ => false", "new ZZZ(\"1\")")
  }

  addSourceFile("LineStartsWithDot.scala",
    """object LineStartsWithDot {
      |  def main(args: Array[String]): Unit = {
      |    Some(1)
      |      .map(_ + 1)
      |      .filter(i => i % 2 == 0)
      |      .foreach(println)
      |  }
      |}""".stripMargin
  )
  def testLineStartsWithDot(): Unit = {
    checkVariants(2) //no variants
    checkVariants(3, "All", "line in function main", "_ + 1")
    checkVariants(4, "All", "line in function main", "i => i % 2 == 0")
    checkVariants(5, "All", "line in function main", "println")

    checkStopResumeSeveralTimes(Breakpoint(2, null), Breakpoint(3, -1), Breakpoint(4, 0), Breakpoint(5, null))(
      "Some(1)", ".map...", "i => i % 2 == 0", ".foreach...", "println"
    )
  }

  addSourceFile("PartialFunctionArg.scala",
     """object PartialFunctionArg {
       |  def main(args: Array[String]): Unit = {
       |    Seq(Option(1)).exists {
       |      case None =>
       |        true
       |      case Some(i) =>
       |        false
       |    }
       |  }
       |}
    """.stripMargin.trim)
  def testPartialFunctionArg(): Unit = {
    checkStopResumeSeveralTimes(Breakpoint(5, null), Breakpoint(6, null))(
      "case Some(i) =>", "false"
    )
  }

  addSourceFile("LikeDefaultArgName.scala",
  """object LikeDefaultArgName {
    |  def main(args: Array[String]): Unit = {
    |    def default() = {
    |      "stop here"
    |    }
    |
    |    None.getOrElse(default())
    |  }
    |}""".stripMargin)

  def testLikeDefaultArgName(): Unit = {
    checkStopResumeSeveralTimes(Breakpoint(3, null))(
      "\"stop here\""
    )
  }

  addSourceFile("BreakpointInTrait.scala",
  """object BreakpointInTrait {
    |  def main(args: Array[String]): Unit = {
    |    val a = new AAA
    |    a.foo()
    |    a.foo("x")
    |  }
    |
    |  class AAA extends TraitExample
    |}
    |
    |trait TraitExample extends SecondTrait {
    |  val x = 1
    |
    |  def foo(): Unit = {
    |    2
    |  }
    |}
    |
    |trait SecondTrait {
    |  def foo(s: String): Unit = {
    |    3
    |  }
    |}
  """.stripMargin)
  def testBreakpointInTrait(): Unit = {
    checkStopResumeSeveralTimes(Breakpoint(11, null), Breakpoint(14, null), Breakpoint(20, null))(
      "val x = 1",
      "2",
      "3"
    )
  }

  addSourceFile("test/name_with_backticks/`class with backticks`.scala",
  s"""package test.`name_in_backticks`
     |
     |class `class with backticks` {
     |  def foo(): Unit = {
     |    println(1)
     |  }
     |
     |  def `method with backticks`(): Unit = {
     |    println(2)
     |  }
     |}
     |""".stripMargin)
  addSourceFile("BreakpointWithBackticks.scala",
    """import test.`name_in_backticks`.`class with backticks`
      |
      |object BreakpointWithBackticks {
      |  def main(args: Array[String]): Unit = {
      |    new `class with backticks`().foo()
      |    new `class with backticks`().`method with backticks`()
      |  }
      |}""".stripMargin)
  def testBreakpointWithBackticks(): Unit = {
    val fileWithBackticks = "test/name_with_backticks/`class with backticks`.scala"
    checkStopResumeSeveralTimes(Breakpoint(4, null, fileWithBackticks), Breakpoint(8, null, fileWithBackticks))(
      "println(1)",
      "println(2)",
    )
  }
}