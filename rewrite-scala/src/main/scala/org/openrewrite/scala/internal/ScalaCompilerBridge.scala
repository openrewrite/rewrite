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

import dotty.tools.dotc.ast.Trees.*
import dotty.tools.dotc.ast.{tpd, untpd}
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Phases
import dotty.tools.dotc.parsing.Parsers
import dotty.tools.dotc.util.SourceFile
import dotty.tools.dotc.reporting.{Diagnostic, Reporter, StoreReporter}
import dotty.tools.dotc.{CompilationUnit, Run, Compiler, Driver}
import dotty.tools.dotc.config.ScalaSettings

import scala.collection.mutable.ListBuffer
import java.io.File
import java.nio.file.Paths
import java.util.{ArrayList, HashMap, LinkedHashSet, List => JList, Map => JMap}

/**
 * Bridge to the Scala 3 (Dotty) compiler for parsing and type-checking Scala source files.
 *
 * Supports two modes:
 * - parse(): Parse-only (legacy, for single files)
 * - compileAll(): Batch parse + type-check for type attribution
 */
class ScalaCompilerBridge {

  // Create a custom driver class to access protected members
  private class ParsingDriver extends Driver {
    def getInitialContext: Context = initCtx
  }

  /**
   * Batch-compile multiple Scala source files through the typer phase.
   * Returns a map from path to ScalaParseResult, each containing both the
   * untyped tree (for formatting fidelity) and the typed tree (for type info).
   *
   * On per-unit typer failure, that unit gets typedTree=None while others succeed.
   */
  def compileAll(sources: JList[SourceEntry], classpath: JList[String], outputDir: String = null): JMap[String, ScalaParseResult] = {
    val results = new HashMap[String, ScalaParseResult]()

    val driver = new ParsingDriver()
    val baseCtx: Context = driver.getInitialContext

    // Configure source version, output directory, and classpath.
    given ctx: Context = {
      val fresh = baseCtx.fresh
      // Accept both Scala 2 and Scala 3 syntax (including indentation-based).
      fresh.setSetting(fresh.settings.source, "3.6-migration")
      // Send compiler output to a designated dir so .class files don't pollute cwd.
      if (outputDir != null) {
        fresh.setSetting(fresh.settings.outputDir, dotty.tools.io.AbstractFile.getDirectory(outputDir))
      }
      val cpString = ScalaCompilerBridge.buildClasspath(classpath)
      if (cpString.nonEmpty) {
        fresh.setSetting(fresh.settings.classpath, cpString)
      }
      fresh
    }

    // Phase 1: Parse all sources into CompilationUnits
    val units = new ArrayList[CompilationUnit]()
    val sourceEntries = new ArrayList[SourceEntry]()

    val srcIter = sources.iterator()
    while (srcIter.hasNext) {
      val entry = srcIter.next()
      val path = entry.path
      val content = entry.content

      // Handle simple expression wrapping
      val (adjustedContent, needsUnwrap) = if (isSimpleExpression(content)) {
        (s"object ExprWrapper { val result = $content }", true)
      } else {
        (content, false)
      }

      val source = SourceFile.virtual(path, adjustedContent)
      val unit = CompilationUnit(source)

      // Parse using the parser (this always works)
      val parser = new Parsers.Parser(source)(using ctx.fresh.setCompilationUnit(unit))
      val tree = parser.parse()

      // Unwrap if needed
      val finalTree = if (needsUnwrap && !tree.isEmpty) {
        extractExpression(tree).getOrElse(tree)
      } else {
        tree
      }

      // Store parse-only result initially
      results.put(path, ScalaParseResult(finalTree, None, new ArrayList[ScalaWarning](), needsUnwrap, ctx))

      // Track unit for batch type-checking
      unit.untpdTree = tree
      units.add(unit)
      sourceEntries.add(entry)
    }

    // Phase 2: Attempt batch type-checking
    try {
      val compiler = new Compiler()
      val run = compiler.newRun(using ctx)

      // Convert to Scala list for the compiler
      val unitList = {
        val buf = new ListBuffer[CompilationUnit]()
        val uIter = units.iterator()
        while (uIter.hasNext) buf += uIter.next()
        buf.toList
      }

      run.compileUnits(unitList)

      // Use the run's context — symbols from tpd.Tree are only valid in this context
      given runCtx: Context = run.runContext

      // Extract typed trees per unit
      for (i <- 0 until units.size()) {
        val unit = units.get(i)
        val entry = sourceEntries.get(i)
        val path = entry.path
        val existing = results.get(path)

        try {
          val typedTree = unit.tpdTree
          if (typedTree != null && !typedTree.isEmpty) {
            results.put(path, existing.copy(typedTree = Some(typedTree), context = runCtx))
          }
        } catch {
          case _: Throwable =>
        }
      }
    } catch {
      case _: Throwable =>
        // Batch compilation failed entirely; all units keep typedTree=None
    }

    results
  }

