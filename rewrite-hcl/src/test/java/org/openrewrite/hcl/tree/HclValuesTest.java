/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.hcl.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.hcl.HclIsoVisitor;
import org.openrewrite.hcl.HclParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class HclValuesTest {

    @Test
    void staticallyKnownValues() {
        // given
        Map<String, Expression> values = attributeValues(
          """
            a = "hello"
            b = ("hello")
            c = ""
            d = 3
            e = 1.50
            f = true
            g = "with \\"escape\\""
            """
        );

        // when
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach((name, value) -> result.put(name, HclValues.stringValue(value)));

        // then
        assertThat(result).containsExactly(
          entry("a", "hello"),
          entry("b", "hello"),
          entry("c", ""),
          entry("d", "3"),
          entry("e", "1.50"),
          entry("f", "true"),
          entry("g", "with \\\"escape\\\"")
        );
    }

    @Test
    void valuesThatAreNotStaticallyKnown() {
        // given
        Map<String, Expression> values = attributeValues(
          """
            a = "${var.a}b"
            b = "${var.a}"
            c = var.a
            d = null
            e = ["a"]
            f = <<EOT
            hello
            EOT
            """
        );

        // when
        List<String> result = new ArrayList<>();
        values.values().forEach(value -> result.add(HclValues.stringValue(value)));

        // then
        assertThat(result).hasSize(6).containsOnlyNulls();
    }

    @Test
    void labelAndBlockNames() {
        // given
        Hcl.Block block = (Hcl.Block) parse(
          """
            resource "aws_instance" web {
            }
            """
        ).getBody().get(0);

        // when
        List<String> labels = new ArrayList<>();
        block.getLabels().forEach(label -> labels.add(label.getSimpleName()));

        // then
        assertThat(block.getSimpleName()).isEqualTo("resource");
        assertThat(labels).containsExactly("aws_instance", "web");
        assertThat(block.getLabelName(0)).isEqualTo("aws_instance");
        assertThat(block.getLabelName(1)).isEqualTo("web");
        assertThat(block.getLabelName(2)).isNull();
        assertThat(block.getLabelName(-1)).isNull();
    }

    @Test
    void blockWithoutTypeHasNoSimpleName() {
        // given
        Hcl.Block block = (Hcl.Block) parse(
          """
            locals {
            }
            """
        ).getBody().get(0);

        // when
        Hcl.Block anonymous = block.withType(null);

        // then
        assertThat(anonymous.getSimpleName()).isNull();
        assertThat(anonymous.getLabelName(0)).isNull();
    }

    @Test
    void attributeValueKeepsInterpolations() {
        // given
        Hcl.Block block = (Hcl.Block) parse(
          """
            provider {
              key = "prefix-${var.a}-suffix"
            }
            """
        ).getBody().get(0);

        // when
        String value = block.getAttributeValue("key");

        // then
        assertThat(value).isEqualTo("prefix-${var.a}-suffix");
    }

    private static Hcl.ConfigFile parse(String hcl) {
        return (Hcl.ConfigFile) HclParser.builder().build()
          .parse(new InMemoryExecutionContext(e -> {
              throw new AssertionError(e);
          }), hcl)
          .findFirst()
          .orElseThrow();
    }

    private static Map<String, Expression> attributeValues(String hcl) {
        Map<String, Expression> values = new LinkedHashMap<>();
        new HclIsoVisitor<Integer>() {
            @Override
            public Hcl.Attribute visitAttribute(Hcl.Attribute attribute, Integer p) {
                values.put(attribute.getSimpleName(), attribute.getValue());
                return attribute;
            }
        }.visit(parse(hcl), 0);
        return values;
    }
}
