/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.yaml.tree.Yaml;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class SortYaml extends Recipe {
    @Option(displayName = "Sort by", description = "The order in which to sort the keys in the YAML document. " +
            "When providing multiple keys, the first key is the primary sort key, the second key is the secondary sort key, and so on. " +
            "Fields which do not match the provided keys will be sorted to the end. " +
            "When not more nested fields provided, the keys will remain in the order they were found. " +
            "You can order parent items on a (sub)property also by mentioning the property within brackets on the last \"qualifier\". " +
            "Using `**` will result in deeply sorting of the natural order. This can be used both globally or on a particular property. " +
            "Be aware that using `**` will be a 'greedy' action as this rule matches ALL property paths. Perhaps you only want to use `**` with some other property?" +
            "Sequences will never be touched.", example = "[stages, include, *.stage, *[stage], *.artifacts.**, **[prop]")
    List<String> sortBy;

    @Option(displayName = "File pattern", description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all.", required = false, example = ".gitlab-ci.yml")
    @Nullable
    String filePattern;

    @Override
    public String getDisplayName() {
        return "Sort YAML documents";
    }

    @Override
    public String getDescription() {
        return "Sort a YAML file by the provided keys.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate()
                .and(Validated.test("sortBy", "Must provide at least one key to sort by", sortBy, keys -> !keys.isEmpty()))
                .and(Validated.test("sortBy", "sortBy keys must not contain null", sortBy, keys -> keys.stream().noneMatch(Objects::isNull)))
                .and(Validated.test("sortBy", "sortBy keys must not be empty", sortBy, keys -> keys.stream().noneMatch(String::isEmpty)))
                .and(Validated.test("sortBy", "sortBy keys must not end with a period", sortBy, keys -> keys.stream().noneMatch(key -> key.endsWith("."))))
                .and(Validated.test("sortBy", "sortBy property extractor not properly opened/closed", sortBy, keys -> keys.stream().noneMatch(key -> (keys.contains("[") && !keys.contains("]")) || (keys.contains("]") && !keys.contains("["))))
                .and(Validated.test("sortBy", "sortBy can only contain 1 property extractor", sortBy, keys -> keys.stream().noneMatch(key -> key.indexOf("[") != key.lastIndexOf("["))))
                .and(Validated.test("sortBy", "sortBy can not have anything specified after the property specifier", sortBy, keys -> keys.stream().noneMatch(key -> key.contains("]") && key.indexOf("]") != key.length() - 1)))
                .and(Validated.test("sortBy", "sortBy can only contain property specifier at the last parameter", sortBy, keys -> keys.stream().allMatch(key -> !key.contains("[") || !key.contains(".") || key.indexOf("[") > key.lastIndexOf(".")))))
                .and(Validated.test("sortBy", "sortBy can not contain wildcard in property specifier", sortBy, keys -> keys.stream().allMatch(key -> !key.contains("[") || key.indexOf("[") > key.lastIndexOf("*"))));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles(filePattern), new YamlIsoVisitor<ExecutionContext>() {

            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext executionContext) {
                return (Yaml.Document) sort(document, null);
            }

            private Yaml sort(Yaml yaml, @Nullable String path) {
                if (yaml instanceof Yaml.Block) {
                    return sortBlock((Yaml.Block) yaml, path);
                } else if (yaml instanceof Yaml.Document) {
                    return ((Yaml.Document) yaml).withBlock((Yaml.Block) sort(((Yaml.Document) yaml).getBlock(), null));
                } else {
                    return yaml;
                }
            }

            private Yaml.Block sortBlock(Yaml.Block yaml, @Nullable String path) {
                if (yaml instanceof Yaml.Mapping) {
                    Yaml.Mapping mapping = (Yaml.Mapping) yaml;
                    List<Yaml.Mapping.Entry> entries = mapping.getEntries();
                    if (entries.isEmpty()) {
                        return mapping;
                    }
                    Yaml.Mapping.Entry first = entries.get(0);
                    Yaml.Mapping.Entry last = entries.get(entries.size() - 1);
                    String firstPrefix = first.getPrefix();

                    List<Yaml.Mapping.Entry> sortedEntries = entries.stream().sorted(comparingSortPreference(path, sortBy)).map(entry -> {
                                String key = entry.getKey().getValue();
                                String prefixedPath = path == null ? key : path + "." + key;
                                Yaml.Block sortedBlock = sortBlock(entry.getValue(), prefixedPath);
                                if (sortedBlock.equals(entry.getValue())) {
                                    return entry.withPrefix(last.getPrefix());
                                }
                                return entry.withId(Tree.randomId()).withPrefix(last.getPrefix()).withValue(sortedBlock);
                            })
                            .collect(Collectors.toList());
                    sortedEntries = ListUtils.mapFirst(sortedEntries, entry -> entry.withPrefix(firstPrefix));
                    if (!sortedEntries.equals(entries)) {
                        return ((Yaml.Mapping) yaml).withId(Tree.randomId()).withEntries(sortedEntries);
                    }
                }
                return yaml;
            }

            private Comparator<Yaml.Mapping.Entry> comparingSortPreference(@Nullable String path, List<String> sortBy) {
                return (entry1, entry2) -> {
                    String entry1Key = entry1.getKey().getValue();
                    String entry2Key = entry2.getKey().getValue();
                    String entry1PrefixedPath = path == null ? entry1Key : path + "." + entry1Key;
                    String entry2PrefixedPath = path == null ? entry2Key : path + "." + entry2Key;
                    Integer entry1SortIndex = findSortIndex(entry1PrefixedPath, true).orElseGet(() -> findSortIndex(entry1PrefixedPath, false).orElse(null));
                    Integer entry2SortIndex = findSortIndex(entry2PrefixedPath, true).orElseGet(() -> findSortIndex(entry2PrefixedPath, false).orElse(null));

                    if (entry1SortIndex == null && entry2SortIndex == null) {
                        return 0;
                    } else if (entry1SortIndex == null) {
                        return 1;
                    } else if (entry2SortIndex == null) {
                        return -1;
                    } else if (entry1SortIndex.equals(entry2SortIndex)) {
                        String sorter = sortBy.get(entry1SortIndex);
                        String matcher = sorter.contains("[") ? sorter.substring(0, sorter.indexOf("[")) : sorter;
                        if (exactMatch(entry1PrefixedPath, matcher) && exactMatch(entry2PrefixedPath, matcher)) {
                            if (sorter.contains("[")) {
                                String property = sorter.substring(sorter.indexOf("[") + 1, sorter.indexOf("]"));
                                if (entry1.getValue() instanceof Yaml.Mapping && entry2.getValue() instanceof Yaml.Mapping) {
                                    Yaml.Mapping mapping1 = (Yaml.Mapping) entry1.getValue();
                                    Yaml.Mapping mapping2 = (Yaml.Mapping) entry2.getValue();
                                    Yaml.Mapping.Entry entry1Property = findPropertyRecursively(mapping1, property);
                                    Yaml.Mapping.Entry entry2Property = findPropertyRecursively(mapping2, property);
                                    if (entry1Property != null && entry2Property != null) {
                                        if (entry1Property.getValue() instanceof Yaml.Scalar && entry2Property.getValue() instanceof Yaml.Scalar) {
                                            return ((Yaml.Scalar) entry1Property.getValue()).getValue().compareTo(((Yaml.Scalar) entry2Property.getValue()).getValue());
                                        }
                                    }
                                }
                            } else if (sorter.equals("*") || sorter.equals("**") || sorter.endsWith(".*") || sorter.endsWith(".**")) {
                                return entry1Key.compareTo(entry2Key);
                            }
                        }
                        return 0;
                    } else {
                        return entry1SortIndex.compareTo(entry2SortIndex);
                    }
                };
            }

            private Yaml.Mapping.@Nullable Entry findPropertyRecursively(Yaml.Mapping mapping, String property) {
                for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                    if (entry.getKey().getValue().equals(property)) {
                        return entry;
                    }
                    if (entry.getValue() instanceof Yaml.Mapping) {
                        Yaml.Mapping.Entry found = findPropertyRecursively((Yaml.Mapping) entry.getValue(), property);
                        if (found != null) {
                            return found;
                        }
                    }
                }
                return null;
            }

            private Optional<Integer> findSortIndex(String path, boolean exact) {
                for (int i = 0; i < sortBy.size(); i++) {
                    String sorter = sortBy.get(i);
                    String matcher = sorter.contains("[") ? sorter.substring(0, sorter.indexOf("[")) : sorter;
                    if (exact && (matcher.equals(path) || exactMatch(path, matcher))) {
                        return Optional.of(i);
                    }
                    if (!exact && (matcher.startsWith(path + ".") || matches(path, matcher))) {
                        return Optional.of(i);
                    }
                }
                return Optional.empty();
            }

            // key        sortBy      matches
            // a.b.c      a.b         true
            // a.b.c      a.b.c       true
            // a.b.c      a.b.c.d     false
            // a.b.c      *.b         true
            // a.b.c      *.b.c       true
            // a.b.c      *.b.c.d     false
            // a.b.c      a.*.c       true
            // a.b.c      a.*.c.d     false
            // a.b.c      a.*.b       false
            // a.b.c      a.b.*       true
            // a.b.c      **          true
            // a.b.c      **.d        false
            // a.b.c      **.c        true
            // a.b.c      a.**        true
            // a.b.c      a.**.c      true
            // a.b.c      a.**.d      false
            private boolean matches(String key, String sortBy) {
                String[] keyParts = key.split("\\.");
                String[] sortByParts = sortBy.split("\\.");

                int keyLength = keyParts.length;
                int sortByLength = sortByParts.length;

                int i = 0, j = 0;
                while (i < keyLength && j < sortByLength) {
                    if (sortByParts[j].equals("**")) {
                        if (j == sortByLength - 1) {
                            return true;
                        }
                        j++;
                        while (i < keyLength && !keyParts[i].equals(sortByParts[j])) {
                            i++;
                        }
                    } else if (sortByParts[j].equals("*") || sortByParts[j].equals(keyParts[i])) {
                        i++;
                        j++;
                    } else {
                        return false;
                    }
                }
                return j == sortByLength;
            }

            // key        sortBy      matches
            // a.b.c      a.b.c       true
            // a.b.c      *.b.c       true
            // a.b.c      a.*.c       true
            // a.b.c      a.b.*       true
            // a.b.c      **          true
            // a.b.c      a.**        true
            // a.b.c      a.**.c      true
            // a.b.c      **.c        true
            private boolean exactMatch(String key, String sortBy) {
                String[] keyParts = key.split("\\.");
                String[] sortByParts = sortBy.split("\\.");

                int keyLength = keyParts.length;
                int sortByLength = sortByParts.length;

                int i = 0, j = 0;
                while (i < keyLength && j < sortByLength) {
                    if (sortByParts[j].equals("**")) {
                        if (j == sortByLength - 1) {
                            return true;
                        }
                        j++;
                        while (i < keyLength && !keyParts[i].equals(sortByParts[j])) {
                            i++;
                        }
                    } else if (sortByParts[j].equals("*") || sortByParts[j].equals(keyParts[i])) {
                        i++;
                        j++;
                    } else {
                        return false;
                    }
                }
                return i == keyLength && j == sortByLength;
            }
        });
    }
}
