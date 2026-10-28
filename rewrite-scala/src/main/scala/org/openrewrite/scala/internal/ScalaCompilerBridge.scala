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
import dotty.tools.dotc.core.{Flags, Periods, Phases}
import dotty.tools.dotc.core.NameOps.*
import dotty.tools.dotc.core.Names.Name
import dotty.tools.dotc.core.Phases.Phase
import dotty.tools.dotc.core.StdNames.nme
import dotty.tools.dotc.core.Symbols.{NoSymbol, Symbol}
import dotty.tools.dotc.typer.TyperPhase
import dotty.tools.dotc.parsing.{Parser, Parsers}
import dotty.tools.dotc.util.SourceFile
import dotty.tools.dotc.reporting.{Diagnostic, StoreReporter}
import dotty.tools.dotc.{CompilationUnit, Run, Compiler, Driver}
import dotty.tools.dotc.config.ScalaSettings

import scala.collection.mutable.ListBuffer
import java.io.File
import java.nio.file.Paths
import java.util.{ArrayList, Collections, HashMap, IdentityHashMap, LinkedHashSet, List => JList, Map => JMap, Set => JSet}

/**
 * Bridge to the Scala 3 (Dotty) compiler for parsing and type-checking Scala source files.
 *
 * Supports two modes:
 * - parse(): Parse-only (legacy, for single files)
 * - compileAll(): Batch parse + type-check for type attribution
 *
 * `compileAll` takes its compiler from `compilers` when given, and [[release]] hands it back.
 */
class ScalaCompilerBridge(compilers: ScalaCompilerCache) {

  def this() = this(null)

  /** The compiler of the last `compileAll`, whose trees and symbols are in use until [[release]]. */
  private var compiler: ScalaCompiler = null

  // Create a custom driver class to access protected members
  private class ParsingDriver extends Driver {
    def getInitialContext: Context = initCtx
  }

  /**
   * Creates a fully configured compiler context on the `ContextBase` of the
   * given initial context: Scala 2 compat source version, `postfixOps`, the
   * given reporter, and (when non-empty) output directory and classpath.
   */
  private def configuredContext(initialContext: Context, reporter: StoreReporter, classpath: String, outputDir: String = null): Context = {
    val fresh = initialContext.fresh
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
      fresh.setSetting(fresh.settings.outputDir,
        new dotty.tools.io.PlainDirectory(new dotty.tools.io.Directory(Paths.get(outputDir))))
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
    val cpString = ScalaCompilerBridge.buildClasspath(classpath)
    val comp = if (compilers == null) new ScalaCompiler(cpString, reuse = false) else compilers.acquire(cpString)
    compiler = comp
    try compileAll(comp, sources, cpString, outputDir)
    catch {
      case t: Throwable =>
        comp.reusable = false
        throw t
    }
  }

  /**
   * Hands the compiler of the last `compileAll` back to `compilers` for reuse. Call it once
   * nothing uses the trees, symbols or contexts of the results any more.
   */
  def release(): Unit = {
    val comp = compiler
    compiler = null
    if (comp != null && compilers != null) compilers.release(comp)
  }

