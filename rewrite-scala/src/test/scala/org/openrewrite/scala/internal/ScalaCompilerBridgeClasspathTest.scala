/*
 * Copyright 2026 the original author or authors.
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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.java.JavaParser

import java.nio.file.Path
import java.util.ArrayList

class ScalaCompilerBridgeClasspathTest {

  @Test
  def typeChecksWithScalaStdlibWhenClasspathLacksIt(@TempDir outDir: Path): Unit = {
    // given
    val classpath = new ArrayList[String]()
    JavaParser.dependenciesFromClasspath("assertj-core").forEach(p => classpath.add(p.toString))
    val entries = new ArrayList[SourceEntry]()
    entries.add(SourceEntry("Service.scala", "class Service { self: Logging with Config => }"))

    // when
    val results = new ScalaCompilerBridge().compileAll(entries, classpath, outDir.toString)

    // then
    val result = results.get("Service.scala")
    assertNotNull(result, "result present")
    assertNotNull(result.tree, "untyped tree populated")
    assertTrue(result.typedTree.isDefined, "typed tree populated")
  }
}
