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

import org.openrewrite.TreeProcessor;
import org.openrewrite.yaml.tree.Yaml;

public class YamlProcessor<P> extends TreeProcessor<Yaml, P> implements YamlVisitor<Yaml, P> {

    @Override
    public Yaml visitDocuments(Yaml.Documents documents, P p) {
        return documents.withDocuments(call(documents.getDocuments(), p));
    }

    @Override
    public Yaml visitDocument(Yaml.Document document, P p) {
        return document.withBlocks(call(document.getBlocks(), p));
    }

    @Override
    public Yaml visitMapping(Yaml.Mapping mapping, P p) {
        return mapping.withEntries(call(mapping.getEntries(), p));
    }

    @Override
    public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        Yaml.Mapping.Entry e = entry;
        e = e.withKey(call(e.getKey(), p));
        return e.withValue(call(e.getValue(), p));
    }

    @Override
    public Yaml visitScalar(Yaml.Scalar scalar, P p) {
        return scalar;
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence sequence, P p) {
        return sequence.withEntries(call(sequence.getEntries(), p));
    }

    @Override
    public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, P p) {
        return entry.withBlock(call(entry.getBlock(), p));
    }

    // todo -- proper usage here? Feels like a matter of execution context?
    public void maybeCoalesceProperties() {
        if (getAfterVisit().stream().noneMatch(CoalesceProperties.class::isInstance)) {
            doAfterVisit(new CoalesceProperties());
        }
    }
}
