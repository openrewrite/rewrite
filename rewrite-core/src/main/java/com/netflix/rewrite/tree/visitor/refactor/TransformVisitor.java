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
package com.netflix.rewrite.tree.visitor.refactor;

import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Tr.*;
import com.netflix.rewrite.tree.Tree;
import com.netflix.rewrite.tree.visitor.CursorAstVisitor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

@RequiredArgsConstructor
public class TransformVisitor extends CursorAstVisitor<Tree> {
    private static final Logger logger = LoggerFactory.getLogger(TransformVisitor.class);

    private final Iterable<AstTransform> transformations;

    @Override
    public Tree defaultTo(Tree t) {
        return t;
    }

    @Override
    public Tree visitAnnotation(Annotation annotation) {
        return transform(annotation,
                t(Annotation::getArgs, Annotation::withArgs),
                t(Annotation::getArgs, Annotation::withArgs, Annotation.Arguments::getArgs, Annotation.Arguments::withArgs));
    }

    @Override
    public Tree visitArrayAccess(ArrayAccess arrayAccess) {
        return transform(arrayAccess,
                t(ArrayAccess::getIndexed, ArrayAccess::withIndexed),
                t(ArrayAccess::getDimension, ArrayAccess::withDimension, ArrayAccess.Dimension::getIndex, ArrayAccess.Dimension::withIndex));
    }

    @Override
    public Tree visitArrayType(ArrayType arrayType) {
        return transform(arrayType, t(ArrayType::getElementType, ArrayType::withElementType));
    }

    @Override
    public Tree visitAssert(Assert azzert) {
        return transform(azzert, t(Assert::getCondition, Assert::withCondition));
    }

    @Override
    public Tree visitAssign(Assign assign) {
        return transform(assign,
                t(Assign::getVariable, Assign::withVariable),
                t(Assign::getAssignment, Assign::withAssignment));
    }

    @Override
    public Tree visitAssignOp(AssignOp assignOp) {
        return transform(assignOp,
                t(AssignOp::getVariable, AssignOp::withVariable),
                t(AssignOp::getAssignment, AssignOp::withAssignment));
    }

    @Override
    public Tree visitBinary(Binary binary) {
        return transform(binary,
                t(Binary::getLeft, Binary::withLeft),
                t(Binary::getRight, Binary::withRight));
    }

    @Override
    public Tree visitBlock(Tr.Block<Tree> block) {
        return transform(block, t(Block::getStatements, Block::withStatements));
    }

    @Override
    public Tree visitCase(Case caze) {
        return transform(caze,
                t(Case::getPattern, Case::withPattern),
                t(Case::getStatements, Case::withStatements));
    }

    @Override
    public Tree visitCatch(Try.Catch catzh) {
        return transform(catzh,
                t(Try.Catch::getParam, Try.Catch::withParam),
                t(Try.Catch::getBody, Try.Catch::withBody));
    }

    @Override
    public Tree visitClassDecl(ClassDecl classDecl) {
        return transform(classDecl,
                t(ClassDecl::getAnnotations, ClassDecl::withAnnotations),
                t(ClassDecl::getName, ClassDecl::withName),
                t(ClassDecl::getExtends, ClassDecl::withExtendings),
                t(ClassDecl::getImplements, ClassDecl::withImplementings),
                t(ClassDecl::getTypeParams, ClassDecl::withTypeParams),
                t(ClassDecl::getBody, ClassDecl::withBody));
    }

    @Override
    public Tree visitCompilationUnit(CompilationUnit cu) {
        return transform(cu,
                t(CompilationUnit::getPackageDecl, CompilationUnit::withPackageDecl),
                t(CompilationUnit::getImports, CompilationUnit::withImports),
                t(CompilationUnit::getClasses, CompilationUnit::withClasses));
    }

    @Override
    public Tree visitContinue(Continue continueStatement) {
        return transform(continueStatement);
    }

    @Override
    public Tree visitDoWhileLoop(DoWhileLoop doWhileLoop) {
        return transform(doWhileLoop,
                t(DoWhileLoop::getWhileCondition, DoWhileLoop::withWhileCondition, DoWhileLoop.While::getCondition, DoWhileLoop.While::withCondition),
                t(DoWhileLoop::getBody, DoWhileLoop::withBody));
    }

    @Override
    public Tree visitEmpty(Empty empty) {
        return transform(empty);
    }

