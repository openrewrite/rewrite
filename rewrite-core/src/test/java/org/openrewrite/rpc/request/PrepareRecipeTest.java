/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.rpc.request;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.moderne.jsonrpc.JsonRpcRequest;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.rpc.internal.PreparedRecipeCache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PrepareRecipeTest {

    @Test
    void causesAnotherCycleOmittedForHostsThatDidNotAskForIt() throws IOException {
        assertThat(wire(PrepareRecipe.Handler.prepareTree(new CausesAnotherCycle(), new PreparedRecipeCache(), false)))
          .doesNotContain("causesAnotherCycle");
    }

    @Test
    void causesAnotherCycleOmittedWhenFalse() throws IOException {
        assertThat(wire(PrepareRecipe.Handler.prepareTree(new org.openrewrite.text.ChangeText("hi"), new PreparedRecipeCache(), true)))
          .doesNotContain("causesAnotherCycle");
    }

    @Test
    void causesAnotherCycleSentWhenRequested() throws IOException {
        assertThat(wire(PrepareRecipe.Handler.prepareTree(new CausesAnotherCycle(), new PreparedRecipeCache(), true)))
          .contains("\"causesAnotherCycle\":true");
    }

    private static String wire(PrepareRecipeResponse response) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonMessageFormatter(new SimpleModule()).serialize(JsonRpcRequest.newRequest("PrepareRecipe", response), out);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    static class CausesAnotherCycle extends Recipe {
        @Override
        public String getDisplayName() {
            return "Causes another cycle";
        }

        @Override
        public String getDescription() {
            return "Causes another cycle.";
        }

        @Override
        public boolean causesAnotherCycle() {
            return true;
        }
    }
}
