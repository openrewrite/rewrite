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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.TypeNameMatcher;
import org.openrewrite.java.table.TypeUses;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.trait.Trait;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindTypes extends Recipe {
    transient TypeUses typeUses = new TypeUses(this);

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
        TypeNameMatcher fullyQualifiedType = TypeNameMatcher.fromPattern(fullyQualifiedTypeName);

        return Preconditions.check(new UsesType<>(fullyQualifiedTypeName, false), new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof JavaSourceFile || sourceFile instanceof SourceFileWithReferences;
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    return new JavaSourceFileVisitor(fullyQualifiedType).visit(tree, ctx);
                } else if (tree instanceof SourceFileWithReferences) {
                    SourceFileWithReferences sourceFile = (SourceFileWithReferences) tree;
                    SourceFileWithReferences.References references = sourceFile.getReferences();
                    TypeMatcher matcher = new TypeMatcher(fullyQualifiedTypeName);
                    Set<Tree> matches = references.findMatches(matcher).stream().map(Trait::getTree).collect(toSet());
                    return new ReferenceVisitor(matches).visit(tree, ctx);
                }
                return tree;
            }
        });
    }

    @SuppressWarnings("unused")
    public static Set<NameTree> findAssignable(J j, String fullyQualifiedClassName) {
        return find(true, j, fullyQualifiedClassName);
    }

    public static Set<NameTree> find(J j, String fullyQualifiedClassName) {
        return find(false, j, fullyQualifiedClassName);
    }

    private static Set<NameTree> find(boolean checkAssignability, J j, String fullyQualifiedClassName) {
        TypeNameMatcher fullyQualifiedType = TypeNameMatcher.fromPattern(fullyQualifiedClassName);

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
                        "class".equals(fa.getName().getSimpleName())) {
                    ns.add(fieldAccess);
                }
                return fa;
            }
        };

        Set<NameTree> ts = new HashSet<>();
        findVisitor.visit(j, ts);
        return ts;
    }

    private static boolean typeMatches(boolean checkAssignability, TypeNameMatcher matcher,
                                       JavaType.@Nullable FullyQualified test) {
        if (test == null) {
            return false;
        }
        if (checkAssignability) {
            return TypeUtils.isAssignableTo(
                    t -> t instanceof JavaType.FullyQualified &&
                            matcher.matches(((JavaType.FullyQualified) t).getFullyQualifiedName()),
                    test);
        } else {
            return matcher.matches(test.getFullyQualifiedName());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class ReferenceVisitor extends TreeVisitor<Tree, ExecutionContext> {
        Set<Tree> matches;

        @Override
        public Tree postVisit(Tree tree, ExecutionContext ctx) {
            return matches.contains(tree) ? SearchResult.found(tree) : tree;
        }
    }

    private class JavaSourceFileVisitor extends JavaVisitor<ExecutionContext> {
        private final TypeNameMatcher fullyQualifiedType;

        public JavaSourceFileVisitor(TypeNameMatcher fullyQualifiedType) {
            this.fullyQualifiedType = fullyQualifiedType;
        }

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
                    return found(ident, ctx);
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
                return found(n, ctx);
            }
            return n;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            J.FieldAccess fa = (J.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(fa.getTarget().getType());
            if (typeMatches(Boolean.TRUE.equals(checkAssignability), fullyQualifiedType, type) &&
                    "class".equals(fa.getName().getSimpleName())) {
                return found(fa, ctx);
            }
            return fa;
        }

        private <J2 extends TypedTree> J2 found(J2 j, ExecutionContext ctx) {
            JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(j.getType());
            if (!j.getMarkers().findFirst(SearchResult.class).isPresent()) {
                // Avoid double-counting results in the data table
                typeUses.insertRow(ctx, new TypeUses.Row(
                        getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath().toString(),
                        j.printTrimmed(getCursor().getParentTreeCursor()),
                        fqn == null ? j.getType().toString() : fqn.getFullyQualifiedName()
                ));
            }
            return SearchResult.found(j);
        }
    }
}
