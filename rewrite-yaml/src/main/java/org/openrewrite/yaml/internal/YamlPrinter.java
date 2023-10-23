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
package org.openrewrite.yaml.internal;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.function.UnaryOperator;

public class YamlPrinter<P> extends YamlVisitor<PrintOutputCapture<P>> {

    @Override
    public Yaml visitDocument(Yaml.Document document, PrintOutputCapture<P> p) {
        beforeSyntax(document, p);
        if (document.isExplicit()) {
            p.append("---");
        }
        visit(document.getBlock(), p);
        if (document.getEnd() != null) {
            p.append(document.getEnd().getPrefix());
            if (document.getEnd().isExplicit()) {
                p.append("...");
            }
        }
        afterSyntax(document, p);
        return document;
    }

    @Override
    public Yaml visitDocuments(Yaml.Documents documents, PrintOutputCapture<P> p) {
        visitMarkers(documents.getMarkers(), p);
        visit(documents.getDocuments(), p);
        afterSyntax(documents, p);
        return documents;
    }

    @Override
    public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, PrintOutputCapture<P> p) {
        p.append(entry.getPrefix());
        if (entry.isDash()) {
            p.append('-');
        }
        visit(entry.getBlock(), p);
        if (entry.getTrailingCommaPrefix() != null) {
            p.append(entry.getTrailingCommaPrefix()).append(',');
        }
        afterSyntax(entry, p);
        return entry;
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence sequence, PrintOutputCapture<P> p) {
        visitMarkers(sequence.getMarkers(), p);
        if (sequence.getAnchor() != null) {
            visit(sequence.getAnchor(), p);
        }
        if (sequence.getOpeningBracketPrefix() != null) {
            p.append(sequence.getOpeningBracketPrefix()).append('[');
        }
        Yaml result = super.visitSequence(sequence, p);
        if (sequence.getClosingBracketPrefix() != null) {
            p.append(sequence.getClosingBracketPrefix()).append(']');
        }

        afterSyntax(result, p);
        return result;
    }

    @Override
    public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, PrintOutputCapture<P> p) {
        beforeSyntax(entry, p);
        visit(entry.getKey(), p);
        p.append(entry.getBeforeMappingValueIndicator()).append(':');
        visit(entry.getValue(), p);
        afterSyntax(entry, p);
        return entry;
    }

    @Override
    public Yaml visitMapping(Yaml.Mapping mapping, PrintOutputCapture<P> p) {
        visitMarkers(mapping.getMarkers(), p);
        if (mapping.getAnchor() != null) {
            visit(mapping.getAnchor(), p);
        }
        if (mapping.getOpeningBracePrefix() != null) {
            p.append(mapping.getOpeningBracePrefix()).append('{');
        }
        Yaml result = super.visitMapping(mapping, p);
        if (mapping.getClosingBracePrefix() != null) {
            p.append(mapping.getClosingBracePrefix()).append('}');
        }
        afterSyntax(result, p);
        return result;
    }

    @Override
    public Yaml visitScalar(Yaml.Scalar scalar, PrintOutputCapture<P> p) {
        beforeSyntax(scalar, p);
        if (scalar.getAnchor() != null) {
            visit(scalar.getAnchor(), p);
        }
        switch (scalar.getStyle()) {
            case DOUBLE_QUOTED:
                p.append('"')
                        .append(scalar.getValue())
                        .append('"');
                break;
            case SINGLE_QUOTED:
                p.append('\'')
                        .append(scalar.getValue())
                        .append('\'');
                break;
            case LITERAL:
                p.append('|')
                        .append(scalar.getValue());
                break;
            case FOLDED:
                p.append('>')
                        .append(scalar.getValue());
                break;
            case PLAIN:
            default:
                p.append(scalar.getValue());
                break;

        }
        afterSyntax(scalar, p);
        return scalar;
    }

    public Yaml visitAnchor(Yaml.Anchor anchor, PrintOutputCapture<P> p) {
        visitMarkers(anchor.getMarkers(), p);
        p.append(anchor.getPrefix());
        p.append("&");
        p.append(anchor.getKey());
        p.append(anchor.getPostfix());
        afterSyntax(anchor, p);
        return anchor;
    }

    public Yaml visitAlias(Yaml.Alias alias, PrintOutputCapture<P> p) {
        beforeSyntax(alias, p);
        p.append("*");
        if (alias.getAnchor() != null) {
            p.append(alias.getAnchor().getKey());
        }
        afterSyntax(alias, p);
        return alias;
    }

    private static final UnaryOperator<String> YAML_MARKER_WRAPPER =
            out -> "~~" + out + (out.isEmpty() ? "" : "~~") + ">";

    private void beforeSyntax(Yaml y, PrintOutputCapture<P> p) {
        for (Marker marker : y.getMarkers().getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), YAML_MARKER_WRAPPER));
        }
        p.append(y.getPrefix());
        visitMarkers(y.getMarkers(), p);
        for (Marker marker : y.getMarkers().getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), YAML_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(Yaml y, PrintOutputCapture<P> p) {
        for (Marker marker : y.getMarkers().getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), YAML_MARKER_WRAPPER));
        }
    }
}
