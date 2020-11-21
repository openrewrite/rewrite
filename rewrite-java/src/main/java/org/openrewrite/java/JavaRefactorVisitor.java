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

import org.openrewrite.AbstractRefactorVisitor;
import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

/**
 * This is the class you want to extend from if your visitor might ever want to swap the AST element being visited
 * for a different type of AST element. For example visiting a method declaration and returning a field.
 * Most visitors do not need this flexibility and can extend JavaIsomorphicRefactorVisitor whose type signatures
 * insist that when visiting an AST element the returned element must be of the same type.
 */
public class JavaRefactorVisitor extends AbstractRefactorVisitor<J> implements JavaSourceVisitor<J> {
    protected JavaFormatter formatter;
    protected TreeBuilder treeBuilder;

    @Override
    public J visitStatement(Statement statement) {
        return statement;
    }

    @Override
    public J visitTypeName(NameTree name) {
        return name;
    }

    @Override
    public J visitAnnotatedType(J.AnnotatedType annotatedType) {
        J.AnnotatedType a = refactor(annotatedType, this::visitExpression);
        a = a.withAnnotations(refactor(a.getAnnotations()));
        return a.withTypeExpr(refactor(a.getTypeExpr()));
    }

    @Override
    public J visitAnnotation(J.Annotation annotation) {
        J.Annotation a = refactor(annotation, this::visitExpression);
        a = a.withArgs(refactor(a.getArgs() == null ? null :
                a.getArgs().withArgs(refactor(a.getArgs().getArgs()))));
        return a.withAnnotationType(refactor(a.getAnnotationType()));
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess) {
        J.ArrayAccess a = refactor(arrayAccess, this::visitExpression);
        a = a.withIndexed(refactor(a.getIndexed()));
        return a.withDimension(a.getDimension().withIndex(refactor(a.getDimension().getIndex())));
    }

    @Override
    public J visitArrayType(J.ArrayType arrayType) {
        J.ArrayType a = arrayType;
        a = a.withDimensions(refactor(a.getDimensions()));
        return a.withElementType(refactor(a.getElementType()));
    }

    @Override
    public J visitAssert(J.Assert azzert) {
        J.Assert a = refactor(azzert, this::visitStatement);
        return a.withCondition(refactor(a.getCondition()));
    }

    @Override
    public J visitAssign(J.Assign assign) {
        J.Assign a = refactor(assign, this::visitStatement);
        a = refactor(a, this::visitExpression);
        a = a.withVariable(refactor(a.getVariable()));
        return a.withAssignment(refactor(a.getAssignment()));
    }

    @Override
    public J visitAssignOp(J.AssignOp assignOp) {
        J.AssignOp a = refactor(assignOp, this::visitStatement);
        a = refactor(a, this::visitExpression);
        a = a.withVariable(refactor(a.getVariable()));
        return a.withAssignment(refactor(a.getAssignment()));
    }

    @Override
    public J visitBinary(J.Binary binary) {
        J.Binary b = refactor(binary, this::visitExpression);
        b = b.withLeft(refactor(b.getLeft()));
        return b.withRight(refactor(b.getRight()));
    }

    @Override
    public J visitBlock(J.Block<J> block) {
        J.Block<J> b = refactor(block, this::visitStatement);
        b = b.withStatements(refactor(b.getStatements()));
        return b.withEnd(refactor(b.getEnd()));
    }

    @Override
    public J visitBreak(J.Break breakStatement) {
        J.Break b = refactor(breakStatement, this::visitStatement);
        return b.withLabel(refactor(b.getLabel()));
    }

    @Override
    public J visitCase(J.Case caze) {
        J.Case c = refactor(caze, this::visitStatement);
        c = c.withPattern(refactor(c.getPattern()));
        return c.withStatements(refactor(c.getStatements()));
    }

    @Override
    public J visitCatch(J.Try.Catch catzh) {
        J.Try.Catch c = catzh;
        c = c.withParam(refactor(c.getParam()));
        return c.withBody(refactor(c.getBody()));
    }

