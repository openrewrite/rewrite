/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.golang;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.golang.marker.*;
import org.openrewrite.golang.tree.ChanDirMarker;
import org.openrewrite.golang.internal.StrconvQuote;
import org.openrewrite.golang.tree.Go;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.marker.Semicolon;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Prints Go LSTs. A port of the native printer in {@code pkg/printer/go_printer.go},
 * with which it must agree byte for byte.
 */
public class GolangPrinter<P> extends GolangVisitor<PrintOutputCapture<P>> {

    private static final UnaryOperator<String> MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    @Override
    public J visitGoCompilationUnit(Go.CompilationUnit cu, PrintOutputCapture<P> p) {
        if (cu.isCharsetBomMarked()) {
            p.append('\uFEFF');
        }
        beforeSyntax(cu, p);
        p.append("package");
        printRightPadded(cu.getPadding().getPackageDecl(), p);

        JContainer<J.Import> imports = cu.getImportsContainer();
        if (imports != null) {
            visitSpace(imports.getBefore(), p);
            p.append("import");

            GroupedImport grouped = imports.getMarkers().findFirst(GroupedImport.class).orElse(null);
            boolean isGrouped = grouped != null;
            if (grouped != null) {
                visitSpace(grouped.getBefore(), p);
                p.append('(');
            }
            for (JRightPadded<J.Import> anImport : imports.getPadding().getElements()) {
                ImportBlock block = anImport.getElement().getMarkers().findFirst(ImportBlock.class).orElse(null);
                if (block != null) {
                    if (block.isClosePrevious()) {
                        p.append(')');
                    }
                    visitSpace(block.getBefore(), p);
                    p.append("import");
                    if (block.isGrouped()) {
                        visitSpace(block.getGroupedBefore(), p);
                        p.append('(');
                    }
                    isGrouped = block.isGrouped();
                }
                printStatement(anImport, p);
            }
            if (isGrouped) {
                p.append(')');
            }
        }

        printStatements(cu.getPadding().getStatements(), p);

        afterSyntax(cu, p);
        visitSpace(cu.getEof(), p);
        return cu;
    }

    @Override
    public J visitIdentifier(J.Identifier ident, PrintOutputCapture<P> p) {
        beforeSyntax(ident, p);
        p.append(ident.getSimpleName());
        afterSyntax(ident, p);
        return ident;
    }

    @Override
    public J visitLiteral(J.Literal literal, PrintOutputCapture<P> p) {
        beforeSyntax(literal, p);
        p.append(literal.getValueSource());
        afterSyntax(literal, p);
        return literal;
    }

