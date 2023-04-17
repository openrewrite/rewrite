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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.cleanup.ShortenFullyQualifiedTypeReferences;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Value
@EqualsAndHashCode(callSuper = false)
public class StandardizeNullabilityAnnotations extends Recipe {

    @Option(displayName = "Nullability annotations to use",
            description = "All other nullability annotations will be replaced by these.",
            example = "javax.annotation.Nullable")
    List<String> nullabilityAnnotationsFqn;

    @Option(displayName = "Additional nullability annotations that will be considered known to this recipe",
            description = "This option enables the recipe user to migrate to or away from nullability annotations that are not contained within the base set of known nullability annotations.",
            required = false)
    Set<NullabilityAnnotation> additionalNullabilityAnnotations;

    public StandardizeNullabilityAnnotations(List<String> nullabilityAnnotationsFqn) {
        this(nullabilityAnnotationsFqn, new HashSet<>());
    }

    @JsonCreator
    public StandardizeNullabilityAnnotations(List<String> nullabilityAnnotationsFqn, @Nullable Set<NullabilityAnnotation> additionalNullabilityAnnotations) {
        this.nullabilityAnnotationsFqn = nullabilityAnnotationsFqn;
        this.additionalNullabilityAnnotations = additionalNullabilityAnnotations != null ? additionalNullabilityAnnotations : new HashSet<>();
        // During replacement we insert annotation using their fully qualified name
        // To cleanup whereever possible afterwards we chain this recipe
        doNext(new ShortenFullyQualifiedTypeReferences());
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
    public Set<String> getTags() {
        return Stream.of("nullability").collect(Collectors.toSet());
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
        return Stream.concat(Arrays.stream(KnownNullabilityAnnotations.values()), additionalNullabilityAnnotations.stream()).collect(Collectors.toSet());
    }
}
