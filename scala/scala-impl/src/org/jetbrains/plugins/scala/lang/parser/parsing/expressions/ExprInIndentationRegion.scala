package org.jetbrains.plugins.scala
package lang
package parser
package parsing
package expressions

import com.intellij.psi.tree.IElementType
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.parsing.base.Extension
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilder
import org.jetbrains.plugins.scala.lang.parser.parsing.statements.{ConstrBlock, Def}
import org.jetbrains.plugins.scala.lang.parser.parsing.top.TmplDef

import scala.annotation.tailrec

sealed trait ExprInIndentationRegion extends ParsingRule {
  protected def exprParser: ParsingRule
  protected def exprPartParser: ParsingRule = exprParser
  protected def blockType: IElementType = ScCodeBlockElementType.BlockExpression

  private final val isFollowSetIfIndented = Set(
    ScalaTokenTypes.tRPARENTHESIS,
    ScalaTokenTypes.tRBRACE,
    ScalaTokenTypes.kELSE,
    ScalaTokenTypes.kCATCH,
    ScalaTokenTypes.kFINALLY,
    ScalaTokenTypes.tCOMMA,
  )

  override def parse(implicit builder: ScalaPsiBuilder): Boolean = {
    if (!builder.isScala3 || !builder.isScala3IndentationBasedSyntaxEnabled) {
      return exprParser()
    }
    if (builder.getTokenType == ScalaTokenTypes.tLBRACE) {
      return exprParser()
    }

    val prevIndent = builder.findPreviousIndent
    val indentationForExprBlock = prevIndent match {
      case Some(indent) => indent
      case None =>
        return exprParser() // expression doesn't start from a new line, parse single expression
    }
    val parentIndent = builder.currentIndentationWidth
    indentationForExprBlock.compare(parentIndent) match {
      case x if x > 0  => // ok, expression is indented
      case x if x == 0 =>
        return exprParser() // expression is not indented, parse single expression
      case _           =>
        val errorMarker = builder.mark()
        errorMarker.error(ScalaBundle.message("line.is.indented.too.far.to.the.left"))
        val parsed = exprParser()
        if (!parsed) {
          // we do not want to show the error if we do not have valid expression,
          // e.g. in `class A {\n  def foo = \n}`
          errorMarker.drop()
        }
        return parsed // expression is unindented, parse single expression
    }

    val blockMarker = builder.mark()
    builder.withIndentationWidth(indentationForExprBlock) {

      blockMarker.setCustomEdgeTokenBinders(ScalaTokenBinders.PRECEDING_WS_AND_COMMENT_TOKENS, null)

      // We need to early parse those definitions which begin with a soft keyword
      // (extension, inline, transparent, infix, open)
      // If we don't parse them early, they will be wrongly parsed as expressions with errors.
      // For example `extension (x: String)` will be parsed as a method call
      val firstParsedAsDefWithSoftKeyword = Extension() || Def() || TmplDef()

      val firstParsedAsExpr =
        !firstParsedAsDefWithSoftKeyword && exprPartParser()

      val firstParsedAsBlockStat =
        firstParsedAsDefWithSoftKeyword || !firstParsedAsExpr && BlockStat()

      val firstParsed = firstParsedAsExpr || firstParsedAsBlockStat

      @tailrec
      def parseRest(isBlock: Boolean): Boolean = {
        def isOutdent = builder.findPreviousIndent.exists(_ < indentationForExprBlock)
        if (!isOutdent) {
          val tt = builder.getTokenType
          if (tt == ScalaTokenTypes.tSEMICOLON) {
            builder.advanceLexer() // ate ;
            parseRest(isBlock = true)
          } else if (builder.eof() || isFollowSetIfIndented(builder.getTokenType)) {
            isBlock
          } else if (!ResultExpr(stopOnOutdent = true) && !BlockStat()) {
            builder.advanceLexer() // ate something
            parseRest(isBlock = true)
          } else {
            parseRest(isBlock = true)
          }
        } else {
          isBlock
        }
      }

      /**
       * If the first body element is not an expression, we also wrap it into block.
       * E.g. in such silly definition with a single variable definition: {{{
       *   def foo =
       *     var inner = 42
       * }}}
       */
      if (parseRest(isBlock = false) || firstParsedAsBlockStat) {
        blockMarker.done(blockType)
        true
      } else {
        blockMarker.drop()
        firstParsed
      }
    }
  }
}

object ExprInIndentationRegion extends ExprInIndentationRegion {
  override protected def exprParser: ParsingRule = Expr
}

object PostfixExprInIndentationRegion extends ExprInIndentationRegion {
  override protected def exprParser: ParsingRule = PostfixExpr
}

object ConstrExprInIndentationRegion extends ExprInIndentationRegion {
  override protected def exprParser: ParsingRule = ConstBlockExpr
  override protected def exprPartParser: ParsingRule = new ParsingRule {
    override def parse(implicit builder: ScalaPsiBuilder): Boolean =
      ConstBlockExpr.parseFirstConstrBlockExpr()
  }
  override protected def blockType: IElementType = ScalaElementType.CONSTR_BLOCK_EXPR
}


private object ConstBlockExpr extends ParsingRule {
  override def parse(implicit builder: ScalaPsiBuilder): Boolean = {
    if (builder.getTokenType == ScalaTokenTypes.tLBRACE)
      ConstrBlock()
    else
      parseFirstConstrBlockExpr()
  }

  // We expect a self invocation as first statement/sole expression,
  // but if there is no self invocation,
  // don't fail and just parse an expression.
  // This will make the following parse
  //
  //   def this() = ???
  def parseFirstConstrBlockExpr()(implicit builder: ScalaPsiBuilder): Boolean =
    SelfInvocation() || Expr()
}