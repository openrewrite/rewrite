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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.cleanup.ModifierOrder.sortModifiers;

public class FinalClassVisitor extends JavaIsoVisitor<ExecutionContext> {

    Tree visitRoot;

    final Set<String> typesToFinalize = new HashSet<>();
    final Set<String> typesToNotFinalize = new HashSet<>();

    @Override
    public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
        boolean root = false;
        if (visitRoot == null && tree != null) {
            visitRoot = tree;
            root = true;
        }
        J result = super.visit(tree, ctx);
        if (root) {
            visitRoot = null;
            typesToFinalize.removeAll(typesToNotFinalize);
            if (!typesToFinalize.isEmpty()) {
                result = new FinalizingVisitor(typesToFinalize).visit(tree, ctx);
            }
        }
        return result;
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDeclaration, ExecutionContext ctx) {
        J.ClassDeclaration cd = super.visitClassDeclaration(classDeclaration, ctx);

        if (cd.getKind() != J.ClassDeclaration.Kind.Type.Class || cd.hasModifier(J.Modifier.Type.Abstract)
                || cd.hasModifier(J.Modifier.Type.Final) || cd.getType() == null) {
            return cd;
        }

        excludeSupertypes(cd.getType());

        boolean allPrivate = true;
        int constructorCount = 0;
        for (Statement s : cd.getBody().getStatements()) {
            if (s instanceof J.MethodDeclaration && ((J.MethodDeclaration) s).isConstructor()) {
                J.MethodDeclaration constructor = (J.MethodDeclaration) s;
                constructorCount++;
                if (!constructor.hasModifier(J.Modifier.Type.Private)) {
                    allPrivate = false;
                }
            }
            if (constructorCount > 0 && !allPrivate) {
                return cd;
            }
        }

        if (constructorCount > 0) {
            typesToFinalize.add(cd.getType().getFullyQualifiedName());
        }

        return cd;
    }

    private void excludeSupertypes(JavaType.FullyQualified type) {
        if (type.getSupertype() != null && type.getOwningClass() != null
                && typesToNotFinalize.add(type.getSupertype().getFullyQualifiedName())) {
            excludeSupertypes(type.getSupertype());
        }
    }

    /**
     * Adding the `final` modifier is performed in a second phase, because we first need to check if any of the
     * classes need to remain non-final due to inheritance.
     */
    private static class FinalizingVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final Set<String> typesToFinalize;

        public FinalizingVisitor(Set<String> typesToFinalize) {
            this.typesToFinalize = typesToFinalize;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
            if (cd.getType() != null && typesToFinalize.remove(cd.getType().getFullyQualifiedName())) {
                List<J.Modifier> modifiers = new ArrayList<>(cd.getModifiers());
                modifiers.add(new J.Modifier(randomId(), Space.EMPTY, Markers.EMPTY, J.Modifier.Type.Final, emptyList()));
                modifiers = sortModifiers(modifiers);
                cd = cd.withModifiers(modifiers);
                if (cd.getType() instanceof JavaType.Class && !cd.getType().hasFlags(Flag.Final)) {
                    Set<Flag> flags = new HashSet<>(cd.getType().getFlags());
                    flags.add(Flag.Final);
                    cd = cd.withType(((JavaType.Class) cd.getType()).withFlags(flags));
                }

                // Temporary work around until issue https://github.com/openrewrite/rewrite/issues/2348 is implemented.
                if (!cd.getLeadingAnnotations().isEmpty()) {
                    // Setting the prefix to empty will cause the `Spaces` visitor to fix the formatting.
                    cd = cd.getAnnotations().withKind(cd.getAnnotations().getKind().withPrefix(Space.EMPTY));
                }

                assert getCursor().getParent() != null;
                cd = autoFormat(cd, cd.getName(), ctx, getCursor().getParent());
            }
            return cd;
        }
    }
}
