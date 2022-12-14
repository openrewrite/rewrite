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

    private final Xml.Document x = new XmlParser().parse(
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
    ).get(0);

    @Test
    void matchAbsolute() {
        assertThat(match("/dependencies/dependency")).isTrue();
        assertThat(match("/dependencies/*/artifactId")).isTrue();
        assertThat(match("/dependencies/*")).isTrue();
        assertThat(match("/dependencies/dne")).isFalse();
    }

    @Test
    void matchAbsoluteAttribute() {
        assertThat(match("/dependencies/dependency/artifactId/@scope")).isTrue();
        assertThat(match("/dependencies/dependency/artifactId/@scope")).isTrue();
        assertThat(match("/dependencies/dependency/artifactId/@*")).isTrue();
        assertThat(match("/dependencies/dependency/groupId/@*")).isFalse();
    }

    @Test
    void matchRelative() {
        assertThat(match("dependencies")).isTrue();
        assertThat(match("dependency")).isTrue();
        assertThat(match("//dependency")).isTrue();
        assertThat(match("dependency/*")).isTrue();
        assertThat(match("dne")).isFalse();
    }

    @Test
    void matchRelativeAttribute() {
        assertThat(match("dependency/artifactId/@scope")).isTrue();
        assertThat(match("dependency/artifactId/@*")).isTrue();
        assertThat(match("//dependency/artifactId/@scope")).isTrue();
    }

    private boolean match(String xpath) {
        XPathMatcher matcher = new XPathMatcher(xpath);
        return !TreeVisitor.collect(new XmlVisitor<>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if(matcher.matches(getCursor())) {
                    return SearchResult.found(tag);
                }
                return super.visitTag(tag, ctx);
            }

            @Override
            public Xml visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
                if(matcher.matches(getCursor())) {
                    return SearchResult.found(attribute);
                }
                return super.visitAttribute(attribute, ctx);
            }
        }, x, new ArrayList<>()).isEmpty();
    }
}
