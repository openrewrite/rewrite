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
package org.openrewrite.java.internal.template;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.format.AutoFormatVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.openrewrite.java.tree.JavaCoordinates.Mode.INSERTION;

/**
 * This visitor will insert the generated elements into the correct location within an AST and return the mutated
 * version.
 */
public class InsertAtCoordinates extends JavaVisitor<List<? extends J>> {
    private static final J.Block EMPTY_BLOCK = new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
            new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY), Collections.emptyList(), Space.EMPTY);

    private final UUID insertId;
    private final Space.Location location;
    private final JavaCoordinates coordinates;

    public InsertAtCoordinates(JavaCoordinates coordinates) {
        this.coordinates = coordinates;
        this.insertId = coordinates.getTree().getId();
        this.location = coordinates.getSpaceLocation();
    }

    @Nullable
    @Override
    public J preVisit(@Nullable J tree, List<? extends J> generated) {
        if (tree == null || !coordinates.isReplaceWholeCursorValue() || !tree.getId().equals(insertId)) {
            return tree;
        }
        // Handles all cases where there is a replace on the current element.
        if (generated.size() == 1) {
            //noinspection ConstantConditions
            return new AutoFormatVisitor<>().visit(generated.get(0), 0, getCursor()).withPrefix(tree.getPrefix());
        } else {
            throw new IllegalStateException("The template generated the incorrect number of elements.");
        }
    }

    @Override
    public J visitBlock(J.Block block, List<? extends J> generated) {
        J.Block b = visitAndCast(block, generated, super::visitBlock);

        if (b.getId().equals(insertId) && location == Space.Location.BLOCK_END) {
            AutoFormatVisitor<Integer> autoFormat = new AutoFormatVisitor<>();
            for (J j : generated) {
                if (!(j instanceof Statement)) {
                    throw new IllegalStateException("Attempted to insert a tree of type " + j.getClass().getSimpleName() + " as a block statement");
                }
            }
            List<Statement> formatted = generated.stream()
                    .map(it -> autoFormat.visit(it, 0, getCursor()))
                    .map(Statement.class::cast)
                    .collect(Collectors.toList());

            return b.withStatements(ListUtils.concatAll(b.getStatements(), formatted));
        } else if (b.getStatements().stream().anyMatch(s -> insertId.equals(s.getId()))) {
            AutoFormatVisitor<Integer> autoFormat = new AutoFormatVisitor<>();
            List<Statement> formatted = generated.stream()
                    .map(it -> autoFormat.visit(it, 0, getCursor()))
                    .map(Statement.class::cast)
                    .collect(Collectors.toList());

            //noinspection ConstantConditions
            b = b.withStatements(maybeMergeList(b.getStatements(), formatted));
        }
        return b;
    }

    @Override
    @SuppressWarnings("unchecked")
    public J visitClassDeclaration(J.ClassDeclaration classDeclaration, List<? extends J> generated) {
        J.ClassDeclaration c = visitAndCast(classDeclaration, generated, super::visitClassDeclaration);
        if (insertId.equals(c.getId())) {
            AutoFormatVisitor<Integer> autoFormat = new AutoFormatVisitor<>();
            switch (location) {
                case ANNOTATIONS: {
                    J.ClassDeclaration temp;
                    if (INSERTION.equals(coordinates.getMode())) {
                        temp = c.withLeadingAnnotations(ListUtils.insertInOrder(c.getLeadingAnnotations(), (J.Annotation) generated.get(0),
                                coordinates.getComparator()));
                    } else {
                        temp = c.withLeadingAnnotations((List<J.Annotation>) generated);
                    }
                    //If the annotations have changed, reformat the class declaration (so we can get properly formatted
                    //annotations). The class declaration is reformatted (minus the body) and we must format
                    //relative to the first enclosing J.Block or the J.CompilationUnit
                    temp = (J.ClassDeclaration) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getFormattingParent(getCursor()));
                    assert temp != null;
                    c = temp.withBody(c.getBody());
                    break;
                }
                case TYPE_PARAMETERS: {
                    J.ClassDeclaration temp = c.withTypeParameters((List<J.TypeParameter>) generated);
                    temp = (J.ClassDeclaration) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    c = c.getPadding().withTypeParameters(temp.getPadding().getTypeParameters());
                    break;
                }
                case EXTENDS: {
                    J.ClassDeclaration temp = c.withExtends((TypeTree) generated.get(0));
                    temp = (J.ClassDeclaration) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    c = c.getPadding().withExtends(temp.getPadding().getExtends());
                    break;
                }
                case IMPLEMENTS: {
                    J.ClassDeclaration temp = c.withImplements((List<TypeTree>) generated);
                    temp = (J.ClassDeclaration) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    c = c.getPadding().withImplements(temp.getPadding().getImplements());
                    break;
                }
                case BLOCK_PREFIX:
                    c = c.withBody((J.Block) generated.get(0));
                    break;
            }
        } else {
            J.ClassDeclaration temp = c.withLeadingAnnotations(maybeMergeList(c.getLeadingAnnotations(), generated));
            if (temp != c) {
                //If the annotations have changed, reformat the class declaration (so we can get properly formatted
                //annotations). The class declaration is reformatted (minus the body) and we must format
                //relative to the first enclosing J.Block or the J.CompilationUnit
                temp = (J.ClassDeclaration) new AutoFormatVisitor<>().visit(temp.withBody(EMPTY_BLOCK), 0, getFormattingParent(getCursor()));
                assert temp != null;
                return temp.withBody(c.getBody());
            }
            temp = c.withTypeParameters(maybeMergeList(c.getTypeParameters(), generated));
            if (temp != c) {
                //If the type parameters have changed, apply formatting to the container.
                temp = (J.ClassDeclaration) new AutoFormatVisitor<>().visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                assert temp != null;
                return c.getPadding().withTypeParameters(temp.getPadding().getTypeParameters());
            }

            temp = c.withImplements(maybeMergeList(c.getImplements(), generated));
            if (temp != c) {
                //If the implements clause has changed, apply formatting to the container.
                temp = (J.ClassDeclaration) new AutoFormatVisitor<>().visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                assert temp != null;
                return c.getPadding().withImplements(temp.getPadding().getImplements());
            }
        }
        return c;
    }

    @Override
    @SuppressWarnings("unchecked")
    public J visitMethodDeclaration(J.MethodDeclaration method, List<? extends J> generated) {
        J.MethodDeclaration m = visitAndCast(method, generated, super::visitMethodDeclaration);
        AutoFormatVisitor<Integer> autoFormat = new AutoFormatVisitor<>();
        if (insertId.equals(m.getId())) {
            switch (location) {
                case ANNOTATIONS: {
                    J.MethodDeclaration temp;
                    if (INSERTION.equals(coordinates.getMode())) {
                        temp = m.withLeadingAnnotations(ListUtils.insertInOrder(m.getLeadingAnnotations(), (J.Annotation) generated.get(0),
                                coordinates.getComparator()));
                    } else {
                        temp = m.withLeadingAnnotations((List<J.Annotation>) generated);
                    }
                    //If the annotations have changed, reformat the method declaration (so we can get properly formatted
                    //annotations). The entire method declaration is reformatted (minus the body) and we must format
                    //relative to the first enclosing J.Block.
                    temp = (J.MethodDeclaration) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0,
                            getFormattingParent(getCursor()));
                    assert temp != null;
                    m = temp.withBody(m.getBody());
                    break;
                }
                case TYPE_PARAMETERS: {
                    J.MethodDeclaration temp = m.withTypeParameters((List<J.TypeParameter>) generated);
                    temp = (J.MethodDeclaration) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    m = m.withTypeParameters(temp.getTypeParameters());
                    break;
                }
                case METHOD_DECLARATION_PARAMETERS: {
                    J.MethodDeclaration temp = m.withParameters((List<Statement>) generated);
                    temp = (J.MethodDeclaration) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    m = m.getPadding().withParameters(temp.getPadding().getParameters());
                    break;
                }
                case THROWS: {
                    J.MethodDeclaration temp = m.withThrows((List<NameTree>) generated);
                    temp = (J.MethodDeclaration) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    m = m.getPadding().withThrows(temp.getPadding().getThrows());
                    break;
                }
                case BLOCK_PREFIX:
                    m = m.withBody((J.Block) generated.get(0));
                    break;
            }
        } else {
            J.MethodDeclaration temp = m.withLeadingAnnotations(maybeMergeList(m.getLeadingAnnotations(), generated));
            if (temp != m) {
                //If the annotations have changed, reformat the method declaration (so we can get properly formatted
                //annotations). The entire method declaration is reformatted (minus the body) and we must format
                //relative to the first enclosing J.Block.
                temp = (J.MethodDeclaration) new AutoFormatVisitor<>().visit(m.withBody(EMPTY_BLOCK), 0,
                        getFormattingParent(getCursor()));
                assert temp != null;
                return temp.withBody(m.getBody());

            }

            temp = m.withTypeParameters(maybeMergeList(m.getTypeParameters(), generated));
            if (temp != m) {
                //Auto-format the type parameters if they have been changed.
                temp = (J.MethodDeclaration) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                assert temp != null;
                return m.getAnnotations().withTypeParameters(temp.getAnnotations().getTypeParameters());
            }

            temp = m.withParameters(maybeMergeList(m.getParameters(), generated));
            if (temp != m) {
                //Auto-format the parameters if they have been changed.
                temp = (J.MethodDeclaration) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                assert temp != null;
                return m.getPadding().withParameters(temp.getPadding().getParameters());
            }

            temp = m.withThrows(maybeMergeList(m.getThrows(), generated));
            if (temp != m) {
                //Auto-format the throws clause if a change has been made.
                temp = (J.MethodDeclaration) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                assert temp != null;
                return m.getPadding().withThrows(temp.getPadding().getThrows());
            }
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, List<? extends J> generated) {
        J.VariableDeclarations m = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, generated);
        AutoFormatVisitor<Integer> autoFormat = new AutoFormatVisitor<>();
        if (insertId.equals(m.getId())) {
            if (location == Space.Location.ANNOTATIONS) {
                if (INSERTION.equals(coordinates.getMode())) {
                    m = m.withLeadingAnnotations(ListUtils.insertInOrder(m.getLeadingAnnotations(), (J.Annotation) generated.get(0),
                            coordinates.getComparator()));
                } else {
                    m = m.withLeadingAnnotations((List<J.Annotation>) generated);
                }
                //If the annotations have changed, reformat the variable declaration (so we can get properly formatted
                //annotations). We must format relative to the first enclosing J.Block.
                m = (J.VariableDeclarations) autoFormat.visit(m, 0, getFormattingParent(getCursor()));
            }
        } else {
            J.VariableDeclarations temp = m.withLeadingAnnotations(maybeMergeList(m.getLeadingAnnotations(), generated));
            if (temp != m) {
                //If the annotations have changed, reformat the variable declaration (so we can get properly formatted
                //annotations). We must format relative to the first enclosing J.Block.
                m = (J.VariableDeclarations) autoFormat.visit(m, 0, getFormattingParent(getCursor()));
            }
        }
        assert m != null;
        return m;
    }

    @Override
    @SuppressWarnings("unchecked")
    public J visitMethodInvocation(J.MethodInvocation method, List<? extends J> generated) {
        J.MethodInvocation m = visitAndCast(method, generated, super::visitMethodInvocation);
        J.MethodInvocation temp = m;
        if (insertId.equals(m.getId()) && location == Space.Location.METHOD_INVOCATION_ARGUMENTS) {
            temp = m.withArguments((List<Expression>) generated);
        } else if (!coordinates.isReplacement()) {
            //noinspection ConstantConditions
            temp = m.withArguments(maybeMergeList(m.getArguments(), generated));
        }

        if (temp != m) {
            //If the arguments have changed, reformat those arguments.
            temp = (J.MethodInvocation) new AutoFormatVisitor<>().visit(temp, 0, getCursor());
            assert temp != null;
            m = m.getPadding().withArguments(temp.getPadding().getArguments());
        }
        return m;
    }

    /**
     * Annotations typically have a method declaration, class declaration, or variable declaration as their parent, for
     * the sake of formatting, the template will find the parent cursor for the first enclosing block or, if the
     * annotation is on the top level class, the Compilation unit.
     *
     * @return A parent cursor that can be used for formatting purposes.
     */
    private Cursor getFormattingParent(Cursor originalCursor) {
        return originalCursor.dropParentUntil(v -> v instanceof J.Block || v instanceof J.CompilationUnit);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T extends J> List<T> maybeMergeList(@Nullable List<T> originalList, List<? extends J> generated) {
        if (originalList != null) {
            for (int index = 0; index < originalList.size(); index++) {
                if (insertId.equals(originalList.get(index).getId())) {
                    List<T> newList = new ArrayList<>();
                    if (coordinates.isReplacement()) {
                        newList.addAll(originalList.subList(0, index + 1));
                        newList.addAll((List<T>) generated);
                        newList.addAll(originalList.subList(index + 1, originalList.size()));
                    } else {
                        newList.addAll(originalList.subList(0, index));
                        newList.addAll((List<T>) generated);
                        newList.addAll(originalList.subList(index, originalList.size()));
                    }
                    return newList;
                }
            }
        }
        return originalList;
    }
}
