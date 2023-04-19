/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.nullability;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

class StandardizeNullabilityAnnotationsVisitor extends JavaIsoVisitor<ExecutionContext> {

    private static final String NULLABILITY_ANNOTATION_MARKER = "nullabilityAnnotations";

    private final Map<String, NullabilityAnnotation> knownNullabilityDictionary;

    private final List<NullabilityAnnotation> nullabilityAnnotations;

    public StandardizeNullabilityAnnotationsVisitor(Set<NullabilityAnnotation> knownNullabilityAnnotations, List<NullabilityAnnotation> nullabilityAnnotations) {
        this.knownNullabilityDictionary = knownNullabilityAnnotations.stream().collect(Collectors.toMap(NullabilityAnnotation::getFqn, Function.identity()));
        this.nullabilityAnnotations = nullabilityAnnotations;
    }


    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext executionContext) {
        J.Annotation jAnnotation = super.visitAnnotation(annotation, executionContext);
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(jAnnotation.getType());
        if (fullyQualified != null && knownNullabilityDictionary.containsKey(fullyQualified.getFullyQualifiedName())) {
            NullabilityAnnotation nullabilityAnnotation = knownNullabilityDictionary.get(fullyQualified.getFullyQualifiedName());
            getCursor().getParentOrThrow()
                    .computeMessageIfAbsent(NULLABILITY_ANNOTATION_MARKER, k -> new HashSet<>())
                    .add(new MatchedNullabilityAnnotation(jAnnotation, nullabilityAnnotation));
        }
        return jAnnotation;
    }

    @Override
    public J.AnnotatedType visitAnnotatedType(J.AnnotatedType t, ExecutionContext executionContext) {
        J.AnnotatedType annotatedType = super.visitAnnotatedType(t, executionContext);
        Set<MatchedNullabilityAnnotation> matchedNullabilityAnnotations = getCursor().pollMessage(NULLABILITY_ANNOTATION_MARKER);
        if (matchedNullabilityAnnotations == null || matchedNullabilityAnnotations.isEmpty()) {
            return annotatedType;
        }
        Set<NullabilityAnnotation.Target> targetTypes = new HashSet<>();
        targetTypes.add(NullabilityAnnotation.Target.TYPE_USE);
        Cursor parentScopeCursor = getCursor().dropParentUntil(o -> o instanceof J.ClassDeclaration || o instanceof J.MethodDeclaration || Cursor.ROOT_VALUE.equals(o));
        if (Cursor.ROOT_VALUE.equals(parentScopeCursor.getValue())) {
            targetTypes.add(NullabilityAnnotation.Target.LOCAL_FIELD);
        } else if (parentScopeCursor.getValue() instanceof J.MethodDeclaration) {
            targetTypes.add(NullabilityAnnotation.Target.PARAMETER);
        } else if (parentScopeCursor.getValue() instanceof J.ClassDeclaration) {
            targetTypes.add(NullabilityAnnotation.Target.FIELD);
        }
        LinkedHashSet<NullabilityAnnotation> annotationsForReplacement = getAnnotationsForReplacement(matchedNullabilityAnnotations, targetTypes);
        if (annotationsForReplacement.isEmpty()) {
            return annotatedType;
        }

        maybeRemoveMatchedAnnotationImports(matchedNullabilityAnnotations);

        List<J.Annotation> currentNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getJAnnotation).collect(Collectors.toList());
        List<J.Annotation> cleanedAnnotations = new LinkedList<>(annotatedType.getAnnotations());
        cleanedAnnotations.removeAll(currentNullabilityAnnotations);

        J.AnnotatedType cleanAnnotatedType = annotatedType.withAnnotations(cleanedAnnotations);
        J.AnnotatedType typeWithNewAnnotations = annotationsForReplacement.stream().reduce(
                cleanAnnotatedType,
                (J.AnnotatedType currentType, NullabilityAnnotation annotation) -> currentType.withTemplate(
                        annotationTemplate(annotation),
                        currentType.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName))
                ), (oldType, newType) -> newType
        );
        Space originalPrefix = annotatedType.getAnnotations().isEmpty()
            ? annotatedType.getTypeExpression().getPrefix()
            : annotatedType.getAnnotations().get(0).getPrefix();
        return typeWithNewAnnotations.withAnnotations(ListUtils.mapFirst(typeWithNewAnnotations.getAnnotations(), a -> a.withPrefix(originalPrefix)));
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
        J.ClassDeclaration classDeclaration = super.visitClassDeclaration(classDecl, executionContext);
        Set<MatchedNullabilityAnnotation> matchedNullabilityAnnotations = getCursor().pollMessage(NULLABILITY_ANNOTATION_MARKER);
        if (matchedNullabilityAnnotations == null || matchedNullabilityAnnotations.isEmpty()) {
            return classDeclaration;
        }
        LinkedHashSet<NullabilityAnnotation> annotationsForReplacement = getAnnotationsForReplacement(matchedNullabilityAnnotations, EnumSet.of(NullabilityAnnotation.Target.TYPE, NullabilityAnnotation.Target.TYPE_USE));
        if (annotationsForReplacement.isEmpty()) {
            return classDeclaration;
        }

        maybeRemoveMatchedAnnotationImports(matchedNullabilityAnnotations);

        List<J.Annotation> currentNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getJAnnotation).collect(Collectors.toList());
        List<J.Annotation> cleanedAnnotations = new LinkedList<>(classDeclaration.getLeadingAnnotations());
        cleanedAnnotations.removeAll(currentNullabilityAnnotations);

        J.ClassDeclaration cleanClassDeclaration = classDeclaration.withLeadingAnnotations(cleanedAnnotations);
        return annotationsForReplacement.stream().reduce(
                cleanClassDeclaration,
                (J.ClassDeclaration currentDeclaration, NullabilityAnnotation annotation) -> currentDeclaration.withTemplate(
                        annotationTemplate(annotation),
                        currentDeclaration.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName))
                ), (oldDeclaration, newDeclaration) -> newDeclaration
        );
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
        J.MethodDeclaration methodDeclaration = super.visitMethodDeclaration(method, executionContext);
        Set<MatchedNullabilityAnnotation> matchedNullabilityAnnotations = getCursor().pollMessage(NULLABILITY_ANNOTATION_MARKER);
        if (matchedNullabilityAnnotations == null || matchedNullabilityAnnotations.isEmpty()) {
            return methodDeclaration;
        }
        LinkedHashSet<NullabilityAnnotation> annotationsForReplacement = getAnnotationsForReplacement(matchedNullabilityAnnotations, EnumSet.of(NullabilityAnnotation.Target.METHOD, NullabilityAnnotation.Target.TYPE_USE));
        if (annotationsForReplacement.isEmpty()) {
            return methodDeclaration;
        }

        maybeRemoveMatchedAnnotationImports(matchedNullabilityAnnotations);

        List<J.Annotation> currentNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getJAnnotation).collect(Collectors.toList());
        List<J.Annotation> cleanedAnnotations = new LinkedList<>(methodDeclaration.getLeadingAnnotations());
        cleanedAnnotations.removeAll(currentNullabilityAnnotations);

        J.MethodDeclaration cleanedMethodDeclaration = methodDeclaration.withLeadingAnnotations(cleanedAnnotations);
        return annotationsForReplacement.stream().reduce(
                cleanedMethodDeclaration,
                (J.MethodDeclaration currentDeclaration, NullabilityAnnotation annotation) -> currentDeclaration.withTemplate(
                        annotationTemplate(annotation),
                        currentDeclaration.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName))
                ), (oldDeclaration, newDeclaration) -> newDeclaration
        );
    }

    @Override
    public J.Package visitPackage(J.Package pkg, ExecutionContext executionContext) {
        J.Package jPackage = super.visitPackage(pkg, executionContext);
        Set<MatchedNullabilityAnnotation> matchedNullabilityAnnotations = getCursor().pollMessage(NULLABILITY_ANNOTATION_MARKER);
        if (matchedNullabilityAnnotations == null || matchedNullabilityAnnotations.isEmpty()) {
            return jPackage;
        }
        LinkedHashSet<NullabilityAnnotation> annotationsForReplacement = getAnnotationsForReplacement(matchedNullabilityAnnotations, EnumSet.of(NullabilityAnnotation.Target.PACKAGE));
        if (annotationsForReplacement.isEmpty()) {
            return jPackage;
        }

        maybeRemoveMatchedAnnotationImports(matchedNullabilityAnnotations);

        List<J.Annotation> currentNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getJAnnotation).collect(Collectors.toList());
        List<J.Annotation> cleanedAnnotations = new LinkedList<>(jPackage.getAnnotations());
        cleanedAnnotations.removeAll(currentNullabilityAnnotations);

        J.Package cleanedPackage = jPackage.withAnnotations(cleanedAnnotations);
        return annotationsForReplacement.stream().reduce(
                cleanedPackage,
                (J.Package currentDeclaration, NullabilityAnnotation annotation) -> currentDeclaration.withTemplate(
                        annotationTemplate(annotation),
                        currentDeclaration.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName))
                ), (oldDeclaration, newDeclaration) -> newDeclaration
        );
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
        J.VariableDeclarations variableDeclarations = super.visitVariableDeclarations(multiVariable, executionContext);
        Set<MatchedNullabilityAnnotation> matchedNullabilityAnnotations = getCursor().pollMessage(NULLABILITY_ANNOTATION_MARKER);
        if (matchedNullabilityAnnotations == null || matchedNullabilityAnnotations.isEmpty()) {
            return variableDeclarations;
        }
        Set<NullabilityAnnotation.Target> targetTypes = new HashSet<>();
        targetTypes.add(NullabilityAnnotation.Target.TYPE_USE);
        Cursor parentScopeCursor = getCursor().dropParentUntil(o -> o instanceof J.ClassDeclaration || o instanceof J.MethodDeclaration || Cursor.ROOT_VALUE.equals(o));
        if (Cursor.ROOT_VALUE.equals(parentScopeCursor.getValue())) {
            targetTypes.add(NullabilityAnnotation.Target.LOCAL_FIELD);
        } else if (parentScopeCursor.getValue() instanceof J.MethodDeclaration) {
            targetTypes.add(NullabilityAnnotation.Target.PARAMETER);
        } else if (parentScopeCursor.getValue() instanceof J.ClassDeclaration) {
            targetTypes.add(NullabilityAnnotation.Target.FIELD);
        }
        LinkedHashSet<NullabilityAnnotation> annotationsForReplacement = getAnnotationsForReplacement(matchedNullabilityAnnotations, targetTypes);
        if (annotationsForReplacement.isEmpty()) {
            return variableDeclarations;
        }

        maybeRemoveMatchedAnnotationImports(matchedNullabilityAnnotations);

        List<J.Annotation> currentNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getJAnnotation).collect(Collectors.toList());
        List<J.Annotation> cleanedAnnotations = new LinkedList<>(variableDeclarations.getLeadingAnnotations());
        cleanedAnnotations.removeAll(currentNullabilityAnnotations);

        J.VariableDeclarations cleanedVariableDeclaration = variableDeclarations.withLeadingAnnotations(cleanedAnnotations);
        return annotationsForReplacement.stream().reduce(
                cleanedVariableDeclaration,
                (J.VariableDeclarations currentDeclaration, NullabilityAnnotation annotation) -> currentDeclaration.withTemplate(
                        annotationTemplate(annotation),
                        currentDeclaration.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName))
                ), (oldDeclaration, newDeclaration) -> newDeclaration
        );
    }

    private LinkedHashSet<NullabilityAnnotation> getAnnotationsForReplacement(Set<MatchedNullabilityAnnotation> matchedNullabilityAnnotations, Set<NullabilityAnnotation.Target> possibleTargetTypes) {
        Set<NullabilityAnnotation> usedNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getNullabilityAnnotation).collect(Collectors.toSet());
        Set<NullabilityAnnotation.Nullability> matchedNullabilities = usedNullabilityAnnotations.stream().map(NullabilityAnnotation::getNullability).collect(Collectors.toSet());
        if (matchedNullabilities.isEmpty()) {
            // no nullabilities, nothing to do
            return new LinkedHashSet<>();
        }
        if (matchedNullabilities.size() > 1) {
            // different nullabilities on a single element, we better do nothing

            // TODO log useful information
            // Thought:
            // Maybe we should transfer the logging to the caller
            // as that code knows more about the element we failed at
            return new LinkedHashSet<>();
        }

        NullabilityAnnotation.Nullability nullability = matchedNullabilities.stream().findFirst().orElse(null);
        Set<NullabilityAnnotation.Scope> scopesToCover = usedNullabilityAnnotations
                .stream()
                .map(NullabilityAnnotation::getScopes)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        if (scopesToCover.isEmpty()) {
            return new LinkedHashSet<>();
        }

        LinkedHashSet<NullabilityAnnotation> annotationsForReplacement = getAnnotationsForReplacement(nullability, possibleTargetTypes, scopesToCover);
        if (annotationsForReplacement.isEmpty()) {
            // We were not able to find a good replacement.

            // TODO log useful information
            // Thought:
            // Maybe we should transfer the logging to the caller
            // as that code knows more about the element we failed at.
        }
        if (Objects.equals(usedNullabilityAnnotations, annotationsForReplacement)) {
            // Our replacement would not change used annotations.
            // Only the order may have changed as we collect matched annotation in a set.
            // Anyway, no semantic change -> we return an empty list.
            return new LinkedHashSet<>();
        }
        return annotationsForReplacement;
    }

    private LinkedHashSet<NullabilityAnnotation> getAnnotationsForReplacement(NullabilityAnnotation.Nullability nullability, Set<NullabilityAnnotation.Target> targetTypes, Set<NullabilityAnnotation.Scope> scopesToCover) {
        List<NullabilityAnnotation> usableNullabilityAnnotations = getNullabilityAnnotationsForTargets(nullability, targetTypes);
        Optional<NullabilityAnnotation> singleAnnotation = usableNullabilityAnnotations
                .stream()
                .filter(a -> Objects.equals(scopesToCover, a.getScopes()))
                .findFirst();

        if (singleAnnotation.isPresent()) {
            LinkedHashSet<NullabilityAnnotation> result = new LinkedHashSet<>();
            result.add(singleAnnotation.get());
            return result;
        }

        Set<NullabilityAnnotation.Scope> coveredScopes = new HashSet<>();
        LinkedHashSet<NullabilityAnnotation> usedAnnotations = new LinkedHashSet<>();
        for (NullabilityAnnotation possibleAnnotation : usableNullabilityAnnotations) {
            if (Objects.equals(scopesToCover, coveredScopes)) {
                break;
            }
            coveredScopes.addAll(possibleAnnotation.getScopes());
            usedAnnotations.add(possibleAnnotation);
        }
        return Objects.equals(scopesToCover, coveredScopes)
                ? usedAnnotations
                : new LinkedHashSet<>();
    }

    private List<NullabilityAnnotation> getNullabilityAnnotationsForTargets(NullabilityAnnotation.Nullability nullability, Set<NullabilityAnnotation.Target> targets) {
        return nullabilityAnnotations
                .stream()
                .filter(a -> a.getNullability() == nullability)
                .filter(a -> a.getTargets().stream().anyMatch(targets::contains))
                .collect(Collectors.toList());
    }

    private void maybeRemoveMatchedAnnotationImports(Collection<MatchedNullabilityAnnotation> matchedAnnotations) {
        matchedAnnotations.forEach(a -> maybeRemoveImport(a.getNullabilityAnnotation().getFqn()));
    }

    private JavaTemplate annotationTemplate(NullabilityAnnotation annotation) {
        return JavaTemplate.builder(this::getCursor, "@" + annotation.getFqn())
                .imports(annotation.getFqn())
                .javaParser(() -> JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()).build())
                .build();
    }

    @Value
    @AllArgsConstructor
    private static class MatchedNullabilityAnnotation {

        J.Annotation jAnnotation;

        NullabilityAnnotation nullabilityAnnotation;
    }
}
