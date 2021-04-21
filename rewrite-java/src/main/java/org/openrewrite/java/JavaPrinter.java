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
package org.openrewrite.java;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;

import static org.openrewrite.java.tree.J.Modifier.*;

public class JavaPrinter<P> extends JavaVisitor<P> {

    private static final String PRINTER_ACC_KEY = "printed";

    private final TreePrinter<P> treePrinter;

    public JavaPrinter(TreePrinter<P> treePrinter) {
        this.treePrinter = treePrinter;
    }

    @NonNull
    protected StringBuilder getPrinter() {
        StringBuilder acc = getCursor().getRoot().getNearestMessage(PRINTER_ACC_KEY);
        if (acc == null) {
            acc = new StringBuilder();
            getCursor().getRoot().putMessage(PRINTER_ACC_KEY, acc);
        }
        return acc;
    }

    public String print(J j, P p) {
        setCursor(new Cursor(null, "EPSILON"));
        visit(j, p);
        return getPrinter().toString();
    }

    @Override
    @Nullable
    public J visit(@Nullable Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        StringBuilder printerAcc = getPrinter();
        treePrinter.doBefore(tree, printerAcc, p);
        tree = super.visit(tree, p);
        if (tree != null) {
            treePrinter.doAfter(tree, printerAcc, p);
        }
        return (J) tree;
    }

    protected void visit(@Nullable List<? extends J> nodes, P p) {
        if (nodes != null) {
            for (J node : nodes) {
                visit(node, p);
            }
        }
    }

