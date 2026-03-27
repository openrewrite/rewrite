/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.csharp.marker.MSBuildProject;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.internal.StringUtils.matchesGlob;

/**
 * Precondition that matches .csproj files which reference a specific NuGet package.
 * Use with {@link Preconditions#check} to scope recipes to projects that have a
 * particular package dependency.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class FindNuGetPackageReference extends Recipe {

    @Option(displayName = "Package name",
            description = "The NuGet package name to search for. Supports glob patterns (e.g., `Swashbuckle.*`).",
            example = "Swashbuckle.AspNetCore")
    String packageName;

    @Override
    public String getDisplayName() {
        return "Find NuGet package reference";
    }

    @Override
    public String getDescription() {
        return "Searches for .csproj files that reference a specific NuGet package. " +
               "Intended for use as a precondition to scope other recipes.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new FindMSBuildProject(),
                new XmlIsoVisitor<ExecutionContext>() {
                    @Override
                    public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                        return document.getMarkers().findFirst(MSBuildProject.class)
                                .filter(marker -> {
                                    for (MSBuildProject.TargetFramework tfm : marker.getTargetFrameworks()) {
                                        for (MSBuildProject.PackageReference ref : tfm.getPackageReferences()) {
                                            if (matchesGlob(ref.getInclude(), packageName)) {
                                                return true;
                                            }
                                        }
                                    }
                                    return false;
                                })
                                .map(marker -> SearchResult.found(document))
                                .orElse(document);
                    }
                }
        );
    }
}
