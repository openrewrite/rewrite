/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.search.UsesType;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveAnnotation extends Recipe {
    @Option(displayName = "Annotation pattern",
            description = "An annotation pattern, expressed as a method pattern.",
            example = "@java.lang.SuppressWarnings(\"deprecation\")")
    String annotationPattern;

    String displayName = "Remove annotation";

    String description = "Remove matching annotations wherever they occur.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        AnnotationMatcher annotationMatcher = new AnnotationMatcher(annotationPattern);
        // Only run on source files that reference the annotation type. The visitor is reachable
        // directly (e.g. via `getVisitor().visitNonNull(...)` on a non-source-file node), where the
        // precondition is bypassed, so this only filters whole-file traversals.
        return Preconditions.check(new UsesType<>(annotationMatcher.getAnnotationName(), true),
                new RemoveAnnotationVisitor(annotationMatcher));
    }
}
