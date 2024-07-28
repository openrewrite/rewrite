/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.LeadingBrace;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.util.List;
import java.util.function.UnaryOperator;

public class JavadocPrinter<P> extends JavadocVisitor<PrintOutputCapture<P>> {
    public JavadocPrinter() {
        super(new JavadocJavaPrinter<>());
    }

    @Override
    public Javadoc visitAttribute(Javadoc.Attribute attribute, PrintOutputCapture<P> p) {
        beforeSyntax(attribute, p);
        p.append(attribute.getName());
        if (attribute.getSpaceBeforeEqual() != null && !attribute.getSpaceBeforeEqual().isEmpty()) {
            visit(attribute.getSpaceBeforeEqual(), p);
            if (attribute.getValue() != null) {
                p.append('=');
                visit(attribute.getValue(), p);
            }
        }
        afterSyntax(attribute, p);
        return attribute;
    }

    @Override
    public Javadoc visitAuthor(Javadoc.Author author, PrintOutputCapture<P> p) {
        beforeSyntax(author, p);
        p.append("@author");
        visit(author.getName(), p);
        afterSyntax(author, p);
        return author;
    }

    @Override
    public Javadoc visitDeprecated(Javadoc.Deprecated deprecated, PrintOutputCapture<P> p) {
        beforeSyntax(deprecated, p);
        p.append("@deprecated");
        visit(deprecated.getDescription(), p);
        afterSyntax(deprecated, p);
        return deprecated;
    }

    @Override
    public Javadoc visitDocComment(Javadoc.DocComment javadoc, PrintOutputCapture<P> p) {
        beforeSyntax(javadoc, p);
        p.append("/**");
        visit(javadoc.getBody(), p);
        p.append("*/");
        afterSyntax(javadoc, p);
        return javadoc;
    }

    @Override
    public Javadoc visitDocRoot(Javadoc.DocRoot docRoot, PrintOutputCapture<P> p) {
        beforeSyntax(docRoot, p);
        p.append("{@docRoot");
        visit(docRoot.getEndBrace(), p);
        afterSyntax(docRoot, p);
        return docRoot;
    }

    @Override
    public Javadoc visitDocType(Javadoc.DocType docType, PrintOutputCapture<P> p) {
        beforeSyntax(docType, p);
        p.append("<!doctype");
        visit(docType.getText(), p);
        p.append('>');
        afterSyntax(docType, p);
        return docType;
    }

    @Override
    public Javadoc visitEndElement(Javadoc.EndElement endElement, PrintOutputCapture<P> p) {
        beforeSyntax(endElement, p);
        p.append("</").append(endElement.getName());
        visit(endElement.getSpaceBeforeEndBracket(), p);
        p.append('>');
        afterSyntax(endElement, p);
        return endElement;
    }

    @Override
    public Javadoc visitErroneous(Javadoc.Erroneous erroneous, PrintOutputCapture<P> p) {
        beforeSyntax(erroneous, p);
        visit(erroneous.getText(), p);
        afterSyntax(erroneous, p);
        return erroneous;
    }

    @Override
    public Javadoc visitHidden(Javadoc.Hidden hidden, PrintOutputCapture<P> p) {
        beforeSyntax(hidden, p);
        p.append("@hidden");
        visit(hidden.getBody(), p);
        afterSyntax(hidden, p);
        return hidden;
    }

    @Override
    public Javadoc visitIndex(Javadoc.Index index, PrintOutputCapture<P> p) {
        beforeSyntax(index, p);
        p.append("{@index");
        visit(index.getSearchTerm(), p);
        visit(index.getDescription(), p);
        visit(index.getEndBrace(), p);
        afterSyntax(index, p);
        return index;
    }

