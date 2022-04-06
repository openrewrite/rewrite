/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.groovy;

import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;

/**
 * Adapt a JavaIsoVisitor to a Groovy AST.
 * This allows for reuse of all of a JavaIsoVisitor's method implementations that don't need to be altered for a Groovy AST.
 *
 * Groovy-specific AST elements will have their Markers and, if applicable, type visited by the delegate.
 *
 * @param <T>
 * @param <P>
 */
public class DelegatingGroovyVisitor<T extends JavaIsoVisitor<P>, P> extends GroovyVisitor<P> {
    protected final T delegate;
    public DelegatingGroovyVisitor(T delegate) {
        this.delegate = delegate;
    }

    @Override
    public J visitExpression(Expression expression, P p) {
        return super.visitExpression(delegate.visitExpression(expression, p), p);
    }

    @Override
    public J visitStatement(Statement statement, P p) {
        return super.visitStatement(delegate.visitStatement(statement, p), p);
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, P p) {
        return super.visitSpace(space, loc, p);
    }

    @Override
    public @Nullable JavaType visitType(@Nullable JavaType javaType, P p) {
        return super.visitType(delegate.visitType(javaType, p), p);
    }

    @Override
    public <N extends NameTree> N visitTypeName(N nameTree, P p) {
        return super.visitTypeName(delegate.visitTypeName(nameTree, p), p);
    }

    @Override
    public J visitContinue(J.Continue continueStatement, P p) {
        return super.visitContinue(delegate.visitContinue(continueStatement, p), p);
    }

    @Override
    public <Y extends J> J visitControlParentheses(J.ControlParentheses<Y> controlParens, P p) {
        return super.visitControlParentheses(delegate.visitControlParentheses(controlParens, p), p);
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        return super.visitDoWhileLoop(delegate.visitDoWhileLoop(doWhileLoop, p), p);
    }

