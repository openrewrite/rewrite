package org.openrewrite.maven;

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenSourceVisitor;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.AbstractXmlSourceVisitor;
import org.openrewrite.xml.tree.Xml;


abstract class AbstractMavenSourceVisitor<R> extends AbstractXmlSourceVisitor<R>
        implements MavenSourceVisitor<R> {
    @Override
    public R visitPom(Maven maven) {
        Xml.Document doc = maven.getDocument();
        return reduce(
                visitDocument(doc),
                defaultTo(maven)
        );
    }
    @Override
    public R visitTag(Xml.Tag tag) {

        // xpath thing here...
        return super.visitTag(tag);
    }
    @Override
    public R visitDependency(Xml.Tag tag) {
        return defaultTo(tag);
    }
}
