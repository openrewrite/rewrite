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
package org.openrewrite.java;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.format.MinimumViableSpacingVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.*;

import static java.util.Collections.emptyList;

public class ChangeMethodAccessLevelVisitor extends JavaIsoVisitor<ExecutionContext> {
    private static final Set<J.Modifier.Type> EXPLICIT_ACCESS_LEVELS
            = new HashSet<>(Arrays.asList(J.Modifier.Type.Public, J.Modifier.Type.Private, J.Modifier.Type.Protected));
    private final MethodMatcher methodMatcher;
    private final MethodAccessLevel newAccessLevel;

    public ChangeMethodAccessLevelVisitor(MethodMatcher methodMatcher, MethodAccessLevel newAccessLevel) {
        this.methodMatcher = methodMatcher;
        this.newAccessLevel = newAccessLevel;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, executionContext);
        J.ClassDeclaration classDecl = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
        if (methodMatcher.matches(method, classDecl)) {
            MethodAccessLevel currentMethodAccessLevel = m.getModifiers().stream()
                    .map(J.Modifier::getType)
                    .filter(EXPLICIT_ACCESS_LEVELS::contains)
                    .map(MethodAccessLevel::fromModifierType)
                    .findAny()
                    .orElse(MethodAccessLevel.Package_private);
            if (currentMethodAccessLevel == newAccessLevel) {
                // No changes required
                return m;
            }

            // Replace former modifier by new modifier if package-private is not involved
            if (EXPLICIT_ACCESS_LEVELS.contains(currentMethodAccessLevel.modifierType)
                    && EXPLICIT_ACCESS_LEVELS.contains(newAccessLevel.modifierType)) {
                m = m.withModifiers(
                    ListUtils.map(m.getModifiers(), mod -> mod.getType() == currentMethodAccessLevel.modifierType ? mod.withType(newAccessLevel.modifierType) : mod)
                );
            }
            // If current access level is package-private (no modifier), add the new modifier
            else if (currentMethodAccessLevel == MethodAccessLevel.Package_private) {
                J.Modifier mod = new J.Modifier(Tree.randomId(), Space.build(" ", emptyList()), Markers.EMPTY, newAccessLevel.modifierType, Collections.emptyList());
                m = m.withModifiers(ListUtils.concat(mod, m.getModifiers()));
                m = (J.MethodDeclaration) new MinimumViableSpacingVisitor<Integer>(
                        m.getName()).visit(m, 0, getCursor());
                m = maybeAutoFormat(m, m, executionContext, getCursor().dropParentUntil(J.class::isInstance));
            }
            // If target access level is package-private (no modifier), remove the current access level modifier
            // and copy any associated comments
            else if (newAccessLevel == MethodAccessLevel.Package_private) {
                final List<Comment> modifierComments = new ArrayList<>();
                List<J.Modifier> modifiers = ListUtils.map(m.getModifiers(), mod -> {
                    if (mod.getType() == currentMethodAccessLevel.modifierType) {
                        modifierComments.addAll(mod.getComments());
                        return null;
                    }
                    // copy access level modifier comment to next modifier if it exists
                    if (!modifierComments.isEmpty()) {
                        J.Modifier nextModifier = mod.withComments(ListUtils.concatAll(new ArrayList<>(modifierComments), mod.getComments()));
                        modifierComments.clear();
                        return nextModifier;
                    }
                    return mod;
                });
                // if no following modifier exists, add comments to method itself
                if (!modifierComments.isEmpty()) {
                    m = m.withComments(ListUtils.concatAll(m.getComments(), modifierComments));
                }
                m = maybeAutoFormat(m, m.withModifiers(modifiers), executionContext, getCursor().dropParentUntil(J.class::isInstance));
            }
        }
        return m;
    }


    @RequiredArgsConstructor
    public enum MethodAccessLevel {
        Public("public", J.Modifier.Type.Public),
        Protected("protected", J.Modifier.Type.Protected),
        Private("private", J.Modifier.Type.Private),
        Package_private("package-private", null);

        private final String keyword;
        private final J.Modifier.Type modifierType;

        public static MethodAccessLevel fromKeyword(String keyword) {
            for (MethodAccessLevel accessLevel : values()) {
                if (accessLevel.keyword.equals(keyword)) {
                    return accessLevel;
                }
            }
            throw new IllegalArgumentException("Invalid keyword for method access level: " + keyword);
        }

        private static MethodAccessLevel fromModifierType(J.Modifier.Type modifierType) {
            for (MethodAccessLevel accessLevel : values()) {
                if (Objects.equals(accessLevel.modifierType, modifierType)) {
                    return accessLevel;
                }
            }
            throw new IllegalArgumentException("Invalid J.Modifier.Type for method access level: " + modifierType);
        }
    }
}
