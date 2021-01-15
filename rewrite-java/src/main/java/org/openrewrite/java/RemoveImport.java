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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class RemoveImport<P> extends JavaIsoProcessor<P> {
    @EqualsAndHashCode.Include
    private final String type;

    private final JavaType.Class classType;
    private final MethodMatcher methodMatcher;

    private J.Import namedImport;
    private J.Import starImport;
    private J.Import staticStarImport;

    private final Set<String> referencedTypes = new HashSet<>();
    private final Set<J.Ident> referencedMethods = new HashSet<>();
    private final Set<String> referencedFields = new HashSet<>();
    private final Set<J.Import> staticNamedImports = Collections.newSetFromMap(new IdentityHashMap<>());

    public RemoveImport(String type) {
        this.type = type;
        this.methodMatcher = new MethodMatcher(type + " *(..)");
        this.classType = JavaType.Class.build(type);
        setCursoringOn();
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
        c = staticImportDeletions(classImportDeletions(c));
        if (c.getImports().isEmpty()) {
            if (c.getClasses().iterator().next().getPrefix().getWhitespace().startsWith("\n")) {
                c = c.withClasses(ListUtils.mapFirst(c.getClasses(), cl -> cl.withPrefix(cl.getPrefix().withWhitespace(
                        cl.getPrefix().getWhitespace().replaceFirst("\n", "")))));
            }
        }
        if (c.getImports().size() == 1) {
            if (c.getPackageDecl() == null) {
                c = c.withImports(
                        ListUtils.mapFirst(c.getImports(),
                                i -> i.withElem(
                                        i.getElem().withPrefix(
                                                i.getElem().getPrefix().withWhitespace(
                                                        i.getElem().getPrefix().getWhitespace().replace("\n", "")
                                                )
                                        )
                                )
                        )
                );
            }
        }
        return c;
    }

    @Override
    public J.Import visitImport(J.Import impoort, P p) {
        if (impoort.isStatic()) {
            if (impoort.getQualid().getTarget().printTrimmed().equals(type)) {
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
    public J.Ident visitIdentifier(J.Ident ident, P p) {
        if (getCursor().getPathAsStream().noneMatch(J.Import.class::isInstance)) {
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
            return cu.withImports(
                    ListUtils.map(cu.getImports(), im -> im.map(i -> i == starImport ?
                            new J.Import(randomId(),
                                    i.getPrefix(),
                                    Markers.EMPTY,
                                    null,
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
        return cu.withImports(ListUtils.map(cu.getImports(), i -> i.getElem() == impoort ? null : i));
    }
}
