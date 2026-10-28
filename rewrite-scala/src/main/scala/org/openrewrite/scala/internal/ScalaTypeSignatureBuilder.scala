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

import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.core.Names
import dotty.tools.dotc.core.Symbols
import dotty.tools.dotc.core.Types.*

import java.util.HashSet
import java.util.StringJoiner

/**
 * Generates unique string signatures for Dotty types, used as cache keys
 * in JavaTypeCache. Follows the same contract as GroovyAstTypeSignatureBuilder.
 */
class ScalaTypeSignatureBuilder(using ctx: Context) {

  private var typeVariableNameStack: HashSet[String] = null

  private var depth = 0

  def signature(tpe: Type): String = {
    if (tpe == null) return "{undefined}"

    depth += 1
    try {
      tpe.dealias match {
        case ct: ConstantType =>
          primitiveSignature(ct)
        case tr: TypeRef =>
          typeRefSignature(tr)
        case tmr: TermRef =>
          termRefSignature(tmr)
        case at: AppliedType if at.isRepeatedParam =>
          signature(at.translateFromRepeated(toArray = false))
        case at: AppliedType =>
          parameterizedSignature(at)
        case tp: TypeParamRef =>
          genericSignature(tp)
        case tb: TypeBounds =>
          boundsSignature(tb)
        case mt: MethodType =>
          "{method}"
        case pt: PolyType =>
          "{method}"
        case at: AndType =>
          signature(at.tp1) + " & " + signature(at.tp2)
        case ot: OrType =>
          unionMembers(ot).map(_._1).mkString(" | ")
        case NoType =>
          "{undefined}"
        case _ =>
          tpe.show
      }
    } catch {
      // A type too deep to describe is undefined as a whole. Answering at the level that ran out
      // of stack would let the caller go on to its next argument and descend just as deep again.
      case e: VirtualMachineError if depth > 1 => throw e
      case _: Throwable => "{undefined}"
    } finally {
      depth -= 1
    }
  }

  private def primitiveSignature(ct: ConstantType): String = {
    ct.value.tag match {
      case dotty.tools.dotc.core.Constants.BooleanTag => "boolean"
      case dotty.tools.dotc.core.Constants.ByteTag => "byte"
      case dotty.tools.dotc.core.Constants.ShortTag => "short"
      case dotty.tools.dotc.core.Constants.CharTag => "char"
      case dotty.tools.dotc.core.Constants.IntTag => "int"
      case dotty.tools.dotc.core.Constants.LongTag => "long"
      case dotty.tools.dotc.core.Constants.FloatTag => "float"
      case dotty.tools.dotc.core.Constants.DoubleTag => "double"
      case dotty.tools.dotc.core.Constants.StringTag => "java.lang.String"
      case dotty.tools.dotc.core.Constants.NullTag => "null"
      case dotty.tools.dotc.core.Constants.UnitTag => "void"
      case _ => ct.show
    }
  }

  private def typeRefSignature(tr: TypeRef): String = {
    val sym = tr.symbol
    if (!sym.exists) return tr.show
    if (sym.isTypeParam) return typeVariableSignature(sym.name.toString, tr.underlying)

    mapScalaFqnToJava(binaryName(sym))
  }

  private def termRefSignature(tmr: TermRef): String = {
    val sym = tmr.symbol
    if (!sym.exists) return tmr.show
    sym.fullName.toString
  }

  def parameterizedSignature(at: AppliedType): String = {
    if (at.tycon.typeSymbol == ctx.definitions.ArrayClass) return signature(at.args.head) + "[]"
    val base = signature(at.tycon)
    val tj = new StringJoiner(", ", "<", ">")
    at.args.foreach { arg =>
      tj.add(signature(arg))
    }
    base + tj.toString
  }

  private def genericSignature(tp: TypeParamRef): String =
    typeVariableSignature(tp.paramName.toString, tp.underlying)

  private def typeVariableSignature(name: String, bounds: Type): String = {
    if (typeVariableNameStack == null)
      typeVariableNameStack = new HashSet[String]()

    // Already being described further out: only the frame that added the name takes it off,
    // or a second mention of it in the same bound would describe the bound all over again.
    if (!typeVariableNameStack.add(name)) {
      return "Generic{" + name + "}"
    }

    try {
      val sb = new StringBuilder("Generic{" + name)

      bounds match {
        case tb: TypeBounds if !(tb.hi =:= ctx.definitions.AnyType) =>
          sb.append(" extends ")
          sb.append(signature(tb.hi))
        case _ =>
      }

      sb.append("}").toString()
    } finally {
      typeVariableNameStack.remove(name)
    }
  }

