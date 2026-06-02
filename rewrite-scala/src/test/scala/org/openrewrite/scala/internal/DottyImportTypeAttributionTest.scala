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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.java.JavaParser

import java.nio.file.Path
import java.util.ArrayList
import scala.jdk.CollectionConverters.*

/**
 * Regression tests for the stdlib self-heal: when a caller passes a
 * Scala-ish classpath (one that contains a `scala3-compiler` jar, for
 * instance) but is missing one of the stdlib jars dotty relies on
 * (scala-library / tasty-core), the bridge must add the missing stdlib
 * entries itself. Otherwise dotty's typer blows up with either
 * `ClassCastException: NoSymbol cannot be cast to ClassSymbol` in
 * `Definitions.UnitClass` or the related "Bad symbolic reference" TASTy
 * error when the source imports dotty compiler APIs.
 */
class DottyImportTypeAttributionTest {

  /**
   * Mimic a caller (e.g. `mod build`) that passes only scala3-compiler
   * without the full transitive stdlib. Without the self-heal, dotty fails.
   */
  private def scalaCompilerOnlyClasspath: java.util.List[String] = {
    val all = new ArrayList[String]()
    JavaParser.runtimeClasspath().forEach(p => all.add(p.toString))
    val filtered = new ArrayList[String]()
    all.asScala.foreach { entry =>
      if (entry.contains("scala3-compiler_3")) filtered.add(entry)
    }
    filtered
  }

  @Test
  def typeChecksOrdinaryScalaWithOnlyCompilerOnClasspath(@TempDir outDir: Path): Unit = {
    val bridge = new ScalaCompilerBridge()
    val source =
      """package example
        |
        |class Foo {
        |  def bar(x: Int): Option[Int] = Some(x)
        |}
        |""".stripMargin

    val entries = new ArrayList[SourceEntry]()
    entries.add(SourceEntry("Foo.scala", source))

    val results = bridge.compileAll(entries, scalaCompilerOnlyClasspath, outDir.toString)

    val result = results.get("Foo.scala")
    assertNotNull(result, "result present")
    assertNotNull(result.tree, "untyped tree populated")
    assertTrue(
      result.typedTree.isDefined,
      "typed tree should be populated even when caller's classpath only has scala3-compiler"
    )
  }

  @Test
  def typeChecksFileImportingDottyContexts(@TempDir outDir: Path): Unit = {
    val bridge = new ScalaCompilerBridge()
    val source =
      """package example
        |
        |import dotty.tools.dotc.core.Contexts.*
        |
        |class Foo {
        |  def bar(using ctx: Context): Int = 42
        |}
        |""".stripMargin

    val entries = new ArrayList[SourceEntry]()
    entries.add(SourceEntry("Foo.scala", source))

    val results = bridge.compileAll(entries, scalaCompilerOnlyClasspath, outDir.toString)

    val result = results.get("Foo.scala")
    assertNotNull(result, "result present")
    assertNotNull(result.tree, "untyped tree populated")
    assertTrue(
      result.typedTree.isDefined,
      "typed tree should be populated for a Scala file that imports dotty compiler APIs"
    )
  }
}
