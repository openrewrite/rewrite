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
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.internal.StringUtils.matchesGlob;

/**
 * Removes a NuGet PackageReference from .csproj files.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveNuGetPackageReference extends Recipe {

    @Option(displayName = "Package name",
            description = "The NuGet package name to remove. Supports glob patterns.",
            example = "Newtonsoft.Json")
    String packageName;

    @Override
    public String getDisplayName() {
        return "Remove NuGet package reference";
    }

    @Override
    public String getDescription() {
        return "Removes a `<PackageReference>` element from .csproj files.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new FindMSBuildProject(),
                new XmlIsoVisitor<ExecutionContext>() {
                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        Xml.Tag t = super.visitTag(tag, ctx);
                        if ("PackageReference".equals(t.getName())) {
                            String include = t.getAttributes().stream()
                                    .filter(a -> "Include".equalsIgnoreCase(a.getKeyAsString()))
                                    .map(a -> a.getValue().getValue())
                                    .findFirst()
                                    .orElse(null);
                            if (include != null && matchesGlob(include, packageName)) {
                                doAfterVisit(new RemoveContentVisitor<>(t, true, false));
                            }
                        }
                        return t;
                    }
                }
        );
    }
}