    @Override
    public Tree visitEnumValue(EnumValue enoom) {
        return transform(enoom,
                t(EnumValue::getName, EnumValue::withName),
                t(EnumValue::getInitializer, EnumValue::withInitializer, EnumValue.Arguments::getArgs, EnumValue.Arguments::withArgs));
    }

    @Override
    public Tree visitEnumValueSet(EnumValueSet enums) {
        return transform(enums, t(EnumValueSet::getEnums, EnumValueSet::withEnums));
    }

    @Override
    public Tree visitFieldAccess(FieldAccess fieldAccess) {
        return transform(fieldAccess, t(FieldAccess::getTarget, FieldAccess::withTarget));
    }

    @Override
    public Tree visitFinally(Try.Finally finallie) {
        return transform(finallie, t(Try.Finally::getBody, Try.Finally::withBody));
    }

    @Override
    public Tree visitForLoop(ForLoop forLoop) {
        return transform(forLoop,
                t(ForLoop::getControl, ForLoop::withControl, ForLoop.Control::getInit, ForLoop.Control::withInit),
                t(ForLoop::getControl, ForLoop::withControl, ForLoop.Control::getCondition, ForLoop.Control::withCondition),
                t(ForLoop::getControl, ForLoop::withControl, ForLoop.Control::getUpdate, ForLoop.Control::withUpdate),
                t(ForLoop::getBody, ForLoop::withBody));
    }

    @Override
    public Tree visitForEachLoop(ForEachLoop forLoop) {
        return transform(forLoop,
                t(ForEachLoop::getControl, ForEachLoop::withControl, ForEachLoop.Control::getVariable, ForEachLoop.Control::withVariable),
                t(ForEachLoop::getControl, ForEachLoop::withControl, ForEachLoop.Control::getIterable, ForEachLoop.Control::withIterable),
                t(ForEachLoop::getBody, ForEachLoop::withBody));
    }

    @Override
    public Tree visitIdentifier(Ident ident) {
        return transform(ident);
    }

    @Override
    public Tree visitIf(If iff) {
        return transform(iff,
                t(If::getIfCondition, If::withIfCondition),
                t(If::getThenPart, If::withThenPart),
                t(If::getElsePart, If::withElsePart));
    }

    @Override
    public Tree visitElse(If.Else elze) {
        return transform(elze, t(If.Else::getStatement, If.Else::withStatement));
    }

    @Override
    public Tree visitImport(Import impoort) {
        return transform(impoort, t(Import::getQualid, Import::withQualid));
    }

    @Override
    public Tree visitInstanceOf(InstanceOf instanceOf) {
        return transform(instanceOf,
                t(InstanceOf::getExpr, InstanceOf::withExpr),
                t(InstanceOf::getClazz, InstanceOf::withClazz));
    }

    @Override
    public Tree visitLabel(Label label) {
        return transform(label, t(Label::getStatement, Label::withStatement));
    }

    @Override
    public Tree visitLambda(Lambda lambda) {
        return transform(lambda,
                t(Lambda::getParamSet, Lambda::withParamSet, Lambda.Parameters::getParams, Lambda.Parameters::withParams),
                t(Lambda::getBody, Lambda::withBody));
    }

    @Override
    public Tree visitLiteral(Literal literal) {
        return transform(literal);
    }

    @Override
    public Tree visitMemberReference(MemberReference memberRef) {
        return transform(memberRef,
                t(MemberReference::getContaining, MemberReference::withContaining),
                t(MemberReference::getTypeParameters, MemberReference::withTypeParameters),
                t(MemberReference::getReference, MemberReference::withReference));
    }

    @Override
    public Tree visitMethod(MethodDecl method) {
        return transform(method,
                t(MethodDecl::getReturnTypeExpr, MethodDecl::withReturnTypeExpr),
                t(MethodDecl::getParams, MethodDecl::withParams, MethodDecl.Parameters::getParams, MethodDecl.Parameters::withParams),
                t(MethodDecl::getThrows, MethodDecl::withThrowz, MethodDecl.Throws::getExceptions, MethodDecl.Throws::withExceptions),
                t(MethodDecl::getDefaultValue, MethodDecl::withDefaultValue, MethodDecl.Default::getValue, MethodDecl.Default::withValue),
                t(MethodDecl::getTypeParameters, MethodDecl::withTypeParameters),
                t(MethodDecl::getBody, MethodDecl::withBody));
    }