  private def boundsSignature(tb: TypeBounds): String = {
    if (!(tb.hi =:= ctx.definitions.AnyType)) {
      "? extends " + signature(tb.hi)
    } else if (!(tb.lo =:= ctx.definitions.NothingType)) {
      "? super " + signature(tb.lo)
    } else {
      "?"
    }
  }

  def methodSignature(sym: Symbols.Symbol): String = methodSignature(sym, sym.info)

  /** The signature of the method with the given parameter and result types, as instantiated at a call. */
  def methodSignature(sym: Symbols.Symbol, info: Type): String = {
    val owner = methodOwnerFqn(sym.owner)
    val name = if (sym.isConstructor) "<constructor>" else sym.name.toString
    val returnType = info.finalResultType match {
      case NoType => "void"
      case rt => signature(rt)
    }

    val sb = new StringBuilder(owner)
    sb.append("{name=").append(name)
    sb.append(",return=").append(returnType)

    val paramTypes = new StringJoiner(",", "[", "]")
    info.stripPoly match {
      case mt: MethodType =>
        mt.paramInfos.foreach(pt => paramTypes.add(signature(parameterType(sym, pt))))
      case _ =>
    }
    sb.append(",parameters=").append(paramTypes)
    sb.append("}")
    sb.toString()
  }

  /** The type of a parameter of the method as the JVM sees it: a repeated parameter is an array of a Java method, a `Seq` otherwise. */
  def parameterType(method: Symbols.Symbol, pt: Type): Type =
    if (!pt.isRepeatedParam) pt
    else if (method.is(Flags.JavaDefined)) pt.translateParameterized(ctx.definitions.RepeatedParamClass, ctx.definitions.ArrayClass)
    else pt.translateFromRepeated(toArray = false)

  // Sorted so that `A | B` and `B | A` share one cache key.
  def unionMembers(ot: OrType): List[(String, Type)] =
    unionAlternatives(ot).map(t => signature(t) -> t).distinctBy(_._1).sortBy(_._1)

  private def unionAlternatives(tpe: Type): List[Type] = tpe.dealias match {
    case ot: OrType => unionAlternatives(ot.tp1) ++ unionAlternatives(ot.tp2)
    case nothing if nothing.isExactlyNothing => Nil
    case other => List(other)
  }

  /**
   * The FQN of a class by its JVM binary name (`Outer$Inner`, `object A` as `A$`), with `scala.Any`/`scala.AnyRef`
   * as `java.lang.Object`. Java statics live on a module class `X$` and are keyed under `X` like the Java parser does.
   */
  def classFqn(sym: Symbols.Symbol): String = methodOwnerFqn(sym) match {
    case "scala.Any" | "scala.AnyRef" => "java.lang.Object"
    case other => other
  }

  /** The owner of a method in its signature: its class by binary name, with Java statics under the Java class. */
  def methodOwnerFqn(owner: Symbols.Symbol): String = {
    val fqn = binaryName(owner)
    if (owner.is(Flags.JavaDefined) && owner.is(Flags.ModuleClass)) fqn.stripSuffix("$") else fqn
  }

  def variableSignature(sym: Symbols.Symbol): String = {
    val name = "{name=" + sym.name.toString
    if (sym.owner.isClass) return binaryName(sym.owner) + name + "}"
    val owner = if (sym.owner.is(Flags.Method)) methodSignature(sym.owner) else sym.owner.fullName.toString
    owner + name + (if (sym.span.exists) ",pos=" + sym.span.start else "") + "}"
  }

  /** `flatName` leaves out the package, and joins a nested class to a module class owner without a doubled `$`. */
  private def binaryName(sym: Symbols.Symbol): String = {
    val pkg = sym.enclosingPackageClass
    if (pkg.isEffectiveRoot) sym.flatName.toString
    else pkg.fullName.toString + "." + sym.flatName
  }

  def variableSignature(name: String): String = {
    "{undefined}{name=" + name + "}"
  }

  /**
   * Map Scala standard library FQNs to their Java equivalents where appropriate.
   */
  private def mapScalaFqnToJava(fqn: String): String = fqn match {
    case "scala.Int" => "int"
    case "scala.Long" => "long"
    case "scala.Short" => "short"
    case "scala.Byte" => "byte"
    case "scala.Char" => "char"
    case "scala.Float" => "float"
    case "scala.Double" => "double"
    case "scala.Boolean" => "boolean"
    case "scala.Unit" => "void"
    case "scala.Any" => "java.lang.Object"
    case "scala.AnyRef" => "java.lang.Object"
    case "scala.Matchable" => "java.lang.Object"
    case "scala.Nothing" => "scala.Nothing"
    case "scala.Null" => "null"
    case other => other
  }
}
