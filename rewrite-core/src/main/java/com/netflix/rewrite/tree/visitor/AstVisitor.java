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
package com.netflix.rewrite.tree.visitor;

import com.netflix.rewrite.internal.lang.Nullable;
import com.netflix.rewrite.tree.*;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public abstract class AstVisitor<R> {
    public abstract R defaultTo(@Nullable Tree t);

    /**
     * Some sensible defaults for reduce (boolean OR, list concatenation, or else just the value of r1).
     * Override if your particular visitor needs to reduce values in a different way.
     */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    public R reduce(R r1, R r2) {
        if (r1 instanceof Boolean) {
            return (R) (Boolean) ((Boolean) r1 || (Boolean) r2);
        }
        if (r1 instanceof Set) {
            return (R) Stream.concat(
                    stream(((Iterable<?>) r1).spliterator(), false),
                    stream(((Iterable<?>) r2).spliterator(), false)
            ).collect(Collectors.toSet());
        }
        if (r1 instanceof Collection) {
            return (R) Stream.concat(
                    stream(((Iterable<?>) r1).spliterator(), false),
                    stream(((Iterable<?>) r2).spliterator(), false)
            ).collect(toList());
        }
        return r1 == null ? r2 : r1;
    }

    public R visit(Tree tree) {
        return tree == null ? defaultTo(null) :
                reduce(tree.accept(this), visitTree(tree));
    }

    public R visit(@Nullable Iterable<? extends Tree> nodes) {
        R r = defaultTo(null);
        if (nodes != null) {
            for (Tree node : nodes) {
                if (node != null) {
                    r = reduce(r, visit(node));
                }
            }
        }
        return r;
    }

    private R visitAfter(R r, @Nullable Tree node) {
        return node == null ? r : reduce(r, visit(node));
    }

    private R visitAfter(R r, @Nullable Iterable<? extends Tree> nodes) {
        return reduce(r, visit(nodes));
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

    public R visitTree(Tree tree) {
        return defaultTo(tree);
    }

    public R visitExpression(Expression expr) {
        return defaultTo(expr);
    }

    public R visitTypeName(NameTree name) {
        return defaultTo(name);
    }

    public R visitEnd() {
        return defaultTo(null);
    }

    public R visitAnnotation(Tr.Annotation annotation) {
        return visitTypeNameAfter(
                visitAfter(
                        visit(annotation.getAnnotationType()),
                        annotation.getArgs() == null ? null : annotation.getArgs().getArgs()
                ),
                annotation.getAnnotationType()
        );
    }

    public R visitArrayAccess(Tr.ArrayAccess arrayAccess) {
        return visitAfter(visit(arrayAccess.getIndexed()), arrayAccess.getDimension().getIndex());
    }

    public R visitArrayType(Tr.ArrayType arrayType) {
        return visitTypeNameAfter(visit(arrayType.getElementType()), arrayType.getElementType());
    }

    public R visit(Tr.Assert azzert) {
        return visit(azzert.getCondition());
    }

    public R visitAssert(Tr.Assert azzert) {
        return visit(azzert.getCondition());
    }

    public R visitAssign(Tr.Assign assign) {
        return visitAfter(visit(assign.getVariable()), assign.getAssignment());
    }

    public R visitAssignOp(Tr.AssignOp assignOp) {
        return visitAfter(visit(assignOp.getVariable()), assignOp.getAssignment());
    }

    public R visitBinary(Tr.Binary binary) {
        return visitAfter(visit(binary.getLeft()), binary.getRight());
    }

    public R visitBlock(Tr.Block<Tree> block) {
        return visit(block.getStatements());
    }

    public R visitBreak(Tr.Break breakStatement) {
        return visit(breakStatement.getLabel());
    }

    public R visitCase(Tr.Case caze) {
        return visitAfter(visit(caze.getPattern()), caze.getStatements());
    }

    public R visitCatch(Tr.Catch catzh) {
        return visitAfter(visit(catzh.getParam()), catzh.getBody());
    }

    public R visitClassDecl(Tr.ClassDecl classDecl) {
        return visitTypeNamesAfter(
                visitTypeNameAfter(
                        visitAfter(
                                visitAfter(
                                        visitAfter(
                                                visitAfter(
                                                        visitAfter(
                                                                visitAfter(
                                                                        visit(classDecl.getAnnotations()),
                                                                        classDecl.getModifiers()
                                                                ),
                                                                classDecl.getName()
                                                        ),
                                                        classDecl.getTypeParams() == null ? null : classDecl.getTypeParams().getParams()
                                                ),
                                                classDecl.getExtends()
                                        ),
                                        classDecl.getImplements()
                                ),
                                classDecl.getBody()
                        ),
                        classDecl.getExtends()
                ),
                classDecl.getImplements()
        );
    }

    public R visitCompilationUnit(Tr.CompilationUnit cu) {
        return reduce(
                visitAfter(
                        visitAfter(
                                visit(cu.getImports()),
                                cu.getPackageDecl()
                        ),
                        cu.getClasses()
                ),
                visitEnd()
        );
    }

    public R visitContinue(Tr.Continue continueStatement) {
        return visit(continueStatement.getLabel());
    }

    public R visitDoWhileLoop(Tr.DoWhileLoop doWhileLoop) {
        return visitAfter(visit(doWhileLoop.getCondition()), doWhileLoop.getBody());
    }

    public R visitEmpty(Tr.Empty empty) {
        return defaultTo(empty);
    }

    public R visitEnumValue(Tr.EnumValue enoom) {
        return visitAfter(
                visit(enoom.getName()),
                enoom.getInitializer() == null ? null : enoom.getInitializer().getArgs()
        );
    }

    public R visitEnumValueSet(Tr.EnumValueSet enums) {
        return visit(enums.getEnums());
    }

    public R visitFieldAccess(Tr.FieldAccess fieldAccess) {
        return visitTypeNameAfter(
                visitAfter(
                        visit(fieldAccess.getTarget()),
                        fieldAccess.getName()
                ),
                fieldAccess.asClassReference()
        );
    }

    public R visitForLoop(Tr.ForLoop forLoop) {
        return visitAfter(
                visitAfter(
                        visitAfter(
                                visit(forLoop.getControl().getInit()),
                                forLoop.getControl().getCondition()
                        ),
                        forLoop.getControl().getUpdate()
                ),
                forLoop.getBody()
        );
    }

    public R visitForEachLoop(Tr.ForEachLoop forEachLoop) {
        return visitAfter(
                visitAfter(
                        visit(forEachLoop.getControl().getVariable()),
                        forEachLoop.getControl().getIterable()
                ),
                forEachLoop.getBody()
        );
    }

    public R visitIdentifier(Tr.Ident ident) {
        return defaultTo(ident);
    }

    public R visitIf(Tr.If iff) {
        return visitAfter(
                visitAfter(
                        visit(iff.getIfCondition()),
                        iff.getThenPart()

                ),
                iff.getElsePart() == null ? null : iff.getElsePart().getStatement()
        );
    }

    public R visitImport(Tr.Import impoort) {
        return visit(impoort.getQualid());
    }

    public R visitInstanceOf(Tr.InstanceOf instanceOf) {
        return visitAfter(visit(instanceOf.getExpr()), instanceOf.getClazz());
    }

    public R visitLabel(Tr.Label label) {
        return visitAfter(visit(label.getLabel()), label.getStatement());
    }

    public R visitLambda(Tr.Lambda lambda) {
        return visitAfter(visit(lambda.getParamSet().getParams()), lambda.getBody());
    }

    public R visitLiteral(Tr.Literal literal) {
        return defaultTo(literal);
    }

    public R visitMemberReference(Tr.MemberReference memberRef) {
        return visitAfter(visit(memberRef.getContaining()), memberRef.getReference());
    }

    public R visitMethod(Tr.MethodDecl method) {
        return visitTypeNamesAfter(
                visitTypeNameAfter(
                        visitAfter(
                                visitAfter(
                                        visitAfter(
                                                visitAfter(
                                                        visitAfter(
                                                                visitAfter(
                                                                        visitAfter(
                                                                                visitAfter(
                                                                                        visit(method.getAnnotations()),
                                                                                        method.getModifiers()
                                                                                ),
                                                                                method.getTypeParameters() == null ? null : method.getTypeParameters().getParams()
                                                                        ),
                                                                        method.getReturnTypeExpr()
                                                                ),
                                                                method.getName()
                                                        ),
                                                        method.getParams().getParams()
                                                ),
                                                method.getThrows() == null ? null : method.getThrows().getExceptions()
                                        ),
                                        method.getBody()
                                ),
                                method.getDefaultValue()
                        ),
                        method.getReturnTypeExpr()
                ),
                method.getThrows() == null ? null : method.getThrows().getExceptions()
        );
    }

    public R visitMethodInvocation(Tr.MethodInvocation method) {
        R selectTypeVisit = (method.getSelect() instanceof NameTree && (method.getType() != null && method.getType().hasFlags(Flag.Static))) ?
                visitTypeName((NameTree) method.getSelect()) : defaultTo(method);

        return reduce(
                visitAfter(
                        visitAfter(
                                visitAfter(
                                        visit(method.getSelect()),
                                        method.getName()
                                ),
                                method.getArgs().getArgs()
                        ),
                        method.getTypeParameters()
                ),
                selectTypeVisit
        );
    }

    public R visitMultiCatch(Tr.MultiCatch multiCatch) {
        return visitTypeNamesAfter(
                visit(multiCatch.getAlternatives()),
                multiCatch.getAlternatives()
        );
    }

    public R visitMultiVariable(Tr.VariableDecls multiVariable) {
        R varTypeVisit = multiVariable.getTypeExpr() instanceof Tr.MultiCatch ?
                defaultTo(multiVariable) :
                multiVariable.getTypeExpr() == null ?
                        defaultTo(multiVariable) :
                        visitTypeName(multiVariable.getTypeExpr());

        return reduce(
                visitAfter(
                        visitAfter(
                                visitAfter(
                                        visit(multiVariable.getAnnotations()),
                                        multiVariable.getModifiers()
                                ),
                                multiVariable.getTypeExpr()
                        ),
                        multiVariable.getVars()
                ),
                varTypeVisit
        );
    }

    public R visitNewArray(Tr.NewArray newArray) {
        return visitTypeNameAfter(
                visitAfter(
                        visitAfter(
                                visit(newArray.getTypeExpr()),
                                newArray.getDimensions().stream().map(Tr.NewArray.Dimension::getSize).collect(toList())
                        ),
                        newArray.getInitializer() == null ? null : newArray.getInitializer().getElements()
                ),
                newArray.getTypeExpr()
        );
    }

    public R visitNewClass(Tr.NewClass newClass) {
        return visitTypeNameAfter(
                visitAfter(
                        visitAfter(
                                visit(newClass.getClazz()),
                                newClass.getArgs().getArgs()
                        ),
                        newClass.getBody()
                ),
                newClass.getClazz()
        );
    }

    public R visitPackage(Tr.Package pkg) {
        return visit(pkg.getExpr());
    }

    public R visitParameterizedType(Tr.ParameterizedType type) {
        return visitTypeNamesAfter(
                visitTypeNameAfter(
                        visitAfter(
                                visit(type.getClazz()),
                                type.getTypeArguments() == null ? null : type.getTypeArguments().getArgs()
                        ),
                        type.getClazz()
                ),
                type.getTypeArguments() == null ? null : type.getTypeArguments().getArgs().stream()
                        .filter(NameTree.class::isInstance)
                        .map(NameTree.class::cast)
                        .collect(Collectors.toList())
        );
    }

    public <T extends Tree> R visitParentheses(Tr.Parentheses<T> parens) {
        return visit(parens.getTree());
    }

    public R visitPrimitive(Tr.Primitive primitive) {
        return defaultTo(primitive);
    }

    public R visitReturn(Tr.Return retrn) {
        return visit(retrn.getExpr());
    }

    public R visitSwitch(Tr.Switch switzh) {
        return visitAfter(visit(switzh.getSelector()), switzh.getCases());
    }

    public R visitSynchronized(Tr.Synchronized synch) {
        return visitAfter(visit(synch.getLock()), synch.getBody());
    }

    public R visitTernary(Tr.Ternary ternary) {
        return visitAfter(
                visitAfter(
                        visit(ternary.getCondition()),
                        ternary.getTruePart()
                ),
                ternary.getFalsePart()
        );
    }

    public R visitThrow(Tr.Throw thrown) {
        return visit(thrown.getException());
    }

    public R visitTry(Tr.Try tryable) {
        return visitAfter(
                visitAfter(
                        visitAfter(
                                visit(tryable.getResources() == null ? null : tryable.getResources().getDecls()),
                                tryable.getBody()
                        ),
                        tryable.getCatches()
                ),
                tryable.getFinally() == null ? null : tryable.getFinally().getBlock()
        );
    }

    public R visitTypeCast(Tr.TypeCast typeCast) {
        return visitTypeNameAfter(
                visitAfter(
                        visit(typeCast.getClazz()),
                        typeCast.getExpr()
                ),
                typeCast.getClazz().getTree()
        );
    }

    public R visitTypeParameter(Tr.TypeParameter typeParam) {
        return visitTypeNameAfter(
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
                typeParam.getName()
        );
    }

    public R visitTypeParameters(Tr.TypeParameters typeParams) {
        return visit(typeParams.getParams());
    }

    public R visitUnary(Tr.Unary unary) {
        return visit(unary.getExpr());
    }

    public R visitUnparsedSource(Tr.UnparsedSource unparsed) {
        return defaultTo(unparsed);
    }

    public R visitVariable(Tr.VariableDecls.NamedVar variable) {
        return visitAfter(
                visitAfter(
                        visit(variable.getName()),
                        variable.getDimensionsAfterName()
                ),
                variable.getInitializer()
        );
    }

    public R visitWhileLoop(Tr.WhileLoop whileLoop) {
        return visitAfter(visit(whileLoop.getCondition()), whileLoop.getBody());
    }

    public R visitWildcard(Tr.Wildcard wildcard) {
        return visitTypeNameAfter(
                visitAfter(
                        visit(wildcard.getBound()),
                        wildcard.getBoundedType()
                ),
                wildcard.getBoundedType()
        );
    }
}