    @Override
    public Tree visitMethodInvocation(MethodInvocation method) {
        return transform(method,
                t(MethodInvocation::getSelect, MethodInvocation::withSelect),
                t(MethodInvocation::getArgs, MethodInvocation::withArgs, MethodInvocation.Arguments::getArgs, MethodInvocation.Arguments::withArgs),
                t(MethodInvocation::getTypeParameters, MethodInvocation::withTypeParameters));
    }

    @Override
    public Tree visitMultiCatch(MultiCatch multiCatch) {
        return transform(multiCatch, t(MultiCatch::getAlternatives, MultiCatch::withAlternatives));
    }

    @Override
    public Tree visitMultiVariable(VariableDecls multiVariable) {
        return transform(multiVariable,
                t(VariableDecls::getTypeExpr, VariableDecls::withTypeExpr),
                t(VariableDecls::getVars, VariableDecls::withVars));
    }

    @Override
    public Tree visitNewArray(NewArray newArray) {
        return transform(newArray,
                t(NewArray::getTypeExpr, NewArray::withTypeExpr),
                t(NewArray::getDimensions, NewArray::withDimensions),
                t(NewArray::getInitializer, NewArray::withInitializer));
    }

    @Override
    public Tree visitNewClass(NewClass newClass) {
        return transform(newClass,
                t(NewClass::getClazz, NewClass::withClazz),
                t(NewClass::getArgs, NewClass::withArgs, NewClass.Arguments::getArgs, NewClass.Arguments::withArgs),
                t(NewClass::getBody, NewClass::withBody));
    }

    @Override
    public Tree visitPackage(Tr.Package pkg) {
        return transform(pkg, t(Tr.Package::getExpr, Tr.Package::withExpr));
    }

    @Override
    public Tree visitParameterizedType(ParameterizedType type) {
        return transform(type,
                t(ParameterizedType::getClazz, ParameterizedType::withClazz),
                t(ParameterizedType::getTypeParameters, ParameterizedType::withTypeParameters));
    }

    @Override
    public <T extends Tree> Tree visitParentheses(Parentheses<T> parens) {
        return transform(parens, t(Parentheses::getTree, Parentheses::withTree));
    }

    @Override
    public Tree visitPrimitive(Primitive primitive) {
        return transform(primitive);
    }

    @Override
    public Tree visitReturn(Return retrn) {
        return transform(retrn, t(Return::getExpr, Return::withExpr));
    }

    @Override
    public Tree visitSwitch(Switch switzh) {
        return transform(switzh,
                t(Switch::getSelector, Switch::withSelector),
                t(Switch::getCases, Switch::withCases));
    }

    @Override
    public Tree visitSynchronized(Synchronized synch) {
        return transform(synch,
                t(Synchronized::getLock, Synchronized::withLock),
                t(Synchronized::getBody, Synchronized::withBody));
    }

    @Override
    public Tree visitTernary(Ternary ternary) {
        return transform(ternary,
                t(Ternary::getCondition, Ternary::withCondition),
                t(Ternary::getTruePart, Ternary::withTruePart),
                t(Ternary::getFalsePart, Ternary::withFalsePart));
    }

    @Override
    public Tree visitThrow(Throw thrown) {
        return transform(thrown, t(Throw::getException, Throw::withException));
    }

    @Override
    public Tree visitTry(Try tryable) {
        return transform(tryable,
                t(Try::getResources, Try::withResources, Try.Resources::getDecls, Try.Resources::withDecls),
                t(Try::getBody, Try::withBody),
                t(Try::getCatches, Try::withCatches),
                t(Try::getFinally, Try::withFinallie));
    }

    @Override
    public Tree visitTypeCast(TypeCast typeCast) {
        return transform(typeCast,
                t(TypeCast::getClazz, TypeCast::withClazz),
                t(TypeCast::getExpr, TypeCast::withExpr));
    }

    @Override
    public Tree visitTypeParameter(TypeParameter typeParam) {
        return transform(typeParam,
                t(TypeParameter::getAnnotations, TypeParameter::withAnnotations),
                t(TypeParameter::getName, TypeParameter::withName),
                t(TypeParameter::getBounds, TypeParameter::withBounds, TypeParameter.Bounds::getTypes, TypeParameter.Bounds::withTypes));
    }

    @Override
    public Tree visitTypeParameters(TypeParameters typeParams) {
        return transform(typeParams, t(TypeParameters::getParams, TypeParameters::withParams));
    }