  /**
   * Parses a single Scala source file (parse-only, no type checking).
   * Retained for backward compatibility.
   */
  def parse(path: String, content: String): ScalaParseResult = {
    // Create a custom reporter to collect warnings
    val warnings = new ListBuffer[ScalaWarning]()

    // Create our custom driver and get a proper context with Scala 2 compat
    val driver = new ParsingDriver()
    given Context = {
      val fresh = driver.getInitialContext.fresh
      fresh.setSetting(fresh.settings.source, "3.6-migration")
      fresh
    }

    // For simple expressions, wrap in a valid compilation unit
    val (adjustedContent, needsUnwrap) = if (isSimpleExpression(content)) {
      (s"object ExprWrapper { val result = $content }", true)
    } else {
      (content, false)
    }

    // Create source file
    val source = SourceFile.virtual(path, adjustedContent)

    // Parse the source
    val unit = CompilationUnit(source)
    val parser = new Parsers.Parser(source)(using ctx.fresh.setCompilationUnit(unit))

    val tree = parser.parse()

    // If we wrapped the expression, extract it
    val finalTree = if (needsUnwrap && !tree.isEmpty) {
      extractExpression(tree).getOrElse(tree)
    } else {
      tree
    }

    // Convert warnings to Java list
    val javaWarnings = new ArrayList[ScalaWarning]()
    warnings.foreach(javaWarnings.add)

    ScalaParseResult(finalTree, None, javaWarnings, needsUnwrap, ctx)
  }

  private def extractExpression(tree: untpd.Tree)(using Context): Option[untpd.Tree] = tree match {
    case pkgDef: untpd.PackageDef =>
      pkgDef.stats.collectFirst {
        case mod: untpd.ModuleDef if mod.name.toString == "ExprWrapper" =>
          mod.impl.body.collectFirst {
            case vd: untpd.ValDef if vd.name.toString == "result" =>
              // The rhs (right-hand side) is the expression we want
              vd.rhs
          }
      }.flatten
    case _ => None
  }

  private def isSimpleExpression(content: String): Boolean = {
    val trimmed = content.trim
    // Check if it's likely a simple expression (doesn't start with keywords that indicate declarations)
    // Also check if it contains multiple lines, which would indicate a block of statements
    val hasMultipleLines = trimmed.contains('\n')

    // Check for postfix operators - they need special handling
    val hasPostfixOperator = trimmed.matches(".*[a-zA-Z0-9_)]\\s*[!?]\\s*$")

    // Check for declaration keywords with regex to handle arbitrary spacing
    val declarationPattern = """^\s*(package|import|class|object|trait|def|val|var|type|private|protected|public|final|lazy|implicit|case\s+class|case\s+object)\s""".r
    val startsWithDeclaration = declarationPattern.findFirstIn(trimmed).isDefined

    !hasMultipleLines &&
    !hasPostfixOperator &&
    !startsWithDeclaration &&
    !trimmed.startsWith("//") &&
    !trimmed.startsWith("/*") &&
    trimmed.nonEmpty
  }
}