    @Override
    public J visitBinary(J.Binary binary, PrintOutputCapture<P> p) {
        beforeSyntax(binary, p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), p);
        p.append(binaryOperator(binary.getOperator()));
        visit(binary.getRight(), p);
        afterSyntax(binary, p);
        return binary;
    }

    @Override
    public J visitBlock(J.Block block, PrintOutputCapture<P> p) {
        beforeSyntax(block, p);
        p.append('{');
        printStatements(block.getPadding().getStatements(), p);
        visitSpace(block.getEnd(), p);
        p.append('}');
        afterSyntax(block, p);
        return block;
    }

    @Override
    public J visitReturn(J.Return retrn, PrintOutputCapture<P> p) {
        beforeSyntax(retrn, p);
        p.append("return");
        visit(retrn.getExpression(), p);
        afterSyntax(retrn, p);
        return retrn;
    }

    @Override
    public J visitGoReturn(Go.Return aReturn, PrintOutputCapture<P> p) {
        beforeSyntax(aReturn, p);
        p.append("return");
        printSeparated(aReturn.getPadding().getExpressions(), ",", p);
        afterSyntax(aReturn, p);
        return aReturn;
    }

    @Override
    public J visitIf(J.If iff, PrintOutputCapture<P> p) {
        // An `if` with an init clause sits in a StatementWithInit, which owns the prefix and the init.
        Go.StatementWithInit wrapper = statementWithInitWrapper();
        beforeWrapped(wrapper, iff, p);
        p.append("if");
        if (wrapper != null) {
            printStatement(wrapper.getPadding().getInit(), p);
        }
        printUnparenthesized(iff.getIfCondition(), p);
        printRightPadded(iff.getPadding().getThenPart(), p);
        visit(iff.getElsePart(), p);
        afterWrapped(wrapper, iff, p);
        return iff;
    }

    @Override
    public J visitElse(J.If.Else elze, PrintOutputCapture<P> p) {
        beforeSyntax(elze, p);
        p.append("else");
        printRightPadded(elze.getPadding().getBody(), p);
        afterSyntax(elze, p);
        return elze;
    }

    @Override
    public J visitAssignment(J.Assignment assignment, PrintOutputCapture<P> p) {
        beforeSyntax(assignment, p);
        visit(assignment.getVariable(), p);
        visitSpace(assignment.getPadding().getAssignment().getBefore(), p);
        p.append(assignment.getMarkers().findFirst(ShortVarDecl.class).isPresent() ? ":=" : "=");
        visit(assignment.getAssignment(), p);
        afterSyntax(assignment, p);
        return assignment;
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, PrintOutputCapture<P> p) {
        // A method with a receiver sits in a Go.MethodDeclaration, which owns the prefix and the receiver.
        Go.MethodDeclaration wrapper = methodDeclarationWrapper();
        printDirectives(method.getLeadingAnnotations(), p);
        beforeWrapped(wrapper, method, p);
        if (!method.getMarkers().findFirst(InterfaceMethod.class).isPresent()) {
            p.append("func");
        }
        if (wrapper != null) {
            printParamList(wrapper.getPadding().getReceiver(), p);
        }
        // a function literal has an empty name, which still prints its space and markers
        visit(method.getName(), p);
        visit(method.getPadding().getTypeParameters(), p);
        printParamList(method.getPadding().getParameters(), p);
        visit(method.getReturnTypeExpression(), p);
        visit(method.getBody(), p);
        afterWrapped(wrapper, method, p);
        return method;
    }

    @Override
    public J visitGoMethodDeclaration(Go.MethodDeclaration methodDeclaration, PrintOutputCapture<P> p) {
        visit(methodDeclaration.getDeclaration(), p);
        return methodDeclaration;
    }

    private Go.@Nullable MethodDeclaration methodDeclarationWrapper() {
        Object parent = parentTree();
        return parent instanceof Go.MethodDeclaration ? (Go.MethodDeclaration) parent : null;
    }

    @Override
    public J visitStatementWithInit(Go.StatementWithInit statementWithInit, PrintOutputCapture<P> p) {
        visit(statementWithInit.getStatement(), p);
        return statementWithInit;
    }

    private Go.@Nullable StatementWithInit statementWithInitWrapper() {
        Object parent = parentTree();
        return parent instanceof Go.StatementWithInit ? (Go.StatementWithInit) parent : null;
    }

    private @Nullable Object parentTree() {
        for (Cursor c = getCursor().getParent(); c != null; c = c.getParent()) {
            if (c.getValue() instanceof Tree) {
                return c.getValue();
            }
        }
        return null;
    }

    @Override
    public J visitTypeParameters(J.TypeParameters typeParameters, PrintOutputCapture<P> p) {
        beforeSyntax(typeParameters, p);
        p.append('[');
        printCommaSeparated(typeParameters.getPadding().getTypeParameters(), typeParameters.getMarkers(), p);
        p.append(']');
        afterSyntax(typeParameters, p);
        return typeParameters;
    }

    @Override
    public J visitTypeParameter(J.TypeParameter typeParameter, PrintOutputCapture<P> p) {
        beforeSyntax(typeParameter, p);
        visit(typeParameter.getName(), p);
        JContainer<TypeTree> bounds = typeParameter.getPadding().getBounds();
        if (bounds != null) {
            visitSpace(bounds.getBefore(), p);
            printSeparated(bounds.getPadding().getElements(), ",", p);
        }
        afterSyntax(typeParameter, p);
        return typeParameter;
    }

    private void printParamList(JContainer<Statement> params, PrintOutputCapture<P> p) {
        visitSpace(params.getBefore(), p);
        p.append('(');
        printCommaSeparated(params.getPadding().getElements(), params.getMarkers(), p);
        p.append(')');
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
        beforeSyntax(fieldAccess, p);
        visit(fieldAccess.getTarget(), p);
        visitSpace(fieldAccess.getPadding().getName().getBefore(), p);
        p.append('.');
        visit(fieldAccess.getName(), p);
        afterSyntax(fieldAccess, p);
        return fieldAccess;
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
        beforeSyntax(method, p);
        JRightPadded<Expression> select = method.getPadding().getSelect();
        if (select != null) {
            printRightPadded(select, p);
            // a call of something other than a name has an empty one
            if (!method.getSimpleName().isEmpty()) {
                p.append('.');
            }
        }
        visit(method.getName(), p);
        JContainer<Expression> typeParameters = method.getPadding().getTypeParameters();
        if (typeParameters != null) {
            printTypeArgs(typeParameters, p);
        }
        JContainer<Expression> arguments = method.getPadding().getArguments();
        visitSpace(arguments.getBefore(), p);
        p.append('(');
        printCommaSeparated(arguments.getPadding().getElements(), method.getMarkers(), p);
        p.append(')');
        afterSyntax(method, p);
        return method;
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
        // On a var or const the leading annotations are `//go:` directives; otherwise they are a struct field's tag.
        Markers markers = multiVariable.getMarkers();
        boolean structField = !markers.findFirst(VarKeyword.class).isPresent() &&
                !markers.findFirst(ConstDecl.class).isPresent();
        List<J.Annotation> annotations = multiVariable.getLeadingAnnotations();
        if (!structField) {
            printDirectives(annotations, p);
        }
        beforeSyntax(multiVariable, p);
        if (!markers.findFirst(GroupedSpec.class).isPresent()) {
            if (markers.findFirst(ConstDecl.class).isPresent()) {
                p.append("const");
            } else if (markers.findFirst(VarKeyword.class).isPresent()) {
                p.append("var");
            }
        }

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables =
                multiVariable.getPadding().getVariables();
        for (int i = 0; i < variables.size(); i++) {
            JRightPadded<J.VariableDeclarations.NamedVariable> variable = variables.get(i);
            beforeSyntax(variable.getElement(), p);
            visit(variable.getElement().getName(), p);
            afterSyntax(variable.getElement(), p);
            visitSpace(variable.getAfter(), p);
            if (i < variables.size() - 1) {
                p.append(',');
            }
        }
        if (multiVariable.getVarargs() != null) {
            visitSpace(multiVariable.getVarargs(), p);
            p.append("...");
        }
        visit(multiVariable.getTypeExpression(), p);
        if (structField && !annotations.isEmpty()) {
            printStructTag(annotations, markers.findFirst(StructTagQuote.class).orElse(null), p);
        }
        boolean firstInitializer = true;
        for (JRightPadded<J.VariableDeclarations.NamedVariable> variable : variables) {
            JLeftPadded<Expression> initializer = variable.getElement().getPadding().getInitializer();
            if (initializer != null) {
                visitSpace(initializer.getBefore(), p);
                p.append(firstInitializer ? '=' : ',');
                firstInitializer = false;
                visit(initializer.getElement(), p);
            }
        }
        afterSyntax(multiVariable, p);
        return multiVariable;
    }

    private void printStructTag(List<J.Annotation> annotations, @Nullable StructTagQuote quote,
                                PrintOutputCapture<P> p) {
        // the first annotation's prefix is the space before the opening quote
        PrintOutputCapture<P> body = p.clone();
        for (int i = 0; i < annotations.size(); i++) {
            J.Annotation annotation = annotations.get(i);
            beforePrefix(annotation.getMarkers(), getCursor(), i == 0 ? p : body);
            visitSpace(annotation.getPrefix(), i == 0 ? p : body);
            beforeSyntax(annotation.getMarkers(), getCursor(), body);
            printAnnotationBody(annotation, body);
            afterSyntax(annotation.getMarkers(), getCursor(), body);
        }
        if (quote == null || "`".equals(quote.getQuote())) {
            p.append('`').append(body.getOut()).append('`');
        } else if (!StringUtils.isNullOrEmpty(quote.getValueSource()) && body.getOut().equals(quote.getValue())) {
            // as written, escapes included, while the tag still says the same
            p.append(quote.getValueSource());
        } else {
            p.append(StrconvQuote.quote(body.getOut()));
        }
    }

    @Override
    public J visitDeclarationBlock(Go.DeclarationBlock declarationBlock, PrintOutputCapture<P> p) {
        printDirectives(declarationBlock.getLeadingAnnotations(), p);
        beforeSyntax(declarationBlock, p);
        p.append(declarationBlock.getKind() == Go.DeclKind.CONST ? "const" : "var");
        printSpecGroup(declarationBlock.getPadding().getSpecs(), p);
        afterSyntax(declarationBlock, p);
        return declarationBlock;
    }

    private void printSpecGroup(@Nullable JContainer<Statement> specs, PrintOutputCapture<P> p) {
        if (specs != null) {
            visitSpace(specs.getBefore(), p);
            p.append('(');
            printStatements(specs.getPadding().getElements(), p);
            p.append(')');
        }
    }

    @Override
    public J visitVariable(J.VariableDeclarations.NamedVariable variable, PrintOutputCapture<P> p) {
        beforeSyntax(variable, p);
        visit(variable.getName(), p);
        afterSyntax(variable, p);
        return variable;
    }

    @Override
    public J visitImport(J.Import impoort, PrintOutputCapture<P> p) {
        beforeSyntax(impoort, p);
        JLeftPadded<J.Identifier> alias = impoort.getPadding().getAlias();
        if (alias != null) {
            visitSpace(alias.getBefore(), p);
            visit(alias.getElement(), p);
        }
        // The path is a string literal in Go, held as a field access named after its content, whose parts
        // are printed for the space and markers on them.
        J.FieldAccess qualid = impoort.getQualid();
        setCursor(new Cursor(getCursor(), qualid));
        beforeSyntax(qualid, p);
        visit(qualid.getTarget(), p);
        visitSpace(qualid.getPadding().getName().getBefore(), p);
        J.Identifier name = qualid.getName();
        beforeSyntax(name, p);
        // an import that only carries the space of an empty group has no path
        String path = name.getSimpleName();
        p.append(path.isEmpty() || path.startsWith("`") ? path : '"' + path + '"');
        afterSyntax(name, p);
        afterSyntax(qualid, p);
        setCursor(getCursor().getParent());
        afterSyntax(impoort, p);
        return impoort;
    }

    @Override
    public J visitSwitch(J.Switch switzh, PrintOutputCapture<P> p) {
        Go.StatementWithInit wrapper = statementWithInitWrapper();
        beforeWrapped(wrapper, switzh, p);
        p.append("switch");
        if (wrapper != null) {
            printStatement(wrapper.getPadding().getInit(), p);
        }
        printUnparenthesized(switzh.getSelector(), p);
        visit(switzh.getCases(), p);
        afterWrapped(wrapper, switzh, p);
        return switzh;
    }

    @Override
    public J visitSelect(Go.Select select, PrintOutputCapture<P> p) {
        beforeSyntax(select, p);
        p.append("select");
        visit(select.getBody(), p);
        afterSyntax(select, p);
        return select;
    }

    @Override
    public J visitCase(J.Case caze, PrintOutputCapture<P> p) {
        beforeSyntax(caze, p);
        JContainer<J> caseLabels = caze.getPadding().getCaseLabels();
        List<JRightPadded<J>> labels = caseLabels.getPadding().getElements();
        if (isDefaultCase(labels)) {
            visitSpace(caseLabels.getBefore(), p);
            printRightPadded(labels.get(0), p);
        } else if (!labels.isEmpty()) {
            p.append("case");
            visitSpace(caseLabels.getBefore(), p);
            printSeparated(labels, ",", p);
        }
        visitSpace(caze.getPadding().getStatements().getBefore(), p);
        p.append(':');
        printStatements(caze.getPadding().getStatements().getPadding().getElements(), p);
        afterSyntax(caze, p);
        return caze;
    }

    // `default:` is a single identifier label; `default` is reserved, so no case expression can be one.
    private static boolean isDefaultCase(List<JRightPadded<J>> labels) {
        if (labels.size() != 1 || !(labels.get(0).getElement() instanceof J.Identifier)) {
            return false;
        }
        return "default".equals(((J.Identifier) labels.get(0).getElement()).getSimpleName());
    }

    @Override
    public J visitAssignmentOperation(J.AssignmentOperation assignOp, PrintOutputCapture<P> p) {
        beforeSyntax(assignOp, p);
        visit(assignOp.getVariable(), p);
        visitSpace(assignOp.getPadding().getOperator().getBefore(), p);
        p.append(assignmentOperator(assignOp.getOperator()));
        visit(assignOp.getAssignment(), p);
        afterSyntax(assignOp, p);
        return assignOp;
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop, PrintOutputCapture<P> p) {
        beforeSyntax(forLoop, p);
        p.append("for");
        visit(forLoop.getControl(), p);
        printRightPadded(forLoop.getPadding().getBody(), p);
        afterSyntax(forLoop, p);
        return forLoop;
    }

    @Override
    public J visitForControl(J.ForLoop.Control control, PrintOutputCapture<P> p) {
        beforeSyntax(control, p);
        List<JRightPadded<Statement>> init = control.getPadding().getInit();
        JRightPadded<Expression> condition = control.getPadding().getCondition();
        List<JRightPadded<Statement>> update = control.getPadding().getUpdate();
        if (control.getMarkers().findFirst(ImplicitForClauses.class).isPresent()) {
            // `for cond {}` or `for {}`: init and update hold placeholders, printed for their space and markers
            init.forEach(clause -> printRightPadded(clause, p));
            printRightPadded(condition, p);
            update.forEach(clause -> printRightPadded(clause, p));
        } else if (!init.isEmpty()) {
            printRightPadded(init.get(0), p);
            p.append(';');
            printRightPadded(condition, p);
            p.append(';');
            if (!update.isEmpty()) {
                printRightPadded(update.get(0), p);
            }
        } else {
            printRightPadded(condition, p);
        }
        afterSyntax(control, p);
        return control;
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forLoop, PrintOutputCapture<P> p) {
        beforeSyntax(forLoop, p);
        p.append("for");
        visit(forLoop.getControl(), p);
        printRightPadded(forLoop.getPadding().getBody(), p);
        afterSyntax(forLoop, p);
        return forLoop;
    }

    @Override
    public J visitForEachControl(J.ForEachLoop.Control control, PrintOutputCapture<P> p) {
        beforeSyntax(control, p);
        printRightPadded(control.getPadding().getVariable(), p);
        p.append("range");
        printRightPadded(control.getPadding().getIterable(), p);
        afterSyntax(control, p);
        return control;
    }

    @Override
    public J visitAnnotation(J.Annotation annotation, PrintOutputCapture<P> p) {
        beforeSyntax(annotation, p);
        printAnnotationBody(annotation, p);
        afterSyntax(annotation, p);
        return annotation;
    }

    // Struct tag form: `key:"value"`.
    private void printAnnotationBody(J.Annotation annotation, PrintOutputCapture<P> p) {
        visit(annotation.getAnnotationType(), p);
        JContainer<Expression> arguments = annotation.getPadding().getArguments();
        if (arguments != null) {
            visitSpace(arguments.getBefore(), p);
            p.append(':');
            printSeparated(arguments.getPadding().getElements(), "", p);
        }
    }

    // Directive form, the text after `//`: `go:linkname x runtime.x`.
    private void printDirectiveBody(J.Annotation annotation, PrintOutputCapture<P> p) {
        visit(annotation.getAnnotationType(), p);
        JContainer<Expression> arguments = annotation.getPadding().getArguments();
        if (arguments != null) {
            visitSpace(arguments.getBefore(), p);
            printSeparated(arguments.getPadding().getElements(), "", p);
        }
    }

    private void printDirectives(List<J.Annotation> annotations, PrintOutputCapture<P> p) {
        for (J.Annotation annotation : annotations) {
            beforeSyntax(annotation, p);
            p.append("//");
            printDirectiveBody(annotation, p);
            afterSyntax(annotation, p);
        }
    }

    @Override
    public J visitUnary(J.Unary unary, PrintOutputCapture<P> p) {
        beforeSyntax(unary, p);
        J.Unary.Type operator = unary.getOperator();
        if (operator == J.Unary.Type.PostIncrement || operator == J.Unary.Type.PostDecrement) {
            visit(unary.getExpression(), p);
            visitSpace(unary.getPadding().getOperator().getBefore(), p);
            p.append(unaryOperator(operator));
        } else {
            visitSpace(unary.getPadding().getOperator().getBefore(), p);
            p.append(unaryOperator(operator));
            visit(unary.getExpression(), p);
        }
        afterSyntax(unary, p);
        return unary;
    }

    @Override
    public J visitGoUnary(Go.Unary unary, PrintOutputCapture<P> p) {
        beforeSyntax(unary, p);
        visitSpace(unary.getPadding().getOperator().getBefore(), p);
        switch (unary.getOperator()) {
            case AddressOf:
                p.append('&');
                break;
            case Indirection:
                p.append('*');
                break;
            case Receive:
                p.append("<-");
                break;
            default:
                p.append('?');
        }
        visit(unary.getExpression(), p);
        afterSyntax(unary, p);
        return unary;
    }

    @Override
    public J visitGoBinary(Go.Binary binary, PrintOutputCapture<P> p) {
        beforeSyntax(binary, p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), p);
        p.append(binary.getOperator() == Go.Binary.Type.AndNot ? "&^" : "?");
        visit(binary.getRight(), p);
        afterSyntax(binary, p);
        return binary;
    }

    @Override
    public J visitGoAssignmentOperation(Go.AssignmentOperation assignOp, PrintOutputCapture<P> p) {
        beforeSyntax(assignOp, p);
        visit(assignOp.getVariable(), p);
        visitSpace(assignOp.getPadding().getOperator().getBefore(), p);
        p.append(assignOp.getOperator() == Go.AssignmentOperation.Type.AndNot ? "&^=" : "?=");
        visit(assignOp.getAssignment(), p);
        afterSyntax(assignOp, p);
        return assignOp;
    }

    @Override
    public J visitGoVariadic(Go.Variadic variadic, PrintOutputCapture<P> p) {
        beforeSyntax(variadic, p);
        if (variadic.isPostfix()) {
            visit(variadic.getElement(), p);
            visitSpace(variadic.getDots(), p);
            p.append("...");
        } else {
            visitSpace(variadic.getDots(), p);
            p.append("...");
            visit(variadic.getElement(), p);
        }
        afterSyntax(variadic, p);
        return variadic;
    }

    @Override
    public J visitBreak(J.Break breakStatement, PrintOutputCapture<P> p) {
        beforeSyntax(breakStatement, p);
        p.append("break");
        visit(breakStatement.getLabel(), p);
        afterSyntax(breakStatement, p);
        return breakStatement;
    }

    @Override
    public J visitContinue(J.Continue continueStatement, PrintOutputCapture<P> p) {
        beforeSyntax(continueStatement, p);
        p.append("continue");
        visit(continueStatement.getLabel(), p);
        afterSyntax(continueStatement, p);
        return continueStatement;
    }

    @Override
    public J visitLabel(J.Label label, PrintOutputCapture<P> p) {
        beforeSyntax(label, p);
        printRightPadded(label.getPadding().getLabel(), p);
        p.append(':');
        visit(label.getStatement(), p);
        afterSyntax(label, p);
        return label;
    }

    @Override
    public J visitGoStatement(Go.GoStatement goStmt, PrintOutputCapture<P> p) {
        beforeSyntax(goStmt, p);
        p.append("go");
        visit(goStmt.getExpression(), p);
        afterSyntax(goStmt, p);
        return goStmt;
    }

    @Override
    public J visitDefer(Go.Defer defer, PrintOutputCapture<P> p) {
        beforeSyntax(defer, p);
        p.append("defer");
        visit(defer.getExpression(), p);
        afterSyntax(defer, p);
        return defer;
    }

    @Override
    public J visitSend(Go.Send send, PrintOutputCapture<P> p) {
        beforeSyntax(send, p);
        visit(send.getChannelExpr(), p);
        visitSpace(send.getPadding().getArrow().getBefore(), p);
        p.append("<-");
        visit(send.getArrow(), p);
        afterSyntax(send, p);
        return send;
    }

    @Override
    public J visitGoto(Go.Goto gotoStmt, PrintOutputCapture<P> p) {
        beforeSyntax(gotoStmt, p);
        p.append("goto");
        visit(gotoStmt.getLabelIdent(), p);
        afterSyntax(gotoStmt, p);
        return gotoStmt;
    }

    @Override
    public J visitFallthrough(Go.Fallthrough fallthrough, PrintOutputCapture<P> p) {
        beforeSyntax(fallthrough, p);
        p.append("fallthrough");
        afterSyntax(fallthrough, p);
        return fallthrough;
    }

    @Override
    public J visitArrayType(J.ArrayType arrayType, PrintOutputCapture<P> p) {
        beforeSyntax(arrayType, p);
        JLeftPadded<Space> dimension = arrayType.getDimension();
        if (dimension != null) {
            visitSpace(dimension.getBefore(), p);
        }
        p.append('[');
        if (dimension != null) {
            visitSpace(dimension.getElement(), p);
        }
        p.append(']');
        visit(arrayType.getElementType(), p);
        afterSyntax(arrayType, p);
        return arrayType;
    }

    @Override
    public J visitGoArrayType(Go.ArrayType arrayType, PrintOutputCapture<P> p) {
        beforeSyntax(arrayType, p);
        p.append('[');
        printRightPadded(arrayType.getPadding().getLength(), p);
        p.append(']');
        visit(arrayType.getElementType(), p);
        afterSyntax(arrayType, p);
        return arrayType;
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens, PrintOutputCapture<P> p) {
        beforeSyntax(parens, p);
        p.append('(');
        printRightPadded(parens.getPadding().getTree(), p);
        p.append(')');
        afterSyntax(parens, p);
        return parens;
    }

    @Override
    public J visitParenthesizedTypeTree(J.ParenthesizedTypeTree parTree, PrintOutputCapture<P> p) {
        beforeSyntax(parTree, p);
        visit(parTree.getParenthesizedType(), p);
        afterSyntax(parTree, p);
        return parTree;
    }

    // Go's conversion `T(x)`: the type, then the parentheses around the operand.
    @Override
    public J visitTypeCast(J.TypeCast typeCast, PrintOutputCapture<P> p) {
        beforeSyntax(typeCast, p);
        // the parentheses follow the type they hold, and so does their prefix
        J.ControlParentheses<TypeTree> clazz = typeCast.getClazz();
        beforePrefix(clazz.getMarkers(), getCursor(), p);
        beforeSyntax(clazz.getMarkers(), getCursor(), p);
        visit(clazz.getTree(), p);
        visitSpace(clazz.getPrefix(), p);
        p.append('(');
        visit(typeCast.getExpression(), p);
        visitSpace(clazz.getPadding().getTree().getAfter(), p);
        typeCast.getMarkers().findFirst(TrailingComma.class)
                .ifPresent(trailingComma -> printTrailingComma(trailingComma, p));
        p.append(')');
        afterSyntax(clazz, p);
        afterSyntax(typeCast, p);
        return typeCast;
    }

    @Override
    public J visitTypeAssertion(Go.TypeAssertion typeAssertion, PrintOutputCapture<P> p) {
        beforeSyntax(typeAssertion, p);
        printRightPadded(typeAssertion.getPadding().getLeft(), p);
        p.append('.');
        visit(typeAssertion.getAssertedType(), p);
        afterSyntax(typeAssertion, p);
        return typeAssertion;
    }

    @Override
    public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, PrintOutputCapture<P> p) {
        beforeSyntax(controlParens, p);
        p.append('(');
        printRightPadded(controlParens.getPadding().getTree(), p);
        p.append(')');
        afterSyntax(controlParens, p);
        return controlParens;
    }

    // Go writes `if` conditions and `switch` selectors without the parentheses that J models.
    private void printUnparenthesized(J.ControlParentheses<?> controlParens, PrintOutputCapture<P> p) {
        beforeSyntax(controlParens, p);
        printRightPadded(controlParens.getPadding().getTree(), p);
        afterSyntax(controlParens, p);
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, PrintOutputCapture<P> p) {
        beforeSyntax(arrayAccess, p);
        visit(arrayAccess.getIndexed(), p);
        visit(arrayAccess.getDimension(), p);
        afterSyntax(arrayAccess, p);
        return arrayAccess;
    }

    @Override
    public J visitArrayDimension(J.ArrayDimension arrayDimension, PrintOutputCapture<P> p) {
        beforeSyntax(arrayDimension, p);
        p.append('[');
        printRightPadded(arrayDimension.getPadding().getIndex(), p);
        p.append(']');
        afterSyntax(arrayDimension, p);
        return arrayDimension;
    }

    @Override
    public J visitIndexList(Go.IndexList indexList, PrintOutputCapture<P> p) {
        beforeSyntax(indexList, p);
        visit(indexList.getTarget(), p);
        JContainer<Expression> indices = indexList.getPadding().getIndices();
        visitSpace(indices.getBefore(), p);
        p.append('[');
        printSeparated(indices.getPadding().getElements(), ",", p);
        p.append(']');
        afterSyntax(indexList, p);
        return indexList;
    }

    private void printTypeArgs(JContainer<Expression> args, PrintOutputCapture<P> p) {
        visitSpace(args.getBefore(), p);
        p.append('[');
        printCommaSeparated(args.getPadding().getElements(), args.getMarkers(), p);
        p.append(']');
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type, PrintOutputCapture<P> p) {
        beforeSyntax(type, p);
        visit(type.getClazz(), p);
        JContainer<Expression> typeParameters = type.getPadding().getTypeParameters();
        if (typeParameters != null) {
            printTypeArgs(typeParameters, p);
        }
        afterSyntax(type, p);
        return type;
    }

    @Override
    public J visitComposite(Go.Composite composite, PrintOutputCapture<P> p) {
        beforeSyntax(composite, p);
        visit(composite.getTypeExpr(), p);
        JContainer<Expression> elements = composite.getPadding().getElements();
        visitSpace(elements.getBefore(), p);
        p.append('{');
        printCommaSeparated(elements.getPadding().getElements(), composite.getMarkers(), p);
        p.append('}');
        afterSyntax(composite, p);
        return composite;
    }

    @Override
    public J visitKeyValue(Go.KeyValue keyValue, PrintOutputCapture<P> p) {
        beforeSyntax(keyValue, p);
        visit(keyValue.getKeyExpr(), p);
        visitSpace(keyValue.getPadding().getValue().getBefore(), p);
        p.append(':');
        visit(keyValue.getValue(), p);
        afterSyntax(keyValue, p);
        return keyValue;
    }

    @Override
    public J visitSliceExpr(Go.SliceExpr slice, PrintOutputCapture<P> p) {
        beforeSyntax(slice, p);
        visit(slice.getIndexed(), p);
        visitSpace(slice.getOpenBracket(), p);
        p.append('[');
        printRightPadded(slice.getPadding().getLow(), p);
        p.append(':');
        printRightPadded(slice.getPadding().getHigh(), p);
        if (slice.getMax() != null) {
            p.append(':');
            visit(slice.getMax(), p);
        }
        visitSpace(slice.getCloseBracket(), p);
        p.append(']');
        afterSyntax(slice, p);
        return slice;
    }

    @Override
    public J visitMapType(Go.MapType mapType, PrintOutputCapture<P> p) {
        beforeSyntax(mapType, p);
        p.append("map");
        visitSpace(mapType.getOpenBracket(), p);
        p.append('[');
        printRightPadded(mapType.getPadding().getKey(), p);
        p.append(']');
        visit(mapType.getValue(), p);
        afterSyntax(mapType, p);
        return mapType;
    }

    @Override
    public J visitExpressionStatement(Go.ExpressionStatement expressionStatement, PrintOutputCapture<P> p) {
        beforeSyntax(expressionStatement, p);
        visit(expressionStatement.getExpression(), p);
        afterSyntax(expressionStatement, p);
        return expressionStatement;
    }

    @Override
    public J visitStatementExpression(Go.StatementExpression statementExpression, PrintOutputCapture<P> p) {
        beforeSyntax(statementExpression, p);
        visit(statementExpression.getStatement(), p);
        afterSyntax(statementExpression, p);
        return statementExpression;
    }

    @Override
    public J visitPointerType(Go.PointerType pointerType, PrintOutputCapture<P> p) {
        beforeSyntax(pointerType, p);
        p.append('*');
        visit(pointerType.getElem(), p);
        afterSyntax(pointerType, p);
        return pointerType;
    }

    @Override
    public J visitChannel(Go.Channel channel, PrintOutputCapture<P> p) {
        beforeSyntax(channel, p);
        Space beforeDir = channel.getMarkers().findFirst(ChanDirMarker.class)
                .map(ChanDirMarker::getBefore)
                .orElse(Space.EMPTY);
        switch (channel.getDir()) {
            case BIDI:
                p.append("chan");
                break;
            case SEND_ONLY:
                p.append("chan");
                visitSpace(beforeDir, p);
                p.append("<-");
                break;
            case RECV_ONLY:
                p.append("<-");
                visitSpace(beforeDir, p);
                p.append("chan");
                break;
        }
        visit(channel.getValue(), p);
        afterSyntax(channel, p);
        return channel;
    }

    @Override
    public J visitFuncType(Go.FuncType funcType, PrintOutputCapture<P> p) {
        beforeSyntax(funcType, p);
        p.append("func");
        printParamList(funcType.getPadding().getParameters(), p);
        visit(funcType.getReturnType(), p);
        afterSyntax(funcType, p);
        return funcType;
    }

    @Override
    public J visitUnion(Go.Union union, PrintOutputCapture<P> p) {
        beforeSyntax(union, p);
        printSeparated(union.getPadding().getTypes(), "|", p);
        afterSyntax(union, p);
        return union;
    }

    @Override
    public J visitUnderlyingType(Go.UnderlyingType underlyingType, PrintOutputCapture<P> p) {
        beforeSyntax(underlyingType, p);
        p.append('~');
        visit(underlyingType.getElement(), p);
        afterSyntax(underlyingType, p);
        return underlyingType;
    }

    @Override
    public J visitTypeList(Go.TypeList typeList, PrintOutputCapture<P> p) {
        beforeSyntax(typeList, p);
        printParamList(typeList.getPadding().getTypes(), p);
        afterSyntax(typeList, p);
        return typeList;
    }

    @Override
    public J visitCommClause(Go.CommClause commClause, PrintOutputCapture<P> p) {
        beforeSyntax(commClause, p);
        if (commClause.getComm() != null) {
            p.append("case");
            visit(commClause.getComm(), p);
        } else {
            p.append("default");
        }
        visitSpace(commClause.getColon(), p);
        p.append(':');
        printStatements(commClause.getPadding().getBody(), p);
        afterSyntax(commClause, p);
        return commClause;
    }

    @Override
    public J visitMultiAssignment(Go.MultiAssignment multiAssignment, PrintOutputCapture<P> p) {
        beforeSyntax(multiAssignment, p);
        printSeparated(multiAssignment.getPadding().getVariables(), ",", p);
        visitSpace(multiAssignment.getPadding().getOperator().getBefore(), p);
        p.append(multiAssignment.getMarkers().findFirst(ShortVarDecl.class).isPresent() ? ":=" : "=");
        printSeparated(multiAssignment.getPadding().getValues(), ",", p);
        afterSyntax(multiAssignment, p);
        return multiAssignment;
    }

    @Override
    public J visitStructType(Go.StructType structType, PrintOutputCapture<P> p) {
        beforeSyntax(structType, p);
        p.append("struct");
        visit(structType.getBody(), p);
        afterSyntax(structType, p);
        return structType;
    }

    @Override
    public J visitInterfaceType(Go.InterfaceType interfaceType, PrintOutputCapture<P> p) {
        beforeSyntax(interfaceType, p);
        p.append("interface");
        visit(interfaceType.getBody(), p);
        afterSyntax(interfaceType, p);
        return interfaceType;
    }

    @Override
    public J visitTypeDecl(Go.TypeDecl typeDecl, PrintOutputCapture<P> p) {
        printDirectives(typeDecl.getLeadingAnnotations(), p);
        beforeSyntax(typeDecl, p);
        if (!typeDecl.getMarkers().findFirst(GroupedSpec.class).isPresent()) {
            p.append("type");
        }
        JContainer<Statement> specs = typeDecl.getPadding().getSpecs();
        if (specs != null) {
            printSpecGroup(specs, p);
        } else {
            visit(typeDecl.getName(), p);
            visit(typeDecl.getTypeParameters(), p);
            JLeftPadded<Space> assign = typeDecl.getPadding().getAssign();
            if (assign != null) {
                visitSpace(assign.getBefore(), p);
                p.append('=');
            }
            visit(typeDecl.getDefinition(), p);
        }
        afterSyntax(typeDecl, p);
        return typeDecl;
    }

    @Override
    public J visitEmpty(J.Empty empty, PrintOutputCapture<P> p) {
        beforeSyntax(empty, p);
        if (empty.getMarkers().findFirst(Semicolon.class).isPresent()) {
            p.append(';');
        }
        afterSyntax(empty, p);
        return empty;
    }

    private void printStatements(List<? extends JRightPadded<? extends J>> statements, PrintOutputCapture<P> p) {
        for (JRightPadded<? extends J> statement : statements) {
            printStatement(statement, p);
        }
    }

    private void printStatement(JRightPadded<? extends J> statement, PrintOutputCapture<P> p) {
        printRightPadded(statement, p);
        if (statement.getMarkers().findFirst(Semicolon.class).isPresent()) {
            p.append(';');
        }
    }

    private void printRightPadded(@Nullable JRightPadded<? extends J> right, PrintOutputCapture<P> p) {
        if (right != null) {
            visit(right.getElement(), p);
            visitSpace(right.getAfter(), p);
        }
    }

    private void printSeparated(List<? extends JRightPadded<? extends J>> elements, String separator,
                                PrintOutputCapture<P> p) {
        for (int i = 0; i < elements.size(); i++) {
            printRightPadded(elements.get(i), p);
            if (i < elements.size() - 1) {
                p.append(separator);
            }
        }
    }

    private void printCommaSeparated(List<? extends JRightPadded<? extends J>> elements, Markers markers,
                                     PrintOutputCapture<P> p) {
        TrailingComma trailingComma = markers.findFirst(TrailingComma.class).orElse(null);
        for (int i = 0; i < elements.size(); i++) {
            printRightPadded(elements.get(i), p);
            if (i < elements.size() - 1) {
                p.append(',');
            } else if (trailingComma != null) {
                printTrailingComma(trailingComma, p);
            }
        }
    }

    private void printTrailingComma(TrailingComma trailingComma, PrintOutputCapture<P> p) {
        visitSpace(trailingComma.getBefore(), p);
        p.append(',');
        visitSpace(trailingComma.getAfter(), p);
    }

    private void visitSpace(Space space, PrintOutputCapture<P> p) {
        printSpace(space, getCursor(), p);
    }

    static void printSpace(Space space, Cursor cursor, PrintOutputCapture<?> p) {
        p.append(space.getWhitespace());
        for (Comment comment : space.getComments()) {
            comment.printComment(cursor, p);
            p.append(comment.getSuffix());
        }
    }

    private void beforeSyntax(J j, PrintOutputCapture<P> p) {
        beforeSyntax(j.getPrefix(), j.getMarkers(), getCursor(), p);
    }

    private void afterSyntax(J j, PrintOutputCapture<P> p) {
        afterSyntax(j.getMarkers(), getCursor(), p);
    }

    // A wrapper prints nothing itself: the node it wraps prints its prefix and markers around its own.
    private void beforeWrapped(@Nullable J wrapper, J j, PrintOutputCapture<P> p) {
        Markers outer = wrapper == null ? Markers.EMPTY : wrapper.getMarkers();
        beforePrefix(outer, getCursor(), p);
        beforePrefix(j.getMarkers(), getCursor(), p);
        if (wrapper != null) {
            visitSpace(wrapper.getPrefix(), p);
        }
        visitSpace(j.getPrefix(), p);
        beforeSyntax(outer, getCursor(), p);
        beforeSyntax(j.getMarkers(), getCursor(), p);
    }

    private void afterWrapped(@Nullable J wrapper, J j, PrintOutputCapture<P> p) {
        afterSyntax(j, p);
        if (wrapper != null) {
            afterSyntax(wrapper, p);
        }
    }

    static void beforeSyntax(Space prefix, Markers markers, Cursor cursor, PrintOutputCapture<?> p) {
        beforePrefix(markers, cursor, p);
        printSpace(prefix, cursor, p);
        beforeSyntax(markers, cursor, p);
    }

    private static void beforePrefix(Markers markers, Cursor cursor, PrintOutputCapture<?> p) {
        for (Marker marker : markers.getMarkers()) {
            // stored LSTs hold a null where a marker had no RPC codec when they were parsed
            if (marker != null) {
                p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(cursor, marker), MARKER_WRAPPER));
            }
        }
    }

    private static void beforeSyntax(Markers markers, Cursor cursor, PrintOutputCapture<?> p) {
        for (Marker marker : markers.getMarkers()) {
            if (marker != null) {
                p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(cursor, marker), MARKER_WRAPPER));
            }
        }
    }

    static void afterSyntax(Markers markers, Cursor cursor, PrintOutputCapture<?> p) {
        for (Marker marker : markers.getMarkers()) {
            if (marker != null) {
                p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(cursor, marker), MARKER_WRAPPER));
            }
        }
    }

    private static String binaryOperator(J.Binary.Type operator) {
        switch (operator) {
            case Addition:
                return "+";
            case Subtraction:
                return "-";
            case Multiplication:
                return "*";
            case Division:
                return "/";
            case Modulo:
                return "%";
            case Equal:
                return "==";
            case NotEqual:
                return "!=";
            case LessThan:
                return "<";
            case LessThanOrEqual:
                return "<=";
            case GreaterThan:
                return ">";
            case GreaterThanOrEqual:
                return ">=";
            case And:
                return "&&";
            case Or:
                return "||";
            case BitAnd:
                return "&";
            case BitOr:
                return "|";
            case BitXor:
                return "^";
            case LeftShift:
                return "<<";
            case RightShift:
                return ">>";
            default:
                return "?";
        }
    }

    private static String assignmentOperator(J.AssignmentOperation.Type operator) {
        switch (operator) {
            case Addition:
                return "+=";
            case Subtraction:
                return "-=";
            case Multiplication:
                return "*=";
            case Division:
                return "/=";
            case Modulo:
                return "%=";
            case BitAnd:
                return "&=";
            case BitOr:
                return "|=";
            case BitXor:
                return "^=";
            case LeftShift:
                return "<<=";
            case RightShift:
                return ">>=";
            default:
                return "?=";
        }
    }

    private static String unaryOperator(J.Unary.Type operator) {
        switch (operator) {
            case Negative:
            case PreDecrement:
                return "-";
            case Positive:
            case PreIncrement:
                return "+";
            case Not:
                return "!";
            case Complement:
                return "^";
            case PostIncrement:
                return "++";
            case PostDecrement:
                return "--";
            default:
                return "?";
        }
    }
}
