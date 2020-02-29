/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.visitor.refactor;

import org.openrewrite.RefactorVisitorSupport;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaSourceVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public abstract class JavaRefactorVisitor extends JavaSourceVisitor<J> implements RefactorVisitorSupport {
    protected Formatter formatter;

    @Override
    public J defaultTo(Tree t) {
        return (J) t;
    }

    @Override
    public J visitAnnotation(J.Annotation annotation) {
        J.Annotation a = refactor(annotation, this::visitExpression);
        a = refactor(a, super::visitAnnotation);
        a = a.withArgs(refactor(a.getArgs() == null ? null :
                a.getArgs().withArgs(refactor(a.getArgs().getArgs()))));
        return a.withAnnotationType(refactor(a.getAnnotationType()));
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess) {
        J.ArrayAccess a = refactor(arrayAccess, this::visitExpression);
        a = refactor(a, super::visitArrayAccess);
        a = a.withIndexed(refactor(a.getIndexed()));
        return a.withDimension(a.getDimension().withIndex(refactor(a.getDimension().getIndex())));
    }

    @Override
    public J visitArrayType(J.ArrayType arrayType) {
        J.ArrayType a = refactor(arrayType, super::visitArrayType);
        a = a.withDimensions(refactor(a.getDimensions()));
        return a.withElementType(refactor(a.getElementType()));
    }

    @Override
    public J visitAssert(J.Assert azzert) {
        J.Assert a = refactor(azzert, this::visitStatement);
        a = refactor(a, super::visitAssert);
        return a.withCondition(refactor(a.getCondition()));
    }

    @Override
    public J visitAssign(J.Assign assign) {
        J.Assign a = refactor(assign, this::visitStatement);
        a = refactor(a, this::visitExpression);
        a = refactor(a, super::visitAssign);
        a = a.withVariable(refactor(a.getVariable()));
        return a.withAssignment(refactor(a.getAssignment()));
    }

    @Override
    public J visitAssignOp(J.AssignOp assignOp) {
        J.AssignOp a = refactor(assignOp, this::visitStatement);
        a = refactor(a, this::visitExpression);
        a = refactor(a, super::visitAssignOp);
        a = a.withVariable(refactor(a.getVariable()));
        return a.withAssignment(refactor(a.getAssignment()));
    }

    @Override
    public J visitBinary(J.Binary binary) {
        J.Binary b = refactor(binary, this::visitExpression);
        b = refactor(b, super::visitBinary);
        b = b.withLeft(refactor(b.getLeft()));
        return b.withRight(refactor(b.getRight()));
    }

    @Override
    public J visitBlock(J.Block<Tree> block) {
        J.Block<Tree> b = refactor(block, this::visitStatement);
        b = refactor(b, super::visitBlock);
        return b.withStatements(refactor(b.getStatements()));
    }

    @Override
    public J visitBreak(J.Break breakStatement) {
        J.Break b = refactor(breakStatement, this::visitStatement);
        b = refactor(b, super::visitBreak);
        return b.withLabel(refactor(b.getLabel()));
    }

    @Override
    public J visitCase(J.Case caze) {
        J.Case c = refactor(caze, this::visitStatement);
        c = refactor(c, super::visitCase);
        c = c.withPattern(refactor(c.getPattern()));
        return c.withStatements(refactor(c.getStatements()));
    }

    @Override
    public J visitCatch(J.Try.Catch catzh) {
        J.Try.Catch c = refactor(catzh, super::visitCatch);
        c = c.withParam(refactor(c.getParam()));
        return c.withBody(refactor(c.getBody()));
    }

    @Override
    public J visitClassDecl(J.ClassDecl classDecl) {
        J.ClassDecl c = refactor(classDecl, this::visitStatement);
        c = refactor(c, super::visitClassDecl);
        c = c.withAnnotations(refactor(c.getAnnotations()));
        c = c.withModifiers(refactor(c.getModifiers()));
        c = c.withTypeParameters(refactor(c.getTypeParameters()));
        c = c.withTypeParameters(c.getTypeParameters() == null ? null :
                c.getTypeParameters().withParams(refactor(c.getTypeParameters().getParams())));
        c = c.withKind(refactor(c.getKind()));
        c = c.withName(refactor(c.getName()));
        c = c.withExtends(refactor(c.getExtends()));
        c = c.withImplements(refactor(c.getImplements()));
        return c.withBody(refactor(c.getBody()));
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu) {
        formatter = new Formatter(cu);
        J.CompilationUnit c = refactor(cu, super::visitCompilationUnit);
        c = c.withPackageDecl(refactor(c.getPackageDecl()));
        c = c.withImports(refactor(c.getImports()));
        return c.withClasses(refactor(c.getClasses()));
    }

    @Override
    public J visitContinue(J.Continue continueStatement) {
        J.Continue c = refactor(continueStatement, this::visitStatement);
        c = refactor(c, super::visitContinue);
        return c.withLabel(refactor(c.getLabel()));
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop) {
        J.DoWhileLoop d = refactor(doWhileLoop, this::visitStatement);
        d = refactor(d, super::visitDoWhileLoop);
        d = d.withWhileCondition(refactor(d.getWhileCondition()));
        return d.withBody(refactor(d.getBody()));
    }

    @Override
    public J visitEmpty(J.Empty empty) {
        J.Empty e = refactor(empty, this::visitStatement);
        e = refactor(e, this::visitExpression);
        return super.visitEmpty(e);
    }

    @Override
    public J visitEnumValue(J.EnumValue enoom) {
        J.EnumValue e = refactor(enoom, this::visitStatement);
        e = refactor(e, super::visitEnumValue);
        e = e.withName(refactor(e.getName()));
        return e.withInitializer(refactor(e.getInitializer()));
    }

    @Override
    public J visitEnumValueSet(J.EnumValueSet enums) {
        J.EnumValueSet e = refactor(enums, this::visitStatement);
        e = refactor(e, super::visitEnumValueSet);
        return e.withEnums(refactor(e.getEnums()));
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess) {
        J.FieldAccess f = refactor(fieldAccess, this::visitExpression);
        f = refactor(f, super::visitFieldAccess);
        f = f.withTarget(refactor(f.getTarget()));
        return f.withName(refactor(f.getName()));
    }

    @Override
    public J visitFinally(J.Try.Finally finallie) {
        J.Try.Finally f = refactor(finallie, super::visitFinally);
        return f.withBody(refactor(f.getBody()));
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forLoop) {
        J.ForEachLoop f = refactor(forLoop, this::visitStatement);
        f = refactor(f, super::visitForEachLoop);
        f = f.withControl(f.getControl().withVariable(refactor(f.getControl().getVariable())));
        f = f.withControl(f.getControl().withIterable(refactor(f.getControl().getIterable())));
        return f.withBody(refactor(f.getBody()));
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop) {
        J.ForLoop f = refactor(forLoop, this::visitStatement);
        f = refactor(f, super::visitForLoop);
        f = f.withControl(f.getControl().withInit(refactor(f.getControl().getInit())));
        f = f.withControl(f.getControl().withCondition(refactor(f.getControl().getCondition())));
        f = f.withControl(f.getControl().withUpdate(refactor(f.getControl().getUpdate())));
        return f.withBody(refactor(f.getBody()));
    }

    @Override
    public J visitIdentifier(J.Ident ident) {
        return super.visitIdentifier(refactor(ident, this::visitExpression));
    }

    @Override
    public J visitIf(J.If iff) {
        J.If i = refactor(iff, this::visitStatement);
        i = refactor(i, super::visitIf);
        i = i.withIfCondition(refactor(i.getIfCondition()));
        i = i.withThenPart(refactor(i.getThenPart()));
        return i.withElsePart(refactor(i.getElsePart()));
    }

    @Override
    public J visitElse(J.If.Else elze) {
        J.If.Else e = refactor(elze, super::visitElse);
        return e.withStatement(refactor(e.getStatement()));
    }

    @Override
    public J visitImport(J.Import impoort) {
        J.Import i = refactor(impoort, super::visitImport);
        return i.withQualid(refactor(i.getQualid()));
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf) {
        J.InstanceOf i = refactor(instanceOf, this::visitExpression);
        i = refactor(i, super::visitInstanceOf);
        i = i.withExpr(refactor(i.getExpr()));
        return i.withClazz(refactor(i.getClazz()));
    }

    @Override
    public J visitLabel(J.Label label) {
        J.Label l = refactor(label, this::visitStatement);
        l = refactor(l, super::visitLabel);
        return l.withStatement(refactor(l.getStatement()));
    }

    @Override
    public J visitLambda(J.Lambda lambda) {
        J.Lambda l = refactor(lambda, this::visitExpression);
        l = refactor(l, super::visitLambda);
        l = l.withParamSet(refactor(l.getParamSet()));
        l = l.withArrow(refactor(l.getArrow()));
        return l.withBody(refactor(l.getBody()));
    }

    @Override
    public J visitLiteral(J.Literal literal) {
        return super.visitLiteral(refactor(literal, this::visitExpression));
    }

    @Override
    public J visitMemberReference(J.MemberReference memberRef) {
        J.MemberReference m = refactor(memberRef, super::visitMemberReference);
        m = m.withContaining(refactor(m.getContaining()));
        m = m.withTypeParameters(refactor(m.getTypeParameters()));
        m = m.withTypeParameters(m.getTypeParameters() == null ? null :
                m.getTypeParameters().withParams(refactor(m.getTypeParameters().getParams())));
        return m.withReference(refactor(m.getReference()));
    }

    @Override
    public J visitMethod(J.MethodDecl method) {
        J.MethodDecl m = refactor(method, super::visitMethod);
        m = m.withAnnotations(refactor(m.getAnnotations()));
        m = m.withTypeParameters(refactor(m.getTypeParameters()));
        m = m.withTypeParameters(m.getTypeParameters() == null ? null :
                m.getTypeParameters().withParams(refactor(m.getTypeParameters().getParams())));
        m = m.withReturnTypeExpr(refactor(m.getReturnTypeExpr()));
        m = m.withParams(refactor(m.getParams()));
        m = m.withParams(m.getParams().withParams(refactor(m.getParams().getParams())));
        m = m.withThrows(refactor(m.getThrows()));
        return m.withBody(refactor(m.getBody()));
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method) {
        J.MethodInvocation m = refactor(method, this::visitStatement);
        m = refactor(m, this::visitExpression);
        m = refactor(m, super::visitMethodInvocation);
        m = m.withSelect(refactor(m.getSelect()));
        m = m.withTypeParameters(refactor(m.getTypeParameters()));
        m = m.withTypeParameters(m.getTypeParameters() == null ? null :
                m.getTypeParameters().withParams(refactor(m.getTypeParameters().getParams())));
        m = m.withName(refactor(m.getName()));
        m = m.withArgs(refactor(m.getArgs()));
        return m = m.withArgs(m.getArgs().withArgs(refactor(m.getArgs().getArgs())));
    }

    @Override
    public J visitMultiCatch(J.MultiCatch multiCatch) {
        J.MultiCatch m = refactor(multiCatch, super::visitMultiCatch);
        return m.withAlternatives(refactor(m.getAlternatives()));
    }

    @Override
    public J visitMultiVariable(J.VariableDecls multiVariable) {
        J.VariableDecls m = refactor(multiVariable, this::visitStatement);
        m = refactor(m, super::visitMultiVariable);
        m = m.withModifiers(refactor(m.getModifiers()));
        m = m.withAnnotations(refactor(m.getAnnotations()));
        m = m.withTypeExpr(refactor(m.getTypeExpr()));
        m = m.withVars(refactor(m.getVars()));
        return m.withVarargs(refactor(m.getVarargs()));
    }

    @Override
    public J visitNewArray(J.NewArray newArray) {
        J.NewArray n = refactor(newArray, this::visitExpression);
        n = refactor(n, super::visitNewArray);
        n = n.withTypeExpr(refactor(n.getTypeExpr()));
        return n.withInitializer(refactor(n.getInitializer()));
    }

    @Override
    public J visitNewClass(J.NewClass newClass) {
        J.NewClass n = refactor(newClass, this::visitStatement);
        n = refactor(n, this::visitExpression);
        n = refactor(n, super::visitNewClass);
        n = n.withClazz(refactor(n.getClazz()));
        n = n.withArgs(refactor(n.getArgs()));
        return n.withBody(refactor(n.getBody()));
    }

    @Override
    public J visitPackage(J.Package pkg) {
        J.Package p = refactor(pkg, super::visitPackage);
        return p.withExpr(refactor(p.getExpr()));
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type) {
        J.ParameterizedType p = refactor(type, this::visitExpression);
        p = refactor(p, super::visitParameterizedType);
        p = p.withClazz(refactor(p.getClazz()));
        p = p.withTypeParameters(refactor(p.getTypeParameters()));
        return p.withTypeParameters(p.getTypeParameters() == null ? null :
                p.getTypeParameters().withParams(refactor(p.getTypeParameters().getParams())));
    }

    @Override
    public <T extends Tree> J visitParentheses(J.Parentheses<T> parens) {
        J.Parentheses<T> p = refactor(parens, this::visitExpression);
        p = refactor(p, super::visitParentheses);
        return p.withTree(refactor(p.getTree()));
    }

    @Override
    public J visitPrimitive(J.Primitive primitive) {
        return super.visitPrimitive(refactor(primitive, this::visitExpression));
    }

    @Override
    public J visitReturn(J.Return retrn) {
        J.Return r = refactor(retrn, this::visitStatement);
        r = refactor(r, super::visitReturn);
        return r.withExpr(refactor(r.getExpr()));
    }

    @Override
    public J visitSwitch(J.Switch switzh) {
        J.Switch s = refactor(switzh, this::visitStatement);
        s = refactor(s, super::visitSwitch);
        s = s.withSelector(refactor(s.getSelector()));
        return s.withCases(refactor(s.getCases()));
    }

    @Override
    public J visitSynchronized(J.Synchronized synch) {
        J.Synchronized s = refactor(synch, this::visitStatement);
        s = refactor(s, super::visitSynchronized);
        s = s.withLock(refactor(s.getLock()));
        return s.withBody(refactor(s.getBody()));
    }

    @Override
    public J visitTernary(J.Ternary ternary) {
        J.Ternary t = refactor(ternary, this::visitExpression);
        t = refactor(t, super::visitTernary);
        t = t.withCondition(refactor(t.getCondition()));
        t = t.withTruePart(refactor(t.getTruePart()));
        return t.withFalsePart(refactor(t.getFalsePart()));
    }

    @Override
    public J visitThrow(J.Throw thrown) {
        J.Throw t = refactor(thrown, this::visitStatement);
        t = refactor(t, super::visitThrow);
        return t.withException(refactor(t.getException()));
    }

    @Override
    public J visitTry(J.Try tryable) {
        J.Try t = refactor(tryable, this::visitStatement);
        t = refactor(t, super::visitTry);
        t = t.withResources(t.getResources() == null ? null :
                t.getResources().withDecls(refactor(t.getResources().getDecls())));
        t = t.withBody(refactor(t.getBody()));
        t = t.withCatches(refactor(t.getCatches()));
        return t.withFinally(refactor(t.getFinally()));
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast) {
        J.TypeCast t = refactor(typeCast, this::visitExpression);
        t = refactor(t, super::visitTypeCast);
        t = t.withClazz(refactor(t.getClazz()));
        return t.withExpr(refactor(t.getExpr()));
    }

    @Override
    public J visitTypeParameter(J.TypeParameter typeParam) {
        J.TypeParameter t = refactor(typeParam, super::visitTypeParameter);
        t = t.withAnnotations(refactor(t.getAnnotations()));
        t = t.withName(refactor(t.getName()));
        return t.withBounds(t.getBounds() == null ? null : t.getBounds()
                .withTypes(refactor(t.getBounds().getTypes())));
    }

    @Override
    public J visitTypeParameters(J.TypeParameters typeParams) {
        J.TypeParameters t = refactor(typeParams, super::visitTypeParameters);
        return t.withParams(refactor(t.getParams()));
    }

    @Override
    public J visitUnary(J.Unary unary) {
        J.Unary u = refactor(unary, this::visitStatement);
        u = refactor(u, this::visitExpression);
        u = refactor(u, super::visitUnary);
        return u.withExpr(refactor(u.getExpr()));
    }

    @Override
    public J visitUnparsedSource(J.UnparsedSource unparsed) {
        J.UnparsedSource u = refactor(unparsed, this::visitStatement);
        u = refactor(u, this::visitExpression);
        return super.visitUnparsedSource(u);
    }

    @Override
    public J visitVariable(J.VariableDecls.NamedVar variable) {
        J.VariableDecls.NamedVar v = refactor(variable, super::visitVariable);
        v = v.withName(refactor(v.getName()));
        return v.withInitializer(refactor(v.getInitializer()));
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop) {
        J.WhileLoop w = refactor(whileLoop, this::visitStatement);
        w = refactor(w, super::visitWhileLoop);
        w = w.withCondition(refactor(w.getCondition()));
        return w.withBody(refactor(w.getBody()));
    }

    @Override
    public J visitWildcard(J.Wildcard wildcard) {
        J.Wildcard w = refactor(wildcard, this::visitExpression);
        w = refactor(w, super::visitWildcard);
        return w.withBoundedType(refactor(w.getBoundedType()));
    }

    protected void maybeAddImport(@Nullable JavaType.Class clazz) {
        if (clazz != null) {
            maybeAddImport(clazz.getFullyQualifiedName());
        }
    }

    protected void maybeAddImport(String fullyQualifiedName) {
        AddImport op = new AddImport(fullyQualifiedName, null, true);
        if (!andThen().contains(op)) {
            andThen(op);
        }
    }

    protected void maybeRemoveImport(@Nullable JavaType.Class clazz) {
        if (clazz != null) {
            maybeRemoveImport(clazz.getFullyQualifiedName());
        }
    }

    protected void maybeRemoveImport(String fullyQualifiedName) {
        RemoveImport op = new RemoveImport(fullyQualifiedName);
        if (!andThen().contains(op)) {
            andThen(op);
        }
    }
}
