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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.yaml.YamlIsoVisitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
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
          yaml(
                """
            foo:
              bar: >
                A multiline string.
              baz:
                quz: Another string.
            """,
                spec -> spec.afterRecipe(doc -> new YamlIsoVisitor<>() {
                    @Override
                    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Object o) {
                        if ("baz".equals(entry.getKey().getValue())) {
                            // After a literal/folded scalar, the trailing newline is kept in the
                            // scalar value, so the next entry's prefix is just indentation
                            assertThat(entry.getPrefix()).isEqualTo("  ");
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
          yaml(
                """
            foo # look mom, no mapping
            """,
                spec -> spec.afterRecipe(documents -> {
                    Yaml.Block maybeScalar = documents.getDocuments().getFirst().getBlock();
                    Assertions.assertThat(maybeScalar).isInstanceOf(Yaml.Scalar.class);
                }))
        );
    }

    @Nested
    class BlockScalarBody {

        private Yaml.Scalar literal(String value) {
            return new Yaml.Scalar(randomId(), "", Markers.EMPTY, Yaml.Scalar.Style.LITERAL, null, null, value);
        }

        @Test
        void getBodyStripsCrFromLfBody() {
            Yaml.Scalar s = literal("\n  line one\n  line two\n");
            assertThat(s.getBody()).isEqualTo("line one\nline two");
        }

        @Test
        void getBodyStripsCrFromCrlfBody() {
            Yaml.Scalar s = literal("\r\n  line one\r\n  line two\r\n");
            assertThat(s.getBody()).isEqualTo("line one\nline two");
        }

        @Test
        void withBodyKeepsLfForLfScalar() {
            Yaml.Scalar s = literal("\n  line one\n  line two\n");
            assertThat(s.withBody("new one\nnew two").getValue()).isEqualTo("\n  new one\n  new two\n");
        }

        @Test
        void withBodyEmitsCrlfForCrlfScalar() {
            Yaml.Scalar s = literal("\r\n  line one\r\n  line two\r\n");
            assertThat(s.withBody("new one\nnew two").getValue()).isEqualTo("\r\n  new one\r\n  new two\r\n");
        }
    }
}