  private def compileAll(comp: ScalaCompiler, sources: JList[SourceEntry], cpString: String, outputDir: String): JMap[String, ScalaParseResult] = {
    val results = new HashMap[String, ScalaParseResult]()

    // Buffer diagnostics in-memory so they don't leak to stderr via Dotty's default ConsoleReporter.
    val storeReporter = new StoreReporter(null)

    val configuredCtx: Context = configuredContext(comp.initialContext, storeReporter, cpString, outputDir)
    val runId = comp.nextRunId()
    val typerTrees = new IdentityHashMap[CompilationUnit, tpd.Tree]()
    val recorder = new TyperRecorder

    // Create the Run up front. Run construction initializes Definitions and
    // ContextBase state (e.g., `ctx.base.TypeBoundsEmpty`) that the parser
    // requires when it sees `A with B` types — those go through
    // `untpd.makeAndType` → `Definitions.andType`, which would throw
    // MissingCoreLibraryException on an uninitialized base. Parsing under
    // `run.runContext` keeps that path safe. If the Run can't be constructed
    // at all, fall back to a bare context, where `with` types fail per-file.
    val (runOpt, parseContext) =
      try {
        val r = new TyperTreeCapturingCompiler(typerTrees, recorder, runId).newRun(using configuredCtx)
        comp.ran(r)
        (Some(r), r.runContext)
      } catch {
        case _: Throwable =>
          comp.reusable = false
          (None, configuredCtx)
      }
    given ctx: Context = parseContext

    // Phase 1: Parse all sources into CompilationUnits
    val units = new ArrayList[CompilationUnit]()
    val indexes = new IdentityHashMap[CompilationUnit, UntypedNodeIndex]()
    val sourceEntries = new ArrayList[SourceEntry]()
    val parsedTrees = new ArrayList[untpd.Tree]()

    val srcIter = sources.iterator()
    while (srcIter.hasNext) {
      val entry = srcIter.next()
      try {
        val parsed = parseOne(entry.path, entry.content)

        results.put(entry.path, buildResult(parsed, new ArrayList[ScalaWarning]()))

        // Track unit for batch type-checking. The *raw* parsed tree (pre-unwrap)
        // is what the typer needs — it's the form the parser actually produced.
        // The typer records types on the untyped trees it is given, so it gets
        // trees of its own rather than the ones the LST is built from.
        val unit = parseForTyping(entry)
        units.add(unit)
        val index = UntypedNodeIndex(unit.untpdTree)
        indexes.put(unit, index)
        recorder.track(index)
        sourceEntries.add(entry)
        parsedTrees.add(parsed.rawTree)
      } catch {
        case _: Throwable =>
          // Leave this entry out of the results so the caller falls back to a
          // single-file parse, surfacing the failure as a ParseError for this
          // file only instead of aborting the whole batch.
          comp.reusable = false
      }
    }

    // Phase 2: Attempt batch type-checking (only if we managed to create a Run).
    runOpt.foreach { firstRun =>
      try {
        comp.enters(units)(using firstRun.runContext)
        firstRun.compileUnits(toScalaList(units))

        val unresolvable = unresolvableWildcardImports(units, typerTrees)(using firstRun.runContext)
        val firstTyping = Typing(firstRun, units, typerTrees, recorder, indexes)
        val Typing(run, typedUnits, typedTrees, typedRecorder, typedIndexes) =
          if (unresolvable.isEmpty) firstTyping
          else retypeWithoutImports(sourceEntries, unresolvable, configuredCtx, comp, runId).getOrElse(firstTyping)

        // Use the run's context — symbols from tpd.Tree are only valid in this context
        given runCtx: Context = run.runContext

        // Extract typed trees per unit
        for (i <- 0 until typedUnits.size()) {
          val unit = typedUnits.get(i)
          val entry = sourceEntries.get(i)
          val path = entry.path
          val existing = results.get(path)

          try {
            val typedTree = Option(typedTrees.get(unit)).getOrElse(unit.tpdTree)
            if (typedTree != null && !typedTree.isEmpty) {
              val typedNodes = Option(typedIndexes.get(unit))
                .map(TypedNodes.pair(parsedTrees.get(i), _, typedRecorder))
                .getOrElse(TypedNodes.empty)
              results.put(path, existing.copy(typedTree = Some(typedTree), typedNodes = typedNodes, context = runCtx))
            }
          } catch {
            case _: Throwable =>
              comp.reusable = false
          }
        }
        typedRecorder.clear()
      } catch {
        case _: Throwable =>
          // Batch compilation failed entirely; all units keep typedTree=None
          comp.reusable = false
      } finally {
        recorder.clear()
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

  private def toScalaList(units: JList[CompilationUnit]): List[CompilationUnit] = {
    val buf = new ListBuffer[CompilationUnit]()
    units.forEach(u => buf += u)
    buf.toList
  }

  /**
   * Wildcard imports whose qualifier didn't resolve, by source path. Dotty treats every
   * name as a member of an erroneous import qualifier, so such an import shadows all
   * outer definitions, including the `scala` package and `Predef`: with
   * `import doobie.implicits._` and no doobie on the classpath, `List(1)` no longer
   * resolves. Typically that just means the classpath is incomplete.
   */
  private def unresolvableWildcardImports(units: JList[CompilationUnit],
                                          typerTrees: IdentityHashMap[CompilationUnit, tpd.Tree])(using Context): Map[String, Set[(Int, Int)]] = {
    var found = Map.empty[String, Set[(Int, Int)]]
    units.forEach { unit =>
      val spans = scala.collection.mutable.Set.empty[(Int, Int)]
      try {
        new tpd.TreeTraverser {
          override def traverse(tree: tpd.Tree)(using Context): Unit = tree match {
            case imp: tpd.Import =>
              if (imp.span.exists && imp.selectors.exists(_.isWildcard) && imp.expr.tpe.isErroneous) {
                spans += ((imp.span.start, imp.span.end))
              }
            case _ => traverseChildren(tree)
          }
        }.traverse(Option(typerTrees.get(unit)).getOrElse(unit.tpdTree))
      } catch {
        case _: Throwable =>
      }
      if (spans.nonEmpty) found += unit.source.path -> spans.toSet
    }
    found
  }

  /**
   * Type-check the sources once more in a fresh run, leaving out the given imports, so
   * that names they shadowed resolve again. Sources are parsed anew since the typer
   * attaches symbols to the untyped trees of the previous run; spans are unaffected,
   * so the typed trees still line up with the originally parsed ones. Diagnostics of
   * this run are discarded: the ones of the first run describe the actual sources.
   */
  private def retypeWithoutImports(entries: JList[SourceEntry],
                                   imports: Map[String, Set[(Int, Int)]],
                                   ctx0: Context,
                                   comp: ScalaCompiler,
                                   runId: Periods.RunId): Option[Typing] =
    try {
      val typerTrees = new IdentityHashMap[CompilationUnit, tpd.Tree]()
      val recorder = new TyperRecorder
      val run = new TyperTreeCapturingCompiler(typerTrees, recorder, runId)
        .newRun(using ctx0.fresh.setReporter(new StoreReporter(null)))
      comp.ran(run)
      given Context = run.runContext
      val units = new ArrayList[CompilationUnit]()
      val indexes = new IdentityHashMap[CompilationUnit, UntypedNodeIndex]()
      entries.forEach { entry =>
        val unit = parseForTyping(entry)
        val dropped = imports.getOrElse(entry.path, Set.empty)
        if (dropped.nonEmpty) {
          unit.untpdTree = new untpd.UntypedTreeMap {
            override def transformStats(trees: List[untpd.Tree], exprOwner: dotty.tools.dotc.core.Symbols.Symbol)(using Context): List[untpd.Tree] =
              super.transformStats(trees.filterNot {
                case imp: untpd.Import => imp.span.exists && dropped.contains((imp.span.start, imp.span.end))
                case _ => false
              }, exprOwner)
          }.transform(unit.untpdTree)
        }
        units.add(unit)
        val index = UntypedNodeIndex(unit.untpdTree)
        indexes.put(unit, index)
        recorder.track(index)
      }
      run.compileUnits(toScalaList(units))
      Some(Typing(run, units, typerTrees, recorder, indexes))
    } catch {
      case _: Throwable =>
        comp.reusable = false
        None
    }

  private def parseForTyping(entry: SourceEntry)(using Context): CompilationUnit = {
    val parsed = parseOne(entry.path, entry.content, reportDiagnostics = false)
    parsed.unit.untpdTree = parsed.rawTree
    parsed.unit
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

    val configuredCtx: Context = configuredContext(new ParsingDriver().getInitialContext, storeReporter, ScalaCompilerBridge.buildClasspath(null))

    // Best-effort Run init so `ctx.base` is wired up for `A with B` parsing
    // (which goes through `Definitions.andType` and throws on a bare context).
    given Context = try {
      (new Compiler()).newRun(using configuredCtx).runContext
    } catch {
      case _: Throwable => configuredCtx
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
  private def parseOne(path: String, content: String, reportDiagnostics: Boolean = true)(using ctx: Context): ParsedUnit = {
    val wrapping =
      if (path != null && path.endsWith(".sbt")) Wrapping.ObjectScript
      else if (isSimpleExpression(content)) Wrapping.Expression
      else Wrapping.None

    // Each attempt parses under its own reporter so a failed attempt's syntax
    // errors don't surface on the file when a later attempt parses cleanly.
    def attempt(sourceVersion: Option[String]): (ParsedUnit, List[Diagnostic]) = {
      val reporter = new StoreReporter(null)
      val source = SourceFile.virtual(path, wrapping.wrap(content))
      val unit = CompilationUnit(source)
      val pctx = ctx.fresh.setCompilationUnit(unit).setReporter(reporter)
      sourceVersion.foreach(v => pctx.setSetting(pctx.settings.source, v))
      val parser = new Parsers.Parser(source)(using pctx)
      val rawTree = parser.parse()
      val finalTree = wrapping match {
        case Wrapping.Expression if !rawTree.isEmpty => extractExpression(rawTree)(using pctx).getOrElse(rawTree)
        case _ => rawTree
      }
      (ParsedUnit(rawTree, finalTree, unit, wrapping), reporter.removeBufferedMessages(using pctx))
    }
    def hasSyntaxErrors(diags: List[Diagnostic]): Boolean = diags.exists(_.level >= 2)

    // Parse under the configured source version first. On syntax errors, retry
    // under `3.0-migration`, where Dotty's scanner treats the Scala 3 keywords
    // `enum`, `given` and `erased` as plain identifiers — Scala 2 sources
    // legitimately use them as names. Keep the first attempt when the retry
    // doesn't parse cleanly either.
    val (first, firstDiags) = attempt(None)
    val (chosen, chosenDiags) =
      if (hasSyntaxErrors(firstDiags)) {
        val (second, secondDiags) = attempt(Some("3.0-migration"))
        if (hasSyntaxErrors(secondDiags)) (first, firstDiags) else (second, secondDiags)
      } else (first, firstDiags)

    if (reportDiagnostics) chosenDiags.foreach(d => ctx.reporter.report(d))
    chosen
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
    !trimmed.startsWith(";") &&
    trimmed.nonEmpty
  }
}

object ScalaCompilerBridge {

  private val ScalaLibraryProbe: Class[?] = classOf[scala.Option[?]]

  /**
   * Probe classes covering every jar dotty needs to type-check any Scala source:
   * scala-library (Scala stdlib), tasty-core (TASTy reader), and scala3-compiler
   * (dotty itself, needed when parsing code that imports `dotty.tools.*`).
   *
   * The bridge code is loaded from these jars, so we can always find them via
   * `ProtectionDomain.getCodeSource()` regardless of what the caller passes.
   */
  private val StdlibProbeClasses: List[Class[?]] = List(
    ScalaLibraryProbe,
    classOf[dotty.tools.tasty.TastyFormat.type],
    classOf[dotty.tools.dotc.core.Contexts.type]
  )

  /**
   * Build the classpath string for dotty. Caller entries come first; the Scala
   * standard library the bridge is loaded from is always appended (like the
   * Kotlin parser always adds kotlin-stdlib), because a non-empty classpath
   * overrides dotty's default lookup and without the stdlib dotty's
   * `Definitions` can't resolve foundational types like `scala.Unit`.
   *
   * When the caller's classpath already looks like a Scala project classpath
   * (it contains at least one `scala*`, `tasty*` or `dotty*` artifact), the
   * remaining jars dotty needs (tasty-core, scala3-compiler) are appended too,
   * avoiding "Bad symbolic reference" failures when sources import dotty APIs.
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
    appendJarsOf(if (hasScalaArtifact) StdlibProbeClasses else List(ScalaLibraryProbe), sb, present)
    sb.toString()
  }

  private def appendJarsOf(probes: List[Class[?]], sb: StringBuilder, present: LinkedHashSet[String]): Unit = {
    for (cls <- probes) {
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
 * The full compiler pipeline replaces `unit.tpdTree` phase by phase, so after
 * `compileUnits` it holds the lowered (erased, lambda-lifted, ...) tree for any
 * unit that compiled without errors. Type attribution needs the typer's tree,
 * whose spans and types still correspond to the source, so capture it right
 * after typing while letting the later phases run for their diagnostics.
 *
 * Units are type-checked from the untyped trees the bridge parsed, rather than
 * being parsed again by the compiler's parser phase, and the typer records the
 * typed tree of each of their nodes.
 *
 * Its runs have the given id, which a [[ScalaCompiler]] increases from one
 * compilation to the next, as Dotty requires of the runs on one `ContextBase`.
 */
private class TyperTreeCapturingCompiler(typerTrees: IdentityHashMap[CompilationUnit, tpd.Tree],
                                         recorder: TyperRecorder,
                                         runId: Periods.RunId) extends Compiler {
  override def nextRunId: Periods.RunId = runId

  override def phases: List[List[Phase]] = {
    val all = super.phases.map(_.map {
      case _: Parser => new KeepParsedTrees
      case _: TyperPhase => new RecordingTyperPhase(recorder)
      case phase => phase
    })
    val typerGroup = all.indexWhere(_.exists(_.isInstanceOf[TyperPhase]))
    if (typerGroup < 0) all
    else {
      val (upToTyper, rest) = all.splitAt(typerGroup + 1)
      upToTyper ::: List(new CaptureTyperTrees(typerTrees)) :: rest
    }
  }
}

/**
 * A compiler whose symbol table outlives a compilation, so that a [[ScalaCompilerCache]]
 * can reuse the symbols it loaded from the classpath. A compilation enters its top-level
 * definitions into the packages of that symbol table, and [[clean]] removes them again so
 * that no compilation sees the sources of another.
 */
private[internal] final class ScalaCompiler(val classpath: String, reuse: Boolean) {
  val initialContext: Context = (new ContextBase).initialCtx

  /** Dotty allows only the thread that first compiled with a `ContextBase` to compile with it. */
  val thread: Thread = Thread.currentThread()

  /** Whether [[clean]] can restore the symbol table to what it was before the last compilation. */
  var reusable: Boolean = reuse

  private var runId: Periods.RunId = Periods.InitialRunId
  private var lastContext: Context = null

  /** The members of the packages the last compilation declares definitions in, from before it. */
  private var packages: List[(Symbol, JSet[Symbol])] = Nil

  /** The id of the first symbol the last compilation created. */
  private var firstSymbolId: Int = 0

  def nextRunId(): Periods.RunId = {
    runId += 1
    runId
  }

  /** Records the latest run of a compilation, in whose context [[clean]] works. */
  def ran(run: Run): Unit =
    lastContext = run.runContext

  /** Records the members of the packages the units declare definitions in, before the units are entered. */
  def enters(units: JList[CompilationUnit])(using Context): Unit =
    if (reusable && packages.isEmpty) {
      val declared = new LinkedHashSet[Symbol]()
      declared.add(ctx.definitions.RootClass)
      declared.add(ctx.definitions.EmptyPackageClass)
      units.forEach { unit =>
        for (path <- packagePaths(unit.untpdTree, Nil)) {
          var owner: Symbol = ctx.definitions.RootClass
          for (name <- path if owner.exists) {
            val pkg = owner.info.decls.lookup(name.toTermName)
            owner = if (pkg.is(Flags.Package)) pkg.moduleClass else NoSymbol
            if (owner.exists) declared.add(owner)
          }
        }
      }
      declared.forEach { pkg =>
        val members = Collections.newSetFromMap(new IdentityHashMap[Symbol, java.lang.Boolean]())
        pkg.info.decls.toList.foreach(members.add)
        packages = (pkg, members) :: packages
      }
      firstSymbolId = ctx.base.nextSymId
    }

  /** The packages a tree declares definitions in, with its package clauses and package objects. */
  private def packagePaths(tree: untpd.Tree, prefix: List[Name]): List[List[Name]] = tree match {
    case untpd.PackageDef(pid, stats) =>
      val path = if (pid.name == nme.EMPTY_PACKAGE) prefix else prefix ++ qualifiedName(pid)
      path :: stats.flatMap {
        case pkg: untpd.PackageDef => packagePaths(pkg, path)
        case obj: untpd.ModuleDef if obj.mods.is(Flags.Package) => List(path :+ obj.name)
        case _ => Nil
      }
    case _ => Nil
  }

  private def qualifiedName(tree: untpd.Tree): List[Name] = tree match {
    case untpd.Ident(name) => List(name)
    case untpd.Select(qual, name) => qualifiedName(qual) :+ name
    case _ => Nil
  }

  /**
   * Removes the symbols the last compilation entered into packages: its definitions, the
   * packages, companions and proxies the namer added for them, and the nested classes later
   * phases lifted out of them. Returns whether the compiler can be reused, which it can't
   * when one of its definitions replaced a member of the classpath.
   */
  def clean(): Boolean = {
    val context = lastContext
    lastContext = null
    if (reusable && context != null) {
      given Context = context
      try {
        for ((pkg, before) <- packages) {
          val scope = pkg.info.decls.openForMutations
          val now = scope.toList
          val added = now.filter(sym => sym.id >= firstSymbolId && !synthesized(pkg, sym))
          for (sym <- added; name <- enteredNames(sym)) scope.unlink(sym, name)
          if (now.count(before.contains) != before.size || scope.toList.exists(added.contains)) {
            reusable = false
          }
          pkg.asClass.classDenot.invalidateMemberCaches()
        }
      } catch {
        case _: Throwable => reusable = false
      }
    }
    packages = Nil
    reusable
  }

  /** A class lifted out of another is entered under the flat name the last phases gave it. */
  private def enteredNames(sym: Symbol)(using Context): Set[Name] = {
    val denot = sym.lastKnownDenotation
    Set(denot.initial.name, denot.name, atPhase(ctx.base.allPhases.last)(sym.denot.name))
  }

  /** Classes of the `scala` package that any compilation creates on first use, like `Function23`. */
  private def synthesized(pkg: Symbol, sym: Symbol)(using Context): Boolean =
    pkg == ctx.definitions.ScalaPackageClass && {
      val name = sym.lastKnownDenotation.name
      name.isTypeName && name.isSyntheticFunction
    }
}

/**
 * The compilers of the parsers of one `ScalaParser.Builder` and its clones, kept between
 * parses, one for each thread that parses.
 */
final class ScalaCompilerCache {
  private val idle = new ThreadLocal[ScalaCompiler]

  private[internal] def acquire(classpath: String): ScalaCompiler = {
    val compiler = idle.get()
    idle.remove()
    if (compiler != null && compiler.classpath == classpath) compiler else new ScalaCompiler(classpath, reuse = true)
  }

  private[internal] def release(compiler: ScalaCompiler): Unit =
    if (compiler.thread == Thread.currentThread() && compiler.clean()) idle.set(compiler)
}

/** The outcome of type-checking a batch of units in one run. */
private case class Typing(run: Run,
                          units: JList[CompilationUnit],
                          typerTrees: IdentityHashMap[CompilationUnit, tpd.Tree],
                          recorder: TyperRecorder,
                          indexes: IdentityHashMap[CompilationUnit, UntypedNodeIndex])

private class KeepParsedTrees extends Parser {
  override def parse(using ctx: Context): Boolean =
    !ctx.compilationUnit.untpdTree.isEmpty || super.parse
}

private class CaptureTyperTrees(typerTrees: IdentityHashMap[CompilationUnit, tpd.Tree]) extends Phase {
  override def phaseName: String = "captureTyperTrees"

  override def isRunnable(using Context): Boolean = true

  override def isCheckable: Boolean = false

  override protected def run(using ctx: Context): Unit =
    typerTrees.put(ctx.compilationUnit, ctx.compilationUnit.tpdTree)
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
  context: Context,
  typedNodes: TypedNodes = TypedNodes.empty
)

private[internal] object ScalaParseResultConstants:
  val ObjectScriptPrefix: String = "object __SbtScript__ {\n"
  val ObjectScriptWrapperName: String = "__SbtScript__"

/**
 * Represents a warning or error from the Scala compiler.
 */
case class ScalaWarning(message: String, line: Int, column: Int, level: String)
