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
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.format.AutoFormatVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class JavaVisitor<P> extends TreeVisitor<J, P> {

    @Incubating(since = "7.0.0")
    public <J2 extends J> J2 generate(JavaTemplate template, JavaCoordinates coordinates, Object... parameters) {
        //TODO Not Implemented.
        return null;
    }

    /**
     * This overload can be used to generate a template's contents as the last statement in a block.
     *
     * @param template   A template instance
     * @param parameters Template parameters
     * @param <J2>       Expected type returned from the template.
     * @return A list of generated elements
     * @deprecated This method is deprecated and will be removed in 7.0.
     */
    @Deprecated
    public <J2 extends J> List<J2> generateLastInBlock(JavaTemplate template, Cursor blockCursor, Object... parameters) {
        if (blockCursor.getValue() instanceof J.Block) {
            return template.generate(new Cursor(blockCursor, ((J.Block) blockCursor.getValue()).getEnd()), parameters);
        } else {
            throw new IllegalArgumentException("The cursor must reference a J.Block");
        }
    }

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

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public <J2 extends J> J2 maybeAutoFormat(J2 before, J2 after, P p) {
        if(before != after) {
            return (J2) new AutoFormatVisitor<>().visit(after, p, getCursor());
        }
        return after;
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

    @SuppressWarnings("unused")
    public Space visitSpace(Space space, Space.Location loc, P p) {
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
        @SuppressWarnings("unchecked") List<JRightPadded<J2>> js = ListUtils.map(nameTrees.getPadding().getElems(),
                t -> t.getElem() instanceof NameTree ? (JRightPadded<J2>) visitTypeName((JRightPadded<NameTree>) t, p) : t);
        return js == nameTrees.getPadding().getElems() ? nameTrees : JContainer.build(nameTrees.getBefore(), js, Markers.EMPTY);
    }

    public J visitAnnotatedType(J.AnnotatedType annotatedType, P p) {
        J.AnnotatedType a = visitAndCast(annotatedType, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ANNOTATED_TYPE_PREFIX, p));
        a = visitAndCast(a, p, this::visitExpression);
        a = a.withAnnotations(ListUtils.map(a.getAnnotations(), e -> visitAndCast(e, p)));
        a = a.withTypeExpr(visitAndCast(a.getTypeExpr(), p));
        return a.withTypeExpr(visitTypeName(a.getTypeExpr(), p));
    }

    public J visitAnnotation(J.Annotation annotation, P p) {
        J.Annotation a = visitAndCast(annotation, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ANNOTATION_PREFIX, p));
        a = visitAndCast(a, p, this::visitExpression);
        if (a.getPadding().getArgs() != null) {
            a = a.getPadding().withArgs(visitContainer(a.getPadding().getArgs(), JContainer.Location.ANNOTATION_ARGUMENTS, p));
        }
        a = a.withAnnotationType(visitAndCast(a.getAnnotationType(), p));
        return a.withAnnotationType(visitTypeName(a.getAnnotationType(), p));
    }

    public J visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
        J.ArrayAccess a = visitAndCast(arrayAccess, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ARRAY_ACCESS_PREFIX, p));
        a = visitAndCast(a, p, this::visitExpression);
        a = a.withIndexed(visitAndCast(a.getIndexed(), p));
        a = a.withDimension(visitAndCast(a.getDimension(), p));
        return a;
    }

    public J visitArrayDimension(J.ArrayDimension arrayDimension, P p) {
        J.ArrayDimension a = visitAndCast(arrayDimension, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.DIMENSION_PREFIX, p));
        a = a.getPadding().withIndex(visitRightPadded(a.getPadding().getIndex(), JRightPadded.Location.ARRAY_INDEX, p));
        return a;
    }

    public J visitArrayType(J.ArrayType arrayType, P p) {
        J.ArrayType a = visitAndCast(arrayType, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ARRAY_TYPE_PREFIX, p));
        a = visitAndCast(a, p, this::visitExpression);
        a = a.withElementType(visitAndCast(a.getElementType(), p));
        a = a.withElementType(visitTypeName(a.getElementType(), p));
        a = a.withDimensions(
                ListUtils.map(a.getDimensions(), dim ->
                        visitRightPadded(dim.withElem(
                                visitSpace(dim.getElem(), Space.Location.DIMENSION, p)
                        ), JRightPadded.Location.DIMENSION, p)
                )
        );
        return a;
    }

    public J visitAssert(J.Assert azzert, P p) {
        J.Assert a = visitAndCast(azzert, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ASSERT_PREFIX, p));
        a = visitAndCast(a, p, this::visitStatement);
        return a.withCondition(visitAndCast(a.getCondition(), p));
    }

    public J visitAssign(J.Assign assign, P p) {
        J.Assign a = visitAndCast(assign, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ASSIGN_PREFIX, p));
        a = visitAndCast(a, p, this::visitStatement);
        a = visitAndCast(a, p, this::visitExpression);
        a = a.withVariable(visitAndCast(a.getVariable(), p));
        return a.getPadding().withAssignment(visitLeftPadded(a.getPadding().getAssignment(),
                JLeftPadded.Location.ASSIGNMENT, p));
    }

    public J visitAssignOp(J.AssignOp assignOp, P p) {
        J.AssignOp a = visitAndCast(assignOp, p, this::visitEach);
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ASSIGN_OP_PREFIX, p));
        a = visitAndCast(a, p, this::visitStatement);
        a = visitAndCast(a, p, this::visitExpression);
        a = a.withVariable(visitAndCast(a.getVariable(), p));
        a = a.getPadding().withOperator(visitLeftPadded(a.getPadding().getOperator(), JLeftPadded.Location.ASSIGN_OP_OPERATOR, p));
        return a.withAssignment(visitAndCast(a.getAssignment(), p));
    }

    public J visitBinary(J.Binary binary, P p) {
        J.Binary b = visitAndCast(binary, p, this::visitEach);
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BINARY_PREFIX, p));
        b = visitAndCast(b, p, this::visitExpression);
        b = b.withLeft(visitAndCast(b.getLeft(), p));
        b = b.getPadding().withOperator(visitLeftPadded(b.getPadding().getOperator(), JLeftPadded.Location.BINARY_OPERATOR, p));
        return b.withRight(visitAndCast(b.getRight(), p));
    }

    public J visitBlock(J.Block block, P p) {
        J.Block b = visitAndCast(block, p, this::visitEach);
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BLOCK_PREFIX, p));
        b = b.getPadding().withStatic(visitRightPadded(b.getPadding().getStatic(), JRightPadded.Location.STATIC_INIT, p));
        b = visitAndCast(b, p, this::visitStatement);
        b = b.getPadding().withStatements(ListUtils.map(b.getPadding().getStatements(), t ->
                visitRightPadded(t, JRightPadded.Location.BLOCK_STATEMENT, p)));
        return b.withEnd(visitSpace(b.getEnd(), Space.Location.BLOCK_END, p));
    }

    public J visitBreak(J.Break breakStatement, P p) {
        J.Break b = visitAndCast(breakStatement, p, this::visitEach);
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BREAK_PREFIX, p));
        b = visitAndCast(b, p, this::visitStatement);
        return b.withLabel(visitAndCast(b.getLabel(), p));
    }

    public J visitCase(J.Case caze, P p) {
        J.Case c = visitAndCast(caze, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CASE_PREFIX, p));
        c = visitAndCast(c, p, this::visitStatement);
        c = c.withPattern(visitAndCast(c.getPattern(), p));
        return c.getPadding().withStatements(visitContainer(c.getPadding().getStatements(), JContainer.Location.CASE, p));
    }

    public J visitCatch(J.Try.Catch catzh, P p) {
        J.Try.Catch c = visitAndCast(catzh, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CATCH_PREFIX, p));
        c = c.withParam(visitAndCast(c.getParam(), p));
        return c.withBody(visitAndCast(c.getBody(), p));
    }

    public J visitClassDecl(J.ClassDecl classDecl, P p) {
        J.ClassDecl c = visitAndCast(classDecl, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CLASS_DECL_PREFIX, p));
        c = visitAndCast(c, p, this::visitStatement);
        c = c.withAnnotations(ListUtils.map(c.getAnnotations(), a -> visitAndCast(a, p)));
        c = c.withModifiers(ListUtils.map(c.getModifiers(), m -> visitAndCast(m, p)));
        c = c.withModifiers(ListUtils.map(c.getModifiers(),
                mod -> mod.withPrefix(visitSpace(mod.getPrefix(), Space.Location.MODIFIER_PREFIX, p))));
        if (c.getPadding().getTypeParameters() != null) {
            c = c.getPadding().withTypeParameters(visitContainer(c.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p));
        }
        c = c.getPadding().withKind(visitLeftPadded(c.getPadding().getKind(), JLeftPadded.Location.CLASS_KIND, p));
        c = c.withName(visitAndCast(c.getName(), p));
        if (c.getPadding().getExtends() != null) {
            c = c.getPadding().withExtends(visitLeftPadded(c.getPadding().getExtends(), JLeftPadded.Location.EXTENDS, p));
        }
        c = c.getPadding().withExtends(visitTypeName(c.getPadding().getExtends(), p));
        if (c.getPadding().getImplements() != null) {
            c = c.getPadding().withImplements(visitContainer(c.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, p));
        }
        c = c.getPadding().withImplements(visitTypeNames(c.getPadding().getImplements(), p));
        return c.withBody(visitAndCast(c.getBody(), p));
    }

    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        J.CompilationUnit c = visitAndCast(cu, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        if (c.getPadding().getPackageDecl() != null) {
            c = c.getPadding().withPackageDecl(visitRightPadded(c.getPadding().getPackageDecl(), JRightPadded.Location.PACKAGE, p));
        }
        c = c.getPadding().withImports(ListUtils.map(c.getPadding().getImports(), t -> visitRightPadded(t, JRightPadded.Location.IMPORT, p)));
        c = c.withClasses(ListUtils.map(c.getClasses(), e -> visitAndCast(e, p)));
        return c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
    }

    public J visitContinue(J.Continue continueStatement, P p) {
        J.Continue c = visitAndCast(continueStatement, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CONTINUE_PREFIX, p));
        c = visitAndCast(c, p, this::visitStatement);
        return c.withLabel(visitAndCast(c.getLabel(), p));
    }

    public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, P p) {
        J.ControlParentheses<T> cp = visitAndCast(controlParens, p, this::visitEach);
        cp = cp.withPrefix(visitSpace(cp.getPrefix(), Space.Location.CONTROL_PARENTHESES_PREFIX, p));
        cp = visitAndCast(cp, p, this::visitExpression);
        return cp.getPadding().withTree(visitRightPadded(cp.getPadding().getTree(), JRightPadded.Location.PARENTHESES, p));
    }

    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        J.DoWhileLoop d = visitAndCast(doWhileLoop, p, this::visitEach);
        d = d.withPrefix(visitSpace(d.getPrefix(), Space.Location.DO_WHILE_PREFIX, p));
        d = visitAndCast(d, p, this::visitStatement);
        d = d.getPadding().withWhileCondition(visitLeftPadded(d.getPadding().getWhileCondition(), JLeftPadded.Location.WHILE_CONDITION, p));
        return d.getPadding().withBody(visitRightPadded(d.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, p));
    }

    public J visitEmpty(J.Empty empty, P p) {
        J.Empty e = visitAndCast(empty, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.EMPTY_PREFIX, p));
        e = visitAndCast(e, p, this::visitStatement);
        e = visitAndCast(e, p, this::visitExpression);
        return e;
    }

    public J visitEnumValue(J.EnumValue enoom, P p) {
        J.EnumValue e = visitAndCast(enoom, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.ENUM_VALUE_PREFIX, p));
        e = e.withName(visitAndCast(e.getName(), p));
        return e.withInitializer(visitAndCast(e.getInitializer(), p));
    }

    public J visitEnumValueSet(J.EnumValueSet enums, P p) {
        J.EnumValueSet e = visitAndCast(enums, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.ENUM_VALUE_SET_PREFIX, p));
        e = visitAndCast(e, p, this::visitStatement);
        return e.getPadding().withEnums(ListUtils.map(e.getPadding().getEnums(), t -> visitRightPadded(t, JRightPadded.Location.ENUM_VALUE, p)));
    }

    public J visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        J.FieldAccess f = visitAndCast(fieldAccess, p, this::visitEach);
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FIELD_ACCESS_PREFIX, p));
        f = visitTypeName(f, p);
        f = visitAndCast(f, p, this::visitExpression);
        f = f.withTarget(visitAndCast(f.getTarget(), p));
        return f.getPadding().withName(visitLeftPadded(f.getPadding().getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p));
    }

    public J visitForEachLoop(J.ForEachLoop forLoop, P p) {
        J.ForEachLoop f = visitAndCast(forLoop, p, this::visitEach);
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FOR_EACH_LOOP_PREFIX, p));
        f = visitAndCast(f, p, this::visitStatement);
        f = f.withControl(visitAndCast(f.getControl(), p));
        return f.getPadding().withBody(visitRightPadded(f.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p));
    }

    public J visitForEachControl(J.ForEachLoop.Control control, P p) {
        J.ForEachLoop.Control c = visitAndCast(control, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.FOR_EACH_CONTROL_PREFIX, p));
        c = c.getPadding().withVariable(visitRightPadded(c.getPadding().getVariable(), JRightPadded.Location.FOREACH_VARIABLE, p));
        return c.getPadding().withIterable(visitRightPadded(c.getPadding().getIterable(), JRightPadded.Location.FOREACH_ITERABLE, p));
    }

    public J visitForLoop(J.ForLoop forLoop, P p) {
        J.ForLoop f = visitAndCast(forLoop, p, this::visitEach);
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FOR_PREFIX, p));
        f = visitAndCast(f, p, this::visitStatement);
        f = f.withControl(visitAndCast(f.getControl(), p));
        return f.getPadding().withBody(visitRightPadded(f.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p));
    }

    public J visitForControl(J.ForLoop.Control control, P p) {
        J.ForLoop.Control c = visitAndCast(control, p, this::visitEach);
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.FOR_CONTROL_PREFIX, p));
        c = c.getPadding().withInit(visitRightPadded(c.getPadding().getInit(), JRightPadded.Location.FOR_INIT, p));
        c = c.getPadding().withCondition(visitRightPadded(c.getPadding().getCondition(), JRightPadded.Location.FOR_CONDITION, p));
        return c.getPadding().withUpdate(ListUtils.map(c.getPadding().getUpdate(), t -> visitRightPadded(t, JRightPadded.Location.FOR_UPDATE, p)));
    }

    public J visitIdentifier(J.Ident ident, P p) {
        J.Ident i = visitAndCast(ident, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.IDENTIFIER_PREFIX, p));
        return visitAndCast(i, p, this::visitExpression);
    }

    public J visitElse(J.If.Else elze, P p) {
        J.If.Else e = visitAndCast(elze, p, this::visitEach);
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.ELSE_PREFIX, p));
        return e.getPadding().withBody(visitRightPadded(e.getPadding().getBody(), JRightPadded.Location.IF_ELSE, p));
    }

    public J visitIf(J.If iff, P p) {
        J.If i = visitAndCast(iff, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.IF_PREFIX, p));
        i = visitAndCast(i, p, this::visitStatement);
        i = i.withIfCondition(visitAndCast(i.getIfCondition(), p));
        i = i.getPadding().withThenPart(visitRightPadded(i.getPadding().getThenPart(), JRightPadded.Location.IF_THEN, p));
        i = i.withElsePart(visitAndCast(i.getElsePart(), p));
        return i;
    }

    public J visitImport(J.Import impoort, P p) {
        J.Import i = visitAndCast(impoort, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.IMPORT_PREFIX, p));
        i = i.withStatik(visitLeftPadded(i.getPadding().getStatic(), JLeftPadded.Location.STATIC_IMPORT, p));
        return i.withQualid(visitAndCast(i.getQualid(), p));
    }

    public J visitInstanceOf(J.InstanceOf instanceOf, P p) {
        J.InstanceOf i = visitAndCast(instanceOf, p, this::visitEach);
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.INSTANCEOF_PREFIX, p));
        i = visitAndCast(i, p, this::visitExpression);
        i = i.getPadding().withExpr(visitRightPadded(i.getPadding().getExpr(), JRightPadded.Location.INSTANCEOF, p));
        return i.withClazz(visitAndCast(i.getClazz(), p));
    }

    public J visitLabel(J.Label label, P p) {
        J.Label l = visitAndCast(label, p, this::visitEach);
        l = l.withPrefix(visitSpace(l.getPrefix(), Space.Location.LABEL_PREFIX, p));
        l = visitAndCast(l, p, this::visitStatement);
        l = l.getPadding().withLabel(visitRightPadded(l.getPadding().getLabel(), JRightPadded.Location.LABEL, p));
        return l.withStatement(visitAndCast(l.getStatement(), p));
    }

    public J visitLambda(J.Lambda lambda, P p) {
        J.Lambda l = visitAndCast(lambda, p, this::visitEach);
        l = l.withPrefix(visitSpace(l.getPrefix(), Space.Location.LAMBDA_PREFIX, p));
        l = visitAndCast(l, p, this::visitExpression);
        l = l.withParameters(
                l.getParameters().withPrefix(
                        visitSpace(l.getParameters().getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, p)
                )
        );
        l = l.withParameters(
                l.getParameters().getPadding().withParams(
                        ListUtils.map(l.getParameters().getPadding().getParams(),
                                param -> visitRightPadded(param, JRightPadded.Location.LAMBDA_PARAM, p)
                        )
                )
        );
        l = l.withParameters(visitAndCast(l.getParameters(), p));
        l = l.withArrow(visitSpace(l.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p));
        return l.withBody(visitAndCast(l.getBody(), p));
    }

    public J visitLiteral(J.Literal literal, P p) {
        J.Literal l = visitAndCast(literal, p, this::visitEach);
        l = l.withPrefix(visitSpace(l.getPrefix(), Space.Location.LITERAL_PREFIX, p));
        return visitAndCast(l, p, this::visitExpression);
    }

    public J visitMemberReference(J.MemberReference memberRef, P p) {
        J.MemberReference m = visitAndCast(memberRef, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.MEMBER_REFERENCE_PREFIX, p));
        m = m.withContaining(visitAndCast(m.getContaining(), p));
        if (m.getPadding().getTypeParameters() != null) {
            m = m.getPadding().withTypeParameters(visitContainer(m.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p));
        }
        return m.getPadding().withReference(visitLeftPadded(m.getPadding().getReference(), JLeftPadded.Location.MEMBER_REFERENCE_NAME, p));
    }

    public J visitMethod(J.MethodDecl method, P p) {
        J.MethodDecl m = visitAndCast(method, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.METHOD_DECL_PREFIX, p));
        m = visitAndCast(m, p, this::visitStatement);
        m = m.withAnnotations(ListUtils.map(m.getAnnotations(), a -> visitAndCast(a, p)));
        m = m.withModifiers(ListUtils.map(m.getModifiers(), e -> visitAndCast(e, p)));
        m = m.withModifiers(ListUtils.map(m.getModifiers(),
                mod -> mod.withPrefix(visitSpace(mod.getPrefix(), Space.Location.MODIFIER_PREFIX, p))));
        if (m.getPadding().getTypeParameters() != null) {
            m = m.getPadding().withTypeParameters(visitContainer(m.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p));
        }
        m = m.withReturnTypeExpr(visitAndCast(m.getReturnTypeExpr(), p));
        m = m.withReturnTypeExpr(
                m.getReturnTypeExpr() == null ?
                        null :
                        visitTypeName(m.getReturnTypeExpr(), p));
        m = m.withName(visitAndCast(m.getName(), p));
        m = m.getPadding().withParams(visitContainer(m.getPadding().getParams(), JContainer.Location.METHOD_DECL_ARGUMENTS, p));
        if (m.getPadding().getThrows() != null) {
            m = m.getPadding().withThrows(visitContainer(m.getPadding().getThrows(), JContainer.Location.THROWS, p));
        }
        m = m.getPadding().withThrows(visitTypeNames(m.getPadding().getThrows(), p));
        m = m.withBody(visitAndCast(m.getBody(), p));
        if (m.getPadding().getDefaultValue() != null) {
            m = m.getPadding().withDefaultValue(visitLeftPadded(m.getPadding().getDefaultValue(), JLeftPadded.Location.METHOD_DECL_DEFAULT_VALUE, p));
        }
        return m;
    }

    public J visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = visitAndCast(method, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.METHOD_INVOCATION_PREFIX, p));
        m = visitAndCast(m, p, this::visitStatement);
        m = visitAndCast(m, p, this::visitExpression);
        if (m.getPadding().getSelect() != null && m.getPadding().getSelect().getElem() instanceof NameTree &&
                method.getType() != null && method.getType().hasFlags(Flag.Static)) {
            //noinspection unchecked
            m = m.getPadding().withSelect(
                    (JRightPadded<Expression>) (JRightPadded<?>)
                            visitTypeName((JRightPadded<NameTree>) (JRightPadded<?>) m.getPadding().getSelect(), p));
        }
        if (m.getPadding().getSelect() != null) {
            m = m.getPadding().withSelect(visitRightPadded(m.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, p));
        }
        if (m.getPadding().getTypeParameters() != null) {
            m = m.getPadding().withTypeParameters(visitContainer(m.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p));
        }
        m = m.getPadding().withTypeParameters(visitTypeNames(m.getPadding().getTypeParameters(), p));
        m = m.withName(visitAndCast(m.getName(), p));
        return m.getPadding().withArgs(visitContainer(m.getPadding().getArgs(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, p));
    }

    public J visitMultiCatch(J.MultiCatch multiCatch, P p) {
        J.MultiCatch m = visitAndCast(multiCatch, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.MULTI_CATCH_PREFIX, p));
        return m.getPadding().withAlternatives(ListUtils.map(m.getPadding().getAlternatives(), t ->
                visitTypeName(visitRightPadded(t, JRightPadded.Location.CATCH_ALTERNATIVE, p), p)));
    }

    public J visitMultiVariable(J.VariableDecls multiVariable, P p) {
        J.VariableDecls m = visitAndCast(multiVariable, p, this::visitEach);
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.MULTI_VARIABLE_PREFIX, p));
        m = visitAndCast(m, p, this::visitStatement);
        m = m.withAnnotations(ListUtils.map(m.getAnnotations(), a -> visitAndCast(a, p)));
        m = m.withModifiers(Objects.requireNonNull(ListUtils.map(m.getModifiers(), e -> visitAndCast(e, p))));
        m = m.withModifiers(ListUtils.map(m.getModifiers(),
                mod -> mod.withPrefix(visitSpace(mod.getPrefix(), Space.Location.MODIFIER_PREFIX, p))));
        m = m.withTypeExpr(visitAndCast(m.getTypeExpr(), p));
        m = m.withDimensionsBeforeName(ListUtils.map(m.getDimensionsBeforeName(), dim ->
                dim.withBefore(visitSpace(dim.getBefore(), Space.Location.DIMENSION, p))
                        .withElem(visitSpace(dim.getElem(), Space.Location.DIMENSION, p))
        ));
        m = m.withTypeExpr(m.getTypeExpr() == null ?
                null :
                visitTypeName(m.getTypeExpr(), p));
        m = m.withVarargs(m.getVarargs() == null ?
                null :
                visitSpace(m.getVarargs(), Space.Location.VARARGS, p));
        return m.getPadding().withVars(ListUtils.map(m.getPadding().getVars(), t -> visitRightPadded(t, JRightPadded.Location.NAMED_VARIABLE, p)));
    }

    public J visitNewArray(J.NewArray newArray, P p) {
        J.NewArray n = visitAndCast(newArray, p, this::visitEach);
        n = n.withPrefix(visitSpace(n.getPrefix(), Space.Location.NEW_ARRAY_PREFIX, p));
        n = visitAndCast(n, p, this::visitExpression);
        n = n.withTypeExpr(visitAndCast(n.getTypeExpr(), p));
        n = n.withTypeExpr(n.getTypeExpr() == null ?
                null :
                visitTypeName(n.getTypeExpr(), p));
        n = n.withDimensions(ListUtils.map(n.getDimensions(), d -> visitAndCast(d, p)));
        if (n.getPadding().getInitializer() != null) {
            n = n.getPadding().withInitializer(visitContainer(n.getPadding().getInitializer(), JContainer.Location.NEW_ARRAY_INITIALIZER, p));
        }
        return n;
    }

    public J visitNewClass(J.NewClass newClass, P p) {
        J.NewClass n = visitAndCast(newClass, p, this::visitEach);
        n = n.withPrefix(visitSpace(n.getPrefix(), Space.Location.NEW_CLASS_PREFIX, p));
        if (n.getPadding().getEncl() != null) {
            n = n.getPadding().withEncl(visitRightPadded(n.getPadding().getEncl(), JRightPadded.Location.NEW_CLASS_ENCL, p));
        }
        n = visitAndCast(n, p, this::visitStatement);
        n = visitAndCast(n, p, this::visitExpression);
        n = n.withNew(visitSpace(n.getNew(), Space.Location.NEW_PREFIX, p));
        n = n.withClazz(visitAndCast(n.getClazz(), p));
        n = n.withClazz(n.getClazz() == null ?
                null :
                visitTypeName(n.getClazz(), p));
        if (n.getPadding().getArgs() != null) {
            n = n.getPadding().withArgs(visitContainer(n.getPadding().getArgs(), JContainer.Location.NEW_CLASS_ARGS, p));
        }
        return n.withBody(visitAndCast(n.getBody(), p));
    }

    public J visitPackage(J.Package pkg, P p) {
        J.Package pa = visitAndCast(pkg, p, this::visitEach);
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), Space.Location.PACKAGE_PREFIX, p));
        return pa.withExpr(visitAndCast(pa.getExpr(), p));
    }

    public J visitParameterizedType(J.ParameterizedType type, P p) {
        J.ParameterizedType pt = visitAndCast(type, p, this::visitEach);
        pt = pt.withPrefix(visitSpace(pt.getPrefix(), Space.Location.PARAMETERIZED_TYPE_PREFIX, p));
        pt = visitAndCast(pt, p, this::visitExpression);
        pt = pt.withClazz(visitAndCast(pt.getClazz(), p));
        pt = pt.withClazz(visitTypeName(pt.getClazz(), p));
        if (pt.getPadding().getTypeParameters() != null) {
            pt = pt.getPadding().withTypeParameters(visitContainer(pt.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p));
        }
        return pt.getPadding().withTypeParameters(visitTypeNames(pt.getPadding().getTypeParameters(), p));
    }

    public <T extends J> J visitParentheses(J.Parentheses<T> parens, P p) {
        J.Parentheses<T> pa = visitAndCast(parens, p, this::visitEach);
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), Space.Location.PARENTHESES_PREFIX, p));
        pa = visitAndCast(pa, p, this::visitExpression);
        return pa.getPadding().withTree(visitRightPadded(pa.getPadding().getTree(), JRightPadded.Location.PARENTHESES, p));
    }

    public J visitPrimitive(J.Primitive primitive, P p) {
        J.Primitive pr = visitAndCast(primitive, p, this::visitEach);
        pr = pr.withPrefix(visitSpace(pr.getPrefix(), Space.Location.PRIMITIVE_PREFIX, p));
        return visitAndCast(pr, p, this::visitExpression);
    }

    public J visitReturn(J.Return retrn, P p) {
        J.Return r = visitAndCast(retrn, p, this::visitEach);
        r = r.withPrefix(visitSpace(r.getPrefix(), Space.Location.RETURN_PREFIX, p));
        r = visitAndCast(r, p, this::visitStatement);
        return r.withExpr(visitAndCast(r.getExpr(), p));
    }

    public J visitSwitch(J.Switch switzh, P p) {
        J.Switch s = visitAndCast(switzh, p, this::visitEach);
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.SWITCH_PREFIX, p));
        s = visitAndCast(s, p, this::visitStatement);
        s = s.withSelector(visitAndCast(s.getSelector(), p));
        return s.withCases(visitAndCast(s.getCases(), p));
    }

    public J visitSynchronized(J.Synchronized synch, P p) {
        J.Synchronized s = visitAndCast(synch, p, this::visitEach);
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.SYNCHRONIZED_PREFIX, p));
        s = visitAndCast(s, p, this::visitStatement);
        s = s.withLock(visitAndCast(s.getLock(), p));
        return s.withBody(visitAndCast(s.getBody(), p));
    }

    public J visitTernary(J.Ternary ternary, P p) {
        J.Ternary t = visitAndCast(ternary, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TERNARY_PREFIX, p));
        t = visitAndCast(t, p, this::visitExpression);
        t = t.withCondition(visitAndCast(t.getCondition(), p));
        t = t.getPadding().withTruePart(visitLeftPadded(t.getPadding().getTruePart(), JLeftPadded.Location.TERNARY_TRUE, p));
        return t.getPadding().withFalsePart(visitLeftPadded(t.getPadding().getFalsePart(), JLeftPadded.Location.TERNARY_FALSE, p));
    }

    public J visitThrow(J.Throw thrown, P p) {
        J.Throw t = visitAndCast(thrown, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.THROW_PREFIX, p));
        t = visitAndCast(t, p, this::visitStatement);
        return t.withException(visitAndCast(t.getException(), p));
    }

    public J visitTry(J.Try tryable, P p) {
        J.Try t = visitAndCast(tryable, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TRY_PREFIX, p));
        t = visitAndCast(t, p, this::visitStatement);
        if (t.getPadding().getResources() != null) {
            t = t.getPadding().withResources(visitContainer(
                    t.getPadding().getResources().getPadding().withElems(
                            ListUtils.map(t.getPadding().getResources().getPadding().getElems(),
                                    res -> res.withElem(
                                            res.getElem().withPrefix(
                                                    visitSpace(res.getElem().getPrefix(), Space.Location.TRY_RESOURCE, p)
                                            )
                                    )
                            )
                    ),
                    JContainer.Location.TRY_RESOURCES, p));
        }
        t = t.withBody(visitAndCast(t.getBody(), p));
        t = t.withCatches(ListUtils.map(t.getCatches(), c -> visitAndCast(c, p)));
        if (t.getPadding().getFinally() != null) {
            t = t.getPadding().withFinally(visitLeftPadded(t.getPadding().getFinally(), JLeftPadded.Location.TRY_FINALLY, p));
        }
        return t;
    }

    public J visitTypeCast(J.TypeCast typeCast, P p) {
        J.TypeCast t = visitAndCast(typeCast, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TYPE_CAST_PREFIX, p));
        t = visitAndCast(t, p, this::visitExpression);
        t = t.withClazz(visitAndCast(t.getClazz(), p));
        t = t.withClazz(t.getClazz().withTree(visitTypeName(t.getClazz().getTree(), p)));
        return t.withExpr(visitAndCast(t.getExpr(), p));
    }

    public J visitTypeParameter(J.TypeParameter typeParam, P p) {
        J.TypeParameter t = visitAndCast(typeParam, p, this::visitEach);
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TYPE_PARAMETERS_PREFIX, p));
        t = t.withAnnotations(ListUtils.map(t.getAnnotations(), a -> visitAndCast(a, p)));
        t = t.withName(visitAndCast(t.getName(), p));
        if (t.getName() instanceof NameTree) {
            t = t.withName((Expression) visitTypeName((NameTree) t.getName(), p));
        }
        if (t.getPadding().getBounds() != null) {
            t = t.getPadding().withBounds(visitContainer(t.getPadding().getBounds(), JContainer.Location.TYPE_BOUNDS, p));
        }
        return t.getPadding().withBounds(visitTypeNames(t.getPadding().getBounds(), p));
    }

    public J visitUnary(J.Unary unary, P p) {
        J.Unary u = visitAndCast(unary, p, this::visitEach);
        u = u.withPrefix(visitSpace(u.getPrefix(), Space.Location.UNARY_PREFIX, p));
        u = visitAndCast(u, p, this::visitStatement);
        u = visitAndCast(u, p, this::visitExpression);
        u = u.getPadding().withOperator(visitLeftPadded(u.getPadding().getOperator(), JLeftPadded.Location.UNARY_OPERATOR, p));
        return u.withExpr(visitAndCast(u.getExpr(), p));
    }

    public J visitVariable(J.VariableDecls.NamedVar variable, P p) {
        J.VariableDecls.NamedVar v = visitAndCast(variable, p, this::visitEach);
        v = v.withPrefix(visitSpace(v.getPrefix(), Space.Location.VARIABLE_PREFIX, p));
        v = v.withName(visitAndCast(v.getName(), p));
        v = v.withDimensionsAfterName(
                ListUtils.map(v.getDimensionsAfterName(),
                        dim -> dim.withBefore(visitSpace(dim.getBefore(), Space.Location.DIMENSION_PREFIX, p))
                                .withElem(visitSpace(dim.getElem(), Space.Location.DIMENSION, p))
                )
        );
        if (v.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(visitLeftPadded(v.getPadding().getInitializer(),
                    JLeftPadded.Location.VARIABLE_INITIALIZER, p));
        }
        return v;
    }

    public J visitWhileLoop(J.WhileLoop whileLoop, P p) {
        J.WhileLoop w = visitAndCast(whileLoop, p, this::visitEach);
        w = w.withPrefix(visitSpace(w.getPrefix(), Space.Location.WHILE_PREFIX, p));
        w = visitAndCast(w, p, this::visitStatement);
        w = w.withCondition(visitAndCast(w.getCondition(), p));
        return w.getPadding().withBody(visitRightPadded(w.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, p));
    }

    public J visitWildcard(J.Wildcard wildcard, P p) {
        J.Wildcard w = visitAndCast(wildcard, p, this::visitEach);
        w = w.withPrefix(visitSpace(w.getPrefix(), Space.Location.WILDCARD_PREFIX, p));
        w = visitAndCast(w, p, this::visitExpression);
        if (w.getPadding().getBound() != null) {
            w = w.getPadding().withBound(
                    w.getPadding().getBound().withBefore(
                            visitSpace(w.getPadding().getBound().getBefore(), Space.Location.WILDCARD_BOUND, p)
                    )
            );
        }
        w = w.withBoundedType(visitAndCast(w.getBoundedType(), p));
        if (w.getBoundedType() != null) {
            // i.e. not a "wildcard" type
            w = w.withBoundedType(visitTypeName(w.getBoundedType(), p));
        }
        return w;
    }

    public <T> JRightPadded<T> visitRightPadded(JRightPadded<T> right, JRightPadded.Location loc, P p) {
        if (cursored) {
            setCursor(new Cursor(getCursor(), right));
        }

        T t = right.getElem();
        if (t instanceof J) {
            //noinspection unchecked
            t = visitAndCast((J) right.getElem(), p);
        }

        Space after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);

        if (cursored) {
            setCursor(getCursor().getParent());
        }

        return (after == right.getAfter() && t == right.getElem()) ? right : new JRightPadded<>(t, after, right.getMarkers());
    }

    public <T> JLeftPadded<T> visitLeftPadded(JLeftPadded<T> left, JLeftPadded.Location loc, P p) {
        if (cursored) {
            setCursor(new Cursor(getCursor(), left));
        }

        Space before = visitSpace(left.getBefore(), loc.getBeforeLocation(), p);
        T t = left.getElem();

        if (t instanceof J) {
            //noinspection unchecked
            t = visitAndCast((J) left.getElem(), p);
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

        Space before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
        List<JRightPadded<J2>> js = ListUtils.map(container.getPadding().getElems(), t -> visitRightPadded(t, loc.getElemLocation(), p));

        if (cursored) {
            setCursor(getCursor().getParent());
        }

        return js == container.getPadding().getElems() && before == container.getBefore() ?
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
                if (!(childClass.getKind().equals(J.ClassDecl.Kind.Class)) ||
                        childClass.hasModifier(J.Modifier.Type.Static)) {
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
