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
package org.openrewrite.kotlin.tree;

import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Space;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserAssertions {

    private ParserAssertions() {
    }

    static void customizeExecutionContext(ExecutionContext ctx) {
        if (ctx.getMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION) == null) {
            ctx.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, true);
        }
    }

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before) {
        return kotlin(before, s -> {
        });
    }

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before, Consumer<SourceSpec<K.CompilationUnit>> spec) {
        SourceSpec<K.CompilationUnit> kotlin = new SourceSpec<>(
                K.CompilationUnit.class, null, KotlinParser.builder(), before,
                SourceSpec.EachResult.noop,
                ParserAssertions::customizeExecutionContext
        );
        acceptSpec(spec, kotlin);
        return kotlin;
    }

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before, @Language("kotlin") String after) {
        return kotlin(before, after, s -> {
        });
    }

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before, @Language("kotlin") String after,
                                     Consumer<SourceSpec<K.CompilationUnit>> spec) {
        SourceSpec<K.CompilationUnit> kotlin = new SourceSpec<>(K.CompilationUnit.class, null, KotlinParser.builder(), before,
                SourceSpec.EachResult.noop,
                ParserAssertions::customizeExecutionContext).after(s -> after);
        acceptSpec(spec, kotlin);
        return kotlin;
    }

    private static void acceptSpec(Consumer<SourceSpec<K.CompilationUnit>> spec, SourceSpec<K.CompilationUnit> kotlin) {
        Consumer<K.CompilationUnit> userSuppliedAfterRecipe = kotlin.getAfterRecipe();
        kotlin.afterRecipe(userSuppliedAfterRecipe::accept);
        spec.andThen(isFullyParsed()).accept(kotlin);
    }

    public static Consumer<SourceSpec<K.CompilationUnit>> isFullyParsed() {
        return spec -> spec.afterRecipe(cu -> new JavaVisitor<Integer>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer integer) {
                assertThat(space.getWhitespace().trim()).isEmpty();
                return super.visitSpace(space, loc, integer);
            }
        }.visit(cu, 0));
    }
}
