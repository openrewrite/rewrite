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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.marker.JavaSearchResult;
import org.openrewrite.java.tree.*;

import java.util.HashSet;
import java.util.Set;

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

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>(fullyQualifiedTypeName);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaType.FullyQualified fullyQualifiedType = JavaType.Class.build(fullyQualifiedTypeName);
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitIdentifier(J.Identifier ident, ExecutionContext executionContext) {
                if (ident.getType() != null) {
                    if (fullyQualifiedType.equals(TypeUtils.asFullyQualified(ident.getType()))) {
                        return ident.withMarkers(ident.getMarkers().addIfAbsent(new JavaSearchResult(FindTypes.this)));
                    }
                }
                return super.visitIdentifier(ident, executionContext);
            }

            @Override
            public <N extends NameTree> N visitTypeName(N name, ExecutionContext ctx) {
                N n = super.visitTypeName(name, ctx);
                JavaType.FullyQualified asFullyQualified = TypeUtils.asFullyQualified(n.getType());
                if (asFullyQualified != null && fullyQualifiedType.isAssignableFrom(asFullyQualified) &&
                        getCursor().firstEnclosing(J.Import.class) == null) {
                    return n.withMarkers(n.getMarkers().addIfAbsent(new JavaSearchResult(FindTypes.this)));
                }
                return n;
            }

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                J.FieldAccess fa = (J.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
                JavaType.FullyQualified asFullyQualified = TypeUtils.asFullyQualified(fa.getTarget().getType());
                if (asFullyQualified != null && fullyQualifiedType.isAssignableFrom(asFullyQualified) &&
                        fa.getName().getSimpleName().equals("class")) {
                    return fa.withMarkers(fa.getMarkers().addIfAbsent(new JavaSearchResult(FindTypes.this)));
                }
                return fa;
            }
        };
    }

    public static Set<NameTree> find(J j, String fullyQualifiedClassName) {
        JavaType.FullyQualified fullyQualifiedType = JavaType.Class.build(fullyQualifiedClassName);

        JavaIsoVisitor<Set<NameTree>> findVisitor = new JavaIsoVisitor<Set<NameTree>>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier ident, Set<NameTree> ns) {
                if (ident.getType() != null) {
                    if (fullyQualifiedType.equals(TypeUtils.asFullyQualified(ident.getType()))) {
                        ns.add(ident);
                    }
                }
                return super.visitIdentifier(ident, ns);
            }

            @Override
            public <N extends NameTree> N visitTypeName(N name, Set<NameTree> ns) {
                N n = super.visitTypeName(name, ns);
                JavaType.FullyQualified asFullyQualified = TypeUtils.asFullyQualified(n.getType());
                if (asFullyQualified != null && fullyQualifiedType.isAssignableFrom(asFullyQualified) &&
                        getCursor().firstEnclosing(J.Import.class) == null) {
                    ns.add(name);
                }
                return n;
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Set<NameTree> ns) {
                J.FieldAccess fa = super.visitFieldAccess(fieldAccess, ns);
                JavaType.FullyQualified asFullyQualified = TypeUtils.asFullyQualified(fa.getTarget().getType());
                if (asFullyQualified != null && fullyQualifiedType.isAssignableFrom(asFullyQualified) &&
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
