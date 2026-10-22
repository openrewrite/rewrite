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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.function.Predicate;

import static java.util.Arrays.asList;

public class RemoveReproducibleArchiveDefaults extends Recipe {

    @Getter
    final String displayName = "Remove archive settings that match the Gradle 9 reproducible defaults";

    @Getter
    final String description = "Gradle 9 makes archive tasks reproducible by default: `preserveFileTimestamps` " +
            "defaults to `false` and `reproducibleFileOrder` to `true`. This recipe removes configuration that sets " +
            "either property to its new default, and drops a configuration block that is left empty as a result. " +
            "Settings that opt out of reproducibility are kept. " +
            "See [build output should be byte-for-byte reproducible](https://docs.gradle.org/current/userguide/best_practices_security.html#builds_should_be_reproducible).";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Tree t = tree;
                for (RemovePropertyConfigurationVisitor removal : asList(
                        removal("preserveFileTimestamps", false),
                        removal("isPreserveFileTimestamps", false),
                        removal("reproducibleFileOrder", true),
                        removal("isReproducibleFileOrder", true))) {
                    t = removal.visit(t, ctx);
                }
                return t;
            }
        });
    }

    private static RemovePropertyConfigurationVisitor removal(String propertyName, boolean defaultValue) {
        return new RemovePropertyConfigurationVisitor(propertyName, owner -> true, isLiteral(defaultValue));
    }

    private static Predicate<Expression> isLiteral(boolean value) {
        return expression -> expression instanceof J.Literal && Boolean.valueOf(value).equals(((J.Literal) expression).getValue());
    }
}
