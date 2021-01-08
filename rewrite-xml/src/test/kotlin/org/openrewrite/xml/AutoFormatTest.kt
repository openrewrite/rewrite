/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.xml

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.*
import org.openrewrite.internal.StringUtils
import org.openrewrite.xml.internal.XmlPrinter
import org.openrewrite.xml.tree.Xml

class AutoFormatTest {

    private val parser: XmlParser = XmlParser()

    @Test
    fun autoFormatTag() {
        val xml = """
            <project>
              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                  <exclusions>
                    <exclusion>
              <groupId>org.junit.vintage</groupId>
              <artifactId>junit-vintage-engine</artifactId>
            </exclusion>
                  </exclusions>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent()
        val xmlDocument = parser.parse(StringUtils.trimIndent(xml)).iterator().next()
        val processed = AutoFormat(xmlDocument.root).visit(xmlDocument, ExecutionContext.builder().build())
        val xmlPrinter = XmlPrinter<ExecutionContext>(TreePrinter.identity())
        val after = xmlPrinter.visit(processed, ExecutionContext.builder().build())
        assertThat(after).isEqualTo("""
            <project>
              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                  <exclusions>
                    <exclusion>
                      <groupId>org.junit.vintage</groupId>
                      <artifactId>junit-vintage-engine</artifactId>
                    </exclusion>
                  </exclusions>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent())
    }
}
