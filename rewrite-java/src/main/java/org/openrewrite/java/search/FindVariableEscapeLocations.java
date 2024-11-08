/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
public class FindVariableEscapeLocations extends JavaIsoVisitor<ExecutionContext> {

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
        method = super.visitMethodInvocation(method, executionContext);

        boolean noLeaks = findLocalArguments(method.getArguments()).isEmpty();

        return noLeaks ? method : SearchResult.found(method);
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext executionContext) {
        assignment = super.visitAssignment(assignment, executionContext);

        J.Identifier assigmentTarget = (J.Identifier) assignment.getVariable();
        if (isPrimitive(assigmentTarget)) {
            return assignment;
        }

        boolean targetIsOutsider = isNotLocalVar(assigmentTarget);
        boolean assignedByLocalVar = extractIdentifiers(assignment.getAssignment()).stream()
                .map(FindVariableEscapeLocations::isLocalVar)
                .reduce(false, Boolean::logicalOr);
        boolean leaksVariable = targetIsOutsider && assignedByLocalVar;

        return leaksVariable ? SearchResult.found(assignment) : assignment;
    }

    @Override
    public J.Return visitReturn(J.Return _return, ExecutionContext executionContext) {
        _return = super.visitReturn(_return, executionContext);

        if (_return.getExpression() == null) {
            return _return;
        }

        boolean returnsLocalVar = extractIdentifiers(_return.getExpression()).stream()
                .map(FindVariableEscapeLocations::extractIdentifiers)
                .flatMap(Collection::stream)
                .filter(i -> !isPrimitive(i))
                .map(FindVariableEscapeLocations::isLocalVar)
                .reduce(false, Boolean::logicalOr);

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
        return TreeVisitor.collect(new FindVariableEscapeLocations(), subtree, new HashSet<>())
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

        return TreeVisitor.collect(new FindVariableEscapeLocations(), subtree, new HashSet<>())
                .stream()
                .map(extractIdentifiers)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Extracts Set of Identifiers from J.Identifiers and Ternary operators.
     * These two potentially participate in very case but will not lead to escaping themselves.
     */
    private static Set<J.Identifier> extractIdentifiers(Expression assignment) {
        Set<J.Identifier> identifiers = new HashSet<>();
        // fast return if already an J.Identifier
        if (assignment instanceof J.Identifier) {
            identifiers.add((J.Identifier) assignment);
        }
        if (assignment instanceof J.Ternary) {
            // if ternary we only need to care about direct assigment.
            // the other possibilities (J.MethodInvocation, J.NewClass) are handled by the visitor itself.
            if (((J.Ternary) assignment).getFalsePart() instanceof J.Identifier) {
                identifiers.add((J.Identifier) ((J.Ternary) assignment).getFalsePart());
            }
            if (((J.Ternary) assignment).getTruePart() instanceof J.Identifier) {
                identifiers.add((J.Identifier) ((J.Ternary) assignment).getTruePart());
            }
        }

        return identifiers;
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
                .map(FindVariableEscapeLocations::extractIdentifiers)
                .flatMap(Collection::stream)
                .filter(FindVariableEscapeLocations::isNotPrimitive)
                .filter(FindVariableEscapeLocations::isLocalVar)
                .collect(Collectors.toList());
    }
}
