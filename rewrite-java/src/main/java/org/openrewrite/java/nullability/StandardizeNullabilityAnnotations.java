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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.ObjectUtils;
import org.openrewrite.*;

import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openrewrite.java.nullability.NullabilityAnnotation.nonNull;
import static org.openrewrite.java.nullability.NullabilityAnnotation.nullable;


@Value
@EqualsAndHashCode(callSuper = false)
public class StandardizeNullabilityAnnotations extends Recipe {

    // TODO fill in targets and scopes for all annotations
    private static final Set<NullabilityAnnotation> KNOWN_NULLABILITY_ANNOTATIONS = Stream.of(
            nonNull("android.annotation.NonNull"),
            nonNull("android.support.annotation.NonNull"),
            nonNull("android.support.annotation.RecentlyNonNull"),
            nonNull("androidx.annotation.NonNull"),
            nonNull("androidx.annotation.RecentlyNonNull"),
            nonNull("com.android.annotations.NonNull"),
            nonNull("com.google.firebase.database.annotations.NotNull"),
            nonNull("com.google.firebase.internal.NonNull"),
            nonNull("com.mongodb.lang.NonNull"),
            nonNull("com.sun.istack.NotNull"),
            nonNull("com.sun.istack.internal.NotNull"),
            nonNull("com.unboundid.util.NotNull"),
            nonNull("edu.umd.cs.findbugs.annotations.NonNull"),
            nonNull("io.micrometer.core.lang.NonNull"),
            nonNull("io.reactivex.annotations.NonNull"),
            nonNull("io.reactivex.rxjava3.annotations.NonNull"),
            nonNull("javax.annotation.Nonnull").withAllTargets().withScopes(),
            nonNull("javax.validation.constraints.NotNull"),
            nonNull("libcore.util.NonNull"),
            nonNull("lombok.NonNull"),
            nonNull("org.antlr.v4.runtime.misc.NotNull"),
            nonNull("org.checkerframework.checker.nullness.compatqual.NonNullDecl"),
            nonNull("org.checkerframework.checker.nullness.compatqual.NonNullType"),
            nonNull("org.checkerframework.checker.nullness.qual.NonNull"),
            nonNull("org.codehaus.commons.nullanalysis.NotNull"),
            nonNull("org.eclipse.jdt.annotation.NonNull"),
            nonNull("org.eclipse.jgit.annotations.NonNull"),
            nonNull("org.eclipse.lsp4j.jsonrpc.validation.NonNull"),
            nonNull("org.jetbrains.annotations.NotNull"),
            nonNull("org.jmlspecs.annotation.NonNull"),
            nonNull("org.netbeans.api.annotations.common.NonNull"),
            nonNull("org.openrewrite.internal.lang.NonNull").withAllTargets().withAllScopes(),
            nonNull("org.openrewrite.internal.lang.NonNullFields").withTargets(ElementType.PACKAGE, ElementType.TYPE).withScopes(ElementType.FIELD),
            nonNull("org.openrewrite.internal.lang.NonNullApi").withTargets(ElementType.PACKAGE, ElementType.TYPE).withScopes(ElementType.METHOD, ElementType.PARAMETER),
            nonNull("org.springframework.lang.NonNull"),
            nonNull("reactor.util.annotation.NonNull"),

            nullable("android.annotation.Nullable"),
            nullable("android.support.annotation.Nullable"),
            nullable("android.support.annotation.RecentlyNullable"),
            nullable("androidx.annotation.Nullable"),
            nullable("androidx.annotation.RecentlyNullable"),
            nullable("com.android.annotations.Nullable"),
            nullable("com.beust.jcommander.internal.Nullable"),
            nullable("com.google.api.server.spi.config.Nullable"),
            nullable("com.google.firebase.database.annotations.Nullable"),
            nullable("com.google.firebase.internal.Nullable"),
            nullable("com.google.gerrit.common.Nullable"),
            nullable("com.mongodb.lang.Nullable"),
            nullable("com.sun.istack.Nullable"),
            nullable("com.sun.istack.internal.Nullable"),
            nullable("com.unboundid.util.Nullable"),
            nullable("edu.umd.cs.findbugs.annotations.CheckForNull"),
            nullable("edu.umd.cs.findbugs.annotations.Nullable"),
            nullable("edu.umd.cs.findbugs.annotations.PossiblyNull"),
            nullable("edu.umd.cs.findbugs.annotations.UnknownNullness"),
            nullable("io.micrometer.core.lang.Nullable"),
            nullable("io.reactivex.annotations.Nullable"),
            nullable("io.reactivex.rxjava3.annotations.Nullable"),
            nullable("javax.annotation.CheckForNull"),
            nullable("javax.annotation.Nullable").withAllTargets().withScopes(),
            nullable("junitparams.converters.Nullable"),
            nullable("libcore.util.Nullable"),
            nullable("org.apache.avro.reflect.Nullable"),
            nullable("org.apache.cxf.jaxrs.ext.Nullable"),
            nullable("org.apache.shindig.common.Nullable"),
            nullable("org.checkerframework.checker.nullness.compatqual.NullableDecl"),
            nullable("org.checkerframework.checker.nullness.compatqual.NullableType"),
            nullable("org.checkerframework.checker.nullness.qual.Nullable"),
            nullable("org.codehaus.commons.nullanalysis.Nullable"),
            nullable("org.eclipse.jdt.annotation.Nullable"),
            nullable("org.eclipse.jgit.annotations.Nullable"),
            nullable("org.jetbrains.annotations.Nullable"),
            nullable("org.jetbrains.annotations.UnknownNullability"),
            nullable("org.jmlspecs.annotation.Nullable"),
            nullable("org.jspecify.nullness.Nullable"),
            nullable("org.jspecify.nullness.NullnessUnspecified"),
            nullable("org.netbeans.api.annotations.common.CheckForNull"),
            nullable("org.netbeans.api.annotations.common.NullAllowed"),
            nullable("org.netbeans.api.annotations.common.NullUnknown"),
            nullable("org.openrewrite.internal.lang.Nullable").withAllTargets().withAllScopes(),
            nullable("org.openrewrite.internal.lang.NullFields").withTargets(ElementType.PACKAGE, ElementType.TYPE).withScopes(ElementType.FIELD),
            nullable("org.springframework.lang.Nullable"),
            nullable("reactor.util.annotation.Nullable")
    ).collect(Collectors.toSet());

