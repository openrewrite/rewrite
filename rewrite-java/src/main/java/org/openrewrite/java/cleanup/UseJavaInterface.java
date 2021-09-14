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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.*;

public class UseJavaInterface extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use `Deque`, `List`, `Map`, `ConcurrentMap`, `Queue`, `Set`, `SortedMap`, and `SortedSet`";
    }

    @Override
    public String getDescription() {
        return "Replaces declarations of implemented classes in Java collection with the equivalent interface.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1319");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(10);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        Map<String, String> rspecRulesReplaceTypeMap = new HashMap<>();
        rspecRulesReplaceTypeMap.put("java.util.ArrayDeque", "java.util.Deque");
        rspecRulesReplaceTypeMap.put("java.util.concurrent.ConcurrentLinkedDeque", "java.util.Deque");
        // List
        rspecRulesReplaceTypeMap.put("java.util.AbstractList", "java.util.List");
        rspecRulesReplaceTypeMap.put("java.util.AbstractSequentialList", "java.util.List");
        rspecRulesReplaceTypeMap.put("java.util.ArrayList", "java.util.List");
        rspecRulesReplaceTypeMap.put("java.util.LinkedList", "java.util.List");
        rspecRulesReplaceTypeMap.put("java.util.concurrent.CopyOnWriteArrayList", "java.util.List");
        // Map
        rspecRulesReplaceTypeMap.put("java.util.AbstractMap", "java.util.Map");
        rspecRulesReplaceTypeMap.put("java.util.EnumMap", "java.util.Map");
        rspecRulesReplaceTypeMap.put("java.util.HashMap", "java.util.Map");
        rspecRulesReplaceTypeMap.put("java.util.Hashtable", "java.util.Map");
        rspecRulesReplaceTypeMap.put("java.util.IdentityHashMap", "java.util.Map");
        rspecRulesReplaceTypeMap.put("java.util.LinkedHashMap", "java.util.Map");
        rspecRulesReplaceTypeMap.put("java.util.WeakHashMap", "java.util.Map");
        // ConcurrentMap
        rspecRulesReplaceTypeMap.put("java.util.concurrent.ConcurrentHashMap", "java.util.concurrent.ConcurrentMap");
        rspecRulesReplaceTypeMap.put("java.util.concurrent.ConcurrentSkipListMap", "java.util.concurrent.ConcurrentMap");
        // Queue
        rspecRulesReplaceTypeMap.put("java.util.AbstractQueue", "java.util.Queue");
        rspecRulesReplaceTypeMap.put("java.util.concurrent.ConcurrentLinkedQueue", "java.util.Queue");
        rspecRulesReplaceTypeMap.put("java.util.concurrent.SynchronousQueue", "java.util.Queue");
        // Set
        rspecRulesReplaceTypeMap.put("java.util.AbstractSet", "java.util.Set");
        rspecRulesReplaceTypeMap.put("java.util.EnumSet", "java.util.Set");
        rspecRulesReplaceTypeMap.put("java.util.HashSet", "java.util.Set");
        rspecRulesReplaceTypeMap.put("java.util.LinkedHashSet", "java.util.Set");
        rspecRulesReplaceTypeMap.put("java.util.concurrent.CopyOnWriteArraySet", "java.util.Set");
        // Sorted
        rspecRulesReplaceTypeMap.put("java.util.TreeMap", "java.util.SortedMap");
        rspecRulesReplaceTypeMap.put("java.util.TreeSet", "java.util.SortedSet");

        return new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                for (JavaType type : cu.getTypesInUse()) {
                    if (type instanceof JavaType.Class && rspecRulesReplaceTypeMap.containsKey(((JavaType.Class) type).getFullyQualifiedName())) {
                        return super.visitCompilationUnit(cu, executionContext);
                    }
                }

                return cu;
            }

            @Override
            public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                J.MethodDeclaration m = (J.MethodDeclaration) super.visitMethodDeclaration(method, executionContext);
                if (m.hasModifier(J.Modifier.Type.Public) && m.getReturnTypeExpression() != null) {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(m.getReturnTypeExpression().getType());
                    if (fq != null && rspecRulesReplaceTypeMap.containsKey(fq.getFullyQualifiedName())) {
                        JavaType.FullyQualified newType = TypeUtils.asFullyQualified(
                                JavaType.buildType(rspecRulesReplaceTypeMap.get(fq.getFullyQualifiedName())));
                        if (newType != null) {
                            maybeAddImport(newType.getFullyQualifiedName());
                            maybeRemoveImport(fq.getFullyQualifiedName());
                            TypeTree typeExpression;
                            if (m.getReturnTypeExpression() instanceof J.Identifier) {
                                typeExpression = J.Identifier.build(
                                        Tree.randomId(),
                                        m.getReturnTypeExpression().getPrefix(),
                                        Markers.EMPTY,
                                        newType.getClassName(),
                                        newType);
                            } else {
                                J.ParameterizedType parameterizedType = (J.ParameterizedType) m.getReturnTypeExpression();
                                J.Identifier returnType = J.Identifier.build(
                                        Tree.randomId(),
                                        Space.EMPTY,
                                        Markers.EMPTY,
                                        newType.getClassName(),
                                        newType);
                                typeExpression = parameterizedType.withClazz(returnType);
                            }
                            m = m.withReturnTypeExpression(typeExpression);
                        }
                    }
                }
                return m;
            }

            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                J.VariableDeclarations mv = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, executionContext);
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(mv.getType());
                if (multiVariable.hasModifier(J.Modifier.Type.Public) &&
                        fq != null && rspecRulesReplaceTypeMap.containsKey(fq.getFullyQualifiedName())) {
                    JavaType.FullyQualified newType = TypeUtils.asFullyQualified(
                            JavaType.buildType(rspecRulesReplaceTypeMap.get(fq.getFullyQualifiedName())));
                    if (newType != null) {
                        maybeAddImport(newType.getFullyQualifiedName());
                        maybeRemoveImport(fq.getFullyQualifiedName());

                        TypeTree typeExpression;
                        if (mv.getTypeExpression() == null) {
                            typeExpression = null;
                        } else if (mv.getTypeExpression() instanceof J.Identifier) {
                            typeExpression = J.Identifier.build(
                                    mv.getTypeExpression().getId(),
                                    mv.getTypeExpression().getPrefix(),
                                    Markers.EMPTY,
                                    newType.getClassName(),
                                    newType);
                        } else {
                            J.ParameterizedType parameterizedType = (J.ParameterizedType) mv.getTypeExpression();
                            J.Identifier returnType = J.Identifier.build(
                                    mv.getTypeExpression().getId(),
                                    Space.EMPTY,
                                    Markers.EMPTY,
                                    newType.getClassName(),
                                    newType);
                            typeExpression = parameterizedType.withClazz(returnType);
                        }

                        mv = mv.withTypeExpression(typeExpression);
                        mv = mv.withVariables(ListUtils.map(mv.getVariables(), var -> {
                            JavaType.FullyQualified varType = TypeUtils.asFullyQualified(var.getType());
                            if (varType != null && !varType.equals(newType)) {
                                return var.withType(newType).withName(var.getName().withType(newType));
                            }
                            return var;
                        }));
                    }
                }
                return mv;
            }
        };
    }
}
