package org.openrewrite.maven;

import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.AbstractXmlSourceVisitor;

public abstract class AbstractMavenSourceVisitor<R> extends AbstractXmlSourceVisitor<R>
    implements MavenSourceVisitor<R> {

    @Override
    public R visitMaven(Maven maven) {
        return visitDocument(maven);
    }
}
