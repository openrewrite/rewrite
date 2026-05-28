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

import dotty.tools.dotc.ast.{tpd, Trees}
import dotty.tools.dotc.core.Constants
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.core.Symbols
import dotty.tools.dotc.core.Symbols.Symbol
import dotty.tools.dotc.core.Types.*
import dotty.tools.dotc.util.Spans

import org.openrewrite.java.internal.JavaTypeFactory
import org.openrewrite.java.tree.{Flag, JavaType, TypeUtils}

import java.util
import java.util.{ArrayList, Collections}
import scala.collection.mutable

/**
 * Maps Dotty compiler types to OpenRewrite's JavaType model.
 *
 * Uses two lookup strategies:
 * 1. Span-based: maps source position → typed tree node for direct type extraction.
 *    Uses start position as primary key (end positions may differ between untpd/tpd).
 * 2. Name-based: maps symbol name → Symbol for fallback when spans don't match.
 *
 * Both maps are built from the typed tree using Dotty's TreeTraverser.
 */
class ScalaTypeMapping(typeFactory: JavaTypeFactory, typedTree: tpd.Tree)(using ctx: Context) {

  private val signatureBuilder = new ScalaTypeSignatureBuilder
  private val unknown: JavaType = JavaType.Unknown.getInstance()

  // Span-based lookup: start position → typed tree nodes (multiple nodes may share a start position)
  // Uses start position only — end positions may differ between untpd and tpd trees
  private val spanMap: mutable.Map[Int, mutable.ListBuffer[tpd.Tree]] = mutable.Map.empty

  // Name-based lookup: symbol name → Symbol (fallback)
  private val symbolsByName: mutable.Map[String, Symbol] = mutable.Map.empty

  // Build both maps via a single traversal
  buildMaps()

