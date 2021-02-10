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
import org.openrewrite.java.AddImport;
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
        setCursoringOn();
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, Cursor insertionScope) {

        if (coordinates.getSpaceLocation() == Space.Location.REPLACE && tree != null && tree.getId().equals(coordinates.getTree().getId())) {
            getPrinter().append(code);
            return (J) tree;
        } else if (tree != null &&tree.getId().equals(changing.getId())) {
            //Once the Id of the tree matches the ID of possible mutated tree, navigation
            //for the sake of printing the synthetic class uses the "changing" class.
            return super.visit(changing, insertionScope);
        } else {
            return super.visit(tree, insertionScope);
        }
    }


    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, Cursor insertionScope) {
        for (String impoort : imports) {
            doAfterVisit(new AddImport<>(impoort, null, false));
        }
        return super.visitCompilationUnit(cu, insertionScope);
    }

    @Override
    public J visitBlock(J.Block block, Cursor insertionScope) {
        Cursor parent = getCursor().dropParentUntil(J.class::isInstance);
        if (!insertionScope.isScopeInPath(block) && !(parent.getValue() instanceof J.ClassDeclaration)) {
            J.Block b = block.withStatements(emptyList());
            return super.visitBlock(b, insertionScope);
        }

        if(coordinates.getTree().getId().equals(block.getId()) && coordinates.getSpaceLocation().equals(Space.Location.BLOCK_PREFIX)) {
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
            getPrinter().append(code);
        }
        return super.visitSpace(space, loc, cursor);
    }

    @Override
    public J visitClassDeclaration(J.ClassDeclaration classDecl, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(classDecl) || !classDecl.getId().equals(coordinates.getTree().getId())) {
            return super.visitClassDeclaration(classDecl.withAnnotations(emptyList()), insertionScope);
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

        if (coordinates.getSpaceLocation().equals(Space.Location.ANNOTATION_PREFIX)) {
            getPrinter().append(code);
        } else {
            visit(classDecl.getAnnotations(), insertionScope);
        }

        visitModifiers(classDecl.getModifiers(), insertionScope);
        visitSpace(classDecl.getPadding().getKind().getBefore(), Space.Location.CLASS_KIND, insertionScope);
        StringBuilder acc = getPrinter();
        acc.append(kind);
        visit(classDecl.getName(), insertionScope);

        if (coordinates.getSpaceLocation().equals(Space.Location.TYPE_PARAMETERS)) {
            getPrinter().append(code);
        } else {
            visitContainer("<", classDecl.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", insertionScope);
        }

        if (coordinates.getSpaceLocation().equals(Space.Location.EXTENDS)) {
            getPrinter().append(code);
        } else {
            visitLeftPadded("extends", classDecl.getPadding().getExtends(), JLeftPadded.Location.EXTENDS, insertionScope);
        }

        if (coordinates.getSpaceLocation().equals(Space.Location.IMPLEMENTS)) {
            getPrinter().append(code);
        } else {
            visitContainer(classDecl.getKind().equals(J.ClassDeclaration.Kind.Interface) ? "extends" : "implements",
                    classDecl.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, ",", null, insertionScope);
        }

        if (coordinates.getSpaceLocation().equals(Space.Location.BLOCK_PREFIX)) {
            getPrinter().append(code);
        } else {
            visit(classDecl.getBody(), insertionScope);
        }

        return classDecl;
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(method)) {
            return super.visitMethodDeclaration(method.withAnnotations(emptyList()).withBody(EMPTY_BLOCK), insertionScope);
        }

        visitSpace(method.getPrefix(), Space.Location.METHOD_DECLARATION_PREFIX, insertionScope);

        if (coordinates.getSpaceLocation().equals(Space.Location.ANNOTATION_PREFIX)) {
            getPrinter().append(code);
        } else {
            visit(method.getAnnotations(), insertionScope);
        }

        visitModifiers(method.getModifiers(), insertionScope);

        if (coordinates.getSpaceLocation().equals(Space.Location.TYPE_PARAMETERS)) {
            getPrinter().append(code);
        } else {
            visitContainer("<", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", insertionScope);
        }

        visit(method.getReturnTypeExpression(), insertionScope);
        visit(method.getName(), insertionScope);

        if (coordinates.getSpaceLocation().equals(Space.Location.METHOD_DECLARATION_PARAMETERS)) {
            getPrinter().append(code);
        } else {
            visitContainer("(", method.getPadding().getParameters(), JContainer.Location.METHOD_DECLARATION_PARAMETERS, ",", ")", insertionScope);
        }

        if (coordinates.getSpaceLocation().equals(Space.Location.THROWS)) {
            getPrinter().append(code);
        } else {
            visitContainer("throws", method.getPadding().getThrows(), JContainer.Location.THROWS, ",", null, insertionScope);
        }

        if (coordinates.getSpaceLocation().equals(Space.Location.BLOCK_PREFIX)) {
            getPrinter().append(code);
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

        if (coordinates.getSpaceLocation().equals(Space.Location.TYPE_PARAMETERS)) {
            getPrinter().append(code);
        } else {
            visitContainer("<", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", insertionScope);
        }
        visit(method.getName(), insertionScope);
        if (coordinates.getSpaceLocation().equals(Space.Location.METHOD_INVOCATION_ARGUMENTS)) {
            getPrinter().append(code);
        } else {
            visitContainer("(", method.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, ",", ")", insertionScope);
        }
        return method;
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(multiVariable)) {
            return super.visitVariableDeclarations(multiVariable.withAnnotations(emptyList()), insertionScope);
        } else if (!multiVariable.getId().equals(coordinates.getTree().getId())) {
            return super.visitVariableDeclarations(multiVariable.withAnnotations(emptyList()), insertionScope);
        }

        StringBuilder acc = getPrinter();
        visitSpace(multiVariable.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, insertionScope);

        if (coordinates.getSpaceLocation().equals(Space.Location.ANNOTATION_PREFIX)) {
            getPrinter().append(code);
        } else {
            visit(multiVariable.getAnnotations(), insertionScope);
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
        visitRightPadded(multiVariable.getPadding().getVars(), JRightPadded.Location.NAMED_VARIABLE, ",", insertionScope);
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
     * possibly mutated tree "changing" until the coordinates are found.
     *
     * @param parentScope The parent scope is root from the original AST
     * @param changing The possibly mutated (from a previous operation) branch
     * @param coordinates The coordinates within the changing branch to search for
     * @return A cursor representing the path from the original compilation unit to the coordinates element in the mutated tree
     */
    public static Cursor findCoordinateCursor(Cursor parentScope, Tree changing, JavaCoordinates coordinates) {
        AtomicReference<Cursor> cursorReference = new AtomicReference<>(parentScope);
        new ExtractInsertionCursor(coordinates, parentScope).visit(changing, cursorReference);
        return cursorReference.get();
    }

    private static class ExtractInsertionCursor extends JavaVisitor<AtomicReference<Cursor>> {
        private final UUID insertionId;
        private ExtractInsertionCursor(JavaCoordinates coordinates, Cursor parent) {
            insertionId = coordinates.getTree().getId();
            setCursoringOn();
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
