package org.openrewrite.java.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.SearchResult;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Find escaping points of local variables.
 * An escaping point is the statement where a reference to a local variable leafs it scope an is now accessible from outside the method.
 * In some situation such escaping is wanted, think of getters, but everytime its importent to rethink.
 * Is mutability, synchronization or information hiding a problem here?
 */
//todo ternary operator support needed
public class FindVariablesEscapeLocation extends JavaIsoVisitor<ExecutionContext> {

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
        method = super.visitMethodInvocation(method, executionContext);

        boolean noLeaks = findLocalArguments(method.getArguments()).isEmpty();

        return noLeaks ? method : SearchResult.found(method);
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext executionContext) {
        assignment = super.visitAssignment(assignment, executionContext);

        J.Identifier identifier = (J.Identifier) assignment.getVariable();
        if (isPrimitive(identifier)) {
            return assignment;
        }

        boolean targetIsOutsider = isNotLocalVar(identifier);
        boolean assignedByLocalVar = assignment.getAssignment() instanceof J.Identifier && isLocalVar(((J.Identifier) assignment.getAssignment()));
        boolean leaksVariable = targetIsOutsider && assignedByLocalVar;

        return leaksVariable ? SearchResult.found(assignment) : assignment;
    }

    @Override
    public J.Return visitReturn(J.Return _return, ExecutionContext executionContext) {
        _return = super.visitReturn(_return, executionContext);

        boolean returnsLocalVar = _return.getExpression() instanceof J.Identifier
                                  && !isPrimitive(((J.Identifier) _return.getExpression()))
                                  && isLocalVar(((J.Identifier) _return.getExpression()));

        return returnsLocalVar ? SearchResult.found(_return) : _return;
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
        newClass = super.visitNewClass(newClass, executionContext);

        boolean noLeaks = findLocalArguments(newClass.getArguments()).isEmpty();

        return noLeaks ? newClass : SearchResult.found(newClass);
    }

    /**
     * Finds statements that enable escaping of local variables from the given subtree
     */
    public static Set<J> find(J subtree) {
        return TreeVisitor.collect(new FindVariablesEscapeLocation(), subtree, new HashSet<>())
                .stream()
                .filter(Statement.class::isInstance)
                .map(Statement.class::cast)
                .collect(Collectors.toSet());
    }

    /**
     * Finds identifier of local variables that escape  from the given subtree
     */
    public static Set<J.Identifier> findLeakingVars(J subtree) {
        Function<? super Tree, Set<J.Identifier>> extractIdentifiers = t -> {
            Set<J.Identifier> identifiers = new HashSet<>();
            if (t instanceof J.Return) {
                Expression expr = ((J.Return) t).getExpression();
                if (expr instanceof J.Identifier) {
                    identifiers.add((J.Identifier) expr);
                }
            } else if (t instanceof J.Assignment) {
                Expression expr = ((J.Assignment) t).getAssignment();
                if (expr instanceof J.Identifier) {
                    identifiers.add((J.Identifier) expr);
                }
            } else if (t instanceof J.NewClass) {
                identifiers.addAll(findLocalArguments(((J.NewClass) t).getArguments()));
            } else if(t instanceof J.MethodInvocation) {
                identifiers.addAll(findLocalArguments(((J.MethodInvocation) t).getArguments()));
            }

            return identifiers;
        };

        return TreeVisitor.collect(new FindVariablesEscapeLocation(), subtree, new HashSet<>())
                .stream()
                .map(extractIdentifiers)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private static boolean isLocalVar(J.Identifier identifier) {
        JavaType.Variable fieldType = identifier.getFieldType();

        if (fieldType != null) {
            JavaType owner = fieldType.getOwner();

            boolean isOwnedByMethod = owner instanceof JavaType.Method;
            if (isOwnedByMethod) {
                boolean isMethodParameter = ((JavaType.Method) owner).getParameterNames().contains(identifier.getSimpleName());
                return !isMethodParameter;
            }
        }

        return false;
    }

    private static boolean isNotLocalVar(J.Identifier identifier) {
        return !isLocalVar(identifier);
    }

    private static boolean isPrimitive(J.Identifier identifier) {
        JavaType.Variable fieldType = identifier.getFieldType();
        return fieldType != null && fieldType.getType() instanceof JavaType.Primitive;
    }

    private static boolean isNotPrimitive(J.Identifier identifier) {
        return !isPrimitive(identifier);
    }

    private static List<J.Identifier> findLocalArguments(List<Expression> arguments) {
        return arguments.stream()
                .filter(J.Identifier.class::isInstance)
                .map(J.Identifier.class::cast)
                .filter(FindVariablesEscapeLocation::isNotPrimitive)
                .filter(FindVariablesEscapeLocation::isLocalVar)
                .collect(Collectors.toList());
    }
}
