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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.protobuf.ProtoVisitor;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.protobuf.Assertions.proto;

class FieldTest implements RewriteTest {

    @Test
    void primitive() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              message MyMessage {
                optional uint32 age = 1;
              }
              """
          )
        );
    }

    @Test
    void fieldNamedGroup() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              message MyMessage {
                optional string group = 8;
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "bool", "bytes", "double", "edition", "enum", "extend", "extensions",
      "fixed32", "fixed64", "float", "group", "import", "int32", "int64", "map",
      "max", "message", "oneof", "option", "optional", "package", "public",
      "repeated", "required", "reserved", "returns", "rpc", "service", "sfixed32",
      "sfixed64", "sint32", "sint64", "stream", "string", "syntax", "to",
      "uint32", "uint64", "weak"
    })
    void keywordUsedAsIdentifier(String keyword) {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              message MyMessage {
                optional bool %1$s = 1;
              }
              enum MyEnum {
                %1$s = 1;
              }
              """.formatted(keyword)
          )
        );
    }

    @Test
    void hexadecimalFieldNumber() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              message MyMessage {
                optional int32 f = 0x10;
              }
              """,
            spec -> spec.beforeRecipe(protoDoc -> {
                List<Object> numbers = TreeVisitor.collect(new ProtoVisitor<>() {
                    @Override
                    public Proto visitField(Proto.Field field, ExecutionContext ctx) {
                        return SearchResult.found(field);
                    }
                }, protoDoc, new ArrayList<>(), Proto.Field.class, f -> f.getNumber().getValue());

                assertThat(numbers).containsExactly(16);
            }))
        );
    }

    @Test
    void fullIdent() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              message MyMessage {
                optional MyMessage msg = 1;
              }
              """
          )
        );
    }

    @Test
    void fieldOption() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              message MyMessage {
                repeated int32 samples = 4 [ packed = true ];
              }
              """
          )
        );
    }
}
