/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.internal

import dotty.tools.dotc.ast.Trees.*
import dotty.tools.dotc.ast.untpd
import dotty.tools.dotc.core.Constants.*
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Names.*
import dotty.tools.dotc.util.{SourcePosition, Spans}
import org.openrewrite.java.tree.{J, JavaType, JLeftPadded, JRightPadded, Space, Statement, Expression}
import org.openrewrite.marker.Markers
import org.openrewrite.Tree
import java.util.{ArrayList, Collections, List => JList, UUID}
import scala.jdk.CollectionConverters.*

/**
 * Visitor that traverses the Scala compiler AST and builds OpenRewrite LST nodes.
 */
class ScalaTreeVisitor(source: String, offsetAdjustment: Int = 0)(implicit ctx: Context) {
  
  private var cursor = 0
  
  def visitTree(tree: untpd.Tree): J = tree match {
    case _ if tree.isEmpty => visitUnknown(tree)
    case lit: untpd.Literal => visitLiteral(lit)
    case num: untpd.Number => visitNumber(num)
    case id: untpd.Ident => visitIdent(id)
    case app: untpd.Apply => visitApply(app)
    case sel: untpd.Select => visitSelect(sel)
    case _ => 
      // Debug: print unknown tree types
      // println(s"Unknown tree type: ${tree.getClass.getSimpleName}")
      visitUnknown(tree)
  }
  
  private def visitLiteral(lit: untpd.Literal): J.Literal = {
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
    val valueSource = extractSource(num.span)
    
    // Parse the number to determine its type and value
    val (value: Any, javaType: JavaType.Primitive) = valueSource match {
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
  
  private def visitIdent(id: untpd.Ident): J.Identifier = {
    val prefix = extractPrefix(id.span)
    val sourceText = extractSource(id.span) // Extract source to move cursor
    val simpleName = id.name.toString
    
    new J.Identifier(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      Collections.emptyList(),
      simpleName,
      null, // type will be set later
      null  // variable will be set later
    )
  }
  
  private def visitApply(app: untpd.Apply): J = {
    // In Scala, binary operations like "1 + 2" are parsed as Apply(Select(1, +), List(2))
    // Unary operations like "-x" are parsed as Apply(Select(x, unary_-), List())
    app.fun match {
      case sel: untpd.Select if app.args.isEmpty && isUnaryOperator(sel.name.toString) =>
        // This is a unary operation
        visitUnary(sel)
      case sel: untpd.Select if app.args.length == 1 && isBinaryOperator(sel.name.toString) =>
        // This is likely a binary operation (infix notation)
        visitBinary(sel, app.args.head)
      case sel: untpd.Select =>
        // Method call with dot notation like "1.+(2)"
        visitMethodInvocation(app)
      case _ =>
        // Other kinds of applications - for now treat as unknown
        visitUnknown(app)
    }
  }
  
  private def visitUnary(sel: untpd.Select): J.Unary = {
    val expr = visitTree(sel.qualifier).asInstanceOf[Expression]
    val operator = mapUnaryOperator(sel.name.toString)
    
    new J.Unary(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      JLeftPadded.build(operator),
      expr,
      null // type will be set later
    )
  }
  
  private def isUnaryOperator(name: String): Boolean = {
    name match {
      case "unary_-" | "unary_+" | "unary_!" | "unary_~" => true
      case _ => false
    }
  }
  
  private def mapUnaryOperator(op: String): J.Unary.Type = op match {
    case "unary_-" => J.Unary.Type.Negative
    case "unary_+" => J.Unary.Type.Positive
    case "unary_!" => J.Unary.Type.Not
    case "unary_~" => J.Unary.Type.Complement
    case _ => J.Unary.Type.Not // default
  }
  
  private def visitMethodInvocation(app: untpd.Apply): J = {
    // For method invocations, we need to handle the entire expression
    // Check if the span is reasonable
    val adjustedStart = Math.max(0, app.span.start - offsetAdjustment)
    val adjustedEnd = Math.min(source.length, app.span.end - offsetAdjustment)
    
    // For now, just use Unknown to capture the whole expression correctly
    visitUnknown(app)
  }
  
  private def isBinaryOperator(name: String): Boolean = {
    // Check if this is a known binary operator
    Set("+", "-", "*", "/", "%", "==", "!=", "<", ">", "<=", ">=", 
        "&&", "||", "&", "|", "^", "<<", ">>", ">>>", "::", "++").contains(name)
  }
  
  private def visitBinary(sel: untpd.Select, right: untpd.Tree): J.Binary = {
    val left = visitTree(sel.qualifier).asInstanceOf[Expression]
    val operator = mapOperator(sel.name.toString)
    val rightExpr = visitTree(right).asInstanceOf[Expression]
    
    new J.Binary(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      left,
      JLeftPadded.build(operator),
      rightExpr,
      null // type will be set later
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
  
  private def visitSelect(sel: untpd.Select): J = {
    // For now, treat Select as a field access or method reference
    // This handles cases like "obj.field" or method references
    val target = visitTree(sel.qualifier).asInstanceOf[Expression]
    val name = new J.Identifier(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      Collections.emptyList(),
      sel.name.toString,
      null,
      null
    )
    
    new J.FieldAccess(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      target,
      JLeftPadded.build(name),
      null
    )
  }
  
  private def visitUnknown(tree: untpd.Tree): J.Unknown = {
    val prefix = extractPrefix(tree.span)
    val sourceText = extractSource(tree.span)
    
    val unknownSource = new J.Unknown.Source(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      sourceText
    )
    
    new J.Unknown(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      unknownSource
    )
  }
  
  private def extractPrefix(span: Spans.Span): Space = {
    if (!span.exists) {
      return Space.EMPTY
    }
    
    val start = cursor
    val adjustedTreeStart = Math.max(0, span.start - offsetAdjustment)
    
    if (adjustedTreeStart > cursor && adjustedTreeStart <= source.length) {
      cursor = adjustedTreeStart
      Space.build(source.substring(start, adjustedTreeStart), Collections.emptyList())
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
      // Debug output
      // println(s"extractSource: span=(${span.start}, ${span.end}), adjusted=($adjustedStart, $adjustedEnd), result='$result'")
      result
    } else {
      ""
    }
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