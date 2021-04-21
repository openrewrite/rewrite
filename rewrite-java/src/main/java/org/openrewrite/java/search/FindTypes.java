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
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.marker.JavaSearchResult;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * This recipe finds all explicit references to a type.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class FindTypes extends Recipe {

    @Option(displayName = "Fully-qualified type name",
            description = "A fully-qualified type name, that is used to find matching type references.",
            example = "java.util.List")
    String fullyQualifiedTypeName;

    @Override
    public String getDisplayName() {
        return "Find types";
    }

    @Override
    public String getDescription() {
        return "Find type references by name.";
    }

    UUID id = Tree.randomId();

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public <N extends NameTree> N visitTypeName(N name, ExecutionContext ctx) {
                N n = super.visitTypeName(name, ctx);
                JavaType.Class asClass = TypeUtils.asClass(n.getType());
                if (asClass != null && asClass.getFullyQualifiedName().equals(fullyQualifiedTypeName) &&
                        getCursor().firstEnclosing(J.Import.class) == null) {
                    ctx.putMessageInSet(JavaType.FOUND_TYPE_CONTEXT_KEY, asClass);
                    return n.withMarkers(n.getMarkers().addOrUpdate(new JavaSearchResult(id, FindTypes.this)));
                }
                return n;
            }

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                J.FieldAccess fa = (J.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
                JavaType.Class asClass = TypeUtils.asClass(fa.getTarget().getType());
                if (asClass != null && asClass.getFullyQualifiedName().equals(fullyQualifiedTypeName) &&
                        fa.getName().getSimpleName().equals("class")) {
                    ctx.putMessageInSet(JavaType.FOUND_TYPE_CONTEXT_KEY, asClass);
                    return fa.withMarkers(fa.getMarkers().addOrUpdate(new JavaSearchResult(id, FindTypes.this)));
                }
                return fa;
            }
        };
    }

    public static Set<NameTree> find(J j, String fullyQualifiedClassName) {
        JavaIsoVisitor<Set<NameTree>> findVisitor = new JavaIsoVisitor<Set<NameTree>>() {

            @Override
            public <N extends NameTree> N visitTypeName(N name, Set<NameTree> ns) {
                N n = super.visitTypeName(name, ns);
                JavaType.Class asClass = TypeUtils.asClass(n.getType());
                if (asClass != null && asClass.getFullyQualifiedName().equals(fullyQualifiedClassName) &&
                        getCursor().firstEnclosing(J.Import.class) == null) {
                    ns.add(name);
                }
                return n;
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Set<NameTree> ns) {
                J.FieldAccess fa = super.visitFieldAccess(fieldAccess, ns);
                JavaType.Class targetClass = TypeUtils.asClass(fa.getTarget().getType());
                if (targetClass != null && targetClass.getFullyQualifiedName().equals(fullyQualifiedClassName) &&
                        fa.getName().getSimpleName().equals("class")) {
                    ns.add(fieldAccess);
                }
                return fa;
            }
        };

        Set<NameTree> ts = new HashSet<>();
        findVisitor.visit(j, ts);
        return ts;
    }
}
