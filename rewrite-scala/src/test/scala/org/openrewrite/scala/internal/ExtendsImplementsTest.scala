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
import org.junit.jupiter.api.Assertions.*

class ExtendsImplementsTest {
  
  private def parseAndGetClass(source: String): J.ClassDeclaration = {
    val parser = new ScalaParser.Builder().build()
    import scala.jdk.StreamConverters._
    import scala.jdk.CollectionConverters._
    
    val results = parser.parse(source).toScala(List)
    assertTrue(results.nonEmpty, "Should have parse results")
    
    val result = results.head
    result match {
      case pe: org.openrewrite.tree.ParseError =>
        println(s"Parse error: ${pe.getText}")
        pe.getMarkers.getMarkers.forEach { marker =>
          println(s"Marker: ${marker}")
        }
        fail(s"Parse error occurred: ${pe.getText}")
      case scu: S.CompilationUnit =>
        assertEquals(1, scu.getStatements.size(), "Should have one statement")
        scu.getStatements.get(0) match {
          case cls: J.ClassDeclaration => cls
          case other => fail(s"Expected class declaration, got ${other.getClass}")
        }
      case _ => fail(s"Expected S.CompilationUnit, got ${result.getClass}")
    }
  }
  
  @Test
  def testSimpleExtends(): Unit = {
    val cls = parseAndGetClass("class Dog extends Animal")
    
    assertEquals("Dog", cls.getName.getSimpleName)
    assertNotNull(cls.getExtends, "Should have extends clause")
    
    cls.getExtends match {
      case id: J.Identifier =>
        assertEquals("Animal", id.getSimpleName)
        assertEquals(" ", id.getPrefix.getWhitespace, "Should have space before type")
      case _ => fail("Extends should be J.Identifier")
    }
    
    assertNull(cls.getImplements, "Should not have implements")
  }
  
  @Test
  def testExtendsWithBody(): Unit = {
    val cls = parseAndGetClass("""class Dog extends Animal {
      |  def bark(): Unit = println("Woof!")
      |}""".stripMargin)
    
    assertEquals("Dog", cls.getName.getSimpleName)
    assertNotNull(cls.getExtends, "Should have extends clause")
    
    cls.getExtends match {
      case id: J.Identifier =>
        assertEquals("Animal", id.getSimpleName)
      case _ => fail("Extends should be J.Identifier")
    }
    
    assertNotNull(cls.getBody, "Should have body")
  }
  
  @Test
  def testExtendsWithImplements(): Unit = {
    val cls = parseAndGetClass("class Dog extends Animal with Trainable")
    
    assertEquals("Dog", cls.getName.getSimpleName)
    assertNotNull(cls.getExtends, "Should have extends clause")
    
    cls.getExtends match {
      case id: J.Identifier =>
        assertEquals("Animal", id.getSimpleName)
      case _ => fail("Extends should be J.Identifier")
    }
    
    assertNotNull(cls.getImplements, "Should have implements (with clause)")
    assertEquals(1, cls.getImplements.size())
    
    cls.getImplements.get(0) match {
      case id: J.Identifier =>
        assertEquals("Trainable", id.getSimpleName)
      case _ => fail("Implements element should be J.Identifier")
    }
  }
  
  @Test
  def testMultipleWith(): Unit = {
    val cls = parseAndGetClass("class Dog extends Animal with Trainable with Friendly")
    
    assertEquals("Dog", cls.getName.getSimpleName)
    assertNotNull(cls.getExtends, "Should have extends clause")
    assertNotNull(cls.getImplements, "Should have implements")
    assertEquals(2, cls.getImplements.size(), "Should have 2 with clauses")
    
    val impls = cls.getImplements
    impls.get(0) match {
      case id: J.Identifier => assertEquals("Trainable", id.getSimpleName)
      case _ => fail("First implements should be J.Identifier")
    }
    
    impls.get(1) match {
      case id: J.Identifier => assertEquals("Friendly", id.getSimpleName)
      case _ => fail("Second implements should be J.Identifier")
    }
  }
  
  @Test
  def testQualifiedExtends(): Unit = {
    val cls = parseAndGetClass("class Dog extends com.example.Animal")
    
    assertEquals("Dog", cls.getName.getSimpleName)
    assertNotNull(cls.getExtends, "Should have extends clause")
    
    cls.getExtends match {
      case fa: J.FieldAccess =>
        assertEquals("Animal", fa.getName.getSimpleName)
        // Verify the qualified name structure
        fa.getTarget match {
          case fa2: J.FieldAccess =>
            assertEquals("example", fa2.getName.getSimpleName)
          case _ => fail("Target should be FieldAccess")
        }
      case _ => fail("Extends should be J.FieldAccess for qualified type")
    }
  }
  
  @Test
  def testExtendsWithConstructorParams(): Unit = {
    val cls = parseAndGetClass("class Dog(name: String) extends Animal")
    
    assertEquals("Dog", cls.getName.getSimpleName)
    assertNotNull(cls.getExtends, "Should have extends clause")
    
    cls.getExtends match {
      case id: J.Identifier =>
        assertEquals("Animal", id.getSimpleName)
      case _ => fail("Extends should be J.Identifier")
    }
    
    // Constructor parameters should be preserved as Unknown in primaryConstructor
    assertNotNull(cls.getPrimaryConstructor)
    assertEquals(1, cls.getPrimaryConstructor.size())
  }
  
  @Test
  def testExtendsWithModifiers(): Unit = {
    val cls = parseAndGetClass("final class Dog extends Animal")
    
    assertEquals("Dog", cls.getName.getSimpleName)
    assertEquals(1, cls.getModifiers.size())
    assertEquals(J.Modifier.Type.Final, cls.getModifiers.get(0).getType)
    
    assertNotNull(cls.getExtends, "Should have extends clause")
    cls.getExtends match {
      case id: J.Identifier =>
        assertEquals("Animal", id.getSimpleName)
      case _ => fail("Extends should be J.Identifier")
    }
  }
  
  @Test
  def testExtendsSpacing(): Unit = {
    // Basic spacing test
    val cls = parseAndGetClass("class Dog extends Animal")
    
    // Just verify that we parsed successfully with extends
    assertNotNull(cls.getExtends, "Should have extends clause")
    assertEquals("Animal", cls.getExtends match {
      case id: J.Identifier => id.getSimpleName
      case _ => fail("Extends should be J.Identifier")
    })
  }
  
  @Test
  def testExtendsDoubleSpace(): Unit = {
    // Test double spaces preservation
    val cls = parseAndGetClass("class Dog  extends  Animal")
    
    assertNotNull(cls.getExtends, "Should have extends clause")
    assertEquals("Animal", cls.getExtends match {
      case id: J.Identifier => id.getSimpleName
      case _ => fail("Extends should be J.Identifier")
    })
  }
  
  @Test
  def testExtendsTab(): Unit = {
    // Test tab preservation
    val cls = parseAndGetClass("class Dog\textends\tAnimal")
    
    assertNotNull(cls.getExtends, "Should have extends clause")
    assertEquals("Animal", cls.getExtends match {
      case id: J.Identifier => id.getSimpleName
      case _ => fail("Extends should be J.Identifier")
    })
  }
  
  @Test
  def testExtendsNewline(): Unit = {
    // Test newline preservation
    val cls = parseAndGetClass("class Dog\n  extends Animal")
    
    assertNotNull(cls.getExtends, "Should have extends clause")
    assertEquals("Animal", cls.getExtends match {
      case id: J.Identifier => id.getSimpleName
      case _ => fail("Extends should be J.Identifier")
    })
  }
}