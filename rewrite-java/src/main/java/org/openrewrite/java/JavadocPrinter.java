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
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.util.List;

public class JavadocPrinter<P> extends JavadocVisitor<P> {
    private static final String PRINTER_ACC_KEY = "printed";

    private final TreePrinter<P> treePrinter;

    public JavadocPrinter(TreePrinter<P> treePrinter) {
        this.javaVisitor = new JavadocJavaPrinter();
        this.treePrinter = treePrinter;
    }

    @NonNull
    protected StringBuilder getPrinter() {
        StringBuilder acc = getCursor().getRoot().getNearestMessage(PRINTER_ACC_KEY);
        if (acc == null) {
            acc = new StringBuilder();
            getCursor().getRoot().putMessage(PRINTER_ACC_KEY, acc);
        }
        return acc;
    }

    public String print(Javadoc j, P p) {
        setCursor(new Cursor(null, "EPSILON"));
        visit(j, p);
        return getPrinter().toString();
    }

    @Override
    @Nullable
    public Javadoc visit(@Nullable Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        StringBuilder printerAcc = getPrinter();
        treePrinter.doBefore(tree, printerAcc, p);
        tree = super.visit(tree, p);
        if (tree != null) {
            treePrinter.doAfter(tree, printerAcc, p);
        }
        return (Javadoc) tree;
    }

