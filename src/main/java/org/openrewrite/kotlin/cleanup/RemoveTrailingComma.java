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
package org.openrewrite.kotlin.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.kotlin.format.TrailingCommaVisitor;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveTrailingComma extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove trailing comma in Kotlin";
    }

    @Override
    public String getDescription() {
        return "Remove trailing commas in variable, parameter, and class property lists.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TrailingCommaVisitor<>(false);
    }
}
