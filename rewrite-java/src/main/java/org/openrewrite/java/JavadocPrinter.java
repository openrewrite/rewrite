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

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.util.List;

public class JavadocPrinter<P> extends JavadocVisitor<PrintOutputCapture<P>> {
    public JavadocPrinter() {
        this.javaVisitor = new JavadocJavaPrinter();
    }

    @Override
    public Javadoc visitAttribute(Javadoc.Attribute attribute, PrintOutputCapture<P> p) {
        visitMarkers(attribute.getMarkers(), p);
        p.out.append(attribute.getName());
        if (attribute.getSpaceBeforeEqual() != null) {
            visit(attribute.getSpaceBeforeEqual(), p);
            if (attribute.getValue() != null) {
                p.out.append('=');
                visit(attribute.getValue(), p);
            }
        }
        return attribute;
    }

    @Override
    public Javadoc visitAuthor(Javadoc.Author author, PrintOutputCapture<P> p) {
        visitMarkers(author.getMarkers(), p);
        p.out.append("@author");
        visit(author.getName(), p);
        return author;
    }

    @Override
    public Javadoc visitDeprecated(Javadoc.Deprecated deprecated, PrintOutputCapture<P> p) {
        visitMarkers(deprecated.getMarkers(), p);
        p.out.append("@deprecated");
        visit(deprecated.getDescription(), p);
        return deprecated;
    }

    @Override
    public Javadoc visitDocComment(Javadoc.DocComment javadoc, PrintOutputCapture<P> p) {
        visitMarkers(javadoc.getMarkers(), p);
        p.out.append("/**");
        visit(javadoc.getBody(), p);
        p.out.append("*/");
        return javadoc;
    }

    @Override
    public Javadoc visitDocRoot(Javadoc.DocRoot docRoot, PrintOutputCapture<P> p) {
        visitMarkers(docRoot.getMarkers(), p);
        p.out.append("{@docRoot");
        visit(docRoot.getEndBrace(), p);
        return docRoot;
    }

    @Override
    public Javadoc visitDocType(Javadoc.DocType docType, PrintOutputCapture<P> p) {
        visitMarkers(docType.getMarkers(), p);
        p.out.append("<!doctype");
        visit(docType.getText(), p);
        p.out.append('>');
        return docType;
    }

    @Override
    public Javadoc visitEndElement(Javadoc.EndElement endElement, PrintOutputCapture<P> p) {
        visitMarkers(endElement.getMarkers(), p);
        p.out.append("</").append(endElement.getName());
        visit(endElement.getSpaceBeforeEndBracket(), p);
        p.out.append('>');
        return endElement;
    }

    @Override
    public Javadoc visitErroneous(Javadoc.Erroneous erroneous, PrintOutputCapture<P> p) {
        visitMarkers(erroneous.getMarkers(), p);
        visit(erroneous.getText(), p);
        return erroneous;
    }

    @Override
    public Javadoc visitHidden(Javadoc.Hidden hidden, PrintOutputCapture<P> p) {
        visitMarkers(hidden.getMarkers(), p);
        p.out.append("@hidden");
        visit(hidden.getBody(), p);
        return hidden;
    }

    @Override
    public Javadoc visitIndex(Javadoc.Index index, PrintOutputCapture<P> p) {
        visitMarkers(index.getMarkers(), p);
        p.out.append("{@index");
        visit(index.getSearchTerm(), p);
        visit(index.getDescription(), p);
        visit(index.getEndBrace(), p);
        return index;
    }

    @Override
    public Javadoc visitInheritDoc(Javadoc.InheritDoc inheritDoc, PrintOutputCapture<P> p) {
        visitMarkers(inheritDoc.getMarkers(), p);
        p.out.append("{@inheritDoc");
        visit(inheritDoc.getEndBrace(), p);
        return inheritDoc;
    }

    @Override
    public Javadoc visitInlinedValue(Javadoc.InlinedValue value, PrintOutputCapture<P> p) {
        visitMarkers(value.getMarkers(), p);
        p.out.append("{@value");
        visit(value.getSpaceBeforeTree(), p);
        javaVisitor.visit(value.getTree(), p);
        visit(value.getEndBrace(), p);
        return value;
    }

    @Override
    public Javadoc visitLineBreak(Javadoc.LineBreak lineBreak, PrintOutputCapture<P> p) {
        visitMarkers(lineBreak.getMarkers(), p);
        p.out.append(lineBreak.getMargin());
        return lineBreak;
    }

