package org.jetbrains.plugins.scala.lang.dfa.testlang

import com.intellij.codeInspection.dataFlow.interpreter.{RunnerResult, StandardDataFlowInterpreter}
import com.intellij.codeInspection.dataFlow.jvm.JvmDfaMemoryStateImpl
import com.intellij.codeInspection.dataFlow.lang.ir.DfaInstructionState
import com.intellij.codeInspection.dataFlow.value.DfaValueFactory
import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestAdapter
import org.jetbrains.plugins.scala.dfa.testlang.LangParser
import org.jetbrains.plugins.scala.dfa.testlang.dfa.{TestLangControlFlowBuilder, TestLangDfaListener}

import scala.jdk.CollectionConverters._


class TestLangDfaTest extends ScalaLightCodeInsightFixtureTestAdapter {

  def test_simple(): Unit = {
    val code =
      """
        |3
        |2 + 3
        |""".stripMargin
    val program = LangParser.parse(code)

    val factory = new DfaValueFactory(getProject)
    val flow = new TestLangControlFlowBuilder(factory, program).buildFlow()
    println(flow)
    val listener = new TestLangDfaListener()
    val interpreter = new StandardDataFlowInterpreter(flow, listener)

    val states = List(new JvmDfaMemoryStateImpl(factory))
    val instructionStates = states.map {
      state => new DfaInstructionState(flow.getInstruction(0), state)
    }
    if (interpreter.interpret(instructionStates.asJava) == RunnerResult.OK) {
      println(listener)
      println("constant conditions:")
      println(listener.constantConditions)
    }
  }
}
