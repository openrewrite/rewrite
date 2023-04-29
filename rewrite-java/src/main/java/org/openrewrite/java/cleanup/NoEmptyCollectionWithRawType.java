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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;

public class NoEmptyCollectionWithRawType extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use `Collections#emptyList()`, `emptyMap()`, and `emptySet()`";
    }

    @Override
    public String getDescription() {
        return "Replaces `Collections#EMPTY_..` with methods that return generic types.";
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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Map<String, String> updateFields = new HashMap<>();
        updateFields.put("EMPTY_LIST", "emptyList");
        updateFields.put("EMPTY_MAP", "emptyMap");
        updateFields.put("EMPTY_SET", "emptySet");

        return Preconditions.check(new UsesType<>("java.util.Collections", false), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitImport(J.Import anImport, ExecutionContext executionContext) {
                J.Identifier name = anImport.getQualid().getName();
                if (anImport.isStatic() && name.getSimpleName().startsWith("EMPTY_") &&
                    TypeUtils.isOfClassType(anImport.getQualid().getTarget().getType(), "java.util.Collections")) {
                    return anImport.withQualid(anImport.getQualid().withName(name.withSimpleName(updateFields.get(name.getSimpleName()))));
                }
                return super.visitImport(anImport, executionContext);
            }

            @Override
            public J visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                J.Identifier id = (J.Identifier) super.visitIdentifier(identifier, ctx);
                JavaType.Variable varType = id.getFieldType();
                if (varType != null && TypeUtils.isOfClassType(varType.getOwner(), "java.util.Collections") &&
                    varType.getName().startsWith("EMPTY_")) {

                    J.Identifier methodId = new J.Identifier(
                            Tree.randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            updateFields.get(varType.getName()),
                            null,
                            null
                    );

                    JavaType.Method method = null;

                    //noinspection ConstantConditions
                    for (JavaType.Method m : TypeUtils.asFullyQualified(varType.getOwner()).getMethods()) {
                        if (m.getName().equals(updateFields.get(id.getSimpleName()))) {
                            method = m;
                            break;
                        }
                    }

                    return new J.MethodInvocation(
                            Tree.randomId(),
                            id.getPrefix(),
                            id.getMarkers(),
                            null,
                            null,
                            methodId,
                            JContainer.build(emptyList()),
                            method
                    );
                }
                return id;
            }
        });
    }
}
