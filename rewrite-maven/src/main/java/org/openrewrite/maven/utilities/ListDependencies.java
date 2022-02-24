/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.maven.utilities;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.text.PlainText;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class ListDependencies extends Recipe {
    @Option(displayName = "Scope",
            description = "A scope to use when it is not what can be inferred from usage. Most of the time this will be left empty, but " +
                    "is used when adding a runtime, provided, or compile dependency.",
            example = "runtime",
            valid = {"compile", "runtime", "provided", "test"},
            required = false)
    @Nullable
    String scope;

    @Override
    public String getDisplayName() {
        return "List all dependencies";
    }

    @Override
    public String getDescription() {
        return "List all the dependencies in a scope and add to a text file.";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        Set<String> dependencies = new HashSet<>();

        for (SourceFile sourceFile : before) {
            new MavenVisitor<ExecutionContext>() {
                @Override
                public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                    for (ResolvedDependency resolvedDependency : getResolutionResult()
                            .getDependencies()
                            .get(Scope.fromName(scope))) {
                        dependencies.add(resolvedDependency.getGav().toString());
                    }

                    return super.visitDocument(document, ctx);
                }
            }.visit(sourceFile, ctx);
        }

        return ListUtils.concat(before, new PlainText(randomId(), Paths.get("dependencies.txt"), Markers.EMPTY,
                dependencies.stream().sorted().collect(Collectors.joining("\n"))));
    }
}
