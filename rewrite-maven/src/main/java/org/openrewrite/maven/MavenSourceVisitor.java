package org.openrewrite.maven;

import org.openrewrite.SourceVisitor;
import org.openrewrite.maven.tree.Maven;

public abstract class MavenSourceVisitor<R> extends SourceVisitor<R> {
    public R visitPom(Maven.Pom pom) {
        return defaultTo(pom);
    }

    public R visitParent(Maven.Parent parent) {
        return defaultTo(parent);
    }

    public R visitDependency(Maven.Dependency dependency) {
        return defaultTo(dependency);
    }

    public R visitProperty(Maven.Property property) {
        return defaultTo(property);
    }
}
