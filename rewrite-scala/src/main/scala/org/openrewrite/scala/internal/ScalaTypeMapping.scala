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

import dotty.tools.dotc.ast.{tpd, untpd, Trees}
import dotty.tools.dotc.core.Constants
import dotty.tools.dotc.core.StdNames.nme
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.core.Symbols
import dotty.tools.dotc.core.Symbols.Symbol
import dotty.tools.dotc.core.Types.*
import dotty.tools.dotc.util.Spans

import org.openrewrite.java.internal.{JavaReflectionTypeMapping, JavaTypeFactory}
import org.openrewrite.java.tree.{Flag, JavaType, TypeUtils}

import java.util
import java.util.{ArrayList, Collections, IdentityHashMap}
import scala.collection.mutable

/**
 * Maps Dotty compiler types to OpenRewrite's JavaType model.
 *
 * Nodes of the untyped tree are first looked up in [[TypedNodes]], which holds the typed tree
 * the typer produced from each of them. For nodes the typer didn't record, there are two
 * further lookup strategies:
 * 1. Span-based: maps source position → typed tree node for direct type extraction.
 *    Typed nodes spanning exactly the untpd node's source range are preferred; nodes
 *    the typer marks synthetic (e.g. the `Apply` of an infix operation) still cover
 *    the right source range, so they are kept as a lower-priority match.
 * 2. Name-based: maps symbol name → Symbol for fallback when spans don't match.
 *
 * Both maps are built from the typed tree using Dotty's TreeTraverser.
 */
