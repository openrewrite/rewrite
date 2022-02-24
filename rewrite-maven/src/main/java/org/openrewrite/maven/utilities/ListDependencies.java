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
