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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class HasBuildToolVersion extends Recipe {

    @Option(displayName = "Build tool type",
            description = "The build tool to search for.",
            example = "Maven")
    BuildTool.Type type;

    @Option(displayName = "Build tool version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "3.6.0-9999")
    String version;

    @Override
    public String getDisplayName() {
        return "Find files with a particular build tool version";
    }

    @Override
    public String getDescription() {
        return "Finds Java source files built with a particular build tool. " +
               "This is useful especially as a precondition for other recipes.";
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (version != null) {
            validated = validated.and(Semver.validate(version, null));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator = requireNonNull(Semver.validate(version, null).getValue());
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree != null) {
                    return tree.getMarkers().findFirst(BuildTool.class)
                            .filter(buildTool -> buildTool.getType() == type)
                            .filter(buildTool -> versionComparator.isValid(null, buildTool.getVersion()))
                            .map(version -> SearchResult.found(tree))
                            .orElse(tree);
                }
                return tree;
            }
        };
    }
}
