package org.openrewrite.groovy.format;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;

public class GStringCurlyBraces extends Recipe {
    @Override
    public String getDisplayName() {
        return "Groovy GString curly braces";
    }

    @Override
    public String getDescription() {
        return "In Groovy [GStrings](https://docs.groovy-lang.org/latest/html/api/groovy/lang/GString.html), curly braces are optional for single variable expressions. " +
               "This recipe adds them, so that the expression is always surrounded by curly braces.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GroovyVisitor<ExecutionContext>() {
            @Override
            public J visitGStringValue(G.GString.Value value, ExecutionContext executionContext) {
                return value.withEnclosedInBraces(true);
            }
        };
    }
}
