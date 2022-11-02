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
package org.openrewrite.gradle.search;

import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Paths;

public class FindGradleProject extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find Gradle projects";
    }

    @Override
    public String getDescription() {
        return "Gradle projects are those with `build.gradle` or `build.gradle.kts` files.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visitSourceFile(SourceFile sourceFile, ExecutionContext executionContext) {
                if (sourceFile.getSourcePath().endsWith(Paths.get("build.gradle")) ||
                    sourceFile.getSourcePath().endsWith(Paths.get("build.gradle.kts"))) {
                    return SearchResult.found(sourceFile);
                }
                return sourceFile;
            }
        };
    }
}