class ScalaTypeMapping(typeFactory: JavaTypeFactory, typedTree: tpd.Tree,
                       typedNodes: TypedNodes = TypedNodes.empty)(using ctx: Context) {

  private val signatureBuilder = new ScalaTypeSignatureBuilder
  private val unknown: JavaType = JavaType.Unknown.getInstance()

  // For mapping Scala constant tags and other real Java classes (like
  // java.lang.String) through proper reflection-driven type attribution
  // rather than a synthetic stub.
  private val reflectionTypeMapping = new JavaReflectionTypeMapping(typeFactory)

  // Span-based lookup: start position → typed tree nodes in pre-order (multiple nodes may share a start position)
  private val spanMap: mutable.Map[Int, mutable.ListBuffer[tpd.Tree]] = mutable.Map.empty

  // Name-based lookup: symbol name → Symbol (fallback)
  private val symbolsByName: mutable.Map[String, Symbol] = mutable.Map.empty

  // Implicit conversions the typer inserted, by the tree they convert
  private val conversions = new IdentityHashMap[tpd.Tree, tpd.Apply]()

  // Build both maps via a single traversal
  buildMaps()

  private def buildMaps(): Unit = {
    try {
      val traverser = new tpd.TreeTraverser {
        override def traverse(tree: tpd.Tree)(using Context): Unit = {
          // Index by start position for span lookup
          if (tree.span.exists && !tree.span.isZeroExtent && !tree.isInstanceOf[Trees.InferredTypeTree[?]]) {
            spanMap.getOrElseUpdate(tree.span.start, mutable.ListBuffer.empty) += tree
          }

          tree match {
            case app @ Trees.Apply(fn, List(arg)) if isInsertedConversion(app, fn, arg) => conversions.put(arg, app)
            case _ =>
          }

          // Index by name for name-based fallback
          try {
            val sym = tree.symbol
            if (sym != null && sym.exists && sym.name != null && !isErrorSymbol(sym) && !sym.is(Flags.Package)) {
              val name = sym.name.toString
              if (name.nonEmpty && name != "<none>" && name != "<empty>") {
                symbolsByName.put(name, sym)
              }
            }
          } catch {
            case _: Throwable =>
          }

          traverseChildren(tree)
        }
      }
      traverser.traverse(typedTree)
    } catch {
      case _: Throwable => // If traversal fails, maps stay empty; types will be null
    }
  }

  /**
   * The typer inserts a conversion as a call spanning just the converted tree, which
   * tells it apart from a call of the same method the author wrote.
   */
  private def isInsertedConversion(app: tpd.Apply, fn: tpd.Tree, arg: tpd.Tree): Boolean =
    try {
      app.span.exists && arg.span.exists &&
        app.span.start == arg.span.start && app.span.end == arg.span.end && {
        val sym = fn.symbol
        sym.exists && sym.is(Flags.Method) && !sym.isConstructor &&
          (sym.isOneOf(Flags.GivenOrImplicit) || sym.name == nme.apply && sym.owner.derivesFrom(ctx.definitions.ConversionClass))
      }
    } catch {
      case _: Throwable => false
    }

  // --- Node-based lookup ---

  /** The type of the node as typed on its own, before any adaptation to its context. */
  def typeOf(tree: untpd.Tree): Option[JavaType] =
    typedNodes.get(tree).flatMap(node => Option(expressionType(resolved(node))))

  /**
   * The node typed on its own, with the alternative of an overloaded method it refers to
   * resolved. The typer picks the alternative while adapting the reference to its context.
   */
  private def resolved(node: TypedNode): tpd.Tree = {
    val own = node.own
    if ((node.adapted eq own) || !isOverloaded(own)) own
    else {
      var found: tpd.Tree = null
      try {
        new tpd.TreeTraverser {
          override def traverse(tree: tpd.Tree)(using Context): Unit =
            if (found == null) {
              if (tree.getClass == own.getClass && tree.span == own.span && !isOverloaded(tree)) found = tree
              else traverseChildren(tree)
            }
        }.traverse(node.adapted)
      } catch {
        case _: Throwable =>
      }
      if (found != null) found else own
    }
  }

  private def isOverloaded(tree: tpd.Tree): Boolean =
    try tree.tpe match {
      case ref: TermRef => ref.denot.isOverloaded
      case _ => false
    } catch {
      case _: Throwable => false
    }

  /**
   * The method a call, method reference or method definition refers to. A call that doesn't
   * type-check has none, even if the compiler resolved the name of the method it calls.
   */
  def methodTypeOf(tree: untpd.Tree): Option[JavaType.Method] =
    typedNodes.get(tree).flatMap { node =>
      if (isIllTypedCall(tree, node)) Some(null)
      else calledSymbol(resolved(node)).filterNot(isErrorSymbol).map(sym => try mapMethodType(sym) catch { case _: Throwable => null })
    }

  /**
   * Whether the node refers to a method taking parameters, so that applying it calls the method
   * rather than the `apply` of its result. This holds even when the application doesn't type-check.
   */
  def refersToMethodWithParameters(tree: untpd.Tree): Boolean =
    typedNodes.get(tree).exists { node =>
      try node.own.tpe match {
        case ref: TermRef =>
          val alternatives = ref.denot.alternatives
          alternatives.nonEmpty && alternatives.forall(d => d.symbol.is(Flags.Method) && d.info.stripPoly.isInstanceOf[MethodType])
        case _ => false
      } catch {
        case _: Throwable => false
      }
    }

  /** The parameter that the named argument `name` of the call is passed to. */
  def parameterOf(call: untpd.Tree, name: String): Option[JavaType.Variable] =
    typedNodes.get(call)
      .flatMap(node => calledSymbol(application(resolved(node))))
      .flatMap(method => method.paramSymss.flatten.find(p => p.isTerm && p.name.toString == name))
      .flatMap(parameter => Option(try mapVariableType(parameter) catch { case _: Throwable => null }))

  /** Named arguments passed out of order are lifted into a block that ends with the call itself. */
  private def application(tree: tpd.Tree): tpd.Tree = tree match {
    case Trees.Block(_, expr) => application(expr)
    case Trees.Typed(expr, _) => application(expr)
    case _ => tree
  }

  /** The variable a definition or reference refers to, if it refers to a variable. */
  def variableTypeOf(tree: untpd.Tree): Option[JavaType.Variable] =
    typedNodes.get(tree).flatMap { node =>
      val sym = try resolved(node).symbol catch { case _: Throwable => null }
      if (sym == null || !sym.exists) None
      else Some(if (isVariable(sym)) try mapVariableType(sym) catch { case _: Throwable => null } else null)
    }

  /** The implicit conversion the typer applied to the node's value, if any. */
  def implicitConversionOf(tree: untpd.Tree): Option[(JavaType.Method, JavaType)] =
    typedNodes.get(tree).flatMap { node =>
      Option(conversions.get(node.adapted)).orElse(Option(conversions.get(node.own)))
    }.map { conversion =>
      val sym = conversion.fun.symbol
      (if (isErrorSymbol(sym)) null else try mapMethodType(sym) catch { case _: Throwable => null }, expressionType(conversion))
    }

  /** The method and resulting function type, if the typer eta-expanded the method the node refers to. */
  def etaExpansionOf(tree: untpd.Tree): Option[(JavaType.Method, JavaType)] =
    typedNodes.get(tree).filter(node => (node.adapted ne node.own) && isClosure(node.adapted)).flatMap { node =>
      calledSymbol(resolved(node)).filterNot(isErrorSymbol).map { sym =>
        (try mapMethodType(sym) catch { case _: Throwable => null }, expressionType(node.adapted))
      }
    }

  private def isClosure(tree: tpd.Tree): Boolean = tree match {
    case Trees.Block(List(_: Trees.DefDef[?]), _: Trees.Closure[?]) => true
    case Trees.Block(List(_: Trees.DefDef[?]), Trees.Typed(_: Trees.Closure[?], _)) => true
    case Trees.Typed(expr, _) => isClosure(expr)
    case _ => false
  }

  /**
   * Whether the call itself fails to type-check: the method can't be resolved or applied,
   * or an argument doesn't conform to the parameter it is passed to. A call with an argument
   * that doesn't type-check itself, typically for a missing dependency, can't be judged.
   */
  private def isIllTypedCall(tree: untpd.Tree, node: TypedNode): Boolean = {
    val arguments: List[untpd.Tree] = tree match {
      case app: untpd.Apply => app.args
      case infix: untpd.InfixOp => List(infix.right)
      case _ => return false
    }
    def flatten(arg: untpd.Tree): List[untpd.Tree] = arg match {
      case Trees.NamedArg(_, value) => flatten(value)
      case untpd.Parens(inner) => flatten(inner)
      case untpd.Tuple(elems) => elems.flatMap(flatten)
      case _ => List(arg)
    }
    val typedArguments = arguments.flatMap(flatten).flatMap(typedNodes.get)
    if (typedArguments.exists(a => isErrorType(a.own))) false
    else isErrorType(node.own) || typedArguments.exists(a => isErrorType(a.adapted))
  }

  private def isErrorType(tree: tpd.Tree): Boolean =
    try tree.tpe.isError catch { case _: Throwable => false }

  // --- Public API ---

  /**
   * Look up JavaType for an untpd tree node by matching its span start position
   * to the corresponding tpd node.
   */
  def typeFor(span: Spans.Span): JavaType = {
    if (!span.exists) return null
    val exact = exactlyAt(span).filterNot(t => isLambdaScaffolding(t) || isVarargsScaffolding(t))
    exact.find(!_.span.isSynthetic).orElse(exact.headOption)
      .orElse(spanMap.get(span.start).flatMap(_.filterNot(_.span.isSynthetic).lastOption))
      .map(expressionType)
      .orNull
  }

  /** Look up the function type of a lambda, whose span the typer shares between the closure and its body. */
  def closureTypeFor(span: Spans.Span): JavaType = {
    if (!span.exists) return null
    exactlyAt(span).collectFirst {
      case b @ Trees.Block(_, _: Trees.Closure[?]) => b
      case c: Trees.Closure[?] => c
    }.map(expressionType).getOrElse(typeFor(span))
  }

  private def expressionType(tree: tpd.Tree): JavaType =
    try {
      val tpe = tree.tpe
      if (tpe == null || tpe == NoType || isPackage(tree.symbol)) null
      else mapType(tpe)
    } catch {
      case _: Throwable => null
    }

  /** Typed nodes whose span covers exactly the given source range, outermost first. */
  private def exactlyAt(span: Spans.Span): List[tpd.Tree] =
    spanMap.get(span.start) match {
      case Some(trees) => trees.iterator.filter(_.span.end == span.end).toList
      case None => Nil
    }

  /** The `Block(DefDef($anonfun), Closure)` encoding of a lambda shares the lambda's span with its body. */
  private def isLambdaScaffolding(tree: tpd.Tree): Boolean = tree match {
    case _: Trees.Closure[?] | _: Trees.DefDef[?] => true
    case Trees.Block(_, _: Trees.Closure[?]) => true
    case _ => false
  }

  /** Repeated arguments are wrapped in synthetic `Typed(SeqLiteral(...))` nodes spanning the argument. */
  private def isVarargsScaffolding(tree: tpd.Tree): Boolean =
    tree.span.isSynthetic && (tree.isInstanceOf[Trees.Typed[?]] || tree.isInstanceOf[Trees.SeqLiteral[?]])

  private def isPackage(sym: Symbol): Boolean =
    sym != null && sym.exists && sym.is(Flags.Package)

  /**
   * Unresolved references are typed with error symbols: synthetic artifacts whose info is an
   * `ErrorType`, owned by whatever scope the lookup failed in (often the root package).
   */
  private def isErrorSymbol(sym: Symbol): Boolean =
    try {
      sym.isAllOf(Flags.SyntheticArtifact) && (sym.info match {
        case TypeAlias(alias) => alias.isError
        case info => info.isError
      })
    } catch {
      case _: Throwable => true
    }

  /** Look up JavaType by identifier name (fallback for when spans don't match). */
  def typeForName(name: String): JavaType = {
    if (name == null || name.isEmpty) return null
    symbolsByName.get(name) match {
      case Some(sym) =>
        try {
          val tpe = sym.info
          val mapped = if (tpe == null || tpe == NoType) null else mapType(tpe)
          if (mapped == null || mapped.isInstanceOf[JavaType.Unknown]) typeForPredefAlias(name)
          else mapped
        } catch {
          case _: Throwable => typeForPredefAlias(name)
        }
      case None => typeForPredefAlias(name)
    }
  }

  private def typeForPredefAlias(name: String): JavaType = name match {
    case "String" => JavaType.ShallowClass.build("java.lang.String")
    case "Int" => JavaType.Primitive.Int
    case "Long" => JavaType.Primitive.Long
    case "Short" => JavaType.Primitive.Short
    case "Byte" => JavaType.Primitive.Byte
    case "Char" => JavaType.Primitive.Char
    case "Float" => JavaType.Primitive.Float
    case "Double" => JavaType.Primitive.Double
    case "Boolean" => JavaType.Primitive.Boolean
    case "Unit" => JavaType.Primitive.Void
    case "Any" | "AnyRef" => JavaType.ShallowClass.build("java.lang.Object")
    case "Null" => JavaType.Primitive.Null
    case "Nothing" => JavaType.ShallowClass.build("scala.Nothing")
    case _ => null
  }

  /** Look up JavaType.Method of the call spanning the given source range. */
  def methodTypeFor(span: Spans.Span): JavaType.Method = {
    if (!span.exists) return null
    // Calls in a chain such as `a.b(x).c(y)` all start where `a` does, so only nodes
    // ending within the requested range may answer; otherwise an inner call would
    // pick up the method type of an enclosing one.
    val exact = exactlyAt(span)
    val candidates =
      if (exact.nonEmpty) exact
      else spanMap.get(span.start).map(_.iterator.filter(_.span.end <= span.end).toList).getOrElse(Nil)
    candidates.iterator.map(calledSymbol).collectFirst { case Some(sym) => sym } match {
      case Some(sym) if !isErrorSymbol(sym) =>
        try mapMethodType(sym) catch { case _: Throwable => null }
      case _ => null
    }
  }

  private def calledSymbol(tree: tpd.Tree): Option[Symbol] =
    try {
      val sym = tree.symbol
      if (sym != null && sym.exists && (sym.is(Flags.Method) || sym.isConstructor)) Some(sym)
      else tree match {
        case Trees.Apply(fun, _) =>
          val funSym = fun.asInstanceOf[tpd.Tree].symbol
          if (funSym != null && funSym.exists) Some(funSym) else None
        case _ => None
      }
    } catch {
      case _: Throwable => None
    }

  /** Look up JavaType.Method by name (fallback). */
  def methodTypeForName(name: String): JavaType.Method = {
    if (name == null || name.isEmpty) return null
    symbolsByName.get(name) match {
      case Some(sym) if (sym.is(Flags.Method) || sym.isConstructor) && !isErrorSymbol(sym) =>
        try { mapMethodType(sym) } catch { case _: Throwable => null }
      case _ => null
    }
  }

  /** Look up JavaType.Variable of the definition or reference spanning exactly the given source range. */
  def variableTypeFor(span: Spans.Span): JavaType.Variable = {
    if (!span.exists) return null
    val exact = exactlyAt(span)
    val referenced = exact.filterNot(_.span.isSynthetic) ++ exact.filter(_.span.isSynthetic)
    referenced.iterator.map(t => try t.symbol catch { case _: Throwable => null })
      .find(sym => sym != null && sym.exists) match {
      case Some(sym) if isVariable(sym) =>
        try mapVariableType(sym) catch { case _: Throwable => null }
      case _ => null
    }
  }

  private def isVariable(sym: Symbol): Boolean =
    sym.isTerm && !sym.is(Flags.Method) && !sym.is(Flags.Package) && !isErrorSymbol(sym)

  /** Look up JavaType.Variable by name (fallback). */
  def variableTypeForName(name: String): JavaType.Variable = {
    if (name == null || name.isEmpty) return null
    symbolsByName.get(name) match {
      case Some(sym) if !sym.isClass && isVariable(sym) =>
        try { mapVariableType(sym) } catch { case _: Throwable => null }
      case _ => null
    }
  }

  /** Resolve a fully-qualified type name to a JavaType using the compiler's symbol table. */
  def typeForFqn(fqn: String): JavaType = {
    if (fqn == null || fqn.isEmpty) return null
    try {
      val sym = Symbols.requiredClass(fqn)
      if (sym.exists) mapType(sym.typeRef)
      else null
    } catch {
      case _: Throwable => null
    }
  }

  // --- Core type mapping ---

  def mapType(tpe: Type): JavaType = {
    if (tpe == null || tpe == NoType) return null

    val sig = signatureBuilder.signature(tpe)

    try {
      tpe.dealias match {
        case ct: ConstantType => mapConstantType(ct)
        case tr: TypeRef => mapTypeRef(tr, sig)
        case tmr: TermRef => mapTermRef(tmr, sig)
        case tt: ThisType => mapType(tt.tref)
        case et: ExprType => mapType(et.resultType)
        case at: AppliedType => mapAppliedType(at, sig)
        case tp: TypeParamRef => mapGenericTypeVariable(tp, sig)
        case tb: TypeBounds => mapTypeBounds(tb, sig)
        case at: AndType => mapType(at.tp1)
        case _: OrType => unknown
        case ci: ClassInfo =>
          // Use the class symbol's TypeRef signature to be consistent with TypeRef-based lookups.
          // This prevents creating duplicate JavaType.Class instances for the same type.
          val rawFqn = ci.cls.fullName.toString
          val fqn = if (rawFqn.endsWith("$")) rawFqn.dropRight(1) else rawFqn
          val normalizedFqn = fqn match {
            case "scala.Any" | "scala.AnyRef" => "java.lang.Object"
            case other => other
          }
          mapClassType(ci.cls, normalizedFqn, normalizedFqn)
        case mt: MethodType => mapMethodResultType(mt)
        case pt: PolyType => mapMethodResultType(pt.resultType)
        case _ => unknown
      }
    } catch {
      case _: Throwable => unknown
    }
  }

  /** Map a MethodType to its result type (for expressions that are method calls). */
  private def mapMethodResultType(tpe: Type): JavaType = {
    tpe match {
      case mt: MethodType => mapType(mt.resultType)
      case other => mapType(other)
    }
  }

  private def mapConstantType(ct: ConstantType): JavaType = {
    ct.value.tag match {
      case Constants.BooleanTag => JavaType.Primitive.Boolean
      case Constants.ByteTag => JavaType.Primitive.Byte
      case Constants.ShortTag => JavaType.Primitive.Short
      case Constants.CharTag => JavaType.Primitive.Char
      case Constants.IntTag => JavaType.Primitive.Int
      case Constants.LongTag => JavaType.Primitive.Long
      case Constants.FloatTag => JavaType.Primitive.Float
      case Constants.DoubleTag => JavaType.Primitive.Double
      case Constants.StringTag => reflectionTypeMapping.`type`(classOf[String])
      case Constants.NullTag => JavaType.Primitive.Null
      case Constants.UnitTag => JavaType.Primitive.Void
      case _ => mapType(ct.underlying)
    }
  }

  private def mapTypeRef(tr: TypeRef, sig: String): JavaType = {
    val sym = tr.symbol
    if (!sym.exists) return unknown

    val fqn = sym.fullName.toString

    fqn match {
      case "scala.Int" => return JavaType.Primitive.Int
      case "scala.Long" => return JavaType.Primitive.Long
      case "scala.Short" => return JavaType.Primitive.Short
      case "scala.Byte" => return JavaType.Primitive.Byte
      case "scala.Char" => return JavaType.Primitive.Char
      case "scala.Float" => return JavaType.Primitive.Float
      case "scala.Double" => return JavaType.Primitive.Double
      case "scala.Boolean" => return JavaType.Primitive.Boolean
      case "scala.Unit" => return JavaType.Primitive.Void
      case _ =>
    }

    // Strip trailing $ from Scala companion object names (e.g., Collections$ → Collections)
    val baseFqn = if (fqn.endsWith("$")) fqn.dropRight(1) else fqn
    val javaFqn = baseFqn match {
      case "scala.Any" | "scala.AnyRef" => "java.lang.Object"
      case other => other
    }

    mapClassType(sym, javaFqn, sig)
  }

  private def mapTermRef(tmr: TermRef, sig: String): JavaType = {
    val sym = tmr.symbol
    if (!sym.exists) return unknown
    val underlying = tmr.underlying
    if (underlying != null && underlying != NoType) mapType(underlying)
    else unknown
  }

  private def mapAppliedType(at: AppliedType, sig: String): JavaType = {
    val baseType = mapType(at.tycon) match {
      case p: JavaType.Parameterized => p.getType
      case fq: JavaType.FullyQualified => fq
      case _ => return unknown
    }

    typeFactory.computeParameterized(sig, (pt: JavaType.Parameterized) => {
      val typeArgs = new ArrayList[JavaType]()
      at.args.foreach { arg =>
        val mapped = mapType(arg)
        typeArgs.add(if (mapped != null) mapped else unknown)
      }
      pt.unsafeSet(baseType, typeArgs)
    })
  }

  private def mapGenericTypeVariable(tp: TypeParamRef, sig: String): JavaType = {
    val name = tp.paramName.toString
    typeFactory.computeGenericTypeVariable(sig, name, JavaType.GenericTypeVariable.Variance.INVARIANT,
      (gtv: JavaType.GenericTypeVariable) => {
        var bounds: ArrayList[JavaType] = null
        var resolvedVariance = JavaType.GenericTypeVariable.Variance.INVARIANT

        tp.underlying match {
          case tb: TypeBounds =>
            if (!(tb.hi =:= ctx.definitions.AnyType)) {
              bounds = new ArrayList[JavaType]()
              val mapped = mapType(tb.hi)
              if (mapped != null) bounds.add(mapped)
              resolvedVariance = JavaType.GenericTypeVariable.Variance.COVARIANT
            }
          case _ =>
        }

        gtv.unsafeSet(name, resolvedVariance, bounds)
      })
  }

  private def mapTypeBounds(tb: TypeBounds, sig: String): JavaType = {
    typeFactory.computeGenericTypeVariable(sig, "?", JavaType.GenericTypeVariable.Variance.INVARIANT,
      (gtv: JavaType.GenericTypeVariable) => {
        var bounds: ArrayList[JavaType] = null
        var variance = JavaType.GenericTypeVariable.Variance.INVARIANT

        if (!(tb.hi =:= ctx.definitions.AnyType)) {
          bounds = new ArrayList[JavaType]()
          bounds.add(mapType(tb.hi))
          variance = JavaType.GenericTypeVariable.Variance.COVARIANT
        } else if (!(tb.lo =:= ctx.definitions.NothingType)) {
          bounds = new ArrayList[JavaType]()
          bounds.add(mapType(tb.lo))
          variance = JavaType.GenericTypeVariable.Variance.CONTRAVARIANT
        }

        gtv.unsafeSet("?", variance, bounds)
      })
  }

  def mapClassType(sym: Symbol, fqn: String, sig: String): JavaType.FullyQualified = {
    // Synthesize a canonical-but-empty Class for library types to avoid deep
    // recursive type hierarchies that cause StackOverflowError in recipes like
    // ChangeType. Source-defined classes (those with a sourceFile) get full
    // population below.
    val isSourceDefined = try {
      val src = sym.source
      src != null && src.exists && !sym.is(Flags.JavaDefined) && src.name.endsWith(".scala")
    } catch {
      case _: Throwable => false
    }

    if (!isSourceDefined) {
      return typeFactory.computeClass(fqn, Flag.Public.getBitMask, JavaType.FullyQualified.Kind.Class, _ => {
        // Library type — body intentionally not populated.
      })
    }

    val kind = if (sym.is(Flags.Trait)) JavaType.FullyQualified.Kind.Interface
    else if (sym.is(Flags.Enum)) JavaType.FullyQualified.Kind.Enum
    else JavaType.FullyQualified.Kind.Class

    val flagsBits = mapFlags(sym)

    typeFactory.computeClass(fqn, flagsBits, kind, (clazz: JavaType.Class) => {
      // For source-defined classes, populate members and methods (but use ShallowClass for supertypes)
      val supertype: JavaType.FullyQualified = try {
        val parentTypes = sym.info match {
          case ci: ClassInfo => ci.parents
          case _ => Nil
        }
        parentTypes.headOption.flatMap { pt =>
          Option(TypeUtils.asFullyQualified(mapType(pt)))
        }.orNull
      } catch {
        case _: Throwable => null
      }

      val interfaces: util.List[JavaType.FullyQualified] = try {
        val parentTypes = sym.info match {
          case ci: ClassInfo => ci.parents.drop(1)
          case _ => Nil
        }
        if (parentTypes.nonEmpty) {
          val ifaces = new ArrayList[JavaType.FullyQualified]()
          parentTypes.foreach { pt =>
            val mapped = TypeUtils.asFullyQualified(mapType(pt))
            if (mapped != null) ifaces.add(mapped)
          }
          ifaces
        } else null
      } catch {
        case _: Throwable => null
      }

      // Populate methods
      val methods: util.List[JavaType.Method] = try {
        val decls = sym.info match {
          case ci: ClassInfo => ci.decls.toList
          case _ => Nil
        }
        val methodSyms = decls.filter(s => s.is(Flags.Method) && !s.isConstructor && !s.name.toString.startsWith("$"))
        if (methodSyms.nonEmpty) {
          val ms = new ArrayList[JavaType.Method]()
          methodSyms.foreach { m =>
            try { ms.add(mapMethodType(m)) } catch { case _: Throwable => }
          }
          ms
        } else null
      } catch {
        case _: Throwable => null
      }

      // Populate fields
      val members: util.List[JavaType.Variable] = try {
        val decls = sym.info match {
          case ci: ClassInfo => ci.decls.toList
          case _ => Nil
        }
        val fieldSyms = decls.filter(s => s.isTerm && !s.is(Flags.Method) && !s.name.toString.startsWith("$"))
        if (fieldSyms.nonEmpty) {
          val fs = new ArrayList[JavaType.Variable]()
          fieldSyms.foreach { f =>
            try { fs.add(mapVariableType(f)) } catch { case _: Throwable => }
          }
          fs
        } else null
      } catch {
        case _: Throwable => null
      }

      clazz.unsafeSet(null, supertype, null, null, interfaces, members, methods)
    })
  }

  // --- Method and Variable type mapping ---

  def mapMethodType(sym: Symbol): JavaType.Method = {
    if (sym == null || !sym.exists) return null

    val sig = signatureBuilder.methodSignature(sym)
    val name = if (sym.isConstructor) "<constructor>" else sym.name.toString
    val flagsBits = mapFlags(sym)

    val paramNamesArr: Array[String] = sym.info match {
      case mt: MethodType => mt.paramNames.iterator.map(_.toString).toArray
      case pt: PolyType => pt.resultType match {
        case mt: MethodType => mt.paramNames.iterator.map(_.toString).toArray
        case _ => null
      }
      case _ => null
    }

    val finalParamNames = paramNamesArr
    typeFactory.methodFor(sig,
      () => new JavaType.Method(null, flagsBits, null, name, null, finalParamNames, null, null, null, null, null),
      (method: JavaType.Method) => {
        val paramTypes: util.List[JavaType] = sym.info match {
          case mt: MethodType =>
            val pts = new ArrayList[JavaType]()
            mt.paramInfos.foreach(pt => pts.add(mapType(pt)))
            pts
          case pt: PolyType => pt.resultType match {
            case mt: MethodType =>
              val pts = new ArrayList[JavaType]()
              mt.paramInfos.foreach(pt2 => pts.add(mapType(pt2)))
              pts
            case _ => null
          }
          case _ => null
        }

        val declaringType = try {
          TypeUtils.asFullyQualified(mapType(sym.owner.info))
        } catch {
          case _: Throwable => null
        }
        val returnType = mapType(sym.info.finalResultType)

        method.unsafeSet(declaringType, returnType, paramTypes, null, null)
      })
  }

  def mapVariableType(sym: Symbol): JavaType.Variable = {
    if (sym == null || !sym.exists) return null

    val sig = signatureBuilder.variableSignature(sym)
    val flagsBits = mapFlags(sym)
    typeFactory.variableFor(sig, () => {
      val variable = new JavaType.Variable(null, flagsBits, sym.name.toString, null, null, null)
      val ownerType = try { variableOwner(sym) } catch { case _: Throwable => unknown }
      val varType = try { mapType(sym.info) } catch { case _: Throwable => unknown }
      variable.unsafeSet(ownerType, varType, null.asInstanceOf[java.util.List[JavaType.FullyQualified]])
      variable
    })
  }

  /**
   * Parameters and locals are owned by their method, as in Java. Lambdas and the
   * right-hand side of a local `val` introduce owners of their own, which are skipped.
   */
  private def variableOwner(sym: Symbol): JavaType = {
    var owner = sym.owner
    while (owner.exists && (owner.isAnonymousFunction || owner.isLocalDummy ||
      (owner.isTerm && !owner.is(Flags.Method) && !owner.is(Flags.Module)))) {
      owner = owner.owner
    }
    if (owner.is(Flags.Method)) mapMethodType(owner) else mapType(owner.info)
  }

  /** Create a constructor method type for a given class type. */
  def mapConstructorType(fq: JavaType.FullyQualified): JavaType.Method = {
    val sig = fq.getFullyQualifiedName + "{name=<constructor>,return=" + fq.getFullyQualifiedName + ",parameters=[]}"
    typeFactory.methodFor(sig,
      () => new JavaType.Method(null, Flag.Public.getBitMask, null, "<constructor>",
        null, null.asInstanceOf[Array[String]], null, null, null, null, null),
      (method: JavaType.Method) => {
        method.unsafeSet(fq, fq, java.util.Collections.emptyList[JavaType](), null, null)
      })
  }

  // --- Helpers ---

  private def mapFlags(sym: Symbol): Long = {
    var bits = 0L
    if (sym.is(Flags.Private)) bits |= Flag.Private.getBitMask
    else if (sym.is(Flags.Protected)) bits |= Flag.Protected.getBitMask
    else bits |= Flag.Public.getBitMask

    if (sym.is(Flags.Abstract)) bits |= Flag.Abstract.getBitMask
    if (sym.is(Flags.Final) || sym.is(Flags.Module)) bits |= Flag.Final.getBitMask
    if (sym.isStatic) bits |= Flag.Static.getBitMask
    bits
  }
}
