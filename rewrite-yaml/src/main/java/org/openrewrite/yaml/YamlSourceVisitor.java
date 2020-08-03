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

public interface YamlSourceVisitor<R> extends SourceVisitor<R> {

    R visitDocuments(Yaml.Documents documents);
    R visitDocument(Yaml.Document document);
    R visitSequence(Yaml.Sequence sequence);
    R visitSequenceEntry(Yaml.Sequence.Entry entry);
    R visitMapping(Yaml.Mapping mapping);
    R visitMappingEntry(Yaml.Mapping.Entry entry);
    R visitScalar(Yaml.Scalar scalar);
}
