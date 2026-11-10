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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.MethodCall;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeMethodAccessLevelVisitor<P> extends JavaIsoVisitor<P> {
    private static final String CHANGED_DECLARATIONS = "CHANGED_DECLARATIONS";

    private static final Collection<J.Modifier.Type> EXPLICIT_ACCESS_LEVELS = Arrays.asList(J.Modifier.Type.Public,
            J.Modifier.Type.Private, J.Modifier.Type.Protected);

    MethodMatcher methodMatcher;

    J.Modifier.@Nullable Type newAccessLevel;

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        if (changes(method, getCursor().firstEnclosing(J.ClassDeclaration.class))) {
            J.Modifier.Type currentMethodAccessLevel = accessLevel(m);

            // Replace former modifier by new modifier if package-private is not involved
            if (EXPLICIT_ACCESS_LEVELS.contains(currentMethodAccessLevel) && EXPLICIT_ACCESS_LEVELS.contains(newAccessLevel)) {
                m = m.withModifiers(
                        ListUtils.map(m.getModifiers(), mod -> mod.getType() == currentMethodAccessLevel ?
                                mod.withType(newAccessLevel) : mod)
                );
            }

            // If current access level is package-private (no modifier), add the new modifier
            else if (currentMethodAccessLevel == null) {
                J.Modifier mod = new J.Modifier(Tree.randomId(), Space.build(" ", emptyList()), Markers.EMPTY, null, newAccessLevel, emptyList());
                m = m.withModifiers(ListUtils.concat(mod, m.getModifiers()));

                if(method.getModifiers().isEmpty()) {
                    J.TypeParameters typeParams = m.getPadding().getTypeParameters();
                    if(typeParams == null) {
                        TypeTree returnExpr = m.getReturnTypeExpression();
                        if(returnExpr == null) {
                            m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), m.getName().getPrefix()));
                            m = m.withName(m.getName().withPrefix(Space.format(" ")));
                        } else {
                            m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), returnExpr.getPrefix()));
                            m = m.withReturnTypeExpression(returnExpr.withPrefix(Space.format(" ")));
                        }
                    } else {
                        m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), typeParams.getPrefix()));
                        m = m.getPadding().withTypeParameters(typeParams.withPrefix(Space.format(" ")));
                    }
                }
                else {
                    m = m.withModifiers(ListUtils.map(m.getModifiers(), (i, mod2) -> {
                        if (i == 0) {
                            return mod2.withPrefix(method.getModifiers().get(0).getPrefix());
                        } else if (i == 1) {
                            return mod2.withPrefix(Space.format(" "));
                        }
                        return mod2;
                    }));
                }
            }

            // If target access level is package-private (no modifier), remove the current access level modifier
            // and copy any associated comments and space
            else if (newAccessLevel == null) {
                final List<Comment> modifierComments = new ArrayList<>();
                final AtomicReference<@Nullable Space> removedModifierPrefix = new AtomicReference<>(null);
                List<J.Modifier> modifiers = ListUtils.map(m.getModifiers(), mod -> {
                    if (mod.getType() == currentMethodAccessLevel) {
                        modifierComments.addAll(mod.getComments());
                        removedModifierPrefix.set(mod.getPrefix());
                        return null;
                    }

                    Space removedSpace = removedModifierPrefix.getAndSet(null);
                    if (removedSpace != null) {
                        J.Modifier nextModifier = mod.withPrefix(removedSpace.withComments(
                                ListUtils.concatAll(removedSpace.getComments(), mod.getComments())));
                        modifierComments.clear();

                        return nextModifier;
                    }

                    // copy access level modifier comment to next modifier if it exists (fallback)
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
                m = maybeAutoFormat(m, m.withModifiers(modifiers), p).withBody(m.getBody());
            }

            if (m.getMethodType() != null) {
                m = m.withMethodType(withAccessLevel(m.getMethodType()));
            }
        }
        return m;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = super.visitMethodInvocation(method, p);
        J.MethodInvocation retyped = withAccessLevel(m);
        return retyped == m || retyped.getName().getType() == null ? retyped :
                retyped.withName(retyped.getName().withType(retyped.getMethodType()));
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, P p) {
        return withAccessLevel(super.visitNewClass(newClass, p));
    }

    @Override
    public J.MemberReference visitMemberReference(J.MemberReference memberRef, P p) {
        return withAccessLevel(super.visitMemberReference(memberRef, p));
    }

    @SuppressWarnings("unchecked")
    private <C extends MethodCall> C withAccessLevel(C call) {
        JavaType.Method methodType = call.getMethodType();
        return methodType != null && methodMatcher.matches(call) && changedDeclarations().contains(key(methodType)) ?
                (C) call.withMethodType(withAccessLevel(methodType)) : call;
    }

    /**
     * The methods whose declarations in this file get the new access level. The pattern can also match a method
     * declared elsewhere, or one whose declaration keeps its access level, and the calls of those keep theirs.
     */
    private Set<String> changedDeclarations() {
        Cursor cursor = getCursor();
        while (cursor != null && !(cursor.getValue() instanceof JavaSourceFile)) {
            cursor = cursor.getParent();
        }
        if (cursor == null) {
            return emptySet();
        }
        JavaSourceFile sourceFile = cursor.getValue();
        // a recipe can run several of these visitors under one cursor
        return cursor.computeMessageIfAbsent(CHANGED_DECLARATIONS + System.identityHashCode(this), k -> {
            Set<String> changed = new HashSet<>();
            new JavaIsoVisitor<Set<String>>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Set<String> c) {
                    if (method.getMethodType() != null &&
                        changes(method, getCursor().firstEnclosing(J.ClassDeclaration.class))) {
                        c.add(key(method.getMethodType()));
                    }
                    return super.visitMethodDeclaration(method, c);
                }
            }.visit(sourceFile, changed);
            return changed;
        });
    }

    private boolean changes(J.MethodDeclaration method, J.@Nullable ClassDeclaration classDecl) {
        return classDecl != null && methodMatcher.matches(method, classDecl) && accessLevel(method) != newAccessLevel;
    }

    private static String key(JavaType.Method methodType) {
        return methodType.getDeclaringType().getFullyQualifiedName().replace('$', '.') + " " + methodType.getName() +
               "/" + methodType.getParameterTypes().size();
    }

    private static J.Modifier.@Nullable Type accessLevel(J.MethodDeclaration method) {
        return method.getModifiers().stream()
                .map(J.Modifier::getType)
                .filter(EXPLICIT_ACCESS_LEVELS::contains)
                .findAny()
                .orElse(null);
    }

    private JavaType.Method withAccessLevel(JavaType.Method methodType) {
        Set<Flag> flags = EnumSet.noneOf(Flag.class);
        flags.addAll(methodType.getFlags());
        flags.removeAll(Arrays.asList(Flag.Public, Flag.Protected, Flag.Private));
        if (newAccessLevel != null) {
            flags.add(newAccessLevel == J.Modifier.Type.Public ? Flag.Public :
                    newAccessLevel == J.Modifier.Type.Protected ? Flag.Protected : Flag.Private);
        }
        return methodType.withFlags(flags);
    }
}
