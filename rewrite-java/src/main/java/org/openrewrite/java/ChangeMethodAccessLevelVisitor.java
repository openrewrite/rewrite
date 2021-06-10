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

import lombok.RequiredArgsConstructor;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.format.MinimumViableSpacingVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.*;

public class ChangeMethodAccessLevelVisitor<P> extends JavaIsoVisitor<P> {
    private static final Set<J.Modifier.Type> EXPLICIT_ACCESS_LEVELS
            = new HashSet<>(Arrays.asList(J.Modifier.Type.Public, J.Modifier.Type.Private, J.Modifier.Type.Protected));
    private final MethodMatcher methodMatcher;
    private final MethodAccessLevel newAccessLevel;

    public ChangeMethodAccessLevelVisitor(MethodMatcher methodMatcher, MethodAccessLevel newAccessLevel) {
        this.methodMatcher = methodMatcher;
        this.newAccessLevel = newAccessLevel;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);
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

            } else if (currentMethodAccessLevel == MethodAccessLevel.Package_private) {
                // If current access level is package-private (no modifier), add the new modifier
                J.Modifier mod = new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, newAccessLevel.modifierType, Collections.emptyList());

                if (!m.getModifiers().isEmpty()) {
                    J.Modifier firstModifier = m.getModifiers().iterator().next();
                    // Move any comments from first modifier to the new modifier
                    if (!firstModifier.getComments().isEmpty()) {
                        mod = mod.withComments(firstModifier.getComments());
                        if (!firstModifier.getPrefix().getWhitespace().isEmpty()) {
                            mod = mod.withPrefix(firstModifier.getPrefix());
                        }
                        m = m.withModifiers(ListUtils.mapFirst(m.getModifiers(), modifier -> modifier.withComments(Collections.emptyList())));
                    }
                    // Add an extra space to the first modifier before adding the new modifier
                    if (Space.firstPrefix(m.getModifiers()).getWhitespace().isEmpty()) {
                        m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(),
                                firstModifier.getPrefix().withWhitespace(" ")));
                    }
                } else {
                    // Move any comments from return type to the new modifier
                    if (m.getReturnTypeExpression() != null && !m.getReturnTypeExpression().getComments().isEmpty()) {
                        mod = mod.withComments(m.getReturnTypeExpression().getComments());
                        if (!m.getReturnTypeExpression().getPrefix().getWhitespace().isEmpty()) {
                            mod = mod.withPrefix(m.getReturnTypeExpression().getPrefix());
                        }
                        m = m.withReturnTypeExpression(m.getReturnTypeExpression().withComments(Collections.emptyList()));
                    }
                }
                m = m.withModifiers(ListUtils.concat(mod, m.getModifiers()));
                m = (J.MethodDeclaration) new MinimumViableSpacingVisitor<Integer>(
                        m.getName()).visit(m, 0, getCursor());

            } else if (newAccessLevel == MethodAccessLevel.Package_private) {
                // If target access level is package-private (no modifier), remove the current access level modifier
                // and copy any associated comments
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
                m = maybeAutoFormat(m, m.withModifiers(modifiers), p, getCursor().dropParentUntil(J.class::isInstance));
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
