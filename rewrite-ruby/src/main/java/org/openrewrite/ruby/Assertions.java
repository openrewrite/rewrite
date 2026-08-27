/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.ruby;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ruby.tree.Rb;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public class Assertions {

    private Assertions() {
    }

    private static final RubyParser.Builder rubyParser = RubyParser.builder();

    public static SourceSpecs ruby(@Language("rb") @Nullable String before) {
        return ruby(before, s -> {
        });
    }

    public static SourceSpecs ruby(@Language("rb") @Nullable String before, Consumer<SourceSpec<Rb.CompilationUnit>> spec) {
        SourceSpec<Rb.CompilationUnit> ruby = new SourceSpec<>(Rb.CompilationUnit.class, null, rubyParser, before, null);
        spec.accept(ruby);
        return ruby;
    }

    public static SourceSpecs ruby(@Language("rb") @Nullable String before, @Language("rb") @Nullable String after) {
        return ruby(before, after, s -> {
        });
    }

    public static SourceSpecs ruby(@Language("rb") @Nullable String before, @Language("rb") @Nullable String after,
                                   Consumer<SourceSpec<Rb.CompilationUnit>> spec) {
        SourceSpec<Rb.CompilationUnit> ruby = new SourceSpec<>(Rb.CompilationUnit.class, null, rubyParser, before, s -> after);
        spec.accept(ruby);
        return ruby;
    }

    public static SourceSpecs gemfile(@Language("rb") @Nullable String before) {
        return gemfile(before, s -> {
        });
    }

    public static SourceSpecs gemfile(@Language("rb") @Nullable String before, Consumer<SourceSpec<Rb.CompilationUnit>> spec) {
        SourceSpec<Rb.CompilationUnit> gemfile = new SourceSpec<>(Rb.CompilationUnit.class, null, rubyParser, before, null)
                .path("Gemfile");
        spec.accept(gemfile);
        return gemfile;
    }

    public static SourceSpecs rakefile(@Language("rb") @Nullable String before) {
        return rakefile(before, s -> {
        });
    }

    public static SourceSpecs rakefile(@Language("rb") @Nullable String before, Consumer<SourceSpec<Rb.CompilationUnit>> spec) {
        SourceSpec<Rb.CompilationUnit> rakefile = new SourceSpec<>(Rb.CompilationUnit.class, null, rubyParser, before, null)
                .path("Rakefile");
        spec.accept(rakefile);
        return rakefile;
    }
}
