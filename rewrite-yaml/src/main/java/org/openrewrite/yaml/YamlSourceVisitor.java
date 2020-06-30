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

import org.openrewrite.SourceVisitor;
import org.openrewrite.yaml.tree.Yaml;

public abstract class YamlSourceVisitor<R> extends SourceVisitor<R> {
    public R visitDocuments(Yaml.Documents documents) {
        return reduce(
                defaultTo(documents),
                visit(documents.getDocuments())
        );
    }

    public R visitDocument(Yaml.Document document) {
        return reduce(
                defaultTo(document),
                visit(document.getBlocks())
        );
    }

    public R visitSequence(Yaml.Sequence sequence) {
        return reduce(
                defaultTo(sequence),
                visit(sequence.getEntries())
        );
    }

    public R visitSequenceEntry(Yaml.Sequence.Entry entry) {
        return reduce(
                defaultTo(entry),
                visit(entry.getBlock())
        );
    }

    public R visitMapping(Yaml.Mapping mapping) {
        return reduce(
                defaultTo(mapping),
                visit(mapping.getEntries())
        );
    }

    public R visitMappingEntry(Yaml.Mapping.Entry entry) {
        return reduce(
                defaultTo(entry),
                reduce(
                        visit(entry.getKey()),
                        visit(entry.getValue())
                )
        );
    }

    public R visitScalar(Yaml.Scalar scalar) {
        return defaultTo(scalar);
    }
}
