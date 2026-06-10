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

import dotty.tools.dotc.ast.{Trees, untpd, tpd}
import dotty.tools.dotc.core.Constants.*
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.util.Spans
import org.openrewrite.Tree
import org.openrewrite.java.tree.*
import org.openrewrite.java.marker.ImplicitReturn
import org.openrewrite.java.marker.OmitParentheses
import org.openrewrite.marker.Markers
import org.openrewrite.scala.marker.Implicit
import org.openrewrite.scala.marker.LambdaParameter
import org.openrewrite.scala.marker.IndentedSyntax
import org.openrewrite.scala.marker.OmitBraces
import org.openrewrite.scala.marker.OmitImportBraces
import org.openrewrite.scala.marker.PackageObject
import org.openrewrite.scala.marker.SObject
import org.openrewrite.scala.marker.Semicolon
import org.openrewrite.scala.marker.TypeProjection
import org.openrewrite.scala.marker.ScalaForLoop
import org.openrewrite.scala.marker.BlockArgument
import org.openrewrite.scala.marker.CommaContinuation
import org.openrewrite.scala.marker.FunctionApplication
import org.openrewrite.scala.marker.AsInstanceOfPrefix
import org.openrewrite.scala.marker.TypeAscription
import org.openrewrite.scala.marker.UnderscorePlaceholderLambda
import org.openrewrite.scala.marker.PartialFunctionLiteral
import org.openrewrite.scala.marker.ContextFunctionArrow
import org.openrewrite.scala.marker.Curried
import org.openrewrite.scala.marker.InfixNotation
import org.openrewrite.scala.marker.RightAssociative
import org.openrewrite.scala.marker.ValVarKeyword
import org.openrewrite.scala.tree.S

import java.util
import java.util.{Collections, Arrays}
import scala.jdk.CollectionConverters.*

/**
 * Visitor that traverses the Scala compiler AST and builds OpenRewrite LST nodes.
 */
class ScalaTreeVisitor(
  source: String,
  offsetAdjustment: Int = 0,
  typeMapping: Option[ScalaTypeMapping] = None
)(implicit ctx: Context) {

  private var cursor = 0
  private var isInImportContext = false
  private var currentSyntheticParams: Set[String] = Set.empty

  /** Look up JavaType by span (primary) or name (fallback). */
  private def typeFor(span: Spans.Span): JavaType = typeMapping.map(_.typeFor(span)).orNull
  private def typeForName(name: String): JavaType = typeMapping.map(_.typeForName(name)).orNull

  /** Look up JavaType.Method by span (primary) or name (fallback). */
  private def methodTypeFor(span: Spans.Span): JavaType.Method = typeMapping.map(_.methodTypeFor(span)).orNull
  private def methodTypeForName(name: String): JavaType.Method = typeMapping.map(_.methodTypeForName(name)).orNull

  /** Look up JavaType.Variable by span (primary) or name (fallback). */
  private def variableTypeFor(span: Spans.Span): JavaType.Variable = typeMapping.map(_.variableTypeFor(span)).orNull
  private def variableTypeForName(name: String): JavaType.Variable = typeMapping.map(_.variableTypeForName(name)).orNull

  /** Resolve a fully-qualified type name to a JavaType. */
  private def typeForFqn(fqn: String): JavaType = typeMapping.map(_.typeForFqn(fqn)).orNull

  /** Extract JavaType from a tree node: try span-based, then direct .tpe, then name-based. */
  private def typeOfTree(tree: Trees.Tree[?]): JavaType = {
    // 1. Span-based lookup (matches untpd spans to tpd tree via start position)
    if (tree.span.exists) {
      val result = typeFor(tree.span)
      if (result != null) return result
    }
    // 2. Direct .tpe (works if tree is already typed)
    try {
      val tpe = tree.tpe
      if (tpe != null && tpe.isInstanceOf[dotty.tools.dotc.core.Types.Type]) {
        val result = typeMapping.map(_.mapType(tpe.asInstanceOf[dotty.tools.dotc.core.Types.Type])).orNull
        if (result != null) return result
      }
    } catch {
      case _: Throwable =>
    }
    // 3. Name-based fallback
    tree match {
      case id: Trees.Ident[?] => typeForName(id.name.toString)
      case vd: Trees.ValDef[?] => typeForName(vd.name.toString)
      case sel: Trees.Select[?] => typeForName(sel.name.toString)
      case at: Trees.AppliedTypeTree[?] =>
        // For parameterized types like List[String], resolve type of the base type
        at.tpt match {
          case id: Trees.Ident[?] => typeForName(id.name.toString)
          case sel: Trees.Select[?] => typeForName(sel.name.toString)
          case _ => null
        }
      case _ => null
    }
  }

  /** Extract JavaType.Method from a tree node. */
  private def methodTypeOfTree(tree: Trees.Tree[?]): JavaType.Method = {
    // 1. Span-based lookup
    if (tree.span.exists) {
      val result = methodTypeFor(tree.span)
      if (result != null) return result
      // Also try the fun subtree's span for Apply nodes
      tree match {
        case Trees.Apply(fun, _) if fun.span.exists =>
          val funResult = methodTypeFor(fun.span)
          if (funResult != null) return funResult
        case _ =>
      }
    }
    // 2. Direct symbol
    try {
      val sym = tree.symbol
      if (sym != null && sym.exists && (sym.is(dotty.tools.dotc.core.Flags.Method) || sym.isConstructor)) {
        val result = typeMapping.map(_.mapMethodType(sym)).orNull
        if (result != null) return result
      }
    } catch {
      case _: Throwable =>
    }
    // 3. Name-based fallback — try to resolve method name from tree structure
    tree match {
      case dd: Trees.DefDef[?] => methodTypeForName(dd.name.toString)
      case Trees.Apply(fun, _) =>
        // Extract method name from the Apply's function subtree
        fun match {
          case sel: Trees.Select[?] => methodTypeForName(sel.name.toString)
          case id: Trees.Ident[?] => methodTypeForName(id.name.toString)
          case _ => null
        }
      case _ => null
    }
  }

  /** Extract JavaType.Variable from a tree node. */
  private def variableTypeOfTree(tree: Trees.Tree[?]): JavaType.Variable = {
    // 1. Span-based lookup
    if (tree.span.exists) {
      val result = variableTypeFor(tree.span)
      if (result != null) return result
    }
    // 2. Direct symbol
    try {
      val sym = tree.symbol
      if (sym != null && sym.exists && !sym.is(dotty.tools.dotc.core.Flags.Method) && !sym.isClass) {
        val result = typeMapping.map(_.mapVariableType(sym)).orNull
        if (result != null) return result
      }
    } catch {
      case _: Throwable => // fall through to name-based lookup
    }
    // Fallback: try name-based lookup for untyped trees
    tree match {
      case id: Trees.Ident[?] => variableTypeForName(id.name.toString)
      case vd: Trees.ValDef[?] => variableTypeForName(vd.name.toString)
      case _ => null
    }
  }
  
  def getCursor: Int = cursor

  def getSourceText: String = source

  def getOffsetAdjustment: Int = offsetAdjustment

  def updateCursor(position: Int): Unit = {
    val adjustedPosition = Math.max(0, position - offsetAdjustment)
    if (adjustedPosition > cursor && adjustedPosition <= source.length) {
      cursor = adjustedPosition
    }
  }
  
  def visitTree(tree: Trees.Tree[?]): J = {
    tree match {
      // `Trees.TypeTree.isEmpty` returns true for compiler-synthesized inferred types
      // (because they have no symbol yet in the untyped tree), so this case must come
      // *before* the general `tree.isEmpty` short-circuit that routes to visitUnknown.
      case tt: Trees.TypeTree[?] => visitSyntheticTypeTree(tt)
      case _ if tree.isEmpty => visitUnknown(tree)
      case lit: Trees.Literal[?] => visitLiteral(lit)
      case num: untpd.Number => visitNumber(num)
      case id: Trees.Ident[?] => visitIdent(id)
      case app: Trees.Apply[?] => visitApply(app)
      case sel: Trees.Select[?] => visitSelect(sel)
      case infixOp: untpd.InfixOp => visitInfixOp(infixOp)
      case prefixOp: untpd.PrefixOp => visitPrefixOp(prefixOp)
      case postfixOp: untpd.PostfixOp => visitPostfixOp(postfixOp)
      case parens: untpd.Parens => visitParentheses(parens)
      case imp: Trees.Import[?] => visitImport(imp)
      case exp: Trees.Export[?] => visitExport(exp)
      case pkg: Trees.PackageDef[?] => visitPackageDef(pkg)
      case newTree: Trees.New[?] => visitNew(newTree)
      case vd: Trees.ValDef[?] => visitValDef(vd)
      case md: untpd.ModuleDef if isGivenWithBody(md, md.mods) => preserveGivenWithBodyAsText(md.span)
      case md: untpd.ModuleDef => visitModuleDef(md)
      case asg: Trees.Assign[?] => visitAssign(asg)
      case ifTree: Trees.If[?] => visitIf(ifTree)
      case whileTree: Trees.WhileDo[?] => visitWhileDo(whileTree)
      case forTree: untpd.ForDo => visitForDo(forTree)
      case xmlb: untpd.XMLBlock => visitXmlLiteral(xmlb)
      case block: Trees.Block[?] => visitBlock(block)
      case td: Trees.TypeDef[?] if td.isClassDef && isGivenWithBody(td, td.mods) =>
        preserveGivenWithBodyAsText(td.span)
      case td: Trees.TypeDef[?] if td.isClassDef => visitClassDef(td)
      case td: Trees.TypeDef[?] => visitTypeAlias(td)
      case pd: untpd.PatDef => visitPatDef(pd)
      case dd: Trees.DefDef[?] => visitDefDef(dd)
      case ret: Trees.Return[?] => visitReturn(ret)
      case thr: untpd.Throw => visitThrow(thr)
      case ta: Trees.TypeApply[?] => visitTypeApply(ta)
      case at: Trees.AppliedTypeTree[?] => visitAppliedTypeTree(at)
      case func: untpd.Function => visitFunction(func)
      case typed: Trees.Typed[?] => visitTyped(typed)
      case tuple: untpd.Tuple => visitTuple(tuple)
      case tryTree: Trees.Try[?] => visitTryTree(tryTree)
      case parsedTry: untpd.ParsedTry => visitParsedTry(parsedTry)
      case matchTree: Trees.Match[?] => visitMatchTree(matchTree)
      case thisTree: Trees.This[?] => visitThis(thisTree)
      case superTree: Trees.Super[?] => visitSuper(superTree)
      case interp: untpd.InterpolatedString => visitInterpolatedString(interp)
      case sym: untpd.SymbolLit => visitSymbolLit(sym)
      case na: Trees.NamedArg[?] => visitNamedArg(na)
      case bnt: Trees.ByNameTypeTree[?] => visitByNameTypeTree(bnt)
      case tbt: Trees.TypeBoundsTree[?] => visitTypeBoundsTree(tbt)
      case bind: Trees.Bind[?] => visitBind(bind)
      case alt: Trees.Alternative[?] => visitAlternative(alt)
      case stt: Trees.SingletonTypeTree[?] => visitSingletonTypeTree(stt)
      case rtt: Trees.RefinedTypeTree[?] => visitRefinedTypeTree(rtt)
      case ann: Trees.Annotated[?] => visitAnnotated(ann)
      case mac: untpd.MacroTree => visitMacroTree(mac)
      case ext: untpd.ExtMethods => visitExtMethods(ext)
      case forYield: untpd.ForYield => visitForYield(forYield)
      case _ => visitUnknown(tree)
    }
  }
  
  private def visitLiteral(lit: Trees.Literal[?]): J.Literal = {
    val prefix = extractPrefix(lit.span)
    val value = lit.const.value
    val valueSource = extractSource(lit.span)
    val javaType = constantToJavaType(lit.const)
    
    new J.Literal(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      value,
      valueSource,
      Collections.emptyList(),
      javaType
    )
  }
  
  private def visitNumber(num: untpd.Number): J.Literal = {
    val prefix = extractPrefix(num.span)
    val rawSource = extractSource(num.span)

    // The compiler span may include trailing non-numeric characters (e.g., closing parens).
    // Strip them so we can parse the number correctly.
    val valueSource = rawSource.replaceAll("[^0-9a-fA-FxXlLfFdDeE._+-]", "")

    // Strip underscore digit separators; valueSource keeps them for printing.
    val parseSource = valueSource.replace("_", "")

    // Parse the number to determine its type and value
    val (value: Any, javaType: JavaType.Primitive) = parseSource match {
      case s if s.startsWith("0x") || s.startsWith("0X") =>
        // Hexadecimal literal
        val hexStr = s.substring(2)
        val longVal = java.lang.Long.parseLong(hexStr, 16)
        if (longVal <= Integer.MAX_VALUE) {
          (java.lang.Integer.valueOf(longVal.toInt), JavaType.Primitive.Int)
        } else {
          (java.lang.Long.valueOf(longVal), JavaType.Primitive.Long)
        }
      case s if s.endsWith("L") || s.endsWith("l") =>
        (java.lang.Long.valueOf(s.dropRight(1)), JavaType.Primitive.Long)
      case s if s.endsWith("F") || s.endsWith("f") =>
        (java.lang.Float.valueOf(s.dropRight(1)), JavaType.Primitive.Float)
      case s if s.endsWith("D") || s.endsWith("d") =>
        (java.lang.Double.valueOf(s.dropRight(1)), JavaType.Primitive.Double)
      case s if s.contains(".") || s.contains("e") || s.contains("E") =>
        (java.lang.Double.valueOf(s), JavaType.Primitive.Double)
      case s =>
        try {
          (java.lang.Integer.valueOf(s), JavaType.Primitive.Int)
        } catch {
          case _: NumberFormatException =>
            (java.lang.Long.valueOf(s), JavaType.Primitive.Long)
        }
    }
    
    new J.Literal(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      value,
      valueSource,
      Collections.emptyList(),
      javaType
    )
  }
  
  private def visitIdent(id: Trees.Ident[?]): J = {
    val prefix = extractPrefix(id.span)
    val sourceText = extractSource(id.span) // Extract source to move cursor
    var simpleName = id.name.toString

    // Special handling for wildcard imports: convert Scala's "_" to Java's "*"
    // This is needed because J.Import expects "*" for wildcard imports
    if (simpleName == "_" && isInImportContext) {
      simpleName = "*"
      ident(simpleName, prefix)
    } else if (simpleName == "_" || simpleName.matches("_\\$\\d+") || (currentSyntheticParams.nonEmpty && currentSyntheticParams.contains(simpleName))) {
      // This is an expression wildcard for partially applied functions or pattern matching
      // Synthetic parameters like _$1, _$2 are always wildcards (generated by compiler for underscore placeholders)
      val wildcard = new S.Wildcard(
        Tree.randomId(),
        prefix,
        Markers.EMPTY,
        typeForName(simpleName)
      )
      wildcard
    } else {
      // Preserve backtick-quoted form when source uses it — id.name.toString strips the backticks.
      if (sourceText.startsWith("`") && sourceText.endsWith("`") && sourceText.length >= 2) {
        simpleName = sourceText
      }
      ident(simpleName, prefix, typeOfTree(id), variableTypeOfTree(id))
    }
  }

  private def visitApply(app: Trees.Apply[?]): J = {
    // In Scala, binary operations like "1 + 2" are parsed as Apply(Select(1, +), List(2))
    // Unary operations like "-x" are parsed as Apply(Select(x, unary_-), List())
    // Constructor calls like "new Person()" are parsed as Apply(New(Person), List())
    // Annotations like "@deprecated" are parsed as Apply(Select(New(Ident(deprecated)), <init>), List())
    
    // Check if this is an annotation pattern (will be handled specially when called from visitClassDef)
    // Annotations look like Apply(Select(New(...), <init>), args) with @ in source
    // Constructor calls look the same but have "new" in source
    val isAnnotationPattern = {
      def checkAnnotation(fun: Trees.Tree[?]): Boolean = fun match {
        case sel: Trees.Select[?] if sel.name.toString == "<init>" =>
          sel.qualifier match {
            case _: Trees.New[?] =>
              if (app.span.exists) {
                val adjustedStart = Math.max(0, app.span.start - offsetAdjustment)
                val adjustedEnd = Math.max(0, app.span.end - offsetAdjustment)
                if (adjustedStart < adjustedEnd && adjustedEnd <= source.length) {
                  source.substring(adjustedStart, adjustedEnd).trim.startsWith("@")
                } else false
              } else false
            case _ => false
          }
        // Handle TypeApply wrapper for annotations with type params like @throws[Exception]
        case ta: Trees.TypeApply[?] => checkAnnotation(ta.fun)
        case _ => false
      }
      checkAnnotation(app.fun)
    }
    
    if (isAnnotationPattern) {
      // This is an annotation - convert to J.Annotation
      return visitAnnotation(app)
    }
    
    app.fun match {
      case newTree: Trees.New[?] =>
        // This is a constructor call with arguments (shouldn't happen in Scala 3)
        visitNewClassWithArgs(newTree, app)
      case sel: Trees.Select[?] if sel.name.toString == "<init>" =>
        // This is a constructor call like new Person()
        sel.qualifier match {
          case newTree: Trees.New[?] =>
            visitNewClassWithArgs(newTree, app)
          case _ =>
            visitUnknown(app)
        }
      case sel: Trees.Select[?] if app.args.isEmpty && isUnaryOperator(sel.name.toString) =>
        // This is a unary operation
        visitUnary(sel)
      case sel: Trees.Select[?] if app.args.length == 1 && isBinaryOperator(sel.name.toString) && !isExplicitDotSelect(sel) =>
        // This is likely a binary operation (infix notation)
        visitBinary(sel, app.args.head, Some(app.span))
      case sel: Trees.Select[?] =>
        // Method call with dot notation like "obj.method(args)"
        visitMethodInvocation(app)
      case id: Trees.Ident[?] if id.name.toString == "<init>" =>
        // Auxiliary constructor self-call `this(args)`. Dotty parses this as
        // Apply(fun=Ident("<init>"), args) with a synthetic span on the Ident,
        // so we must recover the `this` keyword from source.
        buildKeywordMethodInvocation(app, "this")
      case id: Trees.Ident[?] =>
        // Function application syntax: func(args)
        // This includes array access (arr(0)), function calls, and more
        visitFunctionApplication(app, id)
      case ta: Trees.TypeApply[?] =>
        // Type-parameterized function call: Array[Int](), List.fill[Int](5)(0)
        visitMethodInvocation(app)
      case innerApp: Trees.Apply[?] =>
        // Curried application: matrix(0)(1), List.fill(5)(0)
        visitMethodInvocation(app)
      case sup: Trees.Super[?] =>
        // super(args) — explicit super-constructor call
        buildKeywordMethodInvocation(app, "super")
      case th: Trees.This[?] =>
        // this(args) — auxiliary constructor self-call
        buildKeywordMethodInvocation(app, "this")
      case _ =>
        // Other Apply forms with an expression callee. Model as J.MethodInvocation
        // with a synthetic "apply" name (mirrors Scala's desugaring of `expr(args)`
        // to `expr.apply(args)`) so method-finding recipes can still match.
        val prefix = extractPrefix(app.span)
        def asExpression(j: J): Expression = j match {
          case e: Expression => e
          case _ => new S.StatementExpression(Tree.randomId(), j)
        }
        val select = asExpression(visitTree(app.fun))

        // Detect block argument `(expr) { ... }` vs normal parens `(expr)(args)`
        val firstArgNonWs = if (app.args.nonEmpty) indexOfNextNonWhitespace(cursor) else -1
        val isBlockArg = firstArgNonWs >= 0 && firstArgNonWs < source.length &&
          source.charAt(firstArgNonWs) == '{'

        val args = new util.ArrayList[JRightPadded[Expression]]()
        var argContainerPrefix = Space.EMPTY
        val markers = new util.ArrayList[org.openrewrite.marker.Marker]()
        markers.add(FunctionApplication.create())

        if (isBlockArg) {
          markers.add(new BlockArgument(Tree.randomId()))
          for (arg <- app.args) {
            val argSpace = extractPrefix(arg.span)
            visitTree(arg) match {
              case expr: Expression =>
                args.add(JRightPadded.build(expr.withPrefix(argSpace).asInstanceOf[Expression]))
              case block: J.Block =>
                val blockExpr = new S.StatementExpression(Tree.randomId(), block.withPrefix(argSpace))
                args.add(JRightPadded.build(blockExpr.asInstanceOf[Expression]))
              case _ =>
            }
          }
        } else {
          val openIdx = positionOfNext("(", cursor)
          argContainerPrefix = if (openIdx > cursor) ScalaSpace.format(source, cursor, openIdx) else Space.EMPTY
          if (openIdx >= 0) cursor = openIdx + 1
          for (i <- app.args.indices) {
            val arg = app.args(i)
            val argExpr = asExpression(visitTree(arg))
            val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
            val afterSpace = if (i == app.args.size - 1) {
              val closePos = positionOfNext(")", Math.max(cursor, argEnd))
              if (closePos > argEnd) ScalaSpace.format(source, argEnd, closePos) else Space.EMPTY
            } else {
              val commaPos = positionOfNext(",", Math.max(cursor, argEnd))
              val space = if (commaPos > argEnd) ScalaSpace.format(source, argEnd, commaPos) else Space.EMPTY
              if (commaPos >= cursor) cursor = commaPos + 1
              space
            }
            args.add(new JRightPadded(argExpr, afterSpace, Markers.EMPTY))
          }
          val closeParen = positionOfNext(")", cursor)
          if (closeParen >= 0) cursor = closeParen + 1
        }

        updateCursor(app.span.end)
        val mt = typeFor(app.span) match { case m: JavaType.Method => m; case _ => null }
        val nameId = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
          Collections.emptyList(), "apply", null, null)
        new J.MethodInvocation(Tree.randomId(), prefix,
          Markers.build(markers),
          JRightPadded.build(select), null, nameId,
          JContainer.build(argContainerPrefix, args, Markers.EMPTY), mt)
    }
  }

  /**
   * Build a J.MethodInvocation for keyword-callee forms like {@code super(args)} and
   * {@code this(args)}. The name carries the keyword text so recipes can match on it.
   */
  private def buildKeywordMethodInvocation(app: Trees.Apply[?], keyword: String): J.MethodInvocation = {
    // The Apply for `this(args)` in an auxiliary constructor self-call has a
    // synthetic span covering only the args, not the `this` keyword. Locate
    // the keyword from the current cursor and derive the prefix from there
    // rather than trusting `app.span.start`.
    val kwIdx = positionOfNext(keyword, cursor)
    val prefix = if (kwIdx >= cursor) ScalaSpace.format(source, cursor, kwIdx) else Space.EMPTY
    if (kwIdx >= 0) cursor = kwIdx + keyword.length
    val nameId = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
      Collections.emptyList(), keyword, null, null)
    // Find `(`
    val openIdx = positionOfNext("(", cursor)
    val argsBefore = if (openIdx > cursor) ScalaSpace.format(source, cursor, openIdx) else Space.EMPTY
    if (openIdx >= 0) cursor = openIdx + 1
    def asExpression(j: J): Expression = j match {
      case e: Expression => e
      case _ => new S.StatementExpression(Tree.randomId(), j)
    }
    val args = new util.ArrayList[JRightPadded[Expression]]()
    for (i <- app.args.indices) {
      val arg = app.args(i)
      val argExpr = asExpression(visitTree(arg))
      val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
      val afterSpace = if (i == app.args.size - 1) {
        val closePos = positionOfNext(")", Math.max(cursor, argEnd))
        if (closePos > argEnd) ScalaSpace.format(source, argEnd, closePos) else Space.EMPTY
      } else {
        val commaPos = positionOfNext(",", Math.max(cursor, argEnd))
        val space = if (commaPos > argEnd) ScalaSpace.format(source, argEnd, commaPos) else Space.EMPTY
        if (commaPos >= cursor) cursor = commaPos + 1
        space
      }
      args.add(new JRightPadded(argExpr, afterSpace, Markers.EMPTY))
    }
    val closeParen = positionOfNext(")", cursor)
    if (closeParen >= 0) cursor = closeParen + 1
    updateCursor(app.span.end)
    val mt = typeFor(app.span) match { case m: JavaType.Method => m; case _ => null }
    new J.MethodInvocation(Tree.randomId(), prefix, Markers.EMPTY,
      null, null, nameId,
      JContainer.build(argsBefore, args, Markers.EMPTY), mt)
  }

  private def visitUnary(sel: Trees.Select[?]): J = {
    val expr = visitTree(sel.qualifier).asInstanceOf[Expression]
    val operator = mapUnaryOperator(sel.name.toString)
    
    new J.Unary(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      JLeftPadded.build(operator),
      expr,
      typeFor(sel.span)
    )
  }

  private def isUnaryOperator(name: String): Boolean = {
    name match {
      case "unary_-" | "unary_+" | "unary_!" | "unary_~" => true
      case _ => false
    }
  }
  
  private def visitAnnotation(app: Trees.Apply[?]): J = {
    val prefix = extractPrefix(app.span)
    
    
    // Extract the annotation type and arguments
    // Handle both direct Select(New(...), <init>) and TypeApply(Select(New(...), <init>), types)
    def extractAnnotationTypeTree(fun: Trees.Tree[?]): Option[Trees.Tree[?]] = fun match {
      case sel: Trees.Select[?] if sel.name.toString == "<init>" =>
        sel.qualifier match {
          case newTree: Trees.New[?] => Some(newTree.tpt)
          case _ => None
        }
      case ta: Trees.TypeApply[?] => extractAnnotationTypeTree(ta.fun)
      case _ => None
    }
    val tpt = extractAnnotationTypeTree(app.fun) match {
      case Some(t) => t
      case None => return visitUnknown(app)
    }
    val args = app.args

    // Create the annotation type: an Ident for a simple name (@deprecated) or a
    // Select chain for a qualified name (@scala.annotation.implicitNotFound).
    val annotTypeTree: NameTree = tpt match {
      case id: Trees.Ident[?] => ident(id.name.toString)
      case sel: Trees.Select[?] =>
        // Qualified name: skip the leading '@', then map the Select chain to a J.FieldAccess.
        val selStart = Math.max(0, sel.span.start - offsetAdjustment)
        if (selStart > cursor) cursor = selStart
        visitTree(sel) match {
          case nt: NameTree => nt
          case _ => return visitUnknown(app)
        }
      case _ => return visitUnknown(app)
    }
    
    // Advance cursor past "@AnnotName(" before visiting arguments
    if (args.nonEmpty && args.head.span.exists) {
      val firstArgStart = Math.max(0, args.head.span.start - offsetAdjustment)
      if (firstArgStart > cursor) {
        // Skip past "@deprecated(" to position cursor at the first arg
        val between = source.substring(cursor, firstArgStart)
        val parenIdx = between.lastIndexOf('(')
        if (parenIdx >= 0) {
          cursor = cursor + parenIdx + 1
        }
      }
    }

    // Convert arguments
    val arguments = if (args.isEmpty) {
      null
    } else {
      buildArgumentContainer[Trees.Tree[?], Expression](
        args.toSeq,
        arg => {
          val expr = visitTree(arg) match {
            case e: Expression => e
            case j: J => new S.StatementExpression(Tree.randomId(), j)
            case _ => visitUnknown(arg)
          }
          updateCursor(arg.span.end)
          expr
        },
        ")"
      )
    }
    
    val annotation = new J.Annotation(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      annotTypeTree,
      arguments
    )
    
    // Update cursor to the end of the annotation
    val adjustedEnd = Math.max(0, app.span.end - offsetAdjustment)
    if (adjustedEnd > cursor) {
      cursor = adjustedEnd
    }
    
    
    annotation
  }
  
  private def mapUnaryOperator(op: String): J.Unary.Type = op match {
    case "unary_-" => J.Unary.Type.Negative
    case "unary_+" => J.Unary.Type.Positive
    case "unary_!" => J.Unary.Type.Not
    case "unary_~" => J.Unary.Type.Complement
    case _ => J.Unary.Type.Not // default
  }
  
  private def visitPrefixOp(prefixOp: untpd.PrefixOp): J = {
    val prefix = extractPrefix(prefixOp.span)
    val operator = mapPrefixOperator(prefixOp.op.name.toString)
    
    // Update cursor to the end of the operator
    updateCursor(prefixOp.op.span.end)
    
    // Now visit the expression
    val expr = visitTree(prefixOp.od) match {
      case e: Expression => e
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(prefixOp)
    }

    new J.Unary(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      JLeftPadded.build(operator).withBefore(Space.EMPTY),
      expr,
      JavaType.Primitive.Boolean
    )
  }
  
  private def visitPostfixOp(postfixOp: untpd.PostfixOp): J = {
    // Check if this is a member reference (method _)
    postfixOp.op match {
      case id: Trees.Ident[?] if id.name.toString == "_" =>
        // This is a member reference like "greet _"
        // Extract prefix for the entire member reference expression
        val prefix = extractPrefix(postfixOp.span)
        
        // Visit the containing expression (e.g., "greet")
        // It already has its content, we just need to convert it to a J node
        val expr = visitTree(postfixOp.od) match {
          case id: J.Identifier => id.withPrefix(Space.EMPTY)
          case fa: J.FieldAccess => fa.withPrefix(Space.EMPTY)
          case mi: J.MethodInvocation => mi.withPrefix(Space.EMPTY)
          case e: Expression => e  // For other expressions, keep as is
          case _ => return visitUnknown(postfixOp)
        }
        
        // Update cursor to the end of the expression to avoid duplication
        updateCursor(postfixOp.span.end)
        
        // Extract the space between the method and underscore
        // Use the od (operand) span and op span to find the space
        val spaceBeforeUnderscore = if (postfixOp.od.span.exists && postfixOp.op.span.exists) {
          val odEnd = postfixOp.od.span.end - offsetAdjustment
          val opStart = postfixOp.op.span.start - offsetAdjustment
          if (odEnd >= 0 && opStart <= source.length) {
            val underscoreStart = indexOfNextNonWhitespace(opStart)
            val operandEnd =
              if (odEnd < underscoreStart) {
                odEnd
              } else {
                source.lastIndexWhere(c => !Character.isWhitespace(c), underscoreStart - 1) + 1
              }
            if (operandEnd <= underscoreStart && underscoreStart < source.length && source.charAt(underscoreStart) == '_') {
              ScalaSpace.format(source, operandEnd, underscoreStart)
            } else {
              Space.EMPTY
            }
          } else {
            Space.EMPTY
          }
        } else {
          Space.EMPTY
        }
        
        // Create a member reference
        new J.MemberReference(
          Tree.randomId(),
          prefix,
          Markers.EMPTY,
          JRightPadded.build(expr),
          null, // No type parameters for now
          JLeftPadded.build(ident("_", spaceBeforeUnderscore, typeFor(postfixOp.span))),
          typeFor(postfixOp.span),
          methodTypeFor(postfixOp.span),
          variableTypeFor(postfixOp.span)
        )
        
      case _ =>
        // Scala postfix call `obj op` is equivalent to `obj.op` — model it as a
        // J.MethodInvocation with no arguments and the InfixNotation marker so the
        // printer emits `<select> <name>` rather than `<select>.<name>()`.
        val prefix = extractPrefix(postfixOp.span)

        val selectExpr = visitTree(postfixOp.od) match {
          case e: Expression => e
          case j: J => new S.StatementExpression(Tree.randomId(), j)
          case _ => return visitUnknown(postfixOp)
        }

        val opName = postfixOp.op.name.toString
        val odEnd = Math.max(0, postfixOp.od.span.end - offsetAdjustment)
        val opStart = Math.max(0, postfixOp.op.span.start - offsetAdjustment)
        val namePrefix = if (odEnd <= opStart && odEnd >= 0 && opStart <= source.length) {
          ScalaSpace.format(source, odEnd, opStart)
        } else {
          Space.format(" ")
        }

        updateCursor(postfixOp.span.end)

        val name = ident(opName, namePrefix, typeFor(postfixOp.op.span))

        val markersList = new util.ArrayList[org.openrewrite.marker.Marker]()
        markersList.add(InfixNotation.create())

        new J.MethodInvocation(
          Tree.randomId(),
          prefix,
          Markers.build(markersList),
          JRightPadded.build(selectExpr),
          null,
          name,
          JContainer.build(Space.EMPTY, new util.ArrayList[JRightPadded[Expression]](), Markers.EMPTY),
          methodTypeFor(postfixOp.span)
        )
    }
  }
  
  private def mapPrefixOperator(op: String): J.Unary.Type = op match {
    case "!" => J.Unary.Type.Not
    case "+" => J.Unary.Type.Positive
    case "-" => J.Unary.Type.Negative
    case "~" => J.Unary.Type.Complement
    case _ => J.Unary.Type.Not // default
  }
  
  private def visitFunctionApplication(app: Trees.Apply[?], id: Trees.Ident[?]): J.MethodInvocation = {
    val prefix = extractPrefix(app.span)

    // In Scala, arr(0) is syntactic sugar for arr.apply(0)
    // We'll represent it as a method invocation with "apply" as the method name

    // The select is the identifier (e.g., "arr")
    val select = visitIdent(id).asInstanceOf[Expression]

    // Detect argument delimiter form:
    //   `{ ... }` — block argument (`Seq { 1 }`)
    //   `: ...`   — Scala 3 colon-indented argument (`f: x => ...`)
    //   `( ... )` — normal parenthesized argument list
    val firstArgNonWs = if (app.args.nonEmpty) indexOfNextNonWhitespace(cursor) else -1
    val isBlockArg = firstArgNonWs >= 0 && firstArgNonWs < source.length &&
      source.charAt(firstArgNonWs) == '{'
    val isColonArg = !isBlockArg && firstArgNonWs >= 0 && firstArgNonWs < source.length &&
      source.charAt(firstArgNonWs) == ':'

    val args = new util.ArrayList[JRightPadded[Expression]]()
    var argContainerPrefix = Space.EMPTY
    val markers = new util.ArrayList[org.openrewrite.marker.Marker]()
    import org.openrewrite.scala.marker.FunctionApplication
    markers.add(FunctionApplication.create())

    if (isBlockArg) {
      // Block argument: Seq { 1 } — mark as block arg and visit args directly
      markers.add(new BlockArgument(Tree.randomId()))
      for (arg <- app.args) {
        val argSpace = extractPrefix(arg.span)
        visitTree(arg) match {
          case expr: Expression =>
            args.add(JRightPadded.build(expr.withPrefix(argSpace).asInstanceOf[Expression]))
          case block: J.Block =>
            val blockExpr = new S.StatementExpression(Tree.randomId(), block.withPrefix(argSpace))
            args.add(JRightPadded.build(blockExpr.asInstanceOf[Expression]))
          case _ =>
        }
      }
    } else if (isColonArg) {
      // Colon-indented argument: `f: arg` or `f:\n  arg`.
      markers.add(new IndentedSyntax(Tree.randomId()))
      if (firstArgNonWs > cursor) {
        argContainerPrefix = ScalaSpace.format(source, cursor, firstArgNonWs)
      }
      cursor = firstArgNonWs + 1
      for (arg <- app.args) {
        visitTree(arg) match {
          case expr: Expression =>
            args.add(JRightPadded.build(expr))
          case block: J.Block =>
            val blockExpr = new S.StatementExpression(Tree.randomId(), block)
            args.add(JRightPadded.build(blockExpr.asInstanceOf[Expression]))
          case stmt: Statement =>
            val stmtExpr = new S.StatementExpression(Tree.randomId(), stmt)
            args.add(JRightPadded.build(stmtExpr.asInstanceOf[Expression]))
          case _ =>
        }
      }
    } else {
      // Normal parenthesized arguments: Seq(1, 2)
      val parenPos = positionOfNext("(")
      if (parenPos >= 0) {
        if (parenPos > cursor) {
          argContainerPrefix = ScalaSpace.format(source, cursor, parenPos)
        }
        cursor = parenPos + 1
      }

      for ((arg, i) <- app.args.zipWithIndex) {
        val visited = visitTree(arg)
        val expr: Expression = visited match {
          case e: Expression => e
          case j: J => new S.StatementExpression(Tree.randomId(), j)
          case _ => null
        }
        if (expr != null) {
          val isLast = i == app.args.length - 1
          if (!isLast) {
            val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
            val commaPos = positionOfNext(",", Math.max(cursor, argEnd))
            val beforeComma = if (commaPos > argEnd) ScalaSpace.format(source, argEnd, commaPos) else Space.EMPTY
            if (commaPos >= 0 && commaPos + 1 < source.length) {
              cursor = commaPos + 1
            }
            args.add(JRightPadded.build(expr).withAfter(beforeComma))
          } else {
            val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
            val closePos = positionOfNext(")", Math.max(cursor, argEnd))
            val beforeClose = if (closePos > argEnd) {
              ScalaSpace.format(source, argEnd, closePos)
            } else Space.EMPTY
            args.add(new JRightPadded(expr, beforeClose, Markers.EMPTY))
          }
        }
      }

      // Skip past the closing parenthesis
      val closeParenPos = positionOfNext(")")
      if (closeParenPos >= 0) {
        cursor = closeParenPos + 1
      }
    }

    val methodName = ident("apply")

    new J.MethodInvocation(
      Tree.randomId(),
      prefix,
      Markers.build(markers),
      JRightPadded.build(select),
      null, // typeParameters
      methodName,
      JContainer.build(argContainerPrefix, args, Markers.EMPTY),
      methodTypeOfTree(app)
    )
  }

  private def visitMethodInvocation(app: Trees.Apply[?]): J = {
    val savedCursor = cursor
    val prefix = extractPrefix(app.span)

    // Note: We deliberately don't create J.ArrayAccess for explicit .apply() calls.
    // In Scala, arr.apply(0) is an explicit method call and should be represented as such.
    // Only the implicit apply syntax arr(0) gets the FunctionApplication marker (handled in visitFunctionApplication).
    
    // Check for special cases like Array creation
    app.fun match {
      case sel: Trees.Select[?] if sel.name.toString == "apply" =>
        // Check if this is Array creation: Array.apply(elements...)
        sel.qualifier match {
          case id: Trees.Ident[?] if id.name.toString == "Array" =>
            // This is array creation: Array(1, 2, 3) which desugars to Array.apply(1, 2, 3)
            return visitNewArray(app, sel)
          case ta: Trees.TypeApply[?] =>
            // Type-parameterized constructor: Array[Int]() desugars to Array.apply[Int]()
            // Preserve full source including ()
            val text = extractSource(app.span)
            updateCursor(app.span.end)
            return ident(text, prefix, typeFor(app.span))
          case _ =>
            // Continue with regular method invocation (including explicit .apply() calls)
        }
      case ta: Trees.TypeApply[?] =>
        // Handle type applications like Array[String]("hello", "world")
        ta.fun match {
          case id: Trees.Ident[?] if id.name.toString == "Array" =>
            // Array[T](...) — preserve full source text including type params and args
            val taText = extractSource(app.span)
            updateCursor(app.span.end)
            return ident(taText, prefix, typeFor(app.span))
          case _ =>
            // Other TypeApply: continue with regular method invocation
        }
      case _ =>
        // Continue with regular method invocation
    }
    
    // Handle the method call target
    var select: Expression = null
    var selectAfterSpace: Space = Space.EMPTY
    var methodName: String = ""
    var typeParamsContainer: JContainer[Expression] = null
    app.fun match {
      case sel: Trees.Select[?] =>
        val target = visitTree(sel.qualifier) match {
          case expr: Expression => expr
          case j: J => new S.StatementExpression(Tree.randomId(), j)
          case _ => cursor = savedCursor; return visitUnknown(app)
        }

        // Capture space between qualifier and the `.` (for multi-line chains)
        val dotPos = positionOfNext(".", cursor)
        val selectAfter = if (dotPos > cursor) {
          ScalaSpace.format(source, cursor, dotPos)
        } else Space.EMPTY
        if (dotPos >= 0) cursor = dotPos + 1

        // Update cursor position to after the method name
        if (sel.nameSpan.exists) {
          val nameEnd = Math.max(0, sel.nameSpan.end - offsetAdjustment)
          if (nameEnd > cursor) {
            cursor = nameEnd
          }
        }

        select = target
        selectAfterSpace = selectAfter
        methodName = sel.name.toString

      case id: Trees.Ident[?] =>
        methodName = id.name.toString

      case typeApp: Trees.TypeApply[?] =>
        // TypeApply as method invocation target (e.g., Option.apply[String]("hi"), __P__.p[int])
        // Extract the Select inside the TypeApply to get target and method name
        typeApp.fun match {
          case sel: Trees.Select[?] =>
            val target = visitTree(sel.qualifier) match {
              case expr: Expression => expr
              case j: J => new S.StatementExpression(Tree.randomId(), j)
              case _ => cursor = savedCursor; return visitUnknown(app)
            }
            val dotPos = positionOfNext(".", cursor)
            val selectAfter = if (dotPos > cursor) ScalaSpace.format(source, cursor, dotPos) else Space.EMPTY
            if (dotPos >= 0) cursor = dotPos + 1
            if (sel.nameSpan.exists) {
              val nameEnd = Math.max(0, sel.nameSpan.end - offsetAdjustment)
              if (nameEnd > cursor) cursor = nameEnd
            }
            // Parse [T] type args — advances cursor past `]`
            select = target
            selectAfterSpace = selectAfter
            methodName = sel.name.toString
            typeParamsContainer = parseTypeApplyArgs(typeApp)
          case id: Trees.Ident[?] =>
            val nameEnd = if (id.span.exists) Math.max(0, id.span.end - offsetAdjustment) else cursor
            if (nameEnd > cursor) cursor = nameEnd
            methodName = id.name.toString
            typeParamsContainer = parseTypeApplyArgs(typeApp)
          case _ =>
            cursor = savedCursor; return visitUnknown(app)
        }

      case innerApp: Trees.Apply[?] =>
        // Curried call: f(1)(2), matrix(0)(1), Array.fill(5)(0), foo(1) { in => bar(in) }.
        // Emit as S.FunctionCall — inner Apply is the function, outer arg list is a JContainer.
        def asExpression(j: J): Expression = j match {
          case e: Expression => e
          case _ => new S.StatementExpression(Tree.randomId(), j)
        }
        def finishAtAppEnd(): Unit = if (app.span.exists) {
          val adjustedEnd = Math.max(0, app.span.end - offsetAdjustment)
          if (adjustedEnd > cursor && adjustedEnd <= source.length) cursor = adjustedEnd
        }
        val methodType = typeFor(app.span) match { case m: JavaType.Method => m; case _ => null }

        val fn = asExpression(visitApply(innerApp))
        val innerEnd = Math.max(0, innerApp.span.end - offsetAdjustment)
        if (innerEnd > cursor && innerEnd <= source.length) cursor = innerEnd

        // Outer arg list is `{ ... }` when next non-ws is `{`, `: ...` when it's `:`,
        // otherwise parenthesized `(...)`.
        val firstNonWs = indexOfNextNonWhitespace(cursor)
        val outerArgs = new util.ArrayList[JRightPadded[Expression]]()

        if (firstNonWs < source.length && source.charAt(firstNonWs) == '{') {
          // Block's own prefix captures the space between `)` and `{`, so leave fn's after-space empty.
          for (arg <- app.args) outerArgs.add(JRightPadded.build(asExpression(visitTree(arg))))
          finishAtAppEnd()
          val markers = Markers.build(Collections.singletonList(new BlockArgument(Tree.randomId())))
          return S.FunctionCall.build(Tree.randomId(), prefix, markers,
            JRightPadded.build(fn), JContainer.build(Space.EMPTY, outerArgs, Markers.EMPTY), methodType)
        }

        if (firstNonWs < source.length && source.charAt(firstNonWs) == ':') {
          // Colon-indented arg list: `foldLeft(0):\n  case ... => ...` — the fn-after space
          // captures any whitespace between `)` and `:`, then the cursor moves past `:`
          // so the arg's own prefix carries the newline+indent before its first token.
          val functionAfterSpace = if (firstNonWs > cursor) ScalaSpace.format(source, cursor, firstNonWs) else Space.EMPTY
          cursor = firstNonWs + 1
          for (arg <- app.args) outerArgs.add(JRightPadded.build(asExpression(visitTree(arg))))
          finishAtAppEnd()
          val markers = Markers.build(Collections.singletonList(new IndentedSyntax(Tree.randomId())))
          return S.FunctionCall.build(Tree.randomId(), prefix, markers,
            new JRightPadded(fn, functionAfterSpace, Markers.EMPTY),
            JContainer.build(Space.EMPTY, outerArgs, Markers.EMPTY), methodType)
        }

        val functionAfterSpace = if (firstNonWs > cursor) ScalaSpace.format(source, cursor, firstNonWs) else Space.EMPTY
        if (firstNonWs < source.length) cursor = firstNonWs + 1 // past `(`

        for (i <- app.args.indices) {
          val arg = app.args(i)
          val argExpr = asExpression(visitTree(arg))
          val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
          val afterSpace = if (i == app.args.size - 1) {
            val closePos = positionOfNext(")", Math.max(cursor, argEnd))
            if (closePos > argEnd) ScalaSpace.format(source, argEnd, closePos) else Space.EMPTY
          } else {
            val commaPos = positionOfNext(",", Math.max(cursor, argEnd))
            val space = if (commaPos > argEnd) ScalaSpace.format(source, argEnd, commaPos) else Space.EMPTY
            if (commaPos >= cursor) cursor = commaPos + 1
            space
          }
          outerArgs.add(new JRightPadded(argExpr, afterSpace, Markers.EMPTY))
        }

        val closeParen = positionOfNext(")", cursor)
        if (closeParen >= 0) cursor = closeParen + 1
        finishAtAppEnd()
        return S.FunctionCall.build(Tree.randomId(), prefix, Markers.EMPTY,
          new JRightPadded(fn, functionAfterSpace, Markers.EMPTY),
          JContainer.build(Space.EMPTY, outerArgs, Markers.EMPTY), methodType)

      case _ =>
        // Other function applications — model as J.MethodInvocation with synthetic
        // "apply" name (mirrors Scala's `expr(args)` => `expr.apply(args)` desugaring)
        // so method-finding recipes can match on it.
        def asExpression(j: J): Expression = j match {
          case e: Expression => e
          case _ => new S.StatementExpression(Tree.randomId(), j)
        }
        val select = asExpression(visitTree(app.fun))
        val openIdx = positionOfNext("(", cursor)
        if (openIdx >= 0) cursor = openIdx + 1
        val outerArgs = new util.ArrayList[JRightPadded[Expression]]()
        for (i <- app.args.indices) {
          val arg = app.args(i)
          val argExpr = asExpression(visitTree(arg))
          val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
          val afterSpace = if (i == app.args.size - 1) {
            val closePos = positionOfNext(")", Math.max(cursor, argEnd))
            if (closePos > argEnd) ScalaSpace.format(source, argEnd, closePos) else Space.EMPTY
          } else {
            val commaPos = positionOfNext(",", Math.max(cursor, argEnd))
            val space = if (commaPos > argEnd) ScalaSpace.format(source, argEnd, commaPos) else Space.EMPTY
            if (commaPos >= cursor) cursor = commaPos + 1
            space
          }
          outerArgs.add(new JRightPadded(argExpr, afterSpace, Markers.EMPTY))
        }
        val closeParen = positionOfNext(")", cursor)
        if (closeParen >= 0) cursor = closeParen + 1
        if (app.span.exists) {
          val end = Math.max(0, app.span.end - offsetAdjustment)
          if (end > cursor && end <= source.length) cursor = end
        }
        val mt = typeFor(app.span) match { case m: JavaType.Method => m; case _ => null }
        val nameId = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
          Collections.emptyList(), "apply", null, null)
        return new J.MethodInvocation(Tree.randomId(), prefix,
          Markers.build(Collections.singletonList(FunctionApplication.create())),
          JRightPadded.build(select), null, nameId,
          JContainer.build(Space.EMPTY, outerArgs, Markers.EMPTY), mt)
    }

    // Determine the argument delimiter form:
    //   `{ ... }` — block argument (`list.foreach { x => ... }`)
    //   `: ...`   — Scala 3 colon-indented argument (`list.foreach: x => ...`)
    //   `( ... )` — normal parenthesized argument list
    val firstArgNonWs = if (app.args.nonEmpty) indexOfNextNonWhitespace(cursor) else -1
    val isBlockArg = firstArgNonWs >= 0 && firstArgNonWs < source.length &&
      source.charAt(firstArgNonWs) == '{'
    val isColonArg = !isBlockArg && firstArgNonWs >= 0 && firstArgNonWs < source.length &&
      source.charAt(firstArgNonWs) == ':'

    var argContainerPrefix = Space.EMPTY
    val args = new util.ArrayList[JRightPadded[Expression]]()

    if (isBlockArg) {
      // Block argument: list.foreach { x => ... }
      for (arg <- app.args) {
        visitTree(arg) match {
          case expr: Expression =>
            args.add(JRightPadded.build(expr))
          case block: J.Block =>
            // Keep the block as-is — the printer uses BlockArgument marker to print { }
            val blockExpr = new S.StatementExpression(Tree.randomId(), block)
            args.add(JRightPadded.build(blockExpr.asInstanceOf[Expression]))
          case _ => cursor = savedCursor; return visitUnknown(app)
        }
      }
    } else if (isColonArg) {
      // Colon-indented argument: `f: arg` or `obj.method:\n  arg`. The captured
      // space-before-`:` goes on the JContainer's prefix so it round-trips; the
      // argument's own prefix carries the newline+indent before its first token.
      if (firstArgNonWs > cursor) {
        argContainerPrefix = ScalaSpace.format(source, cursor, firstArgNonWs)
      }
      cursor = firstArgNonWs + 1
      for (arg <- app.args) {
        visitTree(arg) match {
          case expr: Expression =>
            args.add(JRightPadded.build(expr))
          case block: J.Block =>
            val blockExpr = new S.StatementExpression(Tree.randomId(), block)
            args.add(JRightPadded.build(blockExpr.asInstanceOf[Expression]))
          case stmt: Statement =>
            val stmtExpr = new S.StatementExpression(Tree.randomId(), stmt)
            args.add(JRightPadded.build(stmtExpr.asInstanceOf[Expression]))
          case _ => cursor = savedCursor; return visitUnknown(app)
        }
      }
    } else {
      // Normal parenthesized arguments: list.foreach(x => ...)
      if (app.args.nonEmpty) {
        val parenPos = positionOfNext("(")
        if (parenPos >= 0) {
          if (parenPos > cursor) {
            argContainerPrefix = ScalaSpace.format(source, cursor, parenPos)
          }
          cursor = parenPos + 1
        }
      }

      for (i <- app.args.indices) {
        val arg = app.args(i)
        val visited = visitTree(arg)
        val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
        val isLastArg = i == app.args.size - 1
        val afterSpace = if (isLastArg) {
          val closePos = positionOfNext(")", Math.max(cursor, argEnd))
          if (closePos > argEnd) ScalaSpace.format(source, argEnd, closePos) else Space.EMPTY
        } else {
          val commaPos = positionOfNext(",", Math.max(cursor, argEnd))
          val space = if (commaPos > argEnd) ScalaSpace.format(source, argEnd, commaPos) else Space.EMPTY
          if (commaPos >= cursor) cursor = commaPos + 1
          space
        }
        visited match {
          case expr: Expression =>
            args.add(new JRightPadded(expr, afterSpace, Markers.EMPTY))
          case stmt: Statement =>
            // Statements like throw are expressions in Scala — wrap
            val stmtExpr = new S.StatementExpression(Tree.randomId(), stmt)
            args.add(new JRightPadded(stmtExpr.asInstanceOf[Expression], afterSpace, Markers.EMPTY))
          case _ => cursor = savedCursor; return visitUnknown(app)
        }
      }

      // Skip past the closing parenthesis
      if (app.args.nonEmpty) {
        val closeParenPos = positionOfNext(")")
        if (closeParenPos >= 0) {
          cursor = closeParenPos + 1
        }
      }
    }

    // Extract space before method name (may include comments like /*__p0__*/ for template params)
    val nameSpace = app.fun match {
      case sel: Trees.Select[?] if sel.nameSpan.exists =>
        val nameStart = Math.max(0, sel.nameSpan.start - offsetAdjustment)
        // Find '.' before the name
        val dotSearch = source.lastIndexOf('.', nameStart - 1)
        if (dotSearch >= 0 && dotSearch + 1 < nameStart) {
          ScalaSpace.format(source, dotSearch + 1, nameStart)
        } else Space.EMPTY
      case ta: Trees.TypeApply[?] => ta.fun match {
        case sel: Trees.Select[?] if sel.nameSpan.exists =>
          val nameStart = Math.max(0, sel.nameSpan.start - offsetAdjustment)
          val dotSearch = source.lastIndexOf('.', nameStart - 1)
          if (dotSearch >= 0 && dotSearch + 1 < nameStart) {
            ScalaSpace.format(source, dotSearch + 1, nameStart)
          } else Space.EMPTY
        case _ => Space.EMPTY
      }
      case _ => Space.EMPTY
    }

    val name = ident(methodName, nameSpace)

    val argContainer = JContainer.build(
      argContainerPrefix,
      args,
      Markers.EMPTY
    )

    // Update cursor to end of the apply expression
    if (app.span.exists) {
      val adjustedEnd = Math.max(0, app.span.end - offsetAdjustment)
      if (adjustedEnd > cursor && adjustedEnd <= source.length) {
        cursor = adjustedEnd
      }
    }

    val markers = if (isBlockArg) {
      Markers.build(Collections.singletonList(new BlockArgument(Tree.randomId())))
    } else if (isColonArg) {
      Markers.build(Collections.singletonList(new IndentedSyntax(Tree.randomId())))
    } else {
      Markers.EMPTY
    }

    new J.MethodInvocation(
      Tree.randomId(),
      prefix,
      markers,
      if (select != null) new JRightPadded(select, selectAfterSpace, Markers.EMPTY) else null,
      typeParamsContainer,
      name,
      argContainer,
      methodTypeOfTree(app)
    )
  }

  private def visitArrayAccess(app: Trees.Apply[?], sel: Trees.Select[?]): J = {
    val prefix = extractPrefix(app.span)
    
    // Visit the array/collection expression
    val array = visitTree(sel.qualifier) match {
      case expr: Expression => expr
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(app)
    }

    // Visit the index expression
    val index = visitTree(app.args.head) match {
      case expr: Expression => expr
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(app)
    }
    
    // Create the dimension with the index
    val dimension = new J.ArrayDimension(
      Tree.randomId(),
      Space.EMPTY, // Space before '['
      Markers.EMPTY,
      JRightPadded.build(index)
    )
    
    new J.ArrayAccess(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      array,
      dimension,
      typeFor(app.span)
    )
  }

  private def visitNewArray(app: Trees.Apply[?], sel: Trees.Select[?]): J = {
    val prefix = extractPrefix(app.span)
    
    // In Scala, Array(1, 2, 3) is syntactic sugar for Array.apply(1, 2, 3)
    // We need to map this to J.NewArray
    
    // For now, we'll assume no explicit type parameters (handled elsewhere)
    val typeExpression: TypeTree = null
    
    // Visit array dimensions (empty for Scala array literals)
    val dimensions = Collections.emptyList[J.ArrayDimension]()
    
    // Visit the array initializer elements
    val elements = new util.ArrayList[Expression]()
    for (arg <- app.args) {
      visitTree(arg) match {
        case expr: Expression => elements.add(expr)
        case j: J => elements.add(new S.StatementExpression(Tree.randomId(), j))
        case _ => return visitUnknown(app)
      }
    }

    // Create the initializer container
    val initializer = if (elements.isEmpty) {
      null
    } else {
      // Extract space before opening parenthesis (which acts like opening brace in Java)
      val initPrefix = sourceBefore("(")
      import scala.jdk.CollectionConverters.*
      buildArgumentContainer[Expression, Expression](
        elements.asScala.toSeq, identity, ")", initPrefix, Markers.EMPTY)
    }

    // Update cursor to end of expression
    updateCursor(app.span.end)

    new J.NewArray(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      typeExpression,
      dimensions,
      initializer,
      typeFor(app.span)
    )
  }

  private def visitNewArrayWithType(app: Trees.Apply[?], ta: Trees.TypeApply[?]): J = {
    val prefix = extractPrefix(app.span)
    
    // In Scala, Array[String]("hello", "world") creates a typed array
    // We need to map this to J.NewArray with a type expression
    
    // Visit the type parameter
    val typeExpression = if (ta.args.nonEmpty) {
      visitTree(ta.args.head) match {
        case tt: TypeTree => tt
        case _ => null
      }
    } else {
      null
    }
    
    // Visit array dimensions (empty for Scala array literals)
    val dimensions = Collections.emptyList[J.ArrayDimension]()
    
    // Update cursor to skip past the type parameter section before processing arguments
    if (ta.args.nonEmpty && ta.args.head.span.exists) {
      // Move cursor past the closing ] of the type parameter
      val typeEnd = Math.max(0, ta.args.head.span.end - offsetAdjustment)
      val closeBracketPos = positionOfNext("]", typeEnd)
      if (closeBracketPos >= 0) {
        cursor = closeBracketPos + 1
      }
    }
    
    // Visit the array initializer elements
    val elements = new util.ArrayList[Expression]()
    for (arg <- app.args) {
      visitTree(arg) match {
        case expr: Expression => elements.add(expr)
        case j: J => elements.add(new S.StatementExpression(Tree.randomId(), j))
        case _ => return visitUnknown(app)
      }
    }

    // Create the initializer container
    val initializer = if (elements.isEmpty) {
      // Empty array with type: Array[Int]()
      val initPrefix = sourceBefore("(")
      // Look for closing paren
      sourceBefore(")")
      JContainer.build(initPrefix, Collections.emptyList[JRightPadded[Expression]](), Markers.EMPTY)
    } else {
      val initPrefix = sourceBefore("(")
      import scala.jdk.CollectionConverters.*
      buildArgumentContainer[Expression, Expression](
        elements.asScala.toSeq, identity, ")", initPrefix, Markers.EMPTY)
    }
    
    // Update cursor to end of expression
    updateCursor(app.span.end)
    
    new J.NewArray(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      typeExpression,
      dimensions,
      initializer,
      typeFor(app.span)
    )
  }

  private def isBinaryOperator(name: String): Boolean = {
    // Only include operators that map directly to J.Binary.Type.
    // Scala-specific operators like ::, ++, etc. should be treated as
    // infix method invocations to preserve their source text.
    Set("+", "-", "*", "/", "%", "==", "!=", "<", ">", "<=", ">=",
        "&&", "||", "&", "|", "^", "<<", ">>", ">>>").contains(name)
  }

  private def isExplicitDotSelect(sel: Trees.Select[?]): Boolean = {
    if (!sel.qualifier.span.exists || !sel.nameSpan.exists) {
      return false
    }
    val qualifierEnd = Math.max(0, sel.qualifier.span.end - offsetAdjustment)
    val nameStart = Math.max(0, sel.nameSpan.start - offsetAdjustment)
    qualifierEnd < nameStart &&
      nameStart <= source.length &&
      source.substring(qualifierEnd, nameStart).indexOf('.') >= 0
  }
  
  private def visitBinary(sel: Trees.Select[?], right: Trees.Tree[?], appSpan: Option[Spans.Span] = None): J = {
    // For method calls like "1.+(2)", we need to handle the full span from the Apply node
    val prefix = appSpan match {
      case Some(span) if span.exists => extractPrefix(span)
      case _ => Space.EMPTY
    }
    
    val left = visitTree(sel.qualifier).asInstanceOf[Expression]
    val operator = mapOperator(sel.name.toString)
    val rightExpr = visitTree(right).asInstanceOf[Expression]
    
    // Extract any remaining source from the Apply span if provided
    appSpan.foreach { span =>
      if (span.exists) {
        val adjustedEnd = Math.max(0, span.end - offsetAdjustment)
        if (adjustedEnd > cursor && adjustedEnd <= source.length) {
          cursor = adjustedEnd
        }
      }
    }
    
    new J.Binary(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      left,
      JLeftPadded.build(operator),
      rightExpr,
      typeFor(appSpan.getOrElse(sel.span))
    )
  }

  private def mapOperator(op: String): J.Binary.Type = op match {
    case "+" => J.Binary.Type.Addition
    case "-" => J.Binary.Type.Subtraction
    case "*" => J.Binary.Type.Multiplication
    case "/" => J.Binary.Type.Division
    case "%" => J.Binary.Type.Modulo
    case "==" => J.Binary.Type.Equal
    case "!=" => J.Binary.Type.NotEqual
    case "<" => J.Binary.Type.LessThan
    case ">" => J.Binary.Type.GreaterThan
    case "<=" => J.Binary.Type.LessThanOrEqual
    case ">=" => J.Binary.Type.GreaterThanOrEqual
    case "&&" => J.Binary.Type.And
    case "||" => J.Binary.Type.Or
    case "&" => J.Binary.Type.BitAnd
    case "|" => J.Binary.Type.BitOr
    case "^" => J.Binary.Type.BitXor
    case "<<" => J.Binary.Type.LeftShift
    case ">>" => J.Binary.Type.RightShift
    case ">>>" => J.Binary.Type.UnsignedRightShift
    case _ => 
      // For custom operators or method calls, we'll need a different approach
      // For now, treat as method reference
      J.Binary.Type.Addition // placeholder
  }
  
  private def visitSelect(sel: Trees.Select[?]): J = {
    {
      // Map Select to J.FieldAccess
      // Extract prefix for this select
      val prefix = extractPrefix(sel.span)
      
      // Visit the qualifier (target) - this could be an identifier, another select, etc.
      val target = visitTree(sel.qualifier) match {
        case expr: Expression => expr
        case j: J => new S.StatementExpression(Tree.randomId(), j)
        case _ =>
          // If the qualifier doesn't produce a J at all, fall back to Unknown
          return visitUnknown(sel)
      }
      
      // Extract space before the dot/hash and after it (for the name prefix)
      val qualifierEnd = sel.qualifier.span.end
      val nameStart = sel.nameSpan.start
      var isTypeProjection = false
      var beforeDotSpace: Space = Space.EMPTY
      val dotSpace = if (qualifierEnd < nameStart) {
        val dotStart = Math.max(0, qualifierEnd - offsetAdjustment)
        val nameStartAdjusted = Math.max(0, nameStart - offsetAdjustment)
        if (dotStart < nameStartAdjusted && dotStart >= cursor && nameStartAdjusted <= source.length) {
          // If the name is backtick-quoted, the opening backtick sits between the dot and
          // nameSpan.start (nameSpan does not include the backtick). Strip it so that only
          // whitespace ends up in dotSpace; the backtick is re-attached in nameStr below.
          val rawBetween = source.substring(dotStart, nameStartAdjusted)
          val between = if (rawBetween.endsWith("`")) rawBetween.dropRight(1) else rawBetween
          // Check for type projection (#) or member access (.)
          val hashIndex = positionOfNextIn(between, "#", 0)
          val dotIndex = positionOfNextIn(between, ".", 0)
          if (hashIndex >= 0 && (dotIndex < 0 || hashIndex < dotIndex)) {
            isTypeProjection = true
            beforeDotSpace = if (hashIndex > 0) Space.format(between.substring(0, hashIndex)) else Space.EMPTY
            if (hashIndex + 1 < between.length) Space.format(between.substring(hashIndex + 1)) else Space.EMPTY
          } else if (dotIndex >= 0) {
            beforeDotSpace = if (dotIndex > 0) Space.format(between.substring(0, dotIndex)) else Space.EMPTY
            if (dotIndex + 1 < between.length) Space.format(between.substring(dotIndex + 1)) else Space.EMPTY
          } else {
            Space.EMPTY
          }
        } else {
          Space.EMPTY
        }
      } else {
        Space.EMPTY
      }

      // Create the name identifier with type from the Select node.
      // sel.name.toString strips backticks for names with special characters (e.g. text/html(UTF-8)),
      // so we reconstruct the backtick-quoted form when the source has one.
      // nameSpan covers only the raw name (without backticks); the opening backtick sits at
      // nameSpan.start-1 and the closing one at nameSpan.end in the source.
      val nameStr = {
        val ns = sel.nameSpan
        if (ns.exists) {
          val adjStart = Math.max(0, ns.start - offsetAdjustment)
          val adjEnd = Math.max(0, ns.end - offsetAdjustment)
          if (adjStart >= 0 && adjEnd <= source.length && adjEnd > adjStart) {
            if (adjStart > 0 && source.charAt(adjStart - 1) == '`' && adjEnd < source.length && source.charAt(adjEnd) == '`')
              "`" + source.substring(adjStart, adjEnd) + "`"
            else
              source.substring(adjStart, adjEnd)
          } else sel.name.toString
        } else sel.name.toString
      }
      val name = ident(nameStr, dotSpace, typeOfTree(sel), variableTypeOfTree(sel))
      
      // Consume up to the end of the selection
      if (sel.span.exists) {
        val adjustedEnd = Math.max(0, sel.span.end - offsetAdjustment)
        if (adjustedEnd > cursor && adjustedEnd <= source.length) {
          cursor = adjustedEnd
        }
      }
      
      val faMarkers = if (isTypeProjection) {
        Markers.build(Collections.singletonList(new TypeProjection(Tree.randomId())))
      } else Markers.EMPTY

      new J.FieldAccess(
        Tree.randomId(),
        prefix,
        faMarkers,
        target,
        new JLeftPadded(beforeDotSpace, name, Markers.EMPTY),
        null
      )
    }
  }
  
  private def visitInfixOp(infixOp: untpd.InfixOp): J = {
    val opName = infixOp.op.name.toString

    // Compound assignment like +=, -=, *=, etc. Must end with a single '=' and base
    // must be a known arithmetic/bitwise op. This excludes user-defined operators
    // like ===, =:=, <:<, !==, which are infix method calls, not compound assigns.
    def isCompoundAssign(op: String): Boolean = {
      if (!op.endsWith("=") || op.length < 2) return false
      if (op == "==" || op == "!=" || op == "<=" || op == ">=") return false
      val base = op.dropRight(1)
      base match {
        case "+" | "-" | "*" | "/" | "%" | "&" | "|" | "^" | "<<" | ">>" | ">>>" => true
        case _ => false
      }
    }

    if (isCompoundAssign(opName)) {
      // This is a compound assignment like +=, -=, *=, /=
      val prefix = extractPrefix(infixOp.span)
      
      // Visit the left side (variable)
      val variable = visitTree(infixOp.left) match {
        case expr: Expression => expr
        case j: J => new S.StatementExpression(Tree.randomId(), j)
        case _ => return visitUnknown(infixOp)
      }

      // Map the operator
      val baseOp = opName.dropRight(1) // Remove the '='
      val operator = baseOp match {
        case "+" => J.AssignmentOperation.Type.Addition
        case "-" => J.AssignmentOperation.Type.Subtraction
        case "*" => J.AssignmentOperation.Type.Multiplication
        case "/" => J.AssignmentOperation.Type.Division
        case "%" => J.AssignmentOperation.Type.Modulo
        case "&" => J.AssignmentOperation.Type.BitAnd
        case "|" => J.AssignmentOperation.Type.BitOr
        case "^" => J.AssignmentOperation.Type.BitXor
        case "<<" => J.AssignmentOperation.Type.LeftShift
        case ">>" => J.AssignmentOperation.Type.RightShift
        case ">>>" => J.AssignmentOperation.Type.UnsignedRightShift
        case _ => return visitUnknown(infixOp) // unreachable: isCompoundAssign filters
      }
      
      // Extract space around the operator
      val leftEnd = Math.max(0, infixOp.left.span.end - offsetAdjustment)
      val opStart = Math.max(0, infixOp.op.span.start - offsetAdjustment)
      val opEnd = Math.max(0, infixOp.op.span.end - offsetAdjustment)
      val rightStart = Math.max(0, infixOp.right.span.start - offsetAdjustment)
      
      var operatorSpace = Space.EMPTY
      var valueSpace = Space.EMPTY
      
      if (leftEnd <= opStart && leftEnd >= cursor && opStart <= source.length) {
        operatorSpace = ScalaSpace.format(source, leftEnd, opStart)
      }

      if (opEnd <= rightStart && opEnd >= cursor && rightStart <= source.length) {
        valueSpace = ScalaSpace.format(source, opEnd, rightStart)
      }
      
      // Visit the right side (value) — `x += if (cond) 1 else 2` wraps Statement as Expression
      cursor = Math.max(0, infixOp.right.span.start - offsetAdjustment)
      val value = visitTree(infixOp.right) match {
        case expr: Expression => expr
        case j: J => new S.StatementExpression(Tree.randomId(), j)
        case _ => return visitUnknown(infixOp)
      }

      // Update cursor to the end
      updateCursor(infixOp.span.end)

      new J.AssignmentOperation(
        Tree.randomId(),
        prefix,
        Markers.EMPTY,
        variable,
        JLeftPadded.build(operator).withBefore(operatorSpace),
        value.withPrefix(valueSpace),
        typeOfTree(infixOp)
      )
    } else if (isBinaryOperator(opName)) {
      // This is a regular binary operation like +, -, *, /
      visitBinaryOperation(infixOp)
    } else {
      // This is an infix method call like "list map func"
      visitInfixMethodCall(infixOp)
    }
  }
  
  private def visitBinaryOperation(infixOp: untpd.InfixOp): J = {
    val prefix = extractPrefix(infixOp.span)
    
    // Visit left expression
    val left = visitTree(infixOp.left) match {
      case expr: Expression => expr
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(infixOp)
    }

    // Map operator
    val operator = mapOperator(infixOp.op.name.toString)
    
    // Extract operator space
    val leftEnd = Math.max(0, infixOp.left.span.end - offsetAdjustment)
    val opStart = Math.max(0, infixOp.op.span.start - offsetAdjustment) 
    val opEnd = Math.max(0, infixOp.op.span.end - offsetAdjustment)
    val rightStart = Math.max(0, infixOp.right.span.start - offsetAdjustment)
    
    var operatorSpace = Space.format(" ")
    var rightSpace = Space.format(" ")

    if (leftEnd <= opStart && leftEnd >= cursor && opStart <= source.length) {
      operatorSpace = ScalaSpace.format(source, leftEnd, opStart)
    }

    if (opEnd <= rightStart && opEnd >= cursor && rightStart <= source.length) {
      rightSpace = ScalaSpace.format(source, opEnd, rightStart)
    }
    
    // Visit right expression
    cursor = Math.max(0, infixOp.right.span.start - offsetAdjustment)
    val right = visitTree(infixOp.right) match {
      case expr: Expression => expr
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(infixOp)
    }

    // Update cursor
    updateCursor(infixOp.span.end)

    new J.Binary(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      left,
      JLeftPadded.build(operator).withBefore(operatorSpace),
      right.withPrefix(rightSpace),
      typeOfTree(infixOp)
    )
  }

  private def visitInfixMethodCall(infixOp: untpd.InfixOp): J = {
    val prefix = extractPrefix(infixOp.span)

    // Visit the left-hand-side expression as it appears in source
    val leftExpr = visitTree(infixOp.left) match {
      case expr: Expression => expr
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(infixOp)
    }

    val methodName = infixOp.op.name.toString
    // Scala: any operator ending in ':' is right-associative, so `a op: b` means `b.op:(a)`.
    val rightAssoc = methodName.endsWith(":")

    // Extract space before the method name
    val leftEnd = Math.max(0, infixOp.left.span.end - offsetAdjustment)
    val opStart = Math.max(0, infixOp.op.span.start - offsetAdjustment)
    val methodNameSpace = if (leftEnd <= opStart && leftEnd >= cursor && opStart <= source.length) {
      cursor = opStart
      ScalaSpace.format(source, leftEnd, opStart)
    } else {
      Space.format(" ")
    }

    // Move cursor past the method name
    cursor = Math.max(0, infixOp.op.span.end - offsetAdjustment)

    // Extract space between the operator and the right-hand-side expression
    val opEnd = Math.max(0, infixOp.op.span.end - offsetAdjustment)
    val rightStart = Math.max(0, infixOp.right.span.start - offsetAdjustment)
    val rhsSpace = if (opEnd <= rightStart && opEnd >= cursor && rightStart <= source.length) {
      cursor = rightStart
      ScalaSpace.format(source, opEnd, rightStart)
    } else {
      Space.format(" ")
    }

    // Visit the right-hand-side expression as it appears in source
    val savedCursorArg = cursor
    val rightExpr = visitTree(infixOp.right) match {
      case expr: Expression => expr
      case block: J.Block =>
        // Block arguments in infix calls: `"test" should { ... }`
        new S.StatementExpression(Tree.randomId(), block.withPrefix(rhsSpace))
      case _ => cursor = savedCursorArg; return visitUnknown(infixOp)
    }

    // Create the method name identifier
    val name = ident(methodName, methodNameSpace)

    // For right-associative operators, store semantically: select = right, argument = left.
    // The printer checks for RightAssociative to restore the syntactic order on output.
    val (selectExpr, argExpr) =
      if (rightAssoc) (rightExpr.withPrefix(rhsSpace), leftExpr)
      else (leftExpr, rightExpr.withPrefix(rhsSpace))

    val args = new util.ArrayList[JRightPadded[Expression]]()
    args.add(JRightPadded.build(argExpr).withAfter(Space.EMPTY))

    val markersList = new util.ArrayList[org.openrewrite.marker.Marker]()
    markersList.add(InfixNotation.create())
    if (rightAssoc) markersList.add(RightAssociative.create())

    new J.MethodInvocation(
      Tree.randomId(),
      prefix,
      Markers.build(markersList),
      JRightPadded.build(selectExpr),
      null, // typeParameters
      name,
      JContainer.build(Space.EMPTY, args, Markers.EMPTY),
      methodTypeOfTree(infixOp)
    )
  }

  private def visitParentheses(parens: untpd.Parens): J = {
    // Extract prefix - but check if cursor is already at the opening paren
    val adjustedStart = Math.max(0, parens.span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, parens.span.end - offsetAdjustment)
    
    
    
    val prefix = if (cursor <= adjustedStart) {
      extractPrefix(parens.span)
    } else {
      // Cursor is already past the start, don't extract prefix
      Space.EMPTY
    }
    
    // Update cursor to skip the opening parenthesis
    if (cursor == adjustedStart) {
      cursor = adjustedStart + 1
    }
    
    // Try to access the inner expression directly
    // Parens might have a field like 'tree' or 'expr'
    val innerTree = try {
      // Try different possible field names
      val treeField = parens.getClass.getDeclaredFields.find(f => 
        f.getName.contains("tree") || f.getName.contains("expr") || f.getName.contains("arg")
      )
      
      treeField match {
        case Some(field) =>
          field.setAccessible(true)
          field.get(parens).asInstanceOf[Trees.Tree[?]]
        case None =>
          // Fall back to productElement approach
          if (parens.productArity > 0) {
            parens.productElement(0).asInstanceOf[Trees.Tree[?]]
          } else {
            return visitUnknown(parens)
          }
      }
    } catch {
      case _: Exception => return visitUnknown(parens)
    }
    
    // Visit the inner tree. Scala's `(if (x) a else b)` — inner may be a Statement;
    // wrap via StatementExpression to keep printing round-trippable.
    val innerExpr = visitTree(innerTree) match {
      case expr: Expression => expr
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(parens)
    }
    
    // Extract space before the closing parenthesis
    val innerEnd = innerTree.span.end
    val parenEnd = parens.span.end
    val closingSpace = if (innerEnd < parenEnd - 1) {
      val adjustedInnerEnd = Math.max(0, innerEnd - offsetAdjustment)
      val adjustedParenEnd = Math.max(0, parenEnd - 1 - offsetAdjustment)
      if (adjustedInnerEnd < adjustedParenEnd && adjustedInnerEnd >= cursor && adjustedParenEnd <= source.length) {
        ScalaSpace.format(source, adjustedInnerEnd, adjustedParenEnd)
      } else {
        Space.EMPTY
      }
    } else {
      Space.EMPTY
    }
    
    // Update cursor to just after the closing parenthesis
    // The span might include extra characters, so we need to find the actual closing paren
    val spanText = source.substring(adjustedStart, Math.min(adjustedEnd, source.length))
    val lastParenIndex = spanText.lastIndexOf(')')
    if (lastParenIndex >= 0) {
      cursor = adjustedStart + lastParenIndex + 1
    } else {
      updateCursor(parens.span.end)
    }
    
    new J.Parentheses[Expression](
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      JRightPadded.build(innerExpr).withAfter(closingSpace)
    )
  }
  
  private def visitNewClassWithArgs(newTree: Trees.New[?], app: Trees.Apply[?]): J = {
    // For curried constructor calls `new Foo(a)(b)`, the inner `Apply(Select(New(Foo), <init>), List(a))`
    // has a span starting at `Foo` rather than `new`. Locate the `new` keyword explicitly so we don't
    // swallow it as the NewClass prefix.
    val typeStart = Math.max(0, newTree.tpt.span.start - offsetAdjustment)
    val newPos = positionOfNext("new")
    val prefix = if (newPos >= cursor && newPos < typeStart) {
      val spaceBeforeNew = if (newPos > cursor) ScalaSpace.format(source, cursor, newPos) else Space.EMPTY
      cursor = newPos + 3 // Move past "new"
      spaceBeforeNew
    } else {
      extractPrefix(app.span)
    }

    // Extract space between "new" and type
    val typeSpace = if (cursor < typeStart && typeStart <= source.length) {
      val spaceStr = source.substring(cursor, typeStart)
      cursor = typeStart
      Space.format(spaceStr)
    } else {
      Space.EMPTY
    }
    
    // Visit the type being instantiated
    val clazz = visitTree(newTree.tpt) match {
      case typeTree: TypeTree => typeTree.withPrefix(typeSpace)
      case id: J.Identifier => id.withPrefix(typeSpace)
      case fieldAccess: J.FieldAccess => fieldAccess.withPrefix(typeSpace)
      case _ => return visitUnknown(app)
    }
    
    // Extract space before parentheses
    val typeEnd = Math.max(0, newTree.tpt.span.end - offsetAdjustment)
    val argsStart = if (app.args.nonEmpty) {
      Math.max(0, app.args.head.span.start - offsetAdjustment)
    } else {
      Math.max(0, app.span.end - offsetAdjustment) - 1 // Looking for the closing paren
    }
    
    var beforeParenSpace = Space.EMPTY
    var hasParentheses = false
    if (typeEnd < argsStart && typeEnd >= cursor && argsStart <= source.length) {
      val between = source.substring(typeEnd, argsStart)
      val parenIndex = positionOfNextIn(between, "(", 0)
      if (parenIndex >= 0) {
        hasParentheses = true
        beforeParenSpace = Space.format(between.substring(0, parenIndex))
        cursor = typeEnd + parenIndex + 1
      }
    } else if (app.args.isEmpty && typeEnd >= cursor) {
      // Check if there are parentheses for empty args
      val endBound = Math.min(source.length, Math.max(0, app.span.end - offsetAdjustment))
      if (typeEnd < endBound) {
        val after = source.substring(typeEnd, endBound)
        hasParentheses = after.contains("(") && after.contains(")")
        if (hasParentheses) {
          val parenIndex = positionOfNextIn(after, "(", 0)
          beforeParenSpace = Space.format(after.substring(0, parenIndex))
          cursor = typeEnd + positionOfNextIn(after, ")", 0) + 1
        }
      }
    }
    
    // Visit arguments
    val args = new util.ArrayList[JRightPadded[Expression]]()
    for (i <- app.args.indices) {
      val arg = app.args(i)
      val thisStart = Math.max(0, arg.span.start - offsetAdjustment)

      // Extract prefix space for this argument: space after previous comma,
      // or space after '(' for the first argument.
      var argPrefix = Space.EMPTY
      if (i == 0) {
        if (cursor < thisStart && thisStart <= source.length) {
          argPrefix = ScalaSpace.format(source, cursor, thisStart)
          cursor = thisStart
        }
      } else {
        val prevEnd = Math.max(0, app.args(i - 1).span.end - offsetAdjustment)
        if (prevEnd < thisStart && prevEnd >= cursor && thisStart <= source.length) {
          val between = source.substring(prevEnd, thisStart)
          val commaIndex = positionOfNextIn(between, ",", 0)
          if (commaIndex >= 0) {
            // Attach space between the previous arg's end and the comma to the
            // previously-added rightPadded element so it isn't lost.
            if (commaIndex > 0) {
              val prevRp = args.remove(args.size - 1)
              args.add(prevRp.withAfter(Space.format(between.substring(0, commaIndex))))
            }
            argPrefix = Space.format(between.substring(commaIndex + 1))
            cursor = prevEnd + commaIndex + 1
          }
        }
      }

      visitTree(arg) match {
        case expr: Expression =>
          // Apply the prefix space to the expression
          val exprWithPrefix: Expression = expr.withPrefix(argPrefix)

          // For the last arg, capture trailing whitespace before ')'.
          val afterSpace = if (i == app.args.size - 1) {
            val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
            val closePos = positionOfNext(")", Math.max(cursor, argEnd))
            if (closePos > argEnd) ScalaSpace.format(source, argEnd, closePos)
            else Space.EMPTY
          } else Space.EMPTY

          args.add(new JRightPadded[Expression](exprWithPrefix, afterSpace, Markers.EMPTY))
        case j: J =>
          // Scala statement-as-expression (e.g. if/else, match, block, try) wrapped so it
          // can sit in an argument list. Apply argPrefix and trailing space the same way
          // the Expression branch does so leading newlines and comments aren't lost.
          val stmtExpr: Expression = new S.StatementExpression(Tree.randomId(), j).withPrefix(argPrefix)
          val afterSpace = if (i == app.args.size - 1) {
            val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
            val closePos = positionOfNext(")", Math.max(cursor, argEnd))
            if (closePos > argEnd) ScalaSpace.format(source, argEnd, closePos)
            else Space.EMPTY
          } else Space.EMPTY
          args.add(new JRightPadded[Expression](stmtExpr, afterSpace, Markers.EMPTY))
        case _ => return visitUnknown(app)
      }
    }
    
    // Update cursor to the end
    updateCursor(app.span.end)
    
    val argContainer = if (!hasParentheses && args.isEmpty) {
      // No parentheses and no arguments - e.g. "new Person"
      null
    } else if (args.isEmpty) {
      // Empty parentheses - e.g. "new Person()"
      JContainer.build(beforeParenSpace, Collections.emptyList[JRightPadded[Expression]](), Markers.EMPTY)
    } else {
      // Has arguments - e.g. "new Person(name, age)"
      JContainer.build(beforeParenSpace, args, Markers.EMPTY)
    }
    
    new J.NewClass(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      null, // enclosing
      Space.EMPTY,
      clazz,
      argContainer,
      null, // body for anonymous classes
      {
        // Try span-based lookup first
        val mt = methodTypeOfTree(app)
        if (mt != null) mt
        else {
          // Fall back: construct a constructor method type from the class type.
          // The clazz expression already has its type set from visitTree.
          val classType = clazz match {
            case id: J.Identifier => id.getType
            case pt: J.ParameterizedType => pt.getType
            case fa: J.FieldAccess => fa.getType
            case _ => null
          }
          if (classType != null) {
            val fq = TypeUtils.asFullyQualified(classType)
            if (fq != null) {
              // Construct a minimal constructor method type using the class FQN
              typeMapping.map(_.mapConstructorType(fq)).orNull
            } else null
          } else null
        }
      }
    )
  }

  private def visitNew(newTree: Trees.New[?]): J = {
    // Anonymous classes in Scala are represented as New nodes with Template bodies
    // Example: new Runnable { def run() = ... }
    // The Template contains the parent types and the body implementation
    
    // Extract prefix but skip the "new" keyword
    var prefix = extractPrefix(newTree.span)
    
    // Skip past "new" in the source if present. Leave the whitespace after "new"
    // for the clazz.prefix (or body.prefix when clazz is null) so it round-trips faithfully.
    if (newTree.span.exists) {
      val start = Math.max(0, newTree.span.start - offsetAdjustment)
      val end = Math.max(0, newTree.span.end - offsetAdjustment)
      if (start >= cursor && end <= source.length && start < end) {
        val sourceText = source.substring(start, end)
        val newIndex = positionOfNextIn(sourceText, "new", 0)
        if (newIndex >= 0) {
          val afterNew = start + newIndex + 3
          if (afterNew <= end) {
            updateCursor(afterNew)
          }
        }
      }
    }
    
    // The New node's tpt is the Template containing the anonymous class definition
    newTree.tpt match {
      case template: Trees.Template[?] =>
        // Extract the parent type(s) - usually the first parent is the main type
        val parents = template.parents.filter(p => p.span.exists && !p.span.isSynthetic)

        // Extract the first parent's class type and constructor arguments (if any).
        // For multi-parent (mixin) Templates, only the first parent may carry args.
        val (firstClazz, args): (TypeTree, JContainer[Expression]) = parents.headOption match {
          case None =>
            // `new {}` — no parents at all (anonymous class extending the implicit Object)
            (null, null)
          case Some(firstParent) =>
            firstParent match {
              case app: Trees.Apply[?] if app.fun.isInstanceOf[Trees.Select[?]] &&
                   app.fun.asInstanceOf[Trees.Select[?]].name.toString == "<init>" =>
                // Constructor call with arguments: new Person("John", 30) { ... }
                val sel = app.fun.asInstanceOf[Trees.Select[?]]
                sel.qualifier match {
                  case newInner: Trees.New[?] =>
                    // Visit the type tree directly
                    val typeTree = visitTree(newInner.tpt).asInstanceOf[TypeTree]

                    // Now handle the arguments
                    val argContainer = if (app.args.nonEmpty) {
                      val args = new util.ArrayList[JRightPadded[Expression]]()

                      // Find the opening parenthesis between the type and the first arg.
                      // `app.span.start` may coincide with the type's start, so search
                      // from the type's end up to the first arg's start instead.
                      var beforeParenSpace = Space.EMPTY
                      if (app.span.exists && app.args.nonEmpty) {
                        val typeEnd = Math.max(0, newInner.tpt.span.end - offsetAdjustment)
                        val firstArgStart = Math.max(0, app.args.head.span.start - offsetAdjustment)
                        val searchStart = Math.max(cursor, typeEnd)

                        if (searchStart < firstArgStart && firstArgStart <= source.length) {
                          val between = source.substring(searchStart, firstArgStart)
                          val parenIndex = positionOfNextIn(between, "(", 0)
                          if (parenIndex >= 0) {
                            beforeParenSpace = Space.format(between.substring(0, parenIndex))
                            updateCursor(searchStart + parenIndex + 1)
                          }
                        }
                      }

                      // Visit arguments
                      for ((arg, i) <- app.args.zipWithIndex) {
                        var argPrefix = Space.EMPTY
                        if (i == 0) {
                          val thisStart = Math.max(0, arg.span.start - offsetAdjustment)
                          if (cursor < thisStart && thisStart <= source.length) {
                            argPrefix = ScalaSpace.format(source, cursor, thisStart)
                            updateCursor(thisStart)
                          }
                        } else {
                          val prevEnd = Math.max(0, app.args(i - 1).span.end - offsetAdjustment)
                          val thisStart = Math.max(0, arg.span.start - offsetAdjustment)
                          if (prevEnd < thisStart && prevEnd >= cursor && thisStart <= source.length) {
                            val between = source.substring(prevEnd, thisStart)
                            val commaIndex = positionOfNextIn(between, ",", 0)
                            if (commaIndex >= 0) {
                              argPrefix = Space.format(between.substring(commaIndex + 1))
                              updateCursor(prevEnd + commaIndex + 1)
                            }
                          }
                        }

                        val argJ = visitTree(arg)
                        val visitedArg: Expression = argJ match {
                          case e: Expression => e.withPrefix(argPrefix).asInstanceOf[Expression]
                          case j: J => new S.StatementExpression(Tree.randomId(), j).asInstanceOf[Expression].withPrefix(argPrefix).asInstanceOf[Expression]
                          case _ => visitUnknown(arg).asInstanceOf[Expression].withPrefix(argPrefix).asInstanceOf[Expression]
                        }
                        args.add(new JRightPadded[Expression](visitedArg, Space.EMPTY, Markers.EMPTY))
                      }

                      // Extract space before closing parenthesis for last argument
                      if (app.span.exists && app.args.nonEmpty) {
                        val lastArgEnd = Math.max(0, app.args.last.span.end - offsetAdjustment)
                        val appEnd = Math.max(0, app.span.end - offsetAdjustment)
                        if (lastArgEnd < appEnd && lastArgEnd >= cursor && appEnd <= source.length) {
                          val remaining = source.substring(lastArgEnd, appEnd)
                          val closeParenIndex = positionOfNextIn(remaining, ")", 0)
                          if (closeParenIndex >= 0) {
                            val beforeCloseSpace = Space.format(remaining.substring(0, closeParenIndex))
                            updateCursor(lastArgEnd + closeParenIndex + 1)

                            // Update the last argument's after space
                            if (!args.isEmpty) {
                              val lastArg = args.get(args.size() - 1)
                              args.set(args.size() - 1, lastArg.withAfter(beforeCloseSpace))
                            }
                          }
                        }
                      }

                      JContainer.build(beforeParenSpace, args, Markers.EMPTY)
                    } else {
                      null
                    }

                    (typeTree, argContainer)
                  case _ =>
                    (visitUnknown(sel.qualifier).asInstanceOf[TypeTree], null)
                }
              case _ =>
                // Simple interface/trait: new Runnable { ... }
                val typeTree = visitTree(firstParent).asInstanceOf[TypeTree]
                (typeTree, null)
            }
        }

        // For multi-parent templates (`new X with Y { ... }`), wrap the parents in a
        // J.IntersectionType so the printer can emit the `with` chain. The Scala
        // printer overrides visitIntersectionType to use `with` as the separator.
        val clazz: TypeTree = if (parents.size > 1) {
          val intersectionPrefix = firstClazz.getPrefix
          val mixinElements = new util.ArrayList[JRightPadded[TypeTree]]()
          mixinElements.add(new JRightPadded[TypeTree](
            firstClazz.withPrefix(Space.EMPTY).asInstanceOf[TypeTree],
            sourceBefore("with"),
            Markers.EMPTY))
          for (i <- 1 until parents.size - 1) {
            val tt = visitTree(parents(i)) match {
              case t: TypeTree => t
              case _ => return visitUnknown(parents(i))
            }
            mixinElements.add(new JRightPadded[TypeTree](tt, sourceBefore("with"), Markers.EMPTY))
          }
          val lastTt = visitTree(parents.last) match {
            case t: TypeTree => t
            case _ => return visitUnknown(parents.last)
          }
          mixinElements.add(JRightPadded.build(lastTt))
          new J.IntersectionType(
            Tree.randomId(),
            intersectionPrefix,
            Markers.EMPTY,
            JContainer.build(Space.EMPTY, mixinElements, Markers.EMPTY)
          )
        } else {
          firstClazz
        }

        // Create the anonymous class body when source has a `{` (braced) or `:`
        // (Scala 3 braceless, end-of-line colon) between the parents and the end
        // of the New expression.
        val newTreeEnd = Math.max(0, newTree.span.end - offsetAdjustment)
        val (bodyDelimiter, bodyDelimiterIndex) = if (cursor < newTreeEnd && newTreeEnd <= source.length) {
          val afterCursor = source.substring(cursor, newTreeEnd)
          val braceIndex = positionOfNextIn(afterCursor, "{", 0)
          // For Scala 3 braceless: look for `:` at end of line (not `: Type` annotation).
          // A braceless body colon is followed by a newline, not by a type name.
          val colonIndex = {
            var result = -1
            var idx = positionOfNextIn(afterCursor, ":", 0)
            while (idx >= 0 && result < 0) {
              if (idx + 1 >= afterCursor.length) {
                result = idx
              } else {
                val afterColon = afterCursor.substring(idx + 1)
                val nextNonSpace = afterColon.indexWhere(c => c != ' ' && c != '\t')
                if (nextNonSpace < 0 || afterColon.charAt(nextNonSpace) == '\n' || afterColon.charAt(nextNonSpace) == '\r') {
                  result = idx
                }
              }
              if (result < 0) idx = positionOfNextIn(afterCursor, ":", idx + 1)
            }
            result
          }
          if (braceIndex >= 0 && (colonIndex < 0 || braceIndex < colonIndex)) {
            ('{', braceIndex)
          } else if (colonIndex >= 0) {
            (':', colonIndex)
          } else {
            (' ', -1)
          }
        } else {
          (' ', -1)
        }

        val body = if (bodyDelimiterIndex >= 0) {
          val isBraceless = bodyDelimiter == ':'

          // Filter out synthetic nodes, the compiler-added constructor, and self-reference
          val bodyStatements = template.body.filter { stat =>
            if (stat.span.isSynthetic || !stat.span.exists) false
            else stat match {
              case dd: Trees.DefDef[?] if dd.name.toString == "<init>" => false
              case vd: Trees.ValDef[?] if vd.name.toString == "_" => false
              case _ => true
            }
          }

          // Consume up to and past the body delimiter.
          val searchStart = Math.max(0, cursor)
          val beforeDelim = ScalaSpace.format(source.substring(searchStart, searchStart + bodyDelimiterIndex))
          updateCursor(searchStart + bodyDelimiterIndex + 1)

          // Convert body statements
          val statementPaddings = new util.ArrayList[JRightPadded[Statement]]()

          bodyStatements.foreach { stmt =>
            val stmtJ = visitTree(stmt)
            if (stmtJ.isInstanceOf[Statement]) {
              statementPaddings.add(new JRightPadded[Statement](
                stmtJ.asInstanceOf[Statement],
                Space.EMPTY,
                Markers.EMPTY
              ))
            }
          }

          // Extract end space (before `}` for braced; remaining whitespace for braceless).
          var endSpace = Space.EMPTY
          if (newTree.span.exists) {
            val newEnd = Math.max(0, newTree.span.end - offsetAdjustment)
            if (isBraceless) {
              if (cursor < newEnd && newEnd <= source.length) {
                endSpace = ScalaSpace.format(source, cursor, Math.min(newEnd, source.length))
                updateCursor(newEnd)
              }
            } else if (cursor < newEnd && cursor >= 0 && newEnd <= source.length) {
              val remaining = source.substring(cursor, newEnd)
              val closeBraceIndex = remaining.lastIndexOf('}')
              if (closeBraceIndex >= 0) {
                endSpace = Space.format(remaining.substring(0, closeBraceIndex))
                updateCursor(cursor + closeBraceIndex + 1)
              }
            }
          }

          val blockMarkers = if (isBraceless) {
            Markers.build(Collections.singletonList(new IndentedSyntax(Tree.randomId())))
          } else Markers.EMPTY

          new J.Block(
            Tree.randomId(),
            beforeDelim,
            blockMarkers,
            new JRightPadded[java.lang.Boolean](false, Space.EMPTY, Markers.EMPTY),
            statementPaddings,
            endSpace
          )
        } else {
          null
        }

        new J.NewClass(
          Tree.randomId(),
          prefix,
          Markers.EMPTY,
          null, // enclosing
          Space.SINGLE_SPACE, // Space after "new" keyword
          clazz,
          args,
          body,
          methodTypeOfTree(newTree)
        )

      case _ =>
        // Not an anonymous class, shouldn't happen in visitNew
        visitUnknown(newTree)
    }
  }

  private def visitImport(imp: Trees.Import[?]): J = {
    val classification = classifyImportOrExport(imp, "import")
    classification match {
      case ImportShape.Simple =>
        val (prefix, qualid) = buildImportOrExportQualid(imp)
        new J.Import(
          Tree.randomId(),
          prefix,
          Markers.EMPTY,
          JLeftPadded.build(false),
          qualid,
          null
        )
      case ImportShape.Brace =>
        visitBraceImportOrExport(imp, "import")
      case ImportShape.SimpleContinuation =>
        visitContinuationSimple(imp, "import")
      case ImportShape.BraceContinuation =>
        visitContinuationBrace(imp, "import")
    }
  }

  private def visitExport(exp: Trees.Export[?]): J = {
    // Scala 3 export clauses share their AST shape with imports
    // (Dotty: both extend `Trees.ImportOrExport` with `expr` + `selectors`),
    // so we reuse the same qualid-building logic.
    val classification = classifyImportOrExport(exp, "export")
    classification match {
      case ImportShape.Simple =>
        val (prefix, qualid) = buildImportOrExportQualid(exp)
        new S.Export(Tree.randomId(), prefix, Markers.EMPTY, qualid)
      case ImportShape.Brace =>
        visitBraceImportOrExport(exp, "export")
      case ImportShape.SimpleContinuation =>
        visitContinuationSimple(exp, "export")
      case ImportShape.BraceContinuation =>
        visitContinuationBrace(exp, "export")
    }
  }

  private object ImportShape extends Enumeration {
    val Simple, Brace, SimpleContinuation, BraceContinuation = Value
  }

  private def spanSource(tree: Trees.ImportOrExport[?]): String = {
    if (!tree.span.exists) return ""
    val s = Math.max(0, tree.span.start - offsetAdjustment)
    val e = Math.max(0, tree.span.end - offsetAdjustment)
    if (s < e && e <= source.length) source.substring(s, e) else ""
  }

  private def classifyImportOrExport(
    tree: Trees.ImportOrExport[?],
    keyword: String
  ): ImportShape.Value = {
    val text = spanSource(tree)
    val hasBraces = text.contains("{") || text.contains(" as ") || text.contains("=>")
    val startsWithKeyword = text.trim.startsWith(keyword)
    if (startsWithKeyword) {
      if (hasBraces) ImportShape.Brace else ImportShape.Simple
    } else {
      if (hasBraces) ImportShape.BraceContinuation else ImportShape.SimpleContinuation
    }
  }

  /**
   * Emit a `J.Import` (or simple `S.Export`) for the *continuation* of a
   * comma-separated group, e.g. the second {@code b._} in
   * {@code import a._, b._}. The Dotty tree's span starts at the qualifier
   * (after the comma); whitespace before the comma becomes the statement's
   * prefix, whitespace after the comma is folded into the qualifier's
   * prefix. The resulting statement carries a `CommaContinuation` marker so
   * the printer emits {@code ,} instead of the keyword.
   */
  private def visitContinuationSimple(
    tree: Trees.ImportOrExport[?],
    keyword: String
  ): J = {
    val adjustedStart = Math.max(0, tree.span.start - offsetAdjustment)
    val commaIdx = source.lastIndexOf(',', adjustedStart - 1)
    if (commaIdx < cursor) {
      throw new IllegalStateException(
        s"Continuation $keyword without preceding ',': '${spanSource(tree)}'"
      )
    }
    val prefix = if (cursor < commaIdx)
      ScalaSpace.format(source.substring(cursor, commaIdx))
    else Space.EMPTY
    cursor = commaIdx + 1

    val qualid = buildQualidFromTree(tree)
    val markers = Markers.EMPTY.addIfAbsent(new CommaContinuation(Tree.randomId()))

    if (keyword == "import") {
      new J.Import(Tree.randomId(), prefix, markers, JLeftPadded.build(false), qualid, null)
    } else {
      new S.Export(Tree.randomId(), prefix, markers, qualid)
    }
  }

  /**
   * Shared qualid construction used by both the keyword-led path and the
   * continuation path. Assumes the cursor is positioned at (or before) the
   * qualifier — whitespace between cursor and the qualifier's source span
   * is captured as the qualifier element's prefix via {@code visitTree}.
   */
  private def buildQualidFromTree(tree: Trees.ImportOrExport[?]): J.FieldAccess = {
    val oldInImportContext = isInImportContext
    isInImportContext = true
    val expr = visitTree(tree.expr)
    isInImportContext = oldInImportContext

    var qualid: J.FieldAccess = expr match {
      case fa: J.FieldAccess => fa
      case id: J.Identifier if tree.selectors.nonEmpty =>
        val selector = tree.selectors.head
        selector match {
          case is @ untpd.ImportSelector(sIdent: Trees.Ident[?], untpd.EmptyTree, untpd.EmptyTree) =>
            if (cursor < source.length && source.charAt(cursor) == '.') cursor += 1
            val selName = wildcardOrName(is, sIdent, tree.span)
            val displaySelName = if (cursor < source.length && source.charAt(cursor) == '`') "`" + selName + "`" else selName
            val selectorType: JavaType = lookupSelectorType(tree.expr, selName)
            new J.FieldAccess(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
              id, JLeftPadded.build(ident(displaySelName, Space.EMPTY, selectorType)), selectorType)
          case _ =>
            throw new IllegalStateException("Unexpected non-simple selector in continuation")
        }
      case _ =>
        throw new IllegalStateException(
          s"Expected FieldAccess or Identifier qualifier, got ${expr.getClass.getSimpleName}"
        )
    }

    val alreadyHandledSelector = expr.isInstanceOf[J.Identifier] && tree.selectors.nonEmpty
    if (!alreadyHandledSelector && tree.selectors.nonEmpty && tree.selectors.size == 1) {
      val selector = tree.selectors.head
      selector match {
        case is @ untpd.ImportSelector(idTree: Trees.Ident[?], untpd.EmptyTree, untpd.EmptyTree) =>
          if (cursor < source.length && source.charAt(cursor) == '.') cursor += 1
          val selectorName = wildcardOrName(is, idTree, tree.span)
          val displaySelectorName = if (cursor < source.length && source.charAt(cursor) == '`') "`" + selectorName + "`" else selectorName
          val selectorType: JavaType = lookupSelectorType(tree.expr, selectorName)
          qualid = new J.FieldAccess(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
            qualid, JLeftPadded.build(ident(displaySelectorName, Space.EMPTY, selectorType)), selectorType)
        case _ =>
      }
    }

    if (tree.span.exists) {
      // updateCursor takes a raw (un-adjusted) span position and applies
      // offsetAdjustment itself; don't pre-subtract here.
      updateCursor(tree.span.end)
    }

    qualid
  }

  /**
   * Parse a brace-form import or export: `keyword qualifier.{ selector, ... }`.
   * Returns an `S.Import` (for `keyword == "import"`) or an `S.Export`
   * (for `"export"`).
   */
  private def visitBraceImportOrExport(
    tree: Trees.ImportOrExport[?],
    keyword: String
  ): J = {
    val adjustedStart = Math.max(0, tree.span.start - offsetAdjustment)
    val prefix: Space = if (cursor < adjustedStart) {
      val text = source.substring(cursor, adjustedStart)
      cursor = adjustedStart
      Space.format(text)
    } else Space.EMPTY

    // Consume the keyword and at most one trailing whitespace (additional
    // whitespace becomes the qualifier element's prefix).
    cursor = adjustedStart + keyword.length
    if (cursor < source.length && Character.isWhitespace(source.charAt(cursor))) {
      cursor += 1
    }

    parseBraceImportOrExportBody(tree, keyword, prefix, Markers.EMPTY)
  }

  /**
   * Emit a brace-form `S.Import` (or `S.Export`) for the continuation of a
   * comma-separated group, e.g. the second `b.{y}` in
   * `import a.{x}, b.{y}`. Mirrors [[visitContinuationSimple]] but feeds
   * the brace parser.
   */
  private def visitContinuationBrace(
    tree: Trees.ImportOrExport[?],
    keyword: String
  ): J = {
    val adjustedStart = Math.max(0, tree.span.start - offsetAdjustment)
    val commaIdx = source.lastIndexOf(',', adjustedStart - 1)
    if (commaIdx < cursor) {
      throw new IllegalStateException(
        s"Continuation brace $keyword without preceding ',': '${spanSource(tree)}'"
      )
    }
    val prefix = if (cursor < commaIdx)
      ScalaSpace.format(source.substring(cursor, commaIdx))
    else Space.EMPTY
    cursor = commaIdx + 1

    val markers = Markers.EMPTY.addIfAbsent(new CommaContinuation(Tree.randomId()))
    parseBraceImportOrExportBody(tree, keyword, prefix, markers)
  }

  /**
   * Shared body of the brace-form parser. Assumes the cursor is positioned
   * at the qualifier start (after the keyword for the leading form, after
   * the comma for a continuation). The qualifier element's prefix absorbs
   * any leading whitespace.
   */
  private def parseBraceImportOrExportBody(
    tree: Trees.ImportOrExport[?],
    keyword: String,
    prefix: Space,
    markers: Markers
  ): J = {
    val adjustedEnd = Math.max(0, tree.span.end - offsetAdjustment)
    val spanText = spanSource(tree)

    val oldInImportContext = isInImportContext
    isInImportContext = true
    val qualifierJ = visitTree(tree.expr)
    isInImportContext = oldInImportContext
    val qualifier: Expression = qualifierJ match {
      case e: Expression => e
      case other => throw new IllegalStateException(
        s"Expected Expression for $keyword qualifier, got ${other.getClass.getSimpleName}"
      )
    }

    // Whitespace (and comments) between the qualifier and the `.`.
    val afterQualifier = cursor
    val i = indexOfNextNonWhitespace(afterQualifier)
    val qualifierRightPad = ScalaSpace.format(source.substring(afterQualifier, i))
    if (i >= source.length || source.charAt(i) != '.') {
      throw new IllegalStateException(
        s"Expected '.' before brace in $keyword at position $i: '${spanText}'"
      )
    }
    cursor = i + 1

    // Whitespace (and comments) between `.` and `{` (or, for the Scala 3
    // unbraced single-selector rename form `import a.b.X as Y`, between `.`
    // and the selector name).
    val afterDot = cursor
    val j = indexOfNextNonWhitespace(afterDot)
    val isBraceless = j < source.length && source.charAt(j) != '{'
    val beforeBrace = if (isBraceless) Space.EMPTY
      else ScalaSpace.format(source.substring(afterDot, j))
    if (!isBraceless) {
      cursor = j + 1
    }
    val bracelessMarkers = if (isBraceless)
      markers.addIfAbsent(new OmitImportBraces(Tree.randomId()))
      else markers

    val selectorElems = new util.ArrayList[JRightPadded[S.ImportSelector]]()
    val selectors = tree.selectors
    if (isBraceless) {
      if (selectors.size != 1) {
        throw new IllegalStateException(
          s"Expected exactly one selector for braceless $keyword at position $j: '${spanText}'"
        )
      }
      val sel = selectors.head
      val selStart = cursor
      val p = indexOfNextNonWhitespace(selStart)
      val selPrefix = ScalaSpace.format(source.substring(selStart, p))
      cursor = p
      val selectorNode = parseImportSelector(sel)
      selectorElems.add(new JRightPadded(selectorNode.withPrefix(selPrefix), Space.EMPTY, Markers.EMPTY))
      if (cursor < adjustedEnd) cursor = adjustedEnd
      val selectorContainerB: JContainer[S.ImportSelector] =
        JContainer.build(Space.EMPTY, selectorElems, Markers.EMPTY)
      return (if (keyword == "import") {
        S.Import.build(
          Tree.randomId(),
          prefix,
          bracelessMarkers,
          new JRightPadded(qualifier, qualifierRightPad, Markers.EMPTY),
          beforeBrace,
          selectorContainerB
        )
      } else {
        S.Export.build(
          Tree.randomId(),
          prefix,
          bracelessMarkers,
          qualifier,
          beforeBrace,
          selectorContainerB
        )
      })
    }
    var idx = 0
    while (idx < selectors.size) {
      val sel = selectors(idx)

      val selStart = cursor
      val p = indexOfNextNonWhitespace(selStart)
      val selPrefix = ScalaSpace.format(source.substring(selStart, p))
      cursor = p

      val selectorNode = parseImportSelector(sel)

      val afterSel = cursor
      val q = indexOfNextNonWhitespace(afterSel)
      val afterChar = if (q < source.length) source.charAt(q) else 0.toChar
      if (afterChar == ',') {
        val sepSpace = ScalaSpace.format(source.substring(afterSel, q))
        cursor = q + 1
        selectorElems.add(new JRightPadded(selectorNode.withPrefix(selPrefix), sepSpace, Markers.EMPTY))
      } else if (afterChar == '}') {
        val tailSpace = ScalaSpace.format(source.substring(afterSel, q))
        cursor = q
        selectorElems.add(new JRightPadded(selectorNode.withPrefix(selPrefix), tailSpace, Markers.EMPTY))
      } else {
        throw new IllegalStateException(
          s"Expected ',' or '}' after selector in $keyword at position $q (got '$afterChar')"
        )
      }
      idx += 1
    }

    if (cursor >= source.length || source.charAt(cursor) != '}') {
      throw new IllegalStateException(
        s"Expected '}' to close $keyword selectors at position $cursor: '${spanText}'"
      )
    }
    cursor += 1
    if (cursor < adjustedEnd) cursor = adjustedEnd

    val selectorContainer: JContainer[S.ImportSelector] =
      JContainer.build(Space.EMPTY, selectorElems, Markers.EMPTY)

    if (keyword == "import") {
      S.Import.build(
        Tree.randomId(),
        prefix,
        markers,
        new JRightPadded(qualifier, qualifierRightPad, Markers.EMPTY),
        beforeBrace,
        selectorContainer
      )
    } else {
      S.Export.build(
        Tree.randomId(),
        prefix,
        markers,
        qualifier,
        beforeBrace,
        selectorContainer
      )
    }
  }

  /**
   * Parse a single brace-import / brace-export selector, advancing the cursor.
   * The prefix space is set by the caller; this method produces a selector
   * with `Space.EMPTY` prefix.
   */
  private def parseImportSelector(sel: untpd.ImportSelector): S.ImportSelector = {
    if (sel.isGiven) {
      val kw = "given"
      if (cursor + kw.length > source.length || !source.startsWith(kw, cursor)) {
        throw new IllegalStateException(s"Expected 'given' selector at position $cursor")
      }
      cursor += kw.length
      val givenType: TypeTree = sel.bound match {
        case untpd.EmptyTree => null
        case bound =>
          visitTree(bound) match {
            case tt: TypeTree => tt
            case other => throw new IllegalStateException(
              s"Expected TypeTree for given bound, got ${other.getClass.getSimpleName}"
            )
          }
      }
      S.ImportSelector.build(
        Tree.randomId(), Space.EMPTY, Markers.EMPTY,
        true /* given */, givenType,
        false /* wildcard */, false /* legacyUnderscore */,
        null /* name */, null /* alias */, false /* useAsKeyword */
      )
    } else if (sel.isWildcard) {
      if (cursor >= source.length) {
        throw new IllegalStateException("Unexpected EOF for wildcard selector")
      }
      val wildcardChar = source.charAt(cursor)
      if (wildcardChar != '_' && wildcardChar != '*') {
        throw new IllegalStateException(
          s"Expected '_' or '*' for wildcard selector at position $cursor, got '$wildcardChar'"
        )
      }
      cursor += 1
      S.ImportSelector.build(
        Tree.randomId(), Space.EMPTY, Markers.EMPTY,
        false, null, true, wildcardChar == '_', null, null, false
      )
    } else {
      // Named selector, optionally with `=> alias` or `as alias`.
      val nameStartAbs = Math.max(0, sel.imported.span.start - offsetAdjustment)
      val nameEndAbs = Math.max(0, sel.imported.span.end - offsetAdjustment)
      val displayName = source.substring(nameStartAbs, nameEndAbs)
      val nameIdent = ident(displayName, Space.EMPTY, null)
      cursor = nameEndAbs

      sel.renamed match {
        case untpd.EmptyTree =>
          S.ImportSelector.build(
            Tree.randomId(), Space.EMPTY, Markers.EMPTY,
            false, null, false, false, nameIdent, null, false
          )
        case renamed: Trees.Ident[?] =>
          val p = indexOfNextNonWhitespace(cursor)
          val beforeArrow = ScalaSpace.format(source.substring(cursor, p))
          val useAsKeyword = source.startsWith("as", p)
          val useArrow = source.startsWith("=>", p)
          if (!useAsKeyword && !useArrow) {
            throw new IllegalStateException(
              s"Expected '=>' or 'as' between selector name and alias at position $p"
            )
          }
          cursor = p + 2

          val renamedStartAbs = Math.max(0, renamed.span.start - offsetAdjustment)
          val renamedEndAbs = Math.max(0, renamed.span.end - offsetAdjustment)
          val aliasPrefix = ScalaSpace.format(source.substring(cursor, renamedStartAbs))
          val aliasDisplay = source.substring(renamedStartAbs, renamedEndAbs)
          cursor = renamedEndAbs
          val aliasIdent = ident(aliasDisplay, aliasPrefix, null)
          S.ImportSelector.build(
            Tree.randomId(), Space.EMPTY, Markers.EMPTY,
            false, null, false, false, nameIdent,
            new JLeftPadded(beforeArrow, aliasIdent, Markers.EMPTY),
            useAsKeyword
          )
        case other =>
          throw new IllegalStateException(
            s"Unexpected rename tree in import selector: ${other.getClass.getSimpleName}"
          )
      }
    }
  }

  /**
   * Build the `J.FieldAccess` qualid for a simple import or export — i.e. one
   * with no braces and no aliases. Both `Trees.Import` and `Trees.Export` extend
   * `Trees.ImportOrExport`, which exposes the shared `expr` and `selectors`
   * members the construction depends on.
   *
   * Returns the leading `Space` (whitespace + comments before the keyword) and
   * the qualid. Advances the cursor past the end of the import/export. If the
   * shape can't be modelled, raises via `visitUnknown` so the caller sees a
   * failure rather than a degraded value.
   */
  private def buildImportOrExportQualid(
    tree: Trees.ImportOrExport[?]
  ): (Space, J.FieldAccess) = {
    val adjustedStart = Math.max(0, tree.span.start - offsetAdjustment)
    val prefix = if (cursor < adjustedStart) {
      val prefixText = source.substring(cursor, adjustedStart)
      cursor = adjustedStart
      Space.format(prefixText)
    } else {
      Space.EMPTY
    }

    val keyword =
      if (adjustedStart + "import".length <= source.length && source.startsWith("import", adjustedStart)) "import"
      else "export"
    cursor = Math.max(cursor, adjustedStart + keyword.length)
    if (cursor < source.length && Character.isWhitespace(source.charAt(cursor))) {
      cursor += 1
    }

    val qualid = buildQualidFromTree(tree)
    (prefix, qualid)
  }

  /**
   * For a wildcard selector, read the wildcard char from source so we preserve
   * `*` (Scala 3) vs `_` (legacy) verbatim, and the `given` wildcard form
   * (`import a.given`) which Dotty also models as a wildcard selector.
   * For a named selector, return the name as written in the AST.
   */
  private def wildcardOrName(
    is: untpd.ImportSelector,
    idTree: Trees.Ident[?],
    span: Spans.Span
  ): String = {
    if (!is.isWildcard) return idTree.name.toString
    val adjustedEnd = Math.max(0, span.end - offsetAdjustment)
    if (adjustedEnd > 0 && adjustedEnd <= source.length) {
      val last = source.charAt(adjustedEnd - 1)
      if (last == '*') return "*"
      if (last == '_') return "_"
      // `import a.given` — selector name in the AST is empty/"given"; reconstruct from source.
      val start = Math.max(0, span.start - offsetAdjustment)
      val text = if (start < adjustedEnd) source.substring(start, adjustedEnd) else ""
      if (text.endsWith("given")) return "given"
    }
    "_"
  }

  /**
   * Best-effort type lookup for a selector by FQN. Useful for imports
   * (`scala.collection.mutable` resolves to a known type); for exports the
   * selector usually names a member of a local value, so this typically
   * returns `null` — which is the desired behavior.
   */
  private def lookupSelectorType(expr: Trees.Tree[?], selectorName: String): JavaType = {
    val packagePrefix = extractFqn(expr)
    val fqn = if (packagePrefix.nonEmpty) packagePrefix + "." + selectorName else selectorName
    typeForFqn(fqn)
  }

  /**
   * True for an import or export with no braces and no rename arrows, whose
   * span starts with the given keyword (filters out continuations of
   * comma-separated forms like `import A._, B._`).
   */
  private def isSimpleImportOrExport(tree: Trees.ImportOrExport[?], keyword: String): Boolean = {
    if (!tree.span.exists) return false
    val adjustedStart = Math.max(0, tree.span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, tree.span.end - offsetAdjustment)
    if (adjustedStart < 0 || adjustedEnd > source.length || adjustedEnd <= adjustedStart) return false
    val text = source.substring(adjustedStart, adjustedEnd)
    !text.contains("{") && !text.contains(" as ") && !text.contains("=>") &&
      text.trim.startsWith(keyword)
  }
  
  
  
  private def visitPackageDef(pkg: Trees.PackageDef[?]): J = {
    // Package definitions at the statement level should not be converted to statements
    // They are handled at the compilation unit level
    // Return null to indicate this node should be skipped
    null
  }
  
  private def visitLambdaParameter(vd: Trees.ValDef[?]): J = {
    import dotty.tools.dotc.core.Flags
    import org.openrewrite.scala.marker.LambdaParameter

    val prefix = extractPrefix(vd.span)

    // Check if the type was explicitly written in source or inferred
    // If the source doesn't contain a colon after the name, it's inferred
    val sourceText = extractSource(vd.span)
    val hasExplicitType = sourceText.contains(":")

    // Preserve backtick-quoted names — vd.name.toString strips them. Lambda-param nameSpan
    // can either cover the raw name (matching the `start-1 / end` backtick layout) or include
    // the opening backtick at `start`; cover both shapes.
    val displayName = {
      val rawName = vd.name.toString
      if (vd.nameSpan.exists) {
        val nsStart = Math.max(0, vd.nameSpan.start - offsetAdjustment)
        // dotty names an explicit `_ => expr` parameter `_$N`. The source still reads
        // `_`, so render the wildcard as written.
        if (rawName.startsWith("_$") && nsStart < source.length && source.charAt(nsStart) == '_') "_"
        else if (isBacktickQuotedNameAt(nsStart, rawName.length)) "`" + rawName + "`"
        else if (nsStart < source.length && source.charAt(nsStart) == '`' &&
            nsStart + rawName.length + 1 < source.length &&
            source.charAt(nsStart + rawName.length + 1) == '`') "`" + rawName + "`"
        else rawName
      } else rawName
    }

    // Detect `implicit` modifier on the lambda parameter. The Scala 3 parser
    // exposes it on `vd.mods` but it sits in the source *before* `vd.span.start`
    // (the span covers only the name and optional type). The whitespace before
    // "implicit" is already captured in the enclosing `J.Lambda` prefix, so the
    // modifier itself carries `Space.EMPTY` as its prefix; the space between
    // "implicit" and the parameter name is folded into the name's prefix.
    val nameStartAbs = if (vd.nameSpan.exists)
      Math.max(0, vd.nameSpan.start - offsetAdjustment)
    else
      Math.max(0, vd.span.start - offsetAdjustment)
    val implicitModifier: J.Modifier =
      if (vd.mods != null && vd.mods.is(Flags.Implicit) &&
          nameStartAbs > 0 && nameStartAbs <= source.length) {
        // Walk left over whitespace, then check for the "implicit" keyword.
        var p = nameStartAbs - 1
        while (p >= 0 && Character.isWhitespace(source.charAt(p))) p -= 1
        val kw = "implicit"
        val kwEnd = p + 1
        val kwStart = kwEnd - kw.length
        val boundaryOk = kwStart >= 0 &&
          source.substring(kwStart, kwEnd) == kw &&
          (kwStart == 0 || !Character.isJavaIdentifierPart(source.charAt(kwStart - 1)))
        if (boundaryOk)
          new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
            kw, J.Modifier.Type.LanguageExtension, Collections.emptyList())
        else null
      } else null

    // Space between "implicit" and the parameter name (when modifier is present).
    // `p+1` is the position immediately after the keyword's last character.
    val namePrefix: Space =
      if (implicitModifier != null) {
        var p = nameStartAbs - 1
        while (p >= 0 && Character.isWhitespace(source.charAt(p))) p -= 1
        val gapStart = p + 1
        if (gapStart < nameStartAbs) ScalaSpace.format(source.substring(gapStart, nameStartAbs))
        else Space.EMPTY
      } else Space.EMPTY

    // If there's no explicit type in source, return either a plain identifier
    // or — when an `implicit` modifier is present — a J.VariableDeclarations
    // that can carry it.
    if (!hasExplicitType || vd.tpt == untpd.EmptyTree) {
      if (implicitModifier == null) {
        ident(displayName, prefix)
      } else {
        val name = ident(displayName, namePrefix)
        val variable = new J.VariableDeclarations.NamedVariable(
          Tree.randomId(),
          Space.EMPTY,
          Markers.EMPTY,
          name,
          Collections.emptyList(),
          null,
          null
        )
        new J.VariableDeclarations(
          Tree.randomId(),
          prefix,
          Markers.build(Collections.singletonList(new LambdaParameter())),
          Collections.emptyList(),
          Collections.singletonList(implicitModifier),
          null,
          null,
          Collections.emptyList(),
          Collections.singletonList(JRightPadded.build(variable))
        )
      }
    } else {
      // With a type, we need a full variable declaration
      val name = ident(displayName, namePrefix)

      // Extract the type
      val sourceText = extractSource(vd.span)
      val colonIdx = positionOfNextIn(sourceText, ":", 0)
      val nameLen = displayName.length
      val nameEnd = positionOfNextIn(sourceText, displayName, 0)
      val beforeColon: Space = if (colonIdx > 0 && nameEnd >= 0 && nameEnd + nameLen <= colonIdx) {
        ScalaSpace.format(sourceText.substring(nameEnd + nameLen, colonIdx))
      } else Space.EMPTY
      val typeSpace = if (colonIdx >= 0 && colonIdx + 1 < sourceText.length) {
        ScalaSpace.format(sourceText.substring(colonIdx + 1).takeWhile(_.isWhitespace))
      } else {
        Space.SINGLE_SPACE
      }

      // Use visitTypeTree so type-position shapes that share an untyped node with a
      // value expression (tuple types `(A, B)`, function types `A => B`, repeated
      // types `T*`) map to proper TypeTrees instead of falling through to `null`.
      // Those sub-visitors are cursor-based, so rewind the cursor (parked at the end
      // of the whole parameter span by the `extractSource` above) to the type's start
      // before visiting; the leading space after the colon is applied via `typeSpace`.
      if (vd.tpt.span.exists) {
        cursor = Math.max(0, vd.tpt.span.start - offsetAdjustment)
      }
      val visitedType = visitTypeTree(vd.tpt)
      val typeExpr: TypeTree =
        if (visitedType != null) visitedType.withPrefix(typeSpace) else null

      // Create the variable
      val variable = new J.VariableDeclarations.NamedVariable(
        Tree.randomId(),
        Space.EMPTY,
        Markers.EMPTY,
        name,
        Collections.emptyList(),
        null,
        null
      )

      val modifiers: java.util.List[J.Modifier] =
        if (implicitModifier != null) Collections.singletonList(implicitModifier)
        else Collections.emptyList()

      // Create the variable declarations with a marker to indicate it's a lambda parameter
      new J.VariableDeclarations(
        Tree.randomId(),
        prefix,
        Markers.build(Collections.singletonList(new LambdaParameter())),
        Collections.emptyList(), // no annotations
        modifiers,
        typeExpr,
        if (beforeColon != Space.EMPTY) beforeColon else null,
        Collections.emptyList(),
        Collections.singletonList(JRightPadded.build(variable))
      )
    }
  }
  
  private def visitValDef(vd: Trees.ValDef[?], isLambdaParam: Boolean = false): J = {
    val savedCursorEntry = cursor  // Save for fallback to visitUnknown
    // For lambda parameters, don't look for val/var keywords
    if (isLambdaParam) {
      return visitLambdaParameter(vd)
    }
    
    // Special handling for variables with annotations
    val hasAnnotations = vd.mods != null && vd.mods.annotations.nonEmpty
    val prefix = if (hasAnnotations) {
      // Don't extract prefix yet - annotations will consume their own prefix
      Space.EMPTY
    } else {
      extractPrefix(vd.span)
    }
    
    // Handle annotations first
    val leadingAnnotations = new util.ArrayList[J.Annotation]()
    if (hasAnnotations) {
      for (annot <- vd.mods.annotations) {
        visitTree(annot) match {
          case ann: J.Annotation => leadingAnnotations.add(ann)
          case _ => // Skip if not mapped to annotation
        }
      }
    }
    
    // Extract modifiers and keywords from source
    // When we have annotations, cursor is positioned after them
    val adjustedStart = if (hasAnnotations) cursor else Math.max(0, vd.span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, vd.span.end - offsetAdjustment)
    
    // Extract source to find modifier keywords and val/var
    var valVarKeyword = ""
    var beforeValVarRaw = ""
    var beforeValVar = Space.EMPTY
    var afterValVar = Space.EMPTY
    val modifiers = new util.ArrayList[J.Modifier]()
    var hasExplicitFinal = false
    var hasExplicitLazy = false
    
    // Track the position right after the last modifier keyword (excluding any trailing whitespace).
    // Used later to compute beforeValVar — the whitespace between the last modifier and val/var/given.
    var lastModifierKeywordEnd = -1
    if (adjustedStart >= 0 && adjustedEnd <= source.length && adjustedStart < adjustedEnd) {
      val sourceSnippet = source.substring(adjustedStart, adjustedEnd)

      // First, extract any modifiers before val/var
      // When annotations are present, there may be whitespace before the modifier
      var modifierEndPos = 0
      val trimmedSnippet = sourceSnippet.stripLeading()
      val leadingWs = sourceSnippet.length - trimmedSnippet.length
      modifierEndPos = leadingWs

      // Check for access modifiers (including scoped: private[scope], protected[this])
      if (trimmedSnippet.startsWith("private")) {
        var keyword = "private"
        var keyLen = keyword.length
        val afterKw = trimmedSnippet.substring(keyLen)
        if (afterKw.startsWith("[")) {
          val cb = positionOfNextIn(afterKw, "]", 0)
          if (cb >= 0) { keyword = "private" + afterKw.substring(0, cb + 1); keyLen = keyword.length }
        }
        val modPrefix = if (leadingWs > 0) ScalaSpace.format(sourceSnippet.substring(0, leadingWs)) else Space.EMPTY
        modifiers.add(new J.Modifier(
          Tree.randomId(), modPrefix, Markers.EMPTY,
          keyword, J.Modifier.Type.Private, Collections.emptyList()
        ))
        modifierEndPos = leadingWs + keyLen
        lastModifierKeywordEnd = modifierEndPos
        if (modifierEndPos < sourceSnippet.length && sourceSnippet.charAt(modifierEndPos) == ' ') modifierEndPos += 1
      } else if (trimmedSnippet.startsWith("protected")) {
        var keyword = "protected"
        var keyLen = keyword.length
        val afterKw = trimmedSnippet.substring(keyLen)
        if (afterKw.startsWith("[")) {
          val cb = positionOfNextIn(afterKw, "]", 0)
          if (cb >= 0) { keyword = "protected" + afterKw.substring(0, cb + 1); keyLen = keyword.length }
        }
        val modPrefix = if (leadingWs > 0) ScalaSpace.format(sourceSnippet.substring(0, leadingWs)) else Space.EMPTY
        modifiers.add(new J.Modifier(
          Tree.randomId(), modPrefix, Markers.EMPTY,
          keyword, J.Modifier.Type.Protected, Collections.emptyList()
        ))
        modifierEndPos = leadingWs + keyLen
        lastModifierKeywordEnd = modifierEndPos
        if (modifierEndPos < sourceSnippet.length && sourceSnippet.charAt(modifierEndPos) == ' ') modifierEndPos += 1
      }
      
      // Check for remaining modifiers (in any order) before val/var/def
      // These include: implicit, override, abstract, final, lazy, sealed
      var scanning = true
      while (scanning && modifierEndPos < sourceSnippet.length) {
        val remaining = sourceSnippet.substring(modifierEndPos)
        // When the first modifier in this loop sits directly after annotations (no preceding access
        // modifier consumed leadingWs), preserve that whitespace as the modifier's prefix. Otherwise
        // a newline between `@Ann` and e.g. `lazy val` is silently collapsed to nothing on print.
        val modSpace =
          if (modifierEndPos > leadingWs) Space.SINGLE_SPACE
          else if (leadingWs > 0) ScalaSpace.format(sourceSnippet.substring(0, leadingWs))
          else Space.EMPTY

        if (remaining.startsWith("final ")) {
          hasExplicitFinal = true
          modifiers.add(new J.Modifier(Tree.randomId(), modSpace, Markers.EMPTY,
            "final", J.Modifier.Type.Final, Collections.emptyList()))
          lastModifierKeywordEnd = modifierEndPos + "final".length
          modifierEndPos += "final ".length
        } else if (remaining.startsWith("lazy ")) {
          hasExplicitLazy = true
          modifiers.add(new J.Modifier(Tree.randomId(), modSpace, Markers.EMPTY,
            "lazy", J.Modifier.Type.LanguageExtension, Collections.emptyList()))
          lastModifierKeywordEnd = modifierEndPos + "lazy".length
          modifierEndPos += "lazy ".length
        } else if (remaining.startsWith("implicit ")) {
          modifiers.add(new J.Modifier(Tree.randomId(), modSpace, Markers.EMPTY,
            "implicit", J.Modifier.Type.LanguageExtension, Collections.emptyList()))
          lastModifierKeywordEnd = modifierEndPos + "implicit".length
          modifierEndPos += "implicit ".length
        } else if (remaining.startsWith("override ")) {
          modifiers.add(new J.Modifier(Tree.randomId(), modSpace, Markers.EMPTY,
            "override", J.Modifier.Type.LanguageExtension, Collections.emptyList()))
          lastModifierKeywordEnd = modifierEndPos + "override".length
          modifierEndPos += "override ".length
        } else if (remaining.startsWith("abstract ")) {
          modifiers.add(new J.Modifier(Tree.randomId(), modSpace, Markers.EMPTY,
            "abstract", J.Modifier.Type.Abstract, Collections.emptyList()))
          lastModifierKeywordEnd = modifierEndPos + "abstract".length
          modifierEndPos += "abstract ".length
        } else if (remaining.startsWith("sealed ")) {
          modifiers.add(new J.Modifier(Tree.randomId(), modSpace, Markers.EMPTY,
            "sealed", J.Modifier.Type.Sealed, Collections.emptyList()))
          lastModifierKeywordEnd = modifierEndPos + "sealed".length
          modifierEndPos += "sealed ".length
        } else {
          scanning = false
        }
      }
      
      // Now find val/var/given after modifiers
      val afterModifiers = sourceSnippet.substring(modifierEndPos)
      val valIndex = findKeyword(afterModifiers, "val")
      val varIndex = findKeyword(afterModifiers, "var")
      val givenIndex = findKeyword(afterModifiers, "given")

      val (keywordStart, keyword) = {
        val candidates = List(
          (valIndex, "val"), (varIndex, "var"), (givenIndex, "given")
        ).filter(_._1 >= 0).sortBy(_._1)
        if (candidates.nonEmpty) candidates.head else (-1, "")
      }
      
      if (keywordStart >= 0) {
        // Whitespace between the last modifier keyword (or start of snippet, if none) and val/var/given.
        val totalKeywordPos = modifierEndPos + keywordStart
        val gapStart = if (lastModifierKeywordEnd >= 0) lastModifierKeywordEnd else 0
        if (gapStart < totalKeywordPos) {
          beforeValVarRaw = sourceSnippet.substring(gapStart, totalKeywordPos)
          beforeValVar = Space.format(beforeValVarRaw)
        }
        
        // Move cursor past all modifiers and the keyword
        cursor = adjustedStart + modifierEndPos + keywordStart + keyword.length
        valVarKeyword = keyword
        
        // Extract space after val/var/given
        // Look for the variable name in the source
        val varNameStr = vd.name.toString
        val nameIndex = if (varNameStr.nonEmpty) positionOfNext(varNameStr, cursor) else -1
        if (nameIndex >= cursor && varNameStr.nonEmpty) {
          afterValVar = ScalaSpace.format(source, cursor, nameIndex)
          cursor = nameIndex
        } else if (keyword == "given") {
          // Anonymous given: `given Ordering[Int] = ...`.
          // No identifier in source; map to S.AnonymousGiven so no phantom name leaks
          // into a J type. cursor sits right after the `given` keyword.
          return buildAnonymousGiven(vd, prefix, modifiers, leadingAnnotations, beforeValVar)
        }
      }
    }
    
    // Val is implicitly final in Scala (but don't add it if we already have explicit final)
    val isGiven = valVarKeyword == "given"
    val isFinal = valVarKeyword == "val" || isGiven
    if (isFinal && !hasExplicitFinal) {
      // Add the implicit Final modifier. Whitespace before the keyword is carried in its prefix.
      // The `Given` marker on the J.VariableDeclarations itself signals the printer to emit
      // `given` instead of `val`.
      modifiers.add(new J.Modifier(
        Tree.randomId(),
        beforeValVar,
        Markers.EMPTY,
        null,
        J.Modifier.Type.Final,
        Collections.emptyList()
      ))
    }
    
    // Detect backtick-quoted name once, while cursor still sits at the raw name.
    // (After the type-annotation path runs, cursor moves past the type.)
    val rawValName = vd.name.toString
    val nameIsBacktickQuoted = isBacktickQuotedNameAt(cursor, rawValName.length)

    // Handle type annotation if present
    var typeExpression: TypeTree = null
    var beforeColon = Space.EMPTY
    var afterColon = Space.EMPTY

    if (vd.tpt != null && !vd.tpt.isEmpty && vd.tpt.span.exists) {
      // Find the end of the variable name in source — account for backtick-quoted names
      // (cursor sits at the raw name; the closing backtick, if any, sits one past the raw end).
      val rawNameLen = rawValName.length
      val nameEnd = cursor + rawNameLen + (if (nameIsBacktickQuoted) 1 else 0)
      val typeStart = Math.max(0, vd.tpt.span.start - offsetAdjustment)
      
      if (nameEnd < typeStart && typeStart <= source.length) {
        val between = source.substring(nameEnd, typeStart)
        val colonIndex = positionOfNextIn(between, ":", 0)
        if (colonIndex >= 0) {
          beforeColon = Space.format(between.substring(0, colonIndex))
          afterColon = Space.format(between.substring(colonIndex + 1))
          cursor = typeStart
        }
      }
      
      // Visit the type. Prefer visitTypeTree so type-position shapes that share an
      // untyped node with a value expression (tuple `(A, B)`, function `A => B`,
      // repeated `T*`) map to proper TypeTrees rather than a source-text identifier.
      val savedCursorType = cursor
      val visitedType = visitTypeTree(vd.tpt)
      typeExpression = if (visitedType != null) {
        // For type expressions, preserve the space after the colon
        visitedType.withPrefix(afterColon).asInstanceOf[TypeTree]
      } else {
        // Intersection types (A & B), union types (A | B), and other complex type
        // expressions — preserve source text as identifier
        cursor = savedCursorType
        val typeText = extractSource(vd.tpt.span)
        updateCursor(vd.tpt.span.end)
        ident(typeText, afterColon)
      }
    }
    
    // Check if this is a tuple pattern
    val isTuplePattern = {
      // Look ahead in source to see if we have a tuple pattern like (a, b)
      val lookAhead = source.substring(cursor, Math.min(cursor + 100, source.length))
      lookAhead.trim.startsWith("(") && vd.name.toString == "<pat>"
    }
    
    // Extract variable name or tuple pattern
    val varName: VariableDeclarator = if (isTuplePattern) {
      // This is a tuple pattern like (a, b)
      val tupleStart = positionOfNext("(", cursor)
      if (tupleStart >= 0) {
        val beforeTuple = ScalaSpace.format(source, cursor, tupleStart)
        cursor = tupleStart + 1 // Move past opening paren
        
        // Find the matching closing paren
        var parenCount = 1
        var tupleEnd = cursor
        while (parenCount > 0 && tupleEnd < source.length) {
          if (source.charAt(tupleEnd) == '(') parenCount += 1
          else if (source.charAt(tupleEnd) == ')') parenCount -= 1
          if (parenCount > 0) tupleEnd += 1
        }
        
        // Parse the elements inside the tuple
        val elements = new util.ArrayList[JRightPadded[Expression]]()
        val tupleContent = source.substring(cursor, tupleEnd)
        val parts = tupleContent.split(",")
        
        for (i <- parts.indices) {
          val part = parts(i).trim
          val beforePart = if (i == 0) Space.EMPTY else Space.SINGLE_SPACE
          val elem = ident(part, beforePart)
          val isLast = i == parts.length - 1
          val rightPadding = if (isLast) Space.EMPTY else Space.EMPTY
          elements.add(JRightPadded.build(elem.asInstanceOf[Expression]).withAfter(rightPadding))
        }
        
        cursor = tupleEnd + 1 // Move past closing paren
        
        S.TuplePattern.build(
          Tree.randomId(),
          beforeTuple,
          Markers.EMPTY,
          JContainer.build(Space.EMPTY, elements, Markers.EMPTY),
          null
        )
      } else {
        // Fallback to regular identifier if we can't parse the tuple
        ident(vd.name.toString, afterValVar)
      }
    } else {
      // Preserve backtick-quoted form when source uses it — vd.name.toString strips the backticks.
      // The opening backtick (if any) was already consumed into afterValVar by the index-based
      // search above. Use the flag captured before the type-annotation path moved the cursor.
      if (nameIsBacktickQuoted) {
        // Move the opening backtick out of afterValVar (back) and onto the identifier name itself.
        val afterValVarStr = afterValVar.getWhitespace
        val newAfterValVar = if (afterValVarStr.endsWith("`")) Space.format(afterValVarStr.dropRight(1)) else afterValVar
        ident("`" + rawValName + "`", newAfterValVar, typeOfTree(vd), variableTypeOfTree(vd))
      } else {
        ident(rawValName, afterValVar, typeOfTree(vd), variableTypeOfTree(vd))
      }
    }

    // Update cursor past the name only if we haven't parsed a type and it's not a tuple pattern
    // If we parsed a type or a tuple pattern, the cursor is already past them
    if (typeExpression == null && !isTuplePattern) {
      // Backtick-quoted name has a trailing backtick to skip past.
      val extra = if (nameIsBacktickQuoted) 1 else 0
      cursor = cursor + rawValName.length + extra
    }
    
    // Handle initializer
    var beforeEquals = Space.EMPTY
    var initializer: Expression = null
    
    
    if (vd.rhs != null && !vd.rhs.isEmpty) {
    }
    
    // Check for the special underscore initializer first
    if (vd.rhs != null && vd.rhs.toString == "Ident(_)") {
      // Handle uninitialized var: var x: Int = _
      // Look for the underscore in source
      val underscoreIndex = positionOfNext("_", cursor)
      if (underscoreIndex >= 0) {
        val beforeUnderscore = source.substring(cursor, underscoreIndex)
        val equalsIndex = positionOfNextIn(beforeUnderscore, "=", 0)
        if (equalsIndex >= 0) {
          beforeEquals = Space.format(beforeUnderscore.substring(0, equalsIndex))
          val afterEqualsStr = beforeUnderscore.substring(equalsIndex + 1)
          val afterEquals = Space.format(afterEqualsStr)
          cursor = underscoreIndex + 1
          
          // Create a wildcard for the underscore (Scala's default initializer)
          initializer = new S.Wildcard(
            Tree.randomId(),
            afterEquals,
            Markers.EMPTY,
            null
          )
        }
      }
    } else if (vd.rhs != null && !vd.rhs.isEmpty && vd.rhs.span.exists) {
      val rhsStart = effectiveStart(vd.rhs)

      // Look for equals sign
      if (cursor < rhsStart && rhsStart <= source.length) {
        val beforeRhs = source.substring(cursor, rhsStart)
        val equalsIndex = positionOfNextIn(beforeRhs, "=", 0)
        if (equalsIndex >= 0) {
          beforeEquals = Space.format(beforeRhs.substring(0, equalsIndex))
          val afterEqualsStr = beforeRhs.substring(equalsIndex + 1)
          cursor = rhsStart
          
          // Visit the initializer
          val rhsTree = visitTree(vd.rhs)
          
          rhsTree match {
            case block: J.Block =>
              // In Scala, blocks are expressions. Wrap in S.StatementExpression
              initializer = new S.StatementExpression(
                Tree.randomId(),
                block.withPrefix(Space.format(afterEqualsStr))
              )
              
            case expr: Expression =>
              initializer = expr.withPrefix(Space.format(afterEqualsStr))
              
            case stmt: Statement =>
              // Statements like if/else, match, try are expressions in Scala.
              // Wrap in S.StatementExpression to use as initializer.
              initializer = new S.StatementExpression(Tree.randomId(),
                stmt.withPrefix(Space.format(afterEqualsStr)))
          }
        }
      }
    }
    
    // Update cursor to end of ValDef
    updateCursor(vd.span.end)
    
    // Create variable declarator
    val namedVariable = new J.VariableDeclarations.NamedVariable(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      varName, // VariableDeclarator - J.Identifier implements this
      Collections.emptyList(), // dimensionsAfterName - not used in Scala
      if (initializer != null) JLeftPadded.build(initializer).withBefore(beforeEquals) else null,
      null // variableType - will be set later by type attribution
    )
    
    val declarator = JRightPadded.build(namedVariable)
    
    // Create the variable declarations
    // In Scala, we need to put the type expression in the overall declaration
    // even though it's syntactically attached to each variable
    // Use the varargs field (unused in Scala) to store space before colon
    val varargs: Space = if (beforeColon != Space.EMPTY) beforeColon else null
    val markerList = new util.ArrayList[org.openrewrite.marker.Marker]()
    if (beforeValVarRaw.nonEmpty) {
      markerList.add(ValVarKeyword(Tree.randomId(), beforeValVarRaw))
    }
    if (isGiven) {
      markerList.add(org.openrewrite.scala.marker.Given(Tree.randomId()))
    }
    val variableMarkers =
      if (markerList.isEmpty) Markers.EMPTY else Markers.build(markerList)

    new J.VariableDeclarations(
      Tree.randomId(),
      prefix,
      variableMarkers,
      leadingAnnotations,
      modifiers,
      typeExpression,
      varargs, // Space before colon (repurposed from Java varargs)
      Collections.emptyList(),
      Collections.singletonList(declarator)
    )
  }
  
  /**
   * Detect Scala 3 `given X with { ... }` (and the anonymous variant) lowered by
   * Dotty into a synthesized ClassDef/ModuleDef. The synthesized definitions don't
   * map cleanly to J.ClassDeclaration with the current modeling. Until form 3 has
   * proper structured handling, we preserve the original source text via
   * S.PatternDefinition. A `ModuleDef`/`ClassDef` with the `Given` flag is always a
   * synthesized given-with-body — no other construct produces that combination.
   */
  private def isGivenWithBody(t: Trees.Tree[?], mods: untpd.Modifiers): Boolean = {
    mods != null && mods.is(Flags.Given) && t.span.exists
  }

  private def preserveGivenWithBodyAsText(span: dotty.tools.dotc.util.Spans.Span): S.PatternDefinition = {
    val txtPrefix = extractPrefix(span)
    val txt = extractSource(span)
    updateCursor(span.end)
    new S.PatternDefinition(Tree.randomId(), txtPrefix, Markers.EMPTY, txt)
  }

/**
   * Build an S.AnonymousGiven for `given <Type> = <expr>` (no source-visible name).
   * Cursor must be positioned just past the `given` keyword.
   */
  private def buildAnonymousGiven(
      vd: Trees.ValDef[?],
      prefix: Space,
      modifiers: util.ArrayList[J.Modifier],
      leadingAnnotations: util.ArrayList[J.Annotation],
      beforeKeyword: Space
  ): S.AnonymousGiven = {
    // Cursor sits right after `given`. Visit the type next.
    val typeExpr: TypeTree = if (vd.tpt != null && !vd.tpt.isEmpty && vd.tpt.span.exists) {
      val typeStart = Math.max(0, vd.tpt.span.start - offsetAdjustment)
      val typePrefix = if (cursor < typeStart && typeStart <= source.length)
        ScalaSpace.format(source, cursor, typeStart)
      else Space.SINGLE_SPACE
      cursor = typeStart
      visitTree(vd.tpt) match {
        case tt: TypeTree => tt.withPrefix(typePrefix)
        case other =>
          // Fall back to source-text identifier — preserves printing fidelity but
          // intentionally drops rich typing for unmapped type expressions.
          val tText = extractSource(vd.tpt.span)
          updateCursor(vd.tpt.span.end)
          ident(tText, typePrefix).asInstanceOf[TypeTree]
      }
    } else {
      // Shouldn't happen for anonymous givens; default to an empty identifier
      ident("", Space.EMPTY).asInstanceOf[TypeTree]
    }

    // Initializer: look for `=` between cursor and rhs.span.start.
    var init: JLeftPadded[Expression] = null
    if (vd.rhs != null && !vd.rhs.isEmpty && vd.rhs.span.exists) {
      val rhsStart = Math.max(0, vd.rhs.span.start - offsetAdjustment)
      if (cursor < rhsStart && rhsStart <= source.length) {
        val between = source.substring(cursor, rhsStart)
        val equalsIndex = between.indexOf('=')
        if (equalsIndex >= 0) {
          val beforeEquals = Space.format(between.substring(0, equalsIndex))
          val afterEqualsStr = between.substring(equalsIndex + 1)
          cursor = rhsStart
          val rhsTree = visitTree(vd.rhs)
          var initExpr: Expression = null
          rhsTree match {
            case block: J.Block =>
              initExpr = new S.StatementExpression(Tree.randomId(),
                block.withPrefix(Space.format(afterEqualsStr)))
            case expr: Expression =>
              initExpr = expr.withPrefix(Space.format(afterEqualsStr)).asInstanceOf[Expression]
            case stmt: Statement =>
              initExpr = new S.StatementExpression(Tree.randomId(),
                stmt.withPrefix(Space.format(afterEqualsStr)))
            case _ =>
          }
          if (initExpr != null) {
            init = JLeftPadded.build(initExpr).withBefore(beforeEquals)
          }
        }
      }
    }
    updateCursor(vd.span.end)

    // Whitespace handling around the `given` keyword:
    // - No leading annotations/modifiers: `beforeKeyword` (gap between cursor and `given`)
    //   collapses with the declaration prefix.
    // - Otherwise: declaration prefix is unchanged, and `beforeKeyword` (the gap between
    //   the last modifier and `given`) is preserved via a ValVarKeyword marker so the
    //   printer can emit it before the keyword.
    val (effectivePrefix, gMarkers) =
      if (leadingAnnotations.isEmpty && modifiers.isEmpty) {
        val effPrefix = if (beforeKeyword.getWhitespace.nonEmpty) beforeKeyword else prefix
        (effPrefix, Markers.EMPTY)
      } else if (beforeKeyword.getWhitespace.nonEmpty) {
        (prefix, Markers.build(Collections.singletonList(
          ValVarKeyword(Tree.randomId(), beforeKeyword.getWhitespace))))
      } else {
        (prefix, Markers.EMPTY)
      }

    new S.AnonymousGiven(
      Tree.randomId(),
      effectivePrefix,
      gMarkers,
      leadingAnnotations,
      modifiers,
      typeExpr,
      init
    )
  }

  private def visitModuleDef(md: untpd.ModuleDef): J.ClassDeclaration = {
    val hasAnnotations = md.mods != null && md.mods.annotations.nonEmpty
    // When annotations are present they own the leading whitespace; otherwise
    // the prefix lives on the ModuleDef itself.
    val prefix = if (hasAnnotations) Space.EMPTY else extractPrefix(md.span)

    val leadingAnnotations = new util.ArrayList[J.Annotation]()
    if (hasAnnotations) {
      for (annot <- md.mods.annotations) {
        visitTree(annot) match {
          case ann: J.Annotation => leadingAnnotations.add(ann)
          case _ =>
        }
      }
    }

    // Extract the source text to find modifiers
    val adjustedStart = Math.max(0, md.span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, md.span.end - offsetAdjustment)
    var modifierText = ""
    var objectIndex = -1

    var isEnumCase = false
    // When annotations were consumed, cursor sits after them; modifierText must
    // be derived from the post-annotation position, not the ModuleDef span start.
    val modifierScanStart = if (hasAnnotations) cursor else adjustedStart
    if (modifierScanStart >= cursor && adjustedEnd <= source.length) {
      val sourceSnippet = source.substring(cursor, adjustedEnd)
      objectIndex = findKeyword(sourceSnippet, "object")
      val caseIndex = findKeyword(sourceSnippet, "case")
      // Enum cases have `case` but NO `object` keyword: `case Red extends Color`
      // Case objects have both: `case object Empty`
      if (caseIndex >= 0 && objectIndex < 0) {
        isEnumCase = true
        objectIndex = caseIndex
      }
      if (objectIndex > 0) {
        modifierText = sourceSnippet.substring(0, objectIndex)
      }
    }
    
    // Extract modifiers from text
    val (modifiers, extractedLastModEnd) = extractModifiersFromText(md.mods, modifierText)
    var lastModEnd = extractedLastModEnd

    // `package object X` — the `package` keyword is not a Scala modifier, but for LST
    // fidelity we emit it as a leading modifier and attach a PackageObject marker on the
    // class declaration. Must come before `case` so ordering stays source-faithful.
    val isPackageObject = md.mods.is(Flags.Package) && modifierText.contains("package")
    if (isPackageObject) {
      val packageIndex = findKeyword(modifierText, "package")
      if (packageIndex >= 0) {
        val packageSpace = if (packageIndex > lastModEnd) {
          Space.format(modifierText.substring(lastModEnd, packageIndex))
        } else {
          Space.EMPTY
        }
        modifiers.add(new J.Modifier(
          Tree.randomId(),
          packageSpace,
          Markers.EMPTY,
          "package",
          J.Modifier.Type.LanguageExtension,
          Collections.emptyList()
        ))
        lastModEnd = packageIndex + "package".length
      }
    }

    // Check for case modifier on object definitions (e.g., `case object Foo`).
    // Skip if this is an enum case — `case` is the keyword, not a modifier.
    if (!isEnumCase && modifierText.contains("case")) {
      val caseIndex = positionOfNextIn(modifierText, "case", 0)
      if (caseIndex >= 0) {
        val caseSpace = if (caseIndex > lastModEnd) {
          Space.format(modifierText.substring(lastModEnd, caseIndex))
        } else {
          Space.EMPTY
        }
        modifiers.add(new J.Modifier(
          Tree.randomId(),
          caseSpace,
          Markers.EMPTY,
          "case",
          J.Modifier.Type.LanguageExtension,
          Collections.emptyList()
        ))
      }
    }
    
    // Objects are implicitly final
    modifiers.add(new J.Modifier(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      null, // No keyword for implicit final
      J.Modifier.Type.Final,
      Collections.emptyList()
    ).withMarkers(Markers.build(Collections.singletonList(new Implicit(Tree.randomId())))))
    
    // Find where "object"/"case" keyword ends
    val keywordLen = if (isEnumCase) "case".length else "object".length
    val objectKeywordPos = if (objectIndex >= 0) {
      cursor + objectIndex + keywordLen
    } else {
      cursor
    }
    
    // Extract space between modifiers and "object" keyword
    val kindPrefix = if (!modifiers.isEmpty && objectIndex > 0) {
      val afterModifiers = if (modifierText.contains("case")) {
        positionOfNextIn(modifierText, "case", 0) + "case".length
      } else {
        lastModEnd
      }
      Space.format(modifierText.substring(afterModifiers, objectIndex))
    } else {
      Space.EMPTY
    }
    
    // Update cursor to after "object" keyword
    cursor = objectKeywordPos
    
    // Create the class kind
    val kindType = if (isEnumCase) J.ClassDeclaration.Kind.Type.Enum else J.ClassDeclaration.Kind.Type.Class
    val kind = new J.ClassDeclaration.Kind(
      Tree.randomId(),
      kindPrefix,
      Markers.EMPTY,
      Collections.emptyList(),
      kindType
    )
    
    // Extract space between "object" and the name, accounting for backtick-quoted names.
    val (nameSpace, displayName, nameEndCursor) = if (md.nameSpan.exists) {
      val rawNameStart = Math.max(0, md.nameSpan.start - offsetAdjustment)
      val rawNameLen = Math.max(0, md.nameSpan.end - md.nameSpan.start)
      backtickAwareName(objectKeywordPos, rawNameStart, rawNameLen, md.name.toString)
    } else {
      (Space.format(" "), md.name.toString, cursor)
    }

    // Extract object name
    val name = ident(displayName, nameSpace)

    // Update cursor to after the name (past closing backtick if present)
    if (md.nameSpan.exists) {
      cursor = nameEndCursor
    }
    
    // Objects cannot have type parameters
    val typeParameters: JContainer[J.TypeParameter] = null
    
    // Objects cannot have constructor parameters  
    val primaryConstructor: JContainer[Statement] = null
    
    // Extract extends/with clauses from the implementation template
    var extendings: JLeftPadded[TypeTree] = null
    var implementings: JContainer[TypeTree] = null
    
    md.impl match {
      case tmpl: Trees.Template[?] if tmpl.parents.nonEmpty =>
        // Handle extends/with clauses similar to classes
        // Look for "extends" keyword and extract space before it
        var extendsSpace = Space.format(" ")
        if (cursor < source.length && tmpl.parents.head.span.exists) {
          val parentStart = Math.max(0, tmpl.parents.head.span.start - offsetAdjustment)
          if (cursor < parentStart && parentStart <= source.length) {
            val beforeParent = source.substring(cursor, parentStart)
            val extendsIdx = positionOfNextIn(beforeParent, "extends", 0)
            if (extendsIdx >= 0) {
              // Space is only the whitespace before "extends"
              extendsSpace = Space.format(beforeParent.substring(0, extendsIdx))
              // Update cursor to after "extends" keyword
              cursor = cursor + extendsIdx + "extends".length
            } else {
              // No "extends" found, use full space
              extendsSpace = Space.format(beforeParent)
              cursor = parentStart
            }
          }
        }
        
        // Now visit the parent with cursor positioned correctly
        val firstParent = tmpl.parents.head
        val savedCursorExtends = cursor
        val extendsType = visitTree(firstParent) match {
          case typeTree: TypeTree => typeTree
          case id: J.Identifier => id.asInstanceOf[TypeTree]
          case other =>
            updateCursor(firstParent.span.end)
            ident(extractSource(firstParent.span), other.getPrefix).asInstanceOf[TypeTree]
        }

        extendings = new JLeftPadded(extendsSpace, extendsType, Markers.EMPTY)

        // Handle additional parents as implements (with clauses)
        if (tmpl.parents.size > 1) {
          val implementsList = new util.ArrayList[JRightPadded[TypeTree]]()
          
          // Find space before first "with"
          var containerSpace = Space.format(" ")
          if (cursor < source.length && tmpl.parents(1).span.exists) {
            val firstWithParentStart = Math.max(0, tmpl.parents(1).span.start - offsetAdjustment)
            if (cursor < firstWithParentStart) {
              val beforeFirstWith = source.substring(cursor, firstWithParentStart)
              val withIdx = positionOfNextIn(beforeFirstWith, "with", 0)
              if (withIdx >= 0) {
                containerSpace = Space.format(beforeFirstWith.substring(0, withIdx))
                cursor = cursor + withIdx + "with".length
              }
            }
          }
          
          for (i <- 1 until tmpl.parents.size) {
            val parent = tmpl.parents(i)
            val implType = visitTree(parent) match {
              case typeTree: TypeTree => typeTree
              case id: J.Identifier => id.asInstanceOf[TypeTree]
              case other =>
                updateCursor(parent.span.end)
                ident(extractSource(parent.span), other.getPrefix).asInstanceOf[TypeTree]
            }
            
            // For subsequent traits, extract space between them
            var trailingSpace = Space.EMPTY
            if (i < tmpl.parents.size - 1 && parent.span.exists && tmpl.parents(i + 1).span.exists) {
              val thisEnd = Math.max(0, parent.span.end - offsetAdjustment)
              val nextStart = Math.max(0, tmpl.parents(i + 1).span.start - offsetAdjustment)
              if (thisEnd < nextStart && nextStart <= source.length) {
                val between = source.substring(thisEnd, nextStart)
                val withIdx = positionOfNextIn(between, "with", 0)
                if (withIdx >= 0) {
                  trailingSpace = Space.format(between.substring(0, withIdx))
                  // Update cursor past "with"
                  cursor = thisEnd + withIdx + "with".length
                } else {
                  trailingSpace = Space.format(between)
                }
              }
            }
            
            implementsList.add(new JRightPadded(implType, trailingSpace, Markers.EMPTY))
          }
          implementings = JContainer.build(containerSpace, implementsList, Markers.EMPTY)
        }
        
      case _ =>
    }
    
    // Extract body
    val body = md.impl match {
      case tmpl: Trees.Template[?] if tmpl.body.nonEmpty =>
        // Find the body delimiter: `{` (braced) or `:` (Scala 3 braceless)
        var bodyPrefix = Space.EMPTY
        var isBraceless = false
        if (cursor < source.length && md.span.exists) {
          val remaining = source.substring(cursor, Math.min(md.span.end - offsetAdjustment, source.length))
          val braceIdx = positionOfNextIn(remaining, "{", 0)
          val colonIdx = positionOfNextIn(remaining, ":", 0)
          if (braceIdx >= 0 && (colonIdx < 0 || braceIdx < colonIdx)) {
            // Braced body: object Foo { ... }
            bodyPrefix = Space.format(remaining.substring(0, braceIdx))
            cursor = cursor + braceIdx + 1
          } else if (colonIdx >= 0) {
            // Braceless body: object Foo: ...
            isBraceless = true
            bodyPrefix = Space.format(remaining.substring(0, colonIdx))
            cursor = cursor + colonIdx + 1
          }
        }

        // Create a block from the template body
        val statements = new util.ArrayList[JRightPadded[Statement]]()
        val visitedSpans = new java.util.HashSet[Int]()
        // Sort by source position to preserve source order (Dotty may reorder imports)
        val sortedBody = tmpl.body.sortBy(s => if (s.span.exists) s.span.start else Int.MaxValue)
        sortedBody.foreach { stat =>
          val isSynth = stat.span.isSynthetic || {
            def containsTripleQuestion(t: Trees.Tree[?]): Boolean = t match {
              case sel: Trees.Select[?] => sel.name.toString == "$qmark$qmark$qmark" || sel.name.toString == "???" || containsTripleQuestion(sel.qualifier)
              case id: Trees.Ident[?] => id.name.toString == "$qmark$qmark$qmark" || id.name.toString == "???"
              case app: Trees.Apply[?] => containsTripleQuestion(app.fun)
              case ta: Trees.TypeApply[?] => containsTripleQuestion(ta.fun)
              case _ => false
            }
            containsTripleQuestion(stat)
          }
          if (stat.span.exists && !isSynth && visitedSpans.add(stat.span.start)) {
            visitTree(stat) match {
              case stmt: Statement => statements.add(JRightPadded.build(stmt))
              case expr: Expression =>
                statements.add(JRightPadded.build(new S.ExpressionStatement(Tree.randomId(), expr)))
              case _ =>
            }
          }
        }

        // Find end space
        var endSpace = Space.EMPTY
        if (cursor < source.length && md.span.exists) {
          val endPos = Math.max(0, md.span.end - offsetAdjustment)
          if (isBraceless) {
            // No closing brace for braceless syntax
            if (cursor < endPos) {
              endSpace = ScalaSpace.format(source, cursor, Math.min(endPos, source.length))
            }
            cursor = endPos
          } else {
            val remaining = source.substring(cursor, Math.min(endPos, source.length))
            val closeBraceIdx = remaining.lastIndexOf('}')
            if (closeBraceIdx >= 0) {
              endSpace = Space.format(remaining.substring(0, closeBraceIdx))
              cursor = endPos
            }
          }
        }
        
        val blockMarkers = if (isBraceless) {
          Markers.build(Collections.singletonList(new IndentedSyntax(Tree.randomId())))
        } else Markers.EMPTY

        new J.Block(
          Tree.randomId(),
          bodyPrefix,
          blockMarkers,
          JRightPadded.build(false),
          statements,
          endSpace
        )

      case _ =>
        // Empty body - object without braces
        new J.Block(
          Tree.randomId(),
          Space.EMPTY,
          Markers.EMPTY,
          JRightPadded.build(false),
          Collections.emptyList(),
          Space.EMPTY
        ).withMarkers(Markers.build(Collections.singletonList(new OmitBraces(Tree.randomId()))))
    }
    
    // Update cursor to end of module def
    if (md.span.exists) {
      cursor = Math.max(cursor, md.span.end - offsetAdjustment)
    }
    
    // Create the class declaration with SObject marker (and PackageObject for `package object`)
    val objectMarkers = if (isPackageObject) {
      Markers.build(Arrays.asList(SObject.create(), PackageObject(Tree.randomId())))
    } else {
      Markers.build(Collections.singletonList(SObject.create()))
    }
    new J.ClassDeclaration(
      Tree.randomId(),
      prefix,
      objectMarkers,
      leadingAnnotations,
      modifiers,
      kind,
      name,
      typeParameters,
      primaryConstructor,
      extendings,
      implementings,
      null, // permits
      body,
      TypeUtils.asFullyQualified(typeOfTree(md))
    )
  }

  private def visitAssign(asg: Trees.Assign[?]): J = {
    val prefix = extractPrefix(asg.span)
    
    // Visit the left-hand side. LHS is typically an Identifier/Select but be tolerant
    // of unusual J types by wrapping via StatementExpression.
    val variable = visitTree(asg.lhs) match {
      case expr: Expression => expr
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(asg)
    }

    // Find the position of the equals sign
    val lhsEnd = Math.max(cursor, Math.max(0, asg.lhs.span.end - offsetAdjustment))
    val rhsStart = effectiveStart(asg.rhs)
    var equalsSpace = Space.EMPTY
    var valueSpace = Space.EMPTY
    var isCompoundAssignment = false
    var compoundOperator: J.AssignmentOperation.Type = null
    
    if (lhsEnd < rhsStart && lhsEnd >= cursor && rhsStart <= source.length) {
      val between = source.substring(lhsEnd, rhsStart)
      
      // Check for compound assignment operators
      val compoundPattern = """(\s*)([\+\-\*/%&\|\^]|<<|>>|>>>)=(\s*)""".r
      compoundPattern.findFirstMatchIn(between) match {
        case Some(m) =>
          isCompoundAssignment = true
          equalsSpace = Space.format(m.group(1))
          valueSpace = Space.format(m.group(3))
          compoundOperator = m.group(2) match {
            case "+" => J.AssignmentOperation.Type.Addition
            case "-" => J.AssignmentOperation.Type.Subtraction
            case "*" => J.AssignmentOperation.Type.Multiplication
            case "/" => J.AssignmentOperation.Type.Division
            case "%" => J.AssignmentOperation.Type.Modulo
            case "&" => J.AssignmentOperation.Type.BitAnd
            case "|" => J.AssignmentOperation.Type.BitOr
            case "^" => J.AssignmentOperation.Type.BitXor
            case "<<" => J.AssignmentOperation.Type.LeftShift
            case ">>" => J.AssignmentOperation.Type.RightShift
            case ">>>" => J.AssignmentOperation.Type.UnsignedRightShift
            case _ => J.AssignmentOperation.Type.Addition // fallback
          }
          cursor = rhsStart
        case None =>
          // Regular assignment
          val equalsIndex = positionOfNextIn(between, "=", 0)
          if (equalsIndex >= 0) {
            equalsSpace = Space.format(between.substring(0, equalsIndex))
            val afterEquals = equalsIndex + 1
            if (afterEquals < between.length) {
              valueSpace = Space.format(between.substring(afterEquals))
            }
            cursor = rhsStart
          }
      }
    }
    
    // Visit the right-hand side (value). Scala allows block/if/match on RHS:
    // `x = if (cond) 1 else 2` — wrap Statement results via StatementExpression.
    val value = visitTree(asg.rhs) match {
      case expr: Expression => expr
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(asg)
    }
    
    // Update cursor to the end of the assignment
    updateCursor(asg.span.end)
    
    if (isCompoundAssignment) {
      // Check if rhs is a binary operation with lhs as the left operand
      // Scala desugars x += 5 to x = x + 5
      val assignment = asg.rhs match {
        case app: Trees.Apply[?] =>
          app.fun match {
            case sel: Trees.Select[?] if sel.qualifier == asg.lhs =>
              // This is the desugared form, extract just the right operand
              visitTree(app.args.head) match {
                case expr: Expression => expr
                case _ => value
              }
            case _ => value
          }
        case _ => value
      }
      
      new J.AssignmentOperation(
        Tree.randomId(),
        prefix,
        Markers.EMPTY,
        variable,
        JLeftPadded.build(compoundOperator).withBefore(equalsSpace),
        assignment.withPrefix(valueSpace),
        typeOfTree(asg)
      )
    } else {
      new J.Assignment(
        Tree.randomId(),
        prefix,
        Markers.EMPTY,
        variable,
        JLeftPadded.build(value.withPrefix(valueSpace)).withBefore(equalsSpace),
        typeOfTree(asg)
      )
    }
  }

  private def visitIf(ifTree: Trees.If[?]): J = {
    val prefix = extractPrefix(ifTree.span)


    // Find where the condition parentheses start
    val adjustedStart = Math.max(0, ifTree.span.start - offsetAdjustment)
    val condStart = Math.max(0, ifTree.cond.span.start - offsetAdjustment)

    // Detect Scala 3 `if cond then ...` (no parens). The condition is not wrapped in untpd.Parens,
    // and there is no `(` between `if` and the condition's start position.
    val ifIsParenless = !ifTree.cond.isInstanceOf[untpd.Parens]

    // Extract space before parentheses and move cursor past "if" to the condition
    var beforeParenSpace = Space.EMPTY
    if (adjustedStart < condStart && cursor <= condStart) {
      // Look for the opening parenthesis after "if"
      val searchEnd = Math.min(condStart + 1, source.length) // Include the '(' character
      val between = source.substring(cursor, searchEnd)
      val ifIndex = positionOfNextIn(between, "if", 0)
      if (ifIndex >= 0) {
        val afterIf = ifIndex + 2
        if (ifIsParenless) {
          // Move cursor just past the `if` keyword; the condition's prefix will absorb the space.
          cursor = cursor + afterIf
        } else {
          val remainingStr = between.substring(afterIf)
          val parenIndex = positionOfNextIn(remainingStr, "(", 0)
          if (parenIndex >= 0) {
            beforeParenSpace = Space.format(remainingStr.substring(0, parenIndex))
            // Move cursor to the opening parenthesis
            cursor = cursor + afterIf + parenIndex
          }
        }
      }
    }
    
    // For if conditions, we need to handle Parens specially - extract the inner expression
    val conditionExpr = ifTree.cond match {
      case parens: untpd.Parens =>
        // Skip the opening parenthesis since ControlParentheses will add it
        cursor = cursor + 1
        // Get the inner expression from Parens
        val innerTree = try {
          // Try different possible field names
          val treeField = parens.getClass.getDeclaredFields.find(f => 
            f.getName.contains("tree") || f.getName.contains("expr") || f.getName.contains("arg")
          )
          
          treeField match {
            case Some(field) =>
              field.setAccessible(true)
              field.get(parens).asInstanceOf[Trees.Tree[?]]
            case None =>
              // Fall back to productElement approach
              if (parens.productArity > 0) {
                parens.productElement(0).asInstanceOf[Trees.Tree[?]]
              } else {
                parens
              }
          }
        } catch {
          case _: Exception => parens
        }
        innerTree
      case other => other
    }
    
    // Visit the condition expression
    val condition = visitTree(conditionExpr) match {
      case expr: Expression => expr
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(ifTree)
    }
    
    // Extract space after condition
    var afterCondSpace = Space.EMPTY
    ifTree.cond match {
      case parens: untpd.Parens =>
        // For Parens, we need to extract the space before the closing paren
        val innerEnd = conditionExpr.span.end
        val parenEnd = parens.span.end
        if (innerEnd < parenEnd - 1) {
          val adjustedInnerEnd = Math.max(0, innerEnd - offsetAdjustment)
          val adjustedParenEnd = Math.max(0, parenEnd - 1 - offsetAdjustment)
          if (adjustedInnerEnd < adjustedParenEnd && adjustedInnerEnd >= cursor && adjustedParenEnd <= source.length) {
            afterCondSpace = ScalaSpace.format(source, adjustedInnerEnd, adjustedParenEnd)
            cursor = adjustedParenEnd + 1 // Skip the closing paren
          } else {
            cursor = Math.max(0, parenEnd - offsetAdjustment)
          }
        } else {
          cursor = Math.max(0, parenEnd - offsetAdjustment)
        }
      case _ =>
        // For non-parenthesized conditions, just move cursor to end
        cursor = Math.max(0, ifTree.cond.span.end - offsetAdjustment)
    }

    // For Scala 3 `if cond then ...`, advance the cursor past the `then` keyword
    // (which sits between the condition and the then-branch). The space before `then`
    // is absorbed into afterCondSpace; space after is the thenp's prefix.
    if (ifIsParenless) {
      val thenpStart = Math.max(0, ifTree.thenp.span.start - offsetAdjustment)
      if (cursor < thenpStart && thenpStart <= source.length) {
        val between = source.substring(cursor, thenpStart)
        val thenIdx = positionOfNextIn(between, "then", 0)
        if (thenIdx >= 0) {
          afterCondSpace = Space.format(between.substring(0, thenIdx))
          cursor = cursor + thenIdx + 4
        }
      }
    }

    // Visit the then branch — in Scala, any expression is valid as then-branch
    val thenPart = visitTree(ifTree.thenp) match {
      case stmt: Statement => JRightPadded.build(stmt)
      case j: J =>
        // Wrap any non-Statement J (like Identifier, Literal) in StatementExpression
        JRightPadded.build(new S.StatementExpression(Tree.randomId(), j).asInstanceOf[Statement])
      case _ => return visitUnknown(ifTree)
    }
    
    // Handle optional else branch
    val elsePart = if (ifTree.elsep.isEmpty) {
      null
    } else {
      // Extract space before "else"
      val thenEnd = Math.max(0, ifTree.thenp.span.end - offsetAdjustment)
      val elseStart = Math.max(0, ifTree.elsep.span.start - offsetAdjustment)
      var elsePrefix = Space.EMPTY
      if (thenEnd < elseStart && cursor <= thenEnd) {
        val between = source.substring(thenEnd, elseStart)
        val elseIndex = positionOfNextIn(between, "else", 0)
        if (elseIndex >= 0) {
          elsePrefix = Space.format(between.substring(0, elseIndex))
          cursor = thenEnd + elseIndex + 4 // "else" is 4 chars
        }
      }
      
      visitTree(ifTree.elsep) match {
        case stmt: Statement =>
          new J.If.Else(
            Tree.randomId(),
            elsePrefix,
            Markers.EMPTY,
            JRightPadded.build(stmt)
          )
        case j: J =>
          // Wrap any non-Statement J (like Identifier, Literal) in StatementExpression —
          // mirrors the thenp branch above so `if (x) a else b` with expression operands parses.
          new J.If.Else(
            Tree.randomId(),
            elsePrefix,
            Markers.EMPTY,
            JRightPadded.build(new S.StatementExpression(Tree.randomId(), j).asInstanceOf[Statement])
          )
        case _ => return visitUnknown(ifTree)
      }
    }
    
    // Update cursor to end of the if expression
    updateCursor(ifTree.span.end)

    val ifMarkers = if (ifIsParenless)
      Markers.build(Collections.singletonList(new IndentedSyntax(Tree.randomId())))
    else Markers.EMPTY

    new J.If(
      Tree.randomId(),
      prefix,
      ifMarkers,
      new J.ControlParentheses(
        Tree.randomId(),
        beforeParenSpace,
        Markers.EMPTY,
        JRightPadded.build(condition).withAfter(afterCondSpace)
      ),
      thenPart,
      elsePart
    )
  }
  
  private def visitWhileDo(whileTree: Trees.WhileDo[?]): J = {
    val prefix = extractPrefix(whileTree.span)

    // Find where the condition parentheses start
    val adjustedStart = Math.max(0, whileTree.span.start - offsetAdjustment)
    val condStart = Math.max(0, whileTree.cond.span.start - offsetAdjustment)

    // Detect Scala 3 `while cond do ...` (no parens). Same shape as `if cond then`.
    val whileIsParenless = !whileTree.cond.isInstanceOf[untpd.Parens]

    // Extract space before parentheses and move cursor past "while" to the condition
    var beforeParenSpace = Space.EMPTY
    if (adjustedStart < condStart && cursor <= condStart) {
      val searchEnd = Math.min(condStart + 1, source.length) // Include the '(' character
      val between = source.substring(cursor, searchEnd)
      val whileIndex = positionOfNextIn(between, "while", 0)
      if (whileIndex >= 0) {
        val afterWhile = whileIndex + 5 // "while" is 5 chars
        if (whileIsParenless) {
          cursor = cursor + afterWhile
        } else {
          val remainingStr = between.substring(afterWhile)
          val parenIndex = positionOfNextIn(remainingStr, "(", 0)
          if (parenIndex >= 0) {
            beforeParenSpace = Space.format(remainingStr.substring(0, parenIndex))
            // Move cursor to the opening parenthesis
            cursor = cursor + afterWhile + parenIndex
          }
        }
      }
    }
    
    // For while conditions, we need to handle Parens specially - extract the inner expression
    val conditionExpr = whileTree.cond match {
      case parens: untpd.Parens =>
        // Skip the opening parenthesis since ControlParentheses will add it
        cursor = cursor + 1
        // Get the inner expression from Parens
        val innerTree = try {
          val treeField = parens.getClass.getDeclaredFields.find(f => 
            f.getName.contains("tree") || f.getName.contains("expr") || f.getName.contains("arg")
          )
          
          treeField match {
            case Some(field) =>
              field.setAccessible(true)
              field.get(parens).asInstanceOf[Trees.Tree[?]]
            case None =>
              if (parens.productArity > 0) {
                parens.productElement(0).asInstanceOf[Trees.Tree[?]]
              } else {
                parens
              }
          }
        } catch {
          case _: Exception => parens
        }
        innerTree
      case other => other
    }
    
    // Visit the condition expression
    val condition = visitTree(conditionExpr) match {
      case expr: Expression => expr
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(whileTree)
    }
    
    // Extract space after condition
    var afterCondSpace = Space.EMPTY
    whileTree.cond match {
      case parens: untpd.Parens =>
        val innerEnd = conditionExpr.span.end
        val parenEnd = parens.span.end
        if (innerEnd < parenEnd - 1) {
          val adjustedInnerEnd = Math.max(0, innerEnd - offsetAdjustment)
          val adjustedParenEnd = Math.max(0, parenEnd - 1 - offsetAdjustment)
          if (adjustedInnerEnd < adjustedParenEnd && adjustedInnerEnd >= cursor && adjustedParenEnd <= source.length) {
            afterCondSpace = ScalaSpace.format(source, adjustedInnerEnd, adjustedParenEnd)
            cursor = adjustedParenEnd + 1 // Skip the closing paren
          } else {
            cursor = Math.max(0, parenEnd - offsetAdjustment)
          }
        } else {
          cursor = Math.max(0, parenEnd - offsetAdjustment)
        }
      case _ =>
        cursor = Math.max(0, whileTree.cond.span.end - offsetAdjustment)
    }

    // Scala 3 `while cond do ...` — advance the cursor past the `do` keyword.
    if (whileIsParenless) {
      val bodyStart = Math.max(0, whileTree.body.span.start - offsetAdjustment)
      if (cursor < bodyStart && bodyStart <= source.length) {
        val between = source.substring(cursor, bodyStart)
        val doIdx = positionOfNextIn(between, "do", 0)
        if (doIdx >= 0) {
          afterCondSpace = Space.format(between.substring(0, doIdx))
          cursor = cursor + doIdx + 2
        }
      }
    }

    // Visit the body — wrap non-Statement expressions (e.g. `while (cond) ()`)
    val body = visitTree(whileTree.body) match {
      case stmt: Statement => JRightPadded.build(stmt)
      case j: J => JRightPadded.build(new S.StatementExpression(Tree.randomId(), j).asInstanceOf[Statement])
      case _ => return visitUnknown(whileTree)
    }

    // Update cursor to end of the while loop
    updateCursor(whileTree.span.end)

    val whileMarkers = if (whileIsParenless)
      Markers.build(Collections.singletonList(new IndentedSyntax(Tree.randomId())))
    else Markers.EMPTY

    new J.WhileLoop(
      Tree.randomId(),
      prefix,
      whileMarkers,
      new J.ControlParentheses(
        Tree.randomId(),
        beforeParenSpace,
        Markers.EMPTY,
        JRightPadded.build(condition).withAfter(afterCondSpace)
      ),
      body
    )
  }
  
  private def visitForDo(forTree: untpd.ForDo): J = {
    // Detect Scala 3 paren-less form (`for x <- xs do body`): after `for`, the next
    // non-whitespace is neither `(` nor `{`. The single-generator J.ForEachLoop
    // shortcut assumes parens, so for paren-less route through buildSFor.
    val isParenless = {
      val forStart = Math.max(0, forTree.span.start - offsetAdjustment)
      var i = forStart + 3 // skip "for"
      while (i < source.length && source.charAt(i).isWhitespace) i += 1
      i < source.length && source.charAt(i) != '(' && source.charAt(i) != '{'
    }
    val enums = forTree.enums
    if (!isParenless && enums.size == 1) {
      enums.head match {
        case genFrom: untpd.GenFrom if genFrom.pat.isInstanceOf[Trees.Ident[?]] =>
          return visitSimpleForEach(forTree, genFrom)
        case _ =>
      }
    }
    val prefix = extractPrefix(forTree.span)
    val forIdx = positionOfNext("for", cursor)
    if (forIdx >= cursor) cursor = forIdx + 3
    val endPos = Math.max(0, forTree.span.end - offsetAdjustment)
    import scala.jdk.CollectionConverters.*
    buildSFor(prefix, forTree.enums.asInstanceOf[List[Trees.Tree[?]]], forTree.body, yielding = false, endPos)
  }

  private def visitSimpleForEach(forTree: untpd.ForDo, genFrom: untpd.GenFrom): J = {
    val prefix = extractPrefix(forTree.span)

    // Find the opening delimiter '(' or '{' after "for" keyword
    val forAdjustedStart = Math.max(0, forTree.span.start - offsetAdjustment)
    val controlPrefix = {
      val searchStart = cursor
      val searchEnd = Math.min(source.length, forAdjustedStart + 20) // "for" + some space + delimiter
      if (searchStart < searchEnd) {
        val between = source.substring(searchStart, searchEnd)
        val forIdx = positionOfNextIn(between, "for", 0)
        if (forIdx >= 0) {
          val afterFor = searchStart + forIdx + 3
          // Find '(' or '{' after "for"
          var delimIdx = afterFor
          while (delimIdx < source.length && source.charAt(delimIdx) != '(' && source.charAt(delimIdx) != '{') {
            delimIdx += 1
          }
          val spaceBeforeDelim = if (delimIdx > afterFor) ScalaSpace.format(source, afterFor, delimIdx) else Space.EMPTY
          if (delimIdx < source.length) {
            cursor = delimIdx + 1 // Skip past the opening delimiter
          }
          spaceBeforeDelim
        } else {
          Space.EMPTY
        }
      } else {
        Space.EMPTY
      }
    }

    // Extract the variable name from the pattern
    val varName = genFrom.pat match {
      case ident: Trees.Ident[?] => ident.name.toString
      case _ =>
        // Complex patterns not yet supported
        return visitUnknown(forTree)
    }

    // Extract prefix space before the variable name
    val varPrefix = {
      val patStart = Math.max(0, genFrom.pat.span.start - offsetAdjustment)
      if (patStart > cursor && patStart <= source.length) {
        val ws = source.substring(cursor, patStart)
        cursor = patStart
        Space.format(ws)
      } else {
        Space.EMPTY
      }
    }

    // Advance cursor past the variable name
    updateCursor(genFrom.pat.span.end)

    // Find "<-" in source between variable and iterable, extract after-variable space
    val arrowSpace = {
      val exprStart = Math.max(0, genFrom.expr.span.start - offsetAdjustment)
      val searchRegion = if (exprStart > cursor) source.substring(cursor, exprStart) else ""
      val arrowIdx = positionOfNextIn(searchRegion, "<-", 0)
      if (arrowIdx >= 0) {
        val spaceBeforeArrow = searchRegion.substring(0, arrowIdx)
        cursor = cursor + arrowIdx + 2 // Skip past "<-"
        Space.format(spaceBeforeArrow)
      } else {
        Space.EMPTY
      }
    }

    // Build the variable declaration (no type, since Scala infers it)
    val varDecl = new J.VariableDeclarations(
      Tree.randomId(),
      varPrefix,
      Markers.EMPTY,
      Collections.emptyList(),
      Collections.emptyList(),
      null, // No type expression (Scala infers)
      null, // No varargs
      Collections.singletonList(
        JRightPadded.build(
          new J.VariableDeclarations.NamedVariable(
            Tree.randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            ident(varName, Space.EMPTY, typeFor(genFrom.pat.span)),
            Collections.emptyList(),
            null,
            null
          )
        )
      )
    )

    // Visit the iterable expression
    val iterable = visitTree(genFrom.expr) match {
      case expr: Expression => expr
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(forTree)
    }

    // Find the closing delimiter ')' or '}' after the iterable.
    // Search forward from cursor for the first ')' or '}', bounded by the end
    // of the for-tree span to avoid overshooting.
    val iterableAfter = {
      val forEnd = Math.max(0, forTree.span.end - offsetAdjustment)
      val searchEnd = Math.min(forEnd, source.length)
      var closeIdx = -1
      var idx = cursor
      while (idx < searchEnd && closeIdx < 0) {
        val ch = source.charAt(idx)
        if (ch == ')' || ch == '}') closeIdx = idx
        idx += 1
      }
      if (closeIdx >= 0) {
        val spaceBefore = if (closeIdx > cursor) source.substring(cursor, closeIdx) else ""
        cursor = closeIdx + 1 // Skip past the closing delimiter
        Space.format(spaceBefore)
      } else {
        Space.EMPTY
      }
    }

    // Build control — arrowSpace goes on the outer JRightPadded wrapping the variable
    // so the printer can emit "variable <SPACE> <- iterable"
    val control = new J.ForEachLoop.Control(
      Tree.randomId(),
      controlPrefix,
      Markers.EMPTY,
      JRightPadded.build(varDecl.asInstanceOf[Statement]).withAfter(arrowSpace),
      JRightPadded.build(iterable).withAfter(iterableAfter)
    )

    // Visit the body — wrap non-Statement expressions so `for (x <- xs) x + 1` parses
    val bodyJ = visitTree(forTree.body)
    val body: Statement = bodyJ match {
      case stmt: Statement => stmt
      case j: J => new S.StatementExpression(Tree.randomId(), j).asInstanceOf[Statement]
      case _ => visitUnknown(forTree.body).asInstanceOf[Statement]
    }

    updateCursor(forTree.span.end)

    val forEachLoop = new J.ForEachLoop(
      Tree.randomId(),
      prefix,
      Markers.EMPTY.addIfAbsent(ScalaForLoop.create()),
      control,
      JRightPadded.build(body)
    )
    forEachLoop
  }
  
  private def visitBlock(block: Trees.Block[?]): J.Block = {
    val blockStart = Math.max(0, block.span.start - offsetAdjustment)
    val blockEndAdj = Math.max(0, block.span.end - offsetAdjustment)

    // Find the opening brace — it may be at blockStart, before blockStart (while/for body),
    // or between cursor and the first child
    val savedCursorBeforePrefix = cursor
    var prefix = extractPrefix(block.span) // advances cursor to blockStart

    // Now find and advance past '{'
    var hasBraces = false
    // A `{` at blockStart belongs to this Block only when the Block actually wraps
    // braced syntax. When dotty synthesises a Block to wrap a single-expression
    // function body (e.g. the body of `spec => { ... }: Step`), the wrapper Block's
    // span starts at the inner expression's `{`, but the brace belongs to that inner
    // expression — not to the wrapper. Detect this case and treat the wrapper as
    // braceless so the inner expression's braces aren't printed twice.
    val isSyntheticWrapper = block.stats.isEmpty && !block.expr.isEmpty &&
      block.expr.span.exists &&
      Math.max(0, block.expr.span.start - offsetAdjustment) <= blockStart
    if (!isSyntheticWrapper && blockStart < source.length && source.charAt(blockStart) == '{') {
      // Brace at block span start
      cursor = blockStart + 1
      hasBraces = true
    } else if (!isSyntheticWrapper && savedCursorBeforePrefix < blockStart) {
      // Check if there's a brace BEFORE the block span (e.g., while/for body)
      val beforeSpan = source.substring(savedCursorBeforePrefix, blockStart)
      val braceIdx = positionOfNextIn(beforeSpan, "{", 0)
      if (braceIdx >= 0) {
        // The brace is before the block span — extractPrefix included '{' and
        // everything after it in the Space prefix, which is wrong.
        // Recalculate: prefix should only be the whitespace before '{'.
        val bracePos = savedCursorBeforePrefix + braceIdx
        prefix = if (bracePos > savedCursorBeforePrefix) {
          ScalaSpace.format(source, savedCursorBeforePrefix, bracePos)
        } else {
          Space.EMPTY
        }
        cursor = bracePos + 1 // past '{'
        hasBraces = true
      }
    }
    
    val statements = new util.ArrayList[JRightPadded[Statement]]()


    // Visit all statements in the block
    for (i <- block.stats.indices) {
      val stat = block.stats(i)
      val visitResult = visitTree(stat) match {
        case expr: Expression if !expr.isInstanceOf[Statement] =>
          new S.ExpressionStatement(Tree.randomId(), expr)
        case other => other
      }
      visitResult match {
        case null => // Skip null statements (e.g., package declarations)
        case stmt: Statement =>
          // Extract trailing space after this statement.
          // A procedure-syntax method (method-level OmitBraces marker) has an
          // unreliable dotty span end — it overshoots the real closing `}` because
          // of the synthetic `???` body the parser substitutes. `reparseProcedureBody`
          // already advanced the cursor to just past the real `}`, so trust the cursor
          // here; otherwise the whitespace/comments before the next statement are lost.
          val isProcedureMethod = stmt match {
            case md: J.MethodDeclaration => md.getMarkers.findFirst(classOf[OmitBraces]).isPresent
            case _ => false
          }
          val statEnd = if (isProcedureMethod) cursor else Math.max(0, stat.span.end - offsetAdjustment)
          val nextStart = if (i < block.stats.length - 1) {
            Math.max(0, block.stats(i + 1).span.start - offsetAdjustment)
          } else if (!block.expr.isEmpty) {
            Math.max(0, block.expr.span.start - offsetAdjustment)
          } else {
            // Last statement - look for closing brace
            Math.max(0, block.span.end - offsetAdjustment) - 1
          }
          
          var trailingSpace = Space.EMPTY
          var rpMarkers = Markers.EMPTY
          val trailStart = Math.max(statEnd, cursor)
          if (trailStart < nextStart && nextStart <= source.length) {
            val between = source.substring(trailStart, nextStart)
            // An explicit ';' separator appears before any newline and with only
            // horizontal whitespace between it and the preceding statement.
            val semiIdx = {
              var idx = -1
              var j = 0
              while (idx < 0 && j < between.length) {
                val c = between.charAt(j)
                if (c == ';') idx = j
                else if (c == '\n' || c == '\r') j = between.length
                else if (c != ' ' && c != '\t') j = between.length
                else j += 1
              }
              idx
            }
            if (semiIdx >= 0) {
              trailingSpace = if (semiIdx > 0) Space.format(between.substring(0, semiIdx)) else Space.EMPTY
              rpMarkers = Markers.EMPTY.add(new Semicolon(Tree.randomId()))
              cursor = trailStart + semiIdx + 1
            } else {
              trailingSpace = Space.format(between)
              cursor = nextStart
            }
          }

          statements.add(new JRightPadded[Statement](stmt, trailingSpace, rpMarkers))
        case _ => // Skip non-statement nodes
      }
    }
    
    // Handle the expression part of the block (if any)
    // Skip synthetic ??? added by compiler for procedure syntax
    val isSyntheticExpr = if (!block.expr.isEmpty) {
      def hasTripleQ(t: Trees.Tree[?]): Boolean = t match {
        case sel: Trees.Select[?] => sel.name.toString == "???" || sel.name.toString == "$qmark$qmark$qmark" || hasTripleQ(sel.qualifier)
        case id: Trees.Ident[?] => id.name.toString == "???" || id.name.toString == "$qmark$qmark$qmark"
        case app: Trees.Apply[?] => hasTripleQ(app.fun)
        case ta: Trees.TypeApply[?] => hasTripleQ(ta.fun)
        case _ => false
      }
      hasTripleQ(block.expr) || (block.expr.span.exists && {
        val s = block.expr.span.start - offsetAdjustment
        s < 0 || s >= source.length
      })
    } else false
    if (isSyntheticExpr) {
      // Skip synthetic expr — advance cursor to the block end (past the synthetic text)
      val blockEnd2 = Math.max(0, block.span.end - offsetAdjustment)
      if (blockEnd2 > cursor) {
        // Find the last } in the source and position cursor just before it
        val remaining = if (cursor < blockEnd2 && blockEnd2 <= source.length) source.substring(cursor, blockEnd2) else ""
        val lastBrace = remaining.lastIndexOf('}')
        if (lastBrace >= 0) cursor = cursor + lastBrace
        else cursor = blockEnd2
      }
    }
    if (!block.expr.isEmpty && !isSyntheticExpr) {
      // Visit the expression and check if it's a synthetic ??? that slipped through.
      // Detect structurally (a `Predef.???`-style field access), never by string-matching
      // the printed output — user code such as the string literal `"???"` is not synthetic.
      val exprResult = visitTree(block.expr)
      val isSyntheticResult = exprResult.isInstanceOf[J.FieldAccess] && {
        val fa = exprResult.asInstanceOf[J.FieldAccess]
        fa.getSimpleName == "???" || fa.getSimpleName == "$qmark$qmark$qmark"
      }
      if (isSyntheticResult) {
        // Synthetic ??? slipped through — skip it
      } else exprResult match {
        case expr: Expression =>
          // In Scala, the last expression in a block is the return value
          // Wrap it in a J.Return with ImplicitReturn marker
          val implicitReturn = new J.Return(
            Tree.randomId(),
            expr.getPrefix(),
            Markers.build(Collections.singletonList(new ImplicitReturn(Tree.randomId()))),
            expr.withPrefix(Space.EMPTY)
          )
          
          statements.add(JRightPadded.build(implicitReturn.asInstanceOf[Statement]))
        case stmt: Statement =>
          statements.add(JRightPadded.build(stmt))
        case _ => // Skip
      }
    }
    
    // Extract end padding before closing brace
    val blockEnd = Math.max(0, block.span.end - offsetAdjustment)
    var endPadding = Space.EMPTY
    if (cursor < blockEnd && blockEnd <= source.length) {
      val remaining = source.substring(cursor, blockEnd)
      val braceIndex = remaining.lastIndexOf('}')
      if (braceIndex > 0) {
        endPadding = Space.format(remaining.substring(0, braceIndex))
      }
    }
    
    // Update cursor to end of the block
    updateCursor(block.span.end)
    
    val blockMarkers = if (!hasBraces) {
      Markers.build(Collections.singletonList(new OmitBraces(Tree.randomId())))
    } else Markers.EMPTY

    new J.Block(
      Tree.randomId(),
      prefix,
      blockMarkers,
      JRightPadded.build(false), // not static
      statements,
      endPadding
    )
  }
  
  private def visitClassDef(td: Trees.TypeDef[?]): J.ClassDeclaration = {
    // Special handling for classes with annotations
    val hasAnnotations = td.mods.annotations.nonEmpty
    val prefix = if (hasAnnotations) {
      // Don't extract prefix yet - annotations will consume their own prefix
      Space.EMPTY
    } else {
      extractPrefix(td.span)
    }
    
    // Handle annotations first
    val leadingAnnotations = new util.ArrayList[J.Annotation]()
    for (annot <- td.mods.annotations) {
      visitTree(annot) match {
        case ann: J.Annotation => leadingAnnotations.add(ann)
        case _ => // Skip if not mapped to annotation
      }
    }
    
    // After processing annotations, we need to find where modifiers/class keyword start
    // The cursor should now be positioned after the last annotation
    
    // Extract the source text to find modifiers and class/trait keyword
    val adjustedStart = Math.max(0, td.span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, td.span.end - offsetAdjustment)
    var modifierText = ""
    var classIndex = -1
    var isTrait = false
    var isEnum = false
    var isEnumCaseClass = false
    var sourceSnippet = ""
    
    // Use cursor position (after annotations) instead of adjustedStart
    if (cursor >= 0 && adjustedEnd <= source.length && cursor <= adjustedEnd) {
      sourceSnippet = source.substring(cursor, adjustedEnd)
      // Find the class/trait/enum keyword - use whichever comes first in source.
      // Must match as a whole word (not inside an identifier).
      val classIdx = findKeyword(sourceSnippet, "class")
      val traitIdx = findKeyword(sourceSnippet, "trait")
      val enumIdx = findKeyword(sourceSnippet, "enum")

      // Pick the first keyword found
      val candidates = List(
        (classIdx, false, false),
        (traitIdx, true, false),
        (enumIdx, false, true)
      ).filter(_._1 >= 0).sortBy(_._1)

      if (candidates.nonEmpty) {
        val (idx, isTr, isEn) = candidates.head
        classIndex = idx
        isTrait = isTr
        isEnum = isEn
      } else {
        // No class/trait/enum keyword found — check for enum case class: `case Cons(h: A, ...)`
        // In Scala 3, enum members with params are TypeDefs with only `case` keyword.
        val caseIdx = findKeyword(sourceSnippet, "case")
        if (caseIdx >= 0) {
          classIndex = caseIdx
          isEnum = true
          isEnumCaseClass = true
        }
      }
      if (classIndex > 0) {
        modifierText = sourceSnippet.substring(0, classIndex)
      }
    }
    
    // Extract modifiers
    val (modifiers, lastModEnd) = extractModifiersFromText(td.mods, modifierText)
    var endOfModifiers = lastModEnd

    // Check for case modifier (special handling as it's not a traditional modifier)
    if (modifierText.contains("case")) {
      val caseIndex = positionOfNextIn(modifierText, "case", 0)
      if (caseIndex >= 0) {
        // Add case modifier in the correct position
        val caseSpace = if (caseIndex > lastModEnd) {
          Space.format(modifierText.substring(lastModEnd, caseIndex))
        } else {
          Space.EMPTY
        }
        modifiers.add(new J.Modifier(
          Tree.randomId(),
          caseSpace,
          Markers.EMPTY,
          "case",
          J.Modifier.Type.LanguageExtension,
          Collections.emptyList()
        ))
        endOfModifiers = Math.max(endOfModifiers, caseIndex + "case".length)
      }
    }

    // Advance cursor past the modifier text, then use sourceBefore to capture the
    // whitespace before the kind keyword and consume the keyword itself in one step.
    // This avoids re-including modifier text in the kindPrefix (which would print
    // modifiers like `final` twice).
    val kindKeyword =
      if (isEnumCaseClass) "case"
      else if (isTrait) "trait"
      else if (isEnum) "enum"
      else "class"
    val kindPrefix = if (classIndex >= 0) {
      cursor += endOfModifiers
      sourceBefore(kindKeyword)
    } else {
      Space.EMPTY
    }

    // Extract space between the kind keyword and the name, accounting for backtick-quoted names.
    val (nameSpace, displayClassName, classNameEndCursor) = if (td.nameSpan.exists) {
      val rawNameStart = Math.max(0, td.nameSpan.start - offsetAdjustment)
      val rawNameLen = Math.max(0, td.nameSpan.end - td.nameSpan.start)
      backtickAwareName(cursor, rawNameStart, rawNameLen, td.name.toString)
    } else {
      (Space.format(" "), td.name.toString, cursor)
    }

    val kindType = if (isEnum) {
      J.ClassDeclaration.Kind.Type.Enum
    } else if (isTrait) {
      J.ClassDeclaration.Kind.Type.Interface
    } else {
      J.ClassDeclaration.Kind.Type.Class
    }

    val kind = new J.ClassDeclaration.Kind(
      Tree.randomId(),
      kindPrefix,
      Markers.EMPTY,
      Collections.emptyList(),
      kindType
    )

    // Extract class name
    val name = ident(displayClassName, nameSpace)

    // Update cursor to after name (past closing backtick if present)
    if (td.nameSpan.exists && classNameEndCursor > cursor && classNameEndCursor <= source.length) {
      cursor = classNameEndCursor
    }
    
    // Extract template early to access type parameters
    val template = td.rhs match {
      case tmpl: Trees.Template[?] => tmpl
      case _ => null
    }
    
    // Extract type parameters from the template  
    val typeParameters: JContainer[J.TypeParameter] = if (template != null && template.constr.paramss.nonEmpty) {
      // Check if the first param list contains type parameters (TypeDef nodes)
      val firstParamList = template.constr.paramss.head
      val typeParams = firstParamList.collect { case tparam: Trees.TypeDef[?] => tparam }
      
      if (typeParams.nonEmpty) {
        // Look for opening bracket in source
        var bracketStart = cursor
        if (cursor < source.length) {
          val searchEnd = Math.min(cursor + 100, source.length)
          val searchText = source.substring(cursor, searchEnd)
          val bracketIdx = positionOfNextIn(searchText, "[", 0)
          if (bracketIdx >= 0) {
            bracketStart = cursor + bracketIdx
          }
        }
        
        val openingBracketSpace = if (bracketStart > cursor) {
          ScalaSpace.format(source, cursor, bracketStart)
        } else {
          Space.EMPTY
        }
        
        // Update cursor to after opening bracket
        cursor = bracketStart + 1
        
        // Convert TypeDef nodes to J.TypeParameter
        val jTypeParams = new util.ArrayList[JRightPadded[J.TypeParameter]]()
        typeParams.zipWithIndex.foreach { case (tparam, idx) =>
          val jTypeParam = visitTypeParameter(tparam)
          val isLast = idx == typeParams.size - 1

          val afterParam: Space = if (!isLast && cursor < source.length) {
            val commaPos = positionOfNext(",", cursor)
            if (commaPos >= 0) {
              val before = ScalaSpace.format(source, cursor, commaPos)
              cursor = commaPos + 1
              before
            } else Space.EMPTY
          } else if (isLast && cursor < source.length) {
            // Capture whitespace between last type parameter and closing `]`
            val closePos = positionOfNext("]", cursor)
            if (closePos >= 0) ScalaSpace.format(source, cursor, closePos)
            else Space.EMPTY
          } else Space.EMPTY

          jTypeParams.add(new JRightPadded(jTypeParam, afterParam, Markers.EMPTY))
        }
        
        // Update cursor to after closing bracket.
        // Use cursor (not span.end) since visitTypeParameter advances past [_].
        if (cursor < source.length) {
          val searchEnd = Math.min(cursor + 20, source.length)
          val afterParams = source.substring(cursor, searchEnd)
          val closeBracketIdx = positionOfNextIn(afterParams, "]", 0)
          if (closeBracketIdx >= 0) {
            cursor = cursor + closeBracketIdx + 1
          }
        }
        
        JContainer.build(openingBracketSpace, jTypeParams, Markers.EMPTY)
      } else {
        null
      }
    } else {
      null
    }
    
    // Handle constructor parameters - build a proper JContainer<Statement> of J.VariableDeclarations.
    // The first paramss list may contain TypeDefs (type params, already consumed above); we only
    // process the first list of value (ValDef) parameters here. Additional param clauses
    // (curried `(x)(y)` constructors) are not yet modeled.
    val firstValueParamList: Option[List[Trees.ValDef[?]]] = if (template != null) {
      template.constr.paramss.collectFirst {
        case lst if lst.forall(_.isInstanceOf[Trees.ValDef[?]]) =>
          lst.collect { case vd: Trees.ValDef[?] => vd }
      }
    } else None

    // Source has a primary-constructor parameter list iff the next non-whitespace char is `(`.
    val ctorParenPos: Int = {
      var i = cursor
      while (i < source.length && source.charAt(i).isWhitespace) i += 1
      if (i < source.length && source.charAt(i) == '(') i else -1
    }

    val primaryConstructor: JContainer[Statement] = if (ctorParenPos >= 0) {
      val params = firstValueParamList.getOrElse(Nil)
      val parenSpace = ScalaSpace.format(source, cursor, ctorParenPos)
      cursor = ctorParenPos + 1

      val jParams = new util.ArrayList[JRightPadded[Statement]]()
      params.zipWithIndex.foreach { case (vd, idx) =>
        val isLast = idx == params.size - 1
        val param = visitConstructorParameter(vd, isFirstInList = idx == 0)
        val paramEnd = if (vd.span.exists) Math.max(0, vd.span.end - offsetAdjustment) else cursor
        cursor = Math.max(cursor, paramEnd)
        val afterParam: Space = if (!isLast) {
          val nextParamStart = if (params(idx + 1).span.exists) Math.max(0, params(idx + 1).span.start - offsetAdjustment) else cursor
          if (cursor < nextParamStart && nextParamStart <= source.length) {
            val between = source.substring(cursor, nextParamStart)
            val commaIdx = positionOfNextIn(between, ",", 0)
            if (commaIdx >= 0) {
              val before = Space.format(between.substring(0, commaIdx))
              cursor = cursor + commaIdx + 1
              before
            } else Space.EMPTY
          } else Space.EMPTY
        } else {
          // Capture whitespace between last parameter and closing `)`
          if (cursor < source.length) {
            val remaining = source.substring(cursor, Math.min(cursor + 500, source.length))
            val closeParen = positionOfNextIn(remaining, ")", 0)
            if (closeParen >= 0) Space.format(remaining.substring(0, closeParen)) else Space.EMPTY
          } else Space.EMPTY
        }
        jParams.add(new JRightPadded(param.asInstanceOf[Statement], afterParam, Markers.EMPTY))
      }

      // Advance cursor past closing `)`
      if (cursor < source.length) {
        val remaining = source.substring(cursor, Math.min(cursor + 500, source.length))
        val closeParen = positionOfNextIn(remaining, ")", 0)
        if (closeParen >= 0) cursor = cursor + closeParen + 1
      }

      // Scala 3 allows curried constructor param lists: `class C(a: Int)(using Executor)`.
      // J.ClassDeclaration only models the first list; collect the rest verbatim from source
      // so the printer can re-emit them. Stops at the first non-`(` non-whitespace char.
      val extraListsBuf = new StringBuilder
      var scanCursor = cursor
      var keepScanning = true
      while (keepScanning) {
        var probe = scanCursor
        while (probe < source.length && source.charAt(probe).isWhitespace) probe += 1
        if (probe < source.length && source.charAt(probe) == '(') {
          val closePos = positionOfMatchingClose('(', ')', probe + 1)
          if (closePos >= 0) {
            extraListsBuf.append(source.substring(scanCursor, closePos + 1))
            scanCursor = closePos + 1
          } else {
            keepScanning = false
          }
        } else {
          keepScanning = false
        }
      }
      val containerMarkers: Markers = if (extraListsBuf.nonEmpty) {
        cursor = scanCursor
        Markers.build(Collections.singletonList(
          org.openrewrite.scala.marker.ExtraConstructorParamLists(
            Tree.randomId(), extraListsBuf.toString)))
      } else Markers.EMPTY

      JContainer.build(parenSpace, jParams, containerMarkers)
    } else {
      // No `(` in source — non-constructor class definition. Emit an empty container
      // marked with OmitParentheses so the printer skips emitting `(...)`.
      JContainer.build(Space.EMPTY, new util.ArrayList[JRightPadded[Statement]](),
        Markers.build(Collections.singletonList(new OmitParentheses(Tree.randomId()))))
    }
    
    // Extract extends/implements from Template
    var extendings: JLeftPadded[TypeTree] = null
    var implementings: JContainer[TypeTree] = null
    
    // Filter out synthetic parents added by the compiler (e.g., implicit "extends Object")
    // Only keep parents that have real source spans
    val sourceParents = if (template != null) {
      template.parents.filter(p => p.span.exists && !p.span.isSynthetic)
    } else Nil

    if (sourceParents.nonEmpty) {
        // In Scala, the first parent after the primary constructor is the extends clause
        // Additional parents are the with clauses (implements in Java)

        // First, we need to find where "extends" keyword starts in the source
        // Start searching for "extends" from the current cursor position.
        // The cursor has been advanced past class name, type parameters,
        // and constructor parameters by earlier processing.
        val extendsKeywordPos = cursor
        
        // Look for "extends" keyword in source
        var extendsSpace = Space.EMPTY
        if (extendsKeywordPos < source.length && sourceParents.head.span.exists) {
          val firstParentStart = Math.max(0, sourceParents.head.span.start - offsetAdjustment)
          if (extendsKeywordPos < firstParentStart && firstParentStart <= source.length) {
            val betweenText = source.substring(extendsKeywordPos, firstParentStart)
            val extendsIndex = positionOfNextIn(betweenText, "extends", 0)
            if (extendsIndex >= 0) {
              extendsSpace = Space.format(betweenText.substring(0, extendsIndex))
              // Update cursor to after "extends" keyword
              cursor = extendsKeywordPos + extendsIndex + "extends".length
            }
          }
        }
        
        // First parent is the extends clause
        val firstParent = sourceParents.head
        val savedCursorExtends = cursor
        val extendsType: TypeTree = visitTree(firstParent) match {
          case tt: TypeTree => tt
          case id: J.Identifier => id.asInstanceOf[TypeTree]
          case other =>
            // Constructor call, intersection type, or other complex parent — preserve source
            updateCursor(firstParent.span.end)
            ident(extractSource(firstParent.span), other.getPrefix).asInstanceOf[TypeTree]
        }
        
        extendings = new JLeftPadded(extendsSpace, extendsType, Markers.EMPTY)
        
        // Update cursor to after first parent
        if (firstParent.span.exists) {
          cursor = Math.max(cursor, firstParent.span.end - offsetAdjustment)
        }
        
        // Handle additional parents as implements (with clauses)
        if (sourceParents.size > 1) {
          val implementsList = new util.ArrayList[JRightPadded[TypeTree]]()
          
          // Extract space before the first "with" or "extends" (if no extends clause)
          var containerSpace = Space.EMPTY
          if (extendings == null && sourceParents.nonEmpty) {
            // No extends clause, so first trait uses "extends"
            val firstParent = sourceParents.head
            if (firstParent.span.exists) {
              containerSpace = sourceBefore("extends")
            }
          } else if (extendings != null && sourceParents.size > 1) {
            // We have extends, so look for first "with"
            containerSpace = sourceBefore("with")
          }
          
          for (i <- 1 until sourceParents.size) {
            val parent = sourceParents(i)
            val savedCursorWith = cursor

            val implType: TypeTree = visitTree(parent) match {
              case tt: TypeTree => tt
              case id: J.Identifier => id.asInstanceOf[TypeTree]
              case _ =>
                cursor = savedCursorWith
                visitUnknown(parent)
            }
            
            // Build the right-padded element
            val rightPadded = if (i < sourceParents.size - 1) {
              // Not the last element, look for space before next "with"
              val afterSpace = sourceBefore("with")
              new JRightPadded(implType, afterSpace, Markers.EMPTY)
            } else {
              // Last element, no trailing space needed
              JRightPadded.build(implType)
            }
            
            implementsList.add(rightPadded)
          }
          
          if (!implementsList.isEmpty) {
            implementings = JContainer.build(
              containerSpace,
              implementsList,
              Markers.EMPTY
            )
          }
        }
    }
    
    // Handle the body - TypeDef has rhs which should be a Template for classes
    // For classes without explicit body, we should NOT print empty braces
    val hasExplicitBody = td.rhs match {
      case tmpl: Trees.Template[?] =>
        // A class has an explicit body if:
        // 1. The template has any body statements, OR
        // 2. There's a "{" in the source (even for empty bodies)
        if (tmpl.body.nonEmpty) {
          // If there are body statements, we definitely have a body
          true
        } else if (td.span.exists) {
          // For empty bodies, check if there's a "{" in the source after the cursor.
          // The cursor has been advanced past the name, type params, constructor params,
          // and extends clause, so any `{` here is the body delimiter — not one that
          // could appear earlier inside a string interpolation like `s"${x}"`.
          val classEnd = Math.max(0, td.span.end - offsetAdjustment)
          if (cursor < classEnd && classEnd <= source.length) {
            source.substring(cursor, classEnd).contains("{")
          } else {
            false
          }
        } else {
          false
        }
      case _ => false
    }
    
    val body = if (hasExplicitBody) {
      td.rhs match {
        case template: Trees.Template[?] =>
          // Extract space before the body delimiter: `{` (braced) or `:` (Scala 3 braceless)
          var isClassBraceless = false
          val bodyPrefix = if (td.span.exists) {
            val classEnd = Math.max(0, td.span.end - offsetAdjustment)
            if (cursor < classEnd && classEnd <= source.length) {
              val afterCursor = source.substring(cursor, classEnd)
              val braceIndex = positionOfNextIn(afterCursor, "{", 0)
              // For Scala 3 braceless: look for `:` at end of line (not `: Type` annotation).
              // A braceless body colon is followed by a newline, not by a type name.
              val colonIndex = {
                var result = -1
                var idx = positionOfNextIn(afterCursor, ":", 0)
                while (idx >= 0 && result < 0) {
                  if (idx + 1 >= afterCursor.length) {
                    result = idx // `:` at end of text
                  } else {
                    val afterColon = afterCursor.substring(idx + 1)
                    val nextNonSpace = afterColon.indexWhere(c => c != ' ' && c != '\t')
                    if (nextNonSpace < 0 || afterColon.charAt(nextNonSpace) == '\n' || afterColon.charAt(nextNonSpace) == '\r') {
                      result = idx // `:` followed by newline = braceless body
                    }
                  }
                  if (result < 0) idx = positionOfNextIn(afterCursor, ":", idx + 1)
                }
                result
              }
              if (braceIndex >= 0 && (colonIndex < 0 || braceIndex < colonIndex)) {
                val prefix = Space.format(afterCursor.substring(0, braceIndex))
                cursor = cursor + braceIndex + 1
                prefix
              } else if (colonIndex >= 0) {
                isClassBraceless = true
                val prefix = Space.format(afterCursor.substring(0, colonIndex))
                cursor = cursor + colonIndex + 1
                prefix
              } else {
                Space.format(" ")
              }
            } else {
              Space.EMPTY
            }
          } else {
            Space.EMPTY
          }
          
          // Visit the template body to get statements
          val statements = new util.ArrayList[JRightPadded[Statement]]()
          
          // Visit each statement in the template body.
          // When traversing the typed tree, the compiler duplicates constructor params
          // into the body. Track visited spans to avoid processing the same source twice.
          val visitedSpans = new java.util.HashSet[Int]()
          // Sort by source position to preserve source order
          val sortedBody = template.body.sortBy(s => if (s.span.exists) s.span.start else Int.MaxValue)
          val classEndForBody = if (td.span.exists) Math.max(0, td.span.end - offsetAdjustment) else source.length
          for (idx <- sortedBody.indices) {
            val stat = sortedBody(idx)
            val isSyntheticStat = stat.span.isSynthetic || {
              def hasTripleQ(t: Trees.Tree[?]): Boolean = t match {
                case sel: Trees.Select[?] => sel.name.toString == "???" || sel.name.toString == "$qmark$qmark$qmark" || hasTripleQ(sel.qualifier)
                case id: Trees.Ident[?] => id.name.toString == "???" || id.name.toString == "$qmark$qmark$qmark"
                case app: Trees.Apply[?] => hasTripleQ(app.fun)
                case ta: Trees.TypeApply[?] => hasTripleQ(ta.fun)
                case _ => false
              }
              hasTripleQ(stat)
            }
            if (stat.span.exists && !isSyntheticStat) {
              if (visitedSpans.add(stat.span.start)) {
                val visitedStmt: Statement = visitTree(stat) match {
                  case null => null
                  case stmt: Statement => stmt
                  case expr: Expression => new S.ExpressionStatement(Tree.randomId(), expr)
                  case _ => null
                }
                if (visitedStmt != null) {
                  val statEnd = Math.max(0, stat.span.end - offsetAdjustment)
                  val nextStart = {
                    var k = idx + 1
                    var ns = classEndForBody
                    while (k < sortedBody.size) {
                      val s = sortedBody(k)
                      if (s.span.exists && !s.span.isSynthetic) {
                        ns = Math.max(0, s.span.start - offsetAdjustment)
                        k = sortedBody.size
                      } else {
                        k += 1
                      }
                    }
                    ns
                  }
                  val (trailingSpace, rpMarkers) = consumeTrailingSemicolon(statEnd, nextStart)
                  statements.add(new JRightPadded[Statement](visitedStmt, trailingSpace, rpMarkers))
                }
              }
            }
          }
          
          // Extract end space
          val endSpace = if (td.span.exists) {
            val classEnd = Math.max(0, td.span.end - offsetAdjustment)
            if (isClassBraceless) {
              if (cursor < classEnd) {
                val es = ScalaSpace.format(source, cursor, Math.min(classEnd, source.length))
                cursor = classEnd
                es
              } else Space.EMPTY
            } else if (cursor < classEnd && classEnd <= source.length) {
              val remaining = source.substring(cursor, classEnd)
              val closeBraceIndex = remaining.lastIndexOf("}")
              if (closeBraceIndex >= 0) {
                cursor = classEnd
                Space.format(remaining.substring(0, closeBraceIndex))
              } else Space.EMPTY
            } else Space.EMPTY
          } else Space.EMPTY

          val classBlockMarkers = if (isClassBraceless) {
            Markers.build(Collections.singletonList(new IndentedSyntax(Tree.randomId())))
          } else Markers.EMPTY

          new J.Block(
            Tree.randomId(),
            bodyPrefix,
            classBlockMarkers,
            JRightPadded.build(false),
            statements,
            endSpace
          )
        case _ =>
          // Fallback - shouldn't happen if hasExplicitBody is true
          null
      }
    } else {
      // For classes without body (like "class Empty"), return null
      null
    }
    
    // Update cursor to end of the class
    if (td.span.exists) {
      val adjustedEnd = Math.max(0, td.span.end - offsetAdjustment)
      if (adjustedEnd > cursor && adjustedEnd <= source.length) {
        cursor = adjustedEnd
      }
    }
    
    val classDeclMarkers = if (isEnumCaseClass) {
      Markers.build(Collections.singletonList(SObject.create()))
    } else Markers.EMPTY

    new J.ClassDeclaration(
      Tree.randomId(),
      prefix,
      classDeclMarkers,
      leadingAnnotations, // annotations
      modifiers,
      kind,
      name,
      typeParameters,
      primaryConstructor,
      extendings,
      implementings,
      null, // permits
      body,
      TypeUtils.asFullyQualified(typeOfTree(td))
    )
  }
  
  private def visitReturn(ret: Trees.Return[?]): J = {
    val prefix = extractPrefix(ret.span)

    // Advance cursor past "return" keyword
    val retStart = Math.max(0, ret.span.start - offsetAdjustment)
    if (retStart >= cursor && retStart + "return".length <= source.length) {
      cursor = retStart + "return".length
    }

    // Extract the expression being returned (if any). In Scala, `return`'s argument can be
    // any expression including block/if/match — wrap Statement results via StatementExpression.
    val expr = if (ret.expr.isEmpty) {
      null // void return
    } else {
      visitTree(ret.expr) match {
        case expression: Expression => expression
        case j: J => new S.StatementExpression(Tree.randomId(), j)
        case _ => return visitUnknown(ret)
      }
    }
    
    // Update cursor to the end of the return statement
    updateCursor(ret.span.end)
    
    new J.Return(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      expr
    )
  }

  private def visitThrow(thr: untpd.Throw): J = {
    val prefix = extractPrefix(thr.span)

    // Advance cursor past "throw" keyword
    val thrStart = Math.max(0, thr.span.start - offsetAdjustment)
    if (thrStart >= cursor && thrStart + "throw".length <= source.length) {
      cursor = thrStart + "throw".length
    }

    // Visit the exception expression. `throw` in Scala can take any expression including
    // if/match/block — wrap Statement results via StatementExpression.
    val exception = visitTree(thr.expr) match {
      case expr: Expression => expr
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(thr)
    }
    
    // Update cursor to the end of the throw statement
    updateCursor(thr.span.end)
    
    new J.Throw(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      exception
    )
  }
  
  private def visitTypeApply(ta: Trees.TypeApply[?]): J = {
    val savedCursor = cursor
    // TypeApply represents a type application like List.empty[Int] or obj.asInstanceOf[Type]
    ta.fun match {
      case sel: Trees.Select[?] =>
        // Check if this is asInstanceOf
        if (sel.name.toString == "asInstanceOf" && ta.args.size == 1) {
          // This is a type cast operation: obj.asInstanceOf[Type]
          
          // Visit the expression being cast (with its own prefix)
          // The expression (sel.qualifier) is the object before .asInstanceOf
          val expr = visitTree(sel.qualifier) match {
            case e: Expression => e
            case j: J => new S.StatementExpression(Tree.randomId(), j)
            case _ => return visitUnknown(ta)
          }
          
          // Capture whitespace between the qualifier and ".asInstanceOf"
          // (e.g. when ".asInstanceOf" sits on its own line as part of a chain).
          val asInstanceOfEnd = Math.max(0, sel.span.end - offsetAdjustment)
          val asInstanceOfNameLen = "asInstanceOf".length
          val dotPos = asInstanceOfEnd - asInstanceOfNameLen - 1
          val asInstanceOfPrefix: Space =
            if (cursor >= 0 && cursor <= dotPos && dotPos <= source.length) {
              ScalaSpace.format(source.substring(cursor, dotPos))
            } else {
              Space.EMPTY
            }

          // Update cursor past ".asInstanceOf"
          if (asInstanceOfEnd > cursor) {
            cursor = asInstanceOfEnd
          }
          
          // Now handle the type argument in brackets
          // Extract any space before the opening bracket
          val typeArgStart = ta.args.head.span.start - offsetAdjustment
          val spaceBeforeBracket = if (cursor < typeArgStart && typeArgStart <= source.length) {
            val between = source.substring(cursor, typeArgStart)
            // Find the bracket position
            val bracketPos = positionOfNextIn(between, "[", 0)
            if (bracketPos >= 0) {
              cursor = cursor + bracketPos + 1  // Move past the bracket
              Space.format(between.substring(0, bracketPos))
            } else {
              Space.EMPTY
            }
          } else {
            Space.EMPTY
          }
          
          // Visit the target type
          val targetType = visitTree(ta.args.head) match {
            case tt: TypeTree => tt
            case _ => return visitUnknown(ta)
          }
          
          // Update cursor past the closing bracket
          if (ta.span.end > cursor) {
            cursor = ta.span.end
          }
          
          val typeCastMarkers =
            if (asInstanceOfPrefix.getWhitespace.nonEmpty || !asInstanceOfPrefix.getComments.isEmpty) {
              Markers.EMPTY.addIfAbsent(AsInstanceOfPrefix.create(asInstanceOfPrefix))
            } else {
              Markers.EMPTY
            }

          return new J.TypeCast(
            Tree.randomId(),
            Space.EMPTY,  // TypeCast itself has no prefix - the space is handled by the variable initializer
            typeCastMarkers,
            new J.ControlParentheses[TypeTree](
              Tree.randomId(),
              spaceBeforeBracket,
              Markers.EMPTY,
              JRightPadded.build(targetType)
            ),
            expr
          )
        }
        
        // Check if this is isInstanceOf
        if (sel.name.toString == "isInstanceOf" && ta.args.size == 1) {
          // This is a type check operation: obj.isInstanceOf[Type]
          
          // Extract prefix
          val startPos = Math.max(0, ta.span.start - offsetAdjustment)
          val prefix = if (startPos > cursor && startPos <= source.length) {
            ScalaSpace.format(source, cursor, startPos)
          } else {
            Space.EMPTY
          }
          
          // Update cursor to start of the expression (sel.qualifier)
          cursor = Math.max(0, sel.qualifier.span.start - offsetAdjustment)
          
          // Visit the expression being checked
          val expr = visitTree(sel.qualifier) match {
            case e: Expression => e
            case j: J => new S.StatementExpression(Tree.randomId(), j)
            case _ => return visitUnknown(ta)
          }

          // Capture whitespace between the qualifier and ".isInstanceOf"
          // (e.g. when ".isInstanceOf" sits on its own line as part of a chain).
          val isInstanceOfEnd = Math.max(0, sel.span.end - offsetAdjustment)
          val isInstanceOfNameLen = "isInstanceOf".length
          val isInstanceOfDotPos = isInstanceOfEnd - isInstanceOfNameLen - 1
          val exprAfterSpace: Space =
            if (cursor >= 0 && cursor <= isInstanceOfDotPos && isInstanceOfDotPos <= source.length) {
              ScalaSpace.format(source.substring(cursor, isInstanceOfDotPos))
            } else {
              Space.EMPTY
            }

          // Advance cursor to just after `[` so the target type's own prefix
          // captures any whitespace between `[` and the type.
          val openBracket = positionOfNext("[", cursor)
          if (openBracket >= cursor && openBracket < source.length) {
            cursor = openBracket + 1
          } else {
            cursor = Math.max(0, ta.args.head.span.start - offsetAdjustment)
          }

          // Visit the target type
          val clazz = visitTree(ta.args.head) match {
            case tt: TypeTree => tt
            case _ => return visitUnknown(ta)
          }

          // Update cursor to the end of the TypeApply
          updateCursor(ta.span.end)

          return new J.InstanceOf(
            Tree.randomId(),
            prefix,
            Markers.EMPTY,
            new JRightPadded(expr, exprAfterSpace, Markers.EMPTY),
            clazz,
            null, // pattern (not used in Scala)
            null  // type
          )
        }

        // General method reference with type arguments and no value arguments:
        //   List.newBuilder[Instant], List.empty[Int], Option.empty[String], etc.
        // Maps to J.MethodInvocation with typeParameters populated and arguments
        // marked OmitParentheses (since the source has no `(...)`).
        return visitMethodInvocationFromTypeApply(ta, sel, savedCursor)

      case id: Trees.Ident[?] if id.name.toString == "classOf" && ta.args.size == 1 =>
        // classOf[String] is a type application with no value argument list.
        val prefix = extractPrefix(ta.span)
        val nameEnd = if (id.span.exists) Math.max(0, id.span.end - offsetAdjustment) else cursor + "classOf".length
        if (nameEnd > cursor && nameEnd <= source.length) {
          cursor = nameEnd
        }
        val typeParams = parseTypeApplyArgs(ta)
        val omitParens = Markers.build(Collections.singletonList(new OmitParentheses(Tree.randomId())))
        val args = JContainer.build(Space.EMPTY, Collections.emptyList[JRightPadded[Expression]](), omitParens)
        updateCursor(ta.span.end)
        return new J.MethodInvocation(
          Tree.randomId(),
          prefix,
          Markers.EMPTY,
          null,
          typeParams,
          ident("classOf"),
          args,
          methodTypeOfTree(ta)
        )

      case _ =>
        // Other TypeApply (e.g., Array[Int]): preserve as identifier with source text.
        // Check for trailing () that the span might not cover.
        val prefix = extractPrefix(ta.span)
        var text = extractSource(ta.span)
        var endPos = Math.max(0, ta.span.end - offsetAdjustment)
        if (endPos < source.length && source.charAt(endPos) == '(') {
          val closeIdx = positionOfNext(")", endPos + 1)
          if (closeIdx >= 0) {
            text = text + source.substring(endPos, closeIdx + 1)
            endPos = closeIdx + 1
          }
        }
        cursor = endPos
        return ident(text, prefix, typeFor(ta.span))
    }

    // Shouldn't reach here — all cases return above
    visitUnknown(ta)
  }

  /**
   * Build a J.MethodInvocation from a TypeApply wrapping a Select, for method references
   * that have type arguments but no value argument list (e.g., `List.newBuilder[Instant]`,
   * `List.empty[Int]`). The arguments container is marked with OmitParentheses so the
   * printer emits the call without `()`.
   */
  private def visitMethodInvocationFromTypeApply(ta: Trees.TypeApply[?], sel: Trees.Select[?], savedCursor: Int): J = {
    val prefix = extractPrefix(ta.span)

    // Visit the qualifier (e.g., `List`)
    val qual = visitTree(sel.qualifier) match {
      case e: Expression => e
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => cursor = savedCursor; return visitUnknown(ta)
    }

    // Capture space between qualifier and the `.`
    val dotPos = positionOfNext(".", cursor)
    val selectAfter = if (dotPos > cursor) ScalaSpace.format(source, cursor, dotPos) else Space.EMPTY
    if (dotPos >= 0) cursor = dotPos + 1

    // Method name (e.g., `newBuilder`)
    val methodNameStr = sel.name.toString
    val nameStart = if (sel.nameSpan.exists) Math.max(0, sel.nameSpan.start - offsetAdjustment) else cursor
    val namePrefix = if (nameStart > cursor && nameStart <= source.length) {
      ScalaSpace.format(source, cursor, nameStart)
    } else Space.EMPTY
    cursor = Math.min(source.length, nameStart + methodNameStr.length)
    val name = ident(methodNameStr, namePrefix)

    // Type arguments between `[` and `]`
    val typeParams = parseTypeApplyArgs(ta)

    // Empty value-argument container with OmitParentheses marker
    val omitParens = Markers.build(Collections.singletonList(new OmitParentheses(Tree.randomId())))
    val args = JContainer.build(Space.EMPTY, Collections.emptyList[JRightPadded[Expression]](), omitParens)

    updateCursor(ta.span.end)

    new J.MethodInvocation(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      new JRightPadded(qual, selectAfter, Markers.EMPTY),
      typeParams,
      name,
      args,
      methodTypeOfTree(ta)
    )
  }

  /**
   * Parse the `[T, U, ...]` portion of a TypeApply into a JContainer of type-argument
   * expressions. Assumes `cursor` is positioned at or before the opening `[`.
   * Leaves cursor positioned just past the closing `]`.
   */
  private def parseTypeApplyArgs(ta: Trees.TypeApply[?]): JContainer[Expression] = {
    val beforeBracket = sourceBefore("[")
    buildArgumentContainer[Trees.Tree[?], Expression](
      ta.args,
      arg => visitTree(arg) match {
        case e: Expression => e
        case tt: TypeTree => tt.asInstanceOf[Expression]
        case j: J => new S.StatementExpression(Tree.randomId(), j)
        case _ => visitUnknown(arg)
      },
      "]",
      beforeBracket
    )
  }
  
  private def visitAppliedTypeTree(at: Trees.AppliedTypeTree[?]): J = {
    // Dotty's parser desugars `A with B` (and `&` intersections introduced via
    // `makeAndType`) into an AppliedTypeTree whose `tpt` is a synthetic, zero-
    // span reference to `scala.&`. Detect that shape and emit J.IntersectionType
    // instead of falling through to the parameterized-type path.
    if (isWithIntersectionType(at)) {
      return visitWithIntersectionType(at)
    }

    // AppliedTypeTree represents a parameterized type like List[String]
    val savedCursor = cursor
    val prefix = extractPrefix(at.span)

    // Visit the base type (e.g., List, Map, Option)
    val clazz = visitTree(at.tpt) match {
      case nt: NameTree => nt
      case _ => cursor = savedCursor; return visitUnknown(at)
    }

    // Find bracket positions in absolute source coordinates, skipping comments.
    val atStart = Math.max(0, at.span.start - offsetAdjustment)
    val atEndAbs = Math.min(source.length, Math.max(atStart, at.span.end - offsetAdjustment))
    val openBracketAbs = positionOfNext("[", atStart)
    // The applied type's closing `]` sits right at `at.span.end - 1`.
    val closeBracketAbs = if (atEndAbs > atStart && atEndAbs <= source.length && source.charAt(atEndAbs - 1) == ']') atEndAbs - 1 else -1

    if (openBracketAbs < 0 || closeBracketAbs < 0 || openBracketAbs >= atEndAbs) {
      cursor = savedCursor; return visitUnknown(at)
    }
    val openBracketIdx = openBracketAbs - atStart
    val closeBracketIdx = closeBracketAbs - atStart

    // Extract space before opening bracket
    val baseTypeEnd = clazz match {
      case id: J.Identifier => id.getSimpleName.length
      case _ => openBracketIdx
    }

    val srcSlice = source.substring(atStart, atEndAbs)
    val beforeOpenBracket = if (baseTypeEnd < openBracketIdx) {
      Space.format(srcSlice, baseTypeEnd, openBracketIdx)
    } else {
      Space.EMPTY
    }
    
    // Process type arguments
    val typeArgs = new util.ArrayList[JRightPadded[Expression]]()
    
    if (at.args.nonEmpty) {
      // Position cursor right after the opening bracket so that the first arg's
      // own prefix-extraction picks up any whitespace between `[` and the arg.
      cursor = openBracketAbs + 1

      for (i <- at.args.indices) {
        val arg = at.args(i)
        val argTree = visitTree(arg) match {
          case expr: Expression => expr
          case j: J => new S.StatementExpression(Tree.randomId(), j)
          case _ => cursor = savedCursor; return visitUnknown(at)
        }

        // Extract trailing comma/space
        val isLast = i == at.args.size - 1
        val afterSpace = if (isLast) {
          // Space before closing bracket
          val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
          if (argEnd < closeBracketAbs) {
            val spaceStr = this.source.substring(argEnd, closeBracketAbs)
            Space.format(spaceStr)
          } else {
            Space.EMPTY
          }
        } else {
          // Look for comma and space after it
          val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
          val nextArgStart = if (i + 1 < at.args.size) {
            Math.max(0, at.args(i + 1).span.start - offsetAdjustment)
          } else {
            closeBracketAbs
          }
          
          if (argEnd < nextArgStart && argEnd < this.source.length && nextArgStart <= this.source.length) {
            val between = this.source.substring(argEnd, nextArgStart)
            val commaIdx = positionOfNextIn(between, ",", 0)
            if (commaIdx >= 0) {
              // The "after" space should be everything from the end of the argument up to (but not including) the comma
              // The visitContainer will add the comma and then the prefix of the next element will have the space after the comma
              cursor = argEnd + commaIdx + 1  // Move cursor past the comma
              Space.format(between.substring(0, commaIdx))
            } else {
              Space.EMPTY
            }
          } else {
            Space.EMPTY
          }
        }
        
        typeArgs.add(JRightPadded.build(argTree).withAfter(afterSpace))
      }
    }
    
    // Update cursor to the end of the AppliedTypeTree
    updateCursor(at.span.end)
    
    // Create the type parameters container
    val typeParameters = JContainer.build(
      beforeOpenBracket,
      typeArgs,
      Markers.EMPTY
    )
    
    val paramType = parameterizedTypeForAppliedTypeTree(clazz, typeArgs, typeOfTree(at))

    new J.ParameterizedType(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      clazz,
      typeParameters,
      paramType
    )
  }

  private def parameterizedTypeForAppliedTypeTree(
    clazz: NameTree,
    typeArgs: util.List[JRightPadded[Expression]],
    mappedType: JavaType
  ): JavaType = mappedType match {
    case pt: JavaType.Parameterized => pt
    case _ =>
      val fallbackType =
        if (mappedType == null || mappedType.isInstanceOf[JavaType.Unknown]) clazz.getType
        else mappedType

      fallbackType match {
        case pt: JavaType.Parameterized if !typeArgs.isEmpty =>
          new JavaType.Parameterized(null, pt.getType, typeArgs.asScala.map(typeArgType).asJava)
        case fq: JavaType.FullyQualified if !fq.isInstanceOf[JavaType.Unknown] && !typeArgs.isEmpty =>
          new JavaType.Parameterized(null, fq, typeArgs.asScala.map(typeArgType).asJava)
        case _ =>
          mappedType
      }
  }

  private def typeArgType(arg: JRightPadded[Expression]): JavaType = {
    arg.getElement.getType match {
      case null | _: JavaType.Unknown =>
      case t => return t
    }

    arg.getElement match {
      case id: J.Identifier =>
        val mapped = typeForName(id.getSimpleName)
        if (mapped != null) mapped else JavaType.Unknown.getInstance()
      case _ =>
        JavaType.Unknown.getInstance()
    }
  }

  /**
   * True when `at` is the desugared form of an `A with B` intersection type:
   * a synthetic (zero-span) `tpt` reference to `scala.&` and exactly two args,
   * with the literal keyword `with` appearing in source between the two args.
   *
   * `A & B` syntax is parsed as an `untpd.InfixOp` (not via `makeAndType`), so
   * it doesn't match here.
   */
  private def isWithIntersectionType(at: Trees.AppliedTypeTree[?]): Boolean = {
    if (at.args.size != 2) return false
    val tpt = at.tpt
    // Synthetic tpt: either no span at all, or a zero-length span.
    if (tpt.span.exists && tpt.span.start != tpt.span.end) return false
    val left = at.args.head
    val right = at.args(1)
    if (!left.span.exists || !right.span.exists) return false
    val leftEnd = Math.max(0, left.span.end - offsetAdjustment)
    val rightStart = Math.max(0, right.span.start - offsetAdjustment)
    if (leftEnd >= rightStart || rightStart > source.length) return false
    // Match `with` as a whole keyword (avoid e.g. an identifier containing "with").
    val between = source.substring(leftEnd, rightStart)
    "(?s).*\\bwith\\b.*".r.matches(between)
  }

  /**
   * Flatten a right-associative `A with B with C` (parsed as
   * `AppliedTypeTree(&, [A, AppliedTypeTree(&, [B, C])])`) into the list of
   * leaf type trees `[A, B, C]`.
   */
  private def flattenWithIntersection(at: Trees.AppliedTypeTree[?]): List[Trees.Tree[?]] = {
    val tail = at.args(1) match {
      case inner: Trees.AppliedTypeTree[?] if isWithIntersectionType(inner) =>
        flattenWithIntersection(inner)
      case other =>
        List(other)
    }
    at.args.head :: tail
  }

  private def visitWithIntersectionType(at: Trees.AppliedTypeTree[?]): J = {
    val prefix = extractPrefix(at.span)
    val parts = flattenWithIntersection(at)

    val elements = new util.ArrayList[JRightPadded[TypeTree]](parts.size)
    for (i <- parts.indices) {
      // Don't clobber the cursor here: visitTypeTree → extractPrefix relies on
      // the gap between `cursor` and the part's span.start to capture the
      // whitespace after the previous `with` keyword.
      val tt = visitTypeTree(parts(i))
      if (tt == null) return visitUnknown(parts(i))
      val isLast = i == parts.size - 1
      val afterSpace = if (isLast) Space.EMPTY else sourceBefore("with")
      elements.add(new JRightPadded[TypeTree](tt, afterSpace, Markers.EMPTY))
    }
    updateCursor(at.span.end)
    new J.IntersectionType(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      JContainer.build(Space.EMPTY, elements, Markers.EMPTY)
    )
  }

  private def visitDefDef(dd: Trees.DefDef[?]): J = {
    val adjustedStart = Math.max(0, dd.span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, dd.span.end - offsetAdjustment)
    var isProcedureSyntax = false

    // Procedure syntax (`def f(...) { ... }`) is detected when the body `{` is
    // reached before any method-body `=`. The scan skips parameter clauses and
    // type parameter lists so a default value (`x: Int = 0`) or a function-type
    // parameter (`g: () => Unit`) isn't mistaken for the method-body `=`.
    // Note: nested braces (def foo = { { ... } }) are flattened by the compiler;
    // the AST has one block, so the inner braces are lost in the round-trip.
    if (adjustedStart < adjustedEnd && adjustedEnd <= source.length) {
      isProcedureSyntax = bodyOpensWithBrace(adjustedStart, adjustedEnd)
    }

    // Detect parameterless methods (def name: Type = ...) — no parens in source.
    // Use nameSpan when available so symbolic names like `*` or `+:` are skipped
    // correctly (otherwise the alpha-only name scan stops at the first char and
    // mistakes the body's opening `(` for a parameter list).
    var hasParensInSource = true
    // For `given foo[T]: Bar = ...` (named) or `given [T]: Bar = ...` (anonymous),
    // we must skip past `[type-params]` before deciding. Both cases land here when
    // the underlying DefDef has Given flag set.
    val isGivenDefDef = dd.mods != null && dd.mods.is(Flags.Given)
    if (dd.nameSpan.exists) {
      val nameEnd = Math.max(0, dd.nameSpan.end - offsetAdjustment)
      var i = nameEnd
      while (i < source.length && source.charAt(i).isWhitespace) i += 1
      // Skip a `[...]` type parameter list. After the type params, an ordinary
      // def may have `()` (regular method) or `:`/`=` (parameterless method).
      if (i < source.length && source.charAt(i) == '[') {
        val closePos = positionOfMatchingClose('[', ']', i + 1)
        i = if (closePos >= 0) closePos + 1 else source.length
        while (i < source.length && source.charAt(i).isWhitespace) i += 1
      }
      if (i < source.length) {
        val nextCh = source.charAt(i)
        if (nextCh == ':' || nextCh == '=') {
          hasParensInSource = false
        }
      }
    } else if (adjustedStart < adjustedEnd && adjustedEnd <= source.length) {
      val defSource = source.substring(adjustedStart, adjustedEnd)
      val keyword = if (isGivenDefDef) "given" else "def"
      val kwIdx = positionOfNextIn(defSource, keyword + " ", 0)
      if (kwIdx >= 0) {
        var rest = defSource.substring(kwIdx + keyword.length + 1).stripLeading
        // Anonymous given: keyword is immediately followed by `[T]` (type params)
        // or just `:`/`=`.
        if (rest.startsWith("[")) {
          var depth = 1
          var j = 1
          while (j < rest.length && depth > 0) {
            val c = rest.charAt(j)
            if (c == '[') depth += 1
            else if (c == ']') depth -= 1
            j += 1
          }
          rest = rest.substring(j).stripLeading
        } else {
          // Named def: skip the name itself.
          val nameEnd = rest.indexWhere(c => !c.isLetterOrDigit && c != '_')
          if (nameEnd >= 0) rest = rest.substring(nameEnd).stripLeading
        }
        if (rest.startsWith(":") || rest.startsWith("=")) {
          hasParensInSource = false
        }
      }
    }

    // Note: annotated parameters (@unchecked) are handled by visitMethodParameter

    visitDefDefImpl(dd, hasParensInSource, isProcedureSyntax)
  }

  /**
   * Re-parses a procedure syntax body from source. The Scala 3 parser discards the body
   * of procedure syntax methods (replacing it with `_root_.scala.Predef.???`), so we need
   * to re-parse the block with an explicit `=` to recover the AST.
   */
  private def reparseProcedureBody(dd: Trees.DefDef[?]): J.Block = {
    import dotty.tools.dotc.util.SourceFile
    import dotty.tools.dotc.CompilationUnit
    import dotty.tools.dotc.parsing.Parsers

    // Find the body block in original source
    val defEnd = Math.max(0, dd.span.end - offsetAdjustment)
    val remaining = if (cursor < source.length) source.substring(cursor, Math.min(defEnd, source.length)) else ""
    val braceIdx = positionOfNextIn(remaining, "{", 0)
    if (braceIdx < 0) return null

    val braceStart = cursor + braceIdx

    // Find matching closing brace
    val closingBrace = {
      val p = positionOfMatchingClose('{', '}', braceStart + 1)
      if (p >= 0) p else source.length
    }

    // Extract the body block source (including { and })
    val bodyBlockSource = source.substring(braceStart, Math.min(closingBrace + 1, source.length))

    // Re-parse: wrap in a def with explicit `=` so the parser keeps the body
    val wrapperPrefix = "object _B_ { def _m_(): Unit = "
    val wrapperSuffix = " }"
    val wrapper = s"$wrapperPrefix$bodyBlockSource$wrapperSuffix"

    try {
      val sf = SourceFile.virtual("_body_", wrapper)
      val unit = CompilationUnit(sf)
      val bodyParser = new Parsers.Parser(sf)(using ctx.fresh.setCompilationUnit(unit))
      val parsedTree = bodyParser.parse()

      // Navigate: PackageDef -> ModuleDef -> Template -> DefDef -> rhs (Block)
      val blockTree: Trees.Tree[?] = parsedTree match {
        case pd: Trees.PackageDef[?] if pd.stats.nonEmpty =>
          pd.stats.head match {
            case md: untpd.ModuleDef =>
              val body = md.impl.unforcedBody match {
                case list: scala.collection.immutable.List[?] => list.asInstanceOf[List[Trees.Tree[?]]]
                case _ => Nil
              }
              body.collectFirst { case d: Trees.DefDef[?] if d.name.toString == "_m_" => d.rhs }.getOrElse(null)
            case _ => null
          }
        case _ => null
      }

      if (blockTree == null) return null

      // Visit using a sub-visitor: source=bodyBlockSource, offset=wrapperPrefix.length
      val subVisitor = new ScalaTreeVisitor(bodyBlockSource, wrapperPrefix.length, typeMapping)
      val result = subVisitor.visitTree(blockTree)

      // Attach proper prefix space (space before `{` in original source)
      val prefixSpace = if (cursor < braceStart) {
        ScalaSpace.format(source, cursor, braceStart)
      } else Space.EMPTY

      cursor = closingBrace + 1

      result match {
        case block: J.Block =>
          block.withPrefix(prefixSpace)
        case _ => null
      }
    } catch {
      case _: Exception => null
    }
  }

  private def visitDefDefImpl(dd: Trees.DefDef[?], hasParensInSource: Boolean = true, isProcedureSyntax: Boolean = false): J.MethodDeclaration = {
    val leadingAnnotations = new util.ArrayList[J.Annotation]()
    val hasAnnotations = dd.mods != null && dd.mods.annotations.nonEmpty
    val prefix = extractPrefix(dd.span)

    if (hasAnnotations) {
      for (annot <- dd.mods.annotations) {
        // First try the proper AST visitor
        val savedAnnotCursor = cursor
        val annotResult = try { visitTree(annot) } catch { case _: Exception => cursor = savedAnnotCursor; null }

        annotResult match {
          case ann: J.Annotation => leadingAnnotations.add(ann)
          case _ =>
            // Fallback: build annotation from source text
            cursor = savedAnnotCursor // Reset cursor — visitTree may have advanced it incorrectly
            if (annot.span.exists) {
              val annotStart = Math.max(0, annot.span.start - offsetAdjustment)
              val annotEnd = Math.max(0, annot.span.end - offsetAdjustment)
              // Find the @ before the annotation span (it's in the source but might not be in the span)
              val searchStart = Math.max(0, annotStart - 5)
              val atIdx = if (searchStart < source.length) source.lastIndexOf('@', annotStart) else -1
              val fullStart = if (atIdx >= 0 && atIdx >= searchStart) atIdx else annotStart

              // Prefix = space from cursor to @ position
              val annotPrefix = if (cursor < fullStart && fullStart <= source.length) {
                ScalaSpace.format(source, cursor, fullStart)
              } else Space.EMPTY

              // Source = everything from @ to annotation end
              val annotSource = if (fullStart < annotEnd && annotEnd <= source.length) {
                source.substring(fullStart, annotEnd)
              } else ""
              val annotName = if (annotSource.startsWith("@")) annotSource.substring(1) else annotSource

              val annotId = ident(annotName)
              leadingAnnotations.add(new J.Annotation(Tree.randomId(), annotPrefix, Markers.EMPTY, annotId, null))
              cursor = annotEnd
            }
        }
      }
    }

    val isGivenAlias = dd.mods != null && dd.mods.is(dotty.tools.dotc.core.Flags.Given)
    val keyword = if (isGivenAlias) "given" else "def"
    val adjustedEnd = Math.max(0, dd.span.end - offsetAdjustment)
    var modifierText = ""
    var defIndex = -1

    if (cursor <= adjustedEnd && adjustedEnd <= source.length) {
      val sourceSnippet = source.substring(cursor, adjustedEnd)
      defIndex = positionOfNextIn(sourceSnippet, keyword + " ", 0)
      if (defIndex < 0) defIndex = positionOfNextIn(sourceSnippet, keyword + "\n", 0)
      if (defIndex > 0) {
        modifierText = sourceSnippet.substring(0, defIndex)
      }
    }

    val (modifiers, lastModEnd) = extractModifiersFromText(dd.mods, modifierText)

    // Always capture the whitespace between the last modifier (or start) and "def" / "given"
    // as the prefix of a synthetic LanguageExtension modifier so the keyword round-trips.
    if (defIndex >= 0) {
      val defPrefixText = if (lastModEnd <= modifierText.length) modifierText.substring(lastModEnd) else ""
      modifiers.add(new J.Modifier(Tree.randomId(), Space.format(defPrefixText), Markers.EMPTY,
        keyword, J.Modifier.Type.LanguageExtension, Collections.emptyList()))
    }

    val defKeywordPos = if (defIndex >= 0) cursor + defIndex + keyword.length else cursor
    cursor = defKeywordPos

    val methodType = try { methodTypeOfTree(dd) } catch { case _: Exception => null }

    // Auxiliary constructors are `def this(...)` in source but have name `<init>`
    // in the AST. The nameSpan still points at the `this` keyword in source, so
    // render it as `this` rather than leaking the internal `<init>` name.
    val rawName = if (dd.name.toString == "<init>") "this" else dd.name.toString

    val (nameSpace, displayMethodName, methodNameEndCursor) = if (dd.nameSpan.exists) {
      val rawNameStart = Math.max(0, dd.nameSpan.start - offsetAdjustment)
      val rawNameLen = Math.max(0, dd.nameSpan.end - dd.nameSpan.start)
      backtickAwareName(defKeywordPos, rawNameStart, rawNameLen, rawName)
    } else {
      (Space.format(" "), rawName, cursor)
    }

    val name = ident(displayMethodName, nameSpace)

    if (dd.nameSpan.exists) {
      cursor = Math.max(cursor, methodNameEndCursor)
    }

    // Separate type parameter lists from value parameter lists
    val allParamLists = dd.paramss
    val typeParamList = allParamLists.headOption.flatMap { first =>
      if (first.nonEmpty && first.head.isInstanceOf[Trees.TypeDef[?]]) Some(first) else None
    }
    val valueParamLists = if (typeParamList.isDefined) allParamLists.tail else allParamLists

    // Handle type parameters [T, U]
    val typeParameters: J.TypeParameters = if (typeParamList.isDefined) {
      val typeParams = typeParamList.get.collect { case td: Trees.TypeDef[?] => td }
      val searchEnd = Math.min(cursor + 100, source.length)
      val searchText = source.substring(cursor, searchEnd)
      val bracketIdx = positionOfNextIn(searchText, "[", 0)
      if (bracketIdx >= 0) {
        val bracketSpace = if (bracketIdx > 0) Space.format(searchText.substring(0, bracketIdx)) else Space.EMPTY
        cursor = cursor + bracketIdx + 1

        val jTypeParams = new util.ArrayList[JRightPadded[J.TypeParameter]]()
        typeParams.zipWithIndex.foreach { case (tp, idx) =>
          val jtp = visitTypeParameter(tp)
          val isLast = idx == typeParams.size - 1
          val afterParam = if (cursor < source.length) {
            if (!isLast) {
              val s = source.substring(cursor, Math.min(cursor + 200, source.length))
              val commaIdx = positionOfNextIn(s, ",", 0)
              if (commaIdx >= 0) {
                cursor = cursor + commaIdx + 1
                Space.format(s.substring(0, commaIdx))
              } else Space.EMPTY
            } else {
              // Capture whitespace between last type parameter and closing `]`
              val s = source.substring(cursor, Math.min(cursor + 200, source.length))
              val closeIdx = positionOfNextIn(s, "]", 0)
              if (closeIdx >= 0) Space.format(s.substring(0, closeIdx))
              else Space.EMPTY
            }
          } else Space.EMPTY
          jTypeParams.add(new JRightPadded(jtp, afterParam, Markers.EMPTY))
        }

        val afterSearch = source.substring(cursor, Math.min(cursor + 200, source.length))
        val closeBracket = positionOfNextIn(afterSearch, "]", 0)
        if (closeBracket >= 0) {
          cursor = cursor + closeBracket + 1
        }

        new J.TypeParameters(Tree.randomId(), bracketSpace, Markers.EMPTY, Collections.emptyList(), jTypeParams)
      } else null
    } else null

    // Visit a parameter list from source, returning J.Lambda.Parameters and advancing cursor past `)`
    def visitParamListAsLambdaParams(params: List[Trees.ValDef[?]]): J.Lambda.Parameters = {
      val searchEnd = Math.min(cursor + 50, source.length)
      val searchText = source.substring(cursor, searchEnd)
      val parenIdx = positionOfNextIn(searchText, "(", 0)
      val parenSpace = if (parenIdx > 0) Space.format(searchText.substring(0, parenIdx)) else Space.EMPTY
      if (parenIdx >= 0) cursor = cursor + parenIdx + 1

      val jParams = new util.ArrayList[JRightPadded[J]]()
      params.zipWithIndex.foreach { case (vd, idx) =>
        val param = visitMethodParameter(vd)
        val isLast = idx == params.size - 1
        val afterParam = if (!isLast) {
          val paramEnd = Math.max(0, vd.span.end - offsetAdjustment)
          cursor = Math.max(cursor, paramEnd)
          val nextParamStart = Math.max(0, params(idx + 1).span.start - offsetAdjustment)
          if (cursor < nextParamStart && nextParamStart <= source.length) {
            val between = source.substring(cursor, nextParamStart)
            val commaIdx = positionOfNextIn(between, ",", 0)
            if (commaIdx >= 0) {
              val beforeComma = Space.format(between.substring(0, commaIdx))
              cursor = cursor + commaIdx + 1
              beforeComma
            } else Space.EMPTY
          } else Space.EMPTY
        } else {
          // Capture whitespace between last parameter and closing `)` so
          // multi-line parameter lists with `)` on its own line round-trip.
          val paramEnd = Math.max(0, vd.span.end - offsetAdjustment)
          val lookupStart = Math.max(cursor, paramEnd)
          if (lookupStart < source.length) {
            val remaining = source.substring(lookupStart, Math.min(lookupStart + 200, source.length))
            val closeParen = positionOfNextIn(remaining, ")", 0)
            if (closeParen >= 0) Space.format(remaining.substring(0, closeParen))
            else Space.EMPTY
          } else Space.EMPTY
        }
        jParams.add(new JRightPadded(param.asInstanceOf[J], afterParam, Markers.EMPTY))
      }

      // Find closing paren
      val lastParamEnd = if (params.nonEmpty) Math.max(0, params.last.span.end - offsetAdjustment) else cursor
      cursor = Math.max(cursor, lastParamEnd)
      if (cursor < source.length) {
        val remaining = source.substring(cursor, Math.min(cursor + 50, source.length))
        val closeParen = positionOfNextIn(remaining, ")", 0)
        if (closeParen >= 0) cursor = cursor + closeParen + 1
      }

      new J.Lambda.Parameters(Tree.randomId(), parenSpace, Markers.EMPTY, true, jParams)
    }

    // Handle value parameters — first list goes in J.MethodDeclaration.parameters,
    // additional curried lists become nested J.Lambda nodes (built after the body is parsed)
    val curriedParamLists = new util.ArrayList[J.Lambda.Parameters]()
    val parameters: JContainer[Statement] = if (valueParamLists.nonEmpty) {
      val firstList = valueParamLists.head.collect { case vd: Trees.ValDef[?] => vd }
      if (firstList.nonEmpty) {
        // Build first list as JContainer for J.MethodDeclaration
        val searchEnd = Math.min(cursor + 50, source.length)
        val searchText = source.substring(cursor, searchEnd)
        val parenIdx = positionOfNextIn(searchText, "(", 0)
        val parenSpace = if (parenIdx > 0) Space.format(searchText.substring(0, parenIdx)) else Space.EMPTY
        if (parenIdx >= 0) cursor = cursor + parenIdx + 1

        val jParams = new util.ArrayList[JRightPadded[Statement]]()
        firstList.zipWithIndex.foreach { case (vd, idx) =>
          val param = visitMethodParameter(vd)
          val isLast = idx == firstList.size - 1
          val afterParam = if (!isLast) {
            val paramEnd = Math.max(0, vd.span.end - offsetAdjustment)
            cursor = Math.max(cursor, paramEnd)
            val nextParamStart = Math.max(0, firstList(idx + 1).span.start - offsetAdjustment)
            if (cursor < nextParamStart && nextParamStart <= source.length) {
              val between = source.substring(cursor, nextParamStart)
              val commaIdx = positionOfNextIn(between, ",", 0)
              if (commaIdx >= 0) {
                val beforeComma = Space.format(between.substring(0, commaIdx))
                cursor = cursor + commaIdx + 1
                beforeComma
              } else Space.EMPTY
            } else Space.EMPTY
          } else {
            // Capture whitespace between last parameter and closing `)` so
            // multi-line parameter lists with `)` on its own line round-trip.
            val paramEnd = Math.max(0, vd.span.end - offsetAdjustment)
            val lookupStart = Math.max(cursor, paramEnd)
            if (lookupStart < source.length) {
              val remaining = source.substring(lookupStart, Math.min(lookupStart + 200, source.length))
              val closeParen = positionOfNextIn(remaining, ")", 0)
              if (closeParen >= 0) Space.format(remaining.substring(0, closeParen))
              else Space.EMPTY
            } else Space.EMPTY
          }
          jParams.add(new JRightPadded(param.asInstanceOf[Statement], afterParam, Markers.EMPTY))
        }
        val lastParamEnd = if (firstList.nonEmpty) Math.max(0, firstList.last.span.end - offsetAdjustment) else cursor
        cursor = Math.max(cursor, lastParamEnd)
        if (cursor < source.length) {
          val remaining = source.substring(cursor, Math.min(cursor + 50, source.length))
          val closeParen = positionOfNextIn(remaining, ")", 0)
          if (closeParen >= 0) cursor = cursor + closeParen + 1
        }

        // Build additional param lists as J.Lambda.Parameters for later wrapping
        for (extraList <- valueParamLists.tail) {
          val extraParams = extraList.collect { case vd: Trees.ValDef[?] => vd }
          if (extraParams.nonEmpty) {
            curriedParamLists.add(visitParamListAsLambdaParams(extraParams))
          }
        }

        JContainer.build(parenSpace, jParams, Markers.EMPTY)
      } else if (hasParensInSource) {
        // Empty parameter list ()
        val searchEnd = Math.min(cursor + 50, source.length)
        val searchText = source.substring(cursor, searchEnd)
        val parenIdx = positionOfNextIn(searchText, "(", 0)
        val parenSpace = if (parenIdx > 0) Space.format(searchText.substring(0, parenIdx)) else Space.EMPTY
        if (parenIdx >= 0) {
          cursor = cursor + parenIdx + 1
          val afterSearch = source.substring(cursor, Math.min(cursor + 50, source.length))
          val closeParen = positionOfNextIn(afterSearch, ")", 0)
          if (closeParen >= 0) cursor = cursor + closeParen + 1
        }
        JContainer.build(parenSpace, new util.ArrayList[JRightPadded[Statement]](), Markers.EMPTY)
      } else {
        // Parameterless method — mark so printer omits ()
        JContainer.build(Space.EMPTY, new util.ArrayList[JRightPadded[Statement]](),
          Markers.build(Collections.singletonList(new org.openrewrite.scala.marker.OmitBraces(Tree.randomId()))))
      }
    } else if (hasParensInSource) {
      // Empty parameter list ()
      val searchEnd = Math.min(cursor + 50, source.length)
      val searchText = source.substring(cursor, searchEnd)
      val parenIdx = positionOfNextIn(searchText, "(", 0)
      val parenSpace = if (parenIdx > 0) Space.format(searchText.substring(0, parenIdx)) else Space.EMPTY
      if (parenIdx >= 0) {
        cursor = cursor + parenIdx + 1
        val afterSearch = source.substring(cursor, Math.min(cursor + 50, source.length))
        val closeParen = positionOfNextIn(afterSearch, ")", 0)
        if (closeParen >= 0) cursor = cursor + closeParen + 1
      }
      JContainer.build(parenSpace, new util.ArrayList[JRightPadded[Statement]](), Markers.EMPTY)
    } else {
      // Parameterless method — mark so printer omits ()
      JContainer.build(Space.EMPTY, new util.ArrayList[JRightPadded[Statement]](),
        Markers.build(Collections.singletonList(new org.openrewrite.scala.marker.OmitBraces(Tree.randomId()))))
    }

    // Handle return type `: ReturnType` — only if explicitly written in source
    val returnTypeExpression: TypeTree = dd.tpt match {
      case tpt if tpt != untpd.EmptyTree && tpt.span.exists =>
        // Check if there's an explicit colon between params and the body/end
        val searchEnd = if (dd.rhs != untpd.EmptyTree && dd.rhs.span.exists) {
          Math.max(0, dd.rhs.span.start - offsetAdjustment)
        } else {
          Math.max(0, dd.span.end - offsetAdjustment)
        }
        val betweenText = if (cursor < searchEnd && searchEnd <= source.length) {
          source.substring(cursor, searchEnd)
        } else ""
        val colonIdx = positionOfNextIn(betweenText, ":", 0)
        val equalsIdx = positionOfNextIn(betweenText, "=", 0)

        // Only use the type if there's a colon BEFORE the equals sign (explicit type annotation).
        // For function types like `Int => Int`, the first `=` belongs to the `=>` arrow, not the
        // body assignment — guard against picking that up as the body separator.
        val arrowIdx = positionOfNextIn(betweenText, "=>", 0)
        val bodyEqualsIdx = if (equalsIdx == arrowIdx && arrowIdx >= 0) {
          positionOfNextIn(betweenText, "=", arrowIdx + 2)
        } else equalsIdx
        if (colonIdx >= 0 && (bodyEqualsIdx < 0 || colonIdx < bodyEqualsIdx)) {
          val beforeColon = Space.format(betweenText.substring(0, colonIdx))
          cursor = cursor + colonIdx + 1
          val visited = visitTypeTree(tpt)
          if (visited != null && beforeColon != Space.EMPTY) {
            visited.withMarkers(visited.getMarkers.addIfAbsent(
              org.openrewrite.scala.marker.ReturnTypeColonPrefix.create(beforeColon))).asInstanceOf[TypeTree]
          } else {
            visited
          }
        } else {
          null // Inferred type — not written in source
        }
      case _ => null
    }

    // Handle method body
    var beforeEqualsSpace: Space = Space.EMPTY
    val body: J.Block = dd.rhs match {
      case rhs if isProcedureSyntax && rhs.span.isSynthetic =>
        // Procedure syntax: Scala 3 parser replaces body with `_root_.scala.Predef.???`.
        // The actual body exists in source. Re-parse to get AST nodes.
        reparseProcedureBody(dd)
      case rhs if rhs != untpd.EmptyTree && rhs.span.exists =>
        val rhsStart = effectiveStart(rhs)
        var beforeEquals = Space.EMPTY
        if (!isProcedureSyntax && cursor < rhsStart && rhsStart <= source.length) {
          val beforeBody = source.substring(cursor, rhsStart)
          val equalsIdx = positionOfNextIn(beforeBody, "=", 0)
          if (equalsIdx >= 0) {
            beforeEquals = Space.format(beforeBody.substring(0, equalsIdx))
            cursor = cursor + equalsIdx + 1
          }
        }
        beforeEqualsSpace = beforeEquals
        visitTree(rhs) match {
            case block: J.Block => block
            case expr: Expression =>
              // Wrap Expression in S.ExpressionStatement so it can be used as a Statement.
              val stmtExpr: Statement = expr match {
                case s: Statement => s
                case _ => new S.ExpressionStatement(Tree.randomId(), expr)
              }
              val statements = new util.ArrayList[JRightPadded[Statement]]()
              statements.add(JRightPadded.build(stmtExpr))
              new J.Block(Tree.randomId(), beforeEquals,
                Markers.build(Collections.singletonList(new org.openrewrite.scala.marker.OmitBraces(Tree.randomId()))),
                JRightPadded.build(false), statements, Space.EMPTY)
            case stmt: Statement =>
              val statements = new util.ArrayList[JRightPadded[Statement]]()
              statements.add(JRightPadded.build(stmt))
              new J.Block(Tree.randomId(), beforeEquals,
                Markers.build(Collections.singletonList(new org.openrewrite.scala.marker.OmitBraces(Tree.randomId()))),
                JRightPadded.build(false), statements, Space.EMPTY)
            case _ => null
          }
      case _ => null // Abstract method
    }

    // For procedure syntax, cursor is already correctly set by reparseProcedureBody.
    // Don't use dd.span.end because it extends past the actual method body due to synthetic ??? span.
    if (!isProcedureSyntax) {
      updateCursor(dd.span.end)
    }

    // If curried, wrap the body in a lambda chain: innermost lambda gets the actual body,
    // each outer lambda wraps the next. The method body becomes a synthetic block with the chain.
    val isCurried = !curriedParamLists.isEmpty
    val finalBody: J.Block = if (isCurried) {
      // Build lambda chain from inside out: last curried list wraps the actual body,
      // each preceding list wraps the next lambda.
      // For abstract methods (body == null), innermost lambda gets an empty block.
      var innerBody: J = if (body != null) body.asInstanceOf[J]
        else new J.Block(Tree.randomId(), Space.EMPTY,
          Markers.build(Collections.singletonList(new OmitBraces(Tree.randomId()))),
          JRightPadded.build(false), new util.ArrayList[JRightPadded[Statement]](), Space.EMPTY)
      var i = curriedParamLists.size - 1
      while (i >= 0) {
        val lambdaParams = curriedParamLists.get(i)
        // The printer descends through curried lambdas while the OUTER lambda carries a
        // Curried marker. So every wrapper except the innermost needs the marker.
        val lambdaMarkers = if (i < curriedParamLists.size - 1) {
          Markers.build(Collections.singletonList(new Curried(Tree.randomId())))
        } else Markers.EMPTY
        innerBody = new J.Lambda(Tree.randomId(), Space.EMPTY, lambdaMarkers,
          lambdaParams, Space.EMPTY, innerBody, null)
        i -= 1
      }
      // Wrap outermost lambda in a synthetic OmitBraces block (J.MethodDeclaration.body is J.Block)
      val stmts = new util.ArrayList[JRightPadded[Statement]]()
      stmts.add(JRightPadded.build(innerBody.asInstanceOf[Statement]))
      new J.Block(Tree.randomId(), Space.EMPTY,
        Markers.build(Collections.singletonList(new OmitBraces(Tree.randomId()))),
        JRightPadded.build(false), stmts, Space.EMPTY)
    } else body

    // Build method markers
    val markerList = new util.ArrayList[org.openrewrite.marker.Marker]()
    if (isProcedureSyntax) {
      markerList.add(new OmitBraces(Tree.randomId()))
    }
    if (isCurried) {
      markerList.add(new Curried(Tree.randomId()))
    }
    if (beforeEqualsSpace != Space.EMPTY) {
      markerList.add(org.openrewrite.scala.marker.MethodBodyEqualsPrefix.create(beforeEqualsSpace))
    }
    val methodMarkers = if (!markerList.isEmpty) Markers.build(markerList) else Markers.EMPTY

    new J.MethodDeclaration(
      Tree.randomId(),
      prefix,
      methodMarkers,
      leadingAnnotations,
      modifiers,
      typeParameters,
      returnTypeExpression,
      new J.MethodDeclaration.IdentifierWithAnnotations(name, Collections.emptyList()),
      parameters,
      Collections.emptyList(), // dimensionsAfterName
      null, // throws
      finalBody,
      null, // defaultValue
      methodType
    )
  }

  /**
   * Build a J.VariableDeclarations for a class/trait primary-constructor parameter.
   *
   * Constructor params differ from method params in that they may carry `val`/`var`
   * (paramAccessor) modifiers and access modifiers (`private val`, `protected val`,
   * `private[scope] val`, ...) that turn the param into a class field.
   */
  private def visitConstructorParameter(vd: Trees.ValDef[?], isFirstInList: Boolean): J.VariableDeclarations = {
    import dotty.tools.dotc.core.Flags
    val paramStart = Math.max(0, vd.span.start - offsetAdjustment)
    val nameStart = if (vd.nameSpan.exists) Math.max(0, vd.nameSpan.start - offsetAdjustment) else paramStart

    // Detect `using` (Scala 3 context) / `implicit` (Scala 2) on this clause. The keyword
    // appears once before the first param of its clause; consume it from source and surface
    // it as a J.Modifier so the printer can re-emit it.
    val isUsing = vd.mods != null && vd.mods.is(Flags.Given)
    val isScala2Implicit = vd.mods != null && vd.mods.is(Flags.Implicit) && !isUsing
    val paramModifiers = new util.ArrayList[J.Modifier]()

    var modifierConsumed = false
    val prefix: Space = if ((isUsing || isScala2Implicit) && isFirstInList) {
      val keyword = if (isUsing) "using" else "implicit"
      if (cursor < paramStart && paramStart <= source.length) {
        val leading = source.substring(cursor, paramStart)
        val kwIdx = positionOfNextIn(leading, keyword, 0)
        if (kwIdx >= 0) {
          val outerPrefix = if (kwIdx > 0) Space.format(leading.substring(0, kwIdx)) else Space.EMPTY
          paramModifiers.add(new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
            keyword, J.Modifier.Type.LanguageExtension, Collections.emptyList()))
          cursor = cursor + kwIdx + keyword.length
          modifierConsumed = true
          outerPrefix
        } else if (cursor < paramStart && paramStart <= source.length) {
          ScalaSpace.format(source, cursor, paramStart)
        } else Space.EMPTY
      } else Space.EMPTY
    } else if (cursor < paramStart && paramStart <= source.length) {
      ScalaSpace.format(source, cursor, paramStart)
    } else Space.EMPTY
    if (!modifierConsumed) {
      cursor = Math.max(cursor, paramStart)
    }

    // Parameter-level annotations (e.g. @transient val name)
    val paramAnnotations = new util.ArrayList[J.Annotation]()
    if (vd.mods != null && vd.mods.annotations.nonEmpty) {
      for (annot <- vd.mods.annotations) {
        val savedAC = cursor
        try {
          visitTree(annot) match {
            case ann: J.Annotation => paramAnnotations.add(ann)
            case _ => cursor = savedAC
          }
        } catch { case _: Exception => cursor = savedAC }
      }
    }

    // Scan source between the current cursor and the parameter name for modifiers
    // (`val`/`var`, access modifiers, `override`, `final`).
    if (cursor < nameStart && nameStart <= source.length) {
      val modText = source.substring(cursor, nameStart)
      val (mods, consumed) = parseModifierKeywords(modText, constructorParamModifierKeywords)
      paramModifiers.addAll(mods)
      cursor += consumed
    }

    // Anonymous `using` param: dotty synthesizes a name like `x$1` that does not appear in
    // source. Keep the synthesized name in the LST but attach an OmitName marker so the
    // printer suppresses it. Detection: name matches the `x$N` synthesized pattern, or the
    // text between cursor and the type's start position doesn't contain the name.
    val rawParamName = vd.name.toString
    val anonymousParam: Boolean = isUsing && {
      rawParamName.matches("x\\$\\d+") || {
        if (vd.tpt != null && vd.tpt.span.exists) {
          val tStart = Math.max(0, vd.tpt.span.start - offsetAdjustment)
          if (cursor < tStart && tStart <= source.length) {
            !source.substring(cursor, tStart).contains(rawParamName)
          } else true
        } else true
      }
    }

    // Space between modifiers (or annotations) and the parameter name. Use the backtick-aware
    // helper so `case class CC(`type`: String)` keeps both backticks — `vd.name.toString` strips
    // them, and the closing backtick lives one past `vd.nameSpan.end`.
    val (namePrefix, displayName, nameEndCursor) =
      if (vd.nameSpan.exists && !anonymousParam) {
        val rawNameLen = Math.max(0, vd.nameSpan.end - vd.nameSpan.start)
        backtickAwareName(cursor, nameStart, rawNameLen, rawParamName)
      } else {
        (Space.EMPTY, rawParamName, cursor)
      }
    val paramName = ident(displayName, namePrefix, variableTypeOfTree(vd))
    if (vd.nameSpan.exists && !anonymousParam) {
      cursor = Math.max(cursor, nameEndCursor)
    }

    val paramEnd = Math.max(0, vd.span.end - offsetAdjustment)
    var beforeColon: Space = Space.EMPTY
    val typeExpr: TypeTree = if (anonymousParam && vd.tpt != untpd.EmptyTree && vd.tpt.span.exists) {
      // Anonymous `using` param: source is just the type (e.g. `using Executor`). Walk
      // forward to the type span start, then visit the type — no preceding colon.
      // Match the named-param branch by tolerating a null result from visitTypeTree
      // (some type constructs like infix `A =:= B` are still mapped to null here).
      val typeStart = Math.max(0, vd.tpt.span.start - offsetAdjustment)
      val tt = if (cursor < typeStart && typeStart <= source.length) {
        val typePrefix = ScalaSpace.format(source, cursor, typeStart)
        cursor = typeStart
        val visited = visitTypeTree(vd.tpt)
        if (visited != null) visited.withPrefix(typePrefix).asInstanceOf[TypeTree] else null
      } else {
        visitTypeTree(vd.tpt)
      }
      updateCursor(vd.tpt.span.end)
      tt
    } else if (vd.tpt != untpd.EmptyTree && vd.tpt.span.exists) {
      val colonSearchEnd = Math.min(paramEnd, source.length)
      val between = if (cursor < colonSearchEnd) source.substring(cursor, colonSearchEnd) else ""
      val colonIdx = positionOfNextIn(between, ":", 0)
      if (colonIdx >= 0) {
        if (colonIdx > 0) beforeColon = Space.format(between.substring(0, colonIdx))
        cursor = cursor + colonIdx + 1
        val res = visitTypeTree(vd.tpt)
        if (vd.tpt.span.exists) updateCursor(vd.tpt.span.end)
        res
      } else null
    } else null

    // Default value `= expr`
    var initBefore: Space = Space.EMPTY
    val initializer: Expression = if (vd.rhs != untpd.EmptyTree && vd.rhs.span.exists) {
      val rhsStart = effectiveStart(vd.rhs)
      if (cursor < rhsStart) {
        val before = source.substring(cursor, rhsStart)
        val eqIdx = positionOfNextIn(before, "=", 0)
        if (eqIdx >= 0) {
          initBefore = Space.format(before.substring(0, eqIdx))
          cursor = cursor + eqIdx + 1
        }
      }
      visitTree(vd.rhs) match {
        case expr: Expression => expr
        case _ => null
      }
    } else null

    val namedVariableMarkers =
      if (anonymousParam)
        Markers.build(Collections.singletonList(org.openrewrite.scala.marker.OmitName(Tree.randomId())))
      else Markers.EMPTY

    val variable = new J.VariableDeclarations.NamedVariable(
      Tree.randomId(),
      Space.EMPTY,
      namedVariableMarkers,
      paramName,
      Collections.emptyList(),
      if (initializer != null) new JLeftPadded(initBefore, initializer, Markers.EMPTY) else null,
      variableTypeOfTree(vd)
    )

    cursor = Math.max(cursor, paramEnd)

    new J.VariableDeclarations(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      paramAnnotations,
      paramModifiers,
      typeExpr,
      if (beforeColon != Space.EMPTY) beforeColon else null,
      Collections.emptyList(),
      Collections.singletonList(JRightPadded.build(variable))
    )
  }

  private def visitMethodParameter(vd: Trees.ValDef[?]): J = {
    import dotty.tools.dotc.core.Flags
    val paramModifiers = new util.ArrayList[J.Modifier]()
    val isUsing = vd.mods != null && vd.mods.is(Flags.Given)
    val isScala2Implicit = vd.mods != null && vd.mods.is(Flags.Implicit) && !isUsing
    val prefix: Space = if (isScala2Implicit || isUsing) {
      val keyword = if (isUsing) "using" else "implicit"
      val spanStart = Math.max(0, vd.span.start - offsetAdjustment)
      if (cursor < spanStart && spanStart <= source.length) {
        val leading = source.substring(cursor, spanStart)
        val kwIdx = positionOfNextIn(leading, keyword, 0)
        if (kwIdx >= 0) {
          val modPrefix = if (kwIdx > 0) Space.format(leading.substring(0, kwIdx)) else Space.EMPTY
          paramModifiers.add(new J.Modifier(Tree.randomId(), modPrefix, Markers.EMPTY,
            keyword, J.Modifier.Type.LanguageExtension, Collections.emptyList()))
          cursor += kwIdx + keyword.length
          Space.EMPTY
        } else extractPrefix(vd.span)
      } else extractPrefix(vd.span)
    } else extractPrefix(vd.span)
    val paramStart = Math.max(0, vd.span.start - offsetAdjustment)
    val paramEnd = Math.max(0, vd.span.end - offsetAdjustment)
    val paramSource = if (paramStart < paramEnd && paramEnd <= source.length) source.substring(paramStart, paramEnd) else ""
    val hasExplicitType = paramSource.contains(":")

    // Handle parameter-level annotations (@unchecked, etc.)
    val paramAnnotations = new util.ArrayList[J.Annotation]()
    if (vd.mods != null && vd.mods.annotations.nonEmpty) {
      for (annot <- vd.mods.annotations) {
        val savedAC = cursor
        try {
          visitTree(annot) match {
            case ann: J.Annotation => paramAnnotations.add(ann)
            case _ =>
              cursor = savedAC
              if (annot.span.exists) {
                val aStart = Math.max(0, annot.span.start - offsetAdjustment)
                val aEnd = Math.max(0, annot.span.end - offsetAdjustment)
                val aPrefix = if (cursor < aStart && aStart <= source.length) {
                  ScalaSpace.format(source, cursor, aStart)
                } else Space.EMPTY
                val aSource = if (aStart < aEnd && aEnd <= source.length) source.substring(aStart, aEnd) else ""
                val aName = if (aSource.startsWith("@")) aSource.substring(1) else aSource
                paramAnnotations.add(new J.Annotation(Tree.randomId(), aPrefix, Markers.EMPTY,
                  ident(aName), null))
                cursor = aEnd
              }
          }
        } catch {
          case _: Exception => cursor = savedAC
        }
      }
    }

    // Extract space between annotation (if any) and the parameter name, accounting for backticks.
    // For anonymous parameters (synthesized names that don't appear in source — only legal for
    // `using` params in Scala 3), keep the synthesized name in the LST but attach an OmitName
    // marker so the printer suppresses it. Detection: the param's name does not appear in
    // source between the cursor and the type's start position.
    val rawParamName = vd.name.toString
    val nameInSource: Boolean = {
      if (vd.tpt != null && vd.tpt.span.exists) {
        val tStart = Math.max(0, vd.tpt.span.start - offsetAdjustment)
        if (cursor < tStart && tStart <= source.length) {
          source.substring(cursor, tStart).contains(rawParamName)
        } else true
      } else paramSource.contains(rawParamName)
    }
    val anonymousParam: Boolean = isUsing && (rawParamName.matches("x\\$\\d+") || !nameInSource)
    val (namePrefix, displayParamName, paramNameEnd) =
      if (vd.nameSpan.exists && !anonymousParam) {
        val rawNameStart = Math.max(0, vd.nameSpan.start - offsetAdjustment)
        val rawNameLen = Math.max(0, vd.nameSpan.end - vd.nameSpan.start)
        backtickAwareName(cursor, rawNameStart, rawNameLen, rawParamName)
      } else (Space.EMPTY, rawParamName, cursor)

    val paramName = ident(displayParamName, namePrefix, variableTypeOfTree(vd))

    if (vd.nameSpan.exists && !anonymousParam) {
      cursor = Math.max(cursor, paramNameEnd)
    }

    var beforeColon: Space = Space.EMPTY
    var typeExpr: TypeTree = null
    if (anonymousParam && vd.tpt != untpd.EmptyTree && vd.tpt.span.exists) {
      // Anonymous `using` parameter: source is just the type (e.g. `using Ord[T]`).
      // Walk forward to the type span start, then visit the type.
      val typeStart = Math.max(0, vd.tpt.span.start - offsetAdjustment)
      if (cursor < typeStart && typeStart <= source.length) {
        val typePrefix = ScalaSpace.format(source, cursor, typeStart)
        cursor = typeStart
        typeExpr = visitTypeTree(vd.tpt).withPrefix(typePrefix).asInstanceOf[TypeTree]
        updateCursor(vd.tpt.span.end)
      } else {
        typeExpr = visitTypeTree(vd.tpt)
        updateCursor(vd.tpt.span.end)
      }
    } else if (hasExplicitType && vd.tpt != untpd.EmptyTree) {
      val colonSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 30, source.length)) else ""
      val colonIdx = positionOfNextIn(colonSearch, ":", 0)
      if (colonIdx >= 0) {
        if (colonIdx > 0) beforeColon = Space.format(colonSearch.substring(0, colonIdx))
        cursor = cursor + colonIdx + 1
      }

      typeExpr = visitTypeTree(vd.tpt)
      if (vd.tpt.span.exists) {
        updateCursor(vd.tpt.span.end)
      }
    }

    // Handle default value
    var initBefore: Space = Space.EMPTY
    val initializer: Expression = if (vd.rhs != untpd.EmptyTree && vd.rhs.span.exists) {
      val rhsStart = effectiveStart(vd.rhs)
      if (cursor < rhsStart) {
        val before = source.substring(cursor, rhsStart)
        val eqIdx = positionOfNextIn(before, "=", 0)
        if (eqIdx >= 0) {
          initBefore = Space.format(before.substring(0, eqIdx))
          cursor = cursor + eqIdx + 1
        }
      }
      visitTree(vd.rhs) match {
        case expr: Expression => expr
        case _ => null
      }
    } else null

    val namedVariableMarkers =
      if (anonymousParam)
        Markers.build(Collections.singletonList(org.openrewrite.scala.marker.OmitName(Tree.randomId())))
      else Markers.EMPTY

    val variable = new J.VariableDeclarations.NamedVariable(
      Tree.randomId(),
      Space.EMPTY,
      namedVariableMarkers,
      paramName,
      Collections.emptyList(),
      if (initializer != null) new JLeftPadded(initBefore, initializer, Markers.EMPTY) else null,
      variableTypeOfTree(vd)
    )

    updateCursor(vd.span.end)

    new J.VariableDeclarations(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      paramAnnotations,
      paramModifiers,
      typeExpr,
      if (beforeColon != Space.EMPTY) beforeColon else null,
      Collections.emptyList(),
      Collections.singletonList(JRightPadded.build(variable))
    )
  }

  private def visitTryTree(tryTree: Trees.Try[?]): J = {
    visitTryImpl(tryTree)
  }

  private def visitParsedTry(parsedTry: untpd.ParsedTry): J = {
    // ParsedTry is the untpd version: has expr, handler (a Match), finalizer
    // Extract cases from the handler Match and delegate to common try logic
    val prefix = extractPrefix(parsedTry.span)

    // Advance past "try" keyword
    val tryStart = Math.max(0, parsedTry.span.start - offsetAdjustment)
    if (cursor <= tryStart + 3) {
      val searchText = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 20, source.length)) else ""
      val tryIdx = positionOfNextIn(searchText, "try", 0)
      if (tryIdx >= 0) cursor = cursor + tryIdx + 3
    }

    // Visit the try body
    val omitBracesMarkers = Markers.build(Collections.singletonList(new OmitBraces(Tree.randomId())))
    val body = visitTree(parsedTry.expr) match {
      case block: J.Block => block
      case stmt: Statement =>
        val stmts = new util.ArrayList[JRightPadded[Statement]]()
        stmts.add(JRightPadded.build(stmt))
        new J.Block(Tree.randomId(), Space.EMPTY, omitBracesMarkers, JRightPadded.build(false), stmts, Space.EMPTY)
      case expr: Expression =>
        val stmts = new util.ArrayList[JRightPadded[Statement]]()
        stmts.add(JRightPadded.build(new S.ExpressionStatement(Tree.randomId(), expr)))
        new J.Block(Tree.randomId(), Space.EMPTY, omitBracesMarkers, JRightPadded.build(false), stmts, Space.EMPTY)
      case _ => visitUnknown(parsedTry)
    }

    // Handle catch handler
      val catches = new util.ArrayList[J.Try.Catch]()
    var catchHasBraces = true
    if (!parsedTry.handler.isEmpty && parsedTry.handler.span.exists) {
      val catchAbs = positionOfNext("catch")
      val catchPrefix = if (catchAbs > cursor) ScalaSpace.format(source, cursor, catchAbs) else Space.EMPTY
      if (catchAbs >= cursor) cursor = catchAbs + 5
      // Scala 3 `catch case ... =>` (no braces) — only consume `{` if it appears before the first `case`.
      val braceSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 200, source.length)) else ""
      val braceIdx = positionOfNextIn(braceSearch, "{", 0)
      val firstCaseIdx = positionOfNextIn(braceSearch, "case", 0)
      catchHasBraces = braceIdx >= 0 && (firstCaseIdx < 0 || braceIdx < firstCaseIdx)
      val catchBraceSpace = if (catchHasBraces && braceIdx > 0) Space.format(braceSearch.substring(0, braceIdx)) else Space.EMPTY
      if (catchHasBraces) cursor = cursor + braceIdx + 1

      // The handler should be a Match tree containing the case defs
      val cases: List[Trees.CaseDef[?]] = parsedTry.handler match {
        case matchTree: Trees.Match[?] => matchTree.cases
        case _ => Nil
      }

      for (caseDef <- cases) {
        val casePrefix = extractPrefix(caseDef.span)
        val caseStart = Math.max(0, caseDef.span.start - offsetAdjustment)
        if (caseStart >= cursor) cursor = caseStart
        val caseSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 20, source.length)) else ""
        val caseKwIdx = positionOfNextIn(caseSearch, "case", 0)
        if (caseKwIdx >= 0) cursor = cursor + caseKwIdx + 4

        val arrowSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 200, source.length)) else ""
        val arrowIdx = positionOfNextIn(arrowSearch, "=>", 0)

        // Extract param name and type from the catch pattern without disturbing cursor.
        // Handles: `e: Exception`, `_: A | _: B` (Alternative), `_` (wildcard)
        val (paramName, paramType): (String, TypeTree) = caseDef.pat match {
          case bind: Trees.Bind[?] => bind.body match {
            case typed: Trees.Typed[?] =>
              val tpeName = typed.tpt match { case id: Trees.Ident[?] => id.name.toString; case sel: Trees.Select[?] => extractSource(sel.span); case _ => null }
              (bind.name.toString, if (tpeName != null) ident(tpeName, Space.format(" ")) else null)
            case _ => (bind.name.toString, null)
          }
          case typed: Trees.Typed[?] =>
            val name = typed.expr match { case id: Trees.Ident[?] => id.name.toString; case _ => "_" }
            val tpeName = typed.tpt match { case id: Trees.Ident[?] => id.name.toString; case sel: Trees.Select[?] => extractSource(sel.span); case _ => null }
            (name, if (tpeName != null) ident(tpeName, Space.format(" ")) else null)
          case alt: Trees.Alternative[?] =>
            // Multi-pattern: `_: A | _: B` — preserve full source as the "name", no separate type
            val patSource = extractSource(caseDef.pat.span)
            (patSource, null)
          case _ =>
            // Any other pattern (e.g. UnApply extractor `NonFatal(e)`, tuple, literal) —
            // preserve the full source text so the binding/extractor isn't lost on print.
            val patSource = if (caseDef.pat.span.exists) extractSource(caseDef.pat.span) else ""
            (if (patSource.nonEmpty) patSource else "_", null)
        }
        // Capture the whitespace between the param name and `:` (e.g. `e : Exception`).
        val beforeColon: Space = if (paramType != null && caseDef.pat.span.exists) {
          val patStart = Math.max(0, caseDef.pat.span.start - offsetAdjustment)
          val patEnd = Math.max(0, caseDef.pat.span.end - offsetAdjustment)
          val patSrc = if (patStart < patEnd && patEnd <= source.length) source.substring(patStart, patEnd) else ""
          val colonAt = positionOfNextIn(patSrc, ":", 0)
          val nameAt = if (paramName.nonEmpty) positionOfNextIn(patSrc, paramName, 0) else -1
          if (colonAt > 0 && nameAt >= 0 && nameAt + paramName.length <= colonAt) {
            Space.format(patSrc.substring(nameAt + paramName.length, colonAt))
          } else Space.EMPTY
        } else Space.EMPTY
        updateCursor(caseDef.pat.span.end)
        val arrowAbs = positionOfNext("=>", cursor)
        val spaceBeforeArrow = if (arrowAbs >= cursor) ScalaSpace.format(source, cursor, arrowAbs) else Space.EMPTY
        if (arrowIdx >= 0 && arrowAbs >= 0) cursor = arrowAbs + 2

        val paramId = ident(paramName, Space.format(" "))
        val namedVar = new J.VariableDeclarations.NamedVariable(Tree.randomId(), Space.EMPTY, Markers.EMPTY, paramId, Collections.emptyList(), null, null)
        val varDecl = new J.VariableDeclarations(Tree.randomId(), casePrefix, Markers.EMPTY,
          Collections.emptyList(), Collections.emptyList(), paramType,
          if (beforeColon != Space.EMPTY) beforeColon else null,
          Collections.emptyList(),
          Collections.singletonList(JRightPadded.build(namedVar)))
        val controlPrefix = if (catches.isEmpty) catchBraceSpace else Space.EMPTY
        val controlParens = new J.ControlParentheses[J.VariableDeclarations](Tree.randomId(), controlPrefix, Markers.EMPTY, JRightPadded.build(varDecl).withAfter(spaceBeforeArrow))

        val caseBodyOmitBraces = Markers.build(Collections.singletonList(new OmitBraces(Tree.randomId())))
        val caseBody = visitTree(caseDef.body) match {
          case block: J.Block if block.getMarkers.findFirst(classOf[OmitBraces]).isPresent => block
          case block: J.Block =>
            // Block has its own braces (e.g. `case _ => { stmts }`). Wrap it in an
            // OmitBraces shell so the inner block keeps its own end space and the
            // shell can carry the catch's trailing space (before the outer `}`).
            val s = new util.ArrayList[JRightPadded[Statement]](); s.add(JRightPadded.build(block))
            new J.Block(Tree.randomId(), Space.EMPTY, caseBodyOmitBraces,
              JRightPadded.build(false), s, Space.EMPTY)
          case stmt: Statement =>
            val s = new util.ArrayList[JRightPadded[Statement]](); s.add(JRightPadded.build(stmt))
            new J.Block(Tree.randomId(), Space.EMPTY, caseBodyOmitBraces, JRightPadded.build(false), s, Space.EMPTY)
          case expr: Expression =>
            val s = new util.ArrayList[JRightPadded[Statement]](); s.add(JRightPadded.build(new S.ExpressionStatement(Tree.randomId(), expr)))
            new J.Block(Tree.randomId(), Space.EMPTY, caseBodyOmitBraces, JRightPadded.build(false), s, Space.EMPTY)
          case _ => new J.Block(Tree.randomId(), Space.EMPTY, caseBodyOmitBraces, JRightPadded.build(false), new util.ArrayList(), Space.EMPTY)
        }
        updateCursor(caseDef.span.end)
        val thisCatchPrefix = if (catches.isEmpty) catchPrefix else casePrefix
        catches.add(new J.Try.Catch(Tree.randomId(), thisCatchPrefix, Markers.EMPTY, controlParens, caseBody))
      }
      // Extract end space before closing '}'  and set it on the last catch body
      if (catchHasBraces && cursor < source.length) {
        val ci = positionOfNext("}")
        if (ci >= cursor) {
          val endSpace = if (ci > cursor) ScalaSpace.format(source, cursor, ci) else Space.EMPTY
          // Update last catch body's end space
          if (!catches.isEmpty) {
            val lastCatch = catches.get(catches.size - 1)
            val updatedBody = lastCatch.getBody.withEnd(endSpace)
            catches.set(catches.size - 1, lastCatch.withBody(updatedBody))
          }
          cursor = ci + 1
        }
      }
    }

    // Handle finalizer
    val finallyBlock: JLeftPadded[J.Block] = if (!parsedTry.finalizer.isEmpty && parsedTry.finalizer.span.exists) {
      val finallyAbs = positionOfNext("finally")
      val fSpace = if (finallyAbs > cursor) ScalaSpace.format(source, cursor, finallyAbs) else Space.EMPTY
      if (finallyAbs >= cursor) cursor = finallyAbs + 7
      val parsedFinallyOmitBraces = Markers.build(Collections.singletonList(new OmitBraces(Tree.randomId())))
      val fb = visitTree(parsedTry.finalizer) match {
        case block: J.Block => block
        case stmt: Statement =>
          val s = new util.ArrayList[JRightPadded[Statement]](); s.add(JRightPadded.build(stmt))
          new J.Block(Tree.randomId(), Space.EMPTY, parsedFinallyOmitBraces, JRightPadded.build(false), s, Space.EMPTY)
        case expr: Expression =>
          val s = new util.ArrayList[JRightPadded[Statement]](); s.add(JRightPadded.build(new S.ExpressionStatement(Tree.randomId(), expr)))
          new J.Block(Tree.randomId(), Space.EMPTY, parsedFinallyOmitBraces, JRightPadded.build(false), s, Space.EMPTY)
        case _ => null
      }
      if (fb != null) JLeftPadded.build(fb).withBefore(fSpace) else null
    } else null

    updateCursor(parsedTry.span.end)
    val tryMarkers = if (catchHasBraces) Markers.EMPTY
      else Markers.build(Collections.singletonList(new IndentedSyntax(Tree.randomId())))
    new J.Try(Tree.randomId(), prefix, tryMarkers, null, body, catches, finallyBlock)
  }

  private def visitTryImpl(tryTree: Trees.Try[?]): J.Try = {
    val prefix = extractPrefix(tryTree.span)
    val tryStart = Math.max(0, tryTree.span.start - offsetAdjustment)
    if (tryStart >= cursor && tryStart + 3 <= source.length) cursor = tryStart + 3

    val omitBracesMarkers = Markers.build(Collections.singletonList(new OmitBraces(Tree.randomId())))
    val body = visitTree(tryTree.expr) match {
      case block: J.Block => block
      case stmt: Statement =>
        val stmts = new util.ArrayList[JRightPadded[Statement]]()
        stmts.add(JRightPadded.build(stmt))
        new J.Block(Tree.randomId(), Space.EMPTY, omitBracesMarkers, JRightPadded.build(false), stmts, Space.EMPTY)
      case expr: Expression =>
        val stmts = new util.ArrayList[JRightPadded[Statement]]()
        stmts.add(JRightPadded.build(new S.ExpressionStatement(Tree.randomId(), expr)))
        new J.Block(Tree.randomId(), Space.EMPTY, omitBracesMarkers, JRightPadded.build(false), stmts, Space.EMPTY)
      case _ => return visitUnknown(tryTree).asInstanceOf[J.Try]
    }

    val catches = new util.ArrayList[J.Try.Catch]()
    var catchHasBraces = true
    if (tryTree.cases.nonEmpty) {
      // Extract space before "catch" keyword — this becomes the first Catch's prefix
      val catchAbs = positionOfNext("catch")
      val catchPrefix = if (catchAbs > cursor) ScalaSpace.format(source, cursor, catchAbs) else Space.EMPTY
      if (catchAbs >= cursor) cursor = catchAbs + 5
      // Scala 3 `catch case ... =>` (no braces) — only consume `{` if it appears before the first `case`.
      val braceSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 200, source.length)) else ""
      val braceIdx = positionOfNextIn(braceSearch, "{", 0)
      val firstCaseIdx = positionOfNextIn(braceSearch, "case", 0)
      catchHasBraces = braceIdx >= 0 && (firstCaseIdx < 0 || braceIdx < firstCaseIdx)
      val catchBraceSpace = if (catchHasBraces && braceIdx > 0) Space.format(braceSearch.substring(0, braceIdx)) else Space.EMPTY
      if (catchHasBraces) cursor = cursor + braceIdx + 1

      for (caseDef <- tryTree.cases) {
        val casePrefix = extractPrefix(caseDef.span)
        val caseStart = Math.max(0, caseDef.span.start - offsetAdjustment)
        if (caseStart >= cursor) cursor = caseStart
        val caseSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 20, source.length)) else ""
        val caseKwIdx = positionOfNextIn(caseSearch, "case", 0)
        if (caseKwIdx >= 0) cursor = cursor + caseKwIdx + 4

        val arrowSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 200, source.length)) else ""
        val arrowIdx = positionOfNextIn(arrowSearch, "=>", 0)

        // Extract param name and type from the catch pattern without disturbing cursor.
        // Handles: `e: Exception`, `_: A | _: B` (Alternative), `_` (wildcard)
        val (paramName, paramType): (String, TypeTree) = caseDef.pat match {
          case bind: Trees.Bind[?] => bind.body match {
            case typed: Trees.Typed[?] =>
              val tpeName = typed.tpt match { case id: Trees.Ident[?] => id.name.toString; case sel: Trees.Select[?] => extractSource(sel.span); case _ => null }
              (bind.name.toString, if (tpeName != null) ident(tpeName, Space.format(" ")) else null)
            case _ => (bind.name.toString, null)
          }
          case typed: Trees.Typed[?] =>
            val name = typed.expr match { case id: Trees.Ident[?] => id.name.toString; case _ => "_" }
            val tpeName = typed.tpt match { case id: Trees.Ident[?] => id.name.toString; case sel: Trees.Select[?] => extractSource(sel.span); case _ => null }
            (name, if (tpeName != null) ident(tpeName, Space.format(" ")) else null)
          case alt: Trees.Alternative[?] =>
            // Multi-pattern: `_: A | _: B` — preserve full source as the "name", no separate type
            val patSource = extractSource(caseDef.pat.span)
            (patSource, null)
          case _ =>
            // Any other pattern (e.g. UnApply extractor `NonFatal(e)`, tuple, literal) —
            // preserve the full source text so the binding/extractor isn't lost on print.
            val patSource = if (caseDef.pat.span.exists) extractSource(caseDef.pat.span) else ""
            (if (patSource.nonEmpty) patSource else "_", null)
        }
        // Capture the whitespace between the param name and `:` (e.g. `e : Exception`).
        val beforeColon: Space = if (paramType != null && caseDef.pat.span.exists) {
          val patStart = Math.max(0, caseDef.pat.span.start - offsetAdjustment)
          val patEnd = Math.max(0, caseDef.pat.span.end - offsetAdjustment)
          val patSrc = if (patStart < patEnd && patEnd <= source.length) source.substring(patStart, patEnd) else ""
          val colonAt = positionOfNextIn(patSrc, ":", 0)
          val nameAt = if (paramName.nonEmpty) positionOfNextIn(patSrc, paramName, 0) else -1
          if (colonAt > 0 && nameAt >= 0 && nameAt + paramName.length <= colonAt) {
            Space.format(patSrc.substring(nameAt + paramName.length, colonAt))
          } else Space.EMPTY
        } else Space.EMPTY
        updateCursor(caseDef.pat.span.end)
        val arrowAbs = positionOfNext("=>", cursor)
        val spaceBeforeArrow = if (arrowAbs >= cursor) ScalaSpace.format(source, cursor, arrowAbs) else Space.EMPTY
        if (arrowIdx >= 0 && arrowAbs >= 0) cursor = arrowAbs + 2

        val paramId = ident(paramName, Space.format(" "))
        val namedVar = new J.VariableDeclarations.NamedVariable(Tree.randomId(), Space.EMPTY, Markers.EMPTY, paramId, Collections.emptyList(), null, null)
        val varDecl = new J.VariableDeclarations(Tree.randomId(), casePrefix, Markers.EMPTY,
          Collections.emptyList(), Collections.emptyList(), paramType,
          if (beforeColon != Space.EMPTY) beforeColon else null,
          Collections.emptyList(),
          Collections.singletonList(JRightPadded.build(namedVar)))
        val controlPrefix = if (catches.isEmpty) catchBraceSpace else Space.EMPTY
        val controlParens = new J.ControlParentheses[J.VariableDeclarations](Tree.randomId(), controlPrefix, Markers.EMPTY, JRightPadded.build(varDecl).withAfter(spaceBeforeArrow))

        val caseBodyOmitBraces = Markers.build(Collections.singletonList(new OmitBraces(Tree.randomId())))
        val caseBody = visitTree(caseDef.body) match {
          case block: J.Block if block.getMarkers.findFirst(classOf[OmitBraces]).isPresent => block
          case block: J.Block =>
            val s = new util.ArrayList[JRightPadded[Statement]](); s.add(JRightPadded.build(block))
            new J.Block(Tree.randomId(), Space.EMPTY, caseBodyOmitBraces,
              JRightPadded.build(false), s, Space.EMPTY)
          case stmt: Statement =>
            val s = new util.ArrayList[JRightPadded[Statement]](); s.add(JRightPadded.build(stmt))
            new J.Block(Tree.randomId(), Space.EMPTY, caseBodyOmitBraces, JRightPadded.build(false), s, Space.EMPTY)
          case expr: Expression =>
            val s = new util.ArrayList[JRightPadded[Statement]](); s.add(JRightPadded.build(new S.ExpressionStatement(Tree.randomId(), expr)))
            new J.Block(Tree.randomId(), Space.EMPTY, caseBodyOmitBraces, JRightPadded.build(false), s, Space.EMPTY)
          case _ => new J.Block(Tree.randomId(), Space.EMPTY, caseBodyOmitBraces, JRightPadded.build(false), new util.ArrayList(), Space.EMPTY)
        }
        updateCursor(caseDef.span.end)
        // First catch gets catchPrefix (space before "catch"); subsequent get casePrefix
        val thisCatchPrefix = if (catches.isEmpty) catchPrefix else casePrefix
        catches.add(new J.Try.Catch(Tree.randomId(), thisCatchPrefix, Markers.EMPTY, controlParens, caseBody))
      }
      // Extract end space before closing '}' and set it on the last catch body
      if (catchHasBraces && cursor < source.length) {
        val ci = positionOfNext("}")
        if (ci >= cursor) {
          val endSpace = if (ci > cursor) ScalaSpace.format(source, cursor, ci) else Space.EMPTY
          if (!catches.isEmpty) {
            val lastCatch = catches.get(catches.size - 1)
            val updatedBody = lastCatch.getBody.withEnd(endSpace)
            catches.set(catches.size - 1, lastCatch.withBody(updatedBody))
          }
          cursor = ci + 1
        }
      }
    }

    val finallyBlock: JLeftPadded[J.Block] = if (!tryTree.finalizer.isEmpty && tryTree.finalizer.span.exists) {
      val finallyAbs = positionOfNext("finally")
      val fSpace = if (finallyAbs > cursor) ScalaSpace.format(source, cursor, finallyAbs) else Space.EMPTY
      if (finallyAbs >= cursor) cursor = finallyAbs + 7
      val finallyOmitBraces = Markers.build(Collections.singletonList(new OmitBraces(Tree.randomId())))
      val fb = visitTree(tryTree.finalizer) match {
        case block: J.Block => block
        case stmt: Statement =>
          val s = new util.ArrayList[JRightPadded[Statement]](); s.add(JRightPadded.build(stmt))
          new J.Block(Tree.randomId(), Space.EMPTY, finallyOmitBraces, JRightPadded.build(false), s, Space.EMPTY)
        case expr: Expression =>
          val s = new util.ArrayList[JRightPadded[Statement]](); s.add(JRightPadded.build(new S.ExpressionStatement(Tree.randomId(), expr)))
          new J.Block(Tree.randomId(), Space.EMPTY, finallyOmitBraces, JRightPadded.build(false), s, Space.EMPTY)
        case _ => null
      }
      if (fb != null) JLeftPadded.build(fb).withBefore(fSpace) else null
    } else null

    updateCursor(tryTree.span.end)
    val tryMarkers = if (catchHasBraces) Markers.EMPTY
      else Markers.build(Collections.singletonList(new IndentedSyntax(Tree.randomId())))
    new J.Try(Tree.randomId(), prefix, tryMarkers, null, body, catches, finallyBlock)
  }

  private def visitMatchTree(matchTree: Trees.Match[?]): J = {
    val savedCursor = cursor
    visitMatchImpl(matchTree)
  }

  private def visitMatchImpl(matchTree: Trees.Match[?]): J = {
    val prefix = extractPrefix(matchTree.span)

    // Partial-function literal: either `{ case pat => ... }` (brace form) or an
    // indented form following a colon-arg call like `foldLeft(0):\n  case ...`.
    // Both have a synthetic match selector with NoSpan.
    if (!matchTree.selector.span.exists) {
      val bs = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 20, source.length)) else ""
      val bi = positionOfNextIn(bs, "{", 0)
      val ci = positionOfNextIn(bs, "case", 0)
      val isBraceForm = bi >= 0 && (ci < 0 || bi < ci)
      if (isBraceForm) cursor = cursor + bi + 1
      val casesBlock = buildCasesBlock(matchTree, isBraceForm)
      updateCursor(matchTree.span.end)
      val parameters = new J.Lambda.Parameters(
        Tree.randomId(), Space.EMPTY, Markers.EMPTY, false, Collections.emptyList())
      val lambdaMarkers = new util.ArrayList[org.openrewrite.marker.Marker]()
      lambdaMarkers.add(new PartialFunctionLiteral(Tree.randomId()))
      if (!isBraceForm) lambdaMarkers.add(new IndentedSyntax(Tree.randomId()))
      return new J.Lambda(
        Tree.randomId(), prefix,
        Markers.build(lambdaMarkers),
        parameters, Space.EMPTY, casesBlock, null)
    }

    // `x match { ... }` — the selector can itself be a compound expression (if/block/match).
    // Wrap non-Expression J via StatementExpression so chained expressions parse.
    val selector = visitTree(matchTree.selector) match {
      case expr: Expression => expr
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => return visitUnknown(matchTree)
    }

    val ms = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 30, source.length)) else ""
    val mi = positionOfNextIn(ms, "match", 0)
    val matchKeywordSpace = if (mi > 0) Space.format(ms.substring(0, mi)) else Space.EMPTY
    if (mi >= 0) cursor = cursor + mi + 5
    // Scala 3 `x match\n  case ...` has no `{` before the cases — detect that form.
    val bs = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 200, source.length)) else ""
    val braceIdx = positionOfNextIn(bs, "{", 0)
    val caseIdx = positionOfNextIn(bs, "case", 0)
    val isBraceForm = braceIdx >= 0 && (caseIdx < 0 || braceIdx < caseIdx)
    val matchBraceSpace = if (isBraceForm && braceIdx > 0) Space.format(bs.substring(0, braceIdx)) else Space.EMPTY
    if (isBraceForm) cursor = cursor + braceIdx + 1

    val casesBlock = buildCasesBlock(matchTree, isBraceForm).withPrefix(matchBraceSpace)
    updateCursor(matchTree.span.end)
    val selectorParens = new J.ControlParentheses[Expression](Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(selector).withAfter(matchKeywordSpace))
    val markers = if (isBraceForm) Markers.EMPTY
      else Markers.build(Collections.singletonList(new IndentedSyntax(Tree.randomId())))
    new J.Switch(Tree.randomId(), prefix, markers, selectorParens, casesBlock)
  }

  private def buildCasesBlock(matchTree: Trees.Match[?], hasClosingBrace: Boolean = true): J.Block = {
    val caseStatements = new util.ArrayList[JRightPadded[Statement]]()
    for (caseDef <- matchTree.cases) {
      val casePrefix = extractPrefix(caseDef.span)
      val cs = Math.max(0, caseDef.span.start - offsetAdjustment); if (cs >= cursor) cursor = cs
      val ck = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 20, source.length)) else ""
      val cki = positionOfNextIn(ck, "case", 0); if (cki >= 0) cursor = cursor + cki + 4

      val patternJ = visitTree(caseDef.pat) match { case j: J => j; case _ => ident("_", Space.format(" ")) }

      // Handle guard: `case x if condition =>`
      // Store space-before-if in label's after space so the printer can emit it.
      // Store space between guard expression end and `=>` in the statements
      // container's `before` space (matching the Java printer's convention).
      var guard: Expression = null
      var labelAfter = Space.EMPTY
      var guardArrowSpace = Space.EMPTY
      if (!caseDef.guard.isEmpty && caseDef.guard.span.exists) {
        val ifPos = positionOfNext("if")
        if (ifPos > cursor) labelAfter = ScalaSpace.format(source, cursor, ifPos)
        if (ifPos >= 0) cursor = ifPos + 2  // past "if"
        val guardResult = visitTree(caseDef.guard)
        guardResult match {
          case expr: Expression => guard = expr
          case j: J => guard = new S.StatementExpression(Tree.randomId(), j)
          case _ =>
        }
        val arrowPos = positionOfNext("=>", cursor)
        if (arrowPos >= cursor) guardArrowSpace = ScalaSpace.format(source, cursor, arrowPos)
      } else {
        val arrowPos = positionOfNext("=>", cursor)
        if (arrowPos >= cursor) labelAfter = ScalaSpace.format(source, cursor, arrowPos)
      }
      val labels = new util.ArrayList[JRightPadded[J]]()
      labels.add(JRightPadded.build(patternJ).withAfter(labelAfter))

      val as = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 200, source.length)) else ""
      val ai = positionOfNextIn(as, "=>", 0); if (ai >= 0) { val aa = positionOfNext("=>", cursor); if (aa >= 0) cursor = aa + 2 }

      val caseBodyJ = visitTree(caseDef.body) match { case j: J => JRightPadded.build(j); case _ => null }
      // Compute the end of the body's actual content from the AST (not from
      // the cursor, which may have overshot when Dotty's body span included
      // the `;` or part of the next case).
      val bodyContentEnd: Int = caseDef.body match {
        case b: Trees.Block[?] =>
          if (!b.expr.isEmpty && b.expr.span.exists) Math.max(0, b.expr.span.end - offsetAdjustment)
          else if (b.stats.nonEmpty && b.stats.last.span.exists) Math.max(0, b.stats.last.span.end - offsetAdjustment)
          else if (b.span.exists) Math.max(0, b.span.end - offsetAdjustment)
          else cursor
        case t if t.span.exists => Math.max(0, t.span.end - offsetAdjustment)
        case _ => cursor
      }
      updateCursor(caseDef.span.end)

      // If the next non-whitespace token immediately after the body content is
      // `;`, preserve it via a Semicolon marker on the JRightPadded wrapping
      // the case (e.g. `case a => x; case b => y`).
      var caseRpMarkers = Markers.EMPTY
      val caseRpAfter = Space.EMPTY
      val nextNonWs = indexOfNextNonWhitespace(bodyContentEnd)
      if (nextNonWs < source.length && source.charAt(nextNonWs) == ';') {
        caseRpMarkers = Markers.EMPTY.add(new Semicolon(Tree.randomId()))
        cursor = nextNonWs + 1
      }

      val statementsContainer: JContainer[Statement] =
        JContainer.build(guardArrowSpace, java.util.Collections.emptyList[JRightPadded[Statement]](), Markers.EMPTY)
      val jCase = new J.Case(Tree.randomId(), casePrefix, Markers.EMPTY, J.Case.Type.Rule,
        null, null, JContainer.build(Space.EMPTY, labels, Markers.EMPTY), guard, statementsContainer, caseBodyJ)
      caseStatements.add(new JRightPadded[Statement](jCase.asInstanceOf[Statement], caseRpAfter, caseRpMarkers))
    }

    // Extract space before closing brace of match block (or up to span end for indented form)
    val matchEnd = Math.max(0, matchTree.span.end - offsetAdjustment)
    var matchEndSpace = Space.EMPTY
    if (cursor < matchEnd && matchEnd <= source.length) {
      val remaining = source.substring(cursor, matchEnd)
      if (hasClosingBrace) {
        val closeIdx = remaining.lastIndexOf('}')
        if (closeIdx > 0) {
          matchEndSpace = Space.format(remaining.substring(0, closeIdx))
        }
      }
    }
    new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), caseStatements, matchEndSpace)
  }

  private def visitThis(thisTree: Trees.This[?]): J.Identifier = {
    val prefix = extractPrefix(thisTree.span)
    updateCursor(thisTree.span.end)
    ident("this", prefix, typeFor(thisTree.span))
  }

  private def visitSuper(superTree: Trees.Super[?]): J = {
    val prefix = extractPrefix(superTree.span)
    val mix = superTree.mix
    val hasMix = mix != null && !mix.isEmpty
    // qual is typically a This — its qual field is the outer Ident if any, e.g. `Outer.super`
    val outerName: String = superTree.qual match {
      case t: Trees.This[?] if t.qual != null && !t.qual.isEmpty =>
        t.qual.name.toString
      case _ => null
    }

    if (!hasMix && outerName == null) {
      // Plain `super` — model as J.Identifier so the printer round-trips simply.
      updateCursor(superTree.span.end)
      return new J.Identifier(Tree.randomId(), prefix, Markers.EMPTY, Collections.emptyList(),
        "super", typeFor(superTree.span), null)
    }

    val qualifierIdent: J.Identifier = if (outerName != null) {
      new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, Collections.emptyList(),
        outerName, null, null)
    } else null

    val mixIdent: J.Identifier = if (hasMix) {
      new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, Collections.emptyList(),
        mix.name.toString, null, null)
    } else null

    updateCursor(superTree.span.end)
    new S.QualifiedSuper(Tree.randomId(), prefix, Markers.EMPTY,
      qualifierIdent, mixIdent, typeFor(superTree.span))
  }

  /**
   * Dotty's `untpd.XMLBlock` span starts one character past the leading `<`. Parent visitors
   * (ValDef, Assign, DefDef, ...) compute `rhs.span.start - offsetAdjustment` to advance the
   * cursor to the rhs, which for XML would leave the `<` behind as a stray non-whitespace
   * character in the parent's "before-rhs" Space. Use this helper instead of the raw
   * `tree.span.start - offsetAdjustment` computation so the cursor lands on the `<`.
   */
  private def effectiveStart(tree: Trees.Tree[?]): Int = {
    val adjStart = Math.max(0, tree.span.start - offsetAdjustment)
    tree match {
      case _: untpd.XMLBlock
          if adjStart > 0 && adjStart <= source.length && source.charAt(adjStart - 1) == '<' =>
        adjStart - 1
      case _ => adjStart
    }
  }

  private def visitXmlLiteral(xmlb: untpd.XMLBlock): S.XmlLiteral = {
    // Scala XML literals are parsed into untpd.XMLBlock, whose span covers the original
    // XML source — *except* it starts one char past the leading `<`. Back up to include
    // it. The desugared scala.xml.Elem/NodeBuffer subtree is ignored on purpose: walking
    // it would collide with the actual XML syntax in source.
    val adjustedStart = Math.max(0, xmlb.span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, xmlb.span.end - offsetAdjustment)
    val realStart =
      if (adjustedStart > 0 && adjustedStart <= source.length && source.charAt(adjustedStart - 1) == '<') adjustedStart - 1
      else adjustedStart

    val prefixStart = cursor
    val prefix = if (realStart > cursor && realStart <= source.length) {
      cursor = realStart
      ScalaSpace.format(source, prefixStart, realStart)
    } else Space.EMPTY

    val sourceText =
      if (realStart >= 0 && adjustedEnd <= source.length && adjustedEnd > realStart) {
        val s = source.substring(realStart, adjustedEnd)
        cursor = adjustedEnd
        s
      } else ""

    new S.XmlLiteral(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      sourceText,
      typeOfTree(xmlb)
    )
  }

  private def visitInterpolatedString(interp: untpd.InterpolatedString): S.InterpolatedString = {
    // String interpolation like s"Hello, $name", f"$x%2.2f", raw"a\nb". The dotty AST models
    // this as InterpolatedString(id, segments) where each segment is either a Thicket(literalPart,
    // expr) pair or a trailing bare Literal. We map the literal text chunks to J.Literal and each
    // embedded `$expr` / `${expr}` to S.Interpolation, so the expressions stay first-class.
    val prefix = extractPrefix(interp.span)
    val interpStart = Math.max(0, interp.span.start - offsetAdjustment)
    val interpName = interp.id.toString
    val interpolator = ident(interpName)
    cursor = Math.max(cursor, interpStart + interpName.length)
    val delimiter = if (source.startsWith("\"\"\"", cursor)) "\"\"\"" else "\""
    cursor = Math.min(source.length, cursor + delimiter.length)

    val parts = new util.ArrayList[Expression]()

    def addLiteral(litTree: Trees.Literal[?]): Unit = {
      val ls = Math.max(0, litTree.span.start - offsetAdjustment)
      val le = Math.max(0, litTree.span.end - offsetAdjustment)
      if (le > ls && le <= source.length) {
        val text = source.substring(ls, le)
        if (text.nonEmpty) {
          parts.add(new J.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
            litTree.const.value, text, Collections.emptyList(), JavaType.Primitive.String))
        }
        cursor = le
      }
    }

    def asExpression(j: J): Expression = j match {
      case e: Expression => e
      case s: Statement => new S.StatementExpression(Tree.randomId(), s)
      case other => throw new UnsupportedOperationException(
        s"Interpolation expression did not produce an Expression: ${other.getClass.getName}")
    }

    // A `${...}` block that wraps a single expression is unwrapped so the printer can re-add the
    // braces; a multi-statement block (rare, e.g. `${ val x = 1; x }`) keeps its own braces.
    def singleExpr(b: untpd.Block): Option[untpd.Tree] = {
      val all = b.stats ++ (if (b.expr.isEmpty) Nil else List(b.expr))
      all match {
        case single :: Nil => Some(single)
        case _ => None
      }
    }

    interp.segments.foreach {
      case th: untpd.Thicket =>
        val trees = th.trees
        trees.head match {
          case lit: Trees.Literal[?] => addLiteral(lit)
          case _ =>
        }
        val exprTree = trees(1)
        val dollarPos = cursor
        val braces = dollarPos + 1 < source.length && source.charAt(dollarPos + 1) == '{'
        if (braces) {
          val inner = exprTree match {
            case b: untpd.Block => singleExpr(b)
            case t => Some(t)
          }
          inner match {
            case Some(innerExpr) =>
              cursor = dollarPos + 2 // past `${`
              val exprJ = asExpression(visitTree(innerExpr))
              val blockEnd = Math.max(0, exprTree.span.end - offsetAdjustment)
              val bracePos = blockEnd - 1
              val afterExpr =
                if (bracePos > cursor && bracePos <= source.length) ScalaSpace.format(source.substring(cursor, bracePos))
                else Space.EMPTY
              cursor = Math.max(cursor, blockEnd)
              parts.add(new S.Interpolation(Tree.randomId(), Space.EMPTY, Markers.EMPTY, true, exprJ, afterExpr))
            case None =>
              cursor = dollarPos + 1 // at `{`, let the block self-print its braces
              val exprJ = asExpression(visitTree(exprTree))
              parts.add(new S.Interpolation(Tree.randomId(), Space.EMPTY, Markers.EMPTY, false, exprJ, Space.EMPTY))
          }
        } else {
          cursor = dollarPos + 1 // past `$`, at the identifier
          val exprJ = asExpression(visitTree(exprTree))
          parts.add(new S.Interpolation(Tree.randomId(), Space.EMPTY, Markers.EMPTY, false, exprJ, Space.EMPTY))
        }
      case lit: Trees.Literal[?] =>
        addLiteral(lit)
      case other =>
        throw new UnsupportedOperationException(
          s"Unexpected interpolated string segment: ${other.getClass.getName}")
    }

    cursor = Math.min(source.length, cursor + delimiter.length) // closing quote
    updateCursor(interp.span.end)
    new S.InterpolatedString(Tree.randomId(), prefix, Markers.EMPTY, interpolator, delimiter, parts,
      JavaType.Primitive.String)
  }

  private def visitSymbolLit(sym: untpd.SymbolLit): J.Identifier = {
    // Scala 2 symbol literal 'symbol — preserve as identifier
    val prefix = extractPrefix(sym.span)
    val sourceText = extractSource(sym.span)
    updateCursor(sym.span.end)
    ident(sourceText, prefix)
  }

  private def visitNamedArg(namedArg: Trees.NamedArg[?]): J.Assignment = {
    val prefix = extractPrefix(namedArg.span)
    val nameText = namedArg.name.toString

    // Detect backtick-quoted name and preserve the backticks on the identifier.
    // namedArg.name.toString strips them, but the source may show `name`.
    val spanStart = Math.max(0, namedArg.span.start - offsetAdjustment)
    val isBacktickQuoted = isBacktickQuotedNameAt(spanStart + 1, nameText.length)
    val displayName = if (isBacktickQuoted) "`" + nameText + "`" else nameText
    val nameId = ident(displayName)

    // Advance past name (and surrounding backticks if any), then find "="
    val nameLen = if (isBacktickQuoted) nameText.length + 2 else nameText.length
    cursor = Math.max(cursor, spanStart + nameLen)
    val eqSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 20, source.length)) else ""
    val eqIdx = positionOfNextIn(eqSearch, "=", 0)
    val beforeEq = if (eqIdx > 0) Space.format(eqSearch.substring(0, eqIdx)) else Space.EMPTY
    if (eqIdx >= 0) cursor = cursor + eqIdx + 1

    // Visit the argument value
    val arg = visitTree(namedArg.arg) match {
      case expr: Expression => expr
      case stmt: Statement => new S.StatementExpression(Tree.randomId(), stmt).asInstanceOf[Expression]
      case other => visitUnknown(namedArg)
    }
    updateCursor(namedArg.span.end)
    new J.Assignment(Tree.randomId(), prefix, Markers.EMPTY, nameId,
      JLeftPadded.build(arg).withBefore(beforeEq), typeFor(namedArg.span))
  }

  private def visitBind(bind: Trees.Bind[?]): S.Binding = {
    // At-binding pattern: `p@Person(name, _)`, `all@List(head, _*)`, `msg@(_: String)`.
    val prefix = extractPrefix(bind.span)
    val nameStr = bind.name.toString
    val name = ident(nameStr)
    cursor = cursor + nameStr.length
    val atPos = positionOfNext("@", cursor)
    val beforeAt = if (atPos >= cursor) ScalaSpace.format(source, cursor, atPos) else Space.EMPTY
    if (atPos >= 0) cursor = atPos + 1
    val pattern: Expression = visitTree(bind.body) match {
      case e: Expression => e
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => throw new UnsupportedOperationException(
        s"Bind body did not produce an Expression: ${bind.body.getClass.getSimpleName}")
    }
    updateCursor(bind.span.end)
    new S.Binding(Tree.randomId(), prefix, Markers.EMPTY, name, beforeAt, pattern, typeFor(bind.span))
  }

  private def visitAlternative(alt: Trees.Alternative[?]): S.Alternative = {
    // Pattern alternatives: `case 1 | 2 | 3 => ...` or `case "a" | "b" => ...`
    val prefix = extractPrefix(alt.span)
    val rpPatterns = new util.ArrayList[JRightPadded[Expression]]()
    val trees = alt.trees
    import scala.jdk.CollectionConverters.*
    val treeList = trees.asJava
    var i = 0
    while (i < treeList.size) {
      val t = treeList.get(i)
      val patternSpanStart = Math.max(0, t.span.start - offsetAdjustment)
      // Extract space before this pattern as prefix of the pattern itself (handled by visitTree via extractPrefix)
      val pat: Expression = visitTree(t) match {
        case e: Expression => e
        case j: J => new S.StatementExpression(Tree.randomId(), j)
        case _ => throw new UnsupportedOperationException(
          s"Alternative pattern did not produce an Expression: ${t.getClass.getSimpleName}")
      }
      // Extract space before the `|` (for all but the last pattern)
      val afterSpace: Space = if (i + 1 < treeList.size) {
        val nextTree = treeList.get(i + 1)
        val nextStart = Math.max(0, nextTree.span.start - offsetAdjustment)
        val between = if (cursor < nextStart && nextStart <= source.length) source.substring(cursor, nextStart) else ""
        val pipeIdx = positionOfNextIn(between, "|", 0)
        if (pipeIdx >= 0) {
          val space = Space.format(between.substring(0, pipeIdx))
          cursor = cursor + pipeIdx + 1
          space
        } else {
          Space.EMPTY
        }
      } else {
        Space.EMPTY
      }
      rpPatterns.add(JRightPadded.build(pat).withAfter(afterSpace))
      i += 1
    }
    updateCursor(alt.span.end)
    S.Alternative.build(Tree.randomId(), prefix, Markers.EMPTY,
      JContainer.build(Space.EMPTY, rpPatterns, Markers.EMPTY))
  }

  private def visitSingletonTypeTree(stt: Trees.SingletonTypeTree[?]): S.SingletonType = {
    // Singleton type reference: `None.type`, `obj.type`, `foo.bar.type`
    val prefix = extractPrefix(stt.span)
    val qualifier = visitTree(stt.ref) match {
      case e: Expression => e
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => throw new UnsupportedOperationException(
        s"SingletonTypeTree.ref did not produce an Expression: ${stt.ref.getClass.getSimpleName}")
    }
    // After visiting qualifier, cursor is at the end of qualifier.
    // The remaining source should be whitespace followed by ".type".
    val endPos = Math.max(0, stt.span.end - offsetAdjustment)
    val between = if (cursor < endPos && endPos <= source.length) source.substring(cursor, endPos) else ""
    val dotIdx = positionOfNextIn(between, ".", 0)
    val beforeType = if (dotIdx > 0) Space.format(between.substring(0, dotIdx)) else Space.EMPTY
    cursor = endPos
    new S.SingletonType(Tree.randomId(), prefix, Markers.EMPTY, qualifier, beforeType, typeFor(stt.span))
  }

  private def visitRefinedTypeTree(rtt: Trees.RefinedTypeTree[?]): S.RefinedType = {
    // Structural type refinement: `Any { def greet(): String }`
    // The rtt's own span may not include leading whitespace cleanly. Instead of
    // relying on extractPrefix(rtt.span), compute the prefix from where the parent
    // (or the opening brace, for parent-less refinements) begins.
    val savedCursor = cursor
    val firstChildStart: Int = if (rtt.tpt != null && !rtt.tpt.isEmpty && rtt.tpt.span.exists) {
      Math.max(0, rtt.tpt.span.start - offsetAdjustment)
    } else {
      // No parent; the refinement starts at the opening brace
      val braceIdx = positionOfNext("{", cursor)
      if (braceIdx >= 0) braceIdx else cursor
    }
    val prefix = if (savedCursor < firstChildStart && firstChildStart <= source.length) {
      val s = ScalaSpace.format(source, savedCursor, firstChildStart)
      cursor = firstChildStart
      s
    } else Space.EMPTY

    val parent: TypeTree = if (rtt.tpt != null && !rtt.tpt.isEmpty && rtt.tpt.span.exists) {
      visitTree(rtt.tpt) match {
        case t: TypeTree => t
        case e: Expression => new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
          Collections.emptyList(), e.toString, null, null)
        case _ => null
      }
    } else null

    // Find opening brace
    val braceSearchEnd = Math.min(source.length, Math.max(0, rtt.span.end - offsetAdjustment))
    var braceIdx = positionOfNext("{", cursor)
    if (braceIdx < 0 || braceIdx > braceSearchEnd) braceIdx = -1
    val blockPrefix = if (braceIdx > cursor) ScalaSpace.format(source, cursor, braceIdx) else Space.EMPTY
    if (braceIdx >= 0) cursor = braceIdx + 1

    // Visit refinements as block statements
    val stmts = new util.ArrayList[JRightPadded[Statement]]()
    import scala.jdk.CollectionConverters.*
    val refList = rtt.refinements.asJava
    var i = 0
    while (i < refList.size) {
      val ref = refList.get(i)
      val refJ = visitTree(ref)
      val stmt: Statement = refJ match {
        case s: Statement => s
        case e: Expression => new S.StatementExpression(Tree.randomId(), e.asInstanceOf[J]).asInstanceOf[Statement]
        case _ => throw new UnsupportedOperationException(
          s"Refinement member did not produce a Statement: ${ref.getClass.getSimpleName}")
      }
      stmts.add(JRightPadded.build(stmt))
      i += 1
    }

    val endPos = Math.max(0, rtt.span.end - offsetAdjustment)
    val remaining = if (cursor < endPos && endPos <= source.length) source.substring(cursor, endPos) else ""
    val closeIdx = remaining.lastIndexOf('}')
    val endSpace = if (closeIdx > 0) Space.format(remaining.substring(0, closeIdx)) else Space.EMPTY
    cursor = endPos
    val refinements = new J.Block(Tree.randomId(), blockPrefix, Markers.EMPTY,
      JRightPadded.build(false), stmts, endSpace)
    new S.RefinedType(Tree.randomId(), prefix, Markers.EMPTY, parent, refinements, typeFor(rtt.span))
  }

  private def visitAnnotated(ann: Trees.Annotated[?]): J = {
    // Dotty's `Annotated` covers both annotated expressions (`e: @ann`, with colon) and
    // annotated types (`T @ann`, without colon). Branch on the source to produce the
    // right LST node: `S.AnnotatedExpression` for the former, `S.AnnotatedType` for
    // the latter.
    val prefix = extractPrefix(ann.span)
    val arg: J = visitTree(ann.arg)
    val annotStart = Math.max(0, ann.annot.span.start - offsetAdjustment)
    val between = if (cursor < annotStart && annotStart <= source.length) source.substring(cursor, annotStart) else ""
    val colonIdx = positionOfNextIn(between, ":", 0)
    val isAnnotatedType = colonIdx < 0
    val beforeColon = if (!isAnnotatedType && colonIdx > 0) Space.format(between.substring(0, colonIdx)) else Space.EMPTY
    if (!isAnnotatedType) cursor = cursor + colonIdx + 1
    // The annot is typically Apply(Select(New(Ident), <init>), args). Convert to J.Annotation.
    val annotation = visitTree(ann.annot) match {
      case a: J.Annotation => a
      case _ => throw new UnsupportedOperationException(
        s"Annotated.annot did not produce a J.Annotation: ${ann.annot.getClass.getSimpleName}")
    }
    updateCursor(ann.span.end)
    if (isAnnotatedType) {
      val typeExpr: TypeTree = arg match {
        case tt: TypeTree => tt
        case _ => throw new UnsupportedOperationException(
          s"Annotated.arg in type position did not produce a TypeTree: ${ann.arg.getClass.getSimpleName}")
      }
      // Reuse J.AnnotatedType, even though Java prints annotations before the type.
      // ScalaPrinter overrides visitAnnotatedType to print `T @ann` (type first).
      new J.AnnotatedType(Tree.randomId(), prefix, Markers.EMPTY,
        Collections.singletonList(annotation), typeExpr)
    } else {
      val expr: Expression = arg match {
        case e: Expression => e
        case j: J => new S.StatementExpression(Tree.randomId(), j)
        case _ => throw new UnsupportedOperationException(
          s"Annotated.arg did not produce an Expression: ${ann.arg.getClass.getSimpleName}")
      }
      new S.AnnotatedExpression(Tree.randomId(), prefix, Markers.EMPTY,
        expr, beforeColon, annotation, typeFor(ann.span))
    }
  }

  private def visitMacroTree(mac: untpd.MacroTree): S.Macro = {
    // Scala 3 inline/macro expressions: ${ ... }, '{ ... }, or 'name
    val startAdj = Math.max(0, mac.span.start - offsetAdjustment)
    val firstChar = if (startAdj < source.length) source.charAt(startAdj) else ' '
    val secondChar = if (startAdj + 1 < source.length) source.charAt(startAdj + 1) else ' '
    val kind: S.Macro.Kind =
      if (firstChar == '$' && secondChar == '{') S.Macro.Kind.Splice
      else if (firstChar == '\'' && secondChar == '{') S.Macro.Kind.QuoteBlock
      else if (firstChar == '\'') S.Macro.Kind.QuoteIdent
      else S.Macro.Kind.Scala2Macro
    val macroKeywordStart =
      if (kind == S.Macro.Kind.Scala2Macro) source.lastIndexOf("macro", Math.max(0, startAdj - 1)) else -1
    val prefix =
      if (kind == S.Macro.Kind.Scala2Macro && macroKeywordStart >= cursor) {
        ScalaSpace.format(source, cursor, macroKeywordStart)
      } else {
        extractPrefix(mac.span)
      }

    // Advance past the prefix tokens: `${`, `'{`, or `'`
    cursor = kind match {
      case S.Macro.Kind.Splice | S.Macro.Kind.QuoteBlock => startAdj + 2
      case S.Macro.Kind.QuoteIdent => startAdj + 1
      case S.Macro.Kind.Scala2Macro if macroKeywordStart >= 0 => macroKeywordStart + "macro".length
      case S.Macro.Kind.Scala2Macro => startAdj
    }

    val expr: Expression = visitTree(mac.expr) match {
      case e: Expression => e
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => throw new UnsupportedOperationException(
        s"MacroTree.expr did not produce an Expression: ${mac.expr.getClass.getSimpleName}")
    }

    updateCursor(mac.span.end)
    new S.Macro(Tree.randomId(), prefix, Markers.EMPTY, kind, expr, typeFor(mac.span))
  }

  private def visitExtMethods(ext: untpd.ExtMethods): S.ExtensionMethods = {
    // Scala 3 extension method block: `extension (x: T) { def ... }`
    val prefix = extractPrefix(ext.span)
    val adjustedStart = Math.max(0, ext.span.start - offsetAdjustment)
    val searchStart = Math.max(0, Math.min(cursor, adjustedStart) - "extension".length)
    val parenIdx = positionOfNext("(", searchStart)
    val extKwIdx = if (parenIdx >= 0) source.lastIndexOf("extension", parenIdx) else positionOfNext("extension", cursor)
    val keywordEnd = if (extKwIdx >= 0) extKwIdx + "extension".length else cursor
    cursor = keywordEnd
    val beforeParen = if (parenIdx > keywordEnd) ScalaSpace.format(source, keywordEnd, parenIdx) else Space.EMPTY
    if (parenIdx >= 0) cursor = parenIdx + 1

    // Visit parameters from first parameter clause
    import scala.jdk.CollectionConverters.*
    val rpParams = new util.ArrayList[JRightPadded[Statement]]()
    val paramss = ext.paramss
    if (paramss.nonEmpty) {
      val firstClause = paramss.head.asInstanceOf[List[Trees.Tree[?]]].asJava
      var i = 0
      while (i < firstClause.size) {
        val pTree = firstClause.get(i)
        val pJ = visitTree(pTree)
        val pStmt: Statement = pJ match {
          case s: Statement => s
          case _ => throw new UnsupportedOperationException(
            s"Extension parameter did not produce a Statement: ${pTree.getClass.getSimpleName}")
        }
        // After-space — the comma or closing paren space
        val afterSpace: Space = if (i + 1 < firstClause.size) {
          val nextStart = Math.max(0, firstClause.get(i + 1).span.start - offsetAdjustment)
          val between = if (cursor < nextStart && nextStart <= source.length) source.substring(cursor, nextStart) else ""
          val commaIdx = positionOfNextIn(between, ",", 0)
          if (commaIdx >= 0) {
            val s = Space.format(between.substring(0, commaIdx))
            cursor = cursor + commaIdx + 1
            s
          } else Space.EMPTY
        } else {
          // Last param — find closing paren
          val closeParen = positionOfNext(")", cursor)
          val s = if (closeParen > cursor) ScalaSpace.format(source, cursor, closeParen) else Space.EMPTY
          if (closeParen >= 0) cursor = closeParen + 1
          s
        }
        rpParams.add(JRightPadded.build(pStmt).withAfter(afterSpace))
        i += 1
      }
    }
    val parameters = JContainer.build(beforeParen, rpParams, Markers.EMPTY)

    // Visit method declarations as a block. Find the opening brace within the
    // extension's span. Scala 3 extension can also use indented (braceless) form,
    // in which case there is no `{` between `)` and the span end.
    val extEnd = Math.max(0, ext.span.end - offsetAdjustment)
    val remainingBeforeBody = if (cursor < extEnd && extEnd <= source.length) source.substring(cursor, extEnd) else ""
    val localBraceIdx = positionOfNextIn(remainingBeforeBody, "{", 0)
    val isExtBraceless = localBraceIdx < 0

    val blockPrefix = if (isExtBraceless) {
      Space.EMPTY
    } else {
      val braceIdx = cursor + localBraceIdx
      val bp = if (braceIdx > cursor) ScalaSpace.format(source, cursor, braceIdx) else Space.EMPTY
      cursor = braceIdx + 1
      bp
    }

    val methodStmts = new util.ArrayList[JRightPadded[Statement]]()
    val methods = ext.methods.asJava
    var i = 0
    while (i < methods.size) {
      val m = methods.get(i)
      val mStmt: Statement = visitTree(m) match {
        case s: Statement => s
        case _ => throw new UnsupportedOperationException(
          s"Extension method did not produce a Statement: ${m.getClass.getSimpleName}")
      }
      methodStmts.add(JRightPadded.build(mStmt))
      i += 1
    }

    val endPos = Math.max(0, ext.span.end - offsetAdjustment)
    val remaining = if (cursor < endPos && endPos <= source.length) source.substring(cursor, endPos) else ""
    val endSpace = if (isExtBraceless) {
      Space.format(remaining)
    } else {
      val closeBrace = remaining.lastIndexOf('}')
      if (closeBrace > 0) Space.format(remaining.substring(0, closeBrace)) else Space.EMPTY
    }
    cursor = endPos
    val blockMarkers = if (isExtBraceless) {
      Markers.build(Collections.singletonList(new OmitBraces(Tree.randomId())))
    } else Markers.EMPTY
    val body = new J.Block(Tree.randomId(), blockPrefix, blockMarkers,
      JRightPadded.build(false), methodStmts, endSpace)
    S.ExtensionMethods.build(Tree.randomId(), prefix, Markers.EMPTY, parameters, body)
  }

  /** Build a single for-comprehension enumerator from a dotty tree. */
  private def buildForEnumerator(enumTree: Trees.Tree[?], isLast: Boolean, closeBracketPos: Int): JRightPadded[S.For.Enumerator] = {
    val (kind, lhsTree, rhsTree, opStr): (S.For.Enumerator.Kind, Trees.Tree[?], Trees.Tree[?], String) = enumTree match {
      case g: untpd.GenFrom => (S.For.Enumerator.Kind.Generator, g.pat, g.expr, "<-")
      case g: untpd.GenAlias => (S.For.Enumerator.Kind.Assignment, g.pat, g.expr, "=")
      case _ => (S.For.Enumerator.Kind.Guard, null, enumTree, "if")
    }

    val enumPrefix: Space = if (kind == S.For.Enumerator.Kind.Guard) {
      // Locate the `if` keyword. Everything before `if` is this enumerator's prefix.
      val rhsStart = Math.max(0, rhsTree.span.start - offsetAdjustment)
      val between = if (cursor < rhsStart && rhsStart <= source.length) source.substring(cursor, rhsStart) else ""
      val ifIdx = positionOfNextIn(between, "if", 0)
      if (ifIdx >= 0) {
        val s = if (ifIdx > 0) Space.format(between.substring(0, ifIdx)) else Space.EMPTY
        cursor = cursor + ifIdx + 2 // past `if`
        s
      } else Space.EMPTY
    } else {
      extractPrefix(enumTree.span)
    }

    var lhs: J = null
    if (kind != S.For.Enumerator.Kind.Guard) {
      if (kind == S.For.Enumerator.Kind.Assignment && isGivenEnumeratorPat(lhsTree)) {
        lhs = buildGivenEnumeratorLhs(lhsTree)
      } else {
        lhs = visitTree(lhsTree)
      }
    }

    val rhsStart = Math.max(0, rhsTree.span.start - offsetAdjustment)
    val between = if (cursor < rhsStart && rhsStart <= source.length) source.substring(cursor, rhsStart) else ""
    val beforeOp: Space = kind match {
      case S.For.Enumerator.Kind.Guard =>
        // cursor is past `if`; capture space between `if` and rhs
        if (between.nonEmpty) Space.format(between) else Space.EMPTY
      case _ =>
        val opIdx = positionOfNextIn(between, opStr, 0)
        val s = if (opIdx >= 0) Space.format(between.substring(0, opIdx)) else Space.EMPTY
        if (opIdx >= 0) cursor = cursor + opIdx + opStr.length
        s
    }
    if (kind == S.For.Enumerator.Kind.Guard) cursor = rhsStart

    val rhs: Expression = visitTree(rhsTree) match {
      case e: Expression => e
      case j: J => new S.StatementExpression(Tree.randomId(), j)
      case _ => throw new UnsupportedOperationException(
        s"For enumerator rhs did not produce an Expression: ${rhsTree.getClass.getSimpleName}")
    }

    // Determine the after-space and whether an explicit `;` separator was present.
    // Scala for-comprehensions allow `;` between generators, but newline-separated
    // generators have no `;`. Track this via the Semicolon marker so the printer
    // can round-trip correctly.
    var rpMarkers: Markers = Markers.EMPTY
    val after: Space = if (!isLast) {
      // Look for `;` before any non-whitespace char and before the next enumerator.
      // If only whitespace separates this enumerator from the next, there was no `;`.
      val sep = {
        var idx = -1
        var j = cursor
        while (idx < 0 && j < closeBracketPos) {
          val c = source.charAt(j)
          if (c == ';') idx = j
          else if (!c.isWhitespace) j = closeBracketPos // stop scanning
          else j += 1
        }
        idx
      }
      if (sep >= 0) {
        val s = if (sep > cursor) ScalaSpace.format(source, cursor, sep) else Space.EMPTY
        rpMarkers = Markers.EMPTY.add(new Semicolon(Tree.randomId()))
        cursor = sep + 1
        s
      } else Space.EMPTY
    } else {
      // Last enumerator: capture trailing space up to the closing bracket
      if (closeBracketPos >= cursor) {
        val s = if (closeBracketPos > cursor) ScalaSpace.format(source, cursor, closeBracketPos) else Space.EMPTY
        cursor = closeBracketPos
        s
      } else Space.EMPTY
    }

    val enumerator = new S.For.Enumerator(Tree.randomId(), enumPrefix, Markers.EMPTY,
      kind, lhs, beforeOp, rhs)
    new JRightPadded(enumerator, after, rpMarkers)
  }

  /** True when this GenAlias pattern is a `given` enumerator (`given T = e` / `given x: T = e`). */
  private def isGivenEnumeratorPat(pat: Trees.Tree[?]): Boolean = {
    if (pat == null || !pat.span.exists) return false
    val start = Math.max(0, pat.span.start - offsetAdjustment)
    val kw = "given"
    if (start + kw.length > source.length || !source.regionMatches(start, kw, 0, kw.length)) return false
    val after = start + kw.length
    after >= source.length || !Character.isJavaIdentifierPart(source.charAt(after))
  }

  /**
   * Build the left-hand side of a `given` for-comprehension enumerator. Dotty models
   *   `given T = e`     as Bind(`_`, Typed(`_`, T))
   *   `given x: T = e`  as Typed(Bind(x, _), T)
   * Neither is an at-binding, so visitBind would mangle the `given` keyword. Map the
   * anonymous form to S.AnonymousGiven and the named form to a J.VariableDeclarations
   * carrying the Given marker — matching how top-level givens are modeled. The `= e`
   * part is emitted by the enclosing enumerator, so no initializer is attached here.
   * Cursor must sit at the start of the `given` keyword on entry.
   */
  private def buildGivenEnumeratorLhs(pat: Trees.Tree[?]): J = {
    cursor += "given".length
    // Locate the type tree regardless of the (anonymous vs named) dotty shape.
    val tpt: Trees.Tree[?] = pat match {
      case bind: Trees.Bind[?] if bind.body.isInstanceOf[Trees.Typed[?]] =>
        bind.body.asInstanceOf[Trees.Typed[?]].tpt
      case typed: Trees.Typed[?] => typed.tpt
      case bind: Trees.Bind[?] => bind.body
      case _ => pat
    }
    val tptStart = Math.max(0, tpt.span.start - offsetAdjustment)
    // dotty marks the binding name as `_` even when named, so read the name (if any) from
    // source: a colon between `given` and the type signals the named form `given x: T`.
    val region = if (cursor < tptStart && tptStart <= source.length) source.substring(cursor, tptStart) else ""
    val colonIdx = region.indexOf(':')

    if (colonIdx < 0) {
      val typePrefix = if (region.nonEmpty) Space.format(region) else Space.SINGLE_SPACE
      val typeExpr = visitGivenEnumeratorType(tpt, typePrefix)
      updateCursor(pat.span.end)
      new S.AnonymousGiven(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
        new util.ArrayList[J.Annotation](), new util.ArrayList[J.Modifier](), typeExpr, null)
    } else {
      val nameRegion = region.substring(0, colonIdx)
      val leadWs = nameRegion.takeWhile(_.isWhitespace)
      val rest = nameRegion.drop(leadWs.length)
      val nameStr = rest.takeWhile(c => !c.isWhitespace)
      val trailWs = rest.drop(nameStr.length)
      val afterColonStr = region.substring(colonIdx + 1)
      val afterKeyword = if (leadWs.isEmpty) Space.SINGLE_SPACE else Space.format(leadWs)
      val beforeColon = if (trailWs.isEmpty) Space.EMPTY else Space.format(trailWs)
      val afterColon = if (afterColonStr.isEmpty) Space.SINGLE_SPACE else Space.format(afterColonStr)

      val typeExpr = visitGivenEnumeratorType(tpt, afterColon)
      updateCursor(pat.span.end)

      val namedVariable = new J.VariableDeclarations.NamedVariable(
        Tree.randomId(), afterKeyword, Markers.EMPTY, ident(nameStr), Collections.emptyList(), null, null)
      val modifiers = new util.ArrayList[J.Modifier]()
      modifiers.add(new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
        null, J.Modifier.Type.Final, Collections.emptyList()))
      val markers = Markers.build(Collections.singletonList(
        org.openrewrite.scala.marker.Given(Tree.randomId())))
      val varargs: Space = if (beforeColon != Space.EMPTY) beforeColon else null
      new J.VariableDeclarations(Tree.randomId(), Space.EMPTY, markers,
        new util.ArrayList[J.Annotation](), modifiers, typeExpr, varargs,
        Collections.emptyList(), Collections.singletonList(JRightPadded.build(namedVariable)))
    }
  }

  /** Visit a given enumerator's type at `tpt`, using the supplied prefix. Cursor jumps to the type. */
  private def visitGivenEnumeratorType(tpt: Trees.Tree[?], typePrefix: Space): TypeTree = {
    val typeStart = Math.max(0, tpt.span.start - offsetAdjustment)
    cursor = typeStart
    val savedCursor = cursor
    val visited = visitTypeTree(tpt)
    if (visited != null) visited.withPrefix(typePrefix).asInstanceOf[TypeTree]
    else {
      cursor = savedCursor
      val tText = extractSource(tpt.span)
      updateCursor(tpt.span.end)
      ident(tText, typePrefix).asInstanceOf[TypeTree]
    }
  }

  /** Common parser for ForDo (complex) and ForYield. */
  private def buildSFor(prefix: Space, enums: List[Trees.Tree[?]], body: Trees.Tree[?],
                        yielding: Boolean, spanEnd: Int): S.For = {
    // Find opening bracket: ( or {. If we encounter a non-whitespace character before either,
    // we're in the Scala 3 paren-less form (e.g. `for i <- 1 to 10 yield ...`).
    // A tuple-pattern generator may also start with `(` in paren-less form:
    // `for (a, b) <- xs yield ...`. Do not mistake that pattern delimiter for
    // the comprehension's optional control delimiter.
    def isTuplePatternGeneratorOpen(openIdx: Int): Boolean = {
      var depth = 1
      var j = openIdx + 1
      while (depth > 0 && j < source.length) {
        source.charAt(j) match {
          case '(' => depth += 1
          case ')' => depth -= 1
          case _ =>
        }
        j += 1
      }
      if (depth != 0) {
        false
      } else {
        val afterClose = indexOfNextNonWhitespace(j)
        afterClose < source.length &&
          (source.startsWith("<-", afterClose) || source.charAt(afterClose) == '=')
      }
    }

    val (openIdx, openBracket): (Int, Char) = {
      var i = cursor
      while (i < source.length && source.charAt(i).isWhitespace) i += 1
      if (i < source.length && source.charAt(i) == '{') {
        (i, source.charAt(i))
      } else if (i < source.length && source.charAt(i) == '(' && !isTuplePatternGeneratorOpen(i)) {
        (i, source.charAt(i))
      } else {
        (-1, ' ')  // ' ' sentinel: no bracket (Scala 3 indented form)
      }
    }
    val isParenless = openBracket == ' '
    val beforeOpen: Space = if (openIdx > cursor) ScalaSpace.format(source, cursor, openIdx) else Space.EMPTY
    if (openIdx >= 0) cursor = openIdx + 1
    val closeChar = if (openBracket == '(') ')' else if (openBracket == '{') '}' else '\u0000'

    import scala.jdk.CollectionConverters.*
    val enumsList = enums.asJava
    val rpEnums = new util.ArrayList[JRightPadded[S.For.Enumerator]]()

    // Pre-compute the matching close bracket position for paren/brace form.
    // positionOfNext() would incorrectly stop at the first ')' inside a pattern like `(x)`,
    // so we use positionOfMatchingClose to track depth while skipping comments and strings.
    val closeBracketPosition: Int = if (!isParenless) {
      val p = positionOfMatchingClose(openBracket, closeChar, cursor)
      if (p >= 0) p else spanEnd
    } else spanEnd

    var i = 0
    while (i < enumsList.size) {
      val isLast = i + 1 == enumsList.size
      // Compute the end position of enums: close bracket for paren form; for paren-less,
      // stop at `yield` (yielding form) or body start (do form), whichever isn't a keyword.
      val enumEnd =
        if (isParenless) {
          val bodyStart = Math.max(0, body.span.start - offsetAdjustment)
          if (yielding) {
            val yieldIdx = positionOfNext("yield", cursor)
            if (yieldIdx >= 0 && yieldIdx < bodyStart) yieldIdx else bodyStart
          } else {
            // Paren-less `for ... do <body>`: enumerator region ends at `do`.
            val doIdx = positionOfNext("do", cursor)
            if (doIdx >= 0 && doIdx < bodyStart) doIdx else bodyStart
          }
        }
        else closeBracketPosition
      rpEnums.add(buildForEnumerator(enumsList.get(i), isLast, enumEnd))
      i += 1
    }
    val enumerators = JContainer.build(beforeOpen, rpEnums, Markers.EMPTY)

    // Advance past the closing bracket (only in paren form)
    if (!isParenless && closeBracketPosition < source.length) {
      cursor = closeBracketPosition + 1
    }

    // Capture space before body / `yield` / `do`
    val bodyStart = Math.max(0, body.span.start - offsetAdjustment)
    val rawBetween = if (cursor < bodyStart && bodyStart <= source.length) source.substring(cursor, bodyStart) else ""
    val beforeBody: Space = if (yielding) {
      val yieldIdx = positionOfNextIn(rawBetween, "yield", 0)
      if (yieldIdx >= 0) {
        val s = Space.format(rawBetween.substring(0, yieldIdx))
        cursor = cursor + yieldIdx + "yield".length
        s
      } else Space.EMPTY
    } else if (isParenless) {
      // Paren-less `do` form: capture space before `do`, advance past `do`.
      // Whitespace after `do` becomes the body's prefix.
      val doIdx = positionOfNextIn(rawBetween, "do", 0)
      if (doIdx >= 0) {
        val s = Space.format(rawBetween.substring(0, doIdx))
        cursor = cursor + doIdx + "do".length
        s
      } else Space.EMPTY
    } else {
      Space.format(rawBetween)
    }
    if (!yielding && !isParenless) cursor = bodyStart

    val bodyJ = visitTree(body)
    updateCursor(spanEnd)

    S.For.build(Tree.randomId(), prefix, Markers.EMPTY, enumerators, openBracket,
      yielding, beforeBody, bodyJ, null)
  }

  private def visitForYield(forYield: untpd.ForYield): S.For = {
    // Scala for-comprehension with yield: `for { x <- xs } yield expr`
    val prefix = extractPrefix(forYield.span)
    // Advance past "for"
    val forIdx = positionOfNext("for", cursor)
    if (forIdx >= cursor) cursor = forIdx + 3
    val endPos = Math.max(0, forYield.span.end - offsetAdjustment)
    import scala.jdk.CollectionConverters.*
    buildSFor(prefix, forYield.enums.asInstanceOf[List[Trees.Tree[?]]], forYield.expr, yielding = true, endPos)
  }

  /**
   * By-name parameter type `=> Int`. Modeled as a degenerate `S.FunctionType`:
   * no parameters, unparenthesized. The printer renders empty + unparenthesized
   * params as just `=>`, so this round-trips to `=> Int` without a dedicated type.
   * It stays distinguishable from `() => Int` (empty params, but parenthesized).
   */
  private def visitByNameTypeTree(bnt: Trees.ByNameTypeTree[?]): S.FunctionType = {
    val prefix = extractPrefix(bnt.span)
    val beforeArrow = sourceBefore("=>")
    val returnType = visitTypeTree(bnt.result)
    S.FunctionType.build(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      false,
      JContainer.empty[TypeTree](),
      new JLeftPadded(beforeArrow, returnType, Markers.EMPTY),
      typeOfTree(bnt)
    )
  }

  private def visitTypeBoundsTree(tbt: Trees.TypeBoundsTree[?]): J.Identifier = {
    // Wildcard type bounds: `_` in F[_], or bounds like `_ >: A <: B`
    val prefix = extractPrefix(tbt.span)
    val text = extractSource(tbt.span)
    updateCursor(tbt.span.end)
    ident(text, prefix)
  }

  private def visitTypeAlias(td: Trees.TypeDef[?]): S.TypeAlias = {
    val prefix = extractPrefix(td.span)
    val text = extractSource(td.span)
    updateCursor(td.span.end)
    new S.TypeAlias(Tree.randomId(), prefix, Markers.EMPTY, text)
  }

  private def visitPatDef(pd: untpd.PatDef): S.PatternDefinition = {
    val prefix = extractPrefix(pd.span)
    val text = extractSource(pd.span)
    updateCursor(pd.span.end)
    new S.PatternDefinition(Tree.randomId(), prefix, Markers.EMPTY, text)
  }

  /**
   * Compiler-synthesized `Trees.TypeTree` placeholder for an inferred type with no
   * source text (zero-width span). Returns `J.Empty` (which implements `TypeTree`)
   * so existing `case tt: TypeTree => ...` callers see a valid empty placeholder.
   */
  private def visitSyntheticTypeTree(tt: Trees.TypeTree[?]): J =
    new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY)

  /**
   * Visit a tree in a type position and return a `TypeTree`. Handles `untpd.Function` in a
   * type context (e.g. `Int => Int` as a return type or val type ascription) by building an
   * `S.FunctionType`. For other tree shapes, delegates to `visitTree` and accepts any result
   * that is already a `TypeTree`.
   */
  private def visitTypeTree(tpt: Trees.Tree[?]): TypeTree = tpt match {
    case f: untpd.Function => visitFunctionType(f)
    case po: untpd.PostfixOp if po.op != null && po.op.name.toString == "*" =>
      visitRepeatedType(po)
    case tuple: untpd.Tuple => visitTupleType(tuple)
    case parens: untpd.Parens => visitParenthesizedType(parens)
    case _ =>
      visitTree(tpt) match {
        case tt: TypeTree => tt
        case id: J.Identifier => id
        case _ => null
      }
  }

  /**
   * Build a `J.ParenthesizedTypeTree` for a parenthesized type (e.g. `(Int)` or
   * `(Int => Unit)` as a return type or ascription). Scala 3's parser wraps such a
   * type in `untpd.Parens`, which in an expression position becomes `J.Parentheses`;
   * in a type position we must keep it a `TypeTree` so the parentheses aren't dropped.
   */
  private def visitParenthesizedType(parens: untpd.Parens): J.ParenthesizedTypeTree = {
    val prefix = extractPrefix(parens.span)
    val beforeOpenParen = sourceBefore("(")
    val inner = visitTypeTree(parens.t)
    val afterInner = sourceBefore(")")
    val parenthesized = new J.Parentheses[TypeTree](
      Tree.randomId(),
      beforeOpenParen,
      Markers.EMPTY,
      JRightPadded.build(inner).withAfter(afterInner)
    )
    new J.ParenthesizedTypeTree(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      Collections.emptyList(),
      parenthesized
    )
  }

  /**
   * Build an `S.TupleType` for a tuple in a type position (e.g. `(Int, Int)` as a return type
   * or value type ascription). Scala 3's parser uses `untpd.Tuple` for both value and type
   * tuples; we dispatch to this builder only when the parent context is a type.
   */
  private def visitTupleType(tuple: untpd.Tuple): S.TupleType = {
    // An empty `Tuple(Nil)` shouldn't appear in a type position: `()` parses as the
    // unit value, `() => T` parses as `Function`, and `Unit` is the canonical empty
    // type. Fail loudly if we encounter it so the gap is discovered, not silently
    // round-tripped to wrong source.
    if (tuple.trees.isEmpty) {
      throw new IllegalStateException("Empty tuple in type position is not supported")
    }

    val prefix = extractPrefix(tuple.span)

    // Space between the prefix and `(`. extractPrefix usually lands the cursor
    // exactly at `(`, but if the compiler span starts inside the parens we still
    // want to capture any whitespace here instead of dropping it.
    val beforeOpenParen = sourceBefore("(")

    val elements = new util.ArrayList[JRightPadded[TypeTree]]()
    for (i <- tuple.trees.indices) {
      val elem = visitTypeTree(tuple.trees(i))
      val after = if (i < tuple.trees.size - 1) sourceBefore(",") else sourceBefore(")")
      elements.add(JRightPadded.build(elem).withAfter(after))
    }

    S.TupleType.build(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      JContainer.build(beforeOpenParen, elements, Markers.EMPTY),
      typeOfTree(tuple)
    )
  }

  /**
   * Build an `S.RepeatedType` for a repeated/vararg type position (e.g. `String*`).
   * Scala 3's parser represents `T*` as `PostfixOp(T, Ident("*"))`.
   */
  private def visitRepeatedType(po: untpd.PostfixOp): S.RepeatedType = {
    val prefix = extractPrefix(po.span)
    val elementType = visitTypeTree(po.od)
    val innerEnd = if (po.od.span.exists) Math.max(0, po.od.span.end - offsetAdjustment) else cursor
    val starStart = if (po.op.span.exists) Math.max(0, po.op.span.start - offsetAdjustment) else innerEnd
    val beforeStar = if (cursor < starStart && starStart <= source.length) {
      ScalaSpace.format(source, cursor, starStart)
    } else Space.EMPTY
    if (po.span.exists) updateCursor(po.span.end)
    new S.RepeatedType(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      elementType,
      beforeStar,
      typeOfTree(po)
    )
  }

  /**
   * Build an `S.FunctionType` for an `untpd.Function` node used in a type position
   * (e.g. `Int => Int`, `(Int, String) => Boolean`, `() => Unit`). Each function-type
   * argument is itself a synthetic `ValDef` whose `tpt` carries the actual parameter
   * type and source span.
   */
  private def visitFunctionType(func: untpd.Function): S.FunctionType = {
    val funcStart = Math.max(0, func.span.start - offsetAdjustment)
    val funcEnd = Math.max(0, func.span.end - offsetAdjustment)
    val prefix = if (cursor < funcStart && funcStart <= source.length) {
      ScalaSpace.format(source, cursor, funcStart)
    } else Space.EMPTY
    cursor = Math.max(cursor, funcStart)

    val funcSource = if (funcStart < funcEnd && funcEnd <= source.length) {
      source.substring(funcStart, funcEnd)
    } else ""

    // Find the arrow within the function-type source.
    val relArrowIdx = positionOfNextIn(funcSource, "=>", 0)
    val arrowAbs = if (relArrowIdx >= 0) funcStart + relArrowIdx else funcEnd

    // Detect whether the parameter list is parenthesized. A single unnamed param like
    // `Int => Int` is unparenthesized; everything else (`()`, `(Int)`, `(Int, Long)`)
    // uses parentheses.
    val headTrimmed = funcSource.dropWhile(Character.isWhitespace)
    val parenthesized = headTrimmed.startsWith("(")

    val containerBefore = if (parenthesized) {
      val openParenAbs = funcStart + positionOfNextIn(funcSource, "(", 0)
      val before = if (cursor < openParenAbs) ScalaSpace.format(source, cursor, openParenAbs) else Space.EMPTY
      cursor = openParenAbs + 1
      before
    } else Space.EMPTY

    val paramElements = new util.ArrayList[JRightPadded[TypeTree]]()
    val params = func.args
    val closeParenAbs = if (parenthesized) {
      // Find the matching `)` before the arrow.
      val rel = funcSource.lastIndexOf(')', if (relArrowIdx >= 0) relArrowIdx - 1 else funcSource.length - 1)
      if (rel >= 0) funcStart + rel else arrowAbs
    } else arrowAbs

    for (i <- params.indices) {
      val param = params(i)
      // For a function *type*, each arg ValDef carries the type in `tpt`.
      val paramTpt: Trees.Tree[?] = param match {
        case vd: Trees.ValDef[?] if vd.tpt != untpd.EmptyTree => vd.tpt
        case other => other
      }
      val paramType = visitTypeTree(paramTpt)
      val isLast = i == params.size - 1
      val afterSpace: Space = if (!isLast) {
        // Find the comma separating this param from the next.
        val nextStart = Math.max(0, params(i + 1).span.start - offsetAdjustment)
        if (cursor < nextStart && nextStart <= source.length) {
          val between = source.substring(cursor, nextStart)
          val commaIdx = positionOfNextIn(between, ",", 0)
          if (commaIdx >= 0) {
            val space = Space.format(between.substring(0, commaIdx))
            cursor = cursor + commaIdx + 1
            space
          } else Space.EMPTY
        } else Space.EMPTY
      } else if (parenthesized) {
        // Last param in parens: capture space up to `)`.
        if (cursor < closeParenAbs && closeParenAbs <= source.length) {
          ScalaSpace.format(source, cursor, closeParenAbs)
        } else Space.EMPTY
      } else {
        // Unparen single-arg form: param has no separator; the space before `=>`
        // belongs to the return-type padding, not the param's after-space.
        Space.EMPTY
      }
      paramElements.add(JRightPadded.build(paramType).withAfter(afterSpace))
    }

    if (parenthesized) {
      cursor = closeParenAbs + 1
    }

    val parameters = JContainer.build(containerBefore, paramElements, Markers.EMPTY)

    // Detect the Scala 3 context-function arrow `?=>` (a `?` immediately before `=>`).
    // The `?` is treated as part of the arrow token; a marker records it so the printer
    // re-emits `?=>` instead of `=>`.
    val isContextArrow = arrowAbs > funcStart && arrowAbs - 1 >= 0 &&
      arrowAbs - 1 < source.length && source.charAt(arrowAbs - 1) == '?'
    val arrowTokenStart = if (isContextArrow) arrowAbs - 1 else arrowAbs

    // Space immediately before the arrow token.
    val beforeArrow = if (cursor < arrowTokenStart && arrowTokenStart <= source.length) {
      ScalaSpace.format(source, cursor, arrowTokenStart)
    } else Space.EMPTY

    // Move past the arrow (`=>` is always the last two characters of the token).
    if (arrowAbs >= cursor) {
      cursor = arrowAbs + 2
    }

    // The return type's own prefix (extracted by visitTree) will carry the space
    // immediately after `=>`.
    val returnType = visitTypeTree(func.body)

    if (funcEnd > cursor) cursor = funcEnd

    val funcMarkers = if (isContextArrow)
      Markers.build(Collections.singletonList(new ContextFunctionArrow(java.util.UUID.randomUUID())))
    else Markers.EMPTY

    S.FunctionType.build(
      Tree.randomId(),
      prefix,
      funcMarkers,
      parenthesized,
      parameters,
      new JLeftPadded(beforeArrow, returnType, Markers.EMPTY),
      typeOfTree(func)
    )
  }

  /**
   * Returns `true` when the raw-name slice starting at `rawNameStart` (the first non-backtick
   * char of the identifier) is surrounded by backticks in the source. `Name.toString` strips
   * them, so callers check this to re-attach the backticks for round-trip fidelity.
   */
  private def isBacktickQuotedNameAt(rawNameStart: Int, rawNameLen: Int): Boolean = {
    val end = rawNameStart + rawNameLen
    rawNameStart > 0 && end < source.length &&
      source.charAt(rawNameStart - 1) == '`' && source.charAt(end) == '`'
  }

  /**
   * For an identifier whose raw (backtick-stripped) name occupies `[rawNameStart, rawNameStart + rawNameLen)`
   * in `source`, returns the display name to put on the `J.Identifier` and the cursor position to advance to
   * (just past the closing backtick, if the source is backtick-quoted). The `prefix` covers `[prefixStart, openOfName)`,
   * where `openOfName` is the opening backtick when quoted and `rawNameStart` otherwise — this ensures the prefix
   * stays pure whitespace.
   */
  private def backtickAwareName(prefixStart: Int, rawNameStart: Int, rawNameLen: Int, rawName: String): (Space, String, Int) = {
    if (isBacktickQuotedNameAt(rawNameStart, rawNameLen)) {
      val openTick = rawNameStart - 1
      val prefix = if (prefixStart < openTick) ScalaSpace.format(source, prefixStart, openTick) else Space.EMPTY
      (prefix, "`" + rawName + "`", rawNameStart + rawNameLen + 1)
    } else {
      val prefix = if (prefixStart < rawNameStart) ScalaSpace.format(source, prefixStart, rawNameStart) else Space.EMPTY
      (prefix, rawName, rawNameStart + rawNameLen)
    }
  }

  private def visitUnknown(tree: Trees.Tree[?]): Nothing = {
    val adjStart = Math.max(0, tree.span.start - offsetAdjustment)
    val adjEnd = Math.max(0, tree.span.end - offsetAdjustment)
    val sourceText = if (adjStart < adjEnd && adjEnd <= source.length) source.substring(adjStart, adjEnd) else ""
    throw new UnsupportedOperationException(
      s"Unmapped Scala AST node: ${tree.getClass.getSimpleName} " +
      s"at ${tree.span} source=${sourceText.take(80).replace('\n', ' ')}"
    )
  }
  
  def extractPrefix(span: Spans.Span): Space = {
    if (!span.exists) {
      return Space.EMPTY
    }
    
    val start = cursor
    val adjustedTreeStart = Math.max(0, span.start - offsetAdjustment)
    
    if (adjustedTreeStart > cursor && adjustedTreeStart <= source.length) {
      cursor = adjustedTreeStart
      // Use Space.format to properly extract comments from whitespace
      ScalaSpace.format(source, start, adjustedTreeStart)
    } else {
      Space.EMPTY
    }
  }
  
  private def extractSource(span: Spans.Span): String = {
    if (!span.exists) {
      return ""
    }
    
    val adjustedStart = Math.max(0, span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, span.end - offsetAdjustment)
    
    if (adjustedStart >= 0 && adjustedEnd <= source.length && adjustedEnd > adjustedStart) {
      cursor = adjustedEnd
      val result = source.substring(adjustedStart, adjustedEnd)
      result
    } else {
      ""
    }
  }
  
  /**
   * Build a `JContainer` of comma-separated elements. For each item in
   * `items`, the caller's `convert` function is applied (advancing the cursor
   * over that element's source text). Between consecutive elements this
   * helper consumes the comma via `sourceBefore(",")`. After the last element
   * it consumes `sourceBefore(endDelim)` (e.g. `")"` or `"]"`), so the trailing
   * whitespace before the closing delimiter is captured on the last element.
   */
  private def buildArgumentContainer[A, T <: J](
    items: Seq[A],
    convert: A => T,
    endDelim: String,
    prefix: Space = Space.EMPTY,
    markers: Markers = Markers.EMPTY
  ): JContainer[T] = {
    val padded = new util.ArrayList[JRightPadded[T]](items.size)
    val last = items.size - 1
    var i = 0
    while (i < items.size) {
      val elem = convert(items(i))
      val after =
        if (i < last) sourceBefore(",")
        else sourceBefore(endDelim)
      padded.add(JRightPadded.build(elem).withAfter(after))
      i += 1
    }
    JContainer.build(prefix, padded, markers)
  }

  /**
   * Build a `J.Identifier` with sensible defaults (random id, empty markers,
   * no annotations). Mirrors the JS parser's `mapIdentifier` convenience.
   */
  private def ident(
    name: String,
    prefix: Space = Space.EMPTY,
    typeInfo: JavaType = null,
    fieldType: JavaType.Variable = null
  ): J.Identifier =
    new J.Identifier(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      Collections.emptyList(),
      name,
      typeInfo,
      fieldType
    )

  /**
   * Extract whitespace and comments before the next occurrence of a delimiter,
   * skipping over line/block comments. Similar to sourceBefore in
   * ReloadableJava17Parser.
   */
  private def sourceBefore(untilDelim: String): Space = {
    val delimIndex = positionOfNext(untilDelim)
    if (delimIndex < 0) {
      Space.EMPTY
    } else {
      val prefix = source.substring(cursor, delimIndex)
      cursor = delimIndex + untilDelim.length
      Space.format(prefix)
    }
  }
  
  /**
   * Extract whitespace between the current cursor position and the given position.
   */
  private def spaceBetween(startPos: Int, endPos: Int): Space = {
    val adjustedStart = Math.max(0, startPos - offsetAdjustment)
    val adjustedEnd = Math.max(0, endPos - offsetAdjustment)
    
    if (adjustedStart >= cursor && adjustedEnd > adjustedStart && adjustedEnd <= source.length) {
      val spaceText = source.substring(cursor, adjustedStart)
      cursor = adjustedStart
      Space.format(spaceText)
    } else {
      Space.EMPTY
    }
  }
  
  /**
   * Find the position of the next occurrence of a delimiter, skipping over line
   * (`//...`) and block (`/* ... */`) comments. When the delimiter is a word
   * (starts with a letter/digit/underscore), also enforces whole-word boundaries
   * so e.g. `"if"` doesn't match inside identifiers like `notify`.
   */
  private def positionOfNext(delimiter: String, startFrom: Int = cursor): Int =
    positionOfNextIn(source, delimiter, startFrom)

  /** As {@link positionOfNext} but searches an arbitrary {@code text} (typically a
   *  substring of {@code source}) instead of the class-level source. */
  private def positionOfNextIn(text: String, delimiter: String, startFrom: Int): Int = {
    def isWordChar(c: Char): Boolean = Character.isLetterOrDigit(c) || c == '_'
    val firstIsWord = delimiter.nonEmpty && isWordChar(delimiter.charAt(0))
    val lastIsWord = delimiter.nonEmpty && isWordChar(delimiter.charAt(delimiter.length - 1))
    var inSingleLine = false
    var inMultiLine = false
    var i = startFrom
    while (i <= text.length - delimiter.length) {
      val c = text.charAt(i)
      if (inSingleLine) {
        if (c == '\n') inSingleLine = false
        i += 1
      } else if (inMultiLine) {
        if (c == '*' && i + 1 < text.length && text.charAt(i + 1) == '/') {
          inMultiLine = false
          i += 2
        } else i += 1
      } else if (c == '/' && i + 1 < text.length && text.charAt(i + 1) == '/') {
        inSingleLine = true
        i += 2
      } else if (c == '/' && i + 1 < text.length && text.charAt(i + 1) == '*') {
        inMultiLine = true
        i += 2
      } else if (text.startsWith(delimiter, i)) {
        val before = !firstIsWord || i == 0 || !isWordChar(text.charAt(i - 1))
        val afterPos = i + delimiter.length
        val after = !lastIsWord || afterPos >= text.length || !isWordChar(text.charAt(afterPos))
        if (before && after) return i
        i += 1
      } else i += 1
    }
    -1
  }

  /** Returns the index of the bracket that matches the open bracket assumed to be at
   *  {@code afterOpen - 1}, searching forward from {@code afterOpen} in {@code source}.
   *  Skips nested bracket pairs, {@code //} and {@code /* */} comments, and
   *  {@code "..."} string literals (including {@code \\} escape sequences).
   *  Returns -1 if the matching close is not found before end of source. */
  private def positionOfMatchingClose(openChar: Char, closeChar: Char, afterOpen: Int): Int = {
    var depth = 1
    var i = afterOpen
    var inLineComment = false
    var inBlockComment = false
    var inString = false
    while (i < source.length && depth > 0) {
      val c = source.charAt(i)
      if (inLineComment) {
        if (c == '\n') inLineComment = false
        i += 1
      } else if (inBlockComment) {
        if (c == '*' && i + 1 < source.length && source.charAt(i + 1) == '/') { inBlockComment = false; i += 2 }
        else i += 1
      } else if (inString) {
        if (c == '\\') i += 2
        else { if (c == '"') inString = false; i += 1 }
      } else if (c == '/' && i + 1 < source.length && source.charAt(i + 1) == '/') { inLineComment = true; i += 2 }
      else if (c == '/' && i + 1 < source.length && source.charAt(i + 1) == '*') { inBlockComment = true; i += 2 }
      else if (c == '"') { inString = true; i += 1 }
      else {
        if (c == openChar) depth += 1
        else if (c == closeChar) depth -= 1
        if (depth > 0) i += 1
      }
    }
    if (depth == 0) i else -1
  }

  /** Scans a `def` header in `source[start, end)` and returns true when the method
   *  body is introduced by `{` (Scala 2 procedure syntax) rather than `=`. Balanced
   *  `(...)`/`[...]` groups are skipped via {@code positionOfMatchingClose} so an `=`
   *  inside a parameter clause (a default value, or the `=` of a `=>` function-type
   *  parameter) is not mistaken for the method-body `=`. Comments are skipped too. */
  private def bodyOpensWithBrace(start: Int, end: Int): Boolean = {
    var i = start
    var inLineComment = false
    var inBlockComment = false
    while (i < end) {
      val c = source.charAt(i)
      if (inLineComment) {
        if (c == '\n') inLineComment = false
        i += 1
      } else if (inBlockComment) {
        if (c == '*' && i + 1 < end && source.charAt(i + 1) == '/') { inBlockComment = false; i += 2 }
        else i += 1
      } else if (c == '/' && i + 1 < end && source.charAt(i + 1) == '/') { inLineComment = true; i += 2 }
      else if (c == '/' && i + 1 < end && source.charAt(i + 1) == '*') { inBlockComment = true; i += 2 }
      else if (c == '(' || c == '[') {
        val close = positionOfMatchingClose(c, if (c == '(') ')' else ']', i + 1)
        i = if (close >= 0) close + 1 else end
      } else if (c == '{') return true
      else if (c == '=') return false
      else i += 1
    }
    false
  }

  /**
   * Skip whitespace and Scala comments (`/* ... */` and `//`) and return the
   * position of the next significant character. Returns `source.length` if no
   * such character exists.
   */
  /**
   * After visiting a statement that ended at `statEnd`, detect an explicit `;`
   * separator on the same line and consume it. Returns the JRightPadded
   * trailing space and markers; advances `cursor`.
   */
  private def consumeTrailingSemicolon(statEnd: Int, nextStart: Int): (Space, Markers) = {
    val trailStart = Math.max(statEnd, cursor)
    if (trailStart >= nextStart || nextStart > source.length) {
      return (Space.EMPTY, Markers.EMPTY)
    }
    val between = source.substring(trailStart, nextStart)
    var semiIdx = -1
    var j = 0
    while (semiIdx < 0 && j < between.length) {
      val c = between.charAt(j)
      if (c == ';') semiIdx = j
      else if (c == '\n' || c == '\r') j = between.length
      else if (c != ' ' && c != '\t') j = between.length
      else j += 1
    }
    if (semiIdx >= 0) {
      val trailing = if (semiIdx > 0) Space.format(between.substring(0, semiIdx)) else Space.EMPTY
      cursor = trailStart + semiIdx + 1
      (trailing, Markers.EMPTY.add(new Semicolon(Tree.randomId())))
    } else {
      (Space.EMPTY, Markers.EMPTY)
    }
  }

  private def indexOfNextNonWhitespace(startFrom: Int = cursor): Int = {
    var i = startFrom
    while (i < source.length) {
      val c = source.charAt(i)
      if (Character.isWhitespace(c)) {
        i += 1
      } else if (c == '/' && i + 1 < source.length && source.charAt(i + 1) == '*') {
        val close = source.indexOf("*/", i + 2)
        if (close < 0) return source.length
        i = close + 2
      } else if (c == '/' && i + 1 < source.length && source.charAt(i + 1) == '/') {
        val nl = source.indexOf('\n', i + 2)
        if (nl < 0) return source.length
        i = nl + 1
      } else {
        return i
      }
    }
    i
  }
  
  /** Find a keyword in text, ensuring it's a whole word (not part of an identifier). */
  private def findKeyword(text: String, keyword: String): Int = {
    var pos = 0
    while (pos < text.length) {
      val idx = text.indexOf(keyword, pos)
      if (idx < 0) return -1
      val before = idx == 0 || !Character.isLetterOrDigit(text.charAt(idx - 1))
      val after = idx + keyword.length >= text.length || !Character.isLetterOrDigit(text.charAt(idx + keyword.length))
      if (before && after) return idx
      pos = idx + 1
    }
    -1
  }

  /**
   * Parse modifier keywords out of a raw text window — the source text between the
   * cursor and the keyword that ends the modifier list (`class`/`def`/the parameter
   * name, etc.). Recognises whole-word occurrences only, sorts by source position,
   * captures the inter-modifier whitespace, and expands `private[scope]` /
   * `protected[scope]` qualified-access suffixes.
   *
   * Returns the parsed modifiers along with the offset within `text` where the
   * last consumed modifier ended — callers use that to advance their cursor.
   */
  private def parseModifierKeywords(
      text: String,
      keywords: List[(String, J.Modifier.Type)]
  ): (util.ArrayList[J.Modifier], Int) = {
    val modifierList = new util.ArrayList[J.Modifier]()
    val present = keywords.flatMap { case (kw, mt) =>
      val pos = findKeyword(text, kw)
      if (pos >= 0) Some((pos, kw, mt)) else None
    }.sortBy(_._1)

    var lastEnd = 0
    for ((pos, kw, mt) <- present) {
      val spaceBefore = if (pos > lastEnd) Space.format(text.substring(lastEnd, pos)) else Space.EMPTY

      // `private[scope]` / `protected[scope]` qualified access
      var fullKw = kw
      var kwLen = kw.length
      if ((kw == "private" || kw == "protected") && pos + kwLen < text.length) {
        val afterKw = text.substring(pos + kwLen)
        if (afterKw.startsWith("[")) {
          val close = positionOfNextIn(afterKw, "]", 0)
          if (close >= 0) {
            fullKw = kw + afterKw.substring(0, close + 1)
            kwLen = kw.length + close + 1
          }
        }
      }

      modifierList.add(new J.Modifier(
        Tree.randomId(), spaceBefore, Markers.EMPTY, fullKw, mt, Collections.emptyList()))
      lastEnd = pos + kwLen
    }
    (modifierList, lastEnd)
  }

  /** Definition-level modifiers (class/object/trait/def/val). Skips `case` — needs special handling. */
  private val definitionModifierKeywords: List[(String, J.Modifier.Type)] = List(
    ("private", J.Modifier.Type.Private),
    ("protected", J.Modifier.Type.Protected),
    ("abstract", J.Modifier.Type.Abstract),
    ("final", J.Modifier.Type.Final),
    ("override", J.Modifier.Type.LanguageExtension),
    ("implicit", J.Modifier.Type.LanguageExtension),
    ("sealed", J.Modifier.Type.Sealed),
    ("lazy", J.Modifier.Type.LanguageExtension)
  )

  /** Modifiers legal on a class/trait primary-constructor parameter. */
  private val constructorParamModifierKeywords: List[(String, J.Modifier.Type)] = List(
    ("override", J.Modifier.Type.LanguageExtension),
    ("private", J.Modifier.Type.Private),
    ("protected", J.Modifier.Type.Protected),
    ("final", J.Modifier.Type.Final),
    ("val", J.Modifier.Type.LanguageExtension),
    ("var", J.Modifier.Type.LanguageExtension)
  )

  private def extractModifiersFromText(mods: untpd.Modifiers, modifierText: String): (util.ArrayList[J.Modifier], Int) = {
    // `mods` is unused: `findKeyword` already enforces whole-word matching against the
    // source text, which is authoritative — Dotty flags drift for qualified access like
    // `private[scope]`, where the simple `Flags.Private` bit may not be set.
    parseModifierKeywords(modifierText, definitionModifierKeywords)
  }
  
  private def constantToJavaType(const: Constant): JavaType.Primitive = const.tag match {
    case BooleanTag => JavaType.Primitive.Boolean
    case ByteTag => JavaType.Primitive.Byte
    case CharTag => JavaType.Primitive.Char
    case ShortTag => JavaType.Primitive.Short
    case IntTag => JavaType.Primitive.Int
    case LongTag => JavaType.Primitive.Long
    case FloatTag => JavaType.Primitive.Float
    case DoubleTag => JavaType.Primitive.Double
    case StringTag => JavaType.Primitive.String
    case NullTag => JavaType.Primitive.Null
    case _ => null
  }
  
  private def visitTypeParameter(tparam: Trees.TypeDef[?]): J.TypeParameter = {
    val prefix = extractPrefix(tparam.span)

    // Extract leading annotations (e.g., @specialized(Int))
    val leadingAnnotations = new util.ArrayList[J.Annotation]()
    if (tparam.mods != null && tparam.mods.annotations.nonEmpty) {
      for (annot <- tparam.mods.annotations) {
        visitTree(annot) match {
          case ann: J.Annotation => leadingAnnotations.add(ann)
          case _ =>
        }
      }
    }

    // Check for variance annotation in the source
    val adjustedStart = Math.max(cursor, Math.max(0, tparam.span.start - offsetAdjustment))
    val adjustedEnd = Math.max(0, tparam.span.end - offsetAdjustment)
    var namePrefix = Space.EMPTY
    var nameStr = tparam.name.toString

    if (adjustedStart < adjustedEnd && adjustedStart >= cursor && adjustedEnd <= source.length) {
      val paramSource = source.substring(adjustedStart, adjustedEnd)
      // Capture leading whitespace (e.g., space after annotation) as name prefix
      val stripped = paramSource.stripLeading()
      if (stripped.length < paramSource.length) {
        namePrefix = Space.format(paramSource.substring(0, paramSource.length - stripped.length))
      }
      // Check if it starts with + or - (after stripping whitespace)
      if (stripped.startsWith("+") || stripped.startsWith("-")) {
        val variance = stripped.charAt(0)
        nameStr = variance + tparam.name.toString
        cursor = adjustedStart + (paramSource.length - stripped.length) + 1
      }
      // Check for higher-kinded type params like F[_] or F[_, _]
      // Only match `[` immediately after the name (not in bound types like `T <: Comparable[T]`)
      val nameLen = tparam.name.toString.length + (if (stripped.startsWith("+") || stripped.startsWith("-")) 1 else 0)
      val bracketIdx = if (nameLen < stripped.length && stripped.charAt(nameLen) == '[') nameLen else -1
      if (bracketIdx >= 0) {
        // Extract the full name including brackets: F[_], F[+_, -_], etc.
        // Find matching closing bracket
        var depth = 1
        var i = bracketIdx + 1
        while (i < paramSource.length && depth > 0) {
          if (paramSource.charAt(i) == '[') depth += 1
          else if (paramSource.charAt(i) == ']') depth -= 1
          i += 1
        }
        if (depth == 0) {
          nameStr = stripped.substring(0, i)
          // Trim any trailing bound syntax (e.g., F[_] <: Bound)
          val trimmed = nameStr.trim
          if (trimmed.nonEmpty) nameStr = trimmed
        }
      }
    }

    // Advance cursor past the name (including any [_] brackets).
    // Use source scanning to find the actual end position.
    if (nameStr.contains("[")) {
      // Higher-kinded: find the matching ] after the name
      val nameStart = adjustedStart + (if (nameStr.startsWith("+") || nameStr.startsWith("-")) 1 else 0)
      val bracketStart = positionOfNext("[", nameStart)
      if (bracketStart >= 0) {
        var depth = 1
        var i = bracketStart + 1
        while (i < source.length && depth > 0) {
          if (source.charAt(i) == '[') depth += 1
          else if (source.charAt(i) == ']') depth -= 1
          i += 1
        }
        if (depth == 0 && i > cursor) cursor = i
      }
    } else {
      // Simple name: advance past leading whitespace + name
      val wsLen = if (adjustedStart < adjustedEnd) (source.substring(adjustedStart, adjustedEnd).length - source.substring(adjustedStart, adjustedEnd).stripLeading().length) else 0
      val nameEnd = adjustedStart + wsLen + nameStr.length
      if (nameEnd > cursor && nameEnd <= source.length) cursor = nameEnd
    }

    // Extract the type parameter name
    val name = ident(nameStr, namePrefix)

    def contextBoundName(cxBound: Trees.Tree[?]): String = {
      def selectToName(t: Trees.Tree[?]): String = t match {
        case id: Trees.Ident[?] => id.name.toString
        case sel: Trees.Select[?] => selectToName(sel.qualifier) + "." + sel.name.toString
        case _ => t.toString
      }
      cxBound match {
        case cbt: untpd.ContextBoundTypeTree => selectToName(cbt.tycon)
        case _ => cxBound.toString
      }
    }

    def contextBoundStart(cxBound: Trees.Tree[?]): Int = cxBound match {
      case cbt: untpd.ContextBoundTypeTree if cbt.tycon.span.exists =>
        Math.max(0, cbt.tycon.span.start - offsetAdjustment)
      case _ if cxBound.span.exists =>
        Math.max(0, cxBound.span.start - offsetAdjustment)
      case _ => indexOfNextNonWhitespace(cursor)
    }

    def appendContextBounds(
      cxBoundsList: scala.collection.immutable.List[Trees.Tree[?]],
      boundList: util.ArrayList[JRightPadded[TypeTree]],
      cbEnd: Int
    ): Space = {
      var beforeFirstColon = Space.EMPTY
      cxBoundsList.zipWithIndex.foreach { case (cxBound, idx) =>
        val colonIdx = if (cursor < cbEnd) positionOfNext(":", cursor) else -1
        if (idx == 0 && colonIdx > cursor && colonIdx < cbEnd) {
          beforeFirstColon = ScalaSpace.format(source, cursor, colonIdx)
        }
        if (colonIdx >= 0) cursor = colonIdx + 1

        val boundStart = contextBoundStart(cxBound)
        val boundPrefix = if (boundStart > cursor && boundStart <= source.length) {
          ScalaSpace.format(source, cursor, boundStart)
        } else Space.EMPTY
        val boundName = contextBoundName(cxBound)
        val boundId: TypeTree = ident(boundName, boundPrefix)
        boundList.add(JRightPadded.build(boundId))

        if (cxBound.span.exists) updateCursor(cxBound.span.end)
        else if (boundStart >= 0) cursor = Math.max(cursor, boundStart + boundName.length)
      }
      beforeFirstColon
    }
    
    // Handle bounds from AST.
    // - TypeBoundsTree: upper/lower bounds like `<: Comparable` or `>: Null`
    // - untpd.ContextBounds: context bounds like `: ClassTag` (wraps TypeBoundsTree + cxBounds list)
    // Each bound is wrapped in J.TypeBound to carry its Kind (Upper/Lower).
    val bounds: JContainer[TypeTree] = tparam.rhs match {
      case cb: untpd.ContextBounds =>
        // Context bounds: [T: ClassTag] or [T: Ordering : Show]
        // May also have type bounds: [V >: Null : ClassTag]
        val innerBounds = cb.bounds
        val hasTypeBounds = innerBounds != null && (!innerBounds.lo.isEmpty || !innerBounds.hi.isEmpty)
        val cxBoundsList: scala.collection.immutable.List[Trees.Tree[?]] = cb.cxBounds
        if (hasTypeBounds) {
          // Combined bounds like `>: Null : ClassTag`
          // Model type bounds as TypeBound elements and context bounds as plain
          // type trees so the printer can re-emit the `:` syntax.
          val boundList = new util.ArrayList[JRightPadded[TypeTree]]()
          if (!innerBounds.lo.isEmpty) {
            val loOpIdx = positionOfNext(">:", cursor)
            val loPrefix = if (loOpIdx > cursor) ScalaSpace.format(source, cursor, loOpIdx) else Space.EMPTY
            if (loOpIdx >= 0) cursor = loOpIdx + 2
            val savedC = cursor
            visitTree(innerBounds.lo) match {
              case tt: TypeTree =>
                val loBound: TypeTree = new J.TypeBound(Tree.randomId(), loPrefix, Markers.EMPTY, J.TypeBound.Kind.Lower, tt)
                boundList.add(JRightPadded.build(loBound))
              case _ => cursor = savedC
            }
          }
          if (!innerBounds.hi.isEmpty) {
            val hiOpIdx = positionOfNext("<:", cursor)
            val hiPrefix = if (hiOpIdx > cursor) ScalaSpace.format(source, cursor, hiOpIdx) else Space.EMPTY
            if (hiOpIdx >= 0) cursor = hiOpIdx + 2
            val savedC = cursor
            visitTree(innerBounds.hi) match {
              case tt: TypeTree =>
                val hiBound: TypeTree = new J.TypeBound(Tree.randomId(), hiPrefix, Markers.EMPTY, J.TypeBound.Kind.Upper, tt)
                boundList.add(JRightPadded.build(hiBound))
              case _ => cursor = savedC
            }
          }
          var contextBoundBefore = Space.EMPTY
          if (cxBoundsList.nonEmpty) {
            val cbEnd = if (cb.span.exists) Math.max(0, cb.span.end - offsetAdjustment) else source.length
            contextBoundBefore = appendContextBounds(cxBoundsList, boundList, cbEnd)
          } else if (cb.span.exists) {
            updateCursor(cb.span.end)
          }
          if (!boundList.isEmpty) JContainer.build(contextBoundBefore, boundList, Markers.EMPTY) else null
        } else if (cxBoundsList.nonEmpty) {
          val cbEnd = if (cb.span.exists) Math.max(0, cb.span.end - offsetAdjustment) else source.length
          val boundList = new util.ArrayList[JRightPadded[TypeTree]]()
          val beforeColon = appendContextBounds(cxBoundsList, boundList, cbEnd)
          // The JContainer.before space is printed BEFORE the `:` by the printer.
          JContainer.build(beforeColon, boundList, Markers.EMPTY)
        } else null

      case tb: Trees.TypeBoundsTree[?] if !tb.hi.isEmpty || !tb.lo.isEmpty =>
        // Upper and/or lower bounds: [T <: Comparable], [T >: Null], [T >: Lower <: Upper]
        val boundList = new util.ArrayList[JRightPadded[TypeTree]]()

        // Lower bound first (if present) — appears first in source for [T >: L <: H]
        if (!tb.lo.isEmpty) {
          val loOpIdx = positionOfNext(">:", cursor)
          val loPrefix = if (loOpIdx > cursor) ScalaSpace.format(source, cursor, loOpIdx) else Space.EMPTY
          if (loOpIdx >= 0) cursor = loOpIdx + 2
          val savedCursorLo = cursor
          val loType = visitTree(tb.lo) match {
            case tt: TypeTree => tt
            case _ => cursor = savedCursorLo; visitUnknown(tb.lo).asInstanceOf[TypeTree]
          }
          val loBound: TypeTree = new J.TypeBound(Tree.randomId(), loPrefix, Markers.EMPTY,
            J.TypeBound.Kind.Lower, loType)
          boundList.add(JRightPadded.build(loBound))
        }

        // Upper bound (if present)
        if (!tb.hi.isEmpty) {
          val hiOpIdx = positionOfNext("<:", cursor)
          val hiPrefix = if (hiOpIdx > cursor) ScalaSpace.format(source, cursor, hiOpIdx) else Space.EMPTY
          if (hiOpIdx >= 0) cursor = hiOpIdx + 2
          val savedCursorHi = cursor
          val hiType = visitTree(tb.hi) match {
            case tt: TypeTree => tt
            case _ => cursor = savedCursorHi; visitUnknown(tb.hi).asInstanceOf[TypeTree]
          }
          val hiBound: TypeTree = new J.TypeBound(Tree.randomId(), hiPrefix, Markers.EMPTY,
            J.TypeBound.Kind.Upper, hiType)
          boundList.add(JRightPadded.build(hiBound))
        }

        if (!boundList.isEmpty) JContainer.build(Space.EMPTY, boundList, Markers.EMPTY) else null

      case _ => null
    }

    new J.TypeParameter(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      leadingAnnotations,
      Collections.emptyList(), // modifiers
      name,
      bounds
    )
  }
  
  private def extractTypeParametersSource(td: Trees.TypeDef[?]): String = {
    // This method is not actually used anymore since we get type params from the AST
    // We only need to update the cursor position correctly
    ""
  }
  
  private def visitTyped(typed: Trees.Typed[?]): J = {

    // Call-site varargs splat. Scala 3 form: `f(xs*)`. Scala 2 form: `f(xs: _*)`.
    // Both parse to `Typed(expr, Ident("_*"))`. Distinguish by inspecting the source
    // between the expression and the trailing `*`: a `:` indicates Scala 2 form.
    typed.tpt match {
      case id: Trees.Ident[?] if id.name.toString == "_*" =>
        val prefix = extractPrefix(typed.span)
        val element = visitTree(typed.expr) match {
          case e: Expression => e
          case other => throw new IllegalStateException(
            s"Varargs splat element must be an Expression, got: ${if (other == null) "null" else other.getClass.getName}")
        }
        val typedEnd = if (typed.span.exists) Math.max(0, typed.span.end - offsetAdjustment) else cursor
        val starPos = source.indexOf('*', cursor)
        val starIdx = if (starPos >= cursor && starPos < typedEnd) starPos else typedEnd - 1
        val between = if (cursor >= 0 && starIdx >= cursor && starIdx <= source.length) {
          source.substring(cursor, starIdx)
        } else ""
        val colonIdx = between.indexOf(':')
        val (beforeColon, afterColon, beforeStar) = if (colonIdx >= 0) {
          // Scala 2 form: `<expr> [ws]:[ws]_[ws]*`
          val bColon = Space.format(between.substring(0, colonIdx))
          val afterColonStart = cursor + colonIdx + 1
          val underscoreIdx = source.indexOf('_', afterColonStart)
          val uIdx = if (underscoreIdx >= afterColonStart && underscoreIdx < starIdx) underscoreIdx else starIdx
          val aColon = ScalaSpace.format(source, afterColonStart, uIdx)
          val bStar = ScalaSpace.format(source, uIdx + 1, starIdx)
          (bColon, aColon, bStar)
        } else {
          // Scala 3 form: `<expr>[ws]*`
          (null.asInstanceOf[Space], null.asInstanceOf[Space], Space.format(between))
        }
        updateCursor(typed.span.end)
        return new S.SplatExpression(
          Tree.randomId(),
          prefix,
          Markers.EMPTY,
          element,
          beforeColon,
          afterColon,
          beforeStar,
          typeOfTree(typed)
        )
      case _ =>
    }

    // Check if this is a member reference pattern (expr _)
    typed.tpt match {
      case id: Trees.Ident[?] if id.name.toString == "_" =>
        // This is a member reference like "greet _"
        val prefix = extractPrefix(typed.span)
        
        // Visit the expression part (the method/field being referenced)
        val expr = visitTree(typed.expr) match {
          case e: Expression => e
          case j: J => new S.StatementExpression(Tree.randomId(), j)
          case _ => return visitUnknown(typed)
        }
        
        // Create a member reference
        new J.MemberReference(
          Tree.randomId(),
          prefix,
          Markers.EMPTY,
          JRightPadded.build(expr),
          null, // No type parameters for now
          JLeftPadded.build(ident("_", Space.SINGLE_SPACE, typeFor(typed.span))),
          typeFor(typed.span),
          methodTypeFor(typed.span),
          variableTypeFor(typed.span)
        )
      case _ =>
        // Type ascription: expr: Type
        val savedCursor = cursor
        try {
          val prefix = extractPrefix(typed.span)

          // Visit the expression
          val expr = visitTree(typed.expr) match {
            case e: Expression => e
            case j: J => new S.StatementExpression(Tree.randomId(), j)
            case _ =>
              cursor = savedCursor
              return visitUnknown(typed)
          }

          // Find the colon between expression and type. Capture the space BEFORE the colon
          // separately from the space AFTER the colon (which becomes the type tree's prefix).
          val beforeColon: Space = {
            val tptStart = Math.max(0, typed.tpt.span.start - offsetAdjustment)
            if (tptStart > cursor) {
              val between = source.substring(cursor, tptStart)
              val colonIdx = positionOfNextIn(between, ":", 0)
              if (colonIdx >= 0) {
                cursor = cursor + colonIdx + 1 // Skip past ':'
                Space.format(between.substring(0, colonIdx))
              } else {
                Space.EMPTY
              }
            } else {
              Space.EMPTY
            }
          }

          // Visit the type tree — its natural prefix is the space AFTER the colon.
          // Prefer visitTypeTree so tuple `(A, B)`, function `A => B`, and repeated
          // `T*` ascriptions map to proper TypeTrees instead of failing as unknown.
          val typeTree = visitTypeTree(typed.tpt)
          if (typeTree == null) {
            cursor = savedCursor
            return visitUnknown(typed)
          }

          updateCursor(typed.span.end)

          val markers = if (beforeColon != Space.EMPTY) {
            Markers.build(Collections.singletonList(
              org.openrewrite.scala.marker.TypeAscriptionColonPrefix.create(beforeColon)))
          } else Markers.EMPTY
          new S.TypeAscription(
            Tree.randomId(),
            prefix,
            markers,
            expr,
            typeTree,
            typeFor(typed.span)
          )
        } catch {
          case _: Exception =>
            cursor = savedCursor
            visitUnknown(typed)
        }
    }
  }
  
  private def visitFunction(func: untpd.Function): J = {
    
    // Check if this is a partially applied function or underscore placeholder lambda
    // In Scala, `add(5, _)` is parsed as Function(List(_$1), Apply(add, List(5, _$1)))
    // Also `_ * 2` is parsed as Function(List(_$1), InfixOp(_$1, *, 2))
    // We need to detect these patterns and handle them specially
    
    // Get the parameter names generated by the compiler (like _$1, _$2, etc.)
    val syntheticParams = func.args.collect {
      case vd: Trees.ValDef[?] if vd.name.toString.startsWith("_$") => vd.name.toString
    }.toSet
    
    if (syntheticParams.nonEmpty) {
      // Check if the source contains actual underscore placeholders.
      // `extractSource` advances `cursor` to the span end, which would clobber
      // the prefix extraction below — save and restore so leading whitespace
      // (e.g. the space after a comma in `foo(a, _.toString)`) is preserved.
      val savedCursor = cursor
      val funcSource = extractSource(func.span)
      cursor = savedCursor
      // An explicit `_ => expr` lambda has `=>` between the parameter and the body.
      // For a placeholder lambda like `_.filter(f => f > 0)`, the body starts at the
      // `_` itself, so any `=>` is inside the body, not before it. Only treat the
      // function as an explicit underscore lambda when `=>` appears before the body.
      val isExplicitUnderscoreLambda = if (func.body.span.exists) {
        val funcStart = Math.max(0, func.span.start - offsetAdjustment)
        val bodyStart = Math.max(0, func.body.span.start - offsetAdjustment)
        bodyStart > funcStart && bodyStart <= source.length &&
          source.substring(funcStart, bodyStart).contains("=>")
      } else false
      val hasUnderscorePlaceholder = funcSource.contains("_") && !isExplicitUnderscoreLambda

      // If we have synthetic params and underscore in source, it's likely a placeholder lambda
      // These should be treated as regular lambdas but we skip the synthetic param
      if (hasUnderscorePlaceholder) {
        // Detect partial application like `add(5, _)` where the synthetic param
        // appears as a *direct argument* of an Apply. The placeholder being the
        // receiver of an Apply (e.g. `_.substring(0, 1)`) is NOT partial application
        // and must be handled as a regular underscore placeholder lambda.
        val partialApplication: Option[Trees.Apply[?]] = func.body match {
          case app: Trees.Apply[?] if app.args.exists {
            case id: Trees.Ident[?] => syntheticParams.contains(id.name.toString)
            case _ => false
          } => Some(app)
          case _ => None
        }

        partialApplication match {
          case Some(app) =>
            // Partially applied function - return the underlying call with the lambda's
            // leading whitespace re-attached. Covers method invocations (`add(5, _)`) as
            // well as constructor applications (`new Foo(_)`), which yield a J.NewClass
            // whose own prefix is empty because `extractPrefix` already consumed it.
            val prefix = extractPrefix(func.span)
            val result = visitApply(app)
            result match {
              case e: Expression => return e.withPrefix(prefix).asInstanceOf[Expression]
              case _ => return result
            }
          case None =>
            // For all other cases (like `_ * 2` or `_.substring(0, 1)`),
            // this is an underscore placeholder lambda.
            // We need to create a proper lambda with S.Wildcard in the body.
            val prefix = extractPrefix(func.span)

            // Set a flag to indicate we're in an underscore placeholder context
            val oldSyntheticParams = currentSyntheticParams
            currentSyntheticParams = syntheticParams

            // Visit the body - the visitIdent method will now create S.Wildcard for synthetic params
            val body = visitTree(func.body)

            // Restore the flag
            currentSyntheticParams = oldSyntheticParams

            // Create a wildcard parameter for the lambda parameter list
            val wildcard = new S.Wildcard(
              Tree.randomId(),
              Space.EMPTY,
              Markers.EMPTY,
              null
            )

            val params = new util.ArrayList[JRightPadded[J]]()
            params.add(JRightPadded.build(wildcard))

            val parameters = new J.Lambda.Parameters(
              Tree.randomId(),
              Space.EMPTY,
              Markers.EMPTY,
              false, // no parentheses for underscore syntax
              params
            )

            // Create lambda with the underscore placeholder marker
            val lambda = new J.Lambda(
              Tree.randomId(),
              prefix,
              Markers.build(Collections.singletonList(new UnderscorePlaceholderLambda(Tree.randomId()))),
              parameters,
              Space.EMPTY, // no space before => for underscore syntax
              body,
              null
            )

            return lambda
        }
      }
    }
    
    val prefix = extractPrefix(func.span)

    // Build lambda parameters
    val parameters = new J.Lambda.Parameters(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      false, // parenthesized - will be set based on syntax
      Collections.emptyList() // Will fill in the parameters
    )
    
    // Visit lambda parameters
    val params = new util.ArrayList[JRightPadded[J]]()
    var hasParentheses = false
    
    // Check if parameters are parenthesized by looking at the source.
    // `extractSource` advances cursor to the span end; save and restore so the
    // subsequent loop can extract prefixes from the right position.
    val savedCursorBeforeFunc = cursor
    val funcSource = extractSource(func.span)
    cursor = savedCursorBeforeFunc
    hasParentheses = funcSource.trim.startsWith("(")
    val arrowIndex = positionOfNextIn(funcSource, "=>", 0)

    // When parenthesized, advance cursor past `(` so the first parameter's
    // extractPrefix captures the whitespace between `(` and the first arg.
    // For braceless/curly forms, leave cursor at func.span.start so that any
    // pre-name modifier (e.g. `implicit`) is handled by visitLambdaParameter
    // rather than being absorbed into the parameter's prefix.
    if (hasParentheses) {
      val openParen = positionOfNext("(", cursor)
      if (openParen >= cursor && openParen < source.length) {
        cursor = openParen + 1
      }
    } else {
      // Skip cursor past the func.span (matches pre-fix behavior) so a leading
      // `implicit` keyword on the first parameter isn't double-counted.
      val funcEnd = Math.max(0, func.span.end - offsetAdjustment)
      if (funcEnd >= cursor && funcEnd <= source.length) cursor = funcEnd
    }

    for (i <- func.args.indices) {
      val param = func.args(i)

      // For parameters after the first, we need to handle the comma and space
      if (i > 0) {
        // Look for comma between previous and current parameter
        val prevParam = func.args(i - 1)
        val prevEnd = prevParam.span.end - offsetAdjustment
        val currentStart = param.span.start - offsetAdjustment

        if (prevEnd < currentStart && prevEnd >= cursor && currentStart <= source.length) {
          val between = source.substring(prevEnd, currentStart)
          val commaIdx = positionOfNextIn(between, ",", 0)
          if (commaIdx >= 0) {
            // Move cursor past the comma
            cursor = prevEnd + commaIdx + 1
          }
        }
      }

      // Visit parameter as lambda parameter - it will extract its own prefix
      val paramTree = param match {
        case vd: Trees.ValDef[?] => visitLambdaParameter(vd)
        case _ => visitTree(param)
      }

      // Extract space after the parameter (before comma or closing paren)
      var afterSpace = Space.EMPTY
      if (i < func.args.length - 1) {
        // Not the last parameter, space before comma
        val currentEnd = param.span.end - offsetAdjustment
        if (currentEnd >= cursor) {
          // Look for comma after this parameter
          val searchStart = Math.max(cursor, currentEnd)
          val commaSearchEnd = Math.min(searchStart + 20, source.length) // reasonable search distance
          val searchStr = source.substring(searchStart, commaSearchEnd)
          val commaIdx = positionOfNextIn(searchStr, ",", 0)
          if (commaIdx >= 0) {
            afterSpace = Space.format(searchStr.substring(0, commaIdx))
            cursor = searchStart + commaIdx + 1
          }
        }
      } else {
        // Last parameter — when parenthesized, capture whitespace before `)`.
        val paramEnd = param.span.end - offsetAdjustment
        if (paramEnd >= cursor) {
          cursor = paramEnd
        }
        if (hasParentheses) {
          val closeParen = positionOfNext(")", cursor)
          if (closeParen >= cursor && closeParen < source.length) {
            afterSpace = ScalaSpace.format(source, cursor, closeParen)
            cursor = closeParen
          }
        }
      }

      params.add(JRightPadded.build(paramTree).withAfter(afterSpace))
    }
    
    // Update parameters with the actual params
    val updatedParams = new J.Lambda.Parameters(
      parameters.getId,
      parameters.getPrefix,
      parameters.getMarkers,
      hasParentheses,
      params
    )
    
    // Extract arrow and spacing (arrowIndex already computed above).
    // Scala 3 context-function arrow `?=>` puts a `?` immediately before `=>`;
    // treat the `?` as part of the arrow so that whitespace before it is captured
    // as the arrow prefix.
    var arrowPrefix = Space.EMPTY
    var isContextArrow = false
    if (arrowIndex >= 0) {
      val arrowTokenStart = if (arrowIndex > 0 && funcSource.charAt(arrowIndex - 1) == '?') {
        isContextArrow = true
        arrowIndex - 1
      } else arrowIndex
      // Find the space before the arrow token
      var spaceStart = arrowTokenStart - 1
      while (spaceStart >= 0 && Character.isWhitespace(funcSource.charAt(spaceStart))) {
        spaceStart -= 1
      }
      if (spaceStart < arrowTokenStart - 1) {
        arrowPrefix = Space.format(funcSource.substring(spaceStart + 1, arrowTokenStart))
      }
      // Move cursor to right after the arrow (past =>)
      val arrowEndPos = func.span.start + arrowIndex + 2 - offsetAdjustment
      cursor = arrowEndPos
    }

    // Visit the lambda body - it will extract its own prefix including space after =>
    val body = visitTree(func.body)

    val lambdaMarkers = if (isContextArrow) {
      Markers.build(Collections.singletonList(new ContextFunctionArrow(java.util.UUID.randomUUID())))
    } else Markers.EMPTY

    new J.Lambda(
      Tree.randomId(),
      prefix,
      lambdaMarkers,
      updatedParams,
      arrowPrefix,
      body,
      typeOfTree(func)
    )
  }

  /** Extract a fully qualified name from an AST expression (e.g., Select(Ident("java"), "util") → "java.util"). */
  private def extractFqn(tree: Trees.Tree[?]): String = tree match {
    case id: Trees.Ident[?] => id.name.toString
    case sel: Trees.Select[?] => extractFqn(sel.qualifier) + "." + sel.name.toString
    case _ => ""
  }

  private def visitTuple(tuple: untpd.Tuple): J = {
    val prefix = extractPrefix(tuple.span)

    // Consume the opening paren
    sourceBefore("(")

    // Visit each element
    val elements = new util.ArrayList[JRightPadded[Expression]]()
    for (i <- tuple.trees.indices) {
      val elem = visitTree(tuple.trees(i)) match {
        case expr: Expression => expr
        case j: J => new S.StatementExpression(Tree.randomId(), j)
        case _ => return visitUnknown(tuple)
      }
      val after = if (i < tuple.trees.size - 1) sourceBefore(",") else sourceBefore(")")
      elements.add(JRightPadded.build(elem).withAfter(after))
    }

    if (tuple.trees.isEmpty) {
      sourceBefore(")")
    }

    S.TuplePattern.build(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      JContainer.build(Space.EMPTY, elements, Markers.EMPTY),
      typeOfTree(tuple)
    )
  }

  def getRemainingSource: String = {
    if (cursor < source.length) {
      val remaining = source.substring(cursor)
      // If we have offset adjustment (wrapped expression), we might have extra wrapper code
      // Check if remaining is just whitespace or closing braces from the wrapper
      if (offsetAdjustment > 0) {
        val trimmed = remaining.trim
        // Check if it's just the closing brace from the wrapper
        if (trimmed == "}" || trimmed.isEmpty) {
          ""
        } else {
          remaining
        }
      } else {
        remaining
      }
    } else {
      ""
    }
  }
}