    @Override
    public J visitClassDecl(J.ClassDecl classDecl) {
        J.ClassDecl c = refactor(classDecl, this::visitStatement);
        c = c.withAnnotations(refactor(c.getAnnotations()));
        c = c.withModifiers(refactor(c.getModifiers()));
        c = c.withTypeParameters(refactor(c.getTypeParameters()));
        c = c.withKind(refactor(c.getKind()));
        c = c.withName(refactor(c.getName()));
        c = c.withExtends(refactor(c.getExtends()));
        if (c.getExtends() != null) {
            c = c.withExtends(c.getExtends().withFrom(refactor(c.getExtends().getFrom())));
        }
        c = c.withImplements(refactor(c.getImplements()));
        if (c.getImplements() != null) {
            c = c.withImplements(c.getImplements().withFrom(refactor(c.getImplements().getFrom())));
        }
        return c.withBody(refactor(c.getBody()));
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu) {
        formatter = new JavaFormatter(cu);
        treeBuilder = new TreeBuilder(cu);
        J.CompilationUnit c = cu;
        c = c.withPackageDecl(refactor(c.getPackageDecl()));
        c = c.withImports(refactor(c.getImports()));
        return c.withClasses(refactor(c.getClasses()));
    }

    @Override
    public J visitContinue(J.Continue continueStatement) {
        J.Continue c = refactor(continueStatement, this::visitStatement);
        return c.withLabel(refactor(c.getLabel()));
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop) {
        J.DoWhileLoop d = refactor(doWhileLoop, this::visitStatement);
        d = d.withWhileCondition(refactor(d.getWhileCondition()));
        return d.withBody(refactor(d.getBody()));
    }

    @Override
    public J visitEmpty(J.Empty empty) {
        J.Empty e = refactor(empty, this::visitStatement);
        e = refactor(e, this::visitExpression);
        return e;
    }

    @Override
    public J visitEnumValue(J.EnumValue enoom) {
        J.EnumValue e = refactor(enoom, this::visitStatement);
        e = e.withName(refactor(e.getName()));
        return e.withInitializer(refactor(e.getInitializer()));
    }

    @Override
    public J visitEnumValueSet(J.EnumValueSet enums) {
        J.EnumValueSet e = refactor(enums, this::visitStatement);
        return e.withEnums(refactor(e.getEnums()));
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess) {
        J.FieldAccess f = refactor(fieldAccess, this::visitExpression);
        f = f.withTarget(refactor(f.getTarget()));
        return f.withName(refactor(f.getName()));
    }

    @Override
    public J visitFinally(J.Try.Finally finallie) {
        return finallie.withBody(refactor(finallie.getBody()));
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forLoop) {
        J.ForEachLoop f = refactor(forLoop, this::visitStatement);
        f = f.withControl(f.getControl().withVariable(refactor(f.getControl().getVariable())));
        f = f.withControl(f.getControl().withIterable(refactor(f.getControl().getIterable())));
        return f.withBody(refactor(f.getBody()));
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop) {
        J.ForLoop f = refactor(forLoop, this::visitStatement);
        f = f.withControl(f.getControl().withInit(refactor(f.getControl().getInit())));
        f = f.withControl(f.getControl().withCondition(refactor(f.getControl().getCondition())));
        f = f.withControl(f.getControl().withUpdate(refactor(f.getControl().getUpdate())));
        return f.withBody(refactor(f.getBody()));
    }

    @Override
    public J visitIdentifier(J.Ident ident) {
        return refactor(ident, this::visitExpression);
    }

    @Override
    public J visitIf(J.If iff) {
        J.If i = refactor(iff, this::visitStatement);
        i = i.withIfCondition(refactor(i.getIfCondition()));
        i = i.withThenPart(refactor(i.getThenPart()));
        return i.withElsePart(refactor(i.getElsePart()));
    }

    @Override
    public J visitElse(J.If.Else elze) {
        return elze.withStatement(refactor(elze.getStatement()));
    }

    @Override
    public J visitImport(J.Import impoort) {
        return impoort.withQualid(refactor(impoort.getQualid()));
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf) {
        J.InstanceOf i = refactor(instanceOf, this::visitExpression);
        i = i.withExpr(refactor(i.getExpr()));
        return i.withClazz(refactor(i.getClazz()));
    }

    @Override
    public J visitLabel(J.Label label) {
        J.Label l = refactor(label, this::visitStatement);
        return l.withStatement(refactor(l.getStatement()));
    }

    @Override
    public J visitLambda(J.Lambda lambda) {
        J.Lambda l = refactor(lambda, this::visitExpression);
        l = l.withParamSet(refactor(l.getParamSet()));
        l = l.withArrow(refactor(l.getArrow()));
        return l.withBody(refactor(l.getBody()));
    }

    @Override
    public J visitLiteral(J.Literal literal) {
        return refactor(literal, this::visitExpression);
    }

