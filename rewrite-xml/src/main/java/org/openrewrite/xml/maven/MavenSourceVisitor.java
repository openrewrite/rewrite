package org.openrewrite.xml.maven;

import org.openrewrite.xml.XmlSourceVisitor;

public abstract class MavenSourceVisitor<R> extends XmlSourceVisitor<R> {
    public R visitPom(Maven.Pom pom) {
        return reduce(
                defaultTo(pom),
                visit(pom.getDocument())
        );
    }
}
