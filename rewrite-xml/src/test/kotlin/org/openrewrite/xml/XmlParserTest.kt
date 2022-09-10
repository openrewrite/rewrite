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
import org.openrewrite.Issue
import org.openrewrite.internal.StringUtils
import org.openrewrite.test.RewriteTest
import org.openrewrite.xml.Assertions.xml

class XmlParserTest: RewriteTest {
    private val parser: XmlParser = XmlParser()

    private fun assertUnchanged(before: String) {
        val xmlDocument = parser.parse(StringUtils.trimIndent(before)).iterator().next()
        assertThat(xmlDocument.printAll()).`as`("Source should not be changed").isEqualTo(before)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2189")
    @Test
    fun specialCharacters() = rewriteRun(
        xml("<project>Some &#39;Example&#39;</project>")
    )

    @Test
    fun parseXmlDocument() = assertUnchanged(
        before = """
                <?xml
                    version="1.0" encoding="UTF-8"?>
                <?xml-stylesheet href="mystyle.css" type="text/css"?>
                <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN"
                    "http://www.springframework.org/dtd/spring-beans-2.0.dtd">
                <beans >
                    <bean id="myBean"/>
                </beans>
            """.trimIndent()
    )

    @Test
    fun parsePomDocument() = assertUnchanged(
        before = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!-- comment -->
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <parent>
                <groupId>com.google.guava</groupId>
                <artifactId>guava-parent</artifactId>
                <version>28.2-jre</version>
              </parent>
              <artifactId>guava</artifactId>
              <packaging>bundle</packaging>
              <name>Guava: Google Core Libraries for Java</name>
            </project>
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/145")
    @Test
    fun commentBeforeContent() = assertUnchanged(
        before = """
            <foo>
                <a><!-- comment -->a</a>
            </foo>
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/145")
    @Test
    fun commentBeforeContentNewline() = assertUnchanged(
        before = """
            <foo>
                <a>
                    <!-- comment -->
                    a
                </a>
            </foo>
        """.trimIndent()
    )


    @Issue("https://github.com/openrewrite/rewrite/issues/145")
    @Test
    fun commentAfterContent() = assertUnchanged(
        before = """
            <foo>
                <a>a<!-- comment --></a>
            </foo>
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/145")
    @Test
    fun commentAfterContentNewline() = assertUnchanged(
        before = """
            <foo>
                <a>
                    a
                    <!-- comment -->
                </a>
            </foo>
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1150")
    @Test
    fun parseDocTypeWithoutExternalId() = assertUnchanged(
        before = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE configuration >

            <configuration scan="true">
                <root>
                    <level>WARN</level>
                    <appender-ref ref="CONSOLE"/>
                </root>
            </configuration>
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1203")
    @Test
    fun dtdSubsetMarkupDecl() = assertUnchanged(
        before = """
            <?xml version="1.0"?>
            <!DOCTYPE p [
                <!ELEMENT p ANY>
            ]>
            <p>Hello world!</p>
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1203")
    @Test
    fun dtdSubsetParamEntityRef() = assertUnchanged(
        before = """
            <?xml version="1.0"?>
            <!DOCTYPE p [
                %entity;
            ]>
            <p>Hello world!</p>
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1203")
    @Test
    fun dtdSubsetComment() = assertUnchanged(
        before = """
            <?xml version="1.0"?>
            <!DOCTYPE p [
                <!-- comment -->
            ]>
            <p>Hello world!</p>
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1243")
    @Test
    fun processingInstructions() = assertUnchanged(
        before = """
            <?xml-stylesheet href="mystyle.css" type="text/css"?>
            <execution>
                <?m2e execute onConfiguration,onIncremental?>
            </execution>
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1382")
    @Test
    fun utf8BOMCharacters() = assertUnchanged(
        before = """
            ï»¿<?xml version="1.0" encoding="UTF-8"?>
                <test></test>
            <tag></tag>
        """.trimIndent()
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1382")
    @Test
    fun utf8BOM() = assertUnchanged(
        before = "\\uFEFF<?xml version=\"1.0\" encoding=\"UTF-8\"?><test></test>"
    )
}
