/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.coldfusion;

import org.jspecify.annotations.Nullable;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import javax.annotation.CheckReturnValue;
import java.util.function.Consumer;

@CheckReturnValue
public class Assertions {

    private Assertions() {
    }

    private static final ColdFusionParser.Builder parser = ColdFusionParser.builder();

    /**
     * A template, parsed as {@code template.cfm}.
     */
    public static SourceSpecs cfm(@Nullable String before) {
        return cfm(before, s -> {
        });
    }

    public static SourceSpecs cfm(@Nullable String before, Consumer<SourceSpec<Cf.CompilationUnit>> spec) {
        SourceSpec<Cf.CompilationUnit> cfm = new SourceSpec<>(Cf.CompilationUnit.class, null, parser, before, null)
                .path("template.cfm");
        spec.accept(cfm);
        return cfm;
    }

    public static SourceSpecs cfm(@Nullable String before, @Nullable String after) {
        return cfm(before, after, s -> {
        });
    }

    public static SourceSpecs cfm(@Nullable String before, @Nullable String after, Consumer<SourceSpec<Cf.CompilationUnit>> spec) {
        SourceSpec<Cf.CompilationUnit> cfm = new SourceSpec<>(Cf.CompilationUnit.class, null, parser, before, s -> after)
                .path("template.cfm");
        spec.accept(cfm);
        return cfm;
    }

    /**
     * A component, in tag or script syntax, parsed as {@code Component.cfc}.
     */
    public static SourceSpecs cfc(@Nullable String before) {
        return cfc(before, s -> {
        });
    }

    public static SourceSpecs cfc(@Nullable String before, Consumer<SourceSpec<Cf.CompilationUnit>> spec) {
        SourceSpec<Cf.CompilationUnit> cfc = new SourceSpec<>(Cf.CompilationUnit.class, null, parser, before, null)
                .path("Component.cfc");
        spec.accept(cfc);
        return cfc;
    }

    public static SourceSpecs cfc(@Nullable String before, @Nullable String after) {
        return cfc(before, after, s -> {
        });
    }

    public static SourceSpecs cfc(@Nullable String before, @Nullable String after, Consumer<SourceSpec<Cf.CompilationUnit>> spec) {
        SourceSpec<Cf.CompilationUnit> cfc = new SourceSpec<>(Cf.CompilationUnit.class, null, parser, before, s -> after)
                .path("Component.cfc");
        spec.accept(cfc);
        return cfc;
    }
}
