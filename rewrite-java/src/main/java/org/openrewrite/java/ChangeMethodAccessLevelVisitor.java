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
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static org.openrewrite.internal.ListUtils.concatAll;
import static org.openrewrite.internal.ListUtils.mapLast;
import static org.openrewrite.internal.StringUtils.hasLineBreak;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeMethodAccessLevelVisitor<P> extends JavaIsoVisitor<P> {
    private static final Pattern LINE_BREAK = Pattern.compile("\\R");
    private static final Collection<J.Modifier.Type> EXPLICIT_ACCESS_LEVELS = Arrays.asList(J.Modifier.Type.Public,
            J.Modifier.Type.Private, J.Modifier.Type.Protected);

    MethodMatcher methodMatcher;

    J.Modifier.@Nullable Type newAccessLevel;

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
        if (classDecl == null) {
            return m;
        }
        if (methodMatcher.matches(method, classDecl)) {
            J.Modifier.Type currentMethodAccessLevel = m.getModifiers().stream()
                    .map(J.Modifier::getType)
                    .filter(EXPLICIT_ACCESS_LEVELS::contains)
                    .findAny()
                    .orElse(null);

            if (currentMethodAccessLevel == newAccessLevel) {
                // No changes required
                return m;
            }

            // Replace former modifier by new modifier if package-private is not involved
            if (EXPLICIT_ACCESS_LEVELS.contains(currentMethodAccessLevel) && EXPLICIT_ACCESS_LEVELS.contains(newAccessLevel)) {
                m = m.withModifiers(
                        ListUtils.map(m.getModifiers(), mod -> mod.getType() == currentMethodAccessLevel ?
                                mod.withType(newAccessLevel) : mod)
                );
            }

            // If the current access level is package-private (no modifier), add the new modifier
            else if (currentMethodAccessLevel == null) {
                J.Modifier mod = new J.Modifier(Tree.randomId(), Space.build(" ", emptyList()), Markers.EMPTY, null, newAccessLevel, Collections.emptyList());
                m = m.withModifiers(ListUtils.concat(mod, m.getModifiers()));
                if (method.getModifiers().isEmpty()) {
                    J.TypeParameters typeParams = m.getPadding().getTypeParameters();
                    if (typeParams == null) {
                        TypeTree returnExpr = m.getReturnTypeExpression();
                        if (returnExpr == null) {
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
                } else {
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

            // If the target access level is package-private (no modifier), remove the current access level modifier
            // and copy any associated comments
            else if (newAccessLevel == null) {
                AtomicReference<@Nullable Space> spaceOfRemovedModifier = new AtomicReference<>();
                AtomicReference<List<J.Annotation>> annotationsOfRemovedModifier = new AtomicReference<>();

                List<J.Modifier> modifiers = ListUtils.map(m.getModifiers(), mod -> {
                    if (mod.getType() == currentMethodAccessLevel) {
                        if (!mod.getPrefix().isEmpty() || !mod.getPrefix().getComments().isEmpty()) {
                            spaceOfRemovedModifier.set(mod.getPrefix());
                            annotationsOfRemovedModifier.set(mod.getAnnotations());
                        }
                        return null;
                    }

                    // copy access level modifier comment to the next modifier if it exists
                    Space prefix = spaceOfRemovedModifier.get();
                    if (prefix != null) {
                        List<Comment> comments = concatAll(prefix.getComments(), mod.getComments());
                        mod = mod.withPrefix(Space.build(prefix.getWhitespace(), comments));
                        spaceOfRemovedModifier.set(null);
                    }
                    return mod;
                });

                // if no following modifier exists, add comments to the method itself
                Space prefix = spaceOfRemovedModifier.get();
                if (prefix == null ) {
                    // GOAL: fix the one-off white-space

                    /*
                     if no modifier and annotations:
                       hasLineBreak(m.getPrefix().getWhitespace())
                     if !modifiers.isEmpty()
                       takeLastLElement and check: hasLineBreak(<last-modifer>.getWhitespace())
                     if !annotation.isEmpty()
                       takeLastLElement and check: hasLineBreak(<last-ann>.getWhitespace())
                     */


                    /*if (hasLineBreak(m.getPrefix().getWhitespace())) {
                        System.out.println("-----");
                        if (m.isConstructor() && !hasLineBreak(m.getName().getPrefix().getWhitespace())) {
                            System.out.println(m.getName().getComments());
                            prefix = Space.EMPTY;
                        } else if (!hasLineBreak(m.getReturnTypeExpression().getPrefix().getWhitespace())) {
                            System.out.println(m.getReturnTypeExpression().getComments());
                            prefix = Space.EMPTY;
                        }
                    }*/
                }

                if (prefix != null) {
                    if (m.isConstructor()) {
                        List<Comment> comments = concatAll(prefix.getComments(), m.getName().getComments());
                        m = m.withName(m.getName().withPrefix(Space.build(prefix.getWhitespace(), comments)));
                    } else {
                        TypeTree returnTypeExpression = m.getReturnTypeExpression();
                        List<Comment> comments = concatAll(prefix.getComments(), returnTypeExpression.getPrefix().getComments());
                        System.out.println("Method: " + m);
                        System.out.println("<" + returnTypeExpression.getPrefix().getWhitespace() + ">");
                        m = m.withReturnTypeExpression(returnTypeExpression.withPrefix(Space.build(prefix.getWhitespace(), comments)));
                    }

                    AtomicReference<@Nullable String> annWhitespace = new AtomicReference<>();
                    m = m.withLeadingAnnotations(ListUtils.concatAll(
                            m.getLeadingAnnotations(),
                            ListUtils.mapFirst(annotationsOfRemovedModifier.get(), it -> {
                                annWhitespace.set(it.getPrefix().getWhitespace());
                                return it.withPrefix(it.getPrefix().withWhitespace(""));
                            }))
                    );
                    /*if (!annotationsOfRemovedModifier.get().isEmpty()) {
                        modifiers = ListUtils.mapFirst(modifiers, it -> it.withPrefix(it.getPrefix().withWhitespace(annWhitespace.get() + it.getPrefix().getWhitespace())));
                    }*/
                }
                /*} else {
                    if (m.isConstructor()) {
                    } else {
                        TypeTree returnTypeExpression = m.getReturnTypeExpression();
                        System.out.println("Method: " + m);
                        System.out.println("<" +  returnTypeExpression.getPrefix().getWhitespace() + ">");
                        //m = m.withReturnTypeExpression(returnTypeExpression.withPrefix(Space.build(prefix.getWhitespace(), comments)));
                    }
                }*/
                m = m.withModifiers(modifiers);
                //m = maybeAutoFormat(m, m.withModifiers(modifiers), p).withBody(m.getBody());
            }
        }
        return m;
    }
}
