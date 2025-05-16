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
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;

import static org.openrewrite.internal.StringUtils.isNotEmpty;

@Value
@EqualsAndHashCode(callSuper = false)
public class MergeDuplicateSectionsVisitor<P> extends YamlIsoVisitor<P> {
    private final static String NEW_ROOT_COMMENTS = "NEW_ROOT_COMMENTS";

    Yaml scope;

    @Override
    public Yaml.Document visitDocument(Yaml.Document document, P p) {
        Yaml.Document d = super.visitDocument(document, p);
        String newRootComments = getCursor().getMessage("NEW_ROOT_COMMENTS", "");
        if (!newRootComments.isEmpty()) {
            d = d.withPrefix(d.getPrefix() + newRootComments);
        }
        return d;
    }

    @Override
    public Yaml.Mapping visitMapping(Yaml.Mapping mapping, P p) {
        Yaml.Mapping m = super.visitMapping(mapping, p);

        if (m.getEntries().size() < 2) {
            return m;
        }

        boolean changed = false;
        boolean isMappingUnderDocument = getCursor().getParentOrThrow().getValue() instanceof Yaml.Document;
        Map<String, Yaml.Mapping.Entry> newEntries = new LinkedHashMap<>();
        for (Yaml.Mapping.Entry e : m.getEntries()) {
            String key = e.getKey().getValue();
            String prefix = e.getPrefix();

            if (hasComments(prefix)) {
                changed = false;
                break;
            }

            if (newEntries.containsKey(key)) {
                Yaml.Mapping.Entry existingEntry = newEntries.get(key);
                Yaml.Block value = e.getValue();
                Yaml mergedYaml = new MergeYamlVisitor<>(existingEntry.getValue(), value, false, null, false, null, null)
                        .visitNonNull(existingEntry.getValue(), p);
                newEntries.put(key, existingEntry.withValue((Yaml.Block) mergedYaml));
                changed = true;

                // Not used right now, as we disabled comments;
                // this is just the start to support comments (like MergeYaml does, by moving the comments to the proper places).
                if (isMappingUnderDocument && isNotEmpty(prefix)) {
                    prefix = prefix.startsWith("\n") ? prefix.substring(1) : prefix;
                    String prev = getCursor().getParentOrThrow().getMessage(NEW_ROOT_COMMENTS, "");
                    getCursor().getParentOrThrow().putMessage(NEW_ROOT_COMMENTS, prev + prefix);
                }
            } else {
                newEntries.put(key, e);
            }
        }

        return changed ? m.withEntries(new ArrayList<>(newEntries.values())) : m;
    }

    // TODO We don't support comments yet, as supporting comments is a hard thing to get right
    private static boolean hasComments(String prefix) {
        return isNotEmpty(prefix) && !prefix.matches("^(\\r\\n|\\n|\\r)+$");
    }
}
