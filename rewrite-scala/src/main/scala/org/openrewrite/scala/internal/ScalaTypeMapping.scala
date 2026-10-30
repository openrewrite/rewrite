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
import dotty.tools.dotc.core.NameKinds.DefaultGetterName
import dotty.tools.dotc.core.Names.Name
import dotty.tools.dotc.core.StdNames.nme
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.core.Symbols
import dotty.tools.dotc.core.Symbols.Symbol
import dotty.tools.dotc.core.TypeOps
import dotty.tools.dotc.core.Types.*
import dotty.tools.dotc.util.Spans

import org.openrewrite.java.internal.JavaTypeFactory
import org.openrewrite.java.tree.{Flag, JavaType, TypeUtils}
import org.openrewrite.scala.tree.Union

import java.util
import java.util.{ArrayList, Collections, IdentityHashMap}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

/**
 * What a selector of an import or export imports.
 *
 * @param symbolType the imported definition: the class for a type or an object, the variable
 *                   for a value, the method for a method, unknown when it can't be resolved
 * @param nameType   the type of the imported name, as for a reference to it
 * @param variable   the imported object or value, if any
 * @param isImplicit whether the import brings an implicit or given definition into scope
 */
final case class Imported(symbolType: JavaType, nameType: JavaType, variable: JavaType.Variable, isImplicit: Boolean)

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

  // Span-based lookup: start position → typed tree nodes in pre-order (multiple nodes may share a start position)
  private val spanMap: mutable.Map[Int, mutable.ListBuffer[tpd.Tree]] = mutable.Map.empty

  // Name-based lookup: symbol name → Symbol (fallback)
  private val symbolsByName: mutable.Map[String, Symbol] = mutable.Map.empty

  // Implicit conversions the typer inserted, by the tree they convert
  private val conversions = new IdentityHashMap[tpd.Tree, tpd.Apply]()

  // Implicit arguments the typer inserted, by the innermost node of the untyped tree whose typing they are part of
  private val implicitArguments = new IdentityHashMap[untpd.Tree, ArrayList[tpd.Tree]]()

  // Build both maps via a single traversal
  buildMaps()

  private def buildMaps(): Unit = {
    try {
      val traverser = new tpd.TreeTraverser {
        private var owner: untpd.Tree = null

        override def traverse(tree: tpd.Tree)(using Context): Unit = {
          val enclosingOwner = owner
          typedNodes.ownerOf(tree).foreach(owner = _)

          // Index by start position for span lookup
          if (tree.span.exists && !tree.span.isZeroExtent && !tree.isInstanceOf[Trees.InferredTypeTree[?]]) {
            spanMap.getOrElseUpdate(tree.span.start, mutable.ListBuffer.empty) += tree
          }

          tree match {
            case app @ Trees.Apply(fn, List(arg)) if isInsertedConversion(app, fn, arg) => conversions.put(arg, app)
            case _ =>
          }
          tree match {
            case Trees.Apply(fn, args) if owner != null && isInsertedImplicitApplication(fn, args) =>
              implicitArguments.computeIfAbsent(owner, _ => new ArrayList()).addAll(args.asJava)
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
          owner = enclosingOwner
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

  /**
   * The typer passes implicit arguments by applying the tree that takes them to arguments it
   * found by implicit search, which it positions at the end of that tree, not spanning any source.
   */
  private def isInsertedImplicitApplication(fn: tpd.Tree, args: List[tpd.Tree]): Boolean =
    try {
      args.nonEmpty && args.forall(arg => !arg.span.exists || arg.span.isZeroExtent) && (fn.tpe.widen match {
        case mt: MethodType => mt.isImplicitMethod
        case _ => false
      })
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
      else {
        val call = resolved(node)
        calledSymbol(call).filterNot(isErrorSymbol).map(sym => try invokedMethodType(sym, call) catch { case _: Throwable => null })
      }
    }

  /**
   * The method a call or reference invokes, with its parameter and result types instantiated
   * for the type arguments and the prefix of the call, as `immutable.Set[Int]` for `Set(1, 2)`.
   */
  private def invokedMethodType(sym: Symbol, call: tpd.Tree): JavaType.Method =
    instantiatedInfo(sym, call) match {
      case Some(info) => mapMethodType(sym, info)
      case None => mapMethodType(sym)
    }

  private def instantiatedInfo(sym: Symbol, tree: tpd.Tree): Option[Type] = tree match {
    case Trees.Apply(fn, _) => instantiatedInfo(sym, fn)
    case Trees.Block(_, expr) => instantiatedInfo(sym, expr)
    case Trees.Typed(expr, _) => instantiatedInfo(sym, expr)
    case _: Trees.TypeApply[?] | _: Trees.Select[?] | _: Trees.Ident[?] if tree.symbol == sym =>
      tree.tpe.widenSingleton match {
        case info: (MethodType | ExprType) if !info.isError => Some(info)
        case value: ValueType if !value.isError && isParameterless(sym) => Some(ExprType(value))
        case _ => None
      }
    case _ => None
  }

  private def isParameterless(sym: Symbol): Boolean =
    sym.is(Flags.Method) && !takesParameters(sym.info)

  private def takesParameters(info: Type): Boolean = info.stripPoly.isInstanceOf[MethodType]

  /**
   * Whether the node refers to a method taking parameters, so that applying it calls the method
   * rather than the `apply` of its result. This holds even when the application doesn't type-check.
   */
  def refersToMethodWithParameters(tree: untpd.Tree): Boolean =
    typedNodes.get(tree).exists { node =>
      try node.own.tpe match {
        case ref: TermRef =>
          val alternatives = ref.denot.alternatives
          alternatives.nonEmpty && alternatives.forall(d => d.symbol.is(Flags.Method) && takesParameters(d.info))
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

  /** The definitions the typer passed as implicit arguments while typing the node, if any. */
  def implicitArgumentsOf(tree: untpd.Tree): Option[util.List[JavaType]] =
    Option(implicitArguments.get(tree)).map { args =>
      val definitions = new ArrayList[JavaType]()
      args.forEach(arg => implicitDefinition(arg).foreach(definitions.add))
      definitions
    }.filterNot(_.isEmpty)

  private def implicitDefinition(arg: tpd.Tree): Option[JavaType] = arg match {
    case Trees.Typed(expr, _) => implicitDefinition(expr)
    case Trees.TypeApply(fn, _) => implicitDefinition(fn)
    case Trees.Apply(fn, _) => implicitDefinition(fn)
    case Trees.Block(_, expr) => implicitDefinition(expr)
    case Trees.Inlined(call, _, expansion) => implicitDefinition(if (call.isEmpty) expansion else call)
    case _ =>
      try {
        val sym = arg.symbol
        if (sym == null || !sym.exists || isErrorSymbol(sym) || sym.name.is(DefaultGetterName)) None
        else if (isVariable(sym)) Option(mapVariableType(sym))
        else if (sym.is(Flags.Method)) Option(mapMethodType(sym))
        else None
      } catch {
        case _: Throwable => None
      }
  }

  /**
   * What the selector of an import or export imports. A name that is both a type and an
   * object, such as a class and its companion, is attributed as the type, with the object
   * as its variable.
   */
  def importedOf(tree: untpd.Tree, name: Name): Option[Imported] =
    typedNodes.get(tree).map(_.own).collect {
      case imp: Trees.ImportOrExport[?] => imported(imp.expr.asInstanceOf[tpd.Tree], name)
    }

  private def imported(qualifier: tpd.Tree, name: Name): Imported =
    try {
      val prefix = qualifier.tpe
      if (prefix == null || prefix.isError) return Imported(unknown, unknown, null, isImplicit = false)
      val typeSym = prefix.member(name.toTypeName).symbol
      val terms = prefix.member(name.toTermName).alternatives.map(_.symbol).filter(sym => sym.exists && !isErrorSymbol(sym))
      val isImplicit = terms.exists(_.isOneOf(Flags.GivenOrImplicit))
      val value = terms.find(isVariable)
      val variable = value.map(mapVariableType).orNull
      if (typeSym.exists && !isErrorSymbol(typeSym)) {
        val tpe = mapType(typeSym.typeRef)
        Imported(tpe, tpe, variable, isImplicit)
      } else value match {
        case Some(module) if module.is(Flags.Module) =>
          val tpe = mapType(module.termRef)
          Imported(tpe, tpe, variable, isImplicit)
        case Some(_) =>
          Imported(variable, variable.getType, variable, isImplicit)
        case None => terms.find(_.is(Flags.Method)) match {
          case Some(method) =>
            val methodType = mapMethodType(method)
            Imported(methodType, methodType, null, isImplicit)
          case None if terms.exists(_.is(Flags.Package)) => Imported(null, null, null, isImplicit = false)
          case None => Imported(unknown, unknown, null, isImplicit = false)
        }
      }
    } catch {
      case _: Throwable => Imported(null, null, null, isImplicit = false)
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
      case _: untpd.TypeApply => Nil
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
    else isErrorType(node.own) || typedArguments.exists(a => isErrorType(a.adapted)) ||
      hasNonConformingTypeArguments(node.own)
  }

  /**
   * Whether a type argument of the call is of the wrong kind or out of the bounds of its type
   * parameter. The typer leaves these checks to a later phase, so they are repeated here.
   */
  private def hasNonConformingTypeArguments(call: tpd.Tree): Boolean =
    typeApplication(application(call)).exists { case Trees.TypeApply(fn, args) =>
      try fn.tpe.widen match {
        case poly: PolyType if args.size == poly.paramInfos.size && !args.exists(isErrorType) =>
          args.lazyZip(poly.paramInfos).exists((arg, bounds) => !bounds.isLambdaSub && !arg.tpe.hasSimpleKind) ||
            TypeOps.boundsViolations(args, poly.paramInfos, _.substParams(poly, _), NoType).nonEmpty
        case _ => false
      } catch {
        case _: Throwable => false
      }
    }

  private def typeApplication(tree: tpd.Tree): Option[tpd.TypeApply] = tree match {
    case ta: Trees.TypeApply[?] => Some(ta.asInstanceOf[tpd.TypeApply])
    case Trees.Apply(fn, _) => typeApplication(fn)
    case _ => None
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
    candidates.iterator.flatMap(call => calledSymbol(call).map(_ -> call)).nextOption() match {
      case Some((sym, call)) if !isErrorSymbol(sym) =>
        try invokedMethodType(sym, call) catch { case _: Throwable => null }
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

  /**
   * Look up JavaType.Variable by name (fallback), preferring the variable of that name defined where the tree starts:
   * a pattern binder `v` in `case v: Int` has no typed node of its own span, only the `Bind` that also covers `: Int`.
   */
  def variableTypeForName(name: String, span: Spans.Span): JavaType.Variable = {
    if (name == null || name.isEmpty) return null
    val definedHere = if (!span.exists) None else spanMap.get(span.start).flatMap(_.iterator
      .map(t => try t.symbol catch { case _: Throwable => null })
      .find(sym => sym != null && sym.exists && isVariable(sym) && sym.name.toString == name))
    definedHere.orElse(symbolsByName.get(name)) match {
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
        case at: AppliedType if at.isRepeatedParam => mapType(at.translateFromRepeated(toArray = false))
        case at: AppliedType => mapAppliedType(at, sig)
        case tp: TypeParamRef => mapGenericTypeVariable(tp, sig)
        case tb: TypeBounds => mapTypeBounds(tb, sig)
        case at: AndType => mapType(at.tp1)
        case ot: OrType => mapUnionType(ot, sig)
        case ci: ClassInfo =>
          // Use the class symbol's TypeRef signature to be consistent with TypeRef-based lookups.
          // This prevents creating duplicate JavaType.Class instances for the same type.
          val fqn = signatureBuilder.classFqn(ci.cls)
          mapClassType(ci.cls, fqn, fqn)
        case mt: MethodType => mapMethodResultType(mt)
        case pt: PolyType => mapMethodResultType(pt.resultType)
        case _ => unknown
      }
    } catch {
      case _: Throwable => unknown
    }
  }

  private def mapUnionType(ot: OrType, sig: String): JavaType = signatureBuilder.unionMembers(ot) match {
    case Nil => mapType(ctx.definitions.NothingType)
    case (_, only) :: Nil => mapType(only)
    case members => typeFactory.computeMultiCatch(sig, (union: Union) =>
      union.unsafeSet(members.map(m => Option(mapType(m._2)).getOrElse(unknown)).asJava))
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
      case Constants.NullTag => JavaType.Primitive.Null
      case Constants.UnitTag => JavaType.Primitive.Void
      case _ => mapType(ct.underlying)
    }
  }

  private def mapTypeRef(tr: TypeRef, sig: String): JavaType = {
    val sym = tr.symbol
    if (!sym.exists) return unknown
    if (sym.isTypeParam) return mapTypeVariable(sym.name.toString, tr.underlying, sig)

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
      case "scala.Null" => return JavaType.Primitive.Null
      case "scala.Matchable" => return mapType(ctx.definitions.ObjectType)
      case _ =>
    }

    mapClassType(sym, signatureBuilder.classFqn(sym), sig)
  }

  private def mapTermRef(tmr: TermRef, sig: String): JavaType = {
    val sym = tmr.symbol
    if (!sym.exists) return unknown
    val underlying = tmr.underlying
    if (underlying != null && underlying != NoType) mapType(underlying)
    else unknown
  }

  private def mapAppliedType(at: AppliedType, sig: String): JavaType = {
    if (at.tycon.typeSymbol == ctx.definitions.ArrayClass) return mapArrayType(at, sig)

    val baseType = mapType(at.tycon) match {
      case p: JavaType.Parameterized => p.getType
      case fq: JavaType.FullyQualified => fq
      case typeConstructor: JavaType.GenericTypeVariable => return typeConstructor
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

  private def mapArrayType(at: AppliedType, sig: String): JavaType =
    typeFactory.arrayFor(sig, () => {
      val elemType = mapType(at.args.head)
      new JavaType.Array(null, if (elemType != null) elemType else unknown, null)
    })

  private def mapGenericTypeVariable(tp: TypeParamRef, sig: String): JavaType =
    mapTypeVariable(tp.paramName.toString, tp.underlying, sig)

  private def mapTypeVariable(name: String, underlying: Type, sig: String): JavaType = {
    typeFactory.computeGenericTypeVariable(sig, name, JavaType.GenericTypeVariable.Variance.INVARIANT,
      (gtv: JavaType.GenericTypeVariable) => {
        var bounds: ArrayList[JavaType] = null
        var resolvedVariance = JavaType.GenericTypeVariable.Variance.INVARIANT

        underlying match {
          case tb: TypeBounds =>
            val hi = upperBoundOfKind(tb.hi)
            if (!(hi =:= ctx.definitions.AnyType)) {
              bounds = new ArrayList[JavaType]()
              val mapped = mapType(hi)
              if (mapped != null) bounds.add(mapped)
              resolvedVariance = JavaType.GenericTypeVariable.Variance.COVARIANT
            }
          case _ =>
        }

        gtv.unsafeSet(name, resolvedVariance, bounds)
      })
  }

  /** The upper bound of a type constructor `CC[X] <: Iterable[X]` is that of its applications, `Iterable[X]`. */
  private def upperBoundOfKind(hi: Type): Type = hi match {
    case lambda: HKTypeLambda => lambda.resType
    case other => other
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

  def mapClassType(symbol: Symbol, fqn: String, sig: String): JavaType.FullyQualified = {
    val sym = javaClassOfStatics(symbol)
    // Members of library types are not populated to avoid deep recursive type
    // graphs that cause StackOverflowError in recipes like ChangeType. A class
    // loaded from TASTy has a source too, so also check what it was loaded from.
    val isSourceDefined = try {
      val src = sym.source
      val file = sym.associatedFile
      src != null && src.exists && !sym.is(Flags.JavaDefined) && src.name.endsWith(".scala") &&
        file != null && !file.ext.isScalaBinary
    } catch {
      case _: Throwable => false
    }

    val kind = if (sym.is(Flags.JavaAnnotation)) JavaType.FullyQualified.Kind.Annotation
    else if (sym.is(Flags.Trait)) JavaType.FullyQualified.Kind.Interface
    else if (sym.is(Flags.Enum)) JavaType.FullyQualified.Kind.Enum
    else JavaType.FullyQualified.Kind.Class

    val flagsBits = mapFlags(sym)

    typeFactory.computeClass(fqn, flagsBits, kind, (clazz: JavaType.Class) => {
      val parentTypes = if (fqn == "java.lang.Object") Nil else try {
        sym.info match {
          case ci: ClassInfo => ci.parents
          case _ => Nil
        }
      } catch {
        case _: Throwable => Nil
      }

      val supertype: JavaType.FullyQualified = try {
        parentTypes.headOption.flatMap(pt => Option(TypeUtils.asFullyQualified(mapType(pt)))).orNull
      } catch {
        case _: Throwable => null
      }

      val interfaces: util.List[JavaType.FullyQualified] = try {
        val interfaceTypes = parentTypes.drop(1)
        if (interfaceTypes.nonEmpty) {
          val ifaces = new ArrayList[JavaType.FullyQualified]()
          interfaceTypes.foreach { pt =>
            val mapped = TypeUtils.asFullyQualified(mapType(pt))
            if (mapped != null) ifaces.add(mapped)
          }
          ifaces
        } else null
      } catch {
        case _: Throwable => null
      }

      val methods: util.List[JavaType.Method] = if (!isSourceDefined) null else try {
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

      val members: util.List[JavaType.Variable] = if (!isSourceDefined) null else try {
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

  /** Java statics live on a module class `X$` that shares its key with the Java class `X`, so `X` describes it. */
  private def javaClassOfStatics(sym: Symbol): Symbol =
    if (sym.is(Flags.JavaDefined) && sym.is(Flags.ModuleClass) && sym.companionClass.exists) sym.companionClass
    else sym

  // --- Method and Variable type mapping ---

  def mapMethodType(sym: Symbol): JavaType.Method =
    if (sym == null || !sym.exists) null else mapMethodType(sym, sym.info)

  /** Map the method with the given parameter and result types, as instantiated at a call. */
  private def mapMethodType(sym: Symbol, info: Type): JavaType.Method = {
    val sig = signatureBuilder.methodSignature(sym, info)
    val name = if (sym.isConstructor) "<constructor>" else sym.name.toString
    val flagsBits = mapFlags(sym)

    val paramNamesArr: Array[String] = info.stripPoly match {
      case mt: MethodType => mt.paramNames.iterator.map(_.toString).toArray
      case _ => null
    }

    val finalParamNames = paramNamesArr
    typeFactory.methodFor(sig,
      () => new JavaType.Method(null, flagsBits, null, name, null, finalParamNames, null, null, null, null, null),
      (method: JavaType.Method) => {
        val paramTypes: util.List[JavaType] = info.stripPoly match {
          case mt: MethodType =>
            val pts = new ArrayList[JavaType]()
            mt.paramInfos.foreach(pt => pts.add(mapType(signatureBuilder.parameterType(sym, pt))))
            pts
          case _ => null
        }

        val declaringType = try {
          TypeUtils.asFullyQualified(mapType(sym.owner.info))
        } catch {
          case _: Throwable => null
        }
        val returnType = mapType(info.finalResultType)

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
      val varType = try { mapType(signatureBuilder.parameterType(sym.owner, sym.info)) } catch { case _: Throwable => unknown }
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
    if (sym.isTerm && isVarargs(sym.info)) bits |= Flag.Varargs.getBitMask
    bits
  }

  /**
   * Whether this is the type of a method whose last parameter is repeated, or of a repeated parameter.
   * A method's type gives a repeated parameter as `<repeated>[T]`; the parameter itself is a `Seq[T] @Repeated`.
   */
  private def isVarargs(info: Type): Boolean = info.stripPoly match {
    case mt: MethodType => mt.paramInfos.lastOption.exists(_.isRepeatedParam)
    case tpe => tpe.isRepeatedParam || tpe.hasAnnotation(ctx.definitions.RepeatedAnnot)
  }
}
