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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.search.FindIndentYamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class CoalescePropertiesVisitor<P> extends YamlIsoVisitor<P> {
    private final FindIndentYamlVisitor<P> findIndent = new FindIndentYamlVisitor<>();
    private final List<JsonPathMatcher> exclusionMatchers;
    private final List<JsonPathMatcher> applyToMatchers;

    @Deprecated
    public CoalescePropertiesVisitor() {
        this(null, null);
    }

    @JsonCreator
    public CoalescePropertiesVisitor(@Nullable final List<String> exclusions, @Nullable final List<String> applyTo) {
        exclusionMatchers = exclusions == null ? emptyList() : exclusions.stream().map(JsonPathMatcher::new).collect(toList());
        applyToMatchers = applyTo == null ? emptyList() : applyTo.stream().map(JsonPathMatcher::new).collect(toList());
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
    public Yaml.Mapping visitMapping(Yaml.Mapping mapping, P p) {
        Yaml.Mapping m = super.visitMapping(mapping, p);

        return m.withEntries(ListUtils.map(m.getEntries(), entry -> {
            if (entry.getValue() instanceof Yaml.Mapping) {
                Yaml.Mapping valueMapping = (Yaml.Mapping) entry.getValue();
                if (valueMapping.getEntries().size() == 1) {
                    Yaml.Mapping.Entry subEntry = valueMapping.getEntries().get(0);
                    if (!subEntry.getPrefix().contains("#") && !isExcluded(entry, subEntry) && isApplied(entry)) {
                        int indentToUse = findIndent.getMostCommonIndent() > 0 ? findIndent.getMostCommonIndent() : 4;
                        doAfterVisit(new ShiftFormatLeftVisitor<>(subEntry.getValue(), indentToUse));

                        Yaml.Scalar coalescedKey = ((Yaml.Scalar) entry.getKey()).withValue(entry.getKey().getValue() + "." + subEntry.getKey().getValue());
                        return entry.withKey(coalescedKey).withValue(subEntry.getValue());
                    }
                }
            }
            return entry;
        }));
    }

    private boolean isExcluded(Yaml.Mapping.Entry entry, Yaml.Mapping.Entry subEntry) {
        if (exclusionMatchers.isEmpty()) {
            return false;
        }
        Cursor c = new Cursor(getCursor(), entry);
        Cursor c2 = new Cursor(c, subEntry);
        return match(c, exclusionMatchers) || exclusionMatchers.stream().anyMatch(it -> it.matches(c2));
    }

    private boolean isApplied(Yaml.Mapping.Entry entry) {
        if (applyToMatchers.isEmpty()) {
            return true;
        }
        Cursor c = new Cursor(getCursor(), entry);
        return match(c, applyToMatchers);
    }

    private boolean match(Cursor c, List<JsonPathMatcher> matchers) {
        while (c != null && !c.isRoot()) {
            Cursor current = c;
            if (matchers.stream().anyMatch(it -> it.matches(current))) {
                return true;
            }
            c = c.getParent();
        }
        return false;
    }
}
