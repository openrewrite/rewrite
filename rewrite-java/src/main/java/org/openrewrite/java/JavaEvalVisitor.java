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
import org.openrewrite.EvalContext;
import org.openrewrite.EvalVisitor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JavaEvalVisitor extends EvalVisitor<J> implements JavaVisitor<J, EvalContext> {
    public J visitExpression(Expression expression, EvalContext ctx) {
        return expression;
    }

    public J visitStatement(Statement statement, EvalContext ctx) {
        return statement;
    }

    @Override
    public J visitAnnotatedType(J.AnnotatedType annotatedType, EvalContext ctx) {
        J.AnnotatedType a = eval(annotatedType, ctx, this::visitEach);
        a = eval(a, ctx, this::visitExpression);
        a = a.withAnnotations(eval(a.getAnnotations(), ctx));
        return a.withTypeExpr(eval(a.getTypeExpr(), ctx));
    }

    @Override
    public J visitAnnotation(J.Annotation annotation, EvalContext ctx) {
        J.Annotation a = eval(annotation, ctx, this::visitEach);
        a = eval(a, ctx, this::visitExpression);
        a = a.withArgs(eval(a.getArgs(), ctx));
        return a.withAnnotationType(eval(a.getAnnotationType(), ctx));
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, EvalContext ctx) {
        J.ArrayAccess a = eval(arrayAccess, ctx, this::visitEach);
        a = eval(a, ctx, this::visitExpression);
        a = a.withIndexed(eval(a.getIndexed(), ctx));
        // FIXME implement me!
//        a = a.withDimension(eval(a.getDimension(), ctx));
        return a;
    }

    @Override
    public J visitArrayType(J.ArrayType arrayType, EvalContext ctx) {
        J.ArrayType a = eval(arrayType, ctx, this::visitEach);
        a = eval(a, ctx, this::visitExpression);
        return a.withElementType(eval(a.getElementType(), ctx));
    }

    @Override
    public J visitAssert(J.Assert azzert, EvalContext ctx) {
        J.Assert a = eval(azzert, ctx, this::visitEach);
        a = eval(a, ctx, this::visitStatement);
        return a.withCondition(eval(a.getCondition(), ctx));
    }

    @Override
    public J visitAssign(J.Assign assign, EvalContext ctx) {
        J.Assign a = eval(assign, ctx, this::visitEach);
        a = eval(a, ctx, this::visitStatement);
        a = eval(a, ctx, this::visitExpression);
        a = a.withVariable(eval(a.getVariable(), ctx));
        return a.withAssignment(eval(a.getAssignment(), ctx));
    }

    @Override
    public J visitAssignOp(J.AssignOp assignOp, EvalContext ctx) {
        J.AssignOp a = eval(assignOp, ctx, this::visitEach);
        a = eval(a, ctx, this::visitStatement);
        a = eval(a, ctx, this::visitExpression);
        a = a.withVariable(eval(a.getVariable(), ctx));
        return a.withAssignment(eval(a.getAssignment(), ctx));
    }

    @Override
    public J visitBinary(J.Binary binary, EvalContext ctx) {
        J.Binary b = eval(binary, ctx, this::visitEach);
        b = eval(b, ctx, this::visitExpression);
        b = b.withLeft(eval(b.getLeft(), ctx));
        return b.withRight(eval(b.getRight(), ctx));
    }

    @Override
    public J visitBlock(J.Block block, EvalContext ctx) {
        J.Block b = eval(block, ctx, this::visitEach);
        b = eval(b, ctx, this::visitStatement);
        return b.withStatements(evalMany(b.getStatements(), ctx));
    }

    @Override
    public J visitBreak(J.Break breakStatement, EvalContext ctx) {
        J.Break b = eval(breakStatement, ctx, this::visitEach);
        b = eval(b, ctx, this::visitStatement);
        return b.withLabel(eval(b.getLabel(), ctx));
    }

    @Override
    public J visitCase(J.Case caze, EvalContext ctx) {
        J.Case c = eval(caze, ctx, this::visitEach);
        c = eval(c, ctx, this::visitStatement);
        c = c.withPattern(eval(c.getPattern(), ctx));
        return c.withStatements(evalMany(c.getStatements(), ctx));
    }

    @Override
    public J visitCatch(J.Try.Catch catzh, EvalContext ctx) {
        J.Try.Catch c = catzh;
        c = c.withParam(eval(c.getParam(), ctx));
        return c.withBody(eval(c.getBody(), ctx));
    }

    @Override
    public J visitClassDecl(J.ClassDecl classDecl, EvalContext ctx) {
        J.ClassDecl c = eval(classDecl, ctx, this::visitEach);
        c = eval(c, ctx, this::visitStatement);
        c = c.withAnnotations(eval(c.getAnnotations(), ctx));
        c = c.withModifiers(eval(c.getModifiers(), ctx));
        c = c.withTypeParameters(eval(c.getTypeParameters(), ctx));
        c = c.withName(eval(c.getName(), ctx));
        c = c.withExtends(eval(c.getExtends(), ctx));
        c = c.withImplements(eval(c.getImplements(), ctx));
        return c.withBody(eval(c.getBody(), ctx));
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, EvalContext ctx) {
        J.CompilationUnit c = eval(cu, ctx, this::visitEach);
        c = c.withPackageDecl(eval(c.getPackageDecl(), ctx));
        c = c.withImports(evalMany(c.getImports(), ctx));
        return c.withClasses(eval(c.getClasses(), ctx));
    }

    @Override
    public J visitContinue(J.Continue continueStatement, EvalContext ctx) {
        J.Continue c = eval(continueStatement, ctx, this::visitEach);
        c = eval(c, ctx, this::visitStatement);
        return c.withLabel(eval(c.getLabel(), ctx));
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, EvalContext ctx) {
        J.DoWhileLoop d = eval(doWhileLoop, ctx, this::visitEach);
        d = eval(d, ctx, this::visitStatement);
        d = d.withWhileCondition(eval(d.getWhileCondition(), ctx));
        return d.withBody(eval(d.getBody(), ctx));
    }

    @Override
    public J visitEmpty(J.Empty empty, EvalContext ctx) {
        J.Empty e = eval(empty, ctx, this::visitEach);
        e = eval(e, ctx, this::visitStatement);
        e = eval(e, ctx, this::visitExpression);
        return e;
    }

    @Override
    public J visitEnumValue(J.EnumValue enoom, EvalContext ctx) {
        J.EnumValue e = eval(enoom, ctx, this::visitEach);
        e = e.withName(eval(e.getName(), ctx));
        return e.withInitializer(eval(e.getInitializer(), ctx));
    }

    @Override
    public J visitEnumValueSet(J.EnumValueSet enums, EvalContext ctx) {
        J.EnumValueSet e = eval(enums, ctx, this::visitEach);
        e = eval(e, ctx, this::visitStatement);
        return e.withEnums(evalMany(e.getEnums(), ctx));
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, EvalContext ctx) {
        J.FieldAccess f = eval(fieldAccess, ctx, this::visitEach);
        f = eval(f, ctx, this::visitExpression);
        f = f.withTarget(eval(f.getTarget(), ctx));
        return f.withName(eval(f.getName(), ctx));
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forLoop, EvalContext ctx) {
        J.ForEachLoop f = eval(forLoop, ctx, this::visitEach);
        f = eval(f, ctx, this::visitStatement);
        f = f.withControl(f.getControl().withVariable(eval(f.getControl().getVariable(), ctx)));
        f = f.withControl(f.getControl().withIterable(eval(f.getControl().getIterable(), ctx)));
        return f.withBody(eval(f.getBody(), ctx));
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop, EvalContext ctx) {
        J.ForLoop f = eval(forLoop, ctx, this::visitEach);
        f = eval(f, ctx, this::visitStatement);
        f = f.withControl(f.getControl().withInit(eval(f.getControl().getInit(), ctx)));
        f = f.withControl(f.getControl().withCondition(eval(f.getControl().getCondition(), ctx)));
        f = f.withControl(f.getControl().withUpdate(evalMany(f.getControl().getUpdate(), ctx)));
        return f.withBody(eval(f.getBody(), ctx));
    }

    @Override
    public J visitIdentifier(J.Ident ident, EvalContext ctx) {
        return eval(ident, ctx, this::visitExpression);
    }

    @Override
    public J visitIf(J.If iff, EvalContext ctx) {
        J.If i = eval(iff, ctx, this::visitEach);
        i = eval(i, ctx, this::visitStatement);
        i = i.withIfCondition(eval(i.getIfCondition(), ctx));
        i = i.withThenPart(eval(i.getThenPart(), ctx));

        if(i.getElsePart() != null) {
            JRightPadded<Statement> elze = eval(i.getElsePart().getElem(), ctx);
            if(elze != i.getElsePart().getElem()) {
                i = i.withElsePart(new JLeftPadded<>(i.getElsePart().getBefore(), elze));
            }
        }
        return i;
    }

    @Override
    public J visitImport(J.Import impoort, EvalContext ctx) {
        return impoort.withQualid(eval(impoort.getQualid(), ctx));
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, EvalContext ctx) {
        J.InstanceOf i = eval(instanceOf, ctx, this::visitEach);
        i = eval(i, ctx, this::visitExpression);
        i = i.withExpr(eval(i.getExpr(), ctx));
        return i.withClazz(eval(i.getClazz(), ctx));
    }

    @Override
    public J visitLabel(J.Label label, EvalContext ctx) {
        J.Label l = eval(label, ctx, this::visitEach);
        l = eval(l, ctx, this::visitStatement);
        return l.withStatement(eval(l.getStatement(), ctx));
    }

    @Override
    public J visitLambda(J.Lambda lambda, EvalContext ctx) {
        J.Lambda l = eval(lambda, ctx, this::visitEach);
        l = eval(l, ctx, this::visitExpression);
        l = l.withParameters(eval(l.getParameters(), ctx));
        return l.withBody(eval(l.getBody(), ctx));
    }

    @Override
    public J visitLiteral(J.Literal literal, EvalContext ctx) {
        return eval(literal, ctx, this::visitExpression);
    }

    @Override
    public J visitMemberReference(J.MemberReference memberRef, EvalContext ctx) {
        J.MemberReference m = eval(memberRef, ctx, this::visitEach);
        m = m.withContaining(eval(m.getContaining(), ctx));
        m = m.withTypeParameters(eval(m.getTypeParameters(), ctx));
        return m.withReference(eval(m.getReference(), ctx));
    }

    @Override
    public J visitMethod(J.MethodDecl method, EvalContext ctx) {
        J.MethodDecl m = eval(method, ctx, this::visitEach);
        m = eval(m, ctx, this::visitStatement);
        m = m.withAnnotations(eval(m.getAnnotations(), ctx));
        m = m.withModifiers(eval(m.getModifiers(), ctx));
        m = m.withTypeParameters(eval(m.getTypeParameters(), ctx));
        m = m.withReturnTypeExpr(eval(m.getReturnTypeExpr(), ctx));
        m = m.withName(eval(m.getName(), ctx));
        m = m.withParams(eval(m.getParams(), ctx));
        m = m.withParams(eval(m.getParams(), ctx));
        m = m.withThrows(eval(m.getThrows(), ctx));
        return m.withBody(eval(m.getBody(), ctx));
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, EvalContext ctx) {
        J.MethodInvocation m = eval(method, ctx, this::visitEach);
        m = eval(m, ctx, this::visitStatement);
        m = eval(m, ctx, this::visitExpression);
        m = m.withSelect(eval(m.getSelect(), ctx));
        m = m.withTypeParameters(eval(m.getTypeParameters(), ctx));
        m = m.withName(eval(m.getName(), ctx));
        return m.withArgs(eval(m.getArgs(), ctx));
    }

    @Override
    public J visitMultiCatch(J.MultiCatch multiCatch, EvalContext ctx) {
        return multiCatch.withAlternatives(evalMany(multiCatch.getAlternatives(), ctx));
    }

    @Override
    public J visitMultiVariable(J.VariableDecls multiVariable, EvalContext ctx) {
        J.VariableDecls m = eval(multiVariable, ctx, this::visitEach);
        m = eval(m, ctx, this::visitStatement);
        m = m.withModifiers(eval(m.getModifiers(), ctx));
        m = m.withAnnotations(eval(m.getAnnotations(), ctx));
        m = m.withTypeExpr(eval(m.getTypeExpr(), ctx));
        return m.withVars(evalMany(m.getVars(), ctx));
    }

    @Override
    public J visitNewArray(J.NewArray newArray, EvalContext ctx) {
        J.NewArray n = eval(newArray, ctx, this::visitEach);
        n = eval(n, ctx, this::visitExpression);
        n = n.withTypeExpr(eval(n.getTypeExpr(), ctx));
        return n.withInitializer(eval(n.getInitializer(), ctx));
    }

    @Override
    public J visitNewClass(J.NewClass newClass, EvalContext ctx) {
        J.NewClass n = eval(newClass, ctx, this::visitEach);
        n = eval(n, ctx, this::visitStatement);
        n = eval(n, ctx, this::visitExpression);
        n = n.withClazz(eval(n.getClazz(), ctx));
        n = n.withArgs(eval(n.getArgs(), ctx));
        return n.withBody(eval(n.getBody(), ctx));
    }

    @Override
    public J visitPackage(J.Package pkg, EvalContext ctx) {
        return pkg.withExpr(eval(pkg.getExpr(), ctx));
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type, EvalContext ctx) {
        J.ParameterizedType p = eval(type, ctx, this::visitEach);
        p = eval(p, ctx, this::visitExpression);
        p = p.withClazz(eval(p.getClazz(), ctx));
        return p.withTypeParameters(eval(p.getTypeParameters(), ctx));
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens, EvalContext ctx) {
        J.Parentheses<T> p = eval(parens, ctx, this::visitExpression);
        return p.withTree(eval(p.getTree(), ctx));
    }

    @Override
    public J visitPrimitive(J.Primitive primitive, EvalContext ctx) {
        return eval(primitive, ctx, this::visitExpression);
    }

    @Override
    public J visitReturn(J.Return retrn, EvalContext ctx) {
        J.Return r = eval(retrn, ctx, this::visitEach);
        r = eval(r, ctx, this::visitStatement);
        return r.withExpr(eval(r.getExpr(), ctx));
    }

    @Override
    public J visitSwitch(J.Switch switzh, EvalContext ctx) {
        J.Switch s = eval(switzh, ctx, this::visitEach);
        s = eval(s, ctx, this::visitStatement);
        s = s.withSelector(eval(s.getSelector(), ctx));
        return s.withCases(eval(s.getCases(), ctx));
    }

    @Override
    public J visitSynchronized(J.Synchronized synch, EvalContext ctx) {
        J.Synchronized s = eval(synch, ctx, this::visitEach);
        s = eval(s, ctx, this::visitStatement);
        s = s.withLock(eval(s.getLock(), ctx));
        return s.withBody(eval(s.getBody(), ctx));
    }

    @Override
    public J visitTernary(J.Ternary ternary, EvalContext ctx) {
        J.Ternary t = eval(ternary, ctx, this::visitEach);
        t = eval(t, ctx, this::visitExpression);
        t = t.withCondition(eval(t.getCondition(), ctx));
        t = t.withTruePart(eval(t.getTruePart(), ctx));
        return t.withFalsePart(eval(t.getFalsePart(), ctx));
    }

    @Override
    public J visitThrow(J.Throw thrown, EvalContext ctx) {
        J.Throw t = eval(thrown, ctx, this::visitEach);
        t = eval(t, ctx, this::visitStatement);
        return t.withException(eval(t.getException(), ctx));
    }

    @Override
    public J visitTry(J.Try tryable, EvalContext ctx) {
        J.Try t = eval(tryable, ctx, this::visitEach);
        t = eval(t, ctx, this::visitStatement);
        t = t.withResources(eval(t.getResources(), ctx));
        t = t.withBody(eval(t.getBody(), ctx));
        t = t.withCatches(eval(t.getCatches(), ctx));
        return t.withFinally(eval(t.getFinally(), ctx));
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast, EvalContext ctx) {
        J.TypeCast t = eval(typeCast, ctx, this::visitEach);
        t = eval(t, ctx, this::visitExpression);
        t = t.withClazz(eval(t.getClazz(), ctx));
        return t.withExpr(eval(t.getExpr(), ctx));
    }

    @Override
    public J visitTypeParameter(J.TypeParameter typeParam, EvalContext ctx) {
        J.TypeParameter t = eval(typeParam, ctx, this::visitEach);
        t = t.withAnnotations(eval(t.getAnnotations(), ctx));
        t = t.withName(eval(t.getName(), ctx));
        return t.withBounds(eval(t.getBounds(), ctx));
    }

    @Override
    public J visitUnary(J.Unary unary, EvalContext ctx) {
        J.Unary u = eval(unary, ctx, this::visitEach);
        u = eval(u, ctx, this::visitStatement);
        u = eval(u, ctx, this::visitExpression);
        return u.withExpr(eval(u.getExpr(), ctx));
    }

    @Override
    public J visitVariable(J.VariableDecls.NamedVar variable, EvalContext ctx) {
        J.VariableDecls.NamedVar v = variable;
        v = v.withName(eval(v.getName(), ctx));
        return v.withInitializer(eval(v.getInitializer(), ctx));
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop, EvalContext ctx) {
        J.WhileLoop w = eval(whileLoop, ctx, this::visitEach);
        w = eval(w, ctx, this::visitStatement);
        w = w.withCondition(eval(w.getCondition(), ctx));
        return w.withBody(eval(w.getBody(), ctx));
    }

    @Override
    public J visitWildcard(J.Wildcard wildcard, EvalContext ctx) {
        J.Wildcard w = eval(wildcard, ctx, this::visitEach);
        w = eval(w, ctx, this::visitExpression);
        return w.withBoundedType(eval(w.getBoundedType(), ctx));
    }

    @Nullable
    protected <J2 extends J> JRightPadded<J2> eval(@Nullable JRightPadded<J2> right, EvalContext ctx) {
        if (right == null) {
            return null;
        }
        J2 j = eval(right.getElem(), ctx);
        return j == right.getElem() ? right : new JRightPadded<>(j, right.getAfter());
    }

    @Nullable
    protected <J2 extends J> JLeftPadded<J2> eval(@Nullable JLeftPadded<J2> left, EvalContext ctx) {
        if (left == null) {
            return null;
        }
        J2 j = eval(left.getElem(), ctx);
        return j == left.getElem() ? left : new JLeftPadded<>(left.getBefore(), j);
    }

    @Nullable
    protected <J2 extends J> List<JRightPadded<J2>> evalMany(@Nullable List<JRightPadded<J2>> trees, EvalContext ctx) {
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
    protected <J2 extends J> JContainer<J2> eval(@Nullable JContainer<J2> container, EvalContext ctx) {
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
     * See {@link JavaEvalVisitor#isInSameNameScope}
     *
     * @param child A pointer to an element within the abstract syntax tree
     * @return true if the child is in within the lexical scope of the current cursor
     */
    protected boolean isInSameNameScope(Cursor child) {
        return isInSameNameScope(getCursor(), child);
    }
}
