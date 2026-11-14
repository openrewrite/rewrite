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
import {asRef, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {Xml} from "./tree";
import {updateIfChanged} from "../util";

/**
 * Dispatches an Xml element to the sender's or receiver's method for its kind. The fields of each kind
 * go over the wire in the order `org.openrewrite.xml.internal.rpc.XmlSender` writes them.
 */
abstract class XmlRpcVisitor<Q> {
    visit<R extends Xml>(tree: Xml | undefined, q: Q): R | undefined {
        if (!tree) {
            return undefined;
        }
        const t = this.preVisit(tree, q);
        return t === undefined ? undefined : this.accept(t, q) as R | undefined;
    }

    protected abstract preVisit(tree: Xml, q: Q): Xml | undefined;

    protected accept(t: Xml, q: Q): Xml | undefined {
        switch (t.kind) {
            case Xml.Kind.Document:
                return this.visitDocument(t as Xml.Document, q);
            case Xml.Kind.Prolog:
                return this.visitProlog(t as Xml.Prolog, q);
            case Xml.Kind.XmlDecl:
                return this.visitXmlDecl(t as Xml.XmlDecl, q);
            case Xml.Kind.ProcessingInstruction:
                return this.visitProcessingInstruction(t as Xml.ProcessingInstruction, q);
            case Xml.Kind.Tag:
                return this.visitTag(t as Xml.Tag, q);
            case Xml.Kind.TagClosing:
                return this.visitTagClosing(t as Xml.TagClosing, q);
            case Xml.Kind.Attribute:
                return this.visitAttribute(t as Xml.Attribute, q);
            case Xml.Kind.AttributeValue:
                return this.visitAttributeValue(t as Xml.AttributeValue, q);
            case Xml.Kind.CharData:
                return this.visitCharData(t as Xml.CharData, q);
            case Xml.Kind.Comment:
                return this.visitComment(t as Xml.Comment, q);
            case Xml.Kind.DocTypeDecl:
                return this.visitDocTypeDecl(t as Xml.DocTypeDecl, q);
            case Xml.Kind.DocTypeDeclExternalSubsets:
                return this.visitDocTypeDeclExternalSubsets(t as Xml.DocTypeDeclExternalSubsets, q);
            case Xml.Kind.Element:
                return this.visitElement(t as Xml.Element, q);
            case Xml.Kind.Ident:
                return this.visitIdent(t as Xml.Ident, q);
            case Xml.Kind.JspDirective:
                return this.visitJspDirective(t as Xml.JspDirective, q);
            case Xml.Kind.JspScriptlet:
            case Xml.Kind.JspExpression:
            case Xml.Kind.JspDeclaration:
            case Xml.Kind.JspComment:
                return this.visitJspContent(t as Xml.JspScriptlet | Xml.JspExpression | Xml.JspDeclaration | Xml.JspComment, q);
            default:
                throw new Error(`No RPC codec for kind ${t.kind}`);
        }
    }

    protected abstract visitDocument(tree: Xml.Document, q: Q): Xml | undefined;
    protected abstract visitProlog(tree: Xml.Prolog, q: Q): Xml | undefined;
    protected abstract visitXmlDecl(tree: Xml.XmlDecl, q: Q): Xml | undefined;
    protected abstract visitProcessingInstruction(tree: Xml.ProcessingInstruction, q: Q): Xml | undefined;
    protected abstract visitTag(tree: Xml.Tag, q: Q): Xml | undefined;
    protected abstract visitTagClosing(tree: Xml.TagClosing, q: Q): Xml | undefined;
    protected abstract visitAttribute(tree: Xml.Attribute, q: Q): Xml | undefined;
    protected abstract visitAttributeValue(tree: Xml.AttributeValue, q: Q): Xml | undefined;
    protected abstract visitCharData(tree: Xml.CharData, q: Q): Xml | undefined;
    protected abstract visitComment(tree: Xml.Comment, q: Q): Xml | undefined;
    protected abstract visitDocTypeDecl(tree: Xml.DocTypeDecl, q: Q): Xml | undefined;
    protected abstract visitDocTypeDeclExternalSubsets(tree: Xml.DocTypeDeclExternalSubsets, q: Q): Xml | undefined;
    protected abstract visitElement(tree: Xml.Element, q: Q): Xml | undefined;
    protected abstract visitIdent(tree: Xml.Ident, q: Q): Xml | undefined;
    protected abstract visitJspDirective(tree: Xml.JspDirective, q: Q): Xml | undefined;
    protected abstract visitJspContent(tree: Xml.JspScriptlet | Xml.JspExpression | Xml.JspDeclaration | Xml.JspComment, q: Q): Xml | undefined;
}

class XmlSender extends XmlRpcVisitor<RpcSendQueue> {

    protected preVisit(x: Xml, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(x, x2 => x2.id);
        q.getAndSend(x, x2 => x2.prefix);
        q.getAndSend(x, x2 => asRef(x2.markers));
        return x;
    }

    protected visitDocument(document: Xml.Document, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(document, d => d.sourcePath);
        q.getAndSend(document, d => d.charsetName);
        q.getAndSend(document, d => d.charsetBomMarked);
        q.getAndSend(document, d => d.checksum);
        q.getAndSend(document, d => d.fileAttributes);
        q.getAndSend(document, d => d.prolog, p => this.visit(p, q));
        q.getAndSend(document, d => d.root, t => this.visit(t, q));
        q.getAndSend(document, d => d.eof);
        return document;
    }

    protected visitProlog(prolog: Xml.Prolog, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(prolog, p => p.xmlDecl, d => this.visit(d, q));
        q.getAndSendList(prolog, p => p.misc, m => m.id, m => this.visit(m, q));
        q.getAndSendList(prolog, p => p.jspDirectives, j => j.id, j => this.visit(j, q));
        return prolog;
    }

    protected visitXmlDecl(xmlDecl: Xml.XmlDecl, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(xmlDecl, x => x.name);
        q.getAndSendList(xmlDecl, x => x.attributes, a => a.id, a => this.visit(a, q));
        q.getAndSend(xmlDecl, x => x.beforeTagDelimiterPrefix);
        return xmlDecl;
    }

    protected visitProcessingInstruction(pi: Xml.ProcessingInstruction, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(pi, p => p.name);
        q.getAndSend(pi, p => p.processingInstructions, c => this.visit(c, q));
        q.getAndSend(pi, p => p.beforeTagDelimiterPrefix);
        return pi;
    }

    protected visitTag(tag: Xml.Tag, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(tag, t => t.name);
        q.getAndSendList(tag, t => t.attributes, a => a.id, a => this.visit(a, q));
        q.getAndSendList(tag, t => t.content, c => c.id, c => this.visit(c, q));
        q.getAndSend(tag, t => t.closing, c => this.visit(c, q));
        q.getAndSend(tag, t => t.beforeTagDelimiterPrefix);
        return tag;
    }

    protected visitTagClosing(closing: Xml.TagClosing, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(closing, c => c.name);
        q.getAndSend(closing, c => c.beforeTagDelimiterPrefix);
        return closing;
    }

    protected visitAttribute(attribute: Xml.Attribute, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(attribute, a => a.key, k => this.visit(k, q));
        q.getAndSend(attribute, a => a.beforeEquals);
        q.getAndSend(attribute, a => a.value, v => this.visit(v, q));
        return attribute;
    }

    protected visitAttributeValue(value: Xml.AttributeValue, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(value, v => v.quote);
        q.getAndSend(value, v => v.value);
        return value;
    }

    protected visitCharData(charData: Xml.CharData, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(charData, c => c.cdata);
        q.getAndSend(charData, c => c.text);
        q.getAndSend(charData, c => c.afterText);
        return charData;
    }

    protected visitComment(comment: Xml.Comment, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(comment, c => c.text);
        return comment;
    }

    protected visitDocTypeDecl(docTypeDecl: Xml.DocTypeDecl, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(docTypeDecl, d => d.name, n => this.visit(n, q));
        q.getAndSend(docTypeDecl, d => d.documentDeclaration);
        q.getAndSend(docTypeDecl, d => d.externalId, e => this.visit(e, q));
        q.getAndSendList(docTypeDecl, d => d.internalSubset, i => i.id, i => this.visit(i, q));
        q.getAndSend(docTypeDecl, d => d.externalSubsets, e => this.visit(e, q));
        q.getAndSend(docTypeDecl, d => d.beforeTagDelimiterPrefix);
        return docTypeDecl;
    }

    protected visitDocTypeDeclExternalSubsets(externalSubsets: Xml.DocTypeDeclExternalSubsets, q: RpcSendQueue): Xml | undefined {
        q.getAndSendList(externalSubsets, e => e.elements, e => e.id, e => this.visit(e, q));
        return externalSubsets;
    }

    protected visitElement(element: Xml.Element, q: RpcSendQueue): Xml | undefined {
        q.getAndSendList(element, e => e.subset, i => i.id, i => this.visit(i, q));
        q.getAndSend(element, e => e.beforeTagDelimiterPrefix);
        return element;
    }

    protected visitIdent(ident: Xml.Ident, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(ident, i => i.name);
        return ident;
    }

    protected visitJspDirective(jspDirective: Xml.JspDirective, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(jspDirective, j => j.beforeTypePrefix);
        q.getAndSend(jspDirective, j => j.type);
        q.getAndSendList(jspDirective, j => j.attributes, a => a.id, a => this.visit(a, q));
        q.getAndSend(jspDirective, j => j.beforeDirectiveEndPrefix);
        return jspDirective;
    }

    protected visitJspContent(jsp: Xml.JspScriptlet | Xml.JspExpression | Xml.JspDeclaration | Xml.JspComment, q: RpcSendQueue): Xml | undefined {
        q.getAndSend(jsp, j => j.content);
        return jsp;
    }
}

// noinspection ES6MissingAwait
class XmlReceiver extends XmlRpcVisitor<RpcReceiveQueue> {

    protected preVisit(x: Xml, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(x, {
            id: q.receive(x.id),
            prefix: q.receive(x.prefix),
            markers: q.receive(x.markers),
        });
    }

    protected visitDocument(document: Xml.Document, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(document, {
            sourcePath: q.receive(document.sourcePath),
            charsetName: q.receive(document.charsetName),
            charsetBomMarked: q.receive(document.charsetBomMarked),
            checksum: q.receive(document.checksum),
            fileAttributes: q.receive(document.fileAttributes),
            prolog: q.receive(document.prolog, p => this.visit<Xml.Prolog>(p, q)!),
            root: q.receive(document.root, t => this.visit<Xml.Tag>(t, q)!),
            eof: q.receive(document.eof),
        });
    }

    protected visitProlog(prolog: Xml.Prolog, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(prolog, {
            xmlDecl: q.receive(prolog.xmlDecl, d => this.visit<Xml.XmlDecl>(d, q)),
            misc: q.receiveListDefined(prolog.misc, m => this.visit<Xml.Misc>(m, q)),
            jspDirectives: q.receiveListDefined(prolog.jspDirectives, j => this.visit<Xml.JspDirective>(j, q)),
        });
    }

    protected visitXmlDecl(xmlDecl: Xml.XmlDecl, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(xmlDecl, {
            name: q.receive(xmlDecl.name),
            attributes: q.receiveListDefined(xmlDecl.attributes, a => this.visit<Xml.Attribute>(a, q)),
            beforeTagDelimiterPrefix: q.receive(xmlDecl.beforeTagDelimiterPrefix),
        });
    }

    protected visitProcessingInstruction(pi: Xml.ProcessingInstruction, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(pi, {
            name: q.receive(pi.name),
            processingInstructions: q.receive(pi.processingInstructions, c => this.visit<Xml.CharData>(c, q)),
            beforeTagDelimiterPrefix: q.receive(pi.beforeTagDelimiterPrefix),
        });
    }

    protected visitTag(tag: Xml.Tag, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(tag, {
            name: q.receive(tag.name),
            attributes: q.receiveListDefined(tag.attributes, a => this.visit<Xml.Attribute>(a, q)),
            content: q.receiveList(tag.content, c => this.visit<Xml.Content>(c, q)),
            closing: q.receive(tag.closing, c => this.visit<Xml.TagClosing>(c, q)),
            beforeTagDelimiterPrefix: q.receive(tag.beforeTagDelimiterPrefix),
        });
    }

    protected visitTagClosing(closing: Xml.TagClosing, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(closing, {
            name: q.receive(closing.name),
            beforeTagDelimiterPrefix: q.receive(closing.beforeTagDelimiterPrefix),
        });
    }

    protected visitAttribute(attribute: Xml.Attribute, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(attribute, {
            key: q.receive(attribute.key, k => this.visit<Xml.Ident>(k, q)!),
            beforeEquals: q.receive(attribute.beforeEquals),
            value: q.receive(attribute.value, v => this.visit<Xml.AttributeValue>(v, q)!),
        });
    }

    protected visitAttributeValue(value: Xml.AttributeValue, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(value, {
            quote: q.receive(value.quote),
            value: q.receive(value.value),
        });
    }

    protected visitCharData(charData: Xml.CharData, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(charData, {
            cdata: q.receive(charData.cdata),
            text: q.receive(charData.text),
            afterText: q.receive(charData.afterText),
        });
    }

    protected visitComment(comment: Xml.Comment, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(comment, {
            text: q.receive(comment.text),
        });
    }

    protected visitDocTypeDecl(docTypeDecl: Xml.DocTypeDecl, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(docTypeDecl, {
            name: q.receive(docTypeDecl.name, n => this.visit<Xml.Ident>(n, q)!),
            documentDeclaration: q.receive(docTypeDecl.documentDeclaration),
            externalId: q.receive(docTypeDecl.externalId, e => this.visit<Xml.Ident>(e, q)),
            internalSubset: q.receiveListDefined(docTypeDecl.internalSubset, i => this.visit<Xml.Ident>(i, q)),
            externalSubsets: q.receive(docTypeDecl.externalSubsets, e => this.visit<Xml.DocTypeDeclExternalSubsets>(e, q)),
            beforeTagDelimiterPrefix: q.receive(docTypeDecl.beforeTagDelimiterPrefix),
        });
    }

    protected visitDocTypeDeclExternalSubsets(externalSubsets: Xml.DocTypeDeclExternalSubsets, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(externalSubsets, {
            elements: q.receiveListDefined(externalSubsets.elements, e => this.visit<Xml.Element>(e, q)),
        });
    }

    protected visitElement(element: Xml.Element, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(element, {
            subset: q.receiveListDefined(element.subset, i => this.visit<Xml.Ident>(i, q)),
            beforeTagDelimiterPrefix: q.receive(element.beforeTagDelimiterPrefix),
        });
    }

    protected visitIdent(ident: Xml.Ident, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(ident, {
            name: q.receive(ident.name),
        });
    }

    protected visitJspDirective(jspDirective: Xml.JspDirective, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(jspDirective, {
            beforeTypePrefix: q.receive(jspDirective.beforeTypePrefix),
            type: q.receive(jspDirective.type),
            attributes: q.receiveListDefined(jspDirective.attributes, a => this.visit<Xml.Attribute>(a, q)),
            beforeDirectiveEndPrefix: q.receive(jspDirective.beforeDirectiveEndPrefix),
        });
    }

    protected visitJspContent(jsp: Xml.JspScriptlet | Xml.JspExpression | Xml.JspDeclaration | Xml.JspComment, q: RpcReceiveQueue): Xml | undefined {
        return updateIfChanged(jsp, {
            content: q.receive(jsp.content),
        });
    }
}

const receiver = new XmlReceiver();
const sender = new XmlSender();

for (const kind of Object.values(Xml.Kind)) {
    RpcCodecs.registerCodec(kind, {
        rpcReceive(before: Xml, q: RpcReceiveQueue): Xml {
            return receiver.visit(before, q)!;
        },

        rpcSend(after: Xml, q: RpcSendQueue): void {
            sender.visit(after, q);
        }
    }, Xml.Kind.Document);
}
