/*
 * Copyright 2020 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.yaml.search.FindIndentYamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class CoalescePropertiesVisitor<P> extends YamlIsoVisitor<P> {
    private final FindIndentYamlVisitor<P> findIndent = new FindIndentYamlVisitor<>();
    private final List<String> exclusions;
    private final List<String> applyTo;
    private List<JsonPathMatcher> exclusionMatchers;
    private List<JsonPathMatcher> applyToMatchers;

    public CoalescePropertiesVisitor(@Nullable final List<String> exclusions, @Nullable final List<String> applyTo) {
        this.exclusions = exclusions == null ? emptyList() : exclusions;
        this.applyTo = applyTo == null ? emptyList() : applyTo;
        exclusionMatchers = this.exclusions.stream().map(JsonPathMatcher::new).collect(toList());
        applyToMatchers = this.applyTo.stream().map(JsonPathMatcher::new).collect(toList());
    }

    @Override
    public Yaml.Document visitDocument(Yaml.Document document, P p) {
        if (document != new ReplaceAliasWithAnchorValueVisitor<P>().visit(document, p)) {
            return document;
        }
        findIndent.visit(document, p);
        return super.visitDocument(document, p);
    }

    @Override
    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        int levels = 0;
        Cursor c = getCursor();
        while (c != null && !c.isRoot()) {
            Cursor current = c;
            boolean foundMatch = exclusionMatchers.stream().anyMatch(it -> it.matches(current));
            if (foundMatch) {
                for (int i = 0; i <= levels; i++) {
                    getCursor().getParent(i).putMessage("COALESCE_COLLAPSING", false);
                }
                break;
            } else {
                c = c.getParent();
                levels++;
            }
        }
        return e;
    }

    @Override
    public Yaml.Mapping visitMapping(Yaml.Mapping mapping, P p) {
        Yaml.Mapping m = super.visitMapping(mapping, p);

        boolean allowCoalesce = getCursor().getMessage("COALESCE_COLLAPSING", true);
        if (!allowCoalesce) {
            return m;
        }
        boolean changed = false;
        List<Yaml.Mapping.Entry> entries = new ArrayList<>();

        for (Yaml.Mapping.Entry entry : m.getEntries()) {
            if (entry.getValue() instanceof Yaml.Mapping) {
                Yaml.Mapping valueMapping = (Yaml.Mapping) entry.getValue();
                if (valueMapping.getEntries().size() == 1) { //&& !matchesExclusion(entry) && matchesApplyTo(entry)) {
                    Yaml.Mapping.Entry subEntry = valueMapping.getEntries().iterator().next();
                    if (!subEntry.getPrefix().contains("#")) {// && !matchesExclusion(subEntry) && matchesApplyTo(subEntry)) {
                        Yaml.Scalar coalescedKey = ((Yaml.Scalar) entry.getKey()).withValue(entry.getKey().getValue() + "." + subEntry.getKey().getValue());

                        entries.add(entry.withKey(coalescedKey)
                                .withValue(subEntry.getValue()));

                        int indentToUse = findIndent.getMostCommonIndent() > 0 ? findIndent.getMostCommonIndent() : 4;
                        doAfterVisit(new ShiftFormatLeftVisitor<>(subEntry.getValue(), indentToUse));

                        changed = true;
                    } else {
                        entries.add(entry);
                    }
                } else {
                    entries.add(entry);
                }
            } else {
                entries.add(entry);
            }
        }

        if (changed) {
            m = m.withEntries(entries);
        }

        return m;
    }

    private boolean matchesExclusion(Yaml.Mapping.Entry entry) {
        boolean foundMatch = false;
        Cursor c = new Cursor(getCursor(), entry);
        while (c != null && !c.isRoot()) {
            Cursor current = c;
            foundMatch = exclusionMatchers.stream().anyMatch(it -> it.matches(current));
            if (foundMatch) {
                return foundMatch;
            } else {
                c = c.getParent();
            }
        }
        return foundMatch;
    }

    private boolean matchesApplyTo(Yaml.Mapping.Entry entry) {
        if (applyToMatchers.isEmpty()) {
            return true;
        }
        boolean foundMatch = false;
        Cursor c = new Cursor(getCursor(), entry);
        while (c != null && !c.isRoot()) {
            Cursor current = c;
            foundMatch = applyToMatchers.stream().anyMatch(it -> it.matches(current));
            if (foundMatch) {
                return foundMatch;
            } else {
                c = c.getParent();
            }
        }
        return foundMatch;
    }
}