  private def buildMaps(): Unit = {
    try {
      val traverser = new tpd.TreeTraverser {
        override def traverse(tree: tpd.Tree)(using Context): Unit = {
          // Index by start position for span lookup
          if (tree.span.exists && !tree.span.isZeroExtent && !tree.span.isSynthetic) {
            spanMap.getOrElseUpdate(tree.span.start, mutable.ListBuffer.empty) += tree
          }

          // Index by name for name-based fallback
          try {
            val sym = tree.symbol
            if (sym != null && sym.exists && sym.name != null) {
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

  // --- Public API ---

  /**
   * Look up JavaType for an untpd tree node by matching its span start position
   * to the corresponding tpd node.
   */
  def typeFor(span: Spans.Span): JavaType = {
    if (!span.exists) return null
    spanMap.get(span.start) match {
      case Some(trees) =>
        // Try each node at this position, preferring one with matching end span
        val exact = trees.find(t => t.span.end == span.end)
        val tree = exact.getOrElse(trees.last)
        try {
          val tpe = tree.tpe
          if (tpe == null || tpe == NoType) null
          else mapType(tpe)
        } catch {
          case _: Throwable => null
        }
      case None => null
    }
  }

  /** Look up JavaType by identifier name (fallback for when spans don't match). */
  def typeForName(name: String): JavaType = {
    if (name == null || name.isEmpty) return null
    symbolsByName.get(name) match {
      case Some(sym) =>
        try {
          val tpe = sym.info
          if (tpe == null || tpe == NoType) null
          else mapType(tpe)
        } catch {
          case _: Throwable => null
        }
      case None => null
    }
  }

  /** Look up JavaType.Method by span start position. */
  def methodTypeFor(span: Spans.Span): JavaType.Method = {
    if (!span.exists) return null
    spanMap.get(span.start) match {
      case Some(trees) =>
        // Search all nodes at this position for a method
        // Use index-based loop to avoid NonLocalReturnControl from return-in-foreach
        var i = 0
        while (i < trees.size) {
          val tree = trees(i)
          try {
            val sym = tree.symbol
            if (sym != null && sym.exists && (sym.is(Flags.Method) || sym.isConstructor)) {
              return mapMethodType(sym)
            }
            // For Apply nodes, the method symbol is on the fun subtree
            tree match {
              case Trees.Apply(fun, _) =>
                val funSym = fun.asInstanceOf[tpd.Tree].symbol
                if (funSym != null && funSym.exists) return mapMethodType(funSym)
              case Trees.Select(_, _) =>
                if (sym != null && sym.exists && sym.is(Flags.Method)) return mapMethodType(sym)
              case _ =>
            }
          } catch {
            case _: scala.runtime.NonLocalReturnControl[?] => throw new RuntimeException("unexpected")
            case _: Throwable =>
          }
          i += 1
        }
        null
      case None => null
    }
  }

  /** Look up JavaType.Method by name (fallback). */
  def methodTypeForName(name: String): JavaType.Method = {
    if (name == null || name.isEmpty) return null
    symbolsByName.get(name) match {
      case Some(sym) if sym.is(Flags.Method) || sym.isConstructor =>
        try { mapMethodType(sym) } catch { case _: Throwable => null }
      case _ => null
    }
  }

  /** Look up JavaType.Variable by span start position. */
  def variableTypeFor(span: Spans.Span): JavaType.Variable = {
    if (!span.exists) return null
    spanMap.get(span.start) match {
      case Some(trees) =>
        var i = 0
        while (i < trees.size) {
          try {
            val sym = trees(i).symbol
            if (sym != null && sym.exists && !sym.is(Flags.Method) && !sym.isClass) {
              return mapVariableType(sym)
            }
          } catch {
            case _: Throwable =>
          }
          i += 1
        }
        null
      case None => null
    }
  }

  /** Look up JavaType.Variable by name (fallback). */
  def variableTypeForName(name: String): JavaType.Variable = {
    if (name == null || name.isEmpty) return null
    symbolsByName.get(name) match {
      case Some(sym) if !sym.is(Flags.Method) && !sym.isClass =>
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
    val existing: JavaType = typeFactory.get(sig)
    if (existing != null) return existing

    try {
      tpe.dealias match {
        case ct: ConstantType => mapConstantType(ct)
        case tr: TypeRef => mapTypeRef(tr, sig)
        case tmr: TermRef => mapTermRef(tmr, sig)
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
          val normalizedSig = normalizedFqn
          val existingNorm: JavaType = typeFactory.get(normalizedSig)
          if (existingNorm != null && existingNorm.isInstanceOf[JavaType.FullyQualified])
            existingNorm.asInstanceOf[JavaType.FullyQualified]
          else
            mapClassType(ci.cls, normalizedFqn, normalizedSig)
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
      case Constants.StringTag => JavaType.ShallowClass.build("java.lang.String")
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
      case fq: JavaType.FullyQualified => fq
      case p: JavaType.Parameterized => p.getType
      case _ => return unknown
    }

    typeFactory.computeParameterized(sig, at, (pt: JavaType.Parameterized) => {
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
    typeFactory.computeGenericTypeVariable(sig, name, JavaType.GenericTypeVariable.Variance.INVARIANT, tp,
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
    typeFactory.computeGenericTypeVariable(sig, "?", JavaType.GenericTypeVariable.Variance.INVARIANT, tb,
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
    val existing: JavaType = typeFactory.get(sig)
    if (existing != null && existing.isInstanceOf[JavaType.FullyQualified])
      return existing.asInstanceOf[JavaType.FullyQualified]

    // Use ShallowClass for library types to avoid deep recursive type hierarchies
    // that cause StackOverflowError in recipes like ChangeType.
    // Source-defined classes (those with a sourceFile) get full population.
    val isSourceDefined = try {
      val src = sym.source
      src != null && src.exists && !sym.is(Flags.JavaDefined) && src.name.endsWith(".scala")
    } catch {
      case _: Throwable => false
    }

    if (!isSourceDefined) {
      val shallow = JavaType.ShallowClass.build(fqn)
      typeFactory.put(sig, shallow)
      return shallow
    }

    val kind = if (sym.is(Flags.Trait)) JavaType.FullyQualified.Kind.Interface
    else if (sym.is(Flags.Enum)) JavaType.FullyQualified.Kind.Enum
    else JavaType.FullyQualified.Kind.Class

    val flagsBits = mapFlags(sym)

    typeFactory.computeClass(sig, fqn, flagsBits, kind, sym, (clazz: JavaType.Class) => {
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
        val fieldSyms = decls.filter(s => !s.is(Flags.Method) && !s.isClass && !s.name.toString.startsWith("$"))
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

    typeFactory.computeMethod(sig, flagsBits, name, paramNamesArr, null, null, sym, (method: JavaType.Method) => {
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
    typeFactory.computeVariable(sig, flagsBits, sym.name.toString, sym, (variable: JavaType.Variable) => {
      val ownerType = try { mapType(sym.owner.info) } catch { case _: Throwable => unknown }
      val varType = try { mapType(sym.info) } catch { case _: Throwable => unknown }
      variable.unsafeSet(ownerType, varType, null.asInstanceOf[java.util.List[JavaType.FullyQualified]])
    })
  }

  /** Create a constructor method type for a given class type. */
  def mapConstructorType(fq: JavaType.FullyQualified): JavaType.Method = {
    val sig = fq.getFullyQualifiedName + "{name=<constructor>,return=" + fq.getFullyQualifiedName + ",parameters=[]}"
    typeFactory.computeMethod(sig, Flag.Public.getBitMask, "<constructor>", null, null, null, fq,
      (method: JavaType.Method) =>
        method.unsafeSet(fq, fq, java.util.Collections.emptyList[JavaType](), null, null))
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
