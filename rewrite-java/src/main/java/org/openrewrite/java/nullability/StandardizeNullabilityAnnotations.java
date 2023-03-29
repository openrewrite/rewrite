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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.NonNullApi;


@Value
@NonNullApi
@EqualsAndHashCode(callSuper = false)
public class StandardizeNullabilityAnnotations extends Recipe {

    @Option(displayName = "Nullable Annotation to use",
            description = "All other nullable annotations will be replaced by this one",
            example = "javax.annotation.Nullable")
    String nullableAnnotation;

    @Option(displayName = "Non-null annotation to use",
            description = "All other non-null annotations will be replaced by this one",
            example = "javax.annotation.Nonnull")
    String nonNullAnnotation;

    @Override
    public String getDisplayName() {
        return "Standardize nullability annotations";
    }

    @Override
    public String getDescription() {
        return "Define one null and one non-null annotation to be used. All divergent annotations will be replaced.";
    }
}
