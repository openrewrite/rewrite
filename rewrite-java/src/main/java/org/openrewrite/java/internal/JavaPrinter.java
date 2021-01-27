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
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;

import java.util.List;

import static org.openrewrite.java.tree.J.Modifier.*;

public class JavaPrinter<P> extends JavaVisitor<P> {

    private static final String PRINTER_ACC_KEY = "printed";

    private final TreePrinter<P> treePrinter;

    public JavaPrinter(TreePrinter<P> treePrinter) {
        this.treePrinter = treePrinter;
        setCursoringOn();
    }

    @NonNull
    protected StringBuilder getPrinterAcc() {
        StringBuilder acc = getCursor().getRoot().peekNearestMessage(PRINTER_ACC_KEY);
        if (acc == null) {
            acc = new StringBuilder();
            getCursor().getRoot().putMessage(PRINTER_ACC_KEY, acc);
        }
        return acc;
    }

    public String print(J j, P p) {
        setCursor(new Cursor(null, "EPSILON"));
        visit(j, p);
        return getPrinterAcc().toString();
    }

    @Override
    @Nullable
    public J visit(@Nullable Tree tree, P p) {

        if (tree == null) {
            return defaultValue(null, p);
        }

        StringBuilder printerAcc = getPrinterAcc();
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

    protected void visit(List<? extends JRightPadded<? extends J>> nodes, String suffixBetween, P p) {
        StringBuilder acc = getPrinterAcc();
        for (int i = 0; i < nodes.size(); i++) {
            JRightPadded<? extends J> node = nodes.get(i);
            visit(node.getElem(), p);
            visitSpace(node.getAfter(), p);
            if (i < nodes.size() - 1) {
                acc.append(suffixBetween);
            }
        }
    }

    protected void visit(String before, @Nullable JContainer<? extends J> container, String suffixBetween, @Nullable String after, P p) {
        if (container == null) {
            return;
        }
        StringBuilder acc = getPrinterAcc();
        visitSpace(container.getBefore(), p);
        acc.append(before);
        visit(container.getElem(), suffixBetween, p);
        acc.append(after == null ? "" : after);
    }

    @Override
    public Space visitSpace(Space space, P p) {
        StringBuilder acc = getPrinterAcc();
        acc.append(space.getWhitespace());

        for (Comment comment : space.getComments()) {
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

    protected void visit(@Nullable String prefix, @Nullable JLeftPadded<? extends J> leftPadded, P p) {
        if (leftPadded != null) {
            StringBuilder acc = getPrinterAcc();
            visitSpace(leftPadded.getBefore(), p);
            if (prefix != null) {
                acc.append(prefix);
            }
            visit(leftPadded.getElem(), p);
        }
    }

    protected void visit(@Nullable JRightPadded<? extends J> rightPadded, @Nullable String suffix, P p) {
        if (rightPadded != null) {
            StringBuilder acc = getPrinterAcc();
            visit(rightPadded.getElem(), p);
            visitSpace(rightPadded.getAfter(), p);
            if (suffix != null) {
                acc.append(suffix);
            }
        }
    }

    protected void visitModifiers(Iterable<Modifier> modifiers, P p) {
        StringBuilder acc = getPrinterAcc();
        for (Modifier mod : modifiers) {
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
            visitSpace(mod.getPrefix(), p);
            acc.append(keyword);
        }
    }

    @Override
    public J visitAnnotation(Annotation annotation, P p) {
        visitSpace(annotation.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append("@");
        visit(annotation.getAnnotationType(), p);
        visit("(", annotation.getArgs(), ",", ")", p);
        return annotation;
    }

    @Override
    public J visitArrayDimension(ArrayDimension arrayDimension, P p) {
        visitSpace(arrayDimension.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append("[");
        visit(arrayDimension.getIndex(), "]", p);
        return arrayDimension;
    }

    @Override
    public J visitArrayType(ArrayType arrayType, P p) {
        visitSpace(arrayType.getPrefix(), p);
        visit(arrayType.getElementType(), p);
        StringBuilder acc = getPrinterAcc();
        for (JRightPadded<Space> d : arrayType.getDimensions()) {
            visitSpace(d.getElem(), p);
            acc.append('[');
            visitSpace(d.getAfter(), p);
            acc.append(']');
        }
        return arrayType;
    }

    @Override
    public J visitAssert(Assert azzert, P p) {
        visitSpace(azzert.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append("assert");
        visit(azzert.getCondition(), p);
        return azzert;
    }

    @Override
    public J visitAssign(Assign assign, P p) {
        visitSpace(assign.getPrefix(), p);
        visit(assign.getVariable(), p);
        visit("=", assign.getAssignment(), p);
        return assign;
    }

    @Override
    public J visitAssignOp(AssignOp assignOp, P p) {
        String keyword = "";
        switch (assignOp.getOperator().getElem()) {
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

        visitSpace(assignOp.getPrefix(), p);
        visit(assignOp.getVariable(), p);
        visitSpace(assignOp.getOperator().getBefore(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append(keyword);
        visit(assignOp.getAssignment(), p);
        return assignOp;
    }

    @Override
    public J visitBinary(Binary binary, P p) {
        String keyword = "";
        switch (binary.getOperator().getElem()) {
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
        visitSpace(binary.getPrefix(), p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getOperator().getBefore(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append(keyword);
        visit(binary.getRight(), p);
        return binary;
    }

    @Override
    public J visitBlock(Block block, P p) {
        visitSpace(block.getPrefix(), p);

        StringBuilder acc = getPrinterAcc();

        if (block.getStatic() != null) {
            acc.append("static");
            visitSpace(block.getStatic(), p);
        }

        acc.append('{');
        visitStatements(block.getStatements(), p);
        visitSpace(block.getEnd(), p);
        acc.append('}');
        return block;
    }

    private void visitStatements(List<JRightPadded<Statement>> statements, P p) {
        for (JRightPadded<Statement> paddedStat : statements) {
            visitStatement(paddedStat, p);
        }
    }

    private void visitStatement(@Nullable JRightPadded<Statement> paddedStat, P p) {
        if (paddedStat == null) {
            return;
        }

        visit(paddedStat.getElem(), p);
        visitSpace(paddedStat.getAfter(), p);

        StringBuilder acc = getPrinterAcc();
        Statement s = paddedStat.getElem();
        while (true) {
            if (s instanceof Assert ||
                    s instanceof Assign ||
                    s instanceof AssignOp ||
                    s instanceof Break ||
                    s instanceof Continue ||
                    s instanceof DoWhileLoop ||
                    s instanceof Empty ||
                    s instanceof MethodInvocation ||
                    s instanceof NewClass ||
                    s instanceof Return ||
                    s instanceof Throw ||
                    s instanceof Unary ||
                    s instanceof VariableDecls) {
                acc.append(';');
                return;
            }

            if (s instanceof MethodDecl && ((MethodDecl) s).getBody() == null) {
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
        visitSpace(breakStatement.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append("break");
        visit(breakStatement.getLabel(), p);
        return breakStatement;
    }

    @Override
    public J visitCase(Case caze, P p) {
        visitSpace(caze.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        Expression elem = caze.getPattern();
        if (elem instanceof Ident && ((Ident) elem).getSimpleName().equals("default")) {
            acc.append("default");
        } else {
            acc.append("case");
            visit(elem, p);
        }
        visitSpace(caze.getStatements().getBefore(), p);
        acc.append(':');
        visitStatements(caze.getStatements().getElem(), p);
        return caze;
    }

    @Override
    public J visitCatch(Try.Catch catzh, P p) {
        visitSpace(catzh.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append("catch");
        visit(catzh.getParam(), p);
        visit(catzh.getBody(), p);
        return catzh;
    }

    @Override
    public J visitClassDecl(ClassDecl classDecl, P p) {
        String kind = "";
        switch (classDecl.getKind().getElem()) {
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

        visitSpace(classDecl.getPrefix(), p);
        visit(classDecl.getAnnotations(), p);
        visitModifiers(classDecl.getModifiers(), p);
        visitSpace(classDecl.getKind().getBefore(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append(kind);
        visit(classDecl.getName(), p);
        visit("<", classDecl.getTypeParameters(), ",", ">", p);
        visit("extends", classDecl.getExtends(), p);
        visit(classDecl.getKind().getElem().equals(ClassDecl.Kind.Interface) ? "extends" : "implements",
                classDecl.getImplements(), ",", null, p);
        visit(classDecl.getBody(), p);
        return classDecl;
    }

    @Override
    public J visitCompilationUnit(CompilationUnit cu, P p) {
        visitSpace(cu.getPrefix(), p);
        visit(cu.getPackageDecl(), ";", p);
        visit(cu.getImports(), ";", p);
        StringBuilder acc = getPrinterAcc();
        if (!cu.getImports().isEmpty()) {
            acc.append(";");
        }
        visit(cu.getClasses(), p);
        visitSpace(cu.getEof(), p);
        return cu;
    }

    @Override
    public J visitContinue(Continue continueStatement, P p) {
        visitSpace(continueStatement.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append("continue");
        visit(continueStatement.getLabel(), p);
        return continueStatement;
    }

    @Override
    public <T extends J> J visitControlParentheses(ControlParentheses<T> controlParens, P p) {
        visitSpace(controlParens.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append('(');
        visit(controlParens.getTree(), ")", p);
        return controlParens;
    }

    @Override
    public J visitDoWhileLoop(DoWhileLoop doWhileLoop, P p) {
        visitSpace(doWhileLoop.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append("do");
        visitStatement(doWhileLoop.getBody(), p);
        visit("while", doWhileLoop.getWhileCondition(), p);
        return doWhileLoop;
    }

    @Override
    public J visitElse(If.Else elze, P p) {
        visitSpace(elze.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append("else");
        visitStatement(elze.getBody(), p);
        return elze;
    }

    @Override
    public J visitEnumValue(EnumValue enoom, P p) {

        visitSpace(enoom.getPrefix(), p);
        visit(enoom.getName(), p);
        NewClass initializer = enoom.getInitializer();
        if (enoom.getInitializer() != null) {
            visitSpace(initializer.getPrefix(), p);
            visit("(", initializer.getArgs(), ",", ")", p);
            visit(initializer.getBody(), p);
        }
        return enoom;
    }

    @Override
    public J visitEnumValueSet(EnumValueSet enums, P p) {
        visitSpace(enums.getPrefix(), p);
        visit(enums.getEnums(), ",", p);
        StringBuilder acc = getPrinterAcc();
        if (enums.isTerminatedWithSemicolon()) {
            acc.append(';');
        }
        return enums;
    }

    @Override
    public J visitFieldAccess(FieldAccess fieldAccess, P p) {
        visitSpace(fieldAccess.getPrefix(), p);
        visit(fieldAccess.getTarget(), p);
        visit(".", fieldAccess.getName(), p);
        return fieldAccess;
    }

    @Override
    public J visitForLoop(ForLoop forLoop, P p) {
        visitSpace(forLoop.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append("for");
        ForLoop.Control ctrl = forLoop.getControl();
        visitSpace(ctrl.getPrefix(), p);
        acc.append('(');
        visit(ctrl.getInit(), ";", p);
        visit(ctrl.getCondition(), ";", p);
        visit(ctrl.getUpdate(), ",", p);
        acc.append(')');
        visitStatement(forLoop.getBody(), p);
        return forLoop;
    }

    @Override
    public J visitForEachLoop(ForEachLoop forEachLoop, P p) {
        visitSpace(forEachLoop.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append("for");
        ForEachLoop.Control ctrl = forEachLoop.getControl();
        visitSpace(ctrl.getPrefix(), p);
        acc.append('(');
        visit(ctrl.getVariable(), ":", p);
        visit(ctrl.getIterable(), "", p);
        acc.append(')');
        visitStatement(forEachLoop.getBody(), p);
        return forEachLoop;
    }

    @Override
    public J visitIdentifier(Ident ident, P p) {
        visitSpace(ident.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append(ident.getSimpleName());
        return ident;
    }

    @Override
    public J visitIf(If iff, P p) {
        visitSpace(iff.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append("if");
        visit(iff.getIfCondition(), p);
        visitStatement(iff.getThenPart(), p);
        visit(iff.getElsePart(), p);
        return iff;
    }

    @Override
    public J visitImport(Import impoort, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(impoort.getPrefix(), p);
        acc.append("import");
        if (impoort.isStatic()) {
            if (impoort.getStatic() != null) {
                visitSpace(impoort.getStatic(), p);
            }
            acc.append("static");
        }
        visit(impoort.getQualid(), p);
        return impoort;
    }

    /*
    return fmt(impoort, "import" +
                (impoort.isStatic() ? visit(impoort.getStatic()) + "static" : "") +
                visit(impoort.getQualid(), p));
     */

    @Override
    public J visitInstanceOf(InstanceOf instanceOf, P p) {
        visitSpace(instanceOf.getPrefix(), p);
        visit(instanceOf.getExpr(), "instanceof", p);
        visit(instanceOf.getClazz(), p);
        return instanceOf;
    }

    @Override
    public J visitLabel(Label label, P p) {
        visitSpace(label.getPrefix(), p);
        visit(label.getLabel(), ":", p);
        visit(label.getStatement(), p);
        return label;
    }

    @Override
    public J visitLambda(Lambda lambda, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(lambda.getPrefix(), p);
        visitSpace(lambda.getParameters().getPrefix(), p);
        if (lambda.getParameters().isParenthesized()) {
            acc.append('(');
            visit(lambda.getParameters().getParams(), ",", p);
            acc.append(')');
        } else {
            visit(lambda.getParameters().getParams(), ",", p);
        }
        visitSpace(lambda.getArrow(), p);
        acc.append("->");
        visit(lambda.getBody(), p);
        return lambda;
    }

    @Override
    public J visitLiteral(Literal literal, P p) {
        visitSpace(literal.getPrefix(), p);
        StringBuilder acc = getPrinterAcc();
        acc.append(literal.getValueSource());
        return literal;
    }

    @Override
    public J visitMemberReference(MemberReference memberRef, P p) {
        visitSpace(memberRef.getPrefix(), p);
        visit(memberRef.getContaining(), p);
        visit("<", memberRef.getTypeParameters(), ",", ">", p);
        visit("::", memberRef.getReference(), p);
        return memberRef;
    }

    @Override
    public J visitMethod(MethodDecl method, P p) {
        visitSpace(method.getPrefix(), p);
        visit(method.getAnnotations(), p);
        visitModifiers(method.getModifiers(), p);
        visit("<", method.getTypeParameters(), ",", ">", p);
        visit(method.getReturnTypeExpr(), p);
        visit(method.getName(), p);
        visit("(", method.getParams(), ",", ")", p);
        visit("throws", method.getThrows(), ",", null, p);
        visit(method.getBody(), p);
        visit("default", method.getDefaultValue(), p);
        return method;
    }

    @Override
    public J visitMethodInvocation(MethodInvocation method, P p) {
        visitSpace(method.getPrefix(), p);
        visit(method.getSelect(), ".", p);
        visit("<", method.getTypeParameters(), ",", ">", p);
        visit(method.getName(), p);
        visit("(", method.getArgs(), ",", ")", p);
        return method;
    }

    @Override
    public J visitMultiCatch(MultiCatch multiCatch, P p) {
        visitSpace(multiCatch.getPrefix(), p);
        visit(multiCatch.getAlternatives(), "|", p);
        return multiCatch;
    }

    @Override
    public J visitMultiVariable(VariableDecls multiVariable, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(multiVariable.getPrefix(), p);
        visit(multiVariable.getAnnotations(), p);
        visitModifiers(multiVariable.getModifiers(), p);
        visit(multiVariable.getTypeExpr(), p);
        for (JLeftPadded<Space> dim : multiVariable.getDimensionsBeforeName()) {
            visitSpace(dim.getBefore(), p);
            acc.append('[');
            visitSpace(dim.getElem(), p);
            acc.append(']');
        }
        if (multiVariable.getVarargs() != null) {
            visitSpace(multiVariable.getVarargs(), p);
            acc.append("...");
        }
        visit(multiVariable.getVars(), ",", p);
        return multiVariable;
    }

    @Override
    public J visitNewArray(NewArray newArray, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(newArray.getPrefix(), p);
        if (newArray.getTypeExpr() != null) {
            acc.append("new");
        }
        visit(newArray.getTypeExpr(), p);
        visit(newArray.getDimensions(), p);
        visit("{", newArray.getInitializer(), ",", "}", p);
        return newArray;
    }

    @Override
    public J visitNewClass(NewClass newClass, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(newClass.getPrefix(), p);
        visit(newClass.getEncl(), ".", p);
        visitSpace(newClass.getNew(), p);
        acc.append("new");
        visit(newClass.getClazz(), p);
        visit("(", newClass.getArgs(), ",", ")", p);
        visit(newClass.getBody(), p);
        return newClass;
    }

    @Override
    public J visitPackage(J.Package pkg, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(pkg.getPrefix(), p);
        acc.append("package");
        visit(pkg.getExpr(), p);
        return pkg;
    }

    @Override
    public J visitParameterizedType(ParameterizedType type, P p) {
        visitSpace(type.getPrefix(), p);
        visit(type.getClazz(), p);
        visit("<", type.getTypeParameters(), ",", ">", p);
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
        StringBuilder acc = getPrinterAcc();
        visitSpace(primitive.getPrefix(), p);
        acc.append(keyword);
        return primitive;
    }

    @Override
    public <T extends J> J visitParentheses(Parentheses<T> parens, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(parens.getPrefix(), p);
        acc.append("(");
        visit(parens.getTree(), ")", p);
        return parens;
    }

    @Override
    public J visitReturn(Return retrn, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(retrn.getPrefix(), p);
        acc.append("return");
        visit(retrn.getExpr(), p);
        return retrn;
    }

    @Override
    public J visitSwitch(Switch switzh, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(switzh.getPrefix(), p);
        acc.append("switch");
        visit(switzh.getSelector(), p);
        visit(switzh.getCases(), p);
        return switzh;
    }

    @Override
    public J visitSynchronized(J.Synchronized synch, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(synch.getPrefix(), p);
        acc.append("synchronized");
        visit(synch.getLock(), p);
        visit(synch.getBody(), p);
        return synch;
    }

    @Override
    public J visitTernary(Ternary ternary, P p) {
        visitSpace(ternary.getPrefix(), p);
        visit(ternary.getCondition(), p);
        visit("?", ternary.getTruePart(), p);
        visit(":", ternary.getFalsePart(), p);
        return ternary;
    }

    @Override
    public J visitThrow(Throw thrown, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(thrown.getPrefix(), p);
        acc.append("throw");
        visit(thrown.getException(), p);
        return thrown;
    }

    @Override
    public J visitTry(Try tryable, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(tryable.getPrefix(), p);
        acc.append("try");
        if (tryable.getResources() != null) {
            visitSpace(tryable.getResources().getBefore(), p);
            acc.append('(');
            List<JRightPadded<Try.Resource>> resources = tryable.getResources().getElem();
            for (int i = 0; i < resources.size(); i++) {
                JRightPadded<Try.Resource> resource = resources.get(i);

                visitSpace(resource.getElem().getPrefix(), p);
                visit(resource.getElem().getVariableDecls(), p);

                if (i < resources.size() - 1 || resource.getElem().isTerminatedWithSemicolon()) {
                    acc.append(';');
                }

                visitSpace(resource.getAfter(), p);
            }
            acc.append(')');
        }

        visit(tryable.getBody(), p);
        visit(tryable.getCatches(), p);
        visit("finally", tryable.getFinally(), p);
        return tryable;
    }

    @Override
    public J visitTypeParameter(TypeParameter typeParam, P p) {
        visitSpace(typeParam.getPrefix(), p);
        visit(typeParam.getAnnotations(), p);
        visit(typeParam.getName(), p);
        visit("extends", typeParam.getBounds(), "&", "", p);
        return typeParam;
    }

    @Override
    public J visitUnary(Unary unary, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(unary.getPrefix(), p);
        switch (unary.getOperator().getElem()) {
            case PreIncrement:
                acc.append("++");
                visit(unary.getExpr(), p);
                break;
            case PreDecrement:
                acc.append("--");
                visit(unary.getExpr(), p);
                break;
            case PostIncrement:
                visit(unary.getExpr(), p);
                visitSpace(unary.getOperator().getBefore(), p);
                acc.append("++");
                break;
            case PostDecrement:
                visit(unary.getExpr(), p);
                visitSpace(unary.getOperator().getBefore(), p);
                acc.append("--");
                break;
            case Positive:
                acc.append("+");
                visit(unary.getExpr(), p);
                break;
            case Negative:
                acc.append("-");
                visit(unary.getExpr(), p);
                break;
            case Complement:
                acc.append("~");
                visit(unary.getExpr(), p);
                break;
            case Not:
            default:
                acc.append("!");
                visit(unary.getExpr(), p);
        }
        return unary;
    }

    @Override
    public J visitVariable(VariableDecls.NamedVar variable, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(variable.getPrefix(), p);
        visit(variable.getName(), p);
        for (JLeftPadded<Space> dimension : variable.getDimensionsAfterName()) {
            visitSpace(dimension.getBefore(), p);
            acc.append('[');
            visitSpace(dimension.getElem(), p);
            acc.append(']');
        }
        visit("=", variable.getInitializer(), p);
        return variable;
    }

    @Override
    public J visitWhileLoop(WhileLoop whileLoop, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(whileLoop.getPrefix(), p);
        acc.append("while");
        visit(whileLoop.getCondition(), p);
        visitStatement(whileLoop.getBody(), p);
        return whileLoop;
    }

    @Override
    public J visitWildcard(Wildcard wildcard, P p) {
        StringBuilder acc = getPrinterAcc();
        visitSpace(wildcard.getPrefix(), p);
        acc.append('?');
        if (wildcard.getBound() != null) {
            switch (wildcard.getBound().getElem()) {
                case Extends:
                    visitSpace(wildcard.getBound().getBefore(), p);
                    acc.append("extends");
                    break;
                case Super:
                    visitSpace(wildcard.getBound().getBefore(), p);
                    acc.append("super");
                    break;
            }
        }
        visit(wildcard.getBoundedType(), p);
        return wildcard;
    }
}
