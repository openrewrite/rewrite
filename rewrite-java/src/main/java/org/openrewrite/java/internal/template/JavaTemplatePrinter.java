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
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;

public class JavaTemplatePrinter extends JavaPrinter<Cursor> {
    private static final J.Block EMPTY_BLOCK = new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
            new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY), Collections.emptyList(), Space.EMPTY);

    static final String SNIPPET_MARKER_START = "<<<<START>>>>";
    static final String SNIPPET_MARKER_END = "<<<<END>>>>";

    private final JavaCoordinates coordinates;
    private final Tree changing;
    private final Set<String> imports;
    private final String code;


    public JavaTemplatePrinter(String code, Tree changing, JavaCoordinates coordinates, Set<String> imports) {
        super(TreePrinter.identity());
        this.code = "/*" + SNIPPET_MARKER_START + "*/" + code + "/*" + SNIPPET_MARKER_END + "*/";
        this.coordinates = coordinates;
        this.changing = changing;
        this.imports = imports;
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, Cursor insertionScope) {
        if (coordinates.isReplaceWholeCursorValue() && tree != null && tree.getId().equals(coordinates.getTree().getId())) {
            printTemplate();
            return (J) tree;
        } else if (tree != null && tree.getId().equals(changing.getId())) {
            //Once the Id of the tree matches the ID of possible mutated tree navigation ,for the sake of printing the
            //synthetic class, swaps to the "changing" class.
            return super.visit(changing, insertionScope);
        } else {
            return super.visit(tree, insertionScope);
        }
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, Cursor insertionScope) {
        visitSpace(cu.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, insertionScope);
        visitRightPadded(cu.getPadding().getPackageDeclaration(), JRightPadded.Location.PACKAGE, ";", insertionScope);

        if (!imports.isEmpty()) {
            getPrinter().append("\n\n");
            for (String impoort : imports) {
                getPrinter().append(impoort);
            }
        }
        visitRightPadded(cu.getPadding().getImports(), JRightPadded.Location.IMPORT, ";", insertionScope);
        StringBuilder acc = getPrinter();
        if (!cu.getImports().isEmpty()) {
            acc.append(";");
        }
        visit(cu.getClasses(), insertionScope);
        visitSpace(cu.getEof(), Space.Location.COMPILATION_UNIT_EOF, insertionScope);
        return cu;
    }

    @Override
    public J visitBlock(J.Block block, Cursor insertionScope) {
        Cursor parent = getCursor().dropParentUntil(J.class::isInstance);
        if (!insertionScope.isScopeInPath(block) && !(parent.getValue() instanceof J.ClassDeclaration)) {
            J.Block b = block.withStatements(emptyList());
            return super.visitBlock(b, insertionScope);
        }

        if (coordinates.getTree().getId().equals(block.getId()) && Space.Location.BLOCK_PREFIX.equals(coordinates.getSpaceLocation())) {
            J.Block b = block.withStatements(emptyList());
            return super.visitBlock(b, insertionScope);
        }

        J.Block b = block;
        if (!(parent.getValue() instanceof J.ClassDeclaration)) {

            if (b.getStatements().stream().anyMatch(insertionScope::isScopeInPath)) {
                // If a statement in the block is in insertion scope, then this will render each statement
                // up to the statement that is in insertion scope.
                List<Statement> statementsInScope = new ArrayList<>();
                for (Statement statement : b.getStatements()) {
                    statementsInScope.add(statement);
                    if (insertionScope.isScopeInPath(statement)) {
                        break;
                    }
                }
                b = b.withStatements(statementsInScope);
            }
        }

        return super.visitBlock(b, insertionScope);
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, Cursor cursor) {
        J j = getCursor().firstEnclosing(J.class);
        if (loc == coordinates.getSpaceLocation() && j != null && coordinates.getTree().getId().equals(j.getId())) {
            printTemplate();
        }
        return super.visitSpace(space, loc, cursor);
    }

    @Override
    public J visitClassDeclaration(J.ClassDeclaration classDecl, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(classDecl)) {
            return super.visitClassDeclaration(classDecl.withLeadingAnnotations(emptyList()), insertionScope);
        } else if  (!classDecl.getId().equals(coordinates.getTree().getId())) {
            return super.visitClassDeclaration(classDecl, insertionScope);
        }

        String kind = "";
        switch (classDecl.getKind()) {
            case Class:
                kind = "class";
                break;
            case Enum:
                kind = "enum";
                break;
            case Interface:
                kind = "interface";
                break;
            case Annotation:
                kind = "@interface";
                break;
        }

        visitSpace(classDecl.getPrefix(), Space.Location.CLASS_DECLARATION_PREFIX, insertionScope);

        if (Space.Location.ANNOTATIONS.equals(coordinates.getSpaceLocation())) {
            if (coordinates.isReplacement()) {
                printTemplate();
            }
            else {
                printTemplate();
                visit(classDecl.getLeadingAnnotations(), insertionScope);
            }
        } else {
            visit(classDecl.getLeadingAnnotations(), insertionScope);
        }

        visitModifiers(classDecl.getModifiers(), insertionScope);
        visitSpace(classDecl.getAnnotations().getKind().getPrefix(), Space.Location.CLASS_KIND, insertionScope);
        StringBuilder acc = getPrinter();
        acc.append(kind);
        visit(classDecl.getName(), insertionScope);

        if (coordinates.isReplacement() && Space.Location.TYPE_PARAMETERS.equals(coordinates.getSpaceLocation())) {
            printTemplate();
        } else {
            visitContainer("<", classDecl.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", insertionScope);
        }

        if (coordinates.isReplacement() && Space.Location.EXTENDS.equals(coordinates.getSpaceLocation())) {
            printTemplate();
        } else {
            visitLeftPadded("extends", classDecl.getPadding().getExtends(), JLeftPadded.Location.EXTENDS, insertionScope);
        }

        if (coordinates.isReplacement() && Space.Location.IMPLEMENTS.equals(coordinates.getSpaceLocation())) {
            printTemplate();
        } else {
            visitContainer(classDecl.getKind().equals(J.ClassDeclaration.Kind.Type.Interface) ? "extends" : "implements",
                    classDecl.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, ",", null, insertionScope);
        }

        if (coordinates.isReplacement() && Space.Location.BLOCK_PREFIX.equals(coordinates.getSpaceLocation())) {
            printTemplate();
        } else {
            visit(classDecl.getBody(), insertionScope);
        }

        return classDecl;
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(method)) {
            return super.visitMethodDeclaration(method.withLeadingAnnotations(emptyList()).withBody(EMPTY_BLOCK), insertionScope);
        }

        visitSpace(method.getPrefix(), Space.Location.METHOD_DECLARATION_PREFIX, insertionScope);

        if (Space.Location.ANNOTATIONS.equals(coordinates.getSpaceLocation())) {
            if (coordinates.isReplacement()) {
                printTemplate();
            }
            else {
                printTemplate();
                visit(method.getLeadingAnnotations(), insertionScope);
            }
        } else {
            visit(method.getLeadingAnnotations(), insertionScope);
        }

        visitModifiers(method.getModifiers(), insertionScope);

        if (coordinates.isReplacement() && Space.Location.TYPE_PARAMETERS.equals(coordinates.getSpaceLocation())) {
            printTemplate();
        } else {
            J.TypeParameters typeParameters = method.getAnnotations().getTypeParameters();
            if (typeParameters != null) {
                visit(typeParameters.getAnnotations(), insertionScope);
                visitSpace(typeParameters.getPrefix(), Space.Location.TYPE_PARAMETERS, insertionScope);
                StringBuilder acc = getPrinter();
                acc.append("<");
                visitRightPadded(typeParameters.getPadding().getTypeParameters(), JRightPadded.Location.TYPE_PARAMETER, ",", insertionScope);
                acc.append(">");
            }
        }

        visit(method.getReturnTypeExpression(), insertionScope);
        visit(method.getName(), insertionScope);

        if (coordinates.isReplacement() && Space.Location.METHOD_DECLARATION_PARAMETERS.equals(coordinates.getSpaceLocation())) {
            printTemplate();
        } else {
            visitContainer("(", method.getPadding().getParameters(), JContainer.Location.METHOD_DECLARATION_PARAMETERS, ",", ")", insertionScope);
        }

        if (coordinates.isReplacement() && Space.Location.THROWS.equals(coordinates.getSpaceLocation())) {
            printTemplate();
        } else {
            visitContainer("throws", method.getPadding().getThrows(), JContainer.Location.THROWS, ",", null, insertionScope);
        }

        if (coordinates.isReplacement() && Space.Location.BLOCK_PREFIX.equals(coordinates.getSpaceLocation())) {
            printTemplate();
        } else {
            visit(method.getBody(), insertionScope);
        }

        visitLeftPadded("default", method.getPadding().getDefaultValue(), JLeftPadded.Location.METHOD_DECLARATION_DEFAULT_VALUE, insertionScope);

        return method;
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(method)) {
            // Variables in the original AST only need to be declared, nulls out the initializers.
            return super.visitMethodInvocation(method, insertionScope);
        }

        visitSpace(method.getPrefix(), Space.Location.METHOD_INVOCATION_PREFIX, insertionScope);
        visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, ".", insertionScope);

        if (coordinates.isReplacement() && Space.Location.TYPE_PARAMETERS.equals(coordinates.getSpaceLocation())) {
            printTemplate();
        } else {
            visitContainer("<", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", insertionScope);
        }
        visit(method.getName(), insertionScope);
        if (coordinates.isReplacement() && Space.Location.METHOD_INVOCATION_ARGUMENTS.equals(coordinates.getSpaceLocation())) {
            printTemplate();
        } else {
            visitContainer("(", method.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, ",", ")", insertionScope);
        }
        return method;
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(multiVariable)) {
            return super.visitVariableDeclarations(multiVariable.withLeadingAnnotations(emptyList()), insertionScope);
        } else if (!multiVariable.getId().equals(coordinates.getTree().getId())) {
            return super.visitVariableDeclarations(multiVariable.withLeadingAnnotations(emptyList()), insertionScope);
        }

        StringBuilder acc = getPrinter();
        visitSpace(multiVariable.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, insertionScope);

        if (Space.Location.ANNOTATIONS.equals(coordinates.getSpaceLocation())) {
            if (coordinates.isReplacement()) {
                printTemplate();
            }
            else {
                printTemplate();
                visit(multiVariable.getLeadingAnnotations(), insertionScope);
            }
        } else {
            visit(multiVariable.getLeadingAnnotations(), insertionScope);
        }

        visitModifiers(multiVariable.getModifiers(), insertionScope);
        visit(multiVariable.getTypeExpression(), insertionScope);
        for (JLeftPadded<Space> dim : multiVariable.getDimensionsBeforeName()) {
            visitSpace(dim.getBefore(), Space.Location.DIMENSION_PREFIX, insertionScope);
            acc.append('[');
            visitSpace(dim.getElement(), Space.Location.DIMENSION, insertionScope);
            acc.append(']');
        }
        if (multiVariable.getVarargs() != null) {
            visitSpace(multiVariable.getVarargs(), Space.Location.VARARGS, insertionScope);
            acc.append("...");
        }
        visitRightPadded(multiVariable.getPadding().getVariables(), JRightPadded.Location.NAMED_VARIABLE, ",", insertionScope);
        return multiVariable;
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(variable)) {
            // Variables in the original AST only need to be declared, nulls out the initializers.
            return (J.VariableDeclarations.NamedVariable) super.visitVariable(variable.withInitializer(null), insertionScope);
        }
        return (J.VariableDeclarations.NamedVariable) super.visitVariable(variable, insertionScope);
    }


    /**
     * This method will extends the insertion scope cursor by starting at the parent cursor and then walking into the
     * possibly mutated tree until the coordinates are found.
     *
     * @param parentScope The parent scope is root from the original AST
     * @param changing    The possibly mutated tree
     * @param coordinates The coordinates to search for
     * @return A cursor representing the path from the original compilation unit to the coordinates element in the mutated tree
     */
    public static Cursor findCoordinateCursor(Cursor parentScope, Tree changing, JavaCoordinates coordinates) {
        AtomicReference<Cursor> cursorReference = new AtomicReference<>(parentScope);
        new ExtractInsertionCursor(coordinates, parentScope).visit(parentScope.firstEnclosingOrThrow(J.class), cursorReference);
        return cursorReference.get();
    }

    private void printTemplate() {
        getPrinter().append(code);
    }

    private static class ExtractInsertionCursor extends JavaVisitor<AtomicReference<Cursor>> {
        private final UUID insertionId;

        private ExtractInsertionCursor(JavaCoordinates coordinates, Cursor parent) {
            insertionId = coordinates.getTree().getId();
            setCursor(parent);
        }

        @Override
        public @Nullable J visit(@Nullable Tree tree, AtomicReference<Cursor> cursorReference) {
            if (tree != null && tree.getId().equals(insertionId)) {
                cursorReference.set(getCursor());
            }
            return super.visit(tree, cursorReference);
        }
    }
}
