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
package org.openrewrite.hcl;

import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public final class Assertions {
    private Assertions() {}

    public static SourceSpecs hcl(@Nullable String before) {
        return hcl(before, s -> {
        });
    }

    public static SourceSpecs hcl(@Nullable String before, Consumer<SourceSpec<Hcl.ConfigFile>> spec) {
        SourceSpec<Hcl.ConfigFile> hcl = new SourceSpec<>(Hcl.ConfigFile.class, null, HclParser.builder(), before, null);
        spec.accept(hcl);
        return hcl;
    }

    public static SourceSpecs hcl(@Nullable String before, @Nullable String after) {
        return hcl(before, after, s -> {
        });
    }

    public static SourceSpecs hcl(@Nullable String before, @Nullable String after, Consumer<SourceSpec<Hcl.ConfigFile>> spec) {
        SourceSpec<Hcl.ConfigFile> hcl = new SourceSpec<>(Hcl.ConfigFile.class, null, HclParser.builder(),  before, s -> after);
        spec.accept(hcl);
        return hcl;
    }
}
