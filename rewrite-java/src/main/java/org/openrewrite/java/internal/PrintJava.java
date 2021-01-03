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

import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;

import java.util.List;

import static org.openrewrite.java.tree.J.Modifier.*;

public class PrintJava<P> implements JavaVisitor<String, P> {
    private final TreePrinter<J, P> treePrinter;

    public PrintJava(TreePrinter<J, P> treePrinter) {
        this.treePrinter = treePrinter;
    }

    @Override
    public String defaultValue(@Nullable Tree tree, P p) {
        return "";
    }

    @Override
    public String visit(Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        J t = treePrinter.doFirst((J) tree, p);
        if (t == null) {
            return defaultValue(null, p);
        }

        return treePrinter.doLast(tree, t.accept(this, p), p);
    }

    public String visit(@Nullable List<? extends J> nodes, P p) {
        if (nodes == null) {
            return "";
        }

        StringBuilder acc = new StringBuilder();
        for (J node : nodes) {
            acc.append(visit(node, p));
        }
        return acc.toString();
    }

    protected String visit(List<? extends JRightPadded<? extends J>> nodes, String suffixBetween, P p) {
        StringBuilder acc = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            JRightPadded<? extends J> node = nodes.get(i);
            acc.append(visit(node.getElem(), p)).append(visit(node.getAfter()));
            if (i < nodes.size() - 1) {
                acc.append(suffixBetween);
            }
        }
        return acc.toString();
    }

    protected String visit(String before, @Nullable JContainer<? extends J> container, String suffixBetween, @Nullable String after, P p) {
        if (container == null) {
            return "";
        }
        return visit(container.getBefore()) + before + visit(container.getElem(), suffixBetween, p) +
                (after == null ? "" : after);
    }

    protected String visit(@Nullable Space space) {
        if (space == null) {
            return "";
        }

        StringBuilder fmt = new StringBuilder(space.getWhitespace());

        for (Comment comment : space.getComments()) {
            switch (comment.getStyle()) {
                case LINE:
                    fmt.append("//").append(comment.getText());
                    break;
                case BLOCK:
                    fmt.append("/*").append(comment.getText()).append("*/");
                    break;
                case JAVADOC:
                    fmt.append("/**").append(comment.getText()).append("*/");
                    break;
            }
            fmt.append(comment.getSuffix());
        }

        return fmt.toString();
    }

    protected String fmt(@Nullable J tree, @Nullable String code) {
        return tree == null || code == null ? "" : visit(tree.getPrefix()) + code;
    }

    protected String visit(@Nullable String prefix, @Nullable JLeftPadded<? extends J> leftPadded, P p) {
        return leftPadded == null ? "" : visit(leftPadded.getBefore()) + (prefix == null ? "" : prefix) +
                visit(leftPadded.getElem(), p);
    }

    protected String visit(@Nullable JRightPadded<? extends J> leftPadded, @Nullable String suffix, P p) {
        return leftPadded == null ? "" : visit(leftPadded.getElem(), p) + visit(leftPadded.getAfter()) +
                (suffix == null ? "" : suffix);
    }

    protected String visitModifiers(Iterable<Modifier> modifiers) {
        StringBuilder acc = new StringBuilder();
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
            acc.append(fmt(mod, keyword));
        }

        return acc.toString();
    }

    @Override
    public String visitAnnotatedType(AnnotatedType annotatedType, P p) {
        return fmt(annotatedType, visit(annotatedType.getAnnotations(), p) + visit(annotatedType.getTypeExpr(), p));
    }

    @Override
    public String visitAnnotation(Annotation annotation, P p) {
        String args = annotation.getArgs() == null ? "" : visit("(", annotation.getArgs(), ",", ")", p);
        return fmt(annotation, "@" + visit(annotation.getAnnotationType(), p) + args);
    }

    @Override
    public String visitArrayAccess(ArrayAccess arrayAccess, P p) {
        String dimension = visit(arrayAccess.getDimension().getBefore()) + '[' +
                visit(arrayAccess.getDimension().getElem().getElem(), p) +
                visit(arrayAccess.getDimension().getElem().getAfter()) + ']';
        return fmt(arrayAccess, visit(arrayAccess.getIndexed(), p) + dimension);
    }

    @Override
    public String visitArrayType(ArrayType arrayType, P p) {
        StringBuilder dimensions = new StringBuilder();
        for (JRightPadded<Space> d : arrayType.getDimensions()) {
            dimensions.append(visit(d.getElem()))
                    .append('[')
                    .append(visit(d.getAfter()))
                    .append(']');
        }
        return fmt(arrayType, visit(arrayType.getElementType(), p) + dimensions.toString());
    }

    @Override
    public String visitAssert(Assert azzert, P p) {
        return fmt(azzert, "assert" + visit(azzert.getCondition(), p));
    }

    @Override
    public String visitAssign(Assign assign, P p) {
        return fmt(assign, visit(assign.getVariable().getElem(), p) +
                visit(assign.getVariable().getAfter()) + "=" + visit(assign.getAssignment(), p));
    }

    @Override
    public String visitAssignOp(AssignOp assignOp, P p) {
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

        return fmt(assignOp, visit(assignOp.getVariable(), p) +
                visit(assignOp.getOperator().getBefore()) + keyword +
                visit(assignOp.getAssignment(), p));
    }

    @Override
    public String visitBinary(Binary binary, P p) {
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
        return fmt(binary, visit(binary.getLeft(), p) + visit(binary.getOperator().getBefore()) +
                keyword + visit(binary.getRight(), p));
    }

    @Override
    public String visitBlock(Block block, P p) {
        StringBuilder acc = new StringBuilder();

        if (block.getStatic() != null) {
            acc.append("static").append(visit(block.getStatic()));
        }

        acc.append('{')
                .append(visitStatements(block.getStatements(), p))
                .append(visit(block.getEnd()))
                .append('}');

        return fmt(block, acc.toString());
    }

    private String visitStatements(List<JRightPadded<Statement>> statements, P p) {
        StringBuilder acc = new StringBuilder();
        for (JRightPadded<Statement> paddedStat : statements) {
            acc.append(visitStatement(paddedStat, p));
        }
        return acc.toString();
    }

    private String visitStatement(@Nullable JRightPadded<Statement> paddedStat, P p) {
        if (paddedStat == null) {
            return "";
        }

        String acc = visit(paddedStat.getElem(), p) + visit(paddedStat.getAfter());

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
                return acc + ';';
            }

            if (s instanceof MethodDecl && ((MethodDecl) s).isAbstract()) {
                return acc + ';';
            }

            if (s instanceof Label) {
                s = ((Label) s).getStatement();
                continue;
            }

            return acc;
        }
    }

    @Override
    public String visitBreak(Break breakStatement, P p) {
        return fmt(breakStatement, "break" + visit(breakStatement.getLabel(), p));
    }

    @Override
    public String visitCase(Case caze, P p) {
        String pattern;
        Expression elem = caze.getPattern().getElem();
        if (elem instanceof Ident && ((Ident) elem).getSimpleName().equals("default")) {
            pattern = "default";
        } else {
            pattern = "case" + visit(elem, p);
        }

        return fmt(caze, pattern +
                visit(caze.getPattern().getAfter()) + ":" +
                visitStatements(caze.getStatements(), p));
    }

    @Override
    public String visitCatch(Try.Catch catzh, P p) {
        return fmt(catzh, "catch" + visit(catzh.getParam(), p) + visit(catzh.getBody(), p));
    }

    @Override
    public String visitClassDecl(ClassDecl classDecl, P p) {
        String modifiers = visitModifiers(classDecl.getModifiers());

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

        return fmt(classDecl, visit(classDecl.getAnnotations(), p) +
                modifiers +
                visit(classDecl.getKind().getBefore()) + kind +
                visit(classDecl.getName(), p) +
                visit("<", classDecl.getTypeParameters(), ",", ">", p) +
                visit("extends", classDecl.getExtends(), p) +
                visit(classDecl.getKind().getElem().equals(ClassDecl.Kind.Interface) ? "extends" : "implements",
                        classDecl.getImplements(), ",", null, p) +
                visit(classDecl.getBody(), p));
    }

    @Override
    public String visitCompilationUnit(CompilationUnit cu, P p) {
        return fmt(cu,
                visit(cu.getPackageDecl(), ";", p) +
                        visit(cu.getImports(), ";", p) +
                        (cu.getImports().isEmpty() ? "" : ";") +
                        visit(cu.getClasses(), p) +
                        visit(cu.getEof()));
    }

    @Override
    public String visitContinue(Continue continueStatement, P p) {
        return fmt(continueStatement, "continue" + visit(continueStatement.getLabel(), p));
    }

    @Override
    public String visitDoWhileLoop(DoWhileLoop doWhileLoop, P p) {
        return fmt(doWhileLoop, "do" + visitStatement(doWhileLoop.getBody(), p) +
                visit("while", doWhileLoop.getWhileCondition(), p));
    }

    @Override
    public String visitElse(If.Else elze, P p) {
        return fmt(elze, "else" + visitStatement(elze.getBody(), p));
    }

    @Override
    public String visitEmpty(Empty empty, P p) {
        return fmt(empty, "");
    }

    @Override
    public String visitEnumValue(EnumValue enoom, P p) {
        String init = "";

        NewClass initializer = enoom.getInitializer();
        if (initializer != null) {
            init = fmt(initializer,
                    visit("(", initializer.getArgs(), ",", ")", p) +
                            visit(initializer.getBody(), p));
        }

        return fmt(enoom, visit(enoom.getName(), p) + init);
    }

    @Override
    public String visitEnumValueSet(EnumValueSet enums, P p) {
        return fmt(enums, visit(enums.getEnums(), ",", p) +
                (enums.isTerminatedWithSemicolon() ? ";" : ""));
    }

    @Override
    public String visitFieldAccess(FieldAccess fieldAccess, P p) {
        return fmt(fieldAccess, visit(fieldAccess.getTarget(), ".", p) +
                visit(fieldAccess.getName(), p));
    }

    @Override
    public String visitForLoop(ForLoop forLoop, P p) {
        ForLoop.Control ctrl = forLoop.getControl();
        String expr = fmt(ctrl, "(" +
                visit(ctrl.getInit(), ";", p) +
                visit(ctrl.getCondition(), ";", p) +
                visit(ctrl.getUpdate(), ",", p) +
                ")");
        return fmt(forLoop, "for" + expr + visitStatement(forLoop.getBody(), p));
    }

    @Override
    public String visitForEachLoop(ForEachLoop forEachLoop, P p) {
        ForEachLoop.Control ctrl = forEachLoop.getControl();
        String expr = fmt(ctrl, "(" +
                visit(ctrl.getVariable(), ":", p) +
                visit(ctrl.getIterable(), "", p) +
                ")");
        return fmt(forEachLoop, "for" + expr + visitStatement(forEachLoop.getBody(), p));
    }

    @Override
    public String visitIdentifier(Ident ident, P p) {
        return fmt(ident, ident.getSimpleName());
    }

    @Override
    public String visitIf(If iff, P p) {
        return fmt(iff, "if" + visit(iff.getIfCondition(), p) +
                visitStatement(iff.getThenPart(), p) +
                visit(iff.getElsePart(), p));
    }

    @Override
    public String visitImport(Import impoort, P p) {
        return fmt(impoort, "import" +
                (impoort.isStatic() ? visit(impoort.getStatic()) + "static" : "") +
                visit(impoort.getQualid(), p));
    }

    @Override
    public String visitInstanceOf(InstanceOf instanceOf, P p) {
        return fmt(instanceOf, visit(instanceOf.getExpr(), "instanceof", p) +
                visit(instanceOf.getClazz(), p));
    }

    @Override
    public String visitLabel(Label label, P p) {
        return fmt(label, visit(label.getLabel(), ":", p) +
                visit(label.getStatement(), p));
    }

    @Override
    public String visitLambda(Lambda lambda, P p) {
        String params = visit(lambda.getParameters().getParams(), ",", p);
        String paramSet = fmt(lambda.getParameters(), lambda.getParameters().isParenthesized() ? "(" + params + ")" : params);
        return fmt(lambda, paramSet + visit(lambda.getArrow()) + "->") + visit(lambda.getBody(), p);
    }

    @Override
    public String visitLiteral(Literal literal, P p) {
        return fmt(literal, literal.getValueSource());
    }

    @Override
    public String visitMemberReference(MemberReference memberRef, P p) {
        return fmt(memberRef, visit(memberRef.getContaining(), "::", p) +
                visit("<", memberRef.getTypeParameters(), ",", ">", p) +
                visit(memberRef.getReference(), p));
    }

    @Override
    public String visitMethod(MethodDecl method, P p) {
        return fmt(method,
                visit(method.getAnnotations(), p) +
                        visitModifiers(method.getModifiers()) +
                        visit("<", method.getTypeParameters(), ",", ">", p) +
                        visit(method.getReturnTypeExpr(), p) +
                        visit(method.getName(), p) +
                        visit("(", method.getParams(), ",", ")", p) +
                        visit("throws", method.getThrows(), ",", null, p) +
                        visit(method.getBody(), p) +
                        visit("default", method.getDefaultValue(), p));
    }

    @Override
    public String visitMethodInvocation(MethodInvocation method, P p) {
        return fmt(method,
                visit(method.getSelect(), ".", p) +
                        visit("<", method.getTypeParameters(), ",", ">", p) +
                        visit(method.getName(), p) +
                        visit("(", method.getArgs(), ",", ")", p));
    }

    @Override
    public String visitMultiCatch(MultiCatch multiCatch, P p) {
        return fmt(multiCatch, visit(multiCatch.getAlternatives(), "|", p));
    }

    @Override
    public String visitMultiVariable(VariableDecls multiVariable, P p) {
        StringBuilder acc = new StringBuilder(visit(multiVariable.getAnnotations(), p));
        acc.append(visitModifiers(multiVariable.getModifiers()));
        acc.append(visit(multiVariable.getTypeExpr(), p));

        for (JLeftPadded<Space> dim : multiVariable.getDimensionsBeforeName()) {
            acc.append(visit(dim.getBefore())).append('[')
                    .append(dim.getElem().getWhitespace()).append(']');
        }

        if (multiVariable.getVarargs() != null) {
            acc.append(visit(multiVariable.getVarargs())).append("...");
        }

        acc.append(visit(multiVariable.getVars(), ",", p));

        return fmt(multiVariable, acc.toString());
    }

    @Override
    public String visitNewArray(NewArray newArray, P p) {
        StringBuilder acc = new StringBuilder();
        if (newArray.getTypeExpr() != null) {
            acc.append("new").append(visit(newArray.getTypeExpr(), p));
        }

        for (JLeftPadded<JRightPadded<Expression>> dimension : newArray.getDimensions()) {
            acc.append(visit(dimension.getBefore())).append('[')
                    .append(visit(dimension.getElem().getElem(), p))
                    .append(visit(dimension.getElem().getAfter())).append(']');
        }

        acc.append(visit("{", newArray.getInitializer(), ",", "}", p));

        return fmt(newArray, acc.toString());
    }

    @Override
    public String visitNewClass(NewClass newClass, P p) {
        return fmt(newClass,
                visit(newClass.getEncl(), ".", p) +
                        visit(newClass.getNew()) + "new" +
                        visit(newClass.getClazz(), p) +
                        visit("(", newClass.getArgs(), ",", ")", p) +
                        visit(newClass.getBody(), p));
    }

    @Override
    public String visitPackage(J.Package pkg, P p) {
        return fmt(pkg, "package" + visit(pkg.getExpr(), p));
    }

    @Override
    public String visitParameterizedType(ParameterizedType type, P p) {
        return fmt(type, visit(type.getClazz(), p) +
                visit("<", type.getTypeParameters(), ",", ">", p));
    }

    @Override
    public String visitPrimitive(Primitive primitive, P p) {
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

        return fmt(primitive, keyword);
    }

    @Override
    public <T extends J> String visitParentheses(Parentheses<T> parens, P p) {
        return fmt(parens, "(" + visit(parens.getTree(), ")", p));
    }

    @Override
    public String visitReturn(Return retrn, P p) {
        return fmt(retrn, "return" + visit(retrn.getExpr(), p));
    }

    @Override
    public String visitSwitch(Switch switzh, P p) {
        return fmt(switzh, "switch" + visit(switzh.getSelector(), p) +
                visit(switzh.getCases(), p));
    }

    @Override
    public String visitSynchronized(J.Synchronized synch, P p) {
        return fmt(synch, "synchronized" + visit(synch.getLock(), p) + visit(synch.getBody(), p));
    }

    @Override
    public String visitTernary(Ternary ternary, P p) {
        return fmt(ternary, visit(ternary.getCondition(), "?", p) +
                visit(ternary.getTruePart(), ":", p) +
                visit(ternary.getFalsePart(), p));
    }

    @Override
    public String visitThrow(Throw thrown, P p) {
        return fmt(thrown, "throw" + visit(thrown.getException(), p));
    }

    @Override
    public String visitTry(Try tryable, P p) {
        StringBuilder acc = new StringBuilder("try");

        if (tryable.getResources() != null) {
            acc.append(visit(tryable.getResources().getBefore())).append('(');
            List<JRightPadded<Try.Resource>> resources = tryable.getResources().getElem();
            for (int i = 0; i < resources.size(); i++) {
                JRightPadded<Try.Resource> resource = resources.get(i);

                acc.append(visit(resource.getElem().getPrefix()))
                        .append(visit(resource.getElem().getVariableDecls(), p));

                if (i < resources.size() - 1 || resource.getElem().isTerminatedWithSemicolon()) {
                    acc.append(';');
                }

                acc.append(visit(resource.getAfter()));
            }
            acc.append(')');
        }

        acc.append(visit(tryable.getBody(), p));
        acc.append(visit(tryable.getCatches(), p));
        acc.append(visit("finally", tryable.getFinally(), p));

        return fmt(tryable, acc.toString());
    }

    @Override
    public String visitTypeCast(TypeCast typeCast, P p) {
        return fmt(typeCast, visit(typeCast.getClazz(), p) + visit(typeCast.getExpr(), p));
    }

    @Override
    public String visitTypeParameter(TypeParameter typeParam, P p) {
        return fmt(typeParam, visit(typeParam.getAnnotations(), p) +
                visit(typeParam.getName(), p) +
                visit("extends", typeParam.getBounds(), "&", "", p));
    }

    @Override
    public String visitUnary(Unary unary, P p) {
        switch (unary.getOperator().getElem()) {
            case PreIncrement:
                return fmt(unary, "++" + visit(unary.getExpr(), p));
            case PreDecrement:
                return fmt(unary, "--" + visit(unary.getExpr(), p));
            case PostIncrement:
                return fmt(unary, visit(unary.getExpr(), p) + visit(unary.getOperator().getBefore()) + "++");
            case PostDecrement:
                return fmt(unary, visit(unary.getExpr(), p) + visit(unary.getOperator().getBefore()) + "--");
            case Positive:
                return fmt(unary, "+" + visit(unary.getExpr(), p));
            case Negative:
                return fmt(unary, "-" + visit(unary.getExpr(), p));
            case Complement:
                return fmt(unary, "~" + visit(unary.getExpr(), p));
            case Not:
            default:
                return fmt(unary, "!" + visit(unary.getExpr(), p));
        }
    }

    @Override
    public String visitVariable(VariableDecls.NamedVar variable, P p) {
        StringBuilder dimensions = new StringBuilder();
        for (JLeftPadded<Space> dimension : variable.getDimensionsAfterName()) {
            dimensions.append(visit(dimension.getBefore())).append('[')
                    .append(dimension.getElem().getWhitespace()).append(']');
        }

        return fmt(variable, visit(variable.getName(), p) +
                dimensions.toString() +
                visit("=", variable.getInitializer(), p));
    }

    @Override
    public String visitWhileLoop(WhileLoop whileLoop, P p) {
        return fmt(whileLoop, "while" + visit(whileLoop.getCondition(), p) +
                visitStatement(whileLoop.getBody(), p));
    }

    @Override
    public String visitWildcard(Wildcard wildcard, P p) {
        String bound = "";
        if (wildcard.getBound() != null) {
            switch (wildcard.getBound().getElem()) {
                case Extends:
                    bound = visit(wildcard.getBound().getBefore()) + "extends";
                    break;
                case Super:
                    bound = visit(wildcard.getBound().getBefore()) + "super";
                    break;
            }
        }
        return fmt(wildcard, "?" + bound + visit(wildcard.getBoundedType(), p));
    }
}
