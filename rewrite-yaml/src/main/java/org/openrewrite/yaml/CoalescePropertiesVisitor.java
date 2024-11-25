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

import org.openrewrite.yaml.search.FindIndentYamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.List;

public class CoalescePropertiesVisitor<P> extends YamlIsoVisitor<P> {
    private final FindIndentYamlVisitor<P> findIndent = new FindIndentYamlVisitor<>();

    public CoalescePropertiesVisitor() {
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

        boolean changed = false;
        List<Yaml.Mapping.Entry> entries = new ArrayList<>();

        for (Yaml.Mapping.Entry entry : m.getEntries()) {
            if (entry.getValue() instanceof Yaml.Mapping) {
                Yaml.Mapping valueMapping = (Yaml.Mapping) entry.getValue();
                if (valueMapping.getEntries().size() == 1) {
                    Yaml.Mapping.Entry subEntry = valueMapping.getEntries().iterator().next();
                    if (!subEntry.getPrefix().contains("#")) {
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
}
