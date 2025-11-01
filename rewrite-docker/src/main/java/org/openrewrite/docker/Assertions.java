/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.docker.tree.Dockerfile;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public class Assertions {
    private Assertions() {
    }

    public static SourceSpecs dockerfile(@Language("dockerfile") @Nullable String before) {
        return dockerfile(before, s -> {
        });
    }

    public static SourceSpecs dockerfile(@Language("dockerfile") @Nullable String before,
                                         Consumer<SourceSpec<Dockerfile.Document>> spec) {
        SourceSpec<Dockerfile.Document> dockerfile = new SourceSpec<>(
                Dockerfile.Document.class,
                null,
                DockerfileParser.builder(),
                before,
                null
        );
        spec.accept(dockerfile);
        return dockerfile;
    }

    public static SourceSpecs dockerfile(@Language("dockerfile") @Nullable String before,
                                         @Language("dockerfile") @Nullable String after) {
        return dockerfile(before, after, s -> {
        });
    }

    public static SourceSpecs dockerfile(@Language("dockerfile") @Nullable String before,
                                         @Language("dockerfile") @Nullable String after,
                                         Consumer<SourceSpec<Dockerfile.Document>> spec) {
        SourceSpec<Dockerfile.Document> dockerfile = new SourceSpec<>(
                Dockerfile.Document.class,
                null,
                DockerfileParser.builder(),
                before,
                s -> after
        );
        spec.accept(dockerfile);
        return dockerfile;
    }
}
