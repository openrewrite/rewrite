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

import org.openrewrite.yaml.tree.Yaml;

public class YamlIsoVisitor<P> extends YamlVisitor<P> {
    @Override
    public Yaml.Documents visitDocuments(Yaml.Documents documents, P p) {
        return (Yaml.Documents) super.visitDocuments(documents, p);
    }

    @Override
    public Yaml.Document visitDocument(Yaml.Document document, P p) {
        return (Yaml.Document) super.visitDocument(document, p);
    }

    @Override
    public Yaml.Document.End visitDocumentEnd(Yaml.Document.End end, P p) {
        return (Yaml.Document.End) super.visitDocumentEnd(end, p);
    }

    @Override
    public Yaml.Mapping visitMapping(Yaml.Mapping mapping, P p) {
        return (Yaml.Mapping) super.visitMapping(mapping, p);
    }

    @Override
    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        return (Yaml.Mapping.Entry) super.visitMappingEntry(entry, p);
    }

    @Override
    public Yaml.Scalar visitScalar(Yaml.Scalar scalar, P p) {
        return (Yaml.Scalar) super.visitScalar(scalar, p);
    }

    @Override
    public Yaml.Sequence visitSequence(Yaml.Sequence sequence, P p) {
        return (Yaml.Sequence) super.visitSequence(sequence, p);
    }

    @Override
    public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, P p) {
        return (Yaml.Sequence.Entry) super.visitSequenceEntry(entry, p);
    }
}
