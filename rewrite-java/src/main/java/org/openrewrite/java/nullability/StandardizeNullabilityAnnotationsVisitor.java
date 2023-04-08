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

        List<J.Annotation> currentNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getJAnnotation).collect(Collectors.toList());
        List<J.Annotation> cleanedAnnotations = new LinkedList<>(classDeclaration.getLeadingAnnotations());
        cleanedAnnotations.removeAll(currentNullabilityAnnotations);

        // TODO add imports if necessary
        // TODO remove imports if no more necessary
        J.ClassDeclaration cleanClassDeclaration = classDeclaration.withLeadingAnnotations(cleanedAnnotations);
        return annotationsForReplacement.stream().reduce(
                cleanClassDeclaration,
                (J.ClassDeclaration currentDeclaration, NullabilityAnnotation annotation) -> currentDeclaration.withTemplate(
                        JavaTemplate.builder(this::getCursor, "@" + annotation.getFqn()).build(),
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

        List<J.Annotation> currentNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getJAnnotation).collect(Collectors.toList());
        List<J.Annotation> cleanedAnnotations = new LinkedList<>(methodDeclaration.getLeadingAnnotations());
        cleanedAnnotations.removeAll(currentNullabilityAnnotations);

        // TODO add imports if necessary
        // TODO remove imports if no more necessary
        J.MethodDeclaration cleanedMethodDeclaration = methodDeclaration.withLeadingAnnotations(cleanedAnnotations);
        return annotationsForReplacement.stream().reduce(
                cleanedMethodDeclaration,
                (J.MethodDeclaration currentDeclaration, NullabilityAnnotation annotation) -> currentDeclaration.withTemplate(
                        JavaTemplate.builder(this::getCursor, "@" + annotation.getFqn()).build(),
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

        List<J.Annotation> currentNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getJAnnotation).collect(Collectors.toList());
        List<J.Annotation> cleanedAnnotations = new LinkedList<>(jPackage.getAnnotations());
        cleanedAnnotations.removeAll(currentNullabilityAnnotations);

        // TODO add imports if necessary
        // TODO remove imports if no more necessary
        J.Package cleanedPackage = jPackage.withAnnotations(cleanedAnnotations);
        return annotationsForReplacement.stream().reduce(
                cleanedPackage,
                (J.Package currentDeclaration, NullabilityAnnotation annotation) -> currentDeclaration.withTemplate(
                        JavaTemplate.builder(this::getCursor, "@" + annotation.getFqn()).build(),
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

        List<J.Annotation> currentNullabilityAnnotations = matchedNullabilityAnnotations.stream().map(MatchedNullabilityAnnotation::getJAnnotation).collect(Collectors.toList());
        List<J.Annotation> cleanedAnnotations = new LinkedList<>(variableDeclarations.getLeadingAnnotations());
        cleanedAnnotations.removeAll(currentNullabilityAnnotations);

        // TODO add imports if necessary
        // TODO remove imports if no more necessary
        J.VariableDeclarations cleanedVariableDeclaration = variableDeclarations.withLeadingAnnotations(cleanedAnnotations);
        return annotationsForReplacement.stream().reduce(
                cleanedVariableDeclaration,
                (J.VariableDeclarations currentDeclaration, NullabilityAnnotation annotation) -> currentDeclaration.withTemplate(
                        JavaTemplate.builder(this::getCursor, "@" + annotation.getFqn()).build(),
                        currentDeclaration.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName))
                ), (oldDeclaration, newDeclaration) -> newDeclaration
        );
    }

    private List<NullabilityAnnotation> getAnnotationsForReplacement(ElementType targetType, Set<MatchedNullabilityAnnotation> matchedNullabilityAnnotations) {
        Set<ElementType> scopesToCover = matchedNullabilityAnnotations.stream().flatMap(a -> a.getNullabilityAnnotation().getScopes().stream()).collect(Collectors.toSet());

        List<NullabilityAnnotation> usableNullabilityAnnotations = getNullabilityAnnotationsForTarget(targetType);
        Optional<NullabilityAnnotation> singleAnnotation = usableNullabilityAnnotations
                .stream()
                .filter(a -> Objects.equals(scopesToCover, a.getScopes()))
                .findFirst();

        if (singleAnnotation.isPresent()) {
            return Collections.singletonList(singleAnnotation.get());
        }

        Set<ElementType> coveredScopes = new HashSet<>();
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
                : Collections.emptyList();
    }

    private List<NullabilityAnnotation> getNullabilityAnnotationsForTarget(ElementType target) {
        return nullabilityAnnotations.stream().filter(a -> a.getTargets().contains(target)).collect(Collectors.toList());
    }


    @Value
    @AllArgsConstructor
    private static class MatchedNullabilityAnnotation {

        J.Annotation jAnnotation;

        NullabilityAnnotation nullabilityAnnotation;
    }
}
