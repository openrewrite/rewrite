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

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;

@SuppressWarnings("unused")
public class JavaVisitor<P> extends TreeVisitor<J, P> {

    @Nullable
    protected JavadocVisitor<P> javadocVisitor;

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof JavaSourceFile;
    }

    @Override
    public String getLanguage() {
        return "java";
    }

    /**
     * This method returns a new instance of a Javadoc visitor that will be used by this JavaVisitor.
     *
     * @return The JavadocVisitor associated with the JavaVisitor.
     */
    protected JavadocVisitor<P> getJavadocVisitor() {
        return new JavadocVisitor<>(this);
    }

    /**
     * This method will add an import to the compilation unit if there is a reference to the type. It adds an additional
     * visitor which means the "add import" is deferred and does not complete immediately. This operation is idempotent
     * and calling this method multiple times with the same arguments will only add an import once.
     *
     * @param clazz The class that will be imported into the compilation unit.
     */
    public void maybeAddImport(JavaType.@Nullable FullyQualified clazz) {
        if (clazz != null) {
            maybeAddImport(clazz.getFullyQualifiedName());
        }
    }

    public <J2 extends J> J2 maybeAutoFormat(J2 before, J2 after, P p) {
        return maybeAutoFormat(before, after, p, getCursor().getParentTreeCursor());
    }

    public <J2 extends J> J2 maybeAutoFormat(J2 before, J2 after, P p, Cursor parent) {
        return maybeAutoFormat(before, after, null, p, parent);
    }

    public <J2 extends J> J2 maybeAutoFormat(J2 before, J2 after, @Nullable J stopAfter, P p, Cursor parent) {
        if (before != after) {
            return autoFormat(after, stopAfter, p, parent);
        }
        return after;
    }

    public <J2 extends J> J2 autoFormat(J2 j, P p) {
        return autoFormat(j, p, getCursor().getParentTreeCursor());
    }

    public <J2 extends J> J2 autoFormat(J2 j, P p, Cursor parent) {
        return autoFormat(j, null, p, parent);
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public <J2 extends J> J2 autoFormat(J2 j, @Nullable J stopAfter, P p, Cursor parent) {
        JavaSourceFile cu = (j instanceof JavaSourceFile) ?
                (JavaSourceFile) j :
                getCursor().firstEnclosingOrThrow(JavaSourceFile.class);
        AutoFormatService service = cu.service(AutoFormatService.class);
        return (J2) service.autoFormatVisitor(stopAfter).visit(j, p, parent);
    }

    /**
     * This method will add an import to the compilation unit if there is a reference to the type. It adds an additional
     * visitor which means the "add import" is deferred and does not complete immediately. This operation is idempotent
     * and calling this method multiple times with the same arguments will only add an import once.
     *
     * @param fullyQualifiedName Fully-qualified name of the class.
     */
    public void maybeAddImport(String fullyQualifiedName) {
        maybeAddImport(fullyQualifiedName, null, true);
    }

    /**
     * This method will add a static import to the compilation unit if there is a reference to the type/method. It adds
     * an additional visitor which means the "add import" is deferred and does not complete immediately. This operation
     * is idempotent and calling this method multiple times with the same arguments will only add an import once.
     *
     * @param fullyQualifiedName Fully-qualified name of the class.
     * @param member             The static method or field to be imported. A wildcard "*" may also be used to statically import all methods/fields.
     */
    public void maybeAddImport(String fullyQualifiedName, String member) {
        maybeAddImport(fullyQualifiedName, member, true);
    }

    public void maybeAddImport(String fullyQualifiedName, boolean onlyIfReferenced) {
        maybeAddImport(fullyQualifiedName, null, onlyIfReferenced);
    }

    public void maybeAddImport(String fullyQualifiedName, @Nullable String member, boolean onlyIfReferenced) {
        int lastDotIdx = fullyQualifiedName.lastIndexOf('.');
        String packageName = lastDotIdx != -1 ? fullyQualifiedName.substring(0, lastDotIdx) : null;
        String typeName = lastDotIdx != -1 ? fullyQualifiedName.substring(lastDotIdx + 1) : fullyQualifiedName;
        maybeAddImport(packageName, typeName, member, null, onlyIfReferenced);
    }

    public void maybeAddImport(@Nullable String packageName, String typeName, @Nullable String member, @Nullable String alias, boolean onlyIfReferenced) {
        JavaVisitor<P> visitor = service(ImportService.class).addImportVisitor(packageName, typeName, member, alias, onlyIfReferenced);
        if (!getAfterVisit().contains(visitor)) {
            doAfterVisit(visitor);
        }
    }

    @Incubating(since = "8.2.0")
    public <S> S service(Class<S> service) {
        for (Cursor c = getCursor(); c.getParent() != null; c = c.getParent()) {
            Map<Class<?>, Object> services = c.getMessage("__services");
            if (services != null && services.containsKey(service)) {
                //noinspection unchecked
                return (S) services.get(service);
            }
            if (c.getValue() instanceof JavaSourceFile) {
                S found = ((JavaSourceFile) c.getValue()).service(service);
                services = getCursor().getMessage("__services");
                if (services == null) {
                    c.putMessage("__services", services = new HashMap<>());
                }
                services.put(service, found);
                return found;
            }
        }

        throw new IllegalArgumentException("No JavaSourceFile parent found");
    }

    public void maybeRemoveImport(JavaType.@Nullable FullyQualified clazz) {
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
    public Space visitSpace(@Nullable Space space, Space.Location loc, P p) {
        //noinspection ConstantValue
        if (space == Space.EMPTY || space == Space.SINGLE_SPACE || space == null) {
            return space;
        } else if (space.getComments().isEmpty()) {
            return space;
        }
        return space.withComments(ListUtils.map(space.getComments(), comment -> {
            if (comment instanceof Javadoc) {
                if (javadocVisitor == null) {
                    javadocVisitor = getJavadocVisitor();
                }
                Cursor previous = javadocVisitor.getCursor();
                Comment c = (Comment) javadocVisitor.visit((Javadoc) comment, p, getCursor());
                javadocVisitor.setCursor(previous);
                return c;
            }
            return comment;
        }));
    }

    public @Nullable JavaType visitType(@Nullable JavaType javaType, P p) {
        return javaType;
    }

    public <N extends NameTree> N visitTypeName(N nameTree, P p) {
        return nameTree;
    }

    private <N extends NameTree> @Nullable JLeftPadded<N> visitTypeName(@Nullable JLeftPadded<N> nameTree, P p) {
        return nameTree == null ? null : nameTree.withElement(visitTypeName(nameTree.getElement(), p));
    }

    private <N extends NameTree> @Nullable JRightPadded<N> visitTypeName(@Nullable JRightPadded<N> nameTree, P p) {
        return nameTree == null ? null : nameTree.withElement(visitTypeName(nameTree.getElement(), p));
    }

    private <J2 extends J> @Nullable JContainer<J2> visitTypeNames(@Nullable JContainer<J2> nameTrees, P p) {
        if (nameTrees == null) {
            return null;
        }
        @SuppressWarnings("unchecked") List<JRightPadded<J2>> js = ListUtils.map(nameTrees.getPadding().getElements(),
                t -> t.getElement() instanceof NameTree ? (JRightPadded<J2>) visitTypeName((JRightPadded<NameTree>) t, p) : t);
        return js == nameTrees.getPadding().getElements() ? nameTrees : JContainer.build(nameTrees.getBefore(), js, Markers.EMPTY);
    }

    public J visitAnnotatedType(J.AnnotatedType annotatedType, P p) {
        J.AnnotatedType a = annotatedType;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ANNOTATED_TYPE_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof J.AnnotatedType)) {
            return temp;
        } else {
            a = (J.AnnotatedType) temp;
        }
        a = a.withAnnotations(ListUtils.map(a.getAnnotations(), e -> visitAndCast(e, p)));
        a = a.withTypeExpression(visitAndCast(a.getTypeExpression(), p));
        return a.withTypeExpression(visitTypeName(a.getTypeExpression(), p));
    }

    public J visitAnnotation(J.Annotation annotation, P p) {
        J.Annotation a = annotation;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ANNOTATION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof J.Annotation)) {
            return temp;
        } else {
            a = (J.Annotation) temp;
        }
        if (a.getPadding().getArguments() != null) {
            a = a.getPadding().withArguments(visitContainer(a.getPadding().getArguments(), JContainer.Location.ANNOTATION_ARGUMENTS, p));
        }
        a = a.withAnnotationType(visitAndCast(a.getAnnotationType(), p));
        return a.withAnnotationType(visitTypeName(a.getAnnotationType(), p));
    }

    public J visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
        J.ArrayAccess a = arrayAccess;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ARRAY_ACCESS_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof J.ArrayAccess)) {
            return temp;
        } else {
            a = (J.ArrayAccess) temp;
        }
        a = a.withIndexed(visitAndCast(a.getIndexed(), p));
        a = a.withDimension(visitAndCast(a.getDimension(), p));
        return a.withType(visitType(a.getType(), p));
    }

    public J visitArrayDimension(J.ArrayDimension arrayDimension, P p) {
        J.ArrayDimension a = arrayDimension;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.DIMENSION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        return a.getPadding().withIndex(visitRightPadded(a.getPadding().getIndex(), JRightPadded.Location.ARRAY_INDEX, p));
    }

    public J visitArrayType(J.ArrayType arrayType, P p) {
        J.ArrayType a = arrayType;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ARRAY_TYPE_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof J.ArrayType)) {
            return temp;
        } else {
            a = (J.ArrayType) temp;
        }
        a = a.withElementType(visitAndCast(a.getElementType(), p));
        a = a.withElementType(visitTypeName(a.getElementType(), p));
        a = a.withAnnotations(ListUtils.map(a.getAnnotations(), ann -> visitAndCast(ann, p)));
        if (a.getDimension() != null) {
            a = a.withDimension(a.getDimension()
                    .withBefore(visitSpace(a.getDimension().getBefore(), Space.Location.DIMENSION_PREFIX, p))
                    .withElement(visitSpace(a.getDimension().getElement(), Space.Location.DIMENSION, p)));
        }
        return a.withType(visitType(a.getType(), p));
    }

    public J visitAssert(J.Assert assert_, P p) {
        J.Assert a = assert_;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ASSERT_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Statement temp = (Statement) visitStatement(a, p);
        if (!(temp instanceof J.Assert)) {
            return temp;
        } else {
            a = (J.Assert) temp;
        }
        a = a.withCondition(visitAndCast(a.getCondition(), p));
        if (a.getDetail() != null) {
            a = a.withDetail(visitLeftPadded(a.getDetail(), JLeftPadded.Location.ASSERT_DETAIL, p));
        }
        return a;
    }

    public J visitAssignment(J.Assignment assignment, P p) {
        J.Assignment a = assignment;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ASSIGNMENT_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Statement temp = (Statement) visitStatement(a, p);
        if (!(temp instanceof J.Assignment)) {
            return temp;
        } else {
            a = (J.Assignment) temp;
        }
        Expression temp2 = (Expression) visitExpression(a, p);
        if (!(temp2 instanceof J.Assignment)) {
            return temp2;
        } else {
            a = (J.Assignment) temp2;
        }
        a = a.withVariable(visitAndCast(a.getVariable(), p));
        a = a.getPadding().withAssignment(visitLeftPadded(a.getPadding().getAssignment(), JLeftPadded.Location.ASSIGNMENT, p));
        return a.withType(visitType(a.getType(), p));
    }

    public J visitAssignmentOperation(J.AssignmentOperation assignOp, P p) {
        J.AssignmentOperation a = assignOp;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ASSIGNMENT_OPERATION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Statement temp = (Statement) visitStatement(a, p);
        if (!(temp instanceof J.AssignmentOperation)) {
            return temp;
        } else {
            a = (J.AssignmentOperation) temp;
        }
        Expression temp2 = (Expression) visitExpression(a, p);
        if (!(temp2 instanceof J.AssignmentOperation)) {
            return temp2;
        } else {
            a = (J.AssignmentOperation) temp2;
        }
        a = a.withVariable(visitAndCast(a.getVariable(), p));
        a = a.getPadding().withOperator(visitLeftPadded(a.getPadding().getOperator(), JLeftPadded.Location.ASSIGNMENT_OPERATION_OPERATOR, p));
        a = a.withAssignment(visitAndCast(a.getAssignment(), p));
        return a.withType(visitType(a.getType(), p));
    }

    public J visitBinary(J.Binary binary, P p) {
        J.Binary b = binary;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BINARY_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof J.Binary)) {
            return temp;
        } else {
            b = (J.Binary) temp;
        }
        b = b.withLeft(visitAndCast(b.getLeft(), p));
        b = b.getPadding().withOperator(visitLeftPadded(b.getPadding().getOperator(), JLeftPadded.Location.BINARY_OPERATOR, p));
        b = b.withRight(visitAndCast(b.getRight(), p));
        return b.withType(visitType(b.getType(), p));
    }

    public J visitBlock(J.Block block, P p) {
        J.Block b = block;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BLOCK_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Statement temp = (Statement) visitStatement(b, p);
        if (!(temp instanceof J.Block)) {
            return temp;
        } else {
            b = (J.Block) temp;
        }
        b = b.getPadding().withStatic(visitRightPadded(b.getPadding().getStatic(), JRightPadded.Location.STATIC_INIT, p));
        b = b.getPadding().withStatements(ListUtils.map(b.getPadding().getStatements(), t ->
                visitRightPadded(t, JRightPadded.Location.BLOCK_STATEMENT, p)));
        return b.withEnd(visitSpace(b.getEnd(), Space.Location.BLOCK_END, p));
    }

    public J visitBreak(J.Break breakStatement, P p) {
        J.Break b = breakStatement;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BREAK_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Statement temp = (Statement) visitStatement(b, p);
        if (!(temp instanceof J.Break)) {
            return temp;
        } else {
            b = (J.Break) temp;
        }
        return b.withLabel(visitAndCast(b.getLabel(), p));
    }

    public J visitCase(J.Case case_, P p) {
        J.Case c = case_;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CASE_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        Statement temp = (Statement) visitStatement(c, p);
        if (!(temp instanceof J.Case)) {
            return temp;
        } else {
            c = (J.Case) temp;
        }
        c = c.getPadding().withCaseLabels(visitContainer(c.getPadding().getCaseLabels(), JContainer.Location.CASE_LABEL, p));
        c = c.withGuard(visitAndCast(c.getGuard(), p));
        c = c.getPadding().withBody(visitRightPadded(c.getPadding().getBody(), JRightPadded.Location.CASE_BODY, p));
        return c.getPadding().withStatements(visitContainer(c.getPadding().getStatements(), JContainer.Location.CASE, p));
    }

    public J visitCatch(J.Try.Catch catch_, P p) {
        J.Try.Catch c = catch_;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CATCH_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withParameter(visitAndCast(c.getParameter(), p));
        return c.withBody(visitAndCast(c.getBody(), p));
    }

    public J visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = classDecl;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CLASS_DECLARATION_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        Statement temp = (Statement) visitStatement(c, p);
        if (!(temp instanceof J.ClassDeclaration)) {
            return temp;
        } else {
            c = (J.ClassDeclaration) temp;
        }
        c = c.withLeadingAnnotations(ListUtils.map(c.getLeadingAnnotations(), a -> visitAndCast(a, p)));
        c = c.withModifiers(ListUtils.map(c.getModifiers(), m -> visitAndCast(m, p)));
        //Kind can have annotations associated with it, need to visit those.
        c = c.getPadding().withKind(
                classDecl.getPadding().getKind().withAnnotations(
                        ListUtils.map(classDecl.getPadding().getKind().getAnnotations(), a -> visitAndCast(a, p))
                )
        );
        c = c.getPadding().withKind(
                c.getPadding().getKind().withPrefix(
                        visitSpace(c.getPadding().getKind().getPrefix(), Space.Location.CLASS_KIND, p)
                )
        );
        c = c.withName(visitAndCast(c.getName(), p));
        if (c.getPadding().getTypeParameters() != null) {
            c = c.getPadding().withTypeParameters(visitContainer(c.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p));
        }
        if (c.getPadding().getPrimaryConstructor() != null) {
            c = c.getPadding().withPrimaryConstructor(visitContainer(c.getPadding().getPrimaryConstructor(), JContainer.Location.RECORD_STATE_VECTOR, p));
        }
        if (c.getPadding().getExtends() != null) {
            c = c.getPadding().withExtends(visitLeftPadded(c.getPadding().getExtends(), JLeftPadded.Location.EXTENDS, p));
        }
        c = c.getPadding().withExtends(visitTypeName(c.getPadding().getExtends(), p));
        if (c.getPadding().getImplements() != null) {
            c = c.getPadding().withImplements(visitContainer(c.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, p));
        }
        if (c.getPadding().getPermits() != null) {
            c = c.getPadding().withPermits(visitContainer(c.getPadding().getPermits(), JContainer.Location.PERMITS, p));
        }
        c = c.getPadding().withImplements(visitTypeNames(c.getPadding().getImplements(), p));
        c = c.withBody(visitAndCast(c.getBody(), p));
        return c.withType(visitType(c.getType(), p));
    }

    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        J.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        if (c.getPadding().getPackageDeclaration() != null) {
            c = c.getPadding().withPackageDeclaration(visitRightPadded(c.getPadding().getPackageDeclaration(), JRightPadded.Location.PACKAGE, p));
        }
        c = c.getPadding().withImports(ListUtils.map(c.getPadding().getImports(), t -> visitRightPadded(t, JRightPadded.Location.IMPORT, p)));
        c = c.withClasses(ListUtils.map(c.getClasses(), e -> visitAndCast(e, p)));
        return c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
    }

    public J visitContinue(J.Continue continueStatement, P p) {
        J.Continue c = continueStatement;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CONTINUE_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        Statement temp = (Statement) visitStatement(c, p);
        if (!(temp instanceof J.Continue)) {
            return temp;
        } else {
            c = (J.Continue) temp;
        }
        return c.withLabel(visitAndCast(c.getLabel(), p));
    }

    public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, P p) {
        J.ControlParentheses<T> cp = controlParens;
        cp = cp.withPrefix(visitSpace(cp.getPrefix(), Space.Location.CONTROL_PARENTHESES_PREFIX, p));
        Expression temp = (Expression) visitExpression(cp, p);
        if (!(temp instanceof J.ControlParentheses)) {
            return temp;
        } else {
            //noinspection unchecked
            cp = (J.ControlParentheses<T>) temp;
        }
        cp = cp.getPadding().withTree(visitRightPadded(cp.getPadding().getTree(), JRightPadded.Location.PARENTHESES, p));
        return cp.withMarkers(visitMarkers(cp.getMarkers(), p));
    }

    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        J.DoWhileLoop d = doWhileLoop;
        d = d.withPrefix(visitSpace(d.getPrefix(), Space.Location.DO_WHILE_PREFIX, p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        Statement temp = (Statement) visitStatement(d, p);
        if (!(temp instanceof J.DoWhileLoop)) {
            return temp;
        } else {
            d = (J.DoWhileLoop) temp;
        }
        d = d.getPadding().withWhileCondition(visitLeftPadded(d.getPadding().getWhileCondition(), JLeftPadded.Location.WHILE_CONDITION, p));
        return d.getPadding().withBody(visitRightPadded(d.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, p));
    }

    public J visitEmpty(J.Empty empty, P p) {
        J.Empty e = empty;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.EMPTY_PREFIX, p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        Statement temp = (Statement) visitStatement(e, p);
        if (!(temp instanceof J.Empty)) {
            return temp;
        } else {
            e = (J.Empty) temp;
        }
        Expression temp2 = (Expression) visitExpression(e, p);
        if (!(temp2 instanceof J.Empty)) {
            return temp2;
        } else {
            e = (J.Empty) temp2;
        }
        return e;
    }

    public J visitEnumValue(J.EnumValue enum_, P p) {
        J.EnumValue e = enum_;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.ENUM_VALUE_PREFIX, p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withAnnotations(ListUtils.map(e.getAnnotations(), a -> visitAndCast(a, p)));
        e = e.withName(visitAndCast(e.getName(), p));
        return e.withInitializer(visitAndCast(e.getInitializer(), p));
    }

    public J visitEnumValueSet(J.EnumValueSet enums, P p) {
        J.EnumValueSet e = enums;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.ENUM_VALUE_SET_PREFIX, p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        Statement temp = (Statement) visitStatement(e, p);
        if (!(temp instanceof J.EnumValueSet)) {
            return temp;
        } else {
            e = (J.EnumValueSet) temp;
        }
        return e.getPadding().withEnums(ListUtils.map(e.getPadding().getEnums(), t -> visitRightPadded(t, JRightPadded.Location.ENUM_VALUE, p)));
    }

    public J visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        J.FieldAccess f = fieldAccess;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FIELD_ACCESS_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = visitTypeName(f, p);
        Expression temp = (Expression) visitExpression(f, p);
        if (!(temp instanceof J.FieldAccess)) {
            return temp;
        } else {
            f = (J.FieldAccess) temp;
        }
        Statement tempStat = (Statement) visitStatement(f, p);
        if (!(tempStat instanceof J.FieldAccess)) {
            return tempStat;
        } else {
            f = (J.FieldAccess) tempStat;
        }
        f = f.withTarget(visitAndCast(f.getTarget(), p));
        f = f.getPadding().withName(visitLeftPadded(f.getPadding().getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p));
        return f.withType(visitType(f.getType(), p));
    }

    public J visitForEachLoop(J.ForEachLoop forLoop, P p) {
        J.ForEachLoop f = forLoop;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FOR_EACH_LOOP_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Statement temp = (Statement) visitStatement(f, p);
        if (!(temp instanceof J.ForEachLoop)) {
            return temp;
        } else {
            f = (J.ForEachLoop) temp;
        }
        f = f.withControl(visitAndCast(f.getControl(), p));
        return f.getPadding().withBody(visitRightPadded(f.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p));
    }

    public J visitForEachControl(J.ForEachLoop.Control control, P p) {
        J.ForEachLoop.Control c = control;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.FOR_EACH_CONTROL_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withVariable(visitRightPadded(c.getPadding().getVariable(), JRightPadded.Location.FOREACH_VARIABLE, p));
        return c.getPadding().withIterable(visitRightPadded(c.getPadding().getIterable(), JRightPadded.Location.FOREACH_ITERABLE, p));
    }

    public J visitForLoop(J.ForLoop forLoop, P p) {
        J.ForLoop f = forLoop;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FOR_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Statement temp = (Statement) visitStatement(f, p);
        if (!(temp instanceof J.ForLoop)) {
            return temp;
        } else {
            f = (J.ForLoop) temp;
        }
        f = f.withControl(visitAndCast(f.getControl(), p));
        return f.getPadding().withBody(visitRightPadded(f.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p));
    }

    public J visitForControl(J.ForLoop.Control control, P p) {
        J.ForLoop.Control c = control;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.FOR_CONTROL_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withInit(ListUtils.map(c.getPadding().getInit(), t -> visitRightPadded(t, JRightPadded.Location.FOR_INIT, p)));
        c = c.getPadding().withCondition(visitRightPadded(c.getPadding().getCondition(), JRightPadded.Location.FOR_CONDITION, p));
        return c.getPadding().withUpdate(ListUtils.map(c.getPadding().getUpdate(), t -> visitRightPadded(t, JRightPadded.Location.FOR_UPDATE, p)));
    }

    public J visitParenthesizedTypeTree(J.ParenthesizedTypeTree parTree, P p) {
        J.ParenthesizedTypeTree t = parTree;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.PARENTHESES_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        if (t.getAnnotations() != null && !t.getAnnotations().isEmpty()) {
            t = t.withAnnotations(ListUtils.map(t.getAnnotations(), a -> visitAndCast(a, p)));
        }

        J temp = visitParentheses(t.getParenthesizedType(), p);
        if (!(temp instanceof J.Parentheses)) {
            return temp;
        } else {
            //noinspection unchecked
            t = t.withParenthesizedType((J.Parentheses<TypeTree>) temp);
        }
        return t;
    }

    public J visitIdentifier(J.Identifier ident, P p) {
        J.Identifier i = ident;
        if (i.getAnnotations() != null && !i.getAnnotations().isEmpty()) {
            // performance optimization
            i = i.withAnnotations(ListUtils.map(i.getAnnotations(), a -> visitAndCast(a, p)));
        }
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.IDENTIFIER_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        Expression temp = (Expression) visitExpression(i, p);
        if (!(temp instanceof J.Identifier)) {
            return temp;
        } else {
            i = (J.Identifier) temp;
        }
        i = i.withType(visitType(i.getType(), p));
        return i.withFieldType((JavaType.Variable) visitType(i.getFieldType(), p));
    }

    public J visitElse(J.If.Else else_, P p) {
        J.If.Else e = else_;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.ELSE_PREFIX, p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        return e.getPadding().withBody(visitRightPadded(e.getPadding().getBody(), JRightPadded.Location.IF_ELSE, p));
    }

    public J visitIf(J.If iff, P p) {
        J.If i = iff;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.IF_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        Statement temp = (Statement) visitStatement(i, p);
        if (!(temp instanceof J.If)) {
            return temp;
        } else {
            i = (J.If) temp;
        }
        i = i.withIfCondition(visitAndCast(i.getIfCondition(), p));
        i = i.getPadding().withThenPart(visitRightPadded(i.getPadding().getThenPart(), JRightPadded.Location.IF_THEN, p));
        return i.withElsePart(visitAndCast(i.getElsePart(), p));
    }

    public J visitImport(J.Import import_, P p) {
        J.Import i = import_;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.IMPORT_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.getPadding().withStatic(visitLeftPadded(i.getPadding().getStatic(), JLeftPadded.Location.STATIC_IMPORT, p));
        i = i.withQualid(visitAndCast(i.getQualid(), p));
        return i.getPadding().withAlias(visitLeftPadded(i.getPadding().getAlias(), JLeftPadded.Location.IMPORT_ALIAS_PREFIX, p));
    }

    public J visitInstanceOf(J.InstanceOf instanceOf, P p) {
        J.InstanceOf i = instanceOf;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.INSTANCEOF_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        Expression temp = (Expression) visitExpression(i, p);
        if (!(temp instanceof J.InstanceOf)) {
            return temp;
        } else {
            i = (J.InstanceOf) temp;
        }
        i = i.getPadding().withExpression(visitRightPadded(i.getPadding().getExpression(), JRightPadded.Location.INSTANCEOF, p));
        i = i.withClazz(visitAndCast(i.getClazz(), p));
        i = i.withPattern(visitAndCast(i.getPattern(), p));
        return i.withType(visitType(i.getType(), p));
    }

    public J visitDeconstructionPattern(J.DeconstructionPattern deconstructionPattern, P p) {
        J.DeconstructionPattern d = deconstructionPattern;
        d = d.withPrefix(visitSpace(d.getPrefix(), Space.Location.DECONSTRUCTION_PATTERN_PREFIX, p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withDeconstructor(visitAndCast(d.getDeconstructor(), p));
        d = d.getPadding().withNested(visitContainer(d.getPadding().getNested(), JContainer.Location.DECONSTRUCTION_PATTERN_NESTED, p));
        return d.withType(visitType(d.getType(), p));

    }

    public J visitIntersectionType(J.IntersectionType intersectionType, P p) {
        J.IntersectionType i = intersectionType;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.INTERSECTION_TYPE_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.getPadding().withBounds(visitContainer(i.getPadding().getBounds(), JContainer.Location.TYPE_BOUNDS, p));
        return i.withType(visitType(i.getType(), p));
    }

    public J visitLabel(J.Label label, P p) {
        J.Label l = label;
        l = l.withPrefix(visitSpace(l.getPrefix(), Space.Location.LABEL_PREFIX, p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        Statement temp = (Statement) visitStatement(l, p);
        if (!(temp instanceof J.Label)) {
            return temp;
        } else {
            l = (J.Label) temp;
        }
        l = l.getPadding().withLabel(visitRightPadded(l.getPadding().getLabel(), JRightPadded.Location.LABEL, p));
        return l.withStatement(visitAndCast(l.getStatement(), p));
    }

    public J visitLambda(J.Lambda lambda, P p) {
        J.Lambda l = lambda;
        l = l.withPrefix(visitSpace(l.getPrefix(), Space.Location.LAMBDA_PREFIX, p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        Expression temp = (Expression) visitExpression(l, p);
        if (!(temp instanceof J.Lambda)) {
            return temp;
        } else {
            l = (J.Lambda) temp;
        }
        l = l.withParameters(visitAndCast(l.getParameters(), p));
        l = l.withArrow(visitSpace(l.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p));
        l = l.withBody(visitAndCast(l.getBody(), p));
        return l.withType(visitType(l.getType(), p));
    }

    public J visitLambdaParameters(J.Lambda.Parameters parameters, P p) {
        J.Lambda.Parameters params = parameters;
        params = params.withPrefix(visitSpace(params.getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, p));
        params = params.withMarkers(visitMarkers(params.getMarkers(), p));
        return params.getPadding().withParameters(
                ListUtils.map(params.getPadding().getParameters(),
                        param -> visitRightPadded(param, JRightPadded.Location.LAMBDA_PARAM, p)
                )
        );
    }

    public J visitLiteral(J.Literal literal, P p) {
        J.Literal l = literal;
        l = l.withPrefix(visitSpace(l.getPrefix(), Space.Location.LITERAL_PREFIX, p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        Expression temp = (Expression) visitExpression(l, p);
        if (!(temp instanceof J.Literal)) {
            return temp;
        } else {
            l = (J.Literal) temp;
        }
        return l.withType(visitType(l.getType(), p));
    }

    public J visitMemberReference(J.MemberReference memberRef, P p) {
        J.MemberReference m = memberRef;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.MEMBER_REFERENCE_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Expression temp = (Expression) visitExpression(m, p);
        if (!(temp instanceof J.MemberReference)) {
            return temp;
        } else {
            m = (J.MemberReference) temp;
        }
        m = m.getPadding().withContaining(visitRightPadded(m.getPadding().getContaining(), JRightPadded.Location.MEMBER_REFERENCE_CONTAINING, p));
        if (m.getPadding().getTypeParameters() != null) {
            m = m.getPadding().withTypeParameters(visitContainer(m.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p));
        }
        m = m.getPadding().withReference(visitLeftPadded(m.getPadding().getReference(), JLeftPadded.Location.MEMBER_REFERENCE_NAME, p));
        m = m.withType(visitType(m.getType(), p));
        m = m.withMethodType((JavaType.Method) visitType(m.getMethodType(), p));
        return m.withVariableType((JavaType.Variable) visitType(m.getVariableType(), p));
    }

    public J visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = method;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.METHOD_DECLARATION_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof J.MethodDeclaration)) {
            return temp;
        } else {
            m = (J.MethodDeclaration) temp;
        }
        m = m.withLeadingAnnotations(ListUtils.map(m.getLeadingAnnotations(), a -> visitAndCast(a, p)));
        m = m.withModifiers(ListUtils.map(m.getModifiers(), e -> visitAndCast(e, p)));
        m = m.getAnnotations().withTypeParameters((J.TypeParameters) visit(m.getAnnotations().getTypeParameters(), p));
        m = m.withReturnTypeExpression(visitAndCast(m.getReturnTypeExpression(), p));
        m = m.withReturnTypeExpression(
                m.getReturnTypeExpression() == null ?
                        null :
                        visitTypeName(m.getReturnTypeExpression(), p));
        m = m.getAnnotations().withName(m.getAnnotations().getName().withAnnotations(ListUtils.map(m.getAnnotations().getName().getAnnotations(), a -> visitAndCast(a, p))));
        m = m.withName((J.Identifier) visitNonNull(m.getName(), p));
        m = m.getPadding().withParameters(visitContainer(m.getPadding().getParameters(), JContainer.Location.METHOD_DECLARATION_PARAMETERS, p));
        if (m.getPadding().getThrows() != null) {
            m = m.getPadding().withThrows(visitContainer(m.getPadding().getThrows(), JContainer.Location.THROWS, p));
        }
        m = m.getPadding().withThrows(visitTypeNames(m.getPadding().getThrows(), p));
        m = m.withBody(visitAndCast(m.getBody(), p));
        if (m.getPadding().getDefaultValue() != null) {
            m = m.getPadding().withDefaultValue(visitLeftPadded(m.getPadding().getDefaultValue(), JLeftPadded.Location.METHOD_DECLARATION_DEFAULT_VALUE, p));
        }
        return m.withMethodType((JavaType.Method) visitType(m.getMethodType(), p));
    }

    public J visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = method;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.METHOD_INVOCATION_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof J.MethodInvocation)) {
            return temp;
        } else {
            m = (J.MethodInvocation) temp;
        }
        Expression temp2 = (Expression) visitExpression(m, p);
        if (!(temp2 instanceof J.MethodInvocation)) {
            return temp2;
        } else {
            m = (J.MethodInvocation) temp2;
        }
        if (m.getPadding().getSelect() != null && m.getPadding().getSelect().getElement() instanceof NameTree &&
            method.getMethodType() != null && method.getMethodType().hasFlags(Flag.Static)) {
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
        m = m.withName((J.Identifier) visitNonNull(m.getName(), p));
        m = m.getPadding().withArguments(visitContainer(m.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, p));
        return m.withMethodType((JavaType.Method) visitType(m.getMethodType(), p));
    }

    public J visitModifier(J.Modifier modifer, P p) {
        J.Modifier m = modifer;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.MODIFIER_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        return m.withAnnotations(ListUtils.map(m.getAnnotations(), a -> visitAndCast(a, p)));
    }

    public J visitMultiCatch(J.MultiCatch multiCatch, P p) {
        J.MultiCatch m = multiCatch;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.MULTI_CATCH_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        return m.getPadding().withAlternatives(ListUtils.map(m.getPadding().getAlternatives(), t ->
                visitTypeName(visitRightPadded(t, JRightPadded.Location.CATCH_ALTERNATIVE, p), p)));
    }

    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        J.VariableDeclarations m = multiVariable;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof J.VariableDeclarations)) {
            return temp;
        } else {
            m = (J.VariableDeclarations) temp;
        }
        m = m.withLeadingAnnotations(ListUtils.map(m.getLeadingAnnotations(), a -> visitAndCast(a, p)));
        m = m.withModifiers(Objects.requireNonNull(ListUtils.map(m.getModifiers(), e -> visitAndCast(e, p))));
        m = m.withTypeExpression(visitAndCast(m.getTypeExpression(), p));
        m = m.withTypeExpression(m.getTypeExpression() == null ?
                null :
                visitTypeName(m.getTypeExpression(), p));
        m = m.withVarargs(m.getVarargs() == null ?
                null :
                visitSpace(m.getVarargs(), Space.Location.VARARGS, p));
        return m.getPadding().withVariables(ListUtils.map(m.getPadding().getVariables(), t -> visitRightPadded(t, JRightPadded.Location.NAMED_VARIABLE, p)));
    }

    public J visitNewArray(J.NewArray newArray, P p) {
        J.NewArray n = newArray;
        n = n.withPrefix(visitSpace(n.getPrefix(), Space.Location.NEW_ARRAY_PREFIX, p));
        n = n.withMarkers(visitMarkers(n.getMarkers(), p));
        Expression temp = (Expression) visitExpression(n, p);
        if (!(temp instanceof J.NewArray)) {
            return temp;
        } else {
            n = (J.NewArray) temp;
        }
        n = n.withTypeExpression(visitAndCast(n.getTypeExpression(), p));
        n = n.withTypeExpression(n.getTypeExpression() == null ?
                null :
                visitTypeName(n.getTypeExpression(), p));
        n = n.withDimensions(ListUtils.map(n.getDimensions(), d -> visitAndCast(d, p)));
        if (n.getPadding().getInitializer() != null) {
            n = n.getPadding().withInitializer(visitContainer(n.getPadding().getInitializer(), JContainer.Location.NEW_ARRAY_INITIALIZER, p));
        }
        return n.withType(visitType(n.getType(), p));
    }

    public J visitNewClass(J.NewClass newClass, P p) {
        J.NewClass n = newClass;
        n = n.withPrefix(visitSpace(n.getPrefix(), Space.Location.NEW_CLASS_PREFIX, p));
        n = n.withMarkers(visitMarkers(n.getMarkers(), p));
        if (n.getPadding().getEnclosing() != null) {
            n = n.getPadding().withEnclosing(visitRightPadded(n.getPadding().getEnclosing(), JRightPadded.Location.NEW_CLASS_ENCLOSING, p));
        }
        Statement temp = (Statement) visitStatement(n, p);
        if (!(temp instanceof J.NewClass)) {
            return temp;
        } else {
            n = (J.NewClass) temp;
        }
        Expression temp2 = (Expression) visitExpression(n, p);
        if (!(temp2 instanceof J.NewClass)) {
            return temp2;
        } else {
            n = (J.NewClass) temp2;
        }
        n = n.withNew(visitSpace(n.getNew(), Space.Location.NEW_PREFIX, p));
        n = n.withClazz(visitAndCast(n.getClazz(), p));
        n = n.withClazz(n.getClazz() == null ? null : visitTypeName(n.getClazz(), p));
        n = n.getPadding().withArguments(visitContainer(n.getPadding().getArguments(), JContainer.Location.NEW_CLASS_ARGUMENTS, p));
        n = n.withBody(visitAndCast(n.getBody(), p));
        return n.withConstructorType((JavaType.Method) visitType(n.getConstructorType(), p));
    }

    public J visitNullableType(J.NullableType nullableType, P p) {
        J.NullableType nt = nullableType;
        nt = nt.withPrefix(visitSpace(nt.getPrefix(), Space.Location.NULLABLE_TYPE_PREFIX, p));
        nt = nt.withMarkers(visitMarkers(nt.getMarkers(), p));
        nt = nt.withAnnotations(ListUtils.map(nt.getAnnotations(), a -> visitAndCast(a, p)));

        Expression temp = (Expression) visitExpression(nt, p);
        if (!(temp instanceof J.NullableType)) {
            return temp;
        } else {
            nt = (J.NullableType) temp;
        }

        nt = nt.getPadding().withTypeTree(visitRightPadded(nt.getPadding().getTypeTree(), JRightPadded.Location.NULLABLE, p));
        return nt.withType(visitType(nt.getType(), p));
    }

    public J visitPackage(J.Package pkg, P p) {
        J.Package pa = pkg;
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), Space.Location.PACKAGE_PREFIX, p));
        pa = pa.withMarkers(visitMarkers(pa.getMarkers(), p));
        pa = pa.withExpression(visitAndCast(pa.getExpression(), p));
        return pa.withAnnotations(ListUtils.map(pa.getAnnotations(), a -> visitAndCast(a, p)));
    }

    public J visitParameterizedType(J.ParameterizedType type, P p) {
        J.ParameterizedType pt = type;
        pt = pt.withPrefix(visitSpace(pt.getPrefix(), Space.Location.PARAMETERIZED_TYPE_PREFIX, p));
        pt = pt.withMarkers(visitMarkers(pt.getMarkers(), p));
        Expression temp = (Expression) visitExpression(pt, p);
        if (!(temp instanceof J.ParameterizedType)) {
            return temp;
        } else {
            pt = (J.ParameterizedType) temp;
        }
        pt = pt.withClazz(visitAndCast(pt.getClazz(), p));
        if (pt.getPadding().getTypeParameters() != null) {
            pt = pt.getPadding().withTypeParameters(visitContainer(pt.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p));
        }
        pt = pt.getPadding().withTypeParameters(visitTypeNames(pt.getPadding().getTypeParameters(), p));
        return pt.withType(visitType(pt.getType(), p));
    }

    public <T extends J> J visitParentheses(J.Parentheses<T> parens, P p) {
        J.Parentheses<T> pa = parens;
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), Space.Location.PARENTHESES_PREFIX, p));
        pa = pa.withMarkers(visitMarkers(pa.getMarkers(), p));
        Expression temp = (Expression) visitExpression(pa, p);
        if (!(temp instanceof J.Parentheses)) {
            return temp;
        } else {
            //noinspection unchecked
            pa = (J.Parentheses<T>) temp;
        }
        return pa.getPadding().withTree(visitRightPadded(pa.getPadding().getTree(), JRightPadded.Location.PARENTHESES, p));
    }

    public J visitPrimitive(J.Primitive primitive, P p) {
        J.Primitive pr = primitive;
        pr = pr.withPrefix(visitSpace(pr.getPrefix(), Space.Location.PRIMITIVE_PREFIX, p));
        pr = pr.withMarkers(visitMarkers(pr.getMarkers(), p));
        Expression temp = (Expression) visitExpression(pr, p);
        if (!(temp instanceof J.Primitive)) {
            return temp;
        } else {
            pr = (J.Primitive) temp;
        }
        return pr.withType(visitType(pr.getType(), p));
    }

    public J visitReturn(J.Return return_, P p) {
        J.Return r = return_;
        r = r.withPrefix(visitSpace(r.getPrefix(), Space.Location.RETURN_PREFIX, p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        Statement temp = (Statement) visitStatement(r, p);
        if (!(temp instanceof J.Return)) {
            return temp;
        } else {
            r = (J.Return) temp;
        }
        return r.withExpression(visitAndCast(r.getExpression(), p));
    }

    public J visitSwitch(J.Switch switch_, P p) {
        J.Switch s = switch_;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.SWITCH_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Statement temp = (Statement) visitStatement(s, p);
        if (!(temp instanceof J.Switch)) {
            return temp;
        } else {
            s = (J.Switch) temp;
        }
        s = s.withSelector(visitAndCast(s.getSelector(), p));
        return s.withCases(visitAndCast(s.getCases(), p));
    }

    public J visitSwitchExpression(J.SwitchExpression switch_, P p) {
        J.SwitchExpression s = switch_;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.SWITCH_EXPRESSION_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Expression temp = (Expression) visitExpression(s, p);
        if (!(temp instanceof J.SwitchExpression)) {
            return temp;
        } else {
            s = (J.SwitchExpression) temp;
        }
        s = s.withSelector(visitAndCast(s.getSelector(), p));
        return s.withCases(visitAndCast(s.getCases(), p));
    }

    public J visitSynchronized(J.Synchronized synch, P p) {
        J.Synchronized s = synch;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.SYNCHRONIZED_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Statement temp = (Statement) visitStatement(s, p);
        if (!(temp instanceof J.Synchronized)) {
            return temp;
        } else {
            s = (J.Synchronized) temp;
        }
        s = s.withLock(visitAndCast(s.getLock(), p));
        return s.withBody(visitAndCast(s.getBody(), p));
    }

    public J visitTernary(J.Ternary ternary, P p) {
        J.Ternary t = ternary;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TERNARY_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof J.Ternary)) {
            return temp;
        } else {
            t = (J.Ternary) temp;
        }
        Statement tempStat = (Statement) visitStatement(t, p);
        if (!(tempStat instanceof J.Ternary)) {
            return tempStat;
        } else {
            t = (J.Ternary) tempStat;
        }
        t = t.withCondition(visitAndCast(t.getCondition(), p));
        t = t.getPadding().withTruePart(visitLeftPadded(t.getPadding().getTruePart(), JLeftPadded.Location.TERNARY_TRUE, p));
        t = t.getPadding().withFalsePart(visitLeftPadded(t.getPadding().getFalsePart(), JLeftPadded.Location.TERNARY_FALSE, p));
        return t.withType(visitType(t.getType(), p));
    }

    public J visitThrow(J.Throw thrown, P p) {
        J.Throw t = thrown;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.THROW_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Statement temp = (Statement) visitStatement(t, p);
        if (!(temp instanceof J.Throw)) {
            return temp;
        } else {
            t = (J.Throw) temp;
        }
        return t.withException(visitAndCast(t.getException(), p));
    }

    public J visitTry(J.Try tryable, P p) {
        J.Try t = tryable;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TRY_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Statement temp = (Statement) visitStatement(t, p);
        if (!(temp instanceof J.Try)) {
            return temp;
        } else {
            t = (J.Try) temp;
        }
        if (t.getPadding().getResources() != null) {
            t = t.getPadding().withResources(visitContainer(t.getPadding().getResources(), JContainer.Location.TRY_RESOURCES, p));
        }
        t = t.withBody(visitAndCast(t.getBody(), p));
        t = t.withCatches(ListUtils.map(t.getCatches(), c -> visitAndCast(c, p)));
        if (t.getPadding().getFinally() != null) {
            t = t.getPadding().withFinally(visitLeftPadded(t.getPadding().getFinally(), JLeftPadded.Location.TRY_FINALLY, p));
        }
        return t;
    }

    public J visitTryResource(J.Try.Resource tryResource, P p) {
        J.Try.Resource r = tryResource;
        r = tryResource.withPrefix(visitSpace(r.getPrefix(), Space.Location.TRY_RESOURCE, p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        return tryResource.withVariableDeclarations(visitAndCast(r.getVariableDeclarations(), p));
    }

    public J visitTypeCast(J.TypeCast typeCast, P p) {
        J.TypeCast t = typeCast;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TYPE_CAST_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof J.TypeCast)) {
            return temp;
        } else {
            t = (J.TypeCast) temp;
        }
        t = t.withClazz(visitAndCast(t.getClazz(), p));
        t = t.withClazz(t.getClazz().withTree(visitTypeName(t.getClazz().getTree(), p)));
        return t.withExpression(visitAndCast(t.getExpression(), p));
    }

    public J visitTypeParameter(J.TypeParameter typeParam, P p) {
        J.TypeParameter t = typeParam;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TYPE_PARAMETERS_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.withAnnotations(ListUtils.map(t.getAnnotations(), a -> visitAndCast(a, p)));

        if (t.getModifiers() != null && !t.getModifiers().isEmpty()) {
            t = t.withModifiers(ListUtils.map(t.getModifiers(), m -> visitAndCast(m, p)));
        }

        t = t.withName(visitAndCast(t.getName(), p));
        if (t.getName() instanceof NameTree) {
            t = t.withName((Expression) visitTypeName((NameTree) t.getName(), p));
        }
        if (t.getPadding().getBounds() != null) {
            t = t.getPadding().withBounds(visitContainer(t.getPadding().getBounds(), JContainer.Location.TYPE_BOUNDS, p));
        }
        return t.getPadding().withBounds(visitTypeNames(t.getPadding().getBounds(), p));
    }

    public J visitTypeParameters(J.TypeParameters typeParameters, P p) {
        J.TypeParameters t = typeParameters;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TYPE_PARAMETERS_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.withAnnotations(ListUtils.map(t.getAnnotations(), a -> visitAndCast(a, p)));
        return t.getPadding().withTypeParameters(
                ListUtils.map(t.getPadding().getTypeParameters(),
                        tp -> visitRightPadded(tp, JRightPadded.Location.TYPE_PARAMETER, p)
                )
        );
    }

    public J visitUnary(J.Unary unary, P p) {
        J.Unary u = unary;
        u = u.withPrefix(visitSpace(u.getPrefix(), Space.Location.UNARY_PREFIX, p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        Statement temp = (Statement) visitStatement(u, p);
        if (!(temp instanceof J.Unary)) {
            return temp;
        } else {
            u = (J.Unary) temp;
        }
        Expression temp2 = (Expression) visitExpression(u, p);
        if (!(temp2 instanceof J.Unary)) {
            return temp2;
        } else {
            u = (J.Unary) temp2;
        }
        u = u.getPadding().withOperator(visitLeftPadded(u.getPadding().getOperator(), JLeftPadded.Location.UNARY_OPERATOR, p));
        u = u.withExpression(visitAndCast(u.getExpression(), p));
        return u.withType(visitType(u.getType(), p));
    }

    public J visitUnknown(J.Unknown unknown, P p) {
        J.Unknown u = unknown;
        u = u.withPrefix(visitSpace(u.getPrefix(), Space.Location.UNKNOWN_PREFIX, p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        return u.withSource(visitAndCast(u.getSource(), p));
    }

    public J visitUnknownSource(J.Unknown.Source source, P p) {
        J.Unknown.Source s = source;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.UNKNOWN_SOURCE_PREFIX, p));
        return s.withMarkers(visitMarkers(s.getMarkers(), p));
    }

    public J visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        J.VariableDeclarations.NamedVariable v = variable;
        v = v.withPrefix(visitSpace(v.getPrefix(), Space.Location.VARIABLE_PREFIX, p));
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withDeclarator(visitAndCast(v.getDeclarator(), p));
        v = v.withDimensionsAfterName(
                ListUtils.map(v.getDimensionsAfterName(),
                        dim -> dim.withBefore(visitSpace(dim.getBefore(), Space.Location.DIMENSION_PREFIX, p))
                                .withElement(visitSpace(dim.getElement(), Space.Location.DIMENSION, p))
                )
        );
        if (v.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(visitLeftPadded(v.getPadding().getInitializer(),
                    JLeftPadded.Location.VARIABLE_INITIALIZER, p));
        }
        return v.withVariableType((JavaType.Variable) visitType(v.getVariableType(), p));
    }

    public J visitWhileLoop(J.WhileLoop whileLoop, P p) {
        J.WhileLoop w = whileLoop;
        w = w.withPrefix(visitSpace(w.getPrefix(), Space.Location.WHILE_PREFIX, p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        Statement temp = (Statement) visitStatement(w, p);
        if (!(temp instanceof J.WhileLoop)) {
            return temp;
        } else {
            w = (J.WhileLoop) temp;
        }
        w = w.withCondition(visitAndCast(w.getCondition(), p));
        return w.getPadding().withBody(visitRightPadded(w.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, p));
    }

    public J visitWildcard(J.Wildcard wildcard, P p) {
        J.Wildcard w = wildcard;
        w = w.withPrefix(visitSpace(w.getPrefix(), Space.Location.WILDCARD_PREFIX, p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        Expression temp = (Expression) visitExpression(w, p);
        if (!(temp instanceof J.Wildcard)) {
            return temp;
        } else {
            w = (J.Wildcard) temp;
        }
        w = w.getPadding().withBound(visitLeftPadded(w.getPadding().getBound(), JLeftPadded.Location.WILDCARD_BOUND, p));
        w = w.withBoundedType(visitAndCast(w.getBoundedType(), p));
        if (w.getBoundedType() != null) {
            // i.e. not a "wildcard" type
            w = w.withBoundedType(visitTypeName(w.getBoundedType(), p));
        }
        return w;
    }

    public J visitYield(J.Yield yield, P p) {
        J.Yield y = yield;
        y = y.withPrefix(visitSpace(y.getPrefix(), Space.Location.YIELD_PREFIX, p));
        y = y.withMarkers(visitMarkers(y.getMarkers(), p));
        Statement temp = (Statement) visitStatement(y, p);
        if (!(temp instanceof J.Yield)) {
            return temp;
        } else {
            y = (J.Yield) temp;
        }
        return y.withValue(visitAndCast(y.getValue(), p));
    }

    public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, P p) {
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        if (t instanceof J) {
            //noinspection unchecked
            t = visitAndCast((J) right.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }

        Space after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
        Markers markers = visitMarkers(right.getMarkers(), p);
        return (after == right.getAfter() && t == right.getElement() && markers == right.getMarkers()) ?
                right : new JRightPadded<>(t, after, markers);
    }

    public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, JLeftPadded.Location loc, P p) {
        if (left == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), left));

        Space before = visitSpace(left.getBefore(), loc.getBeforeLocation(), p);
        T t = left.getElement();

        if (t instanceof J) {
            //noinspection unchecked
            t = visitAndCast((J) left.getElement(), p);
        }

        setCursor(getCursor().getParent());
        // If nothing changed leave AST node the same
        if (left.getElement() == t && before == left.getBefore()) {
            return left;
        }

        //noinspection ConstantConditions
        return t == null ? null : new JLeftPadded<>(before, t, left.getMarkers());
    }

    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container,
                                                                  JContainer.Location loc, P p) {
        if (container == null) {
            //noinspection ConstantConditions
            return null;
        }
        setCursor(new Cursor(getCursor(), container));

        Space before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
        List<JRightPadded<J2>> js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));

        setCursor(getCursor().getParent());

        return js == container.getPadding().getElements() && before == container.getBefore() ?
                container :
                JContainer.build(before, js, container.getMarkers());
    }

    public J visitErroneous(J.Erroneous erroneous, P p) {
        J.Erroneous u = erroneous;
        u = u.withPrefix(visitSpace(u.getPrefix(), Space.Location.ERRONEOUS, p));
        return u.withMarkers(visitMarkers(u.getMarkers(), p));
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
                                                   t instanceof J.MethodDeclaration ||
                                                   t instanceof J.Try ||
                                                   t instanceof J.ForLoop ||
                                                   t instanceof J.ForEachLoop).getValue();

        //Now walk up the child path looking for the base scope.
        for (Iterator<Object> it = child.getPath(); it.hasNext(); ) {
            Object childScope = it.next();
            if (childScope instanceof J.ClassDeclaration) {
                J.ClassDeclaration childClass = (J.ClassDeclaration) childScope;
                if (childClass.getKind() != J.ClassDeclaration.Kind.Type.Class ||
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
     * Check if a child LST element is in the same lexical scope as that of the LST element associated with the current
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