    @Override
    public Tree visitUnary(Unary unary) {
        return transform(unary, t(Unary::getExpr, Unary::withExpr));
    }

    @Override
    public Tree visitUnparsedSource(UnparsedSource unparsed) {
        return transform(unparsed);
    }

    @Override
    public Tree visitVariable(VariableDecls.NamedVar variable) {
        return transform(variable,
                t(VariableDecls.NamedVar::getName, VariableDecls.NamedVar::withName),
                t(VariableDecls.NamedVar::getInitializer, VariableDecls.NamedVar::withInitializer));
    }

    @Override
    public Tree visitWhileLoop(WhileLoop whileLoop) {
        return transform(whileLoop,
                t(WhileLoop::getCondition, WhileLoop::withCondition),
                t(WhileLoop::getBody, WhileLoop::withBody));
    }

    @Override
    public Tree visitWildcard(Wildcard wildcard) {
        return transform(wildcard, t(Wildcard::getBoundedType, Wildcard::withBoundedType));
    }

    @Data
    private static class Transformable<T extends Tree, U extends T, F> {
        private final Function<U, F> getter;
        private final BiFunction<U, F, T> with;
    }

    /**
     * One-level-deep field transformation
     */
    private static <T extends Tree, U extends T, F> Transformable<T, U, F> t(Function<U, F> getter,
                                                             BiFunction<U, F, T> with) {
        return new Transformable<>(getter, with);
    }

    /**
     * Two-level-deep field transformation
     */
    private static <T extends Tree, U extends T, F1, F2> Transformable<T, U, F2> t(Function<U, F1> getter1,
                                                                   BiFunction<U, F1, T> with1,
                                                                   Function<F1, F2> getter2,
                                                                   BiFunction<F1, F2, F1> with2) {
        return t(
                t -> {
                    F1 f1 = getter1.apply(t);
                    return f1 == null ? null : getter2.apply(f1);
                },
                (t, f2) -> with1.apply(t, with2.apply(getter1.apply(t), f2)));
    }

    /**
     * Callers to this method specify how to recursively transform a tree.
     *
     * @param original The tree to be transformed
     * @param checks   Defines the nested elements that we should attempt transformations on
     * @param <T>      The type of tree to be transformed
     * @return The mutated tree, or the original tree instance if no mutations occurred.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @SafeVarargs
    private <T extends Tree, U extends T> Tree transform(U original, Transformable<T, U, ?>... checks) {
        Tree maybeMutated = original;
        for (Transformable check : checks) {
            Object field = check.getter.apply(maybeMutated);
            if (field == null) {
                continue;
            }
            Object maybeMutatedField = field instanceof Tree ?
                    visit((Tree) field) :
                    visitList((List) field);
            if (field != maybeMutatedField) {
                maybeMutated = (Tree) check.with.apply(maybeMutated, maybeMutatedField);
            }
        }
        return transformShallow(maybeMutated);
    }

    /**
     * Apply transformations at just this level of the tree. It is the responsibility of each visitXX method to determine
     * which nested levels we should attempt transformations on as well.
     */
    private Tree transformShallow(Tree tree) {
        List<AstTransform> filteredTransforms = stream(transformations.spliterator(), false)
                .filter(t -> t.getId().equals(getCursor().getTree().getId()))
                .filter(t -> t.getTreeType().isInstance(tree))
                .collect(toList());

        Tree mutation = tree;
        for (AstTransform trans : filteredTransforms) {
            if (logger.isDebugEnabled()) {
                logger.debug("Transforming " + mutation.getClass().getSimpleName() + " with " + trans.getName());
                logger.debug("Original: ");
                logger.debug(mutation.printTrimmed() + "\n");
            }

            mutation = trans.getMutation().apply(mutation, getCursor());

            if (logger.isDebugEnabled()) {
                logger.debug("Transformed: ");
                logger.debug(mutation.print() + "\n");
            }
        }

        return mutation;
    }

    @SuppressWarnings("unchecked")
    private <T extends Tree> List<T> visitList(List<T> ts) {
        AtomicBoolean changed = new AtomicBoolean(false);
        List<T> mapped = ts.stream()
                .map(t -> {
                    T mappedElem = (T) visit(t);
                    if (t != mappedElem) {
                        changed.set(true);
                    }
                    return mappedElem;
                })
                .collect(toList());
        return changed.get() ? mapped : ts;
    }
}
