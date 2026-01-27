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
package org.openrewrite.yaml.internal.rpc;

import org.openrewrite.Tree;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

public class YamlSender extends YamlVisitor<RpcSendQueue> {

    @Override
    public Yaml preVisit(Yaml y, RpcSendQueue q) {
        q.getAndSend(y, Tree::getId);
        q.getAndSend(y, Yaml::getPrefix);
        q.getAndSend(y, Tree::getMarkers);
        return y;
    }

    @Override
    public Yaml visitDocuments(Yaml.Documents documents, RpcSendQueue q) {
        q.getAndSend(documents, d -> d.getSourcePath().toString());
        q.getAndSend(documents, d -> d.getCharset().name());
        q.getAndSend(documents, Yaml.Documents::isCharsetBomMarked);
        q.getAndSend(documents, Yaml.Documents::getChecksum);
        q.getAndSend(documents, Yaml.Documents::getFileAttributes);
        q.getAndSendList(documents, Yaml.Documents::getDocuments, doc -> doc.getId(),
                doc -> visit(doc, q));
        q.getAndSend(documents, Yaml.Documents::getSuffix);
        return documents;
    }

    @Override
    public Yaml visitDocument(Yaml.Document document, RpcSendQueue q) {
        q.getAndSendList(document, Yaml.Document::getDirectives, dir -> dir.getId(),
                dir -> visit(dir, q));
        q.getAndSend(document, Yaml.Document::isExplicit);
        q.getAndSend(document, Yaml.Document::getBlock, b -> visit(b, q));
        q.getAndSend(document, Yaml.Document::getEnd, e -> visit(e, q));
        return document;
    }

    @Override
    public Yaml visitDirective(Yaml.Directive directive, RpcSendQueue q) {
        q.getAndSend(directive, Yaml.Directive::getValue);
        q.getAndSend(directive, Yaml.Directive::getSuffix);
        return directive;
    }

    @Override
    public Yaml visitDocumentEnd(Yaml.Document.End end, RpcSendQueue q) {
        q.getAndSend(end, Yaml.Document.End::isExplicit);
        return end;
    }

    @Override
    public Yaml visitMapping(Yaml.Mapping mapping, RpcSendQueue q) {
        q.getAndSend(mapping, Yaml.Mapping::getOpeningBracePrefix);
        q.getAndSendList(mapping, Yaml.Mapping::getEntries, e -> e.getId(),
                e -> visit(e, q));
        q.getAndSend(mapping, Yaml.Mapping::getClosingBracePrefix);
        q.getAndSend(mapping, Yaml.Mapping::getAnchor, a -> visit(a, q));
        q.getAndSend(mapping, Yaml.Mapping::getTag, t -> visit(t, q));
        return mapping;
    }

    @Override
    public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, RpcSendQueue q) {
        q.getAndSend(entry, Yaml.Mapping.Entry::getKey, k -> visit(k, q));
        q.getAndSend(entry, Yaml.Mapping.Entry::getBeforeMappingValueIndicator);
        q.getAndSend(entry, Yaml.Mapping.Entry::getValue, v -> visit(v, q));
        return entry;
    }

    @Override
    public Yaml visitScalar(Yaml.Scalar scalar, RpcSendQueue q) {
        q.getAndSend(scalar, s -> s.getStyle().name());
        q.getAndSend(scalar, Yaml.Scalar::getAnchor, a -> visit(a, q));
        q.getAndSend(scalar, Yaml.Scalar::getTag, t -> visit(t, q));
        q.getAndSend(scalar, Yaml.Scalar::getValue);
        return scalar;
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence sequence, RpcSendQueue q) {
        q.getAndSend(sequence, Yaml.Sequence::getOpeningBracketPrefix);
        q.getAndSendList(sequence, Yaml.Sequence::getEntries, e -> e.getId(),
                e -> visit(e, q));
        q.getAndSend(sequence, Yaml.Sequence::getClosingBracketPrefix);
        q.getAndSend(sequence, Yaml.Sequence::getAnchor, a -> visit(a, q));
        q.getAndSend(sequence, Yaml.Sequence::getTag, t -> visit(t, q));
        return sequence;
    }

    @Override
    public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, RpcSendQueue q) {
        q.getAndSend(entry, Yaml.Sequence.Entry::getBlock, b -> visit(b, q));
        q.getAndSend(entry, Yaml.Sequence.Entry::isDash);
        q.getAndSend(entry, Yaml.Sequence.Entry::getTrailingCommaPrefix);
        return entry;
    }

    @Override
    public Yaml visitAnchor(Yaml.Anchor anchor, RpcSendQueue q) {
        q.getAndSend(anchor, Yaml.Anchor::getPostfix);
        q.getAndSend(anchor, Yaml.Anchor::getKey);
        return anchor;
    }

    @Override
    public Yaml visitAlias(Yaml.Alias alias, RpcSendQueue q) {
        q.getAndSend(alias, Yaml.Alias::getAnchor, a -> visit(a, q));
        return alias;
    }

    @Override
    public Yaml visitTag(Yaml.Tag tag, RpcSendQueue q) {
        q.getAndSend(tag, Yaml.Tag::getName);
        q.getAndSend(tag, Yaml.Tag::getSuffix);
        q.getAndSend(tag, t -> t.getKind().name());
        return tag;
    }
}
