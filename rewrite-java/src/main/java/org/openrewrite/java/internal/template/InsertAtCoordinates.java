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

/**
 * This visitor will insert the generated elements into the correct location within an AST and return the mutated
 * version.
 */
public class InsertAtCoordinates extends JavaVisitor<List<? extends J>> {
    private static final J.Block EMPTY_BLOCK = new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
            new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY), Collections.emptyList(), Space.EMPTY);

    private final UUID insertId;
    private final Space.Location location;

    public InsertAtCoordinates(JavaCoordinates<?> coordinates) {
        this.insertId = coordinates.getTree().getId();
        this.location = coordinates.getSpaceLocation();
        setCursoringOn();
    }

    @Nullable
    @Override
    public J preVisit(@Nullable J tree, List<? extends J> generated) {
        if (tree == null || location != Space.Location.REPLACE || !tree.getId().equals(insertId)) {
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
            for (J j : generated) {
                if (!(j instanceof Statement)) {
                    throw new IllegalStateException("Attempted to insert a tree of type " + j.getClass().getSimpleName() + " as a block statement");
                }
            }
            //noinspection unchecked
            return b.withStatements(ListUtils.concatAll(b.getStatements(), (List<Statement>) generated));
        }
        //noinspection ConstantConditions
        b = b.withStatements(maybeMergeList(b.getStatements(), generated));
        return b;
    }

    @Override
    @SuppressWarnings("unchecked")
    public J visitClassDecl(J.ClassDecl classDeclaration, List<? extends J> generated) {
        J.ClassDecl c = visitAndCast(classDeclaration, generated, super::visitClassDecl);
        if (insertId.equals(c.getId())) {
            AutoFormatVisitor<Integer> autoFormat = new AutoFormatVisitor<>();
            switch (location) {
                case ANNOTATION_PREFIX: {
                    J.ClassDecl temp = c.withAnnotations((List<J.Annotation>) generated);
                    temp = (J.ClassDecl) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    c = temp.withBody(c.getBody());
                    break;
                }
                case TYPE_PARAMETERS: {
                    J.ClassDecl temp = c.withTypeParameters((List<J.TypeParameter>) generated);
                    temp = (J.ClassDecl) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    c = c.getPadding().withTypeParameters(temp.getPadding().getTypeParameters());
                    break;
                }
                case EXTENDS: {
                    J.ClassDecl temp = c.withExtends((TypeTree) generated.get(0));
                    temp = (J.ClassDecl) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    c = c.getPadding().withExtends(temp.getPadding().getExtends());
                    break;
                }
                case IMPLEMENTS: {
                    J.ClassDecl temp = c.withImplements((List<TypeTree>) generated);
                    temp = (J.ClassDecl) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    c = c.getPadding().withImplements(temp.getPadding().getImplements());
                    break;
                }
                case BLOCK_PREFIX:
                    c = c.withBody((J.Block) generated.get(0));
                    break;
            }
        } else {
            c = c.withAnnotations(maybeMergeList(c.getAnnotations(), generated));
            c = c.withTypeParameters(maybeMergeList(c.getTypeParameters(), generated));
            c = c.withImplements(maybeMergeList(c.getImplements(), generated));
        }
        return c;
    }

    @Override
    @SuppressWarnings("unchecked")
    public J visitMethod(J.MethodDecl method, List<? extends J> generated) {
        J.MethodDecl m = visitAndCast(method, generated, super::visitMethod);
        AutoFormatVisitor<Integer> autoFormat = new AutoFormatVisitor<>();
        if (insertId.equals(m.getId())) {
            switch (location) {
                case ANNOTATION_PREFIX: {
                    J.MethodDecl temp = m.withAnnotations((List<J.Annotation>) generated);
                    temp = (J.MethodDecl) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    m = temp.withBody(m.getBody());
                    break;
                }
                case TYPE_PARAMETERS: {
                    J.MethodDecl temp = m.withTypeParameters((List<J.TypeParameter>) generated);
                    temp = (J.MethodDecl) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    m = m.getPadding().withTypeParameters(temp.getPadding().getTypeParameters());
                    break;
                }
                case METHOD_DECL_PARAMETERS: {
                    J.MethodDecl temp = m.withParams((List<Statement>) generated);
                    temp = (J.MethodDecl) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    m = m.getPadding().withParams(temp.getPadding().getParams());
                    break;
                }
                case THROWS: {
                    J.MethodDecl temp = m.withThrows((List<NameTree>) generated);
                    temp = (J.MethodDecl) autoFormat.visit(temp.withBody(EMPTY_BLOCK), 0, getCursor());
                    assert temp != null;
                    m = m.getPadding().withThrows(temp.getPadding().getThrows());
                    break;
                }
                case BLOCK_PREFIX:
                    m = m.withBody((J.Block) generated.get(0));
                    break;
            }
        } else {
            m = m.withAnnotations(maybeMergeList(m.getAnnotations(), generated));
            m = m.withTypeParameters(maybeMergeList(m.getTypeParameters(), generated));
            m = m.withThrows(maybeMergeList(m.getThrows(), generated));
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    @Override
    public J visitMultiVariable(J.VariableDecls multiVariable, List<? extends J> generated) {
        J.VariableDecls m = (J.VariableDecls) super.visitMultiVariable(multiVariable, generated);
        AutoFormatVisitor<Integer> autoFormat = new AutoFormatVisitor<>();
        if (insertId.equals(m.getId())) {
            if (location == Space.Location.ANNOTATION_PREFIX) {
                m = m.withAnnotations((List<J.Annotation>) generated);
                m = (J.VariableDecls) autoFormat.visit(m, 0, getCursor());
                assert m != null;
            }
        } else {
            m = m.withAnnotations(maybeMergeList(m.getAnnotations(), generated));
        }
        return m;
    }

    @Override
    @SuppressWarnings("unchecked")
    public J visitMethodInvocation(J.MethodInvocation method, List<? extends J> generated) {
        J.MethodInvocation m = visitAndCast(method, generated, super::visitMethodInvocation);
        if (insertId.equals(m.getId()) && location == Space.Location.METHOD_INVOCATION_ARGUMENTS) {
            m = m.withArgs((List<Expression>) generated);
        } else {
            //noinspection ConstantConditions
            m = m.withArgs(maybeMergeList(m.getArgs(), generated));
        }

        return m;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T extends J> List<T> maybeMergeList(@Nullable List<T> originalList, List<? extends J> generated) {
        if (originalList != null) {
            for (int index = 0; index < originalList.size(); index++) {
                if (insertId.equals(originalList.get(index).getId())) {
                    List<T> newList = new ArrayList<>();
                    if (location == Space.Location.REPLACE) {
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
