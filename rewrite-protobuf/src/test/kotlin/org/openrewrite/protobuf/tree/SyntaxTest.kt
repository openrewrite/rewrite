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
package org.openrewrite.protobuf.tree

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.protobuf.ProtoParserTest

class SyntaxTest : ProtoParserTest() {

    @Test
    fun syntax() = assertUnchanged(
        before = """
            syntax = 'proto2' ;
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1790")
    @Test
    fun protoNPE() = assertUnchanged(
        before = """
            syntax = "proto2";

            import "google/protobuf/wrappers.proto";

            option java_package = "io.github.murdos.easyrandom.protobuf.testing.proto2";
            option java_multiple_files = true;

            message Proto2Message {
              reserved 99;
              reserved "reservedField";

              required double doubleField = 1;
              required float floatField = 2;
              required int32 int32Field = 3;
              required int64 int64Field = 4;
              required uint32 uint32Field = 5;
              required uint64 uint64Field = 6;
              required sint32 sint32Field = 7;
              required sint64 sint64Field = 8;
              required fixed32 fixed32Field = 9;
              required fixed64 fixed64Field = 10;
              required sfixed32 sfixed32Field = 11;
              required sfixed64 sfixed64Field = 12;
              required bool boolField = 13;
              required string stringField = 14;
              required bytes bytesField = 15;
              required Proto2Enum enumField = 16;
              required google.protobuf.StringValue stringValueField = 17;
              repeated string repeatedStringField = 18;
              required EmbeddedProto2Message embeddedMessage = 19;
              map<string, EmbeddedProto2Message> mapField = 20;

              oneof oneofField {
                double firstChoice = 30;
                string secondChoice = 31;
                Proto2Enum thirdChoice = 32;
                EmbeddedProto2Message forthChoice = 33;
              }
            }

            enum Proto2Enum {
              THIRD_VALUE = 1;
              FOURTH_VALUE = 2;
            }

            message EmbeddedProto2Message {
              required string stringField = 1;
              required Proto2Enum enumField = 2;
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1790")

    @Test
    fun protoNPE2() = assertUnchanged(
        before = """
            syntax = "proto2";
            message Proto2Message {
              reserved 99;
            }
        """
    )
}
