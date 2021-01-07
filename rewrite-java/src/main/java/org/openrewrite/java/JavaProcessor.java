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
import org.openrewrite.TreeProcessor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.util.Iterator;
import java.util.List;

public class JavaProcessor<P> extends TreeProcessor<J, P> implements JavaVisitor<J, P> {
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

    private <N extends NameTree> JLeftPadded<N> visitTypeName(@Nullable JLeftPadded<N> nameTree, P p) {
        return nameTree == null ? null : nameTree.withElem(visitTypeName(nameTree.getElem(), p));
    }

    private <N extends NameTree> JRightPadded<N> visitTypeName(@Nullable JRightPadded<N> nameTree, P p) {
        return nameTree == null ? null : nameTree.withElem(visitTypeName(nameTree.getElem(), p));
    }

    private <J2 extends J> JContainer<J2> visitTypeNames(@Nullable JContainer<J2> nameTrees, P p) {
        if (nameTrees == null) {
            return null;
        }
        @SuppressWarnings("unchecked") List<JRightPadded<J2>> js = ListUtils.map(nameTrees.getElem(),
                t -> t.getElem() instanceof NameTree ? (JRightPadded<J2>) visitTypeName((JRightPadded<NameTree>) t, p) : t);
        return js == nameTrees.getElem() ? nameTrees : JContainer.build(nameTrees.getBefore(), js);
    }

    @Override
    public J visitAnnotatedType(J.AnnotatedType annotatedType, P p) {
        J.AnnotatedType a = call(annotatedType, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitExpression);
        a = a.withAnnotations(call(a.getAnnotations(), p));
        a = a.withTypeExpr(call(a.getTypeExpr(), p));
        return a.withTypeExpr(visitTypeName(a.getTypeExpr(), p));
    }

    @Override
    public J visitAnnotation(J.Annotation annotation, P p) {
        J.Annotation a = call(annotation, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitExpression);
        a = a.withArgs(call(a.getArgs(), p));
        a = a.withAnnotationType(call(a.getAnnotationType(), p));
        return a.withAnnotationType(visitTypeName(a.getAnnotationType(), p));
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
        J.ArrayAccess a = call(arrayAccess, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitExpression);
        a = a.withIndexed(call(a.getIndexed(), p));
        a = a.withDimension(call(a.getDimension(), p));
        return a;
    }

