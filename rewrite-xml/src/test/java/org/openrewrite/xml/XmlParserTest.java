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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

@SuppressWarnings({"CheckDtdRefs", "CheckTagEmptyBody"})
class XmlParserTest implements RewriteTest {

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
    @Test
    void utf8BOM() {
        rewriteRun(
          xml(
            """
              %s<?xml version="1.0" encoding="UTF-8"?><test></test>
              """.formatted("\uFEFF"))
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2250")
    @Test
    void preserveSpaceBeforeAttributeAssignment() {
        rewriteRun(
          xml(
            """
              <?xml version = "1.0" encoding    =   "UTF-8" standalone = "no" ?><blah></blah>
              """)
        );
    }
}
