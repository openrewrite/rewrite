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
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class JavaVisitor<P> extends TreeVisitor<J, P> {
    /**
     * This method will add an import to the compilation unit if there is a reference to the type. It adds an additional
     * visitor which means the "add import" is deferred and does not complete immediately. This operation is idempotent
     * and calling this method multiple times with the same arguments will only add an import once.
     *
     * @param clazz The class that will be imported into the compilation unit.
     */
    public void maybeAddImport(@Nullable JavaType.FullyQualified clazz) {
        if (clazz != null) {
            maybeAddImport(clazz.getFullyQualifiedName());
        }
    }

    /**
     * This method will add an import to the compilation unit if there is a reference to the type. It adds an additional
     * visitor which means the "add import" is deferred and does not complete immediately. This operation is idempotent
     * and calling this method multiple times with the same arguments will only add an import once.
     *
     * @param fullyQualifiedName Fully-qualified name of the class.
     */
    public void maybeAddImport(String fullyQualifiedName) {
        AddImport<P> op = new AddImport<>(fullyQualifiedName, null, true);
        if (!getAfterVisit().contains(op)) {
            doAfterVisit(op);
        }
    }

    /**
     * This method will add a static import to the compilation unit if there is a reference to the type/method. It adds
     * an additional visitor which means the "add import" is deferred and does not complete immediately. This operation
     * is idempotent and calling this method multiple times with the same arguments will only add an import once.
     *
     * @param fullyQualifiedName Fully-qualified name of the class.
     * @param statik             The static method or field to be imported. A wildcard "*" may also be used to statically import all methods/fields.
     */
    public void maybeAddImport(String fullyQualifiedName, String statik) {
        AddImport<P> op = new AddImport<>(fullyQualifiedName, statik, true);
        if (!getAfterVisit().contains(op)) {
            doAfterVisit(op);
        }
    }

    public void maybeRemoveImport(@Nullable JavaType.FullyQualified clazz) {
        if (clazz != null) {
            maybeRemoveImport(clazz.getFullyQualifiedName());
        }
    }

    public void maybeRemoveImport(String fullyQualifiedName) {
        RemoveImport<P> op = new RemoveImport<>(fullyQualifiedName);
        if (!getAfterVisit().contains(op)) {
            doAfterVisit(op);
        }
    }


    public J visitExpression(Expression expression, P p) {
        return expression;
    }

    public J visitStatement(Statement statement, P p) {
        return statement;
    }

    public Space visitSpace(Space space, P p) {
        return space;
    }

    public <N extends NameTree> N visitTypeName(N nameTree, P p) {
        return nameTree;
    }

    @Nullable
    private <N extends NameTree> JLeftPadded<N> visitTypeName(@Nullable JLeftPadded<N> nameTree, P p) {
        return nameTree == null ? null : nameTree.withElem(visitTypeName(nameTree.getElem(), p));
    }

    @Nullable
    private <N extends NameTree> JRightPadded<N> visitTypeName(@Nullable JRightPadded<N> nameTree, P p) {
        return nameTree == null ? null : nameTree.withElem(visitTypeName(nameTree.getElem(), p));
    }

    @Nullable
    private <J2 extends J> JContainer<J2> visitTypeNames(@Nullable JContainer<J2> nameTrees, P p) {
        if (nameTrees == null) {
            return null;
        }
        @SuppressWarnings("unchecked") List<JRightPadded<J2>> js = ListUtils.map(nameTrees.getElem(),
                t -> t.getElem() instanceof NameTree ? (JRightPadded<J2>) visitTypeName((JRightPadded<NameTree>) t, p) : t);
        return js == nameTrees.getElem() ? nameTrees : JContainer.build(nameTrees.getBefore(), js, Markers.EMPTY);
    }

    public J visitAnnotatedType(J.AnnotatedType annotatedType, P p) {
        J.AnnotatedType a = visitAndCast(annotatedType, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = visitAndCast(a, p, this::visitExpression);
        a = a.withAnnotations(ListUtils.map(a.getAnnotations(), e -> visitAndCast(e, p)));
        a = a.withTypeExpr(visitAndCast(a.getTypeExpr(), p));
        return a.withTypeExpr(visitTypeName(a.getTypeExpr(), p));
    }

    public J visitAnnotation(J.Annotation annotation, P p) {
        J.Annotation a = visitAndCast(annotation, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = visitAndCast(a, p, this::visitExpression);
        if (a.getArgs() != null) {
            a = a.withArgs(visitContainer(a.getArgs(), JContainer.Location.ANNOTATION_ARGUMENT, p));
        }
        a = a.withAnnotationType(visitAndCast(a.getAnnotationType(), p));
        return a.withAnnotationType(visitTypeName(a.getAnnotationType(), p));
    }

    public J visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
        J.ArrayAccess a = visitAndCast(arrayAccess, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = visitAndCast(a, p, this::visitExpression);
        a = a.withIndexed(visitAndCast(a.getIndexed(), p));
        a = a.withDimension(visitAndCast(a.getDimension(), p));
        return a;
    }

    public J visitArrayDimension(J.ArrayDimension arrayDimension, P p) {
        J.ArrayDimension a = visitAndCast(arrayDimension, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withIndex(visitRightPadded(a.getIndex(), JRightPadded.Location.ARRAY_INDEX, p));
        return a;
    }

    public J visitArrayType(J.ArrayType arrayType, P p) {
        J.ArrayType a = visitAndCast(arrayType, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = visitAndCast(a, p, this::visitExpression);
        a.withElementType(visitAndCast(a.getElementType(), p));
        return a.withElementType(visitTypeName(a.getElementType(), p));
    }

    public J visitAssert(J.Assert azzert, P p) {
        J.Assert a = visitAndCast(azzert, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = visitAndCast(a, p, this::visitStatement);
        return a.withCondition(visitAndCast(a.getCondition(), p));
    }

    public J visitAssign(J.Assign assign, P p) {
        J.Assign a = visitAndCast(assign, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = visitAndCast(a, p, this::visitStatement);
        a = visitAndCast(a, p, this::visitExpression);
        a = a.withVariable(visitAndCast(a.getVariable(), p));
        return a.withAssignment(visitLeftPadded(a.getAssignment(), JLeftPadded.Location.ASSIGNMENT, p));
    }

    public J visitAssignOp(J.AssignOp assignOp, P p) {
        J.AssignOp a = visitAndCast(assignOp, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = visitAndCast(a, p, this::visitStatement);
        a = visitAndCast(a, p, this::visitExpression);
        a = a.withVariable(visitAndCast(a.getVariable(), p));
        return a.withAssignment(visitAndCast(a.getAssignment(), p));
    }

    public J visitBinary(J.Binary binary, P p) {
        J.Binary b = visitAndCast(binary, p, this::visitEach);
        b = b.withPrefix(visitSpace(b.getPrefix(), p));
        b = visitAndCast(b, p, this::visitExpression);
        b = b.withLeft(visitAndCast(b.getLeft(), p));
        b = b.withOperator(visitLeftPadded(b.getOperator(), JLeftPadded.Location.BINARY_OPERATOR, p));
        return b.withRight(visitAndCast(b.getRight(), p));
    }

    public J visitBlock(J.Block block, P p) {
        J.Block b = visitAndCast(block, p, this::visitEach);
        b = b.withStatik(b.getStatic() == null ?
                null :
                visitSpace(b.getStatic(), p));
        b = b.withPrefix(visitSpace(b.getPrefix(), p));
        b = visitAndCast(b, p, this::visitStatement);
        b = b.withStatements(ListUtils.map(b.getStatements(), t ->
                visitRightPadded(t, JRightPadded.Location.BLOCK_STATEMENT, p)));
        return b.withEnd(visitSpace(b.getEnd(), p));
    }

    public J visitBreak(J.Break breakStatement, P p) {
        J.Break b = visitAndCast(breakStatement, p, this::visitEach);
        b = b.withPrefix(visitSpace(b.getPrefix(), p));
        b = visitAndCast(b, p, this::visitStatement);
        return b.withLabel(visitAndCast(b.getLabel(), p));
    }

    public J visitCase(J.Case caze, P p) {
        J.Case c = visitAndCast(caze, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = visitAndCast(c, p, this::visitStatement);
        c = c.withPattern(visitAndCast(c.getPattern(), p));
        return c.withStatements(visitContainer(c.getStatements(), JContainer.Location.CASE, p));
    }

    public J visitCatch(J.Try.Catch catzh, P p) {
        J.Try.Catch c = visitAndCast(catzh, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withParam(visitAndCast(c.getParam(), p));
        return c.withBody(visitAndCast(c.getBody(), p));
    }

    public J visitClassDecl(J.ClassDecl classDecl, P p) {
        J.ClassDecl c = visitAndCast(classDecl, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = visitAndCast(c, p, this::visitStatement);
        c = c.withAnnotations(ListUtils.map(c.getAnnotations(), a -> visitAndCast(a, p)));
        c = c.withModifiers(ListUtils.map(c.getModifiers(), m -> visitAndCast(m, p)));
        c = c.withModifiers(ListUtils.map(c.getModifiers(),
                mod -> mod.withPrefix(visitSpace(mod.getPrefix(), p))));
        if (c.getTypeParameters() != null) {
            c = c.withTypeParameters(visitContainer(c.getTypeParameters(), JContainer.Location.TYPE_PARAMETER, p));
        }
        c = c.withKind(visitLeftPadded(c.getKind(), JLeftPadded.Location.CLASS_KIND, p));
        c = c.withName(visitAndCast(c.getName(), p));
        if (c.getExtends() != null) {
            c = c.withExtends(visitLeftPadded(c.getExtends(), JLeftPadded.Location.EXTENDS, p));
        }
        c = c.withExtends(visitTypeName(c.getExtends(), p));
        if (c.getImplements() != null) {
            c = c.withImplements(visitContainer(c.getImplements(), JContainer.Location.IMPLEMENTS, p));
        }
        c = c.withImplements(visitTypeNames(c.getImplements(), p));
        return c.withBody(visitAndCast(c.getBody(), p));
    }

    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        J.CompilationUnit c = visitAndCast(cu, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        if (c.getPackageDecl() != null) {
            c = c.withPackageDecl(visitRightPadded(c.getPackageDecl(), JRightPadded.Location.PACKAGE, p));
        }
        c = c.withImports(ListUtils.map(c.getImports(), t -> visitRightPadded(t, JRightPadded.Location.IMPORT, p)));
        c = c.withClasses(ListUtils.map(c.getClasses(), e -> visitAndCast(e, p)));
        return c.withEof(visitSpace(c.getEof(), p));
    }

    public J visitContinue(J.Continue continueStatement, P p) {
        J.Continue c = visitAndCast(continueStatement, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = visitAndCast(c, p, this::visitStatement);
        return c.withLabel(visitAndCast(c.getLabel(), p));
    }

    public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, P p) {
        J.ControlParentheses<T> cpa = visitAndCast(controlParens, p, this::visitEach);
        cpa = cpa.withPrefix(visitSpace(cpa.getPrefix(), p));
        cpa = visitAndCast(cpa, p, this::visitExpression);
        return cpa.withTree(visitRightPadded(cpa.getTree(), JRightPadded.Location.PARENTHESES, p));
    }

    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        J.DoWhileLoop d = visitAndCast(doWhileLoop, p, this::visitEach);
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = visitAndCast(d, p, this::visitStatement);
        d = d.withWhileCondition(visitLeftPadded(d.getWhileCondition(), JLeftPadded.Location.WHILE_CONDITION, p));
        return d.withBody(visitRightPadded(d.getBody(), JRightPadded.Location.WHILE_BODY, p));
    }

    public J visitEmpty(J.Empty empty, P p) {
        J.Empty e = visitAndCast(empty, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = visitAndCast(e, p, this::visitStatement);
        e = visitAndCast(e, p, this::visitExpression);
        return e;
    }

    public J visitEnumValue(J.EnumValue enoom, P p) {
        J.EnumValue e = visitAndCast(enoom, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withName(visitAndCast(e.getName(), p));
        return e.withInitializer(visitAndCast(e.getInitializer(), p));
    }

    public J visitEnumValueSet(J.EnumValueSet enums, P p) {
        J.EnumValueSet e = visitAndCast(enums, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = visitAndCast(e, p, this::visitStatement);
        return e.withEnums(ListUtils.map(e.getEnums(), t -> visitRightPadded(t, JRightPadded.Location.ENUM_VALUE, p)));
    }

    public J visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        J.FieldAccess f = visitAndCast(fieldAccess, p, this::visitEach);
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = visitTypeName(f, p);
        f = visitAndCast(f, p, this::visitExpression);
        f = f.withTarget(visitAndCast(f.getTarget(), p));
        return f.withName(visitLeftPadded(f.getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p));
    }

    public J visitForEachLoop(J.ForEachLoop forLoop, P p) {
        J.ForEachLoop f = visitAndCast(forLoop, p, this::visitEach);
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = visitAndCast(f, p, this::visitStatement);
        f = f.withControl(visitAndCast(f.getControl(), p));
        return f.withBody(visitRightPadded(f.getBody(), JRightPadded.Location.FOR_BODY, p));
    }

    public J visitForEachControl(J.ForEachLoop.Control control, P p) {
        J.ForEachLoop.Control c = visitAndCast(control, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withVariable(visitRightPadded(c.getVariable(), JRightPadded.Location.FOREACH_VARIABLE, p));
        return c.withIterable(visitRightPadded(c.getIterable(), JRightPadded.Location.FOREACH_ITERABLE, p));
    }

    public J visitForLoop(J.ForLoop forLoop, P p) {
        J.ForLoop f = visitAndCast(forLoop, p, this::visitEach);
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = visitAndCast(f, p, this::visitStatement);
        f = f.withControl(visitAndCast(f.getControl(), p));
        return f.withBody(visitRightPadded(f.getBody(), JRightPadded.Location.FOR_BODY, p));
    }

    public J visitForControl(J.ForLoop.Control control, P p) {
        J.ForLoop.Control c = visitAndCast(control, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withInit(visitRightPadded(c.getInit(), JRightPadded.Location.FOR_INIT, p));
        c = c.withCondition(visitRightPadded(c.getCondition(), JRightPadded.Location.FOR_CONDITION, p));
        return c.withUpdate(ListUtils.map(c.getUpdate(), t -> visitRightPadded(t, JRightPadded.Location.FOR_UPDATE, p)));
    }

    public J visitIdentifier(J.Ident ident, P p) {
        J.Ident i = visitAndCast(ident, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        return visitAndCast(i, p, this::visitExpression);
    }

    public J visitElse(J.If.Else elze, P p) {
        J.If.Else e = visitAndCast(elze, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        return e.withBody(visitRightPadded(e.getBody(), JRightPadded.Location.IF_ELSE, p));
    }

    public J visitIf(J.If iff, P p) {
        J.If i = visitAndCast(iff, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = visitAndCast(i, p, this::visitStatement);
        i = i.withIfCondition(visitAndCast(i.getIfCondition(), p));
        i = i.withThenPart(visitRightPadded(i.getThenPart(), JRightPadded.Location.IF_THEN, p));
        i = i.withElsePart(visitAndCast(i.getElsePart(), p));
        return i;
    }

    public J visitImport(J.Import impoort, P p) {
        J.Import i = visitAndCast(impoort, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withStatik(i.getStatic() == null ? null :
                visitSpace(i.getStatic(), p));
        return i.withQualid(visitAndCast(i.getQualid(), p));
    }

    public J visitInstanceOf(J.InstanceOf instanceOf, P p) {
        J.InstanceOf i = visitAndCast(instanceOf, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = visitAndCast(i, p, this::visitExpression);
        i = i.withExpr(visitRightPadded(i.getExpr(), JRightPadded.Location.INSTANCEOF, p));
        return i.withClazz(visitAndCast(i.getClazz(), p));
    }

    public J visitLabel(J.Label label, P p) {
        J.Label l = visitAndCast(label, p, this::visitEach);
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        l = visitAndCast(l, p, this::visitStatement);
        return l.withStatement(visitAndCast(l.getStatement(), p));
    }

    public J visitLambda(J.Lambda lambda, P p) {
        J.Lambda l = visitAndCast(lambda, p, this::visitEach);
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        l = visitAndCast(l, p, this::visitExpression);
        l = l.withParameters(visitAndCast(l.getParameters(), p));
        l = l.withArrow(visitSpace(l.getArrow(), p));
        return l.withBody(visitAndCast(l.getBody(), p));
    }

    public J visitLiteral(J.Literal literal, P p) {
        J.Literal l = visitAndCast(literal, p, this::visitEach);
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        return visitAndCast(l, p, this::visitExpression);
    }

    public J visitMemberReference(J.MemberReference memberRef, P p) {
        J.MemberReference m = visitAndCast(memberRef, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = m.withContaining(visitAndCast(m.getContaining(), p));
        if (m.getTypeParameters() != null) {
            m = m.withTypeParameters(visitContainer(m.getTypeParameters(), JContainer.Location.TYPE_PARAMETER, p));
        }
        return m.withReference(visitLeftPadded(m.getReference(), JLeftPadded.Location.MEMBER_REFERENCE, p));
    }

    public J visitMethod(J.MethodDecl method, P p) {
        J.MethodDecl m = visitAndCast(method, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = visitAndCast(m, p, this::visitStatement);
        m = m.withAnnotations(ListUtils.map(m.getAnnotations(), a -> visitAndCast(a, p)));
        m = m.withModifiers(ListUtils.map(m.getModifiers(), e -> visitAndCast(e, p)));
        m = m.withModifiers(ListUtils.map(m.getModifiers(),
                mod -> mod.withPrefix(visitSpace(mod.getPrefix(), p))));
        if (m.getTypeParameters() != null) {
            m = m.withTypeParameters(visitContainer(m.getTypeParameters(), JContainer.Location.TYPE_PARAMETER, p));
        }
        m = m.withReturnTypeExpr(visitAndCast(m.getReturnTypeExpr(), p));
        m = m.withReturnTypeExpr(
                m.getReturnTypeExpr() == null ?
                        null :
                        visitTypeName(m.getReturnTypeExpr(), p));
        m = m.withName(visitAndCast(m.getName(), p));
        m = m.withParams(visitContainer(m.getParams(), JContainer.Location.METHOD_DECL_ARGUMENT, p));
        if (m.getThrows() != null) {
            m = m.withThrows(visitContainer(m.getThrows(), JContainer.Location.THROWS, p));
        }
        m = m.withThrows(visitTypeNames(m.getThrows(), p));
        return m.withBody(visitAndCast(m.getBody(), p));
    }

    public J visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = visitAndCast(method, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = visitAndCast(m, p, this::visitStatement);
        m = visitAndCast(m, p, this::visitExpression);
        if (m.getSelect() != null && m.getSelect().getElem() instanceof NameTree &&
                method.getType() != null && method.getType().hasFlags(Flag.Static)) {
            //noinspection unchecked
            m = m.withSelect(
                    (JRightPadded<Expression>) (JRightPadded<?>)
                            visitTypeName((JRightPadded<NameTree>) (JRightPadded<?>) m.getSelect(), p));
        }
        if (m.getSelect() != null) {
            m = m.withSelect(visitRightPadded(m.getSelect(), JRightPadded.Location.METHOD_SELECT, p));
        }
        if (m.getTypeParameters() != null) {
            m = m.withTypeParameters(visitContainer(m.getTypeParameters(), JContainer.Location.TYPE_PARAMETER, p));
        }
        m = m.withTypeParameters(visitTypeNames(m.getTypeParameters(), p));
        m = m.withName(visitAndCast(m.getName(), p));
        return m.withArgs(visitContainer(m.getArgs(), JContainer.Location.METHOD_INVOCATION_ARGUMENT, p));
    }

    public J visitMultiCatch(J.MultiCatch multiCatch, P p) {
        J.MultiCatch m = visitAndCast(multiCatch, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        return m.withAlternatives(ListUtils.map(m.getAlternatives(), t ->
                visitTypeName(visitRightPadded(t, JRightPadded.Location.CATCH_ALTERNATIVE, p), p)));
    }

    public J visitMultiVariable(J.VariableDecls multiVariable, P p) {
        J.VariableDecls m = visitAndCast(multiVariable, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = visitAndCast(m, p, this::visitStatement);
        m = m.withAnnotations(ListUtils.map(m.getAnnotations(), a -> visitAndCast(a, p)));
        m = m.withModifiers(Objects.requireNonNull(ListUtils.map(m.getModifiers(), e -> visitAndCast(e, p))));
        m = m.withModifiers(ListUtils.map(m.getModifiers(),
                mod -> mod.withPrefix(visitSpace(mod.getPrefix(), p))));
        m = m.withTypeExpr(visitAndCast(m.getTypeExpr(), p));
        m = m.withTypeExpr(m.getTypeExpr() == null ?
                null :
                visitTypeName(m.getTypeExpr(), p));
        m = m.withVarargs(m.getVarargs() == null ?
                null :
                visitSpace(m.getVarargs(), p));
        return m.withVars(ListUtils.map(m.getVars(), t -> visitRightPadded(t, JRightPadded.Location.NAMED_VARIABLE, p)));
    }

    public J visitNewArray(J.NewArray newArray, P p) {
        J.NewArray n = visitAndCast(newArray, p, this::visitEach);
        n = n.withPrefix(visitSpace(n.getPrefix(), p));
        n = visitAndCast(n, p, this::visitExpression);
        n = n.withTypeExpr(visitAndCast(n.getTypeExpr(), p));
        n = n.withTypeExpr(n.getTypeExpr() == null ?
                null :
                visitTypeName(n.getTypeExpr(), p));
        n = n.withDimensions(ListUtils.map(n.getDimensions(), d -> visitAndCast(d, p)));
        if (n.getInitializer() != null) {
            n = n.withInitializer(visitContainer(n.getInitializer(), JContainer.Location.NEW_ARRAY_INITIALIZER, p));
        }
        return n;
    }

    public J visitNewClass(J.NewClass newClass, P p) {
        J.NewClass n = visitAndCast(newClass, p, this::visitEach);
        n = n.withPrefix(visitSpace(n.getPrefix(), p));
        n = visitAndCast(n, p, this::visitStatement);
        n = visitAndCast(n, p, this::visitExpression);
        n = n.withNew(visitSpace(n.getNew(), p));
        n = n.withClazz(visitAndCast(n.getClazz(), p));
        n = n.withClazz(n.getClazz() == null ?
                null :
                visitTypeName(n.getClazz(), p));
        if (n.getArgs() != null) {
            n = n.withArgs(visitContainer(n.getArgs(), JContainer.Location.NEW_CLASS_ARGS, p));
        }
        return n.withBody(visitAndCast(n.getBody(), p));
    }

    public J visitPackage(J.Package pkg, P p) {
        J.Package pa = visitAndCast(pkg, p, this::visitEach);
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), p));
        return pa.withExpr(visitAndCast(pa.getExpr(), p));
    }

    public J visitParameterizedType(J.ParameterizedType type, P p) {
        J.ParameterizedType pt = visitAndCast(type, p, this::visitEach);
        pt = pt.withPrefix(visitSpace(pt.getPrefix(), p));
        pt = visitAndCast(pt, p, this::visitExpression);
        pt = pt.withClazz(visitAndCast(pt.getClazz(), p));
        pt = pt.withClazz(visitTypeName(pt.getClazz(), p));
        if (pt.getTypeParameters() != null) {
            pt = pt.withTypeParameters(visitContainer(pt.getTypeParameters(), JContainer.Location.TYPE_PARAMETER, p));
        }
        return pt.withTypeParameters(visitTypeNames(pt.getTypeParameters(), p));
    }

    public <T extends J> J visitParentheses(J.Parentheses<T> parens, P p) {
        J.Parentheses<T> pa = visitAndCast(parens, p, this::visitEach);
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), p));
        pa = visitAndCast(pa, p, this::visitExpression);
        return pa.withTree(visitRightPadded(pa.getTree(), JRightPadded.Location.PARENTHESES, p));
    }

    public J visitPrimitive(J.Primitive primitive, P p) {
        J.Primitive pr = visitAndCast(primitive, p, this::visitEach);
        pr = pr.withPrefix(visitSpace(pr.getPrefix(), p));
        return visitAndCast(pr, p, this::visitExpression);
    }

    public J visitReturn(J.Return retrn, P p) {
        J.Return r = visitAndCast(retrn, p, this::visitEach);
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        r = visitAndCast(r, p, this::visitStatement);
        return r.withExpr(visitAndCast(r.getExpr(), p));
    }

    public J visitSwitch(J.Switch switzh, P p) {
        J.Switch s = visitAndCast(switzh, p, this::visitEach);
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = visitAndCast(s, p, this::visitStatement);
        s = s.withSelector(visitAndCast(s.getSelector(), p));
        return s.withCases(visitAndCast(s.getCases(), p));
    }

    public J visitSynchronized(J.Synchronized synch, P p) {
        J.Synchronized s = visitAndCast(synch, p, this::visitEach);
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = visitAndCast(s, p, this::visitStatement);
        s = s.withLock(visitAndCast(s.getLock(), p));
        return s.withBody(visitAndCast(s.getBody(), p));
    }

    public J visitTernary(J.Ternary ternary, P p) {
        J.Ternary t = visitAndCast(ternary, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = visitAndCast(t, p, this::visitExpression);
        t = t.withCondition(visitAndCast(t.getCondition(), p));
        t = t.withTruePart(visitLeftPadded(t.getTruePart(), JLeftPadded.Location.TERNARY_TRUE, p));
        return t.withFalsePart(visitLeftPadded(t.getFalsePart(), JLeftPadded.Location.TERNARY_FALSE, p));
    }

    public J visitThrow(J.Throw thrown, P p) {
        J.Throw t = visitAndCast(thrown, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = visitAndCast(t, p, this::visitStatement);
        return t.withException(visitAndCast(t.getException(), p));
    }

    public J visitTry(J.Try tryable, P p) {
        J.Try t = visitAndCast(tryable, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = visitAndCast(t, p, this::visitStatement);
        if (t.getResources() != null) {
            t = t.withResources(visitContainer(t.getResources(), JContainer.Location.TRY_RESOURCES, p));
        }
        t = t.withBody(visitAndCast(t.getBody(), p));
        t = t.withCatches(ListUtils.map(t.getCatches(), c -> visitAndCast(c, p)));
        if (t.getFinally() != null) {
            t = t.withFinally(visitLeftPadded(t.getFinally(), JLeftPadded.Location.TRY_FINALLY, p));
        }
        return t;
    }

    public J visitTypeCast(J.TypeCast typeCast, P p) {
        J.TypeCast t = visitAndCast(typeCast, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = visitAndCast(t, p, this::visitExpression);
        t = t.withClazz(visitAndCast(t.getClazz(), p));
        t = t.withClazz(t.getClazz().withTree(visitTypeName(t.getClazz().getTree(), p)));
        return t.withExpr(visitAndCast(t.getExpr(), p));
    }

    public J visitTypeParameter(J.TypeParameter typeParam, P p) {
        J.TypeParameter t = visitAndCast(typeParam, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = t.withAnnotations(ListUtils.map(t.getAnnotations(), a -> visitAndCast(a, p)));
        t = t.withName(visitAndCast(t.getName(), p));
        if (t.getName() instanceof NameTree) {
            t = t.withName((Expression) visitTypeName((NameTree) t.getName(), p));
        }
        if (t.getBounds() != null) {
            t = t.withBounds(visitContainer(t.getBounds(), JContainer.Location.TYPE_BOUND, p));
        }
        return t.withBounds(visitTypeNames(t.getBounds(), p));
    }

    public J visitUnary(J.Unary unary, P p) {
        J.Unary u = visitAndCast(unary, p, this::visitEach);
        u = u.withPrefix(visitSpace(u.getPrefix(), p));
        u = visitAndCast(u, p, this::visitStatement);
        u = visitAndCast(u, p, this::visitExpression);
        u = u.withOperator(visitLeftPadded(u.getOperator(), JLeftPadded.Location.UNARY_OPERATOR, p));
        return u.withExpr(visitAndCast(u.getExpr(), p));
    }

    public J visitVariable(J.VariableDecls.NamedVar variable, P p) {
        J.VariableDecls.NamedVar v = visitAndCast(variable, p, this::visitEach);
        v = v.withPrefix(visitSpace(v.getPrefix(), p));
        v = v.withName(visitAndCast(v.getName(), p));
        if (v.getInitializer() != null) {
            v = v.withInitializer(visitLeftPadded(v.getInitializer(),
                    JLeftPadded.Location.VARIABLE_INITIALIZER, p));
        }
        return v;
    }

    public J visitWhileLoop(J.WhileLoop whileLoop, P p) {
        J.WhileLoop w = visitAndCast(whileLoop, p, this::visitEach);
        w = w.withPrefix(visitSpace(w.getPrefix(), p));
        w = visitAndCast(w, p, this::visitStatement);
        w = w.withCondition(visitAndCast(w.getCondition(), p));
        return w.withBody(visitRightPadded(w.getBody(), JRightPadded.Location.WHILE_BODY, p));
    }

    public J visitWildcard(J.Wildcard wildcard, P p) {
        J.Wildcard w = visitAndCast(wildcard, p, this::visitEach);
        w = w.withPrefix(visitSpace(w.getPrefix(), p));
        w = visitAndCast(w, p, this::visitExpression);
        w = w.withBoundedType(visitAndCast(w.getBoundedType(), p));
        if (w.getBoundedType() != null) {
            // i.e. not a "wildcard" type
            w = w.withBoundedType(visitTypeName(w.getBoundedType(), p));
        }
        return w;
    }

    @SuppressWarnings("unused")
    public <J2 extends J> JRightPadded<J2> visitRightPadded(JRightPadded<J2> right, JRightPadded.Location type, P p) {
        if (cursored) {
            setCursor(new Cursor(getCursor(), right));
        }

        J2 j = visitAndCast(right.getElem(), p);
        Space after = visitSpace(right.getAfter(), p);

        if (cursored) {
            setCursor(getCursor().getParent());
        }

        return (after == right.getAfter() && j == right.getElem()) ? right : new JRightPadded<>(j, after, right.getMarkers());
    }


    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, JLeftPadded.Location loc, P p) {
        if (cursored) {
            setCursor(new Cursor(getCursor(), left));
        }

        Space before = visitSpace(left.getBefore(), p);
        T t = left.getElem();

        if (t instanceof J) {
            //noinspection unchecked
            t = visitAndCast((J) left.getElem(), p);
        }

        if (cursored) {
            setCursor(getCursor().getParent());
        }

        return (before == left.getBefore() && t == left.getElem()) ? left : new JLeftPadded<>(before, t, left.getMarkers());
    }

    public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container,
                                                        JContainer.Location loc, P p) {
        if (cursored) {
            setCursor(new Cursor(getCursor(), container));
        }

        Space before = visitSpace(container.getBefore(), p);
        List<JRightPadded<J2>> js = ListUtils.map(container.getElem(), t -> visitRightPadded(t, loc.getElemLocation(), p));

        if (cursored) {
            setCursor(getCursor().getParent());
        }

        return js == container.getElem() && before == container.getBefore() ?
                container :
                JContainer.build(before, js, container.getMarkers());
    }

    /**
     * Check if a child AST element is in the same lexical scope as that of the AST element associated with the base
     * cursor. (i.e.: Are the variables and declarations visible in the base scope also visible to the child AST
     * element?)
     * <p>
     * The base lexical scope is first established by walking up the path of the base cursor to find its first enclosing
     * element. The child path is traversed by walking up the child path elements until either the base scope has
     * been found, a "terminating" element is encountered, or there are no more elements in the path.
     * <P><P>
     * A terminating element is one of the following:
     * <P><P>
     * <li>A static class declaration</li>
     * <li>An enumeration declaration</li>
     * <li>An interface declaration</li>
     * <li>An annotation declaration</li>
     *
     * @param base  A pointer within the AST that is used to establish the "base lexical scope".
     * @param child A pointer within the AST that will be traversed (up the tree) looking for an intersection with the base lexical scope.
     * @return true if the child is in within the lexical scope of the base
     */
    protected boolean isInSameNameScope(Cursor base, Cursor child) {
        //First establish the base scope by finding the first enclosing element.
        Tree baseScope = base.dropParentUntil(t -> t instanceof J.Block ||
                t instanceof J.MethodDecl ||
                t instanceof J.Try ||
                t instanceof J.ForLoop ||
                t instanceof J.ForEachLoop).getValue();

        //Now walk up the child path looking for the base scope.
        for (Iterator<Object> it = child.getPath(); it.hasNext(); ) {
            Object childScope = it.next();
            if (childScope instanceof J.ClassDecl) {
                J.ClassDecl childClass = (J.ClassDecl) childScope;
                if (!(childClass.getKind().getElem().equals(J.ClassDecl.Kind.Class)) ||
                        childClass.hasModifier("static")) {
                    //Short circuit the search if a terminating element is encountered.
                    return false;
                }
            }
            if (childScope instanceof Tree && baseScope.isScope((Tree) childScope)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a child AST element is in the same lexical scope as that of the AST element associated with the current
     * cursor.
     * <p>
     * See {@link JavaVisitor#isInSameNameScope}
     *
     * @param child A pointer to an element within the abstract syntax tree
     * @return true if the child is in within the lexical scope of the current cursor
     */
    protected boolean isInSameNameScope(Cursor child) {
        return isInSameNameScope(getCursor(), child);
    }
}
