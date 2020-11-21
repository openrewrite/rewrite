package org.openrewrite.maven.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import org.openrewrite.Formatting;
import org.openrewrite.Metadata;
import org.openrewrite.SourceFile;
import org.openrewrite.SourceVisitor;
import org.openrewrite.Tree;
import org.openrewrite.maven.MavenSourceVisitor;
import org.openrewrite.xml.tree.Xml;

import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;

@Getter
public class Maven implements Serializable, Tree, SourceFile {
    private final Xml.Document document;
    private final Pom model;
    @JsonCreator
    public Maven(Xml.Document document, Pom model) {
        this.document = document;
        this.model = model;
    }
    public Xml.Document getDocument() {
        return document;
    }
    public Pom getModel() {
        return model;
    }
    @Override
    public final <R> R accept(SourceVisitor<R> v) {
        return v instanceof MavenSourceVisitor ?
                acceptMaven((MavenSourceVisitor<R>) v) : v.defaultTo(null);
    }
    public <R> R acceptMaven(MavenSourceVisitor<R> v) {
        return v.visitPom(this);
    }
    @Override
    public Formatting getFormatting() {
        return document.getFormatting();
    }
    @Override
    public UUID getId() {
        return document.getId();
    }
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Tree> T withFormatting(Formatting fmt) {
        return (T) document.withFormatting(fmt);
    }
    @Override
    public String print() {
        return document.print();
    }

    @Override
    public String getSourcePath() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Collection<Metadata> getMetadata() {
        throw new RuntimeException("Not implemented");
    }
}
