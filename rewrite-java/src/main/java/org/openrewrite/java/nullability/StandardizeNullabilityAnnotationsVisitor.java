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
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.lang.annotation.ElementType;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
        J.ClassDeclaration classDeclaration = super.visitClassDeclaration(classDecl, executionContext);
        Set<MatchedNullabilityAnnotation> matchedNullabilityAnnotations = getCursor().pollMessage(NULLABILITY_ANNOTATION_MARKER);
        if (matchedNullabilityAnnotations == null || matchedNullabilityAnnotations.isEmpty()) {
            return classDeclaration;
        }
        List<NullabilityAnnotation> annotationsForReplacement = getAnnotationsForReplacement(ElementType.TYPE, matchedNullabilityAnnotations);
        if (annotationsForReplacement.isEmpty()) {
            return classDeclaration;
        }

        maybeRemoveMatchedAnnotationImports(matchedNullabilityAnnotations);
        maybeAddReplacementAnnotationImports(annotationsForReplacement);

        List<J.Annotation> currentNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getJAnnotation).collect(Collectors.toList());
        List<J.Annotation> cleanedAnnotations = new LinkedList<>(classDeclaration.getLeadingAnnotations());
        cleanedAnnotations.removeAll(currentNullabilityAnnotations);

        J.ClassDeclaration cleanClassDeclaration = classDeclaration.withLeadingAnnotations(cleanedAnnotations);
        return annotationsForReplacement.stream().reduce(
                cleanClassDeclaration,
                (J.ClassDeclaration currentDeclaration, NullabilityAnnotation annotation) -> currentDeclaration.withTemplate(
                        JavaTemplate.builder(this::getCursor, "@" + annotation.getSimpleName()).build(),
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
        List<NullabilityAnnotation> annotationsForReplacement = getAnnotationsForReplacement(ElementType.METHOD, matchedNullabilityAnnotations);
        if (annotationsForReplacement.isEmpty()) {
            return methodDeclaration;
        }

        maybeRemoveMatchedAnnotationImports(matchedNullabilityAnnotations);
        maybeAddReplacementAnnotationImports(annotationsForReplacement);

        List<J.Annotation> currentNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getJAnnotation).collect(Collectors.toList());
        List<J.Annotation> cleanedAnnotations = new LinkedList<>(methodDeclaration.getLeadingAnnotations());
        cleanedAnnotations.removeAll(currentNullabilityAnnotations);

        J.MethodDeclaration cleanedMethodDeclaration = methodDeclaration.withLeadingAnnotations(cleanedAnnotations);
        return annotationsForReplacement.stream().reduce(
                cleanedMethodDeclaration,
                (J.MethodDeclaration currentDeclaration, NullabilityAnnotation annotation) -> currentDeclaration.withTemplate(
                        JavaTemplate.builder(this::getCursor, "@" + annotation.getSimpleName()).build(),
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
        List<NullabilityAnnotation> annotationsForReplacement = getAnnotationsForReplacement(ElementType.PACKAGE, matchedNullabilityAnnotations);
        if (annotationsForReplacement.isEmpty()) {
            return jPackage;
        }

        maybeRemoveMatchedAnnotationImports(matchedNullabilityAnnotations);
        maybeAddReplacementAnnotationImports(annotationsForReplacement);

        List<J.Annotation> currentNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getJAnnotation).collect(Collectors.toList());
        List<J.Annotation> cleanedAnnotations = new LinkedList<>(jPackage.getAnnotations());
        cleanedAnnotations.removeAll(currentNullabilityAnnotations);

        J.Package cleanedPackage = jPackage.withAnnotations(cleanedAnnotations);
        return annotationsForReplacement.stream().reduce(
                cleanedPackage,
                (J.Package currentDeclaration, NullabilityAnnotation annotation) -> currentDeclaration.withTemplate(
                        JavaTemplate.builder(this::getCursor, "@" + annotation.getSimpleName()).build(),
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
        List<NullabilityAnnotation> annotationsForReplacement = getAnnotationsForReplacement(ElementType.METHOD, matchedNullabilityAnnotations);
        if (annotationsForReplacement.isEmpty()) {
            return variableDeclarations;
        }

        maybeRemoveMatchedAnnotationImports(matchedNullabilityAnnotations);
        maybeAddReplacementAnnotationImports(annotationsForReplacement);

        List<J.Annotation> currentNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getJAnnotation).collect(Collectors.toList());
        List<J.Annotation> cleanedAnnotations = new LinkedList<>(variableDeclarations.getLeadingAnnotations());
        cleanedAnnotations.removeAll(currentNullabilityAnnotations);

        J.VariableDeclarations cleanedVariableDeclaration = variableDeclarations.withLeadingAnnotations(cleanedAnnotations);
        return annotationsForReplacement.stream().reduce(
                cleanedVariableDeclaration,
                (J.VariableDeclarations currentDeclaration, NullabilityAnnotation annotation) -> currentDeclaration.withTemplate(
                        JavaTemplate.builder(this::getCursor, "@" + annotation.getSimpleName()).build(),
                        currentDeclaration.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName))
                ), (oldDeclaration, newDeclaration) -> newDeclaration
        );
    }

    private List<NullabilityAnnotation> getAnnotationsForReplacement(ElementType targetType, Set<MatchedNullabilityAnnotation> matchedNullabilityAnnotations) {
        Set<NullabilityAnnotation> usedNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getNullabilityAnnotation).collect(Collectors.toSet());
        Set<NullabilityAnnotation.Nullability> matchedNullabilities = usedNullabilityAnnotations.stream().map(NullabilityAnnotation::getNullability).collect(Collectors.toSet());
        if (matchedNullabilities.isEmpty()) {
            // no nullabilities, nothing to do
            return emptyList();
        }
        if (matchedNullabilities.size() > 1) {
            // different nullabilities on a single element, we better do nothing

            // TODO log useful information
            // Thought:
            // Maybe we should transfer the logging to the caller
            // as that code knows more about the element we failed at
            return emptyList();
        }

        NullabilityAnnotation.Nullability nullability = matchedNullabilities.stream().findFirst().orElse(null);
        Set<NullabilityAnnotation.Scope> scopesToCover = usedNullabilityAnnotations
                .stream()
                .map(NullabilityAnnotation::getScopes)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        if (scopesToCover.isEmpty()) {
            return emptyList();
        }

        List<NullabilityAnnotation> annotationsForReplacement = getAnnotationsForReplacement(nullability, targetType, scopesToCover);
        if (annotationsForReplacement.isEmpty()) {
            // We were not able to find a good replacement.

            // TODO log useful information
            // Thought:
            // Maybe we should transfer the logging to the caller
            // as that code knows more about the element we failed at.
        }
        if (annotationsForReplacement.containsAll(usedNullabilityAnnotations) && usedNullabilityAnnotations.containsAll(annotationsForReplacement)) {
            // Our replacement would not change used annotations.
            // Only the order may have changed as we collect matched annotation in a set.
            // Anyway, no semantic change -> we return an empty list.
            return emptyList();
        }
        return annotationsForReplacement;
    }

    private List<NullabilityAnnotation> getAnnotationsForReplacement(NullabilityAnnotation.Nullability nullability, ElementType targetType, Set<NullabilityAnnotation.Scope> scopesToCover) {
        List<NullabilityAnnotation> usableNullabilityAnnotations = getNullabilityAnnotationsForTarget(nullability, targetType);
        Optional<NullabilityAnnotation> singleAnnotation = usableNullabilityAnnotations
                .stream()
                .filter(a -> Objects.equals(scopesToCover, a.getScopes()))
                .findFirst();

        if (singleAnnotation.isPresent()) {
            return Collections.singletonList(singleAnnotation.get());
        }

        Set<NullabilityAnnotation.Scope> coveredScopes = new HashSet<>();
        List<NullabilityAnnotation> usedAnnotations = new LinkedList<>();
        for (NullabilityAnnotation possibleAnnotation : usableNullabilityAnnotations) {
            if (Objects.equals(scopesToCover, coveredScopes)) {
                break;
            }
            coveredScopes.addAll(possibleAnnotation.getScopes());
            usedAnnotations.add(possibleAnnotation);
        }
        return Objects.equals(scopesToCover, coveredScopes)
                ? usedAnnotations
                : emptyList();
    }

    private List<NullabilityAnnotation> getNullabilityAnnotationsForTarget(NullabilityAnnotation.Nullability nullability, ElementType target) {
        return nullabilityAnnotations
                .stream()
                .filter(a -> a.getNullability() == nullability)
                .filter(a -> a.getTargets().contains(target))
                .collect(Collectors.toList());
    }

    private void maybeRemoveMatchedAnnotationImports(Collection<MatchedNullabilityAnnotation> matchedAnnotations) {
        matchedAnnotations.forEach(a -> maybeRemoveImport(a.getNullabilityAnnotation().getFqn()));
    }

    private void maybeAddReplacementAnnotationImports(Collection<NullabilityAnnotation> annotations) {
        // Maybe add import does only check for uses. This currently does not work properly with used annotations.
        // As work around we always add imports. We can safely do this, because
        // a) we know the annotations are used
        // b) we know that no fully qualified identifiers were used
        // TODO check again after resolving the class path issue in the test cases
        annotations.forEach(a -> maybeAddImport(a.getFqn(), false));
    }


    @Value
    @AllArgsConstructor
    private static class MatchedNullabilityAnnotation {

        J.Annotation jAnnotation;

        NullabilityAnnotation nullabilityAnnotation;
    }
}
