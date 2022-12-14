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
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.protobuf.ProtoVisitor;
import org.openrewrite.protobuf.tree.Proto.Range;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.protobuf.Assertions.proto;

class ReservedTest implements RewriteTest {

    @Test
    void ranges() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              message MyMessage {
                reserved 1 to 10, 25, 12 to 20;
              }
              """
          )
        );
    }

    @Test
    void reservedRangeValues() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              message MyMessage {
                reserved 1 to 10, 25, 12 to 20;
              }
              """,
            spec -> spec.beforeRecipe(protoDoc -> {
                List<String> ranges = TreeVisitor.collect(new ProtoVisitor<>() {
                    @Override
                    public Proto visitRange(Range range, ExecutionContext ctx) {
                        return SearchResult.found(range);
                    }
                }, protoDoc, new ArrayList<>(), Range.class, r -> r.getFrom().getValue() + "->" +
                                                                  Optional.ofNullable(r.getTo()).map(Proto.Constant::getValue).orElse("null"));

                assertThat(ranges).containsExactly("1->10", "25->null", "12->20");
            }))
        );
    }

    @Test
    void stringLiterals() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              message MyMessage {
                reserved "1" , '10' ;
              }
              """
          )
        );
    }
}
