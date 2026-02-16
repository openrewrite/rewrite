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
package org.openrewrite.bash;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.bash.tree.Bash;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;

import java.util.function.Consumer;

public class Assertions {
    private Assertions() {
    }

    public static SourceSpecs bash(@Language("bash") @Nullable String before) {
        return bash(before, s -> {
        });
    }

    public static SourceSpecs bash(@Language("bash") @Nullable String before,
                                   Consumer<SourceSpec<Bash.Script>> spec) {
        SourceSpec<Bash.Script> bashScript = new SourceSpec<>(
                Bash.Script.class,
                null,
                BashParser.builder(),
                before,
                Assertions::validate,
                ctx -> {
                }
        );
        spec.accept(bashScript);
        return bashScript;
    }

    public static SourceSpecs bash(@Language("bash") @Nullable String before,
                                   @Language("bash") @Nullable String after) {
        return bash(before, after, s -> {
        });
    }

    public static SourceSpecs bash(@Language("bash") @Nullable String before,
                                   @Language("bash") @Nullable String after,
                                   Consumer<SourceSpec<Bash.Script>> spec) {
        SourceSpec<Bash.Script> bashScript = new SourceSpec<>(
                Bash.Script.class,
                null,
                BashParser.builder(),
                before,
                Assertions::validate,
                ctx -> {
                }
        );
        bashScript.after(s -> after);
        spec.accept(bashScript);
        return bashScript;
    }

    private static SourceFile validate(SourceFile sf, TypeValidation tv) {
        return sf;
    }
}
