/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.internal

import dotty.tools.dotc.ast.untpd
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.ast.Trees.*

object ScalaASTDebugTest {
  def main(args: Array[String]): Unit = {
    val bridge = new ScalaCompilerBridge()
    
    // Test binary expression
    val result = bridge.parse("test.scala", "1 + 2")
    
    println(s"Wrapped: ${result.wasWrapped}")
    println(s"Tree class: ${result.tree.getClass.getName}")
    
    // Print tree structure
    printTree(result.tree, 0)
  }
  
  def printTree(tree: untpd.Tree, indent: Int): Unit = {
    val prefix = " " * (indent * 2)
    tree match {
      case app: untpd.Apply =>
        println(s"${prefix}Apply:")
        println(s"${prefix}  fun:")
        printTree(app.fun, indent + 2)
        println(s"${prefix}  args:")
        app.args.foreach(arg => printTree(arg, indent + 2))
      case sel: untpd.Select =>
        println(s"${prefix}Select(name=${sel.name}):")
        printTree(sel.qualifier, indent + 1)
      case id: untpd.Ident =>
        println(s"${prefix}Ident(name=${id.name})")
      case lit: untpd.Literal =>
        println(s"${prefix}Literal(value=${lit.const.value})")
      case block: untpd.Block =>
        println(s"${prefix}Block:")
        block.stats.foreach(stat => printTree(stat, indent + 1))
        println(s"${prefix}  expr:")
        printTree(block.expr, indent + 2)
      case vd: untpd.ValDef =>
        println(s"${prefix}ValDef(name=${vd.name})")
      case td: untpd.TypeDef =>
        println(s"${prefix}TypeDef(name=${td.name})")
      case _ =>
        println(s"${prefix}${tree.getClass.getSimpleName}${if (tree.isEmpty) " (empty)" else ""}")
    }
  }
}