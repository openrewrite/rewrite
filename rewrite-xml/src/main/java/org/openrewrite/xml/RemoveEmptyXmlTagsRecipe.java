package org.openrewrite.xml;

import org.openrewrite.Recipe;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XpathMatcher;

public class RemoveEmptyXmlTagsRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove Empty XML Tags";
    }

    @Override
    public String getDescription() {
        return "This recipe removes empty XML tags when there are no child elements.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XpathMatcher("/pluginRepositories[not(node())]").delete()
                .configureMessage("Remove empty XML tags without child elements");
    }
}