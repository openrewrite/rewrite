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

import org.junit.jupiter.api.Test
import org.openrewrite.scala.ScalaParser
import org.openrewrite.java.tree.*
import org.openrewrite.scala.tree.*

class ExtendsDebugTest {
  
  @Test
  def debugExtendsSpacing(): Unit = {
    val source = """class Dog extends Animal"""
    
    val parser = new ScalaParser.Builder().build()
    import scala.jdk.StreamConverters._
    import scala.jdk.CollectionConverters._
    
    val results = parser.parse(source).toScala(List)
    if (results.nonEmpty) {
      val result = results.head
      
      println(s"Result type: ${result.getClass.getName}")
      
      result match {
        case pe: org.openrewrite.tree.ParseError =>
          println(s"Parse error occurred:")
          println(s"  - Source path: ${pe.getSourcePath}")
          println(s"  - Text: ${pe.getText}")
          pe.getMarkers.getMarkers.forEach { marker =>
            println(s"  - Marker: ${marker}")
          }
        case scu: S.CompilationUnit =>
          println(s"Scala Compilation Unit found")
          println(s"  - Statements count: ${scu.getStatements.size()}")
          
          scu.getStatements.asScala.foreach { 
            case cls: J.ClassDeclaration =>
              println(s"\nClass Declaration: ${cls.getName.getSimpleName}")
              println(s"  - Name: ${cls.getName}")
              println(s"  - Name prefix: '${cls.getName.getPrefix.getWhitespace}'")
              
              val ext = cls.getExtends
              if (ext != null) {
                println(s"\n  - Extends: ${ext}")
                println(s"  - Extends prefix: '${ext.getPrefix.getWhitespace}'")
                println(s"  - Extends type: ${ext.getClass}")
                
                ext match {
                  case id: J.Identifier =>
                    println(s"    - Identifier: ${id.getSimpleName}")
                    println(s"    - Identifier prefix: '${id.getPrefix.getWhitespace}'")
                  case fa: J.FieldAccess =>
                    println(s"    - FieldAccess: ${fa}")
                    println(s"    - FieldAccess prefix: '${fa.getPrefix.getWhitespace}'")
                    println(s"    - Target: ${fa.getTarget}")
                    println(s"    - Name: ${fa.getName}")
                  case unknown: J.Unknown =>
                    println(s"    - Unknown: ${unknown}")
                    println(s"    - Unknown prefix: '${unknown.getPrefix.getWhitespace}'")
                    println(s"    - Unknown source: ${unknown.getSource.getText}")
                  case expr: Expression =>
                    println(s"    - Expression: ${expr}")
                    println(s"    - Expression prefix: '${expr.getPrefix.getWhitespace}'")
                    println(s"    - Expression type: ${expr.getClass}")
                  case _ =>
                    println(s"    - Unknown type: ${ext.getClass}")
                }
              } else {
                println(s"  - No extends clause")
              }
            case _ =>
              println(s"Not a class declaration")
          }
        case cu: J.CompilationUnit =>
          println(s"Compilation Unit found")
          println(s"  - Classes count: ${cu.getClasses.size()}")
          
          cu.getClasses.asScala.foreach { cls =>
            println(s"\nClass Declaration: ${cls.getName.getSimpleName}")
            println(s"  - Name: ${cls.getName}")
            println(s"  - Name prefix: '${cls.getName.getPrefix.getWhitespace}'")
            
            val ext = cls.getExtends
            if (ext != null) {
              println(s"\n  - Extends: ${ext}")
              println(s"  - Extends prefix: '${ext.getPrefix.getWhitespace}'")
              println(s"  - Extends type: ${ext.getClass}")
              
              ext match {
                case id: J.Identifier =>
                  println(s"    - Identifier: ${id.getSimpleName}")
                  println(s"    - Identifier prefix: '${id.getPrefix.getWhitespace}'")
                case fa: J.FieldAccess =>
                  println(s"    - FieldAccess: ${fa}")
                  println(s"    - FieldAccess prefix: '${fa.getPrefix.getWhitespace}'")
                  println(s"    - Target: ${fa.getTarget}")
                  println(s"    - Name: ${fa.getName}")
                case unknown: J.Unknown =>
                  println(s"    - Unknown: ${unknown}")
                  println(s"    - Unknown prefix: '${unknown.getPrefix.getWhitespace}'")
                  println(s"    - Unknown source: ${unknown.getSource.getText}")
                case expr: Expression =>
                  println(s"    - Expression: ${expr}")
                  println(s"    - Expression prefix: '${expr.getPrefix.getWhitespace}'")
                  println(s"    - Expression type: ${expr.getClass}")
                case _ =>
                  println(s"    - Unknown type: ${ext.getClass}")
              }
            } else {
              println(s"  - No extends clause")
            }
          }
        case _ =>
          println(s"Not a compilation unit: ${result.getClass.getName}")
      }
    }
  }
}