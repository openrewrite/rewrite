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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.ChangePackage;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class LowercasePackage extends Recipe {
    @Override
    public String getDisplayName() {
        return "Rename packages to lowercase";
    }

    @Override
    public String getDescription() {
        return "By convention all Java package names should contain only lowercase letters, numbers, and dashes. " +
                "This recipe converts any uppercase letters in package names to be lowercase.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-120");
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Package visitPackage(J.Package pkg, ExecutionContext executionContext) {
                String packageText = pkg.getExpression().print(getCursor()).replaceAll("\\s", "");
                String lowerCase = packageText.toLowerCase();
                if(!packageText.equals(lowerCase)) {
                    doNext(new ChangePackage(packageText, lowerCase, true));
                }
                return pkg;
            }
        };
    }
}
