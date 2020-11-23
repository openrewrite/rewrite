package org.openrewrite.maven;

import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.xml.XmlRefactorVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;

public class MavenRefactorVisitor extends XmlRefactorVisitor
        implements MavenSourceVisitor<Xml> {
    protected Pom model;
    protected Collection<Pom> modules;

    @Override
    public Maven visitMaven(Maven maven) {
        this.model = maven.getModel();
        this.modules = maven.getModules();
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
