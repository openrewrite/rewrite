package org.openrewrite.maven;

import org.openrewrite.AbstractRefactorVisitor;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.XmlRefactorVisitor;
import org.openrewrite.xml.tree.Xml;

class MavenRefactorVisitor extends AbstractRefactorVisitor<Maven>
        implements MavenSourceVisitor<Maven> {

    XmlRefactorVisitor xmlRefactorVisitor = new XmlRefactorVisitor();

//    @Override
//    public Maven visitTree(Tree tree) {
//        assert tree instanceof Maven;
//        Maven m = ((Maven) tree).acceptMaven(this);
//        Xml.Document source = (Xml.Document) xmlRefactorVisitor
//                .visit(m.getDocument());
//        if (source != m.getDocument()) {
//            return new Maven(source, m.getModel());
//        }
//        return m;
//    }

    @Override
    public Maven visitMaven(Maven maven) {
        return maven;
    }

    @Override
    public Maven visitMavenXml(Xml.Document document) {
        return null;
    }

//    @Override
//    public Xml visitDependency(Xml.Tag dependency) {
//        return dependency;
//    }
//
//    @Override
//    public Xml visitProperty(Xml.Tag property) {
//        return property;
//    }
}
