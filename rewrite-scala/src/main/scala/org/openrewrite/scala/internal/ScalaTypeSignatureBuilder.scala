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

  def signature(tpe: Type): String = {
    if (tpe == null) return "{undefined}"

    try {
      tpe.dealias match {
        case ct: ConstantType =>
          primitiveSignature(ct)
        case tr: TypeRef =>
          typeRefSignature(tr)
        case tmr: TermRef =>
          termRefSignature(tmr)
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
          signature(ot.tp1) + " | " + signature(ot.tp2)
        case NoType =>
          "{undefined}"
        case _ =>
          tpe.show
      }
    } catch {
      case _: Throwable => "{undefined}"
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

    val fqn = sym.fullName.toString
    mapScalaFqnToJava(fqn)
  }

  private def termRefSignature(tmr: TermRef): String = {
    val sym = tmr.symbol
    if (!sym.exists) return tmr.show
    sym.fullName.toString
  }

  def parameterizedSignature(at: AppliedType): String = {
    val base = signature(at.tycon)
    val tj = new StringJoiner(", ", "<", ">")
    at.args.foreach { arg =>
      tj.add(signature(arg))
    }
    base + tj.toString
  }

  private def genericSignature(tp: TypeParamRef): String = {
    val name = tp.paramName.toString

    if (typeVariableNameStack == null)
      typeVariableNameStack = new HashSet[String]()

    if (!typeVariableNameStack.add(name)) {
      typeVariableNameStack.remove(name)
      return "Generic{" + name + "}"
    }

    val sb = new StringBuilder("Generic{" + name)

    tp.underlying match {
      case tb: TypeBounds if !(tb.hi =:= ctx.definitions.AnyType) =>
        sb.append(" extends ")
        sb.append(signature(tb.hi))
      case _ =>
    }

    typeVariableNameStack.remove(name)
    sb.append("}").toString()
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

  def methodSignature(sym: Symbols.Symbol): String = {
    val owner = sym.owner.fullName.toString
    val name = if (sym.isConstructor) "<constructor>" else sym.name.toString
    val returnType = sym.info.finalResultType match {
      case NoType => "void"
      case rt => signature(rt)
    }

    val sb = new StringBuilder(owner)
    sb.append("{name=").append(name)
    sb.append(",return=").append(returnType)

    val paramTypes = new StringJoiner(",", "[", "]")
    sym.info match {
      case mt: MethodType =>
        mt.paramInfos.foreach(pt => paramTypes.add(signature(pt)))
      case pt: PolyType =>
        pt.resultType match {
          case mt: MethodType =>
            mt.paramInfos.foreach(pt => paramTypes.add(signature(pt)))
          case _ =>
        }
      case _ =>
    }
    sb.append(",parameters=").append(paramTypes)
    sb.append("}")
    sb.toString()
  }

  def variableSignature(sym: Symbols.Symbol): String = {
    val owner = sym.owner.fullName.toString
    owner + "{name=" + sym.name.toString + "}"
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
    case "scala.Nothing" => "scala.Nothing"
    case "scala.Null" => "null"
    case other => other
  }
}
