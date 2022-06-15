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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.tree.Properties;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangePropertyValue extends Recipe {

    @Option(displayName = "Property key",
            description = "The name of the property key whose value is to be changed.",
            example = "management.metrics.binders.files.enabled")
    String propertyKey;

    @Option(displayName = "New value",
            description = "The new value to be used for key specified by `propertyKey`.",
            example = "false")
    String newValue;

    @Option(displayName = "Old value",
            example = "true",
            required = false,
            description = "Only change the property value if it matches the configured `oldValue`.")
    @Nullable
    String oldValue;

    @Incubating(since = "7.17.0")
    @Option(displayName = "Use relaxed binding",
            description = "Whether to match the `propertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " +
                    "rules. Default is `true`. Set to `false`  to use exact matching.",
            required = false)
    @Nullable
    Boolean relaxedBinding;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "'**/application-*.properties'")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Change property value";
    }

    @Override
    public String getDescription() {
        return "Change a property value leaving the key intact.";
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
        return new ChangePropertyValueVisitor<>();
    }

    public class ChangePropertyValueVisitor<P> extends PropertiesVisitor<P> {
        public ChangePropertyValueVisitor() {
        }

        @Override
        public Properties visitEntry(Properties.Entry entry, P p) {
            if (!Boolean.FALSE.equals(relaxedBinding) ? NameCaseConvention.equalsRelaxedBinding(entry.getKey(), propertyKey) : entry.getKey().equals(propertyKey)) {
                if (oldValue == null ? !entry.getValue().getText().equals(newValue) : oldValue.equals(entry.getValue().getText())) {
                    entry = entry.withValue(entry.getValue().withText(newValue));
                }
            }
            return super.visitEntry(entry, p);
        }
    }

}
