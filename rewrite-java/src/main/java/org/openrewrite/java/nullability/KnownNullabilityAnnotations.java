package org.openrewrite.java.nullability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum KnownNullabilityAnnotations implements NullabilityAnnotation {

    ANDROID_SUPPORT_NULLABLE("android.support.annotation.Nullable", Nullability.NULLABLE, asSet(ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE, ElementType.PACKAGE), allScopes()),
    ANDROID_SUPPORT_NON_NULL("android.support.annotation.NonNull", Nullability.NONNULL, asSet(ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE, ElementType.PACKAGE), allScopes()),

    ANDROIDX_SUPPORT_NULLABLE("androidx.annotation.Nullable", Nullability.NULLABLE, asSet(ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE, ElementType.PACKAGE), allScopes()),
    ANDROIDX_SUPPORT_NON_NULL("androidx.annotation.NonNull", Nullability.NONNULL, asSet(ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE, ElementType.PACKAGE), allScopes()),

    CHECKER_FRAMEWORK_NULLABLE("org.checkerframework.checker.nullness.qual.Nullable", Nullability.NULLABLE, asSet(ElementType.TYPE_USE, ElementType.TYPE_PARAMETER), allScopes()),
    CHECKER_FRAMEWORK_NON_NULL("org.checkerframework.checker.nullness.qual.NonNull", Nullability.NONNULL, asSet(ElementType.TYPE_USE, ElementType.TYPE_PARAMETER), allScopes()),

    ECLIPSE_JDT_NULLABLE("org.eclipse.jdt.annotation.Nullable", Nullability.NULLABLE, asSet(ElementType.TYPE_USE), allScopes()),
    ECLIPSE_JDT_NON_NULL("org.eclipse.jdt.annotation.NonNull", Nullability.NONNULL, asSet(ElementType.TYPE_USE), allScopes()),
    /*
     * The following annotation is commented out, because scopes it applies to are dynamically configurable with parameters.
     * To support this, we would need to convert the static scopes attribute to a provider function which takes the parsed annotation to work with its argument.
     * ECLIPSE_JDT_NON_NULL_BY_DEFAULT("org.eclipse.jdt.annotation.NonNullByDefault", Nullability.NONNULL, allTargets(), annotation -> ...),
     */

    FINDBUGS_CHECK_FOR_NULL("edu.umd.cs.findbugs.annotations.CheckForNull", Nullability.NULLABLE, asSet(ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE), allScopes()),
    FINDBUGS_NULLABLE("edu.umd.cs.findbugs.annotations.Nullable", Nullability.NULLABLE, asSet(ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE), allScopes()),
    FINDBUGS_POSSIBLY_NULL("edu.umd.cs.findbugs.annotations.PossiblyNull", Nullability.NULLABLE, asSet(ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE), allScopes()),
    FINDBUGS_NON_NULL("edu.umd.cs.findbugs.annotations.NonNull", Nullability.NONNULL, asSet(ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE), allScopes()),
    FINDBUGS_UNKNOWN_NULLNESS("edu.umd.cs.findbugs.annotations.UnknownNullness", Nullability.UNKNOWN, asSet(ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE), allScopes()),

    JAKARTA_NULLABLE("jakarta.annotation.Nullable", Nullability.NULLABLE, allTargets(), allScopes()),
    JAKARTA_NON_NULL("jakarta.annotation.Nonnull", Nullability.NONNULL, allTargets(), allScopes()),

    JAVAX_NULLABLE("javax.annotation.Nullable", Nullability.NULLABLE, allTargets(), allScopes()),
    JAVAX_NON_NULL("javax.annotation.Nonnull", Nullability.NONNULL, allTargets(), allScopes()),

    JETBRAINS_NULLABLE("org.jetbrains.annotations.Nullable", Nullability.NULLABLE, asSet(ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE), allScopes()),
    JETBRAINS_NON_NULL("org.jetbrains.annotations.NotNull", Nullability.NONNULL, asSet(ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE), allScopes()),
    JETBRAINS_UNKNOWN_NULLABILITY("org.jetbrains.annotations.UnknownNullability", Nullability.UNKNOWN, asSet(ElementType.TYPE_USE), allScopes()),

    JML_SPECS_NULLABLE("org.jmlspecs.annotation.Nullable", Nullability.NULLABLE, asSet(ElementType.TYPE_USE, ElementType.TYPE_PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER), allScopes()),
    JML_SPECS_NULLABLE_BY_DEFAULT("org.jmlspecs.annotation.NullableByDefault", Nullability.NULLABLE, allTargets(), allScopes()),
    JML_SPECS_NON_NULL("org.jmlspecs.annotation.NonNull", Nullability.NONNULL, asSet(ElementType.TYPE_USE, ElementType.TYPE_PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER), allScopes()),
    JML_SPECS_NON_NULL_BY_DEFAULT("org.jmlspecs.annotation.NonNullByDefault", Nullability.NONNULL, allTargets(), allScopes()),

    JSPECIFY_NULLABLE("org.jspecify.annotations.Nullable", Nullability.NULLABLE, asSet(ElementType.TYPE_USE), allScopes()),
    JSPECIFY_NON_NULL("org.jspecify.annotations.NonNull", Nullability.NONNULL, asSet(ElementType.TYPE_USE), allScopes()),
    JSPECIFY_NULL_MARKED("org.jspecify.annotations.NullMarked", Nullability.NONNULL, asSet(/* ElementType.MODULE, */ ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR), allScopes()),
    JSPECIFY_NULL_UNMARKED("org.jspecify.annotations.NullUnmarked", Nullability.UNKNOWN, asSet(ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR), allScopes()),

    LOMBOK_NON_NULL("lombok.NonNull", Nullability.NONNULL, asSet(ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE), allScopes()),

    NETBEANS_CHECK_FOR_NULL("org.netbeans.api.annotations.common.CheckForNull", Nullability.NULLABLE, asSet(ElementType.METHOD), asSet(Scope.METHOD)),
    NETBEANS_NULL_ALLOWED("org.netbeans.api.annotations.common.NullAllowed", Nullability.NULLABLE, asSet(ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE), asSet(Scope.FIELD, Scope.PARAMETER)),
    NETBEANS_NON_NULL("org.netbeans.api.annotations.common.NonNull", Nullability.NONNULL, asSet(ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE), allScopes()),
    NETBEANS_NULL_UNKNOWN("org.netbeans.api.annotations.common.NullUnknown", Nullability.UNKNOWN, asSet(ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE), allScopes()),

    OPEN_REWRITE_NULLABLE("org.openrewrite.internal.lang.Nullable", Nullability.NULLABLE, allTargets(), allScopes()),
    OPEN_REWRITE_NULL_FIELDS("org.openrewrite.internal.lang.NullFields", Nullability.NULLABLE, asSet(ElementType.PACKAGE, ElementType.TYPE), asSet(Scope.FIELD)),
    OPEN_REWRITE_NON_NULL("org.openrewrite.internal.lang.NonNull", Nullability.NONNULL, allTargets(), allScopes()),
    OPEN_REWRITE_NON_NULL_FIELDS("org.openrewrite.internal.lang.NonNullFields", Nullability.NONNULL, asSet(ElementType.PACKAGE, ElementType.TYPE), asSet(Scope.FIELD)),
    OPEN_REWRITE_NON_NULL_API("org.openrewrite.internal.lang.NonNullApi", Nullability.NONNULL, asSet(ElementType.PACKAGE, ElementType.TYPE), asSet(Scope.METHOD, Scope.PARAMETER)),

    SPRING_NULLABLE("org.springframework.lang.Nullable", Nullability.NULLABLE, asSet(ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD), allScopes()),
    SPRING_NON_NULL("org.springframework.lang.NonNull", Nullability.NONNULL, asSet(ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD), allScopes()),
    SPRING_NON_NULL_FIELDS("org.springframework.lang.NonNullFields", Nullability.NONNULL, asSet(ElementType.PACKAGE), asSet(Scope.FIELD)),
    SPRING_NON_NULL_API("org.springframework.lang.NonNullApi", Nullability.NONNULL, asSet(ElementType.PACKAGE), asSet(Scope.METHOD, Scope.PARAMETER))
    ;

    private final String fqn;
    private final Nullability nullability;
    private final Set<ElementType> targets;
    private final Set<Scope> scopes;

    private static Set<ElementType> allTargets() {
        return asSet(ElementType.values());
    }

    private static Set<Scope> allScopes() {
        return asSet(Scope.values());
    }

    private static <T> Set<T> asSet(T... elements) {
        return Arrays.stream(elements).collect(Collectors.toSet());
    }
}
