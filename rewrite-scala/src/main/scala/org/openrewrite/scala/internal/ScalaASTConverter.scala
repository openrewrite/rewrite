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

import dotty.tools.dotc.ast.Trees.*
import dotty.tools.dotc.ast.untpd
import dotty.tools.dotc.core.Contexts.*
import org.openrewrite.java.tree.{J, Statement}
import java.util.{List => JList, ArrayList}

/**
 * Java-callable wrapper for converting Scala AST to OpenRewrite LST.
 */
class ScalaASTConverter {
  
  /**
   * Converts a Scala parse result to a list of statements.
   */
  def convertToStatements(parseResult: ScalaParseResult, source: String): JList[Statement] = {
    val statements = new ArrayList[Statement]()
    
    // Get the implicit context from the parse result's tree
    given Context = dotty.tools.dotc.core.Contexts.NoContext
    
    // Calculate offset adjustment if content was wrapped
    val offsetAdjustment = if (parseResult.wasWrapped) {
      "object ExprWrapper { val result = ".length
    } else {
      0
    }
    
    val visitor = new ScalaTreeVisitor(source, offsetAdjustment)
    val tree = parseResult.tree
    
    // Handle different types of top-level trees
    tree match {
      case pkgDef: untpd.PackageDef =>
        // Package definition contains multiple statements
        pkgDef.stats.foreach { stat =>
          val converted = visitor.visitTree(stat)
          if (converted.isInstanceOf[Statement]) {
            statements.add(converted.asInstanceOf[Statement])
          }
        }
      case _ =>
        // Single statement
        val converted = visitor.visitTree(tree)
        if (converted.isInstanceOf[Statement]) {
          statements.add(converted.asInstanceOf[Statement])
        }
    }
    
    statements
  }
  
  /**
   * Gets the remaining source after parsing (for EOF space).
   */
  def getRemainingSource(parseResult: ScalaParseResult, source: String): String = {
    given Context = dotty.tools.dotc.core.Contexts.NoContext
    val offsetAdjustment = if (parseResult.wasWrapped) {
      "object ExprWrapper { val result = ".length
    } else {
      0
    }
    val visitor = new ScalaTreeVisitor(source, offsetAdjustment)
    visitor.visitTree(parseResult.tree)
    visitor.getRemainingSource
  }
}