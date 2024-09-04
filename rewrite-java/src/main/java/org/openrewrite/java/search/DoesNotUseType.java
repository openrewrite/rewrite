/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class DoesNotUseType extends Recipe {

    @Option(displayName = "Fully-qualified type name",
            description = "A fully-qualified type name, that is used to find matching type references. " +
                          "Supports glob expressions. `java..*` finds every type from every subpackage of the `java` package.",
            example = "java.util.List")
    String fullyQualifiedTypeName;

    @Option(displayName = "Include implicit type references",
            description = "Whether to include implicit type references, such as those in method signatures.",
            required = false)
    @Nullable
    Boolean includeImplicit;

    @Override
    public String getDisplayName() {
        return "Check whether a type is **not** in use";
    }

    @Override
    public String getDescription() {
        return "Useful as a precondition to skip over compilation units using the argument type.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.not(new UsesType<>(fullyQualifiedTypeName, includeImplicit));
    }
}
