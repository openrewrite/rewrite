package org.openrewrite.maven.tree;

import org.openrewrite.SourceVisitor;
import org.openrewrite.maven.MavenSourceVisitor;
import org.openrewrite.xml.XmlSourceVisitor;
import org.openrewrite.xml.tree.Xml;

public class Maven extends Xml.Document {
    private final transient Pom model;

    public Maven(Xml.Document document) {
        super(
                document.getId(),
                document.getSourcePath(),
                document.getMetadata(),
                document.getProlog(),
                document.getRoot(),
                document.getFormatting()
        );
        model = getMetadata(Pom.class);
        assert model != null;
    }

    public Pom getModel() {
        return model;
    }

    @Override
    public <R> R accept(SourceVisitor<R> v) {
        if (v instanceof MavenSourceVisitor) {
            return ((MavenSourceVisitor<R>) v).visitMaven(this);
        } else if (v instanceof XmlSourceVisitor) {
            return super.accept(v);
        }
        return v.defaultTo(null);
    }
}
