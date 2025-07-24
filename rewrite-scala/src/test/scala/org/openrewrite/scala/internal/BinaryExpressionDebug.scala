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

object BinaryExpressionDebug {
  def main(args: Array[String]): Unit = {
    val bridge = new ScalaCompilerBridge()
    val converter = new ScalaASTConverter()
    
    val source = "1.+(2)"
    println(s"Testing: $source")
    
    val parseResult = bridge.parse("test.scala", source)
    println(s"Was wrapped: ${parseResult.wasWrapped}")
    
    // Get compilation unit result
    val result = converter.convertToCompilationUnit(parseResult, source)
    val statements = result.getStatements
    println(s"Number of statements: ${statements.size()}")
    
    // Get remaining source
    val remaining = converter.getRemainingSource(parseResult, source, result.getLastCursorPosition)
    println(s"Remaining source: '$remaining'")
    println(s"Remaining length: ${remaining.length}")
  }
}