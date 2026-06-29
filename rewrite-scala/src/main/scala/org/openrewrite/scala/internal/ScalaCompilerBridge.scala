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
import dotty.tools.dotc.reporting.{Diagnostic, StoreReporter}
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
   * Creates a fully configured compiler context on a new `ContextBase`:
   * Scala 2 compat source version, `postfixOps`, the given reporter, and
   * (when non-empty) output directory and classpath.
   */
  private def configuredContext(reporter: StoreReporter, classpath: String, outputDir: String = null): Context = {
    val fresh = new ParsingDriver().getInitialContext.fresh
    // Accept both Scala 2 and Scala 3 syntax (including indentation-based).
    fresh.setSetting(fresh.settings.source, "3.6-migration")
    // Enable `postfixOps` so the parser emits `PostfixOp` AST nodes for trailing
    // postfix calls (e.g., `xs filter p list`). Without this, Dotty's parser
    // silently drops the trailing token rather than emitting an AST node.
    fresh.setSetting(fresh.settings.language, List(
      new dotty.tools.dotc.config.Settings.Setting.ChoiceWithHelp[String]("postfixOps", "")
    ))
    fresh.setReporter(reporter)
    // Send compiler output to a designated dir so .class files don't pollute cwd.
    if (outputDir != null) {
      fresh.setSetting(fresh.settings.outputDir, dotty.tools.io.AbstractFile.getDirectory(outputDir))
    }
    if (classpath.nonEmpty) {
      fresh.setSetting(fresh.settings.classpath, classpath)
    }
    fresh
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

    // Buffer diagnostics in-memory so they don't leak to stderr via Dotty's default ConsoleReporter.
    val storeReporter = new StoreReporter(null)

    val cpString = ScalaCompilerBridge.buildClasspath(classpath)
    val configuredCtx: Context = configuredContext(storeReporter, cpString, outputDir)

    // Create the Run up front. Run construction initializes Definitions and
    // ContextBase state (e.g., `ctx.base.TypeBoundsEmpty`) that the parser
    // requires when it sees `A with B` types — those go through
    // `untpd.makeAndType` → `Definitions.andType`, which would throw
    // MissingCoreLibraryException on an uninitialized base. Parsing under
    // `run.runContext` keeps that path safe.
    //
    // Run construction requires the Scala stdlib on dotty's classpath (to
    // resolve `scala.Any`, etc.). When the caller's classpath lacks it — or
    // overrides dotty's default lookup, as in a fat-jar environment — retry
    // with the compiler's own core library jars (located via ProtectionDomain)
    // appended, but use the resulting Run for *parsing only*: callers that
    // narrow the classpath (e.g. ScalaTemplate) do so to avoid type
    // resolution, so type attribution stays off for them; they only need the
    // initialized ContextBase so `with` types parse. The retry needs an
    // entirely new context: the failed attempt has already initialized (and
    // cached) the ContextBase's platform classpath, so updating the setting
    // on a `fresh` of the same base would have no effect. Only if the retry
    // also fails do we fall back to a bare context, where `with` types fail
    // per-file.
    val (runOpt, parseContext): (Option[Run], Context) =
      try {
        val r = (new Compiler()).newRun(using configuredCtx)
        (Some(r), r.runContext)
      } catch {
        case _: Throwable =>
          try {
            val healedCp = ScalaCompilerBridge.appendStdlib(cpString)
            val r = (new Compiler()).newRun(using configuredContext(storeReporter, healedCp, outputDir))
            (None, r.runContext)
          } catch {
            case _: Throwable => (None, configuredCtx)
          }
      }
    given ctx: Context = parseContext

    // Phase 1: Parse all sources into CompilationUnits
    val units = new ArrayList[CompilationUnit]()
    val sourceEntries = new ArrayList[SourceEntry]()

    val srcIter = sources.iterator()
    while (srcIter.hasNext) {
      val entry = srcIter.next()
      try {
        val parsed = parseOne(entry.path, entry.content)

        results.put(entry.path, buildResult(parsed, new ArrayList[ScalaWarning]()))

        // Track unit for batch type-checking. The *raw* parsed tree (pre-unwrap)
        // is what the typer needs — it's the form the parser actually produced.
        parsed.unit.untpdTree = parsed.rawTree
        units.add(parsed.unit)
        sourceEntries.add(entry)
      } catch {
        case _: Throwable =>
          // Leave this entry out of the results so the caller falls back to a
          // single-file parse, surfacing the failure as a ParseError for this
          // file only instead of aborting the whole batch.
      }
    }

    // Phase 2: Attempt batch type-checking (only if we managed to create a Run).
    runOpt.foreach { run =>
      try {
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
    }

    // Drain buffered diagnostics into per-source `warnings` lists so callers
    // can surface them as ParseWarning markers (and optionally log them).
    val drained = storeReporter.removeBufferedMessages
    if (drained.nonEmpty) {
      val byPath = drained.groupBy(diagnosticSourcePath)
      val rIter = results.entrySet().iterator()
      while (rIter.hasNext) {
        val e = rIter.next()
        byPath.get(e.getKey).foreach { diags =>
          val ws = e.getValue.warnings
          diags.foreach(d => ws.add(toScalaWarning(d)))
        }
      }
    }

    results
  }

  private def diagnosticSourcePath(d: Diagnostic): String =
    if (d.pos.exists && d.pos.source != null) d.pos.source.path else ""

  private def toScalaWarning(d: Diagnostic): ScalaWarning = {
    val (line, column) =
      if (d.pos.exists) (d.pos.line + 1, d.pos.column + 1) else (0, 0)
    val level = d.level match {
      case 2 => "error"
      case 1 => "warning"
      case _ => "info"
    }
    ScalaWarning(d.message, line, column, level)
  }

  /**
   * Parses a single Scala source file (parse-only, no type checking).
   * Retained for backward compatibility.
   */
  def parse(path: String, content: String): ScalaParseResult = {
    // Buffer diagnostics in-memory so they don't leak to stderr via Dotty's default ConsoleReporter.
    val storeReporter = new StoreReporter(null)

    val configuredCtx: Context = configuredContext(storeReporter, "")

    // Best-effort Run init so `ctx.base` is wired up for `A with B` parsing
    // (which goes through `Definitions.andType` and throws on a bare context).
    // Same self-heal as compileAll: when dotty's default classpath lookup
    // can't find the stdlib (fat-jar environments), retry Run construction on
    // a new context with the compiler's own core library jars; only then
    // degrade to the bare context, where `A with B` types fail to parse.
    given Context = try {
      (new Compiler()).newRun(using configuredCtx).runContext
    } catch {
      case _: Throwable =>
        try {
          (new Compiler()).newRun(using configuredContext(storeReporter, ScalaCompilerBridge.appendStdlib(""))).runContext
        } catch {
          case _: Throwable => configuredCtx
        }
    }

    val parsed = parseOne(path, content)

    // Drain buffered diagnostics from the reporter so they're surfaced to the caller
    // instead of being silently dropped (or, with the default reporter, printed to stderr).
    val javaWarnings = new ArrayList[ScalaWarning]()
    storeReporter.removeBufferedMessages.foreach(d => javaWarnings.add(toScalaWarning(d)))

    buildResult(parsed, javaWarnings)
  }

  /**
   * Pick a wrapping strategy, wrap the content, parse it, and (for the
   * expression wrapper) unwrap the result. The raw and unwrapped trees are
   * both returned: the typer wants the raw form (`compileUnits` walks
   * `unit.untpdTree`), while downstream consumers want the unwrapped form.
   *
   * `.sbt` build definitions are sequences of top-level expressions — Dotty
   * silently drops those at the file level, so we lift the content into an
   * `object __SbtScript__ { ... }` body where statements are accepted. The
   * single-line expression wrapper is the pre-existing path used by
   * recipe/template machinery to coerce bare expressions through the parser.
   */
  private def parseOne(path: String, content: String)(using ctx: Context): ParsedUnit = {
    val wrapping =
      if (path != null && path.endsWith(".sbt")) Wrapping.ObjectScript
      else if (isSimpleExpression(content)) Wrapping.Expression
      else Wrapping.None

    val source = SourceFile.virtual(path, wrapping.wrap(content))
    val unit = CompilationUnit(source)
    val parser = new Parsers.Parser(source)(using ctx.fresh.setCompilationUnit(unit))
    val rawTree = parser.parse()

    val finalTree = wrapping match {
      case Wrapping.Expression if !rawTree.isEmpty => extractExpression(rawTree).getOrElse(rawTree)
      case _ => rawTree
    }
    ParsedUnit(rawTree, finalTree, unit, wrapping)
  }

  private def buildResult(parsed: ParsedUnit, warnings: JList[ScalaWarning])(using ctx: Context): ScalaParseResult =
    ScalaParseResult(
      parsed.finalTree, None, warnings,
      wasWrapped = parsed.wrapping == Wrapping.Expression,
      wasObjectWrapped = parsed.wrapping == Wrapping.ObjectScript,
      context = ctx
    )

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

    // A leading `@Ann` (optionally `@Ann(...)`) is a declaration annotation, not an
    // expression — wrapping it as `val result = @Ann object Foo` produces garbage.
    val startsWithAnnotation = trimmed.startsWith("@")

    !hasMultipleLines &&
    !hasPostfixOperator &&
    !startsWithDeclaration &&
    !startsWithAnnotation &&
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
      appendStdlibTo(sb, present)
    }
    sb.toString()
  }

  /**
   * Append the compiler's own core library jars (located via `ProtectionDomain`)
   * to an existing classpath string, skipping entries already present. Used to
   * retry `Run` construction when the configured classpath lacks the Scala
   * stdlib that dotty's `Definitions` needs to initialize.
   */
  private[internal] def appendStdlib(cpString: String): String = {
    val sb = new StringBuilder(cpString)
    val present = new LinkedHashSet[String]()
    if (cpString.nonEmpty) {
      cpString.split(File.pathSeparator).foreach(e => if (e.nonEmpty) present.add(fileName(e)))
    }
    appendStdlibTo(sb, present)
    sb.toString()
  }

  private def appendStdlibTo(sb: StringBuilder, present: LinkedHashSet[String]): Unit = {
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
 * Output of the bridge's shared parsing path: the raw parsed tree (what Dotty
 * produced — keeps the wrapper if there was one, needed by the typer), the
 * unwrapped tree visible to downstream consumers, the underlying Dotty
 * `CompilationUnit`, and the wrapping strategy that was applied.
 */
