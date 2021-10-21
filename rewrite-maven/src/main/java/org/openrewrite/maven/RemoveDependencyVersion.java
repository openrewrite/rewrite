package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.xml.tree.Xml.Tag;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveDependencyVersion extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "guava")
    String artifactId;

    @Option(displayName = "Scope",
            description = "Only remove dependencies if they are in this scope. If 'runtime', this will" +
                    "also remove dependencies in the 'compile' scope because 'compile' dependencies are part of the runtime dependency set",
            valid = {"compile", "test", "runtime", "provided"},
            example = "compile",
            required = false)
    @Nullable
    String scope;

    @Override
    public String getDisplayName() {
        return "Remove Maven dependency";
    }

    @Override
    public String getDescription() {
        return "Removes a single dependency from the <dependencies> section of the pom.xml.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveDependencyVersionVisitor();
    }

    @Override
    public Validated validate() {
        return super.validate().and(Validated.test("scope", "Scope must be one of compile, runtime, test, or provided",
                scope, s -> !Scope.Invalid.equals(Scope.fromName(s))));
    }

    private class RemoveDependencyVersionVisitor extends MavenVisitor {

        @Override
        public Xml visitTag(Tag tag, ExecutionContext ctx) {
            if (isDependencyTag(groupId, artifactId)) {
                Pom.Dependency dependency = findDependency(tag);
                Optional<Tag> versionTag = tag.getChild("version");
                if (dependency != null && versionTag.isPresent()) {
                    Scope checkScope = scope != null ? Scope.fromName(scope) : null;
                    if (checkScope == null || checkScope == dependency.getScope() ||
                            (dependency.getScope() != null && dependency.getScope().isInClasspathOf(checkScope))) {
                        doAfterVisit(new RemoveContentVisitor<>(versionTag.get(), true));
                    }
                }
            }

            return super.visitTag(tag, ctx);
        }

    }
}
