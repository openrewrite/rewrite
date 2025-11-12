/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.groovy.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.Objects;

public class MergeSpacesVisitor extends GroovyVisitor<Object> {

    @Override
    public J visitCompilationUnit(G.CompilationUnit cu, Object ctx) {
        if (cu == ctx || !(ctx instanceof G.CompilationUnit)) {
            return cu;
        }
        G.CompilationUnit newCu = (G.CompilationUnit) ctx;
        G.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, newCu.getPrefix()));
        c = c.withMarkers(visitMarkers(c.getMarkers(), newCu.getMarkers()));
        if (c.getPackageDeclaration() != null && newCu.getPackageDeclaration() != null) {
            c = c.withPackageDeclaration((G.Package) visitNonNull(c.getPackageDeclaration(), newCu.getPackageDeclaration()));
        }
        c = c.withStatements(ListUtils.map(c.getStatements(), (index, e) -> visitAndCast(e, newCu.getStatements().get(index))));
        return c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, newCu.getEof()));
    }

    @Override
    public J visitGString(G.GString gString, Object ctx) {
        if (gString == ctx || !(ctx instanceof G.GString)) {
            return gString;
        }
        G.GString newGString = (G.GString) ctx;
        G.GString g = gString;
        g = g.withPrefix(visitSpace(g.getPrefix(), GSpace.Location.GSTRING, newGString.getPrefix()));
        g = g.withMarkers(visitMarkers(g.getMarkers(), newGString.getMarkers()));
        Expression temp = (Expression) visitExpression(g, newGString);
        if (!(temp instanceof G.GString)) {
            return temp;
        } else {
            g = (G.GString) temp;
        }
        g = g.withStrings(ListUtils.map(g.getStrings(), (index, s) -> visit(s, newGString.getStrings().get(index))));
        return g.withType(visitType(gString.getType(), newGString.getType()));
    }

    @Override
    public J visitGStringValue(G.GString.Value value, Object ctx) {
        if (value == ctx || !(ctx instanceof G.GString.Value)) {
            return value;
        }
        G.GString.Value newValue = (G.GString.Value) ctx;
        G.GString.Value v = value;
        v = v.withMarkers(visitMarkers(v.getMarkers(), newValue.getMarkers()));
        v = v.withTree(visit(v.getTree(), newValue.getTree()));
        return v.withAfter(visitSpace(v.getAfter(), GSpace.Location.GSTRING, newValue.getAfter()));
    }

    @Override
    public J visitListLiteral(G.ListLiteral listLiteral, Object ctx) {
        if (listLiteral == ctx || !(ctx instanceof G.ListLiteral)) {
            return listLiteral;
        }
        G.ListLiteral newListLiteral = (G.ListLiteral) ctx;
        G.ListLiteral l = listLiteral;
        l = l.withPrefix(visitSpace(l.getPrefix(), GSpace.Location.LIST_LITERAL, newListLiteral.getPrefix()));
        l = l.withMarkers(visitMarkers(l.getMarkers(), newListLiteral.getMarkers()));
        Expression temp = (Expression) visitExpression(l, newListLiteral);
        if (!(temp instanceof G.ListLiteral)) {
            return temp;
        } else {
            l = (G.ListLiteral) temp;
        }
        l = l.getPadding().withElements(visitContainer(l.getPadding().getElements(), GContainer.Location.LIST_LITERAL_ELEMENTS, newListLiteral.getPadding().getElements()));
        return l.withType(visitType(l.getType(), newListLiteral.getType()));
    }

    @Override
    public J visitMapEntry(G.MapEntry mapEntry, Object ctx) {
        if (mapEntry == ctx || !(ctx instanceof G.MapEntry)) {
            return mapEntry;
        }
        G.MapEntry newMapEntry = (G.MapEntry) ctx;
        G.MapEntry m = mapEntry;
        m = m.withPrefix(visitSpace(m.getPrefix(), GSpace.Location.MAP_ENTRY, newMapEntry.getPrefix()));
        m = m.withMarkers(visitMarkers(m.getMarkers(), newMapEntry.getMarkers()));
        Expression temp = (Expression) visitExpression(m, newMapEntry);
        if (!(temp instanceof G.MapEntry)) {
            return temp;
        } else {
            m = (G.MapEntry) temp;
        }
        m = m.getPadding().withKey(visitRightPadded(m.getPadding().getKey(), GRightPadded.Location.MAP_ENTRY_KEY, newMapEntry.getPadding().getKey()));
        m = m.withValue((Expression) visit(m.getValue(), newMapEntry.getValue()));
        return m.withType(visitType(m.getType(), newMapEntry.getType()));
    }

    @Override
    public J visitMapLiteral(G.MapLiteral mapLiteral, Object ctx) {
        if (mapLiteral == ctx || !(ctx instanceof G.MapLiteral)) {
            return mapLiteral;
        }
        G.MapLiteral newMapLiteral = (G.MapLiteral) ctx;
        G.MapLiteral m = mapLiteral;
        m = m.withPrefix(visitSpace(m.getPrefix(), GSpace.Location.MAP_LITERAL, newMapLiteral.getPrefix()));
        m = m.withMarkers(visitMarkers(m.getMarkers(), newMapLiteral.getMarkers()));
        Expression temp = (Expression) visitExpression(m, newMapLiteral);
        if (!(temp instanceof G.MapLiteral)) {
            return temp;
        } else {
            m = (G.MapLiteral) temp;
        }
        m = m.getPadding().withElements(visitContainer(m.getPadding().getElements(), GContainer.Location.MAP_LITERAL_ELEMENTS, newMapLiteral.getPadding().getElements()));
        return m.withType(visitType(m.getType(), newMapLiteral.getType()));
    }

    @Override
    public J visitUnary(G.Unary unary, Object ctx) {
        if (unary == ctx || !(ctx instanceof G.Unary)) {
            return unary;
        }
        G.Unary newUnary = (G.Unary) ctx;
        G.Unary u = unary;
        u = u.withPrefix(visitSpace(u.getPrefix(), GSpace.Location.UNARY_PREFIX, newUnary.getPrefix()));
        u = u.withMarkers(visitMarkers(u.getMarkers(), newUnary.getMarkers()));
        Expression temp = (Expression) visitExpression(u, newUnary);
        if (!(temp instanceof G.Unary)) {
            return temp;
        } else {
            u = (G.Unary) temp;
        }
        u = u.getPadding().withOperator(visitLeftPadded(u.getPadding().getOperator(), GLeftPadded.Location.UNARY_OPERATOR, newUnary.getPadding().getOperator()));
        u = u.withExpression(visitAndCast(u.getExpression(), newUnary.getExpression()));
        return u.withType(visitType(u.getType(), newUnary.getType()));
    }

    @Override
    public J visitBinary(G.Binary binary, Object ctx) {
        if (binary == ctx || !(ctx instanceof G.Binary)) {
            return binary;
        }
        G.Binary newBinary = (G.Binary) ctx;
        G.Binary b = binary;
        b = b.withPrefix(visitSpace(b.getPrefix(), GSpace.Location.BINARY_PREFIX, newBinary.getPrefix()));
        b = b.withMarkers(visitMarkers(b.getMarkers(), newBinary.getMarkers()));
        Expression temp = (Expression) visitExpression(b, newBinary);
        if (!(temp instanceof G.Binary)) {
            return temp;
        } else {
            b = (G.Binary) temp;
        }
        b = b.withLeft(visitAndCast(b.getLeft(), newBinary.getLeft()));
        b = b.getPadding().withOperator(visitLeftPadded(b.getPadding().getOperator(), GLeftPadded.Location.BINARY_OPERATOR, newBinary.getPadding().getOperator()));
        b = b.withRight(visitAndCast(b.getRight(), newBinary.getRight()));
        return b.withType(visitType(b.getType(), newBinary.getType()));
    }

    @Override
    public J visitRange(G.Range range, Object ctx) {
        if (range == ctx || !(ctx instanceof G.Range)) {
            return range;
        }
        G.Range newRange = (G.Range) ctx;
        G.Range r = range;
        r = r.withPrefix(visitSpace(r.getPrefix(), GSpace.Location.RANGE_PREFIX, newRange.getPrefix()));
        r = r.withMarkers(visitMarkers(r.getMarkers(), newRange.getMarkers()));
        Expression temp = (Expression) visitExpression(r, newRange);
        if (!(temp instanceof G.Range)) {
            return temp;
        } else {
            r = (G.Range) temp;
        }
        r = r.withFrom(visitAndCast(r.getFrom(), newRange.getFrom()));
        r = r.getPadding().withInclusive(visitLeftPadded(r.getPadding().getInclusive(), GLeftPadded.Location.RANGE_INCLUSION, newRange.getPadding().getInclusive()));
        r = r.withTo(visitAndCast(r.getTo(), newRange.getTo()));
        return r.withType(visitType(r.getType(), newRange.getType()));
    }

    @Override
    @SuppressWarnings("unused")
    public Space visitSpace(@Nullable Space space, Space.Location loc, Object ctx) {
        if (space == ctx || !(ctx instanceof Space)) {
            return space;
        }
        Space newSpace = (Space) ctx;
        if (space == null) {
            return newSpace;
        }

        space = space.withWhitespace(newSpace.getWhitespace());
        if (space.getComments().isEmpty() || space.getComments().size() != newSpace.getComments().size()) {
            return space;
        }
        return space.withComments(ListUtils.map(space.getComments(), (index, comment) -> newSpace.getComments().get(index)));
    }

    @Override
    public <N extends NameTree> N visitTypeName(N nameTree, Object ctx) {
        N newNameTree = (N) ctx;
        if (nameTree == newNameTree) {
            return nameTree;
        }
        return nameTree;
    }

    private <N extends NameTree> @Nullable JLeftPadded<N> visitTypeName(@Nullable JLeftPadded<N> nameTree, Object ctx) {
        JLeftPadded<N> newNameTree = (JLeftPadded<N>) ctx;
        if (nameTree == newNameTree) {
            return nameTree;
        }
        return nameTree == null ? null : nameTree.withElement(visitTypeName(nameTree.getElement(), newNameTree == null ? null : newNameTree.getElement()));
    }

    private <N extends NameTree> @Nullable JRightPadded<N> visitTypeName(@Nullable JRightPadded<N> nameTree, Object ctx) {
        JRightPadded<N> newNameTree = (JRightPadded<N>) ctx;
        if (nameTree == newNameTree) {
            return nameTree;
        }
        return nameTree == null ? null : nameTree.withElement(visitTypeName(nameTree.getElement(), newNameTree == null ? null : newNameTree.getElement()));
    }

    private <J2 extends J> @Nullable JContainer<J2> visitTypeNames(@Nullable JContainer<J2> nameTrees, Object ctx) {
        JContainer<J2> newNameTrees = (JContainer<J2>) ctx;
        if (nameTrees == newNameTrees) {
            return nameTrees;
        }
        if (nameTrees == null) {
            return null;
        }
        @SuppressWarnings("unchecked") List<JRightPadded<J2>> js = ListUtils.map(nameTrees.getPadding().getElements(), (index, t) ->
                t.getElement() instanceof NameTree ? (JRightPadded<J2>) visitTypeName((JRightPadded<NameTree>) t, newNameTrees == null ? null : newNameTrees.getPadding().getElements().get(index)) : t);
        return js == nameTrees.getPadding().getElements() ? nameTrees : JContainer.build(nameTrees.getBefore(), js, Markers.EMPTY);
    }

    @Override
    public J visitAnnotatedType(J.AnnotatedType annotatedType, Object ctx) {
        if (annotatedType == ctx || !(ctx instanceof J.AnnotatedType)) {
            return annotatedType;
        }
        J.AnnotatedType newAnnotatedType = (J.AnnotatedType) ctx;
        J.AnnotatedType a = annotatedType;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ANNOTATED_TYPE_PREFIX, newAnnotatedType.getPrefix()));
        a = a.withMarkers(visitMarkers(a.getMarkers(), newAnnotatedType.getMarkers()));
        Expression temp = (Expression) visitExpression(a, newAnnotatedType);
        if (!(temp instanceof J.AnnotatedType)) {
            return temp;
        }
        a = (J.AnnotatedType) temp;
        a = a.withAnnotations(ListUtils.map(a.getAnnotations(), (index, e) -> visitAndCast(e, newAnnotatedType.getAnnotations().get(index))));
        a = a.withTypeExpression(visitAndCast(a.getTypeExpression(), newAnnotatedType.getTypeExpression()));
        return a.withTypeExpression(visitTypeName(a.getTypeExpression(), newAnnotatedType.getTypeExpression()));
    }

    @Override
    public J visitAnnotation(J.Annotation annotation, Object ctx) {
        if (annotation == ctx || !(ctx instanceof J.Annotation)) {
            return annotation;
        }
        J.Annotation newAnnotation = (J.Annotation) ctx;
        J.Annotation a = annotation;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ANNOTATION_PREFIX, newAnnotation.getPrefix()));
        a = a.withMarkers(visitMarkers(a.getMarkers(), newAnnotation.getMarkers()));
        Expression temp = (Expression) visitExpression(a, newAnnotation);
        if (!(temp instanceof J.Annotation)) {
            return temp;
        }
        a = (J.Annotation) temp;
        if (a.getPadding().getArguments() != null && newAnnotation.getPadding().getArguments() != null) {
            a = a.getPadding().withArguments(visitContainer(a.getPadding().getArguments(), JContainer.Location.ANNOTATION_ARGUMENTS, newAnnotation.getPadding().getArguments()));
        }
        a = a.withAnnotationType(visitAndCast(a.getAnnotationType(), newAnnotation.getAnnotationType()));
        return a.withAnnotationType(visitTypeName(a.getAnnotationType(), newAnnotation.getAnnotationType()));
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, Object ctx) {
        if (arrayAccess == ctx || !(ctx instanceof J.ArrayAccess)) {
            return arrayAccess;
        }
        J.ArrayAccess newArrayAccess = (J.ArrayAccess) ctx;
        J.ArrayAccess a = arrayAccess;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ARRAY_ACCESS_PREFIX, newArrayAccess.getPrefix()));
        a = a.withMarkers(visitMarkers(a.getMarkers(), newArrayAccess.getMarkers()));
        Expression temp = (Expression) visitExpression(a, newArrayAccess);
        if (!(temp instanceof J.ArrayAccess)) {
            return temp;
        }
        a = (J.ArrayAccess) temp;
        a = a.withIndexed(visitAndCast(a.getIndexed(), newArrayAccess.getIndexed()));
        a = a.withDimension(visitAndCast(a.getDimension(), newArrayAccess.getDimension()));
        return a.withType(visitType(a.getType(), newArrayAccess.getType()));
    }

    @Override
    public J visitArrayDimension(J.ArrayDimension arrayDimension, Object ctx) {
        if (arrayDimension == ctx || !(ctx instanceof J.ArrayDimension)) {
            return arrayDimension;
        }
        J.ArrayDimension newArrayDimension = (J.ArrayDimension) ctx;
        J.ArrayDimension a = arrayDimension;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.DIMENSION_PREFIX, newArrayDimension.getPrefix()));
        a = a.withMarkers(visitMarkers(a.getMarkers(), newArrayDimension.getMarkers()));
        return a.getPadding().withIndex(visitRightPadded(a.getPadding().getIndex(), JRightPadded.Location.ARRAY_INDEX, newArrayDimension.getPadding().getIndex()));
    }

    @Override
    public J visitArrayType(J.ArrayType arrayType, Object ctx) {
        if (arrayType == ctx || !(ctx instanceof J.ArrayType)) {
            return arrayType;
        }
        J.ArrayType newArrayType = (J.ArrayType) ctx;
        J.ArrayType a = arrayType;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ARRAY_TYPE_PREFIX, newArrayType.getPrefix()));
        a = a.withMarkers(visitMarkers(a.getMarkers(), newArrayType.getMarkers()));
        Expression temp = (Expression) visitExpression(a, newArrayType);
        if (!(temp instanceof J.ArrayType)) {
            return temp;
        }
        a = (J.ArrayType) temp;
        a = a.withElementType(visitAndCast(a.getElementType(), newArrayType.getElementType()));
        a = a.withElementType(visitTypeName(a.getElementType(), newArrayType.getElementType()));
        a = a.withAnnotations(ListUtils.map(a.getAnnotations(), (index, ann) -> visitAndCast(ann, newArrayType.getAnnotations().get(index))));
        if (a.getDimension() != null && newArrayType.getDimension() != null) {
            a = a.withDimension(a.getDimension()
                    .withBefore(visitSpace(a.getDimension().getBefore(), Space.Location.DIMENSION_PREFIX, newArrayType.getDimension().getBefore()))
                    .withElement(visitSpace(a.getDimension().getElement(), Space.Location.DIMENSION, newArrayType.getDimension().getElement())));
        }
        return a.withType(visitType(a.getType(), newArrayType.getType()));
    }

    @Override
    public J visitAssert(J.Assert assert_, Object ctx) {
        if (assert_ == ctx || !(ctx instanceof J.Assert)) {
            return assert_;
        }
        J.Assert newAssert = (J.Assert) ctx;
        J.Assert a = assert_;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ASSERT_PREFIX, newAssert.getPrefix()));
        a = a.withMarkers(visitMarkers(a.getMarkers(), newAssert.getMarkers()));
        Statement temp = (Statement) visitStatement(a, newAssert);
        if (!(temp instanceof J.Assert)) {
            return temp;
        }
        a = (J.Assert) temp;
        a = a.withCondition(visitAndCast(a.getCondition(), newAssert.getCondition()));
        if (a.getDetail() != null && newAssert.getDetail() != null) {
            a = a.withDetail(visitLeftPadded(a.getDetail(), JLeftPadded.Location.ASSERT_DETAIL, newAssert.getDetail()));
        }
        return a;
    }

    @Override
    public J visitAssignment(J.Assignment assignment, Object ctx) {
        if (assignment == ctx || !(ctx instanceof J.Assignment)) {
            return assignment;
        }
        J.Assignment newAssignment = (J.Assignment) ctx;
        J.Assignment a = assignment;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ASSIGNMENT_PREFIX, newAssignment.getPrefix()));
        a = a.withMarkers(visitMarkers(a.getMarkers(), newAssignment.getMarkers()));
        Statement temp = (Statement) visitStatement(a, newAssignment);
        if (!(temp instanceof J.Assignment)) {
            return temp;
        }
        a = (J.Assignment) temp;
        Expression temp2 = (Expression) visitExpression(a, newAssignment);
        if (!(temp2 instanceof J.Assignment)) {
            return temp2;
        }
        a = (J.Assignment) temp2;
        a = a.withVariable(visitAndCast(a.getVariable(), newAssignment.getVariable()));
        a = a.getPadding().withAssignment(visitLeftPadded(a.getPadding().getAssignment(), JLeftPadded.Location.ASSIGNMENT, newAssignment.getPadding().getAssignment()));
        return a.withType(visitType(a.getType(), newAssignment.getType()));
    }

    @Override
    public J visitAssignmentOperation(J.AssignmentOperation assignOp, Object ctx) {
        if (assignOp == ctx || !(ctx instanceof J.AssignmentOperation)) {
            return assignOp;
        }
        J.AssignmentOperation newAssignOp = (J.AssignmentOperation) ctx;
        J.AssignmentOperation a = assignOp;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ASSIGNMENT_OPERATION_PREFIX, newAssignOp.getPrefix()));
        a = a.withMarkers(visitMarkers(a.getMarkers(), newAssignOp.getMarkers()));
        Statement temp = (Statement) visitStatement(a, newAssignOp);
        if (!(temp instanceof J.AssignmentOperation)) {
            return temp;
        }
        a = (J.AssignmentOperation) temp;
        Expression temp2 = (Expression) visitExpression(a, newAssignOp);
        if (!(temp2 instanceof J.AssignmentOperation)) {
            return temp2;
        }
        a = (J.AssignmentOperation) temp2;
        a = a.withVariable(visitAndCast(a.getVariable(), newAssignOp.getVariable()));
        a = a.getPadding().withOperator(visitLeftPadded(a.getPadding().getOperator(), JLeftPadded.Location.ASSIGNMENT_OPERATION_OPERATOR, newAssignOp.getPadding().getOperator()));
        a = a.withAssignment(visitAndCast(a.getAssignment(), newAssignOp.getAssignment()));
        return a.withType(visitType(a.getType(), newAssignOp.getType()));
    }

    @Override
    public J visitBinary(J.Binary binary, Object ctx) {
        if (binary == ctx || !(ctx instanceof J.Binary)) {
            return binary;
        }
        J.Binary newBinary = (J.Binary) ctx;
        J.Binary b = binary;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BINARY_PREFIX, newBinary.getPrefix()));
        b = b.withMarkers(visitMarkers(b.getMarkers(), newBinary.getMarkers()));
        Expression temp = (Expression) visitExpression(b, newBinary);
        if (!(temp instanceof J.Binary)) {
            return temp;
        }
        b = (J.Binary) temp;
        b = b.withLeft(visitAndCast(b.getLeft(), newBinary.getLeft()));
        b = b.getPadding().withOperator(visitLeftPadded(b.getPadding().getOperator(), JLeftPadded.Location.BINARY_OPERATOR, newBinary.getPadding().getOperator()));
        b = b.withRight(visitAndCast(b.getRight(), newBinary.getRight()));
        return b.withType(visitType(b.getType(), newBinary.getType()));
    }

    @Override
    public J visitBlock(J.Block block, Object ctx) {
        if (block == ctx || !(ctx instanceof J.Block)) {
            return block;
        }
        J.Block newBlock = (J.Block) ctx;
        J.Block b = block;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BLOCK_PREFIX, newBlock.getPrefix()));
        b = b.withMarkers(visitMarkers(b.getMarkers(), newBlock.getMarkers()));
        Statement temp = (Statement) visitStatement(b, newBlock);
        if (!(temp instanceof J.Block)) {
            return temp;
        }
        b = (J.Block) temp;
        b = b.getPadding().withStatic(visitRightPadded(b.getPadding().getStatic(), JRightPadded.Location.STATIC_INIT, newBlock.getPadding().getStatic()));
        b = b.getPadding().withStatements(ListUtils.map(b.getPadding().getStatements(), (index, t) ->
                visitRightPadded(t, JRightPadded.Location.BLOCK_STATEMENT, newBlock.getPadding().getStatements().get(index))));
        return b.withEnd(visitSpace(b.getEnd(), Space.Location.BLOCK_END, newBlock.getEnd()));
    }

    @Override
    public J visitBreak(J.Break breakStatement, Object ctx) {
        if (breakStatement == ctx || !(ctx instanceof J.Break)) {
            return breakStatement;
        }
        J.Break newBreakStatement = (J.Break) ctx;
        J.Break b = breakStatement;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BREAK_PREFIX, newBreakStatement.getPrefix()));
        b = b.withMarkers(visitMarkers(b.getMarkers(), newBreakStatement.getMarkers()));
        Statement temp = (Statement) visitStatement(b, newBreakStatement);
        if (!(temp instanceof J.Break)) {
            return temp;
        }
        b = (J.Break) temp;
        return b.withLabel(visitAndCast(b.getLabel(), newBreakStatement.getLabel()));
    }

    @Override
    public J visitCase(J.Case case_, Object ctx) {
        if (case_ == ctx || !(ctx instanceof J.Case)) {
            return case_;
        }
        J.Case newCase = (J.Case) ctx;
        J.Case c = case_;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CASE_PREFIX, newCase.getPrefix()));
        c = c.withMarkers(visitMarkers(c.getMarkers(), newCase.getMarkers()));
        Statement temp = (Statement) visitStatement(c, newCase);
        if (!(temp instanceof J.Case)) {
            return temp;
        }
        c = (J.Case) temp;
        c = c.getPadding().withCaseLabels(visitContainer(c.getPadding().getCaseLabels(), JContainer.Location.CASE_LABEL, newCase.getPadding().getCaseLabels()));
        c = c.withGuard(visitAndCast(c.getGuard(), newCase.getGuard()));
        c = c.getPadding().withBody(visitRightPadded(c.getPadding().getBody(), JRightPadded.Location.CASE_BODY, newCase.getPadding().getBody()));
        return c.getPadding().withStatements(visitContainer(c.getPadding().getStatements(), JContainer.Location.CASE, newCase.getPadding().getStatements()));
    }

    @Override
    public J visitCatch(J.Try.Catch catch_, Object ctx) {
        if (catch_ == ctx || !(ctx instanceof J.Try.Catch)) {
            return catch_;
        }
        J.Try.Catch newCatch = (J.Try.Catch) ctx;
        J.Try.Catch c = catch_;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CATCH_PREFIX, newCatch.getPrefix()));
        c = c.withMarkers(visitMarkers(c.getMarkers(), newCatch.getMarkers()));
        c = c.withParameter(visitAndCast(c.getParameter(), newCatch.getParameter()));
        return c.withBody(visitAndCast(c.getBody(), newCatch.getBody()));
    }

    @Override
    public J visitClassDeclaration(J.ClassDeclaration classDecl, Object ctx) {
        if (classDecl == ctx || !(ctx instanceof J.ClassDeclaration)) {
            return classDecl;
        }
        J.ClassDeclaration newClassDecl = (J.ClassDeclaration) ctx;
        J.ClassDeclaration c = classDecl;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CLASS_DECLARATION_PREFIX, newClassDecl.getPrefix()));
        c = c.withMarkers(visitMarkers(c.getMarkers(), newClassDecl.getMarkers()));
        Statement temp = (Statement) visitStatement(c, newClassDecl);
        if (!(temp instanceof J.ClassDeclaration)) {
            return temp;
        }
        c = (J.ClassDeclaration) temp;
        c = c.withLeadingAnnotations(ListUtils.map(c.getLeadingAnnotations(), (index, a) -> visitAndCast(a, newClassDecl.getLeadingAnnotations().get(index))));
        c = c.withModifiers(ListUtils.map(c.getModifiers(), (index, m) -> visitAndCast(m, newClassDecl.getModifiers().get(index))));
        //Kind can have annotations associated with it, need to visit those.
        c = c.getPadding().withKind(
                classDecl.getPadding().getKind().withAnnotations(
                        ListUtils.map(classDecl.getPadding().getKind().getAnnotations(), (index, a) -> visitAndCast(a, newClassDecl.getPadding().getKind().getAnnotations().get(index)))
                )
        );
        c = c.getPadding().withKind(
                c.getPadding().getKind().withPrefix(
                        visitSpace(c.getPadding().getKind().getPrefix(), Space.Location.CLASS_KIND, newClassDecl.getPadding().getKind().getPrefix())
                )
        );
        c = c.withName(visitAndCast(c.getName(), newClassDecl.getName()));
        if (c.getPadding().getTypeParameters() != null && newClassDecl.getPadding().getTypeParameters() != null) {
            c = c.getPadding().withTypeParameters(visitContainer(c.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, newClassDecl.getPadding().getTypeParameters()));
        }
        if (c.getPadding().getPrimaryConstructor() != null && newClassDecl.getPadding().getPrimaryConstructor() != null) {
            c = c.getPadding().withPrimaryConstructor(visitContainer(c.getPadding().getPrimaryConstructor(), JContainer.Location.RECORD_STATE_VECTOR, newClassDecl.getPadding().getPrimaryConstructor()));
        }
        if (c.getPadding().getExtends() != null && newClassDecl.getPadding().getExtends() != null) {
            c = c.getPadding().withExtends(visitLeftPadded(c.getPadding().getExtends(), JLeftPadded.Location.EXTENDS, newClassDecl.getPadding().getExtends()));
        }
        c = c.getPadding().withExtends(visitTypeName(c.getPadding().getExtends(), newClassDecl.getPadding().getExtends()));
        if (c.getPadding().getImplements() != null && newClassDecl.getPadding().getImplements() != null) {
            c = c.getPadding().withImplements(visitContainer(c.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, newClassDecl.getPadding().getImplements()));
        }
        if (c.getPadding().getPermits() != null && newClassDecl.getPadding().getPermits() != null) {
            c = c.getPadding().withPermits(visitContainer(c.getPadding().getPermits(), JContainer.Location.PERMITS, newClassDecl.getPadding().getPermits()));
        }
        c = c.getPadding().withImplements(visitTypeNames(c.getPadding().getImplements(), newClassDecl.getPadding().getImplements()));
        c = c.withBody(visitAndCast(c.getBody(), newClassDecl.getBody()));
        return c.withType(visitType(c.getType(), newClassDecl.getType()));
    }

    @Override
    public J visitContinue(J.Continue continueStatement, Object ctx) {
        if (continueStatement == ctx || !(ctx instanceof J.Continue)) {
            return continueStatement;
        }
        J.Continue newContinueStatement = (J.Continue) ctx;
        J.Continue c = continueStatement;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CONTINUE_PREFIX, newContinueStatement.getPrefix()));
        c = c.withMarkers(visitMarkers(c.getMarkers(), newContinueStatement.getMarkers()));
        Statement temp = (Statement) visitStatement(c, newContinueStatement);
        if (!(temp instanceof J.Continue)) {
            return temp;
        }
        c = (J.Continue) temp;
        return c.withLabel(visitAndCast(c.getLabel(), newContinueStatement.getLabel()));
    }

    @Override
    public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, Object ctx) {
        J.ControlParentheses<T> newControlParens = (J.ControlParentheses<T>) ctx;
        if (controlParens == newControlParens) {
            return controlParens;
        }
        J.ControlParentheses<T> cp = controlParens;
        cp = cp.withPrefix(visitSpace(cp.getPrefix(), Space.Location.CONTROL_PARENTHESES_PREFIX, newControlParens.getPrefix()));
        Expression temp = (Expression) visitExpression(cp, newControlParens);
        if (!(temp instanceof J.ControlParentheses)) {
            return temp;
        }
        //noinspection unchecked
        cp = (J.ControlParentheses<T>) temp;
        cp = cp.getPadding().withTree(visitRightPadded(cp.getPadding().getTree(), JRightPadded.Location.PARENTHESES, newControlParens.getPadding().getTree()));
        return cp.withMarkers(visitMarkers(cp.getMarkers(), newControlParens.getMarkers()));
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, Object ctx) {
        if (doWhileLoop == ctx || !(ctx instanceof J.DoWhileLoop)) {
            return doWhileLoop;
        }
        J.DoWhileLoop newDoWhileLoop = (J.DoWhileLoop) ctx;
        J.DoWhileLoop d = doWhileLoop;
        d = d.withPrefix(visitSpace(d.getPrefix(), Space.Location.DO_WHILE_PREFIX, newDoWhileLoop.getPrefix()));
        d = d.withMarkers(visitMarkers(d.getMarkers(), newDoWhileLoop.getMarkers()));
        Statement temp = (Statement) visitStatement(d, newDoWhileLoop);
        if (!(temp instanceof J.DoWhileLoop)) {
            return temp;
        }
        d = (J.DoWhileLoop) temp;
        d = d.getPadding().withWhileCondition(visitLeftPadded(d.getPadding().getWhileCondition(), JLeftPadded.Location.WHILE_CONDITION, newDoWhileLoop.getPadding().getWhileCondition()));
        return d.getPadding().withBody(visitRightPadded(d.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, newDoWhileLoop.getPadding().getBody()));
    }

    @Override
    public J visitEmpty(J.Empty empty, Object ctx) {
        if (empty == ctx || !(ctx instanceof J.Empty)) {
            return empty;
        }
        J.Empty newEmpty = (J.Empty) ctx;
        J.Empty e = empty;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.EMPTY_PREFIX, newEmpty.getPrefix()));
        e = e.withMarkers(visitMarkers(e.getMarkers(), newEmpty.getMarkers()));
        Statement temp = (Statement) visitStatement(e, newEmpty);
        if (!(temp instanceof J.Empty)) {
            return temp;
        }
        return visitExpression((J.Empty) temp, newEmpty);
    }

    @Override
    public J visitEnumValue(J.EnumValue enum_, Object ctx) {
        if (enum_ == ctx || !(ctx instanceof J.EnumValue)) {
            return enum_;
        }
        J.EnumValue newEnum = (J.EnumValue) ctx;
        J.EnumValue e = enum_;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.ENUM_VALUE_PREFIX, newEnum.getPrefix()));
        e = e.withMarkers(visitMarkers(e.getMarkers(), newEnum.getMarkers()));
        e = e.withAnnotations(ListUtils.map(e.getAnnotations(), (index, a) -> visitAndCast(a, newEnum.getAnnotations().get(index))));
        e = e.withName(visitAndCast(e.getName(), newEnum.getName()));
        return e.withInitializer(visitAndCast(e.getInitializer(), newEnum.getInitializer()));
    }

    @Override
    public J visitEnumValueSet(J.EnumValueSet enums, Object ctx) {
        if (enums == ctx || !(ctx instanceof J.EnumValueSet)) {
            return enums;
        }
        J.EnumValueSet newEnums = (J.EnumValueSet) ctx;
        J.EnumValueSet e = enums;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.ENUM_VALUE_SET_PREFIX, newEnums.getPrefix()));
        e = e.withMarkers(visitMarkers(e.getMarkers(), newEnums.getMarkers()));
        Statement temp = (Statement) visitStatement(e, newEnums);
        if (!(temp instanceof J.EnumValueSet)) {
            return temp;
        }
        e = (J.EnumValueSet) temp;
        return e.getPadding().withEnums(ListUtils.map(e.getPadding().getEnums(), (index, t) -> visitRightPadded(t, JRightPadded.Location.ENUM_VALUE, newEnums.getPadding().getEnums().get(index))));
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, Object ctx) {
        if (fieldAccess == ctx || !(ctx instanceof J.FieldAccess)) {
            return fieldAccess;
        }
        J.FieldAccess newFieldAccess = (J.FieldAccess) ctx;
        J.FieldAccess f = fieldAccess;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FIELD_ACCESS_PREFIX, newFieldAccess.getPrefix()));
        f = f.withMarkers(visitMarkers(f.getMarkers(), newFieldAccess.getMarkers()));
        f = visitTypeName(f, newFieldAccess);
        Expression temp = (Expression) visitExpression(f, newFieldAccess);
        if (!(temp instanceof J.FieldAccess)) {
            return temp;
        }
        f = (J.FieldAccess) temp;
        Statement tempStat = (Statement) visitStatement(f, newFieldAccess);
        if (!(tempStat instanceof J.FieldAccess)) {
            return tempStat;
        }
        f = (J.FieldAccess) tempStat;
        f = f.withTarget(visitAndCast(f.getTarget(), newFieldAccess.getTarget()));
        f = f.getPadding().withName(visitLeftPadded(f.getPadding().getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, newFieldAccess.getPadding().getName()));
        return f.withType(visitType(f.getType(), newFieldAccess.getType()));
    }

    @Override
    public J visitForEachLoop(J.ForEachLoop forLoop, Object ctx) {
        if (forLoop == ctx || !(ctx instanceof J.ForEachLoop)) {
            return forLoop;
        }
        J.ForEachLoop newForLoop = (J.ForEachLoop) ctx;
        J.ForEachLoop f = forLoop;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FOR_EACH_LOOP_PREFIX, newForLoop.getPrefix()));
        f = f.withMarkers(visitMarkers(f.getMarkers(), newForLoop.getMarkers()));
        Statement temp = (Statement) visitStatement(f, newForLoop);
        if (!(temp instanceof J.ForEachLoop)) {
            return temp;
        }
        f = (J.ForEachLoop) temp;
        f = f.withControl(visitAndCast(f.getControl(), newForLoop.getControl()));
        return f.getPadding().withBody(visitRightPadded(f.getPadding().getBody(), JRightPadded.Location.FOR_BODY, newForLoop.getPadding().getBody()));
    }

    @Override
    public J visitForEachControl(J.ForEachLoop.Control control, Object ctx) {
        if (control == ctx || !(ctx instanceof J.ForEachLoop.Control)) {
            return control;
        }
        J.ForEachLoop.Control newControl = (J.ForEachLoop.Control) ctx;
        J.ForEachLoop.Control c = control;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.FOR_EACH_CONTROL_PREFIX, newControl.getPrefix()));
        c = c.withMarkers(visitMarkers(c.getMarkers(), newControl.getMarkers()));
        c = c.getPadding().withVariable(visitRightPadded(c.getPadding().getVariable(), JRightPadded.Location.FOREACH_VARIABLE, newControl.getPadding().getVariable()));
        return c.getPadding().withIterable(visitRightPadded(c.getPadding().getIterable(), JRightPadded.Location.FOREACH_ITERABLE, newControl.getPadding().getIterable()));
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop, Object ctx) {
        if (forLoop == ctx || !(ctx instanceof J.ForLoop)) {
            return forLoop;
        }
        J.ForLoop newForLoop = (J.ForLoop) ctx;
        J.ForLoop f = forLoop;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FOR_PREFIX, newForLoop.getPrefix()));
        f = f.withMarkers(visitMarkers(f.getMarkers(), newForLoop.getMarkers()));
        Statement temp = (Statement) visitStatement(f, newForLoop);
        if (!(temp instanceof J.ForLoop)) {
            return temp;
        }
        f = (J.ForLoop) temp;
        f = f.withControl(visitAndCast(f.getControl(), newForLoop.getControl()));
        return f.getPadding().withBody(visitRightPadded(f.getPadding().getBody(), JRightPadded.Location.FOR_BODY, newForLoop.getPadding().getBody()));
    }

    @Override
    public J visitForControl(J.ForLoop.Control control, Object ctx) {
        if (control == ctx || !(ctx instanceof J.ForLoop.Control)) {
            return control;
        }
        J.ForLoop.Control newControl = (J.ForLoop.Control) ctx;
        J.ForLoop.Control c = control;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.FOR_CONTROL_PREFIX, newControl.getPrefix()));
        c = c.withMarkers(visitMarkers(c.getMarkers(), newControl.getMarkers()));
        c = c.getPadding().withInit(ListUtils.map(c.getPadding().getInit(), (index, t) -> visitRightPadded(t, JRightPadded.Location.FOR_INIT, newControl.getPadding().getInit().get(index))));
        c = c.getPadding().withCondition(visitRightPadded(c.getPadding().getCondition(), JRightPadded.Location.FOR_CONDITION, newControl.getPadding().getCondition()));
        return c.getPadding().withUpdate(ListUtils.map(c.getPadding().getUpdate(), (index, t) -> visitRightPadded(t, JRightPadded.Location.FOR_UPDATE, newControl.getPadding().getUpdate().get(index))));
    }

    @Override
    public J visitParenthesizedTypeTree(J.ParenthesizedTypeTree parTree, Object ctx) {
        if (parTree == ctx || !(ctx instanceof J.ParenthesizedTypeTree)) {
            return parTree;
        }
        J.ParenthesizedTypeTree newParTree = (J.ParenthesizedTypeTree) ctx;
        J.ParenthesizedTypeTree t = parTree;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.PARENTHESES_PREFIX, newParTree.getPrefix()));
        t = t.withMarkers(visitMarkers(t.getMarkers(), newParTree.getMarkers()));
        if (t.getAnnotations() != null && !t.getAnnotations().isEmpty()) {
            t = t.withAnnotations(ListUtils.map(t.getAnnotations(), (index, a) -> visitAndCast(a, newParTree.getAnnotations().get(index))));
        }

        J temp = visitParentheses(t.getParenthesizedType(), newParTree.getParenthesizedType());
        if (!(temp instanceof J.Parentheses)) {
            return temp;
        }
        //noinspection unchecked
        return t.withParenthesizedType((J.Parentheses<TypeTree>) temp);
    }

    @Override
    public J visitIdentifier(J.Identifier ident, Object ctx) {
        if (ident == ctx || !(ctx instanceof J.Identifier)) {
            return ident;
        }
        J.Identifier newIdent = (J.Identifier) ctx;
        J.Identifier i = ident;
        if (i.getAnnotations() != null && !i.getAnnotations().isEmpty()) {
            // performance optimization
            i = i.withAnnotations(ListUtils.map(i.getAnnotations(), (index, a) -> visitAndCast(a, newIdent.getAnnotations().get(index))));
        }
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.IDENTIFIER_PREFIX, newIdent.getPrefix()));
        i = i.withMarkers(visitMarkers(i.getMarkers(), newIdent.getMarkers()));
        Expression temp = (Expression) visitExpression(i, newIdent);
        if (!(temp instanceof J.Identifier)) {
            return temp;
        }
        i = (J.Identifier) temp;
        i = i.withType(visitType(i.getType(), newIdent.getType()));
        return i.withFieldType((JavaType.Variable) visitType(i.getFieldType(), newIdent.getFieldType()));
    }

    @Override
    public J visitElse(J.If.Else else_, Object ctx) {
        if (else_ == ctx || !(ctx instanceof J.If.Else)) {
            return else_;
        }
        J.If.Else newElse = (J.If.Else) ctx;
        J.If.Else e = else_;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.ELSE_PREFIX, newElse.getPrefix()));
        e = e.withMarkers(visitMarkers(e.getMarkers(), newElse.getMarkers()));
        return e.getPadding().withBody(visitRightPadded(e.getPadding().getBody(), JRightPadded.Location.IF_ELSE, newElse.getPadding().getBody()));
    }

    @Override
    public J visitIf(J.If iff, Object ctx) {
        if (iff == ctx || !(ctx instanceof J.If)) {
            return iff;
        }
        J.If newIff = (J.If) ctx;
        J.If i = iff;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.IF_PREFIX, newIff.getPrefix()));
        i = i.withMarkers(visitMarkers(i.getMarkers(), newIff.getMarkers()));
        Statement temp = (Statement) visitStatement(i, newIff);
        if (!(temp instanceof J.If)) {
            return temp;
        }
        i = (J.If) temp;
        i = i.withIfCondition(visitAndCast(i.getIfCondition(), newIff.getIfCondition()));
        i = i.getPadding().withThenPart(visitRightPadded(i.getPadding().getThenPart(), JRightPadded.Location.IF_THEN, newIff.getPadding().getThenPart()));
        return i.withElsePart(visitAndCast(i.getElsePart(), newIff.getElsePart()));
    }

    @Override
    public J visitImport(J.Import import_, Object ctx) {
        if (import_ == ctx || !(ctx instanceof J.Import)) {
            return import_;
        }
        J.Import newImport = (J.Import) ctx;
        J.Import i = import_;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.IMPORT_PREFIX, newImport.getPrefix()));
        i = i.withMarkers(visitMarkers(i.getMarkers(), newImport.getMarkers()));
        i = i.getPadding().withStatic(visitLeftPadded(i.getPadding().getStatic(), JLeftPadded.Location.STATIC_IMPORT, newImport.getPadding().getStatic()));
        i = i.withQualid(visitAndCast(i.getQualid(), newImport.getQualid()));
        return i.getPadding().withAlias(visitLeftPadded(i.getPadding().getAlias(), JLeftPadded.Location.IMPORT_ALIAS_PREFIX, newImport.getPadding().getAlias()));
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, Object ctx) {
        if (instanceOf == ctx || !(ctx instanceof J.InstanceOf)) {
            return instanceOf;
        }
        J.InstanceOf newInstanceOf = (J.InstanceOf) ctx;
        J.InstanceOf i = instanceOf;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.INSTANCEOF_PREFIX, newInstanceOf.getPrefix()));
        i = i.withMarkers(visitMarkers(i.getMarkers(), newInstanceOf.getMarkers()));
        Expression temp = (Expression) visitExpression(i, newInstanceOf);
        if (!(temp instanceof J.InstanceOf)) {
            return temp;
        }
        i = (J.InstanceOf) temp;
        i = i.getPadding().withExpression(visitRightPadded(i.getPadding().getExpression(), JRightPadded.Location.INSTANCEOF, newInstanceOf.getPadding().getExpression()));
        i = i.withClazz(visitAndCast(i.getClazz(), newInstanceOf.getClazz()));
        i = i.withPattern(visitAndCast(i.getPattern(), newInstanceOf.getPattern()));
        return i.withType(visitType(i.getType(), newInstanceOf.getType()));
    }

    @Override
    public J visitDeconstructionPattern(J.DeconstructionPattern deconstructionPattern, Object ctx) {
        if (deconstructionPattern == ctx || !(ctx instanceof J.DeconstructionPattern)) {
            return deconstructionPattern;
        }
        J.DeconstructionPattern newDeconstructionPattern = (J.DeconstructionPattern) ctx;
        J.DeconstructionPattern d = deconstructionPattern;
        d = d.withPrefix(visitSpace(d.getPrefix(), Space.Location.DECONSTRUCTION_PATTERN_PREFIX, newDeconstructionPattern.getPrefix()));
        d = d.withMarkers(visitMarkers(d.getMarkers(), newDeconstructionPattern.getMarkers()));
        d = d.withDeconstructor(visitAndCast(d.getDeconstructor(), newDeconstructionPattern.getDeconstructor()));
        d = d.getPadding().withNested(visitContainer(d.getPadding().getNested(), JContainer.Location.DECONSTRUCTION_PATTERN_NESTED, newDeconstructionPattern.getPadding().getNested()));
        return d.withType(visitType(d.getType(), newDeconstructionPattern.getType()));

    }

    @Override
    public J visitIntersectionType(J.IntersectionType intersectionType, Object ctx) {
        if (intersectionType == ctx || !(ctx instanceof J.IntersectionType)) {
            return intersectionType;
        }
        J.IntersectionType newIntersectionType = (J.IntersectionType) ctx;
        J.IntersectionType i = intersectionType;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.INTERSECTION_TYPE_PREFIX, newIntersectionType.getPrefix()));
        i = i.withMarkers(visitMarkers(i.getMarkers(), newIntersectionType.getMarkers()));
        i = i.getPadding().withBounds(visitContainer(i.getPadding().getBounds(), JContainer.Location.TYPE_BOUNDS, newIntersectionType.getPadding().getBounds()));
        return i.withType(visitType(i.getType(), newIntersectionType.getType()));
    }

    @Override
    public J visitLabel(J.Label label, Object ctx) {
        if (label == ctx || !(ctx instanceof J.Label)) {
            return label;
        }
        J.Label newLabel = (J.Label) ctx;
        J.Label l = label;
        l = l.withPrefix(visitSpace(l.getPrefix(), Space.Location.LABEL_PREFIX, newLabel.getPrefix()));
        l = l.withMarkers(visitMarkers(l.getMarkers(), newLabel.getMarkers()));
        Statement temp = (Statement) visitStatement(l, newLabel);
        if (!(temp instanceof J.Label)) {
            return temp;
        }
        l = (J.Label) temp;
        l = l.getPadding().withLabel(visitRightPadded(l.getPadding().getLabel(), JRightPadded.Location.LABEL, newLabel.getPadding().getLabel()));
        return l.withStatement(visitAndCast(l.getStatement(), newLabel.getStatement()));
    }

    @Override
    public J visitLambda(J.Lambda lambda, Object ctx) {
        if (lambda == ctx || !(ctx instanceof J.Lambda)) {
            return lambda;
        }
        J.Lambda newLambda = (J.Lambda) ctx;
        J.Lambda l = lambda;
        l = l.withPrefix(visitSpace(l.getPrefix(), Space.Location.LAMBDA_PREFIX, newLambda.getPrefix()));
        l = l.withMarkers(visitMarkers(l.getMarkers(), newLambda.getMarkers()));
        Expression temp = (Expression) visitExpression(l, newLambda);
        if (!(temp instanceof J.Lambda)) {
            return temp;
        }
        l = (J.Lambda) temp;
        l = l.withParameters(visitAndCast(l.getParameters(), newLambda.getParameters()));
        l = l.withArrow(visitSpace(l.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, newLambda.getArrow()));
        l = l.withBody(visitAndCast(l.getBody(), newLambda.getBody()));
        return l.withType(visitType(l.getType(), newLambda.getType()));
    }

    @Override
    public J visitLambdaParameters(J.Lambda.Parameters parameters, Object ctx) {
        if (parameters == ctx || !(ctx instanceof J.Lambda.Parameters)) {
            return parameters;
        }
        J.Lambda.Parameters newParameters = (J.Lambda.Parameters) ctx;
        J.Lambda.Parameters params = parameters;
        params = params.withPrefix(visitSpace(params.getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, newParameters.getPrefix()));
        params = params.withMarkers(visitMarkers(params.getMarkers(), newParameters.getMarkers()));
        return params.getPadding().withParameters(
                ListUtils.map(params.getPadding().getParameters(), (index, param) ->
                        visitRightPadded(param, JRightPadded.Location.LAMBDA_PARAM, newParameters.getPadding().getParameters().get(index))
                )
        );
    }

    @Override
    public J visitLiteral(J.Literal literal, Object ctx) {
        if (literal == ctx || !(ctx instanceof J.Literal)) {
            return literal;
        }
        J.Literal newLiteral = (J.Literal) ctx;
        J.Literal l = literal;
        l = l.withPrefix(visitSpace(l.getPrefix(), Space.Location.LITERAL_PREFIX, newLiteral.getPrefix()));
        l = l.withMarkers(visitMarkers(l.getMarkers(), newLiteral.getMarkers()));
        Expression temp = (Expression) visitExpression(l, newLiteral);
        if (!(temp instanceof J.Literal)) {
            return temp;
        }
        l = (J.Literal) temp;
        return l.withType(visitType(l.getType(), newLiteral.getType()));
    }

    @Override
    public J visitMemberReference(J.MemberReference memberRef, Object ctx) {
        if (memberRef == ctx || !(ctx instanceof J.MemberReference)) {
            return memberRef;
        }
        J.MemberReference newMemberRef = (J.MemberReference) ctx;
        J.MemberReference m = memberRef;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.MEMBER_REFERENCE_PREFIX, newMemberRef.getPrefix()));
        m = m.withMarkers(visitMarkers(m.getMarkers(), newMemberRef.getMarkers()));
        Expression temp = (Expression) visitExpression(m, newMemberRef);
        if (!(temp instanceof J.MemberReference)) {
            return temp;
        }
        m = (J.MemberReference) temp;
        m = m.getPadding().withContaining(visitRightPadded(m.getPadding().getContaining(), JRightPadded.Location.MEMBER_REFERENCE_CONTAINING, newMemberRef.getPadding().getContaining()));
        if (m.getPadding().getTypeParameters() != null && newMemberRef.getPadding().getTypeParameters() != null) {
            m = m.getPadding().withTypeParameters(visitContainer(m.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, newMemberRef.getPadding().getTypeParameters()));
        }
        m = m.getPadding().withReference(visitLeftPadded(m.getPadding().getReference(), JLeftPadded.Location.MEMBER_REFERENCE_NAME, newMemberRef.getPadding().getReference()));
        m = m.withType(visitType(m.getType(), newMemberRef.getType()));
        m = m.withMethodType((JavaType.Method) visitType(m.getMethodType(), newMemberRef.getMethodType()));
        return m.withVariableType((JavaType.Variable) visitType(m.getVariableType(), newMemberRef.getVariableType()));
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, Object ctx) {
        if (method == ctx || !(ctx instanceof J.MethodDeclaration)) {
            return method;
        }
        J.MethodDeclaration newMethod = (J.MethodDeclaration) ctx;
        J.MethodDeclaration m = method;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.METHOD_DECLARATION_PREFIX, newMethod.getPrefix()));
        m = m.withMarkers(visitMarkers(m.getMarkers(), newMethod.getMarkers()));
        Statement temp = (Statement) visitStatement(m, newMethod);
        if (!(temp instanceof J.MethodDeclaration)) {
            return temp;
        }
        m = (J.MethodDeclaration) temp;
        m = m.withLeadingAnnotations(ListUtils.map(m.getLeadingAnnotations(), (index, a) -> visitAndCast(a, newMethod.getLeadingAnnotations().get(index))));
        m = m.withModifiers(ListUtils.map(m.getModifiers(), (index, e) -> visitAndCast(e, newMethod.getModifiers().get(index))));
        m = m.getAnnotations().withTypeParameters((J.TypeParameters) visit(m.getAnnotations().getTypeParameters(), newMethod.getAnnotations().getTypeParameters()));
        m = m.withReturnTypeExpression(visitAndCast(m.getReturnTypeExpression(), newMethod.getReturnTypeExpression()));
        m = m.withReturnTypeExpression(
                m.getReturnTypeExpression() == null ?
                        null :
                        visitTypeName(m.getReturnTypeExpression(), newMethod.getReturnTypeExpression()));
        m = m.getAnnotations().withName(m.getAnnotations().getName().withAnnotations(ListUtils.map(m.getAnnotations().getName().getAnnotations(), (index, a) -> visitAndCast(a, newMethod.getAnnotations().getName().getAnnotations().get(index)))));
        m = m.withName((J.Identifier) visitNonNull(m.getName(), newMethod.getName()));
        m = m.getPadding().withParameters(visitContainer(m.getPadding().getParameters(), JContainer.Location.METHOD_DECLARATION_PARAMETERS, newMethod.getPadding().getParameters()));
        if (m.getPadding().getThrows() != null) {
            m = m.getPadding().withThrows(visitContainer(m.getPadding().getThrows(), JContainer.Location.THROWS, newMethod.getPadding().getThrows()));
        }
        m = m.getPadding().withThrows(visitTypeNames(m.getPadding().getThrows(), newMethod.getPadding().getThrows()));
        m = m.withBody(visitAndCast(m.getBody(), newMethod.getBody()));
        if (m.getPadding().getDefaultValue() != null) {
            m = m.getPadding().withDefaultValue(visitLeftPadded(m.getPadding().getDefaultValue(), JLeftPadded.Location.METHOD_DECLARATION_DEFAULT_VALUE, newMethod.getPadding().getDefaultValue()));
        }
        return m.withMethodType((JavaType.Method) visitType(m.getMethodType(), newMethod.getMethodType()));
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, Object ctx) {
        if (method == ctx || !(ctx instanceof J.MethodInvocation)) {
            return method;
        }
        J.MethodInvocation newMethod = (J.MethodInvocation) ctx;
        J.MethodInvocation m = method;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.METHOD_INVOCATION_PREFIX, newMethod.getPrefix()));
        m = m.withMarkers(visitMarkers(m.getMarkers(), newMethod.getMarkers()));
        Statement temp = (Statement) visitStatement(m, newMethod);
        if (!(temp instanceof J.MethodInvocation)) {
            return temp;
        }
        m = (J.MethodInvocation) temp;
        Expression temp2 = (Expression) visitExpression(m, newMethod);
        if (!(temp2 instanceof J.MethodInvocation)) {
            return temp2;
        }
        m = (J.MethodInvocation) temp2;
        if (m.getPadding().getSelect() != null && m.getPadding().getSelect().getElement() instanceof NameTree &&
                method.getMethodType() != null && method.getMethodType().hasFlags(Flag.Static)) {
            //noinspection unchecked
            m = m.getPadding().withSelect(
                    (JRightPadded<Expression>) (JRightPadded<?>)
                            visitTypeName((JRightPadded<NameTree>) (JRightPadded<?>) m.getPadding().getSelect(), newMethod.getPadding().getSelect()));
        }
        if (m.getPadding().getSelect() != null) {
            m = m.getPadding().withSelect(visitRightPadded(m.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, newMethod.getPadding().getSelect()));
        }
        if (m.getPadding().getTypeParameters() != null) {
            m = m.getPadding().withTypeParameters(visitContainer(m.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, newMethod.getPadding().getTypeParameters()));
        }
        m = m.getPadding().withTypeParameters(visitTypeNames(m.getPadding().getTypeParameters(), newMethod.getPadding().getTypeParameters()));
        m = m.withName((J.Identifier) visitNonNull(m.getName(), newMethod.getName()));
        m = m.getPadding().withArguments(visitContainer(m.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, newMethod.getPadding().getArguments()));
        return m.withMethodType((JavaType.Method) visitType(m.getMethodType(), newMethod.getMethodType()));
    }

    @Override
    public J visitModifier(J.Modifier modifer, Object ctx) {
        if (modifer == ctx || !(ctx instanceof J.Modifier)) {
            return modifer;
        }
        J.Modifier newModifer = (J.Modifier) ctx;
        J.Modifier m = modifer;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.MODIFIER_PREFIX, newModifer.getPrefix()));
        m = m.withMarkers(visitMarkers(m.getMarkers(), newModifer.getMarkers()));
        return m.withAnnotations(ListUtils.map(m.getAnnotations(), (index, a) -> visitAndCast(a, newModifer.getAnnotations().get(index))));
    }

    @Override
    public J visitMultiCatch(J.MultiCatch multiCatch, Object ctx) {
        if (multiCatch == ctx || !(ctx instanceof J.MultiCatch)) {
            return multiCatch;
        }
        J.MultiCatch newMultiCatch = (J.MultiCatch) ctx;
        J.MultiCatch m = multiCatch;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.MULTI_CATCH_PREFIX, newMultiCatch.getPrefix()));
        m = m.withMarkers(visitMarkers(m.getMarkers(), newMultiCatch.getMarkers()));
        return m.getPadding().withAlternatives(ListUtils.map(m.getPadding().getAlternatives(), (index, t) ->
                visitTypeName(visitRightPadded(t, JRightPadded.Location.CATCH_ALTERNATIVE, newMultiCatch.getPadding().getAlternatives().get(index)), newMultiCatch.getPadding().getAlternatives().get(index))));
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, Object ctx) {
        if (multiVariable == ctx || !(ctx instanceof J.VariableDeclarations)) {
            return multiVariable;
        }
        J.VariableDeclarations newMultiVariable = (J.VariableDeclarations) ctx;
        J.VariableDeclarations m = multiVariable;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, newMultiVariable.getPrefix()));
        m = m.withMarkers(visitMarkers(m.getMarkers(), newMultiVariable.getMarkers()));
        Statement temp = (Statement) visitStatement(m, newMultiVariable);
        if (!(temp instanceof J.VariableDeclarations)) {
            return temp;
        }
        m = (J.VariableDeclarations) temp;
        m = m.withLeadingAnnotations(ListUtils.map(m.getLeadingAnnotations(), (index, a) -> visitAndCast(a, newMultiVariable.getLeadingAnnotations().get(index))));
        m = m.withModifiers(Objects.requireNonNull(ListUtils.map(m.getModifiers(), (index, e) -> visitAndCast(e, newMultiVariable.getModifiers().get(index)))));
        m = m.withTypeExpression(visitAndCast(m.getTypeExpression(), newMultiVariable.getTypeExpression()));
        m = m.withTypeExpression(m.getTypeExpression() == null ?
                null :
                visitTypeName(m.getTypeExpression(), newMultiVariable.getTypeExpression()));
        m = m.withVarargs(m.getVarargs() == null ?
                null :
                visitSpace(m.getVarargs(), Space.Location.VARARGS, newMultiVariable.getVarargs()));
        return m.getPadding().withVariables(ListUtils.map(m.getPadding().getVariables(), (index, t) -> visitRightPadded(t, JRightPadded.Location.NAMED_VARIABLE, newMultiVariable.getPadding().getVariables().get(index))));
    }

    @Override
    public J visitNewArray(J.NewArray newArray, Object ctx) {
        if (newArray == ctx || !(ctx instanceof J.NewArray)) {
            return newArray;
        }
        J.NewArray newNewArray = (J.NewArray) ctx;
        J.NewArray n = newArray;
        n = n.withPrefix(visitSpace(n.getPrefix(), Space.Location.NEW_ARRAY_PREFIX, newNewArray.getPrefix()));
        n = n.withMarkers(visitMarkers(n.getMarkers(), newNewArray.getMarkers()));
        Expression temp = (Expression) visitExpression(n, newNewArray);
        if (!(temp instanceof J.NewArray)) {
            return temp;
        }
        n = (J.NewArray) temp;
        n = n.withTypeExpression(visitAndCast(n.getTypeExpression(), newNewArray.getTypeExpression()));
        n = n.withTypeExpression(n.getTypeExpression() == null ?
                null :
                visitTypeName(n.getTypeExpression(), newNewArray.getTypeExpression()));
        n = n.withDimensions(ListUtils.map(n.getDimensions(), (index, d) -> visitAndCast(d, newNewArray.getDimensions().get(index))));
        if (n.getPadding().getInitializer() != null) {
            n = n.getPadding().withInitializer(visitContainer(n.getPadding().getInitializer(), JContainer.Location.NEW_ARRAY_INITIALIZER, newNewArray.getPadding().getInitializer()));
        }
        return n.withType(visitType(n.getType(), newNewArray.getType()));
    }

    @Override
    public J visitNewClass(J.NewClass newClass, Object ctx) {
        if (newClass == ctx || !(ctx instanceof J.NewClass)) {
            return newClass;
        }
        J.NewClass newNewClass = (J.NewClass) ctx;
        J.NewClass n = newClass;
        n = n.withPrefix(visitSpace(n.getPrefix(), Space.Location.NEW_CLASS_PREFIX, newNewClass.getPrefix()));
        n = n.withMarkers(visitMarkers(n.getMarkers(), newNewClass.getMarkers()));
        if (n.getPadding().getEnclosing() != null) {
            n = n.getPadding().withEnclosing(visitRightPadded(n.getPadding().getEnclosing(), JRightPadded.Location.NEW_CLASS_ENCLOSING, newNewClass.getPadding().getEnclosing()));
        }
        Statement temp = (Statement) visitStatement(n, newNewClass);
        if (!(temp instanceof J.NewClass)) {
            return temp;
        }
        n = (J.NewClass) temp;
        Expression temp2 = (Expression) visitExpression(n, newNewClass);
        if (!(temp2 instanceof J.NewClass)) {
            return temp2;
        }
        n = (J.NewClass) temp2;
        n = n.withNew(visitSpace(n.getNew(), Space.Location.NEW_PREFIX, newNewClass.getNew()));
        n = n.withClazz(visitAndCast(n.getClazz(), newNewClass.getClazz()));
        n = n.withClazz(n.getClazz() == null ? null : visitTypeName(n.getClazz(), newNewClass.getClazz()));
        n = n.getPadding().withArguments(visitContainer(n.getPadding().getArguments(), JContainer.Location.NEW_CLASS_ARGUMENTS, newNewClass.getPadding().getArguments()));
        n = n.withBody(visitAndCast(n.getBody(), newNewClass.getBody()));
        return n.withConstructorType((JavaType.Method) visitType(n.getConstructorType(), newNewClass.getConstructorType()));
    }

    @Override
    public J visitNullableType(J.NullableType nullableType, Object ctx) {
        if (nullableType == ctx || !(ctx instanceof J.NullableType)) {
            return nullableType;
        }
        J.NullableType newNullableType = (J.NullableType) ctx;
        J.NullableType nt = nullableType;
        nt = nt.withPrefix(visitSpace(nt.getPrefix(), Space.Location.NULLABLE_TYPE_PREFIX, newNullableType.getPrefix()));
        nt = nt.withMarkers(visitMarkers(nt.getMarkers(), newNullableType.getMarkers()));
        nt = nt.withAnnotations(ListUtils.map(nt.getAnnotations(), (index, a) -> visitAndCast(a, newNullableType.getAnnotations().get(index))));

        Expression temp = (Expression) visitExpression(nt, newNullableType);
        if (!(temp instanceof J.NullableType)) {
            return temp;
        }
        nt = (J.NullableType) temp;

        nt = nt.getPadding().withTypeTree(visitRightPadded(nt.getPadding().getTypeTree(), JRightPadded.Location.NULLABLE, newNullableType.getPadding().getTypeTree()));
        return nt.withType(visitType(nt.getType(), newNullableType.getType()));
    }

    @Override
    public J visitPackage(J.Package pkg, Object ctx) {
        if (pkg == ctx || !(ctx instanceof J.Package)) {
            return pkg;
        }
        J.Package newPkg = (J.Package) ctx;
        J.Package pa = pkg;
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), Space.Location.PACKAGE_PREFIX, newPkg.getPrefix()));
        pa = pa.withMarkers(visitMarkers(pa.getMarkers(), newPkg.getMarkers()));
        pa = pa.withExpression(visitAndCast(pa.getExpression(), newPkg.getExpression()));
        return pa.withAnnotations(ListUtils.map(pa.getAnnotations(), (index, a) -> visitAndCast(a, newPkg.getAnnotations().get(index))));
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type, Object ctx) {
        if (type == ctx || !(ctx instanceof J.ParameterizedType)) {
            return type;
        }
        J.ParameterizedType newType = (J.ParameterizedType) ctx;
        J.ParameterizedType pt = type;
        pt = pt.withPrefix(visitSpace(pt.getPrefix(), Space.Location.PARAMETERIZED_TYPE_PREFIX, newType.getPrefix()));
        pt = pt.withMarkers(visitMarkers(pt.getMarkers(), newType.getMarkers()));
        Expression temp = (Expression) visitExpression(pt, newType);
        if (!(temp instanceof J.ParameterizedType)) {
            return temp;
        }
        pt = (J.ParameterizedType) temp;
        pt = pt.withClazz(visitAndCast(pt.getClazz(), newType.getClazz()));
        if (pt.getPadding().getTypeParameters() != null) {
            pt = pt.getPadding().withTypeParameters(visitContainer(pt.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, newType.getPadding().getTypeParameters()));
        }
        pt = pt.getPadding().withTypeParameters(visitTypeNames(pt.getPadding().getTypeParameters(), newType.getPadding().getTypeParameters()));
        return pt.withType(visitType(pt.getType(), newType.getType()));
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens, Object ctx) {
        J.Parentheses<T> newParens = (J.Parentheses<T>) ctx;
        if (parens == newParens) {
            return parens;
        }
        J.Parentheses<T> pa = parens;
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), Space.Location.PARENTHESES_PREFIX, newParens.getPrefix()));
        pa = pa.withMarkers(visitMarkers(pa.getMarkers(), newParens.getMarkers()));
        Expression temp = (Expression) visitExpression(pa, newParens);
        if (!(temp instanceof J.Parentheses)) {
            return temp;
        }
        //noinspection unchecked
        pa = (J.Parentheses<T>) temp;
        return pa.getPadding().withTree(visitRightPadded(pa.getPadding().getTree(), JRightPadded.Location.PARENTHESES, newParens.getPadding().getTree()));
    }

    @Override
    public J visitPrimitive(J.Primitive primitive, Object ctx) {
        if (primitive == ctx || !(ctx instanceof J.Primitive)) {
            return primitive;
        }
        J.Primitive newPrimitive = (J.Primitive) ctx;
        J.Primitive pr = primitive;
        pr = pr.withPrefix(visitSpace(pr.getPrefix(), Space.Location.PRIMITIVE_PREFIX, newPrimitive.getPrefix()));
        pr = pr.withMarkers(visitMarkers(pr.getMarkers(), newPrimitive.getMarkers()));
        Expression temp = (Expression) visitExpression(pr, newPrimitive);
        if (!(temp instanceof J.Primitive)) {
            return temp;
        }
        pr = (J.Primitive) temp;
        return pr.withType(visitType(pr.getType(), newPrimitive.getType()));
    }

    @Override
    public J visitReturn(J.Return return_, Object ctx) {
        if (return_ == ctx || !(ctx instanceof J.Return)) {
            return return_;
        }
        J.Return newReturn = (J.Return) ctx;
        J.Return r = return_;
        r = r.withPrefix(visitSpace(r.getPrefix(), Space.Location.RETURN_PREFIX, newReturn.getPrefix()));
        r = r.withMarkers(visitMarkers(r.getMarkers(), newReturn.getMarkers()));
        Statement temp = (Statement) visitStatement(r, newReturn);
        if (!(temp instanceof J.Return)) {
            return temp;
        }
        r = (J.Return) temp;
        return r.withExpression(visitAndCast(r.getExpression(), newReturn.getExpression()));
    }

    @Override
    public J visitSwitch(J.Switch switch_, Object ctx) {
        if (switch_ == ctx || !(ctx instanceof J.Switch)) {
            return switch_;
        }
        J.Switch newSwitch = (J.Switch) ctx;
        J.Switch s = switch_;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.SWITCH_PREFIX, newSwitch.getPrefix()));
        s = s.withMarkers(visitMarkers(s.getMarkers(), newSwitch.getMarkers()));
        Statement temp = (Statement) visitStatement(s, newSwitch);
        if (!(temp instanceof J.Switch)) {
            return temp;
        }
        s = (J.Switch) temp;
        s = s.withSelector(visitAndCast(s.getSelector(), newSwitch.getSelector()));
        return s.withCases(visitAndCast(s.getCases(), newSwitch.getCases()));
    }

    @Override
    public J visitSwitchExpression(J.SwitchExpression switch_, Object ctx) {
        if (switch_ == ctx || !(ctx instanceof J.SwitchExpression)) {
            return switch_;
        }
        J.SwitchExpression newSwitch = (J.SwitchExpression) ctx;
        J.SwitchExpression s = switch_;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.SWITCH_EXPRESSION_PREFIX, newSwitch.getPrefix()));
        s = s.withMarkers(visitMarkers(s.getMarkers(), newSwitch.getMarkers()));
        Expression temp = (Expression) visitExpression(s, newSwitch);
        if (!(temp instanceof J.SwitchExpression)) {
            return temp;
        }
        s = (J.SwitchExpression) temp;
        s = s.withSelector(visitAndCast(s.getSelector(), newSwitch.getSelector()));
        return s.withCases(visitAndCast(s.getCases(), newSwitch.getCases()));
    }

    @Override
    public J visitSynchronized(J.Synchronized synch, Object ctx) {
        if (synch == ctx || !(ctx instanceof J.Synchronized)) {
            return synch;
        }
        J.Synchronized newSynch = (J.Synchronized) ctx;
        J.Synchronized s = synch;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.SYNCHRONIZED_PREFIX, newSynch.getPrefix()));
        s = s.withMarkers(visitMarkers(s.getMarkers(), newSynch.getMarkers()));
        Statement temp = (Statement) visitStatement(s, newSynch);
        if (!(temp instanceof J.Synchronized)) {
            return temp;
        }
        s = (J.Synchronized) temp;
        s = s.withLock(visitAndCast(s.getLock(), newSynch.getLock()));
        return s.withBody(visitAndCast(s.getBody(), newSynch.getBody()));
    }

    @Override
    public J visitTernary(J.Ternary ternary, Object ctx) {
        if (ternary == ctx || !(ctx instanceof J.Ternary)) {
            return ternary;
        }
        J.Ternary newTernary = (J.Ternary) ctx;
        J.Ternary t = ternary;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TERNARY_PREFIX, newTernary.getPrefix()));
        t = t.withMarkers(visitMarkers(t.getMarkers(), newTernary.getMarkers()));
        Expression temp = (Expression) visitExpression(t, newTernary);
        if (!(temp instanceof J.Ternary)) {
            return temp;
        }
        t = (J.Ternary) temp;
        Statement tempStat = (Statement) visitStatement(t, newTernary);
        if (!(tempStat instanceof J.Ternary)) {
            return tempStat;
        }
        t = (J.Ternary) tempStat;
        t = t.withCondition(visitAndCast(t.getCondition(), newTernary.getCondition()));
        t = t.getPadding().withTruePart(visitLeftPadded(t.getPadding().getTruePart(), JLeftPadded.Location.TERNARY_TRUE, newTernary.getPadding().getTruePart()));
        t = t.getPadding().withFalsePart(visitLeftPadded(t.getPadding().getFalsePart(), JLeftPadded.Location.TERNARY_FALSE, newTernary.getPadding().getFalsePart()));
        return t.withType(visitType(t.getType(), newTernary.getType()));
    }

    @Override
    public J visitThrow(J.Throw thrown, Object ctx) {
        if (thrown == ctx || !(ctx instanceof J.Throw)) {
            return thrown;
        }
        J.Throw newThrown = (J.Throw) ctx;
        J.Throw t = thrown;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.THROW_PREFIX, newThrown.getPrefix()));
        t = t.withMarkers(visitMarkers(t.getMarkers(), newThrown.getMarkers()));
        Statement temp = (Statement) visitStatement(t, newThrown);
        if (!(temp instanceof J.Throw)) {
            return temp;
        }
        t = (J.Throw) temp;
        return t.withException(visitAndCast(t.getException(), newThrown.getException()));
    }

    @Override
    public J visitTry(J.Try tryable, Object ctx) {
        if (tryable == ctx || !(ctx instanceof J.Try)) {
            return tryable;
        }
        J.Try newTryable = (J.Try) ctx;
        J.Try t = tryable;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TRY_PREFIX, newTryable.getPrefix()));
        t = t.withMarkers(visitMarkers(t.getMarkers(), newTryable.getMarkers()));
        Statement temp = (Statement) visitStatement(t, newTryable);
        if (!(temp instanceof J.Try)) {
            return temp;
        }
        t = (J.Try) temp;
        if (t.getPadding().getResources() != null) {
            t = t.getPadding().withResources(visitContainer(t.getPadding().getResources(), JContainer.Location.TRY_RESOURCES, newTryable.getPadding().getResources()));
        }
        t = t.withBody(visitAndCast(t.getBody(), newTryable.getBody()));
        t = t.withCatches(ListUtils.map(t.getCatches(), (index, c) -> visitAndCast(c, newTryable.getCatches().get(index))));
        if (t.getPadding().getFinally() != null) {
            t = t.getPadding().withFinally(visitLeftPadded(t.getPadding().getFinally(), JLeftPadded.Location.TRY_FINALLY, newTryable.getPadding().getFinally()));
        }
        return t;
    }

    @Override
    public J visitTryResource(J.Try.Resource tryResource, Object ctx) {
        if (tryResource == ctx || !(ctx instanceof J.Try.Resource)) {
            return tryResource;
        }
        J.Try.Resource newTryResource = (J.Try.Resource) ctx;
        J.Try.Resource r = tryResource;
        r = tryResource.withPrefix(visitSpace(r.getPrefix(), Space.Location.TRY_RESOURCE, newTryResource.getPrefix()));
        r = r.withMarkers(visitMarkers(r.getMarkers(), newTryResource.getMarkers()));
        return r.withVariableDeclarations(visitAndCast(r.getVariableDeclarations(), newTryResource.getVariableDeclarations()));
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast, Object ctx) {
        if (typeCast == ctx || !(ctx instanceof J.TypeCast)) {
            return typeCast;
        }
        J.TypeCast newTypeCast = (J.TypeCast) ctx;
        J.TypeCast t = typeCast;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TYPE_CAST_PREFIX, newTypeCast.getPrefix()));
        t = t.withMarkers(visitMarkers(t.getMarkers(), newTypeCast.getMarkers()));
        Expression temp = (Expression) visitExpression(t, newTypeCast);
        if (!(temp instanceof J.TypeCast)) {
            return temp;
        }
        t = (J.TypeCast) temp;
        t = t.withClazz(visitAndCast(t.getClazz(), newTypeCast.getClazz()));
        t = t.withClazz(t.getClazz().withTree(visitTypeName(t.getClazz().getTree(), newTypeCast.getClazz().getTree())));
        return t.withExpression(visitAndCast(t.getExpression(), newTypeCast.getExpression()));
    }

    @Override
    public J visitTypeParameter(J.TypeParameter typeParam, Object ctx) {
        if (typeParam == ctx || !(ctx instanceof J.TypeParameter)) {
            return typeParam;
        }
        J.TypeParameter newTypeParam = (J.TypeParameter) ctx;
        J.TypeParameter t = typeParam;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TYPE_PARAMETERS_PREFIX, newTypeParam.getPrefix()));
        t = t.withMarkers(visitMarkers(t.getMarkers(), newTypeParam.getMarkers()));
        t = t.withAnnotations(ListUtils.map(t.getAnnotations(), (index, a) -> visitAndCast(a, newTypeParam.getAnnotations().get(index))));

        if (t.getModifiers() != null && !t.getModifiers().isEmpty()) {
            t = t.withModifiers(ListUtils.map(t.getModifiers(), (index, m) -> visitAndCast(m, newTypeParam.getModifiers().get(index))));
        }

        t = t.withName(visitAndCast(t.getName(), newTypeParam.getName()));
        if (t.getName() instanceof NameTree) {
            t = t.withName((Expression) visitTypeName((NameTree) t.getName(), (NameTree) newTypeParam.getName()));
        }
        if (t.getPadding().getBounds() != null) {
            t = t.getPadding().withBounds(visitContainer(t.getPadding().getBounds(), JContainer.Location.TYPE_BOUNDS, newTypeParam.getPadding().getBounds()));
        }
        return t.getPadding().withBounds(visitTypeNames(t.getPadding().getBounds(), newTypeParam.getPadding().getBounds()));
    }

    @Override
    public J visitTypeParameters(J.TypeParameters typeParameters, Object ctx) {
        if (typeParameters == ctx || !(ctx instanceof J.TypeParameters)) {
            return typeParameters;
        }
        J.TypeParameters newTypeParameters = (J.TypeParameters) ctx;
        J.TypeParameters t = typeParameters;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TYPE_PARAMETERS_PREFIX, newTypeParameters.getPrefix()));
        t = t.withMarkers(visitMarkers(t.getMarkers(), newTypeParameters.getMarkers()));
        t = t.withAnnotations(ListUtils.map(t.getAnnotations(), (index, a) -> visitAndCast(a, newTypeParameters.getAnnotations().get(index))));
        return t.getPadding().withTypeParameters(
                ListUtils.map(t.getPadding().getTypeParameters(),
                        (index, tp) -> visitRightPadded(tp, JRightPadded.Location.TYPE_PARAMETER, newTypeParameters.getPadding().getTypeParameters().get(index))
                )
        );
    }

    @Override
    public J visitUnary(J.Unary unary, Object ctx) {
        if (unary == ctx || !(ctx instanceof J.Unary)) {
            return unary;
        }
        J.Unary newUnary = (J.Unary) ctx;
        J.Unary u = unary;
        u = u.withPrefix(visitSpace(u.getPrefix(), Space.Location.UNARY_PREFIX, newUnary.getPrefix()));
        u = u.withMarkers(visitMarkers(u.getMarkers(), newUnary.getMarkers()));
        Statement temp = (Statement) visitStatement(u, newUnary);
        if (!(temp instanceof J.Unary)) {
            return temp;
        }
        u = (J.Unary) temp;
        Expression temp2 = (Expression) visitExpression(u, newUnary);
        if (!(temp2 instanceof J.Unary)) {
            return temp2;
        }
        u = (J.Unary) temp2;
        u = u.getPadding().withOperator(visitLeftPadded(u.getPadding().getOperator(), JLeftPadded.Location.UNARY_OPERATOR, newUnary.getPadding().getOperator()));
        u = u.withExpression(visitAndCast(u.getExpression(), newUnary.getExpression()));
        return u.withType(visitType(u.getType(), newUnary.getType()));
    }

    @Override
    public J visitUnknown(J.Unknown unknown, Object ctx) {
        if (unknown == ctx || !(ctx instanceof J.Unknown)) {
            return unknown;
        }
        J.Unknown newUnknown = (J.Unknown) ctx;
        J.Unknown u = unknown;
        u = u.withPrefix(visitSpace(u.getPrefix(), Space.Location.UNKNOWN_PREFIX, newUnknown.getPrefix()));
        u = u.withMarkers(visitMarkers(u.getMarkers(), newUnknown.getMarkers()));
        return u.withSource(visitAndCast(u.getSource(), newUnknown.getSource()));
    }

    @Override
    public J visitUnknownSource(J.Unknown.Source source, Object ctx) {
        if (source == ctx || !(ctx instanceof J.Unknown.Source)) {
            return source;
        }
        J.Unknown.Source newSource = (J.Unknown.Source) ctx;
        J.Unknown.Source s = source;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.UNKNOWN_SOURCE_PREFIX, newSource.getPrefix()));
        return s.withMarkers(visitMarkers(s.getMarkers(), newSource.getMarkers()));
    }

    @Override
    public J visitVariable(J.VariableDeclarations.NamedVariable variable, Object ctx) {
        if (variable == ctx || !(ctx instanceof J.VariableDeclarations.NamedVariable)) {
            return variable;
        }
        J.VariableDeclarations.NamedVariable newVariable = (J.VariableDeclarations.NamedVariable) ctx;
        J.VariableDeclarations.NamedVariable v = variable;
        v = v.withPrefix(visitSpace(v.getPrefix(), Space.Location.VARIABLE_PREFIX, newVariable.getPrefix()));
        v = v.withMarkers(visitMarkers(v.getMarkers(), newVariable.getMarkers()));
        v = v.withDeclarator(visitAndCast(v.getDeclarator(), newVariable.getDeclarator()));
        v = v.withDimensionsAfterName(
                ListUtils.map(v.getDimensionsAfterName(),
                        (index, dim) -> dim.withBefore(visitSpace(dim.getBefore(), Space.Location.DIMENSION_PREFIX, newVariable.getDimensionsAfterName().get(index).getBefore()))
                                .withElement(visitSpace(dim.getElement(), Space.Location.DIMENSION, newVariable.getDimensionsAfterName().get(index).getElement()))
                )
        );
        if (v.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(visitLeftPadded(v.getPadding().getInitializer(),
                    JLeftPadded.Location.VARIABLE_INITIALIZER, newVariable.getPadding().getInitializer()));
        }
        return v.withVariableType((JavaType.Variable) visitType(v.getVariableType(), newVariable.getVariableType()));
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop, Object ctx) {
        if (whileLoop == ctx || !(ctx instanceof J.WhileLoop)) {
            return whileLoop;
        }
        J.WhileLoop newWhileLoop = (J.WhileLoop) ctx;
        J.WhileLoop w = whileLoop;
        w = w.withPrefix(visitSpace(w.getPrefix(), Space.Location.WHILE_PREFIX, newWhileLoop.getPrefix()));
        w = w.withMarkers(visitMarkers(w.getMarkers(), newWhileLoop.getMarkers()));
        Statement temp = (Statement) visitStatement(w, newWhileLoop);
        if (!(temp instanceof J.WhileLoop)) {
            return temp;
        }
        w = (J.WhileLoop) temp;
        w = w.withCondition(visitAndCast(w.getCondition(), newWhileLoop.getCondition()));
        return w.getPadding().withBody(visitRightPadded(w.getPadding().getBody(), JRightPadded.Location.WHILE_BODY, newWhileLoop.getPadding().getBody()));
    }

    @Override
    public J visitWildcard(J.Wildcard wildcard, Object ctx) {
        if (wildcard == ctx || !(ctx instanceof J.Wildcard)) {
            return wildcard;
        }
        J.Wildcard newWildcard = (J.Wildcard) ctx;
        J.Wildcard w = wildcard;
        w = w.withPrefix(visitSpace(w.getPrefix(), Space.Location.WILDCARD_PREFIX, newWildcard.getPrefix()));
        w = w.withMarkers(visitMarkers(w.getMarkers(), newWildcard.getMarkers()));
        Expression temp = (Expression) visitExpression(w, newWildcard);
        if (!(temp instanceof J.Wildcard)) {
            return temp;
        }
        w = (J.Wildcard) temp;
        w = w.getPadding().withBound(visitLeftPadded(w.getPadding().getBound(), JLeftPadded.Location.WILDCARD_BOUND, newWildcard.getPadding().getBound()));
        w = w.withBoundedType(visitAndCast(w.getBoundedType(), newWildcard.getBoundedType()));
        if (w.getBoundedType() != null) {
            // i.e. not a "wildcard" type
            w = w.withBoundedType(visitTypeName(w.getBoundedType(), newWildcard.getBoundedType()));
        }
        return w;
    }

    @Override
    public J visitYield(J.Yield yield, Object ctx) {
        if (yield == ctx || !(ctx instanceof J.Yield)) {
            return yield;
        }
        J.Yield newYield = (J.Yield) ctx;
        J.Yield y = yield;
        y = y.withPrefix(visitSpace(y.getPrefix(), Space.Location.YIELD_PREFIX, newYield.getPrefix()));
        y = y.withMarkers(visitMarkers(y.getMarkers(), newYield.getMarkers()));
        Statement temp = (Statement) visitStatement(y, newYield);
        if (!(temp instanceof J.Yield)) {
            return temp;
        }
        y = (J.Yield) temp;
        return y.withValue(visitAndCast(y.getValue(), newYield.getValue()));
    }

    @Override
    public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, Object ctx) {
        JRightPadded<T> newRight = (JRightPadded<T>) ctx;
        if (right == newRight) {
            return right;
        }
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        if (t instanceof J) {
            //noinspection unchecked
            t = visitAndCast((J) right.getElement(), newRight.getElement());
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }

        Space after = visitSpace(right.getAfter(), loc.getAfterLocation(), newRight.getAfter());
        Markers markers = visitMarkers(right.getMarkers(), newRight.getMarkers());
        return (after == right.getAfter() && t == right.getElement() && markers == right.getMarkers()) ?
                right : new JRightPadded<>(t, after, markers);
    }

    @Override
    public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, JLeftPadded.Location loc, Object ctx) {
        JLeftPadded<T> newLeft = (JLeftPadded<T>) ctx;
        if (left == newLeft) {
            return left;
        }
        if (left == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), left));

        Space before = visitSpace(left.getBefore(), loc.getBeforeLocation(), newLeft.getBefore());
        T t = left.getElement();

        if (t instanceof J) {
            //noinspection unchecked
            t = visitAndCast((J) left.getElement(), newLeft.getElement());
        }

        setCursor(getCursor().getParent());
        // If nothing changed leave AST node the same
        if (left.getElement() == t && before == left.getBefore()) {
            return left;
        }

        //noinspection ConstantConditions
        return t == null ? null : new JLeftPadded<>(before, t, left.getMarkers());
    }

    @Override
    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container,
                                                                  JContainer.Location loc, Object ctx) {
        JContainer<J2> newContainer = (JContainer<J2>) ctx;
        if (container == newContainer) {
            return container;
        }
        if (container == null) {
            //noinspection ConstantConditions
            return null;
        }
        setCursor(new Cursor(getCursor(), container));

        Space before = visitSpace(container.getBefore(), loc.getBeforeLocation(), newContainer.getBefore());
        List<JRightPadded<J2>> js = ListUtils.map(container.getPadding().getElements(), (index, t) -> visitRightPadded(t, loc.getElementLocation(), newContainer == null ? null : newContainer.getPadding().getElements().get(index)));

        setCursor(getCursor().getParent());

        return js == container.getPadding().getElements() && before == container.getBefore() ?
                container :
                JContainer.build(before, js, container.getMarkers());
    }

    @Override
    public J visitErroneous(J.Erroneous erroneous, Object ctx) {
        if (erroneous == ctx || !(ctx instanceof J.Erroneous)) {
            return erroneous;
        }
        J.Erroneous newErroneous = (J.Erroneous) ctx;
        J.Erroneous u = erroneous;
        u = u.withPrefix(visitSpace(u.getPrefix(), Space.Location.ERRONEOUS, newErroneous.getPrefix()));
        return u.withMarkers(visitMarkers(u.getMarkers(), newErroneous.getMarkers()));
    }
}