private[internal] case class ParsedUnit(
  rawTree: untpd.Tree,
  finalTree: untpd.Tree,
  unit: dotty.tools.dotc.CompilationUnit,
  wrapping: Wrapping
)

/**
 * How the bridge transformed the source before parsing.
 *
 *  - `None`: passed verbatim.
 *  - `Expression`: wrapped as `object ExprWrapper { val result = <content> }`
 *    so a bare expression parses; the result tree is the expression itself.
 *  - `ObjectScript`: wrapped as `object __SbtScript__ {\n<content>\n}` so a
 *    sequence of top-level statements parses (used for `.sbt` build files).
 *    The wrapper is left in the tree; downstream code lifts the body.
 */
private[internal] enum Wrapping:
  case None, Expression, ObjectScript

  def wrap(content: String): String = this match
    case Wrapping.None => content
    case Wrapping.Expression => s"object ExprWrapper { val result = $content }"
    case Wrapping.ObjectScript => s"object __SbtScript__ {\n$content\n}"

/**
 * Result of parsing (and optionally type-checking) a Scala source file.
 *
 * @param wasWrapped       true when the bridge wrapped a single-line
 *                         expression as `object ExprWrapper { val result = ... }`
 *                         and then unwrapped before returning the tree.
 * @param wasObjectWrapped true when the bridge wrapped multi-statement content
 *                         (e.g. a `.sbt` file) as `object __SbtScript__ { ... }`.
 *                         The wrapper is still present in `tree`; the
 *                         converter is responsible for lifting its body.
 */
case class ScalaParseResult(
  tree: untpd.Tree,
  typedTree: Option[tpd.Tree],
  warnings: JList[ScalaWarning],
  wasWrapped: Boolean = false,
  wasObjectWrapped: Boolean = false,
  context: Context
)

private[internal] object ScalaParseResultConstants:
  val ObjectScriptPrefix: String = "object __SbtScript__ {\n"
  val ObjectScriptWrapperName: String = "__SbtScript__"

/**
 * Represents a warning or error from the Scala compiler.
 */
case class ScalaWarning(message: String, line: Int, column: Int, level: String)
