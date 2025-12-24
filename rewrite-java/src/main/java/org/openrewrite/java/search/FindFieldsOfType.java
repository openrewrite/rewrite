/*
 * Copyright 2020 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.table.FieldsOfTypeUses;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Finds fields that have a matching type.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class FindFieldsOfType extends Recipe {

    @Option(displayName = "Fully-qualified type name",
            description = "A fully-qualified Java type name, that is used to find matching fields.",
            example = "org.slf4j.api.Logger")
    String fullyQualifiedTypeName;

    @Option(displayName = "Match inherited",
            description = "When enabled, find types that inherit from a deprecated type.",
            required = false)
    @Nullable
    Boolean matchInherited;

    private final transient FieldsOfTypeUses fieldsOfTypeUses = new FieldsOfTypeUses(this);

    @Override
    public String getDisplayName() {
        return "Find fields of type";
    }

    @Override
    public String getInstanceNameSuffix() {
        return "on types `" + fullyQualifiedTypeName + "`";
    }

    @Override
    public String getDescription() {
        return "Finds declared fields matching a particular class name.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                if (multiVariable.getTypeExpression() instanceof J.MultiCatch) {
                    return multiVariable;
                }
                if (multiVariable.getTypeExpression() != null &&
                    hasElementType(multiVariable.getTypeExpression().getType(), fullyQualifiedTypeName,
                                Boolean.TRUE.equals(matchInherited)) &&
                    isField(getCursor())) {

                    // Populate the FieldsOfTypeUses DataTable
                    for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
                        String varType = variable.getType().toString();
                        if (variable.getInitializer() != null && variable.getInitializer().getType() != null) {
                            varType = variable.getInitializer().getType().toString();
                        }
                        fieldsOfTypeUses.insertRow(ctx, new FieldsOfTypeUses.Row(
                            getCursor().firstEnclosingOrThrow(J.CompilationUnit.class).getSourcePath().toString(),
                            variable.getSimpleName(),
                            multiVariable.getTypeExpression().getType().toString(),
                            varType,
                            multiVariable.getModifiers().stream().map(J.Modifier::toString).reduce((m1, m2) -> m1 + " " + m2).orElse(""),
                            multiVariable.printTrimmed(getCursor())
                        ));
                    }
                    return SearchResult.found(multiVariable);
                }
                return multiVariable;
            }
        };
    }

    public static Set<J.VariableDeclarations> find(J j, String fullyQualifiedTypeName) {
        JavaIsoVisitor<Set<J.VariableDeclarations>> findVisitor = new JavaIsoVisitor<Set<J.VariableDeclarations>>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Set<J.VariableDeclarations> vs) {
                if (multiVariable.getTypeExpression() instanceof J.MultiCatch) {
                    return multiVariable;
                }
                if (multiVariable.getTypeExpression() != null &&
                        hasElementType(multiVariable.getTypeExpression().getType(), fullyQualifiedTypeName, true)  &&
                        isField(getCursor())) {
                    vs.add(multiVariable);
                }
                return multiVariable;
            }
        };

        Set<J.VariableDeclarations> vs = new HashSet<>();
        findVisitor.visit(j, vs);
        return vs;
    }

    private static boolean isField(Cursor cursor) {
        Iterator<Object> path = cursor.getPath();
        while (path.hasNext()) {
            Object o = path.next();
            if (o instanceof J.MethodDeclaration) {
                return false;
            }
            if (o instanceof J.ClassDeclaration) {
                return true;
            }
        }
        return true;
    }

    private static boolean hasElementType(@Nullable JavaType type, String fullyQualifiedName,
                                          boolean matchOverrides) {
        if (type instanceof JavaType.Array) {
            return hasElementType(((JavaType.Array) type).getElemType(), fullyQualifiedName, matchOverrides);
        } else if (type instanceof JavaType.FullyQualified || type instanceof JavaType.Primitive) {
            return new TypeMatcher(fullyQualifiedName, matchOverrides).matches(type);
        } else if (type instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) type;
            for (JavaType bound : generic.getBounds()) {
                if (hasElementType(bound, fullyQualifiedName, matchOverrides)) {
                    return true;
                }
            }
        }
        return false;
    }
}