    @Override
    public Javadoc visitInheritDoc(Javadoc.InheritDoc inheritDoc, PrintOutputCapture<P> p) {
        beforeSyntax(inheritDoc, p);
        p.append("{@inheritDoc");
        visit(inheritDoc.getEndBrace(), p);
        afterSyntax(inheritDoc, p);
        return inheritDoc;
    }

    @Override
    public Javadoc visitInlinedValue(Javadoc.InlinedValue value, PrintOutputCapture<P> p) {
        beforeSyntax(value, p);
        p.append("{@value");
        visit(value.getSpaceBeforeTree(), p);
        javaVisitorVisit(value.getTree(), p);
        visit(value.getEndBrace(), p);
        afterSyntax(value, p);
        return value;
    }

    @Override
    public Javadoc visitLineBreak(Javadoc.LineBreak lineBreak, PrintOutputCapture<P> p) {
        beforeSyntax(lineBreak, p);
        p.append(lineBreak.getMargin());
        afterSyntax(lineBreak, p);
        return lineBreak;
    }

    @Override
    public Javadoc visitLink(Javadoc.Link link, PrintOutputCapture<P> p) {
        beforeSyntax(link, p);
        p.append(link.isPlain() ? "{@linkplain" : "{@link");
        visit(link.getSpaceBeforeTree(), p);
        visit(link.getTreeReference(), p);
        visit(link.getLabel(), p);
        visit(link.getEndBrace(), p);
        afterSyntax(link, p);
        return link;
    }

    @Override
    public Javadoc visitLiteral(Javadoc.Literal literal, PrintOutputCapture<P> p) {
        beforeSyntax(literal, p);
        p.append(literal.isCode() ? "{@code" : "{@literal");
        visit(literal.getDescription(), p);
        visit(literal.getEndBrace(), p);
        afterSyntax(literal, p);
        return literal;
    }

    @Override
    public Javadoc visitParameter(Javadoc.Parameter parameter, PrintOutputCapture<P> p) {
        beforeSyntax(parameter, p);
        p.append("@param");
        visit(parameter.getSpaceBeforeName(), p);
        visit(parameter.getNameReference(), p);
        visit(parameter.getDescription(), p);
        afterSyntax(parameter, p);
        return parameter;
    }

    @Override
    public Javadoc visitProvides(Javadoc.Provides provides, PrintOutputCapture<P> p) {
        beforeSyntax(provides, p);
        p.append("@provides");
        visit(provides.getSpaceBeforeServiceType(), p);
        javaVisitorVisit(provides.getServiceType(), p);
        visit(provides.getDescription(), p);
        afterSyntax(provides, p);
        return provides;
    }

    @Override
    public Javadoc visitReturn(Javadoc.Return aReturn, PrintOutputCapture<P> p) {
        beforeSyntax(aReturn, p);
        if (aReturn.getMarkers().findFirst(LeadingBrace.class).isPresent()) {
            p.append("{");
        }
        p.append("@return");
        visit(aReturn.getDescription(), p);
        afterSyntax(aReturn, p);
        return aReturn;
    }

    @Override
    public Javadoc visitSee(Javadoc.See see, PrintOutputCapture<P> p) {
        beforeSyntax(see, p);
        p.append("@see");
        visit(see.getSpaceBeforeTree(), p);
        visit(see.getTreeReference(), p);
        visit(see.getReference(), p);
        afterSyntax(see, p);
        return see;
    }

    @Override
    public Javadoc visitSerial(Javadoc.Serial serial, PrintOutputCapture<P> p) {
        beforeSyntax(serial, p);
        p.append("@serial");
        visit(serial.getDescription(), p);
        afterSyntax(serial, p);
        return serial;
    }

    @Override
    public Javadoc visitSerialData(Javadoc.SerialData serialData, PrintOutputCapture<P> p) {
        beforeSyntax(serialData, p);
        p.append("@serialData");
        visit(serialData.getDescription(), p);
        afterSyntax(serialData, p);
        return serialData;
    }

