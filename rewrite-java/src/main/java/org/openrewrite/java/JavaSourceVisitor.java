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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.toList;

public abstract class JavaSourceVisitor<R> extends SourceVisitor<R> {
    public J.CompilationUnit enclosingCompilationUnit() {
        J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
        if (cu == null) {
            throw new IllegalStateException("Expected to find a J.CompilationUnit in " + this);
        }
        return cu;
    }

    public J.Block<?> enclosingBlock() {
        return getCursor().firstEnclosing(J.Block.class);
    }

    @Nullable
    public J.MethodDecl enclosingMethod() {
        return getCursor().firstEnclosing(J.MethodDecl.class);
    }

    public J.ClassDecl enclosingClass() {
        return getCursor().firstEnclosing(J.ClassDecl.class);
    }

    public boolean isInSameNameScope(Cursor higher, Cursor lower) {
        AtomicBoolean takeWhile = new AtomicBoolean(true);
        return higher.getPathAsStream()
                .filter(t -> t instanceof J.Block ||
                        t instanceof J.MethodDecl ||
                        t instanceof J.Try ||
                        t instanceof J.ForLoop ||
                        t instanceof J.ForEachLoop).findAny()
                .map(higherNameScope -> lower.getPathAsStream()
                        .filter(t -> {
                            takeWhile.set(takeWhile.get() && (
                                    !(t instanceof J.ClassDecl) ||
                                            (((J.ClassDecl) t).getKind() instanceof J.ClassDecl.Kind.Class &&
                                                    !((J.ClassDecl) t).hasModifier("static"))));
                            return takeWhile.get();
                        })
                        .anyMatch(higherNameScope::equals))
                .orElse(false);
    }

    /**
     * @param lower The cursor of the lower scoped tree element to check.
     * @return Whether this cursor shares the same name scope as {@code scope}.
     */
    public boolean isInSameNameScope(Cursor lower) {
        return isInSameNameScope(getCursor(), lower);
    }

    private R visitTypeNameAfter(R r, @Nullable NameTree name) {
        return name == null ? r : reduce(r, visitTypeName(name));
    }

    private R visitTypeNamesAfter(R r, @Nullable Iterable<? extends NameTree> names) {
        if (names != null) {
            for (NameTree name : names) {
                if (name != null) {
                    r = reduce(r, visitTypeName(name));
                }
            }
        }
        return r;
    }

