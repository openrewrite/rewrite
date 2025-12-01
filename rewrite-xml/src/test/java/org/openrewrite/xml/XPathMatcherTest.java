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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.SourceFile;
import org.openrewrite.xml.tree.Xml;

import java.util.concurrent.atomic.AtomicInteger;

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
    ).toList().getFirst();

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
    ).toList().getFirst();

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
    ).toList().getFirst();

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
    ).toList().getFirst();

    @Test
    void matchAbsolute() {
        assertThat(matchCount("/dependencies/dependency", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("/dependencies/*/artifactId", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("/dependencies/*", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("/dependencies//dependency", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("/dependencies//dependency//groupId", xmlDoc)).isEqualTo(1);

        // negative matches
        assertThat(matchCount("/dependencies/dne", xmlDoc)).isEqualTo(0);
        assertThat(matchCount("/dependencies//dne", xmlDoc)).isEqualTo(0);
        assertThat(matchCount("/dependencies//dependency//dne", xmlDoc)).isEqualTo(0);
    }

    @Test
    void matchAbsoluteAttribute() {
        assertThat(matchCount("/dependencies/dependency/artifactId/@scope", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("/dependencies/dependency/artifactId/@*", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("/dependencies/dependency/groupId/@*", xmlDoc)).isEqualTo(0);
        assertThat(matchCount("/dependencies//dependency//@scope", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("/dependencies//dependency//artifactId//@scope", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("/dependencies//dependency//@*", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("/dependencies//dependency//artifactId//@*", xmlDoc)).isEqualTo(2);

        // negative matches
        assertThat(matchCount("/dependencies/dependency/artifactId/@dne", xmlDoc)).isEqualTo(0);
        assertThat(matchCount("/dependencies//dependency//@dne", xmlDoc)).isEqualTo(0);
        assertThat(matchCount("/dependencies//dependency//artifactId//@dne", xmlDoc)).isEqualTo(0);
    }

    @Test
    void matchRelative() {
        assertThat(matchCount("dependencies", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("dependency", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("//dependency", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("dependency/*", xmlDoc)).isEqualTo(3); // 1 groupId + 2 artifactId
        assertThat(matchCount("dne", xmlDoc)).isEqualTo(0);
    }

    @Test
    void matchRelativeAttribute() {
        assertThat(matchCount("dependency/artifactId/@scope", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("dependency/artifactId/@*", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("//dependency/artifactId/@scope", xmlDoc)).isEqualTo(2);
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
        ).toList().getFirst();

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
    ).toList().getFirst();

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
        ).toList().getFirst();
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
    void matchTextNodeTypeTest() {
        // text() node type test works in path steps
        assertThat(match("/root/element1/text()", namespacedXml)).isTrue();
        assertThat(match("//element1/text()", namespacedXml)).isTrue();
        assertThat(match("/root/ns2:element2/text()", namespacedXml)).isTrue();
        // parent has child elements, not direct text content
        assertThat(match("/root/parent/text()", namespacedXml)).isFalse();
    }

    @Test
    void matchContainsFunction() {
        // Basic contains - positive cases
        assertThat(match("contains(/root/element1, 'content1')", namespacedXml)).isTrue();
        assertThat(match("contains(/root/element1, 'content')", namespacedXml)).isTrue();
        assertThat(match("contains(/root/element1, '1')", namespacedXml)).isTrue();
        assertThat(match("contains(/root/element1, 'ent')", namespacedXml)).isTrue();

        // Basic contains - negative cases
        assertThat(match("contains(/root/element1, 'notfound')", namespacedXml)).isFalse();
        assertThat(match("contains(/root/element1, 'CONTENT1')", namespacedXml)).isFalse(); // case sensitive
        assertThat(match("contains(/root/element1, '')", namespacedXml)).isTrue(); // empty string is always contained

        // Contains with different elements
        assertThat(match("contains(/root/ns2:element2, 'content2')", namespacedXml)).isTrue();
        assertThat(match("contains(/root/parent/element3, 'content3')", namespacedXml)).isTrue();

        // Contains with non-existent path
        assertThat(match("contains(/root/nonexistent, 'anything')", namespacedXml)).isFalse();
    }

    @Test
    void matchContainsInPredicate() {
        // contains() in predicate with child element - matches dependency with groupId containing 'openrewrite'
        assertThat(match("/dependencies/dependency[contains(groupId, 'openrewrite')]", xmlDoc)).isTrue();
        assertThat(match("/dependencies/dependency[contains(groupId, 'rewrite')]", xmlDoc)).isTrue();
        assertThat(match("/dependencies/dependency[contains(artifactId, 'rewrite')]", xmlDoc)).isTrue();

        // negative cases
        assertThat(match("/dependencies/dependency[contains(groupId, 'notfound')]", xmlDoc)).isFalse();
        assertThat(match("/dependencies/dependency[contains(artifactId, 'notfound')]", xmlDoc)).isFalse();

        // with descendant axis
        assertThat(match("//dependency[contains(groupId, 'openrewrite')]", xmlDoc)).isTrue();
        assertThat(match("//dependency[contains(artifactId, 'xml')]", xmlDoc)).isTrue();
    }

    @Test
    void matchNotFunction() {
        // not() with contains
        assertThat(match("not(contains(/root/element1, 'content1'))", namespacedXml)).isFalse();
        assertThat(match("not(contains(/root/element1, 'notfound'))", namespacedXml)).isTrue();

        // not() with starts-with
        assertThat(match("not(starts-with(/root/element1, 'content'))", namespacedXml)).isFalse();
        assertThat(match("not(starts-with(/root/element1, 'xyz'))", namespacedXml)).isTrue();

        // Double negation
        assertThat(match("not(not(contains(/root/element1, 'content1')))", namespacedXml)).isTrue();
    }

    @Test
    void matchStringLengthFunction() {
        // string-length with comparisons
        assertThat(match("string-length(/root/element1) > 0", namespacedXml)).isTrue();
        assertThat(match("string-length(/root/element1) > 2", namespacedXml)).isTrue();
        assertThat(match("string-length(/root/element1) > 100", namespacedXml)).isFalse();

        assertThat(match("string-length(/root/element1) < 100", namespacedXml)).isTrue();
        assertThat(match("string-length(/root/element1) < 5", namespacedXml)).isFalse();

        assertThat(match("string-length(/root/element1) = 8", namespacedXml)).isTrue(); // "content1" = 8 chars
        assertThat(match("string-length(/root/element1) = 7", namespacedXml)).isFalse();

        assertThat(match("string-length(/root/element1) >= 8", namespacedXml)).isTrue();
        assertThat(match("string-length(/root/element1) >= 9", namespacedXml)).isFalse();

        assertThat(match("string-length(/root/element1) <= 8", namespacedXml)).isTrue();
        assertThat(match("string-length(/root/element1) <= 7", namespacedXml)).isFalse();

        assertThat(match("string-length(/root/element1) != 7", namespacedXml)).isTrue();
        assertThat(match("string-length(/root/element1) != 8", namespacedXml)).isFalse();

        // string-length of non-existent path
        assertThat(match("string-length(/root/nonexistent) = 0", namespacedXml)).isTrue();
    }

    @Test
    void matchStartsWithFunction() {
        assertThat(match("starts-with(/root/element1, 'content')", namespacedXml)).isTrue();
        assertThat(match("starts-with(/root/element1, 'con')", namespacedXml)).isTrue();
        assertThat(match("starts-with(/root/element1, 'c')", namespacedXml)).isTrue();
        assertThat(match("starts-with(/root/element1, '')", namespacedXml)).isTrue();
        assertThat(match("starts-with(/root/element1, 'content1')", namespacedXml)).isTrue();

        assertThat(match("starts-with(/root/element1, 'ontent')", namespacedXml)).isFalse();
        assertThat(match("starts-with(/root/element1, '1')", namespacedXml)).isFalse();
        assertThat(match("starts-with(/root/element1, 'Content')", namespacedXml)).isFalse(); // case sensitive
    }

    @Test
    void matchEndsWithFunction() {
        assertThat(match("ends-with(/root/element1, '1')", namespacedXml)).isTrue();
        assertThat(match("ends-with(/root/element1, 'ent1')", namespacedXml)).isTrue();
        assertThat(match("ends-with(/root/element1, 'content1')", namespacedXml)).isTrue();
        assertThat(match("ends-with(/root/element1, '')", namespacedXml)).isTrue();

        assertThat(match("ends-with(/root/element1, 'content')", namespacedXml)).isFalse();
        assertThat(match("ends-with(/root/element1, '2')", namespacedXml)).isFalse();
        assertThat(match("ends-with(/root/element1, 'Content1')", namespacedXml)).isFalse(); // case sensitive
    }

    @Test
    void matchSubstringFunctions() {
        // substring-before
        assertThat(match("substring-before(/root/element1, '1') = 'content'", namespacedXml)).isTrue();
        assertThat(match("substring-before(/root/element1, 'tent') = 'con'", namespacedXml)).isTrue();
        assertThat(match("substring-before(/root/element1, 'c') = ''", namespacedXml)).isTrue(); // nothing before first char
        assertThat(match("substring-before(/root/element1, 'notfound') = ''", namespacedXml)).isTrue(); // delimiter not found

        // substring-after
        assertThat(match("substring-after(/root/element1, 'content') = '1'", namespacedXml)).isTrue();
        assertThat(match("substring-after(/root/element1, 'con') = 'tent1'", namespacedXml)).isTrue();
        assertThat(match("substring-after(/root/element1, '1') = ''", namespacedXml)).isTrue(); // nothing after last char
        assertThat(match("substring-after(/root/element1, 'notfound') = ''", namespacedXml)).isTrue(); // delimiter not found
    }

    @Test
    void matchCountFunction() {
        // TODO: count() with wildcard paths like count(/root/*) requires special handling
        // to count all matching nodes rather than evaluating path to text content.
        // For now, count() with a path that has text content returns 1 (truthy)
        assertThat(match("count(/root/element1)", namespacedXml)).isTrue(); // has content, so count >= 1
        assertThat(match("count(/root/element1) > 0", namespacedXml)).isTrue();

        // count of non-existent returns 0
        assertThat(match("count(/root/nonexistent) = 0", namespacedXml)).isTrue();
    }

    @Test
    void matchNestedFunctionCalls() {
        // Nested function calls
        assertThat(match("not(not(contains(/root/element1, 'content1')))", namespacedXml)).isTrue();
        assertThat(match("not(not(not(contains(/root/element1, 'content1'))))", namespacedXml)).isFalse();

        // contains with substring result
        assertThat(match("contains(substring-after(/root/element1, 'con'), 'tent')", namespacedXml)).isTrue();
    }

    @Test
    void matchFunctionWithDescendantPath() {
        // Using // descendant axis in function arguments
        assertThat(match("contains(//element1, 'content1')", namespacedXml)).isTrue();
        assertThat(match("contains(//element3, 'content3')", namespacedXml)).isTrue();
        assertThat(match("string-length(//element1) = 8", namespacedXml)).isTrue();
    }

    @Test
    void matchBooleanExpressionWithRelativePath() {
        // Relative paths in boolean expressions are evaluated from the cursor's context element
        // contains(element1, 'content1') matches at <root> because it has a child <element1> containing 'content1'
        assertThat(matchCount("contains(element1, 'content1')", namespacedXml)).isEqualTo(1); // matches at <root>

        // xmlDoc: <dependencies><dependency><groupId>org.openrewrite</groupId>...</dependency>...</dependencies>
        // contains(groupId, 'openrewrite') matches at <dependency> elements that have child <groupId> containing 'openrewrite'
        assertThat(matchCount("contains(groupId, 'openrewrite')", xmlDoc)).isEqualTo(1); // matches at first <dependency>
        assertThat(matchCount("contains(groupId, 'notfound')", xmlDoc)).isEqualTo(0); // no matches

        // Absolute paths: only match at root element
        // contains(/dependencies/dependency/groupId, 'openrewrite') - matches only at <dependencies> (root)
        assertThat(matchCount("contains(/dependencies/dependency/groupId, 'openrewrite')", xmlDoc)).isEqualTo(1);
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
    @Issue("https://github.com/openrewrite/rewrite/issues/6314")
    void matchTextFunctionCondition() {
        SourceFile xml = new XmlParser().parse(
          """
            <?xml version="1.0" encoding="UTF-8"?>
            <test>
              <foo>bar</foo>
              <foo>notBar</foo>
              <one>
                <two>two</two>
              </one>
              <one>
                <two>
                  <three>three</three>
                </two>
              </one>
              <foo/>
            </test>
            """
        ).toList().getFirst();

        // text() predicate should only match element with specific text content
        assertThat(match("/test/one[two/three/text()='three']", xml)).isTrue();
        assertThat(match("/test/one[two/three/text()='notthree']", xml)).isFalse();
        assertThat(match("/test/one[two/text()='two']", xml)).isTrue();
        assertThat(match("/test/one[two/text()='nottwo']", xml)).isFalse();
        assertThat(match("/test/foo[text()='bar']", xml)).isTrue();
        assertThat(match("/test/foo[text()='notBar']", xml)).isTrue();
        assertThat(match("/test/foo[text()='nonexistent']", xml)).isFalse();
        assertThat(match("//foo[text()='bar']", xml)).isTrue();
        assertThat(match("//foo[text()='notBar']", xml)).isTrue();
        assertThat(match("//foo[text()='nonexistent']", xml)).isFalse();

        // wildcard element with text() predicate
        assertThat(match("/test/*[text()='bar']", xml)).isTrue();
        assertThat(match("//*[text()='bar']", xml)).isTrue();
        assertThat(match("//*[text()='notBar']", xml)).isTrue();

        // combining text() with local-name()
        assertThat(match("//*[local-name()='foo' and text()='bar']", xml)).isTrue();
        assertThat(match("//*[local-name()='foo' and text()='notBar']", xml)).isTrue();
        assertThat(match("//*[local-name()='foo' and text()='nonexistent']", xml)).isFalse();

        // combining text() with or
        assertThat(match("/test/foo[text()='bar' or text()='notBar']", xml)).isTrue();
        assertThat(match("/test/foo[text()='nonexistent' or text()='bar']", xml)).isTrue();
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

    private boolean match(@Language("xpath") String xpath, SourceFile x) {
        return matchCount(xpath, x) > 0;
    }

    private int matchCount(String xpath, SourceFile x) {
        XPathMatcher matcher = new XPathMatcher(xpath);
        return (new XmlVisitor<AtomicInteger>() {
            @Override
            public Xml visitTag(Xml.Tag tag, AtomicInteger ctx) {
                if (matcher.matches(getCursor())) {
                    ctx.incrementAndGet();
                }
                return super.visitTag(tag, ctx);
            }

            @Override
            public Xml visitAttribute(Xml.Attribute attribute, AtomicInteger ctx) {
                if (matcher.matches(getCursor())) {
                    ctx.incrementAndGet();
                }
                return super.visitAttribute(attribute, ctx);
            }
        }).reduce(x, new AtomicInteger()).get();
    }

    @Test
    void matchNodeTypeTests() {
        // text() node type test - matches elements with text content
        assertThat(match("/root/element1/text()", namespacedXml)).isTrue();
        assertThat(match("//element1/text()", namespacedXml)).isTrue();
        assertThat(match("/root/parent/text()", namespacedXml)).isFalse(); // parent has child elements, not direct text
    }

    @Test
    void matchPositionalPredicates() {
        // xmlDoc has two <dependency> elements under <dependencies>
        // [1] selects the first dependency
        assertThat(matchCount("/dependencies/dependency[1]", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("/dependencies/dependency[2]", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("/dependencies/dependency[3]", xmlDoc)).isEqualTo(0); // only 2 dependencies

        // [last()] selects the last element
        assertThat(matchCount("/dependencies/dependency[last()]", xmlDoc)).isEqualTo(1);

        // position() function
        assertThat(matchCount("/dependencies/dependency[position()=1]", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("/dependencies/dependency[position()=2]", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("/dependencies/dependency[position()=3]", xmlDoc)).isEqualTo(0);

        // Combining positional with other predicates
        // The first dependency has groupId, the second doesn't
        assertThat(matchCount("/dependencies/dependency[1]/groupId", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("/dependencies/dependency[2]/groupId", xmlDoc)).isEqualTo(0);

        // position() with comparison operators
        assertThat(matchCount("/dependencies/dependency[position()>1]", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("/dependencies/dependency[position()<2]", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("/dependencies/dependency[position()>=1]", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("/dependencies/dependency[position()<=2]", xmlDoc)).isEqualTo(2);
    }

    @Test
    void matchPositionalWithOtherConditions() {
        // Combining positional predicates with attribute/element conditions
        // First dependency's artifactId has scope="compile", second has scope="test"
        assertThat(matchCount("/dependencies/dependency[1]/artifactId[@scope='compile']", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("/dependencies/dependency[2]/artifactId[@scope='test']", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("/dependencies/dependency[1]/artifactId[@scope='test']", xmlDoc)).isEqualTo(0);
    }

    @Test
    void matchParenthesizedPathExpressions() {
        // Parenthesized path expressions apply predicates to the entire result set
        // (/dependencies/dependency)[1] - first dependency from the entire document
        assertThat(matchCount("(/dependencies/dependency)[1]", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("(/dependencies/dependency)[2]", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("(/dependencies/dependency)[3]", xmlDoc)).isEqualTo(0);

        // (/dependencies/dependency)[last()] - last dependency
        assertThat(matchCount("(/dependencies/dependency)[last()]", xmlDoc)).isEqualTo(1);

        // With position() function
        assertThat(matchCount("(/dependencies/dependency)[position()=1]", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("(/dependencies/dependency)[position()=2]", xmlDoc)).isEqualTo(1);

        // Descendant axis in parenthesized expression
        assertThat(matchCount("(//dependency)[1]", xmlDoc)).isEqualTo(1);
        assertThat(matchCount("(//dependency)[last()]", xmlDoc)).isEqualTo(1);
    }

    @Test
    void matchAdvancedFilterExpressions() {
        // (/path/expr)[predicate]/trailing - filter expression with trailing path
        assertThat(matchCount("(/project/build/plugins/plugin)[1]/groupId", pomXml1)).isEqualTo(1);
        assertThat(matchCount("(/project/build/plugins/plugin)[1]/artifactId", pomXml1)).isEqualTo(1);
        assertThat(matchCount("(/project/build/plugins/plugin)[1]/configuration", pomXml1)).isEqualTo(1);
        assertThat(matchCount("(/project/build/plugins/plugin)[1]/configuration/source", pomXml1)).isEqualTo(1);

        // Test with attribute access in trailing path (not yet supported)
        // assertThat(matchCount("(/dependencies/dependency)[1]/artifactId/@scope", xmlDoc)).isEqualTo(1);
    }

    @Test
    void matchAbbreviatedSyntax() {
        // . means self (current node)
        assertThat(matchCount("/dependencies/./dependency", xmlDoc)).isEqualTo(2);
        assertThat(matchCount("/dependencies/dependency/.", xmlDoc)).isEqualTo(2);

        // .. means parent
        assertThat(matchCount("/dependencies/dependency/groupId/..", xmlDoc)).isEqualTo(1); // only first dependency has groupId
        assertThat(matchCount("/dependencies/dependency/groupId/../artifactId", xmlDoc)).isEqualTo(1);

        // Multiple parent references
        assertThat(matchCount("/dependencies/dependency/groupId/../../dependency", xmlDoc)).isEqualTo(2);
    }

    @Test
    void matchParentAxis() {
        // parent::node() - explicit parent axis with node() test
        assertThat(match("/dependencies/dependency/groupId/parent::node()", xmlDoc)).isTrue();
        assertThat(match("/dependencies/dependency/groupId/parent::dependency", xmlDoc)).isTrue();

        // parent with specific element name
        assertThat(match("/dependencies/dependency/parent::dependencies", xmlDoc)).isTrue();

        // self::node() - explicit self axis
        assertThat(match("/dependencies/self::node()", xmlDoc)).isTrue();
        assertThat(match("/dependencies/self::dependencies", xmlDoc)).isTrue();

        // Combined with other path steps
        assertThat(match("/dependencies/dependency/groupId/parent::dependency/artifactId", xmlDoc)).isTrue();
    }

    @Test
    void matchDescendantWithChildPath() {
        // //plugins/plugin - descendant axis followed by child axis
        // This is used by MavenPlugin matcher
        assertThat(match("//plugins/plugin", pomXml1)).isTrue();
        assertThat(match("//plugins/plugin/groupId", pomXml1)).isTrue();
        assertThat(match("//plugins/plugin/artifactId", pomXml1)).isTrue();
        assertThat(match("//plugins/plugin/configuration", pomXml1)).isTrue();
        assertThat(match("//plugins/plugin/configuration/source", pomXml1)).isTrue();

        // With pluginManagement
        assertThat(match("//plugins/plugin", pomXml2)).isTrue();
        assertThat(match("//pluginManagement/plugins/plugin", pomXml2)).isTrue();

        // Negative cases
        assertThat(match("//plugins/nonexistent", pomXml1)).isFalse();
        assertThat(match("//nonexistent/plugin", pomXml1)).isFalse();
    }

    @Test
    void matchDescendantStartingWithRootElement() {
        // //project/build/... - descendant axis where first step matches root element
        // This pattern is used by RemoveXmlTag and should match when root is <project>
        assertThat(match("//project/build", pomXml1)).isTrue();
        assertThat(match("//project/build/plugins", pomXml1)).isTrue();
        assertThat(match("//project/build/plugins/plugin", pomXml1)).isTrue();
        assertThat(match("//project/build/pluginManagement/plugins/plugin", pomXml2)).isTrue();

        // Should not match if the path doesn't exist
        assertThat(match("//project/build/pluginManagement", pomXml1)).isFalse();
        assertThat(match("//project/nonexistent", pomXml1)).isFalse();
    }

    @Test
    void matchRootElement() {
        // Single-step absolute path should match the root element
        assertThat(matchCount("/project", pomXml1)).isEqualTo(1);
        assertThat(matchCount("/dependencies", xmlDoc)).isEqualTo(1);

        // /project/parent should match the parent element
        SourceFile pomWithParent = new XmlParser().parse(
          """
            <project>
              <parent>
                <groupId>com.example</groupId>
              </parent>
              <version>1.0</version>
            </project>
            """
        ).toList().getFirst();
        assertThat(matchCount("/project/parent", pomWithParent)).isEqualTo(1);
        assertThat(matchCount("/project", pomWithParent)).isEqualTo(1);
    }

    @Test
    void matchRelativePathFromContext() {
        // Relative paths should match based on suffix, allowing match anywhere in document
        // This is important for ChangeTagValue which uses paths like "version"
        assertThat(match("version", pomXml1)).isTrue();
        assertThat(match("groupId", pomXml1)).isTrue();
        assertThat(match("artifactId", pomXml1)).isTrue();

        // Multi-step relative paths
        assertThat(match("configuration/source", pomXml1)).isTrue();
        assertThat(match("plugin/configuration", pomXml1)).isTrue();
        assertThat(match("plugins/plugin", pomXml1)).isTrue();

        // Negative cases - element names that don't exist
        assertThat(match("nonexistent", pomXml1)).isFalse();
        assertThat(match("configuration/nonexistent", pomXml1)).isFalse();
    }

}
