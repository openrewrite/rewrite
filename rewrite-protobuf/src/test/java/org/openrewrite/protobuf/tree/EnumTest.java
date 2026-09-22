/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.protobuf.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.protobuf.ProtoVisitor;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.protobuf.Assertions.proto;

class EnumTest implements RewriteTest {

    @Test
    void one() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              enum MyEnum {
                One = 1;
              }
              """
          )
        );
    }

    @Test
    void negativeValues() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              enum MyEnum {
                Negative = -1;
                SpacedNegative = - 2;
              }
              """,
            spec -> spec.beforeRecipe(protoDoc -> {
                List<Object> values = TreeVisitor.collect(new ProtoVisitor<>() {
                    @Override
                    public Proto visitEnumField(Proto.EnumField enumField, ExecutionContext ctx) {
                        return SearchResult.found(enumField);
                    }
                }, protoDoc, new ArrayList<>(), Proto.EnumField.class, f -> f.getNumber().getValue());

                assertThat(values).containsExactly(-1, -2);
            }))
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1807")
    @Test
    void multipleValues() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              enum MyEnum {
                One = 1;
                Two = 2;
                Three = 3;
              }
              """
          )
        );
    }
}
