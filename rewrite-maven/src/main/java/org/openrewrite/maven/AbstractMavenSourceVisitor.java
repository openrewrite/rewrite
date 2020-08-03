package org.openrewrite.maven;

import org.openrewrite.AbstractSourceVisitor;
import org.openrewrite.maven.tree.Maven;

public abstract class AbstractMavenSourceVisitor<R> extends AbstractSourceVisitor<R>
        implements MavenSourceVisitor<R> {

    public R visitPom(Maven.Pom pom) {
        return reduce(
                defaultTo(pom),
                reduce(
                        reduce(
                                visit(pom.getDependencyManagement()),
                                visit(pom.getDependencies())
                        ),
                        visit(pom.getProperties())
                )
        );
    }

    public R visitParent(Maven.Parent parent) {
        return defaultTo(parent);
    }

    public R visitDependency(Maven.Dependency dependency) {
        return defaultTo(dependency);
    }

    public R visitDependencyManagement(Maven.DependencyManagement dependencyManagement) {
        return reduce(
                defaultTo(dependencyManagement),
                visit(dependencyManagement.getDependencies())
        );
    }

    public R visitProperty(Maven.Property property) {
        return defaultTo(property);
    }
}
