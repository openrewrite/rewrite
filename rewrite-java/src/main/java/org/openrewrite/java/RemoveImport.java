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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;

import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class RemoveImport<P> extends JavaIsoVisitor<P> {
    private static final J.Block EMPTY_BLOCK = new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
            new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY), Collections.emptyList(), Space.EMPTY);

    @EqualsAndHashCode.Include
    private final String type;

    private final JavaType.Class classType;
    private final MethodMatcher methodMatcher;

    @Nullable
    private J.Import namedImport;

    @Nullable
    private J.Import starImport;

    @Nullable
    private J.Import staticStarImport;

    private final Set<String> referencedTypes = new HashSet<>();
    private final Set<J.Identifier> referencedMethods = new HashSet<>();
    private final Set<String> referencedFields = new HashSet<>();
    private final Set<J.Import> staticNamedImports = Collections.newSetFromMap(new IdentityHashMap<>());

    public RemoveImport(String type) {
        this.type = type;
        this.methodMatcher = new MethodMatcher(type + " *(..)");
        this.classType = JavaType.Class.build(type);
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, P p) {
        namedImport = null;
        starImport = null;
        staticStarImport = null;
        referencedTypes.clear();
        referencedMethods.clear();
        referencedFields.clear();
        staticNamedImports.clear();

        J.CompilationUnit c = super.visitCompilationUnit(cu, p);
        J.CompilationUnit temp = staticImportDeletions(classImportDeletions(c));
        if (temp != c) {
            Cursor cursor = new Cursor(null, temp);
            temp = temp.withImports(ListUtils.map(temp.getImports(), i -> autoFormat(i, p, cursor)));
            if (!temp.getClasses().isEmpty()) {
                temp = temp.withClasses(ListUtils.mapFirst(temp.getClasses(), firstClass -> {
                    J.ClassDeclaration tempClass = autoFormat(firstClass.withBody(EMPTY_BLOCK), p, cursor);
                    return firstClass.withPrefix(tempClass.getPrefix());
                }));
            }
            c = temp;
        }
        return c;
    }

    @Override
    public J.Import visitImport(J.Import impoort, P p) {
        if (impoort.isStatic()) {
            if (impoort.getQualid().getTarget().printTrimmed().equals(type) || impoort.getQualid().printTrimmed().equals(type)) {
                if ("*".equals(impoort.getQualid().getSimpleName())) {
                    staticStarImport = impoort;
                } else {
                    staticNamedImports.add(impoort);
                }
            }
        } else {
            if (impoort.getQualid().printTrimmed().equals(type)) {
                namedImport = impoort;
            } else if ("*".equals(impoort.getQualid().getSimpleName()) && type.startsWith(impoort.getQualid().getTarget().printTrimmed())) {
                starImport = impoort;
            }
        }

        return super.visitImport(impoort, p);
    }

    @Override
    public <N extends NameTree> N visitTypeName(N name, P p) {
        JavaType.Class asClass = TypeUtils.asClass(name.getType());
        if (asClass != null && asClass.getPackageName().equals(classType.getPackageName()) &&
                getCursor().getPathAsStream().noneMatch(J.Import.class::isInstance)) {
            referencedTypes.add(asClass.getFullyQualifiedName());
        }
        return super.visitTypeName(name, p);
    }

    @Override
    public J.Identifier visitIdentifier(J.Identifier ident, P p) {
        if (getCursor().getPathAsStream().noneMatch(J.Import.class::isInstance) &&
                !(getCursor().getParentOrThrow().getValue() instanceof J.MethodInvocation)) {
            referencedFields.add(ident.getSimpleName());
        }
        return super.visitIdentifier(ident, p);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        if (methodMatcher.matches(method) && method.getType() != null &&
                method.getType().getDeclaringType().getFullyQualifiedName().equals(type)) {
            referencedMethods.add(method.getName());
        }
        return super.visitMethodInvocation(method, p);
    }

    private J.CompilationUnit classImportDeletions(J.CompilationUnit cu) {
        if (namedImport != null && referencedTypes.stream().noneMatch(t -> t.equals(type))) {
            return delete(cu, namedImport);
        } else if (starImport != null && referencedTypes.isEmpty()) {
            return delete(cu, starImport);
        } else if (starImport != null && referencedTypes.size() == 1) {
            return cu.getPadding().withImports(
                    ListUtils.map(cu.getPadding().getImports(), im -> im.map(i -> i == starImport ?
                            new J.Import(randomId(),
                                    i.getPrefix(),
                                    Markers.EMPTY,
                                    new JLeftPadded<>(Space.EMPTY, false, Markers.EMPTY),
                                    TypeTree.build(referencedTypes.iterator().next())
                                            .withPrefix(Space.format(" "))) :
                            i)
                    )
            );
        } else {
            return cu;
        }
    }

    private J.CompilationUnit staticImportDeletions(J.CompilationUnit cu) {
        if (staticStarImport != null) {
            JavaType.Class qualidType = TypeUtils.asClass(staticStarImport.getQualid().getTarget().getType());
            if (referencedMethods.isEmpty() && noFieldReferences(qualidType, null)) {
                cu = delete(cu, staticStarImport);
            }
        }

        for (J.Import staticImport : staticNamedImports) {
            String methodOrField = staticImport.getQualid().getSimpleName();
            JavaType.Class qualidType = TypeUtils.asClass(staticImport.getQualid().getTarget().getType());
            if (referencedMethods.stream().noneMatch(m -> m.getSimpleName().equals(methodOrField)) &&
                    noFieldReferences(qualidType, methodOrField)) {
                cu = delete(cu, staticImport);
            }
        }

        return cu;
    }

    private boolean noFieldReferences(@Nullable JavaType.Class qualidType, @Nullable String fieldName) {
        return qualidType == null || (
                fieldName != null ? !referencedFields.contains(fieldName) :
                        referencedFields.stream().noneMatch(f -> qualidType.getMembers().stream().anyMatch(v -> f.equals(v.getName())) ||
                                qualidType.getVisibleSupertypeMembers().stream().anyMatch(v -> f.equals(v.getName())))
        );
    }

    private J.CompilationUnit delete(J.CompilationUnit cu, J.Import impoort) {
        return cu.getPadding().withImports(ListUtils.map(cu.getPadding().getImports(), i -> i.getElement() == impoort ? null : i));
    }
}
