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
import dotty.tools.dotc.core.Contexts.*
import dotty.tools.dotc.parsing.Parsers
import dotty.tools.dotc.util.SourceFile
import dotty.tools.dotc.reporting.{Diagnostic, Reporter}
import dotty.tools.dotc.{CompilationUnit, Run, Compiler, Driver}
import dotty.tools.dotc.config.ScalaSettings
import scala.collection.mutable.ListBuffer
import java.util.{ArrayList, List => JList}

/**
 * Bridge to the Scala 3 (Dotty) compiler for parsing Scala source files.
 * This class provides a Java-friendly interface to the Scala compiler API.
 */
class ScalaCompilerBridge {
  
  // Create a custom driver class to access protected members
  private class ParsingDriver extends Driver {
    def getInitialContext: Context = initCtx
  }
  
  /**
   * Parses a Scala source file and returns the parsed AST along with any warnings.
   */
  def parse(path: String, content: String): ScalaParseResult = {
    // Create a custom reporter to collect warnings
    val warnings = new ListBuffer[ScalaWarning]()
    
    // Create our custom driver and get a proper context
    val driver = new ParsingDriver()
    given Context = driver.getInitialContext
    
    // For simple expressions, wrap in a valid compilation unit
    val (adjustedContent, needsUnwrap) = if (isSimpleExpression(content)) {
      (s"object ExprWrapper { val result = $content }", true)
    } else {
      (content, false)
    }
    
    // Create source file
    val source = SourceFile.virtual(path, adjustedContent)
    
    // Parse the source
    val unit = CompilationUnit(source)
    val parser = new Parsers.Parser(source)(using ctx.fresh.setCompilationUnit(unit))
    
    val tree = parser.parse()
    
    // If we wrapped the expression, extract it
    val finalTree = if (needsUnwrap && !tree.isEmpty) {
      extractExpression(tree).getOrElse(tree)
    } else {
      tree
    }
    
    // Convert warnings to Java list
    val javaWarnings = new ArrayList[ScalaWarning]()
    warnings.foreach(javaWarnings.add)
    
    ScalaParseResult(finalTree, javaWarnings, needsUnwrap)
  }
  
  private def extractExpression(tree: untpd.Tree)(using Context): Option[untpd.Tree] = tree match {
    case pkgDef: untpd.PackageDef =>
      pkgDef.stats.collectFirst {
        case mod: untpd.ModuleDef if mod.name.toString == "ExprWrapper" =>
          mod.impl.body.collectFirst {
            case vd: untpd.ValDef if vd.name.toString == "result" => 
              // The rhs (right-hand side) is the expression we want
              vd.rhs
          }
      }.flatten
    case _ => None
  }
  
  private def isSimpleExpression(content: String): Boolean = {
    val trimmed = content.trim
    // Check if it's likely a simple expression (doesn't start with keywords that indicate declarations)
    // Also check if it contains multiple lines, which would indicate a block of statements
    val hasMultipleLines = trimmed.contains('\n')
    
    // Check for postfix operators - they need special handling
    val hasPostfixOperator = trimmed.matches(".*[a-zA-Z0-9_)]\\s*[!?]\\s*$")
    
    !hasMultipleLines &&
    !hasPostfixOperator &&
    !trimmed.startsWith("package") && 
    !trimmed.startsWith("import") &&
    !trimmed.startsWith("class") &&
    !trimmed.startsWith("object") &&
    !trimmed.startsWith("trait") &&
    !trimmed.startsWith("def") &&
    !trimmed.startsWith("val") &&
    !trimmed.startsWith("var") &&
    !trimmed.startsWith("type") &&
    !trimmed.startsWith("private") &&
    !trimmed.startsWith("protected") &&
    !trimmed.startsWith("public") &&
    !trimmed.startsWith("final") &&
    !trimmed.startsWith("lazy") &&
    !trimmed.startsWith("implicit") &&
    !trimmed.startsWith("//") &&
    !trimmed.startsWith("/*") &&
    trimmed.nonEmpty
  }
}

/**
 * Result of parsing a Scala source file.
 */
case class ScalaParseResult(tree: untpd.Tree, warnings: JList[ScalaWarning], wasWrapped: Boolean = false)

/**
 * Represents a warning or error from the Scala compiler.
 */
case class ScalaWarning(message: String, line: Int, column: Int, level: String)