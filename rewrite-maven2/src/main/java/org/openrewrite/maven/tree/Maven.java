package org.openrewrite.maven.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import org.openrewrite.xml.tree.Xml;

@Getter
public class Maven extends Xml.Document {
    private final Xml.Document document;
    private final Pom model;

    @JsonCreator
    public Maven(Xml.Document document, Pom model) {
        super(document.getId(), document.getSourcePath(), document.getMetadata(), document.getProlog(),
                document.getRoot(), document.getFormatting());
        this.document = document;
        this.model = model;
    }
}
