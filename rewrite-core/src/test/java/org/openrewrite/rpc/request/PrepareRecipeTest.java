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
import org.openrewrite.*;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.rpc.internal.PreparedRecipeCache;
import org.openrewrite.text.ChangeText;
import org.openrewrite.text.Find;
import org.openrewrite.text.PlainText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
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

    @Test
    void remoteSchedulerEditsOnlyTheSourceFilesThatMeetTheEditPreconditions() throws Exception {
        DeclarativeRecipe recipe = new DeclarativeRecipe("test.Gated", "Gated", "Gated.", emptySet(), null,
          URI.create("test"), false, emptyList());
        recipe.addPrecondition(new Find("x", null, null, null, null, null, null, null));
        recipe.addUninitialized(new ChangeText("changed"));
        recipe.initialize(emptyList());
        PreparedRecipeCache cache = new PreparedRecipeCache();
        PrepareRecipeResponse prepared = (PrepareRecipeResponse) new PrepareRecipe.Handler(cache, (id, options) -> recipe)
          .handle(new PrepareRecipe("test.Gated", emptyMap(), null, null));

        assertThat(editInTreeOrder(prepared, cache, PlainText.builder().sourcePath(Path.of("x.txt")).text("x").build()).getText()).isEqualTo("changed");
        assertThat(editInTreeOrder(prepared, cache, PlainText.builder().sourcePath(Path.of("y.txt")).text("y").build()).getText()).isEqualTo("y");
    }

    private static PlainText editInTreeOrder(PrepareRecipeResponse prepared, PreparedRecipeCache cache, PlainText text) {
        return editInTreeOrder(prepared, cache, text, new InMemoryExecutionContext());
    }

    private static PlainText editInTreeOrder(PrepareRecipeResponse prepared, PreparedRecipeCache cache, PlainText text,
                                             ExecutionContext ctx) {
        TreeVisitor<?, Object> editor = cache.instantiateVisitor(prepared.getEditVisitor(), null);
        Object p = Visit.visitorP(ctx, prepared.getEditVisitor(), cache);
        PlainText edited = (PlainText) requireNonNull(editor.visit(text, p, new Cursor(null, Cursor.ROOT_VALUE)));
        for (PrepareRecipeResponse child : requireNonNull(prepared.getRecipeList())) {
            edited = editInTreeOrder(child, cache, edited, ctx);
        }
        return edited;
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
