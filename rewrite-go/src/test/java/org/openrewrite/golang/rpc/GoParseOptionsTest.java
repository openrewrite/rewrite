/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.golang.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class GoParseOptionsTest {

    @Test
    void printIdempotencyIsRequestedByDefault() {
        assertThat(GoRewriteRpc.parseOptions(new InMemoryExecutionContext()))
                .containsEntry(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, "true");
    }

    @Test
    void printIdempotencyFollowsTheExecutionContext() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);

        assertThat(GoRewriteRpc.parseOptions(ctx))
                .containsEntry(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, "false");
    }

    @Test
    void optionsSerializeUnderTheKeyTheGoServerReads() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // A rename on either side falls back to the default rather than failing.

        assertThat(mapper.writeValueAsString(new GoParseRequest(Collections.emptyList(), null, null, null,
                Collections.singletonMap(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, "false"))))
                .contains("\"options\":{\"org.openrewrite.requirePrintEqualsInput\":\"false\"}");

        assertThat(mapper.writeValueAsString(new ParseProject(java.nio.file.Paths.get("."), null, null,
                Collections.singletonMap(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, "false"))))
                .contains("\"options\":{\"org.openrewrite.requirePrintEqualsInput\":\"false\"}");
    }
}