    @Override
    public Javadoc visitSerialField(Javadoc.SerialField serialField, PrintOutputCapture<P> p) {
        beforeSyntax(serialField, p);
        p.append("@serialField");
        javaVisitorVisit(serialField.getName(), p);
        javaVisitorVisit(serialField.getType(), p);
        visit(serialField.getDescription(), p);
        afterSyntax(serialField, p);
        return serialField;
    }

    @Override
    public Javadoc visitSince(Javadoc.Since since, PrintOutputCapture<P> p) {
        beforeSyntax(since, p);
        p.append("@since");
        visit(since.getDescription(), p);
        afterSyntax(since, p);
        return since;
    }

    @Override
    public Javadoc visitStartElement(Javadoc.StartElement startElement, PrintOutputCapture<P> p) {
        beforeSyntax(startElement, p);
        p.append('<').append(startElement.getName());
        visit(startElement.getAttributes(), p);
        visit(startElement.getSpaceBeforeEndBracket(), p);
        if (startElement.isSelfClosing()) {
            p.append('/');
        }
        p.append('>');
        afterSyntax(startElement, p);
        return startElement;
    }

    @Override
    public Javadoc visitSummary(Javadoc.Summary summary, PrintOutputCapture<P> p) {
        beforeSyntax(summary, p);
        p.append("{@summary");
        visit(summary.getSummary(), p);
        visit(summary.getBeforeBrace(), p);
        afterSyntax(summary, p);
        return summary;
    }

    @Override
    public Javadoc visitText(Javadoc.Text text, PrintOutputCapture<P> p) {
        beforeSyntax(text, p);
        p.append(text.getText());
        afterSyntax(text, p);
        return text;
    }

    @Override
    public Javadoc visitThrows(Javadoc.Throws aThrows, PrintOutputCapture<P> p) {
        beforeSyntax(aThrows, p);
        p.append(aThrows.isThrowsKeyword() ? "@throws" : "@exception");
        visit(aThrows.getSpaceBeforeExceptionName(), p);
        javaVisitorVisit(aThrows.getExceptionName(), p);
        visit(aThrows.getDescription(), p);
        afterSyntax(aThrows, p);
        return aThrows;
    }

    @Override
    public Javadoc visitUnknownBlock(Javadoc.UnknownBlock unknownBlock, PrintOutputCapture<P> p) {
        beforeSyntax(unknownBlock, p);
        p.append("@").append(unknownBlock.getName());
        visit(unknownBlock.getContent(), p);
        afterSyntax(unknownBlock, p);
        return unknownBlock;
    }

    @Override
    public Javadoc visitUnknownInline(Javadoc.UnknownInline unknownInline, PrintOutputCapture<P> p) {
        beforeSyntax(unknownInline, p);
        p.append("{@").append(unknownInline.getName());
        visit(unknownInline.getContent(), p);
        visit(unknownInline.getEndBrace(), p);
        afterSyntax(unknownInline, p);
        return unknownInline;
    }

    @Override
    public Javadoc visitUses(Javadoc.Uses uses, PrintOutputCapture<P> p) {
        beforeSyntax(uses, p);
        p.append("@uses");
        visit(uses.getBeforeServiceType(), p);
        javaVisitorVisit(uses.getServiceType(), p);
        visit(uses.getDescription(), p);
        afterSyntax(uses, p);
        return uses;
    }

    @Override
    public Javadoc visitVersion(Javadoc.Version since, PrintOutputCapture<P> p) {
        beforeSyntax(since, p);
        p.append("@version");
        visit(since.getBody(), p);
        afterSyntax(since, p);
        return since;
    }

    public void visit(@Nullable List<? extends Javadoc> nodes, PrintOutputCapture<P> p) {
        if (nodes != null) {
            for (Javadoc node : nodes) {
                visit(node, p);
            }
        }
    }