    @Override
    public J visitArrayDimension(J.ArrayDimension arrayDimension, P p) {
        J.ArrayDimension a = call(arrayDimension, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withIndex(call(a.getIndex(), p));
        return a;
    }

    @Override
    public J visitArrayType(J.ArrayType arrayType, P p) {
        J.ArrayType a = call(arrayType, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitExpression);
        a.withElementType(call(a.getElementType(), p));
        return a.withElementType(visitTypeName(a.getElementType(), p));
    }

    @Override
    public J visitAssert(J.Assert azzert, P p) {
        J.Assert a = call(azzert, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitStatement);
        return a.withCondition(call(a.getCondition(), p));
    }

    @Override
    public J visitAssign(J.Assign assign, P p) {
        J.Assign a = call(assign, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitStatement);
        a = call(a, p, this::visitExpression);
        a = a.withVariable(call(a.getVariable(), p));
        return a.withAssignment(call(a.getAssignment(), p));
    }

    @Override
    public J visitAssignOp(J.AssignOp assignOp, P p) {
        J.AssignOp a = call(assignOp, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitStatement);
        a = call(a, p, this::visitExpression);
        a = a.withVariable(call(a.getVariable(), p));
        return a.withAssignment(call(a.getAssignment(), p));
    }

    @Override
    public J visitBinary(J.Binary binary, P p) {
        J.Binary b = call(binary, p, this::visitEach);
        b = b.withPrefix(visitSpace(b.getPrefix(), p));
        b = call(b, p, this::visitExpression);
        b = b.withLeft(call(b.getLeft(), p));
        return b.withRight(call(b.getRight(), p));
    }

    @Override
    public J visitBlock(J.Block block, P p) {
        J.Block b = call(block, p, this::visitEach);
        b = b.withStatik(visitSpace(b.getStatic(), p));
        b = b.withPrefix(visitSpace(b.getPrefix(), p));
        b = call(b, p, this::visitStatement);
        b = b.withStatements(ListUtils.map(b.getStatements(), t -> call(t, p)));
        return b.withEnd(visitSpace(b.getEnd(), p));
    }

    @Override
    public J visitBreak(J.Break breakStatement, P p) {
        J.Break b = call(breakStatement, p, this::visitEach);
        b = b.withPrefix(visitSpace(b.getPrefix(), p));
        b = call(b, p, this::visitStatement);
        return b.withLabel(call(b.getLabel(), p));
    }

    @Override
    public J visitCase(J.Case caze, P p) {
        J.Case c = call(caze, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = call(c, p, this::visitStatement);
        c = c.withPattern(call(c.getPattern(), p));
        return c.withStatements(call(c.getStatements(), p));
    }

    @Override
    public J visitCatch(J.Try.Catch catzh, P p) {
        J.Try.Catch c = call(catzh, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withParam(call(c.getParam(), p));
        return c.withBody(call(c.getBody(), p));
    }

    @Override
    public J visitClassDecl(J.ClassDecl classDecl, P p) {
        J.ClassDecl c = call(classDecl, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = call(c, p, this::visitStatement);
        c = c.withAnnotations(call(c.getAnnotations(), p));
        c = c.withModifiers(call(c.getModifiers(), p));
        c = c.withTypeParameters(call(c.getTypeParameters(), p));
        c = c.withName(call(c.getName(), p));
        c = c.withExtends(call(c.getExtends(), p));
        c = c.withExtends(visitTypeName(c.getExtends(), p));
        c = c.withImplements(call(c.getImplements(), p));
        c = c.withImplements(visitTypeNames(c.getImplements(), p));
        return c.withBody(call(c.getBody(), p));
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        J.CompilationUnit c = call(cu, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withPackageDecl(call(c.getPackageDecl(), p));
        c = c.withImports(ListUtils.map(c.getImports(), t -> call(t, p)));
        c = c.withClasses(call(c.getClasses(), p));
        return c.withEof(visitSpace(c.getEof(), p));
    }

    @Override
    public J visitContinue(J.Continue continueStatement, P p) {
        J.Continue c = call(continueStatement, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = call(c, p, this::visitStatement);
        return c.withLabel(call(c.getLabel(), p));
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        J.DoWhileLoop d = call(doWhileLoop, p, this::visitEach);
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = call(d, p, this::visitStatement);
        d = d.withWhileCondition(call(d.getWhileCondition(), p));
        return d.withBody(call(d.getBody(), p));
    }

    @Override
    public J visitEmpty(J.Empty empty, P p) {
        J.Empty e = call(empty, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = call(e, p, this::visitStatement);
        e = call(e, p, this::visitExpression);
        return e;
    }

    @Override
    public J visitEnumValue(J.EnumValue enoom, P p) {
        J.EnumValue e = call(enoom, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withName(call(e.getName(), p));
        return e.withInitializer(call(e.getInitializer(), p));
    }

    @Override
    public J visitEnumValueSet(J.EnumValueSet enums, P p) {
        J.EnumValueSet e = call(enums, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = call(e, p, this::visitStatement);
        return e.withEnums(ListUtils.map(e.getEnums(), t -> call(t, p)));
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        J.FieldAccess f = call(fieldAccess, p, this::visitEach);
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = visitTypeName(f, p);
        f = call(f, p, this::visitExpression);
        f = f.withTarget(call(f.getTarget(), p));
        return f.withName(call(f.getName(), p));
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forLoop, P p) {
        J.ForEachLoop f = call(forLoop, p, this::visitEach);
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = call(f, p, this::visitStatement);
        f = f.withControl(f.getControl().withVariable(call(f.getControl().getVariable(), p)));
        f = f.withControl(f.getControl().withIterable(call(f.getControl().getIterable(), p)));
        return f.withBody(call(f.getBody(), p));
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop, P p) {
        J.ForLoop f = call(forLoop, p, this::visitEach);
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = call(f, p, this::visitStatement);
        f = f.withControl(f.getControl().withInit(call(f.getControl().getInit(), p)));
        f = f.withControl(f.getControl().withCondition(call(f.getControl().getCondition(), p)));
        f = f.withControl(f.getControl().withUpdate(ListUtils.map(f.getControl().getUpdate(), t -> call(t, p))));
        return f.withBody(call(f.getBody(), p));
    }

    @Override
    public J visitIdentifier(J.Ident ident, P p) {
        J.Ident i = call(ident, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        return call(i, p, this::visitExpression);
    }

    @Override
    public J visitElse(J.If.Else elze, P p) {
        J.If.Else e = call(elze, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        return e.withBody(call(e.getBody(), p));
    }

    @Override
    public J visitIf(J.If iff, P p) {
        J.If i = call(iff, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = call(i, p, this::visitStatement);
        i = i.withIfCondition(call(i.getIfCondition(), p));
        i = i.withThenPart(call(i.getThenPart(), p));
        i = i.withElsePart(call(i.getElsePart(), p));
        return i;
    }

    @Override
    public J visitImport(J.Import impoort, P p) {
        J.Import i = call(impoort, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withStatik(visitSpace(i.getStatic(), p));
        return i.withQualid(call(i.getQualid(), p));
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, P p) {
        J.InstanceOf i = call(instanceOf, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = call(i, p, this::visitExpression);
        i = i.withExpr(call(i.getExpr(), p));
        return i.withClazz(call(i.getClazz(), p));
    }

    @Override
    public J visitLabel(J.Label label, P p) {
        J.Label l = call(label, p, this::visitEach);
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        l = call(l, p, this::visitStatement);
        return l.withStatement(call(l.getStatement(), p));
    }

    @Override
    public J visitLambda(J.Lambda lambda, P p) {
        J.Lambda l = call(lambda, p, this::visitEach);
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        l = call(l, p, this::visitExpression);
        l = l.withParameters(call(l.getParameters(), p));
        l = l.withArrow(visitSpace(l.getArrow(), p));
        return l.withBody(call(l.getBody(), p));
    }

    @Override
    public J visitLiteral(J.Literal literal, P p) {
        J.Literal l = call(literal, p, this::visitEach);
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        return call(l, p, this::visitExpression);
    }

    @Override
    public J visitMemberReference(J.MemberReference memberRef, P p) {
        J.MemberReference m = call(memberRef, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = m.withContaining(call(m.getContaining(), p));
        m = m.withTypeParameters(call(m.getTypeParameters(), p));
        return m.withReference(call(m.getReference(), p));
    }

    @Override
    public J visitMethod(J.MethodDecl method, P p) {
        J.MethodDecl m = call(method, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = call(m, p, this::visitStatement);
        m = m.withAnnotations(call(m.getAnnotations(), p));
        m = m.withModifiers(call(m.getModifiers(), p));
        m = m.withTypeParameters(call(m.getTypeParameters(), p));
        m = m.withReturnTypeExpr(call(m.getReturnTypeExpr(), p));
        m = m.withReturnTypeExpr(visitTypeName(m.getReturnTypeExpr(), p));
        m = m.withName(call(m.getName(), p));
        m = m.withParams(call(m.getParams(), p));
        m = m.withParams(call(m.getParams(), p));
        m = m.withThrows(call(m.getThrows(), p));
        m = m.withThrows(visitTypeNames(m.getThrows(), p));
        return m.withBody(call(m.getBody(), p));
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = call(method, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = call(m, p, this::visitStatement);
        m = call(m, p, this::visitExpression);
        if (m.getSelect() != null && m.getSelect().getElem() instanceof NameTree &&
                method.getType() != null && method.getType().hasFlags(Flag.Static)) {
            //noinspection unchecked
            m = m.withSelect(
                    (JRightPadded<Expression>) (JRightPadded<?>)
                            visitTypeName((JRightPadded<NameTree>) (JRightPadded<?>) m.getSelect(), p));
        }
        m = m.withSelect(call(m.getSelect(), p));
        m = m.withTypeParameters(call(m.getTypeParameters(), p));
        m = m.withTypeParameters(visitTypeNames(m.getTypeParameters(), p));
        m = m.withName(call(m.getName(), p));
        return m.withArgs(call(m.getArgs(), p));
    }

    @Override
    public J visitMultiCatch(J.MultiCatch multiCatch, P p) {
        J.MultiCatch m = call(multiCatch, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        return m.withAlternatives(ListUtils.map(m.getAlternatives(), t -> visitTypeName(call(t, p), p)));
    }

    @Override
    public J visitMultiVariable(J.VariableDecls multiVariable, P p) {
        J.VariableDecls m = call(multiVariable, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = call(m, p, this::visitStatement);
        m = m.withModifiers(call(m.getModifiers(), p));
        m = m.withAnnotations(call(m.getAnnotations(), p));
        m = m.withTypeExpr(call(m.getTypeExpr(), p));
        m = m.withTypeExpr(visitTypeName(m.getTypeExpr(), p));
        m = m.withVarargs(visitSpace(m.getVarargs(), p));
        return m.withVars(ListUtils.map(m.getVars(), t -> call(t, p)));
    }

    @Override
    public J visitNewArray(J.NewArray newArray, P p) {
        J.NewArray n = call(newArray, p, this::visitEach);
        n = n.withPrefix(visitSpace(n.getPrefix(), p));
        n = call(n, p, this::visitExpression);
        n = n.withTypeExpr(call(n.getTypeExpr(), p));
        n = n.withTypeExpr(visitTypeName(n.getTypeExpr(), p));
        n = n.withDimensions(call(n.getDimensions(), p));
        return n.withInitializer(call(n.getInitializer(), p));
    }

    @Override
    public J visitNewClass(J.NewClass newClass, P p) {
        J.NewClass n = call(newClass, p, this::visitEach);
        n = n.withPrefix(visitSpace(n.getPrefix(), p));
        n = call(n, p, this::visitStatement);
        n = call(n, p, this::visitExpression);
        n = n.withNew(visitSpace(n.getNew(), p));
        n = n.withClazz(call(n.getClazz(), p));
        n = n.withClazz(visitTypeName(n.getClazz(), p));
        n = n.withArgs(call(n.getArgs(), p));
        return n.withBody(call(n.getBody(), p));
    }

    @Override
    public J visitPackage(J.Package pkg, P p) {
        J.Package pa = call(pkg, p, this::visitEach);
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), p));
        return pa.withExpr(call(pa.getExpr(), p));
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type, P p) {
        J.ParameterizedType pt = call(type, p, this::visitEach);
        pt = pt.withPrefix(visitSpace(pt.getPrefix(), p));
        pt = call(pt, p, this::visitExpression);
        pt = pt.withClazz(call(pt.getClazz(), p));
        pt = pt.withClazz(visitTypeName(pt.getClazz(), p));
        pt = pt.withTypeParameters(call(pt.getTypeParameters(), p));
        return pt.withTypeParameters(visitTypeNames(pt.getTypeParameters(), p));
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens, P p) {
        J.Parentheses<T> pa = call(parens, p, this::visitEach);
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), p));
        pa = call(pa, p, this::visitExpression);
        return pa.withTree(call(pa.getTree(), p));
    }

    @Override
    public J visitPrimitive(J.Primitive primitive, P p) {
        J.Primitive pr = call(primitive, p, this::visitEach);
        pr = pr.withPrefix(visitSpace(pr.getPrefix(), p));
        return call(pr, p, this::visitExpression);
    }

    @Override
    public J visitReturn(J.Return retrn, P p) {
        J.Return r = call(retrn, p, this::visitEach);
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        r = call(r, p, this::visitStatement);
        return r.withExpr(call(r.getExpr(), p));
    }

    @Override
    public J visitSwitch(J.Switch switzh, P p) {
        J.Switch s = call(switzh, p, this::visitEach);
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = call(s, p, this::visitStatement);
        s = s.withSelector(call(s.getSelector(), p));
        return s.withCases(call(s.getCases(), p));
    }

    @Override
    public J visitSynchronized(J.Synchronized synch, P p) {
        J.Synchronized s = call(synch, p, this::visitEach);
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = call(s, p, this::visitStatement);
        s = s.withLock(call(s.getLock(), p));
        return s.withBody(call(s.getBody(), p));
    }

    @Override
    public J visitTernary(J.Ternary ternary, P p) {
        J.Ternary t = call(ternary, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = call(t, p, this::visitExpression);
        t = t.withCondition(call(t.getCondition(), p));
        t = t.withTruePart(call(t.getTruePart(), p));
        return t.withFalsePart(call(t.getFalsePart(), p));
    }

    @Override
    public J visitThrow(J.Throw thrown, P p) {
        J.Throw t = call(thrown, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = call(t, p, this::visitStatement);
        return t.withException(call(t.getException(), p));
    }

    @Override
    public J visitTry(J.Try tryable, P p) {
        J.Try t = call(tryable, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = call(t, p, this::visitStatement);
        t = t.withResources(call(t.getResources(), p));
        t = t.withBody(call(t.getBody(), p));
        t = t.withCatches(call(t.getCatches(), p));
        return t.withFinally(call(t.getFinally(), p));
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast, P p) {
        J.TypeCast t = call(typeCast, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = call(t, p, this::visitExpression);
        t = t.withClazz(call(t.getClazz(), p));
        t = t.withClazz(t.getClazz().withTree(visitTypeName(t.getClazz().getTree(), p)));
        return t.withExpr(call(t.getExpr(), p));
    }

    @Override
    public J visitTypeParameter(J.TypeParameter typeParam, P p) {
        J.TypeParameter t = call(typeParam, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = t.withAnnotations(call(t.getAnnotations(), p));
        t = t.withName(call(t.getName(), p));
        if (t.getName() instanceof NameTree) {
            t = t.withName((Expression) visitTypeName((NameTree) t.getName(), p));
        }
        t = t.withBounds(call(t.getBounds(), p));
        return t.withBounds(visitTypeNames(t.getBounds(), p));
    }

    @Override
    public J visitUnary(J.Unary unary, P p) {
        J.Unary u = call(unary, p, this::visitEach);
        u = u.withPrefix(visitSpace(u.getPrefix(), p));
        u = call(u, p, this::visitStatement);
        u = call(u, p, this::visitExpression);
        return u.withExpr(call(u.getExpr(), p));
    }

    @Override
    public J visitVariable(J.VariableDecls.NamedVar variable, P p) {
        J.VariableDecls.NamedVar v = call(variable, p, this::visitEach);
        v = v.withPrefix(visitSpace(v.getPrefix(), p));
        v = v.withName(call(v.getName(), p));
        return v.withInitializer(call(v.getInitializer(), p));
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop, P p) {
        J.WhileLoop w = call(whileLoop, p, this::visitEach);
        w = w.withPrefix(visitSpace(w.getPrefix(), p));
        w = call(w, p, this::visitStatement);
        w = w.withCondition(call(w.getCondition(), p));
        return w.withBody(call(w.getBody(), p));
    }

    @Override
    public J visitWildcard(J.Wildcard wildcard, P p) {
        J.Wildcard w = call(wildcard, p, this::visitEach);
        w = w.withPrefix(visitSpace(w.getPrefix(), p));
        w = call(w, p, this::visitExpression);
        w = w.withBoundedType(call(w.getBoundedType(), p));
        return w.withBoundedType(visitTypeName(w.getBoundedType(), p));
    }

    @Nullable
    protected <J2 extends J> JRightPadded<J2> call(@Nullable JRightPadded<J2> right, P p) {
        if (right == null) {
            return null;
        }
        J2 j = call(right.getElem(), p);
        return j == right.getElem() ? right : new JRightPadded<>(j, visitSpace(right.getAfter(), p));
    }

    @Nullable
    protected <J2 extends J> JLeftPadded<J2> call(@Nullable JLeftPadded<J2> left, P p) {
        if (left == null) {
            return null;
        }
        J2 j = call(left.getElem(), p);
        return j == left.getElem() ? left : new JLeftPadded<>(visitSpace(left.getBefore(), p), j);
    }

    @Nullable
    protected <J2 extends J> JContainer<J2> call(@Nullable JContainer<J2> container, P p) {
        if (container == null) {
            return null;
        }
        List<JRightPadded<J2>> js = ListUtils.map(container.getElem(), t -> call(t, p));
        return js == container.getElem() ? container : JContainer.build(container.getBefore(), js);
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
        Tree baseScope = base.getPathAsStream()
                .filter(t -> t instanceof J.Block ||
                        t instanceof J.MethodDecl ||
                        t instanceof J.Try ||
                        t instanceof J.ForLoop ||
                        t instanceof J.ForEachLoop)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("The base cursor does not have an scoped context."));

        //Now walk up the child path looking for the base scope.
        for (Iterator<Tree> it = child.getPath(); it.hasNext(); ) {
            Tree childScope = it.next();
            if (childScope instanceof J.ClassDecl) {
                J.ClassDecl childClass = (J.ClassDecl) childScope;
                if (!(childClass.getKind().getElem().equals(J.ClassDecl.Kind.Class)) ||
                        childClass.hasModifier("static")) {
                    //Short circuit the search if a terminating element is encountered.
                    break;
                }
            }
            if (baseScope.isScope(childScope)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a child AST element is in the same lexical scope as that of the AST element associated with the current
     * cursor.
     * <p>
     * See {@link JavaProcessor#isInSameNameScope}
     *
     * @param child A pointer to an element within the abstract syntax tree
     * @return true if the child is in within the lexical scope of the current cursor
     */
    protected boolean isInSameNameScope(Cursor child) {
        return isInSameNameScope(getCursor(), child);
    }
}