    @Override
    public J visitEmpty(J.Empty empty, P p) {
        return super.visitEmpty(delegate.visitEmpty(empty, p), p);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public J visitEnumValue(J.EnumValue enoom, P p) {
        return super.visitEnumValue(delegate.visitEnumValue(enoom, p), p);
    }

    @Override
    public J visitEnumValueSet(J.EnumValueSet enums, P p) {
        return super.visitEnumValueSet(delegate.visitEnumValueSet(enums, p), p);
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        return super.visitFieldAccess(delegate.visitFieldAccess(fieldAccess, p), p);
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forLoop, P p) {
        return super.visitForEachLoop(delegate.visitForEachLoop(forLoop, p), p);
    }

    @Override
    public J visitForEachControl(J.ForEachLoop.Control control, P p) {
        return super.visitForEachControl(delegate.visitForEachControl(control, p), p);
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop, P p) {
        return super.visitForLoop(delegate.visitForLoop(forLoop, p), p);
    }

    @Override
    public J visitForControl(J.ForLoop.Control control, P p) {
        return super.visitForControl(delegate.visitForControl(control, p), p);
    }

    @Override
    public J visitIdentifier(J.Identifier ident, P p) {
        return super.visitIdentifier(delegate.visitIdentifier(ident, p), p);
    }

    @Override
    public J visitElse(J.If.Else elze, P p) {
        return super.visitElse(delegate.visitElse(elze, p), p);
    }

    @Override
    public J visitIf(J.If iff, P p) {
        return super.visitIf(delegate.visitIf(iff, p), p);
    }

    @Override
    public J visitImport(J.Import impoort, P p) {
        return super.visitImport(delegate.visitImport(impoort, p), p);
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, P p) {
        return super.visitInstanceOf(delegate.visitInstanceOf(instanceOf, p), p);
    }

    @Override
    public J visitLabel(J.Label label, P p) {
        return super.visitLabel(delegate.visitLabel(label, p), p);
    }

    @Override
    public J visitLambda(J.Lambda lambda, P p) {
        return super.visitLambda(delegate.visitLambda(lambda, p), p);
    }

    @Override
    public J visitLiteral(J.Literal literal, P p) {
        return super.visitLiteral(delegate.visitLiteral(literal, p), p);
    }

    @Override
    public J visitMemberReference(J.MemberReference memberRef, P p) {
        return super.visitMemberReference(delegate.visitMemberReference(memberRef, p), p);
    }

    @Override
    public J visitMultiCatch(J.MultiCatch multiCatch, P p) {
        return super.visitMultiCatch(delegate.visitMultiCatch(multiCatch, p), p);
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        return super.visitVariableDeclarations(delegate.visitVariableDeclarations(multiVariable, p), p);
    }

    @Override
    public J visitNewArray(J.NewArray newArray, P p) {
        return super.visitNewArray(delegate.visitNewArray(newArray, p), p);
    }

    @Override
    public J visitNewClass(J.NewClass newClass, P p) {
        return super.visitNewClass(delegate.visitNewClass(newClass, p), p);
    }

    @Override
    public J visitPackage(J.Package pkg, P p) {
        return super.visitPackage(delegate.visitPackage(pkg, p), p);
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type, P p) {
        return super.visitParameterizedType(delegate.visitParameterizedType(type, p), p);
    }

    @Override
    public <Y extends J> J visitParentheses(J.Parentheses<Y> parens, P p) {
        return super.visitParentheses(delegate.visitParentheses(parens, p), p);
    }

    @Override
    public J visitPrimitive(J.Primitive primitive, P p) {
        return super.visitPrimitive(delegate.visitPrimitive(primitive, p), p);
    }

    @Override
    public J visitReturn(J.Return retrn, P p) {
        return super.visitReturn(delegate.visitReturn(retrn, p), p);
    }

    @Override
    public J visitSwitch(J.Switch switzh, P p) {
        return super.visitSwitch(delegate.visitSwitch(switzh, p), p);
    }

    @Override
    public J visitSynchronized(J.Synchronized synch, P p) {
        return super.visitSynchronized(delegate.visitSynchronized(synch, p), p);
    }

    @Override
    public J visitTernary(J.Ternary ternary, P p) {
        return super.visitTernary(delegate.visitTernary(ternary, p), p);
    }

    @Override
    public J visitThrow(J.Throw thrown, P p) {
        return super.visitThrow(delegate.visitThrow(thrown, p), p);
    }

    @Override
    public J visitTry(J.Try tryable, P p) {
        return super.visitTry(delegate.visitTry(tryable, p), p);
    }

    @Override
    public J visitTryResource(J.Try.Resource tryResource, P p) {
        return super.visitTryResource(delegate.visitTryResource(tryResource, p), p);
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast, P p) {
        return super.visitTypeCast(delegate.visitTypeCast(typeCast, p), p);
    }

    @Override
    public J visitTypeParameter(J.TypeParameter typeParam, P p) {
        return super.visitTypeParameter(delegate.visitTypeParameter(typeParam, p), p);
    }

    @Override
    public J visitUnary(J.Unary unary, P p) {
        return super.visitUnary(delegate.visitUnary(unary, p), p);
    }

    @Override
    public J visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        return super.visitVariable(delegate.visitVariable(variable, p), p);
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop, P p) {
        return super.visitWhileLoop(delegate.visitWhileLoop(whileLoop, p), p);
    }

    @Override
    public J visitWildcard(J.Wildcard wildcard, P p) {
        return super.visitWildcard(delegate.visitWildcard(wildcard, p), p);
    }

    @Override
    public <Y> JRightPadded<Y> visitRightPadded(@Nullable JRightPadded<Y> right, JRightPadded.Location loc, P p) {
        return super.visitRightPadded(right, loc, p);
    }

    @Override
    public <Y> JLeftPadded<Y> visitLeftPadded(JLeftPadded<Y> left, JLeftPadded.Location loc, P p) {
        return super.visitLeftPadded(left, loc, p);
    }

    @Override
    public <J2 extends J> JContainer<J2> visitContainer(JContainer<J2> container, JContainer.Location loc, P p) {
        return super.visitContainer(container, loc, p);
    }

    @Override
     public J visitAnnotation(J.Annotation annotation, P p) {
         return super.visitAnnotation(delegate.visitAnnotation(annotation, p), p);
     }

