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
        J.AnnotatedType a = call(annotatedType, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitExpression);
        a = a.withAnnotations(call(a.getAnnotations(), p));
        a = a.withTypeExpr(call(a.getTypeExpr(), p));
        return a.withTypeExpr(visitTypeName(a.getTypeExpr(), p));
    }

    public J visitAnnotation(J.Annotation annotation, P p) {
        J.Annotation a = call(annotation, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitExpression);
        if (a.getArgs() != null) {
            a = a.withArgs(visitContainer(a.getArgs(), JContainer.Location.ANNOTATION_ARGUMENT, p));
        }
        a = a.withAnnotationType(call(a.getAnnotationType(), p));
        return a.withAnnotationType(visitTypeName(a.getAnnotationType(), p));
    }

    public J visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
        J.ArrayAccess a = call(arrayAccess, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitExpression);
        a = a.withIndexed(call(a.getIndexed(), p));
        a = a.withDimension(call(a.getDimension(), p));
        return a;
    }

    public J visitArrayDimension(J.ArrayDimension arrayDimension, P p) {
        J.ArrayDimension a = call(arrayDimension, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withIndex(visitRightPadded(a.getIndex(), JRightPadded.Location.ARRAY_INDEX, p));
        return a;
    }

    public J visitArrayType(J.ArrayType arrayType, P p) {
        J.ArrayType a = call(arrayType, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitExpression);
        a.withElementType(call(a.getElementType(), p));
        return a.withElementType(visitTypeName(a.getElementType(), p));
    }

    public J visitAssert(J.Assert azzert, P p) {
        J.Assert a = call(azzert, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitStatement);
        return a.withCondition(call(a.getCondition(), p));
    }

    public J visitAssign(J.Assign assign, P p) {
        J.Assign a = call(assign, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitStatement);
        a = call(a, p, this::visitExpression);
        a = a.withVariable(call(a.getVariable(), p));
        return a.withAssignment(visitLeftPadded(a.getAssignment(), JLeftPadded.Location.ASSIGNMENT, p));
    }

    public J visitAssignOp(J.AssignOp assignOp, P p) {
        J.AssignOp a = call(assignOp, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = call(a, p, this::visitStatement);
        a = call(a, p, this::visitExpression);
        a = a.withVariable(call(a.getVariable(), p));
        return a.withAssignment(call(a.getAssignment(), p));
    }

    public J visitBinary(J.Binary binary, P p) {
        J.Binary b = call(binary, p, this::visitEach);
        b = b.withPrefix(visitSpace(b.getPrefix(), p));
        b = call(b, p, this::visitExpression);
        b = b.withLeft(call(b.getLeft(), p));
        b = b.withOperator(visitLeftPadded(b.getOperator(), JLeftPadded.Location.BINARY_OPERATOR, p));
        return b.withRight(call(b.getRight(), p));
    }

    public J visitBlock(J.Block block, P p) {
        J.Block b = call(block, p, this::visitEach);
        b = b.withStatik(b.getStatic() == null ?
                null :
                visitSpace(b.getStatic(), p));
        b = b.withPrefix(visitSpace(b.getPrefix(), p));
        b = call(b, p, this::visitStatement);
        b = b.withStatements(ListUtils.map(b.getStatements(), t ->
                visitRightPadded(t, JRightPadded.Location.BLOCK_STATEMENT, p)));
        return b.withEnd(visitSpace(b.getEnd(), p));
    }

    public J visitBreak(J.Break breakStatement, P p) {
        J.Break b = call(breakStatement, p, this::visitEach);
        b = b.withPrefix(visitSpace(b.getPrefix(), p));
        b = call(b, p, this::visitStatement);
        return b.withLabel(call(b.getLabel(), p));
    }

    public J visitCase(J.Case caze, P p) {
        J.Case c = call(caze, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = call(c, p, this::visitStatement);
        c = c.withPattern(call(c.getPattern(), p));
        return c.withStatements(visitContainer(c.getStatements(), JContainer.Location.CASE, p));
    }

    public J visitCatch(J.Try.Catch catzh, P p) {
        J.Try.Catch c = call(catzh, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withParam(call(c.getParam(), p));
        return c.withBody(call(c.getBody(), p));
    }

    public J visitClassDecl(J.ClassDecl classDecl, P p) {
        J.ClassDecl c = call(classDecl, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = call(c, p, this::visitStatement);
        c = c.withAnnotations(call(c.getAnnotations(), p));
        c = c.withModifiers(call(c.getModifiers(), p));
        c = c.withModifiers(ListUtils.map(c.getModifiers(),
                mod -> mod.withPrefix(visitSpace(mod.getPrefix(), p))));
        if (c.getTypeParameters() != null) {
            c = c.withTypeParameters(visitContainer(c.getTypeParameters(), JContainer.Location.TYPE_PARAMETER, p));
        }
        c = c.withName(call(c.getName(), p));
        if (c.getExtends() != null) {
            c = c.withExtends(visitLeftPadded(c.getExtends(), JLeftPadded.Location.EXTENDS, p));
        }
        c = c.withExtends(visitTypeName(c.getExtends(), p));
        if (c.getImplements() != null) {
            c = c.withImplements(visitContainer(c.getImplements(), JContainer.Location.IMPLEMENTS, p));
        }
        c = c.withImplements(visitTypeNames(c.getImplements(), p));
        return c.withBody(call(c.getBody(), p));
    }

    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        J.CompilationUnit c = call(cu, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        if (c.getPackageDecl() != null) {
            c = c.withPackageDecl(visitRightPadded(c.getPackageDecl(), JRightPadded.Location.PACKAGE, p));
        }
        c = c.withImports(ListUtils.map(c.getImports(), t -> visitRightPadded(t, JRightPadded.Location.IMPORT, p)));
        c = c.withClasses(call(c.getClasses(), p));
        return c.withEof(visitSpace(c.getEof(), p));
    }

    public J visitContinue(J.Continue continueStatement, P p) {
        J.Continue c = call(continueStatement, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = call(c, p, this::visitStatement);
        return c.withLabel(call(c.getLabel(), p));
    }

    public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, P p) {
        J.ControlParentheses<T> cpa = call(controlParens, p, this::visitEach);
        cpa = cpa.withPrefix(visitSpace(cpa.getPrefix(), p));
        cpa = call(cpa, p, this::visitExpression);
        return cpa.withTree(visitRightPadded(cpa.getTree(), JRightPadded.Location.PARENTHESES, p));
    }

    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        J.DoWhileLoop d = call(doWhileLoop, p, this::visitEach);
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = call(d, p, this::visitStatement);
        d = d.withWhileCondition(visitLeftPadded(d.getWhileCondition(), JLeftPadded.Location.WHILE_CONDITION, p));
        return d.withBody(visitRightPadded(d.getBody(), JRightPadded.Location.WHILE_BODY, p));
    }

    public J visitEmpty(J.Empty empty, P p) {
        J.Empty e = call(empty, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = call(e, p, this::visitStatement);
        e = call(e, p, this::visitExpression);
        return e;
    }

    public J visitEnumValue(J.EnumValue enoom, P p) {
        J.EnumValue e = call(enoom, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withName(call(e.getName(), p));
        return e.withInitializer(call(e.getInitializer(), p));
    }

    public J visitEnumValueSet(J.EnumValueSet enums, P p) {
        J.EnumValueSet e = call(enums, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = call(e, p, this::visitStatement);
        return e.withEnums(ListUtils.map(e.getEnums(), t -> visitRightPadded(t, JRightPadded.Location.ENUM_VALUE, p)));
    }

    public J visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        J.FieldAccess f = call(fieldAccess, p, this::visitEach);
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = visitTypeName(f, p);
        f = call(f, p, this::visitExpression);
        f = f.withTarget(call(f.getTarget(), p));
        return f.withName(visitLeftPadded(f.getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p));
    }

    public J visitForEachLoop(J.ForEachLoop forLoop, P p) {
        J.ForEachLoop f = call(forLoop, p, this::visitEach);
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = call(f, p, this::visitStatement);
        f = f.withControl(f.getControl().withVariable(visitRightPadded(f.getControl().getVariable(), JRightPadded.Location.FOREACH_VARIABLE, p)));
        f = f.withControl(f.getControl().withIterable(visitRightPadded(f.getControl().getIterable(), JRightPadded.Location.FOREACH_ITERABLE, p)));
        return f.withBody(visitRightPadded(f.getBody(), JRightPadded.Location.FOR_BODY, p));
    }

    public J visitForLoop(J.ForLoop forLoop, P p) {
        J.ForLoop f = call(forLoop, p, this::visitEach);
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = call(f, p, this::visitStatement);
        f = f.withControl(f.getControl().withInit(visitRightPadded(f.getControl().getInit(), JRightPadded.Location.FOR_INIT, p)));
        f = f.withControl(f.getControl().withCondition(visitRightPadded(f.getControl().getCondition(), JRightPadded.Location.FOR_CONDITION, p)));
        f = f.withControl(f.getControl().withUpdate(ListUtils.map(f.getControl().getUpdate(), t -> visitRightPadded(t, JRightPadded.Location.FOR_UPDATE, p))));
        return f.withBody(visitRightPadded(f.getBody(), JRightPadded.Location.FOR_BODY, p));
    }

    public J visitIdentifier(J.Ident ident, P p) {
        J.Ident i = call(ident, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        return call(i, p, this::visitExpression);
    }

    public J visitElse(J.If.Else elze, P p) {
        J.If.Else e = call(elze, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        return e.withBody(visitRightPadded(e.getBody(), JRightPadded.Location.IF_ELSE, p));
    }

    public J visitIf(J.If iff, P p) {
        J.If i = call(iff, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = call(i, p, this::visitStatement);
        i = i.withIfCondition(call(i.getIfCondition(), p));
        i = i.withThenPart(visitRightPadded(i.getThenPart(), JRightPadded.Location.IF_THEN, p));
        i = i.withElsePart(call(i.getElsePart(), p));
        return i;
    }

    public J visitImport(J.Import impoort, P p) {
        J.Import i = call(impoort, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withStatik(i.getStatic() == null ? null :
                visitSpace(i.getStatic(), p));
        return i.withQualid(call(i.getQualid(), p));
    }

    public J visitInstanceOf(J.InstanceOf instanceOf, P p) {
        J.InstanceOf i = call(instanceOf, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = call(i, p, this::visitExpression);
        i = i.withExpr(visitRightPadded(i.getExpr(), JRightPadded.Location.INSTANCEOF, p));
        return i.withClazz(call(i.getClazz(), p));
    }

    public J visitLabel(J.Label label, P p) {
        J.Label l = call(label, p, this::visitEach);
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        l = call(l, p, this::visitStatement);
        return l.withStatement(call(l.getStatement(), p));
    }

    public J visitLambda(J.Lambda lambda, P p) {
        J.Lambda l = call(lambda, p, this::visitEach);
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        l = call(l, p, this::visitExpression);
        l = l.withParameters(call(l.getParameters(), p));
        l = l.withArrow(visitSpace(l.getArrow(), p));
        return l.withBody(call(l.getBody(), p));
    }

    public J visitLiteral(J.Literal literal, P p) {
        J.Literal l = call(literal, p, this::visitEach);
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        return call(l, p, this::visitExpression);
    }

    public J visitMemberReference(J.MemberReference memberRef, P p) {
        J.MemberReference m = call(memberRef, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = m.withContaining(call(m.getContaining(), p));
        if (m.getTypeParameters() != null) {
            m = m.withTypeParameters(visitContainer(m.getTypeParameters(), JContainer.Location.TYPE_PARAMETER, p));
        }
        return m.withReference(visitLeftPadded(m.getReference(), JLeftPadded.Location.MEMBER_REFERENCE, p));
    }

    public J visitMethod(J.MethodDecl method, P p) {
        J.MethodDecl m = call(method, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = call(m, p, this::visitStatement);
        m = m.withAnnotations(call(m.getAnnotations(), p));
        m = m.withModifiers(call(m.getModifiers(), p));
        m = m.withModifiers(ListUtils.map(m.getModifiers(),
                mod -> mod.withPrefix(visitSpace(mod.getPrefix(), p))));
        if (m.getTypeParameters() != null) {
            m = m.withTypeParameters(visitContainer(m.getTypeParameters(), JContainer.Location.TYPE_PARAMETER, p));
        }
        m = m.withReturnTypeExpr(call(m.getReturnTypeExpr(), p));
        m = m.withReturnTypeExpr(
                m.getReturnTypeExpr() == null ?
                        null :
                        visitTypeName(m.getReturnTypeExpr(), p));
        m = m.withName(call(m.getName(), p));
        m = m.withParams(visitContainer(m.getParams(), JContainer.Location.METHOD_DECL_ARGUMENT, p));
        if (m.getThrows() != null) {
            m = m.withThrows(visitContainer(m.getThrows(), JContainer.Location.THROWS, p));
        }
        m = m.withThrows(visitTypeNames(m.getThrows(), p));
        return m.withBody(call(m.getBody(), p));
    }

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
        if (m.getSelect() != null) {
            m = m.withSelect(visitRightPadded(m.getSelect(), JRightPadded.Location.METHOD_SELECT, p));
        }
        if (m.getTypeParameters() != null) {
            m = m.withTypeParameters(visitContainer(m.getTypeParameters(), JContainer.Location.TYPE_PARAMETER, p));
        }
        m = m.withTypeParameters(visitTypeNames(m.getTypeParameters(), p));
        m = m.withName(call(m.getName(), p));
        return m.withArgs(visitContainer(m.getArgs(), JContainer.Location.METHOD_INVOCATION_ARGUMENT, p));
    }

    public J visitMultiCatch(J.MultiCatch multiCatch, P p) {
        J.MultiCatch m = call(multiCatch, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        return m.withAlternatives(ListUtils.map(m.getAlternatives(), t ->
                visitTypeName(visitRightPadded(t, JRightPadded.Location.CATCH_ALTERNATIVE, p), p)));
    }

    public J visitMultiVariable(J.VariableDecls multiVariable, P p) {
        J.VariableDecls m = call(multiVariable, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = call(m, p, this::visitStatement);
        m = m.withModifiers(Objects.requireNonNull(call(m.getModifiers(), p)));
        m = m.withModifiers(ListUtils.map(m.getModifiers(),
                mod -> mod.withPrefix(visitSpace(mod.getPrefix(), p))));
        m = m.withAnnotations(call(m.getAnnotations(), p));
        m = m.withTypeExpr(call(m.getTypeExpr(), p));
        m = m.withTypeExpr(m.getTypeExpr() == null ?
                null :
                visitTypeName(m.getTypeExpr(), p));
        m = m.withVarargs(m.getVarargs() == null ?
                null :
                visitSpace(m.getVarargs(), p));
        return m.withVars(ListUtils.map(m.getVars(), t -> visitRightPadded(t, JRightPadded.Location.NAMED_VARIABLE, p)));
    }

    public J visitNewArray(J.NewArray newArray, P p) {
        J.NewArray n = call(newArray, p, this::visitEach);
        n = n.withPrefix(visitSpace(n.getPrefix(), p));
        n = call(n, p, this::visitExpression);
        n = n.withTypeExpr(call(n.getTypeExpr(), p));
        n = n.withTypeExpr(n.getTypeExpr() == null ?
                null :
                visitTypeName(n.getTypeExpr(), p));
        n = n.withDimensions(call(n.getDimensions(), p));
        if (n.getInitializer() != null) {
            n = n.withInitializer(visitContainer(n.getInitializer(), JContainer.Location.NEW_ARRAY_INITIALIZER, p));
        }
        return n;
    }

    public J visitNewClass(J.NewClass newClass, P p) {
        J.NewClass n = call(newClass, p, this::visitEach);
        n = n.withPrefix(visitSpace(n.getPrefix(), p));
        n = call(n, p, this::visitStatement);
        n = call(n, p, this::visitExpression);
        n = n.withNew(visitSpace(n.getNew(), p));
        n = n.withClazz(call(n.getClazz(), p));
        n = n.withClazz(n.getClazz() == null ?
                null :
                visitTypeName(n.getClazz(), p));
        if (n.getArgs() != null) {
            n = n.withArgs(visitContainer(n.getArgs(), JContainer.Location.NEW_CLASS_ARGS, p));
        }
        return n.withBody(call(n.getBody(), p));
    }

    public J visitPackage(J.Package pkg, P p) {
        J.Package pa = call(pkg, p, this::visitEach);
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), p));
        return pa.withExpr(call(pa.getExpr(), p));
    }

    public J visitParameterizedType(J.ParameterizedType type, P p) {
        J.ParameterizedType pt = call(type, p, this::visitEach);
        pt = pt.withPrefix(visitSpace(pt.getPrefix(), p));
        pt = call(pt, p, this::visitExpression);
        pt = pt.withClazz(call(pt.getClazz(), p));
        pt = pt.withClazz(visitTypeName(pt.getClazz(), p));
        if (pt.getTypeParameters() != null) {
            pt = pt.withTypeParameters(visitContainer(pt.getTypeParameters(), JContainer.Location.TYPE_PARAMETER, p));
        }
        return pt.withTypeParameters(visitTypeNames(pt.getTypeParameters(), p));
    }

    public <T extends J> J visitParentheses(J.Parentheses<T> parens, P p) {
        J.Parentheses<T> pa = call(parens, p, this::visitEach);
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), p));
        pa = call(pa, p, this::visitExpression);
        return pa.withTree(visitRightPadded(pa.getTree(), JRightPadded.Location.PARENTHESES, p));
    }

    public J visitPrimitive(J.Primitive primitive, P p) {
        J.Primitive pr = call(primitive, p, this::visitEach);
        pr = pr.withPrefix(visitSpace(pr.getPrefix(), p));
        return call(pr, p, this::visitExpression);
    }

    public J visitReturn(J.Return retrn, P p) {
        J.Return r = call(retrn, p, this::visitEach);
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        r = call(r, p, this::visitStatement);
        return r.withExpr(call(r.getExpr(), p));
    }

    public J visitSwitch(J.Switch switzh, P p) {
        J.Switch s = call(switzh, p, this::visitEach);
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = call(s, p, this::visitStatement);
        s = s.withSelector(call(s.getSelector(), p));
        return s.withCases(call(s.getCases(), p));
    }

    public J visitSynchronized(J.Synchronized synch, P p) {
        J.Synchronized s = call(synch, p, this::visitEach);
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = call(s, p, this::visitStatement);
        s = s.withLock(call(s.getLock(), p));
        return s.withBody(call(s.getBody(), p));
    }

    public J visitTernary(J.Ternary ternary, P p) {
        J.Ternary t = call(ternary, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = call(t, p, this::visitExpression);
        t = t.withCondition(call(t.getCondition(), p));
        t = t.withTruePart(visitLeftPadded(t.getTruePart(), JLeftPadded.Location.TERNARY_TRUE, p));
        return t.withFalsePart(visitLeftPadded(t.getFalsePart(), JLeftPadded.Location.TERNARY_FALSE, p));
    }

    public J visitThrow(J.Throw thrown, P p) {
        J.Throw t = call(thrown, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = call(t, p, this::visitStatement);
        return t.withException(call(t.getException(), p));
    }

    public J visitTry(J.Try tryable, P p) {
        J.Try t = call(tryable, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = call(t, p, this::visitStatement);
        if (t.getResources() != null) {
            t = t.withResources(visitContainer(t.getResources(), JContainer.Location.TRY_RESOURCES, p));
        }
        t = t.withBody(call(t.getBody(), p));
        t = t.withCatches(call(t.getCatches(), p));
        if (t.getFinally() != null) {
            t = t.withFinally(visitLeftPadded(t.getFinally(), JLeftPadded.Location.TRY_FINALLY, p));
        }
        return t;
    }

    public J visitTypeCast(J.TypeCast typeCast, P p) {
        J.TypeCast t = call(typeCast, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = call(t, p, this::visitExpression);
        t = t.withClazz(call(t.getClazz(), p));
        t = t.withClazz(t.getClazz().withTree(visitTypeName(t.getClazz().getTree(), p)));
        return t.withExpr(call(t.getExpr(), p));
    }

    public J visitTypeParameter(J.TypeParameter typeParam, P p) {
        J.TypeParameter t = call(typeParam, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = t.withAnnotations(call(t.getAnnotations(), p));
        t = t.withName(call(t.getName(), p));
        if (t.getName() instanceof NameTree) {
            t = t.withName((Expression) visitTypeName((NameTree) t.getName(), p));
        }
        if (t.getBounds() != null) {
            t = t.withBounds(visitContainer(t.getBounds(), JContainer.Location.TYPE_BOUND, p));
        }
        return t.withBounds(visitTypeNames(t.getBounds(), p));
    }

    public J visitUnary(J.Unary unary, P p) {
        J.Unary u = call(unary, p, this::visitEach);
        u = u.withPrefix(visitSpace(u.getPrefix(), p));
        u = call(u, p, this::visitStatement);
        u = call(u, p, this::visitExpression);
        u = u.withOperator(visitLeftPadded(u.getOperator(), JLeftPadded.Location.UNARY_OPERATOR, p));
        return u.withExpr(call(u.getExpr(), p));
    }

    public J visitVariable(J.VariableDecls.NamedVar variable, P p) {
        J.VariableDecls.NamedVar v = call(variable, p, this::visitEach);
        v = v.withPrefix(visitSpace(v.getPrefix(), p));
        v = v.withName(call(v.getName(), p));
        if (v.getInitializer() != null) {
            v = v.withInitializer(visitLeftPadded(v.getInitializer(),
                    JLeftPadded.Location.VARIABLE_INITIALIZER, p));
        }
        return v;
    }

    public J visitWhileLoop(J.WhileLoop whileLoop, P p) {
        J.WhileLoop w = call(whileLoop, p, this::visitEach);
        w = w.withPrefix(visitSpace(w.getPrefix(), p));
        w = call(w, p, this::visitStatement);
        w = w.withCondition(call(w.getCondition(), p));
        return w.withBody(visitRightPadded(w.getBody(), JRightPadded.Location.WHILE_BODY, p));
    }

    public J visitWildcard(J.Wildcard wildcard, P p) {
        J.Wildcard w = call(wildcard, p, this::visitEach);
        w = w.withPrefix(visitSpace(w.getPrefix(), p));
        w = call(w, p, this::visitExpression);
        w = w.withBoundedType(call(w.getBoundedType(), p));
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

        J2 j = call(right.getElem(), p);
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
            t = call((J) left.getElem(), p);
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
