/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.properties;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.tree.Properties;

@Value
@EqualsAndHashCode(callSuper = true)
public class DeleteProperty extends Recipe {

    @Override
    public String getDisplayName() {
        return "Delete Property";
    }

    @Override
    public String getDescription() {
        return "Deletes key/value pairs from properties files.";
    }

    @Option(displayName = "Property key",
            description = "The key to be deleted.",
            example = "management.metrics.binders.files.enabled")
    String propertyKey;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/application-*.properties")
    @Nullable
    String fileMatcher;

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    protected PropertiesVisitor<ExecutionContext> getVisitor() {
        return new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitEntry(Properties.Entry entry, ExecutionContext context) {
                if (entry.getKey().equals(propertyKey)) {
                    //noinspection ConstantConditions
                    return null;
                }
                return super.visitEntry(entry, context);
            }
        };
    }
}
