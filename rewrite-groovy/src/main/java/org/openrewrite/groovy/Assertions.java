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
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

import static org.openrewrite.java.Assertions.sourceSet;
import static org.openrewrite.test.SourceSpecs.dir;

public class Assertions {

    private Assertions() {
    }


    public static SourceSpecs groovy(@Language("groovy") @Nullable String before) {
        return groovy(before, s -> {
        });
    }

    public static SourceSpecs groovy(@Language("groovy") @Nullable String before, Consumer<SourceSpec<G.CompilationUnit>> spec) {
        SourceSpec<G.CompilationUnit> groovy = new SourceSpec<>(G.CompilationUnit.class, null, GroovyParser.builder(), before, null);
        spec.accept(groovy);
        return groovy;
    }

    public static SourceSpecs groovy(@Language("groovy") @Nullable String before, @Language("groovy") @Nullable String after) {
        return groovy(before, after, s -> {
        });
    }

    public static SourceSpecs groovy(@Language("groovy") @Nullable String before, @Language("groovy") @Nullable String after,
                                     Consumer<SourceSpec<G.CompilationUnit>> spec) {
        SourceSpec<G.CompilationUnit> groovy = new SourceSpec<>(G.CompilationUnit.class, null, GroovyParser.builder(), before, s -> after);
        spec.accept(groovy);
        return groovy;
    }

    public static SourceSpecs srcMainGroovy(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... javaSources) {
        return dir("src/main/groovy", spec, javaSources);
    }

    public static SourceSpecs srcMainGroovy(SourceSpecs... javaSources) {
        return srcMainGroovy(spec -> sourceSet(spec, "main"), javaSources);
    }

    public static SourceSpecs srcTestGroovy(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... javaSources) {
        return dir("src/test/groovy", spec, javaSources);
    }

    public static SourceSpecs srcTestGroovy(SourceSpecs... javaSources) {
        return srcTestGroovy(spec -> sourceSet(spec, "test"), javaSources);
    }
}
