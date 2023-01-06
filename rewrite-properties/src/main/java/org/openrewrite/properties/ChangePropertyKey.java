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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.tree.Properties;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangePropertyKey extends Recipe {

    @Option(displayName = "Old property key",
            description = "The property key to rename.",
            example = "management.metrics.binders.files.enabled")
    String oldPropertyKey;

    @Option(displayName = "New property key",
            description = "The new name for the key identified by `oldPropertyKey`.",
            example = "management.metrics.enable.process.files")
    String newPropertyKey;

    @Incubating(since = "7.17.0")
    @Option(displayName = "Use relaxed binding",
            description = "Whether to match the `oldPropertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " +
                    "rules. Default is `true`. Set to `false`  to use exact matching.",
            required = false)
    @Nullable
    Boolean relaxedBinding;

    @Incubating(since = "7.8.0")
    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/application-*.properties")
    @Nullable
    String fileMatcher;

    @Option(displayName = "Regex",
            description = "Default false. If enabled, `oldPropertyKey` will be interepreted as a Regular Expression, and capture group contents will be available in `newPropertyKey`",
            required = false,
            example = "true")
    @Nullable
    Boolean regex;

    @Deprecated
    public ChangePropertyKey(String oldPropertyKey, String newPropertyKey,
            @Nullable Boolean relaxedBinding, @Nullable String fileMatcher) {
        this(oldPropertyKey, newPropertyKey, relaxedBinding, fileMatcher, null);
    }

    @JsonCreator
    public ChangePropertyKey(String oldPropertyKey, String newPropertyKey,
            @Nullable Boolean relaxedBinding, @Nullable String fileMatcher,
            @Nullable Boolean regex) {
        this.oldPropertyKey = oldPropertyKey;
        this.newPropertyKey = newPropertyKey;
        this.relaxedBinding = relaxedBinding;
        this.fileMatcher = fileMatcher;
        this.regex = regex;
    }

    @Override
    public String getDisplayName() {
        return "Change property key";
    }

    @Override
    public String getDescription() {
        return "Change a property key leaving the value intact.";
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
        return new ChangePropertyKeyVisitor<>();
    }

    public class ChangePropertyKeyVisitor<P> extends PropertiesVisitor<P> {
        public ChangePropertyKeyVisitor() {
        }

        @Override
        public Properties visitEntry(Properties.Entry entry, P p) {
            if (Boolean.TRUE.equals(regex)) {
                if (!Boolean.FALSE.equals(relaxedBinding)
                        ? NameCaseConvention.matchesRegexRelaxedBinding(entry.getKey(), oldPropertyKey)
                        : entry.getKey().matches(oldPropertyKey)) {
                    entry = entry.withKey(entry.getKey().replaceFirst(oldPropertyKey, newPropertyKey))
                            .withPrefix(entry.getPrefix());
                }
            } else {
                if (!Boolean.FALSE.equals(relaxedBinding)
                        ? NameCaseConvention.equalsRelaxedBinding(entry.getKey(), oldPropertyKey)
                        : entry.getKey().equals(oldPropertyKey)) {
                    entry = entry.withKey(newPropertyKey)
                            .withPrefix(entry.getPrefix());
                }
            }
            return super.visitEntry(entry, p);
        }
    }
}
