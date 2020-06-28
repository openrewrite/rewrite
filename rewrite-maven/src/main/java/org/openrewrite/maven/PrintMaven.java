package org.openrewrite.maven;

import org.openrewrite.Tree;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.internal.PrintXml;

public class PrintMaven extends MavenSourceVisitor<String> {
    private final PrintXml printXml = new PrintXml();

    @Override
    public String defaultTo(Tree t) {
        return "";
    }

    @Override
    public String visitPom(Maven.Pom pom) {
        return printXml.visitDocument(pom.getDocument());
    }
}
