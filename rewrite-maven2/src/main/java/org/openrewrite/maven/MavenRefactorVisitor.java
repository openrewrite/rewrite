package org.openrewrite.maven;

import org.openrewrite.RefactorVisitor;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.XmlRefactorVisitor;
import org.openrewrite.xml.tree.Xml;

class MavenRefactorVisitor extends AbstractMavenSourceVisitor<Xml>
        implements MavenSourceVisitor<Xml>, RefactorVisitor<Xml> {

    XmlRefactorVisitor xmlRefactorVisitor = new XmlRefactorVisitor();

    @Override
    public Xml visitPom(Maven maven) {
        return super.visitPom(maven);
    }

    @Override
    public Xml visitDependency(Xml.Tag tag) {
        return super.visitDependency(tag);
    }
}
