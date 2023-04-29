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
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.tree.Properties;

import java.util.Objects;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangePropertyValue extends Recipe {

    @Option(displayName = "Property key",
            description = "The name of the property key whose value is to be changed.",
            example = "management.metrics.binders.files.enabled")
    String propertyKey;

    @Option(displayName = "New value",
            description = "The new value to be used for key specified by `propertyKey`.")
    String newValue;

    @Option(displayName = "Old value",
            required = false,
            description = "Only change the property value if it matches the configured `oldValue`.")
    @Nullable
    String oldValue;

    @Option(displayName = "Regex",
            description = "Default false. If enabled, `oldValue` will be interepreted as a Regular Expression, and capture group contents will be available in `newValue`",
            required = false)
    @Nullable
    Boolean regex;

    @Incubating(since = "7.17.0")
    @Option(displayName = "Use relaxed binding",
            description = "Whether to match the `propertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " +
                    "rules. Default is `true`. Set to `false`  to use exact matching.",
            required = false)
    @Nullable
    Boolean relaxedBinding;

    @Override
    public String getDisplayName() {
        return "Change property value";
    }

    @Override
    public String getDescription() {
        return "Change a property value leaving the key intact.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangePropertyValueVisitor<>();
    }

    public class ChangePropertyValueVisitor<P> extends PropertiesVisitor<P> {
        public ChangePropertyValueVisitor() {
        }

        @Override
        public Properties visitEntry(Properties.Entry entry, P p) {
            if (!Boolean.FALSE.equals(relaxedBinding) ? NameCaseConvention.equalsRelaxedBinding(entry.getKey(), propertyKey) : entry.getKey().equals(propertyKey)) {
                if (matchesOldValue(entry.getValue())) {
                    Properties.Value updatedValue = updateValue(entry.getValue());
                    if (updatedValue != null) {
                        entry = entry.withValue(updatedValue);
                    }
                }
            }
            return super.visitEntry(entry, p);
        }

        @Nullable // returns null if value should not change
        private Properties.Value updateValue(Properties.Value value) {
            Properties.Value updatedValue = value.withText(Boolean.TRUE.equals(regex)
                    ? value.getText().replaceAll(Objects.requireNonNull(oldValue), newValue) : newValue);
            return updatedValue.getText().equals(value.getText()) ? null : updatedValue;
        }

        private boolean matchesOldValue(Properties.Value value) {
            return StringUtils.isNullOrEmpty(oldValue) ||
                    (Boolean.TRUE.equals(regex)
                            ? value.getText().matches(oldValue)
                            : value.getText().equals(oldValue));
        }
    }

}
