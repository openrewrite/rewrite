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
package org.openrewrite.scala;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

import static org.openrewrite.java.Assertions.sourceSet;
import static org.openrewrite.test.SourceSpecs.dir;

public class Assertions {

    private Assertions() {
    }

    private static ScalaParser.Builder scalaParser = ScalaParser.builder()
            .classpath(JavaParser.runtimeClasspath())
            .logCompilationWarningsAndErrors(true);

    public static SourceSpecs scala(@Language("scala") @Nullable String before) {
        return scala(before, s -> {
        });
    }

    public static SourceSpecs scala(@Language("scala") @Nullable String before, Consumer<SourceSpec<S.CompilationUnit>> spec) {
        SourceSpec<S.CompilationUnit> scala = new SourceSpec<>(S.CompilationUnit.class, null, scalaParser, before, null);
        spec.accept(scala);
        return scala;
    }

    public static SourceSpecs scala(@Language("scala") @Nullable String before, @Language("scala") @Nullable String after) {
        return scala(before, after, s -> {
        });
    }

    public static SourceSpecs scala(@Language("scala") @Nullable String before, @Language("scala") @Nullable String after,
                                    Consumer<SourceSpec<S.CompilationUnit>> spec) {
        SourceSpec<S.CompilationUnit> scala = new SourceSpec<>(S.CompilationUnit.class, null, scalaParser, before, s -> after);
        spec.accept(scala);
        return scala;
    }

    public static SourceSpecs srcMainScala(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... scalaSources) {
        return dir("src/main/scala", spec, scalaSources);
    }

    public static SourceSpecs srcMainScala(SourceSpecs... scalaSources) {
        return srcMainScala(spec -> sourceSet(spec, "main"), scalaSources);
    }

    public static SourceSpecs srcTestScala(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... scalaSources) {
        return dir("src/test/scala", spec, scalaSources);
    }

    public static SourceSpecs srcTestScala(SourceSpecs... scalaSources) {
        return srcTestScala(spec -> sourceSet(spec, "test"), scalaSources);
    }
}