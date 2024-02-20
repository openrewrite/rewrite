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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindTypes extends Recipe {

    @Option(displayName = "Fully-qualified type name",
            description = "A fully-qualified type name, that is used to find matching type references. " +
                          "Supports glob expressions. `java..*` finds every type from every subpackage of the `java` package.",
            example = "java.util.List")
    String fullyQualifiedTypeName;

    @Option(displayName = "Check for assignability",
            description = "When enabled, find type references that are assignable to the provided type.",
            required = false)
    @Nullable
    Boolean checkAssignability;

    @Override
    public String getDisplayName() {
        return "Find types";
    }

    @Override
    public String getDescription() {
        return "Find type references by name.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern fullyQualifiedType = Pattern.compile(StringUtils.aspectjNameToPattern(fullyQualifiedTypeName));

        return Preconditions.check(new UsesType<>(fullyQualifiedTypeName, false), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                if (ident.getType() != null &&
                        getCursor().firstEnclosing(J.Import.class) == null &&
                        getCursor().firstEnclosing(J.FieldAccess.class) == null &&
                        !(getCursor().getParentOrThrow().getValue() instanceof J.ParameterizedType) &&
                        !(getCursor().getParentOrThrow().getValue() instanceof J.ArrayType)) {
                    JavaType.FullyQualified type = TypeUtils.asFullyQualified(ident.getType());
                    if (typeMatches(Boolean.TRUE.equals(checkAssignability), fullyQualifiedType, type) &&
                        ident.getSimpleName().equals(type.getClassName())) {
                        return SearchResult.found(ident);
                    }
                }
                return super.visitIdentifier(ident, ctx);
            }

            @Override
            public <N extends NameTree> N visitTypeName(N name, ExecutionContext ctx) {
                N n = super.visitTypeName(name, ctx);
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(n.getType());
                if (typeMatches(Boolean.TRUE.equals(checkAssignability), fullyQualifiedType, type) &&
                        getCursor().firstEnclosing(J.Import.class) == null) {
                    return SearchResult.found(n);
                }
                return n;
            }

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                J.FieldAccess fa = (J.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(fa.getTarget().getType());
                if (typeMatches(Boolean.TRUE.equals(checkAssignability), fullyQualifiedType, type) &&
                    fa.getName().getSimpleName().equals("class")) {
                    return SearchResult.found(fa);
                }
                return fa;
            }
        });
    }

    public static Set<NameTree> findAssignable(J j, String fullyQualifiedClassName) {
        return find(true, j, fullyQualifiedClassName);
    }

    public static Set<NameTree> find(J j, String fullyQualifiedClassName) {
        return find(false, j, fullyQualifiedClassName);
    }

    private static Set<NameTree> find(boolean checkAssignability, J j, String fullyQualifiedClassName) {
        Pattern fullyQualifiedType = Pattern.compile(StringUtils.aspectjNameToPattern(fullyQualifiedClassName));

        JavaIsoVisitor<Set<NameTree>> findVisitor = new JavaIsoVisitor<Set<NameTree>>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier ident, Set<NameTree> ns) {
                if (ident.getType() != null) {
                    JavaType.FullyQualified type = TypeUtils.asFullyQualified(ident.getType());
                    if (typeMatches(checkAssignability, fullyQualifiedType, type) && ident.getSimpleName().equals(type.getClassName())) {
                        ns.add(ident);
                    }
                }
                return super.visitIdentifier(ident, ns);
            }

            @Override
            public <N extends NameTree> N visitTypeName(N name, Set<NameTree> ns) {
                N n = super.visitTypeName(name, ns);
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(n.getType());
                if (typeMatches(checkAssignability, fullyQualifiedType, type) &&
                    getCursor().firstEnclosing(J.Import.class) == null) {
                    ns.add(name);
                }
                return n;
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Set<NameTree> ns) {
                J.FieldAccess fa = super.visitFieldAccess(fieldAccess, ns);
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(fa.getTarget().getType());
                if (typeMatches(checkAssignability, fullyQualifiedType, type) &&
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

    private static boolean typeMatches(boolean checkAssignability, Pattern pattern,
                                       @Nullable JavaType.FullyQualified test) {
        return test != null && (checkAssignability ?
                test.isAssignableFrom(pattern) :
                pattern.matcher(test.getFullyQualifiedName()).matches()
        );
    }
}
