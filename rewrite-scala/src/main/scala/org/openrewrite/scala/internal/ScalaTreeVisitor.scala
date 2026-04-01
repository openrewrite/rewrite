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
import org.openrewrite.marker.Markers
import org.openrewrite.scala.marker.Implicit
import org.openrewrite.scala.marker.LambdaParameter
import org.openrewrite.scala.marker.IndentedBlock
import org.openrewrite.scala.marker.OmitBraces
import org.openrewrite.scala.marker.SObject
import org.openrewrite.scala.marker.TypeProjection
import org.openrewrite.scala.marker.ScalaForLoop
import org.openrewrite.scala.marker.BlockArgument
import org.openrewrite.scala.marker.UnderscorePlaceholderLambda
import org.openrewrite.scala.tree.S

import java.util
import java.util.{Collections, Arrays, UUID}

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
  
  def updateCursor(position: Int): Unit = {
    val adjustedPosition = Math.max(0, position - offsetAdjustment)
    if (adjustedPosition > cursor && adjustedPosition <= source.length) {
      cursor = adjustedPosition
    }
  }
  
  def visitTree(tree: Trees.Tree[?]): J = {
    tree match {
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
      case pkg: Trees.PackageDef[?] => visitPackageDef(pkg)
      case newTree: Trees.New[?] => visitNew(newTree)
      case vd: Trees.ValDef[?] => visitValDef(vd)
      case md: untpd.ModuleDef => visitModuleDef(md)
      case asg: Trees.Assign[?] => visitAssign(asg)
      case ifTree: Trees.If[?] => visitIf(ifTree)
      case whileTree: Trees.WhileDo[?] => visitWhileDo(whileTree)
      case forTree: untpd.ForDo => visitForDo(forTree)
      case block: Trees.Block[?] => visitBlock(block)
      case td: Trees.TypeDef[?] if td.isClassDef => visitClassDef(td)
      case dd: Trees.DefDef[?] => visitDefDef(dd)
      case ret: Trees.Return[?] => visitReturn(ret)
      case thr: untpd.Throw => visitThrow(thr)
      case ta: Trees.TypeApply[?] => visitTypeApply(ta)
      case at: Trees.AppliedTypeTree[?] => visitAppliedTypeTree(at)
      case func: untpd.Function => visitFunction(func)
      case typed: Trees.Typed[?] => visitTyped(typed)
      case tuple: untpd.Tuple => visitTuple(tuple)
      case tryTree: Trees.Try[?] => visitTryTree(tryTree)
      case matchTree: Trees.Match[?] => visitMatchTree(matchTree)
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
  
  private def visitIdent(id: Trees.Ident[?]): J = {
    val prefix = extractPrefix(id.span)
    val sourceText = extractSource(id.span) // Extract source to move cursor
    var simpleName = id.name.toString
    
    // Special handling for wildcard imports: convert Scala's "_" to Java's "*"
    // This is needed because J.Import expects "*" for wildcard imports
    if (simpleName == "_" && isInImportContext) {
      simpleName = "*"
      new J.Identifier(
        Tree.randomId(),
        prefix,
        Markers.EMPTY,
        Collections.emptyList(),
        simpleName,
        null,
        null
      )
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
      new J.Identifier(
        Tree.randomId(),
        prefix,
        Markers.EMPTY,
        Collections.emptyList(),
        simpleName,
        typeOfTree(id),
        variableTypeOfTree(id)
      )
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
    val isAnnotationPattern = app.fun match {
      case sel: Trees.Select[?] if sel.name.toString == "<init>" =>
        sel.qualifier match {
          case newNode: Trees.New[?] =>
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
      case sel: Trees.Select[?] if app.args.length == 1 && isBinaryOperator(sel.name.toString) =>
        // This is likely a binary operation (infix notation)
        visitBinary(sel, app.args.head, Some(app.span))
      case sel: Trees.Select[?] =>
        // Method call with dot notation like "obj.method(args)"
        visitMethodInvocation(app)
      case id: Trees.Ident[?] =>
        // Function application syntax: func(args)
        // This includes array access (arr(0)), function calls, and more
        visitFunctionApplication(app, id)
      case _ =>
        // Other kinds of applications - for now treat as unknown
        visitUnknown(app)
    }
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
    val (annotationType, args) = app.fun match {
      case sel: Trees.Select[?] if sel.name.toString == "<init>" =>
        sel.qualifier match {
          case newTree: Trees.New[?] =>
            val typeIdent = newTree.tpt match {
              case id: Trees.Ident[?] => id
              case _ => return visitUnknown(app)
            }
            (typeIdent, app.args)
          case _ => return visitUnknown(app)
        }
      case _ => return visitUnknown(app)
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
      val argList = new util.ArrayList[JRightPadded[Expression]]()
      for ((arg, i) <- args.zipWithIndex) {
        val argJ = visitTree(arg)
        val expr = argJ match {
          case e: Expression => e
          case _ => visitUnknown(arg)
        }
        argList.add(JRightPadded.build(expr.asInstanceOf[Expression]).withAfter(Space.EMPTY))
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
  
  private def visitPrefixOp(prefixOp: untpd.PrefixOp): J = {
    val prefix = extractPrefix(prefixOp.span)
    val operator = mapPrefixOperator(prefixOp.op.name.toString)
    
    // Update cursor to the end of the operator
    updateCursor(prefixOp.op.span.end)
    
    // Now visit the expression
    val expr = visitTree(prefixOp.od) match {
      case e: Expression => e
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
          if (odEnd < opStart && odEnd >= 0 && opStart <= source.length) {
            Space.format(source.substring(odEnd, opStart))
          } else {
            Space.SINGLE_SPACE
          }
        } else {
          Space.SINGLE_SPACE
        }
        
        // Create a member reference
        new J.MemberReference(
          Tree.randomId(),
          prefix,
          Markers.EMPTY,
          JRightPadded.build(expr),
          null, // No type parameters for now
          JLeftPadded.build(new J.Identifier(
            Tree.randomId(),
            spaceBeforeUnderscore,
            Markers.EMPTY,
            Collections.emptyList(),
            "_",
            typeFor(postfixOp.span),
            null
          )),
          typeFor(postfixOp.span),
          methodTypeFor(postfixOp.span),
          variableTypeFor(postfixOp.span)
        )
        
      case _ =>
        // Other postfix operators - treat as unary for now
        val prefix = extractPrefix(postfixOp.span)
        
        val expr = visitTree(postfixOp.od) match {
          case e: Expression => e
          case _ => return visitUnknown(postfixOp)
        }
        
        // For postfix operators, we need to determine the operator type
        // Currently only handling as PostDecrement (as a placeholder)
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

    // Detect block argument syntax: `Seq { 1 }` vs parenthesized `Seq(1)`
    val isBlockArg = if (app.args.nonEmpty) {
      val searchEnd = Math.min(cursor + 50, source.length)
      if (cursor < searchEnd) {
        val ahead = source.substring(cursor, searchEnd)
        val firstNonWs = ahead.indexWhere(!_.isWhitespace)
        firstNonWs >= 0 && ahead.charAt(firstNonWs) == '{'
      } else false
    } else false

    val args = new util.ArrayList[JRightPadded[Expression]]()
    val markers = new util.ArrayList[org.openrewrite.marker.Marker]()
    import org.openrewrite.scala.marker.FunctionApplication
    markers.add(FunctionApplication.create())

    if (isBlockArg) {
      // Block argument: Seq { 1 } — mark as block arg and visit args directly
      markers.add(new BlockArgument(UUID.randomUUID()))
      for (arg <- app.args) {
        val argSpace = extractPrefix(arg.span)
        visitTree(arg) match {
          case expr: Expression =>
            args.add(JRightPadded.build(expr))
          case block: J.Block =>
            val blockExpr = new S.BlockExpression(Tree.randomId(), argSpace, Markers.EMPTY,
              block.withPrefix(Space.EMPTY), null)
            args.add(JRightPadded.build(blockExpr.asInstanceOf[Expression]))
          case _ =>
        }
      }
    } else {
      // Normal parenthesized arguments: Seq(1, 2)
      val parenPos = positionOfNext("(")
      if (parenPos >= 0) {
        cursor = parenPos + 1
      }

      for ((arg, i) <- app.args.zipWithIndex) {
        visitTree(arg) match {
          case expr: Expression =>
            val isLast = i == app.args.length - 1
            if (!isLast) {
              val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
              val commaPos = source.indexOf(',', Math.max(cursor, argEnd))
              val afterComma = if (commaPos >= 0 && commaPos + 1 < source.length) {
                cursor = commaPos + 1
                Space.EMPTY
              } else Space.EMPTY
              args.add(JRightPadded.build(expr).withAfter(afterComma))
            } else {
              val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
              val closePos = source.indexOf(')', Math.max(cursor, argEnd))
              val beforeClose = if (closePos > argEnd) {
                Space.format(source.substring(argEnd, closePos))
              } else Space.EMPTY
              args.add(new JRightPadded(expr, beforeClose, Markers.EMPTY))
            }
          case _ =>
        }
      }

      // Skip past the closing parenthesis
      val closeParenPos = positionOfNext(")")
      if (closeParenPos >= 0) {
        cursor = closeParenPos + 1
      }
    }

    val methodName = new J.Identifier(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      Collections.emptyList(),
      "apply",
      null,
      null
    )

    new J.MethodInvocation(
      Tree.randomId(),
      prefix,
      Markers.build(markers),
      JRightPadded.build(select),
      null, // typeParameters
      methodName,
      JContainer.build(Space.EMPTY, args, Markers.EMPTY),
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
          case _ =>
            // Continue with regular method invocation (including explicit .apply() calls)
        }
      case ta: Trees.TypeApply[?] =>
        // Handle type applications like Array[String]("hello", "world")
        ta.fun match {
          case id: Trees.Ident[?] if id.name.toString == "Array" =>
            // This is typed array creation: Array[String](...) 
            return visitNewArrayWithType(app, ta)
          case _ =>
            // Continue with regular method invocation
        }
      case _ =>
        // Continue with regular method invocation
    }
    
    // Handle the method call target
    val (select: Expression, selectAfterSpace: Space, methodName: String, typeParams: java.util.List[Expression]) = app.fun match {
      case sel: Trees.Select[?] =>
        val target = visitTree(sel.qualifier) match {
          case expr: Expression => expr
          case _ => cursor = savedCursor; return visitUnknown(app)
        }

        // Capture space between qualifier and the `.` (for multi-line chains)
        val dotPos = source.indexOf('.', cursor)
        val selectAfter = if (dotPos > cursor) {
          Space.format(source.substring(cursor, dotPos))
        } else Space.EMPTY
        if (dotPos >= 0) cursor = dotPos + 1

        // Update cursor position to after the method name
        if (sel.nameSpan.exists) {
          val nameEnd = Math.max(0, sel.nameSpan.end - offsetAdjustment)
          if (nameEnd > cursor) {
            cursor = nameEnd
          }
        }

        (target, selectAfter, sel.name.toString, Collections.emptyList[Expression]())

      case id: Trees.Ident[?] =>
        (null, Space.EMPTY, id.name.toString, Collections.emptyList[Expression]())

      case typeApp: Trees.TypeApply[?] =>
        cursor = savedCursor; return visitUnknown(app)

      case _ =>
        // Other kinds of function applications
        cursor = savedCursor; return visitUnknown(app)
    }
    
    // Determine if this is a block argument call (no parentheses):
    //   list.foreach { x => println(x) }
    // vs a normal parenthesized call:
    //   list.foreach(x => println(x))
    val isBlockArg = if (app.args.nonEmpty) {
      // Look ahead in source for the next non-whitespace char after the method name
      val searchStart = cursor
      val searchEnd = Math.min(cursor + 50, source.length)
      if (searchStart < searchEnd) {
        val ahead = source.substring(searchStart, searchEnd)
        val firstNonWs = ahead.indexWhere(!_.isWhitespace)
        firstNonWs >= 0 && ahead.charAt(firstNonWs) == '{'
      } else false
    } else false

    val argContainerPrefix = Space.EMPTY
    val args = new util.ArrayList[JRightPadded[Expression]]()

    if (isBlockArg) {
      // Block argument: list.foreach { x => ... }
      // The argument is the block/lambda - visit it directly
      for (arg <- app.args) {
        visitTree(arg) match {
          case expr: Expression =>
            args.add(JRightPadded.build(expr))
          case _ => cursor = savedCursor; return visitUnknown(app)
        }
      }
    } else {
      // Normal parenthesized arguments: list.foreach(x => ...)
      if (app.args.nonEmpty) {
        val parenPos = positionOfNext("(")
        if (parenPos >= 0) {
          cursor = parenPos + 1
        }
      }

      for (i <- app.args.indices) {
        val arg = app.args(i)

        if (i > 0) {
          // For non-first args, advance cursor past the comma separator
          val prevEnd = Math.max(0, app.args(i - 1).span.end - offsetAdjustment)
          val thisStart = Math.max(0, arg.span.start - offsetAdjustment)
          if (prevEnd < thisStart && prevEnd >= cursor && thisStart <= source.length) {
            val between = source.substring(prevEnd, thisStart)
            val commaIndex = between.indexOf(',')
            if (commaIndex >= 0) {
              cursor = prevEnd + commaIndex + 1
            }
          }
        }

        visitTree(arg) match {
          case expr: Expression =>
            // For the last arg, capture trailing space before `)`
            val isLastArg = i == app.args.size - 1
            val afterSpace = if (isLastArg) {
              val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
              val closePos = source.indexOf(')', Math.max(cursor, argEnd))
              if (closePos > argEnd) Space.format(source.substring(argEnd, closePos)) else Space.EMPTY
            } else Space.EMPTY
            args.add(new JRightPadded(expr, afterSpace, Markers.EMPTY))
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

    val nameSpace = Space.EMPTY

    val name = new J.Identifier(
      Tree.randomId(),
      nameSpace,
      Markers.EMPTY,
      Collections.emptyList(),
      methodName,
      null,
      null
    )

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
    } else {
      Markers.EMPTY
    }

    new J.MethodInvocation(
      Tree.randomId(),
      prefix,
      markers,
      if (select != null) new JRightPadded(select, selectAfterSpace, Markers.EMPTY) else null,
      null, // typeParameters - handled separately in TypeApply
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
      case _ => return visitUnknown(app)
    }
    
    // Visit the index expression
    val index = visitTree(app.args.head) match {
      case expr: Expression => expr
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
        case _ => return visitUnknown(app)
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
      
      // Extract space before the dot/hash and after it (for the name prefix)
      val qualifierEnd = sel.qualifier.span.end
      val nameStart = sel.nameSpan.start
      var isTypeProjection = false
      var beforeDotSpace: Space = Space.EMPTY
      val dotSpace = if (qualifierEnd < nameStart) {
        val dotStart = Math.max(0, qualifierEnd - offsetAdjustment)
        val nameStartAdjusted = Math.max(0, nameStart - offsetAdjustment)
        if (dotStart < nameStartAdjusted && dotStart >= cursor && nameStartAdjusted <= source.length) {
          val between = source.substring(dotStart, nameStartAdjusted)
          // Check for type projection (#) or member access (.)
          val hashIndex = between.indexOf('#')
          val dotIndex = between.indexOf('.')
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
      
      // Create the name identifier with type from the Select node
      val name = new J.Identifier(
        Tree.randomId(),
        dotSpace,
        Markers.EMPTY,
        Collections.emptyList(),
        sel.name.toString,
        typeOfTree(sel),
        variableTypeOfTree(sel)
      )
      
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
    
    // Visit the select (left side)
    val select = visitTree(infixOp.left) match {
      case expr: Expression => expr
      case _ => return visitUnknown(infixOp)
    }
    
    // Extract method name
    val methodName = infixOp.op.name.toString
    
    // Extract space before the method name
    val leftEnd = Math.max(0, infixOp.left.span.end - offsetAdjustment)
    val opStart = Math.max(0, infixOp.op.span.start - offsetAdjustment)
    val methodNameSpace = if (leftEnd < opStart && leftEnd >= cursor && opStart <= source.length) {
      cursor = opStart
      Space.format(source.substring(leftEnd, opStart))
    } else {
      Space.format(" ")
    }
    
    // Move cursor past the method name
    cursor = Math.max(0, infixOp.op.span.end - offsetAdjustment)
    
    // Extract space before the argument
    val opEnd = Math.max(0, infixOp.op.span.end - offsetAdjustment)
    val rightStart = Math.max(0, infixOp.right.span.start - offsetAdjustment)
    val argSpace = if (opEnd < rightStart && opEnd >= cursor && rightStart <= source.length) {
      cursor = rightStart
      Space.format(source.substring(opEnd, rightStart))
    } else {
      Space.format(" ")
    }
    
    // Visit the argument (right side)
    val savedCursorArg = cursor
    val arg = visitTree(infixOp.right) match {
      case expr: Expression => expr
      case block: J.Block =>
        // Block arguments in infix calls: `"test" should { ... }`
        new S.BlockExpression(Tree.randomId(), argSpace, Markers.EMPTY,
          block.withPrefix(Space.EMPTY), null)
      case _ => cursor = savedCursorArg; return visitUnknown(infixOp)
    }
    
    // Create the method name identifier
    val name = new J.Identifier(
      Tree.randomId(),
      methodNameSpace,
      Markers.EMPTY,
      Collections.emptyList(),
      methodName,
      null,
      null
    )
    
    // Create the arguments container
    val args = new util.ArrayList[JRightPadded[Expression]]()
    args.add(JRightPadded.build(arg.withPrefix(argSpace)).withAfter(Space.EMPTY))
    
    // Import the InfixNotation marker
    import org.openrewrite.scala.marker.InfixNotation
    
    // Create the method invocation with the InfixNotation marker
    new J.MethodInvocation(
      Tree.randomId(),
      prefix,
      Markers.build(Collections.singletonList(InfixNotation.create())),
      JRightPadded.build(select),
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
  
  private def visitNewClassWithArgs(newTree: Trees.New[?], app: Trees.Apply[?]): J = {
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
      case template: Trees.Template[?] =>
        // Extract the parent type(s) - usually the first parent is the main type
        val parents = template.parents.filter(p => p.span.exists && !p.span.isSynthetic)
        if (parents.isEmpty) {
          return visitUnknown(newTree)
        }
        
        // The first parent is typically an Apply node for constructor calls
        // or just an Ident/Select for interfaces/traits
        val firstParent = parents.head
        
        // Extract the class type and arguments
        val (clazz, args) = firstParent match {
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
          // Filter out synthetic nodes, the compiler-added constructor, and self-reference
          val bodyStatements = template.body.filter { stat =>
            if (stat.span.isSynthetic || !stat.span.exists) false
            else stat match {
              case dd: Trees.DefDef[?] if dd.name.toString == "<init>" => false
              case vd: Trees.ValDef[?] if vd.name.toString == "_" => false
              case _ => true
            }
          }
          
          if (bodyStatements.nonEmpty || {
            // Check for empty body braces `{}` in source
            val bodyStart = Math.max(cursor, Math.max(0, newTree.span.start - offsetAdjustment))
            val bodyEnd = Math.max(0, newTree.span.end - offsetAdjustment)
            bodyStart < bodyEnd && bodyEnd <= source.length && source.substring(bodyStart, bodyEnd).contains("{")
          }) {
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
          methodTypeOfTree(newTree)
        )

      case _ =>
        // Not an anonymous class, shouldn't happen in visitNew
        visitUnknown(newTree)
    }
  }

  private def visitImport(imp: Trees.Import[?]): J = {
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
      // Build the qualid: for `import java.util.List`, expr=Select(Ident(java),util), selector=List
      // For `import java.util`, expr=Ident(java), selector=util
      var qualid: J.FieldAccess = expr match {
        case fa: J.FieldAccess => fa
        case id: J.Identifier if imp.selectors.nonEmpty =>
          // Simple Ident root (e.g., `java` in `import java.util`)
          // Build FieldAccess with the Ident as target and first selector as name
          val selector = imp.selectors.head
          selector match {
            case is @ untpd.ImportSelector(sIdent: Trees.Ident[?], untpd.EmptyTree, untpd.EmptyTree) =>
              if (cursor < source.length && source.charAt(cursor) == '.') cursor += 1
              // For wildcard imports, read source to preserve * vs _
              val selName = if (is.isWildcard) {
                val adjustedEnd = Math.max(0, imp.span.end - offsetAdjustment)
                if (adjustedEnd > 0 && adjustedEnd <= source.length && source.charAt(adjustedEnd - 1) == '*') "*" else "_"
              } else sIdent.name.toString
              val packagePrefix = extractFqn(imp.expr)
              val fqn = if (packagePrefix.nonEmpty) packagePrefix + "." + selName else selName
              val selectorType: JavaType = typeForFqn(fqn)
              new J.FieldAccess(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                id, JLeftPadded.build(new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                  Collections.emptyList(), selName, selectorType, null)), selectorType)
            case _ =>
              // Can't build valid FieldAccess
              return visitUnknown(imp)
          }
        case other =>
          return visitUnknown(imp)
      }

      // Handle selectors for multi-segment imports (e.g., `import java.util.List`)
      // Skip if we already consumed the selector above (single Ident root case)
      val alreadyHandledSelector = expr.isInstanceOf[J.Identifier] && imp.selectors.nonEmpty
      if (!alreadyHandledSelector && imp.selectors.nonEmpty && imp.selectors.size == 1) {
        val selector = imp.selectors.head
        selector match {
          case is @ untpd.ImportSelector(ident: Trees.Ident[?], untpd.EmptyTree, untpd.EmptyTree) =>
            if (cursor < source.length && source.charAt(cursor) == '.') cursor += 1
            // For wildcard imports, use source character to preserve * vs _
            val selectorName = if (is.isWildcard) {
              // Read the actual wildcard char from source
              val adjustedEnd = Math.max(0, imp.span.end - offsetAdjustment)
              if (adjustedEnd > 0 && adjustedEnd <= source.length) {
                val lastChar = source.charAt(adjustedEnd - 1)
                if (lastChar == '*') "*" else "_"
              } else ident.name.toString
            } else ident.name.toString
            val packagePrefix = extractFqn(imp.expr)
            val fqn = if (packagePrefix.nonEmpty) packagePrefix + "." + selectorName else selectorName
            val selectorType: JavaType = typeForFqn(fqn)
            qualid = new J.FieldAccess(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
              qualid,
              JLeftPadded.build(new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                Collections.emptyList(), selectorName, selectorType, null)),
              selectorType)
          case _ =>
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
      // Complex imports: braces, aliases (as/=>), wildcards with braces, etc.
      // Preserve the full source text since these can't map cleanly to J.Import's
      // FieldAccess-based qualid model.
      val prefix = extractPrefix(imp.span)
      val sourceText = extractSource(imp.span)
      val isContinuation = !sourceText.trim.startsWith("import")
      if (isContinuation) {
        // Continuation of a comma-separated import: `import A._, B._`
        // The prefix may contain a comma — include it in the source text.
        // Find the comma before this import's span and include everything from there.
        val adjustedStart = Math.max(0, imp.span.start - offsetAdjustment)
        val commaSearch = if (adjustedStart > 0) source.lastIndexOf(',', adjustedStart) else -1
        val fullText = if (commaSearch >= 0 && commaSearch < adjustedStart) {
          source.substring(commaSearch, Math.max(0, imp.span.end - offsetAdjustment))
        } else sourceText
        updateCursor(imp.span.end)
        return new J.Unknown(
          Tree.randomId(), Space.EMPTY, Markers.EMPTY,
          new J.Unknown.Source(Tree.randomId(), Space.EMPTY, Markers.EMPTY, fullText)
        )
      }

      // sourceText starts with "import " - strip that for the qualid
      val qualidText = sourceText.substring("import ".length)

      // Wrap in FieldAccess with Empty target (required by J.Import).
      val qualid = new J.FieldAccess(
        Tree.randomId(), Space.EMPTY, Markers.EMPTY,
        new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY),
        JLeftPadded.build(new J.Identifier(
          Tree.randomId(), Space.EMPTY, Markers.EMPTY,
          Collections.emptyList(), qualidText, null, null)),
        null
      )

      updateCursor(imp.span.end)

      new J.Import(
        Tree.randomId(),
        prefix,
        Markers.EMPTY,
        JLeftPadded.build(false),
        qualid,
        null
      )
    }
  }
  
  private def isSimpleImport(imp: Trees.Import[?]): Boolean = {
    // Check if this is a simple import without braces
    if (imp.span.exists) {
      val adjustedStart = Math.max(0, imp.span.start - offsetAdjustment)
      val adjustedEnd = Math.max(0, imp.span.end - offsetAdjustment)
      if (adjustedStart >= 0 && adjustedEnd <= source.length && adjustedEnd > adjustedStart) {
        val importText = source.substring(adjustedStart, adjustedEnd)
        // Not simple if:
        // - has braces: import a.{B, C}
        // - has rename: import a.{B as C} or import a.{B => C}
        // - span doesn't contain "import" keyword (part of comma-separated import like `import A._, B._`)
        !importText.contains("{") && !importText.contains(" as ") && !importText.contains("=>") &&
          importText.trim.startsWith("import")
      } else {
        false
      }
    } else {
      false
    }
  }
  
  
  
  private def visitPackageDef(pkg: Trees.PackageDef[?]): J = {
    // Package definitions at the statement level should not be converted to statements
    // They are handled at the compilation unit level
    // Return null to indicate this node should be skipped
    null
  }
  
  private def visitLambdaParameter(vd: Trees.ValDef[?]): J = {
    val prefix = extractPrefix(vd.span)
    
    // Check if the type was explicitly written in source or inferred
    // If the source doesn't contain a colon after the name, it's inferred
    val sourceText = extractSource(vd.span)
    val hasExplicitType = sourceText.contains(":")
    
    // If there's no explicit type in source, just return an identifier
    if (!hasExplicitType || vd.tpt == untpd.EmptyTree) {
      new J.Identifier(
        Tree.randomId(),
        prefix,
        Markers.EMPTY,
        Collections.emptyList(),
        vd.name.toString,
        null,
        null
      )
    } else {
      // With a type, we need a full variable declaration
      val name = new J.Identifier(
        Tree.randomId(),
        Space.EMPTY,
        Markers.EMPTY,
        Collections.emptyList(),
        vd.name.toString,
        null,
        null
      )
      
      // Extract the type
      val sourceText = extractSource(vd.span)
      val colonIdx = sourceText.indexOf(':')
      val typeSpace = if (colonIdx >= 0 && colonIdx + 1 < sourceText.length) {
        Space.format(sourceText.substring(colonIdx + 1).takeWhile(_.isWhitespace))
      } else {
        Space.SINGLE_SPACE
      }
      
      val typeExpr: TypeTree = visitTree(vd.tpt) match {
        case tt: TypeTree => tt.withPrefix(typeSpace)
        case id: J.Identifier => id.withPrefix(typeSpace)
        case unknown: J.Unknown => unknown.withPrefix(typeSpace)  // Handle J.Unknown types
        case _ => null
      }
      
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
      
      // Create the variable declarations with a marker to indicate it's a lambda parameter
      import org.openrewrite.scala.marker.LambdaParameter
      new J.VariableDeclarations(
        Tree.randomId(),
        prefix,
        Markers.build(Collections.singletonList(new LambdaParameter())),
        Collections.emptyList(), // no annotations
        Collections.emptyList(), // no modifiers
        typeExpr,
        null,
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
    var beforeValVar = Space.EMPTY
    var afterValVar = Space.EMPTY
    val modifiers = new util.ArrayList[J.Modifier]()
    var hasExplicitFinal = false
    var hasExplicitLazy = false
    
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
          val cb = afterKw.indexOf(']')
          if (cb >= 0) { keyword = "private" + afterKw.substring(0, cb + 1); keyLen = keyword.length }
        }
        modifiers.add(new J.Modifier(
          Tree.randomId(), Space.EMPTY, Markers.EMPTY,
          keyword, J.Modifier.Type.Private, Collections.emptyList()
        ))
        modifierEndPos = leadingWs + keyLen
        if (modifierEndPos < sourceSnippet.length && sourceSnippet.charAt(modifierEndPos) == ' ') modifierEndPos += 1
      } else if (trimmedSnippet.startsWith("protected")) {
        var keyword = "protected"
        var keyLen = keyword.length
        val afterKw = trimmedSnippet.substring(keyLen)
        if (afterKw.startsWith("[")) {
          val cb = afterKw.indexOf(']')
          if (cb >= 0) { keyword = "protected" + afterKw.substring(0, cb + 1); keyLen = keyword.length }
        }
        modifiers.add(new J.Modifier(
          Tree.randomId(), Space.EMPTY, Markers.EMPTY,
          keyword, J.Modifier.Type.Protected, Collections.emptyList()
        ))
        modifierEndPos = leadingWs + keyLen
        if (modifierEndPos < sourceSnippet.length && sourceSnippet.charAt(modifierEndPos) == ' ') modifierEndPos += 1
      }
      
      // Check for remaining modifiers (in any order) before val/var/def
      // These include: implicit, override, abstract, final, lazy, sealed
      var scanning = true
      while (scanning && modifierEndPos < sourceSnippet.length) {
        val remaining = sourceSnippet.substring(modifierEndPos)
        val modSpace = if (modifierEndPos > leadingWs) Space.SINGLE_SPACE else Space.EMPTY

        if (remaining.startsWith("final ")) {
          hasExplicitFinal = true
          modifiers.add(new J.Modifier(Tree.randomId(), modSpace, Markers.EMPTY,
            "final", J.Modifier.Type.Final, Collections.emptyList()))
          modifierEndPos += "final ".length
        } else if (remaining.startsWith("lazy ")) {
          hasExplicitLazy = true
          modifiers.add(new J.Modifier(Tree.randomId(), modSpace, Markers.EMPTY,
            "lazy", J.Modifier.Type.LanguageExtension, Collections.emptyList()))
          modifierEndPos += "lazy ".length
        } else if (remaining.startsWith("implicit ")) {
          modifiers.add(new J.Modifier(Tree.randomId(), modSpace, Markers.EMPTY,
            "implicit", J.Modifier.Type.LanguageExtension, Collections.emptyList()))
          modifierEndPos += "implicit ".length
        } else if (remaining.startsWith("override ")) {
          modifiers.add(new J.Modifier(Tree.randomId(), modSpace, Markers.EMPTY,
            "override", J.Modifier.Type.LanguageExtension, Collections.emptyList()))
          modifierEndPos += "override ".length
        } else if (remaining.startsWith("abstract ")) {
          modifiers.add(new J.Modifier(Tree.randomId(), modSpace, Markers.EMPTY,
            "abstract", J.Modifier.Type.Abstract, Collections.emptyList()))
          modifierEndPos += "abstract ".length
        } else if (remaining.startsWith("sealed ")) {
          modifiers.add(new J.Modifier(Tree.randomId(), modSpace, Markers.EMPTY,
            "sealed", J.Modifier.Type.Sealed, Collections.emptyList()))
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
        // Extract space before val/var (after modifiers or annotations)
        if (keywordStart > 0) {
          beforeValVar = Space.format(afterModifiers.substring(0, keywordStart))
        } else if (hasAnnotations && modifierEndPos == 0) {
          // When we have annotations but no other modifiers, the space is already in beforeValVar
          // since cursor is positioned after annotations
          beforeValVar = Space.EMPTY
        }
        
        // Move cursor past all modifiers and the keyword
        cursor = adjustedStart + modifierEndPos + keywordStart + keyword.length
        valVarKeyword = keyword
        
        // Extract space after val/var/given
        // Look for the variable name in the source
        val varNameStr = vd.name.toString
        val nameIndex = if (varNameStr.nonEmpty) source.indexOf(varNameStr, cursor) else -1
        if (nameIndex >= cursor && varNameStr.nonEmpty) {
          afterValVar = Space.format(source.substring(cursor, nameIndex))
          cursor = nameIndex
        } else if (keyword == "given") {
          // Anonymous given: `given Ordering[Int] = ...`
          // The name is synthesized and doesn't appear in source.
          // Fall back to Unknown to preserve exact source text.
          cursor = savedCursorEntry
          return visitUnknown(vd)
        }
      }
    }
    
    // Val is implicitly final in Scala (but don't add it if we already have explicit final)
    val isGiven = valVarKeyword == "given"
    val isFinal = valVarKeyword == "val" || isGiven
    if (isFinal && !hasExplicitFinal) {
      // For `given` declarations, store the keyword so the printer outputs "given" not "val"
      val finalKeyword = if (isGiven) "given" else null
      modifiers.add(new J.Modifier(
        Tree.randomId(),
        if (modifiers.isEmpty) beforeValVar else Space.SINGLE_SPACE,
        Markers.EMPTY,
        finalKeyword,
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
      val savedCursorType = cursor
      typeExpression = visitTree(vd.tpt) match {
        case tt: TypeTree =>
          // For type expressions, preserve the space after the colon
          tt match {
            case pt: J.ParameterizedType => pt.withPrefix(afterColon)
            case id: J.Identifier => id.withPrefix(afterColon)
            case fa: J.FieldAccess => fa.withPrefix(afterColon)
            case _ => tt
          }
        case other =>
          // Intersection types (A & B), union types (A | B), and other non-TypeTree
          // type expressions: fall back to Unknown to preserve source text
          cursor = savedCursorType
          val unknown = visitUnknown(vd.tpt)
          unknown.withPrefix(afterColon)
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
      val tupleStart = source.indexOf('(', cursor)
      if (tupleStart >= 0) {
        val beforeTuple = Space.format(source.substring(cursor, tupleStart))
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
          val elem = new J.Identifier(
            Tree.randomId(),
            beforePart,
            Markers.EMPTY,
            Collections.emptyList(),
            part,
            null,
            null
          )
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
        new J.Identifier(
          Tree.randomId(),
          afterValVar,
          Markers.EMPTY,
          Collections.emptyList(),
          vd.name.toString,
          null,
          null
        )
      }
    } else {
      new J.Identifier(
        Tree.randomId(),
        afterValVar,
        Markers.EMPTY,
        Collections.emptyList(),
        vd.name.toString,
        typeOfTree(vd),
        variableTypeOfTree(vd)
      )
    }

    // Update cursor past the name only if we haven't parsed a type and it's not a tuple pattern
    // If we parsed a type or a tuple pattern, the cursor is already past them
    if (typeExpression == null && !isTuplePattern) {
      cursor = cursor + vd.name.toString.length
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
      val underscoreIndex = source.indexOf('_', cursor)
      if (underscoreIndex >= 0) {
        val beforeUnderscore = source.substring(cursor, underscoreIndex)
        val equalsIndex = beforeUnderscore.indexOf('=')
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
          val rhsTree = visitTree(vd.rhs)
          
          rhsTree match {
            case block: J.Block =>
              // In Scala, blocks are expressions. Wrap the block in S.BlockExpression
              val blockExpr = new S.BlockExpression(
                Tree.randomId(),
                Space.format(afterEqualsStr),
                Markers.EMPTY,
                block.withPrefix(Space.EMPTY),
                typeOfTree(vd)
              )
              initializer = blockExpr
              
            case expr: Expression =>
              // Set initializer with space after equals
              initializer = expr match {
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
                case lambda: J.Lambda => lambda.withPrefix(Space.format(afterEqualsStr))
                case mr: J.MemberReference => mr.withPrefix(Space.format(afterEqualsStr))
                case tc: J.TypeCast => tc.withPrefix(Space.format(afterEqualsStr))
                case io: J.InstanceOf => io.withPrefix(Space.format(afterEqualsStr))
                case un: J.Unary => un.withPrefix(Space.format(afterEqualsStr))
                case _ => 
                  // For any other expression type, just return it as-is
                  expr
              }
              
            case _ =>
              // Statements like if/else, match, try are expressions in Scala
              // but not in Java's type system. Wrap in J.Unknown to preserve source.
              cursor = Math.max(0, vd.rhs.span.start - offsetAdjustment)
              val unknownInit = visitUnknown(vd.rhs)
              initializer = unknownInit.withPrefix(Space.format(afterEqualsStr))
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
    new J.VariableDeclarations(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      leadingAnnotations, // Pass the annotations we collected
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
    
    var isEnumCase = false
    if (adjustedStart >= cursor && adjustedEnd <= source.length) {
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
    val (modifiers, lastModEnd) = extractModifiersFromText(md.mods, modifierText)

    // Check for case modifier on object definitions (e.g., `case object Foo`).
    // Skip if this is an enum case — `case` is the keyword, not a modifier.
    if (!isEnumCase && modifierText.contains("case")) {
      val caseIndex = modifierText.indexOf("case")
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
    
    // Create the class kind
    val kindType = if (isEnumCase) J.ClassDeclaration.Kind.Type.Enum else J.ClassDeclaration.Kind.Type.Class
    val kind = new J.ClassDeclaration.Kind(
      Tree.randomId(),
      kindPrefix,
      Markers.EMPTY,
      Collections.emptyList(),
      kindType
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
      case tmpl: Trees.Template[?] if tmpl.parents.nonEmpty =>
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
        val savedCursorExtends = cursor
        val extendsType = visitTree(firstParent) match {
          case typeTree: TypeTree => typeTree
          case _ =>
            cursor = savedCursorExtends
            visitUnknown(firstParent).asInstanceOf[TypeTree]
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
      case tmpl: Trees.Template[?] if tmpl.body.nonEmpty =>
        // Find the body delimiter: `{` (braced) or `:` (Scala 3 braceless)
        var bodyPrefix = Space.EMPTY
        var isBraceless = false
        if (cursor < source.length && md.span.exists) {
          val remaining = source.substring(cursor, Math.min(md.span.end - offsetAdjustment, source.length))
          val braceIdx = remaining.indexOf('{')
          val colonIdx = remaining.indexOf(':')
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
          if (stat.span.exists && !stat.span.isSynthetic && visitedSpans.add(stat.span.start)) {
            visitTree(stat) match {
              case stmt: Statement => statements.add(JRightPadded.build(stmt))
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
              endSpace = Space.format(source.substring(cursor, Math.min(endPos, source.length)))
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
          Markers.build(Collections.singletonList(new IndentedBlock(Tree.randomId())))
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
      TypeUtils.asFullyQualified(typeOfTree(md))
    )
  }

  private def visitAssign(asg: Trees.Assign[?]): J = {
    val prefix = extractPrefix(asg.span)
    
    // Visit the left-hand side
    val variable = visitTree(asg.lhs) match {
      case expr: Expression => expr
      case _ => return visitUnknown(asg)
    }

    // Find the position of the equals sign
    val lhsEnd = Math.max(cursor, Math.max(0, asg.lhs.span.end - offsetAdjustment))
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
        case _ => return visitUnknown(ifTree)
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
  
  private def visitWhileDo(whileTree: Trees.WhileDo[?]): J = {
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
      case _ => return visitUnknown(whileTree)
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
  private def visitSimpleForEach(forTree: untpd.ForDo, genFrom: untpd.GenFrom): J = {
    val prefix = extractPrefix(forTree.span)
    
    // Extract the pattern (variable declaration)
    val pattern = genFrom.pat
    val varName = pattern match {
      case ident: Trees.Ident[?] => ident.name.toString
      case _ => 
        // For now, only handle simple identifier patterns
        return visitUnknown(forTree)
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
      case _ => return visitUnknown(forTree)
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
      case app: Trees.Apply[?] =>
        app.fun match {
          case sel: Trees.Select[?] =>
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
  
  private def visitRangeBasedFor(forTree: untpd.ForDo, genFrom: untpd.GenFrom): J = {
    val prefix = extractPrefix(forTree.span)
    
    // For now, don't capture original source to avoid cursor issues
    val originalSource = ""
    
    // Extract the loop variable name
    val varName = genFrom.pat match {
      case ident: Trees.Ident[?] => ident.name.toString
      case _ => 
        // For now, only handle simple identifier patterns
        return visitUnknown(forTree)
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
      case app: Trees.Apply[?] =>
        app.fun match {
          case sel: Trees.Select[?] =>
            val methodName = sel.name.toString
            val startExpr = visitTree(sel.qualifier).asInstanceOf[Expression]
            val endExpr = visitTree(app.args.head).asInstanceOf[Expression]
            (startExpr, endExpr, methodName == "to")
          case _ => 
            return visitUnknown(forTree)
        }
      case infixOp: untpd.InfixOp =>
        val opName = infixOp.op.name.toString
        val startExpr = visitTree(infixOp.left).asInstanceOf[Expression]
        val endExpr = visitTree(infixOp.right).asInstanceOf[Expression]
        (startExpr, endExpr, opName == "to")
      case _ => 
        return visitUnknown(forTree)
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
  
  private def visitBlock(block: Trees.Block[?]): J.Block = {
    val prefix = extractPrefix(block.span)

    // Move cursor past the opening brace — but only if the block starts with one
    val blockStart = Math.max(0, block.span.start - offsetAdjustment)
    val blockEndAdj = Math.max(0, block.span.end - offsetAdjustment)
    val blockStartsWithBrace = blockStart < source.length && source.charAt(blockStart) == '{'
    if (blockStartsWithBrace) {
      cursor = blockStart + 1
    } else {
      // Check if there's a brace between cursor and the first statement
      val firstChildStart = if (block.stats.nonEmpty) {
        Math.max(0, block.stats.head.span.start - offsetAdjustment)
      } else if (!block.expr.isEmpty) {
        Math.max(0, block.expr.span.start - offsetAdjustment)
      } else blockStart

      if (cursor < firstChildStart && firstChildStart <= source.length) {
        val between = source.substring(cursor, firstChildStart)
        val braceIdx = between.indexOf('{')
        if (braceIdx >= 0) {
          cursor = cursor + braceIdx + 1
        }
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
          val trailStart = Math.max(statEnd, cursor)
          if (trailStart < nextStart && nextStart <= source.length) {
            trailingSpace = Space.format(source.substring(trailStart, nextStart))
            cursor = nextStart
          }
          
          statements.add(JRightPadded.build(stmt).withAfter(trailingSpace))
        case _ => // Skip non-statement nodes
      }
    }
    
    // Handle the expression part of the block (if any)
    if (!block.expr.isEmpty) {
      visitTree(block.expr) match {
        case expr: Expression =>
          // In Scala, the last expression in a block is the return value
          // Wrap it in a J.Return with ImplicitReturn marker
          val implicitReturn = new J.Return(
            Tree.randomId(),
            expr.getPrefix(),
            Markers.build(Collections.singletonList(new ImplicitReturn(UUID.randomUUID()))),
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
    
    new J.Block(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
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
    val keywordLength = if (isTrait) "trait".length else if (isEnum) "enum".length else "class".length
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
          
          jTypeParams.add(JRightPadded.build(jTypeParam))

          // Advance cursor past the comma so the next param's extractPrefix
          // doesn't include it. The printer handles comma output.
          if (!isLast && cursor < source.length) {
            val commaPos = source.indexOf(',', cursor)
            if (commaPos >= 0) cursor = commaPos + 1
          }
        }
        
        // Update cursor to after closing bracket.
        // Use cursor (not span.end) since visitTypeParameter advances past [_].
        if (cursor < source.length) {
          val searchEnd = Math.min(cursor + 20, source.length)
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
    
    // Handle constructor parameters - extract only value parameters
    val cursorBeforeCtorExtraction = cursor
    val constructorParamsSource = if (template != null && template.constr.paramss.size > 1) {
      // If we have type parameters, constructor params are in the second list
      extractConstructorParametersSource(td)
    } else if (template != null && template.constr.paramss.nonEmpty) {
      // Check if the first list has only value parameters
      val firstList = template.constr.paramss.head
      if (firstList.forall(_.isInstanceOf[Trees.ValDef[?]])) {
        extractConstructorParametersSource(td)
      } else {
        ""
      }
    } else {
      ""
    }
    
    val primaryConstructor = if (constructorParamsSource.nonEmpty) {
      // Compute the prefix: whitespace between cursor (before extraction) and '('
      val parenIdx = source.indexOf('(', cursorBeforeCtorExtraction)
      val ctorPrefix = if (parenIdx > cursorBeforeCtorExtraction && parenIdx < cursor) {
        Space.format(source.substring(cursorBeforeCtorExtraction, parenIdx))
      } else Space.EMPTY

      val unknown = new J.Unknown(
        Tree.randomId(),
        ctorPrefix,
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
            val extendsIndex = betweenText.indexOf("extends")
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
          case other =>
            // Fallback to Unknown - handles cases like extends Exception(args)
            // Restore cursor so visitUnknown gets the right prefix
            cursor = savedCursorExtends
            other match {
              case u: J.Unknown => u
              case _ => visitUnknown(firstParent)
            }
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
              case other =>
                cursor = savedCursorWith
                other match {
                  case u: J.Unknown => u
                  case _ => visitUnknown(parent)
                }
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
        case template: Trees.Template[?] =>
          // Extract space before the body delimiter: `{` (braced) or `:` (Scala 3 braceless)
          var isClassBraceless = false
          val bodyPrefix = if (td.span.exists) {
            val classEnd = Math.max(0, td.span.end - offsetAdjustment)
            if (cursor < classEnd && classEnd <= source.length) {
              val afterCursor = source.substring(cursor, classEnd)
              val braceIndex = afterCursor.indexOf("{")
              // For Scala 3 braceless: look for `:` at end of line (not `: Type` annotation).
              // A braceless body colon is followed by a newline, not by a type name.
              val colonIndex = {
                var result = -1
                var idx = afterCursor.indexOf(':')
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
                  if (result < 0) idx = afterCursor.indexOf(':', idx + 1)
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
          for (stat <- sortedBody) {
            if (stat.span.exists && !stat.span.isSynthetic) {
              if (visitedSpans.add(stat.span.start)) {
                visitTree(stat) match {
                  case null =>
                  case stmt: Statement =>
                    statements.add(JRightPadded.build(stmt))
                  case _ =>
                }
              }
            }
          }
          
          // Extract end space
          val endSpace = if (td.span.exists) {
            val classEnd = Math.max(0, td.span.end - offsetAdjustment)
            if (isClassBraceless) {
              if (cursor < classEnd) {
                val es = Space.format(source.substring(cursor, Math.min(classEnd, source.length)))
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
            Markers.build(Collections.singletonList(new IndentedBlock(Tree.randomId())))
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

    // Extract the expression being returned (if any)
    val expr = if (ret.expr.isEmpty) {
      null // void return
    } else {
      visitTree(ret.expr) match {
        case expression: Expression => expression
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

    // Visit the exception expression
    val exception = visitTree(thr.expr) match {
      case expr: Expression => expr
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
            case _ => return visitUnknown(ta)
          }
          
          // Update cursor past ".asInstanceOf"
          val asInstanceOfEnd = sel.span.end
          if (asInstanceOfEnd > cursor) {
            cursor = asInstanceOfEnd
          }
          
          // Now handle the type argument in brackets
          // Extract any space before the opening bracket
          val typeArgStart = ta.args.head.span.start - offsetAdjustment
          val spaceBeforeBracket = if (cursor < typeArgStart && typeArgStart <= source.length) {
            val between = source.substring(cursor, typeArgStart)
            // Find the bracket position
            val bracketPos = between.indexOf('[')
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
          
          return new J.TypeCast(
            Tree.randomId(),
            Space.EMPTY,  // TypeCast itself has no prefix - the space is handled by the variable initializer
            Markers.EMPTY,
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
    cursor = savedCursor
    visitUnknown(ta)
  }
  
  private def visitAppliedTypeTree(at: Trees.AppliedTypeTree[?]): J = {
    // AppliedTypeTree represents a parameterized type like List[String]
    val savedCursor = cursor
    val prefix = extractPrefix(at.span)

    // Save original cursor position
    val originalCursor = cursor
    
    // Visit the base type (e.g., List, Map, Option)
    val clazz = visitTree(at.tpt) match {
      case nt: NameTree => nt
      case _ => cursor = savedCursor; return visitUnknown(at)
    }

    // Extract the source to find bracket positions
    val source = extractSource(at.span)
    val openBracketIdx = source.indexOf('[')
    val closeBracketIdx = source.lastIndexOf(']')

    if (openBracketIdx < 0 || closeBracketIdx < 0) {
      cursor = savedCursor; return visitUnknown(at)
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
      cursor = firstArgStart
      
      for (i <- at.args.indices) {
        val arg = at.args(i)
        val argTree = visitTree(arg) match {
          case expr: Expression => expr
          case _ => cursor = savedCursor; return visitUnknown(at)
        }

        // Extract trailing comma/space
        val isLast = i == at.args.size - 1
        val afterSpace = if (isLast) {
          // Space before closing bracket
          val argEnd = Math.max(0, arg.span.end - offsetAdjustment)
          if (argEnd < closeBracketIdx + originalCursor) {
            val spaceStr = this.source.substring(argEnd, closeBracketIdx + originalCursor)
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
    
    // Resolve type: try span-based first, then derive from the base clazz type
    var paramType: JavaType = typeOfTree(at)
    if (paramType == null) {
      // Fall back to the type of the base class identifier (e.g., List → java.util.List)
      paramType = clazz match {
        case id: J.Identifier => id.getType
        case fa: J.FieldAccess => fa.getType
        case _ => null
      }
    }

    new J.ParameterizedType(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      clazz,
      typeParameters,
      paramType
    )
  }

  private def visitDefDef(dd: Trees.DefDef[?]): J = {
    // Fall back to J.Unknown for cases we can't handle yet
    val hasAnnotations = dd.mods != null && dd.mods.annotations.nonEmpty
    if (hasAnnotations) {
      return visitUnknown(dd)
    }

    // Check for cases we need to fall back to J.Unknown
    val adjustedStart = Math.max(0, dd.span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, dd.span.end - offsetAdjustment)
    if (adjustedStart < adjustedEnd && adjustedEnd <= source.length) {
      val defSource = source.substring(adjustedStart, adjustedEnd)
      // Procedure syntax (no = before body)
      val braceIdx = defSource.indexOf('{')
      val equalsIdx = defSource.indexOf('=')
      if (braceIdx >= 0 && (equalsIdx < 0 || equalsIdx > braceIdx)) {
        return visitUnknown(dd)
      }
      // Nested braces after = (compiler flattens these, breaking round-trip)
      if (equalsIdx >= 0 && braceIdx >= 0) {
        val afterFirstBrace = defSource.indexOf('{', braceIdx + 1)
        if (afterFirstBrace >= 0) {
          val between = defSource.substring(braceIdx + 1, afterFirstBrace).trim()
          if (between.isEmpty) {
            return visitUnknown(dd)
          }
        }
      }
      // Fall back for methods with while/for loops (block whitespace issues)
      if (defSource.contains("while ") || defSource.contains("while(")) {
        return visitUnknown(dd)
      }
    }

    // Fall back for parameterless methods like `def name: Type` (no parens in source)
    if (adjustedStart < adjustedEnd && adjustedEnd <= source.length) {
      val defSource = source.substring(adjustedStart, adjustedEnd)
      val defIdx = defSource.indexOf("def ")
      if (defIdx >= 0) {
        val afterDef = defSource.substring(defIdx + 4)
        // Find end of name (first non-alphanumeric char)
        val nameEnd = afterDef.indexWhere(c => !c.isLetterOrDigit && c != '_')
        if (nameEnd >= 0) {
          val afterName = afterDef.substring(nameEnd).trim()
          // If the next meaningful char after name is : or = (not (), the method has no parens
          if (afterName.startsWith(":") || afterName.startsWith("=")) {
            return visitUnknown(dd)
          }
        }
      }
    }

    // Fall back for methods with complex types in parameters
    // (cursor tracking for types like Map[String, Any], Int => Int is not yet reliable)
    val hasComplexParams = dd.paramss.exists(_.exists {
      case vd: Trees.ValDef[?] =>
        vd.tpt.isInstanceOf[Trees.AppliedTypeTree[?]] ||
        vd.tpt.isInstanceOf[untpd.Function] ||
        (vd.mods != null && vd.mods.annotations.nonEmpty) // parameter-level annotations
      case _ => false
    })
    if (hasComplexParams) {
      return visitUnknown(dd)
    }

    val savedCursor = cursor
    try {
      visitDefDefImpl(dd)
    } catch {
      case _: Exception =>
        cursor = savedCursor
        visitUnknown(dd)
    }
  }

  private def visitDefDefImpl(dd: Trees.DefDef[?]): J.MethodDeclaration = {
    val leadingAnnotations = new util.ArrayList[J.Annotation]()
    val prefix = extractPrefix(dd.span)

    val adjustedEnd = Math.max(0, dd.span.end - offsetAdjustment)
    var modifierText = ""
    var defIndex = -1

    if (cursor <= adjustedEnd && adjustedEnd <= source.length) {
      val sourceSnippet = source.substring(cursor, adjustedEnd)
      defIndex = sourceSnippet.indexOf("def ")
      if (defIndex < 0) defIndex = sourceSnippet.indexOf("def\n")
      if (defIndex > 0) {
        modifierText = sourceSnippet.substring(0, defIndex)
      }
    }

    val (modifiers, _) = extractModifiersFromText(dd.mods, modifierText)

    val defKeywordPos = if (defIndex >= 0) cursor + defIndex + "def".length else cursor
    cursor = defKeywordPos

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

    val methodType = try { methodTypeOfTree(dd) } catch { case _: Exception => null }

    val name = new J.Identifier(
      Tree.randomId(),
      nameSpace,
      Markers.EMPTY,
      Collections.emptyList(),
      dd.name.toString,
      null,
      null
    )

    if (dd.nameSpan.exists) {
      cursor = Math.max(cursor, dd.nameSpan.end - offsetAdjustment)
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
      val bracketIdx = searchText.indexOf('[')
      if (bracketIdx >= 0) {
        val bracketSpace = if (bracketIdx > 0) Space.format(searchText.substring(0, bracketIdx)) else Space.EMPTY
        cursor = cursor + bracketIdx + 1

        val jTypeParams = new util.ArrayList[JRightPadded[J.TypeParameter]]()
        typeParams.foreach { tp =>
          val jtp = visitTypeParameter(tp)
          val afterParam = if (cursor < source.length) {
            val s = source.substring(cursor, Math.min(cursor + 20, source.length))
            val commaIdx = s.indexOf(',')
            if (commaIdx >= 0) {
              cursor = cursor + commaIdx + 1
              Space.format(s.substring(0, commaIdx))
            } else Space.EMPTY
          } else Space.EMPTY
          jTypeParams.add(new JRightPadded(jtp, afterParam, Markers.EMPTY))
        }

        val afterSearch = source.substring(cursor, Math.min(cursor + 50, source.length))
        val closeBracket = afterSearch.indexOf(']')
        if (closeBracket >= 0) {
          cursor = cursor + closeBracket + 1
        }

        new J.TypeParameters(Tree.randomId(), bracketSpace, Markers.EMPTY, Collections.emptyList(), jTypeParams)
      } else null
    } else null

    // Handle value parameters (x: Int, y: String)
    val parameters: JContainer[Statement] = if (valueParamLists.nonEmpty && valueParamLists.head.nonEmpty) {
      val params = valueParamLists.head.collect { case vd: Trees.ValDef[?] => vd }
      val searchEnd = Math.min(cursor + 50, source.length)
      val searchText = source.substring(cursor, searchEnd)
      val parenIdx = searchText.indexOf('(')
      val parenSpace = if (parenIdx > 0) Space.format(searchText.substring(0, parenIdx)) else Space.EMPTY
      if (parenIdx >= 0) cursor = cursor + parenIdx + 1

      val jParams = new util.ArrayList[JRightPadded[Statement]]()
      params.zipWithIndex.foreach { case (vd, idx) =>
        val param = visitMethodParameter(vd)
        val isLast = idx == params.size - 1
        val afterParam = if (!isLast) {
          // Find comma between this param end and next param start
          val paramEnd = Math.max(0, vd.span.end - offsetAdjustment)
          val nextParamStart = Math.max(0, params(idx + 1).span.start - offsetAdjustment)
          cursor = Math.max(cursor, paramEnd)
          if (cursor < nextParamStart && nextParamStart <= source.length) {
            val between = source.substring(cursor, nextParamStart)
            val commaIdx = between.indexOf(',')
            if (commaIdx >= 0) {
              val beforeComma = Space.format(between.substring(0, commaIdx))
              cursor = cursor + commaIdx + 1
              beforeComma
            } else Space.EMPTY
          } else Space.EMPTY
        } else Space.EMPTY
        jParams.add(new JRightPadded(param.asInstanceOf[Statement], afterParam, Markers.EMPTY))
      }

      // Find closing paren - search from cursor, skipping any brackets
      val lastParamEnd = if (params.nonEmpty) Math.max(0, params.last.span.end - offsetAdjustment) else cursor
      cursor = Math.max(cursor, lastParamEnd)
      if (cursor < source.length) {
        val remaining = source.substring(cursor, Math.min(cursor + 50, source.length))
        val closeParen = remaining.indexOf(')')
        if (closeParen >= 0) cursor = cursor + closeParen + 1
      }

      JContainer.build(parenSpace, jParams, Markers.EMPTY)
    } else if (valueParamLists.nonEmpty) {
      // Empty parameter list ()
      val searchEnd = Math.min(cursor + 50, source.length)
      val searchText = source.substring(cursor, searchEnd)
      val parenIdx = searchText.indexOf('(')
      val parenSpace = if (parenIdx > 0) Space.format(searchText.substring(0, parenIdx)) else Space.EMPTY
      if (parenIdx >= 0) {
        cursor = cursor + parenIdx + 1
        val afterSearch = source.substring(cursor, Math.min(cursor + 50, source.length))
        val closeParen = afterSearch.indexOf(')')
        if (closeParen >= 0) cursor = cursor + closeParen + 1
      }
      JContainer.build(parenSpace, new util.ArrayList[JRightPadded[Statement]](), Markers.EMPTY)
    } else {
      JContainer.empty[Statement]()
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
        val colonIdx = betweenText.indexOf(':')
        val equalsIdx = betweenText.indexOf('=')

        // Only use the type if there's a colon BEFORE the equals sign (explicit type annotation)
        if (colonIdx >= 0 && (equalsIdx < 0 || colonIdx < equalsIdx)) {
          cursor = cursor + colonIdx + 1
          visitTree(tpt) match {
            case tt: TypeTree => tt
            case id: J.Identifier => id
            case _ => null
          }
        } else {
          null // Inferred type — not written in source
        }
      case _ => null
    }

    // Handle method body
    val body: J.Block = dd.rhs match {
      case rhs if rhs != untpd.EmptyTree && rhs.span.exists =>
        val rhsStart = Math.max(0, rhs.span.start - offsetAdjustment)
        if (cursor < rhsStart && rhsStart <= source.length) {
          val beforeBody = source.substring(cursor, rhsStart)
          val equalsIdx = beforeBody.indexOf('=')
          if (equalsIdx >= 0) cursor = cursor + equalsIdx + 1
        }
        visitTree(rhs) match {
          case block: J.Block => block
          case expr: Expression =>
            // Wrap single expression in a synthetic block with OmitBraces marker
            val statements = new util.ArrayList[JRightPadded[Statement]]()
            statements.add(JRightPadded.build(expr.asInstanceOf[Statement]))
            new J.Block(
              Tree.randomId(),
              Space.EMPTY,
              Markers.build(Collections.singletonList(new org.openrewrite.scala.marker.OmitBraces(Tree.randomId()))),
              JRightPadded.build(false),
              statements,
              Space.EMPTY
            )
          case stmt: Statement =>
            // Wrap non-expression statement (e.g., match/switch) in a synthetic block
            val statements = new util.ArrayList[JRightPadded[Statement]]()
            statements.add(JRightPadded.build(stmt))
            new J.Block(
              Tree.randomId(),
              Space.EMPTY,
              Markers.build(Collections.singletonList(new org.openrewrite.scala.marker.OmitBraces(Tree.randomId()))),
              JRightPadded.build(false),
              statements,
              Space.EMPTY
            )
          case _ => null
        }
      case _ => null // Abstract method
    }

    updateCursor(dd.span.end)

    new J.MethodDeclaration(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      leadingAnnotations,
      modifiers,
      typeParameters,
      returnTypeExpression,
      new J.MethodDeclaration.IdentifierWithAnnotations(name, Collections.emptyList()),
      parameters,
      null, // throws
      body,
      null, // defaultValue
      methodType
    )
  }

  private def visitMethodParameter(vd: Trees.ValDef[?]): J = {
    val prefix = extractPrefix(vd.span)
    val paramSource = extractSource(vd.span)
    val hasExplicitType = paramSource.contains(":")

    val paramName = new J.Identifier(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      Collections.emptyList(),
      vd.name.toString,
      variableTypeOfTree(vd),
      null
    )

    if (vd.nameSpan.exists) {
      cursor = Math.max(cursor, vd.nameSpan.end - offsetAdjustment)
    }

    val typeExpr: TypeTree = if (hasExplicitType && vd.tpt != untpd.EmptyTree) {
      val colonSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 30, source.length)) else ""
      val colonIdx = colonSearch.indexOf(':')
      if (colonIdx >= 0) cursor = cursor + colonIdx + 1

      val result = visitTree(vd.tpt) match {
        case tt: TypeTree => tt
        case id: J.Identifier => id
        case _ => null
      }
      // Ensure cursor is past the type (including closing brackets for parameterized types)
      if (vd.tpt.span.exists) {
        updateCursor(vd.tpt.span.end)
      }
      result
    } else null

    // Handle default value
    val initializer: Expression = if (vd.rhs != untpd.EmptyTree && vd.rhs.span.exists) {
      val rhsStart = Math.max(0, vd.rhs.span.start - offsetAdjustment)
      if (cursor < rhsStart) {
        val before = source.substring(cursor, rhsStart)
        val eqIdx = before.indexOf('=')
        if (eqIdx >= 0) cursor = cursor + eqIdx + 1
      }
      visitTree(vd.rhs) match {
        case expr: Expression => expr
        case _ => null
      }
    } else null

    val variable = new J.VariableDeclarations.NamedVariable(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      paramName,
      Collections.emptyList(),
      if (initializer != null) JLeftPadded.build(initializer) else null,
      variableTypeOfTree(vd)
    )

    updateCursor(vd.span.end)

    new J.VariableDeclarations(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      Collections.emptyList(),
      Collections.emptyList(),
      typeExpr,
      null,
      Collections.emptyList(),
      Collections.singletonList(JRightPadded.build(variable))
    )
  }
  
  private def visitTryTree(tryTree: Trees.Try[?]): J = {
    val savedCursor = cursor
    try {
      visitTryImpl(tryTree)
    } catch {
      case _: Exception =>
        cursor = savedCursor
        visitUnknown(tryTree)
    }
  }

  private def visitTryImpl(tryTree: Trees.Try[?]): J.Try = {
    val prefix = extractPrefix(tryTree.span)

    // Advance cursor past "try" keyword
    val tryStart = Math.max(0, tryTree.span.start - offsetAdjustment)
    if (tryStart >= cursor && tryStart + 3 <= source.length) {
      cursor = tryStart + 3 // "try".length
    }

    // Visit the try body
    val body = visitTree(tryTree.expr) match {
      case block: J.Block => block
      case expr: Expression =>
        val stmts = new util.ArrayList[JRightPadded[Statement]]()
        stmts.add(JRightPadded.build(expr.asInstanceOf[Statement]))
        new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), stmts, Space.EMPTY)
      case _ => return visitUnknown(tryTree).asInstanceOf[J.Try]
    }

    // Handle catch cases
    val catches = new util.ArrayList[J.Try.Catch]()
    if (tryTree.cases.nonEmpty) {
      // Find "catch" keyword and opening brace
      val catchSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 50, source.length)) else ""
      val catchIdx = catchSearch.indexOf("catch")
      val catchSpace = if (catchIdx > 0) Space.format(catchSearch.substring(0, catchIdx)) else Space.EMPTY
      if (catchIdx >= 0) cursor = cursor + catchIdx + 5 // past "catch"

      // Find opening brace of catch block
      val braceSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 20, source.length)) else ""
      val braceIdx = braceSearch.indexOf('{')
      if (braceIdx >= 0) cursor = cursor + braceIdx + 1

      for (caseDef <- tryTree.cases) {
        val casePrefix = extractPrefix(caseDef.span)
        // Advance past "case" keyword
        val caseStart = Math.max(0, caseDef.span.start - offsetAdjustment)
        if (caseStart >= cursor) cursor = caseStart
        val caseSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 20, source.length)) else ""
        val caseKwIdx = caseSearch.indexOf("case")
        if (caseKwIdx >= 0) cursor = cursor + caseKwIdx + 4 // past "case"

        // Extract the pattern — everything between "case " and " =>"
        val patternSource = extractSource(caseDef.pat.span)
        val arrowSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 200, source.length)) else ""
        val arrowIdx = arrowSearch.indexOf("=>")

        // Create a variable declaration for the catch parameter
        val paramName = caseDef.pat match {
          case bind: Trees.Bind[?] => bind.name.toString
          case _ => "_"
        }
        val paramType = caseDef.pat match {
          case bind: Trees.Bind[?] => bind.body match {
            case typed: Trees.Typed[?] => visitTree(typed.tpt) match {
              case tt: TypeTree => tt
              case id: J.Identifier => id
              case _ => null
            }
            case _ => null
          }
          case typed: Trees.Typed[?] => visitTree(typed.tpt) match {
            case tt: TypeTree => tt
            case id: J.Identifier => id
            case _ => null
          }
          case _ => null
        }
        updateCursor(caseDef.pat.span.end)

        // Skip past =>
        if (arrowIdx >= 0) {
          val actualArrow = source.indexOf("=>", cursor)
          if (actualArrow >= 0) cursor = actualArrow + 2
        }

        val paramId = new J.Identifier(Tree.randomId(), Space.format(" "), Markers.EMPTY, Collections.emptyList(), paramName, null, null)
        val namedVar = new J.VariableDeclarations.NamedVariable(Tree.randomId(), Space.EMPTY, Markers.EMPTY, paramId, Collections.emptyList(), null, null)
        val varDecl = new J.VariableDeclarations(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
          Collections.emptyList(), Collections.emptyList(), paramType, null, Collections.emptyList(),
          Collections.singletonList(JRightPadded.build(namedVar)))
        val controlParens = new J.ControlParentheses[J.VariableDeclarations](Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(varDecl))

        // Visit the case body
        val caseBody = visitTree(caseDef.body) match {
          case block: J.Block => block
          case expr: Expression =>
            val stmts = new util.ArrayList[JRightPadded[Statement]]()
            stmts.add(JRightPadded.build(expr.asInstanceOf[Statement]))
            new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), stmts, Space.EMPTY)
          case stmt: Statement =>
            val stmts = new util.ArrayList[JRightPadded[Statement]]()
            stmts.add(JRightPadded.build(stmt))
            new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), stmts, Space.EMPTY)
          case _ =>
            new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), new util.ArrayList(), Space.EMPTY)
        }

        updateCursor(caseDef.span.end)
        catches.add(new J.Try.Catch(Tree.randomId(), casePrefix, Markers.EMPTY, controlParens, caseBody))
      }

      // Skip past closing brace of catch block
      if (cursor < source.length) {
        val remaining = source.substring(cursor, Math.min(cursor + 50, source.length))
        val closeIdx = remaining.indexOf('}')
        if (closeIdx >= 0) cursor = cursor + closeIdx + 1
      }
    }

    // Handle finally block
    val finallyBlock: JLeftPadded[J.Block] = if (!tryTree.finalizer.isEmpty && tryTree.finalizer.span.exists) {
      val finallySearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 50, source.length)) else ""
      val finallyIdx = finallySearch.indexOf("finally")
      val finallySpace = if (finallyIdx > 0) Space.format(finallySearch.substring(0, finallyIdx)) else Space.EMPTY
      if (finallyIdx >= 0) cursor = cursor + finallyIdx + 7 // past "finally"

      val finallyBody = visitTree(tryTree.finalizer) match {
        case block: J.Block => block
        case expr: Expression =>
          val stmts = new util.ArrayList[JRightPadded[Statement]]()
          stmts.add(JRightPadded.build(expr.asInstanceOf[Statement]))
          new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), stmts, Space.EMPTY)
        case _ => null
      }
      if (finallyBody != null) JLeftPadded.build(finallyBody).withBefore(finallySpace) else null
    } else null

    updateCursor(tryTree.span.end)

    new J.Try(Tree.randomId(), prefix, Markers.EMPTY, null, body, catches, finallyBlock)
  }

  private def visitMatchTree(matchTree: Trees.Match[?]): J = {
    val savedCursor = cursor
    try {
      visitMatchImpl(matchTree)
    } catch {
      case _: Exception =>
        cursor = savedCursor
        visitUnknown(matchTree)
    }
  }

  private def visitMatchImpl(matchTree: Trees.Match[?]): J = {
    val prefix = extractPrefix(matchTree.span)

    // Visit the selector expression
    val selector = visitTree(matchTree.selector) match {
      case expr: Expression => expr
      case _ => return visitUnknown(matchTree)
    }

    // Find "match" keyword
    val matchSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 30, source.length)) else ""
    val matchIdx = matchSearch.indexOf("match")
    if (matchIdx >= 0) cursor = cursor + matchIdx + 5 // past "match"

    // Find opening brace
    val braceSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 20, source.length)) else ""
    val braceIdx = braceSearch.indexOf('{')
    if (braceIdx >= 0) cursor = cursor + braceIdx + 1

    // Visit each case
    val caseStatements = new util.ArrayList[JRightPadded[Statement]]()
    for (caseDef <- matchTree.cases) {
      val casePrefix = extractPrefix(caseDef.span)

      // Advance past "case" keyword
      val caseStart = Math.max(0, caseDef.span.start - offsetAdjustment)
      if (caseStart >= cursor) cursor = caseStart
      val caseSearch = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 20, source.length)) else ""
      val caseKwIdx = caseSearch.indexOf("case")
      if (caseKwIdx >= 0) cursor = cursor + caseKwIdx + 4 // past "case"

      // Visit the pattern as a label
      val patternJ = visitTree(caseDef.pat) match {
        case j: J => j
        case _ => new J.Identifier(Tree.randomId(), Space.format(" "), Markers.EMPTY, Collections.emptyList(), "_", null, null)
      }
      val labels = new util.ArrayList[JRightPadded[J]]()
      labels.add(JRightPadded.build(patternJ))

      // Skip past =>
      val arrowSearch2 = if (cursor < source.length) source.substring(cursor, Math.min(cursor + 200, source.length)) else ""
      val arrowIdx2 = arrowSearch2.indexOf("=>")
      if (arrowIdx2 >= 0) {
        val actualArrow = source.indexOf("=>", cursor)
        if (actualArrow >= 0) cursor = actualArrow + 2
      }

      // Visit the case body
      val caseBodyJ = visitTree(caseDef.body) match {
        case j: J => JRightPadded.build(j)
        case _ => null
      }

      updateCursor(caseDef.span.end)

      val jCase = new J.Case(Tree.randomId(), casePrefix, Markers.EMPTY,
        J.Case.Type.Rule,
        null, // deprecated pattern
        null, // deprecated expressions
        JContainer.build(Space.EMPTY, labels, Markers.EMPTY), // caseLabels
        null, // guard
        JContainer.empty(), // statements (empty for Rule type)
        caseBodyJ)
      caseStatements.add(JRightPadded.build(jCase.asInstanceOf[Statement]))
    }

    // Find closing brace
    if (cursor < source.length) {
      val remaining = source.substring(cursor, Math.min(cursor + 100, source.length))
      val closeIdx = remaining.indexOf('}')
      val endSpace = if (closeIdx > 0) Space.format(remaining.substring(0, closeIdx)) else Space.EMPTY
    }

    // Build the cases block
    val casesBlock = new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
      JRightPadded.build(false), caseStatements, Space.EMPTY)

    updateCursor(matchTree.span.end)

    // Wrap selector in ControlParentheses for J.Switch
    val selectorParens = new J.ControlParentheses[Expression](Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(selector))

    new J.Switch(Tree.randomId(), prefix, Markers.EMPTY, selectorParens, casesBlock)
  }

  private def visitUnknown(tree: Trees.Tree[?]): J.Unknown = {
    val prefix = extractPrefix(tree.span)
    val sourceText = extractSource(tree.span)
    
    // Debug what's being marked as unknown
    if (sourceText.contains("greet") || sourceText.contains("_")) {
    }
    
    // Debug: Check if this is a New node
    if (tree.isInstanceOf[Trees.New[?]]) {
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

  private def extractModifiersFromText(mods: untpd.Modifiers, modifierText: String): (util.ArrayList[J.Modifier], Int) = {
    import dotty.tools.dotc.core.Flags
    val modifierList = new util.ArrayList[J.Modifier]()
    
    // The order matters - we'll add them in the order they appear in source
    val modifierKeywords = List(
      ("private", Flags.Private, J.Modifier.Type.Private),
      ("protected", Flags.Protected, J.Modifier.Type.Protected),
      ("abstract", Flags.Abstract, J.Modifier.Type.Abstract),
      ("final", Flags.Final, J.Modifier.Type.Final),
      ("override", Flags.Override, J.Modifier.Type.LanguageExtension),
      ("implicit", Flags.Implicit, J.Modifier.Type.LanguageExtension),
      ("sealed", Flags.Sealed, J.Modifier.Type.Sealed),
      ("lazy", Flags.Lazy, J.Modifier.Type.LanguageExtension)
      // Skip "case" for now - needs special handling
    )
    
    // Create a list of (position, keyword, type) for modifiers that are present.
    // Check both the compiler flags AND the source text - qualified access like
    // private[testing] may not always set the simple Private/Protected flag.
    val presentModifiers = modifierKeywords.flatMap { case (keyword, flag, modType) =>
      val pos = findKeyword(modifierText, keyword)
      if (pos >= 0 && (mods.is(flag) || modifierText.contains(keyword))) {
        Some((pos, keyword, modType))
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
      
      // Check for scope qualifier like private[testing] or protected[this]
      var fullKeyword = keyword
      var keywordLen = keyword.length
      if ((keyword == "private" || keyword == "protected") && pos + keywordLen < modifierText.length) {
        val afterKeyword = modifierText.substring(pos + keywordLen)
        if (afterKeyword.startsWith("[")) {
          val closeBracket = afterKeyword.indexOf(']')
          if (closeBracket >= 0) {
            fullKeyword = keyword + afterKeyword.substring(0, closeBracket + 1)
            keywordLen = keyword.length + closeBracket + 1
          }
        }
      }

      modifierList.add(new J.Modifier(
        Tree.randomId(),
        spaceBefore,
        Markers.EMPTY,
        fullKeyword,
        modType,
        Collections.emptyList()
      ))

      lastEnd = pos + keywordLen
    }
    
    // Don't advance cursor here - callers handle cursor positioning
    // based on the classIndex/keyword position they've already computed.
    
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
      val bracketStart = source.indexOf('[', nameStart)
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
    val name = new J.Identifier(
      Tree.randomId(),
      namePrefix,
      Markers.EMPTY,
      Collections.emptyList(),
      nameStr,
      null,
      null
    )
    
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
          // Model type bounds as TypeBound elements.
          // TODO: Context bound part should desugar to implicit param.
          val boundList = new util.ArrayList[JRightPadded[TypeTree]]()
          if (!innerBounds.lo.isEmpty) {
            val loOpIdx = source.indexOf(">:", cursor)
            val loPrefix = if (loOpIdx > cursor) Space.format(source.substring(cursor, loOpIdx)) else Space.EMPTY
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
            val hiOpIdx = source.indexOf("<:", cursor)
            val hiPrefix = if (hiOpIdx > cursor) Space.format(source.substring(cursor, hiOpIdx)) else Space.EMPTY
            if (hiOpIdx >= 0) cursor = hiOpIdx + 2
            val savedC = cursor
            visitTree(innerBounds.hi) match {
              case tt: TypeTree =>
                val hiBound: TypeTree = new J.TypeBound(Tree.randomId(), hiPrefix, Markers.EMPTY, J.TypeBound.Kind.Upper, tt)
                boundList.add(JRightPadded.build(hiBound))
              case _ => cursor = savedC
            }
          }
          if (cb.span.exists) updateCursor(cb.span.end)
          if (!boundList.isEmpty) JContainer.build(Space.EMPTY, boundList, Markers.EMPTY) else null
        } else if (cxBoundsList.nonEmpty) {
          val boundList = new util.ArrayList[JRightPadded[TypeTree]]()
          // For each context bound, extract the bound type name
          cxBoundsList.foreach { cxBound =>
            val boundName = cxBound match {
              case cbt: untpd.ContextBoundTypeTree =>
                cbt.tycon match {
                  case id: Trees.Ident[?] => id.name.toString
                  case sel: Trees.Select[?] => sel.name.toString
                  case _ => cxBound.toString
                }
              case _ => cxBound.toString
            }
            val boundId: TypeTree = new J.Identifier(
              Tree.randomId(), Space.format(" "), Markers.EMPTY,
              Collections.emptyList(), boundName, null, null)
            boundList.add(JRightPadded.build(boundId))
          }
          // Advance cursor past the entire bounds expression in source
          // The ContextBounds span should cover `: Ordering` text
          if (cb.span.exists) {
            updateCursor(cb.span.end)
          }
          // The JContainer.before space is printed BEFORE the `:` by the printer.
          JContainer.build(Space.EMPTY, boundList, Markers.EMPTY)
        } else null

      case tb: Trees.TypeBoundsTree[?] if !tb.hi.isEmpty || !tb.lo.isEmpty =>
        // Upper and/or lower bounds: [T <: Comparable], [T >: Null], [T >: Lower <: Upper]
        val boundList = new util.ArrayList[JRightPadded[TypeTree]]()

        // Lower bound first (if present) — appears first in source for [T >: L <: H]
        if (!tb.lo.isEmpty) {
          val loOpIdx = source.indexOf(">:", cursor)
          val loPrefix = if (loOpIdx > cursor) Space.format(source.substring(cursor, loOpIdx)) else Space.EMPTY
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
          val hiOpIdx = source.indexOf("<:", cursor)
          val hiPrefix = if (hiOpIdx > cursor) Space.format(source.substring(cursor, hiOpIdx)) else Space.EMPTY
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
  
  private def extractConstructorParametersSource(td: Trees.TypeDef[?]): String = {
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
  
  private def createPrimaryConstructor(constructorParams: List[Trees.ValDef[?]], template: Trees.Template[?]): J.MethodDeclaration = {
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
  
  private def visitTyped(typed: Trees.Typed[?]): J = {
    
    // Check if this is a member reference pattern (expr _)
    typed.tpt match {
      case id: Trees.Ident[?] if id.name.toString == "_" =>
        // This is a member reference like "greet _"
        val prefix = extractPrefix(typed.span)
        
        // Visit the expression part (the method/field being referenced)
        val expr = visitTree(typed.expr) match {
          case e: Expression => e
          case _ => return visitUnknown(typed)
        }
        
        // Create a member reference
        new J.MemberReference(
          Tree.randomId(),
          prefix,
          Markers.EMPTY,
          JRightPadded.build(expr),
          null, // No type parameters for now
          JLeftPadded.build(new J.Identifier(
            Tree.randomId(),
            Space.SINGLE_SPACE,
            Markers.EMPTY,
            Collections.emptyList(),
            "_",
            typeFor(typed.span),
            null
          )),
          typeFor(typed.span),
          methodTypeFor(typed.span),
          variableTypeFor(typed.span)
        )
      case _ =>
        // Other typed expressions - for now treat as unknown
        visitUnknown(typed)
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
      // Check if the source contains actual underscore placeholders
      val funcSource = extractSource(func.span)
      val hasUnderscorePlaceholder = funcSource.contains("_")
      
      // If we have synthetic params and underscore in source, it's likely a placeholder lambda
      // These should be treated as regular lambdas but we skip the synthetic param
      if (hasUnderscorePlaceholder) {
        func.body match {
          case app: Trees.Apply[?] =>
            // This might be a partially applied function like add(5, _)
            // Check if it's a method invocation with underscore arguments
            var hasPartialApplication = false
            app.args.foreach {
              case id: Trees.Ident[?] if syntheticParams.contains(id.name.toString) =>
                hasPartialApplication = true
              case _ =>
            }
            
            if (hasPartialApplication) {
              // Partially applied function - return the method invocation
              val prefix = extractPrefix(func.span)
              val result = visitApply(app)
              result match {
                case mi: J.MethodInvocation => return mi.withPrefix(prefix)
                case _ => return result
              }
            }
          case _ =>
            // For other cases like `_ * 2`, this is an underscore placeholder lambda
            // We need to create a proper lambda with S.Wildcard in the body
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
              Markers.build(Collections.singletonList(new UnderscorePlaceholderLambda(UUID.randomUUID()))),
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
    
    // Check if parameters are parenthesized by looking at the source
    val funcSource = extractSource(func.span)
    hasParentheses = funcSource.trim.startsWith("(")
    val arrowIndex = funcSource.indexOf("=>")
    
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
          val commaIdx = between.indexOf(',')
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
          val commaIdx = searchStr.indexOf(',')
          if (commaIdx >= 0) {
            afterSpace = Space.format(searchStr.substring(0, commaIdx))
            cursor = searchStart + commaIdx + 1
          }
        }
      } else {
        // Last parameter, look for space before closing paren
        val paramEnd = param.span.end - offsetAdjustment
        if (paramEnd >= cursor) {
          cursor = paramEnd
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
    
    // Extract arrow and spacing (arrowIndex already computed above)
    var arrowPrefix = Space.EMPTY
    if (arrowIndex >= 0) {
      // Find the space before =>
      var spaceStart = arrowIndex - 1
      while (spaceStart >= 0 && Character.isWhitespace(funcSource.charAt(spaceStart))) {
        spaceStart -= 1
      }
      if (spaceStart < arrowIndex - 1) {
        arrowPrefix = Space.format(funcSource.substring(spaceStart + 1, arrowIndex))
      }
      // Move cursor to right after the arrow (past =>)
      val arrowEndPos = func.span.start + arrowIndex + 2 - offsetAdjustment
      cursor = arrowEndPos
    }
    
    // Visit the lambda body - it will extract its own prefix including space after =>
    val body = visitTree(func.body)
    
    new J.Lambda(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
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