     @Override
     public J visitAnnotatedType(J.AnnotatedType annotatedType, P p) {
         return super.visitAnnotatedType(delegate.visitAnnotatedType(annotatedType, p), p);
     }

     @Override
     public J visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
         return super.visitArrayAccess(delegate.visitArrayAccess(arrayAccess, p), p);
     }

     @Override
     public J visitArrayType(J.ArrayType arrayType, P p) {
         return super.visitArrayType(delegate.visitArrayType(arrayType, p), p);
     }

     @Override
     public J visitArrayDimension(J.ArrayDimension arrayDimension, P p) {
         return super.visitArrayDimension(delegate.visitArrayDimension(arrayDimension, p), p);
     }

    @Override
    public J visitAssert(J.Assert azzert, P p) {
        return super.visitAssert(delegate.visitAssert(azzert, p), p);
    }

    @Override
    public J visitAssignment(J.Assignment assignment, P p) {
        return super.visitAssignment(delegate.visitAssignment(assignment, p), p);
    }

    @Override
    public J visitAssignmentOperation(J.AssignmentOperation assignOp, P p) {
        return super.visitAssignmentOperation(delegate.visitAssignmentOperation(assignOp, p), p);
    }

    @Override
    public J visitBinary(J.Binary binary, P p) {
        return super.visitBinary(delegate.visitBinary(binary, p), p);
    }

    @Override
    public J visitBlock(J.Block block, P p) {
        return super.visitBlock(delegate.visitBlock(block, p), p);
    }

    @Override
    public J visitBreak(J.Break breakStatement, P p) {
        return super.visitBreak(delegate.visitBreak(breakStatement, p), p);
    }

    @Override
    public J visitCase(J.Case caze, P p) {
        return super.visitCase(delegate.visitCase(caze, p), p);
    }

    @Override
    public J visitCatch(J.Try.Catch catzh, P p) {
        return super.visitCatch(delegate.visitCatch(catzh, p), p);
    }

    @Override
     public J visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
         return super.visitClassDeclaration(delegate.visitClassDeclaration(classDecl, p), p);
     }

     @Override
     public J visitMethodDeclaration(J.MethodDeclaration method, P p) {
         return super.visitMethodDeclaration(delegate.visitMethodDeclaration(method, p), p);
     }

     @Override
     public J visitMethodInvocation(J.MethodInvocation method, P p) {
         return super.visitMethodInvocation(delegate.visitMethodInvocation(method, p), p);
     }

     @Override
    public G.GString visitGString(G.GString gString, P p) {
        G.GString g = (G.GString) super.visitGString(gString, p);
        g = g.withMarkers(delegate.visitMarkers(g.getMarkers(), p));
        g = g.withType(delegate.visitType(gString.getType(), p));
        return g;
    }

    @Override
    public G.ListLiteral visitListLiteral(G.ListLiteral listLiteral, P p) {
        G.ListLiteral l = (G.ListLiteral) super.visitListLiteral(listLiteral, p);
        l = l.withMarkers(delegate.visitMarkers(l.getMarkers(), p));
        l = l.withType(delegate.visitType(listLiteral.getType(), p));
        return l;
    }

    @Override
    public G.MapEntry visitMapEntry(G.MapEntry mapEntry, P p) {
        G.MapEntry m = (G.MapEntry) super.visitMapEntry(mapEntry, p);
        m = m.withMarkers(delegate.visitMarkers(m.getMarkers(), p));
        m = m.withType(delegate.visitType(mapEntry.getType(), p));
        return m;
    }

    @Override
    public G.MapLiteral visitMapLiteral(G.MapLiteral mapLiteral, P p) {
        G.MapLiteral m = (G.MapLiteral) super.visitMapLiteral(mapLiteral, p);
        m = m.withMarkers(delegate.visitMarkers(m.getMarkers(), p));
        m = m.withType(delegate.visitType(mapLiteral.getType(), p));
        return m;
    }

    @Override
    public G.Binary visitBinary(G.Binary binary, P p) {
        G.Binary b = (G.Binary) super.visitBinary(binary, p);
        b = b.withMarkers(delegate.visitMarkers(b.getMarkers(), p));
        b = b.withType(delegate.visitType(binary.getType(), p));
        return b;
    }
}
