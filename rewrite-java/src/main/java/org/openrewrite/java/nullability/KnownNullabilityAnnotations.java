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
import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
@AllArgsConstructor
public enum KnownNullabilityAnnotations implements NullabilityAnnotation {

    ANDROID_SUPPORT_NULLABLE("android.support.annotation.Nullable", Nullability.NULLABLE, EnumSet.of(Target.METHOD, Target.PARAMETER, Target.FIELD, Target.LOCAL_FIELD, Target.PACKAGE), EnumSet.allOf(Scope.class)),
    ANDROID_SUPPORT_NON_NULL("android.support.annotation.NonNull", Nullability.NONNULL, EnumSet.of(Target.METHOD, Target.PARAMETER, Target.FIELD, Target.LOCAL_FIELD, Target.PACKAGE), EnumSet.allOf(Scope.class)),

    ANDROIDX_SUPPORT_NULLABLE("androidx.annotation.Nullable", Nullability.NULLABLE, EnumSet.of(Target.METHOD, Target.PARAMETER, Target.FIELD, Target.LOCAL_FIELD, Target.PACKAGE), EnumSet.allOf(Scope.class)),
    ANDROIDX_SUPPORT_NON_NULL("androidx.annotation.NonNull", Nullability.NONNULL, EnumSet.of(Target.METHOD, Target.PARAMETER, Target.FIELD, Target.LOCAL_FIELD, Target.PACKAGE), EnumSet.allOf(Scope.class)),

    CHECKER_FRAMEWORK_NULLABLE("org.checkerframework.checker.nullness.qual.Nullable", Nullability.NULLABLE, EnumSet.of(Target.TYPE_USE), EnumSet.allOf(Scope.class)),
    CHECKER_FRAMEWORK_NON_NULL("org.checkerframework.checker.nullness.qual.NonNull", Nullability.NONNULL, EnumSet.of(Target.TYPE_USE), EnumSet.allOf(Scope.class)),

    ECLIPSE_JDT_NULLABLE("org.eclipse.jdt.annotation.Nullable", Nullability.NULLABLE, EnumSet.of(Target.TYPE_USE), EnumSet.allOf(Scope.class)),
    ECLIPSE_JDT_NON_NULL("org.eclipse.jdt.annotation.NonNull", Nullability.NONNULL, EnumSet.of(Target.TYPE_USE), EnumSet.allOf(Scope.class)),
    /*
     * The following annotation is commented out, because scopes it applies to are dynamically configurable with parameters.
     * To support this, we would need to convert the static scopes attribute to a provider function which takes the parsed annotation to work with its argument.
     * ECLIPSE_JDT_NON_NULL_BY_DEFAULT("org.eclipse.jdt.annotation.NonNullByDefault", Nullability.NONNULL, EnumSet.allOf(Target.class), annotation -> ...),
     */

    FINDBUGS_CHECK_FOR_NULL("edu.umd.cs.findbugs.annotations.CheckForNull", Nullability.NULLABLE, EnumSet.of(Target.FIELD, Target.METHOD, Target.PARAMETER, Target.LOCAL_FIELD), EnumSet.allOf(Scope.class)),
    FINDBUGS_NULLABLE("edu.umd.cs.findbugs.annotations.Nullable", Nullability.NULLABLE, EnumSet.of(Target.FIELD, Target.METHOD, Target.PARAMETER, Target.LOCAL_FIELD), EnumSet.allOf(Scope.class)),
    FINDBUGS_POSSIBLY_NULL("edu.umd.cs.findbugs.annotations.PossiblyNull", Nullability.NULLABLE, EnumSet.of(Target.FIELD, Target.METHOD, Target.PARAMETER, Target.LOCAL_FIELD), EnumSet.allOf(Scope.class)),
    FINDBUGS_NON_NULL("edu.umd.cs.findbugs.annotations.NonNull", Nullability.NONNULL, EnumSet.of(Target.FIELD, Target.METHOD, Target.PARAMETER, Target.LOCAL_FIELD), EnumSet.allOf(Scope.class)),
    FINDBUGS_UNKNOWN_NULLNESS("edu.umd.cs.findbugs.annotations.UnknownNullness", Nullability.UNKNOWN, EnumSet.of(Target.FIELD, Target.METHOD, Target.PARAMETER, Target.LOCAL_FIELD), EnumSet.allOf(Scope.class)),

