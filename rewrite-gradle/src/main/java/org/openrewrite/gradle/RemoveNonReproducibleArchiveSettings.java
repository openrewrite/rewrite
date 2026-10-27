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
package org.openrewrite.gradle;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;
import java.util.Set;

import static java.util.Collections.singleton;

public class RemoveNonReproducibleArchiveSettings extends Recipe {

    @Getter
    final String displayName = "Remove archive settings that defeat reproducible builds";

    @Getter
    final String description = "Remove `preserveFileTimestamps = true` and `reproducibleFileOrder = false` from " +
            "archive tasks, in either DSL spelling, so the archive takes Gradle's own default instead. Timestamps " +
            "written into an archive, and file order that follows the filesystem, both make the same sources produce " +
            "a different artifact on each build, which means a published artifact cannot be checked against a " +
            "rebuild of its sources. The reproducible values written explicitly are left alone, since someone who " +
            "wrote them agrees with the default and said so deliberately. See the " +
            "[Gradle best practice](https://docs.gradle.org/current/userguide/best_practices_security.html#builds_should_be_reproducible).";

    @Getter
    final Set<String> tags = singleton("security");

    @Getter
    final Duration estimatedEffortPerOccurrence = Duration.ofMinutes(2);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                return b.withStatements(ListUtils.filter(b.getStatements(),
                        statement -> !defeatsReproducibility(statement)));
            }
        });
    }

    private static boolean defeatsReproducibility(Statement statement) {
        if (!(statement instanceof J.Assignment)) {
            return false;
        }
        J.Assignment assignment = (J.Assignment) statement;

        String name = variableName(assignment.getVariable());
        if (name == null) {
            return false;
        }

        Object value = assignment.getAssignment() instanceof J.Literal ?
                ((J.Literal) assignment.getAssignment()).getValue() :
                null;
        if (!(value instanceof Boolean)) {
            return false;
        }

        // The Kotlin DSL spells a Boolean property's accessor with the `is` prefix
        if ("preserveFileTimestamps".equals(name) || "isPreserveFileTimestamps".equals(name)) {
            return Boolean.TRUE.equals(value);
        }
        if ("reproducibleFileOrder".equals(name) || "isReproducibleFileOrder".equals(name)) {
            return Boolean.FALSE.equals(value);
        }
        return false;
    }

    private static @Nullable String variableName(Expression variable) {
        if (variable instanceof J.Identifier) {
            return ((J.Identifier) variable).getSimpleName();
        }
        if (variable instanceof J.FieldAccess) {
            return ((J.FieldAccess) variable).getSimpleName();
        }
        return null;
    }
}
