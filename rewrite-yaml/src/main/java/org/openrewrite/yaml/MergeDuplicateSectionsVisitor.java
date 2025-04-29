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
import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class MergeDuplicateSectionsVisitor<P> extends YamlIsoVisitor<P> {
    Yaml scope;

    @Override
    public Yaml.Mapping visitMapping(Yaml.Mapping mapping, P p) {
        Yaml.Mapping m = super.visitMapping(mapping, p);

        if (m.getEntries().size() < 2) {
            return m;
        }

        boolean changed = false;
        Map<String, Yaml.Mapping.Entry> newEntries = new LinkedHashMap<>();
        for (Yaml.Mapping.Entry e : m.getEntries()) {
            String key = e.getKey().getValue();
            if (newEntries.containsKey(key)) {
                Yaml.Mapping.Entry existingEntry = newEntries.get(key);
                Yaml.Block value = e.getValue();
                Yaml mergedYaml = new MergeYamlVisitor<>(existingEntry.getValue(), value, false, null, false, null, null)
                        .visitNonNull(existingEntry.getValue(), p);
                newEntries.put(key, existingEntry.withValue((Yaml.Block) mergedYaml));
                changed = true;
            } else {
                newEntries.put(key, e);
            }
        }

        return changed ? m.withEntries(new ArrayList<>(newEntries.values())) : m;
    }
}
