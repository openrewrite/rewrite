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


import kotlin.Suppress;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ThrowingConsumer;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.kotlin.tree.KSpace;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.UncheckedConsumer;
import org.opentest4j.AssertionFailedError;

import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.openrewrite.java.Assertions.sourceSet;
import static org.openrewrite.test.SourceSpecs.dir;

@SuppressWarnings({"unused", "unchecked", "OptionalGetWithoutIsPresent", "DataFlowIssue"})
public final class Assertions {

    private Assertions() {
    }

    static void customizeExecutionContext(ExecutionContext ctx) {
        if (ctx.getMessage(KotlinParser.SKIP_SOURCE_SET_TYPE_GENERATION) == null) {
            ctx.putMessage(KotlinParser.SKIP_SOURCE_SET_TYPE_GENERATION, true);
        }
    }

    // A helper method to adjust white spaces in the input kotlin source to help us detect parse-to-print idempotent issues
    // Just change from `before` to `adjustSpaces(before)` below in the `kotlin()` method to test locally
    @SuppressWarnings("IfStatementWithIdenticalBranches")
    @Nullable
    private static String adjustSpaces(@Nullable String input) {
        if (input == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int count = 0;
        int limit = 1;
        char pre = 0;
        for (char c : input.toCharArray()) {
            if (c == ' ') {
                if (pre == ' ') {
                    count++;
                    if (count <= limit) {
                        out.append(c);
                    }
                } else {
                    count++;
                    out.append(c);
                }
            } else {
                if (pre == ' ') {
                    for (int i = count; i < limit; i++) {
                        out.append(' ');
                    }
                    count = 0;
                    limit++;
                    if (limit > 5) {
                        limit = 1;
                    }
                }
                out.append(c);
            }
            pre = c;
        }

        return out.toString();
    }

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before) {
        // Change `before` to `adjustSpaces(before)` to test spaces locally here
        return kotlin(before, s -> {
        });
    }

    public static SourceSpecs kotlinScript(@Language("kts") @Nullable String before) {
        return kotlinScript(before, s -> {
        });
    }

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before, Consumer<SourceSpec<K.CompilationUnit>> spec) {
        SourceSpec<K.CompilationUnit> kotlin = new SourceSpec<>(
                K.CompilationUnit.class, null, KotlinParser.builder(), before,
                SourceSpec.ValidateSource.noop,
                Assertions::customizeExecutionContext
        );
        acceptSpec(spec, kotlin);
        return kotlin;
    }

    public static SourceSpecs kotlinScript(@Language("kts") @Nullable String before, Consumer<SourceSpec<K.CompilationUnit>> spec) {
        SourceSpec<K.CompilationUnit> kotlinScript = new SourceSpec<>(
                K.CompilationUnit.class, null, KotlinParser.builder().isKotlinScript(true), before,
                SourceSpec.ValidateSource.noop,
                Assertions::customizeExecutionContext
        );
        acceptSpec(spec, kotlinScript);
        return kotlinScript;
    }

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before, @Language("kotlin") String after) {
        return kotlin(before, after, s -> {
        });
    }

    public static SourceSpecs kotlin(@Language("kotlin") @Nullable String before, @Language("kotlin") String after,
                                     Consumer<SourceSpec<K.CompilationUnit>> spec) {
        SourceSpec<K.CompilationUnit> kotlin = new SourceSpec<>(K.CompilationUnit.class, null, KotlinParser.builder(), before,
                SourceSpec.ValidateSource.noop,
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
//        Consumer<K.CompilationUnit> consumer = kotlin.getAfterRecipe().andThen(isFullyParsed()).andThen(spaceConscious(kotlin));
        Consumer<K.CompilationUnit> consumer = kotlin.getAfterRecipe().andThen(isFullyParsed());
        kotlin.afterRecipe(consumer::accept);
        spec.accept(kotlin);
    }

    public static ThrowingConsumer<K.CompilationUnit> isFullyParsed() {
        return cu -> new KotlinIsoVisitor<Integer>() {
            @Override
            public J visitUnknown(J.Unknown unknown, Integer integer) {
                throw new AssertionFailedError("Parsing error, J.Unknown detected");
            }

            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer integer) {
                if (!space.getWhitespace().trim().isEmpty()) {
                    throw new AssertionFailedError("Parsing error detected, whitespace contains non-whitespace characters: " + space.getWhitespace());
                }
                return super.visitSpace(space, loc, integer);
            }
        }.visit(cu, 0);
    }

    public static UncheckedConsumer<SourceSpec<?>> spaceConscious() {
        return source -> {
            if (source.getSourceFileType() == K.CompilationUnit.class) {
                SourceSpec<K.CompilationUnit> kotlinSourceSpec = (SourceSpec<K.CompilationUnit>) source;
                kotlinSourceSpec.afterRecipe(spaceConscious(kotlinSourceSpec));
            }
        };
    }

    public static ThrowingConsumer<K.CompilationUnit> spaceConscious(SourceSpec<K.CompilationUnit> spec) {
        return cu -> {
            K.CompilationUnit visited = (K.CompilationUnit) new KotlinIsoVisitor<Integer>() {
                int id = 0;

                @Override
                public Space visitSpace(Space space, KSpace.Location loc, Integer integer) {
                    return next(space);
                }

                @NotNull
                private Space next(Space space) {
                    if (!space.getComments().isEmpty()) {
                        return space;
                    }
                    return space.withComments(Collections.singletonList(new TextComment(true, Integer.toString(id++), "", Markers.EMPTY)));
                }

                @Override
                public Space visitSpace(Space space, Space.Location loc, Integer integer) {
                    Cursor parentCursor = getCursor().getParentOrThrow();
                    if (loc == Space.Location.IDENTIFIER_PREFIX && parentCursor.getValue() instanceof J.Annotation) {
                        return space;
                    } else if (loc == Space.Location.IDENTIFIER_PREFIX && parentCursor.getValue() instanceof J.Break &&
                            ((J.Break) parentCursor.getValue()).getLabel() == getCursor().getValue()) {
                        return space;
                    } else if (loc == Space.Location.IDENTIFIER_PREFIX && parentCursor.getValue() instanceof K.KReturn &&
                            ((K.KReturn) parentCursor.getValue()).getLabel() == getCursor().getValue()) {
                        return space;
                    } else if (loc == Space.Location.LABEL_SUFFIX) {
                        return space;
                    } else if (getCursor().firstEnclosing(J.Import.class) != null) {
                        return space;
                    } else if (getCursor().firstEnclosing(J.Package.class) != null) {
                        return space;
                    }
                    return next(space);
                }
            }.visit(cu, 0);
            try {
                String s = visited.printAll();
                InMemoryExecutionContext ctx = new InMemoryExecutionContext();
                ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);
                SourceFile cu2 = spec.getParser().build().parse(ctx, s).findFirst().get();
                String s1 = cu2.printAll();
                assertEquals(s, s1, "Parser is not whitespace print idempotent");
            } catch (Exception e) {
                fail(e);
            }
        };
    }
}
