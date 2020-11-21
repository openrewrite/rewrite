package org.openrewrite.maven;

import org.openrewrite.AbstractSourceVisitor;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.AbstractXmlSourceVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

public abstract class AbstractMavenSourceVisitor<R> extends AbstractSourceVisitor<R>
        implements MavenSourceVisitor<R> {

    @Override
    public R visitMaven(Maven maven) {
        Xml.Document doc = maven.getDocument();
        return reduce(
                defaultTo(maven),
                visitMavenXml(doc)
        );
    }

    @Override
    public R visitMavenXml(Xml.Document document) {
        return defaultTo(document);
    }

    public static abstract class AbstractMavenXmlSourceVisitor<S> extends AbstractXmlSourceVisitor<S>
            implements MavenXmlVisitor<S> {
        private final XPathMatcher dependencyMatcher = new XPathMatcher("//dependencies/dependency");

        @Override
        public S visitTag(Xml.Tag tag) {
            if (dependencyMatcher.matches(getCursor())) {
                return reduce(super.visitTag(tag), visitDependency(tag));
            }
            return super.visitTag(tag);
        }

        @Override
        public S visitDependency(Xml.Tag dependency) {
            return defaultTo(dependency);
        }

        @Override
        public S visitProperty(Xml.Tag property) {
            return defaultTo(property);
        }
    }
}
