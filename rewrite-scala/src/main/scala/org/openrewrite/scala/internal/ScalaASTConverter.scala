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
import org.openrewrite.Tree
import org.openrewrite.java.tree.*
import org.openrewrite.marker.Markers

import java.util
import java.util.{Collections, List as JList}

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
    val imports = new util.ArrayList[J.Import]()
    val statements = new util.ArrayList[Statement]()
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

    // Debug: Print tree structure
    System.out.println(s"Top-level tree: ${tree.getClass.getSimpleName}")
    System.out.println(s"Tree span: ${tree.span}")
    
    // Check if tree is empty (parse error case)
    if (tree.isEmpty) {
      // Return empty result for parse errors
      return CompilationUnitResult(packageDecl, imports, statements)
    }

    // Handle different types of top-level trees
    tree match {
      case pkgDef: untpd.PackageDef =>
        // Extract package declaration and create J.Package using the visitor
        // This ensures the cursor is properly updated
        val packageName = extractPackageName(pkgDef)
        if (packageName.nonEmpty && packageName != "<empty>") {
          // Create package with proper prefix tracking
          packageDecl = createPackageDeclaration(pkgDef, visitor)
        }

        // Process the statements within the package
        pkgDef.stats.foreach {
          case _: untpd.PackageDef =>
          case imp: untpd.Import =>
            // Handle imports - simple ones as J.Import, complex ones as statements
            val converted = visitor.visitTree(imp)
            converted match {
              case jImport: J.Import =>
                imports.add(jImport)
              case unknown: J.Unknown =>
                // Complex imports that we can't map to J.Import yet
                statements.add(unknown)
              case null =>
              case _: J.Empty =>
              case _ =>
            }
          case stat =>
            val converted = visitor.visitTree(stat)
            converted match {
              case null =>
              case _: J.Empty =>
              case stmt: Statement =>
                statements.add(stmt)
              case other =>
            }
        }
      case imp: untpd.Import =>
        // Top-level import - simple ones as J.Import, complex ones as statements
        val converted = visitor.visitTree(imp)
        converted match {
          case jImport: J.Import =>
            imports.add(jImport)
          case unknown: J.Unknown =>
            // Complex imports that we can't map to J.Import yet
            statements.add(unknown)
          case null => // Skip null returns
          case _: J.Empty => // Skip empty nodes
          case _ => // Skip non-statements
        }
      case _ =>
        // Single statement
        System.out.println(s"Processing single statement: ${tree.getClass.getSimpleName}")
        val converted = visitor.visitTree(tree)
        converted match {
          case null => // Skip null returns
          case _: J.Empty => // Skip empty nodes
          case stmt: Statement => statements.add(stmt)
          case _ => // Skip non-statements
        }
    }

    new CompilationUnitResult(packageDecl, imports, statements)
  }

  /**
   * Creates a J.Package from a Scala PackageDef.
   */
  private def createPackageDeclaration(pkgDef: untpd.PackageDef, visitor: ScalaTreeVisitor): J.Package = {
    // Extract the prefix (whitespace before 'package' keyword)
    val prefix = visitor.extractPrefix(pkgDef.span)

    // Extract the package name
    val packageName = extractPackageName(pkgDef)

    // Find the end of the package declaration in the source
    // This includes "package" keyword + package name
    val packageEndPos = pkgDef.pid.span.end

    // Update the visitor's cursor to after the package declaration
    // This is crucial to prevent the package text from being included
    // in the prefix of subsequent statements
    visitor.updateCursor(packageEndPos)

    // Create package expression
    val packageExpr: Expression = TypeTree.build(packageName)

    new J.Package(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      packageExpr.withPrefix(Space.build(" ", Collections.emptyList())),
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
   * Extracts the package name from a PackageDef.
   */
  private def extractPackageName(pkg: untpd.PackageDef): String = {
    pkg.pid match {
      case id: untpd.Ident => id.name.toString
      case sel: untpd.Select => extractQualifiedName(sel)
      case _ => ""
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

    // For package definitions, we need to handle them specially
    // to avoid visiting the package itself
    parseResult.tree match {
      case pkgDef: untpd.PackageDef =>
        // Visit only the statements within the package
        pkgDef.stats.foreach(visitor.visitTree)
      case tree =>
        // For other trees, visit normally
        visitor.visitTree(tree)
    }

    visitor.getRemainingSource
  }
}