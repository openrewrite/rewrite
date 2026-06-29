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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.protobuf.Assertions.proto;

class GroupTest implements RewriteTest {

    @Test
    void group() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              message Test {
                 optional group OptionalGroup = 4 {
                   required string RequiredField = 5;
                 }
              }
              """
          )
        );
    }

    @Test
    void groupWithoutSyntax() {
        rewriteRun(
          proto(
            """
              message Test {
                 required string label = 1;
                 optional int32 type = 2[default=77];
                 repeated int64 reps = 3;
                 optional group OptionalGroup = 4{
                   required string RequiredField = 5;
                 }
              }
              """
          )
        );
    }

    @SuppressWarnings("ProtoFileSyntax")
    @Test
    void protoExample() {
        // The exact source that previously failed print idempotency (proto2 group field)
        rewriteRun(
          proto(
            """
              package protoexample;

              enum FOO {X=17;};

              message Test {
                 required string label = 1;
                 optional int32 type = 2[default=77];
                 repeated int64 reps = 3;
                 optional group OptionalGroup = 4{
                   required string RequiredField = 5;
                 }
              }
              """
          )
        );
    }

    @Test
    void repeatedGroup() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              message Test {
                 repeated group Result = 1 {
                   required string url = 2;
                   optional string title = 3;
                 }
              }
              """
          )
        );
    }

    @Test
    void nestedGroup() {
        rewriteRun(
          proto(
            """
              syntax = 'proto2';
              message Test {
                 optional group Outer = 1 {
                   optional group Inner = 2 {
                     required int32 value = 3;
                   }
                 }
              }
              """
          )
        );
    }
}
