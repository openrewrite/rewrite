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
package org.openrewrite.docker;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Assertions {
    private Assertions() {
    }

    public static SourceSpecs docker(@Language("dockerfile") @Nullable String before) {
        return docker(before, s -> {
        });
    }

    public static SourceSpecs docker(@Language("dockerfile") @Nullable String before,
                                     Consumer<SourceSpec<Docker.File>> spec) {
        SourceSpec<Docker.File> dockerfile = new SourceSpec<>(
                Docker.File.class,
                null,
                DockerParser.builder(),
                before,
                Assertions::validate,
                ctx -> {
                }
        );
        spec.accept(dockerfile);
        return dockerfile;
    }

    public static SourceSpecs docker(@Language("dockerfile") @Nullable String before,
                                     @Language("dockerfile") @Nullable String after) {
        return docker(before, after, s -> {
        });
    }

    public static SourceSpecs docker(@Language("dockerfile") @Nullable String before,
                                     @Language("dockerfile") @Nullable String after,
                                     Consumer<SourceSpec<Docker.File>> spec) {
        SourceSpec<Docker.File> dockerfile = new SourceSpec<>(
                Docker.File.class,
                null,
                DockerParser.builder(),
                before,
                Assertions::validate,
                ctx -> {
                }
        ).after(s -> after);
        spec.accept(dockerfile);
        return dockerfile;
    }

    private static SourceFile validate(SourceFile sf, TypeValidation tv) {
        if (!tv.allowNonWhitespaceInWhitespace()) {
            List<Docker> elementsWithNonBlankWhitespace = new DockerIsoVisitor<List<Docker>>() {
                @Override
                public Space visitSpace(Space space, List<Docker> elements) {
                    if (!space.getWhitespace().trim().isEmpty()) {
                        elements.add(getCursor().firstEnclosingOrThrow(Docker.class));
                    }
                    return super.visitSpace(space, elements);
                }
            }.reduce(sf, new ArrayList<>());
            if (!elementsWithNonBlankWhitespace.isEmpty()) {
                throw new AssertionError("Expected no non-whitespace in whitespace, but found: " + elementsWithNonBlankWhitespace);
            }
        }
        return sf;
    }
}
