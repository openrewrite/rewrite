/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {PrintOutputCapture, TreePrinters} from "../print";
import {Cursor} from "../tree";
import {XmlVisitor} from "./visitor";
import {Xml} from "./tree";

class XmlPrinter extends XmlVisitor<PrintOutputCapture> {

    protected async visitDocument(document: Xml.Document, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(document, p);
        await this.visit(document.prolog, p);
        if (document.root) {
            await this.visit(document.root, p);
        }
        this.afterSyntax(document, p);
        p.append(document.eof);
        return document;
    }

    protected async visitProlog(prolog: Xml.Prolog, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(prolog, p);
        if (prolog.xmlDecl) {
            await this.visit(prolog.xmlDecl, p);
        }
        await this.visitAll(prolog.misc, p);
        await this.visitAll(prolog.jspDirectives, p);
        this.afterSyntax(prolog, p);
        return prolog;
    }

    protected async visitXmlDecl(xmlDecl: Xml.XmlDecl, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(xmlDecl, p);
        p.append("<?").append(xmlDecl.name);
        await this.visitAll(xmlDecl.attributes, p);
        p.append(xmlDecl.beforeTagDelimiterPrefix).append("?>");
        this.afterSyntax(xmlDecl, p);
        return xmlDecl;
    }

    protected async visitTag(tag: Xml.Tag, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(tag, p);
        p.append("<").append(tag.name);
        await this.visitAll(tag.attributes, p);
        p.append(tag.beforeTagDelimiterPrefix);
        if (tag.markers.markers.some(m => m.kind === Xml.MarkersKind.HtmlVoidElement)) {
            p.append(">");
        } else if (!tag.closing) {
            p.append("/>");
        } else {
            p.append(">");
            await this.visitAll(tag.content ?? [], p);
            await this.visit(tag.closing, p);
        }
        this.afterSyntax(tag, p);
        return tag;
    }

    protected async visitTagClosing(closing: Xml.TagClosing, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(closing, p);
        p.append("</").append(closing.name).append(closing.beforeTagDelimiterPrefix).append(">");
        this.afterSyntax(closing, p);
        return closing;
    }

    protected async visitAttribute(attribute: Xml.Attribute, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(attribute, p);
        p.append(attribute.key.prefix)
            .append(attribute.key.name)
            .append(attribute.beforeEquals)
            .append("=");
        await this.visit(attribute.value, p);
        this.afterSyntax(attribute, p);
        return attribute;
    }

    protected async visitAttributeValue(value: Xml.AttributeValue, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(value, p);
        const delimiter = value.quote === "Double" ? '"' : "'";
        p.append(delimiter).append(value.value).append(delimiter);
        this.afterSyntax(value, p);
        return value;
    }

    protected async visitComment(comment: Xml.Comment, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(comment, p);
        p.append("<!--").append(comment.text).append("-->");
        this.afterSyntax(comment, p);
        return comment;
    }

    protected async visitProcessingInstruction(pi: Xml.ProcessingInstruction, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(pi, p);
        p.append("<?").append(pi.name);
        if (pi.processingInstructions) {
            await this.visit(pi.processingInstructions, p);
        }
        p.append(pi.beforeTagDelimiterPrefix).append("?>");
        this.afterSyntax(pi, p);
        return pi;
    }

    protected async visitCharData(charData: Xml.CharData, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(charData, p);
        if (charData.cdata) {
            p.append("<![CDATA[").append(charData.text).append("]]>");
        } else {
            p.append(charData.text);
        }
        p.append(charData.afterText);
        this.afterSyntax(charData, p);
        return charData;
    }

