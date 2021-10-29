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

@EqualsAndHashCode(callSuper = true)
@Value
public class MergeProperty extends Recipe {

    @Option(displayName = "Old property key",
            description = "The property key to rename.",
            example = "metrics")
    String oldKey;

    @Option(displayName = "Property snippet",
            description = "The property snippet to replace the old property key.",
            example = "prometheus.metrics")
    String newKey;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/application-*.properties")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Merge properties";
    }

    @Override
    public String getDescription() {
        return "Replaces all the matching keys with the new key.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitEntry(Properties.Entry entry, ExecutionContext executionContext) {
                if (!entry.getKey().contains(newKey) && entry.getKey().contains(oldKey)) {
                    int indexOfKey = entry.getKey().indexOf(oldKey);
                    if (indexOfKey == 0) {
                        String suffix = entry.getKey().substring(oldKey.length());
                        return entry.withKey(newKey + suffix);
                    } else {
                        String prefix = entry.getKey().substring(0, indexOfKey);
                        String suffix = entry.getKey().substring(prefix.length() + oldKey.length());
                        return entry.withKey(prefix + newKey + suffix);
                    }
                }
                return super.visitEntry(entry, executionContext);
            }
        };
    }
}
