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

import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;
import org.openrewrite.yaml.tree.YamlKey;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class YamlReceiver extends YamlVisitor<RpcReceiveQueue> {

    @Override
    public Yaml preVisit(Yaml y, RpcReceiveQueue q) {
        y = y.withId(q.receiveAndGet(y.getId(), UUID::fromString));
        y = y.withPrefix(q.receive(y.getPrefix()));
        return y.withMarkers(q.receive(y.getMarkers()));
    }

    @Override
    public Yaml visitDocuments(Yaml.Documents documents, RpcReceiveQueue q) {
        Yaml.Documents d = documents;
        d = d.withSourcePath(q.<Path, String>receiveAndGet(d.getSourcePath(), Paths::get));
        d = (Yaml.Documents) d.withCharset(q.<Charset, String>receiveAndGet(d.getCharset(), Charset::forName));
        d = d.withCharsetBomMarked(q.receive(d.isCharsetBomMarked()));
        d = d.withChecksum(q.receive(d.getChecksum()));
        d = d.withFileAttributes(q.receive(d.getFileAttributes()));
        d = d.withDocuments(q.receiveList(d.getDocuments(), doc -> (Yaml.Document) visitNonNull(doc, q)));
        return d.withSuffix(q.receive(d.getSuffix()));
    }

    @Override
    public Yaml visitDocument(Yaml.Document document, RpcReceiveQueue q) {
        return document
                .withDirectives(q.receiveList(document.getDirectives(), dir -> (Yaml.Directive) visitNonNull(dir, q)))
                .withExplicit(q.receive(document.isExplicit()))
                .withBlock(q.receive(document.getBlock(), b -> (Yaml.Block) visitNonNull(b, q)))
                .withEnd(q.receive(document.getEnd(), e -> (Yaml.Document.End) visitNonNull(e, q)));
    }

    @Override
    public Yaml visitDirective(Yaml.Directive directive, RpcReceiveQueue q) {
        return directive
                .withValue(q.receive(directive.getValue()))
                .withSuffix(q.receive(directive.getSuffix()));
    }

    @Override
    public Yaml visitDocumentEnd(Yaml.Document.End end, RpcReceiveQueue q) {
        return end.withExplicit(q.receive(end.isExplicit()));
    }

    @Override
    public Yaml visitMapping(Yaml.Mapping mapping, RpcReceiveQueue q) {
        return mapping
                .withOpeningBracePrefix(q.receive(mapping.getOpeningBracePrefix()))
                .withEntries(q.receiveList(mapping.getEntries(), e -> (Yaml.Mapping.Entry) visitNonNull(e, q)))
                .withClosingBracePrefix(q.receive(mapping.getClosingBracePrefix()))
                .withAnchor(q.receive(mapping.getAnchor(), a -> (Yaml.Anchor) visitNonNull(a, q)))
                .withTag(q.receive(mapping.getTag(), t -> (Yaml.Tag) visitNonNull(t, q)));
    }

    @Override
    public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, RpcReceiveQueue q) {
        return entry
                .withKey(q.receive(entry.getKey(), k -> (YamlKey) visitNonNull(k, q)))
                .withBeforeMappingValueIndicator(q.receive(entry.getBeforeMappingValueIndicator()))
                .withValue(q.receive(entry.getValue(), v -> (Yaml.Block) visitNonNull(v, q)));
    }

    @Override
    public Yaml visitScalar(Yaml.Scalar scalar, RpcReceiveQueue q) {
        return scalar
                .withStyle(q.<Yaml.Scalar.Style, String>receiveAndGet(scalar.getStyle(), Yaml.Scalar.Style::valueOf))
                .withAnchor(q.receive(scalar.getAnchor(), a -> (Yaml.Anchor) visitNonNull(a, q)))
                .withTag(q.receive(scalar.getTag(), t -> (Yaml.Tag) visitNonNull(t, q)))
                .withValue(q.receive(scalar.getValue()));
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence sequence, RpcReceiveQueue q) {
        return sequence
                .withOpeningBracketPrefix(q.receive(sequence.getOpeningBracketPrefix()))
                .withEntries(q.receiveList(sequence.getEntries(), e -> (Yaml.Sequence.Entry) visitNonNull(e, q)))
                .withClosingBracketPrefix(q.receive(sequence.getClosingBracketPrefix()))
                .withAnchor(q.receive(sequence.getAnchor(), a -> (Yaml.Anchor) visitNonNull(a, q)))
                .withTag(q.receive(sequence.getTag(), t -> (Yaml.Tag) visitNonNull(t, q)));
    }

    @Override
    public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, RpcReceiveQueue q) {
        return entry
                .withBlock(q.receive(entry.getBlock(), b -> (Yaml.Block) visitNonNull(b, q)))
                .withDash(q.receive(entry.isDash()))
                .withTrailingCommaPrefix(q.receive(entry.getTrailingCommaPrefix()));
    }

    @Override
    public Yaml visitAnchor(Yaml.Anchor anchor, RpcReceiveQueue q) {
        return anchor
                .withPostfix(q.receive(anchor.getPostfix()))
                .withKey(q.receive(anchor.getKey()));
    }

    @Override
    public Yaml visitAlias(Yaml.Alias alias, RpcReceiveQueue q) {
        return alias.withAnchor(q.receive(alias.getAnchor(), a -> (Yaml.Anchor) visitNonNull(a, q)));
    }

    @Override
    public Yaml visitTag(Yaml.Tag tag, RpcReceiveQueue q) {
        return tag
                .withName(q.receive(tag.getName()))
                .withSuffix(q.receive(tag.getSuffix()))
                .withKind(q.<Yaml.Tag.Kind, String>receiveAndGet(tag.getKind(), Yaml.Tag.Kind::valueOf));
    }
}