    public R visitExpression(Expression expr) {
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

    public R visitStatement(Statement statement) {
        return defaultTo(statement);
    }

    public R visitTypeName(NameTree name) {
        return defaultTo(name);
    }

    public R visitAnnotatedType(J.AnnotatedType annotatedType) {
        return reduce(
                defaultTo(annotatedType),
                reduce(
                        visitExpression(annotatedType),
                        visitTypeNameAfter(
                                visitAfter(
                                        visit(annotatedType.getTypeExpr()),
                                        annotatedType.getAnnotations()
                                ),
                                annotatedType.getTypeExpr()
                        )
                )
        );
    }

    public R visitAnnotation(J.Annotation annotation) {
        return reduce(
                defaultTo(annotation),
                reduce(
                        visitExpression(annotation),
                        visitTypeNameAfter(
                                visitAfter(
                                        visit(annotation.getAnnotationType()),
                                        annotation.getArgs() == null ? null : annotation.getArgs().getArgs()
                                ),
                                annotation.getAnnotationType()
                        )
                )
        );
    }

    public R visitArrayAccess(J.ArrayAccess arrayAccess) {
        return reduce(
                defaultTo(arrayAccess),
                reduce(
                        visitExpression(arrayAccess),
                        visitAfter(visit(arrayAccess.getDimension().getIndex()), arrayAccess.getIndexed())
                )
        );
    }

    public R visitArrayType(J.ArrayType arrayType) {
        return reduce(
                defaultTo(arrayType),
                reduce(
                        visitExpression(arrayType),
                        visitTypeNameAfter(visit(arrayType.getElementType()), arrayType.getElementType())
                )
        );
    }

    public R visitAssert(J.Assert azzert) {
        return reduce(
                defaultTo(azzert),
                reduce(
                        visitStatement(azzert),
                        visit(azzert.getCondition())
                )
        );
    }

    public R visitAssign(J.Assign assign) {
        return reduce(
                defaultTo(assign),
                reduce(
                        visitExpression(assign),
                        reduce(visitStatement(assign),
                                visitAfter(visit(assign.getVariable()), assign.getAssignment())
                        )
                )
        );
    }

    public R visitAssignOp(J.AssignOp assignOp) {
        return reduce(
                defaultTo(assignOp),
                reduce(
                        visitExpression(assignOp),
                        reduce(visitStatement(assignOp),
                                visitAfter(visit(assignOp.getVariable()), assignOp.getAssignment())
                        )
                )
        );
    }

    public R visitBinary(J.Binary binary) {
        return reduce(
                defaultTo(binary),
                reduce(
                        visitExpression(binary),
                        visitAfter(visit(binary.getLeft()), binary.getRight())
                )
        );
    }

    public R visitBlock(J.Block<J> block) {
        return reduce(
                defaultTo(block),
                reduce(
                        visitStatement(block),
                        reduce(
                                visit(block.getStatements()),
                                visit(block.getEnd())
                        )
                )
        );
    }

    public R visitBreak(J.Break breakStatement) {
        return reduce(
                defaultTo(breakStatement),
                reduce(
                        visitStatement(breakStatement),
                        visit(breakStatement.getLabel())
                )
        );
    }

    public R visitCase(J.Case caze) {
        return reduce(
                defaultTo(caze),
                reduce(
                        visitStatement(caze),
                        visitAfter(visit(caze.getStatements()), caze.getPattern())
                )
        );
    }

    public R visitCatch(J.Try.Catch catzh) {
        return reduce(
                defaultTo(catzh),
                visitAfter(visit(catzh.getBody()), catzh.getParam())
        );
    }

    public R visitClassDecl(J.ClassDecl classDecl) {
        return reduce(
                defaultTo(classDecl),
                reduce(
                        visitStatement(classDecl),
                        visitTypeNamesAfter(
                                visitTypeNameAfter(
                                        visitAfter(
                                                visitAfter(
                                                        visitAfter(
                                                                visitAfter(
                                                                        visitAfter(
                                                                                visitAfter(
                                                                                        visit(classDecl.getBody()),
                                                                                        classDecl.getModifiers()
                                                                                ),
                                                                                classDecl.getName()
                                                                        ),
                                                                        classDecl.getTypeParameters()
                                                                ),
                                                                classDecl.getExtends()
                                                        ),
                                                        classDecl.getImplements()
                                                ),
                                                classDecl.getAnnotations()
                                        ),
                                        classDecl.getExtends() == null ? null : classDecl.getExtends().getFrom()
                                ),
                                classDecl.getImplements() == null ? null : classDecl.getImplements().getFrom()
                        )
                )
        );
    }

    public R visitCompilationUnit(J.CompilationUnit cu) {
        return reduce(
                defaultTo(cu),
                visitAfter(
                        visitAfter(
                                visit(cu.getImports()),
                                cu.getPackageDecl()
                        ),
                        cu.getClasses()
                )
        );
    }

    public R visitContinue(J.Continue continueStatement) {
        return reduce(
                defaultTo(continueStatement),
                reduce(
                        visitStatement(continueStatement),
                        visit(continueStatement.getLabel())
                )
        );
    }

    public R visitDoWhileLoop(J.DoWhileLoop doWhileLoop) {
        return reduce(
                defaultTo(doWhileLoop),
                reduce(
                        visitStatement(doWhileLoop),
                        visitAfter(visit(doWhileLoop.getBody()), doWhileLoop.getWhileCondition().getCondition())
                )
        );
    }

    public R visitEmpty(J.Empty empty) {
        return reduce(
                defaultTo(empty),
                reduce(
                        visitExpression(empty),
                        visitStatement(empty)
                )
        );
    }

    public R visitEnumValue(J.EnumValue enoom) {
        return reduce(
                defaultTo(enoom),
                reduce(
                        visitStatement(enoom),
                        visitAfter(
                                visit(enoom.getName()),
                                enoom.getInitializer() == null ? null : enoom.getInitializer().getArgs()
                        )
                )
        );
    }

    public R visitEnumValueSet(J.EnumValueSet enums) {
        return reduce(
                defaultTo(enums),
                reduce(
                        visitStatement(enums),
                        visit(enums.getEnums())
                )
        );
    }

    public R visitFinally(J.Try.Finally finallie) {
        return reduce(
                defaultTo(finallie),
                visit(finallie.getBody())
        );
    }

    public R visitFieldAccess(J.FieldAccess fieldAccess) {
        return reduce(
                defaultTo(fieldAccess),
                reduce(
                        visitExpression(fieldAccess),
                        visitTypeNameAfter(
                                visitAfter(
                                        visit(fieldAccess.getTarget()),
                                        fieldAccess.getName()
                                ),
                                fieldAccess.asClassReference()
                        )
                )
        );
    }

    public R visitForEachLoop(J.ForEachLoop forEachLoop) {
        return reduce(
                defaultTo(forEachLoop),
                reduce(
                        visitStatement(forEachLoop),
                        visitAfter(
                                visitAfter(
                                        visit(forEachLoop.getBody()),
                                        forEachLoop.getControl().getIterable()
                                ),
                                forEachLoop.getControl().getVariable()
                        )
                )
        );
    }

    public R visitForLoop(J.ForLoop forLoop) {
        return reduce(
                defaultTo(forLoop),
                reduce(
                        visitStatement(forLoop),
                        visitAfter(
                                visitAfter(
                                        visitAfter(
                                                visit(forLoop.getBody()),
                                                forLoop.getControl().getUpdate()
                                        ),
                                        forLoop.getControl().getCondition()
                                ),
                                forLoop.getControl().getInit()
                        )
                )
        );
    }

    public R visitIdentifier(J.Ident ident) {
        return reduce(
                defaultTo(ident),
                visitExpression(ident)
        );
    }

    public R visitIf(J.If iff) {
        return reduce(
                defaultTo(iff),
                reduce(
                        visitStatement(iff),
                        visitAfter(
                                visitAfter(
                                        visit(iff.getThenPart()),
                                        iff.getElsePart()
                                ),
                                iff.getIfCondition()
                        )
                )
        );
    }

    public R visitElse(J.If.Else elze) {
        return reduce(
                defaultTo(elze),
                visit(elze.getStatement())
        );
    }

    public R visitImport(J.Import impoort) {
        return reduce(
                defaultTo(impoort),
                visit(impoort.getQualid())
        );
    }

    public R visitInstanceOf(J.InstanceOf instanceOf) {
        return reduce(
                defaultTo(instanceOf),
                reduce(
                        visitExpression(instanceOf),
                        visitAfter(visit(instanceOf.getExpr()), instanceOf.getClazz())
                )
        );
    }

    public R visitLabel(J.Label label) {
        return reduce(
                defaultTo(label),
                reduce(
                        visitStatement(label),
                        visitAfter(visit(label.getLabel()), label.getStatement())
                )
        );
    }

    public R visitLambda(J.Lambda lambda) {
        return reduce(
                defaultTo(lambda),
                reduce(
                        visitExpression(lambda),
                        visitAfter(visit(lambda.getParamSet().getParams()), lambda.getBody())
                )
        );
    }

    public R visitLiteral(J.Literal literal) {
        return reduce(
                defaultTo(literal),
                visitExpression(literal)
        );
    }

    public R visitMemberReference(J.MemberReference memberRef) {
        return reduce(
                defaultTo(memberRef),
                visitAfter(
                        visitAfter(
                                visit(memberRef.getTypeParameters()),
                                memberRef.getReference()
                        ),
                        memberRef.getContaining()
                )
        );
    }

    public R visitMethod(J.MethodDecl method) {
        return reduce(
                defaultTo(method),
                visitTypeNamesAfter(
                        visitTypeNameAfter(
                                visitAfter(
                                        visitAfter(
                                                visitAfter(
                                                        visitAfter(
                                                                visitAfter(
                                                                        visitAfter(
                                                                                visitAfter(
                                                                                        visitAfter(
                                                                                                visit(method.getBody()),
                                                                                                method.getModifiers()
                                                                                        ),
                                                                                        method.getTypeParameters()
                                                                                ),
                                                                                method.getName()
                                                                        ),
                                                                        method.getReturnTypeExpr()
                                                                ),
                                                                method.getParams().getParams()
                                                        ),
                                                        method.getThrows() == null ? null : method.getThrows().getExceptions()
                                                ),
                                                method.getAnnotations()
                                        ),
                                        method.getDefaultValue()
                                ),
                                method.getReturnTypeExpr()
                        ),
                        method.getThrows() == null ? null : method.getThrows().getExceptions()
                )
        );
    }

    public R visitMethodInvocation(J.MethodInvocation method) {
        R selectTypeVisit = (method.getSelect() instanceof NameTree && (method.getType() != null && method.getType().hasFlags(Flag.Static))) ?
                visitTypeName((NameTree) method.getSelect()) : defaultTo(method);

        return reduce(
                defaultTo(method),
                reduce(
                        visitStatement(method),
                        reduce(
                                visitExpression(method),
                                reduce(
                                        selectTypeVisit,
                                        visitAfter(
                                                visitAfter(
                                                        visitAfter(
                                                                visit(method.getArgs().getArgs()),
                                                                method.getName()
                                                        ),
                                                        method.getTypeParameters()
                                                ),
                                                method.getSelect()
                                        )
                                )
                        )
                )
        );
    }

    public R visitMultiCatch(J.MultiCatch multiCatch) {
        return reduce(
                defaultTo(multiCatch),
                visitTypeNamesAfter(
                        visit(multiCatch.getAlternatives()),
                        multiCatch.getAlternatives()
                )
        );
    }

    public R visitMultiVariable(J.VariableDecls multiVariable) {
        R varTypeVisit = multiVariable.getTypeExpr() instanceof J.MultiCatch ?
                defaultTo(multiVariable) :
                multiVariable.getTypeExpr() == null ?
                        defaultTo(multiVariable) :
                        visitTypeName(multiVariable.getTypeExpr());

        return reduce(
                defaultTo(multiVariable),
                reduce(
                        visitStatement(multiVariable),
                        reduce(
                                varTypeVisit,
                                visitAfter(
                                        visitAfter(
                                                visitAfter(
                                                        visit(multiVariable.getVars()),
                                                        multiVariable.getTypeExpr()
                                                ),
                                                multiVariable.getModifiers()
                                        ),
                                        multiVariable.getAnnotations()
                                )
                        )
                )
        );
    }

    public R visitNewArray(J.NewArray newArray) {
        return reduce(
                defaultTo(newArray),
                reduce(
                        visitExpression(newArray),
                        visitTypeNameAfter(
                                visitAfter(
                                        visitAfter(
                                                visit(newArray.getTypeExpr()),
                                                newArray.getDimensions().stream().map(J.NewArray.Dimension::getSize).collect(toList())
                                        ),
                                        newArray.getInitializer() == null ? null : newArray.getInitializer().getElements()
                                ),
                                newArray.getTypeExpr()
                        )
                )
        );
    }

    public R visitNewClass(J.NewClass newClass) {
        return reduce(
                defaultTo(newClass),
                reduce(
                        visitExpression(newClass),
                        reduce(
                                visitStatement(newClass),
                                visitTypeNameAfter(
                                        visitAfter(
                                                visitAfter(
                                                        visit(newClass.getBody()),
                                                        newClass.getArgs() == null ? null : newClass.getArgs().getArgs()
                                                ),
                                                newClass.getClazz()
                                        ),
                                        newClass.getClazz()
                                )
                        )
                )
        );
    }

    public R visitPackage(J.Package pkg) {
        return reduce(
                defaultTo(pkg),
                visit(pkg.getExpr())
        );
    }

    public R visitParameterizedType(J.ParameterizedType type) {
        return reduce(
                defaultTo(type),
                reduce(
                        visitExpression(type),
                        visitTypeNameAfter(
                                visitAfter(
                                        visit(type.getTypeParameters()),
                                        type.getClazz()
                                ),
                                type.getClazz()
                        )
                )
        );
    }

    public <T extends J> R visitParentheses(J.Parentheses<T> parens) {
        return reduce(
                defaultTo(parens),
                reduce(
                        visitExpression(parens),
                        visit(parens.getTree())
                )
        );
    }

    public R visitPrimitive(J.Primitive primitive) {
        return reduce(
                defaultTo(primitive),
                visitExpression(primitive)
        );
    }

    public R visitReturn(J.Return retrn) {
        return reduce(
                defaultTo(retrn),
                reduce(
                        visitStatement(retrn),
                        visit(retrn.getExpr())
                )
        );
    }

    public R visitSwitch(J.Switch switzh) {
        return reduce(
                defaultTo(switzh),
                reduce(
                        visitStatement(switzh),
                        visitAfter(visit(switzh.getCases()), switzh.getSelector())
                )
        );
    }

    public R visitSynchronized(J.Synchronized synch) {
        return reduce(
                defaultTo(synch),
                reduce(
                        visitStatement(synch),
                        visitAfter(visit(synch.getBody()), synch.getLock())
                )
        );
    }

    public R visitTernary(J.Ternary ternary) {
        return reduce(
                defaultTo(ternary),
                reduce(
                        visitExpression(ternary),
                        visitAfter(
                                visitAfter(
                                        visit(ternary.getFalsePart()),
                                        ternary.getTruePart()
                                ),
                                ternary.getCondition()
                        )
                )
        );
    }

    public R visitThrow(J.Throw thrown) {
        return reduce(
                defaultTo(thrown),
                reduce(
                        visitStatement(thrown),
                        visit(thrown.getException())
                )
        );
    }

    public R visitTry(J.Try tryable) {
        return reduce(
                defaultTo(tryable),
                reduce(
                        visitStatement(tryable),
                        visitAfter(
                                visitAfter(
                                        visitAfter(
                                                visit(tryable.getBody()),
                                                tryable.getResources() == null ? null : tryable.getResources().getDecls()
                                        ),
                                        tryable.getCatches()
                                ),
                                tryable.getFinally()
                        )
                )
        );
    }

    public R visitTypeCast(J.TypeCast typeCast) {
        return reduce(
                defaultTo(typeCast),
                reduce(
                        visitExpression(typeCast),
                        visitTypeNameAfter(
                                visitAfter(
                                        visit(typeCast.getClazz()),
                                        typeCast.getExpr()
                                ),
                                typeCast.getClazz().getTree()
                        )
                )
        );
    }

    public R visitTypeParameter(J.TypeParameter typeParam) {
        return reduce(
                defaultTo(typeParam),
                visitTypeNameAfter(
                        visitTypeNamesAfter(
                                visitAfter(
                                        visitAfter(
                                                visit(typeParam.getAnnotations()),
                                                typeParam.getName()
                                        ),
                                        typeParam.getBounds() == null ? null : typeParam.getBounds().getTypes()
                                ),
                                typeParam.getBounds() == null ? null : typeParam.getBounds().getTypes()
                        ),
                        typeParam.getName() instanceof NameTree ? (NameTree) typeParam.getName() : null
                )
        );
    }

    public R visitTypeParameters(J.TypeParameters typeParams) {
        return reduce(
                defaultTo(typeParams),
                visit(typeParams.getParams())
        );
    }

    public R visitUnary(J.Unary unary) {
        return reduce(
                defaultTo(unary),
                reduce(
                        visitStatement(unary),
                        reduce(
                                visitExpression(unary),
                                visit(unary.getExpr())
                        )
                )
        );
    }

    public R visitUnparsedSource(J.UnparsedSource unparsed) {
        return reduce(
                defaultTo(unparsed),
                reduce(
                        visitStatement(unparsed),
                        visitExpression(unparsed)
                )
        );
    }

    public R visitVariable(J.VariableDecls.NamedVar variable) {
        return reduce(
                defaultTo(variable),
                visitAfter(
                        visitAfter(
                                visit(variable.getInitializer()),
                                variable.getDimensionsAfterName()
                        ),
                        variable.getName()
                )
        );
    }

    public R visitWhileLoop(J.WhileLoop whileLoop) {
        return reduce(
                defaultTo(whileLoop),
                reduce(
                        visitStatement(whileLoop),
                        visitAfter(visit(whileLoop.getBody()), whileLoop.getCondition())
                )
        );
    }

    public R visitWildcard(J.Wildcard wildcard) {
        return reduce(
                defaultTo(wildcard),
                reduce(
                        visitExpression(wildcard),
                        visitTypeNameAfter(
                                visitAfter(
                                        visit(wildcard.getBound()),
                                        wildcard.getBoundedType()
                                ),
                                wildcard.getBoundedType()
                        )
                )
        );
    }
}
