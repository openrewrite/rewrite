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
package org.openrewrite.maven;

import org.openrewrite.Recipe;

import java.util.Arrays;
import java.util.List;

public class ReplaceRemovedRootDirectoryProperties extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace removed root directory properties";
    }

    @Override
    public String getDescription() {
        return "Maven 4 removed support for deprecated root directory properties. " +
               "This recipe replaces `${executionRootDirectory}` with `${session.rootDirectory}` " +
               "and `${multiModuleProjectDirectory}` with `${project.rootDirectory}`.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                new RenamePropertyKey("executionRootDirectory", "session.rootDirectory"),
                new RenamePropertyKey("multiModuleProjectDirectory", "project.rootDirectory")
        );
    }
}
