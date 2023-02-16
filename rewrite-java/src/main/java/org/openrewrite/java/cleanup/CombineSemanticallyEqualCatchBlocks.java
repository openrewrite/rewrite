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
package org.openrewrite.java.cleanup;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyMap;

@Incubating(since = "7.25.0")
public class CombineSemanticallyEqualCatchBlocks extends Recipe {

    @Override
    public String getDisplayName() {
        return "Combine semantically equal catch blocks";
    }

    @Override
    public String getDescription() {
        return "Combine catches in a try that contain semantically equivalent blocks. " +
                "No change will be made when a caught exception exists if combing catches may change application behavior or type attribution is missing.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2147");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new CombineSemanticallyEqualCatchBlocksVisitor();
    }

    private static class CombineSemanticallyEqualCatchBlocksVisitor extends JavaVisitor<ExecutionContext> {

        @Override
        public J visitTry(J.Try tryable, ExecutionContext executionContext) {
            J.Try t = (J.Try) super.visitTry(tryable, executionContext);
            Map<J.Try.Catch, List<J.Try.Catch>> semanticallyEqualCatchesMap = new LinkedHashMap<>();
            List<J.Try.Catch> catches = t.getCatches();
            // Check if the try contains semantically equal catch blocks.
            for (int i = 0; i < catches.size(); i++) {
                J.Try.Catch from = catches.get(i);
                for (int j = i + 1; j < catches.size(); j++) {
                    J.Try.Catch to = catches.get(j);
                    if (SemanticallyEqual.areEqual(from.getBody(), to.getBody()) &&
                            containSameComments(from.getBody(), to.getBody())) {
                        List<J.Try.Catch> semanticallyEqualCatch = semanticallyEqualCatchesMap.computeIfAbsent(from, k -> new ArrayList<>());
                        semanticallyEqualCatch.add(to);
                    }
                }
            }

            if (!semanticallyEqualCatchesMap.isEmpty()) {
                // Collect the identifiers of caught exceptions that are subtypes or implementations of an exception that is caught later in a different catch.
                Map<J.Try.Catch, Map<J.Try.Catch, Set<J.Identifier>>> parentChildClassRelationship = new HashMap<>();
                for (int i = 0; i < catches.size(); i++) {
                    J.Try.Catch from = catches.get(i);
                    for (int j = i + 1; j < catches.size(); j++) {
                        J.Try.Catch to = catches.get(j);
                        // Both 'from' and 'to' may be multi-catches.
                        for (J.Identifier fromIdentifier : getCaughtExceptions(from)) {
                            for (J.Identifier toIdentifier : getCaughtExceptions(to)) {
                                if (fromIdentifier.getType() != null && toIdentifier.getType() != null &&
                                        TypeUtils.isAssignableTo(toIdentifier.getType(), fromIdentifier.getType())) {
                                    Map<J.Try.Catch, Set<J.Identifier>> subTypesMap = parentChildClassRelationship.computeIfAbsent(from, key -> new HashMap<>());
                                    Set<J.Identifier> childClassIdentifiers = subTypesMap.computeIfAbsent(to, key -> new HashSet<>());
                                    childClassIdentifiers.add(fromIdentifier);
                                }
                            }
                        }
                    }
                }

                // Collect the catches that are safe to combine.
                Map<J.Try.Catch, List<J.Try.Catch>> combineCatchesMap = new HashMap<>();
                for (Map.Entry<J.Try.Catch, List<J.Try.Catch>> semanticallyEqualCatches : semanticallyEqualCatchesMap.entrySet()) {
                    J.Try.Catch from = semanticallyEqualCatches.getKey();
                    OUTER:
                    for (J.Try.Catch to : semanticallyEqualCatches.getValue()) {
                        // Check if any catch exists between two catches with semantically equal blocks that is not semantically equal.
                        int indexFrom = catches.indexOf(from);
                        int indexTo = catches.indexOf(to);
                        if ((indexTo - indexFrom) != 1) { // Sequential catches are always safe to combine. I.E. Catch 1 followed by Catch 2.
                            int start = indexFrom + 1;
                            int end = indexTo - 1;
                            for (; start <= end; start++) {
                                J.Try.Catch mayChangeApplicationBehavior = catches.get(start);
                                if (parentChildClassRelationship.containsKey(mayChangeApplicationBehavior) &&
                                        parentChildClassRelationship.get(t.getCatches().get(start)).containsKey(to)) {
                                    if (!semanticallyEqualCatchesMap.containsKey(mayChangeApplicationBehavior)) {
                                        // Skip because combining the catches may change application behavior.
                                        continue OUTER;
                                    }
                                }
                            }
                        }

                        List<J.Try.Catch> toCatches = combineCatchesMap.computeIfAbsent(from, key -> new ArrayList<>());
                        toCatches.add(to);
                    }
                }

                for (Map.Entry<J.Try.Catch, List<J.Try.Catch>> catchMapEntry : combineCatchesMap.entrySet()) {
                    doAfterVisit(new CombineCatches(
                            catchMapEntry.getKey(),
                            catchMapEntry.getValue(),
                            parentChildClassRelationship.getOrDefault(catchMapEntry.getKey(), emptyMap())));
                    doAfterVisit(new RemoveCatches(catchMapEntry.getValue()));
                }
            }

            return t;
        }

        @SuppressWarnings("ConstantConditions")
        static class RemoveCatches extends JavaVisitor<ExecutionContext> {
            private final List<J.Try.Catch> removeCatches;

            RemoveCatches(@Nullable List<J.Try.Catch> removeCatches) {
                this.removeCatches = removeCatches;
            }

            @Override
            public J visitMultiCatch(J.MultiCatch multiCatch, ExecutionContext executionContext) {
                Cursor parentCursor = getCursor().dropParentUntil(is -> is instanceof J.Try.Catch || is instanceof J.Try);
                if (removeCatches != null && parentCursor.getValue() instanceof J.Try.Catch) {
                    if (removeCatches.contains((J.Try.Catch) parentCursor.getValue())) {
                        return null;
                    }
                }
                return super.visitMultiCatch(multiCatch, executionContext);
            }

            @Override
            public J visitCatch(J.Try.Catch _catch, ExecutionContext executionContext) {
                if (removeCatches != null) {
                    if (removeCatches.contains(_catch)) {
                        return null;
                    }
                }
                return super.visitCatch(_catch, executionContext);
            }
        }

        private static class CombineCatches extends JavaVisitor<ExecutionContext> {
            private final J.Try.Catch scope;
            private final List<J.Try.Catch> equivalentCatches;
            private final Map<J.Try.Catch, Set<J.Identifier>> childClassesToExclude;

            CombineCatches(J.Try.Catch scope,
                           List<J.Try.Catch> equivalentCatches,
                           Map<J.Try.Catch, Set<J.Identifier>> childClassesToExclude) {
                this.scope = scope;
                this.equivalentCatches = equivalentCatches;
                this.childClassesToExclude = childClassesToExclude;
            }

            @Override
            public J visitMultiCatch(J.MultiCatch multiCatch, ExecutionContext executionContext) {
                J.MultiCatch m = (J.MultiCatch) super.visitMultiCatch(multiCatch, executionContext);
                Cursor parentCursor = getCursor().dropParentUntil(is -> is instanceof J.Try.Catch || is instanceof J.Try);
                if (parentCursor.getValue() instanceof J.Try.Catch) {
                    J.Try.Catch parent = parentCursor.getValue();
                    if (parent == scope) {
                        List<JRightPadded<NameTree>> combinedCatches = combineEquivalentCatches();
                        m = maybeAutoFormat(m, m.getPadding().withAlternatives(combinedCatches), executionContext);
                    }
                }
                return m;
            }

            @Override
            public J visitCatch(J.Try.Catch _catch, ExecutionContext executionContext) {
                J.Try.Catch c = (J.Try.Catch) super.visitCatch(_catch, executionContext);
                if (c == scope && !isMultiCatch(c)) {
                    if (c.getParameter().getTree().getTypeExpression() != null) {
                        List<JRightPadded<NameTree>> combinedCatches = combineEquivalentCatches();
                        c = maybeAutoFormat(c, c.withParameter(c.getParameter()
                                .withTree(c.getParameter().getTree()
                                        .withTypeExpression(new J.MultiCatch(Tree.randomId(), Space.EMPTY, Markers.EMPTY, combinedCatches)))),
                                executionContext);
                    }
                }
                return c;
            }

            private List<JRightPadded<NameTree>> combineEquivalentCatches() {
                Set<J.Identifier> removeIdentifiers = new HashSet<>();

                List<JRightPadded<NameTree>> combinedCatches = new ArrayList<>();
                for (J.Try.Catch equivalentCatch : equivalentCatches) {
                    Set<J.Identifier> childClasses = childClassesToExclude.get(equivalentCatch);
                    if (childClasses != null) {
                        // Remove child classes that will be unnecessary since the parent exists in the new multi-catch.
                        removeIdentifiers.addAll(childClasses);
                    }

                    // Whitespace works slightly differently between single catches and multi-catches.
                    // The prefix of each `J.Identifier` is set to `Space.EMPTY` so that auto-format will make all the appropriate changes.
                    if (isMultiCatch(equivalentCatch)) {
                        if (equivalentCatch.getParameter().getTree().getTypeExpression() != null) {
                            J.MultiCatch newMultiCatch = (J.MultiCatch) equivalentCatch.getParameter().getTree().getTypeExpression();
                            List<JRightPadded<NameTree>> rightPaddedAlternatives = newMultiCatch.getPadding().getAlternatives();
                            for (JRightPadded<NameTree> alternative : rightPaddedAlternatives) {
                                J.Identifier identifier = (J.Identifier) alternative.getElement();
                                identifier = identifier.withPrefix(Space.EMPTY);
                                alternative = alternative.withElement(identifier);
                                combinedCatches.add(alternative);
                            }
                        }
                    } else {
                        if (equivalentCatch.getParameter().getTree().getTypeExpression() != null) {
                            J.Identifier identifier = ((J.Identifier) equivalentCatch.getParameter().getTree().getTypeExpression());
                            identifier = identifier.withPrefix(Space.EMPTY);
                            JRightPadded<NameTree> rightPadded = JRightPadded.build(identifier);
                            combinedCatches.add(rightPadded);
                        }
                    }
                }

                // Add exceptions in `scope` last to filter out exceptions that are children of parent classes
                // that were added into the new catch.
                if (isMultiCatch(scope)) {
                    J.MultiCatch multiCatch = (J.MultiCatch) scope.getParameter().getTree().getTypeExpression();
                    if (multiCatch != null) {
                        List<JRightPadded<NameTree>> alternatives = multiCatch.getPadding().getAlternatives();
                        for (int i = alternatives.size() - 1; i >= 0; i--) {
                            if (!removeIdentifiers.contains((J.Identifier) alternatives.get(i).getElement())) {
                                JRightPadded<NameTree> alternative = alternatives.get(i);
                                alternative = alternative.withElement(alternative.getElement().withPrefix(Space.EMPTY));
                                // Preserve the order of the original catches.
                                combinedCatches.add(0, alternative);
                            }
                        }
                    }
                } else {
                    J.Identifier identifier = (J.Identifier) scope.getParameter().getTree().getTypeExpression();
                    if (identifier != null && !removeIdentifiers.contains(identifier)) {
                        identifier = identifier.withPrefix(Space.EMPTY);
                        JRightPadded<NameTree> newCatch = JRightPadded.build(identifier);
                        // Preserve the order of the original catches.
                        combinedCatches.add(0, newCatch);
                    }
                }
                return combinedCatches;
            }
        }

        private static boolean containSameComments(J.Block body1, J.Block body2) {
            CommentVisitor commentVisitor = new CommentVisitor();
            commentVisitor.visit(body1, body2);
            return commentVisitor.isEqual.get();
        }


        /**
         * This visitor is a slight variation of {@link SemanticallyEqual} that accounts for differences
         * in comments between two trees. The visitor was separated, because comments are not considered
         * a part of semantic equivalence.
         *
         * Bug fixes related to semantic equality that are found by {@link CombineSemanticallyEqualCatchBlocks}
         * should be applied to {@link SemanticallyEqual} too.
         */
        @SuppressWarnings("ConstantConditions")
        private static class CommentVisitor extends JavaIsoVisitor<J> {
            AtomicBoolean isEqual = new AtomicBoolean(true);
            private final boolean compareMethodArguments = false;

            private boolean nullMissMatch(Object obj1, Object obj2) {
                return (obj1 == null && obj2 != null || obj1 != null && obj2 == null);
            }

            private boolean doesNotContainSameComments(Space space1, Space space2) {
                if (space1.getComments().size() != space2.getComments().size()) {
                    return true;
                }

                for (int i = 0; i < space1.getComments().size(); i++) {
                    Comment comment1 = space1.getComments().get(i);
                    Comment comment2 = space2.getComments().get(i);
                    if (!comment1.printComment(getCursor().getParentOrThrow()).equals(comment2.printComment(getCursor().getParentOrThrow()))) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public Expression visitExpression(Expression expression, J j) {
                if (isEqual.get()) {
                    if (!TypeUtils.isOfType(expression.getType(), ((Expression) j).getType())) {
                        isEqual.set(false);
                        return expression;
                    }
                }

                Expression compareTo = (Expression) j;
                if (doesNotContainSameComments(expression.getPrefix(), compareTo.getPrefix())) {
                    isEqual.set(false);
                }
                return expression;
            }

            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Annotation)) {
                        isEqual.set(false);
                        return annotation;
                    }

                    J.Annotation compareTo = (J.Annotation) j;
                    if (!TypeUtils.isOfType(annotation.getType(), compareTo.getType()) ||
                            nullMissMatch(annotation.getArguments(), compareTo.getArguments()) ||
                            annotation.getArguments() != null && compareTo.getArguments() != null && annotation.getArguments().size() != compareTo.getArguments().size() ||
                            doesNotContainSameComments(annotation.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return annotation;
                    }

                    this.visitTypeName(annotation.getAnnotationType(), compareTo.getAnnotationType());
                    if (annotation.getArguments() != null && compareTo.getArguments() != null) {
                        for (int i = 0; i < annotation.getArguments().size(); i++) {
                            this.visit(annotation.getArguments().get(i), compareTo.getArguments().get(i));
                        }
                    }
                }
                return annotation;
            }

            @Override
            public J.AnnotatedType visitAnnotatedType(J.AnnotatedType annotatedType, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.AnnotatedType)) {
                        isEqual.set(false);
                        return annotatedType;
                    }

                    J.AnnotatedType compareTo = (J.AnnotatedType) j;
                    if (!TypeUtils.isOfType(annotatedType.getType(), compareTo.getType()) ||
                            annotatedType.getAnnotations().size() != compareTo.getAnnotations().size() ||
                            doesNotContainSameComments(annotatedType.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return annotatedType;
                    }

                    this.visitTypeName(annotatedType.getTypeExpression(), compareTo.getTypeExpression());
                    for (int i = 0; i < annotatedType.getAnnotations().size(); i++) {
                        this.visit(annotatedType.getAnnotations().get(i), compareTo.getAnnotations().get(i));
                    }
                }
                return annotatedType;
            }

            @Override
            public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.ArrayAccess)) {
                        isEqual.set(false);
                        return arrayAccess;
                    }

                    J.ArrayAccess compareTo = (J.ArrayAccess) j;
                    if (nullMissMatch(arrayAccess.getType(), compareTo.getType()) ||
                            !TypeUtils.isOfType(arrayAccess.getType(), compareTo.getType()) ||
                            doesNotContainSameComments(arrayAccess.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return arrayAccess;
                    }

                    this.visit(arrayAccess.getIndexed(), compareTo.getIndexed());
                    this.visit(arrayAccess.getDimension(), compareTo.getDimension());
                }
                return arrayAccess;
            }

            @Override
            public J.ArrayDimension visitArrayDimension(J.ArrayDimension arrayDimension, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.ArrayDimension)) {
                        isEqual.set(false);
                        return arrayDimension;
                    }

                    J.ArrayDimension compareTo = (J.ArrayDimension) j;
                    if (doesNotContainSameComments(arrayDimension.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return arrayDimension;
                    }
                    this.visit(arrayDimension.getIndex(), compareTo.getIndex());
                }
                return arrayDimension;
            }

            @Override
            public J.ArrayType visitArrayType(J.ArrayType arrayType, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.ArrayType)) {
                        isEqual.set(false);
                        return arrayType;
                    }

                    J.ArrayType compareTo = (J.ArrayType) j;
                    if (!TypeUtils.isOfType(arrayType.getType(), compareTo.getType()) ||
                            doesNotContainSameComments(arrayType.getPrefix(), compareTo.getPrefix()) ||
                            arrayType.getDimensions().size() != compareTo.getDimensions().size()) {
                        isEqual.set(false);
                        return arrayType;
                    }

                    for (int i = 0; i < arrayType.getDimensions().size(); i++) {
                        if (doesNotContainSameComments(arrayType.getDimensions().get(i).getElement(), compareTo.getDimensions().get(i).getElement())) {
                            isEqual.set(false);
                            return arrayType;
                        }
                    }

                    this.visitTypeName(arrayType.getElementType(), compareTo.getElementType());
                }
                return arrayType;
            }

            @Override
            public J.Assert visitAssert(J.Assert _assert, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Assert)) {
                        isEqual.set(false);
                        return _assert;
                    }

                    J.Assert compareTo = (J.Assert) j;
                    if (nullMissMatch(_assert.getDetail(), compareTo.getDetail()) ||
                            doesNotContainSameComments(_assert.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return _assert;
                    }

                    this.visit(_assert.getCondition(), compareTo.getCondition());
                    if (_assert.getDetail() != null && compareTo.getDetail() != null) {
                        this.visit(_assert.getDetail().getElement(), compareTo.getDetail().getElement());
                    }
                }
                return _assert;
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Assignment)) {
                        isEqual.set(false);
                        return assignment;
                    }

                    J.Assignment compareTo = (J.Assignment) j;
                    if (nullMissMatch(assignment.getType(), compareTo.getType()) ||
                            !TypeUtils.isOfType(assignment.getType(), compareTo.getType()) ||
                            doesNotContainSameComments(assignment.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return assignment;
                    }

                    this.visit(assignment.getAssignment(), compareTo.getAssignment());
                    this.visit(assignment.getVariable(), compareTo.getVariable());
                }
                return assignment;
            }

            @Override
            public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.AssignmentOperation)) {
                        isEqual.set(false);
                        return assignOp;
                    }

                    J.AssignmentOperation compareTo = (J.AssignmentOperation) j;
                    if (nullMissMatch(assignOp.getType(), compareTo.getType()) ||
                            !TypeUtils.isOfType(assignOp.getType(), compareTo.getType()) ||
                            doesNotContainSameComments(assignOp.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return assignOp;
                    }

                    this.visit(assignOp.getAssignment(), compareTo.getAssignment());
                    this.visit(assignOp.getVariable(), compareTo.getVariable());
                }
                return assignOp;
            }

            @Override
            public J.Binary visitBinary(J.Binary binary, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Binary)) {
                        isEqual.set(false);
                        return binary;
                    }

                    J.Binary compareTo = (J.Binary) j;
                    if (nullMissMatch(binary.getType(), compareTo.getType()) ||
                            !TypeUtils.isOfType(binary.getType(), compareTo.getType()) ||
                            binary.getOperator() != compareTo.getOperator() ||
                            doesNotContainSameComments(binary.getPadding().getOperator().getBefore(), compareTo.getPadding().getOperator().getBefore()) ||
                            doesNotContainSameComments(binary.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return binary;
                    }

                    this.visit(binary.getLeft(), compareTo.getLeft());
                    this.visit(binary.getRight(), compareTo.getRight());
                }
                return binary;
            }

            @Override
            public J.Block visitBlock(J.Block block, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Block)) {
                        isEqual.set(false);
                        return block;
                    }

                    J.Block compareTo = (J.Block) j;
                    if (block.getStatements().size() != compareTo.getStatements().size() ||
                            doesNotContainSameComments(block.getPrefix(), compareTo.getPrefix()) ||
                            doesNotContainSameComments(block.getEnd(), compareTo.getEnd())) {
                        isEqual.set(false);
                        return block;
                    }

                    for (int i = 0; i < block.getStatements().size(); i++) {
                        this.visit(block.getStatements().get(i), compareTo.getStatements().get(i));
                    }
                }
                return block;
            }

            @Override
            public J.Break visitBreak(J.Break breakStatement, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Break)) {
                        isEqual.set(false);
                        return breakStatement;
                    }

                    J.Break compareTo = (J.Break) j;
                    if (nullMissMatch(breakStatement.getLabel(), compareTo.getLabel()) ||
                            doesNotContainSameComments(breakStatement.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return breakStatement;
                    }
                    if (breakStatement.getLabel() != null && compareTo.getLabel() != null) {
                        this.visit(breakStatement.getLabel(), compareTo.getLabel());
                    }
                }
                return breakStatement;
            }

            @Override
            public J.Case visitCase(J.Case _case, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Case)) {
                        isEqual.set(false);
                        return _case;
                    }

                    J.Case compareTo = (J.Case) j;
                    if (_case.getStatements().size() != compareTo.getStatements().size() ||
                            doesNotContainSameComments(_case.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return _case;
                    }

                    this.visit(_case.getPattern(), compareTo.getPattern());
                    for (int i = 0; i < _case.getStatements().size(); i++) {
                        this.visit(_case.getStatements().get(i), compareTo.getStatements().get(i));
                    }
                }
                return _case;
            }

            @Override
            public J.Try.Catch visitCatch(J.Try.Catch _catch, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Try.Catch)) {
                        isEqual.set(false);
                        return _catch;
                    }

                    J.Try.Catch compareTo = (J.Try.Catch) j;
                    if (doesNotContainSameComments(_catch.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return _catch;
                    }

                    this.visit(_catch.getParameter(), compareTo.getParameter());
                    this.visit(_catch.getBody(), compareTo.getBody());
                }
                return _catch;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.ClassDeclaration)) {
                        isEqual.set(false);
                        return classDecl;
                    }

                    J.ClassDeclaration compareTo = (J.ClassDeclaration) j;
                    if (!TypeUtils.isOfType(classDecl.getType(), compareTo.getType()) ||
                            classDecl.getModifiers().size() != compareTo.getModifiers().size() ||
                            !new HashSet<>(classDecl.getModifiers()).containsAll(compareTo.getModifiers()) ||
                            classDecl.getKind() != compareTo.getKind() ||
                            classDecl.getLeadingAnnotations().size() != compareTo.getLeadingAnnotations().size() ||

                            nullMissMatch(classDecl.getExtends(), compareTo.getExtends()) ||

                            nullMissMatch(classDecl.getTypeParameters(), compareTo.getTypeParameters()) ||
                            classDecl.getTypeParameters() != null && compareTo.getTypeParameters() != null && classDecl.getTypeParameters().size() != compareTo.getTypeParameters().size() ||

                            nullMissMatch(classDecl.getImplements(), compareTo.getImplements()) ||
                            classDecl.getImplements() != null && compareTo.getImplements() != null && classDecl.getImplements().size() != compareTo.getImplements().size() ||
                            doesNotContainSameComments(classDecl.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return classDecl;
                    }

                    this.visit(classDecl.getName(), compareTo.getName());
                    for (int i = 0; i < classDecl.getLeadingAnnotations().size(); i++) {
                        this.visit(classDecl.getLeadingAnnotations().get(i), compareTo.getLeadingAnnotations().get(i));
                    }

                    if (classDecl.getExtends() != null && compareTo.getExtends() != null) {
                        this.visit(classDecl.getExtends(), compareTo.getExtends());
                    }

                    if (classDecl.getTypeParameters() != null && compareTo.getTypeParameters() != null) {
                        for (int i = 0; i < classDecl.getTypeParameters().size(); i++) {
                            this.visit(classDecl.getTypeParameters().get(i), compareTo.getTypeParameters().get(i));
                        }
                    }

                    if (classDecl.getImplements() != null && compareTo.getImplements() != null) {
                        for (int i = 0; i < classDecl.getImplements().size(); i++) {
                            this.visit(classDecl.getImplements().get(i), compareTo.getImplements().get(i));
                        }
                    }

                    this.visit(classDecl.getBody(), compareTo.getBody());

                }
                return classDecl;
            }

            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.CompilationUnit)) {
                        isEqual.set(false);
                        return cu;
                    }

                    J.CompilationUnit compareTo = (J.CompilationUnit) j;
                    if (nullMissMatch(cu.getPackageDeclaration(), compareTo.getPackageDeclaration()) ||
                            cu.getImports().size() != compareTo.getImports().size() ||
                            cu.getClasses().size() != compareTo.getClasses().size() ||
                            doesNotContainSameComments(cu.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return cu;
                    }

                    if (cu.getPackageDeclaration() != null && compareTo.getPackageDeclaration() != null) {
                        this.visit(cu.getPackageDeclaration(), compareTo.getPackageDeclaration());
                    }
                    for (int i = 0; i < cu.getImports().size(); i++) {
                        this.visit(cu.getImports().get(i), compareTo.getImports().get(i));
                    }
                    for (int i = 0; i < cu.getClasses().size(); i++) {
                        this.visit(cu.getClasses().get(i), compareTo.getClasses().get(i));
                    }
                }
                return cu;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T extends J> J.ControlParentheses<T> visitControlParentheses(J.ControlParentheses<T> controlParens, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.ControlParentheses)) {
                        isEqual.set(false);
                        return controlParens;
                    }

                    J.ControlParentheses<T> compareTo = (J.ControlParentheses<T>) j;
                    if (!TypeUtils.isOfType(controlParens.getType(), compareTo.getType()) ||
                            doesNotContainSameComments(controlParens.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return controlParens;
                    }
                    this.visit(controlParens.getTree(), compareTo.getTree());
                }
                return controlParens;
            }

            @Override
            public J.Continue visitContinue(J.Continue continueStatement, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Continue)) {
                        isEqual.set(false);
                        return continueStatement;
                    }

                    J.Continue compareTo = (J.Continue) j;
                    if (nullMissMatch(continueStatement.getLabel(), compareTo.getLabel()) ||
                            doesNotContainSameComments(continueStatement.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return continueStatement;
                    }

                    if (continueStatement.getLabel() != null && compareTo.getLabel() != null) {
                        this.visit(continueStatement.getLabel(), compareTo.getLabel());
                    }
                }
                return continueStatement;
            }

            @Override
            public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.DoWhileLoop)) {
                        isEqual.set(false);
                        return doWhileLoop;
                    }

                    J.DoWhileLoop compareTo = (J.DoWhileLoop) j;
                    if (doesNotContainSameComments(doWhileLoop.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return doWhileLoop;
                    }

                    this.visit(doWhileLoop.getWhileCondition(), compareTo.getWhileCondition());
                    this.visit(doWhileLoop.getBody(), compareTo.getBody());
                }
                return doWhileLoop;
            }

            @Override
            public J.If.Else visitElse(J.If.Else elze, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.If.Else)) {
                        isEqual.set(false);
                        return elze;
                    }

                    J.If.Else compareTo = (J.If.Else) j;
                    if (doesNotContainSameComments(elze.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return elze;
                    }
                    this.visit(elze.getBody(), compareTo.getBody());
                }
                return elze;
            }

            @Override
            public J.Empty visitEmpty(J.Empty empty, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Empty)) {
                        isEqual.set(false);
                        return empty;
                    }

                    J.Empty compareTo = (J.Empty) j;
                    if (empty.getType() == null && compareTo.getType() != null ||
                            empty.getType() != null && compareTo.getType() == null ||
                            doesNotContainSameComments(empty.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return empty;
                    }
                }
                return empty;
            }

            @Override
            public J.EnumValue visitEnumValue(J.EnumValue _enum, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.EnumValue)) {
                        isEqual.set(false);
                        return _enum;
                    }

                    J.EnumValue compareTo = (J.EnumValue) j;
                    if (nullMissMatch(_enum.getAnnotations(), compareTo.getAnnotations()) ||
                            _enum.getAnnotations().size() != compareTo.getAnnotations().size() ||
                            doesNotContainSameComments(_enum.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return _enum;
                    }

                    this.visit(_enum.getName(), compareTo.getName());
                    for (int i = 0; i < _enum.getAnnotations().size(); i++) {
                        this.visit(_enum.getAnnotations().get(i), compareTo.getAnnotations().get(i));
                    }
                    if (_enum.getInitializer() != null && compareTo.getInitializer() != null) {
                        this.visit(_enum.getInitializer(), compareTo.getInitializer());
                    }
                }
                return _enum;
            }

            @Override
            public J.EnumValueSet visitEnumValueSet(J.EnumValueSet enums, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.EnumValueSet)) {
                        isEqual.set(false);
                        return enums;
                    }

                    J.EnumValueSet compareTo = (J.EnumValueSet) j;
                    if (enums.getEnums().size() != compareTo.getEnums().size() ||
                            doesNotContainSameComments(enums.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return enums;
                    }

                    for (int i = 0; i < enums.getEnums().size(); i++) {
                        this.visit(enums.getEnums().get(i), compareTo.getEnums().get(i));
                    }
                }
                return enums;
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.FieldAccess)) {
                        isEqual.set(false);
                        return fieldAccess;
                    }

                    J.FieldAccess compareTo = (J.FieldAccess) j;
                    if (!TypeUtils.isOfType(fieldAccess.getType(), compareTo.getType()) ||
                            !TypeUtils.isOfType(fieldAccess.getTarget().getType(), compareTo.getTarget().getType()) ||
                            doesNotContainSameComments(fieldAccess.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return fieldAccess;
                    }
                    this.visit(fieldAccess.getName(), compareTo.getName());
                }
                return fieldAccess;
            }

            @Override
            public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.ForEachLoop)) {
                        isEqual.set(false);
                        return forLoop;
                    }

                    J.ForEachLoop compareTo = (J.ForEachLoop) j;
                    if (doesNotContainSameComments(forLoop.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return forLoop;
                    }

                    this.visit(forLoop.getControl(), compareTo.getControl());
                    this.visit(forLoop.getBody(), compareTo.getBody());
                }
                return forLoop;
            }

            @Override
            public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.ForEachLoop.Control)) {
                        isEqual.set(false);
                        return control;
                    }

                    J.ForEachLoop.Control compareTo = (J.ForEachLoop.Control) j;
                    if (doesNotContainSameComments(control.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return control;
                    }
                    this.visit(control.getVariable(), compareTo.getVariable());
                    this.visit(control.getIterable(), compareTo.getIterable());
                }
                return control;
            }

            @Override
            public J.ForLoop visitForLoop(J.ForLoop forLoop, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.ForLoop)) {
                        isEqual.set(false);
                        return forLoop;
                    }

                    J.ForLoop compareTo = (J.ForLoop) j;
                    if (doesNotContainSameComments(forLoop.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return forLoop;
                    }
                    this.visit(forLoop.getControl(), compareTo.getControl());
                    this.visit(forLoop.getBody(), compareTo.getBody());
                }
                return forLoop;
            }

            @Override
            public J.ForLoop.Control visitForControl(J.ForLoop.Control control, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.ForLoop.Control)) {
                        isEqual.set(false);
                        return control;
                    }

                    J.ForLoop.Control compareTo = (J.ForLoop.Control) j;
                    if (control.getInit().size() != compareTo.getInit().size() ||
                            control.getUpdate().size() != compareTo.getUpdate().size() ||
                            doesNotContainSameComments(control.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return control;
                    }
                    this.visit(control.getCondition(), compareTo.getCondition());
                    for (int i = 0; i < control.getInit().size(); i++) {
                        this.visit(control.getInit().get(i), compareTo.getInit().get(i));
                    }
                    for (int i = 0; i < control.getUpdate().size(); i++) {
                        this.visit(control.getUpdate().get(i), compareTo.getUpdate().get(i));
                    }
                }
                return control;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Identifier)) {
                        isEqual.set(false);
                        return identifier;
                    }

                    J.Identifier compareTo = (J.Identifier) j;
                    if (!identifier.getSimpleName().equals(compareTo.getSimpleName()) ||
                            !TypeUtils.isOfType(identifier.getType(), compareTo.getType()) ||
                            doesNotContainSameComments(identifier.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return identifier;
                    }
                }
                return identifier;
            }

            @Override
            public J.If visitIf(J.If iff, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.If)) {
                        isEqual.set(false);
                        return iff;
                    }

                    J.If compareTo = (J.If) j;
                    if (nullMissMatch(iff.getElsePart(), compareTo.getElsePart()) ||
                            doesNotContainSameComments(iff.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return iff;
                    }
                    this.visit(iff.getIfCondition(), compareTo.getIfCondition());
                    this.visit(iff.getThenPart(), compareTo.getThenPart());
                    if (iff.getElsePart() != null && compareTo.getElsePart() != null) {
                        this.visit(iff.getElsePart(), compareTo.getElsePart());
                    }
                }
                return iff;
            }

            @Override
            public J.Import visitImport(J.Import _import, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Import)) {
                        isEqual.set(false);
                        return _import;
                    }

                    J.Import compareTo = (J.Import) j;
                    if (_import.isStatic() != compareTo.isStatic() ||
                            !_import.getPackageName().equals(compareTo.getPackageName()) ||
                            !_import.getClassName().equals(compareTo.getClassName()) ||
                            !TypeUtils.isOfType(_import.getQualid().getType(), compareTo.getQualid().getType())) {
                        isEqual.set(false);
                        return _import;
                    }
                }
                return _import;
            }

            @Override
            public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.InstanceOf)) {
                        isEqual.set(false);
                        return instanceOf;
                    }

                    J.InstanceOf compareTo = (J.InstanceOf) j;
                    if (!TypeUtils.isOfType(instanceOf.getType(), compareTo.getType()) ||
                            doesNotContainSameComments(instanceOf.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return instanceOf;
                    }
                    this.visit(instanceOf.getClazz(), compareTo.getClazz());
                    this.visit(instanceOf.getExpression(), compareTo.getExpression());
                }
                return instanceOf;
            }

            @Override
            public J.Label visitLabel(J.Label label, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Label)) {
                        isEqual.set(false);
                        return label;
                    }

                    J.Label compareTo = (J.Label) j;
                    if (doesNotContainSameComments(label.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return label;
                    }
                    this.visit(label.getLabel(), compareTo.getLabel());
                    this.visit(label.getStatement(), compareTo.getStatement());
                }
                return label;
            }

            @Override
            public J.Lambda visitLambda(J.Lambda lambda, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Lambda)) {
                        isEqual.set(false);
                        return lambda;
                    }

                    J.Lambda compareTo = (J.Lambda) j;
                    if (lambda.getParameters().isParenthesized() != compareTo.getParameters().isParenthesized() ||
                            lambda.getParameters().getParameters().size() != compareTo.getParameters().getParameters().size() ||
                            doesNotContainSameComments(lambda.getPrefix(), compareTo.getPrefix()) ||
                            doesNotContainSameComments(lambda.getArrow(), compareTo.getArrow())) {
                        isEqual.set(false);
                        return lambda;
                    }
                    this.visit(lambda.getBody(), compareTo.getBody());
                    for (int i = 0; i < lambda.getParameters().getParameters().size(); i++) {
                        this.visit(lambda.getParameters().getParameters().get(i), compareTo.getParameters().getParameters().get(i));
                    }
                }
                return lambda;
            }

            @Override
            public J.Literal visitLiteral(J.Literal literal, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Literal)) {
                        isEqual.set(false);
                        return literal;
                    }

                    J.Literal compareTo = (J.Literal) j;
                    if (!TypeUtils.isOfType(literal.getType(), compareTo.getType()) ||
                            !Objects.equals(literal.getValue(), compareTo.getValue()) ||
                            doesNotContainSameComments(literal.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return literal;
                    }
                }
                return literal;
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.MemberReference)) {
                        isEqual.set(false);
                        return memberRef;
                    }

                    J.MemberReference compareTo = (J.MemberReference) j;
                    if (!TypeUtils.isOfType(memberRef.getType(), compareTo.getType()) ||
                            !TypeUtils.isOfType(memberRef.getVariableType(), compareTo.getVariableType()) ||
                            !TypeUtils.isOfType(memberRef.getMethodType(), compareTo.getMethodType()) ||
                            nullMissMatch(memberRef.getTypeParameters(), compareTo.getTypeParameters()) ||
                            memberRef.getTypeParameters() != null && compareTo.getTypeParameters() != null && memberRef.getTypeParameters().size() != compareTo.getTypeParameters().size() ||
                            doesNotContainSameComments(memberRef.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return memberRef;
                    }

                    this.visit(memberRef.getReference(), compareTo.getReference());
                    this.visit(memberRef.getContaining(), compareTo.getContaining());
                    if (memberRef.getTypeParameters() != null && compareTo.getTypeParameters() != null) {
                        for (int i = 0; i < memberRef.getTypeParameters().size(); i++) {
                            this.visit(memberRef.getTypeParameters().get(i), compareTo.getTypeParameters().get(i));
                        }
                    }
                }
                return memberRef;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.MethodDeclaration)) {
                        isEqual.set(false);
                        return method;
                    }

                    J.MethodDeclaration compareTo = (J.MethodDeclaration) j;
                    if (!TypeUtils.isOfType(method.getMethodType(), compareTo.getMethodType()) ||
                            method.getModifiers().size() != compareTo.getModifiers().size() ||
                            !new HashSet<>(method.getModifiers()).containsAll(compareTo.getModifiers()) ||

                            method.getLeadingAnnotations().size() != compareTo.getLeadingAnnotations().size() ||
                            method.getParameters().size() != compareTo.getParameters().size() ||

                            nullMissMatch(method.getReturnTypeExpression(), compareTo.getReturnTypeExpression()) ||

                            nullMissMatch(method.getTypeParameters(), compareTo.getTypeParameters()) ||
                            method.getTypeParameters() != null && compareTo.getTypeParameters() != null && method.getTypeParameters().size() != compareTo.getTypeParameters().size() ||

                            nullMissMatch(method.getThrows(), compareTo.getThrows()) ||
                            method.getThrows() != null && compareTo.getThrows() != null && method.getThrows().size() != compareTo.getThrows().size() ||

                            nullMissMatch(method.getBody(), compareTo.getBody()) ||
                            method.getBody().getStatements() != null && compareTo.getBody().getStatements() != null && method.getBody().getStatements().size() != compareTo.getBody().getStatements().size() ||
                            doesNotContainSameComments(method.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return method;
                    }

                    this.visit(method.getName(), compareTo.getName());
                    for (int i = 0; i < method.getLeadingAnnotations().size(); i++) {
                        this.visit(method.getLeadingAnnotations().get(i), compareTo.getLeadingAnnotations().get(i));
                    }

                    for (int i = 0; i < method.getParameters().size(); i++) {
                        this.visit(method.getParameters().get(i), compareTo.getParameters().get(i));
                    }

                    if (method.getReturnTypeExpression() != null && compareTo.getReturnTypeExpression() != null) {
                        this.visitTypeName(method.getReturnTypeExpression(), compareTo.getReturnTypeExpression());
                    }

                    if (method.getTypeParameters() != null && compareTo.getTypeParameters() != null) {
                        for (int i = 0; i < method.getTypeParameters().size(); i++) {
                            this.visit(method.getTypeParameters().get(i), compareTo.getTypeParameters().get(i));
                        }
                    }

                    if (method.getThrows() != null && compareTo.getThrows() != null) {
                        for (int i = 0; i < method.getThrows().size(); i++) {
                            this.visitTypeName(method.getThrows().get(i), compareTo.getThrows().get(i));
                        }
                    }

                    if (method.getBody() != null && compareTo.getBody() != null) {
                        this.visit(method.getBody(), compareTo.getBody());
                    }
                }
                return method;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.MethodInvocation)) {
                        isEqual.set(false);
                        return method;
                    }

                    J.MethodInvocation compareTo = (J.MethodInvocation) j;
                    if (!TypeUtils.isOfType(method.getMethodType(), compareTo.getMethodType()) ||
                            nullMissMatch(method.getSelect(), compareTo.getSelect()) ||
                            method.getArguments().size() != compareTo.getArguments().size() ||
                            method.getTypeParameters() != null && compareTo.getTypeParameters() != null && method.getTypeParameters().size() != compareTo.getTypeParameters().size() ||
                            doesNotContainSameComments(method.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return method;
                    }

                    this.visit(method.getName(), compareTo.getName());
                    this.visit(method.getSelect(), compareTo.getSelect());
                    boolean containsLiteral = false;
                    if (!compareMethodArguments) {
                        for (int i = 0; i < method.getArguments().size(); i++) {
                            if (method.getArguments().get(i) instanceof J.Literal || compareTo.getArguments().get(i) instanceof J.Literal) {
                                containsLiteral = true;
                                break;
                            }
                        }
                        if (!containsLiteral) {
                            if (nullMissMatch(method.getMethodType(), compareTo.getMethodType()) ||
                                    !TypeUtils.isOfType(method.getMethodType(), compareTo.getMethodType())) {
                                isEqual.set(false);
                                return method;
                            }
                        }
                    }
                    if (compareMethodArguments || containsLiteral) {
                        for (int i = 0; i < method.getArguments().size(); i++) {
                            this.visit(method.getArguments().get(i), compareTo.getArguments().get(i));
                        }
                    }
                    if (method.getTypeParameters() != null && compareTo.getTypeParameters() != null) {
                        for (int i = 0; i < method.getTypeParameters().size(); i++) {
                            this.visit(method.getTypeParameters().get(i), compareTo.getTypeParameters().get(i));
                        }
                    }
                }
                return method;
            }

            @Override
            public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.MultiCatch)) {
                        isEqual.set(false);
                        return multiCatch;
                    }

                    J.MultiCatch compareTo = (J.MultiCatch) j;
                    if (!(multiCatch.getType() instanceof JavaType.MultiCatch) ||
                            !(compareTo.getType() instanceof JavaType.MultiCatch) ||
                            ((JavaType.MultiCatch) multiCatch.getType()).getThrowableTypes().size() != ((JavaType.MultiCatch) compareTo.getType()).getThrowableTypes().size() ||
                            multiCatch.getAlternatives().size() != compareTo.getAlternatives().size() ||
                            doesNotContainSameComments(multiCatch.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return multiCatch;
                    }

                    for (int i = 0; i < ((JavaType.MultiCatch) multiCatch.getType()).getThrowableTypes().size(); i++) {
                        JavaType first = ((JavaType.MultiCatch) multiCatch.getType()).getThrowableTypes().get(i);
                        JavaType second = ((JavaType.MultiCatch) compareTo.getType()).getThrowableTypes().get(i);
                        if (!TypeUtils.isOfType(first, second)) {
                            isEqual.set(false);
                            return multiCatch;
                        }
                    }

                    for (int i = 0; i < multiCatch.getAlternatives().size(); i++) {
                        this.visit(multiCatch.getAlternatives().get(i), compareTo.getAlternatives().get(i));
                    }
                }
                return multiCatch;
            }

            @Override
            public J.NewArray visitNewArray(J.NewArray newArray, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.NewArray)) {
                        isEqual.set(false);
                        return newArray;
                    }

                    J.NewArray compareTo = (J.NewArray) j;
                    if (!TypeUtils.isOfType(newArray.getType(), compareTo.getType()) ||
                            newArray.getDimensions().size() != compareTo.getDimensions().size() ||
                            nullMissMatch(newArray.getTypeExpression(), compareTo.getTypeExpression()) ||
                            nullMissMatch(newArray.getInitializer(), compareTo.getInitializer()) ||
                            newArray.getInitializer() != null && compareTo.getInitializer() != null && newArray.getInitializer().size() != compareTo.getInitializer().size() ||
                            doesNotContainSameComments(newArray.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return newArray;
                    }

                    for (int i = 0; i < newArray.getDimensions().size(); i++) {
                        this.visit(newArray.getDimensions().get(i), compareTo.getDimensions().get(i));
                    }
                    if (newArray.getTypeExpression() != null && compareTo.getTypeExpression() != null) {
                        this.visit(newArray.getTypeExpression(), compareTo.getTypeExpression());
                    }
                    if (newArray.getInitializer() != null && compareTo.getInitializer() != null) {
                        for (int i = 0; i < newArray.getInitializer().size(); i++) {
                            this.visit(newArray.getInitializer().get(i), compareTo.getInitializer().get(i));
                        }
                    }
                }
                return newArray;
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.NewClass)) {
                        isEqual.set(false);
                        return newClass;
                    }

                    J.NewClass compareTo = (J.NewClass) j;
                    if (!TypeUtils.isOfType(newClass.getType(), compareTo.getType()) ||
                            !TypeUtils.isOfType(newClass.getConstructorType(), compareTo.getConstructorType()) ||
                            nullMissMatch(newClass.getEnclosing(), compareTo.getEnclosing()) ||
                            nullMissMatch(newClass.getClazz(), compareTo.getClazz()) ||
                            nullMissMatch(newClass.getConstructorType(), compareTo.getConstructorType()) ||
                            nullMissMatch(newClass.getBody(), compareTo.getBody()) ||
                            nullMissMatch(newClass.getArguments(), compareTo.getArguments()) ||
                            newClass.getArguments().size() != compareTo.getArguments().size() ||
                            doesNotContainSameComments(newClass.getPrefix(), compareTo.getPrefix()) ||
                            doesNotContainSameComments(newClass.getNew(), compareTo.getNew())) {
                        isEqual.set(false);
                        return newClass;
                    }

                    if (newClass.getEnclosing() != null && compareTo.getEnclosing() != null) {
                        this.visit(newClass.getEnclosing(), compareTo.getEnclosing());
                    }
                    if (newClass.getClazz() != null && compareTo.getClazz() != null) {
                        this.visit(newClass.getClazz(), compareTo.getClazz());
                    }
                    if (newClass.getBody() != null && compareTo.getBody() != null) {
                        this.visit(newClass.getBody(), compareTo.getBody());
                    }
                    boolean containsLiteral = false;
                    if (!compareMethodArguments) {
                        for (int i = 0; i < newClass.getArguments().size(); i++) {
                            if (newClass.getArguments().get(i) instanceof J.Literal || compareTo.getArguments().get(i) instanceof J.Literal) {
                                containsLiteral = true;
                                break;
                            }
                        }
                        if (!containsLiteral) {
                            if (nullMissMatch(newClass.getConstructorType(), compareTo.getConstructorType()) ||
                                    newClass.getConstructorType() != null && compareTo.getConstructorType() != null && !TypeUtils.isOfType(newClass.getConstructorType(), compareTo.getConstructorType())) {
                                isEqual.set(false);
                                return newClass;
                            }
                        }
                    }
                    if (compareMethodArguments || containsLiteral) {
                        for (int i = 0; i < newClass.getArguments().size(); i++) {
                            this.visit(newClass.getArguments().get(i), compareTo.getArguments().get(i));
                        }
                    }
                }
                return newClass;
            }

            @Override
            public J.Package visitPackage(J.Package pkg, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Package)) {
                        isEqual.set(false);
                        return pkg;
                    }

                    J.Package compareTo = (J.Package) j;
                    if (pkg.getAnnotations().size() != compareTo.getAnnotations().size() ||
                            !pkg.getExpression().toString().equals(compareTo.getExpression().toString())) {
                        isEqual.set(false);
                        return pkg;
                    }
                    for (int i = 0; i < pkg.getAnnotations().size(); i++) {
                        this.visit(pkg.getAnnotations().get(i), compareTo.getAnnotations().get(i));
                    }
                }
                return pkg;
            }

            @Override
            public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.ParameterizedType)) {
                        isEqual.set(false);
                        return type;
                    }

                    J.ParameterizedType compareTo = (J.ParameterizedType) j;
                    if (!TypeUtils.isOfType(type.getType(), compareTo.getType()) ||
                            nullMissMatch(type.getTypeParameters(), compareTo.getTypeParameters()) ||
                            type.getTypeParameters() != null && compareTo.getTypeParameters() != null && type.getTypeParameters().size() != compareTo.getTypeParameters().size() ||
                            doesNotContainSameComments(type.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return type;
                    }

                    if (type.getTypeParameters() != null && compareTo.getTypeParameters() != null) {
                        for (int i = 0; i < type.getTypeParameters().size(); i++) {
                            this.visit(type.getTypeParameters().get(i), compareTo.getTypeParameters().get(i));
                        }
                    }
                }
                return type;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Parentheses)) {
                        isEqual.set(false);
                        return parens;
                    }

                    J.Parentheses<T> compareTo = (J.Parentheses<T>) j;
                    if (doesNotContainSameComments(parens.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return parens;
                    }
                    this.visit(parens.getTree(), compareTo.getTree());
                }
                return parens;
            }

            @Override
            public J.Primitive visitPrimitive(J.Primitive primitive, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Primitive)) {
                        isEqual.set(false);
                        return primitive;
                    }

                    J.Primitive compareTo = (J.Primitive) j;
                    if (!TypeUtils.isOfType(primitive.getType(), compareTo.getType()) ||
                            doesNotContainSameComments(primitive.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return primitive;
                    }
                }
                return primitive;
            }

            @Override
            public J.Return visitReturn(J.Return _return, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Return)) {
                        isEqual.set(false);
                        return _return;
                    }

                    J.Return compareTo = (J.Return) j;
                    if (nullMissMatch(_return.getExpression(), compareTo.getExpression()) ||
                            doesNotContainSameComments(_return.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return _return;
                    }

                    if (_return.getExpression() != null && compareTo.getExpression() != null) {
                        this.visit(_return.getExpression(), compareTo.getExpression());
                    }
                }
                return _return;
            }

            @Override
            public J.Switch visitSwitch(J.Switch _switch, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Switch)) {
                        isEqual.set(false);
                        return _switch;
                    }

                    J.Switch compareTo = (J.Switch) j;
                    if (doesNotContainSameComments(_switch.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return _switch;
                    }
                    this.visit(_switch.getCases(), compareTo.getCases());
                }
                return _switch;
            }

            @Override
            public J.Synchronized visitSynchronized(J.Synchronized _sync, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Synchronized)) {
                        isEqual.set(false);
                        return _sync;
                    }

                    J.Synchronized compareTo = (J.Synchronized) j;
                    if (doesNotContainSameComments(_sync.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return _sync;
                    }
                    this.visit(_sync.getLock(), compareTo.getLock());
                    this.visit(_sync.getBody(), compareTo.getBody());
                }
                return _sync;
            }

            @Override
            public J.Ternary visitTernary(J.Ternary ternary, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Ternary)) {
                        isEqual.set(false);
                        return ternary;
                    }

                    J.Ternary compareTo = (J.Ternary) j;
                    if (!TypeUtils.isOfType(ternary.getType(), compareTo.getType()) ||
                            doesNotContainSameComments(ternary.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return ternary;
                    }
                    this.visit(ternary.getCondition(), compareTo.getCondition());
                    this.visit(ternary.getTruePart(), compareTo.getTruePart());
                    this.visit(ternary.getFalsePart(), compareTo.getFalsePart());
                }
                return ternary;
            }

            @Override
            public J.Throw visitThrow(J.Throw thrown, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Throw)) {
                        isEqual.set(false);
                        return thrown;
                    }

                    J.Throw compareTo = (J.Throw) j;
                    if (doesNotContainSameComments(thrown.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return thrown;
                    }
                    this.visit(thrown.getException(), compareTo.getException());
                }
                return thrown;
            }

            @Override
            public J.Try visitTry(J.Try _try, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Try)) {
                        isEqual.set(false);
                        return _try;
                    }

                    J.Try compareTo = (J.Try) j;
                    if (_try.getCatches().size() != compareTo.getCatches().size() ||
                            nullMissMatch(_try.getFinally(), compareTo.getFinally()) ||
                            nullMissMatch(_try.getResources(), compareTo.getResources()) ||
                            _try.getResources() != null && compareTo.getResources() != null && _try.getResources().size() != compareTo.getResources().size() ||
                            doesNotContainSameComments(_try.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return _try;
                    }
                    this.visit(_try.getBody(), compareTo.getBody());
                    for (int i = 0; i < _try.getCatches().size(); i++) {
                        this.visit(_try.getCatches().get(i), compareTo.getCatches().get(i));
                    }
                    if (_try.getResources() != null && compareTo.getResources() != null) {
                        for (int i = 0; i < _try.getResources().size(); i++) {
                            this.visit(_try.getResources().get(i), compareTo.getResources().get(i));
                        }
                    }
                    if (_try.getFinally() != null && compareTo.getFinally() != null) {
                        this.visit(_try.getFinally(), compareTo.getFinally());
                    }
                }
                return _try;
            }

            @Override
            public J.Try.Resource visitTryResource(J.Try.Resource tryResource, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Try.Resource)) {
                        isEqual.set(false);
                        return tryResource;
                    }

                    J.Try.Resource compareTo = (J.Try.Resource) j;
                    if (tryResource.isTerminatedWithSemicolon() != compareTo.isTerminatedWithSemicolon() ||
                            doesNotContainSameComments(tryResource.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return tryResource;
                    }
                    this.visit(tryResource.getVariableDeclarations(), compareTo.getVariableDeclarations());
                }
                return tryResource;
            }

            @Override
            public J.TypeCast visitTypeCast(J.TypeCast typeCast, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.TypeCast)) {
                        isEqual.set(false);
                        return typeCast;
                    }

                    J.TypeCast compareTo = (J.TypeCast) j;
                    if (doesNotContainSameComments(typeCast.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return typeCast;
                    }
                    this.visit(typeCast.getClazz(), compareTo.getClazz());
                    this.visit(typeCast.getExpression(), compareTo.getExpression());
                }
                return typeCast;
            }

            @Override
            public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.TypeParameter)) {
                        isEqual.set(false);
                        return typeParam;
                    }

                    J.TypeParameter compareTo = (J.TypeParameter) j;
                    if (typeParam.getAnnotations().size() != compareTo.getAnnotations().size() ||
                            nullMissMatch(typeParam.getBounds(), compareTo.getBounds()) ||
                            typeParam.getBounds().size() != compareTo.getBounds().size() ||
                            doesNotContainSameComments(typeParam.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return typeParam;
                    }
                    this.visit(typeParam.getName(), compareTo.getName());
                    for (int i = 0; i < typeParam.getAnnotations().size(); i++) {
                        this.visit(typeParam.getAnnotations().get(i), compareTo.getAnnotations().get(i));
                    }
                    if (typeParam.getBounds() != null && compareTo.getBounds() != null) {
                        for (int i = 0; i < typeParam.getBounds().size(); i++) {
                            this.visit(typeParam.getBounds().get(i), compareTo.getBounds().get(i));
                        }
                    }
                }
                return typeParam;
            }

            @Override
            public J.Unary visitUnary(J.Unary unary, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Unary)) {
                        isEqual.set(false);
                        return unary;
                    }

                    J.Unary compareTo = (J.Unary) j;
                    if (nullMissMatch(unary.getType(), compareTo.getType()) ||
                            !TypeUtils.isOfType(unary.getType(), compareTo.getType()) ||
                            unary.getOperator() != compareTo.getOperator() ||
                            doesNotContainSameComments(unary.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return unary;
                    }

                    this.visit(unary.getExpression(), compareTo.getExpression());
                }
                return unary;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.VariableDeclarations)) {
                        isEqual.set(false);
                        return multiVariable;
                    }

                    J.VariableDeclarations compareTo = (J.VariableDeclarations) j;
                    if (!TypeUtils.isOfType(multiVariable.getType(), compareTo.getType()) ||
                            nullMissMatch(multiVariable.getTypeExpression(), compareTo.getTypeExpression()) ||
                            multiVariable.getVariables().size() != compareTo.getVariables().size() ||
                            multiVariable.getLeadingAnnotations().size() != compareTo.getLeadingAnnotations().size() ||
                            doesNotContainSameComments(multiVariable.getPrefix(), compareTo.getPrefix()) ||
                            doesNotContainSameComments(multiVariable.getVarargs(), compareTo.getVarargs())) {
                        isEqual.set(false);
                        return multiVariable;
                    }

                    if (multiVariable.getTypeExpression() != null && compareTo.getTypeExpression() != null) {
                        this.visitTypeName(multiVariable.getTypeExpression(), compareTo.getTypeExpression());
                    }
                    for (int i = 0; i < multiVariable.getLeadingAnnotations().size(); i++) {
                        this.visit(multiVariable.getLeadingAnnotations().get(i), compareTo.getLeadingAnnotations().get(i));
                    }
                    for (int i = 0; i < multiVariable.getVariables().size(); i++) {
                        this.visit(multiVariable.getVariables().get(i), compareTo.getVariables().get(i));
                    }
                }
                return multiVariable;
            }

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.VariableDeclarations.NamedVariable)) {
                        isEqual.set(false);
                        return variable;
                    }

                    J.VariableDeclarations.NamedVariable compareTo = (J.VariableDeclarations.NamedVariable) j;
                    if (!TypeUtils.isOfType(variable.getType(), compareTo.getType()) ||
                            nullMissMatch(variable.getInitializer(), compareTo.getInitializer()) ||
                            doesNotContainSameComments(variable.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return variable;
                    }
                    this.visit(variable.getName(), compareTo.getName());
                    if (variable.getInitializer() != null && compareTo.getInitializer() != null) {
                        this.visit(variable.getInitializer(), compareTo.getInitializer());
                    }
                }
                return variable;
            }

            @Override
            public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.WhileLoop)) {
                        isEqual.set(false);
                        return whileLoop;
                    }

                    J.WhileLoop compareTo = (J.WhileLoop) j;
                    if (doesNotContainSameComments(whileLoop.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return whileLoop;
                    }
                    this.visit(whileLoop.getBody(), compareTo.getBody());
                    this.visit(whileLoop.getCondition(), compareTo.getCondition());
                }
                return whileLoop;
            }

            @Override
            public J.Wildcard visitWildcard(J.Wildcard wildcard, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof J.Wildcard)) {
                        isEqual.set(false);
                        return wildcard;
                    }

                    J.Wildcard compareTo = (J.Wildcard) j;
                    if (wildcard.getBound() != compareTo.getBound() ||
                            nullMissMatch(wildcard.getBoundedType(), compareTo.getBoundedType()) ||
                            doesNotContainSameComments(wildcard.getPrefix(), compareTo.getPrefix())) {
                        isEqual.set(false);
                        return wildcard;
                    }
                    if (wildcard.getBoundedType() != null && compareTo.getBoundedType() != null) {
                        this.visitTypeName(wildcard.getBoundedType(), compareTo.getBoundedType());
                    }
                }
                return wildcard;
            }

            @Override
            public <N extends NameTree> N visitTypeName(N firstTypeName, J j) {
                if (isEqual.get()) {
                    if (!(j instanceof NameTree) && !TypeUtils.isOfType(firstTypeName.getType(), ((NameTree) j).getType()) ||
                            doesNotContainSameComments(firstTypeName.getPrefix(), j.getPrefix())) {
                        isEqual.set(false);
                        return firstTypeName;
                    }
                }
                return firstTypeName;
            }
        }

        /**
         * Collection the caught exceptions from a {@link J.Try.Catch}.
         */
        private static Set<J.Identifier> getCaughtExceptions(J.Try.Catch aCatch) {
            Set<J.Identifier> caughtExceptions = new HashSet<>();
            if (isMultiCatch(aCatch)) {
                J.MultiCatch multiCatch = (J.MultiCatch) aCatch.getParameter().getTree().getTypeExpression();
                if (multiCatch != null) {
                    for (NameTree alternative : multiCatch.getAlternatives()) {
                        J.Identifier identifier = (J.Identifier) alternative;
                        caughtExceptions.add(identifier);
                    }
                }
            } else {
                J.Identifier identifier = (J.Identifier) aCatch.getParameter().getTree().getTypeExpression();
                if (identifier != null) {
                    caughtExceptions.add(identifier);
                }
            }
            return caughtExceptions;
        }

        /**
         * Returns true of a {@link J.Try.Catch} is a {@link J.MultiCatch}.
         * Note: A null type expression will produce a false negative, but the recipe will not
         * change catches with a null type.
         */
        private static boolean isMultiCatch(J.Try.Catch aCatch) {
            return aCatch.getParameter().getTree().getTypeExpression() instanceof J.MultiCatch;
        }
    }
}
