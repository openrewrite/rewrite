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
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class XPathMatcherTest {

    private final Xml.Document xmlDoc = new XmlParser().parse(
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
    ).findFirst().orElseThrow(() -> new IllegalArgumentException("Could not parse as XML"));

    private final Xml.Document pomXml = new XmlParser().parse(
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
          pomXml)).isTrue();
        assertThat(match("/project/build/plugins/plugin[artifactId='maven-compiler-plugin']/configuration/source",
          pomXml)).isTrue();
        assertThat(match("/project/build/plugins/plugin[artifactId='somethingElse']/configuration/source",
          pomXml)).isFalse();
    }

    private boolean match(String xpath, Xml.Document x) {
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