    @Override
    public Javadoc visitReference(Javadoc.Reference reference, PrintOutputCapture<P> p) {
        getCursor().putMessageOnFirstEnclosing(Javadoc.DocComment.class, "JAVADOC_LINE_BREAKS", reference.getLineBreaks());
        getCursor().putMessageOnFirstEnclosing(Javadoc.DocComment.class, "JAVADOC_LINE_BREAK_INDEX", 0);
        javaVisitorVisit(reference.getTree(), p);
        afterSyntax(reference, p);
        return reference;
    }

    static class JavadocJavaPrinter<P> extends JavaVisitor<PrintOutputCapture<P>> {
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
            beforeSyntax(method, Space.Location.IDENTIFIER_PREFIX, p);
            visit(method.getSelect(), p);
            if (method.getSelect() != null) {
                p.append('#');
            }
            p.append(method.getSimpleName());
            visitContainer("(", method.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, ",", ")", p);
            afterSyntax(method, p);
            return method;
        }

        @Override
        public J visitIdentifier(J.Identifier ident, PrintOutputCapture<P> p) {
            beforeSyntax(ident, Space.Location.IDENTIFIER_PREFIX, p);
            p.append(ident.getSimpleName());
            afterSyntax(ident, p);
            return ident;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
            beforeSyntax(fieldAccess, Space.Location.FIELD_ACCESS_PREFIX, p);
            visit(fieldAccess.getTarget(), p);
            visitLeftPadded(".", fieldAccess.getPadding().getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p);
            afterSyntax(fieldAccess, p);
            return fieldAccess;
        }

        @Override
        public J visitMemberReference(J.MemberReference memberRef, PrintOutputCapture<P> p) {
            beforeSyntax(memberRef, Space.Location.MEMBER_REFERENCE_PREFIX, p);
            visit(memberRef.getContaining(), p);
            visitLeftPadded("#", memberRef.getPadding().getReference(), JLeftPadded.Location.MEMBER_REFERENCE_NAME, p);
            afterSyntax(memberRef, p);
            return memberRef;
        }

        @Override
        public J visitArrayType(J.ArrayType arrayType, PrintOutputCapture<P> p) {
            beforeSyntax(arrayType, Space.Location.ARRAY_TYPE_PREFIX, p);
            visit(arrayType.getElementType(), p);
            visit(arrayType.getAnnotations(), p);
            if (arrayType.getDimension() != null) {
                visitSpace(arrayType.getDimension().getBefore(), Space.Location.DIMENSION_PREFIX, p);
                p.append('[');
                visitSpace(arrayType.getDimension().getElement(), Space.Location.DIMENSION, p);
                p.append(']');
            }
            afterSyntax(arrayType, p);
            return arrayType;
        }

        @Override
        public J visitParameterizedType(J.ParameterizedType type, PrintOutputCapture<P> p) {
            beforeSyntax(type, Space.Location.IDENTIFIER_PREFIX, p);
            visit(type.getClazz(), p);
            visitContainer("<", type.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
            afterSyntax(type, p);
            return type;
        }

        @Override
        public J visitTypeParameter(J.TypeParameter typeParam, PrintOutputCapture<P> p) {
            beforeSyntax(typeParam, Space.Location.TYPE_PARAMETERS_PREFIX, p);
            p.append("<");
            visit(typeParam.getName(), p);
            p.append(">");
            afterSyntax(typeParam, p);
            return typeParam;
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
            List<Javadoc.LineBreak> lineBreaks = getCursor().getNearestMessage("JAVADOC_LINE_BREAKS");
            Integer index = getCursor().getNearestMessage("JAVADOC_LINE_BREAK_INDEX");

            String whitespace = space.getWhitespace();
            if (lineBreaks != null && !lineBreaks.isEmpty() && index != null && whitespace.contains("\n")) {
                for (int i = 0; i < whitespace.length(); i++) {
                    char c = whitespace.charAt(i);
                    // The Space from a JavaDoc will not contain a CR because the JavaDoc parser
                    // filters out other new line characters. CRLF is detected through the source
                    // and only exists through LineBreaks.
                    if (c == '\n') {
                        visitLineBreak(lineBreaks.get(index), p);
                        index++;
                    } else {
                        p.append(c);
                    }
                }
                getCursor().putMessageOnFirstEnclosing(Javadoc.DocComment.class, "JAVADOC_LINE_BREAK_INDEX", index);
            } else {
                p.append(whitespace);
            }
            return space;
        }

        @Override
        public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
            if (marker instanceof SearchResult) {
                String description = ((SearchResult) marker).getDescription();
                p.append("~~")
                        .append(description == null ? "" : "(" + description + ")~~")
                        .append(">");
            }
            //noinspection unchecked
            return (M) marker;
        }