    @Override
    public J visitMemberReference(J.MemberReference memberRef) {
        J.MemberReference m = memberRef;
        m = m.withContaining(refactor(m.getContaining()));
        m = m.withTypeParameters(refactor(m.getTypeParameters()));
        return m.withReference(refactor(m.getReference()));
    }

    @Override
    public J visitMethod(J.MethodDecl method) {
        J.MethodDecl m = method;
        m = m.withAnnotations(refactor(m.getAnnotations()));
        m = m.withModifiers(refactor(m.getModifiers()));
        m = m.withTypeParameters(refactor(m.getTypeParameters()));
        m = m.withReturnTypeExpr(refactor(m.getReturnTypeExpr()));
        m = m.withName(refactor(m.getName()));
        m = m.withParams(refactor(m.getParams()));
        m = m.withParams(m.getParams().withParams(refactor(m.getParams().getParams())));
        m = m.withThrows(refactor(m.getThrows()));
        return m.withBody(refactor(m.getBody()));
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method) {
        J.MethodInvocation m = refactor(method, this::visitStatement);
        m = refactor(m, this::visitExpression);
        m = m.withSelect(refactor(m.getSelect()));
        m = m.withTypeParameters(refactor(m.getTypeParameters()));
        m = m.withName(refactor(m.getName()));
        m = m.withArgs(refactor(m.getArgs()));
        return m.withArgs(m.getArgs().withArgs(refactor(m.getArgs().getArgs())));
    }

    @Override
    public J visitMultiCatch(J.MultiCatch multiCatch) {
        return multiCatch.withAlternatives(refactor(multiCatch.getAlternatives()));
    }

    @Override
    public J visitMultiVariable(J.VariableDecls multiVariable) {
        J.VariableDecls m = refactor(multiVariable, this::visitStatement);
        m = m.withModifiers(refactor(m.getModifiers()));
        m = m.withAnnotations(refactor(m.getAnnotations()));
        m = m.withTypeExpr(refactor(m.getTypeExpr()));
        m = m.withVars(refactor(m.getVars()));
        return m.withVarargs(refactor(m.getVarargs()));
    }

    @Override
    public J visitNewArray(J.NewArray newArray) {
        J.NewArray n = refactor(newArray, this::visitExpression);
        n = n.withTypeExpr(refactor(n.getTypeExpr()));
        return n.withInitializer(refactor(n.getInitializer()));
    }

    @Override
    public J visitNewClass(J.NewClass newClass) {
        J.NewClass n = refactor(newClass, this::visitStatement);
        n = refactor(n, this::visitExpression);
        n = n.withClazz(refactor(n.getClazz()));
        n = n.withArgs(refactor(n.getArgs()));
        return n.withBody(refactor(n.getBody()));
    }

    @Override
    public J visitPackage(J.Package pkg) {
        return pkg.withExpr(refactor(pkg.getExpr()));
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type) {
        J.ParameterizedType p = refactor(type, this::visitExpression);
        p = p.withClazz(refactor(p.getClazz()));
        return p.withTypeParameters(refactor(p.getTypeParameters()));
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens) {
        J.Parentheses<T> p = refactor(parens, this::visitExpression);
        return p.withTree(refactor(p.getTree()));
    }

    @Override
    public J visitPrimitive(J.Primitive primitive) {
        return refactor(primitive, this::visitExpression);
    }

    @Override
    public J visitReturn(J.Return retrn) {
        J.Return r = refactor(retrn, this::visitStatement);
        return r.withExpr(refactor(r.getExpr()));
    }

    @Override
    public J visitSwitch(J.Switch switzh) {
        J.Switch s = refactor(switzh, this::visitStatement);
        s = s.withSelector(refactor(s.getSelector()));
        return s.withCases(refactor(s.getCases()));
    }

    @Override
    public J visitSynchronized(J.Synchronized synch) {
        J.Synchronized s = refactor(synch, this::visitStatement);
        s = s.withLock(refactor(s.getLock()));
        return s.withBody(refactor(s.getBody()));
    }

    @Override
    public J visitTernary(J.Ternary ternary) {
        J.Ternary t = refactor(ternary, this::visitExpression);
        t = t.withCondition(refactor(t.getCondition()));
        t = t.withTruePart(refactor(t.getTruePart()));
        return t.withFalsePart(refactor(t.getFalsePart()));
    }

    @Override
    public J visitThrow(J.Throw thrown) {
        J.Throw t = refactor(thrown, this::visitStatement);
        return t.withException(refactor(t.getException()));
    }