    @Option(displayName = "Nullability annotations to use",
            description = "All other nullability annotations will be replaced by these.",
            example = "javax.annotation.Nullable")
    List<String> nullabilityAnnotationsFqn;

    @Option(displayName = "Nullability annotations to use",
            description = "All other nullability annotations will be replaced by these.",
            required = false)
    Set<NullabilityAnnotation> additionalNullabilityAnnotations;

    public StandardizeNullabilityAnnotations(List<String> nullabilityAnnotationsFqn) {
        this(nullabilityAnnotationsFqn, new HashSet<>());
    }

    @JsonCreator
    public StandardizeNullabilityAnnotations(List<String> nullabilityAnnotationsFqn, Set<NullabilityAnnotation> additionalNullabilityAnnotations) {
        this.nullabilityAnnotationsFqn = nullabilityAnnotationsFqn;
        this.additionalNullabilityAnnotations = ObjectUtils.defaultIfNull(additionalNullabilityAnnotations, new HashSet<>());
    }

    @Override
    public String getDisplayName() {
        return "Standardize nullability annotations";
    }

    @Override
    public String getDescription() {
        return "Define one null and one non-null annotation to be used. All divergent annotations will be replaced.";
    }

    @Override
    public Validated validate() {
        return super.validate().and(Validated.test("nullableAnnotationsFqn", "must be resolvable as known nullability annotations", this.nullabilityAnnotationsFqn, fqns -> fqns.size() == getNullabilityAnnotations().size()));
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new StandardizeNullabilityAnnotationsVisitor(getKnownNullabilityAnnotations(), getNullabilityAnnotations());
    }

    private List<NullabilityAnnotation> getNullabilityAnnotations() {
        return getNullabilityAnnotationsFqn()
                .stream()
                .flatMap(fqn -> getKnownNullabilityAnnotations().stream().filter(annotation -> Objects.equals(fqn, annotation.getFqn())))
                .collect(Collectors.toList());
    }

    private Set<NullabilityAnnotation> getKnownNullabilityAnnotations() {
        return Stream.concat(KNOWN_NULLABILITY_ANNOTATIONS.stream(), additionalNullabilityAnnotations.stream())
                .filter(annotation -> !annotation.getTargets().isEmpty()) // TODO remove, when list of known annotation is fully setup
                .filter(annotation -> !annotation.getScopes().isEmpty()) // TODO remove, when list of known annotation is fully setup
                .collect(Collectors.toSet());
    }
}
