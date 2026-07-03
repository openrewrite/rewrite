/*
 * Copyright 2025 the original author or authors.
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

import dotty.tools.dotc.ast.{Trees, untpd}
import dotty.tools.dotc.core.Contexts.*
import org.openrewrite.Tree
import org.openrewrite.java.internal.JavaTypeFactory
import org.openrewrite.java.tree.*
import org.openrewrite.marker.Markers
import org.openrewrite.scala.marker.{IndentedSyntax, PackageSemicolon}
import org.openrewrite.scala.tree.S

import java.util
import java.util.{Collections, List as JList}

/**
 * Result of converting a Scala AST to compilation unit components.
 * Imports (plain `J.Import` and brace-form `S.Import`) are part of
 * [[statements]] in source order — Scala allows them anywhere a statement
 * can appear, so there is no separate imports list.
 */
class CompilationUnitResult(
                             val packageDecl: J.Package,
                             val statements: JList[Statement],
                             val lastCursorPosition: Int
                           ) {
  def getPackageDecl: J.Package = packageDecl

  def getStatements: JList[Statement] = statements

  def getLastCursorPosition: Int = lastCursorPosition
}

/**
 * Java-callable wrapper for converting Scala AST to OpenRewrite LST.
 */
class ScalaASTConverter {

  /**
   * Converts a Scala parse result to compilation unit components.
   */
  def convertToCompilationUnit(parseResult: ScalaParseResult, source: String, typeFactory: JavaTypeFactory = null): CompilationUnitResult = {
    val statements = new util.ArrayList[Statement]()
    var packageDecl: J.Package = null

    // Use the context from the parse result (carries type info from batch compilation)
    given Context = if (parseResult.context != null) parseResult.context else dotty.tools.dotc.core.Contexts.NoContext

    // Calculate offset adjustment if content was wrapped before parsing.
    // For `.sbt` content wrapped as `object __SbtScript__ { ... }`, spans in
    // the parsed tree are offset by the wrapper prefix (`object __SbtScript__ {\n`).
    val offsetAdjustment =
      if (parseResult.wasObjectWrapped) ScalaParseResultConstants.ObjectScriptPrefix.length
      else if (parseResult.wasWrapped) "object ExprWrapper { val result = ".length
      else 0

    // Build type mapping from typed tree if available
    val mapping: Option[ScalaTypeMapping] = if (typeFactory != null) {
      parseResult.typedTree.map(tpd => new ScalaTypeMapping(typeFactory, tpd))
    } else None

    val visitor = new ScalaTreeVisitor(source, offsetAdjustment, mapping)
    // Traverse the untyped tree for source-faithful structure.
    // Types are extracted via ScalaTypeMapping's span-based lookup into the typed tree.
    val tree: Trees.Tree[?] = parseResult.tree

    // Check if tree is empty (parse error case)
    if (tree.isEmpty) {
      // Return empty result for parse errors
      return new CompilationUnitResult(packageDecl, statements, 0)
    }

    // Handle different types of top-level trees
    tree match {
      case pkgDef: Trees.PackageDef[?] if isBracedPackage(pkgDef, visitor) =>
        // A single top-level braced package is a scope that owns its body, so it
        // becomes a statement rather than the compilation unit's package header.
        statements.add(buildBracedPackage(pkgDef, visitor))
      case pkgDef: Trees.PackageDef[?] =>
        // Extract package declaration and create J.Package using the visitor
        // This ensures the cursor is properly updated
        val packageName = extractPackageName(pkgDef)
        if (packageName.nonEmpty && packageName != "<empty>") {
          // Create package with proper prefix tracking
          packageDecl = createPackageDeclaration(pkgDef, visitor)
        }

        // For `.sbt` content the bridge wrapped statements in
        // `object __SbtScript__ { ... }` so Dotty would accept them at the
        // file level. Lift the wrapper's body back up here so downstream
        // visiting sees the original statements as direct children of the
        // compilation unit. Filter out empty/synthetic trees Dotty may emit
        // for the object's constructor — those carry no span and would crash
        // visitUnknown.
        val rawStats: Seq[Trees.Tree[?]] =
          if (parseResult.wasObjectWrapped) {
            pkgDef.stats.collectFirst {
              case mod: untpd.ModuleDef
                if mod.name.toString == ScalaParseResultConstants.ObjectScriptWrapperName =>
                mod.impl.body.filter(s => !s.isEmpty && s.span.exists)
            }.getOrElse(pkgDef.stats)
          } else pkgDef.stats

        statements.addAll(convertBody(rawStats, visitor))
      case imp: Trees.Import[?] =>
        // Top-level import (no enclosing package).
        val converted = visitor.visitTree(imp)
        converted match {
          case stmt: Statement => statements.add(stmt)
          case null => // Skip null returns
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

    new CompilationUnitResult(packageDecl, statements, visitor.getCursor)
  }

  /**
   * Converts the body statements of a package (or the compilation unit) into a flat
   * list, visiting in source order so the visitor's cursor advances monotonically.
   * Braced packages nested among the statements become [[S.PackageDeclaration]] so
   * they keep their own scope; non-braced nested packages are not yet modeled.
   */
  private def convertBody(rawStats: Seq[Trees.Tree[?]], visitor: ScalaTreeVisitor): util.ArrayList[Statement] = {
    val out = new util.ArrayList[Statement]()
    // Sort by source position to ensure source order is preserved —
    // the Dotty parser may reorder brace imports internally.
    val sortedStats = rawStats.sortBy(s => if (s.span.exists) s.span.start else Int.MaxValue)
    sortedStats.foreach {
      case pkg: Trees.PackageDef[?] if isBracedPackage(pkg, visitor) =>
        out.add(buildBracedPackage(pkg, visitor))
      case _: Trees.PackageDef[?] =>
        // Non-braced nested package — not yet modeled, skip.
      case imp: Trees.Import[?] =>
        visitor.visitTree(imp) match {
          case stmt: Statement => out.add(stmt)
          case _ =>
        }
      case stat =>
        visitor.visitTree(stat) match {
          case null =>
          case _: J.Empty =>
          case stmt: Statement => out.add(stmt)
          case _ =>
        }
    }
    out
  }

  /**
   * A `PackageDef` is braced when a `{` immediately follows its package name
   * (`package a { ... }`), as opposed to the file-header (`package a.b.c`),
   * indented (`package a:`) or synthetic empty-package forms.
   */
  private def isBracedPackage(pkgDef: Trees.PackageDef[?], visitor: ScalaTreeVisitor): Boolean = {
    if (!pkgDef.pid.span.exists) return false
    val packageName = extractPackageName(pkgDef)
    if (packageName.isEmpty || packageName == "<empty>") return false
    val srcText = visitor.getSourceText
    val scanStart = pkgDef.pid.span.end - visitor.getOffsetAdjustment
    if (scanStart < 0 || scanStart > srcText.length) return false
    var i = scanStart
    while (i < srcText.length && Character.isWhitespace(srcText.charAt(i))) {
      i += 1
    }
    i < srcText.length && srcText.charAt(i) == '{'
  }

  /**
   * Builds an [[S.PackageDeclaration]] for a braced package, recursing into its body
   * (so nested/sibling braced packages are handled uniformly). The `{`/`}` are owned
   * by the body [[J.Block]]; the [[J.Package]] head carries only `package <name>`.
   */
  private def buildBracedPackage(pkgDef: Trees.PackageDef[?], visitor: ScalaTreeVisitor): S.PackageDeclaration = {
    val prefix = visitor.extractPrefix(pkgDef.span)

    val packageExpr: Expression = TypeTree.build(packageNameFromSource(pkgDef, visitor))
    val namePkg = new J.Package(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      packageExpr.withPrefix(Space.build(" ", Collections.emptyList())),
      Collections.emptyList()
    )

    val srcText = visitor.getSourceText
    val srcOffset = visitor.getOffsetAdjustment

    // Consume the space before `{` (becomes the block prefix) and the `{` itself.
    val scanStart = pkgDef.pid.span.end - srcOffset
    var braceIdx = scanStart
    while (braceIdx < srcText.length && srcText.charAt(braceIdx) != '{') {
      braceIdx += 1
    }
    val beforeBrace = srcText.substring(scanStart, braceIdx)
    visitor.updateCursor(braceIdx + 1 + srcOffset)

    val bodyStmts = convertBody(pkgDef.stats, visitor)
    val rpStmts = new util.ArrayList[JRightPadded[Statement]]()
    bodyStmts.forEach(s => rpStmts.add(JRightPadded.build(s)))

    // Consume the space before `}` (becomes the block end) and the `}` itself.
    val afterStart = visitor.getCursor - srcOffset
    var closeIdx = afterStart
    while (closeIdx < srcText.length && srcText.charAt(closeIdx) != '}') {
      closeIdx += 1
    }
    val afterBody = srcText.substring(afterStart, closeIdx)
    visitor.updateCursor(closeIdx + 1 + srcOffset)

    val block = new J.Block(
      Tree.randomId(),
      ScalaSpace.format(beforeBrace),
      Markers.EMPTY,
      JRightPadded.build(false),
      rpStmts,
      ScalaSpace.format(afterBody)
    )

    new S.PackageDeclaration(Tree.randomId(), prefix, Markers.EMPTY, namePkg, block)
  }

  /**
   * Creates a J.Package from a Scala PackageDef.
   */
  private def createPackageDeclaration(pkgDef: Trees.PackageDef[?], visitor: ScalaTreeVisitor): J.Package = {
    // Extract the prefix (whitespace before 'package' keyword)
    val prefix = visitor.extractPrefix(pkgDef.span)

    // Find the end of the package declaration in the source
    // This includes "package" keyword + package name
    val packageEndPos = pkgDef.pid.span.end

    // Detect Scala 3 indented package syntax: `package foo.bar:` followed by an indented region,
    // and consume the delimiter so the printer re-emits it. Braced packages
    // (`package foo.bar { ... }`) are handled separately as S.PackageDeclaration.
    val srcText = visitor.getSourceText
    val srcOffset = visitor.getOffsetAdjustment
    val scanStart = packageEndPos - srcOffset
    var scanIdx = scanStart
    while (scanIdx < srcText.length && (srcText.charAt(scanIdx) == ' ' || srcText.charAt(scanIdx) == '\t')) {
      scanIdx += 1
    }
    val nextChar = if (scanIdx < srcText.length) srcText.charAt(scanIdx) else 0.toChar
    val hasIndentedColon = nextChar == ':'
    val hasSemicolon = nextChar == ';'
    val cursorAfter =
      if (hasIndentedColon || hasSemicolon) scanIdx + 1 + srcOffset
      else packageEndPos

    // Update the visitor's cursor to after the package declaration
    // This is crucial to prevent the package text from being included
    // in the prefix of subsequent statements
    visitor.updateCursor(cursorAfter)

    // Create package expression
    val packageExpr: Expression = TypeTree.build(packageNameFromSource(pkgDef, visitor))

    val markerList = new util.ArrayList[org.openrewrite.marker.Marker]()
    if (hasIndentedColon) {
      markerList.add(new IndentedSyntax(Tree.randomId()))
    }
    if (hasSemicolon) {
      markerList.add(PackageSemicolon(Tree.randomId()))
    }
    val markers = if (markerList.isEmpty) Markers.EMPTY else Markers.build(markerList)

    new J.Package(
      Tree.randomId(),
      prefix,
      markers,
      packageExpr.withPrefix(Space.build(" ", Collections.emptyList())),
      Collections.emptyList()
    )
  }

  /**
   * Extracts a qualified name from a Select tree.
   */
  private def extractQualifiedName(sel: Trees.Select[?]): String = {
    sel.qualifier match {
      case id: Trees.Ident[?] => s"${id.name}.${sel.name}"
      case innerSel: Trees.Select[?] => s"${extractQualifiedName(innerSel)}.${sel.name}"
      case _ => sel.name.toString
    }
  }

  /**
   * The package name as written in source (the `pid` span), so backtick-quoted
   * segments like `` `trait` `` keep their backticks — the compiler's name strings
   * strip them. Falls back to [[extractPackageName]] when the span is unusable.
   */
  private def packageNameFromSource(pkgDef: Trees.PackageDef[?], visitor: ScalaTreeVisitor): String = {
    if (pkgDef.pid.span.exists) {
      val srcText = visitor.getSourceText
      val srcOffset = visitor.getOffsetAdjustment
      val start = pkgDef.pid.span.start - srcOffset
      val end = pkgDef.pid.span.end - srcOffset
      if (start >= 0 && start < end && end <= srcText.length) {
        return srcText.substring(start, end)
      }
    }
    extractPackageName(pkgDef)
  }

  /**
   * Extracts the package name from a PackageDef.
   */
  private def extractPackageName(pkg: Trees.PackageDef[?]): String = {
    pkg.pid match {
      case id: Trees.Ident[?] => id.name.toString
      case sel: Trees.Select[?] => extractQualifiedName(sel)
      case _ => ""
    }
  }

  /**
   * Converts a Scala parse result to a list of statements (backward compatibility).
   */
  def convertToStatements(parseResult: ScalaParseResult, source: String): JList[Statement] = {
    convertToCompilationUnit(parseResult, source, null).statements
  }

  /**
   * Gets the remaining source after parsing (for EOF space).
   * This should return the source text after the last parsed element.
   */
  def getRemainingSource(parseResult: ScalaParseResult, source: String, lastCursorPosition: Int): String = {
    // If tree is empty (parse error), don't return any remaining source
    // The Unknown node will handle the entire source
    if (parseResult.tree.isEmpty) {
      return ""
    }

    // Return any remaining source after the last cursor position
    if (lastCursorPosition < source.length) {
      source.substring(lastCursorPosition)
    } else {
      ""
    }
  }
}