    @Override
    public J visitTry(J.Try tryable) {
        J.Try t = refactor(tryable, this::visitStatement);
        t = t.withResources(t.getResources() == null ? null :
                t.getResources().withDecls(refactor(t.getResources().getDecls())));
        t = t.withBody(refactor(t.getBody()));
        t = t.withCatches(refactor(t.getCatches()));
        return t.withFinally(refactor(t.getFinally()));
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast) {
        J.TypeCast t = refactor(typeCast, this::visitExpression);
        t = t.withClazz(refactor(t.getClazz()));
        return t.withExpr(refactor(t.getExpr()));
    }

    @Override
    public J visitTypeParameter(J.TypeParameter typeParam) {
        J.TypeParameter t = typeParam;
        t = t.withAnnotations(refactor(t.getAnnotations()));
        t = t.withName(refactor(t.getName()));
        return t.withBounds(t.getBounds() == null ? null : t.getBounds()
                .withTypes(refactor(t.getBounds().getTypes())));
    }

    @Override
    public J visitTypeParameters(J.TypeParameters typeParams) {
        return typeParams.withParams(refactor(typeParams.getParams()));
    }

    @Override
    public J visitUnary(J.Unary unary) {
        J.Unary u = refactor(unary, this::visitStatement);
        u = refactor(u, this::visitExpression);
        return u.withExpr(refactor(u.getExpr()));
    }

    @Override
    public J visitUnparsedSource(J.UnparsedSource unparsed) {
        J.UnparsedSource u = refactor(unparsed, this::visitStatement);
        u = refactor(u, this::visitExpression);
        return u;
    }

    @Override
    public J visitVariable(J.VariableDecls.NamedVar variable) {
        J.VariableDecls.NamedVar v = variable;
        v = v.withName(refactor(v.getName()));
        return v.withInitializer(refactor(v.getInitializer()));
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop) {
        J.WhileLoop w = refactor(whileLoop, this::visitStatement);
        w = w.withCondition(refactor(w.getCondition()));
        return w.withBody(refactor(w.getBody()));
    }

    @Override
    public J visitWildcard(J.Wildcard wildcard) {
        J.Wildcard w = refactor(wildcard, this::visitExpression);
        return w.withBoundedType(refactor(w.getBoundedType()));
    }

    /**
     * This method will add an import to the compilation unit.
     *
     * @param fullyQualifiedName Fully-qualified name of the class.
     */
    public void addImport(String fullyQualifiedName) {
        AddImport op = new AddImport();
        op.setType(fullyQualifiedName);
        op.setOnlyIfReferenced(false);
        if (!andThen().contains(op)) {
            andThen(op);
        }
    }

    /**
     * This method will add an import to the compilation unit if there is a reference to the type in the compilation
     * unit.
     *
     * @param clazz The class that will be imported into the compliation unit.
     */
    public void maybeAddImport(@Nullable JavaType.FullyQualified clazz) {
        if (clazz != null) {
            maybeAddImport(clazz.getFullyQualifiedName());
        }
    }

    /**
     * This method will add an import to the compilation unit if there is a reference to the type in the compilation
     * unit.
     *
     * @param fullyQualifiedName Fully-qualified name of the class.
     */
    public void maybeAddImport(String fullyQualifiedName) {
        AddImport op = new AddImport();
        op.setType(fullyQualifiedName);
        if (!andThen().contains(op)) {
            andThen(op);
        }
    }

    /**
     * This method will add a static method import to the compilation unit if there is a reference to the method
     * in the compilation unit.
     *
     * @param fullyQualifiedName Fully-qualified name of the class.
     * @param staticMethod The static method to be imported. A wildcard "*" may also be used to statically import all methods.
     */
    public void maybeAddImport(@NonNull String fullyQualifiedName, @NonNull String staticMethod) {
        AddImport op = new AddImport();
        op.setType(fullyQualifiedName);
        op.setStaticMethod(staticMethod);
        if (!andThen().contains(op)) {
            andThen(op);
        }
    }

    public void maybeRemoveImport(@Nullable JavaType.FullyQualified clazz) {
        if (clazz != null) {
            maybeRemoveImport(clazz.getFullyQualifiedName());
        }
    }

    public void maybeRemoveImport(String fullyQualifiedName) {
        RemoveImport op = new RemoveImport();
        op.setType(fullyQualifiedName);
        if (!andThen().contains(op)) {
            andThen(op);
        }
    }

    public void maybeUnwrapParentheses(Cursor parensCursor) {
        if (UnwrapParentheses.Scoped.isUnwrappable(parensCursor)) {
            andThen(new UnwrapParentheses.Scoped(parensCursor.getTree()));
        }
    }
}
