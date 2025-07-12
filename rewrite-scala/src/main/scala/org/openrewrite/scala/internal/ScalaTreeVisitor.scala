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
import org.openrewrite.scala.marker.Implicit
import org.openrewrite.scala.marker.OmitBraces
import org.openrewrite.scala.marker.SObject

import java.util
import java.util.{Collections, Arrays}

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
        // Extract the source for the opening bracket
        val typeParamsSource = extractTypeParametersSource(td)
        val openingBracketSpace = if (typeParamsSource.startsWith("[")) {
          Space.EMPTY
        } else {
          Space.format(" ")
        }
        
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
          
          for (i <- 1 until template.parents.size) {
            val parent = template.parents(i)
            
            // Look for "with" keyword before this parent
            var withSpace = Space.EMPTY
            if (parent.span.exists && i > 0) {
              val prevParentEnd = template.parents(i - 1).span.end - offsetAdjustment
              val thisParentStart = parent.span.start - offsetAdjustment
              if (prevParentEnd < thisParentStart && thisParentStart <= source.length) {
                val betweenText = source.substring(prevParentEnd, thisParentStart)
                val withIndex = betweenText.indexOf("with")
                if (withIndex >= 0) {
                  withSpace = Space.format(betweenText.substring(0, withIndex))
                  // Update cursor past "with"
                  cursor = prevParentEnd + withIndex + "with".length
                }
              }
            }
            
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
            
            // Extract space between "with" and the type
            val implTypeSpace = if (cursor < parent.span.start - offsetAdjustment) {
              Space.format(source.substring(cursor, parent.span.start - offsetAdjustment))
            } else {
              Space.format(" ")
            }
            
            // Convert to TypeTree
            val implType: TypeTree = implTypeExpr match {
              case id: J.Identifier =>
                TypeTree.build(id.getSimpleName).asInstanceOf[Expression].withPrefix(implTypeSpace).asInstanceOf[TypeTree]
              case fieldAccess: J.FieldAccess =>
                fieldAccess.withPrefix(implTypeSpace)
              case unknown: J.Unknown =>
                unknown.withPrefix(implTypeSpace)
              case other =>
                new J.Unknown(
                  Tree.randomId(),
                  implTypeSpace,
                  Markers.EMPTY,
                  new J.Unknown.Source(
                    Tree.randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    other.toString
                  )
                )
            }
            
            implementsList.add(
              JRightPadded.build(implType)
            )
            
            // Update cursor
            if (parent.span.exists) {
              cursor = Math.max(cursor, parent.span.end - offsetAdjustment)
            }
          }
          
          if (!implementsList.isEmpty) {
            implementings = JContainer.build(
              Space.EMPTY, // No space before implements in Scala
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
        // Check if there's a body in the source
        if (td.span.exists) {
          val classEnd = Math.max(0, td.span.end - offsetAdjustment)
          if (cursor < classEnd && classEnd <= source.length) {
            val afterCursor = source.substring(cursor, classEnd)
            afterCursor.contains("{")
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
                Space.EMPTY
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
  
  private def visitTypeParameter(tparam: untpd.TypeDef): J.TypeParameter = {
    val prefix = extractPrefix(tparam.span)
    
    // Extract the type parameter name
    val name = new J.Identifier(
      Tree.randomId(),
      Space.EMPTY,
      Markers.EMPTY,
      Collections.emptyList(),
      tparam.name.toString,
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