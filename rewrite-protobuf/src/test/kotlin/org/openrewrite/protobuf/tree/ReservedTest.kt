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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.protobuf.ProtoParser
import org.openrewrite.protobuf.ProtoParserTest
import org.openrewrite.protobuf.tree.Proto.Range

class ReservedTest : ProtoParserTest() {

    @Test
    fun ranges() = assertUnchanged(
        before = """
            syntax = 'proto2';
            message MyMessage {
              reserved 1 to 10, 25, 12 to 20;
            }
        """
    )

    @Test
    fun reservedRangeValues() {
        val protoDoc = ProtoParser().parse("""
            syntax = 'proto2';
            message MyMessage {
              reserved 1 to 10, 25, 12 to 20;
            }
        """)[0]
        assertThat(protoDoc).isNotNull
        val message: Proto.Message = protoDoc.body[0] as Proto.Message
        val reserved: Proto.Reserved = message.body.statements[0] as Proto.Reserved
        val r1: Range = reserved.reservations?.get(0) as Range
        assertThat(r1.from.value).isEqualTo(1)
        assertThat(r1.to.value).isEqualTo(10)
        val r2: Range = reserved.reservations?.get(1) as Range
        assertThat(r2.from.value).isEqualTo(25)
        assertThat(r2.to).isNull()
        val r3: Range = reserved.reservations?.get(2) as Range
        assertThat(r3.from.value).isEqualTo(12)
        assertThat(r3.to.value).isEqualTo(20)
    }

    @Test
    fun stringLiterals() = assertUnchanged(
        before = """
            syntax = 'proto2';
            message MyMessage {
              reserved "1" , '10' ;
            }
        """
    )
}