    protected async visitDocTypeDecl(docTypeDecl: Xml.DocTypeDecl, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(docTypeDecl, p);
        p.append("<!").append(docTypeDecl.documentDeclaration);
        await this.visit(docTypeDecl.name, p);
        if (docTypeDecl.externalId) {
            await this.visit(docTypeDecl.externalId, p);
        }
        await this.visitAll(docTypeDecl.internalSubset, p);
        if (docTypeDecl.externalSubsets) {
            await this.visit(docTypeDecl.externalSubsets, p);
        }
        p.append(docTypeDecl.beforeTagDelimiterPrefix).append(">");
        this.afterSyntax(docTypeDecl, p);
        return docTypeDecl;
    }

    protected async visitDocTypeDeclExternalSubsets(externalSubsets: Xml.DocTypeDeclExternalSubsets, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(externalSubsets, p);
        p.append("[");
        await this.visitAll(externalSubsets.elements, p);
        p.append("]");
        this.afterSyntax(externalSubsets, p);
        return externalSubsets;
    }

    protected async visitElement(element: Xml.Element, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(element, p);
        await this.visitAll(element.subset, p);
        p.append(element.beforeTagDelimiterPrefix);
        this.afterSyntax(element, p);
        return element;
    }

    protected async visitIdent(ident: Xml.Ident, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(ident, p);
        p.append(ident.name);
        this.afterSyntax(ident, p);
        return ident;
    }

    protected async visitJspDirective(jsp: Xml.JspDirective, p: PrintOutputCapture): Promise<Xml | undefined> {
        this.beforeSyntax(jsp, p);
        p.append("<%@").append(jsp.beforeTypePrefix).append(jsp.type);
        await this.visitAll(jsp.attributes, p);
        p.append(jsp.beforeDirectiveEndPrefix).append("%>");
        this.afterSyntax(jsp, p);
        return jsp;
    }

    protected async visitJspScriptlet(jsp: Xml.JspScriptlet, p: PrintOutputCapture): Promise<Xml | undefined> {
        return this.printDelimited(jsp, "<%", jsp.content, "%>", p);
    }

    protected async visitJspExpression(jsp: Xml.JspExpression, p: PrintOutputCapture): Promise<Xml | undefined> {
        return this.printDelimited(jsp, "<%=", jsp.content, "%>", p);
    }

    protected async visitJspDeclaration(jsp: Xml.JspDeclaration, p: PrintOutputCapture): Promise<Xml | undefined> {
        return this.printDelimited(jsp, "<%!", jsp.content, "%>", p);
    }

    protected async visitJspComment(jsp: Xml.JspComment, p: PrintOutputCapture): Promise<Xml | undefined> {
        return this.printDelimited(jsp, "<%--", jsp.content, "--%>", p);
    }

    private printDelimited(x: Xml, open: string, content: string, close: string, p: PrintOutputCapture): Xml {
        this.beforeSyntax(x, p);
        p.append(open).append(content).append(close);
        this.afterSyntax(x, p);
        return x;
    }

    private async visitAll(nodes: Xml[], p: PrintOutputCapture): Promise<void> {
        for (const node of nodes) {
            await this.visit(node, p);
        }
    }

    private beforeSyntax(x: Xml, p: PrintOutputCapture): void {
        for (const marker of x.markers.markers) {
            p.append(p.markerPrinter.beforePrefix(marker, new Cursor(marker, this.cursor), this.xmlMarkerWrapper));
        }
        p.append(x.prefix);
        for (const marker of x.markers.markers) {
            p.append(p.markerPrinter.beforeSyntax(marker, new Cursor(marker, this.cursor), this.xmlMarkerWrapper));
        }
    }

    private afterSyntax(x: Xml, p: PrintOutputCapture): void {
        for (const marker of x.markers.markers) {
            p.append(p.markerPrinter.afterSyntax(marker, new Cursor(marker, this.cursor), this.xmlMarkerWrapper));
        }
    }

    private xmlMarkerWrapper = (out: string): string => `<!--~~${out}${out ? "~~" : ""}>-->`;
}

TreePrinters.register(Xml.Kind.Document, () => new XmlPrinter());
