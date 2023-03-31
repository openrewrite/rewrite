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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.NonNullApi;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Value
@NonNullApi
@EqualsAndHashCode(callSuper = false)
public class StandardizeNullabilityAnnotations extends Recipe {

    private static final Set<String> KNOWN_NULLABLE_ANNOTATIONS = Stream.of(
            "javax.annotation.Nullable",
            "jakarta.annotation.Nullable",
            "org.eclipse.jdt.annotation.Nullable",
            "org.jetbrains.annotations.Nullable",
            "org.netbeans.api.annotations.common.NullAllowed",
            "androidx.annotation.Nullable",
            "android.support.annotation.Nullable",
            "org.checkerframework.checker.nullness.qual.Nullable",
            "edu.umd.cs.findbugs.annotations.Nullable",
            "org.springframework.lang.Nullable",
            "org.jmlspecs.annotation.Nullable",
            "org.openrewrite.internal.lang.Nullable",
            "lombok.NonNull"
    ).collect(Collectors.toSet());

    private static final Set<String> KNOWN_NON_NULL_ANNOTATIONS = Stream.of(
            "javax.annotation.Nonnull",
            "jakarta.annotation.Nonnull",
            "org.eclipse.jdt.annotation.NonNull",
            "org.jetbrains.annotations.NotNull",
            "org.netbeans.api.annotations.common.NonNull",
            "androidx.annotation.NonNull",
            "android.support.annotation.NonNull",
            "org.checkerframework.checker.nullness.qual.NonNull",
            "edu.umd.cs.findbugs.annotations.NonNull",
            "org.springframework.lang.NonNull",
            "org.jmlspecs.annotation.NonNull",
            "org.openrewrite.internal.lang.NonNull"
    ).collect(Collectors.toSet());

    @Option(displayName = "Nullable Annotation to use",
            description = "All other nullable annotations will be replaced by this one.",
            example = "javax.annotation.Nullable")
    String nullableAnnotation;

    @Option(displayName = "Non-null annotation to use",
            description = "All other non-null annotations will be replaced by this one.",
            example = "javax.annotation.Nonnull")
    String nonNullAnnotation;

    public StandardizeNullabilityAnnotations(String nullableAnnotation, String nonNullAnnotation) {
        this.nullableAnnotation = nullableAnnotation;
        this.nonNullAnnotation = nonNullAnnotation;
        getNullableAnnotationToReplace().forEach(annotation -> doNext(new ChangeType(annotation, nullableAnnotation, true)));
        getNonNullAnnotationToReplace().forEach(annotation -> doNext(new ChangeType(annotation, nonNullAnnotation, true)));
    }

    @Override
    public String getDisplayName() {
        return "Standardize nullability annotations";
    }

    @Override
    public String getDescription() {
        return "Define one null and one non-null annotation to be used. All divergent annotations will be replaced.";
    }

    private Set<String> getNullableAnnotationToReplace() {
        return KNOWN_NULLABLE_ANNOTATIONS.stream().filter(annotation -> !nullableAnnotation.equals(annotation)).collect(Collectors.toSet());
    }

    private Set<String> getNonNullAnnotationToReplace() {
        return KNOWN_NULLABLE_ANNOTATIONS.stream().filter(annotation -> !nullableAnnotation.equals(annotation)).collect(Collectors.toSet());
    }
}
