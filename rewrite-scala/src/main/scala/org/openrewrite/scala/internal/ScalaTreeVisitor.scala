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
import dotty.tools.dotc.util.Spans
import org.openrewrite.Tree
import org.openrewrite.java.tree.*
import org.openrewrite.marker.Markers

import java.util
import java.util.Collections

/**
 * Visitor that traverses the Scala compiler AST and builds OpenRewrite LST nodes.
 */
class ScalaTreeVisitor(source: String, offsetAdjustment: Int = 0)(implicit ctx: Context) {
  
  private var cursor = 0
  
  def updateCursor(position: Int): Unit = {
    val adjustedPosition = Math.max(0, position - offsetAdjustment)
    if (adjustedPosition > cursor && adjustedPosition <= source.length) {
      cursor = adjustedPosition
    }
  }
  
  def visitTree(tree: untpd.Tree): J = tree match {
      case _ if tree.isEmpty => visitUnknown(tree)
      case lit: untpd.Literal => visitLiteral(lit)
      case num: untpd.Number => visitNumber(num)
      case id: untpd.Ident => visitIdent(id)
      case app: untpd.Apply => visitApply(app)
      case sel: untpd.Select => visitSelect(sel)
      case parens: untpd.Parens => visitParentheses(parens)
      case imp: untpd.Import => visitImport(imp)
      case pkg: untpd.PackageDef => visitPackageDef(pkg)
      case vd: untpd.ValDef => visitValDef(vd)
      case md: untpd.ModuleDef => visitModuleDef(md)
      case asg: untpd.Assign => visitAssign(asg)
      case ifTree: untpd.If => visitIf(ifTree)
      case whileTree: untpd.WhileDo => visitWhileDo(whileTree)
      case forTree: untpd.ForDo => visitForDo(forTree)
      case block: untpd.Block => visitBlock(block)
      case td: untpd.TypeDef if td.isClassDef => visitClassDef(td)
      case dd: untpd.DefDef => visitDefDef(dd)
      case _ => visitUnknown(tree)
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
    // In Scala, binary operations like "1 + 2" are parsed as Apply(Select(1, +), List(2))
    // Unary operations like "-x" are parsed as Apply(Select(x, unary_-), List())
    app.fun match {
      case sel: untpd.Select if app.args.isEmpty && isUnaryOperator(sel.name.toString) =>
        // This is a unary operation
        visitUnary(sel)
      case sel: untpd.Select if app.args.length == 1 && isBinaryOperator(sel.name.toString) =>
        // This is likely a binary operation (infix notation)
        // This is likely a binary operation (infix notation)
        visitBinary(sel, app.args.head, Some(app.span))
      case sel: untpd.Select =>
        // Method call with dot notation like "1.+(2)"
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
    val prefix = extractPrefix(app.span)
    
    // Handle the method call target
    val (select: Expression, methodName: String, typeParams: java.util.List[Expression]) = app.fun match {
      case sel: untpd.Select =>
        // Method call like obj.method(...) or package.Class.method(...)
        val target = visitTree(sel.qualifier) match {
          case expr: Expression => expr
          case _ => return visitUnknown(app)
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
    
    // Visit arguments
    val args = new util.ArrayList[JRightPadded[Expression]]()
    for (i <- app.args.indices) {
      val arg = app.args(i)
      visitTree(arg) match {
        case expr: Expression => 
          // For now, assume no trailing comma or space after each argument
          // This will need refinement to handle formatting properly
          val rightPadded = JRightPadded.build(expr)
          args.add(rightPadded)
        case _ => return visitUnknown(app) // If any argument fails, fall back
      }
    }
    
    // Extract space before the method name (after the dot if there's a select)
    val nameSpace = if (select != null) {
      app.fun match {
        case sel: untpd.Select =>
          val qualifierEnd = sel.qualifier.span.end
          val nameStart = sel.nameSpan.start
          if (qualifierEnd < nameStart) {
            val dotStart = Math.max(0, qualifierEnd - offsetAdjustment)
            val nameStartAdjusted = Math.max(0, nameStart - offsetAdjustment)
            if (dotStart < nameStartAdjusted && dotStart >= cursor && nameStartAdjusted <= source.length) {
              val between = source.substring(dotStart, nameStartAdjusted)
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
        case _ => Space.EMPTY
      }
    } else {
      Space.EMPTY
    }
    
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
      Space.EMPTY,  // space before '('
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
  
  private def visitParentheses(parens: untpd.Parens): J = {
    // Since we know Parens exist but couldn't find the direct accessor,
    // let's handle it through the Unknown mechanism for now
    // This preserves the parentheses in the output
    visitUnknown(parens)
  }
  
  private def visitImport(imp: untpd.Import): J = {
    // For now, keep imports as Unknown until we resolve the cursor management issues
    // The problem is complex:
    // 1. Import nodes are being visited multiple times
    // 2. Field access building is consuming source incorrectly
    // 3. Prefix extraction is duplicating the "import" keyword
    visitUnknown(imp)
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
    // For now, preserve val/var declarations as Unknown to maintain exact formatting
    // This will be replaced with proper S.ValDeclaration or similar in the future
    visitUnknown(vd)
  }
  
  private def visitModuleDef(md: untpd.ModuleDef): J = {
    // For now, preserve object declarations as Unknown to maintain exact formatting
    // This will be replaced with proper object support in the future
    visitUnknown(md)
  }
  
  private def visitAssign(asg: untpd.Assign): J = {
    // For now, preserve assignments as Unknown to maintain exact formatting
    // This will be replaced with proper assignment support in the future
    visitUnknown(asg)
  }
  
  private def visitIf(ifTree: untpd.If): J.If = {
    val prefix = extractPrefix(ifTree.span)
    
    // Visit the condition expression
    val condition = visitTree(ifTree.cond) match {
      case expr: Expression => expr
      case _ => return visitUnknown(ifTree).asInstanceOf[J.If]
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
      visitTree(ifTree.elsep) match {
        case stmt: Statement => 
          new J.If.Else(
            Tree.randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            JRightPadded.build(stmt)
          )
        case _ => return visitUnknown(ifTree).asInstanceOf[J.If]
      }
    }
    
    // Update cursor to end of the if expression
    if (ifTree.span.exists) {
      val adjustedEnd = Math.max(0, ifTree.span.end - offsetAdjustment)
      if (adjustedEnd > cursor && adjustedEnd <= source.length) {
        cursor = adjustedEnd
      }
    }
    
    new J.If(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      new J.ControlParentheses(
        Tree.randomId(),
        Space.EMPTY,
        Markers.EMPTY,
        JRightPadded.build(condition)
      ),
      thenPart,
      elsePart
    )
  }
  
  private def visitWhileDo(whileTree: untpd.WhileDo): J.WhileLoop = {
    val prefix = extractPrefix(whileTree.span)
    
    // Visit the condition expression
    val condition = visitTree(whileTree.cond) match {
      case expr: Expression => expr
      case _ => return visitUnknown(whileTree).asInstanceOf[J.WhileLoop]
    }
    
    // Visit the body
    val body = visitTree(whileTree.body) match {
      case stmt: Statement => JRightPadded.build(stmt)
      case _ => return visitUnknown(whileTree).asInstanceOf[J.WhileLoop]
    }
    
    // Update cursor to end of the while loop
    if (whileTree.span.exists) {
      val adjustedEnd = Math.max(0, whileTree.span.end - offsetAdjustment)
      if (adjustedEnd > cursor && adjustedEnd <= source.length) {
        cursor = adjustedEnd
      }
    }
    
    new J.WhileLoop(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      new J.ControlParentheses(
        Tree.randomId(),
        Space.EMPTY,
        Markers.EMPTY,
        JRightPadded.build(condition)
      ),
      body
    )
  }
  
  private def visitForDo(forTree: untpd.ForDo): J = {
    // For loops in Scala are quite complex with generators, guards, and definitions
    // For now, preserve them as Unknown nodes until we can properly model them
    // This will be replaced with proper S.ForComprehension support in the future
    visitUnknown(forTree)
  }
  
  private def visitBlock(block: untpd.Block): J.Block = {
    val prefix = extractPrefix(block.span)
    val statements = new util.ArrayList[JRightPadded[Statement]]()
    
    // Visit all statements in the block
    for (stat <- block.stats) {
      visitTree(stat) match {
        case null => // Skip null statements (e.g., package declarations)
        case stmt: Statement => 
          statements.add(JRightPadded.build(stmt))
        case unknown: J.Unknown =>
          // For now, skip unknown nodes in blocks
          // In the future we could wrap them in a statement
        case _ => // Skip non-statement nodes
      }
    }
    
    // Handle the expression part of the block (if any)
    if (!block.expr.isEmpty) {
      visitTree(block.expr) match {
        case stmt: Statement => 
          statements.add(JRightPadded.build(stmt))
        case unknown: J.Unknown =>
          // For now, skip unknown nodes in blocks
        case _ => // Skip
      }
    }
    
    // Update cursor to end of the block
    if (block.span.exists) {
      val adjustedEnd = Math.max(0, block.span.end - offsetAdjustment)
      if (adjustedEnd > cursor && adjustedEnd <= source.length) {
        cursor = adjustedEnd
      }
    }
    
    new J.Block(
      Tree.randomId(),
      prefix,
      Markers.EMPTY,
      JRightPadded.build(false), // not static
      statements,
      Space.EMPTY
    )
  }
  
  private def visitClassDef(td: untpd.TypeDef): J.ClassDeclaration = {
    val prefix = extractPrefix(td.span)
    
    // Extract the source text to find modifiers and class keyword
    val adjustedStart = Math.max(0, td.span.start - offsetAdjustment)
    val adjustedEnd = Math.max(0, td.span.end - offsetAdjustment)
    var modifierText = ""
    var classIndex = -1
    
    if (adjustedStart >= cursor && adjustedEnd <= source.length) {
      val sourceSnippet = source.substring(cursor, adjustedEnd)
      classIndex = sourceSnippet.indexOf("class")
      if (classIndex > 0) {
        modifierText = sourceSnippet.substring(0, classIndex)
      }
    }
    
    // Extract modifiers
    val (modifiers, lastModEnd) = extractModifiersFromText(td.mods, modifierText)
    
    // Find where "class" keyword ends
    val classKeywordPos = if (classIndex >= 0) {
      cursor + classIndex + "class".length
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
    val kindPrefix = if (!modifiers.isEmpty && classIndex > 0 && lastModEnd < classIndex) {
      Space.format(modifierText.substring(lastModEnd, classIndex))
    } else {
      Space.EMPTY
    }
    
    val kind = new J.ClassDeclaration.Kind(
      Tree.randomId(),
      kindPrefix,
      Markers.EMPTY,
      Collections.emptyList(),
      J.ClassDeclaration.Kind.Type.Class
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
    
    // For now, no type parameters, no primary constructor, no extends/implements
    val typeParameters: JContainer[J.TypeParameter] = null
    val primaryConstructor: JContainer[Statement] = null
    val extendings: JLeftPadded[TypeTree] = null
    val implementings: JContainer[TypeTree] = null
    
    // Handle the body - TypeDef has rhs which should be a Template for classes
    // For classes without explicit body, we should NOT print empty braces
    val (hasExplicitBody, bodyPrefix) = td.rhs match {
      case template: untpd.Template =>
        // Check if there's a body in the source
        if (td.span.exists && td.nameSpan.exists) {
          val nameEnd = Math.max(0, td.nameSpan.end - offsetAdjustment)
          val classEnd = Math.max(0, td.span.end - offsetAdjustment)
          if (nameEnd < classEnd && nameEnd >= 0 && classEnd <= source.length) {
            val afterName = source.substring(nameEnd, classEnd)
            val braceIndex = afterName.indexOf("{")
            if (braceIndex >= 0) {
              // Extract space before the opening brace
              val spaceBeforeBrace = Space.format(afterName.substring(0, braceIndex))
              // Update cursor to after the opening brace
              cursor = nameEnd + braceIndex + 1
              (true, spaceBeforeBrace)
            } else {
              (false, Space.EMPTY)
            }
          } else {
            (false, Space.EMPTY)
          }
        } else {
          (false, Space.EMPTY)
        }
      case _ => (false, Space.EMPTY)
    }
    
    val body = if (hasExplicitBody) {
      td.rhs match {
        case template: untpd.Template =>
          // Visit the template body to get statements
          val statements = new util.ArrayList[JRightPadded[Statement]]()
          
          // Visit each statement in the template body
          for (stat <- template.body) {
            visitTree(stat) match {
              case null => // Skip null statements
              case stmt: Statement => 
                statements.add(JRightPadded.build(stmt))
              case _ => // Skip non-statement nodes
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
          // Fallback
          new J.Block(
            Tree.randomId(),
            bodyPrefix,
            Markers.EMPTY,
            JRightPadded.build(false),
            Collections.emptyList(),
            Space.EMPTY
          )
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
      null  // type
    )
  }
  
  private def visitDefDef(dd: untpd.DefDef): J = {
    // For now, preserve method declarations as Unknown to maintain exact formatting
    // This will be replaced with proper method declaration support in the future
    visitUnknown(dd)
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
      // Debug output
      // Debug output removed
      result
    } else {
      ""
    }
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