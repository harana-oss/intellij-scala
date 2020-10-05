package org.jetbrains.plugins.scala.dfa
package cfg

import scala.collection.SeqView

trait Block {
  type SourceInfo
  type Node = cfg.Node { type SourceInfo = Block.this.SourceInfo }

  def graph: Graph[SourceInfo]

  def name: String
  def index: Int

  def nodeBegin: Int
  def nodeEnd: Int

  def nodes: SeqView[Node]
  def nodeIndices: Range

  def incoming: Seq[Block]
  def outgoing: Seq[Block]

  final def headNode: Option[Node] = nodes.headOption
  final def lastNode: Option[Node] = nodes.lastOption
}
