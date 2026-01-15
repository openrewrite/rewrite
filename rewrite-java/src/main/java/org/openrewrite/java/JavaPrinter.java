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
 * See the License for the specifiJ language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.java.marker.*;
import org.openrewrite.java.tree.*;
import org.openrewrite.java.tree.J.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

public class JavaPrinter<P> extends JavaVisitor<PrintOutputCapture<P>> {
    protected void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, JRightPadded.Location location, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            JRightPadded<? extends J> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), location.getAfterLocation(), p);
            visitMarkers(node.getMarkers(), p);
            if (i < nodes.size() - 1) {
                p.append(suffixBetween);
            }
        }
    }

    protected void visitContainer(String before, @Nullable JContainer<? extends J> container, JContainer.Location location,
                                  String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        beforeSyntax(container.getBefore(), container.getMarkers(), location.getBeforeLocation(), p);
        p.append(before);
        visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
        afterSyntax(container.getMarkers(), p);
        p.append(after == null ? "" : after);
    }

    @Override
    @SuppressWarnings("ForLoopReplaceableByForEach")
    public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
        p.append(space.getWhitespace());

        List<Comment> comments = space.getComments();
        for (int i = 0; i < comments.size(); i++) {
            Comment comment = comments.get(i);
            visitMarkers(comment.getMarkers(), p);
            comment.printComment(getCursor(), p);
            p.append(comment.getSuffix());
        }
        return space;
    }

    protected void visitLeftPadded(@Nullable String prefix, @Nullable JLeftPadded<? extends J> leftPadded, JLeftPadded.Location location, PrintOutputCapture<P> p) {
        if (leftPadded != null) {
            beforeSyntax(leftPadded.getBefore(), leftPadded.getMarkers(), location.getBeforeLocation(), p);
            if (prefix != null) {
                p.append(prefix);
            }
            visit(leftPadded.getElement(), p);
            afterSyntax(leftPadded.getMarkers(), p);
        }
    }

    protected void visitRightPadded(@Nullable JRightPadded<? extends J> rightPadded, JRightPadded.Location location, @Nullable String suffix, PrintOutputCapture<P> p) {
        if (rightPadded != null) {
            beforeSyntax(Space.EMPTY, rightPadded.getMarkers(), null, p);
            visit(rightPadded.getElement(), p);
            afterSyntax(rightPadded.getMarkers(), p);
            visitSpace(rightPadded.getAfter(), location.getAfterLocation(), p);
            if (suffix != null) {
                p.append(suffix);
            }
        }
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        if (marker instanceof TrailingComma) {
            p.append(',');
            visitSpace(((TrailingComma) marker).getSuffix(), Space.Location.TRAILING_COMMA_SUFFIX, p);
        }
        //noinspection unchecked
        return (M) marker;
    }

    @Override
    public J visitModifier(Modifier mod, PrintOutputCapture<P> p) {
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
            case NonSealed:
                keyword = "non-sealed";
                break;
            case Sealed:
                keyword = "sealed";
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
            case Async:
                keyword = "async";
                break;
            case Reified:
                keyword = "reified";
                break;
            case Inline:
                keyword = "inline";
                break;
            case LanguageExtension:
                keyword = mod.getKeyword();
                break;
        }
        beforeSyntax(mod, Space.Location.MODIFIER_PREFIX, p);
        p.append(keyword);
        afterSyntax(mod, p);
        return mod;
    }

    @Override
    public J visitAnnotation(Annotation annotation, PrintOutputCapture<P> p) {
        beforeSyntax(annotation, Space.Location.ANNOTATION_PREFIX, p);
        p.append('@');
        visit(annotation.getAnnotationType(), p);
        visitContainer("(", annotation.getPadding().getArguments(), JContainer.Location.ANNOTATION_ARGUMENTS, ",", ")", p);
        afterSyntax(annotation, p);
        return annotation;
    }

    @Override
    public J visitAnnotatedType(AnnotatedType annotatedType, PrintOutputCapture<P> p) {
        beforeSyntax(annotatedType, Space.Location.ANNOTATED_TYPE_PREFIX, p);
        visit(annotatedType.getAnnotations(), p);
        visit(annotatedType.getTypeExpression(), p);
        afterSyntax(annotatedType, p);
        return annotatedType;
    }

    @Override
    public J visitArrayDimension(ArrayDimension arrayDimension, PrintOutputCapture<P> p) {
        beforeSyntax(arrayDimension, Space.Location.DIMENSION_PREFIX, p);
        p.append('[');
        visitRightPadded(arrayDimension.getPadding().getIndex(), JRightPadded.Location.ARRAY_INDEX, "]", p);
        afterSyntax(arrayDimension, p);
        return arrayDimension;
    }

    @Override
    public J visitArrayType(ArrayType arrayType, PrintOutputCapture<P> p) {
        beforeSyntax(arrayType, Space.Location.ARRAY_TYPE_PREFIX, p);
        TypeTree type = arrayType;
        while (type instanceof ArrayType) {
            type = ((ArrayType) type).getElementType();
        }
        visit(type, p);
        visit(arrayType.getAnnotations(), p);
        if (arrayType.getDimension() != null) {
            visitSpace(arrayType.getDimension().getBefore(), Space.Location.DIMENSION_PREFIX, p);
            if (arrayType.getMarkers().findFirst(Varargs.class).isPresent()) {
                // Print varargs syntax
                p.append("...");
            } else {
                // Print regular array brackets
                p.append('[');
                visitSpace(arrayType.getDimension().getElement(), Space.Location.DIMENSION, p);
                p.append(']');
            }

            if (arrayType.getElementType() instanceof J.ArrayType) {
                printDimensions((ArrayType) arrayType.getElementType(), p);
            }
        }
        afterSyntax(arrayType, p);
        return arrayType;
    }

    private void printDimensions(J.ArrayType arrayType, PrintOutputCapture<P> p) {
        beforeSyntax(arrayType, Space.Location.ARRAY_TYPE_PREFIX, p);
        visit(arrayType.getAnnotations(), p);
        visitSpace(arrayType.getDimension().getBefore(), Space.Location.DIMENSION_PREFIX, p);
        if (arrayType.getMarkers().findFirst(Varargs.class).isPresent()) {
            // Print varargs syntax
            p.append("...");
        } else {
            // Print regular array brackets
            p.append('[');
            visitSpace(arrayType.getDimension().getElement(), Space.Location.DIMENSION, p);
            p.append(']');
        }
        if (arrayType.getElementType() instanceof J.ArrayType) {
            printDimensions((ArrayType) arrayType.getElementType(), p);
        }
        afterSyntax(arrayType, p);
    }

    @Override
    public J visitAssert(Assert assert_, PrintOutputCapture<P> p) {
        beforeSyntax(assert_, Space.Location.ASSERT_PREFIX, p);
        p.append("assert");
        visit(assert_.getCondition(), p);
        visitLeftPadded(":", assert_.getDetail(), JLeftPadded.Location.ASSERT_DETAIL, p);
        afterSyntax(assert_, p);
        return assert_;
    }

    @Override
    public J visitAssignment(Assignment assignment, PrintOutputCapture<P> p) {
        beforeSyntax(assignment, Space.Location.ASSIGNMENT_PREFIX, p);
        visit(assignment.getVariable(), p);
        visitLeftPadded("=", assignment.getPadding().getAssignment(), JLeftPadded.Location.ASSIGNMENT, p);
        afterSyntax(assignment, p);
        return assignment;
    }

    @Override
    public J visitAssignmentOperation(AssignmentOperation assignOp, PrintOutputCapture<P> p) {
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
        beforeSyntax(assignOp, Space.Location.ASSIGNMENT_OPERATION_PREFIX, p);
        visit(assignOp.getVariable(), p);
        visitSpace(assignOp.getPadding().getOperator().getBefore(), Space.Location.ASSIGNMENT_OPERATION_OPERATOR, p);
        p.append(keyword);
        visit(assignOp.getAssignment(), p);
        afterSyntax(assignOp, p);
        return assignOp;
    }

    @Override
    public J visitBinary(Binary binary, PrintOutputCapture<P> p) {
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
        beforeSyntax(binary, Space.Location.BINARY_PREFIX, p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), Space.Location.BINARY_OPERATOR, p);
        p.append(keyword);
        visit(binary.getRight(), p);
        afterSyntax(binary, p);
        return binary;
    }

    @Override
    public J visitBlock(Block block, PrintOutputCapture<P> p) {
        beforeSyntax(block, Space.Location.BLOCK_PREFIX, p);

        if (block.isStatic()) {
            p.append("static");
            visitRightPadded(block.getPadding().getStatic(), JRightPadded.Location.STATIC_INIT, p);
        }

        boolean omitBraces = block.getMarkers().findFirst(OmitBraces.class).isPresent();
        if (!omitBraces) {
            p.append("{");
        }
        visitStatements(block.getPadding().getStatements(), JRightPadded.Location.BLOCK_STATEMENT, p);
        visitSpace(block.getEnd(), Space.Location.BLOCK_END, p);
        if (!omitBraces) {
            p.append("}");
        }
        afterSyntax(block, p);
        return block;
    }

    protected void visitStatements(List<JRightPadded<Statement>> statements, JRightPadded.Location location, PrintOutputCapture<P> p) {
        for (JRightPadded<Statement> paddedStat : statements) {
            visitStatement(paddedStat, location, p);
        }
    }

    protected void visitStatement(@Nullable JRightPadded<Statement> paddedStat, JRightPadded.Location location, PrintOutputCapture<P> p) {
        if (paddedStat == null) {
            return;
        }

        visit(paddedStat.getElement(), p);
        visitSpace(paddedStat.getAfter(), location.getAfterLocation(), p);
        visitMarkers(paddedStat.getMarkers(), p);
        printStatementTerminator(paddedStat.getElement(), p);
    }

    protected void printStatementTerminator(Statement s, PrintOutputCapture<P> p) {
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
                    s instanceof VariableDeclarations ||
                    s instanceof Yield) {
                p.append(';');
                return;
            }

            if (s instanceof MethodDeclaration && ((MethodDeclaration) s).getBody() == null) {
                if (!hasError(s))
                    p.append(';');
                return;
            }

            if (s instanceof Label) {
                s = ((Label) s).getStatement();
                continue;
            }

            if (getCursor().getValue() instanceof Case) {
                Object aSwitch =
                        getCursor()
                                .dropParentUntil(
                                        c -> c instanceof Switch ||
                                                c instanceof SwitchExpression ||
                                                c == Cursor.ROOT_VALUE
                                )
                                .getValue();
                if (aSwitch instanceof SwitchExpression) {
                    Case aCase = getCursor().getValue();
                    if (!(aCase.getBody() instanceof Block || s instanceof Block)) {
                        p.append(';');
                    }
                    return;
                }
            }

            return;
        }
    }

    private static boolean hasError(Tree tree) {
        AtomicBoolean isError = new AtomicBoolean(false);
        new JavaIsoVisitor<AtomicBoolean>() {

            @Override
            public Erroneous visitErroneous(Erroneous erroneous, AtomicBoolean atomicBoolean) {
                atomicBoolean.set(true);
                return erroneous;
            }
        }.visit(tree, isError);
        return isError.get();
    }

    @Override
    public J visitBreak(Break breakStatement, PrintOutputCapture<P> p) {
        beforeSyntax(breakStatement, Space.Location.BREAK_PREFIX, p);
        p.append("break");
        visit(breakStatement.getLabel(), p);
        afterSyntax(breakStatement, p);
        return breakStatement;
    }

    @Override
    public J visitCase(Case case_, PrintOutputCapture<P> p) {
        beforeSyntax(case_, Space.Location.CASE_PREFIX, p);
        J elem = case_.getCaseLabels().get(0);
        if (!(elem instanceof Identifier) || !"default".equals(((Identifier) elem).getSimpleName())) {
            p.append("case");
        }
        visitContainer("", case_.getPadding().getCaseLabels(), JContainer.Location.CASE_LABEL, ",", "", p);
        if (case_.getGuard() != null) {
            p.append("when");
            visit(case_.getGuard(), p);
        }
        visitSpace(case_.getPadding().getStatements().getBefore(), Space.Location.CASE, p);
        p.append(case_.getType() == Case.Type.Statement ? ":" : "->");
        visitStatements(case_.getPadding().getStatements().getPadding()
                .getElements(), JRightPadded.Location.CASE, p);
        if (case_.getBody() instanceof Statement) {
            //noinspection unchecked
            visitStatement((JRightPadded<Statement>) (JRightPadded<?>) case_.getPadding().getBody(),
                    JRightPadded.Location.CASE_BODY, p);
        } else {
            visitRightPadded(case_.getPadding().getBody(), JRightPadded.Location.CASE_BODY, ";", p);
        }
        afterSyntax(case_, p);
        return case_;
    }

    @Override
    public J visitCatch(Try.Catch catch_, PrintOutputCapture<P> p) {
        beforeSyntax(catch_, Space.Location.CATCH_PREFIX, p);
        p.append("catch");
        visit(catch_.getParameter(), p);
        visit(catch_.getBody(), p);
        afterSyntax(catch_, p);
        return catch_;
    }

    @Override
    public J visitClassDeclaration(ClassDeclaration classDecl, PrintOutputCapture<P> p) {
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
            case Record:
                kind = "record";
                break;
        }

        if (classDecl.getMarkers().findFirst(CompactSourceFile.class).isPresent()) {
            beforeSyntax(classDecl, Space.Location.CLASS_DECLARATION_PREFIX, p);
            visit(classDecl.getBody(), p);
            afterSyntax(classDecl, p);
            return classDecl;
        }

        beforeSyntax(classDecl, Space.Location.CLASS_DECLARATION_PREFIX, p);
        visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
        visit(classDecl.getLeadingAnnotations(), p);
        for (Modifier m : classDecl.getModifiers()) {
            visitModifier(m, p);
        }
        visit(classDecl.getPadding().getKind().getAnnotations(), p);
        visitSpace(classDecl.getPadding().getKind().getPrefix(), Space.Location.CLASS_KIND, p);
        p.append(kind);
        visit(classDecl.getName(), p);
        visitContainer("<", classDecl.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
        visitContainer("(", classDecl.getPadding().getPrimaryConstructor(), JContainer.Location.RECORD_STATE_VECTOR, ",", ")", p);
        visitLeftPadded("extends", classDecl.getPadding().getExtends(), JLeftPadded.Location.EXTENDS, p);
        visitContainer(classDecl.getKind() == ClassDeclaration.Kind.Type.Interface ? "extends" : "implements",
                classDecl.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, ",", null, p);
        visitContainer("permits", classDecl.getPadding().getPermits(), JContainer.Location.PERMITS, ",", null, p);
        visit(classDecl.getBody(), p);
        afterSyntax(classDecl, p);
        return classDecl;
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, PrintOutputCapture<P> p) {
        beforeSyntax(cu, Space.Location.COMPILATION_UNIT_PREFIX, p);
        visitRightPadded(cu.getPadding().getPackageDeclaration(), JRightPadded.Location.PACKAGE, ";", p);
        visitRightPadded(cu.getPadding().getImports(), JRightPadded.Location.IMPORT, ";", p);
        if (!cu.getImports().isEmpty()) {
            p.append(';');
        }
        visit(cu.getClasses(), p);
        afterSyntax(cu, p);
        visitSpace(cu.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        return cu;
    }

    @Override
    public J visitContinue(Continue continueStatement, PrintOutputCapture<P> p) {
        beforeSyntax(continueStatement, Space.Location.CONTINUE_PREFIX, p);
        p.append("continue");
        visit(continueStatement.getLabel(), p);
        afterSyntax(continueStatement, p);
        return continueStatement;
    }

    @Override
    public <T extends J> J visitControlParentheses(ControlParentheses<T> controlParens, PrintOutputCapture<P> p) {
        beforeSyntax(controlParens, Space.Location.CONTROL_PARENTHESES_PREFIX, p);
        p.append('(');
        visitRightPadded(controlParens.getPadding().getTree(), JRightPadded.Location.PARENTHESES, ")", p);
        afterSyntax(controlParens, p);
        return controlParens;
    }

    @Override
    public J visitDoWhileLoop(DoWhileLoop doWhileLoop, PrintOutputCapture<P> p) {
        beforeSyntax(doWhileLoop, Space.Location.DO_WHILE_PREFIX, p);
        p.append("do");
        visitStatement(doWhileLoop.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, p);
        visitLeftPadded("while", doWhileLoop.getPadding().getWhileCondition(), JLeftPadded.Location.WHILE_CONDITION, p);
        afterSyntax(doWhileLoop, p);
        return doWhileLoop;
    }

    @Override
    public J visitElse(If.Else else_, PrintOutputCapture<P> p) {
        beforeSyntax(else_, Space.Location.ELSE_PREFIX, p);
        p.append("else");
        visitStatement(else_.getPadding().getBody(), JRightPadded.Location.IF_ELSE, p);
        afterSyntax(else_, p);
        return else_;
    }

    @Override
    public J visitEmpty(J.Empty empty, PrintOutputCapture<P> p) {
        beforeSyntax(empty, Space.Location.EMPTY_PREFIX, p);
        afterSyntax(empty, p);
        return empty;
    }

    @Override
    public J visitEnumValue(EnumValue enum_, PrintOutputCapture<P> p) {
        beforeSyntax(enum_, Space.Location.ENUM_VALUE_PREFIX, p);
        visit(enum_.getAnnotations(), p);
        visit(enum_.getName(), p);
        NewClass initializer = enum_.getInitializer();
        if (enum_.getInitializer() != null) {
            visitSpace(initializer.getPrefix(), Space.Location.NEW_CLASS_PREFIX, p);
            visitSpace(initializer.getNew(), Space.Location.NEW_PREFIX, p);
            if (!initializer.getPadding().getArguments().getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                visitContainer("(", initializer.getPadding().getArguments(), JContainer.Location.NEW_CLASS_ARGUMENTS, ",", ")", p);
            }
            visit(initializer.getBody(), p);
        }
        afterSyntax(enum_, p);
        return enum_;
    }

    @Override
    public J visitEnumValueSet(EnumValueSet enums, PrintOutputCapture<P> p) {
        beforeSyntax(enums, Space.Location.ENUM_VALUE_SET_PREFIX, p);
        visitRightPadded(enums.getPadding().getEnums(), JRightPadded.Location.ENUM_VALUE, ",", p);
        if (enums.isTerminatedWithSemicolon()) {
            p.append(';');
        }
        afterSyntax(enums, p);
        return enums;
    }

    @Override
    public J visitFieldAccess(FieldAccess fieldAccess, PrintOutputCapture<P> p) {
        beforeSyntax(fieldAccess, Space.Location.FIELD_ACCESS_PREFIX, p);
        visit(fieldAccess.getTarget(), p);
        visitLeftPadded(".", fieldAccess.getPadding().getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p);
        afterSyntax(fieldAccess, p);
        return fieldAccess;
    }

    @Override
    public J visitForLoop(ForLoop forLoop, PrintOutputCapture<P> p) {
        beforeSyntax(forLoop, Space.Location.FOR_PREFIX, p);
        p.append("for");
        ForLoop.Control ctrl = forLoop.getControl();
        visitSpace(ctrl.getPrefix(), Space.Location.FOR_CONTROL_PREFIX, p);
        p.append('(');
        visitRightPadded(ctrl.getPadding().getInit(), JRightPadded.Location.FOR_INIT, ",", p);
        p.append(';');
        visitRightPadded(ctrl.getPadding().getCondition(), JRightPadded.Location.FOR_CONDITION, ";", p);
        visitRightPadded(ctrl.getPadding().getUpdate(), JRightPadded.Location.FOR_UPDATE, ",", p);
        p.append(')');
        visitStatement(forLoop.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p);
        afterSyntax(forLoop, p);
        return forLoop;
    }

    @Override
    public J visitForEachLoop(ForEachLoop forEachLoop, PrintOutputCapture<P> p) {
        beforeSyntax(forEachLoop, Space.Location.FOR_EACH_LOOP_PREFIX, p);
        p.append("for");
        ForEachLoop.Control ctrl = forEachLoop.getControl();
        visitSpace(ctrl.getPrefix(), Space.Location.FOR_EACH_CONTROL_PREFIX, p);
        p.append('(');
        visitRightPadded(ctrl.getPadding().getVariable(), JRightPadded.Location.FOREACH_VARIABLE, ":", p);
        visitRightPadded(ctrl.getPadding().getIterable(), JRightPadded.Location.FOREACH_ITERABLE, "", p);
        p.append(')');
        visitStatement(forEachLoop.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p);
        afterSyntax(forEachLoop, p);
        return forEachLoop;
    }

    @Override
    public J visitIdentifier(Identifier ident, PrintOutputCapture<P> p) {
        visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
        visit(ident.getAnnotations(), p);
        beforeSyntax(ident, Space.Location.IDENTIFIER_PREFIX, p);
        p.append(ident.getSimpleName());
        afterSyntax(ident, p);
        return ident;
    }

    @Override
    public J visitIf(If iff, PrintOutputCapture<P> p) {
        beforeSyntax(iff, Space.Location.IF_PREFIX, p);
        p.append("if");
        visit(iff.getIfCondition(), p);
        visitStatement(iff.getPadding().getThenPart(), JRightPadded.Location.IF_THEN, p);
        visit(iff.getElsePart(), p);
        afterSyntax(iff, p);
        return iff;
    }

    @Override
    public J visitImport(J.Import import_, PrintOutputCapture<P> p) {
        beforeSyntax(import_, Space.Location.IMPORT_PREFIX, p);
        p.append("import");
        if (import_.isStatic()) {
            visitSpace(import_.getPadding().getStatic().getBefore(), Space.Location.STATIC_IMPORT, p);
            p.append("static");
        }
        visit(import_.getQualid(), p);
        afterSyntax(import_, p);
        return import_;
    }

    @Override
    public J visitInstanceOf(InstanceOf instanceOf, PrintOutputCapture<P> p) {
        beforeSyntax(instanceOf, Space.Location.INSTANCEOF_PREFIX, p);
        visitRightPadded(instanceOf.getPadding().getExpression(), JRightPadded.Location.INSTANCEOF, "instanceof", p);
        visit(instanceOf.getModifier(), p);
        visit(instanceOf.getClazz(), p);
        visit(instanceOf.getPattern(), p);
        afterSyntax(instanceOf, p);
        return instanceOf;
    }

    @Override
    public J visitDeconstructionPattern(DeconstructionPattern deconstructionPattern, PrintOutputCapture<P> p) {
        beforeSyntax(deconstructionPattern, Space.Location.DECONSTRUCTION_PATTERN_PREFIX, p);
        visitAndCast(deconstructionPattern.getDeconstructor(), p);
        visitContainer("(", deconstructionPattern.getPadding().getNested(), JContainer.Location.DECONSTRUCTION_PATTERN_NESTED, ",", ")", p);
        afterSyntax(deconstructionPattern, p);
        return deconstructionPattern;
    }

    @Override
    public J visitIntersectionType(IntersectionType intersectionType, PrintOutputCapture<P> p) {
        beforeSyntax(intersectionType, Space.Location.INTERSECTION_TYPE_PREFIX, p);
        visitContainer("", intersectionType.getPadding().getBounds(), JContainer.Location.TYPE_BOUNDS, "&", "", p);
        afterSyntax(intersectionType, p);
        return intersectionType;
    }

    @Override
    public J visitLabel(Label label, PrintOutputCapture<P> p) {
        beforeSyntax(label, Space.Location.LABEL_PREFIX, p);
        visitRightPadded(label.getPadding().getLabel(), JRightPadded.Location.LABEL, ":", p);
        visit(label.getStatement(), p);
        afterSyntax(label, p);
        return label;
    }

    @Override
    public J visitLambda(Lambda lambda, PrintOutputCapture<P> p) {
        beforeSyntax(lambda, Space.Location.LAMBDA_PREFIX, p);
        visitSpace(lambda.getParameters().getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, p);
        visitMarkers(lambda.getParameters().getMarkers(), p);
        if (lambda.getParameters().isParenthesized()) {
            p.append('(');
            visitRightPadded(lambda.getParameters().getPadding().getParameters(), JRightPadded.Location.LAMBDA_PARAM, ",", p);
            p.append(')');
        } else {
            visitRightPadded(lambda.getParameters().getPadding().getParameters(), JRightPadded.Location.LAMBDA_PARAM, ",", p);
        }
        visitSpace(lambda.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p);
        p.append("->");
        visit(lambda.getBody(), p);
        afterSyntax(lambda, p);
        return lambda;
    }

    @Override
    public J visitLiteral(Literal literal, PrintOutputCapture<P> p) {
        beforeSyntax(literal, Space.Location.LITERAL_PREFIX, p);
        List<Literal.UnicodeEscape> unicodeEscapes = literal.getUnicodeEscapes();
        if (unicodeEscapes == null) {
            p.append(literal.getValueSource());
        } else if (literal.getValueSource() != null) {
            Iterator<Literal.UnicodeEscape> surrogateIter = unicodeEscapes.iterator();
            Literal.UnicodeEscape surrogate = surrogateIter.hasNext() ?
                    surrogateIter.next() : null;
            int i = 0;
            if (surrogate != null && surrogate.getValueSourceIndex() == 0) {
                p.append("\\u").append(surrogate.getCodePoint());
                if (surrogateIter.hasNext()) {
                    surrogate = surrogateIter.next();
                }
            }

            String valueSource = literal.getValueSource();
            for (int j = 0; j < valueSource.length(); j++) {
                char c = valueSource.charAt(j);
                p.append(c);
                if (surrogate != null && surrogate.getValueSourceIndex() == ++i) {
                    while (surrogate != null && surrogate.getValueSourceIndex() == i) {
                        p.append("\\u").append(surrogate.getCodePoint());
                        surrogate = surrogateIter.hasNext() ? surrogateIter.next() : null;
                    }
                }
            }
        }
        afterSyntax(literal, p);
        return literal;
    }

    @Override
    public J visitMemberReference(MemberReference memberRef, PrintOutputCapture<P> p) {
        beforeSyntax(memberRef, Space.Location.MEMBER_REFERENCE_PREFIX, p);
        visitRightPadded(memberRef.getPadding().getContaining(), JRightPadded.Location.MEMBER_REFERENCE_CONTAINING, p);
        p.append("::");
        visitContainer("<", memberRef.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
        visitLeftPadded("", memberRef.getPadding().getReference(), JLeftPadded.Location.MEMBER_REFERENCE_NAME, p);
        afterSyntax(memberRef, p);
        return memberRef;
    }

    @Override
    public J visitMethodDeclaration(MethodDeclaration method, PrintOutputCapture<P> p) {
        beforeSyntax(method, Space.Location.METHOD_DECLARATION_PREFIX, p);
        visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
        visit(method.getLeadingAnnotations(), p);
        for (Modifier m : method.getModifiers()) {
            visitModifier(m, p);
        }
        visit(method.getAnnotations().getTypeParameters(), p);
        visit(method.getReturnTypeExpression(), p);
        visit(method.getAnnotations().getName().getAnnotations(), p);
        visit(method.getName(), p);
        if (!method.getMarkers().findFirst(CompactConstructor.class).isPresent()) {
            visitContainer("(", method.getPadding().getParameters(), JContainer.Location.METHOD_DECLARATION_PARAMETERS, ",", ")", p);
        }
        visitContainer("throws", method.getPadding().getThrows(), JContainer.Location.THROWS, ",", null, p);
        visit(method.getBody(), p);
        visitLeftPadded("default", method.getPadding().getDefaultValue(), JLeftPadded.Location.METHOD_DECLARATION_DEFAULT_VALUE, p);
        afterSyntax(method, p);
        return method;
    }

    @Override
    public J visitMethodInvocation(MethodInvocation method, PrintOutputCapture<P> p) {
        beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);
        visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, ".", p);
        visitContainer("<", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
        visit(method.getName(), p);
        visitContainer("(", method.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, ",", ")", p);
        afterSyntax(method, p);
        return method;
    }

    @Override
    public J visitMultiCatch(MultiCatch multiCatch, PrintOutputCapture<P> p) {
        beforeSyntax(multiCatch, Space.Location.MULTI_CATCH_PREFIX, p);
        visitRightPadded(multiCatch.getPadding().getAlternatives(), JRightPadded.Location.CATCH_ALTERNATIVE, "|", p);
        afterSyntax(multiCatch, p);
        return multiCatch;
    }

    @Override
    public J visitVariableDeclarations(VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
        beforeSyntax(multiVariable, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
        visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
        visit(multiVariable.getLeadingAnnotations(), p);
        for (Modifier m : multiVariable.getModifiers()) {
            visitModifier(m, p);
        }
        visit(multiVariable.getTypeExpression(), p);
        if (multiVariable.getVarargs() != null) {
            visitSpace(multiVariable.getVarargs(), Space.Location.VARARGS, p);
            p.append("...");
        }
        visitRightPadded(multiVariable.getPadding().getVariables(), JRightPadded.Location.NAMED_VARIABLE, ",", p);
        afterSyntax(multiVariable, p);
        return multiVariable;
    }

    @Override
    public J visitNewArray(NewArray newArray, PrintOutputCapture<P> p) {
        beforeSyntax(newArray, Space.Location.NEW_ARRAY_PREFIX, p);
        if (newArray.getTypeExpression() != null) {
            p.append("new");
        }
        visit(newArray.getTypeExpression(), p);
        visit(newArray.getDimensions(), p);
        visitContainer("{", newArray.getPadding().getInitializer(), JContainer.Location.NEW_ARRAY_INITIALIZER, ",", "}", p);
        afterSyntax(newArray, p);
        return newArray;
    }

    @Override
    public J visitNewClass(NewClass newClass, PrintOutputCapture<P> p) {
        beforeSyntax(newClass, Space.Location.NEW_CLASS_PREFIX, p);
        visitRightPadded(newClass.getPadding().getEnclosing(), JRightPadded.Location.NEW_CLASS_ENCLOSING, ".", p);
        visitSpace(newClass.getNew(), Space.Location.NEW_PREFIX, p);
        p.append("new");
        visit(newClass.getClazz(), p);
        if (!newClass.getPadding().getArguments().getMarkers().findFirst(OmitParentheses.class).isPresent()) {
            visitContainer("(", newClass.getPadding().getArguments(), JContainer.Location.NEW_CLASS_ARGUMENTS, ",", ")", p);
        }
        visit(newClass.getBody(), p);
        afterSyntax(newClass, p);
        return newClass;
    }

    @Override
    public J visitNullableType(J.NullableType nt, PrintOutputCapture<P> p) {
        beforeSyntax(nt, Space.Location.NULLABLE_TYPE_PREFIX, p);
        visit(nt.getTypeTree(), p);
        visitSpace(nt.getPadding().getTypeTree().getAfter(), Space.Location.NULLABLE_TYPE_SUFFIX, p);
        p.append("?");
        afterSyntax(nt, p);
        return nt;
    }

    @Override
    public J visitPackage(J.Package pkg, PrintOutputCapture<P> p) {
        for (Annotation a : pkg.getAnnotations()) {
            visitAnnotation(a, p);
        }
        beforeSyntax(pkg, Space.Location.PACKAGE_PREFIX, p);
        p.append("package");
        visit(pkg.getExpression(), p);
        afterSyntax(pkg, p);
        return pkg;
    }

    @Override
    public J visitParameterizedType(ParameterizedType type, PrintOutputCapture<P> p) {
        beforeSyntax(type, Space.Location.PARAMETERIZED_TYPE_PREFIX, p);
        visit(type.getClazz(), p);
        visitContainer("<", type.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
        afterSyntax(type, p);
        return type;
    }

    @Override
    public J visitPrimitive(Primitive primitive, PrintOutputCapture<P> p) {
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
            case None:
                throw new IllegalStateException("Unable to print None primitive");
            case Null:
                throw new IllegalStateException("Unable to print Null primitive");
            default:
                throw new IllegalStateException("Unable to print non-primitive type");
        }
        beforeSyntax(primitive, Space.Location.PRIMITIVE_PREFIX, p);
        p.append(keyword);
        afterSyntax(primitive, p);
        return primitive;
    }

    @Override
    public <T extends J> J visitParentheses(Parentheses<T> parens, PrintOutputCapture<P> p) {
        beforeSyntax(parens, Space.Location.PARENTHESES_PREFIX, p);
        p.append('(');
        visitRightPadded(parens.getPadding().getTree(), JRightPadded.Location.PARENTHESES, ")", p);
        afterSyntax(parens, p);
        return parens;
    }

    @Override
    public J visitReturn(Return return_, PrintOutputCapture<P> p) {
        beforeSyntax(return_, Space.Location.RETURN_PREFIX, p);
        p.append("return");
        visit(return_.getExpression(), p);
        afterSyntax(return_, p);
        return return_;
    }

    @Override
    public J visitSwitch(Switch switch_, PrintOutputCapture<P> p) {
        beforeSyntax(switch_, Space.Location.SWITCH_PREFIX, p);
        p.append("switch");
        visit(switch_.getSelector(), p);
        visit(switch_.getCases(), p);
        afterSyntax(switch_, p);
        return switch_;
    }

    @Override
    public J visitSwitchExpression(SwitchExpression switch_, PrintOutputCapture<P> p) {
        beforeSyntax(switch_, Space.Location.SWITCH_EXPRESSION_PREFIX, p);
        p.append("switch");
        visit(switch_.getSelector(), p);
        visit(switch_.getCases(), p);
        afterSyntax(switch_, p);
        return switch_;
    }

    @Override
    public J visitSynchronized(J.Synchronized synch, PrintOutputCapture<P> p) {
        beforeSyntax(synch, Space.Location.SYNCHRONIZED_PREFIX, p);
        p.append("synchronized");
        visit(synch.getLock(), p);
        visit(synch.getBody(), p);
        afterSyntax(synch, p);
        return synch;
    }

    @Override
    public J visitTernary(Ternary ternary, PrintOutputCapture<P> p) {
        beforeSyntax(ternary, Space.Location.TERNARY_PREFIX, p);
        visit(ternary.getCondition(), p);
        visitLeftPadded("?", ternary.getPadding().getTruePart(), JLeftPadded.Location.TERNARY_TRUE, p);
        visitLeftPadded(":", ternary.getPadding().getFalsePart(), JLeftPadded.Location.TERNARY_FALSE, p);
        afterSyntax(ternary, p);
        return ternary;
    }

    @Override
    public J visitThrow(Throw thrown, PrintOutputCapture<P> p) {
        beforeSyntax(thrown, Space.Location.THROW_PREFIX, p);
        p.append("throw");
        visit(thrown.getException(), p);
        afterSyntax(thrown, p);
        return thrown;
    }

    @Override
    public J visitTry(Try tryable, PrintOutputCapture<P> p) {
        beforeSyntax(tryable, Space.Location.TRY_PREFIX, p);
        p.append("try");
        if (tryable.getPadding().getResources() != null) {
            //Note: we do not call visitContainer here because the last resource may or may not be semicolon terminated.
            //      Doing this means that visitTryResource is not called, therefore this logiJ must visit the resources.
            visitSpace(tryable.getPadding().getResources().getBefore(), Space.Location.TRY_RESOURCES, p);
            p.append('(');
            List<JRightPadded<Try.Resource>> resources = tryable.getPadding().getResources().getPadding().getElements();
            for (JRightPadded<Try.Resource> resource : resources) {
                visitSpace(resource.getElement().getPrefix(), Space.Location.TRY_RESOURCE, p);
                visitMarkers(resource.getElement().getMarkers(), p);
                visit(resource.getElement().getVariableDeclarations(), p);

                if (resource.getElement().isTerminatedWithSemicolon()) {
                    p.append(';');
                }

                visitSpace(resource.getAfter(), Space.Location.TRY_RESOURCE_SUFFIX, p);
            }
            p.append(')');
        }

        visit(tryable.getBody(), p);
        visit(tryable.getCatches(), p);
        visitLeftPadded("finally", tryable.getPadding().getFinally(), JLeftPadded.Location.TRY_FINALLY, p);
        afterSyntax(tryable, p);
        return tryable;
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast, PrintOutputCapture<P> p) {
        beforeSyntax(typeCast, Space.Location.TYPE_CAST_PREFIX, p);
        visit(typeCast.getClazz(), p);
        visit(typeCast.getExpression(), p);
        afterSyntax(typeCast, p);
        return typeCast;
    }

    @Override
    public J visitTypeParameter(TypeParameter typeParam, PrintOutputCapture<P> p) {
        beforeSyntax(typeParam, Space.Location.TYPE_PARAMETERS_PREFIX, p);
        visit(typeParam.getAnnotations(), p);
        visit(typeParam.getName(), p);
        visitContainer("extends", typeParam.getPadding().getBounds(), JContainer.Location.TYPE_BOUNDS, "&", "", p);
        afterSyntax(typeParam, p);
        return typeParam;
    }

    @Override
    public J visitTypeParameters(TypeParameters typeParameters, PrintOutputCapture<P> p) {
        visit(typeParameters.getAnnotations(), p);
        visitSpace(typeParameters.getPrefix(), Space.Location.TYPE_PARAMETERS, p);
        visitMarkers(typeParameters.getMarkers(), p);
        p.append('<');
        visitRightPadded(typeParameters.getPadding().getTypeParameters(), JRightPadded.Location.TYPE_PARAMETER, ",", p);
        p.append('>');
        return typeParameters;
    }

    @Override
    public J visitUnary(Unary unary, PrintOutputCapture<P> p) {
        beforeSyntax(unary, Space.Location.UNARY_PREFIX, p);
        switch (unary.getOperator()) {
            case PreIncrement:
                p.append("++");
                visit(unary.getExpression(), p);
                break;
            case PreDecrement:
                p.append("--");
                visit(unary.getExpression(), p);
                break;
            case PostIncrement:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                p.append("++");
                break;
            case PostDecrement:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                p.append("--");
                break;
            case Positive:
                p.append('+');
                visit(unary.getExpression(), p);
                break;
            case Negative:
                p.append('-');
                visit(unary.getExpression(), p);
                break;
            case Complement:
                p.append('~');
                visit(unary.getExpression(), p);
                break;
            case Not:
            default:
                p.append('!');
                visit(unary.getExpression(), p);
        }
        afterSyntax(unary, p);
        return unary;
    }

    @Override
    public J visitUnknown(J.Unknown unknown, PrintOutputCapture<P> p) {
        beforeSyntax(unknown, Space.Location.UNKNOWN_PREFIX, p);
        visit(unknown.getSource(), p);
        afterSyntax(unknown, p);
        return unknown;
    }

    @Override
    public J visitUnknownSource(J.Unknown.Source source, PrintOutputCapture<P> p) {
        beforeSyntax(source, Space.Location.UNKNOWN_SOURCE_PREFIX, p);
        p.append(source.getText());
        afterSyntax(source, p);
        return source;
    }

    @Override
    public J visitVariable(VariableDeclarations.NamedVariable variable, PrintOutputCapture<P> p) {
        beforeSyntax(variable, Space.Location.VARIABLE_PREFIX, p);
        visit(variable.getName(), p);
        for (JLeftPadded<Space> dimension : variable.getDimensionsAfterName()) {
            visitSpace(dimension.getBefore(), Space.Location.DIMENSION_PREFIX, p);
            p.append('[');
            visitSpace(dimension.getElement(), Space.Location.DIMENSION, p);
            p.append(']');
        }
        visitLeftPadded("=", variable.getPadding().getInitializer(), JLeftPadded.Location.VARIABLE_INITIALIZER, p);
        afterSyntax(variable, p);
        return variable;
    }

    @Override
    public J visitWhileLoop(WhileLoop whileLoop, PrintOutputCapture<P> p) {
        beforeSyntax(whileLoop, Space.Location.WHILE_PREFIX, p);
        p.append("while");
        visit(whileLoop.getCondition(), p);
        visitStatement(whileLoop.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, p);
        afterSyntax(whileLoop, p);
        return whileLoop;
    }

    @Override
    public J visitWildcard(Wildcard wildcard, PrintOutputCapture<P> p) {
        beforeSyntax(wildcard, Space.Location.WILDCARD_PREFIX, p);
        p.append('?');
        if (wildcard.getPadding().getBound() != null) {
            //noinspection ConstantConditions
            switch (wildcard.getBound()) {
                case Extends:
                    visitSpace(wildcard.getPadding().getBound().getBefore(), Space.Location.WILDCARD_BOUND, p);
                    p.append("extends");
                    break;
                case Super:
                    visitSpace(wildcard.getPadding().getBound().getBefore(), Space.Location.WILDCARD_BOUND, p);
                    p.append("super");
                    break;
            }
        }
        visit(wildcard.getBoundedType(), p);
        afterSyntax(wildcard, p);
        return wildcard;
    }

    @Override
    public J visitYield(Yield yield, PrintOutputCapture<P> p) {
        beforeSyntax(yield, Space.Location.YIELD_PREFIX, p);
        if (!yield.isImplicit()) {
            p.append("yield");
        }
        visit(yield.getValue(), p);
        afterSyntax(yield, p);
        return yield;
    }

    @Override
    public J visitErroneous(Erroneous error, PrintOutputCapture<P> p) {
        beforeSyntax(error, Space.Location.ERRONEOUS, p);
        p.append(error.getText());
        afterSyntax(error, p);
        return error;
    }

    private static final UnaryOperator<String> JAVA_MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    protected void beforeSyntax(J j, Space.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(j.getPrefix(), j.getMarkers(), loc, p);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    protected void beforeSyntax(Space prefix, Markers markers, Space.@Nullable Location loc, PrintOutputCapture<P> p) {
        List<Marker> markersList = markers.getMarkers();
        for (int i = 0; i < markersList.size(); i++) {
            Marker marker = markersList.get(i);
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
        if (loc != null) {
            visitSpace(prefix, loc, p);
        }
        visitMarkers(markers, p);
        for (int i = 0; i < markersList.size(); i++) {
            Marker marker = markersList.get(i);
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
    }

    protected void afterSyntax(J j, PrintOutputCapture<P> p) {
        afterSyntax(j.getMarkers(), p);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    protected void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        List<Marker> markersMarkers = markers.getMarkers();
        for (int i = 0; i < markersMarkers.size(); i++) {
            Marker marker = markersMarkers.get(i);
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), JAVA_MARKER_WRAPPER));
        }
    }
}
