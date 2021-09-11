/*
 * Copyright 2021 the original author or authors.
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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;

import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NoEmptyCollectionWithRawType extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use `Collections#` `emptyList()`, `emptyMap()`, and `emptySet()`";
    }

    @Override
    public String getDescription() {
        return "Replaces `Collections#EMPTY_..` with methods that return generic types.";
    }

    @Nullable
    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("java.util.Collections");
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1596");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {

        return new JavaVisitor<ExecutionContext>() {
            private final JavaType.FullyQualified COLLECTIONS_FQN =
                    JavaType.Class.build("java.util.Collections");

            private final Map<String, String> updateFields = new HashMap<>();
            {
                updateFields.put("EMPTY_LIST", "emptyList");
                updateFields.put("EMPTY_MAP", "emptyMap");
                updateFields.put("EMPTY_SET", "emptySet");
            }

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                J.FieldAccess fa = (J.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
                if (TypeUtils.isOfType(COLLECTIONS_FQN, fa.getTarget().getType()) &&
                        updateFields.containsKey(fa.getName().getSimpleName())) {

                    boolean isImport = getCursor().dropParentUntil(is ->
                            is instanceof J.CompilationUnit ||
                            is instanceof J.ClassDeclaration).getValue() instanceof J.CompilationUnit;
                    String postFix = isImport ? "" : "()";

                    String fieldAccessName = fa.getTarget() instanceof J.FieldAccess ?
                            COLLECTIONS_FQN.getFullyQualifiedName() : COLLECTIONS_FQN.getClassName();

                    return TypeTree.build(fieldAccessName + "." + updateFields.get(fieldAccess.getName().getSimpleName()) + postFix)
                            .withPrefix(fa.getPrefix());
                }
                return fa;
            }

            @Override
            public J visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                J.Identifier id = (J.Identifier) super.visitIdentifier(identifier, ctx);
                if (isTargetFieldType(id) && updateFields.containsKey(id.getSimpleName())) {
                    JavaType.Class type = TypeUtils.asClass(JavaType.buildType("java.util.Collections"));
                    if (type != null) {
                        JavaType.Method method =
                                type.getMethods().stream()
                                        .filter(m -> m.getName().equals(updateFields.get(id.getSimpleName())))
                                        .collect(Collectors.toList()).get(0);

                        J.Identifier methodId = J.Identifier.build(
                                Tree.randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                updateFields.get(id.getSimpleName()),
                                null,
                                null);

                        return new J.MethodInvocation(
                                Tree.randomId(),
                                id.getPrefix(),
                                id.getMarkers(),
                                null,
                                null,
                                methodId,
                                JContainer.build(Collections.emptyList()),
                                method
                        );
                    }
                }
                return id;
            }

            private boolean isTargetFieldType(J.Identifier identifier) {
                if (identifier.getFieldType() != null && identifier.getFieldType() instanceof JavaType.Variable) {
                    JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(((JavaType.Variable) identifier.getFieldType()).getOwner());
                    return fqn != null && COLLECTIONS_FQN.getFullyQualifiedName().equals(fqn.getFullyQualifiedName());
                }
                return false;
            }
        };
    }
}
