package org.openrewrite.xml.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class HasNotNamespaceUri extends Recipe {

    @Option(displayName = "Namespace URI",
            description = "The Namespace URI to check.",
            example = "http://www.w3.org/2001/XMLSchema-instance")
    String namespaceUri;

    @Override
    public String getDisplayName() {
        return "Find tags without Namespace URI";
    }

    @Override
    public String getDescription() {
        return "Find XML that do not have a specific Namespace URI, optionally restricting the search by an XPath expression.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext executionContext) {
                boolean notInUse = HasNamespaceUri.find(document, namespaceUri, null).isEmpty();

                if (notInUse) {
                    return document.withRoot(SearchResult.found(document.getRoot()));
                }
                return document;
            }
        };
    }
}
