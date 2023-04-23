package org.openrewrite.java.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Value
@EqualsAndHashCode(callSuper = true)
public class IsBlockLambdaConvertibleVisitor extends JavaIsoVisitor<AtomicBoolean> {

    Map<Pair<String, JavaType>, Boolean> localVariables = new HashMap<>();
    Pair<String, JavaType> excludedVariable;

    public static AtomicBoolean isBlockLambdaConvertible(J.Block block, Cursor cursor, J.Identifier _excludedVariable) {
        IsBlockLambdaConvertibleVisitor isBlockLambdaConvertibleVisitor = new IsBlockLambdaConvertibleVisitor(new ImmutablePair<String, JavaType>(_excludedVariable.getSimpleName(), _excludedVariable.getFieldType()));
        isBlockLambdaConvertibleVisitor.populateLocalVariables(cursor);
        return isBlockLambdaConvertibleVisitor.reduce(block, new AtomicBoolean(true));
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, AtomicBoolean isConvertible) {
        if (!isConvertible.get()) {
            return assignment;
        }
        Pair<String, JavaType> variable = new ImmutablePair<>(((J.Identifier) assignment.getVariable()).getSimpleName(), ((J.Identifier) assignment.getVariable()).getFieldType());
        if (localVariables.containsKey(variable)) {
            isConvertible.set(false);
            return assignment;
        }
        return super.visitAssignment(assignment, isConvertible);
    }

    @Override
    public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean isConvertible) {
        if (!isConvertible.get()) {
            return identifier;
        }
        if (Objects.nonNull(identifier.getFieldType())) {
            Pair<String, JavaType> variable = new ImmutablePair<>(identifier.getSimpleName(), identifier.getFieldType());
            if (localVariables.containsKey(variable) && !localVariables.get(variable)) {
                isConvertible.set(false);
                return identifier;
            }
        }
        return super.visitIdentifier(identifier, isConvertible);
    }

    @Override
    public J.Return visitReturn(J.Return _return, AtomicBoolean isConvertible) {
        if (!isConvertible.get()) {
            return _return;
        }
        isConvertible.set(false);
        return _return;
    }

    private void populateLocalVariables(Cursor cursor) {
        J.MethodDeclaration methodDeclaration = cursor.firstEnclosing(J.MethodDeclaration.class);
        if (methodDeclaration == null) {
            throw new IllegalStateException("A method declaration is required in the cursor path.");
        }

        new JavaIsoVisitor<Map<Pair<String, JavaType>, Boolean>>() {

            final Map<Pair<String, JavaType>, Boolean> isInitializedOrAssigned = new HashMap<>();

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations variableDeclarations, Map<Pair<String, JavaType>, Boolean> localVariables) {
                /* filtering out variables declared inside given block */
                if (cursor.isScopeInPath(variableDeclarations)) {
                    return super.visitVariableDeclarations(variableDeclarations, localVariables);
                }
                variableDeclarations.getVariables().forEach(namedVariable -> {
                    Pair<String, JavaType> variable = new ImmutablePair<>(namedVariable.getName().getSimpleName(), namedVariable.getVariableType());
                    localVariables.put(variable, true);
                    if (Objects.isNull(namedVariable.getInitializer())) {
                        isInitializedOrAssigned.put(variable, false);
                    } else {
                        isInitializedOrAssigned.put(variable, true);
                    }

                });
                return super.visitVariableDeclarations(variableDeclarations, localVariables);
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, Map<Pair<String, JavaType>, Boolean> localVariables) {
                // check if the local variable is assigned after being initialized or been assigned multiple times then variable is not effectively final
                Pair<String, JavaType> variable = new ImmutablePair<>(((J.Identifier) assignment.getVariable()).getSimpleName(), ((J.Identifier) assignment.getVariable()).getFieldType());
                if (Objects.equals(variable, excludedVariable)) {
                    return assignment;
                }
                if (localVariables.containsKey(variable)) {
                    if (isInitializedOrAssigned.get(variable)) {
                        localVariables.replace(variable, false);
                    } else {
                        isInitializedOrAssigned.replace(variable, true);
                    }
                }
                return assignment;
            }

        }.visit(methodDeclaration, localVariables);
    }
}
