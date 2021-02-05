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
package org.openrewrite.java.internal;

import org.openrewrite.Cursor;
import org.openrewrite.TreePrinter;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

class JavaTemplatePrinter extends JavaPrinter<Cursor> {
    private final JavaCoordinates<?> coordinates;
    private final Set<String> imports;
    private final String code;

    JavaTemplatePrinter(String code, JavaCoordinates<?> coordinates, Set<String> imports) {
        super(TreePrinter.identity());
        this.code = code;
        this.coordinates = coordinates;
        this.imports = imports;
        setCursoringOn();
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
        if (coordinates.getTree().getId().equals(block.getId()) ||
                (!insertionScope.isScopeInPath(block) && !(parent.getValue() instanceof J.ClassDecl))) {
            return block.withStatements(emptyList());
        }

        J.Block b;
        if (!(parent.getValue() instanceof J.ClassDecl)) {
            b = visitAndCast(block, insertionScope, this::preVisit);
            b = visitAndCast(b, insertionScope, this::visitStatement);

            if (b.getStatements().stream().anyMatch(insertionScope::isScopeInPath)) {
                // If a statement in the block is in insertion scope, then this will render each statement
                // up to the statement that is in insertion scope.
                List<Statement> statementsInScope = new ArrayList<>();
                for (Statement statement : b.getStatements()) {
                    statementsInScope.add((Statement) visit(statement, insertionScope));
                    if (insertionScope.isScopeInPath(statement)) {
                        break;
                    }
                }
                b = b.withStatements(statementsInScope);
            }
        } else {
            b = (J.Block) super.visitBlock(block, insertionScope);
        }

        return b;
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, Cursor cursor) {
        J j = getCursor().firstEnclosing(J.class);
        if (loc.equals(coordinates.getSpaceLocation()) && coordinates.getTree().equals(j)) {
            getPrinter().append(code);
        }
        return super.visitSpace(space, loc, cursor);
    }

    @Override
    public J visitClassDecl(J.ClassDecl classDecl, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(classDecl)) {
            return super.visitClassDecl(classDecl.withAnnotations(emptyList()), insertionScope);
        } else if (!classDecl.getId().equals(coordinates.getTree().getId())) {
            return classDecl.withAnnotations(emptyList());
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

        visitSpace(classDecl.getPrefix(), Space.Location.CLASS_DECL_PREFIX, insertionScope);

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
            visitContainer(classDecl.getKind().equals(J.ClassDecl.Kind.Interface) ? "extends" : "implements",
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
    public J.MethodDecl visitMethod(J.MethodDecl method, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(method)) {
            return method.withAnnotations(emptyList()).withBody(null);
        } else if (!method.getId().equals(coordinates.getTree().getId())) {
            return method.withAnnotations(emptyList());
        }

        visitSpace(method.getPrefix(), Space.Location.METHOD_DECL_PREFIX, insertionScope);

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

        visit(method.getReturnTypeExpr(), insertionScope);
        visit(method.getName(), insertionScope);

        if (coordinates.getSpaceLocation().equals(Space.Location.METHOD_DECL_PARAMETERS)) {
            getPrinter().append(code);
        } else {
            visitContainer("(", method.getPadding().getParams(), JContainer.Location.METHOD_DECL_PARAMETERS, ",", ")", insertionScope);
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

        visitLeftPadded("default", method.getPadding().getDefaultValue(), JLeftPadded.Location.METHOD_DECL_DEFAULT_VALUE, insertionScope);

        return method;
    }

    @Override
    public J visitMultiVariable(J.VariableDecls multiVariable, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(multiVariable)) {
            return multiVariable.withAnnotations(emptyList());
        } else if (!multiVariable.getId().equals(coordinates.getTree().getId())) {
            return multiVariable.withAnnotations(emptyList());
        }

        StringBuilder acc = getPrinter();
        visitSpace(multiVariable.getPrefix(), Space.Location.MULTI_VARIABLE_PREFIX, insertionScope);

        if (coordinates.getSpaceLocation().equals(Space.Location.ANNOTATION_PREFIX)) {
            getPrinter().append(code);
        } else {
            visit(multiVariable.getAnnotations(), insertionScope);
        }

        visitModifiers(multiVariable.getModifiers(), insertionScope);
        visit(multiVariable.getTypeExpr(), insertionScope);
        for (JLeftPadded<Space> dim : multiVariable.getDimensionsBeforeName()) {
            visitSpace(dim.getBefore(), Space.Location.DIMENSION_PREFIX, insertionScope);
            acc.append('[');
            visitSpace(dim.getElem(), Space.Location.DIMENSION, insertionScope);
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
    public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, Cursor insertionScope) {
        if (!insertionScope.isScopeInPath(variable)) {
            // Variables in the original AST only need to be declared, nulls out the initializers.
            return variable.withInitializer(null);
        }
        return (J.VariableDecls.NamedVar) super.visitVariable(variable, insertionScope);
    }
}
