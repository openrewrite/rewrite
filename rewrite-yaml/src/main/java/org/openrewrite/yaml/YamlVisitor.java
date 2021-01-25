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
import org.openrewrite.yaml.tree.Yaml;

public class YamlVisitor<P> extends TreeVisitor<Yaml, P> {

    public Yaml visitDocuments(Yaml.Documents documents, P p) {
        return documents.withDocuments(call(documents.getDocuments(), p));
    }

    public Yaml visitDocument(Yaml.Document document, P p) {
        return document.withBlocks(call(document.getBlocks(), p));
    }

    public Yaml visitMapping(Yaml.Mapping mapping, P p) {
        return mapping.withEntries(call(mapping.getEntries(), p));
    }

    public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        Yaml.Mapping.Entry e = entry;
        e = e.withKey(call(e.getKey(), p));
        return e.withValue(call(e.getValue(), p));
    }

    public Yaml visitScalar(Yaml.Scalar scalar, P p) {
        return scalar;
    }

    public Yaml visitSequence(Yaml.Sequence sequence, P p) {
        return sequence.withEntries(call(sequence.getEntries(), p));
    }

    public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, P p) {
        return entry.withBlock(call(entry.getBlock(), p));
    }

    public void maybeCoalesceProperties() {
        if (getAfterVisit().stream().noneMatch(CoalesceProperties.CoalescePropertiesVisitor.class::isInstance)) {
            doAfterVisit(new CoalesceProperties());
        }
    }
}
