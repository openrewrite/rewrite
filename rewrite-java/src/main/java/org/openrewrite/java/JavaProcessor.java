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
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeProcessor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JavaProcessor extends TreeProcessor<J> implements JavaVisitor<J, ExecutionContext> {
    public J visitExpression(Expression expression, ExecutionContext ctx) {
        return expression;
    }

    public J visitStatement(Statement statement, ExecutionContext ctx) {
        return statement;
    }

    @Override
    public J visitAnnotatedType(J.AnnotatedType annotatedType, ExecutionContext ctx) {
        J.AnnotatedType a = call(annotatedType, ctx, this::visitEach);
        a = call(a, ctx, this::visitExpression);
        a = a.withAnnotations(call(a.getAnnotations(), ctx));
        return a.withTypeExpr(call(a.getTypeExpr(), ctx));
    }

    @Override
    public J visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
        J.Annotation a = call(annotation, ctx, this::visitEach);
        a = call(a, ctx, this::visitExpression);
        a = a.withArgs(eval(a.getArgs(), ctx));
        return a.withAnnotationType(call(a.getAnnotationType(), ctx));
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, ExecutionContext ctx) {
        J.ArrayAccess a = call(arrayAccess, ctx, this::visitEach);
        a = call(a, ctx, this::visitExpression);
        a = a.withIndexed(call(a.getIndexed(), ctx));
        a = a.withDimension(call(a.getDimension(), ctx));
        return a;
    }

    @Override
    public J visitArrayDimension(J.ArrayDimension arrayDimension, ExecutionContext ctx) {
        J.ArrayDimension a = call(arrayDimension, ctx, this::visitEach);
        a = a.withIndex(eval(a.getIndex(), ctx));
        return a;
    }

    @Override
    public J visitArrayType(J.ArrayType arrayType, ExecutionContext ctx) {
        J.ArrayType a = call(arrayType, ctx, this::visitEach);
        a = call(a, ctx, this::visitExpression);
        return a.withElementType(call(a.getElementType(), ctx));
    }

    @Override
    public J visitAssert(J.Assert azzert, ExecutionContext ctx) {
        J.Assert a = call(azzert, ctx, this::visitEach);
        a = call(a, ctx, this::visitStatement);
        return a.withCondition(call(a.getCondition(), ctx));
    }

    @Override
    public J visitAssign(J.Assign assign, ExecutionContext ctx) {
        J.Assign a = call(assign, ctx, this::visitEach);
        a = call(a, ctx, this::visitStatement);
        a = call(a, ctx, this::visitExpression);
        a = a.withVariable(call(a.getVariable(), ctx));
        return a.withAssignment(eval(a.getAssignment(), ctx));
    }

    @Override
    public J visitAssignOp(J.AssignOp assignOp, ExecutionContext ctx) {
        J.AssignOp a = call(assignOp, ctx, this::visitEach);
        a = call(a, ctx, this::visitStatement);
        a = call(a, ctx, this::visitExpression);
        a = a.withVariable(call(a.getVariable(), ctx));
        return a.withAssignment(call(a.getAssignment(), ctx));
    }

    @Override
    public J visitBinary(J.Binary binary, ExecutionContext ctx) {
        J.Binary b = call(binary, ctx, this::visitEach);
        b = call(b, ctx, this::visitExpression);
        b = b.withLeft(call(b.getLeft(), ctx));
        return b.withRight(call(b.getRight(), ctx));
    }

    @Override
    public J visitBlock(J.Block block, ExecutionContext ctx) {
        J.Block b = call(block, ctx, this::visitEach);
        b = call(b, ctx, this::visitStatement);
        return b.withStatements(evalMany(b.getStatements(), ctx));
    }

    @Override
    public J visitBreak(J.Break breakStatement, ExecutionContext ctx) {
        J.Break b = call(breakStatement, ctx, this::visitEach);
        b = call(b, ctx, this::visitStatement);
        return b.withLabel(call(b.getLabel(), ctx));
    }

    @Override
    public J visitCase(J.Case caze, ExecutionContext ctx) {
        J.Case c = call(caze, ctx, this::visitEach);
        c = call(c, ctx, this::visitStatement);
        c = c.withPattern(call(c.getPattern(), ctx));
        return c.withStatements(c.getStatements().withElem(evalMany(c.getStatements().getElem(), ctx)));
    }

    @Override
    public J visitCatch(J.Try.Catch catzh, ExecutionContext ctx) {
        J.Try.Catch c = call(catzh, ctx, this::visitEach);
        c = c.withParam(call(c.getParam(), ctx));
        return c.withBody(call(c.getBody(), ctx));
    }

    @Override
    public J visitClassDecl(J.ClassDecl classDecl, ExecutionContext ctx) {
        J.ClassDecl c = call(classDecl, ctx, this::visitEach);
        c = call(c, ctx, this::visitStatement);
        c = c.withAnnotations(call(c.getAnnotations(), ctx));
        c = c.withModifiers(call(c.getModifiers(), ctx));
        c = c.withTypeParameters(eval(c.getTypeParameters(), ctx));
        c = c.withName(call(c.getName(), ctx));
        c = c.withExtends(eval(c.getExtends(), ctx));
        c = c.withImplements(eval(c.getImplements(), ctx));
        return c.withBody(call(c.getBody(), ctx));
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
        J.CompilationUnit c = call(cu, ctx, this::visitEach);
        c = c.withPackageDecl(eval(c.getPackageDecl(), ctx));
        c = c.withImports(evalMany(c.getImports(), ctx));
        return c.withClasses(call(c.getClasses(), ctx));
    }

    @Override
    public J visitContinue(J.Continue continueStatement, ExecutionContext ctx) {
        J.Continue c = call(continueStatement, ctx, this::visitEach);
        c = call(c, ctx, this::visitStatement);
        return c.withLabel(call(c.getLabel(), ctx));
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, ExecutionContext ctx) {
        J.DoWhileLoop d = call(doWhileLoop, ctx, this::visitEach);
        d = call(d, ctx, this::visitStatement);
        d = d.withWhileCondition(eval(d.getWhileCondition(), ctx));
        return d.withBody(eval(d.getBody(), ctx));
    }

    @Override
    public J visitEmpty(J.Empty empty, ExecutionContext ctx) {
        J.Empty e = call(empty, ctx, this::visitEach);
        e = call(e, ctx, this::visitStatement);
        e = call(e, ctx, this::visitExpression);
        return e;
    }

    @Override
    public J visitEnumValue(J.EnumValue enoom, ExecutionContext ctx) {
        J.EnumValue e = call(enoom, ctx, this::visitEach);
        e = e.withName(call(e.getName(), ctx));
        return e.withInitializer(call(e.getInitializer(), ctx));
    }

    @Override
    public J visitEnumValueSet(J.EnumValueSet enums, ExecutionContext ctx) {
        J.EnumValueSet e = call(enums, ctx, this::visitEach);
        e = call(e, ctx, this::visitStatement);
        return e.withEnums(evalMany(e.getEnums(), ctx));
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
        J.FieldAccess f = call(fieldAccess, ctx, this::visitEach);
        f = call(f, ctx, this::visitExpression);
        f = f.withTarget(call(f.getTarget(), ctx));
        return f.withName(eval(f.getName(), ctx));
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forLoop, ExecutionContext ctx) {
        J.ForEachLoop f = call(forLoop, ctx, this::visitEach);
        f = call(f, ctx, this::visitStatement);
        f = f.withControl(f.getControl().withVariable(eval(f.getControl().getVariable(), ctx)));
        f = f.withControl(f.getControl().withIterable(eval(f.getControl().getIterable(), ctx)));
        return f.withBody(eval(f.getBody(), ctx));
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop, ExecutionContext ctx) {
        J.ForLoop f = call(forLoop, ctx, this::visitEach);
        f = call(f, ctx, this::visitStatement);
        f = f.withControl(f.getControl().withInit(eval(f.getControl().getInit(), ctx)));
        f = f.withControl(f.getControl().withCondition(eval(f.getControl().getCondition(), ctx)));
        f = f.withControl(f.getControl().withUpdate(evalMany(f.getControl().getUpdate(), ctx)));
        return f.withBody(eval(f.getBody(), ctx));
    }

    @Override
    public J visitIdentifier(J.Ident ident, ExecutionContext ctx) {
        J.Ident i = call(ident, ctx, this::visitEach);
        return call(i, ctx, this::visitExpression);
    }

    @Override
    public J visitElse(J.If.Else elze, ExecutionContext ctx) {
        J.If.Else e = call(elze, ctx, this::visitEach);
        return e.withBody(eval(e.getBody(), ctx));
    }

    @Override
    public J visitIf(J.If iff, ExecutionContext ctx) {
        J.If i = call(iff, ctx, this::visitEach);
        i = call(i, ctx, this::visitStatement);
        i = i.withIfCondition(call(i.getIfCondition(), ctx));
        i = i.withThenPart(eval(i.getThenPart(), ctx));
        i = i.withElsePart(call(i.getElsePart(), ctx));
        return i;
    }

    @Override
    public J visitImport(J.Import impoort, ExecutionContext ctx) {
        J.Import i = call(impoort, ctx, this::visitEach);
        return i.withQualid(call(i.getQualid(), ctx));
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, ExecutionContext ctx) {
        J.InstanceOf i = call(instanceOf, ctx, this::visitEach);
        i = call(i, ctx, this::visitExpression);
        i = i.withExpr(eval(i.getExpr(), ctx));
        return i.withClazz(call(i.getClazz(), ctx));
    }

    @Override
    public J visitLabel(J.Label label, ExecutionContext ctx) {
        J.Label l = call(label, ctx, this::visitEach);
        l = call(l, ctx, this::visitStatement);
        return l.withStatement(call(l.getStatement(), ctx));
    }

    @Override
    public J visitLambda(J.Lambda lambda, ExecutionContext ctx) {
        J.Lambda l = call(lambda, ctx, this::visitEach);
        l = call(l, ctx, this::visitExpression);
        l = l.withParameters(call(l.getParameters(), ctx));
        return l.withBody(call(l.getBody(), ctx));
    }

    @Override
    public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
        J.Literal l = call(literal, ctx, this::visitEach);
        return call(l, ctx, this::visitExpression);
    }

    @Override
    public J visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
        J.MemberReference m = call(memberRef, ctx, this::visitEach);
        m = m.withContaining(call(m.getContaining(), ctx));
        m = m.withTypeParameters(eval(m.getTypeParameters(), ctx));
        return m.withReference(eval(m.getReference(), ctx));
    }

    @Override
    public J visitMethod(J.MethodDecl method, ExecutionContext ctx) {
        J.MethodDecl m = call(method, ctx, this::visitEach);
        m = call(m, ctx, this::visitStatement);
        m = m.withAnnotations(call(m.getAnnotations(), ctx));
        m = m.withModifiers(call(m.getModifiers(), ctx));
        m = m.withTypeParameters(eval(m.getTypeParameters(), ctx));
        m = m.withReturnTypeExpr(call(m.getReturnTypeExpr(), ctx));
        m = m.withName(call(m.getName(), ctx));
        m = m.withParams(eval(m.getParams(), ctx));
        m = m.withParams(eval(m.getParams(), ctx));
        m = m.withThrows(eval(m.getThrows(), ctx));
        return m.withBody(call(m.getBody(), ctx));
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        J.MethodInvocation m = call(method, ctx, this::visitEach);
        m = call(m, ctx, this::visitStatement);
        m = call(m, ctx, this::visitExpression);
        m = m.withSelect(eval(m.getSelect(), ctx));
        m = m.withTypeParameters(eval(m.getTypeParameters(), ctx));
        m = m.withName(call(m.getName(), ctx));
        return m.withArgs(eval(m.getArgs(), ctx));
    }

    @Override
    public J visitMultiCatch(J.MultiCatch multiCatch, ExecutionContext ctx) {
        J.MultiCatch m = call(multiCatch, ctx, this::visitEach);
        return m.withAlternatives(evalMany(m.getAlternatives(), ctx));
    }

    @Override
    public J visitMultiVariable(J.VariableDecls multiVariable, ExecutionContext ctx) {
        J.VariableDecls m = call(multiVariable, ctx, this::visitEach);
        m = call(m, ctx, this::visitStatement);
        m = m.withModifiers(call(m.getModifiers(), ctx));
        m = m.withAnnotations(call(m.getAnnotations(), ctx));
        m = m.withTypeExpr(call(m.getTypeExpr(), ctx));
        return m.withVars(evalMany(m.getVars(), ctx));
    }

    @Override
    public J visitNewArray(J.NewArray newArray, ExecutionContext ctx) {
        J.NewArray n = call(newArray, ctx, this::visitEach);
        n = call(n, ctx, this::visitExpression);
        n = n.withTypeExpr(call(n.getTypeExpr(), ctx));
        n = n.withDimensions(call(n.getDimensions(), ctx));
        return n.withInitializer(eval(n.getInitializer(), ctx));
    }

    @Override
    public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
        J.NewClass n = call(newClass, ctx, this::visitEach);
        n = call(n, ctx, this::visitStatement);
        n = call(n, ctx, this::visitExpression);
        n = n.withClazz(call(n.getClazz(), ctx));
        n = n.withArgs(eval(n.getArgs(), ctx));
        return n.withBody(call(n.getBody(), ctx));
    }

    @Override
    public J visitPackage(J.Package pkg, ExecutionContext ctx) {
        J.Package p = call(pkg, ctx, this::visitEach);
        return p.withExpr(call(p.getExpr(), ctx));
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type, ExecutionContext ctx) {
        J.ParameterizedType p = call(type, ctx, this::visitEach);
        p = call(p, ctx, this::visitExpression);
        p = p.withClazz(call(p.getClazz(), ctx));
        return p.withTypeParameters(eval(p.getTypeParameters(), ctx));
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens, ExecutionContext ctx) {
        J.Parentheses<T> p = call(parens, ctx, this::visitEach);
        p = call(p, ctx, this::visitExpression);
        return p.withTree(eval(p.getTree(), ctx));
    }

    @Override
    public J visitPrimitive(J.Primitive primitive, ExecutionContext ctx) {
        J.Primitive p = call(primitive, ctx, this::visitEach);
        return call(p, ctx, this::visitExpression);
    }

    @Override
    public J visitReturn(J.Return retrn, ExecutionContext ctx) {
        J.Return r = call(retrn, ctx, this::visitEach);
        r = call(r, ctx, this::visitStatement);
        return r.withExpr(call(r.getExpr(), ctx));
    }

    @Override
    public J visitSwitch(J.Switch switzh, ExecutionContext ctx) {
        J.Switch s = call(switzh, ctx, this::visitEach);
        s = call(s, ctx, this::visitStatement);
        s = s.withSelector(call(s.getSelector(), ctx));
        return s.withCases(call(s.getCases(), ctx));
    }

    @Override
    public J visitSynchronized(J.Synchronized synch, ExecutionContext ctx) {
        J.Synchronized s = call(synch, ctx, this::visitEach);
        s = call(s, ctx, this::visitStatement);
        s = s.withLock(call(s.getLock(), ctx));
        return s.withBody(call(s.getBody(), ctx));
    }

    @Override
    public J visitTernary(J.Ternary ternary, ExecutionContext ctx) {
        J.Ternary t = call(ternary, ctx, this::visitEach);
        t = call(t, ctx, this::visitExpression);
        t = t.withCondition(call(t.getCondition(), ctx));
        t = t.withTruePart(eval(t.getTruePart(), ctx));
        return t.withFalsePart(eval(t.getFalsePart(), ctx));
    }

    @Override
    public J visitThrow(J.Throw thrown, ExecutionContext ctx) {
        J.Throw t = call(thrown, ctx, this::visitEach);
        t = call(t, ctx, this::visitStatement);
        return t.withException(call(t.getException(), ctx));
    }

    @Override
    public J visitTry(J.Try tryable, ExecutionContext ctx) {
        J.Try t = call(tryable, ctx, this::visitEach);
        t = call(t, ctx, this::visitStatement);
        t = t.withResources(eval(t.getResources(), ctx));
        t = t.withBody(call(t.getBody(), ctx));
        t = t.withCatches(call(t.getCatches(), ctx));
        return t.withFinally(eval(t.getFinally(), ctx));
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast, ExecutionContext ctx) {
        J.TypeCast t = call(typeCast, ctx, this::visitEach);
        t = call(t, ctx, this::visitExpression);
        t = t.withClazz(call(t.getClazz(), ctx));
        return t.withExpr(call(t.getExpr(), ctx));
    }

    @Override
    public J visitTypeParameter(J.TypeParameter typeParam, ExecutionContext ctx) {
        J.TypeParameter t = call(typeParam, ctx, this::visitEach);
        t = t.withAnnotations(call(t.getAnnotations(), ctx));
        t = t.withName(call(t.getName(), ctx));
        return t.withBounds(eval(t.getBounds(), ctx));
    }

    @Override
    public J visitUnary(J.Unary unary, ExecutionContext ctx) {
        J.Unary u = call(unary, ctx, this::visitEach);
        u = call(u, ctx, this::visitStatement);
        u = call(u, ctx, this::visitExpression);
        return u.withExpr(call(u.getExpr(), ctx));
    }

    @Override
    public J visitVariable(J.VariableDecls.NamedVar variable, ExecutionContext ctx) {
        J.VariableDecls.NamedVar v = call(variable, ctx, this::visitEach);
        v = v.withName(call(v.getName(), ctx));
        return v.withInitializer(eval(v.getInitializer(), ctx));
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop, ExecutionContext ctx) {
        J.WhileLoop w = call(whileLoop, ctx, this::visitEach);
        w = call(w, ctx, this::visitStatement);
        w = w.withCondition(call(w.getCondition(), ctx));
        return w.withBody(eval(w.getBody(), ctx));
    }

    @Override
    public J visitWildcard(J.Wildcard wildcard, ExecutionContext ctx) {
        J.Wildcard w = call(wildcard, ctx, this::visitEach);
        w = call(w, ctx, this::visitExpression);
        return w.withBoundedType(call(w.getBoundedType(), ctx));
    }

    @Nullable
    protected <J2 extends J> JRightPadded<J2> eval(@Nullable JRightPadded<J2> right, ExecutionContext ctx) {
        if (right == null) {
            return null;
        }
        J2 j = call(right.getElem(), ctx);
        return j == right.getElem() ? right : new JRightPadded<>(j, right.getAfter());
    }

    @Nullable
    protected <J2 extends J> JLeftPadded<J2> eval(@Nullable JLeftPadded<J2> left, ExecutionContext ctx) {
        if (left == null) {
            return null;
        }
        J2 j = call(left.getElem(), ctx);
        return j == left.getElem() ? left : new JLeftPadded<>(left.getBefore(), j);
    }

    @Nullable
    protected <J2 extends J> List<JRightPadded<J2>> evalMany(@Nullable List<JRightPadded<J2>> trees, ExecutionContext ctx) {
        if (trees == null) {
            return null;
        }

        List<JRightPadded<J2>> mutatedTrees = new ArrayList<>(trees.size());
        boolean changed = false;
        for (JRightPadded<J2> tree : trees) {
            JRightPadded<J2> mutated = eval(tree, ctx);
            if (mutated != tree) {
                changed = true;
            }
            mutatedTrees.add(mutated);
        }

        return changed ? mutatedTrees : trees;
    }

    @Nullable
    protected <J2 extends J> JContainer<J2> eval(@Nullable JContainer<J2> container, ExecutionContext ctx) {
        if (container == null) {
            return null;
        }
        List<JRightPadded<J2>> js = evalMany(container.getElem(), ctx);
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
