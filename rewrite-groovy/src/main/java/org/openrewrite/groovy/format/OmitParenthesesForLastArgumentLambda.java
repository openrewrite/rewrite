package org.openrewrite.groovy.format;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.J;

import static org.openrewrite.Tree.randomId;

public class OmitParenthesesForLastArgumentLambda extends Recipe {

    @Override
    public String getDisplayName() {
        return "Move a closure which is the last argument of a method invocation out of parentheses";
    }

    @Override
    public String getDescription() {
        return "Groovy allows a shorthand syntax that allows a closure to be placed outside of parentheses.";
    }

    @Override
    public GroovyVisitor<ExecutionContext> getVisitor() {
        return new GroovyIsoVisitor<ExecutionContext>() {

        };
    }
}
