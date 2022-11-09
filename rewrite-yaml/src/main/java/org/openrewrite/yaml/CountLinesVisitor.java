/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.Tree;
import org.openrewrite.yaml.tree.Yaml;

import java.util.concurrent.atomic.AtomicInteger;

public class CountLinesVisitor extends YamlVisitor<AtomicInteger> {
    @Override
    public Yaml visitDocuments(Yaml.Documents documents, AtomicInteger count) {
        if(documents.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitDocuments(documents, count);
    }

    @Override
    public Yaml visitDocument(Yaml.Document document, AtomicInteger count) {
        if(document.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitDocument(document, count);
    }

    @Override
    public Yaml visitMapping(Yaml.Mapping mapping, AtomicInteger count) {
        if(mapping.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitMapping(mapping, count);
    }

    @Override
    public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, AtomicInteger count) {
        if(entry.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitMappingEntry(entry, count);
    }

    @Override
    public Yaml visitScalar(Yaml.Scalar scalar, AtomicInteger count) {
        if(scalar.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitScalar(scalar, count);
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence sequence, AtomicInteger count) {
        if(sequence.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitSequence(sequence, count);
    }

    @Override
    public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, AtomicInteger count) {
        if(entry.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitSequenceEntry(entry, count);
    }

    @Override
    public Yaml visitAnchor(Yaml.Anchor anchor, AtomicInteger count) {
        if(anchor.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitAnchor(anchor, count);
    }

    @Override
    public Yaml visitAlias(Yaml.Alias alias, AtomicInteger count) {
        if(alias.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitAlias(alias, count);
    }

    public static int countLines(Tree tree) {
        return new CountLinesVisitor().reduce(tree, new AtomicInteger()).get();
    }
}
