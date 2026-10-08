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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.yaml.trait.BlockScalar;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePropertyValue extends Recipe {
    @Option(displayName = "Property key",
            description = "The key to look for. Supports glob patterns.",
            example = "management.metrics.binders.*.enabled")
    String propertyKey;

    @Option(example = "newValue", displayName = "New value",
            description = "The new value to be used for key specified by `propertyKey`.")
    String newValue;

    @Option(example = "oldValue", displayName = "Old value",
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


    @Option(displayName = "File pattern",
            description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all.",
            required = false,
            example = ".github/workflows/*.yml")
    @Nullable
    String filePattern;

    String displayName = "Change YAML property";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", propertyKey, newValue);
    }

    String description = "Change a YAML property. Expects dot notation for nested YAML mappings, similar to how Spring Boot interprets `application.yml` files.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(
                Validated.test("oldValue", "is required if `regex` is enabled", oldValue,
                        value -> !(Boolean.TRUE.equals(regex) && StringUtils.isNullOrEmpty(value))));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        NameCaseConvention.Compiled keyMatcher = (!Boolean.FALSE.equals(relaxedBinding) ?
                NameCaseConvention.LOWER_CAMEL :
                NameCaseConvention.EXACT).compile(propertyKey);

        return Preconditions.check(new FindSourceFiles(filePattern), new YamlIsoVisitor<ExecutionContext>() {
            Set<UUID> anchorsToUpdate = emptySet();
            Map<UUID, Yaml.Scalar> aliasesToInline = emptyMap();

            @Override
            public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
                planAliasedValues(documents);
                return super.visitDocuments(documents, ctx);
            }

            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, ExecutionContext ctx) {
                Yaml.Scalar s = super.visitScalar(scalar, ctx);
                if (s.getAnchor() != null && anchorsToUpdate.contains(s.getAnchor().getId())) {
                    Yaml.Scalar updated = updateScalar(s, getCursor().getParentOrThrow());
                    if (updated != null) {
                        s = updated;
                    }
                }
                return s;
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                String prop = getProperty(getCursor());
                if (keyMatcher.matchesGlob(prop) && matchesOldValue(e.getValue(), aliasesToInline)) {
                    Yaml.Block updatedValue = updateValue(e.getValue(), getCursor(), aliasesToInline);
                    if (updatedValue != null) {
                        e = e.withValue(updatedValue);
                    }
                }
                return e;
            }

            // Change the anchor itself only when no other key aliases it; otherwise replace the matching aliases
            private void planAliasedValues(Yaml.Documents documents) {
                Map<UUID, Yaml.Scalar> anchored = new HashMap<>();
                Set<UUID> definedUnderMatchingKey = new HashSet<>();
                Set<UUID> aliasedFromMatchingKey = new HashSet<>();
                Set<UUID> aliasedElsewhere = new HashSet<>();
                new YamlIsoVisitor<Integer>() {
                    @Override
                    public Yaml.Scalar visitScalar(Yaml.Scalar scalar, Integer p) {
                        if (scalar.getAnchor() != null && !isMappingKey(getCursor())) {
                            anchored.put(scalar.getAnchor().getId(), scalar);
                            if (isValueOfMatchingKey(getCursor(), keyMatcher)) {
                                definedUnderMatchingKey.add(scalar.getAnchor().getId());
                            }
                        }
                        return super.visitScalar(scalar, p);
                    }

                    @Override
                    public Yaml visitAlias(Yaml.Alias alias, Integer p) {
                        (isValueOfMatchingKey(getCursor(), keyMatcher) ? aliasedFromMatchingKey : aliasedElsewhere)
                                .add(alias.getAnchor().getId());
                        return super.visitAlias(alias, p);
                    }
                }.visit(documents, 0);

                anchorsToUpdate = new HashSet<>();
                aliasesToInline = new HashMap<>();
                for (Map.Entry<UUID, Yaml.Scalar> anchor : anchored.entrySet()) {
                    UUID id = anchor.getKey();
                    Yaml.Scalar scalar = anchor.getValue();
                    if (!aliasedFromMatchingKey.contains(id) || definedUnderMatchingKey.contains(id) ||
                        !matchesOldValue(scalar, emptyMap())) {
                        continue;
                    }
                    if (!aliasedElsewhere.contains(id)) {
                        anchorsToUpdate.add(id);
                    } else if (isSingleLine(scalar)) {
                        aliasesToInline.put(id, scalar);
                    }
                }
            }
        });
    }

    private static boolean isValueOfMatchingKey(Cursor cursor, NameCaseConvention.Compiled keyMatcher) {
        Cursor value = cursor;
        Cursor parent = value.getParentTreeCursor();
        while (parent.getValue() instanceof Yaml.Sequence.Entry) {
            value = parent.getParentTreeCursor();
            parent = value.getParentTreeCursor();
        }
        return parent.getValue() instanceof Yaml.Mapping.Entry &&
               ((Yaml.Mapping.Entry) parent.getValue()).getValue() == value.getValue() &&
               keyMatcher.matchesGlob(getProperty(parent));
    }

    private static boolean isMappingKey(Cursor cursor) {
        Object parent = cursor.getParentTreeCursor().getValue();
        return parent instanceof Yaml.Mapping.Entry && ((Yaml.Mapping.Entry) parent).getKey() == cursor.getValue();
    }

    private static boolean isSingleLine(Yaml.Scalar scalar) {
        return scalar.getValue().indexOf('\n') == -1 && scalar.getValue().indexOf('\r') == -1;
    }

    // returns null if value should not change
    private Yaml.@Nullable Block updateValue(Yaml.Block value, Cursor parent, Map<UUID, Yaml.Scalar> aliasesToInline) {
        if (value instanceof Yaml.Scalar) {
            return updateScalar((Yaml.Scalar) value, parent);
        }
        if (value instanceof Yaml.Alias) {
            Yaml.Scalar target = aliasesToInline.get(((Yaml.Alias) value).getAnchor().getId());
            return target == null ? null : updateScalar(target
                    .withId(randomId())
                    .withPrefix(value.getPrefix())
                    .withAnchor(null), parent);
        }
        if (value instanceof Yaml.Sequence) {
            Yaml.Sequence sequence = (Yaml.Sequence) value;
            return sequence.withEntries(ListUtils.map(sequence.getEntries(), entry -> {
                if (matchesOldValue(entry.getBlock(), aliasesToInline)) {
                    Yaml.Block updatedValue = updateValue(entry.getBlock(), parent, aliasesToInline);
                    if (updatedValue != null) {
                        return entry.withBlock(updatedValue);
                    }
                }
                return entry;
            }));
        }
        return null;
    }

    private Yaml.@Nullable Scalar updateScalar(Yaml.Scalar scalar, Cursor parent) {
        BlockScalar block = new BlockScalar.Matcher().get(scalar, parent).orElse(null);
        String body = block != null ? block.getBody() : scalar.getValue();
        String updatedBody = Boolean.TRUE.equals(regex) ?
                body.replaceAll(Objects.requireNonNull(oldValue), newValue) :
                newValue;
        if (body.equals(updatedBody)) {
            return null;
        }
        return block != null ? block.withBody(updatedBody) : scalar.withValue(updatedBody);
    }

    private boolean matchesOldValue(Yaml.Block value, Map<UUID, Yaml.Scalar> aliasesToInline) {
        if (value instanceof Yaml.Scalar) {
            Yaml.Scalar scalar = (Yaml.Scalar) value;
            return StringUtils.isNullOrEmpty(oldValue) ||
                   (Boolean.TRUE.equals(regex) ?
                           Pattern.compile(oldValue).matcher(scalar.getValue()).find() :
                           scalar.getValue().equals(oldValue));
        } else if (value instanceof Yaml.Alias) {
            return aliasesToInline.containsKey(((Yaml.Alias) value).getAnchor().getId());
        } else if (value instanceof Yaml.Sequence) {
            for (Yaml.Sequence.Entry entry : ((Yaml.Sequence) value).getEntries()) {
                if (matchesOldValue(entry.getBlock(), aliasesToInline)) {
                    return true;
                }
            }
        }
        return false;
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