    JAKARTA_NULLABLE("jakarta.annotation.Nullable", Nullability.NULLABLE, EnumSet.allOf(Target.class), EnumSet.allOf(Scope.class)),
    JAKARTA_NON_NULL("jakarta.annotation.Nonnull", Nullability.NONNULL, EnumSet.allOf(Target.class), EnumSet.allOf(Scope.class)),

    JAVAX_NULLABLE("javax.annotation.Nullable", Nullability.NULLABLE, EnumSet.allOf(Target.class), EnumSet.allOf(Scope.class)),
    JAVAX_NON_NULL("javax.annotation.Nonnull", Nullability.NONNULL, EnumSet.allOf(Target.class), EnumSet.allOf(Scope.class)),

    JETBRAINS_NULLABLE("org.jetbrains.annotations.Nullable", Nullability.NULLABLE, EnumSet.of(Target.METHOD, Target.FIELD, Target.PARAMETER, Target.LOCAL_FIELD, Target.TYPE_USE), EnumSet.allOf(Scope.class)),
    JETBRAINS_NON_NULL("org.jetbrains.annotations.NotNull", Nullability.NONNULL, EnumSet.of(Target.METHOD, Target.FIELD, Target.PARAMETER, Target.LOCAL_FIELD, Target.TYPE_USE), EnumSet.allOf(Scope.class)),
    JETBRAINS_UNKNOWN_NULLABILITY("org.jetbrains.annotations.UnknownNullability", Nullability.UNKNOWN, EnumSet.of(Target.TYPE_USE), EnumSet.allOf(Scope.class)),

    JML_SPECS_NULLABLE("org.jmlspecs.annotation.Nullable", Nullability.NULLABLE, EnumSet.of(Target.TYPE_USE, Target.FIELD, Target.METHOD, Target.LOCAL_FIELD, Target.PARAMETER), EnumSet.allOf(Scope.class)),
    JML_SPECS_NULLABLE_BY_DEFAULT("org.jmlspecs.annotation.NullableByDefault", Nullability.NULLABLE, EnumSet.allOf(Target.class), EnumSet.allOf(Scope.class)),
    JML_SPECS_NON_NULL("org.jmlspecs.annotation.NonNull", Nullability.NONNULL, EnumSet.of(Target.TYPE_USE, Target.FIELD, Target.METHOD, Target.LOCAL_FIELD, Target.PARAMETER), EnumSet.allOf(Scope.class)),
    JML_SPECS_NON_NULL_BY_DEFAULT("org.jmlspecs.annotation.NonNullByDefault", Nullability.NONNULL, EnumSet.allOf(Target.class), EnumSet.allOf(Scope.class)),

    JSPECIFY_NULLABLE("org.jspecify.annotations.Nullable", Nullability.NULLABLE, EnumSet.of(Target.TYPE_USE), EnumSet.allOf(Scope.class)),
    JSPECIFY_NON_NULL("org.jspecify.annotations.NonNull", Nullability.NONNULL, EnumSet.of(Target.TYPE_USE), EnumSet.allOf(Scope.class)),
    JSPECIFY_NULL_MARKED("org.jspecify.annotations.NullMarked", Nullability.NONNULL, EnumSet.of(Target.MODULE, Target.PACKAGE, Target.TYPE, Target.METHOD), EnumSet.allOf(Scope.class)),
    JSPECIFY_NULL_UNMARKED("org.jspecify.annotations.NullUnmarked", Nullability.UNKNOWN, EnumSet.of(Target.PACKAGE, Target.TYPE, Target.METHOD), EnumSet.allOf(Scope.class)),

