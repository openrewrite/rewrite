/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.kotlin;


import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.kotlin.internal.KotlinParsingException;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

import static org.openrewrite.java.Assertions.sourceSet;
import static org.openrewrite.test.SourceSpecs.dir;

public final class Assertions {

    private Assertions() {
    }

    static void customizeExecutionContext(ExecutionContext ctx) {
        if (ctx.getMessage(KotlinParser.SKIP_SOURCE_SET_TYPE_GENERATION) == null) {
            ctx.putMessage(KotlinParser.SKIP_SOURCE_SET_TYPE_GENERATION, true);
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
                Assertions::customizeExecutionContext
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
                Assertions::customizeExecutionContext).after(s -> after);
        acceptSpec(spec, kotlin);
        return kotlin;
    }

    public static SourceSpecs srcMainKotlin(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... kotlinSources) {
        return dir("src/main/kotlin", spec, kotlinSources);
    }

    public static SourceSpecs srcMainKotlin(SourceSpecs... kotlinSources) {
        return srcMainKotlin(spec -> sourceSet(spec, "main"), kotlinSources);
    }

    public static SourceSpecs srcTestKotlin(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... kotlinSources) {
        return dir("src/test/kotlin", spec, kotlinSources);
    }

    public static SourceSpecs srcTestKotlin(SourceSpecs... kotlinSources) {
        return srcTestKotlin(spec -> sourceSet(spec, "test"), kotlinSources);
    }

    private static void acceptSpec(Consumer<SourceSpec<K.CompilationUnit>> spec, SourceSpec<K.CompilationUnit> kotlin) {
        Consumer<K.CompilationUnit> userSuppliedAfterRecipe = kotlin.getAfterRecipe();
        kotlin.afterRecipe(userSuppliedAfterRecipe::accept);
        isFullyParsed().andThen(spec).accept(kotlin);
    }

    public static Consumer<SourceSpec<K.CompilationUnit>> isFullyParsed() {
        return spec -> spec.afterRecipe(cu -> {
            new KotlinIsoVisitor<Integer>() {
                @Override
                public J visitUnknown(J.Unknown unknown, Integer integer) {
                    throw new KotlinParsingException("Parsing error, J.Unknown detected", new RuntimeException());
                }

                @Override
                public Space visitSpace(Space space, Space.Location loc, Integer integer) {
                    if (!space.getWhitespace().trim().isEmpty()) {
                        throw new KotlinParsingException("Parsing error detected, whitespace contains non-whitespace characters: " + space.getWhitespace(), new RuntimeException());
                    }
                    return super.visitSpace(space, loc, integer);
                }
            }.visit(cu, 0);
        });
    }
}
