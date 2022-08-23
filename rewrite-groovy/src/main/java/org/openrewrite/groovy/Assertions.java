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
package org.openrewrite.groovy;


import org.intellij.lang.annotations.Language;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.test.DslParserBuilder;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public class Assertions {

    private Assertions() {
    }

    private static final DslParserBuilder groovyParser = new DslParserBuilder("groovy", GroovyParser.builder());

    public static SourceSpecs groovy(@Language("groovy") @Nullable String before) {
        return groovy(before, s -> {
        });
    }

    public static SourceSpecs groovy(@Language("groovy") @Nullable String before, Consumer<SourceSpec<G.CompilationUnit>> spec) {
        SourceSpec<G.CompilationUnit> groovy = new SourceSpec<>(G.CompilationUnit.class, null, groovyParser, before, null);
        spec.accept(groovy);
        return groovy;
    }

    public static SourceSpecs groovy(@Language("groovy") @Nullable String before, @Language("groovy") String after) {
        return groovy(before, after, s -> {
        });
    }

    public static SourceSpecs groovy(@Language("groovy") @Nullable String before, @Language("groovy") String after,
                                     Consumer<SourceSpec<G.CompilationUnit>> spec) {
        SourceSpec<G.CompilationUnit> groovy = new SourceSpec<>(G.CompilationUnit.class, null, groovyParser, before, after);
        spec.accept(groovy);
        return groovy;
    }
}
