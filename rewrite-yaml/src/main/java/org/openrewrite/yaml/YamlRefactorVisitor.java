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

import org.openrewrite.AbstractRefactorVisitor;
import org.openrewrite.refactor.Formatter;
import org.openrewrite.yaml.search.FindIndentYaml;
import org.openrewrite.yaml.tree.Yaml;

public class YamlRefactorVisitor extends AbstractRefactorVisitor<Yaml>
        implements YamlSourceVisitor<Yaml> {

    protected Formatter formatter;

    @Override
    public Yaml visitDocuments(Yaml.Documents documents) {
        formatter = new Formatter(documents, FindIndentYaml::new);
        return documents.withDocuments(refactor(documents.getDocuments()));
    }

    @Override
    public Yaml visitDocument(Yaml.Document document) {
        return document.withBlocks(refactor(document.getBlocks()));
    }

    @Override
    public Yaml visitMapping(Yaml.Mapping mapping) {
        return mapping.withEntries(refactor(mapping.getEntries()));
    }

    @Override
    public Yaml visitMappingEntry(Yaml.Mapping.Entry entry) {
        Yaml.Mapping.Entry e = entry;
        e = e.withKey(refactor(e.getKey()));
        return e.withValue(refactor(e.getValue()));
    }

    @Override
    public Yaml visitScalar(Yaml.Scalar scalar) {
        return scalar;
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence sequence) {
        return sequence.withEntries(refactor(sequence.getEntries()));
    }

    @Override
    public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry) {
        return entry.withBlock(refactor(entry.getBlock()));
    }

    public void maybeCoalesceProperties() {
        if (andThen().stream().noneMatch(CoalesceProperties.class::isInstance)) {
            andThen(new CoalesceProperties());
        }
    }
}