    @Override
    public Javadoc visitLink(Javadoc.Link link, PrintOutputCapture<P> p) {
        visitMarkers(link.getMarkers(), p);
        p.out.append(link.isPlain() ? "{@linkplain" : "{@link");
        visit(link.getSpaceBeforeTree(), p);
        javaVisitor.visit(link.getTree(), p);
        visit(link.getLabel(), p);
        visit(link.getEndBrace(), p);
        return link;
    }

    @Override
    public Javadoc visitLiteral(Javadoc.Literal literal, PrintOutputCapture<P> p) {
        visitMarkers(literal.getMarkers(), p);
        p.out.append(literal.isCode() ? "{@code" : "{@literal");
        visit(literal.getDescription(), p);
        visit(literal.getEndBrace(), p);
        return literal;
    }

    @Override
    public Javadoc visitParameter(Javadoc.Parameter parameter, PrintOutputCapture<P> p) {
        visitMarkers(parameter.getMarkers(), p);
        p.out.append("@param");
        visit(parameter.getSpaceBeforeName(), p);
        javaVisitor.visit(parameter.getName(), p);
        visit(parameter.getDescription(), p);
        return parameter;
    }

    @Override
    public Javadoc visitProvides(Javadoc.Provides provides, PrintOutputCapture<P> p) {
        visitMarkers(provides.getMarkers(), p);
        p.out.append("@provides");
        visit(provides.getSpaceBeforeServiceType(), p);
        javaVisitor.visit(provides.getServiceType(), p);
        visit(provides.getDescription(), p);
        return provides;
    }

    @Override
    public Javadoc visitReturn(Javadoc.Return aReturn, PrintOutputCapture<P> p) {
        visitMarkers(aReturn.getMarkers(), p);
        p.out.append("@return");
        visit(aReturn.getDescription(), p);
        return aReturn;
    }

    @Override
    public Javadoc visitSee(Javadoc.See see, PrintOutputCapture<P> p) {
        visitMarkers(see.getMarkers(), p);
        p.out.append("@see");
        visit(see.getSpaceBeforeTree(), p);
        javaVisitor.visit(see.getTree(), p);
        visit(see.getReference(), p);
        return see;
    }

    @Override
    public Javadoc visitSerial(Javadoc.Serial serial, PrintOutputCapture<P> p) {
        visitMarkers(serial.getMarkers(), p);
        p.out.append("@serial");
        visit(serial.getDescription(), p);
        return serial;
    }

    @Override
    public Javadoc visitSerialData(Javadoc.SerialData serialData, PrintOutputCapture<P> p) {
        visitMarkers(serialData.getMarkers(), p);
        p.out.append("@serialData");
        visit(serialData.getDescription(), p);
        return serialData;
    }

    @Override
    public Javadoc visitSerialField(Javadoc.SerialField serialField, PrintOutputCapture<P> p) {
        visitMarkers(serialField.getMarkers(), p);
        p.out.append("@serialField");
        javaVisitor.visit(serialField.getName(), p);
        javaVisitor.visit(serialField.getType(), p);
        visit(serialField.getDescription(), p);
        return serialField;
    }

    @Override
    public Javadoc visitSince(Javadoc.Since since, PrintOutputCapture<P> p) {
        visitMarkers(since.getMarkers(), p);
        p.out.append("@since");
        visit(since.getDescription(), p);
        return since;
    }

    @Override
    public Javadoc visitStartElement(Javadoc.StartElement startElement, PrintOutputCapture<P> p) {
        visitMarkers(startElement.getMarkers(), p);
        p.out.append('<').append(startElement.getName());
        visit(startElement.getAttributes(), p);
        visit(startElement.getSpaceBeforeEndBracket(), p);
        if (startElement.isSelfClosing()) {
            p.out.append('/');
        }
        p.out.append('>');
        return startElement;
    }

    @Override
    public Javadoc visitSummary(Javadoc.Summary summary, PrintOutputCapture<P> p) {
        visitMarkers(summary.getMarkers(), p);
        p.out.append("{@summary");
        visit(summary.getSummary(), p);
        visit(summary.getBeforeBrace(), p);
        return summary;
    }

    @Override
    public Javadoc visitText(Javadoc.Text text, PrintOutputCapture<P> p) {
        visitMarkers(text.getMarkers(), p);
        p.out.append(text.getText());
        return text;
    }

    @Override
    public Javadoc visitThrows(Javadoc.Throws aThrows, PrintOutputCapture<P> p) {
        visitMarkers(aThrows.getMarkers(), p);
        p.out.append(aThrows.isThrowsKeyword() ? "@throws" : "@exception");
        visit(aThrows.getSpaceBeforeExceptionName(), p);
        javaVisitor.visit(aThrows.getExceptionName(), p);
        visit(aThrows.getDescription(), p);
        return aThrows;
    }

