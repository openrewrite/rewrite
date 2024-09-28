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
          <parent>
              <element3 ns3:attr='test'>content3</element3>
              <ns2:element4 ns3:attr='test2'>content4</ns2:element4>
          </parent>
        </root>
        """
    ).toList().get(0);

    @Test
    void matchAbsolute() {
        assertThat(match("/dependencies/dependency", xmlDoc)).isTrue();
        assertThat(match("/dependencies/*/artifactId", xmlDoc)).isTrue();
        assertThat(match("/dependencies/*", xmlDoc)).isTrue();
        assertThat(match("/dependencies//dependency", xmlDoc)).isTrue();
        assertThat(match("/dependencies//dependency//groupId", xmlDoc)).isTrue();

        // negative matches
        assertThat(match("/dependencies/dne", xmlDoc)).isFalse();
        assertThat(match("/dependencies//dne", xmlDoc)).isFalse();
        assertThat(match("/dependencies//dependency//dne", xmlDoc)).isFalse();
    }

    @Test
    void matchAbsoluteAttribute() {
        assertThat(match("/dependencies/dependency/artifactId/@scope", xmlDoc)).isTrue();
        assertThat(match("/dependencies/dependency/artifactId/@scope", xmlDoc)).isTrue();
        assertThat(match("/dependencies/dependency/artifactId/@*", xmlDoc)).isTrue();
        assertThat(match("/dependencies/dependency/groupId/@*", xmlDoc)).isFalse();
        assertThat(match("/dependencies//dependency//@scope", xmlDoc)).isTrue();
        assertThat(match("/dependencies//dependency//artifactId//@scope", xmlDoc)).isTrue();
        assertThat(match("/dependencies//dependency//@*", xmlDoc)).isTrue();
        assertThat(match("/dependencies//dependency//artifactId//@*", xmlDoc)).isTrue();

        // negative matches
        assertThat(match("/dependencies/dependency/artifactId/@dne", xmlDoc)).isFalse();
        assertThat(match("/dependencies/dependency/artifactId/@dne", xmlDoc)).isFalse();
        assertThat(match("/dependencies//dependency//@dne", xmlDoc)).isFalse();
        assertThat(match("/dependencies//dependency//artifactId//@dne", xmlDoc)).isFalse();

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
    void matchNestedElementsWithSameName() {
        var xml = new XmlParser().parse(
          """
            <?xml version="1.0" encoding="UTF-8"?>
            <root>
                <element foo="bar">
                    <element foo="bar">
                        <test>auie</test>
                    </element>
                    <element qux="quux" />
                </element>
            </root>
            """
        ).toList().get(0);

        // no / at start
        assertThat(match("element/test", xml)).isTrue();
        assertThat(match("element[@foo='bar']/test", xml)).isTrue();
        assertThat(match("element[@foo='baz']/test", xml)).isFalse();
        assertThat(match("element/@qux", xml)).isTrue();
        assertThat(match("dne[@foo='bar']/test", xml)).isFalse();

        // // at start
        assertThat(match("//element/test", xml)).isTrue();
        assertThat(match("//element[@foo='bar']/test", xml)).isTrue();
        assertThat(match("//element[@foo='baz']/test", xml)).isFalse();
        assertThat(match("//element/@qux", xml)).isTrue();
        assertThat(match("//dne[@foo='bar']/test", xml)).isFalse();

        // TODO // in the middle without / (or with //) at start (not currently supported)
//        assertThat(match("root//element/test", xml)).isTrue();
//        assertThat(match("root//element[@foo='bar']/test", xml)).isTrue();
//        assertThat(match("root//element[@foo='baz']/test", xml)).isFalse();
//        assertThat(match("root//element/@qux", xml)).isTrue();
//        assertThat(match("root//dne[@foo='bar']/test", xml)).isFalse();

        // // in the middle with / at start
        assertThat(match("/root//element/test", xml)).isTrue();
        assertThat(match("/root//element[@foo='bar']/test", xml)).isTrue();
        assertThat(match("/root//element[@foo='baz']/test", xml)).isFalse();
        assertThat(match("/root//element/@qux", xml)).isTrue();
        assertThat(match("/root//dne[@foo='bar']/test", xml)).isFalse();
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
        // skip 2+ levels with //
        assertThat(match("/project/build//plugin/configuration/source", pomXml2)).isTrue();
        assertThat(match("/project//configuration/source", pomXml2)).isTrue();
        assertThat(match("/project//plugin//source", pomXml2)).isTrue();
    }

    private final SourceFile attributeXml = new XmlParser().parse(
      """
        <?xml version="1.0" encoding="UTF-8"?>
        <root>
          <element1 foo="bar"><foo>baz</foo></element1>
        </root>
        """
    ).toList().get(0);

    @Test
    void attributePredicate() {
        assertThat(match("/root/element1[@foo='bar']", attributeXml)).isTrue();
        assertThat(match("/root/element1[@foo='baz']", attributeXml)).isFalse();
        assertThat(match("/root/element1[foo='bar']", attributeXml)).isFalse();
        assertThat(match("/root/element1[foo='baz']", attributeXml)).isTrue();
    }

    @Test
    void wildcards() {
        // condition with wildcard attribute
        assertThat(match("/root/element1[@*='bar']", attributeXml)).isTrue();
        assertThat(match("/root/element1[@*='baz']", attributeXml)).isFalse();

        // condition with wildcard element
        assertThat(match("/root/element1[*='bar']", attributeXml)).isFalse();
        assertThat(match("/root/element1[*='baz']", attributeXml)).isTrue();

        // absolute xpath with wildcard element
        assertThat(match("/root/*[@foo='bar']", attributeXml)).isTrue();
        assertThat(match("/root/*[@*='bar']", attributeXml)).isTrue();
        assertThat(match("/root/*[@foo='baz']", attributeXml)).isFalse();
        assertThat(match("/root/*[@*='baz']", attributeXml)).isFalse();

        // relative xpath with wildcard element
        assertThat(match("//*[@foo='bar']", attributeXml)).isTrue();
        assertThat(match("//*[@foo='baz']", attributeXml)).isFalse();
        assertThat(match("//*[foo='bar']", attributeXml)).isFalse();
        assertThat(match("//*[foo='baz']", attributeXml)).isTrue();
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
        assertThat(match("//element1[foo='bar']", xml)).isFalse();
        assertThat(match("//element1[foo='baz']", xml)).isTrue();
        assertThat(match("//element1[@foo='bar']", xml)).isTrue();
        assertThat(match("//element1[foo='baz']/test", xml)).isTrue();
        assertThat(match("//element1[foo='baz']/baz", xml)).isFalse();
        assertThat(match("//element1[foo='bar']/test", xml)).isFalse();
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3919")
    void namespaceMatchFunctions() {
        assertThat(match("/root/element1", namespacedXml)).isTrue();
        assertThat(match("/root/ns2:element2", namespacedXml)).isTrue();
        assertThat(match("/root/dne", namespacedXml)).isFalse();

        // Namespace functions
        assertThat(match("/*[local-name()='element1']", namespacedXml)).isFalse();
        assertThat(match("//*[local-name()='element1']", namespacedXml)).isTrue();
        assertThat(match("/root/*[local-name()='element1']", namespacedXml)).isTrue();
        assertThat(match("/root/*[namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();
        assertThat(match("/*[namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isFalse();
        assertThat(match("//*[namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();
        assertThat(match("//@*[namespace-uri()='http://www.example.com/namespace3']", namespacedXml)).isTrue();
    }

    @Test
    @Disabled
    void otherUncoveredXpathFunctions() {
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

    @Test
    void matchLocalNameFunctionCondition() {
        assertThat(match("/*[local-name()='root']", namespacedXml)).isTrue();
        assertThat(match("/*[local-name()='element1']", namespacedXml)).isFalse();
        assertThat(match("/*[local-name()='element2']", namespacedXml)).isFalse();
        assertThat(match("//*[local-name()='element1']", namespacedXml)).isTrue();
        assertThat(match("//*[local-name()='element2']", namespacedXml)).isTrue();
        assertThat(match("//*[local-name()='dne']", namespacedXml)).isFalse();

        assertThat(match("/root[local-name()='root']", namespacedXml)).isTrue();
        assertThat(match("//element1[local-name()='element1']", namespacedXml)).isTrue();
        assertThat(match("//element2[local-name()='element2']", namespacedXml)).isFalse();
        assertThat(match("//ns2:element2[local-name()='element2']", namespacedXml)).isTrue();
        assertThat(match("//dne[local-name()='dne']", namespacedXml)).isFalse();

        assertThat(match("/root//element1[local-name()='element1']", namespacedXml)).isTrue();
    }

    @Test
    void matchNamespaceUriFunctionCondition() {
        assertThat(match("/root/*[namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();
        assertThat(match("/*[namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isFalse();
        assertThat(match("//*[namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();
        assertThat(match("//ns2:element2[namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();
        assertThat(match("//element2[namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isFalse();
    }

    @Test
    void matchAttributeCondition() {
        assertThat(match("//*[@*='content3']", namespacedXml)).isTrue();
        assertThat(match("//*[@*='content2']", namespacedXml)).isFalse();
        assertThat(match("//*[@ns3:attribute1='content3']", namespacedXml)).isTrue();
        assertThat(match("//*[@attribute1='content3']", namespacedXml)).isFalse();
        assertThat(match("//element1[@ns3:attribute1='content3']", namespacedXml)).isTrue();
        assertThat(match("//element1[@attribute1='content3']", namespacedXml)).isFalse();
        assertThat(match("//element1[@*='content3']", namespacedXml)).isTrue();
        assertThat(match("//element1[@*='dne']", namespacedXml)).isFalse();
        assertThat(match("/root/element1[@*='content3']", namespacedXml)).isTrue();
        assertThat(match("/root/element1[@*='dne']", namespacedXml)).isFalse();
        assertThat(match("/root//element1[@*='content3']", namespacedXml)).isTrue();
        assertThat(match("/root//element1[@*='dne']", namespacedXml)).isFalse();
    }

    @Test
    void matchAttributeElement() {
        assertThat(match("//@ns3:attribute1", namespacedXml)).isTrue();
        assertThat(match("//@attribute1", namespacedXml)).isFalse();
        assertThat(match("//@*", namespacedXml)).isTrue();
        assertThat(match("//@*[local-name()='attribute1']", namespacedXml)).isTrue();
        assertThat(match("//@*[local-name()='attribute2']", namespacedXml)).isFalse();
        assertThat(match("//@*[namespace-uri()='http://www.example.com/namespace3']", namespacedXml)).isTrue();
        assertThat(match("//@*[namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isFalse();

        assertThat(match("//element1/@*", namespacedXml)).isTrue();
        assertThat(match("/root/element1/@*", namespacedXml)).isTrue();
        assertThat(match("//element1/@*[namespace-uri()='http://www.example.com/namespace3']", namespacedXml)).isTrue();
        assertThat(match("//element1/@*[namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isFalse();
        assertThat(match("//ns2:element2/@*", namespacedXml)).isFalse();
        assertThat(match("/root/ns2:element2/@*", namespacedXml)).isFalse();

        assertThat(match("/root/parent/element3/@attr", namespacedXml)).isFalse();
        assertThat(match("/root/parent/element3/@ns3:attr", namespacedXml)).isTrue();
        assertThat(match("/root/parent/element3/@ns3:attr[namespace-uri()='http://www.example.com/namespace3']", namespacedXml)).isTrue();
        assertThat(match("//element3/@ns3:attr[namespace-uri()='http://www.example.com/namespace3']", namespacedXml)).isTrue();

        assertThat(match("/root//element1/@*", namespacedXml)).isTrue();
        assertThat(match("/root//element1/@*[namespace-uri()='http://www.example.com/namespace3']", namespacedXml)).isTrue();
    }

    @Test
    void matchMultipleConditions() {
        assertThat(match("//*[namespace-uri()='http://www.example.com/namespace2'][local-name()='element2']", namespacedXml)).isTrue();
        assertThat(match("//*[local-name()='element2'][namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();

        assertThat(match("//*[namespace-uri()='http://www.example.com/namespace2'][local-name()='dne']", namespacedXml)).isFalse();
        assertThat(match("//*[local-name()='dne'][namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isFalse();

        assertThat(match("//*[local-name()='element1'][@ns3:attribute1='content3']", namespacedXml)).isTrue();
        assertThat(match("//@*[namespace-uri()='http://www.example.com/namespace3'][local-name()='attribute1']", namespacedXml)).isTrue();
        assertThat(match("//@*[namespace-uri()='http://www.example.com/namespace3'][local-name()='dne']", namespacedXml)).isFalse();

        assertThat(match("//*[@ns3:attr='test'][local-name()='element3']", namespacedXml)).isTrue();
        assertThat(match("//*[@ns3:attr='test'][local-name()='elementX']", namespacedXml)).isFalse();

        assertThat(match("//*[@ns3:attr='test2'][local-name()='element4'][namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();
        assertThat(match("//*[@ns3:attr='testX'][local-name()='element4'][namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isFalse();
        assertThat(match("//*[@ns3:attr='test2'][local-name()='elementX'][namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isFalse();
        assertThat(match("//*[@ns3:attr='test2'][local-name()='element4'][namespace-uri()='http://www.example.com/namespaceX']", namespacedXml)).isFalse();
    }

    @Test
    void matchConditionsWithConjunctions() {
        // T&T, T&F, F&T, F&F
        assertThat(match("//*[local-name()='element3' and @ns3:attr='test']", namespacedXml)).isTrue();
        assertThat(match("//*[local-name()='element3' and @ns3:attr='dne']", namespacedXml)).isFalse();
        assertThat(match("//*[local-name()='dne' and @ns3:attr='test']", namespacedXml)).isFalse();
        assertThat(match("//*[local-name()='dne' and @ns3:attr='dne']", namespacedXml)).isFalse();

        // T|T, T|F, F|T, F|F
        assertThat(match("//*[local-name()='element2' or namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();
        assertThat(match("//*[local-name()='element2' or namespace-uri()='dne']", namespacedXml)).isTrue();
        assertThat(match("//*[local-name()='dne' or local-name()='element2']", namespacedXml)).isTrue();
        assertThat(match("//*[local-name()='dne' or local-name()='dne2']", namespacedXml)).isFalse();

        assertThat(match("//@*[namespace-uri()='dne' or namespace-uri()='http://www.example.com/namespace3']", namespacedXml)).isTrue();

        // T&T&T = T
        assertThat(match("//*[local-name()='element4' and namespace-uri()='http://www.example.com/namespace2' and @ns3:attr='test2']", namespacedXml)).isTrue();
        // T&T&F = F
        assertThat(match("//*[local-name()='element4' and namespace-uri()='http://www.example.com/namespace2' and @ns3:attr='dne']", namespacedXml)).isFalse();
        // T&T|F = T
        assertThat(match("//*[local-name()='element4' and namespace-uri()='http://www.example.com/namespace2' or @ns3:attr='dne']", namespacedXml)).isTrue();
        // T&F|T = T
        assertThat(match("//*[local-name()='element4' and @ns3:attr='dne' or namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();
        // T&F|F = F
        assertThat(match("//*[local-name()='element4' and @ns3:attr='dne' or namespace-uri()='http://www.example.com/namespaceX']", namespacedXml)).isFalse();

        // F|F|T = T
        assertThat(match("//*[local-name()='dne' or local-name()='dne2' or local-name()='element2']", namespacedXml)).isTrue();

        // [T&T][T] = T
        assertThat(match("//*[local-name()='element4' and namespace-uri()='http://www.example.com/namespace2'][@ns3:attr='test2']", namespacedXml)).isTrue();
        // [T&T][F] = F
        assertThat(match("//*[local-name()='element4' and namespace-uri()='http://www.example.com/namespace2'][@ns3:attr='dne']", namespacedXml)).isFalse();
        // [F&T][T] = F
        assertThat(match("//*[local-name()='dne' and namespace-uri()='http://www.example.com/namespace2'][@ns3:attr='test2']", namespacedXml)).isFalse();
        // [F|T][T] = T
        assertThat(match("//*[local-name()='dne' or local-name()='element4'][namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();
        // [F|T][F] = F
        assertThat(match("//*[local-name()='dne' or local-name()='element4'][namespace-uri()='http://www.example.com/namespaceX']", namespacedXml)).isFalse();

        // F|T&T = T
        assertThat(match("//*[local-name()='dne' or local-name()='element4' and namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();
        // F|T&F = F
        assertThat(match("//*[local-name()='dne' or local-name()='element4' and namespace-uri()='http://www.example.com/namespaceX']", namespacedXml)).isFalse();
        // F|F&T = F
        assertThat(match("//*[local-name()='dne' or namespace-uri()='http://www.example.com/namespaceX' and local-name()='element4']", namespacedXml)).isFalse();

        // T|F & T = T
        assertThat(match("//*[local-name()='element4' or local-name()='dne' and namespace-uri()='http://www.example.com/namespace2']", namespacedXml)).isTrue();
        // T|F & F = T
        assertThat(match("//*[local-name()='element4' or local-name()='dne' and namespace-uri()='http://www.example.com/namespaceX']", namespacedXml)).isTrue();
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
