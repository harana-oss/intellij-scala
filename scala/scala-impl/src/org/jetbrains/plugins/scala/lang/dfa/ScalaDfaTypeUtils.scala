package org.jetbrains.plugins.scala.lang.dfa

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.dataFlow.rangeSet.LongRangeBinOp
import com.intellij.codeInspection.dataFlow.types.{DfType, DfTypes}
import com.intellij.codeInspection.dataFlow.value.RelationType
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.scala.codeInspection.ScalaInspectionBundle
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.api.base.literals._

object ScalaDfaTypeUtils {

  final object InfixOperators {
    val Arithmetic: Map[String, LongRangeBinOp] = Map(
      "+" -> LongRangeBinOp.PLUS,
      "-" -> LongRangeBinOp.MINUS,
      "*" -> LongRangeBinOp.MUL,
      "/" -> LongRangeBinOp.DIV,
      "%" -> LongRangeBinOp.MOD
    )

    val Relational: Map[String, RelationType] = Map(
      "<" -> RelationType.LT,
      "<=" -> RelationType.LE,
      ">" -> RelationType.GT,
      ">=" -> RelationType.GE,
      "==" -> RelationType.EQ,
      "!=" -> RelationType.NE
    )

    val Logical: Map[String, LogicalOperation] = Map(
      "&&" -> LogicalOperation.And,
      "||" -> LogicalOperation.Or
    )
  }

  def literalToDfType(literal: ScLiteral): DfType = literal match {
    case _: ScNullLiteral => DfTypes.NULL
    case int: ScIntegerLiteral => DfTypes.intValue(int.getValue)
    case long: ScLongLiteral => DfTypes.longValue(long.getValue)
    case float: ScFloatLiteral => DfTypes.floatValue(float.getValue)
    case double: ScDoubleLiteral => DfTypes.doubleValue(double.getValue)
    case boolean: ScBooleanLiteral => DfTypes.booleanValue(boolean.getValue)
    case char: ScCharLiteral => DfTypes.intValue(char.getValue.toInt)
    case _ => DfType.TOP
  }

  def dfTypeToReportedConstant(dfType: DfType): DfaConstantValue = dfType match {
    case DfTypes.TRUE => DfaConstantValue.True
    case DfTypes.FALSE => DfaConstantValue.False
    case _ => DfaConstantValue.Unknown
  }

  @Nls
  def constantValueToProblemMessage(value: DfaConstantValue, warningType: ProblemHighlightType): String = value match {
    case DfaConstantValue.True => ScalaInspectionBundle.message("displayname.condition.always.true", warningType)
    case DfaConstantValue.False => ScalaInspectionBundle.message("displayname.condition.always.false", warningType)
    case _ => throw new IllegalStateException(s"Trying to report an unexpected DFA constant value: $value")
  }
}