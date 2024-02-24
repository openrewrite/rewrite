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
package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePropertyValue extends Recipe {
    @Option(displayName = "Property key",
            description = "The key to look for. Supports glob patterns.",
            example = "management.metrics.binders.*.enabled")
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
            description = "Default `false`. If enabled, `oldValue` will be interpreted as a Regular Expression, " +
                          "to replace only all parts that match the regex. Capturing group can be used in `newValue`.",
            required = false)
    @Nullable
    Boolean regex;

    @Option(displayName = "Use relaxed binding",
            description = "Whether to match the `propertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " +
                          "rules. Default is `true`. Set to `false`  to use exact matching.",
            required = false)
    @Nullable
    Boolean relaxedBinding;

    @Override
    public String getDisplayName() {
        return "Change YAML property";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", propertyKey, newValue);
    }

    @Override
    public String getDescription() {
        return "Change a YAML property. Expects dot notation for nested YAML mappings, similar to how Spring Boot interprets `application.yml` files.";
    }

    @Override
    public Validated validate() {
        return super.validate().and(
                Validated.test("oldValue", "is required if `regex` is enabled", oldValue,
                        value -> !(Boolean.TRUE.equals(regex) && StringUtils.isNullOrEmpty(value))));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                String prop = getProperty(getCursor());
                if (matchesPropertyKey(prop) && matchesOldValue(e.getValue())) {
                    Yaml.Scalar updatedValue = updateValue(e.getValue());
                    if (updatedValue != null) {
                        e = e.withValue(updatedValue);
                    }
                }
                return e;
            }
        };
    }

    @Nullable // returns null if value should not change
    private Yaml.Scalar updateValue(Yaml.Block value) {
        if (!(value instanceof Yaml.Scalar)) {
            return null;
        }
        Yaml.Scalar scalar = (Yaml.Scalar) value;
        Yaml.Scalar newScalar = scalar.withValue(Boolean.TRUE.equals(regex)
                ? scalar.getValue().replaceAll(Objects.requireNonNull(oldValue), newValue)
                : newValue);
        return scalar.getValue().equals(newScalar.getValue()) ? null : newScalar;
    }

    private boolean matchesPropertyKey(String prop) {
        return !Boolean.FALSE.equals(relaxedBinding)
                ? NameCaseConvention.matchesGlobRelaxedBinding(prop, propertyKey)
                : StringUtils.matchesGlob(prop, propertyKey);
    }

    private boolean matchesOldValue(Yaml.Block value) {
        if (!(value instanceof Yaml.Scalar)) {
            return false;
        }
        Yaml.Scalar scalar = (Yaml.Scalar) value;
        return StringUtils.isNullOrEmpty(oldValue) ||
               (Boolean.TRUE.equals(regex)
                       ? Pattern.compile(oldValue).matcher(scalar.getValue()).find()
                       : scalar.getValue().equals(oldValue));
    }

    private static String getProperty(Cursor cursor) {
        StringBuilder asProperty = new StringBuilder();
        Iterator<Object> path = cursor.getPath();
        int i = 0;
        while (path.hasNext()) {
            Object next = path.next();
            if (next instanceof Yaml.Mapping.Entry) {
                Yaml.Mapping.Entry entry = (Yaml.Mapping.Entry) next;
                if (i++ > 0) {
                    asProperty.insert(0, '.');
                }
                asProperty.insert(0, entry.getKey().getValue());
            }
        }
        return asProperty.toString();
    }
}
