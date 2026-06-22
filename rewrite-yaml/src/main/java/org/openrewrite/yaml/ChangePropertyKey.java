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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.search.FindProperty;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
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

    @Option(example = "List.of(\"group\")", displayName = "Except",
            description = "If any of these property keys exist as direct children of `oldPropertyKey`, then they will not be moved to `newPropertyKey`.",
            required = false)
    @Nullable
    List<String> except;


    @Option(displayName = "File pattern",
            description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all.",
            required = false,
            example = ".github/workflows/*.yml")
    @Nullable
    String filePattern;

    String displayName = "Change property key";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", oldPropertyKey, newPropertyKey);
    }

    String description = "Change a YAML property key while leaving the value intact. Expects dot notation for nested YAML mappings, similar to how Spring Boot interprets `application.yml` files.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles(filePattern), new ChangePropertyKeyVisitor<>());
    }

    private class ChangePropertyKeyVisitor<P> extends YamlIsoVisitor<P> {
        private final NameCaseConvention.Compiled oldKeyMatcher;
        private final NameCaseConvention.Compiled oldKeyWildcardMatcher;
        private final NameCaseConvention.Compiled newKeyMatcher;
        private final NameCaseConvention.Compiled newKeyWildcardMatcher;
        private final List<NameCaseConvention.Compiled> exceptMatchers;
        private final List<NameCaseConvention.Compiled> exceptWildcardMatchers;

        ChangePropertyKeyVisitor() {
            NameCaseConvention convention = !Boolean.FALSE.equals(relaxedBinding) ?
                    NameCaseConvention.LOWER_CAMEL :
                    NameCaseConvention.EXACT;
            this.oldKeyMatcher = convention.compile(oldPropertyKey);
            this.oldKeyWildcardMatcher = convention.compile(oldPropertyKey + ".*");
            this.newKeyMatcher = convention.compile(newPropertyKey);
            this.newKeyWildcardMatcher = convention.compile(newPropertyKey + ".*");
            List<String> excluded = excludedSubKeys();
            this.exceptMatchers = new ArrayList<>(excluded.size());
            this.exceptWildcardMatchers = new ArrayList<>(excluded.size());
            for (String subkey : excluded) {
                exceptMatchers.add(convention.compile(oldPropertyKey + "." + subkey));
                exceptWildcardMatchers.add(convention.compile(oldPropertyKey + "." + subkey + ".*"));
            }
        }

        @Override
        public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
            Yaml.Mapping.Entry e = super.visitMappingEntry(entry, p);
            if (getCursor().firstEnclosing(Yaml.Sequence.class) != null) {
                return e;
            }
            Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream()
                    .filter(Yaml.Mapping.Entry.class::isInstance)
                    .map(Yaml.Mapping.Entry.class::cast)
                    .collect(toCollection(ArrayDeque::new));

            String prop = stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false)
                    .map(e2 -> e2.getKey().getValue())
                    .collect(joining("."));

            if (newPropertyKey.startsWith(oldPropertyKey) &&
                    (newKeyMatcher.matchesGlob(prop) ||
                     newKeyWildcardMatcher.matchesGlob(prop) ||
                     childMatchesNewPropertyKey(entry, prop))) {
                return e;
            }

            String propertyToTest = newPropertyKey;
            if (oldKeyMatcher.matchesGlob(prop)) {
                Iterator<Yaml.Mapping.Entry> propertyEntriesLeftToRight = propertyEntries.descendingIterator();
                while (propertyEntriesLeftToRight.hasNext()) {
                    Yaml.Mapping.Entry propertyEntry = propertyEntriesLeftToRight.next();
                    String value = propertyEntry.getKey().getValue() + ".";

                    if ((!propertyToTest.startsWith(value ) || (propertyToTest.startsWith(value) && !propertyEntriesLeftToRight.hasNext())) &&
                        hasNonExcludedValues(propertyEntry)) {
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
                if (oldKeyWildcardMatcher.matchesGlob(prop) &&
                        !(oldKeyWildcardMatcher.matchesGlob(parentProp) ||
                          oldKeyMatcher.matchesGlob(parentProp)) &&
                        noneMatchExcluded(prop)) {
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

        private boolean noneMatchExcluded(String key) {
            for (int i = 0; i < exceptMatchers.size(); i++) {
                if (exceptMatchers.get(i).matchesGlob(key) || exceptWildcardMatchers.get(i).matchesGlob(key)) {
                    return false;
                }
            }
            return true;
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
            if (entry.getKey().getValue().equals(subKey) ||
                    entry.getKey().getValue().startsWith(subKey + ".")) {
                return true;
            }
        }
        return false;
    }

    private static boolean noneMatch(Yaml.Mapping.Entry entry, List<String> subKeys) {
        for (String subKey : subKeys) {
            if (entry.getKey().getValue().equals(subKey) ||
                    entry.getKey().getValue().startsWith(subKey + ".")) {
                return false;
            }
        }
        return true;
    }

    private List<String> excludedSubKeys() {
        return except != null ? except : emptyList();
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
                                Yaml.Scalar.Style.PLAIN, null, null, subproperty),
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
                        // If a prefix of the relocated key already exists as a mapping in this scope,
                        // nest the remainder under it rather than inserting a dotted key at this level.
                        Yaml.Mapping incoming = nestUnderExistingPrefix(m, newEntry);
                        if (incoming == null) {
                            incoming = m.withEntries(singletonList(newEntry));
                        }
                        Yaml.Mapping mergedMapping = (Yaml.Mapping) new MergeYamlVisitor<>(m, incoming, true, null, false, null, null).visitMapping(m, p);
                        // Preserve the leading prefix of the first entry at the document root: when the relocated
                        // property's original tree was the first top-level entry and is fully removed, auto-format
                        // would otherwise introduce a blank line before the new first entry.
                        boolean atDocumentRoot = getCursor().getParentOrThrow().getValue() instanceof Yaml.Document;
                        String firstEntryPrefix = mergedMapping.getEntries().get(0).getPrefix();
                        m = maybeAutoFormat(m, mergedMapping, p, getCursor().getParentOrThrow());
                        if (atDocumentRoot && !m.getEntries().isEmpty()) {
                            m = m.withEntries(ListUtils.mapFirst(m.getEntries(), e -> e.withPrefix(firstEntryPrefix)));
                        }
                    }
                }
            }

            return m;
        }

        /**
         * When relocating {@code newEntry} (a dot-separated key) into {@code mapping}, descend through any
         * mapping entries whose keys already form a prefix of the relocated key and nest the remaining
         * portion of the key under the deepest such mapping. Returns {@code null} when no prefix exists,
         * in which case the caller inserts the dotted key at the current level as before.
         */
        private Yaml.@Nullable Mapping nestUnderExistingPrefix(Yaml.Mapping mapping, Yaml.Mapping.Entry newEntry) {
            String[] segments = ((Yaml.Scalar) newEntry.getKey()).getValue().split("\\.");
            if (segments.length < 2) {
                return null;
            }

            List<String> nestKeys = new ArrayList<>();
            Yaml.Mapping current = mapping;
            int consumed = 0;
            while (consumed < segments.length - 1) {
                Yaml.Mapping.Entry match = null;
                for (Yaml.Mapping.Entry e : current.getEntries()) {
                    if (e.getValue() instanceof Yaml.Mapping) {
                        String[] keyParts = e.getKey().getValue().split("\\.");
                        if (consumed + keyParts.length < segments.length && matchesPrefix(segments, consumed, keyParts)) {
                            match = e;
                            break;
                        }
                    }
                }
                if (match == null) {
                    break;
                }
                nestKeys.add(match.getKey().getValue());
                current = (Yaml.Mapping) match.getValue();
                consumed += match.getKey().getValue().split("\\.").length;
            }
            if (nestKeys.isEmpty()) {
                return null;
            }

            String remainder = String.join(".", Arrays.copyOfRange(segments, consumed, segments.length));
            Yaml.Mapping.Entry leaf = newEntry.withKey(((Yaml.Scalar) newEntry.getKey()).withValue(remainder));
            Yaml.Mapping nested = new Yaml.Mapping(randomId(), Markers.EMPTY, null, singletonList(leaf), null, null, null);
            for (int i = nestKeys.size() - 1; i >= 0; i--) {
                Yaml.Mapping.Entry wrapper = new Yaml.Mapping.Entry(randomId(), "", Markers.EMPTY,
                        new Yaml.Scalar(randomId(), "", Markers.EMPTY, Yaml.Scalar.Style.PLAIN, null, null, nestKeys.get(i)),
                        "", nested);
                nested = new Yaml.Mapping(randomId(), Markers.EMPTY, null, singletonList(wrapper), null, null, null);
            }
            return nested;
        }

        private boolean matchesPrefix(String[] segments, int start, String[] keyParts) {
            for (int i = 0; i < keyParts.length; i++) {
                if (!segments[start + i].equals(keyParts[i])) {
                    return false;
                }
            }
            return true;
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
                String firstEntryPrefix = m.getEntries().get(0).getPrefix();
                entries = ListUtils.map(entries, e -> e.withPrefix(firstEntryPrefix));
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
