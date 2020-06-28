package org.openrewrite.maven;

import org.openrewrite.RefactorVisitorSupport;
import org.openrewrite.Tree;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.XmlRefactorVisitor;
import org.openrewrite.xml.tree.Xml;

public class MavenRefactorVisitor extends MavenSourceVisitor<Maven> implements RefactorVisitorSupport {
    XmlRefactorVisitor xmlRefactorVisitor = new XmlRefactorVisitor() {
    };

    @Override
    public Maven defaultTo(Tree t) {
        return (Maven) t;
    }

    @Override
    public Maven visitPom(Maven.Pom pom) {
        Maven.Pom p = refactor(pom, super::visitPom);
        p = p.withDependencies(refactor(pom.getDependencies()));
        p = p.withProperties(refactor(pom.getProperties()));
        return p;
    }

    @Override
    public Maven visitDependency(Maven.Dependency dependency) {
        Maven.Dependency d = refactor(dependency, super::visitDependency);
        Xml.Tag t = (Xml.Tag) xmlRefactorVisitor.visitTag(d.getTag());
        if(t != d.getTag()) {
            return new Maven.Dependency(d.getModel(), t);
        }
        return d;
    }

    @Override
    public Maven visitProperty(Maven.Property property) {
        Maven.Property p = refactor(property, super::visitProperty);
        Xml.Tag t = (Xml.Tag) xmlRefactorVisitor.visitTag(p.getTag());
        if(t != p.getTag()) {
            return new Maven.Property(t);
        }
        return p;
    }
}