    @Override
    public Javadoc visitUnknownBlock(Javadoc.UnknownBlock unknownBlock, PrintOutputCapture<P> p) {
        visitMarkers(unknownBlock.getMarkers(), p);
        p.out.append("@").append(unknownBlock.getName());
        visit(unknownBlock.getContent(), p);
        return unknownBlock;
    }

    @Override
    public Javadoc visitUnknownInline(Javadoc.UnknownInline unknownInline, PrintOutputCapture<P> p) {
        visitMarkers(unknownInline.getMarkers(), p);
        p.out.append("{@").append(unknownInline.getName());
        visit(unknownInline.getContent(), p);
        visit(unknownInline.getEndBrace(), p);
        return unknownInline;
    }

    @Override
    public Javadoc visitUses(Javadoc.Uses uses, PrintOutputCapture<P> p) {
        visitMarkers(uses.getMarkers(), p);
        p.out.append("@uses");
        visit(uses.getBeforeServiceType(), p);
        javaVisitor.visit(uses.getServiceType(), p);
        visit(uses.getDescription(), p);
        return uses;
    }

    @Override
    public Javadoc visitVersion(Javadoc.Version since, PrintOutputCapture<P> p) {
        visitMarkers(since.getMarkers(), p);
        p.out.append("@version");
        visit(since.getBody(), p);
        return since;
    }

    public void visit(@Nullable List<? extends Javadoc> nodes, PrintOutputCapture<P> p) {
        if (nodes != null) {
            for (Javadoc node : nodes) {
                visit(node, p);
            }
        }
    }

    class JavadocJavaPrinter extends JavaVisitor<PrintOutputCapture<P>> {
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
            visitMarkers(method.getMarkers(), p);
            visitSpace(method.getPrefix(), Space.Location.IDENTIFIER_PREFIX, p);
            visit(method.getSelect(), p);
            if (method.getSelect() != null) {
                p.out.append('#');
            }
            p.out.append(method.getSimpleName());
            visitContainer("(", method.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, ",", ")", p);
            return method;
        }

        @Override
        public J visitIdentifier(J.Identifier ident, PrintOutputCapture<P> p) {
            visitMarkers(ident.getMarkers(), p);
            visitSpace(ident.getPrefix(), Space.Location.IDENTIFIER_PREFIX, p);
            p.out.append(ident.getSimpleName());
            return ident;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
            visitSpace(fieldAccess.getPrefix(), Space.Location.FIELD_ACCESS_PREFIX, p);
            visitMarkers(fieldAccess.getMarkers(), p);
            visit(fieldAccess.getTarget(), p);
            visitLeftPadded(".", fieldAccess.getPadding().getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p);
            return fieldAccess;
        }

        @Override
        public J visitMemberReference(J.MemberReference memberRef, PrintOutputCapture<P> p) {
            visitSpace(memberRef.getPrefix(), Space.Location.MEMBER_REFERENCE_PREFIX, p);
            visitMarkers(memberRef.getMarkers(), p);
            visit(memberRef.getContaining(), p);
            visitLeftPadded("#", memberRef.getPadding().getReference(), JLeftPadded.Location.MEMBER_REFERENCE_NAME, p);
            return memberRef;
        }

        @Override
        public J visitTypeParameter(J.TypeParameter typeParam, PrintOutputCapture<P> p) {
            visitSpace(typeParam.getPrefix(), Space.Location.TYPE_PARAMETERS_PREFIX, p);
            visitMarkers(typeParam.getMarkers(), p);
            p.out.append("<");
            visit(typeParam.getName(), p);
            p.out.append(">");
            return typeParam;
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
            p.out.append(space.getWhitespace());
            return space;
        }

        private void visitLeftPadded(@Nullable String prefix, @Nullable JLeftPadded<? extends J> leftPadded, JLeftPadded.Location location, PrintOutputCapture<P> p) {
            if (leftPadded != null) {
                visitSpace(leftPadded.getBefore(), location.getBeforeLocation(), p);
                if (prefix != null) {
                    p.out.append(prefix);
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
            p.out.append(before);
            visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
            p.out.append(after == null ? "" : after);
        }

        private void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, JRightPadded.Location location, String suffixBetween, PrintOutputCapture<P> p) {
            for (int i = 0; i < nodes.size(); i++) {
                JRightPadded<? extends J> node = nodes.get(i);
                visit(node.getElement(), p);
                visitSpace(node.getAfter(), location.getAfterLocation(), p);
                if (i < nodes.size() - 1) {
                    p.out.append(suffixBetween);
                }
            }
        }
    }
}
