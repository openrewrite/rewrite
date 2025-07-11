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
import org.openrewrite.java.tree.{J, Space, Statement}
import org.openrewrite.Tree

import java.util.{ArrayList, Collections, List as JList}

/**
 * Result of converting a Scala AST to compilation unit components.
 */
class CompilationUnitResult(
  val packageDecl: J.Package,
  val imports: JList[J.Import],
  val statements: JList[Statement]
) {
  def getPackageDecl: J.Package = packageDecl
  def getImports: JList[J.Import] = imports
  def getStatements: JList[Statement] = statements
}

/**
 * Java-callable wrapper for converting Scala AST to OpenRewrite LST.
 */
class ScalaASTConverter {
  
  /**
   * Converts a Scala parse result to compilation unit components.
   */
  def convertToCompilationUnit(parseResult: ScalaParseResult, source: String): CompilationUnitResult = {
    val imports = new ArrayList[J.Import]()
    val statements = new ArrayList[Statement]()
    var packageDecl: J.Package = null
    
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
    
    // Check if tree is empty (parse error case)
    if (tree.isEmpty) {
      // Return empty result for parse errors
      return CompilationUnitResult(packageDecl, imports, statements)
    }
    
    // Handle different types of top-level trees
    tree match {
      case pkgDef: untpd.PackageDef =>
        // For now, just preserve the entire package as Unknown
        // Proper package/import extraction will be implemented later
        val converted = visitor.visitTree(tree)
        if (converted.isInstanceOf[Statement]) {
          statements.add(converted.asInstanceOf[Statement])
        }
      case _ =>
        // Single statement
        val converted = visitor.visitTree(tree)
        if (converted.isInstanceOf[Statement]) {
          statements.add(converted.asInstanceOf[Statement])
        }
    }
    
    new CompilationUnitResult(packageDecl, imports, statements)
  }
  
  /**
   * Creates a J.Package from a Scala PackageDef.
   */
  private def createPackageDeclaration(pkgDef: untpd.PackageDef, visitor: ScalaTreeVisitor): J.Package = {
    // For now, create a simple package declaration
    // The package name is in pkgDef.pid (package identifier)
    val packageName = pkgDef.pid match {
      case id: untpd.Ident => id.name.toString
      case sel: untpd.Select => extractQualifiedName(sel)
      case _ => ""
    }
    
    // Create a simple identifier for now - proper package expression building will be added later
    val expr = new J.Identifier(
      Tree.randomId(),
      Space.EMPTY,
      org.openrewrite.marker.Markers.EMPTY,
      Collections.emptyList(),
      packageName,
      null,
      null
    )
    
    new J.Package(
      Tree.randomId(),
      visitor.extractPrefix(pkgDef.span),
      org.openrewrite.marker.Markers.EMPTY,
      expr,
      Collections.emptyList()
    )
  }
  
  /**
   * Extracts a qualified name from a Select tree.
   */
  private def extractQualifiedName(sel: untpd.Select): String = {
    sel.qualifier match {
      case id: untpd.Ident => s"${id.name}.${sel.name}"
      case innerSel: untpd.Select => s"${extractQualifiedName(innerSel)}.${sel.name}"
      case _ => sel.name.toString
    }
  }
  
  /**
   * Converts a Scala parse result to a list of statements (backward compatibility).
   */
  def convertToStatements(parseResult: ScalaParseResult, source: String): JList[Statement] = {
    convertToCompilationUnit(parseResult, source).statements
  }
  
  /**
   * Gets the remaining source after parsing (for EOF space).
   */
  def getRemainingSource(parseResult: ScalaParseResult, source: String): String = {
    given Context = dotty.tools.dotc.core.Contexts.NoContext
    
    // If tree is empty (parse error), don't return any remaining source
    // The Unknown node will handle the entire source
    if (parseResult.tree.isEmpty) {
      return ""
    }
    
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