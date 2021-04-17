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

import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.cleanup.RemoveUnusedVisitor;
import org.openrewrite.yaml.tree.Yaml;

public class YamlVisitor<P> extends TreeVisitor<Yaml, P> {

    @Override
    public String getLanguage() {
        return "yaml";
    }

    public Yaml visitDocuments(Yaml.Documents documents, P p) {
        return documents.withDocuments(ListUtils.map(documents.getDocuments(), d -> visitAndCast(d, p)));
    }

    public Yaml visitDocument(Yaml.Document document, P p) {
        return document.withBlocks(ListUtils.map(document.getBlocks(), b -> visitAndCast(b, p)));
    }

    public Yaml visitMapping(Yaml.Mapping mapping, P p) {
        return mapping.withEntries(ListUtils.map(mapping.getEntries(), e -> visitAndCast(e, p)));
    }

    public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        Yaml.Mapping.Entry e = entry;
        e = e.withKey(visitAndCast(e.getKey(), p));
        return e.withValue(visitAndCast(e.getValue(), p));
    }

    public Yaml visitScalar(Yaml.Scalar scalar, P p) {
        return scalar;
    }

    public Yaml visitSequence(Yaml.Sequence sequence, P p) {
        return sequence.withEntries(ListUtils.map(sequence.getEntries(), e -> visitAndCast(e, p)));
    }

    public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, P p) {
        return entry.withBlock(visitAndCast(entry.getBlock(), p));
    }

    public void maybeCoalesceProperties() {
        if (getAfterVisit().stream().noneMatch(CoalescePropertiesVisitor.class::isInstance)) {
            doAfterVisit(new CoalescePropertiesVisitor<>());
        }
    }

    public void removeUnused() {
        if (getAfterVisit().stream().noneMatch(RemoveUnusedVisitor.class::isInstance)) {
            doAfterVisit(new RemoveUnusedVisitor<>());
        }
    }
}