        private void visitLineBreak(Javadoc.LineBreak lineBreak, PrintOutputCapture<P> p) {
            beforeSyntax(Space.EMPTY, lineBreak.getMarkers(), null, p);
            p.append(lineBreak.getMargin());
            afterSyntax(lineBreak.getMarkers(), p);
        }

        private void visitLeftPadded(@Nullable String prefix, @Nullable JLeftPadded<? extends J> leftPadded, JLeftPadded.Location location, PrintOutputCapture<P> p) {
            if (leftPadded != null) {
                visitSpace(leftPadded.getBefore(), location.getBeforeLocation(), p);
                if (prefix != null) {
                    p.append(prefix);
                }
                visit(leftPadded.getElement(), p);
            }
        }

        @SuppressWarnings("SameParameterValue")
        private void visitContainer(String before, @Nullable JContainer<? extends J> container, JContainer.Location location, String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
            if (container == null) {
                return;
            }
            visitSpace(container.getBefore(), location.getBeforeLocation(), p);
            p.append(before);
            visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
            p.append(after == null ? "" : after);
        }

        private void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, JRightPadded.Location location, String suffixBetween, PrintOutputCapture<P> p) {
            for (int i = 0; i < nodes.size(); i++) {
                JRightPadded<? extends J> node = nodes.get(i);
                visit(node.getElement(), p);
                visitSpace(node.getAfter(), location.getAfterLocation(), p);
                if (i < nodes.size() - 1) {
                    p.append(suffixBetween);
                }
            }
        }

        private void beforeSyntax(J j, Space.Location loc, PrintOutputCapture<P> p) {
            beforeSyntax(j.getPrefix(), j.getMarkers(), loc, p);
        }

        private void beforeSyntax(Space prefix, Markers markers, @Nullable Space.Location loc, PrintOutputCapture<P> p) {
            for (Marker marker : markers.getMarkers()) {
                p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVADOC_MARKER_WRAPPER));
            }
            if (loc != null) {
                visitSpace(prefix, loc, p);
            }
            visitMarkers(markers, p);
            for (Marker marker : markers.getMarkers()) {
                p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVADOC_MARKER_WRAPPER));
            }
        }

        private void afterSyntax(J j, PrintOutputCapture<P> p) {
            afterSyntax(j.getMarkers(), p);
        }

        private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
            for (Marker marker : markers.getMarkers()) {
                p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), JAVADOC_MARKER_WRAPPER));
            }
        }
    }

    private static final UnaryOperator<String> JAVADOC_MARKER_WRAPPER =
            out -> "~~" + out + (out.isEmpty() ? "" : "~~") + ">";

    private void beforeSyntax(Javadoc j, PrintOutputCapture<P> p) {
        beforeSyntax(j.getMarkers(), p);
    }

    private void beforeSyntax(Markers markers, PrintOutputCapture<P> p) {
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVADOC_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(Javadoc j, PrintOutputCapture<P> p) {
        afterSyntax(j.getMarkers(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), JAVADOC_MARKER_WRAPPER));
        }
    }
}