    LOMBOK_NON_NULL("lombok.NonNull", Nullability.NONNULL, EnumSet.of(Target.FIELD, Target.METHOD, Target.PARAMETER, Target.LOCAL_FIELD, Target.TYPE_USE), EnumSet.allOf(Scope.class)),

    NETBEANS_CHECK_FOR_NULL("org.netbeans.api.annotations.common.CheckForNull", Nullability.NULLABLE, EnumSet.of(Target.METHOD), EnumSet.of(Scope.METHOD)),
    NETBEANS_NULL_ALLOWED("org.netbeans.api.annotations.common.NullAllowed", Nullability.NULLABLE, EnumSet.of(Target.FIELD, Target.PARAMETER, Target.LOCAL_FIELD), EnumSet.of(Scope.FIELD, Scope.PARAMETER)),
    NETBEANS_NON_NULL("org.netbeans.api.annotations.common.NonNull", Nullability.NONNULL, EnumSet.of(Target.FIELD, Target.METHOD, Target.PARAMETER, Target.LOCAL_FIELD), EnumSet.allOf(Scope.class)),
    NETBEANS_NULL_UNKNOWN("org.netbeans.api.annotations.common.NullUnknown", Nullability.UNKNOWN, EnumSet.of(Target.FIELD, Target.METHOD, Target.PARAMETER, Target.LOCAL_FIELD), EnumSet.allOf(Scope.class)),

    OPEN_REWRITE_NULLABLE("org.openrewrite.internal.lang.Nullable", Nullability.NULLABLE, EnumSet.of(Target.METHOD, Target.PARAMETER, Target.FIELD, Target.TYPE, Target.TYPE_USE), EnumSet.allOf(Scope.class)),
    OPEN_REWRITE_NULL_FIELDS("org.openrewrite.internal.lang.NullFields", Nullability.NULLABLE, EnumSet.of(Target.PACKAGE, Target.TYPE), EnumSet.of(Scope.FIELD)),
    OPEN_REWRITE_NON_NULL("org.openrewrite.internal.lang.NonNull", Nullability.NONNULL, EnumSet.of(Target.METHOD, Target.PARAMETER, Target.FIELD, Target.TYPE, Target.TYPE_USE), EnumSet.allOf(Scope.class)),
    OPEN_REWRITE_NON_NULL_FIELDS("org.openrewrite.internal.lang.NonNullFields", Nullability.NONNULL, EnumSet.of(Target.PACKAGE, Target.TYPE), EnumSet.of(Scope.FIELD)),
    OPEN_REWRITE_NON_NULL_API("org.openrewrite.internal.lang.NonNullApi", Nullability.NONNULL, EnumSet.of(Target.PACKAGE, Target.TYPE), EnumSet.of(Scope.METHOD, Scope.PARAMETER)),

    SPRING_NULLABLE("org.springframework.lang.Nullable", Nullability.NULLABLE, EnumSet.of(Target.METHOD, Target.PARAMETER, Target.FIELD), EnumSet.allOf(Scope.class)),
    SPRING_NON_NULL("org.springframework.lang.NonNull", Nullability.NONNULL, EnumSet.of(Target.METHOD, Target.PARAMETER, Target.FIELD), EnumSet.allOf(Scope.class)),
    SPRING_NON_NULL_FIELDS("org.springframework.lang.NonNullFields", Nullability.NONNULL, EnumSet.of(Target.PACKAGE), EnumSet.of(Scope.FIELD)),
    SPRING_NON_NULL_API("org.springframework.lang.NonNullApi", Nullability.NONNULL, EnumSet.of(Target.PACKAGE), EnumSet.of(Scope.METHOD, Scope.PARAMETER))
    ;

    private final String fqn;
    private final Nullability nullability;
    private final Set<Target> targets;
    private final Set<Scope> scopes;
}
