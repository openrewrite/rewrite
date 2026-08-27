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
package org.openrewrite.gradle.gradle9;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;

import java.util.Locale;

public class RemoveExitEnvironmentVar extends Recipe {

    @Getter
    final String displayName = "Remove the deprecated `exitEnvironmentVar` start script property";

    @Getter
    final String description = "Gradle 9.5 reworked the Windows start script template so that " +
            "`CreateStartScripts.getExitEnvironmentVar()` and `setExitEnvironmentVar(String)` no longer affect the " +
            "generated scripts, and Gradle 9.6 deprecates them. This recipe removes `exitEnvironmentVar` " +
            "configuration from `CreateStartScripts` task configuration, and drops a configuration block that is " +
            "left empty as a result. Custom start script templates that still reference the variable need to be " +
            "updated by hand.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(),
                new RemoveDeprecatedPropertyVisitor("exitEnvironmentVar",
                        token -> token.toLowerCase(Locale.ROOT).endsWith("startscripts")));
    }
}
