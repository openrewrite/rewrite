/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.internal

import dotty.tools.dotc.CompilationUnit
import dotty.tools.dotc.ast.{tpd, untpd}
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Types.{Type, TypeVars}
import dotty.tools.dotc.typer.{Typer, TyperPhase}

import java.util.{Collections, IdentityHashMap}
import scala.collection.mutable

/**
 * What the typer made of a node of the untyped tree: `own` is the node typed on its own,
 * `adapted` is that tree after adapting it to the type its context expects, which may wrap
 * it, for instance in an implicit conversion or an eta-expansion.
 */
final case class TypedNode(own: tpd.Tree, adapted: tpd.Tree)

/**
 * The typed trees of the nodes of the untyped tree an LST is built from. The typer works on
 * a parse of its own, so nodes are paired with their counterparts in that parse by their
 * source range, kind and order of appearance.
 */
final class TypedNodes private (nodes: IdentityHashMap[untpd.Tree, TypedNode]) {
  def get(tree: untpd.Tree): Option[TypedNode] = Option(nodes.get(tree))

  // The innermost node each typed tree is the typing or adaptation of
  private lazy val owners: IdentityHashMap[tpd.Tree, untpd.Tree] = {
    val owners = new IdentityHashMap[tpd.Tree, untpd.Tree]()
    def claim(typed: tpd.Tree, tree: untpd.Tree): Unit = {
      val current = owners.get(typed)
      if (current == null || encloses(current, tree)) owners.put(typed, tree)
    }
    nodes.forEach { (tree, node) =>
      claim(node.adapted, tree)
      claim(node.own, tree)
    }
    owners
  }

  private def encloses(outer: untpd.Tree, inner: untpd.Tree): Boolean =
    outer.span.contains(inner.span) && outer.span != inner.span

  /** The node of the untyped tree that the typed tree is the typing or adaptation of, if any. */
  def ownerOf(tree: tpd.Tree): Option[untpd.Tree] = Option(owners.get(tree))
}

object TypedNodes {
  val empty: TypedNodes = new TypedNodes(new IdentityHashMap())

  private[internal] def pair(parsed: untpd.Tree, typing: UntypedNodeIndex, recorder: TyperRecorder)(using Context): TypedNodes = {
    val nodes = new IdentityHashMap[untpd.Tree, TypedNode]()
    UntypedNodeIndex(parsed).foreach { (key, trees) =>
      val counterparts = typing.at(key)
      trees.iterator.zip(counterparts.iterator).foreach { (tree, counterpart) =>
        recorder.get(counterpart).foreach(nodes.put(tree, _))
      }
    }
    new TypedNodes(nodes)
  }
}

/** The nodes of an untyped tree that have a source range, by range and kind, in pre-order. */
private[internal] final class UntypedNodeIndex private (nodes: mutable.Map[UntypedNodeIndex.Key, mutable.ArrayBuffer[untpd.Tree]]) {
  def at(key: UntypedNodeIndex.Key): collection.Seq[untpd.Tree] = nodes.getOrElse(key, Nil)

  def foreach(f: (UntypedNodeIndex.Key, collection.Seq[untpd.Tree]) => Unit): Unit = nodes.foreach(f.tupled)
}

private[internal] object UntypedNodeIndex {
  type Key = (Int, Int, Class[?])

  def apply(tree: untpd.Tree)(using Context): UntypedNodeIndex = {
    val nodes = mutable.HashMap.empty[Key, mutable.ArrayBuffer[untpd.Tree]]
    try {
      new untpd.UntypedTreeTraverser {
        override def traverse(tree: untpd.Tree)(using Context): Unit = {
          if (tree.span.exists) {
            nodes.getOrElseUpdate((tree.span.start, tree.span.end, tree.getClass), mutable.ArrayBuffer.empty) += tree
          }
          traverseChildren(tree)
        }
      }.traverse(tree)
    } catch {
      case _: Throwable =>
    }
    new UntypedNodeIndex(nodes)
  }
}

/**
 * Typed trees by the untyped tree the typer produced them from, and adapted trees by the
 * typed tree they were adapted from. When a tree is typed or adapted more than once, the
 * last result wins. Arguments of calls are typed and adapted in separate steps, so the
 * adaptation can't be recorded together with the typing.
 *
 * Only the tracked nodes of the parsed source are recorded, and only the adaptations of their
 * latest typings, so trees the typer builds and discards along the way, for instance while
 * searching implicits or resolving overloads, aren't kept alive.
 */
private[internal] final class TyperRecorder {
  private val sources = Collections.newSetFromMap(new IdentityHashMap[untpd.Tree, java.lang.Boolean]())
  private val typed = new IdentityHashMap[untpd.Tree, tpd.Tree]()
  // How many source nodes each typed tree is currently the typing of
  private val typings = new IdentityHashMap[tpd.Tree, Integer]()
  private val adapted = new IdentityHashMap[tpd.Tree, tpd.Tree]()

  def track(index: UntypedNodeIndex): Unit = index.foreach((_, trees) => trees.foreach(sources.add))

  def recordTyped(tree: untpd.Tree, result: tpd.Tree): Unit =
    if (sources.contains(tree) && !isCachedAdaptation(tree, result)) {
      val previous = typed.put(tree, result)
      if (previous ne result) {
        if (previous != null) release(previous)
        val count = typings.get(result)
        typings.put(result, if (count == null) 1 else count + 1)
      }
    }

  /**
   * A tree typed ahead, such as the right-hand side of a definition whose type is inferred,
   * is typed again as the adapted tree the typer cached the first time around.
   */
  private def isCachedAdaptation(tree: untpd.Tree, result: tpd.Tree): Boolean = {
    val previous = typed.get(tree)
    previous != null && (previous ne result) && (adapted.get(previous) eq result)
  }

  private def release(typing: tpd.Tree): Unit = {
    val count = typings.get(typing)
    if (count == null || count <= 1) {
      typings.remove(typing)
      adapted.remove(typing)
    } else typings.put(typing, count - 1)
  }

  def recordAdapted(tree: tpd.Tree, result: tpd.Tree): Unit =
    if (typings.containsKey(tree)) adapted.put(tree, result)

  def get(tree: untpd.Tree): Option[TypedNode] =
    Option(typed.get(tree)).map(own => TypedNode(own, Option(adapted.get(own)).getOrElse(own)))

  def clear(): Unit = {
    sources.clear()
    typed.clear()
    typings.clear()
    adapted.clear()
  }
}

private[internal] class RecordingTyper(recorder: TyperRecorder, nestingLevel: Int = 0) extends Typer(nestingLevel) {
  override def newLikeThis(nestingLevel: Int): Typer = new RecordingTyper(recorder, nestingLevel)

  override def typedUnadapted(initTree: untpd.Tree, pt: Type, locked: TypeVars)(using Context): tpd.Tree = {
    val typed = super.typedUnadapted(initTree, pt, locked)
    recorder.recordTyped(initTree, typed)
    typed
  }

  override def adapt(tree: tpd.Tree, pt: Type, locked: TypeVars)(using Context): tpd.Tree = {
    val adapted = super.adapt(tree, pt, locked)
    recorder.recordAdapted(tree, adapted)
    adapted
  }
}

private[internal] class RecordingTyperPhase(recorder: TyperRecorder) extends TyperPhase {
  override def runOn(units: List[CompilationUnit])(using ctx: Context): List[CompilationUnit] =
    super.runOn(units)(using ctx.fresh.setTyper(new RecordingTyper(recorder)))
}
