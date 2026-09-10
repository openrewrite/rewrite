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
package org.openrewrite.python.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;

import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class PythonParseOptionsTest {

    @Test
    void printIdempotencyIsRequestedByDefault() {
        assertThat(PythonRewriteRpc.parseOptions(new InMemoryExecutionContext()))
                .containsEntry(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, "true");
    }

    @Test
    void printIdempotencyFollowsTheExecutionContext() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);

        assertThat(PythonRewriteRpc.parseOptions(ctx))
                .containsEntry(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, "false");

        assertThat(PythonRewriteRpc.parseOptions(ctx, "2.7"))
                .containsEntry(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, "false")
                .containsEntry("languageLevel", "2.7");
    }

    /**
     * The server matches this key by its literal spelling, and a spelling it does not
     * recognize reads as the default rather than as an error — so a rename on either
     * side turns the check silently back on instead of failing.
     */
    @Test
    void theOptionsFieldCarriesTheKeyLiteralThePythonServerMatches() throws Exception {
        assertThat(new ObjectMapper().writeValueAsString(new ParseProject(Paths.get("."), null, null, null,
                Collections.singletonMap(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, "false"))))
                .contains("\"options\":{\"org.openrewrite.requirePrintEqualsInput\":\"false\"}");
    }
}