    protected void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, JRightPadded.Location location, String suffixBetween, P p) {
        StringBuilder acc = getPrinter();
        for (int i = 0; i < nodes.size(); i++) {
            JRightPadded<? extends J> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), location.getAfterLocation(), p);
            if (i < nodes.size() - 1) {
                acc.append(suffixBetween);
            }
        }
    }

    protected void visitContainer(String before, @Nullable JContainer<? extends J> container, JContainer.Location location, String suffixBetween, @Nullable String after, P p) {
        if (container == null) {
            return;
        }
        StringBuilder acc = getPrinter();
        visitSpace(container.getBefore(), location.getBeforeLocation(), p);
        acc.append(before);
        visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
        acc.append(after == null ? "" : after);
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, P p) {
        StringBuilder acc = getPrinter();
        acc.append(space.getWhitespace());

        for (Comment comment : space.getComments()) {
            visitMarkers(comment.getMarkers(), p);
            switch (comment.getStyle()) {
                case LINE:
                    acc.append("//").append(comment.getText());
                    break;
                case BLOCK:
                    acc.append("/*").append(comment.getText()).append("*/");
                    break;
                case JAVADOC:
                    acc.append("/**").append(comment.getText()).append("*/");
                    break;
            }
            acc.append(comment.getSuffix());
        }
        return space;
    }

    protected void visitLeftPadded(@Nullable String prefix, @Nullable JLeftPadded<? extends J> leftPadded, JLeftPadded.Location location, P p) {
        if (leftPadded != null) {
            StringBuilder acc = getPrinter();
            visitSpace(leftPadded.getBefore(), location.getBeforeLocation(), p);
            if (prefix != null) {
                acc.append(prefix);
            }
            visit(leftPadded.getElement(), p);
        }
    }

    protected void visitRightPadded(@Nullable JRightPadded<? extends J> rightPadded, JRightPadded.Location location, @Nullable String suffix, P p) {
        if (rightPadded != null) {
            StringBuilder acc = getPrinter();
            visit(rightPadded.getElement(), p);
            visitSpace(rightPadded.getAfter(), location.getAfterLocation(), p);
            if (suffix != null) {
                acc.append(suffix);
            }
        }
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, P p) {
        StringBuilder acc = getPrinter();
        treePrinter.doBefore(marker, acc, p);
        acc.append(marker.print(treePrinter, p));
        treePrinter.doAfter(marker, acc, p);
        //noinspection unchecked
        return (M) marker;
    }

    @Override
    public Markers visitMarkers(Markers markers, P p) {
        StringBuilder acc = getPrinter();
        treePrinter.doBefore(markers, acc, p);
        Markers m = super.visitMarkers(markers, p);
        treePrinter.doAfter(markers, acc, p);
        return m;
    }

    protected void visitModifiers(Iterable<Modifier> modifiers, P p) {
        StringBuilder acc = getPrinter();
        for (Modifier mod : modifiers) {
            visit(mod.getAnnotations(), p);
            String keyword = "";
            switch (mod.getType()) {
                case Default:
                    keyword = "default";
                    break;
                case Public:
                    keyword = "public";
                    break;
                case Protected:
                    keyword = "protected";
                    break;
                case Private:
                    keyword = "private";
                    break;
                case Abstract:
                    keyword = "abstract";
                    break;
                case Static:
                    keyword = "static";
                    break;
                case Final:
                    keyword = "final";
                    break;
                case Native:
                    keyword = "native";
                    break;
                case Strictfp:
                    keyword = "strictfp";
                    break;
                case Synchronized:
                    keyword = "synchronized";
                    break;
                case Transient:
                    keyword = "transient";
                    break;
                case Volatile:
                    keyword = "volatile";
                    break;
            }
            visitSpace(mod.getPrefix(), Space.Location.MODIFIER_PREFIX, p);
            visitMarkers(mod.getMarkers(), p);

            acc.append(keyword);
        }
    }

    @Override
    public J visitAnnotation(Annotation annotation, P p) {
        visitSpace(annotation.getPrefix(), Space.Location.ANNOTATION_PREFIX, p);
        visitMarkers(annotation.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append("@");
        visit(annotation.getAnnotationType(), p);
        visitContainer("(", annotation.getPadding().getArguments(), JContainer.Location.ANNOTATION_ARGUMENTS, ",", ")", p);
        return annotation;
    }

    @Override
    public J visitAnnotatedType(AnnotatedType annotatedType, P p) {
        visitSpace(annotatedType.getPrefix(), Space.Location.ANNOTATED_TYPE_PREFIX, p);
        visitMarkers(annotatedType.getMarkers(), p);
        visit(annotatedType.getAnnotations(), p);
        visit(annotatedType.getTypeExpression(), p);
        return annotatedType;
    }

    @Override
    public J visitArrayDimension(ArrayDimension arrayDimension, P p) {
        visitSpace(arrayDimension.getPrefix(), Space.Location.DIMENSION_PREFIX, p);
        visitMarkers(arrayDimension.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append("[");
        visitRightPadded(arrayDimension.getPadding().getIndex(), JRightPadded.Location.ARRAY_INDEX, "]", p);
        return arrayDimension;
    }

    @Override
    public J visitArrayType(ArrayType arrayType, P p) {
        visitSpace(arrayType.getPrefix(), Space.Location.ARRAY_TYPE_PREFIX, p);
        visitMarkers(arrayType.getMarkers(), p);
        visit(arrayType.getElementType(), p);
        StringBuilder acc = getPrinter();
        for (JRightPadded<Space> d : arrayType.getDimensions()) {
            visitSpace(d.getElement(), Space.Location.DIMENSION, p);
            acc.append('[');
            visitSpace(d.getAfter(), Space.Location.DIMENSION_SUFFIX, p);
            acc.append(']');
        }
        return arrayType;
    }

    @Override
    public J visitAssert(Assert azzert, P p) {
        visitSpace(azzert.getPrefix(), Space.Location.ASSERT_PREFIX, p);
        visitMarkers(azzert.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append("assert");
        visit(azzert.getCondition(), p);
        return azzert;
    }

    @Override
    public J visitAssignment(Assignment assignment, P p) {
        visitSpace(assignment.getPrefix(), Space.Location.ASSIGNMENT_PREFIX, p);
        visitMarkers(assignment.getMarkers(), p);
        visit(assignment.getVariable(), p);
        visitLeftPadded("=", assignment.getPadding().getAssignment(), JLeftPadded.Location.ASSIGNMENT, p);
        return assignment;
    }

    @Override
    public J visitAssignmentOperation(AssignmentOperation assignOp, P p) {
        String keyword = "";
        switch (assignOp.getOperator()) {
            case Addition:
                keyword = "+=";
                break;
            case Subtraction:
                keyword = "-=";
                break;
            case Multiplication:
                keyword = "*=";
                break;
            case Division:
                keyword = "/=";
                break;
            case Modulo:
                keyword = "%=";
                break;
            case BitAnd:
                keyword = "&=";
                break;
            case BitOr:
                keyword = "|=";
                break;
            case BitXor:
                keyword = "^=";
                break;
            case LeftShift:
                keyword = "<<=";
                break;
            case RightShift:
                keyword = ">>=";
                break;
            case UnsignedRightShift:
                keyword = ">>>=";
                break;
        }
        visitSpace(assignOp.getPrefix(), Space.Location.ASSIGNMENT_OPERATION_PREFIX, p);
        visitMarkers(assignOp.getMarkers(), p);
        visit(assignOp.getVariable(), p);
        visitSpace(assignOp.getPadding().getOperator().getBefore(), Space.Location.ASSIGNMENT_OPERATION_OPERATOR, p);
        StringBuilder acc = getPrinter();
        acc.append(keyword);
        visit(assignOp.getAssignment(), p);
        return assignOp;
    }

    @Override
    public J visitBinary(Binary binary, P p) {
        String keyword = "";
        switch (binary.getOperator()) {
            case Addition:
                keyword = "+";
                break;
            case Subtraction:
                keyword = "-";
                break;
            case Multiplication:
                keyword = "*";
                break;
            case Division:
                keyword = "/";
                break;
            case Modulo:
                keyword = "%";
                break;
            case LessThan:
                keyword = "<";
                break;
            case GreaterThan:
                keyword = ">";
                break;
            case LessThanOrEqual:
                keyword = "<=";
                break;
            case GreaterThanOrEqual:
                keyword = ">=";
                break;
            case Equal:
                keyword = "==";
                break;
            case NotEqual:
                keyword = "!=";
                break;
            case BitAnd:
                keyword = "&";
                break;
            case BitOr:
                keyword = "|";
                break;
            case BitXor:
                keyword = "^";
                break;
            case LeftShift:
                keyword = "<<";
                break;
            case RightShift:
                keyword = ">>";
                break;
            case UnsignedRightShift:
                keyword = ">>>";
                break;
            case Or:
                keyword = "||";
                break;
            case And:
                keyword = "&&";
                break;
        }
        visitSpace(binary.getPrefix(), Space.Location.BINARY_PREFIX, p);
        visitMarkers(binary.getMarkers(), p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), Space.Location.BINARY_OPERATOR, p);
        StringBuilder acc = getPrinter();
        acc.append(keyword);
        visit(binary.getRight(), p);
        return binary;
    }

    @Override
    public J visitBlock(Block block, P p) {
        visitSpace(block.getPrefix(), Space.Location.BLOCK_PREFIX, p);
        visitMarkers(block.getMarkers(), p);

        StringBuilder acc = getPrinter();

        if (block.isStatic()) {
            acc.append("static");
            visitRightPadded(block.getPadding().getStatic(), JRightPadded.Location.STATIC_INIT, p);
        }

        acc.append('{');
        visitStatements(block.getPadding().getStatements(), JRightPadded.Location.BLOCK_STATEMENT, p);
        visitSpace(block.getEnd(), Space.Location.BLOCK_END, p);
        acc.append('}');
        return block;
    }

    private void visitStatements(List<JRightPadded<Statement>> statements, JRightPadded.Location location, P p) {
        for (JRightPadded<Statement> paddedStat : statements) {
            visitStatement(paddedStat, location, p);
        }
    }

    private void visitStatement(@Nullable JRightPadded<Statement> paddedStat, JRightPadded.Location location, P p) {
        if (paddedStat == null) {
            return;
        }

        visit(paddedStat.getElement(), p);
        visitSpace(paddedStat.getAfter(), location.getAfterLocation(), p);

        StringBuilder acc = getPrinter();
        Statement s = paddedStat.getElement();
        while (true) {
            if (s instanceof Assert ||
                    s instanceof Assignment ||
                    s instanceof AssignmentOperation ||
                    s instanceof Break ||
                    s instanceof Continue ||
                    s instanceof DoWhileLoop ||
                    s instanceof Empty ||
                    s instanceof MethodInvocation ||
                    s instanceof NewClass ||
                    s instanceof Return ||
                    s instanceof Throw ||
                    s instanceof Unary ||
                    s instanceof VariableDeclarations) {
                acc.append(';');
                return;
            }

            if (s instanceof MethodDeclaration && ((MethodDeclaration) s).getBody() == null) {
                acc.append(';');
                return;
            }

            if (s instanceof Label) {
                s = ((Label) s).getStatement();
                continue;
            }
            return;
        }
    }

    @Override
    public J visitBreak(Break breakStatement, P p) {
        visitSpace(breakStatement.getPrefix(), Space.Location.BREAK_PREFIX, p);
        visitMarkers(breakStatement.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append("break");
        visit(breakStatement.getLabel(), p);
        return breakStatement;
    }

    @Override
    public J visitCase(Case caze, P p) {
        visitSpace(caze.getPrefix(), Space.Location.CASE_PREFIX, p);
        visitMarkers(caze.getMarkers(), p);
        StringBuilder acc = getPrinter();
        Expression elem = caze.getPattern();
        if (elem instanceof Identifier && ((Identifier) elem).getSimpleName().equals("default")) {
            acc.append("default");
        } else {
            acc.append("case");
            visit(elem, p);
        }
        visitSpace(caze.getPadding().getStatements().getBefore(), Space.Location.CASE, p);
        acc.append(':');
        visitStatements(caze.getPadding().getStatements().getPadding().getElements(), JRightPadded.Location.CASE, p);
        return caze;
    }

    @Override
    public J visitCatch(Try.Catch catzh, P p) {
        visitSpace(catzh.getPrefix(), Space.Location.CATCH_PREFIX, p);
        visitMarkers(catzh.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append("catch");
        visit(catzh.getParameter(), p);
        visit(catzh.getBody(), p);
        return catzh;
    }

    @Override
    public J visitClassDeclaration(ClassDeclaration classDecl, P p) {
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

        visitSpace(classDecl.getPrefix(), Space.Location.CLASS_DECLARATION_PREFIX, p);
        visitMarkers(classDecl.getMarkers(), p);
        visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
        visit(classDecl.getLeadingAnnotations(), p);
        visitModifiers(classDecl.getModifiers(), p);
        visit(classDecl.getAnnotations().getKind().getAnnotations(), p);
        visitSpace(classDecl.getAnnotations().getKind().getPrefix(), Space.Location.CLASS_KIND, p);
        StringBuilder acc = getPrinter();
        acc.append(kind);
        visit(classDecl.getName(), p);
        visitContainer("<", classDecl.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
        visitLeftPadded("extends", classDecl.getPadding().getExtends(), JLeftPadded.Location.EXTENDS, p);
        visitContainer(classDecl.getKind().equals(ClassDeclaration.Kind.Type.Interface) ? "extends" : "implements",
                classDecl.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, ",", null, p);
        visit(classDecl.getBody(), p);
        return classDecl;
    }

    @Override
    public J visitCompilationUnit(CompilationUnit cu, P p) {
        visitSpace(cu.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p);
        visitMarkers(cu.getMarkers(), p);
        visitRightPadded(cu.getPadding().getPackageDeclaration(), JRightPadded.Location.PACKAGE, ";", p);
        visitRightPadded(cu.getPadding().getImports(), JRightPadded.Location.IMPORT, ";", p);
        StringBuilder acc = getPrinter();
        if (!cu.getImports().isEmpty()) {
            acc.append(";");
        }
        visit(cu.getClasses(), p);
        visitSpace(cu.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        return cu;
    }

    @Override
    public J visitContinue(Continue continueStatement, P p) {
        visitSpace(continueStatement.getPrefix(), Space.Location.CONTINUE_PREFIX, p);
        visitMarkers(continueStatement.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append("continue");
        visit(continueStatement.getLabel(), p);
        return continueStatement;
    }

    @Override
    public <T extends J> J visitControlParentheses(ControlParentheses<T> controlParens, P p) {
        visitSpace(controlParens.getPrefix(), Space.Location.CONTROL_PARENTHESES_PREFIX, p);
        visitMarkers(controlParens.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append('(');
        visitRightPadded(controlParens.getPadding().getTree(), JRightPadded.Location.PARENTHESES, ")", p);
        return controlParens;
    }

    @Override
    public J visitDoWhileLoop(DoWhileLoop doWhileLoop, P p) {
        visitSpace(doWhileLoop.getPrefix(), Space.Location.DO_WHILE_PREFIX, p);
        visitMarkers(doWhileLoop.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append("do");
        visitStatement(doWhileLoop.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, p);
        visitLeftPadded("while", doWhileLoop.getPadding().getWhileCondition(), JLeftPadded.Location.WHILE_CONDITION, p);
        return doWhileLoop;
    }

    @Override
    public J visitElse(If.Else elze, P p) {
        visitSpace(elze.getPrefix(), Space.Location.ELSE_PREFIX, p);
        visitMarkers(elze.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append("else");
        visitStatement(elze.getPadding().getBody(), JRightPadded.Location.IF_ELSE, p);
        return elze;
    }

    @Override
    public J visitEnumValue(EnumValue enoom, P p) {
        visitSpace(enoom.getPrefix(), Space.Location.ENUM_VALUE_PREFIX, p);
        visitMarkers(enoom.getMarkers(), p);
        visit(enoom.getAnnotations(), p);
        visit(enoom.getName(), p);
        NewClass initializer = enoom.getInitializer();
        if (enoom.getInitializer() != null) {
            visitSpace(initializer.getPrefix(), Space.Location.NEW_CLASS_PREFIX, p);
            visitSpace(initializer.getNew(), Space.Location.NEW_PREFIX, p);
            visitContainer("(", initializer.getArguments(), JContainer.Location.NEW_CLASS_ARGUMENTS, ",", ")", p);
            visit(initializer.getBody(), p);
        }
        return enoom;
    }

    @Override
    public J visitEnumValueSet(EnumValueSet enums, P p) {
        visitSpace(enums.getPrefix(), Space.Location.ENUM_VALUE_SET_PREFIX, p);
        visitMarkers(enums.getMarkers(), p);
        visitRightPadded(enums.getPadding().getEnums(), JRightPadded.Location.ENUM_VALUE, ",", p);
        StringBuilder acc = getPrinter();
        if (enums.isTerminatedWithSemicolon()) {
            acc.append(';');
        }
        return enums;
    }

    @Override
    public J visitFieldAccess(FieldAccess fieldAccess, P p) {
        visitSpace(fieldAccess.getPrefix(), Space.Location.FIELD_ACCESS_PREFIX, p);
        visitMarkers(fieldAccess.getMarkers(), p);
        visit(fieldAccess.getTarget(), p);
        visitLeftPadded(".", fieldAccess.getPadding().getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p);
        return fieldAccess;
    }

    @Override
    public J visitForLoop(ForLoop forLoop, P p) {
        visitSpace(forLoop.getPrefix(), Space.Location.FOR_PREFIX, p);
        visitMarkers(forLoop.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append("for");
        ForLoop.Control ctrl = forLoop.getControl();
        visitSpace(ctrl.getPrefix(), Space.Location.FOR_CONTROL_PREFIX, p);
        acc.append('(');
        visitRightPadded(ctrl.getPadding().getInit(), JRightPadded.Location.FOR_INIT, ";", p);
        visitRightPadded(ctrl.getPadding().getCondition(), JRightPadded.Location.FOR_CONDITION, ";", p);
        visitRightPadded(ctrl.getPadding().getUpdate(), JRightPadded.Location.FOR_UPDATE, ",", p);
        acc.append(')');
        visitStatement(forLoop.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p);
        return forLoop;
    }

    @Override
    public J visitForEachLoop(ForEachLoop forEachLoop, P p) {
        visitSpace(forEachLoop.getPrefix(), Space.Location.FOR_EACH_LOOP_PREFIX, p);
        visitMarkers(forEachLoop.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append("for");
        ForEachLoop.Control ctrl = forEachLoop.getControl();
        visitSpace(ctrl.getPrefix(), Space.Location.FOR_EACH_CONTROL_PREFIX, p);
        acc.append('(');
        visitRightPadded(ctrl.getPadding().getVariable(), JRightPadded.Location.FOREACH_VARIABLE, ":", p);
        visitRightPadded(ctrl.getPadding().getIterable(), JRightPadded.Location.FOREACH_ITERABLE, "", p);
        acc.append(')');
        visitStatement(forEachLoop.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p);
        return forEachLoop;
    }

    @Override
    public J visitIdentifier(Identifier ident, P p) {
        visitSpace(ident.getPrefix(), Space.Location.IDENTIFIER_PREFIX, p);
        visitMarkers(ident.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(ident.getSimpleName());
        return ident;
    }

    @Override
    public J visitIf(If iff, P p) {
        visitSpace(iff.getPrefix(), Space.Location.IF_PREFIX, p);
        visitMarkers(iff.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append("if");
        visit(iff.getIfCondition(), p);
        visitStatement(iff.getPadding().getThenPart(), JRightPadded.Location.IF_THEN, p);
        visit(iff.getElsePart(), p);
        return iff;
    }

    @Override
    public J visitImport(Import impoort, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(impoort.getPrefix(), Space.Location.IMPORT_PREFIX, p);
        visitMarkers(impoort.getMarkers(), p);
        acc.append("import");
        if (impoort.isStatic()) {
            visitSpace(impoort.getPadding().getStatic().getBefore(), Space.Location.STATIC_IMPORT, p);
            acc.append("static");
        }
        visit(impoort.getQualid(), p);
        return impoort;
    }

    @Override
    public J visitInstanceOf(InstanceOf instanceOf, P p) {
        visitSpace(instanceOf.getPrefix(), Space.Location.INSTANCEOF_PREFIX, p);
        visitMarkers(instanceOf.getMarkers(), p);
        visitRightPadded(instanceOf.getPadding().getExpr(), JRightPadded.Location.INSTANCEOF, "instanceof", p);
        visit(instanceOf.getClazz(), p);
        return instanceOf;
    }

    @Override
    public J visitLabel(Label label, P p) {
        visitSpace(label.getPrefix(), Space.Location.LABEL_PREFIX, p);
        visitMarkers(label.getMarkers(), p);
        visitRightPadded(label.getPadding().getLabel(), JRightPadded.Location.LABEL, ":", p);
        visit(label.getStatement(), p);
        return label;
    }

    @Override
    public J visitLambda(Lambda lambda, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(lambda.getPrefix(), Space.Location.LAMBDA_PREFIX, p);
        visitMarkers(lambda.getMarkers(), p);
        visitSpace(lambda.getParameters().getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, p);
        visitMarkers(lambda.getParameters().getMarkers(), p);
        if (lambda.getParameters().isParenthesized()) {
            acc.append('(');
            visitRightPadded(lambda.getParameters().getPadding().getParams(), JRightPadded.Location.LAMBDA_PARAM, ",", p);
            acc.append(')');
        } else {
            visitRightPadded(lambda.getParameters().getPadding().getParams(), JRightPadded.Location.LAMBDA_PARAM, ",", p);
        }
        visitSpace(lambda.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p);
        acc.append("->");
        visit(lambda.getBody(), p);
        return lambda;
    }

    @Override
    public J visitLiteral(Literal literal, P p) {
        visitSpace(literal.getPrefix(), Space.Location.LITERAL_PREFIX, p);
        visitMarkers(literal.getMarkers(), p);
        StringBuilder acc = getPrinter();
        Literal.ModifiedUtf8Surrogate modifiedUtf8Surrogate = literal.getModifiedUtf8Surrogate();
        if (modifiedUtf8Surrogate != null) {
            acc.append(modifiedUtf8Surrogate.getEscapeSequence()).append(modifiedUtf8Surrogate.getCodePoint());
        } else {
            acc.append(literal.getValueSource());
        }
        return literal;
    }

    @Override
    public J visitMemberReference(MemberReference memberRef, P p) {
        visitSpace(memberRef.getPrefix(), Space.Location.MEMBER_REFERENCE_PREFIX, p);
        visitMarkers(memberRef.getMarkers(), p);
        visit(memberRef.getContaining(), p);
        visitContainer("<", memberRef.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
        visitLeftPadded("::", memberRef.getPadding().getReference(), JLeftPadded.Location.MEMBER_REFERENCE_NAME, p);
        return memberRef;
    }

    @Override
    public J visitMethodDeclaration(MethodDeclaration method, P p) {
        visitSpace(method.getPrefix(), Space.Location.METHOD_DECLARATION_PREFIX, p);
        visitMarkers(method.getMarkers(), p);
        visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
        visit(method.getLeadingAnnotations(), p);
        visitModifiers(method.getModifiers(), p);
        TypeParameters typeParameters = method.getAnnotations().getTypeParameters();
        if (typeParameters != null) {
            visit(typeParameters.getAnnotations(), p);
            visitSpace(typeParameters.getPrefix(), Space.Location.TYPE_PARAMETERS, p);
            visitMarkers(typeParameters.getMarkers(), p);
            StringBuilder acc = getPrinter();
            acc.append("<");
            visitRightPadded(typeParameters.getPadding().getTypeParameters(), JRightPadded.Location.TYPE_PARAMETER, ",", p);
            acc.append(">");
        }
        visit(method.getReturnTypeExpression(), p);
        visit(method.getAnnotations().getName().getAnnotations(), p);
        visit(method.getName(), p);
        visitContainer("(", method.getPadding().getParameters(), JContainer.Location.METHOD_DECLARATION_PARAMETERS, ",", ")", p);
        visitContainer("throws", method.getPadding().getThrows(), JContainer.Location.THROWS, ",", null, p);
        visit(method.getBody(), p);
        visitLeftPadded("default", method.getPadding().getDefaultValue(), JLeftPadded.Location.METHOD_DECLARATION_DEFAULT_VALUE, p);
        return method;
    }

    @Override
    public J visitMethodInvocation(MethodInvocation method, P p) {
        visitSpace(method.getPrefix(), Space.Location.METHOD_INVOCATION_PREFIX, p);
        visitMarkers(method.getMarkers(), p);
        visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, ".", p);
        visitContainer("<", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
        visit(method.getName(), p);
        visitContainer("(", method.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, ",", ")", p);
        return method;
    }

    @Override
    public J visitMultiCatch(MultiCatch multiCatch, P p) {
        visitSpace(multiCatch.getPrefix(), Space.Location.MULTI_CATCH_PREFIX, p);
        visitMarkers(multiCatch.getMarkers(), p);
        visitRightPadded(multiCatch.getPadding().getAlternatives(), JRightPadded.Location.CATCH_ALTERNATIVE, "|", p);
        return multiCatch;
    }

    @Override
    public J visitVariableDeclarations(VariableDeclarations multiVariable, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(multiVariable.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
        visitMarkers(multiVariable.getMarkers(), p);
        visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
        visit(multiVariable.getLeadingAnnotations(), p);
        visitModifiers(multiVariable.getModifiers(), p);
        visit(multiVariable.getTypeExpression(), p);
        for (JLeftPadded<Space> dim : multiVariable.getDimensionsBeforeName()) {
            visitSpace(dim.getBefore(), Space.Location.DIMENSION_PREFIX, p);
            acc.append('[');
            visitSpace(dim.getElement(), Space.Location.DIMENSION, p);
            acc.append(']');
        }
        if (multiVariable.getVarargs() != null) {
            visitSpace(multiVariable.getVarargs(), Space.Location.VARARGS, p);
            acc.append("...");
        }
        visitRightPadded(multiVariable.getPadding().getVariables(), JRightPadded.Location.NAMED_VARIABLE, ",", p);
        return multiVariable;
    }

    @Override
    public J visitNewArray(NewArray newArray, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(newArray.getPrefix(), Space.Location.NEW_ARRAY_PREFIX, p);
        visitMarkers(newArray.getMarkers(), p);
        if (newArray.getTypeExpression() != null) {
            acc.append("new");
        }
        visit(newArray.getTypeExpression(), p);
        visit(newArray.getDimensions(), p);
        visitContainer("{", newArray.getPadding().getInitializer(), JContainer.Location.NEW_ARRAY_INITIALIZER, ",", "}", p);
        return newArray;
    }

    @Override
    public J visitNewClass(NewClass newClass, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(newClass.getPrefix(), Space.Location.NEW_CLASS_PREFIX, p);
        visitMarkers(newClass.getMarkers(), p);
        visitRightPadded(newClass.getPadding().getEnclosing(), JRightPadded.Location.NEW_CLASS_ENCLOSING, ".", p);
        visitSpace(newClass.getNew(), Space.Location.NEW_PREFIX, p);
        acc.append("new");
        visit(newClass.getClazz(), p);
        visitContainer("(", newClass.getArguments(), JContainer.Location.NEW_CLASS_ARGUMENTS, ",", ")", p);
        visit(newClass.getBody(), p);
        return newClass;
    }

    @Override
    public J visitPackage(J.Package pkg, P p) {
        StringBuilder acc = getPrinter();
        pkg.getAnnotations().forEach(a -> visitAnnotation(a, p));
        visitSpace(pkg.getPrefix(), Space.Location.PACKAGE_PREFIX, p);
        visitMarkers(pkg.getMarkers(), p);
        acc.append("package");
        visit(pkg.getExpression(), p);
        return pkg;
    }

    @Override
    public J visitParameterizedType(ParameterizedType type, P p) {
        visitSpace(type.getPrefix(), Space.Location.PARAMETERIZED_TYPE_PREFIX, p);
        visitMarkers(type.getMarkers(), p);
        visit(type.getClazz(), p);
        visitContainer("<", type.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
        return type;
    }

    @Override
    public J visitPrimitive(Primitive primitive, P p) {
        String keyword;
        switch (primitive.getType()) {
            case Boolean:
                keyword = "boolean";
                break;
            case Byte:
                keyword = "byte";
                break;
            case Char:
                keyword = "char";
                break;
            case Double:
                keyword = "double";
                break;
            case Float:
                keyword = "float";
                break;
            case Int:
                keyword = "int";
                break;
            case Long:
                keyword = "long";
                break;
            case Short:
                keyword = "short";
                break;
            case Void:
                keyword = "void";
                break;
            case String:
                keyword = "String";
                break;
            case Wildcard:
                keyword = "*";
                break;
            case None:
                throw new IllegalStateException("Unable to print None primitive");
            case Null:
                throw new IllegalStateException("Unable to print Null primitive");
            default:
                throw new IllegalStateException("Unable to print non-primitive type");
        }
        StringBuilder acc = getPrinter();
        visitSpace(primitive.getPrefix(), Space.Location.PRIMITIVE_PREFIX, p);
        visitMarkers(primitive.getMarkers(), p);
        acc.append(keyword);
        return primitive;
    }

    @Override
    public <T extends J> J visitParentheses(Parentheses<T> parens, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(parens.getPrefix(), Space.Location.PARENTHESES_PREFIX, p);
        visitMarkers(parens.getMarkers(), p);
        acc.append("(");
        visitRightPadded(parens.getPadding().getTree(), JRightPadded.Location.PARENTHESES, ")", p);
        return parens;
    }

    @Override
    public J visitReturn(Return retrn, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(retrn.getPrefix(), Space.Location.RETURN_PREFIX, p);
        visitMarkers(retrn.getMarkers(), p);
        acc.append("return");
        visit(retrn.getExpression(), p);
        return retrn;
    }

    @Override
    public J visitSwitch(Switch switzh, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(switzh.getPrefix(), Space.Location.SWITCH_PREFIX, p);
        visitMarkers(switzh.getMarkers(), p);
        acc.append("switch");
        visit(switzh.getSelector(), p);
        visit(switzh.getCases(), p);
        return switzh;
    }

    @Override
    public J visitSynchronized(J.Synchronized synch, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(synch.getPrefix(), Space.Location.SYNCHRONIZED_PREFIX, p);
        visitMarkers(synch.getMarkers(), p);
        acc.append("synchronized");
        visit(synch.getLock(), p);
        visit(synch.getBody(), p);
        return synch;
    }

    @Override
    public J visitTernary(Ternary ternary, P p) {
        visitSpace(ternary.getPrefix(), Space.Location.TERNARY_PREFIX, p);
        visitMarkers(ternary.getMarkers(), p);
        visit(ternary.getCondition(), p);
        visitLeftPadded("?", ternary.getPadding().getTruePart(), JLeftPadded.Location.TERNARY_TRUE, p);
        visitLeftPadded(":", ternary.getPadding().getFalsePart(), JLeftPadded.Location.TERNARY_FALSE, p);
        return ternary;
    }

    @Override
    public J visitThrow(Throw thrown, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(thrown.getPrefix(), Space.Location.THROW_PREFIX, p);
        visitMarkers(thrown.getMarkers(), p);
        acc.append("throw");
        visit(thrown.getException(), p);
        return thrown;
    }

    @Override
    public J visitTry(Try tryable, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(tryable.getPrefix(), Space.Location.TRY_PREFIX, p);
        visitMarkers(tryable.getMarkers(), p);
        acc.append("try");
        if (tryable.getPadding().getResources() != null) {
            visitSpace(tryable.getPadding().getResources().getBefore(), Space.Location.TRY_RESOURCES, p);
            acc.append('(');
            List<JRightPadded<Try.Resource>> resources = tryable.getPadding().getResources().getPadding().getElements();
            for (int i = 0; i < resources.size(); i++) {
                JRightPadded<Try.Resource> resource = resources.get(i);

                visitSpace(resource.getElement().getPrefix(), Space.Location.TRY_RESOURCE, p);
                visit(resource.getElement().getVariableDeclarations(), p);

                if (i < resources.size() - 1 || resource.getElement().isTerminatedWithSemicolon()) {
                    acc.append(';');
                }

                visitSpace(resource.getAfter(), Space.Location.TRY_RESOURCE_SUFFIX, p);
            }
            acc.append(')');
        }

        visit(tryable.getBody(), p);
        visit(tryable.getCatches(), p);
        visitLeftPadded("finally", tryable.getPadding().getFinally(), JLeftPadded.Location.TRY_FINALLY, p);
        return tryable;
    }

    @Override
    public J visitTypeParameter(TypeParameter typeParam, P p) {
        visitSpace(typeParam.getPrefix(), Space.Location.TYPE_PARAMETERS_PREFIX, p);
        visitMarkers(typeParam.getMarkers(), p);
        visit(typeParam.getAnnotations(), p);
        visit(typeParam.getName(), p);
        visitContainer("extends", typeParam.getPadding().getBounds(), JContainer.Location.TYPE_BOUNDS, "&", "", p);
        return typeParam;
    }

    @Override
    public J visitUnary(Unary unary, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(unary.getPrefix(), Space.Location.UNARY_PREFIX, p);
        visitMarkers(unary.getMarkers(), p);
        switch (unary.getOperator()) {
            case PreIncrement:
                acc.append("++");
                visit(unary.getExpression(), p);
                break;
            case PreDecrement:
                acc.append("--");
                visit(unary.getExpression(), p);
                break;
            case PostIncrement:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                acc.append("++");
                break;
            case PostDecrement:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                acc.append("--");
                break;
            case Positive:
                acc.append("+");
                visit(unary.getExpression(), p);
                break;
            case Negative:
                acc.append("-");
                visit(unary.getExpression(), p);
                break;
            case Complement:
                acc.append("~");
                visit(unary.getExpression(), p);
                break;
            case Not:
            default:
                acc.append("!");
                visit(unary.getExpression(), p);
        }
        return unary;
    }

    @Override
    public J visitVariable(VariableDeclarations.NamedVariable variable, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(variable.getPrefix(), Space.Location.VARIABLE_PREFIX, p);
        visitMarkers(variable.getMarkers(), p);
        visit(variable.getName(), p);
        for (JLeftPadded<Space> dimension : variable.getDimensionsAfterName()) {
            visitSpace(dimension.getBefore(), Space.Location.DIMENSION_PREFIX, p);
            acc.append('[');
            visitSpace(dimension.getElement(), Space.Location.DIMENSION, p);
            acc.append(']');
        }
        visitLeftPadded("=", variable.getPadding().getInitializer(), JLeftPadded.Location.VARIABLE_INITIALIZER, p);
        return variable;
    }

    @Override
    public J visitWhileLoop(WhileLoop whileLoop, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(whileLoop.getPrefix(), Space.Location.WHILE_PREFIX, p);
        visitMarkers(whileLoop.getMarkers(), p);
        acc.append("while");
        visit(whileLoop.getCondition(), p);
        visitStatement(whileLoop.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, p);
        return whileLoop;
    }

    @Override
    public J visitWildcard(Wildcard wildcard, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(wildcard.getPrefix(), Space.Location.WILDCARD_PREFIX, p);
        visitMarkers(wildcard.getMarkers(), p);
        acc.append('?');
        if (wildcard.getPadding().getBound() != null) {
            //noinspection ConstantConditions
            switch (wildcard.getBound()) {
                case Extends:
                    visitSpace(wildcard.getPadding().getBound().getBefore(), Space.Location.WILDCARD_BOUND, p);
                    acc.append("extends");
                    break;
                case Super:
                    visitSpace(wildcard.getPadding().getBound().getBefore(), Space.Location.WILDCARD_BOUND, p);
                    acc.append("super");
                    break;
            }
        }
        visit(wildcard.getBoundedType(), p);
        return wildcard;
    }
}