    @Override
    public Javadoc visitAttribute(Javadoc.Attribute attribute, P p) {
        visitMarkers(attribute.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(attribute.getPrefix()).append(attribute.getName());
        if(attribute.getBeforeEqual() != null) {
            visit(attribute.getBeforeEqual(), p);
            if(attribute.getValue() != null) {
                acc.append('=');
                visit(attribute.getValue(), p);
            }
        }
        return attribute;
    }

    @Override
    public Javadoc visitAuthor(Javadoc.Author author, P p) {
        visitMarkers(author.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(author.getPrefix()).append("@author");
        visit(author.getName(), p);
        return author;
    }

    @Override
    public Javadoc visitDeprecated(Javadoc.Deprecated deprecated, P p) {
        visitMarkers(deprecated.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(deprecated.getPrefix()).append("@deprecated");
        visit(deprecated.getDescription(), p);
        return deprecated;
    }

    @Override
    public Javadoc visitDocComment(Javadoc.DocComment javadoc, P p) {
        visitMarkers(javadoc.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append("/**");
        visit(javadoc.getBody(), p);
        acc.append("*/");
        return javadoc;
    }

    @Override
    public Javadoc visitDocRoot(Javadoc.DocRoot docRoot, P p) {
        visitMarkers(docRoot.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(docRoot.getPrefix()).append("{@docRoot")
                .append(docRoot.getBeforeEndBrace()).append('}');
        return docRoot;
    }

    @Override
    public Javadoc visitDocType(Javadoc.DocType docType, P p) {
        visitMarkers(docType.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(docType.getPrefix()).append("<!doctype")
                .append(docType.getText()).append('>');
        return docType;
    }

    @Override
    public Javadoc visitEndElement(Javadoc.EndElement endElement, P p) {
        visitMarkers(endElement.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(endElement.getPrefix()).append("</").append(endElement.getName())
                .append(endElement.getBeforeEndBracket()).append('>');
        return endElement;
    }

    @Override
    public Javadoc visitErroneous(Javadoc.Erroneous erroneous, P p) {
        visitMarkers(erroneous.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(erroneous.getPrefix());
        visit(erroneous.getText(), p);
        return erroneous;
    }

    @Override
    public Javadoc visitHidden(Javadoc.Hidden hidden, P p) {
        visitMarkers(hidden.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(hidden.getPrefix()).append("@hidden");
        visit(hidden.getBody(), p);
        return hidden;
    }

    @Override
    public Javadoc visitIndex(Javadoc.Index index, P p) {
        visitMarkers(index.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(index.getPrefix()).append("{@index");
        visit(index.getSearchTerm(), p);
        visit(index.getDescription(), p);
        acc.append('}');
        return index;
    }

    @Override
    public Javadoc visitInheritDoc(Javadoc.InheritDoc inheritDoc, P p) {
        visitMarkers(inheritDoc.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(inheritDoc.getPrefix()).append("{@inheritDoc")
                .append(inheritDoc.getBeforeEndBrace()).append('}');
        return inheritDoc;
    }

    @Override
    public Javadoc visitInlinedValue(Javadoc.InlinedValue value, P p) {
        visitMarkers(value.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(value.getPrefix()).append("{@value");
        javaVisitor.visit(value.getTree(), p);
        acc.append(value.getBeforeEndBrace()).append('}');
        return value;
    }

    @Override
    public Javadoc visitLineBreak(Javadoc.LineBreak lineBreak, P p) {
        visitMarkers(lineBreak.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append('\n').append(lineBreak.getMargin());
        return lineBreak;
    }

    @Override
    public Javadoc visitLink(Javadoc.Link link, P p) {
        visitMarkers(link.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(link.getPrefix());
        acc.append(link.isPlain() ? "{@linkplain" : "{@link");
        javaVisitor.visit(link.getTree(), p);
        visit(link.getLabel(), p);
        acc.append(link.getBeforeEndBrace());
        return link;
    }

    @Override
    public Javadoc visitLiteral(Javadoc.Literal literal, P p) {
        visitMarkers(literal.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(literal.getPrefix()).append(literal.isCode() ? "{@code" : "{@literal");
        visit(literal.getDescription(), p);
        acc.append("}");
        return literal;
    }

    @Override
    public Javadoc visitParameter(Javadoc.Parameter parameter, P p) {
        visitMarkers(parameter.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(parameter.getPrefix()).append("@param");
        javaVisitor.visit(parameter.getName(), p);
        visit(parameter.getDescription(), p);
        return parameter;
    }

    @Override
    public Javadoc visitProvides(Javadoc.Provides provides, P p) {
        visitMarkers(provides.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(provides.getPrefix()).append("@provides");
        javaVisitor.visit(provides.getServiceType(), p);
        visit(provides.getDescription(), p);
        return provides;
    }

    @Override
    public Javadoc visitReturn(Javadoc.Return aReturn, P p) {
        visitMarkers(aReturn.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(aReturn.getPrefix()).append("@return");
        visit(aReturn.getDescription(), p);
        return aReturn;
    }

    @Override
    public Javadoc visitSee(Javadoc.See see, P p) {
        visitMarkers(see.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(see.getPrefix()).append("@see");
        javaVisitor.visit(see.getTree(), p);
        visit(see.getReference(), p);
        return see;
    }

    @Override
    public Javadoc visitSerial(Javadoc.Serial serial, P p) {
        visitMarkers(serial.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(serial.getPrefix()).append("@serial");
        visit(serial.getDescription(), p);
        return serial;
    }

    @Override
    public Javadoc visitSerialData(Javadoc.SerialData serialData, P p) {
        visitMarkers(serialData.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(serialData.getPrefix()).append("@serialData");
        visit(serialData.getDescription(), p);
        return serialData;
    }

    @Override
    public Javadoc visitSerialField(Javadoc.SerialField serialField, P p) {
        visitMarkers(serialField.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(serialField.getPrefix()).append("@serialField");
        javaVisitor.visit(serialField.getName(), p);
        javaVisitor.visit(serialField.getType(), p);
        visit(serialField.getDescription(), p);
        return serialField;
    }

    @Override
    public Javadoc visitSince(Javadoc.Since since, P p) {
        visitMarkers(since.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(since.getPrefix()).append("@since");
        visit(since.getDescription(), p);
        return since;
    }

    @Override
    public Javadoc visitStartElement(Javadoc.StartElement startElement, P p) {
        visitMarkers(startElement.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(startElement.getPrefix()).append('<').append(startElement.getName());
        visit(startElement.getAttributes(), p);
        acc.append(startElement.getBeforeEndBracket());
        if(startElement.isSelfClosing()) {
            acc.append('/');
        }
        acc.append('>');
        return startElement;
    }

    @Override
    public Javadoc visitSummary(Javadoc.Summary summary, P p) {
        visitMarkers(summary.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(summary.getPrefix()).append("{@summary");
        visit(summary.getSummary(), p);
        acc.append('}');
        return summary;
    }

    @Override
    public Javadoc visitText(Javadoc.Text text, P p) {
        visitMarkers(text.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(text.getText());
        return text;
    }

    @Override
    public Javadoc visitThrows(Javadoc.Throws aThrows, P p) {
        visitMarkers(aThrows.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(aThrows.getPrefix()).append(aThrows.isThrowsKeyword() ? "@throws" : "@exception");
        javaVisitor.visit(aThrows.getExceptionName(), p);
        visit(aThrows.getDescription(), p);
        return aThrows;
    }

    @Override
    public Javadoc visitUnknownBlock(Javadoc.UnknownBlock unknownBlock, P p) {
        visitMarkers(unknownBlock.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(unknownBlock.getPrefix()).append("@").append(unknownBlock.getName());
        visit(unknownBlock.getContent(), p);
        return unknownBlock;
    }

    @Override
    public Javadoc visitUnknownInline(Javadoc.UnknownInline unknownInline, P p) {
        visitMarkers(unknownInline.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(unknownInline.getPrefix()).append("{@").append(unknownInline.getName())
                .append(unknownInline.getBeforeEndBrace()).append('}');
        return unknownInline;
    }

    @Override
    public Javadoc visitUses(Javadoc.Uses uses, P p) {
        visitMarkers(uses.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(uses.getPrefix()).append("@uses");
        javaVisitor.visit(uses.getServiceType(), p);
        visit(uses.getDescription(), p);
        return uses;
    }

    @Override
    public Javadoc visitVersion(Javadoc.Version since, P p) {
        visitMarkers(since.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append(since.getPrefix()).append("@version");
        visit(since.getBody(), p);
        return since;
    }

    protected void visit(@Nullable List<? extends Javadoc> nodes, P p) {
        if (nodes != null) {
            for (Javadoc node : nodes) {
                visit(node, p);
            }
        }
    }

    class JavadocJavaPrinter extends JavaVisitor<P> {
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, P p) {
            visitMarkers(method.getMarkers(), p);
            visitSpace(method.getPrefix(), Space.Location.IDENTIFIER_PREFIX, p);
            StringBuilder acc = getPrinter();
            visit(method.getSelect(), p);
            acc.append('#').append(method.getSimpleName());
            visitContainer("(", method.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, ",", ")", p);
            return method;
        }

        @Override
        public J visitIdentifier(J.Identifier ident, P p) {
            visitMarkers(ident.getMarkers(), p);
            visitSpace(ident.getPrefix(), Space.Location.IDENTIFIER_PREFIX, p);
            StringBuilder acc = getPrinter();
            acc.append(ident.getSimpleName());
            return ident;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, P p) {
            visitSpace(fieldAccess.getPrefix(), Space.Location.FIELD_ACCESS_PREFIX, p);
            visitMarkers(fieldAccess.getMarkers(), p);
            visit(fieldAccess.getTarget(), p);
            visitLeftPadded(".", fieldAccess.getPadding().getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p);
            return fieldAccess;
        }

        @Override
        public J visitMemberReference(J.MemberReference memberRef, P p) {
            visitSpace(memberRef.getPrefix(), Space.Location.MEMBER_REFERENCE_PREFIX, p);
            visitMarkers(memberRef.getMarkers(), p);
            visit(memberRef.getContaining(), p);
            visitLeftPadded("#", memberRef.getPadding().getReference(), JLeftPadded.Location.MEMBER_REFERENCE_NAME, p);
            return memberRef;
        }

        @Override
        public J visitTypeParameter(J.TypeParameter typeParam, P p) {
            visitSpace(typeParam.getPrefix(), Space.Location.TYPE_PARAMETERS_PREFIX, p);
            visitMarkers(typeParam.getMarkers(), p);
            StringBuilder acc = getPrinter();
            acc.append("<");
            visit(typeParam.getName(), p);
            acc.append(">");
            return typeParam;
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, P p) {
            getPrinter().append(space.getWhitespace());
            return space;
        }

        private void visitLeftPadded(@Nullable String prefix, @Nullable JLeftPadded<? extends J> leftPadded, JLeftPadded.Location location, P p) {
            if (leftPadded != null) {
                StringBuilder acc = getPrinter();
                visitSpace(leftPadded.getBefore(), location.getBeforeLocation(), p);
                if (prefix != null) {
                    acc.append(prefix);
                }
                visit(leftPadded.getElement(), p);
            }
        }

        private void visitContainer(String before, @Nullable JContainer<? extends J> container, JContainer.Location location, String suffixBetween, @Nullable String after, P p) {
            if (container == null) {
                return;
            }
            StringBuilder acc = getPrinter();
            visitSpace(container.getBefore(), location.getBeforeLocation(), p);
            acc.append(before);
            visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
            acc.append(after == null ? "" : after);
        }

        private void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, JRightPadded.Location location, String suffixBetween, P p) {
            StringBuilder acc = getPrinter();
            for (int i = 0; i < nodes.size(); i++) {
                JRightPadded<? extends J> node = nodes.get(i);
                visit(node.getElement(), p);
                visitSpace(node.getAfter(), location.getAfterLocation(), p);
                if (i < nodes.size() - 1) {
                    acc.append(suffixBetween);
                }
            }
        }
    }
}
