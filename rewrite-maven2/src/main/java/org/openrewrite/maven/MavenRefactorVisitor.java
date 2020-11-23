package org.openrewrite.maven;

import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.xml.XmlRefactorVisitor;
import org.openrewrite.xml.tree.Xml;

public class MavenRefactorVisitor extends XmlRefactorVisitor
        implements MavenSourceVisitor<Xml> {
    protected Pom model;

    @Override
    public Maven visitMaven(Maven maven) {
        this.model = maven.getModel();
        return (Maven) visitDocument(maven);
    }

    @Override
    public final Xml visitDocument(Xml.Document document) {
        Xml.Document refactored = refactor(document, super::visitDocument);
        if (refactored != document) {
            return new Maven(refactored);
        }
        return refactored;
    }
}
