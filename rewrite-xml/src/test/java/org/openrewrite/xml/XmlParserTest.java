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
package org.openrewrite.xml;

import java.util.stream.Stream;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Parser.Input;
import org.openrewrite.SourceFile;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.trait.Reference;
import org.openrewrite.tree.ParseError;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.xml.Assertions.xml;

@SuppressWarnings({"CheckDtdRefs", "CheckTagEmptyBody"})
class XmlParserTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(
          () -> new XmlVisitor<>() {
              @Override
              public Xml visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, ExecutionContext executionContext) {
                  assertNotNull(docTypeDecl.getPrefixUnsafe(), "prefix should not be null");
                  assertNotNull(docTypeDecl.getName(), "name should not be null");
                  assertNotNull(docTypeDecl.getInternalSubset(), "internalSubset should not be null");
                  assertNotNull(docTypeDecl.getBeforeTagDelimiterPrefix(), "beforeTagDelimiterPrefix should not be null");
                  return super.visitDocTypeDecl(docTypeDecl, executionContext);
              }
          }
        ));
    }

    @Test
    void jsp() {
        rewriteRun(
          xml(
            //language=html
            """
              <!DOCTYPE html>
              <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
              <%@ taglib prefix="s" uri="/struts-tags" %>
              <html lang="en">
                <head>
                  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
                  <title>Hello World!</title>
                </head>
                <body>
                  <h2><s:property value="messageStore.message" /></h2>
                </body>
              </html>
              """,
            spec -> spec.path("index.jsp")
          )
        );
    }

    @Test
    void jspScriptlet() {
        rewriteRun(
          xml(
            //language=html
            """
              <!DOCTYPE html>
              <html>
                <body>
                  <%
                    String name = request.getParameter("name");
                    if (name == null) {
                        name = "Guest";
                    }
                  %>
                  <h1>Welcome!</h1>
                </body>
              </html>
              """,
            spec -> spec.path("scriptlet.jsp")
          )
        );
    }

    @Test
    void jspExpression() {
        rewriteRun(
          xml(
            //language=html
            """
              <!DOCTYPE html>
              <html>
                <body>
                  <h1>Current time: <%= new java.util.Date() %></h1>
                  <p>Your name: <%= request.getParameter("name") %></p>
                </body>
              </html>
              """,
            spec -> spec.path("expression.jsp")
          )
        );
    }

    @Test
    void jspDeclaration() {
        rewriteRun(
          xml(
            //language=html
            """
              <!DOCTYPE html>
              <%!
                private int counter = 0;

                public int incrementCounter() {
                    return ++counter;
                }
              %>
              <html>
                <body>
                  <h1>Page visits: <%= incrementCounter() %></h1>
                </body>
              </html>
              """,
            spec -> spec.path("declaration.jsp")
          )
        );
    }

    @Test
    void jspComment() {
        rewriteRun(
          xml(
            //language=html
            """
              <!DOCTYPE html>
              <html>
                <body>
                  <%-- This is a JSP comment that won't appear in the HTML output --%>
                  <h1>Hello World</h1>
                  <%--
                    Multi-line JSP comment
                    for documenting JSP code
                  --%>
                </body>
              </html>
              """,
            spec -> spec.path("comment.jsp")
          )
        );
    }

    @Test
    void jspEmptyFile() {
        rewriteRun(
          xml(
            //language=html
            """
              """,
            spec -> spec.path("empty.jsp")
          )
        );
    }

    @Test
    void jspNoHtmlContent() {
        rewriteRun(
          xml(
            //language=html
            """
              <%-- This is a JSP comment that won't appear in the HTML output --%>
              """,
            spec -> spec.path("noHtmlContent.jsp")
          )
        );
    }

    @Test
    void jspScriptletBeforeHtml() {
        rewriteRun(
          xml(
            //language=html
            """
              <%-- This is a JSP comment that won't appear in the HTML output --%>
              <% String test = "hello"; %>
              <html></html>
              """,
            spec -> spec.path("scripletBeforeHtml.jsp")
          )
        );
    }

    @Test
    void mixedJspElements() {
        rewriteRun(
          xml(
            //language=html
            """
              <!DOCTYPE html>
              <%@ page language="java" contentType="text/html; charset=UTF-8" %>
              <html>
                <body>
                  <%!
                    private String greeting = "Hello";
                  %>
                  <%-- Display greeting --%>
                  <h1><%= greeting %> from JSP!</h1>
                  <%
                    for(int i = 1; i <= 3; i++) {
                  %>
                    <p>Line <%= i %></p>
                  <% } %>
                </body>
              </html>
              """,
            spec -> spec.path("mixed.jsp")
          )
        );
    }

    @Test
    void lowerCaseDocType() {
        rewriteRun(
          xml(
            //language=html
            """
              <!doctype html>
              <html lang="en">
                <body>
                  <h2><s:property value="messageStore.message" /></h2>
                </body>
              </html>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2189")
    @Test
    void specialCharacters() {
        rewriteRun(
          xml("<project>Some &#39;Example&#39;</project>")
        );
    }

    @Test
    void parseXmlDocument() {
        rewriteRun(
          xml(
            """
              <?xml
                  version="1.0" encoding="UTF-8"?>
              <?xml-stylesheet href="mystyle.css" type="text/css"?>
              <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN"
                  "http://www.springframework.org/dtd/spring-beans-2.0.dtd">
              <beans >
                  <bean id="myBean"/>
              </beans>
              """
          )
        );
    }

    @Test
    void javaReferenceDocument() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xmlns="http://www.springframework.org/schema/beans"
                  xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
                <bean id="testBean" class="org.springframework.beans.TestBean" scope="prototype">
                  <property name="age" value="10"/>
                  <property name="sibling">
                      <bean class="org.springframework.beans.TestBean">
                          <property name="age" value="11" class="java.lang.Integer"/>
                          <property name="someName">
                              <value>java.lang.String</value>
                          </property>
                          <property name="someOtherName">
                              <value>java.lang</value>
                          </property>
                      </bean>
                  </property>
                </bean>
              </beans>
              """,
            spec -> spec.afterRecipe(doc -> {
                assertThat(doc.getReferences().getReferences().stream().anyMatch(typeRef -> "java.lang.String".equals(typeRef.getValue()))).isTrue();
                assertThat(doc.getReferences().getReferences().stream().anyMatch(typeRef -> typeRef.getKind() == Reference.Kind.TYPE)).isTrue();
                assertThat(doc.getReferences().getReferences().stream().anyMatch(typeRef -> typeRef.getKind() == Reference.Kind.PACKAGE)).isTrue();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2290")
    @Test
    void cdataTagWhitespace() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
                  <suppress>
                      <notes>
                          <![CDATA[
                        file name: foo.jar
                        ]]>
                      </notes>
                      <gav regex="true">^:foo:.*${'$'}</gav>
                      <cve>CVE-2020-000</cve>
                  </suppress>
              </suppressions>
              """
          )
        );
    }

    @Test
    void parsePomDocument() {
        rewriteRun(
          xml(
            """
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
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/145")
    @Test
    void commentBeforeContent() {
        rewriteRun(
          xml(
            """
              <foo>
                  <a><!-- comment -->a</a>
              </foo>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3301")
    @Test
    void singleQuestionMarkContent() {
        rewriteRun(
          xml(
            """
              <foo>
                  <a><!-- comment -->a</a>
                  <literal>List&lt;?&gt;</literal>
              </foo>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/145")
    @Test
    void commentBeforeContentNewline() {
        rewriteRun(
          xml(
            """
              <foo>
                  <a>
                      <!-- comment -->
                      a
                  </a>
              </foo>
              """
          )
        );
    }


    @Issue("https://github.com/openrewrite/rewrite/issues/145")
    @Test
    void commentAfterContent() {
        rewriteRun(
          xml(
            """
              <foo>
                  <a>a<!-- comment --></a>
              </foo>
              """
          )
        );
    }


    @Issue("https://github.com/openrewrite/rewrite/issues/145")
    @Test
    void commentAfterContentNewline() {
        rewriteRun(
          xml(
            """
              <foo>
                  <a>
                      a
                      <!-- comment -->
                  </a>
              </foo>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1150")
    @Test
    void parseDocTypeWithoutExternalId() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <!DOCTYPE configuration >

              <configuration scan="true">
                  <root>
                      <level>WARN</level>
                      <appender-ref ref="CONSOLE"/>
                  </root>
              </configuration>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1203")
    @Test
    void dtdSubsetMarkupDecl() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0"?>
              <!DOCTYPE p [
                  <!ELEMENT p ANY>
              ]>
              <p>Hello world!</p>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1203")
    @Test
    void dtdSubsetParamEntityRef() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0"?>
              <!DOCTYPE p [
                  %entity;
              ]>
              <p>Hello world!</p>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1203")
    @Test
    void dtdSubsetComment() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0"?>
              <!DOCTYPE p [
                  <!-- comment -->
              ]>
              <p>Hello world!</p>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1243")
    @Test
    void processingInstructions() {
        rewriteRun(
          xml(
            """
              <?xml-stylesheet href="mystyle.css" type="text/css"?>
              <execution>
                  <?m2e execute onConfiguration,onIncremental?>
              </execution>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1382")
    @Test
    void utf8BOMCharacters() {
        rewriteRun(
          xml(
            "ï»¿<?xml version=\"1.0\" encoding=\"UTF-8\"?><test></test>"
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1382")
    @MethodSource
    @ParameterizedTest
    void testUtf8WithAndWithoutBom(@Language("xml") String xml, boolean hasBom) {
        XmlParser parser = XmlParser.builder().build();
        SourceFile parsed = parser.parse(xml).findFirst().orElseThrow();

        assertThat(parsed).isInstanceOf(Xml.class);

        assertThat(parsed.isCharsetBomMarked()).isEqualTo(hasBom);

        SourceFile checked = parser.requirePrintEqualsInput(
          parsed, Input.fromString(xml), null, new InMemoryExecutionContext()
        );

        assertThat(checked).isNotInstanceOf(ParseError.class);
        assertThat(checked).isSameAs(parsed);

        rewriteRun(xml(xml));
    }

    static Stream<Arguments> testUtf8WithAndWithoutBom() {
        return Stream.of(
            Arguments.of("""
              <?xml version="1.0" encoding="UTF-8"?><a />
              """, false),
            Arguments.of("""
              \uFEFF<?xml version="1.0" encoding="UTF-8"?><a />
              """, true),
            Arguments.of("""
              <?xml version="1.0" encoding="UTF-8"?><test></test>
              """, false),
            Arguments.of("""
              \uFEFF<?xml version="1.0" encoding="UTF-8"?><test></test>
              """, true)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2250")
    @Test
    void preserveSpaceBeforeAttributeAssignment() {
        rewriteRun(
          xml(
            """
              <?xml version = "1.0" encoding    =   "UTF-8" standalone = "no" ?><blah></blah>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3175")
    @Test
    void linkWithQuestionMark() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0" encoding="ISO-8859-1"?>
              <?xml-stylesheet type="text/xsl" href="/name/other?link"?>
              <blah></blah>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3442")
    @Test
    void preserveWhitespaceOnEntities() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <message><text>&lt;?xml version='1.0' encoding='UTF-8'?&gt;&#13;
              &lt;note&gt;&#13;
                  &lt;to&gt;Tove&lt;/to&gt;&#13;
                  &lt;from&gt;Jani&lt;/from&gt;&#13;
                  &lt;heading&gt;Reminder&lt;/heading&gt;&#13;
                  &lt;body&gt;Don't forget me this weekend!&lt;/body&gt;&#13;
              &lt;/note&gt;&#13;
              &#13;
              </text></message>
              """
          )
        );
    }

    @DisabledOnOs(OS.WINDOWS)
    @ParameterizedTest
    @ValueSource(strings = {
      "foo.xml",
      "proj.csproj",
      "/foo/bar/baz.jsp",
      "packages.config"
    })
    void acceptWithValidPaths(String path) {
        assertThat(new XmlParser().accept(Path.of(path))).isTrue();
    }

    @DisabledOnOs(OS.WINDOWS)
    @ParameterizedTest
    @ValueSource(strings = {
      ".xml",
      "foo.xml.",
      "file.cpp",
      "/foo/bar/baz.xml.txt"
    })
    void acceptWithInvalidPaths(String path) {
        assertThat(new XmlParser().accept(Path.of(path))).isFalse();
    }

    @Test
    void CRsWithNoLFs() {
        rewriteRun(
          xml(
            "<?xml version=\"1.0\"?>CR<a>CR</a>".replace("CR", "\r")
          )
        );
    }

    @Test
    void utf8SurrogatePairsInComments() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project>
                  <!-- 👇 Problem below -->
                  <dependency>
                      <groupId>org.example</groupId>
                      <artifactId>example</artifactId>
                  </dependency>
                  <!-- 👆 Problem above -->
              </project>
              """
          )
        );
    }

    @Test
    void utf8SurrogatePairsSimple() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <!-- 👇 -->
              <a></a>
              """
          )
        );
    }
}