object ScalaCompilerBridge {

  /**
   * Probe classes covering every jar dotty needs to type-check any Scala source:
   * scala-library (Scala stdlib), tasty-core (TASTy reader), and scala3-compiler
   * (dotty itself, needed when parsing code that imports `dotty.tools.*`).
   *
   * The bridge code is loaded from these jars, so we can always find them via
   * `ProtectionDomain.getCodeSource()` regardless of what the caller passes.
   */
  private val StdlibProbeClasses: List[Class[?]] = List(
    classOf[scala.Option[?]],
    classOf[dotty.tools.tasty.TastyFormat.type],
    classOf[dotty.tools.dotc.core.Contexts.type]
  )

  /**
   * Build the classpath string for dotty. Caller entries come first; stdlib
   * jars are appended only when the caller's classpath already looks like a
   * Scala project classpath (i.e. it contains at least one `scala*`, `tasty*`
   * or `dotty*` artifact) but is missing one of the stdlib jars dotty needs.
   * Without stdlib on the classpath, dotty's `Definitions` can't resolve
   * foundational types like `scala.Unit`, leading to `ClassCastException` /
   * "Bad symbolic reference" failures in the typer. The bridge is loaded from
   * those same jars so we can always locate them via `ProtectionDomain`.
   *
   * Restricting self-heal to Scala-looking classpaths preserves the behaviour
   * expected by callers (such as `ScalaTemplate`) that intentionally pass a
   * narrow non-Scala classpath to avoid type resolution.
   */
  private[internal] def buildClasspath(classpath: JList[String]): String = {
    val sb = new StringBuilder()
    val present = new LinkedHashSet[String]()
    var hasScalaArtifact = false
    if (classpath != null) {
      val it = classpath.iterator()
      while (it.hasNext) {
        val e = it.next()
        if (e != null && !e.isEmpty) {
          if (sb.nonEmpty) sb.append(File.pathSeparator)
          sb.append(e)
          val fn = fileName(e)
          present.add(fn)
          if (isScalaArtifact(fn)) hasScalaArtifact = true
        }
      }
    }
    if (hasScalaArtifact) {
      for (cls <- StdlibProbeClasses) {
        locateJar(cls).foreach { jar =>
          if (!present.contains(fileName(jar))) {
            if (sb.nonEmpty) sb.append(File.pathSeparator)
            sb.append(jar)
            present.add(fileName(jar))
          }
        }
      }
    }
    sb.toString()
  }

  private def isScalaArtifact(fileName: String): Boolean = {
    val n = fileName.toLowerCase
    n.startsWith("scala") || n.startsWith("tasty") || n.startsWith("dotty")
  }

  private def fileName(path: String): String = {
    val p = Paths.get(path).getFileName
    if (p == null) path else p.toString
  }

  private def locateJar(cls: Class[?]): Option[String] = {
    val pd = cls.getProtectionDomain
    if (pd == null) return None
    val cs = pd.getCodeSource
    if (cs == null) return None
    val url = cs.getLocation
    if (url == null) return None
    try Some(Paths.get(url.toURI).toString)
    catch { case _: Throwable => None }
  }
}

/**
 * A source file entry for batch compilation.
 */
case class SourceEntry(path: String, content: String)

/**
 * Result of parsing (and optionally type-checking) a Scala source file.
 */
case class ScalaParseResult(
  tree: untpd.Tree,
  typedTree: Option[tpd.Tree],
  warnings: JList[ScalaWarning],
  wasWrapped: Boolean = false,
  context: Context
)

/**
 * Represents a warning or error from the Scala compiler.
 */
case class ScalaWarning(message: String, line: Int, column: Int, level: String)
