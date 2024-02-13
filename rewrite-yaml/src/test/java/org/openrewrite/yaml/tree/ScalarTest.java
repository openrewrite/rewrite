/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.yaml.tree;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.yaml.YamlIsoVisitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.Assertions.yaml;

class ScalarTest implements RewriteTest {

    @Test
    void multilineScalar() {
        rewriteRun(
          yaml(
            """
              key: value that spans
                multiple lines
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3531")
    @Test
    void multilineString() {
        rewriteRun(
          yaml("""
            foo:
              bar: >
                A multiline string.
              baz:
                quz: Another string.
            """, spec -> spec.afterRecipe(doc -> new YamlIsoVisitor<>() {
                @Override
                public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Object o) {
                    if ("baz".equals(entry.getKey().getValue())) {
                        assertThat(entry.getPrefix()).isEqualTo("\n  ");
                    }
                    return super.visitMappingEntry(entry, o);
                }
            }.visit(doc, 0))
          )
        );
    }

    @Test
    void loneScalar() {
        rewriteRun(
          yaml("""
            foo # look mom, no mapping
            """, spec -> spec.afterRecipe(documents -> {
                Yaml.Block maybeScalar = documents.getDocuments().get(0).getBlock();
                Assertions.assertThat(maybeScalar).isInstanceOf(Yaml.Scalar.class);
          }))
        );
    }
}
