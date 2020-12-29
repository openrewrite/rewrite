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
import org.openrewrite.SourceVisitor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.util.Iterator;

public interface JavaSourceVisitor<R> extends SourceVisitor<R> {

    default J.CompilationUnit enclosingCompilationUnit() {
        J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
        if (cu == null) {
            throw new IllegalStateException("Expected to find a J.CompilationUnit in " + this);
        }
        return cu;
    }

    default J.Block enclosingBlock() {
        return getCursor().firstEnclosing(J.Block.class);
    }

    @Nullable
    default J.MethodDecl enclosingMethod() {
        return getCursor().firstEnclosing(J.MethodDecl.class);
    }

    default J.ClassDecl enclosingClass() {
        return getCursor().firstEnclosing(J.ClassDecl.class);
    }

    /**
     * Check if a child AST element is in the same lexical scope as that of the AST element associated with the base
     * cursor. (i.e.: Are the variables and declarations visible in the base scope also visible to the child AST
     * element?)
     *
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
     * @param base A pointer within the AST that is used to establish the "base lexical scope".
     * @param child A pointer within the AST that will be traversed (up the tree) looking for an intersection with the base lexical scope.
     * @return true if the child is in within the lexical scope of the base
     */
    default boolean isInSameNameScope(Cursor base, Cursor child) {
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
     *
     * See {@link JavaSourceVisitor#isInSameNameScope}
     *
     * @param child A pointer to an element within the abstract syntax tree
     * @return true if the child is in within the lexical scope of the current cursor
     */
    default boolean isInSameNameScope(Cursor child) {
        return isInSameNameScope(getCursor(), child);
    }

    default R visitExpression(Expression expr) {
        if (expr.getType() instanceof JavaType.FullyQualified) {
            JavaType.FullyQualified exprType = (JavaType.FullyQualified) expr.getType();
            if (expr instanceof J.FieldAccess) {
                if (((J.FieldAccess) expr).getSimpleName().equals(exprType.getClassName())) {
                    return reduce(defaultTo(expr), visitTypeName((NameTree) expr));
                }
            } else if (expr instanceof J.Ident) {
                if (((J.Ident) expr).getSimpleName().equals(exprType.getClassName())) {
                    return reduce(defaultTo(expr), visitTypeName((NameTree) expr));
                }
            }
        }
        return defaultTo(expr);
    }

    R visitStatement(Statement statement);
    R visitTypeName(NameTree name);
    R visitAnnotatedType(J.AnnotatedType annotatedType);
    R visitAnnotation(J.Annotation annotation);
    R visitArrayAccess(J.ArrayAccess arrayAccess);
    R visitArrayType(J.ArrayType arrayType);
    R visitAssert(J.Assert azzert);
    R visitAssign(J.Assign assign);
    R visitAssignOp(J.AssignOp assignOp);
    R visitBinary(J.Binary binary);
    R visitBlock(J.Block block);
    R visitBreak(J.Break breakStatement);
    R visitCase(J.Case caze);
    R visitCatch(J.Try.Catch catzh);
    R visitClassDecl(J.ClassDecl classDecl);
    R visitCompilationUnit(J.CompilationUnit cu);
    R visitContinue(J.Continue continueStatement);
    R visitDoWhileLoop(J.DoWhileLoop doWhileLoop);
    R visitEmpty(J.Empty empty);
    R visitEnumValue(J.EnumValue enoom);
    R visitEnumValueSet(J.EnumValueSet enums);
    R visitFieldAccess(J.FieldAccess fieldAccess);
    R visitForEachLoop(J.ForEachLoop forEachLoop);
    R visitForLoop(J.ForLoop forLoop);
    R visitIdentifier(J.Ident ident);
    R visitIf(J.If iff);
    R visitImport(J.Import impoort);
    R visitInstanceOf(J.InstanceOf instanceOf);
    R visitLabel(J.Label label);
    R visitLambda(J.Lambda lambda);
    R visitLiteral(J.Literal literal);
    R visitMemberReference(J.MemberReference memberRef);
    R visitMethod(J.MethodDecl method);
    R visitMethodInvocation(J.MethodInvocation method);
    R visitMultiCatch(J.MultiCatch multiCatch);
    R visitMultiVariable(J.VariableDecls multiVariable);
    R visitNewArray(J.NewArray newArray);
    R visitNewClass(J.NewClass newClass);
    R visitPackage(J.Package pkg);
    R visitParameterizedType(J.ParameterizedType type);
    <T extends J> R visitParentheses(J.Parentheses<T> parens);
    R visitPrimitive(J.Primitive primitive);
    R visitReturn(J.Return retrn);
    R visitSwitch(J.Switch switzh);
    R visitSynchronized(J.Synchronized synch);
    R visitTernary(J.Ternary ternary);
    R visitThrow(J.Throw thrown);
    R visitTry(J.Try tryable);
    R visitTypeCast(J.TypeCast typeCast);
    R visitTypeParameter(J.TypeParameter typeParam);
    R visitUnary(J.Unary unary);
    R visitVariable(J.VariableDecls.NamedVar variable);
    R visitWhileLoop(J.WhileLoop whileLoop);
    R visitWildcard(J.Wildcard wildcard);
}
