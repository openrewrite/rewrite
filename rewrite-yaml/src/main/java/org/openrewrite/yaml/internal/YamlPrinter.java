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

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

public class YamlPrinter<P> extends YamlVisitor<PrintOutputCapture<P>> {

    @Override
    public Yaml visitDocument(Yaml.Document document, PrintOutputCapture<P> p) {
        p.out.append(document.getPrefix());
        visitMarkers(document.getMarkers(), p);
        if (document.isExplicit()) {
            p.out.append("---");
        }
        visit(document.getBlock(), p);
        if (document.getEnd() != null) {
            p.out.append(document.getEnd().getPrefix());
            if (document.getEnd().isExplicit()) {
                p.out.append("...");
            }
        }
        return document;
    }

    @Override
    public Yaml visitDocuments(Yaml.Documents documents, PrintOutputCapture<P> p) {
        visitMarkers(documents.getMarkers(), p);
        visit(documents.getDocuments(), p);
        return documents;
    }

    @Override
    public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, PrintOutputCapture<P> p) {
        p.out.append(entry.getPrefix());
        if(entry.isDash()) {
            p.out.append('-');
        }
        visit(entry.getBlock(), p);
        if(entry.getTrailingCommaPrefix() != null) {
            p.out.append(entry.getTrailingCommaPrefix()).append(',');
        }
        return entry;
    }

    @Override
    public Yaml visitSequence(Yaml.Sequence sequence, PrintOutputCapture<P> p) {
        visitMarkers(sequence.getMarkers(), p);
        if (sequence.getAnchor() != null) {
            visit(sequence.getAnchor(), p);
        }
        if(sequence.getOpeningBracketPrefix() != null) {
            p.out.append(sequence.getOpeningBracketPrefix()).append('[');
        }
        Yaml result = super.visitSequence(sequence, p);
        if(sequence.getClosingBracketPrefix() != null) {
            p.out.append(sequence.getClosingBracketPrefix()).append(']');
        }

        return result;
    }

    @Override
    public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, PrintOutputCapture<P> p) {
        p.out.append(entry.getPrefix());
        visitMarkers(entry.getMarkers(), p);
        visit(entry.getKey(), p);
        p.out.append(entry.getBeforeMappingValueIndicator()).append(':');
        visit(entry.getValue(), p);
        return entry;
    }

    @Override
    public Yaml visitMapping(Yaml.Mapping mapping, PrintOutputCapture<P> p) {
        visitMarkers(mapping.getMarkers(), p);
        if (mapping.getAnchor() != null) {
            visit(mapping.getAnchor(), p);
        }
        if (mapping.getOpeningBracePrefix() != null) {
            p.out.append(mapping.getOpeningBracePrefix()).append('{');
        }
        Yaml result = super.visitMapping(mapping, p);
        if (mapping.getClosingBracePrefix() != null) {
            p.out.append(mapping.getClosingBracePrefix()).append('}');
        }
        return result;
    }

    @Override
    public Yaml visitScalar(Yaml.Scalar scalar, PrintOutputCapture<P> p) {
        p.out.append(scalar.getPrefix());
        visitMarkers(scalar.getMarkers(), p);
        if (scalar.getAnchor() != null) {
            visit(scalar.getAnchor(), p);
        }
        switch (scalar.getStyle()) {
            case DOUBLE_QUOTED:
                p.out.append('"')
                        .append(scalar.getValue()
                                .replace("\\", "\\\\")
                                .replace("\0", "\\0")
                                .replace("\u0007", "\\a")
                                .replace("\b", "\\b")
                                .replace("\t", "\\t")
                                .replace("\n", "\\n")
                                .replace("\u000B", "\\v")
                                .replace("\f", "\\f")
                                .replace("\r", "\\r")
                                .replace("\u001B", "\\e")
                                .replace("\"", "\\\"")
                                .replace("\u0085", "\\N")
                                .replace("\u00A0", "\\_")
                                .replace("\u2028", "\\L")
                                .replace("\u2029", "\\P")
                        )
                        .append('"');
                break;
            case SINGLE_QUOTED:
                p.out.append('\'')
                        .append(scalar.getValue())
                        .append('\'');
                break;
            case LITERAL:
                p.out.append('|')
                        .append(scalar.getValue());
                break;
            case FOLDED:
                p.out.append('>')
                        .append(scalar.getValue());
                break;
            case PLAIN:
            default:
                p.out.append(scalar.getValue());
                break;

        }
        return scalar;
    }

    public Yaml visitAnchor(Yaml.Anchor anchor, PrintOutputCapture<P> p) {
        visitMarkers(anchor.getMarkers(), p);
        p.out.append(anchor.getPrefix());
        p.out.append("&");
        p.out.append(anchor.getKey());
        p.out.append(anchor.getPostfix());
        return anchor;
    }

    public Yaml visitAlias(Yaml.Alias alias, PrintOutputCapture<P> p) {
        p.out.append(alias.getPrefix());
        visitMarkers(alias.getMarkers(), p);
        p.out.append("*");
        p.out.append(alias.getAnchor().getKey());
        return alias;
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        if(marker instanceof SearchResult) {
            String description = ((SearchResult) marker).getDescription();
            p.out.append("~~")
                    .append(description == null ? "" : "(" + description + ")~~")
                    .append(">");
        }
        //noinspection unchecked
        return (M) marker;
    }
}
