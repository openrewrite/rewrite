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
package org.openrewrite.gradle.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.table.JVMTestSuitesDefined;
import org.openrewrite.gradle.trait.JvmTestSuite;
import org.openrewrite.marker.SearchResult;

import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindJVMTestSuites extends Recipe {

    transient JVMTestSuitesDefined jvmTestSuitesDefined = new JVMTestSuitesDefined(this);

    @Option(displayName = "Insert rows",
            description = "Whether to insert rows into the table. Defaults to true.")
    @Nullable
    Boolean insertRows;

    @Override
    public String getDisplayName() {
        return "Find Gradle JVMTestSuite plugin configuration";
    }

    @Override
    public String getDescription() {
        return "Find Gradle JVMTestSuite plugin configurations and produce a data table.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean tableAvailable = this.insertRows == null || this.insertRows;
        return Preconditions.check(new IsBuildGradle<>(), new JvmTestSuite.Matcher().asVisitor((suite, ctx) -> {
            if (tableAvailable) {
                jvmTestSuitesDefined.insertRow(ctx, new JVMTestSuitesDefined.Row(suite.getName()));
            }
            return SearchResult.found(suite.getTree());
        }));
    }

    public static Set<JvmTestSuite> jvmTestSuites(SourceFile sourceFile) {
        if (!IsBuildGradle.matches(sourceFile.getSourcePath())) {
            return emptySet();
        }

        return new JvmTestSuite.Matcher().lower(sourceFile)
                .collect(toSet());
    }
}
