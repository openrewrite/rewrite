/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite;

import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@NonNull
public class RecipeTest {
    @Nullable
    protected Recipe recipe;

    @Nullable
    protected TreePrinter<?, ?> treePrinter;

    public void assertChanged(Parser<?> parser,
                             String before,
                             String after) {
        assertChanged(parser, recipe, before, after);
    }

    public void assertChanged(Parser<?> parser,
                             Recipe recipe,
                             String before,
                             String after) {
        assertThat(recipe).as("A recipe must be specified").isNotNull();

        SourceFile source = parser.parse(StringUtils.trimIndent(before)).iterator().next();

        List<Result> results = recipe.run(singletonList(source),
                ExecutionContext.builder()
                        .maxCycles(2)
                        .doOnError(t -> fail("Recipe threw an exception", t))
                        .build());

        if(results.isEmpty()) {
            fail("The recipe must make changes");
        }

        Optional<Result> result = results.stream()
                .filter(s -> source.equals(s.getBefore()))
                .findAny();

        //noinspection SimplifyOptionalCallChains
        if(!result.isPresent()) {
            fail("The recipe must make changes");
            return;
        }

        assertThat(result.get().getAfter()).isNotNull();
        assertThat(result.get().getAfter().printTrimmed(treePrinter)).isEqualTo(StringUtils.trimIndent(after));
    }

    public void assertUnchanged(Parser<?> parser, String before) {
        assertUnchanged(parser, recipe, before);
    }

    public void assertUnchanged(Parser<?> parser, Recipe recipe, String before) {
        assertThat(recipe).as("A recipe must be specified").isNotNull();

        SourceFile source = parser.parse(StringUtils.trimIndent(before)).iterator().next();

        List<Result> results = recipe.run(singletonList(source));
        assertThat(results).as("The recipe must not make changes").isEmpty();
    }
}
