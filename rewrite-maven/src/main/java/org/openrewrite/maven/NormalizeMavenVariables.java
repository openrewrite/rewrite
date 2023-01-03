package org.openrewrite.maven;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class NormalizeMavenVariables extends Recipe {

    @Override
    public String getDisplayName() {
        return "Normalize Maven variables";
    }

    @Override
    public String getDescription() {
        return "Variables are all referenced by the prefix `project.`. You may also see references with `pom.` as the " +
               "prefix, or the prefix omitted entirely - these forms are now deprecated and should not be used.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            private final List<String> properties = Arrays.asList(
                    "basedir",
                    "groupId",
                    "artifactId",
                    "version",
                    "build.timestamp"
            );

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext executionContext) {
                Xml.Tag t = super.visitTag(tag, executionContext);
                Optional<String> value = t.getValue();
                if (value.isPresent()) {
                    String newValue = value
                            .filter(v -> properties.stream().anyMatch(prop -> v.equals("${" + prop + "}")))
                            .map(v -> "${project." + v.substring(2))
                            .orElse(value.get());
                    return value.get().equals(newValue) ? t : t.withValue(newValue);
                }
                return t;
            }
        };
    }
}
