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

import dotty.tools.dotc.ast.untpd
import dotty.tools.dotc.core.Constants.*
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.util.Spans
import org.openrewrite.Tree
import org.openrewrite.java.tree.*
import org.openrewrite.marker.Markers
import org.openrewrite.scala.marker.Implicit
import org.openrewrite.scala.marker.OmitBraces
import org.openrewrite.scala.marker.SObject
import org.openrewrite.scala.marker.ScalaForLoop
import org.openrewrite.scala.marker.ScalaLazyVal

import java.util
import java.util.{Collections, Arrays}

/**
 * Visitor that traverses the Scala compiler AST and builds OpenRewrite LST nodes.
 */
class ScalaTreeVisitor(source: String, offsetAdjustment: Int = 0)(implicit ctx: Context) {
  
  private var cursor = 0
  private var isInImportContext = false
  
  def getCursor: Int = cursor
  
  def updateCursor(position: Int): Unit = {
    val adjustedPosition = Math.max(0, position - offsetAdjustment)
    if (adjustedPosition > cursor && adjustedPosition <= source.length) {
      cursor = adjustedPosition
    }
  }
  
  def visitTree(tree: untpd.Tree): J = {
    tree match {
      case _ if tree.isEmpty => visitUnknown(tree)
      case lit: untpd.Literal => visitLiteral(lit)
      case num: untpd.Number => visitNumber(num)
      case id: untpd.Ident => visitIdent(id)
      case app: untpd.Apply => visitApply(app)
      case sel: untpd.Select => visitSelect(sel)
      case infixOp: untpd.InfixOp => visitInfixOp(infixOp)
      case prefixOp: untpd.PrefixOp => visitPrefixOp(prefixOp)
      case postfixOp: untpd.PostfixOp => visitPostfixOp(postfixOp)
      case parens: untpd.Parens => visitParentheses(parens)
      case imp: untpd.Import => visitImport(imp)
      case pkg: untpd.PackageDef => visitPackageDef(pkg)
      case newTree: untpd.New => visitNew(newTree)
      case vd: untpd.ValDef => visitValDef(vd)
      case md: untpd.ModuleDef => visitModuleDef(md)
      case asg: untpd.Assign => visitAssign(asg)
      case ifTree: untpd.If => visitIf(ifTree)
      case whileTree: untpd.WhileDo => visitWhileDo(whileTree)
      case forTree: untpd.ForDo => visitForDo(forTree)
      case block: untpd.Block => visitBlock(block)
      case td: untpd.TypeDef if td.isClassDef => visitClassDef(td)
      case dd: untpd.DefDef => visitDefDef(dd)
      case ret: untpd.Return => visitReturn(ret)
      case thr: untpd.Throw => visitThrow(thr)
      case ta: untpd.TypeApply => visitTypeApply(ta)
      case at: untpd.AppliedTypeTree => visitAppliedTypeTree(at)
      case func: untpd.Function => visitFunction(func)
      case _ => visitUnknown(tree)
    }
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
    var simpleName = id.name.toString
    
    // Special handling for wildcard imports: convert Scala's "_" to Java's "*"
    // This is needed because J.Import expects "*" for wildcard imports
    if (simpleName == "_" && isInImportContext) {
      simpleName = "*"
    }
    
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
    // Constructor calls like "new Person()" are parsed as Apply(New(Person), List())
    // Annotations like "@deprecated" are parsed as Apply(Select(New(Ident(deprecated)), <init>), List())
    
    // Check if this is an annotation pattern (will be handled specially when called from visitClassDef)
    // Annotations look like Apply(Select(New(...), <init>), args) with @ in source
    // Constructor calls look the same but have "new" in source
    val isAnnotationPattern = app.fun match {
      case sel: untpd.Select if sel.name.toString == "<init>" =>
        sel.qualifier match {
          case newNode: untpd.New =>
            // Check if the source has @ before the type (annotation) or "new" (constructor)
            if (app.span.exists) {
              val adjustedStart = Math.max(0, app.span.start - offsetAdjustment)
              val adjustedEnd = Math.max(0, app.span.end - offsetAdjustment)
              if (adjustedStart < adjustedEnd && adjustedEnd <= source.length) {
                val sourceText = source.substring(adjustedStart, adjustedEnd)
                sourceText.trim.startsWith("@")
              } else {
                false
              }
            } else {
              false
            }
          case _ => false
        }
      case _ => false
    }
    
    if (isAnnotationPattern) {
      // This is an annotation - convert to J.Annotation
      return visitAnnotation(app)
    }
    
    app.fun match {
      case newTree: untpd.New =>
        // This is a constructor call with arguments (shouldn't happen in Scala 3)
        System.out.println(s"DEBUG visitApply: Handling new class with New node, app.span=${app.span}, newTree.span=${newTree.span}")
        visitNewClassWithArgs(newTree, app)
      case sel: untpd.Select if sel.name.toString == "<init>" =>
        // This is a constructor call like new Person()
        sel.qualifier match {
          case newTree: untpd.New =>
            System.out.println(s"DEBUG visitApply: Handling new class with Select <init>, app.span=${app.span}")
            visitNewClassWithArgs(newTree, app)
          case _ =>
            visitUnknown(app)
        }
      case sel: untpd.Select if app.args.isEmpty && isUnaryOperator(sel.name.toString) =>
        // This is a unary operation
        visitUnary(sel)
      case sel: untpd.Select if app.args.length == 1 && isBinaryOperator(sel.name.toString) =>
        // This is likely a binary operation (infix notation)
        visitBinary(sel, app.args.head, Some(app.span))
      case sel: untpd.Select =>
        // Method call with dot notation like "obj.method(args)"
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
  
  private def visitAnnotation(app: untpd.Apply): J.Annotation = {
    val prefix = extractPrefix(app.span)
    
    
    // Extract the annotation type and arguments
    val (annotationType, args) = app.fun match {
      case sel: untpd.Select if sel.name.toString == "<init>" =>
        sel.qualifier match {
          case newTree: untpd.New =>
            val typeIdent = newTree.tpt match {
              case id: untpd.Ident => id
              case _ => return visitUnknown(app).asInstanceOf[J.Annotation]
            }
            (typeIdent, app.args)
          case _ => return visitUnknown(app).asInstanceOf[J.Annotation]
        }
      case _ => return visitUnknown(app).asInstanceOf[J.Annotation]
    }
    
    // Create the annotation type
    val annotTypeTree = new J.Identifier(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      Collections.emptyList(),
      annotationType.name.toString,
      null,
      null
    )
    
    // Convert arguments
    val arguments = if (args.isEmpty) {
      null
    } else {
      val argList = new util.ArrayList[JRightPadded[Expression]]()
      for ((arg, i) <- args.zipWithIndex) {
        val expr = visitTree(arg).asInstanceOf[Expression]
        val isLast = i == args.length - 1
        argList.add(JRightPadded.build(expr).withAfter(if (isLast) Space.EMPTY else Space.SINGLE_SPACE))
      }
      JContainer.build(
        Space.EMPTY,
        argList,
        Markers.EMPTY
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
  
  private def visitPrefixOp(prefixOp: untpd.PrefixOp): J.Unary = {
    val prefix = extractPrefix(prefixOp.span)
    val operator = mapPrefixOperator(prefixOp.op.name.toString)
    
    // Update cursor to the end of the operator
    updateCursor(prefixOp.op.span.end)
    
    // Now visit the expression
    val expr = visitTree(prefixOp.od) match {
      case e: Expression => e
      case _ => return visitUnknown(prefixOp).asInstanceOf[J.Unary]
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
  
  private def visitPostfixOp(postfixOp: untpd.PostfixOp): J.Unary = {
    val prefix = extractPrefix(postfixOp.span)
    
    val expr = visitTree(postfixOp.od) match {
      case e: Expression => e
      case _ => return visitUnknown(postfixOp).asInstanceOf[J.Unary]
    }
    
    // For postfix operators, we need to determine the operator type
    // Currently only handling "!" as PostDecrement (as a placeholder)
    // In a real implementation, we'd need to map specific postfix operators
    val operator = J.Unary.Type.PostDecrement // This is a placeholder
    
    new J.Unary(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      JLeftPadded.build(operator).withBefore(Space.EMPTY),
      expr,
      JavaType.Primitive.Boolean
    )
  }
  
  private def mapPrefixOperator(op: String): J.Unary.Type = op match {
    case "!" => J.Unary.Type.Not
    case "+" => J.Unary.Type.Positive
    case "-" => J.Unary.Type.Negative
    case "~" => J.Unary.Type.Complement
    case _ => J.Unary.Type.Not // default
  }
  
  private def visitMethodInvocation(app: untpd.Apply): J = {
    val prefix = extractPrefix(app.span)
    
    // Check if this is array/collection access (apply method with single argument)
    app.fun match {
      case sel: untpd.Select if sel.name.toString == "apply" && app.args.length == 1 =>
        // This is array/collection access: arr(index) which desugars to arr.apply(index)
        return visitArrayAccess(app, sel)
      case sel: untpd.Select if sel.name.toString == "apply" =>
        // Check if this is Array creation: Array.apply(elements...)
        sel.qualifier match {
          case id: untpd.Ident if id.name.toString == "Array" =>
            // This is array creation: Array(1, 2, 3) which desugars to Array.apply(1, 2, 3)
            return visitNewArray(app, sel)
          case _ =>
            // Continue with regular method invocation
        }
      case ta: untpd.TypeApply =>
        // Handle type applications like Array[String]("hello", "world")
        ta.fun match {
          case id: untpd.Ident if id.name.toString == "Array" =>
            // This is typed array creation: Array[String](...) 
            return visitNewArrayWithType(app, ta)
          case _ =>
            // Continue with regular method invocation
        }
      case _ =>
        // Continue with regular method invocation
    }
    
    // Handle the method call target
    val (select: Expression, methodName: String, typeParams: java.util.List[Expression]) = app.fun match {
      case sel: untpd.Select =>
        // Method call like obj.method(...) or package.Class.method(...)
        // The Select node represents the full method access (e.g., System.out.println)
        // We need to use sel.qualifier as the receiver and sel.name as the method name
        
        // Debug: check what we're dealing with
        // println(s"DEBUG visitMethodInvocation: sel=$sel, qualifier=${sel.qualifier}, name=${sel.name}")
        
        val target = visitTree(sel.qualifier) match {
          case expr: Expression => expr
          case _ => return visitUnknown(app)
        }
        
        // Update cursor position to after the method name to avoid re-reading it
        if (sel.nameSpan.exists) {
          val nameEnd = Math.max(0, sel.nameSpan.end - offsetAdjustment)
          if (nameEnd > cursor) {
            cursor = nameEnd
          }
        }
        
        (target, sel.name.toString, Collections.emptyList[Expression]())
        
      case id: untpd.Ident =>
        // Simple function call like println(...)
        (null, id.name.toString, Collections.emptyList[Expression]())
        
      case typeApp: untpd.TypeApply =>
        // Method with type parameters like List.empty[Int]
        // For now, fall back to Unknown for type applications
        return visitUnknown(app)
        
      case _ =>
        // Other kinds of function applications
        return visitUnknown(app)
    }
    
    // Extract space before opening parenthesis
    val argContainerPrefix = if (app.args.nonEmpty) {
      // Look for the opening parenthesis after the current cursor position
      sourceBefore("(")
    } else {
      // No arguments, but there might still be empty parentheses
      // Look for the opening parenthesis
      val parenIndex = positionOfNext("(")
      if (parenIndex >= 0) {
        sourceBefore("(")
      } else {
        Space.EMPTY
      }
    }
    
    // Visit arguments
    val args = new util.ArrayList[JRightPadded[Expression]]()
    for (i <- app.args.indices) {
      val arg = app.args(i)
      
      // Extract prefix space for this argument (space after previous comma)
      var argPrefix = Space.EMPTY
      if (i > 0) {
        val prevEnd = Math.max(0, app.args(i - 1).span.end - offsetAdjustment)
        val thisStart = Math.max(0, arg.span.start - offsetAdjustment)
        if (prevEnd < thisStart && prevEnd >= cursor && thisStart <= source.length) {
          val between = source.substring(prevEnd, thisStart)
          val commaIndex = between.indexOf(',')
          if (commaIndex >= 0) {
            argPrefix = Space.format(between.substring(commaIndex + 1))
            cursor = prevEnd + commaIndex + 1
          }
        }
      }
      
      visitTree(arg) match {
        case expr: Expression => 
          // Apply the prefix space to the expression
          val exprWithPrefix = expr match {
            case lit: J.Literal => lit.withPrefix(argPrefix)
            case id: J.Identifier => id.withPrefix(argPrefix)
            case mi: J.MethodInvocation => mi.withPrefix(argPrefix)
            case na: J.NewArray => na.withPrefix(argPrefix)
            case bin: J.Binary => bin.withPrefix(argPrefix)
            case aa: J.ArrayAccess => aa.withPrefix(argPrefix)
            case fa: J.FieldAccess => fa.withPrefix(argPrefix)
            case paren: J.Parentheses[_] => paren.withPrefix(argPrefix)
            case unknown: J.Unknown => unknown.withPrefix(argPrefix)
            case nc: J.NewClass => nc.withPrefix(argPrefix)
            case asg: J.Assignment => asg.withPrefix(argPrefix)
            case _ => expr
          }
          
          args.add(JRightPadded.build(exprWithPrefix))
        case _ => return visitUnknown(app) // If any argument fails, fall back
      }
    }
    
    // For method names, we typically don't need a prefix space since the dot is handled by the printer
    val nameSpace = Space.EMPTY
    
    // Create the method name identifier
    val name = new J.Identifier(
      Tree.randomId(),
      nameSpace,
      Markers.EMPTY,
      Collections.emptyList(),
      methodName,
      null,
      null
    )
    
    // Build the arguments container
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
    
    new J.MethodInvocation(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      if (select != null) JRightPadded.build(select) else null,
      null, // typeParameters - handled separately in TypeApply
      name,
      argContainer,
      null  // method type will be set later
    )
  }
  
  private def visitArrayAccess(app: untpd.Apply, sel: untpd.Select): J.ArrayAccess = {
    val prefix = extractPrefix(app.span)
    
    // Visit the array/collection expression
    val array = visitTree(sel.qualifier) match {
      case expr: Expression => expr
      case _ => return visitUnknown(app).asInstanceOf[J.ArrayAccess]
    }
    
    // Visit the index expression
    val index = visitTree(app.args.head) match {
      case expr: Expression => expr
      case _ => return visitUnknown(app).asInstanceOf[J.ArrayAccess]
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
      null // type will be set later
    )
  }
  
  private def visitNewArray(app: untpd.Apply, sel: untpd.Select): J.NewArray = {
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
        case _ => return visitUnknown(app).asInstanceOf[J.NewArray]
      }
    }
    
    // Create the initializer container
    val initializer = if (elements.isEmpty) {
      null
    } else {
      // Extract space before opening parenthesis (which acts like opening brace in Java)
      val initPrefix = sourceBefore("(")
      
      // Build padded elements with proper spacing
      val paddedElements = new util.ArrayList[JRightPadded[Expression]]()
      for (i <- 0 until elements.size()) {
        val elem = elements.get(i)
        // Extract space after element (before comma or closing paren)
        val afterSpace = if (i < elements.size() - 1) {
          sourceBefore(",")
        } else {
          sourceBefore(")")
        }
        paddedElements.add(JRightPadded.build(elem).withAfter(afterSpace))
      }
      
      JContainer.build(initPrefix, paddedElements, Markers.EMPTY)
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
      null // type will be set later
    )
  }
  
  private def visitNewArrayWithType(app: untpd.Apply, ta: untpd.TypeApply): J.NewArray = {
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
      val closeBracketPos = source.indexOf(']', typeEnd)
      if (closeBracketPos >= 0) {
        cursor = closeBracketPos + 1
      }
    }
    
    // Visit the array initializer elements
    val elements = new util.ArrayList[Expression]()
    for (arg <- app.args) {
      visitTree(arg) match {
        case expr: Expression => elements.add(expr)
        case _ => return visitUnknown(app).asInstanceOf[J.NewArray]
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
      // Extract space before opening parenthesis
      val initPrefix = sourceBefore("(")
      
      // Build padded elements with proper spacing
      val paddedElements = new util.ArrayList[JRightPadded[Expression]]()
      for (i <- 0 until elements.size()) {
        val elem = elements.get(i)
        // Extract space after element (before comma or closing paren)
        val afterSpace = if (i < elements.size() - 1) {
          sourceBefore(",")
        } else {
          sourceBefore(")")
        }
        paddedElements.add(JRightPadded.build(elem).withAfter(afterSpace))
      }
      
      JContainer.build(initPrefix, paddedElements, Markers.EMPTY)
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
      null // type will be set later
    )
  }
  
  private def isBinaryOperator(name: String): Boolean = {
    // Check if this is a known binary operator
    Set("+", "-", "*", "/", "%", "==", "!=", "<", ">", "<=", ">=", 
        "&&", "||", "&", "|", "^", "<<", ">>", ">>>", "::", "++").contains(name)
  }
  
  private def visitBinary(sel: untpd.Select, right: untpd.Tree, appSpan: Option[Spans.Span] = None): J.Binary = {
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
    // Check if this is a unary operator method reference without application
    if (isUnaryOperator(sel.name.toString)) {
      // This is something like "x.unary_-" without parentheses - preserve as Unknown
      visitUnknown(sel)
    } else {
      // Map Select to J.FieldAccess
      // Extract prefix for this select
      val prefix = extractPrefix(sel.span)
      
      // Visit the qualifier (target) - this could be an identifier, another select, etc.
      val target = visitTree(sel.qualifier) match {
        case expr: Expression => expr
        case _ => 
          // If the qualifier doesn't produce an expression, fall back to Unknown
          return visitUnknown(sel)
      }
      
      // Extract the space before the dot
      val qualifierEnd = sel.qualifier.span.end
      val nameStart = sel.nameSpan.start
      val dotSpace = if (qualifierEnd < nameStart) {
        val dotStart = Math.max(0, qualifierEnd - offsetAdjustment)
        val nameStartAdjusted = Math.max(0, nameStart - offsetAdjustment)
        if (dotStart < nameStartAdjusted && dotStart >= cursor && nameStartAdjusted <= source.length) {
          val between = source.substring(dotStart, nameStartAdjusted)
          // Find the dot and extract space before the name
          val dotIndex = between.indexOf('.')
          if (dotIndex >= 0 && dotIndex + 1 < between.length) {
            Space.format(between.substring(dotIndex + 1))
          } else {
            Space.EMPTY
          }
        } else {
          Space.EMPTY
        }
      } else {
        Space.EMPTY
      }
      
      // Create the name identifier
      val name = new J.Identifier(
        Tree.randomId(),
        dotSpace,
        Markers.EMPTY,
        Collections.emptyList(),
        sel.name.toString,
        null,
        null
      )
      
      // Consume up to the end of the selection
      if (sel.span.exists) {
        val adjustedEnd = Math.max(0, sel.span.end - offsetAdjustment)
        if (adjustedEnd > cursor && adjustedEnd <= source.length) {
          cursor = adjustedEnd
        }
      }
      
      new J.FieldAccess(
        Tree.randomId(),
        prefix,
        Markers.EMPTY,
        target,
        JLeftPadded.build(name),
        null
      )
    }
  }
  
  private def visitInfixOp(infixOp: untpd.InfixOp): J = {
    val opName = infixOp.op.name.toString
    
    // Check if this is a compound assignment operator
    if (opName.endsWith("=") && opName != "==" && opName != "!=" && opName != "<=" && opName != ">=" && opName.length > 1) {
      // This is a compound assignment like +=, -=, *=, /=
      val prefix = extractPrefix(infixOp.span)
      
      // Visit the left side (variable)
      val variable = visitTree(infixOp.left) match {
        case expr: Expression => expr
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
        case _ => return visitUnknown(infixOp)
      }
      
      // Extract space around the operator
      val leftEnd = Math.max(0, infixOp.left.span.end - offsetAdjustment)
      val opStart = Math.max(0, infixOp.op.span.start - offsetAdjustment)
      val opEnd = Math.max(0, infixOp.op.span.end - offsetAdjustment)
      val rightStart = Math.max(0, infixOp.right.span.start - offsetAdjustment)
      
      var operatorSpace = Space.EMPTY
      var valueSpace = Space.EMPTY
      
      if (leftEnd < opStart && leftEnd >= cursor && opStart <= source.length) {
        operatorSpace = Space.format(source.substring(leftEnd, opStart))
      }
      
      if (opEnd < rightStart && opEnd >= cursor && rightStart <= source.length) {
        valueSpace = Space.format(source.substring(opEnd, rightStart))
      }
      
      // Visit the right side (value)
      cursor = Math.max(0, infixOp.right.span.start - offsetAdjustment)
      val value = visitTree(infixOp.right) match {
        case expr: Expression => expr
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
        null // type
      )
    } else {
      // This is a regular binary operation
      visitBinaryOperation(infixOp)
    }
  }
  
  private def visitBinaryOperation(infixOp: untpd.InfixOp): J.Binary = {
    val prefix = extractPrefix(infixOp.span)
    
    // Visit left expression
    val left = visitTree(infixOp.left) match {
      case expr: Expression => expr
      case _ => return visitUnknown(infixOp).asInstanceOf[J.Binary]
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
    
    if (leftEnd < opStart && leftEnd >= cursor && opStart <= source.length) {
      operatorSpace = Space.format(source.substring(leftEnd, opStart))
    }
    
    if (opEnd < rightStart && opEnd >= cursor && rightStart <= source.length) {
      rightSpace = Space.format(source.substring(opEnd, rightStart))
    }
    
    // Visit right expression  
    cursor = Math.max(0, infixOp.right.span.start - offsetAdjustment)
    val right = visitTree(infixOp.right) match {
      case expr: Expression => expr
      case _ => return visitUnknown(infixOp).asInstanceOf[J.Binary]
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
      null // type
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
          field.get(parens).asInstanceOf[untpd.Tree]
        case None =>
          // Fall back to productElement approach
          if (parens.productArity > 0) {
            parens.productElement(0).asInstanceOf[untpd.Tree]
          } else {
            return visitUnknown(parens)
          }
      }
    } catch {
      case _: Exception => return visitUnknown(parens)
    }
    
    // Visit the inner tree
    val innerExpr = visitTree(innerTree) match {
      case expr: Expression => expr
      case _ => return visitUnknown(parens)
    }
    
    // Extract space before the closing parenthesis
    val innerEnd = innerTree.span.end
    val parenEnd = parens.span.end
    val closingSpace = if (innerEnd < parenEnd - 1) {
      val adjustedInnerEnd = Math.max(0, innerEnd - offsetAdjustment)
      val adjustedParenEnd = Math.max(0, parenEnd - 1 - offsetAdjustment)
      if (adjustedInnerEnd < adjustedParenEnd && adjustedInnerEnd >= cursor && adjustedParenEnd <= source.length) {
        Space.format(source.substring(adjustedInnerEnd, adjustedParenEnd))
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
  
  private def visitNewClassWithArgs(newTree: untpd.New, app: untpd.Apply): J.NewClass = {
    // The Apply node has the full span including "new", use its prefix
    val prefix = extractPrefix(app.span)
    
    // Extract space between "new" and the type
    // First, consume "new" keyword
    val newPos = positionOfNext("new")
    if (newPos >= 0 && newPos == cursor) {
      cursor += 3 // Move past "new"
    }
    
    // Extract space between "new" and type
    val typeStart = Math.max(0, newTree.tpt.span.start - offsetAdjustment)
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
      case _ => return visitUnknown(app).asInstanceOf[J.NewClass]
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
      val parenIndex = between.indexOf('(')
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
          val parenIndex = after.indexOf('(')
          beforeParenSpace = Space.format(after.substring(0, parenIndex))
          cursor = typeEnd + after.indexOf(')') + 1
        }
      }
    }
    
    // Visit arguments
    val args = new util.ArrayList[JRightPadded[Expression]]()
    for (i <- app.args.indices) {
      val arg = app.args(i)
      
      // Extract prefix space for this argument (space after previous comma)
      var argPrefix = Space.EMPTY
      if (i > 0) {
        val prevEnd = Math.max(0, app.args(i - 1).span.end - offsetAdjustment)
        val thisStart = Math.max(0, arg.span.start - offsetAdjustment)
        if (prevEnd < thisStart && prevEnd >= cursor && thisStart <= source.length) {
          val between = source.substring(prevEnd, thisStart)
          val commaIndex = between.indexOf(',')
          if (commaIndex >= 0) {
            argPrefix = Space.format(between.substring(commaIndex + 1))
            cursor = prevEnd + commaIndex + 1
          }
        }
      }
      
      visitTree(arg) match {
        case expr: Expression =>
          // Apply the prefix space to the expression
          val exprWithPrefix = expr match {
            case lit: J.Literal => lit.withPrefix(argPrefix)
            case id: J.Identifier => id.withPrefix(argPrefix)
            case mi: J.MethodInvocation => mi.withPrefix(argPrefix)
            case na: J.NewArray => na.withPrefix(argPrefix)
            case bin: J.Binary => bin.withPrefix(argPrefix)
            case aa: J.ArrayAccess => aa.withPrefix(argPrefix)
            case fa: J.FieldAccess => fa.withPrefix(argPrefix)
            case paren: J.Parentheses[_] => paren.withPrefix(argPrefix)
            case unknown: J.Unknown => unknown.withPrefix(argPrefix)
            case nc: J.NewClass => nc.withPrefix(argPrefix)
            case asg: J.Assignment => asg.withPrefix(argPrefix)
            case _ => expr
          }
          
          args.add(JRightPadded.build(exprWithPrefix))
        case _ => return visitUnknown(app).asInstanceOf[J.NewClass]
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
      null // constructorType
    )
  }

  private def visitNew(newTree: untpd.New): J.NewClass = {
    // Anonymous classes in Scala are represented as New nodes with Template bodies
    // Example: new Runnable { def run() = ... }
    // The Template contains the parent types and the body implementation
    
    // Extract prefix but skip the "new" keyword
    var prefix = extractPrefix(newTree.span)
    
    // Skip past "new" in the source if present
    if (newTree.span.exists) {
      val start = Math.max(0, newTree.span.start - offsetAdjustment)
      val end = Math.max(0, newTree.span.end - offsetAdjustment)
      if (start >= cursor && end <= source.length && start < end) {
        val sourceText = source.substring(start, end)
        val newIndex = sourceText.indexOf("new")
        if (newIndex >= 0) {
          // Move cursor past "new" and any following space
          val afterNew = start + newIndex + 3
          if (afterNew < end) {
            updateCursor(afterNew)
            // Extract space after "new" keyword  
            val afterNewText = source.substring(afterNew, end)
            val spaceMatch = afterNewText.takeWhile(_.isWhitespace)
            if (spaceMatch.nonEmpty) {
              updateCursor(afterNew + spaceMatch.length)
            }
          }
        }
      }
    }
    
    // The New node's tpt is the Template containing the anonymous class definition
    newTree.tpt match {
      case template: untpd.Template =>
        // Extract the parent type(s) - usually the first parent is the main type
        val parents = template.parents
        if (parents.isEmpty) {
          return visitUnknown(newTree).asInstanceOf[J.NewClass]
        }
        
        // The first parent is typically an Apply node for constructor calls
        // or just an Ident/Select for interfaces/traits
        val firstParent = parents.head
        
        // Extract the class type and arguments
        val (clazz, args) = firstParent match {
          case app: untpd.Apply if app.fun.isInstanceOf[untpd.Select] && 
               app.fun.asInstanceOf[untpd.Select].name.toString == "<init>" =>
            // Constructor call with arguments: new Person("John", 30) { ... }
            val sel = app.fun.asInstanceOf[untpd.Select]
            sel.qualifier match {
              case newInner: untpd.New =>
                // Visit the type tree directly
                val typeTree = visitTree(newInner.tpt).asInstanceOf[TypeTree]
                
                // Now handle the arguments
                val argContainer = if (app.args.nonEmpty) {
                  val args = new util.ArrayList[JRightPadded[Expression]]()
                  
                  // Find the opening parenthesis
                  var beforeParenSpace = Space.EMPTY
                  if (app.span.exists) {
                    val typeEnd = Math.max(0, newInner.tpt.span.end - offsetAdjustment)
                    val argsStart = Math.max(0, app.span.start - offsetAdjustment)
                    
                    if (typeEnd < argsStart && typeEnd >= cursor && argsStart <= source.length) {
                      val between = source.substring(typeEnd, argsStart)
                      val parenIndex = between.indexOf('(')
                      if (parenIndex >= 0) {
                        beforeParenSpace = Space.format(between.substring(0, parenIndex))
                        updateCursor(typeEnd + parenIndex + 1)
                      }
                    }
                  }
                  
                  // Visit arguments
                  for ((arg, i) <- app.args.zipWithIndex) {
                    var argPrefix = Space.EMPTY
                    if (i > 0) {
                      val prevEnd = Math.max(0, app.args(i - 1).span.end - offsetAdjustment)
                      val thisStart = Math.max(0, arg.span.start - offsetAdjustment)
                      if (prevEnd < thisStart && prevEnd >= cursor && thisStart <= source.length) {
                        val between = source.substring(prevEnd, thisStart)
                        val commaIndex = between.indexOf(',')
                        if (commaIndex >= 0) {
                          argPrefix = Space.format(between.substring(commaIndex + 1))
                          updateCursor(prevEnd + commaIndex + 1)
                        }
                      }
                    }
                    
                    val argJ = visitTree(arg)
                    val visitedArg: Expression = if (argJ.isInstanceOf[Expression]) {
                      argJ.asInstanceOf[Expression].withPrefix(argPrefix)
                    } else {
                      System.out.println(s"DEBUG visitNew: Unexpected type for argument: ${argJ.getClass}, argJ=$argJ, arg=$arg")
                      visitUnknown(arg).asInstanceOf[Expression].withPrefix(argPrefix)
                    }
                    args.add(new JRightPadded[Expression](visitedArg, Space.EMPTY, Markers.EMPTY))
                  }
                  
                  // Extract space before closing parenthesis for last argument
                  if (app.span.exists && app.args.nonEmpty) {
                    val lastArgEnd = Math.max(0, app.args.last.span.end - offsetAdjustment)
                    val appEnd = Math.max(0, app.span.end - offsetAdjustment)
                    if (lastArgEnd < appEnd && lastArgEnd >= cursor && appEnd <= source.length) {
                      val remaining = source.substring(lastArgEnd, appEnd)
                      val closeParenIndex = remaining.indexOf(')')
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
        
        // Create the anonymous class body
        val body = if (template.body.nonEmpty) {
          // Filter out the synthetic constructor and self-reference
          val bodyStatements = template.body.filter {
            case dd: untpd.DefDef if dd.name.toString == "<init>" => false
            case vd: untpd.ValDef if vd.name.toString == "_" => false
            case _ => true
          }
          
          if (bodyStatements.nonEmpty) {
            // Extract space before the opening brace
            var beforeBrace = Space.EMPTY
            if (newTree.span.exists && clazz.getPrefix.getWhitespace.isEmpty) {
              val newStart = Math.max(0, newTree.span.start - offsetAdjustment)
              val newEnd = Math.max(0, newTree.span.end - offsetAdjustment)
              
              // Find the position after the type/arguments and before the brace
              val searchStart = if (args != null && args.getElements != null && args.getElements.size() > 0) {
                // After the closing parenthesis of arguments
                Math.max(cursor, newStart)
              } else {
                // After the type name
                Math.max(cursor, newStart)
              }
              
              if (searchStart < newEnd && searchStart >= 0 && newEnd <= source.length) {
                val sourceText = source.substring(searchStart, newEnd)
                val braceIndex = sourceText.indexOf('{')
                if (braceIndex >= 0) {
                  beforeBrace = Space.format(sourceText.substring(0, braceIndex))
                  updateCursor(searchStart + braceIndex + 1)
                }
              }
            }
            
            // Convert body statements
            val statements = new util.ArrayList[J]()
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
            
            // Extract space before closing brace
            var beforeCloseBrace = Space.EMPTY
            if (newTree.span.exists) {
              val newEnd = Math.max(0, newTree.span.end - offsetAdjustment)
              if (cursor < newEnd && cursor >= 0 && newEnd <= source.length) {
                val remaining = source.substring(cursor, newEnd)
                val closeBraceIndex = remaining.lastIndexOf('}')
                if (closeBraceIndex >= 0) {
                  beforeCloseBrace = Space.format(remaining.substring(0, closeBraceIndex))
                  updateCursor(cursor + closeBraceIndex + 1)
                }
              }
            }
            
            new J.Block(
              Tree.randomId(),
              beforeBrace,
              Markers.EMPTY,
              new JRightPadded[java.lang.Boolean](false, Space.EMPTY, Markers.EMPTY),
              statementPaddings,
              beforeCloseBrace
            )
          } else {
            null
          }
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
          null // constructorType
        )
        
      case _ =>
        // Not an anonymous class, shouldn't happen in visitNew
        visitUnknown(newTree).asInstanceOf[J.NewClass]
    }
  }

  private def visitImport(imp: untpd.Import): J = {
    // Check if this is a simple import (no braces) that can map to J.Import
    if (isSimpleImport(imp)) {
      // Extract the prefix - should only be whitespace/comments before "import"
      val adjustedStart = Math.max(0, imp.span.start - offsetAdjustment)
      val prefix = if (cursor < adjustedStart) {
        val prefixText = source.substring(cursor, adjustedStart)
        cursor = adjustedStart
        Space.format(prefixText)
      } else {
        Space.EMPTY
      }
      
      
      // Set import context flag for identifier processing
      val oldInImportContext = isInImportContext
      isInImportContext = true
      
      // Save current cursor position and move it to the start of the import expression
      // The import expression (imp.expr) should have its span starting after "import "
      val savedCursor = cursor
      if (imp.expr.span.exists) {
        val exprStart = Math.max(0, imp.expr.span.start - offsetAdjustment)
        cursor = exprStart
      }
      
      // Visit the import expression to get the field access
      val expr = visitTree(imp.expr)
      
      // Restore import context flag
      isInImportContext = oldInImportContext
      
      // For imports, we need a FieldAccess that includes the selectors
      var qualid = expr match {
        case fa: J.FieldAccess => fa
        case id: J.Identifier => 
          // Single identifier imports need to be wrapped in a FieldAccess
          // This shouldn't happen for valid Scala imports, but handle it just in case
          new J.FieldAccess(
            Tree.randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY),
            JLeftPadded.build(id),
            null
          )
        case other => 
          // Fall back to Unknown for complex cases
          return visitUnknown(imp)
      }
      
      // Handle selectors - in Scala, import java.util.List has "java.util" as expr and "List" as selector
      if (imp.selectors.nonEmpty && imp.selectors.size == 1) {
        val selector = imp.selectors.head
        selector match {
          case untpd.ImportSelector(ident: untpd.Ident, untpd.EmptyTree, untpd.EmptyTree) =>
            // Simple selector like "List" in "import java.util.List"
            // Need to advance cursor past the dot before the selector
            if (cursor < source.length && source.charAt(cursor) == '.') {
              cursor += 1 // Skip the dot
            }
            
            val selectorName = new J.Identifier(
              Tree.randomId(),
              Space.EMPTY,
              Markers.EMPTY,
              Collections.emptyList(),
              ident.name.toString,
              null,
              null
            )
            
            // Create a new FieldAccess with the selector
            qualid = new J.FieldAccess(
              Tree.randomId(),
              Space.EMPTY,
              Markers.EMPTY,
              qualid,
              JLeftPadded.build(selectorName),
              null
            )
          case _ =>
            // Complex selectors (aliases, wildcards, etc.) - keep as is for now
        }
      }
      
      // Update cursor to the end of the import
      if (imp.span.exists) {
        val adjustedEnd = Math.max(0, imp.span.end - offsetAdjustment)
        updateCursor(adjustedEnd)
      }
      
      // Create J.Import
      new J.Import(
        Tree.randomId(),
        prefix,
        Markers.EMPTY,
        JLeftPadded.build(false), // static imports are not supported in Scala
        qualid,
        null // no alias for simple imports
      )
    } else {
      // For complex imports with braces, aliases, etc., keep as Unknown for now
      // We'll implement S.Import for these later
      visitUnknown(imp)
    }
  }
  
  private def isSimpleImport(imp: untpd.Import): Boolean = {
    // Check if this is a simple import without braces
    if (imp.span.exists) {
      val adjustedStart = Math.max(0, imp.span.start - offsetAdjustment)
      val adjustedEnd = Math.max(0, imp.span.end - offsetAdjustment)
      if (adjustedStart >= 0 && adjustedEnd <= source.length && adjustedEnd > adjustedStart) {
        val importText = source.substring(adjustedStart, adjustedEnd)
        !importText.contains("{")
      } else {
        false
      }
    } else {
      false
    }
  }
  
  
  
  private def visitPackageDef(pkg: untpd.PackageDef): J = {
    // Package definitions at the statement level should not be converted to statements
    // They are handled at the compilation unit level
    // Return null to indicate this node should be skipped
    null
  }
  
  private def visitValDef(vd: untpd.ValDef): J = {
    val prefix = extractPrefix(vd.span)
    
    // Extract modifiers and keywords from source
    val adjustedStart = Math.max(0, vd.span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, vd.span.end - offsetAdjustment)
    
    // Extract source to find modifier keywords and val/var
    var valVarKeyword = ""
    var beforeValVar = Space.EMPTY
    var afterValVar = Space.EMPTY
    val modifiers = new util.ArrayList[J.Modifier]()
    var hasExplicitFinal = false
    var hasExplicitLazy = false
    
    if (adjustedStart >= cursor && adjustedEnd <= source.length && adjustedStart < adjustedEnd) {
      val sourceSnippet = source.substring(cursor, adjustedEnd)
      
      // First, extract any modifiers before val/var
      var modifierEndPos = 0
      
      // Check for access modifiers
      if (sourceSnippet.startsWith("private ")) {
        modifiers.add(new J.Modifier(
          Tree.randomId(),
          Space.EMPTY,
          Markers.EMPTY,
          "private",
          J.Modifier.Type.Private,
          Collections.emptyList()
        ))
        modifierEndPos = "private ".length
      } else if (sourceSnippet.startsWith("protected ")) {
        modifiers.add(new J.Modifier(
          Tree.randomId(),
          Space.EMPTY,
          Markers.EMPTY,
          "protected",
          J.Modifier.Type.Protected,
          Collections.emptyList()
        ))
        modifierEndPos = "protected ".length
      }
      
      // Check for final modifier after access modifier
      val afterAccess = sourceSnippet.substring(modifierEndPos)
      if (afterAccess.startsWith("final ")) {
        hasExplicitFinal = true
        modifiers.add(new J.Modifier(
          Tree.randomId(),
          if (modifierEndPos > 0) Space.SINGLE_SPACE else Space.EMPTY,
          Markers.EMPTY,
          "final",
          J.Modifier.Type.Final,
          Collections.emptyList()
        ))
        modifierEndPos += "final ".length
      }
      
      // Check for lazy modifier
      val afterFinal = sourceSnippet.substring(modifierEndPos)
      if (afterFinal.startsWith("lazy ")) {
        hasExplicitLazy = true
        modifiers.add(new J.Modifier(
          Tree.randomId(),
          if (modifierEndPos > 0) Space.SINGLE_SPACE else Space.EMPTY,
          Markers.EMPTY,
          "lazy",
          J.Modifier.Type.LanguageExtension,
          Collections.emptyList()
        ))
        modifierEndPos += "lazy ".length
      }
      
      // Now find val/var after modifiers
      val afterModifiers = sourceSnippet.substring(modifierEndPos)
      val valIndex = afterModifiers.indexOf("val")
      val varIndex = afterModifiers.indexOf("var")
      
      val (keywordStart, keyword) = if (valIndex >= 0 && (varIndex < 0 || valIndex < varIndex)) {
        (valIndex, "val")
      } else if (varIndex >= 0) {
        (varIndex, "var")
      } else {
        (-1, "")
      }
      
      if (keywordStart >= 0) {
        // Extract space before val/var (after modifiers)
        if (keywordStart > 0) {
          beforeValVar = Space.format(afterModifiers.substring(0, keywordStart))
        }
        
        // Move cursor past all modifiers and the keyword
        cursor = cursor + modifierEndPos + keywordStart + keyword.length
        valVarKeyword = keyword
        
        // Extract space after val/var
        // Look for the variable name in the source
        val varNameStr = vd.name.toString
        val nameIndex = source.indexOf(varNameStr, cursor)
        if (nameIndex >= cursor) {
          afterValVar = Space.format(source.substring(cursor, nameIndex))
          cursor = nameIndex
        }
      }
    }
    
    // Val is implicitly final in Scala (but don't add it if we already have explicit final)
    val isFinal = valVarKeyword == "val"
    if (isFinal && !hasExplicitFinal) {
      modifiers.add(new J.Modifier(
        Tree.randomId(),
        if (modifiers.isEmpty) beforeValVar else Space.SINGLE_SPACE,
        Markers.EMPTY,
        null, // No keyword for implicit final
        J.Modifier.Type.Final,
        Collections.emptyList()
      ))
    }
    
    // Handle type annotation if present
    var typeExpression: TypeTree = null
    var beforeColon = Space.EMPTY
    var afterColon = Space.EMPTY
    
    if (vd.tpt != null && !vd.tpt.isEmpty && vd.tpt.span.exists) {
      // Find the end of the variable name in source
      val nameEnd = cursor + vd.name.toString.length
      val typeStart = Math.max(0, vd.tpt.span.start - offsetAdjustment)
      
      if (nameEnd < typeStart && typeStart <= source.length) {
        val between = source.substring(nameEnd, typeStart)
        val colonIndex = between.indexOf(':')
        if (colonIndex >= 0) {
          beforeColon = Space.format(between.substring(0, colonIndex))
          afterColon = Space.format(between.substring(colonIndex + 1))
          cursor = typeStart
        }
      }
      
      // Visit the type
      typeExpression = visitTree(vd.tpt) match {
        case tt: TypeTree => 
          // For type expressions in variable declarations, we need to preserve
          // the space after the colon
          tt match {
            case pt: J.ParameterizedType => pt.withPrefix(afterColon)
            case id: J.Identifier => id.withPrefix(afterColon)
            case fa: J.FieldAccess => fa.withPrefix(afterColon)
            case _ => tt
          }
        case _ => null
      }
    }
    
    // Extract variable name
    val varName = new J.Identifier(
      Tree.randomId(),
      afterValVar,
      Markers.EMPTY,
      Collections.emptyList(),
      vd.name.toString,
      null,
      null
    )
    
    // Update cursor past the name only if we haven't parsed a type
    // If we parsed a type, the cursor is already past the type
    if (typeExpression == null) {
      cursor = cursor + vd.name.toString.length
    }
    
    // Handle initializer
    var beforeEquals = Space.EMPTY
    var initializer: Expression = null
    
    if (vd.rhs != null && !vd.rhs.isEmpty && vd.rhs.span.exists) {
      val rhsStart = Math.max(0, vd.rhs.span.start - offsetAdjustment)
      
      // Look for equals sign
      if (cursor < rhsStart && rhsStart <= source.length) {
        val beforeRhs = source.substring(cursor, rhsStart)
        val equalsIndex = beforeRhs.indexOf('=')
        if (equalsIndex >= 0) {
          beforeEquals = Space.format(beforeRhs.substring(0, equalsIndex))
          val afterEqualsStr = beforeRhs.substring(equalsIndex + 1)
          cursor = rhsStart
          
          // Visit the initializer
          val rhsExpr = visitTree(vd.rhs) match {
            case expr: Expression => expr
            case _ => null
          }
          
          if (rhsExpr != null) {
            // Set initializer with space after equals
            initializer = rhsExpr match {
              case lit: J.Literal => lit.withPrefix(Space.format(afterEqualsStr))
              case id: J.Identifier => id.withPrefix(Space.format(afterEqualsStr))
              case mi: J.MethodInvocation => mi.withPrefix(Space.format(afterEqualsStr))
              case na: J.NewArray => na.withPrefix(Space.format(afterEqualsStr))
              case bin: J.Binary => bin.withPrefix(Space.format(afterEqualsStr))
              case aa: J.ArrayAccess => aa.withPrefix(Space.format(afterEqualsStr))
              case fa: J.FieldAccess => fa.withPrefix(Space.format(afterEqualsStr))
              case paren: J.Parentheses[_] => paren.withPrefix(Space.format(afterEqualsStr))
              case unknown: J.Unknown => unknown.withPrefix(Space.format(afterEqualsStr))
              case nc: J.NewClass => nc.withPrefix(Space.format(afterEqualsStr))
              case _ => 
                // For any other expression type, just return it as-is
                rhsExpr
            }
          }
        }
      }
    } else if (vd.rhs != null && vd.rhs.toString == "_") {
      // Handle uninitialized var: var x: Int = _
      // Look for the underscore in source
      val underscoreIndex = source.indexOf('_', cursor)
      if (underscoreIndex >= 0) {
        val beforeUnderscore = source.substring(cursor, underscoreIndex)
        val equalsIndex = beforeUnderscore.indexOf('=')
        if (equalsIndex >= 0) {
          beforeEquals = Space.format(beforeUnderscore.substring(0, equalsIndex))
          val afterEquals = Space.format(beforeUnderscore.substring(equalsIndex + 1))
          cursor = underscoreIndex + 1
          
          // Create a special identifier for the underscore
          initializer = new J.Identifier(
            Tree.randomId(),
            afterEquals,
            Markers.EMPTY,
            Collections.emptyList(),
            "_",
            null,
            null
          )
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
    new J.VariableDeclarations(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      Collections.emptyList(), // leadingAnnotations
      modifiers,
      typeExpression, // Store type here for now
      null, // varargs
      Collections.emptyList(), // dimensionsBeforeName
      Collections.singletonList(declarator)
    )
  }
  
  private def visitModuleDef(md: untpd.ModuleDef): J.ClassDeclaration = {
    val prefix = extractPrefix(md.span)
    
    // Extract the source text to find modifiers  
    val adjustedStart = Math.max(0, md.span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, md.span.end - offsetAdjustment)
    var modifierText = ""
    var objectIndex = -1
    
    if (adjustedStart >= cursor && adjustedEnd <= source.length) {
      val sourceSnippet = source.substring(cursor, adjustedEnd)
      objectIndex = sourceSnippet.indexOf("object")
      if (objectIndex > 0) {
        modifierText = sourceSnippet.substring(0, objectIndex)
      }
    }
    
    // Extract modifiers from text
    val (modifiers, lastModEnd) = extractModifiersFromText(md.mods, modifierText)
    
    // Check for case modifier (special handling as it's not a traditional modifier)
    if (modifierText.contains("case")) {
      val caseIndex = modifierText.indexOf("case")
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
    
    // Find where "object" keyword ends
    val objectKeywordPos = if (objectIndex >= 0) {
      cursor + objectIndex + "object".length
    } else {
      cursor
    }
    
    // Extract space between modifiers and "object" keyword
    val kindPrefix = if (!modifiers.isEmpty && objectIndex > 0) {
      val afterModifiers = if (modifierText.contains("case")) {
        modifierText.indexOf("case") + "case".length
      } else {
        lastModEnd
      }
      Space.format(modifierText.substring(afterModifiers, objectIndex))
    } else {
      Space.EMPTY
    }
    
    // Update cursor to after "object" keyword
    cursor = objectKeywordPos
    
    // Create the class kind (object instead of class)
    val kind = new J.ClassDeclaration.Kind(
      Tree.randomId(),
      kindPrefix,
      Markers.EMPTY,
      Collections.emptyList(),
      J.ClassDeclaration.Kind.Type.Class // We use Class type but mark with SObject
    )
    
    // Extract space between "object" and the name
    val nameStart = if (md.nameSpan.exists) {
      Math.max(0, md.nameSpan.start - offsetAdjustment)
    } else {
      objectKeywordPos
    }
    
    val nameSpace = if (objectKeywordPos < nameStart && nameStart <= source.length) {
      Space.format(source.substring(objectKeywordPos, nameStart))
    } else {
      Space.format(" ") // Default to single space
    }
    
    // Extract object name
    val name = new J.Identifier(
      Tree.randomId(),
      nameSpace,
      Markers.EMPTY,
      Collections.emptyList(),
      md.name.toString,
      null,
      null
    )
    
    // Update cursor to after the name
    if (md.nameSpan.exists) {
      cursor = Math.max(0, md.nameSpan.end - offsetAdjustment)
    }
    
    // Objects cannot have type parameters
    val typeParameters: JContainer[J.TypeParameter] = null
    
    // Objects cannot have constructor parameters  
    val primaryConstructor: JContainer[Statement] = null
    
    // Extract extends/with clauses from the implementation template
    var extendings: JLeftPadded[TypeTree] = null
    var implementings: JContainer[TypeTree] = null
    
    md.impl match {
      case tmpl: untpd.Template if tmpl.parents.nonEmpty =>
        // Handle extends/with clauses similar to classes
        // Look for "extends" keyword and extract space before it
        var extendsSpace = Space.format(" ")
        if (cursor < source.length && tmpl.parents.head.span.exists) {
          val parentStart = Math.max(0, tmpl.parents.head.span.start - offsetAdjustment)
          if (cursor < parentStart && parentStart <= source.length) {
            val beforeParent = source.substring(cursor, parentStart)
            val extendsIdx = beforeParent.indexOf("extends")
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
        val extendsType = visitTree(firstParent) match {
          case typeTree: TypeTree => typeTree
          case _ => visitUnknown(firstParent).asInstanceOf[TypeTree]
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
              val withIdx = beforeFirstWith.indexOf("with")
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
              case _ => visitUnknown(parent).asInstanceOf[TypeTree]
            }
            
            // For subsequent traits, extract space between them
            var trailingSpace = Space.EMPTY
            if (i < tmpl.parents.size - 1 && parent.span.exists && tmpl.parents(i + 1).span.exists) {
              val thisEnd = Math.max(0, parent.span.end - offsetAdjustment)
              val nextStart = Math.max(0, tmpl.parents(i + 1).span.start - offsetAdjustment)
              if (thisEnd < nextStart && nextStart <= source.length) {
                val between = source.substring(thisEnd, nextStart)
                val withIdx = between.indexOf("with")
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
      case tmpl: untpd.Template if tmpl.body.nonEmpty =>
        // Find the opening brace
        var bodyPrefix = Space.EMPTY
        if (cursor < source.length && md.span.exists) {
          val remaining = source.substring(cursor, Math.min(md.span.end - offsetAdjustment, source.length))
          val braceIdx = remaining.indexOf('{')
          if (braceIdx >= 0) {
            bodyPrefix = Space.format(remaining.substring(0, braceIdx))
            cursor = cursor + braceIdx + 1 // Skip past the opening brace
          }
        }
        
        // Create a block from the template body
        val statements = new util.ArrayList[JRightPadded[Statement]]()
        tmpl.body.foreach { stat =>
          visitTree(stat) match {
            case stmt: Statement => statements.add(JRightPadded.build(stmt))
            case _ => // Skip non-statements
          }
        }
        
        // Find the closing brace to get the end space
        var endSpace = Space.EMPTY
        if (cursor < source.length && md.span.exists) {
          val endPos = Math.max(0, md.span.end - offsetAdjustment) 
          val remaining = source.substring(cursor, Math.min(endPos, source.length))
          val closeBraceIdx = remaining.lastIndexOf('}')
          if (closeBraceIdx >= 0) {
            endSpace = Space.format(remaining.substring(0, closeBraceIdx))
            cursor = endPos // Update to end of object
          }
        }
        
        new J.Block(
          Tree.randomId(),
          bodyPrefix,
          Markers.EMPTY,
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
    
    // Create the class declaration with SObject marker
    new J.ClassDeclaration(
      Tree.randomId(),
      prefix,
      Markers.build(Collections.singletonList(SObject.create())),
      Collections.emptyList(), // annotations
      modifiers,
      kind,
      name,
      typeParameters,
      primaryConstructor,
      extendings,
      implementings,
      null, // permits
      body,
      null // type
    )
  }
  
  private def visitAssign(asg: untpd.Assign): J = {
    val prefix = extractPrefix(asg.span)
    
    // Visit the left-hand side (variable)
    val variable = visitTree(asg.lhs) match {
      case expr: Expression => expr
      case _ => return visitUnknown(asg)
    }
    
    // Find the position of the equals sign
    val lhsEnd = Math.max(0, asg.lhs.span.end - offsetAdjustment)
    val rhsStart = Math.max(0, asg.rhs.span.start - offsetAdjustment)
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
          val equalsIndex = between.indexOf('=')
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
    
    // Visit the right-hand side (value)
    val value = visitTree(asg.rhs) match {
      case expr: Expression => expr
      case _ => return visitUnknown(asg)
    }
    
    // Update cursor to the end of the assignment
    updateCursor(asg.span.end)
    
    if (isCompoundAssignment) {
      // Check if rhs is a binary operation with lhs as the left operand
      // Scala desugars x += 5 to x = x + 5
      val assignment = asg.rhs match {
        case app: untpd.Apply =>
          app.fun match {
            case sel: untpd.Select if sel.qualifier == asg.lhs =>
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
        null // type
      )
    } else {
      new J.Assignment(
        Tree.randomId(),
        prefix,
        Markers.EMPTY,
        variable,
        JLeftPadded.build(value.withPrefix(valueSpace)).withBefore(equalsSpace),
        null // type - will be inferred later
      )
    }
  }
  
  private def visitIf(ifTree: untpd.If): J.If = {
    val prefix = extractPrefix(ifTree.span)
    
    
    // Find where the condition parentheses start
    val adjustedStart = Math.max(0, ifTree.span.start - offsetAdjustment)
    val condStart = Math.max(0, ifTree.cond.span.start - offsetAdjustment)
    
    // Extract space before parentheses and move cursor past "if" to the condition
    var beforeParenSpace = Space.EMPTY
    if (adjustedStart < condStart && cursor <= condStart) {
      // Look for the opening parenthesis after "if"
      val searchEnd = Math.min(condStart + 1, source.length) // Include the '(' character
      val between = source.substring(cursor, searchEnd)
      val ifIndex = between.indexOf("if")
      if (ifIndex >= 0) {
        val afterIf = ifIndex + 2
        val remainingStr = between.substring(afterIf)
        val parenIndex = remainingStr.indexOf('(')
        if (parenIndex >= 0) {
          beforeParenSpace = Space.format(remainingStr.substring(0, parenIndex))
          // Move cursor to the opening parenthesis
          cursor = cursor + afterIf + parenIndex
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
              field.get(parens).asInstanceOf[untpd.Tree]
            case None =>
              // Fall back to productElement approach
              if (parens.productArity > 0) {
                parens.productElement(0).asInstanceOf[untpd.Tree]
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
      case _ => return visitUnknown(ifTree).asInstanceOf[J.If]
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
            afterCondSpace = Space.format(source.substring(adjustedInnerEnd, adjustedParenEnd))
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
    
    // Visit the then branch
    val thenPart = visitTree(ifTree.thenp) match {
      case stmt: Statement => JRightPadded.build(stmt)
      case _ => return visitUnknown(ifTree).asInstanceOf[J.If]
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
        val elseIndex = between.indexOf("else")
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
        case _ => return visitUnknown(ifTree).asInstanceOf[J.If]
      }
    }
    
    // Update cursor to end of the if expression
    updateCursor(ifTree.span.end)
    
    new J.If(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
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
  
  private def visitWhileDo(whileTree: untpd.WhileDo): J.WhileLoop = {
    val prefix = extractPrefix(whileTree.span)
    
    // Find where the condition parentheses start
    val adjustedStart = Math.max(0, whileTree.span.start - offsetAdjustment)
    val condStart = Math.max(0, whileTree.cond.span.start - offsetAdjustment)
    
    // Extract space before parentheses and move cursor past "while" to the condition
    var beforeParenSpace = Space.EMPTY
    if (adjustedStart < condStart && cursor <= condStart) {
      val searchEnd = Math.min(condStart + 1, source.length) // Include the '(' character
      val between = source.substring(cursor, searchEnd)
      val whileIndex = between.indexOf("while")
      if (whileIndex >= 0) {
        val afterWhile = whileIndex + 5 // "while" is 5 chars
        val remainingStr = between.substring(afterWhile)
        val parenIndex = remainingStr.indexOf('(')
        if (parenIndex >= 0) {
          beforeParenSpace = Space.format(remainingStr.substring(0, parenIndex))
          // Move cursor to the opening parenthesis
          cursor = cursor + afterWhile + parenIndex
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
              field.get(parens).asInstanceOf[untpd.Tree]
            case None =>
              if (parens.productArity > 0) {
                parens.productElement(0).asInstanceOf[untpd.Tree]
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
      case _ => return visitUnknown(whileTree).asInstanceOf[J.WhileLoop]
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
            afterCondSpace = Space.format(source.substring(adjustedInnerEnd, adjustedParenEnd))
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
    
    // Visit the body
    val body = visitTree(whileTree.body) match {
      case stmt: Statement => JRightPadded.build(stmt)
      case _ => return visitUnknown(whileTree).asInstanceOf[J.WhileLoop]
    }
    
    // Update cursor to end of the while loop
    updateCursor(whileTree.span.end)
    
    new J.WhileLoop(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
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
    // For now, preserve all for loops as Unknown until we can properly handle cursor management
    // This ensures that the original Scala syntax is preserved when printing
    visitUnknown(forTree)
  }
  
  // The following methods are temporarily commented out until we can properly handle cursor management
  // for converting Scala for comprehensions to Java-style loops
  
  /*
  private def visitSimpleForEach(forTree: untpd.ForDo, genFrom: untpd.GenFrom): J.ForEachLoop = {
    val prefix = extractPrefix(forTree.span)
    
    // Extract the pattern (variable declaration)
    val pattern = genFrom.pat
    val varName = pattern match {
      case ident: untpd.Ident => ident.name.toString
      case _ => 
        // For now, only handle simple identifier patterns
        return visitUnknown(forTree).asInstanceOf[J.ForEachLoop]
    }
    
    // Create variable declaration for the loop variable
    val varDecl = new J.VariableDeclarations(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      Collections.emptyList(), // No leading annotations
      Collections.emptyList(), // No modifiers
      null, // Type will be inferred
      null, // No varargs
      Collections.singletonList(
        JRightPadded.build(
          new J.VariableDeclarations.NamedVariable(
            Tree.randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            new J.Identifier(
              Tree.randomId(),
              Space.EMPTY,
              Markers.EMPTY,
              Collections.emptyList(),
              varName,
              null,
              null
            ),
            Collections.emptyList(), // No dimension brackets
            null, // No initializer in the loop variable
            null // No variable type
          )
        )
      )
    )
    
    // Visit the iterable expression
    val iterable = visitTree(genFrom.expr) match {
      case expr: Expression => expr
      case _ => return visitUnknown(forTree).asInstanceOf[J.ForEachLoop]
    }
    
    // Create the control structure
    val control = new J.ForEachLoop.Control(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      JRightPadded.build(varDecl),
      JRightPadded.build(iterable)
    )
    
    // Visit the body
    // For loops require a statement body, but Scala allows expressions
    // For now, we'll convert the body to a statement or Unknown
    val bodyJ = visitTree(forTree.body)
    val body: Statement = bodyJ match {
      case stmt: Statement => stmt
      case _ => 
        // Wrap non-statement bodies as Unknown to preserve them
        visitUnknown(forTree.body).asInstanceOf[Statement]
    }
    
    // Update cursor to end of for loop
    if (forTree.span.exists) {
      val adjustedEnd = Math.max(0, forTree.span.end - offsetAdjustment)
      if (adjustedEnd > cursor && adjustedEnd <= source.length) {
        cursor = adjustedEnd
      }
    }
    
    new J.ForEachLoop(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      control,
      JRightPadded.build(body)
    )
  }
  
  private def isRangeBasedFor(genFrom: untpd.GenFrom): Boolean = {
    // Check if the expression is a range (e.g., "1 to 10" or "0 until n")
    genFrom.expr match {
      case app: untpd.Apply =>
        app.fun match {
          case sel: untpd.Select =>
            val methodName = sel.name.toString
            // Check for "to" or "until" methods
            methodName == "to" || methodName == "until"
          case _ => false
        }
      case infixOp: untpd.InfixOp =>
        val opName = infixOp.op.name.toString
        // Check for "to" or "until" infix operators
        opName == "to" || opName == "until"
      case _ => false
    }
  }
  
  private def visitRangeBasedFor(forTree: untpd.ForDo, genFrom: untpd.GenFrom): J.ForLoop = {
    val prefix = extractPrefix(forTree.span)
    
    // For now, don't capture original source to avoid cursor issues
    val originalSource = ""
    
    // Extract the loop variable name
    val varName = genFrom.pat match {
      case ident: untpd.Ident => ident.name.toString
      case _ => 
        // For now, only handle simple identifier patterns
        return visitUnknown(forTree).asInstanceOf[J.ForLoop]
    }
    
    // We need to set the cursor correctly before visiting sub-expressions
    // The cursor should be at the start of the generator expression
    if (genFrom.expr.span.exists) {
      val exprStart = Math.max(0, genFrom.expr.span.start - offsetAdjustment)
      if (exprStart >= 0 && exprStart <= source.length) {
        cursor = exprStart
      }
    }
    
    // Extract range information
    val (start, end, isInclusive) = genFrom.expr match {
      case app: untpd.Apply =>
        app.fun match {
          case sel: untpd.Select =>
            val methodName = sel.name.toString
            val startExpr = visitTree(sel.qualifier).asInstanceOf[Expression]
            val endExpr = visitTree(app.args.head).asInstanceOf[Expression]
            (startExpr, endExpr, methodName == "to")
          case _ => 
            return visitUnknown(forTree).asInstanceOf[J.ForLoop]
        }
      case infixOp: untpd.InfixOp =>
        val opName = infixOp.op.name.toString
        val startExpr = visitTree(infixOp.left).asInstanceOf[Expression]
        val endExpr = visitTree(infixOp.right).asInstanceOf[Expression]
        (startExpr, endExpr, opName == "to")
      case _ => 
        return visitUnknown(forTree).asInstanceOf[J.ForLoop]
    }
    
    // Create the initialization: int i = start
    val init = new J.VariableDeclarations(
      Tree.randomId(),
      Space.format(" "), // Add space before "int"
      Markers.EMPTY,
      Collections.emptyList(), // No annotations
      Collections.emptyList(), // No modifiers
      TypeTree.build("int").asInstanceOf[TypeTree].withPrefix(Space.EMPTY), // Explicit int type
      null, // No varargs
      Collections.singletonList(
        JRightPadded.build(
          new J.VariableDeclarations.NamedVariable(
            Tree.randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            new J.Identifier(
              Tree.randomId(),
              Space.format(" "), // Space before variable name
              Markers.EMPTY,
              Collections.emptyList(),
              varName,
              null,
              null
            ),
            Collections.emptyList(), // No dimensions
            new JLeftPadded(Space.format(" "), start.withPrefix(Space.format(" ")), Markers.EMPTY), // Initializer with spaces
            null // No variable type
          )
        )
      )
    )
    
    // Create the condition: i < end or i <= end
    val varRef = new J.Identifier(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      Collections.emptyList(),
      varName,
      null,
      null
    )
    
    val operator = if (isInclusive) J.Binary.Type.LessThanOrEqual else J.Binary.Type.LessThan
    val condition = new J.Binary(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      varRef,
      new JLeftPadded(Space.format(" "), operator, Markers.EMPTY),
      end.withPrefix(Space.format(" ")),
      null
    )
    
    // Create the update: i++ (or i += 1)
    val updateVarRef = new J.Identifier(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      Collections.emptyList(),
      varName,
      null,
      null
    )
    
    val update = new J.Unary(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      JLeftPadded.build(J.Unary.Type.PostIncrement),
      updateVarRef,
      null
    )
    
    // Visit the body
    val bodyJ = visitTree(forTree.body)
    val body: Statement = bodyJ match {
      case stmt: Statement => stmt
      case _ => 
        // Wrap non-statement bodies as Unknown to preserve them
        visitUnknown(forTree.body).asInstanceOf[Statement]
    }
    
    // Update cursor to end of for loop
    if (forTree.span.exists) {
      val adjustedEnd = Math.max(0, forTree.span.end - offsetAdjustment)
      if (adjustedEnd > cursor && adjustedEnd <= source.length) {
        cursor = adjustedEnd
      }
    }
    
    // Create the for loop control
    val control = new J.ForLoop.Control(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      Collections.singletonList(JRightPadded.build(init.asInstanceOf[Statement])),
      JRightPadded.build(condition),
      Collections.singletonList(JRightPadded.build(update.asInstanceOf[Statement]))
    )
    
    val forLoop = new J.ForLoop(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      control,
      JRightPadded.build(body)
    )
    
    // Add marker to preserve original Scala syntax
    if (originalSource.nonEmpty) {
      forLoop.withMarkers(forLoop.getMarkers().addIfAbsent(ScalaForLoop.create(originalSource)))
    } else {
      forLoop
    }
  }
  */
  
  private def visitBlock(block: untpd.Block): J.Block = {
    val prefix = extractPrefix(block.span)
    
    // Move cursor past the opening brace
    val adjustedStart = Math.max(0, block.span.start - offsetAdjustment)
    if (cursor <= adjustedStart && adjustedStart < source.length) {
      val braceIndex = source.indexOf('{', adjustedStart)
      if (braceIndex >= 0 && braceIndex < source.length) {
        cursor = braceIndex + 1
      }
    }
    
    val statements = new util.ArrayList[JRightPadded[Statement]]()
    
    // Visit all statements in the block
    for (i <- block.stats.indices) {
      val stat = block.stats(i)
      visitTree(stat) match {
        case null => // Skip null statements (e.g., package declarations)
        case stmt: Statement => 
          // Extract trailing space after this statement
          val statEnd = Math.max(0, stat.span.end - offsetAdjustment)
          val nextStart = if (i < block.stats.length - 1) {
            Math.max(0, block.stats(i + 1).span.start - offsetAdjustment)
          } else if (!block.expr.isEmpty) {
            Math.max(0, block.expr.span.start - offsetAdjustment)
          } else {
            // Last statement - look for closing brace
            Math.max(0, block.span.end - offsetAdjustment) - 1
          }
          
          var trailingSpace = Space.EMPTY
          if (statEnd < nextStart && cursor <= statEnd) {
            trailingSpace = Space.format(source.substring(statEnd, nextStart))
            cursor = nextStart
          }
          
          statements.add(JRightPadded.build(stmt).withAfter(trailingSpace))
        case _ => // Skip non-statement nodes
      }
    }
    
    // Handle the expression part of the block (if any)
    if (!block.expr.isEmpty) {
      visitTree(block.expr) match {
        case stmt: Statement => 
          // Extract space before closing brace
          val exprEnd = Math.max(0, block.expr.span.end - offsetAdjustment)
          val blockEnd = Math.max(0, block.span.end - offsetAdjustment)
          var endSpace = Space.EMPTY
          if (exprEnd < blockEnd && cursor <= exprEnd) {
            val remaining = source.substring(exprEnd, blockEnd)
            val braceIndex = remaining.lastIndexOf('}')
            if (braceIndex > 0) {
              endSpace = Space.format(remaining.substring(0, braceIndex))
            }
          }
          statements.add(JRightPadded.build(stmt).withAfter(endSpace))
        case _ => // Skip
      }
    }
    
    // Extract end padding before closing brace
    val blockEnd = Math.max(0, block.span.end - offsetAdjustment)
    var endPadding = Space.EMPTY
    if (cursor < blockEnd && statements.isEmpty()) {
      // Empty block - extract space between braces
      val remaining = source.substring(cursor, blockEnd)
      val braceIndex = remaining.lastIndexOf('}')
      if (braceIndex > 0) {
        endPadding = Space.format(remaining.substring(0, braceIndex))
      }
    }
    
    // Update cursor to end of the block
    updateCursor(block.span.end)
    
    new J.Block(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      JRightPadded.build(false), // not static
      statements,
      endPadding
    )
  }
  
  private def visitClassDef(td: untpd.TypeDef): J.ClassDeclaration = {
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
    var sourceSnippet = ""
    
    // Use cursor position (after annotations) instead of adjustedStart
    if (cursor >= 0 && adjustedEnd <= source.length && cursor <= adjustedEnd) {
      sourceSnippet = source.substring(cursor, adjustedEnd)
      classIndex = sourceSnippet.indexOf("class")
      if (classIndex < 0) {
        classIndex = sourceSnippet.indexOf("trait")
        if (classIndex >= 0) {
          isTrait = true
        }
      }
      if (classIndex > 0) {
        modifierText = sourceSnippet.substring(0, classIndex)
      }
    }
    
    // Extract modifiers
    val (modifiers, lastModEnd) = extractModifiersFromText(td.mods, modifierText)
    
    // Check for case modifier (special handling as it's not a traditional modifier)
    if (modifierText.contains("case")) {
      val caseIndex = modifierText.indexOf("case")
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
      }
    }
    
    // Find where "class" or "trait" keyword ends
    val keywordLength = if (isTrait) "trait".length else "class".length
    val classKeywordPos = if (classIndex >= 0) {
      cursor + classIndex + keywordLength
    } else {
      cursor
    }
    
    // Extract space between "class" and the name
    val nameStart = if (td.nameSpan.exists) {
      Math.max(0, td.nameSpan.start - offsetAdjustment)
    } else {
      classKeywordPos
    }
    
    val nameSpace = if (classKeywordPos < nameStart && nameStart <= source.length) {
      Space.format(source.substring(classKeywordPos, nameStart))
    } else {
      Space.format(" ") // Default to single space
    }
    
    // Extract class kind with proper prefix space
    val kindPrefix = if (hasAnnotations && classIndex >= 0) {
      // When we have annotations, the space between the last annotation and "class" goes here
      Space.format(sourceSnippet.substring(0, classIndex))
    } else if (!modifiers.isEmpty && classIndex > 0) {
      val afterModifiers = if (modifierText.contains("case")) {
        val caseIndex = modifierText.indexOf("case")
        if (caseIndex >= 0) {
          caseIndex + "case".length
        } else {
          lastModEnd
        }
      } else {
        lastModEnd
      }
      if (afterModifiers < classIndex) {
        Space.format(modifierText.substring(afterModifiers, classIndex))
      } else {
        Space.EMPTY
      }
    } else {
      Space.EMPTY
    }
    
    val kindType = if (isTrait) {
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
    
    // Update cursor to after "class" keyword
    cursor = classKeywordPos
    
    // Extract class name
    val name = new J.Identifier(
      Tree.randomId(),
      nameSpace,
      Markers.EMPTY,
      Collections.emptyList(),
      td.name.toString,
      null,
      null
    )
    
    // Update cursor to after name
    if (td.nameSpan.exists) {
      val nameEnd = Math.max(0, td.nameSpan.end - offsetAdjustment)
      if (nameEnd > cursor && nameEnd <= source.length) {
        cursor = nameEnd
      }
    }
    
    // Extract template early to access type parameters
    val template = td.rhs match {
      case tmpl: untpd.Template => tmpl
      case _ => null
    }
    
    // Extract type parameters from the template  
    val typeParameters: JContainer[J.TypeParameter] = if (template != null && template.constr.paramss.nonEmpty) {
      // Check if the first param list contains type parameters (TypeDef nodes)
      val firstParamList = template.constr.paramss.head
      val typeParams = firstParamList.collect { case tparam: untpd.TypeDef => tparam }
      
      if (typeParams.nonEmpty) {
        // Look for opening bracket in source
        var bracketStart = cursor
        if (cursor < source.length) {
          val searchEnd = Math.min(cursor + 100, source.length)
          val searchText = source.substring(cursor, searchEnd)
          val bracketIdx = searchText.indexOf('[')
          if (bracketIdx >= 0) {
            bracketStart = cursor + bracketIdx
          }
        }
        
        val openingBracketSpace = if (bracketStart > cursor) {
          Space.format(source.substring(cursor, bracketStart))
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
          
          // Determine trailing space/comma
          val trailingSpace = if (!isLast) {
            // Look for comma in source between this param and next
            if (idx + 1 < typeParams.size && tparam.span.exists && typeParams(idx + 1).span.exists) {
              val thisEnd = tparam.span.end - offsetAdjustment
              val nextStart = typeParams(idx + 1).span.start - offsetAdjustment
              if (thisEnd < nextStart && nextStart <= source.length) {
                val between = source.substring(thisEnd, nextStart)
                val commaIdx = between.indexOf(',')
                if (commaIdx >= 0) {
                  Space.format(between.substring(commaIdx + 1))
                } else {
                  Space.EMPTY
                }
              } else {
                Space.EMPTY
              }
            } else {
              Space.EMPTY
            }
          } else {
            Space.EMPTY
          }
          
          if (!isLast && trailingSpace != Space.EMPTY) {
            jTypeParams.add(new JRightPadded(jTypeParam, trailingSpace, Markers.EMPTY))
          } else {
            jTypeParams.add(JRightPadded.build(jTypeParam))
          }
        }
        
        // Update cursor to after closing bracket
        if (typeParams.nonEmpty && typeParams.last.span.exists) {
          val lastParamEnd = typeParams.last.span.end - offsetAdjustment
          if (lastParamEnd < source.length) {
            val searchEnd = Math.min(lastParamEnd + 10, source.length)
            val afterParams = source.substring(lastParamEnd, searchEnd)
            val closeBracketIdx = afterParams.indexOf(']')
            if (closeBracketIdx >= 0) {
              cursor = lastParamEnd + closeBracketIdx + 1
            }
          }
        }
        
        JContainer.build(openingBracketSpace, jTypeParams, Markers.EMPTY)
      } else {
        null
      }
    } else {
      null
    }
    
    // Handle constructor parameters - extract only value parameters
    val constructorParamsSource = if (template != null && template.constr.paramss.size > 1) {
      // If we have type parameters, constructor params are in the second list
      extractConstructorParametersSource(td)
    } else if (template != null && template.constr.paramss.nonEmpty) {
      // Check if the first list has only value parameters
      val firstList = template.constr.paramss.head
      if (firstList.forall(_.isInstanceOf[untpd.ValDef])) {
        extractConstructorParametersSource(td)
      } else {
        ""
      }
    } else {
      ""
    }
    
    val primaryConstructor = if (constructorParamsSource.nonEmpty) {
      // Create Unknown node to preserve constructor parameters
      val unknown = new J.Unknown(
        Tree.randomId(),
        Space.EMPTY,
        Markers.EMPTY,
        new J.Unknown.Source(
          Tree.randomId(),
          Space.EMPTY,
          Markers.EMPTY,
          constructorParamsSource
        )
      )
      // Wrap in a container
      JContainer.build(
        Space.EMPTY,
        Collections.singletonList(JRightPadded.build(unknown.asInstanceOf[Statement])),
        Markers.EMPTY
      )
    } else {
      null
    }
    
    // Extract extends/implements from Template
    var extendings: JLeftPadded[TypeTree] = null
    var implementings: JContainer[TypeTree] = null
    
    if (template != null && template.parents.nonEmpty) {
        // In Scala, the first parent after the primary constructor is the extends clause
        // Additional parents are the with clauses (implements in Java)
        
        // First, we need to find where "extends" keyword starts in the source
        val extendsKeywordPos = if (td.nameSpan.exists && constructorParamsSource.nonEmpty) {
          // After constructor parameters
          cursor
        } else if (td.nameSpan.exists) {
          // After class name (no constructor params)
          Math.max(0, td.nameSpan.end - offsetAdjustment)
        } else {
          cursor
        }
        
        // Look for "extends" keyword in source
        var extendsSpace = Space.EMPTY
        if (extendsKeywordPos < source.length && template.parents.head.span.exists) {
          val firstParentStart = Math.max(0, template.parents.head.span.start - offsetAdjustment)
          if (extendsKeywordPos < firstParentStart && firstParentStart <= source.length) {
            val betweenText = source.substring(extendsKeywordPos, firstParentStart)
            val extendsIndex = betweenText.indexOf("extends")
            if (extendsIndex >= 0) {
              extendsSpace = Space.format(betweenText.substring(0, extendsIndex))
              // Update cursor to after "extends" keyword
              cursor = extendsKeywordPos + extendsIndex + "extends".length
            }
          }
        }
        
        // First parent is the extends clause
        val firstParent = template.parents.head
        val extendsTypeExpr = visitTree(firstParent) match {
          case id: J.Identifier =>
            // Simple type like "Animal" - already has the right prefix from visiting
            id
          case fieldAccess: J.FieldAccess =>
            // Qualified type like "com.example.Animal"
            fieldAccess
          case unknown: J.Unknown =>
            // Complex type we can't handle yet (like generics)
            unknown
          case _ =>
            // Fallback to Unknown
            visitUnknown(firstParent)
        }
        
        // Convert to TypeTree
        val extendsType: TypeTree = extendsTypeExpr match {
          case id: J.Identifier =>
            // The identifier already has the correct prefix from visitIdent, just use it as is
            id
          case fieldAccess: J.FieldAccess =>
            // The field access already has the correct prefix from visitSelect
            fieldAccess
          case unknown: J.Unknown =>
            // The unknown already has the correct prefix
            unknown
          case other =>
            // This shouldn't happen but let's be safe
            val typeSpace = if (cursor < firstParent.span.start - offsetAdjustment) {
              Space.format(source.substring(cursor, firstParent.span.start - offsetAdjustment))
            } else {
              Space.format(" ")
            }
            new J.Unknown(
              Tree.randomId(),
              typeSpace,
              Markers.EMPTY,
              new J.Unknown.Source(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                other.toString
              )
            )
        }
        
        extendings = new JLeftPadded(extendsSpace, extendsType, Markers.EMPTY)
        
        // Update cursor to after first parent
        if (firstParent.span.exists) {
          cursor = Math.max(cursor, firstParent.span.end - offsetAdjustment)
        }
        
        // Handle additional parents as implements (with clauses)
        if (template.parents.size > 1) {
          val implementsList = new util.ArrayList[JRightPadded[TypeTree]]()
          
          // Extract space before the first "with" or "extends" (if no extends clause)
          var containerSpace = Space.EMPTY
          if (extendings == null && template.parents.nonEmpty) {
            // No extends clause, so first trait uses "extends"
            val firstParent = template.parents.head
            if (firstParent.span.exists) {
              containerSpace = sourceBefore("extends")
            }
          } else if (extendings != null && template.parents.size > 1) {
            // We have extends, so look for first "with"
            containerSpace = sourceBefore("with")
          }
          
          for (i <- 1 until template.parents.size) {
            val parent = template.parents(i)
            
            val implTypeExpr = visitTree(parent) match {
              case id: J.Identifier =>
                id
              case fieldAccess: J.FieldAccess =>
                fieldAccess
              case unknown: J.Unknown =>
                unknown
              case _ =>
                visitUnknown(parent)
            }
            
            // Convert to TypeTree - the expression already has its prefix from visiting
            val implType: TypeTree = implTypeExpr match {
              case id: J.Identifier =>
                id
              case fieldAccess: J.FieldAccess =>
                fieldAccess
              case unknown: J.Unknown =>
                unknown
              case other =>
                new J.Unknown(
                  Tree.randomId(),
                  Space.EMPTY,
                  Markers.EMPTY,
                  new J.Unknown.Source(
                    Tree.randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    other.toString
                  )
                )
            }
            
            // Build the right-padded element
            val rightPadded = if (i < template.parents.size - 1) {
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
      case tmpl: untpd.Template =>
        // A class has an explicit body if:
        // 1. The template has any body statements, OR
        // 2. There's a "{" in the source (even for empty bodies)
        if (tmpl.body.nonEmpty) {
          // If there are body statements, we definitely have a body
          true
        } else if (td.span.exists) {
          // For empty bodies, check if there's a "{" in the entire class span
          val classStart = Math.max(0, td.span.start - offsetAdjustment)
          val classEnd = Math.max(0, td.span.end - offsetAdjustment)
          if (classStart < classEnd && classEnd <= source.length) {
            val classSource = source.substring(classStart, classEnd)
            classSource.contains("{")
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
        case template: untpd.Template =>
          // Extract space before the opening brace
          val bodyPrefix = if (td.span.exists) {
            val classEnd = Math.max(0, td.span.end - offsetAdjustment)
            if (cursor < classEnd && classEnd <= source.length) {
              val afterCursor = source.substring(cursor, classEnd)
              val braceIndex = afterCursor.indexOf("{")
              if (braceIndex >= 0) {
                val prefix = Space.format(afterCursor.substring(0, braceIndex))
                // Update cursor to after the opening brace
                cursor = cursor + braceIndex + 1
                prefix
              } else {
                // The brace might already be consumed, look for it from class start
                val classStart = Math.max(0, td.span.start - offsetAdjustment)
                val classSource = source.substring(classStart, classEnd)
                val nameEnd = classSource.indexOf(td.name.toString) + td.name.toString.length
                val afterName = classSource.substring(nameEnd)
                val braceInAfterName = afterName.indexOf("{")
                if (braceInAfterName >= 0) {
                  // Found the brace, update cursor to after it
                  val bracePos = classStart + nameEnd + braceInAfterName + 1
                  if (bracePos > cursor) {
                    val prefix = Space.format(source.substring(cursor, bracePos - 1))
                    cursor = bracePos
                    prefix
                  } else {
                    // Brace is before cursor, just use single space
                    Space.format(" ")
                  }
                } else {
                  Space.format(" ")
                }
              }
            } else {
              Space.EMPTY
            }
          } else {
            Space.EMPTY
          }
          
          // Visit the template body to get statements
          val statements = new util.ArrayList[JRightPadded[Statement]]()
          
          // Visit each statement in the template body
          for (stat <- template.body) {
            // Check if this is a method declaration (DefDef) - these should always be included
            stat match {
              case _: untpd.DefDef =>
                // Always include method declarations, even if marked synthetic
                visitTree(stat) match {
                  case null => // Skip null statements
                  case stmt: Statement => 
                    statements.add(JRightPadded.build(stmt))
                  case _ => // Skip non-statement nodes
                }
              case _ =>
                // For non-methods, skip synthetic nodes (like the ??? in abstract classes)
                if (!stat.span.isSynthetic) {
                  visitTree(stat) match {
                    case null => // Skip null statements
                    case stmt: Statement => 
                      statements.add(JRightPadded.build(stmt))
                    case _ => // Skip non-statement nodes
                  }
                }
            }
          }
          
          // Extract the space before the closing brace
          val endSpace = if (td.span.exists) {
            val classEnd = Math.max(0, td.span.end - offsetAdjustment)
            if (cursor < classEnd && classEnd <= source.length) {
              val remaining = source.substring(cursor, classEnd)
              val closeBraceIndex = remaining.lastIndexOf("}")
              if (closeBraceIndex >= 0) {
                cursor = classEnd // Move cursor to end
                Space.format(remaining.substring(0, closeBraceIndex))
              } else {
                Space.EMPTY
              }
            } else {
              Space.EMPTY
            }
          } else {
            Space.EMPTY
          }
          
          new J.Block(
            Tree.randomId(),
            bodyPrefix,
            Markers.EMPTY,
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
    
    new J.ClassDeclaration(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
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
      null  // type
    )
  }
  
  private def visitReturn(ret: untpd.Return): J.Return = {
    val prefix = extractPrefix(ret.span)
    
    // Extract the expression being returned (if any)
    val expr = if (ret.expr.isEmpty) {
      null // void return
    } else {
      visitTree(ret.expr) match {
        case expression: Expression => expression
        case _ => return visitUnknown(ret).asInstanceOf[J.Return]
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

  private def visitThrow(thr: untpd.Throw): J.Throw = {
    val prefix = extractPrefix(thr.span)
    
    // Visit the exception expression
    val exception = visitTree(thr.expr) match {
      case expr: Expression => expr
      case _ => return visitUnknown(thr).asInstanceOf[J.Throw]
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
  
  private def visitTypeApply(ta: untpd.TypeApply): J = {
    // TypeApply represents a type application like List.empty[Int] or obj.asInstanceOf[Type]
    ta.fun match {
      case sel: untpd.Select =>
        // Check if this is asInstanceOf
        if (sel.name.toString == "asInstanceOf" && ta.args.size == 1) {
          // This is a type cast operation: obj.asInstanceOf[Type]
          
          // For TypeCast, we need to extract prefix carefully
          // The prefix should be any whitespace before the entire expression
          val startPos = Math.max(0, ta.span.start - offsetAdjustment)
          val prefix = if (startPos > cursor && startPos <= source.length) {
            Space.format(source.substring(cursor, startPos))
          } else {
            Space.EMPTY
          }
          
          // Update cursor to start of the expression (sel.qualifier)
          cursor = Math.max(0, sel.qualifier.span.start - offsetAdjustment)
          
          // Visit the expression being cast
          val expr = visitTree(sel.qualifier) match {
            case e: Expression => e
            case _ => return visitUnknown(ta)
          }
          
          // Update cursor to start of type argument
          cursor = Math.max(0, ta.args.head.span.start - offsetAdjustment)
          
          // Visit the target type
          val targetType = visitTree(ta.args.head) match {
            case tt: TypeTree => tt
            case _ => return visitUnknown(ta)
          }
          
          // Update cursor to the end of the TypeApply to consume the entire expression
          updateCursor(ta.span.end)
          
          return new J.TypeCast(
            Tree.randomId(),
            prefix,
            Markers.EMPTY,
            new J.ControlParentheses[TypeTree](
              Tree.randomId(),
              Space.EMPTY,
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
            Space.format(source.substring(cursor, startPos))
          } else {
            Space.EMPTY
          }
          
          // Update cursor to start of the expression (sel.qualifier)
          cursor = Math.max(0, sel.qualifier.span.start - offsetAdjustment)
          
          // Visit the expression being checked
          val expr = visitTree(sel.qualifier) match {
            case e: Expression => e
            case _ => return visitUnknown(ta)
          }
          
          // Update cursor to start of type argument
          cursor = Math.max(0, ta.args.head.span.start - offsetAdjustment)
          
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
            JRightPadded.build(expr),
            clazz,
            null, // pattern (not used in Scala)
            null  // type
          )
        }
        
      case _ =>
        // Other TypeApply cases
    }
    
    // For other TypeApply cases, preserve as Unknown
    visitUnknown(ta)
  }
  
  private def visitAppliedTypeTree(at: untpd.AppliedTypeTree): J = {
    // AppliedTypeTree represents a parameterized type like List[String]
    val prefix = extractPrefix(at.span)
    
    // Save original cursor position
    val originalCursor = cursor
    
    // Visit the base type (e.g., List, Map, Option)
    val clazz = visitTree(at.tpt) match {
      case nt: NameTree => nt
      case _ => return visitUnknown(at)
    }
    
    // Extract the source to find bracket positions
    val source = extractSource(at.span)
    System.out.println(s"DEBUG visitAppliedTypeTree: full source='$source', cursor before args=$cursor")
    val openBracketIdx = source.indexOf('[')
    val closeBracketIdx = source.lastIndexOf(']')
    
    if (openBracketIdx < 0 || closeBracketIdx < 0) {
      return visitUnknown(at)
    }
    
    // Extract space before opening bracket
    val baseTypeEnd = clazz match {
      case id: J.Identifier => id.getSimpleName.length
      case fa: J.FieldAccess => source.indexOf('[')
      case _ => source.indexOf('[')
    }
    
    val beforeOpenBracket = if (baseTypeEnd < openBracketIdx) {
      Space.format(source.substring(baseTypeEnd, openBracketIdx))
    } else {
      Space.EMPTY
    }
    
    // Process type arguments
    val typeArgs = new util.ArrayList[JRightPadded[Expression]]()
    
    if (at.args.nonEmpty) {
      // Update cursor to the start of the first argument
      val firstArgStart = Math.max(0, at.args.head.span.start - offsetAdjustment)
      System.out.println(s"DEBUG: Moving cursor from $cursor to $firstArgStart for first arg")
      cursor = firstArgStart
      
      for (i <- at.args.indices) {
        val arg = at.args(i)
        System.out.println(s"DEBUG: Processing arg $i, cursor=$cursor")
        val argTree = visitTree(arg) match {
          case expr: Expression => expr
          case _ => return visitUnknown(at)
        }
        System.out.println(s"DEBUG: After visiting arg $i, cursor=$cursor, argTree=$argTree")
        
        // Extract trailing comma/space
        val isLast = i == at.args.size - 1
        val afterSpace = if (isLast) {
          // Space before closing bracket
          val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
          if (argEnd < closeBracketIdx + originalCursor) {
            val spaceStr = this.source.substring(argEnd, closeBracketIdx + originalCursor)
            System.out.println(s"DEBUG: Last arg space='$spaceStr'")
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
            closeBracketIdx + originalCursor
          }
          
          if (argEnd < nextArgStart && argEnd < this.source.length && nextArgStart <= this.source.length) {
            val between = this.source.substring(argEnd, nextArgStart)
            val commaIdx = between.indexOf(',')
            if (commaIdx >= 0 && commaIdx + 1 < between.length) {
              cursor = argEnd + commaIdx + 1
              Space.format(between.substring(commaIdx + 1))
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
    
    new J.ParameterizedType(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      clazz,
      typeParameters,
      null // type
    )
  }

  private def visitDefDef(dd: untpd.DefDef): J = {
    // For now, preserve method declarations as Unknown to maintain exact formatting
    // The implementation is complex due to:
    // 1. Scala's 'def' keyword vs Java's method declaration syntax
    // 2. The '=' syntax for method bodies
    // 3. Single-expression methods without braces
    // 4. Proper space handling between return type and '='
    visitUnknown(dd)
  }
  
  private def visitDefDefFull(dd: untpd.DefDef): J.MethodDeclaration = {
    val prefix = extractPrefix(dd.span)
    
    // Extract modifiers
    val adjustedStart = Math.max(0, dd.span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, dd.span.end - offsetAdjustment)
    var modifierText = ""
    var defIndex = -1
    
    if (adjustedStart >= cursor && adjustedEnd <= source.length) {
      val sourceSnippet = source.substring(cursor, adjustedEnd)
      defIndex = sourceSnippet.indexOf("def")
      if (defIndex > 0) {
        modifierText = sourceSnippet.substring(0, defIndex)
      }
    }
    
    val (modifiers, lastModEnd) = extractModifiersFromText(dd.mods, modifierText)
    
    // Update cursor to after "def" keyword
    val defKeywordPos = if (defIndex >= 0) {
      cursor + defIndex + "def".length
    } else {
      cursor
    }
    cursor = defKeywordPos
    
    // Extract method name
    val nameStart = if (dd.nameSpan.exists) {
      Math.max(0, dd.nameSpan.start - offsetAdjustment)
    } else {
      defKeywordPos
    }
    
    val nameSpace = if (defKeywordPos < nameStart && nameStart <= source.length) {
      Space.format(source.substring(defKeywordPos, nameStart))
    } else {
      Space.format(" ")
    }
    
    val name = new J.Identifier(
      Tree.randomId(),
      nameSpace,
      Markers.EMPTY,
      Collections.emptyList(),
      dd.name.toString,
      null,
      null
    )
    
    // Update cursor to after name
    if (dd.nameSpan.exists) {
      cursor = Math.max(cursor, dd.nameSpan.end - offsetAdjustment)
    }
    
    // Handle type parameters
    val typeParameters: JContainer[J.TypeParameter] = if (dd.paramss.nonEmpty) {
      // Check if first param list is type parameters
      val firstParamList = dd.paramss.head
      val typeParams = firstParamList.collect { case tparam: untpd.TypeDef => tparam }
      
      if (typeParams.nonEmpty) {
        // Look for opening bracket
        var bracketStart = cursor
        if (cursor < source.length) {
          val searchEnd = Math.min(cursor + 50, source.length)
          val searchText = source.substring(cursor, searchEnd)
          val bracketIdx = searchText.indexOf('[')
          if (bracketIdx >= 0) {
            bracketStart = cursor + bracketIdx
          }
        }
        
        val openingBracketSpace = if (bracketStart > cursor) {
          Space.format(source.substring(cursor, bracketStart))
        } else {
          Space.EMPTY
        }
        
        cursor = bracketStart + 1
        
        val jTypeParams = new util.ArrayList[JRightPadded[J.TypeParameter]]()
        typeParams.zipWithIndex.foreach { case (tparam, idx) =>
          val jTypeParam = visitTypeParameter(tparam)
          val isLast = idx == typeParams.size - 1
          jTypeParams.add(JRightPadded.build(jTypeParam))
        }
        
        // Update cursor past closing bracket
        if (cursor < source.length) {
          val searchEnd = Math.min(cursor + 100, source.length)
          val afterParams = source.substring(cursor, searchEnd)
          val closeBracketIdx = afterParams.indexOf(']')
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
    
    // Handle value parameters
    val parameters: JContainer[Statement] = {
      // For now, create empty parameter container
      // TODO: Implement proper parameter handling with J.VariableDeclarations
      JContainer.empty[Statement]()
    }
    
    // Handle return type
    val returnTypeExpression: TypeTree = dd.tpt match {
      case untpd.EmptyTree => null
      case tpt if tpt.span.exists =>
        // Look for colon before return type
        val tptStart = Math.max(0, tpt.span.start - offsetAdjustment)
        if (cursor < tptStart && tptStart <= source.length) {
          val beforeType = source.substring(cursor, tptStart)
          val colonIdx = beforeType.indexOf(':')
          if (colonIdx >= 0) {
            cursor = cursor + colonIdx + 1
          }
        }
        
        visitTree(tpt) match {
          case tt: TypeTree => tt
          case _ => null
        }
      case _ => null
    }
    
    // Handle method body
    val body: J.Block = dd.rhs match {
      case untpd.EmptyTree => null // Abstract method
      case rhs if rhs.span.exists =>
        // Look for equals sign before body
        val rhsStart = Math.max(0, rhs.span.start - offsetAdjustment)
        if (cursor < rhsStart && rhsStart <= source.length) {
          val beforeBody = source.substring(cursor, rhsStart)
          val equalsIdx = beforeBody.indexOf('=')
          if (equalsIdx >= 0) {
            cursor = cursor + equalsIdx + 1
          }
        }
        
        visitTree(rhs) match {
          case block: J.Block => block
          case expr: Expression =>
            // Wrap single expression in block
            val statements = new util.ArrayList[JRightPadded[Statement]]()
            statements.add(JRightPadded.build(expr.asInstanceOf[Statement]))
            new J.Block(
              Tree.randomId(),
              Space.EMPTY,
              Markers.EMPTY,
              JRightPadded.build(false),
              statements,
              Space.EMPTY
            )
          case _ => null
        }
      case _ => null
    }
    
    // Update cursor to end of method
    updateCursor(dd.span.end)
    
    new J.MethodDeclaration(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      Collections.emptyList(), // leadingAnnotations
      modifiers,
      null, // typeParameters (J.TypeParameters type, not JContainer)
      returnTypeExpression,
      new J.MethodDeclaration.IdentifierWithAnnotations(
        name,
        Collections.emptyList()
      ),
      parameters,
      null, // throws
      body,
      null, // defaultValue
      null  // methodType
    )
  }
  
  private def visitUnknown(tree: untpd.Tree): J.Unknown = {
    val prefix = extractPrefix(tree.span)
    val sourceText = extractSource(tree.span)
    
    // Debug: Check if this is a New node
    if (tree.isInstanceOf[untpd.New]) {
      System.out.println(s"DEBUG visitUnknown for New: sourceText='$sourceText', tree=$tree, span=${tree.span}")
    }
    
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
  
  def extractPrefix(span: Spans.Span): Space = {
    if (!span.exists) {
      return Space.EMPTY
    }
    
    val start = cursor
    val adjustedTreeStart = Math.max(0, span.start - offsetAdjustment)
    
    if (adjustedTreeStart > cursor && adjustedTreeStart <= source.length) {
      cursor = adjustedTreeStart
      // Use Space.format to properly extract comments from whitespace
      Space.format(source.substring(start, adjustedTreeStart))
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
   * Extract whitespace and comments before the next occurrence of a delimiter.
   * Similar to sourceBefore in ReloadableJava17Parser.
   */
  private def sourceBefore(untilDelim: String): Space = {
    val delimIndex = source.indexOf(untilDelim, cursor)
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
   * Find the position of the next occurrence of a delimiter.
   */
  private def positionOfNext(delimiter: String, startFrom: Int = cursor): Int = {
    val pos = source.indexOf(delimiter, startFrom)
    if (pos >= 0) pos else -1
  }
  
  /**
   * Skip whitespace and return the position of the next non-whitespace character.
   */
  private def indexOfNextNonWhitespace(startFrom: Int = cursor): Int = {
    var i = startFrom
    while (i < source.length && Character.isWhitespace(source.charAt(i))) {
      i += 1
    }
    i
  }
  
  private def extractModifiersFromText(mods: untpd.Modifiers, modifierText: String): (util.ArrayList[J.Modifier], Int) = {
    import dotty.tools.dotc.core.Flags
    val modifierList = new util.ArrayList[J.Modifier]()
    
    // The order matters - we'll add them in the order they appear in source
    val modifierKeywords = List(
      ("private", Flags.Private, J.Modifier.Type.Private),
      ("protected", Flags.Protected, J.Modifier.Type.Protected),
      ("abstract", Flags.Abstract, J.Modifier.Type.Abstract),
      ("final", Flags.Final, J.Modifier.Type.Final)
      // Skip "case" for now - needs special handling
    )
    
    // Create a list of (position, keyword, type) for modifiers that are present
    val presentModifiers = modifierKeywords.flatMap { case (keyword, flag, modType) =>
      if (mods.is(flag)) {
        val pos = modifierText.indexOf(keyword)
        if (pos >= 0) Some((pos, keyword, modType)) else None
      } else None
    }.sortBy(_._1) // Sort by position in source
    
    // Build modifiers with proper spacing
    var lastEnd = 0
    for ((pos, keyword, modType) <- presentModifiers) {
      // Space before this modifier
      val spaceBefore = if (pos > lastEnd) {
        Space.format(modifierText.substring(lastEnd, pos))
      } else {
        Space.EMPTY
      }
      
      modifierList.add(new J.Modifier(
        Tree.randomId(),
        spaceBefore,
        Markers.EMPTY,
        keyword,
        modType,
        Collections.emptyList()
      ))
      
      lastEnd = pos + keyword.length
    }
    
    // Update cursor to skip past the modifiers we've consumed
    if (!modifierList.isEmpty && modifierText.nonEmpty) {
      cursor = cursor + lastEnd
    }
    
    (modifierList, lastEnd)
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
  
  private def visitTypeParameter(tparam: untpd.TypeDef): J.TypeParameter = {
    val prefix = extractPrefix(tparam.span)
    
    // Check for variance annotation in the source
    val adjustedStart = Math.max(0, tparam.span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, tparam.span.end - offsetAdjustment)
    var varianceSpace = Space.EMPTY
    var nameStr = tparam.name.toString
    
    if (adjustedStart < adjustedEnd && adjustedStart >= cursor && adjustedEnd <= source.length) {
      val paramSource = source.substring(adjustedStart, adjustedEnd)
      // Check if it starts with + or -
      if (paramSource.startsWith("+") || paramSource.startsWith("-")) {
        // Include the variance annotation in the name
        val variance = paramSource.charAt(0)
        nameStr = variance + tparam.name.toString
        cursor = adjustedStart + 1 // Skip past the variance symbol
      }
    }
    
    // Extract the type parameter name
    val name = new J.Identifier(
      Tree.randomId(),
      varianceSpace,
      Markers.EMPTY,
      Collections.emptyList(),
      nameStr,
      null,
      null
    )
    
    // Handle bounds if present
    val bounds: JContainer[TypeTree] = tparam.rhs match {
      case bounds: untpd.TypeBoundsTree if !bounds.lo.isEmpty || !bounds.hi.isEmpty =>
        // TODO: Implement bounds properly
        null
      case _ => null
    }
    
    new J.TypeParameter(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      Collections.emptyList(), // annotations
      Collections.emptyList(), // modifiers
      name,
      bounds
    )
  }
  
  private def extractTypeParametersSource(td: untpd.TypeDef): String = {
    // This method is not actually used anymore since we get type params from the AST
    // We only need to update the cursor position correctly
    ""
  }
  
  private def extractConstructorParametersSource(td: untpd.TypeDef): String = {
    // Extract constructor parameters from source
    if (td.span.exists && td.nameSpan.exists) {
      // First check if we have type parameters and skip past them
      var searchStart = Math.max(0, td.nameSpan.end - offsetAdjustment)
      
      // Skip type parameters if present
      if (searchStart < source.length && source.charAt(searchStart) == '[') {
        var depth = 1
        var i = searchStart + 1
        while (i < source.length && depth > 0) {
          source.charAt(i) match {
            case '[' => depth += 1
            case ']' => depth -= 1
            case _ =>
          }
          i += 1
        }
        if (depth == 0) {
          searchStart = i // Start looking for constructor params after type params
        }
      }
      
      val classEnd = Math.max(0, td.span.end - offsetAdjustment)
      
      if (searchStart < classEnd && searchStart >= 0 && classEnd <= source.length) {
        val afterNameAndTypeParams = source.substring(searchStart, classEnd)
        
        // Look for opening parenthesis after class name and type parameters
        // Check if it starts with parenthesis (possibly with whitespace)
        val trimmed = afterNameAndTypeParams.trim()
        if (trimmed.startsWith("(")) {
          // Find the position of the opening parenthesis
          val parenStart = afterNameAndTypeParams.indexOf("(")
          
          // Find matching closing parenthesis
          var depth = 1
          var i = parenStart + 1
          while (i < afterNameAndTypeParams.length && depth > 0) {
            afterNameAndTypeParams(i) match {
              case '(' => depth += 1
              case ')' => depth -= 1
              case _ =>
            }
            i += 1
          }
          
          if (depth == 0) {
            // Extract the parameters including parentheses
            val params = afterNameAndTypeParams.substring(parenStart, i)
            // Update cursor to after the parameters
            cursor = searchStart + i
            return params
          }
        }
      }
    }
    ""
  }
  
  private def createPrimaryConstructor(constructorParams: List[untpd.ValDef], template: untpd.Template): J.MethodDeclaration = {
    // Create method name with Implicit marker (similar to Kotlin)
    val name = new J.Identifier(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY, // TODO: Add Scala implicit marker
      Collections.emptyList(),
      "<constructor>",
      null,
      null
    )
    
    // Visit constructor parameters
    val params = new util.ArrayList[JRightPadded[Statement]]()
    for (param <- constructorParams) {
      // For now, preserve constructor parameters as Unknown
      val paramTree = visitUnknown(param)
      params.add(JRightPadded.build(paramTree.asInstanceOf[Statement]))
    }
    
    // Build parameter container
    val paramContainer = if (params.isEmpty) {
      JContainer.empty[Statement]()
    } else {
      JContainer.build(
        Space.EMPTY,
        params,
        Markers.EMPTY
      )
    }
    
    new J.MethodDeclaration(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY, // TODO: Add Scala PrimaryConstructor marker
      Collections.emptyList(), // annotations
      Collections.emptyList(), // modifiers
      null, // type parameters
      null, // return type
      new J.MethodDeclaration.IdentifierWithAnnotations(
        name,
        Collections.emptyList()
      ),
      paramContainer,
      null, // throws
      null, // body
      null, // default value
      null  // method type
    )
  }
  
  private def visitFunction(func: untpd.Function): J.Lambda = {
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
    
    // Check if parameters are parenthesized by looking at the source
    val funcSource = extractSource(func.span)
    hasParentheses = funcSource.trim.startsWith("(")
    
    for (i <- func.args.indices) {
      val param = func.args(i)
      val paramTree = visitTree(param) match {
        case vd: J.VariableDeclarations =>
          // Convert VariableDeclarations to a simple parameter
          if (vd.getVariables.size() == 1) {
            val variable = vd.getVariables.get(0)
            new J.VariableDeclarations(
              vd.getId,
              vd.getPrefix,
              vd.getMarkers,
              vd.getLeadingAnnotations,
              vd.getModifiers,
              vd.getTypeExpression,
              null,
              vd.getDimensionsBeforeName,
              util.Arrays.asList(
                JRightPadded.build(variable).withAfter(
                  if (i < func.args.length - 1) Space.format(", ") else Space.EMPTY
                )
              )
            )
          } else {
            vd
          }
        case other => other
      }
      params.add(JRightPadded.build(paramTree))
    }
    
    // Update parameters with the actual params
    val updatedParams = new J.Lambda.Parameters(
      parameters.getId,
      parameters.getPrefix,
      parameters.getMarkers,
      hasParentheses,
      params
    )
    
    // Extract arrow and spacing
    val arrowIndex = funcSource.indexOf("=>")
    var arrowPrefix = Space.EMPTY
    if (arrowIndex > 0) {
      // Find the space before =>
      var spaceStart = arrowIndex - 1
      while (spaceStart >= 0 && Character.isWhitespace(funcSource.charAt(spaceStart))) {
        spaceStart -= 1
      }
      if (spaceStart < arrowIndex - 1) {
        arrowPrefix = Space.format(funcSource.substring(spaceStart + 1, arrowIndex))
      }
      // Move cursor past the arrow
      cursor = Math.max(cursor, func.span.start + arrowIndex + 2 - offsetAdjustment)
    }
    
    // Visit the lambda body
    val body = visitTree(func.body)
    
    new J.Lambda(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      updatedParams,
      arrowPrefix,
      body,
      null // type
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