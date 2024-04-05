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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class XPathMatcherTest {

    private final SourceFile xmlDoc = new XmlParser().parse(
      """
        <?xml version="1.0" encoding="UTF-8"?>
        <dependencies>
            <dependency>
                <groupId>org.openrewrite</groupId>
                <artifactId scope="compile">rewrite-xml</artifactId>
            </dependency>
            <dependency>
                <artifactId scope="test">assertj-core</artifactId>
            </dependency>
        </dependencies>
        """
    ).toList().get(0);

    private final SourceFile pomXml1 = new XmlParser().parse(
      """
        <project>
          <groupId>com.mycompany.app</groupId>
          <artifactId>my-app</artifactId>
          <version>1</version>
          <build>
            <plugins>
              <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.0</version>
                <configuration>
                  <source>1.8</source>
                  <target>1.8</target>
                </configuration>
              </plugin>
            </plugins>
          </build>
        </project>
        """
    ).toList().get(0);

    private final SourceFile pomXml2 = new XmlParser().parse(
      """
        <project>
          <groupId>com.mycompany.app</groupId>
          <artifactId>my-app</artifactId>
          <version>1</version>
          <build>
            <pluginManagement>
              <plugins>
                <plugin>
                  <groupId>org.apache.maven.plugins</groupId>
                  <artifactId>maven-compiler-plugin</artifactId>
                  <version>3.8.0</version>
                  <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                  </configuration>
                </plugin>
              </plugins>
            </pluginManagement>
          </build>
        </project>
        """
    ).toList().get(0);

    private final SourceFile namespacedXml = new XmlParser().parse(
      """
        <?xml version="1.0" encoding="UTF-8"?>
        <root xmlns="http://www.example.com/namespace1"
              xmlns:ns2="http://www.example.com/namespace2"
              xmlns:ns3="http://www.example.com/namespace3"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://www.example.com/namespace1 http://www.example.com/namespace1.xsd
                                  http://www.example.com/namespace2 http://www.example.com/namespace2.xsd
                                  http://www.example.com/namespace3 http://www.example.com/namespace3.xsd">
          <element1 ns3:attribute1="content3">content1</element1>
          <ns2:element2>content2</ns2:element2>
        </root>
        """
    ).toList().get(0);

    @Test
    void matchAbsolute() {
        assertThat(match("/dependencies/dependency", xmlDoc)).isTrue();
        assertThat(match("/dependencies/*/artifactId", xmlDoc)).isTrue();
        assertThat(match("/dependencies/*", xmlDoc)).isTrue();
        assertThat(match("/dependencies/dne", xmlDoc)).isFalse();
    }

    @Test
    void matchAbsoluteAttribute() {
        assertThat(match("/dependencies/dependency/artifactId/@scope", xmlDoc)).isTrue();
        assertThat(match("/dependencies/dependency/artifactId/@scope", xmlDoc)).isTrue();
        assertThat(match("/dependencies/dependency/artifactId/@*", xmlDoc)).isTrue();
        assertThat(match("/dependencies/dependency/groupId/@*", xmlDoc)).isFalse();
    }

    @Test
    void matchRelative() {
        assertThat(match("dependencies", xmlDoc)).isTrue();
        assertThat(match("dependency", xmlDoc)).isTrue();
        assertThat(match("//dependency", xmlDoc)).isTrue();
        assertThat(match("dependency/*", xmlDoc)).isTrue();
        assertThat(match("dne", xmlDoc)).isFalse();
    }

    @Test
    void matchRelativeAttribute() {
        assertThat(match("dependency/artifactId/@scope", xmlDoc)).isTrue();
        assertThat(match("dependency/artifactId/@*", xmlDoc)).isTrue();
        assertThat(match("//dependency/artifactId/@scope", xmlDoc)).isTrue();
    }

    @Test
    void matchPom() {
        assertThat(match("/project/build/plugins/plugin/configuration/source",
          pomXml1)).isTrue();
        assertThat(match("/project/build/plugins/plugin[artifactId='maven-compiler-plugin']/configuration/source",
          pomXml1)).isTrue();
        assertThat(match("//plugin[artifactId='maven-compiler-plugin']/configuration/source",
          pomXml1)).isTrue();
        assertThat(match("/project/build/plugins/plugin[groupId='org.apache.maven.plugins']/configuration/source",
          pomXml1)).isTrue();
        assertThat(match("/project/build/plugins/plugin[artifactId='somethingElse']/configuration/source",
          pomXml1)).isFalse();
        assertThat(match("/project/build//plugins/plugin/configuration/source",
          pomXml1)).isTrue();
        assertThat(match("/project/build//plugins/plugin/configuration/source",
          pomXml2)).isTrue();
    }

    @Test
    void attributePredicate() {
        SourceFile xml = new XmlParser().parse(
          """
            <?xml version="1.0" encoding="UTF-8"?>
            <root>
              <element1 foo="bar"><foo>baz</foo></element1>
            </root>
            """
        ).toList().get(0);
        assertThat(match("/root/element1[@foo='bar']", xml)).isTrue();
        assertThat(match("/root/element1[@foo='baz']", xml)).isFalse();
        assertThat(match("/root/element1[foo='bar']", xml)).isFalse();
        assertThat(match("/root/element1[foo='baz']", xml)).isTrue();
    }

    @Test
    void relativePathsWithConditions() {
        SourceFile xml = new XmlParser().parse(
          """
            <?xml version="1.0" encoding="UTF-8"?>
            <root>
              <element1 foo="bar">
                <foo>baz</foo>
                <test>asdf</test>
              </element1>
            </root>
            """
        ).toList().get(0);
        assertThat(match("//element1[@foo='bar']", xml)).isTrue();
        assertThat(match("//element1[foo='baz']/test", xml)).isTrue();
        assertThat(match("//element1[foo='bar']/test", xml)).isFalse();
    }

    @Test
    @Disabled
    @Issue("https://github.com/openrewrite/rewrite/issues/3919")
    void matchFunctions() {
        assertThat(match("/root/element1", namespacedXml)).isTrue();
        assertThat(match("/root/ns2:element2", namespacedXml)).isTrue();
        assertThat(match("/root/dne", namespacedXml)).isFalse();

        // Namespace functions
        assertThat(match("/*[local-name()='element1']", namespacedXml)).isFalse();
        assertThat(match("//*[local-name()='element1']", namespacedXml)).isFalse();
        assertThat(match("/root/*[local-name()='element1']", namespacedXml)).isTrue();
        assertThat(match("/root/*[namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();
        assertThat(match("/*[namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isFalse();
        assertThat(match("//*[namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();
        assertThat(match("//@*[namespace-uri()='http://www.example.com/namespace3']", namespacedXml)).isTrue();

        // Other common XPath functions
        assertThat(match("contains(/root/element1, 'content1')", namespacedXml)).isTrue();
        assertThat(match("not(contains(/root/element1, 'content1'))", namespacedXml)).isFalse();
        assertThat(match("string-length(/root/element1) > 2", namespacedXml)).isTrue();
        assertThat(match("starts-with(/root/element1, 'content1')", namespacedXml)).isTrue();
        assertThat(match("ends-with(/root/element1, 'content1')", namespacedXml)).isTrue();
        assertThat(match("substring-before(/root/element1, '1') = 'content'", namespacedXml)).isTrue();
        assertThat(match("substring-after(/root/element1, 'content') = '1'", namespacedXml)).isTrue();
        assertThat(match("/root/element1/text()", namespacedXml)).isTrue();
        assertThat(match("count(/root/*)", namespacedXml)).isTrue();
    }

    private boolean match(String xpath, SourceFile x) {
        XPathMatcher matcher = new XPathMatcher(xpath);
        return !TreeVisitor.collect(new XmlVisitor<>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (matcher.matches(getCursor())) {
                    return SearchResult.found(tag);
                }
                return super.visitTag(tag, ctx);
            }

            @Override
            public Xml visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
                if (matcher.matches(getCursor())) {
                    return SearchResult.found(attribute);
                }
                return super.visitAttribute(attribute, ctx);
            }
        }, x, new ArrayList<>()).isEmpty();
    }
}
