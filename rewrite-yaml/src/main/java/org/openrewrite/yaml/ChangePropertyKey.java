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
package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.search.FindProperty;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.openrewrite.Tree.randomId;

/**
 * Nested YAML mappings are interpreted as dot
 * separated property names, e.g. as Spring Boot
 * interprets application.yml files.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePropertyKey extends Recipe {

    @Option(displayName = "Old property key",
            description = "The property key to rename. Supports glob patterns.",
            example = "management.metrics.binders.*.enabled")
    String oldPropertyKey;

    @Option(displayName = "New property key",
            description = "The new name for the property key.",
            example = "management.metrics.enable.process.files")
    String newPropertyKey;

    @Option(displayName = "Use relaxed binding",
            description = "Whether to match the `oldPropertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " +
                    "rules. Defaults to `true`. If you want to use exact matching in your search, set this to `false`.",
            required = false)
    @Nullable
    Boolean relaxedBinding;

    @Option(displayName = "Except",
            description = "If any of these property keys exist as direct children of `oldPropertyKey`, then they will not be moved to `newPropertyKey`.",
            required = false)
    @Nullable
    List<String> except;

    @Override
    public String getDisplayName() {
        return "Change property key";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", oldPropertyKey, newPropertyKey);
    }

    @Override
    public String getDescription() {
        return "Change a YAML property key while leaving the value intact. Expects dot notation for nested YAML mappings, similar to how Spring Boot interprets `application.yml` files.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangePropertyKeyVisitor<>();
    }

    private class ChangePropertyKeyVisitor<P> extends YamlIsoVisitor<P> {
        @Override
        public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
            Yaml.Mapping.Entry e = super.visitMappingEntry(entry, p);
            if (getCursor().firstEnclosing(Yaml.Sequence.class) != null) {
                return e;
            }
            Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream()
                    .filter(Yaml.Mapping.Entry.class::isInstance)
                    .map(Yaml.Mapping.Entry.class::cast)
                    .collect(Collectors.toCollection(ArrayDeque::new));

            String prop = stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false)
                    .map(e2 -> e2.getKey().getValue())
                    .collect(Collectors.joining("."));

            if (newPropertyKey.startsWith(oldPropertyKey)
                    && (matches(prop, newPropertyKey) || matches(prop, newPropertyKey + ".*") || childMatchesNewPropertyKey(entry, prop))) {
                return e;
            }

            String propertyToTest = newPropertyKey;
            if (matches(prop, oldPropertyKey)) {
                Iterator<Yaml.Mapping.Entry> propertyEntriesLeftToRight = propertyEntries.descendingIterator();
                while (propertyEntriesLeftToRight.hasNext()) {
                    Yaml.Mapping.Entry propertyEntry = propertyEntriesLeftToRight.next();
                    String value = propertyEntry.getKey().getValue() + ".";

                    if ((!propertyToTest.startsWith(value ) || (propertyToTest.startsWith(value) && !propertyEntriesLeftToRight.hasNext()))
                        && hasNonExcludedValues(propertyEntry)) {
                        doAfterVisit(new InsertSubpropertyVisitor<>(
                                propertyEntry,
                                propertyToTest,
                                entry
                        ));
                        break;
                    }
                    propertyToTest = propertyToTest.substring(value.length());
                }
            } else {
                String parentProp = prop.substring(0, prop.length() - e.getKey().getValue().length()).replaceAll(".$", "");
                if (matches(prop, oldPropertyKey + ".*") &&
                        !(matches(parentProp, oldPropertyKey + ".*") || matches(parentProp, oldPropertyKey)) &&
                        noneMatch(prop, oldPropertyKey, excludedSubKeys())) {
                    Iterator<Yaml.Mapping.Entry> propertyEntriesLeftToRight = propertyEntries.descendingIterator();
                    while (propertyEntriesLeftToRight.hasNext()) {
                        Yaml.Mapping.Entry propertyEntry = propertyEntriesLeftToRight.next();
                        String value = propertyEntry.getKey().getValue() + ".";

                        if (!propertyToTest.startsWith(value ) || (propertyToTest.startsWith(value) && !propertyEntriesLeftToRight.hasNext())) {
                            doAfterVisit(new InsertSubpropertyVisitor<>(
                                    propertyEntry,
                                    propertyToTest + prop.substring(oldPropertyKey.length()),
                                    entry
                            ));
                            break;
                        }
                        propertyToTest = propertyToTest.substring(value.length());
                    }
                }
            }

            return e;
        }

        private boolean childMatchesNewPropertyKey(Yaml.Mapping.Entry entry, String cursorPropertyKey) {
            String rescopedNewPropertyKey = newPropertyKey.replaceFirst(
                    Pattern.quote(cursorPropertyKey),
                    entry.getKey().getValue());
            return !FindProperty.find(entry, rescopedNewPropertyKey, relaxedBinding).isEmpty();
        }
    }

    private boolean hasNonExcludedValues(Yaml.Mapping.Entry propertyEntry) {
        if (!(propertyEntry.getValue() instanceof Yaml.Mapping)) {
            return true;
        } else {
            for (Yaml.Mapping.Entry entry : ((Yaml.Mapping) propertyEntry.getValue()).getEntries()) {
                if (noneMatch(entry, excludedSubKeys())) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean hasExcludedValues(Yaml.Mapping.Entry propertyEntry) {
        if (propertyEntry.getValue() instanceof Yaml.Mapping) {
            for (Yaml.Mapping.Entry entry : ((Yaml.Mapping) propertyEntry.getValue()).getEntries()) {
                if (anyMatch(entry, excludedSubKeys())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean anyMatch(Yaml.Mapping.Entry entry, List<String> subKeys) {
        for (String subKey : subKeys) {
            if (entry.getKey().getValue().equals(subKey)
                    || entry.getKey().getValue().startsWith(subKey + ".")) {
                return true;
            }
        }
        return false;
    }

    private static boolean noneMatch(Yaml.Mapping.Entry entry, List<String> subKeys) {
        for (String subKey : subKeys) {
            if (entry.getKey().getValue().equals(subKey)
                    || entry.getKey().getValue().startsWith(subKey + ".")) {
                return false;
            }
        }
        return true;
    }

    private boolean noneMatch(String key, String basePattern, List<String> excludedSubKeys) {
        for (String subkey : excludedSubKeys) {
            String subKeyPattern = basePattern + "." + subkey;
            if (matches(key, subKeyPattern) || matches(key, subKeyPattern + ".*")) {
                return false;
            }
        }
        return true;
    }

    private boolean matches(String string, String pattern) {
        return !Boolean.FALSE.equals(relaxedBinding) ?
                NameCaseConvention.matchesGlobRelaxedBinding(string, pattern) :
                StringUtils.matchesGlob(string, pattern);
    }

    private List<String> excludedSubKeys() {
        return except != null ? except : Collections.emptyList();
    }

    private class InsertSubpropertyVisitor<P> extends YamlIsoVisitor<P> {
        private final Yaml.Mapping.Entry scope;
        private final String subproperty;
        private final Yaml.Mapping.Entry entryToReplace;

        private InsertSubpropertyVisitor(Yaml.Mapping.Entry scope, String subproperty, Yaml.Mapping.Entry entryToReplace) {
            this.scope = scope;
            this.subproperty = subproperty;
            this.entryToReplace = entryToReplace;
        }

        @Override
        public Yaml.Mapping visitMapping(Yaml.Mapping mapping, P p) {
            Yaml.Mapping m = super.visitMapping(mapping, p);
            if (m.getEntries().contains(scope)) {
                String newEntryPrefix = scope.getPrefix();
                Yaml.Mapping.Entry newEntry = new Yaml.Mapping.Entry(randomId(),
                        newEntryPrefix,
                        Markers.EMPTY,
                        new Yaml.Scalar(randomId(), "", Markers.EMPTY,
                                Yaml.Scalar.Style.PLAIN, null, subproperty),
                        scope.getBeforeMappingValueIndicator(),
                        removeExclusions(entryToReplace.getValue().copyPaste()));

                if (hasExcludedValues(entryToReplace)) {
                    m = m.withEntries(ListUtils.concat(m.getEntries(), newEntry));
                } else {
                    if (m.getEntries().contains(entryToReplace)) {
                        m = m.withEntries(ListUtils.map(m.getEntries(), e -> {
                            if (e.equals(entryToReplace)) {
                                return newEntry.withPrefix(e.getPrefix());
                            }
                            return e;
                        }));
                    } else {
                        m = (Yaml.Mapping) new DeletePropertyVisitor<>(entryToReplace).visitNonNull(m, p);
                        Yaml.Mapping newMapping = m.withEntries(Collections.singletonList(newEntry));
                        Yaml.Mapping mergedMapping = (Yaml.Mapping) new MergeYamlVisitor<>(m, newMapping, true, null, false).visitMapping(m, p);
                        m = maybeAutoFormat(m, mergedMapping, p, getCursor().getParentOrThrow());
                    }
                }
            }

            return m;
        }

        @Override
        public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry originalEntry, P p) {
            final Yaml.Mapping.Entry e = super.visitMappingEntry(originalEntry, p);
            if (e == entryToReplace && hasNonExcludedValues(entryToReplace) && e.getValue() instanceof Yaml.Mapping) {
                return e.withValue(onlyExclusions((Yaml.Mapping) e.getValue()));
            }
            return e;
        }

        private Yaml.Mapping onlyExclusions(final Yaml.Mapping mapping) {
            List<Yaml.Mapping.Entry> list = new ArrayList<>();
            for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                if (!noneMatch(entry, excludedSubKeys())) {
                    list.add(entry);
                }
            }
            return mapping.withEntries(list);
        }

        private Yaml.Block removeExclusions(Yaml.Block block) {
            if (!(block instanceof Yaml.Mapping)) {
                return block;
            } else {
                Yaml.Mapping mapping = (Yaml.Mapping) block;
                List<Yaml.Mapping.Entry> list = new ArrayList<>();
                for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                    if (noneMatch(entry, excludedSubKeys())) {
                        list.add(entry);
                    }
                }
                return mapping.withEntries(list);
            }
        }
    }

    private static class DeletePropertyVisitor<P> extends YamlIsoVisitor<P> {
        private final Yaml.Mapping.Entry scope;

        private DeletePropertyVisitor(Yaml.Mapping.Entry scope) {
            this.scope = scope;
        }

        @Override
        public Yaml.Mapping visitMapping(Yaml.Mapping mapping, P p) {
            Yaml.Mapping m = super.visitMapping(mapping, p);

            boolean changed = false;
            List<Yaml.Mapping.Entry> entries = new ArrayList<>();
            for (Yaml.Mapping.Entry entry : m.getEntries()) {
                if (entry == scope || (entry.getValue() instanceof Yaml.Mapping && ((Yaml.Mapping) entry.getValue()).getEntries().isEmpty())) {
                    changed = true;
                } else {
                    entries.add(entry);
                }
            }

            if (entries.size() == 1) {
                entries = ListUtils.map(entries, e -> e.withPrefix(""));
            }

            if (changed) {
                m = m.withEntries(entries);

                if (getCursor().getParentOrThrow().getValue() instanceof Yaml.Document) {
                    Yaml.Document document = getCursor().getParentOrThrow().getValue();
                    if (!document.isExplicit()) {
                        m = m.withEntries(m.getEntries());
                    }
                }
            }
            return m;
        }
    }
}
