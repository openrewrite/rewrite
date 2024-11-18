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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.cleanup.RemoveUnusedVisitor;
import org.openrewrite.yaml.format.AutoFormatVisitor;
import org.openrewrite.yaml.tree.Yaml;

public class YamlVisitor<P> extends TreeVisitor<Yaml, P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Yaml.Documents;
    }

    @Override
    public String getLanguage() {
        return "yaml";
    }

    public <Y2 extends Yaml> Y2 maybeAutoFormat(Y2 before, Y2 after, P p) {
        return maybeAutoFormat(before, after, p, getCursor());
    }

    public <Y2 extends Yaml> Y2 maybeAutoFormat(Y2 before, Y2 after, P p, Cursor cursor) {
        return maybeAutoFormat(before, after, null, p, cursor);
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public <Y2 extends Yaml> Y2 maybeAutoFormat(Y2 before, Y2 after, @Nullable Yaml stopAfter, P p, Cursor cursor) {
        if (before != after) {
            return (Y2) new AutoFormatVisitor<>(stopAfter).visit(after, p, cursor);
        }
        return after;
    }

    public <Y2 extends Yaml> Y2 autoFormat(Y2 y, P p) {
        return autoFormat(y, p, getCursor());
    }

    public <Y2 extends Yaml> Y2 autoFormat(Y2 y, P p, Cursor cursor) {
        return autoFormat(y, null, p, cursor);
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public <Y2 extends Yaml> Y2 autoFormat(Y2 y, @Nullable Yaml stopAfter, P p, Cursor cursor) {
        return (Y2) new AutoFormatVisitor<>(stopAfter).visit(y, p, cursor);
    }

    public Yaml visitDocuments(Yaml.Documents documents, P p) {
        System.out.println("visitDocumentSSSSSS");
        return documents.withDocuments(ListUtils.map(documents.getDocuments(), d -> visitAndCast(d, p)))
                .withMarkers(visitMarkers(documents.getMarkers(), p));
    }

    public Yaml visitDocument(Yaml.Document document, P p) {
        System.out.println("visitDocument");
        return document.withBlock((Yaml.Block) visit(document.getBlock(), p))
                .withMarkers(visitMarkers(document.getMarkers(), p));
    }

    public Yaml visitDocumentEnd(Yaml.Document.End end, P p) {
        return end.withMarkers(visitMarkers(end.getMarkers(), p));
    }

    public Yaml visitMapping(Yaml.Mapping mapping, P p) {
        return mapping.withEntries(ListUtils.map(mapping.getEntries(), e -> visitAndCast(e, p)))
                .withMarkers(visitMarkers(mapping.getMarkers(), p));
    }

    public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, P p) {
        Yaml.Mapping.Entry e = entry;
        e = e.withKey(visitAndCast(e.getKey(), p));
        e = e.withValue(visitAndCast(e.getValue(), p));
        return e.withMarkers(visitMarkers(e.getMarkers(), p));
    }

    public Yaml visitScalar(Yaml.Scalar scalar, P p) {
        return scalar.withAnchor(visitAndCast(scalar.getAnchor(), p))
                .withMarkers(visitMarkers(scalar.getMarkers(), p));
    }

    public Yaml visitSequence(Yaml.Sequence sequence, P p) {
        return sequence.withEntries(ListUtils.map(sequence.getEntries(), e -> visitAndCast(e, p)))
                .withMarkers(visitMarkers(sequence.getMarkers(), p));
    }

    public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, P p) {
        return entry.withBlock(visitAndCast(entry.getBlock(), p))
                .withMarkers(visitMarkers(entry.getMarkers(), p));
    }

    public Yaml visitAnchor(Yaml.Anchor anchor, P p) {
        return anchor.withMarkers(visitMarkers(anchor.getMarkers(), p));
    }

    public Yaml visitAlias(Yaml.Alias alias, P p) {
        return alias.withAnchor(visitAndCast(alias.getAnchor(), p))
                .withMarkers(visitMarkers(alias.getMarkers(), p));
    }

    @Deprecated
    public void maybeCoalesceProperties() {
        if (getAfterVisit().stream().noneMatch(CoalescePropertiesVisitor.class::isInstance)) {
            doAfterVisit(new CoalescePropertiesVisitor<>());
        }
    }

    public void removeUnused(@Nullable Cursor cursorParent) {
        if (getAfterVisit().stream().noneMatch(RemoveUnusedVisitor.class::isInstance)) {
            doAfterVisit(new RemoveUnusedVisitor<>(cursorParent));
        }
    }
}
