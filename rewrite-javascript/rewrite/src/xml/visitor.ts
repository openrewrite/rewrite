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
import {mapAsync, updateIfChanged} from "../util";
import {TreeVisitor, ValidRecipeReturnType} from "../visitor";
import {SourceFile} from "../tree";
import {isXml, Xml} from "./tree";
import {create, Draft} from "mutative";

/**
 * Visits an XML LST, mirroring `org.openrewrite.xml.XmlVisitor`.
 */
export class XmlVisitor<P> extends TreeVisitor<Xml, P> {
    async isAcceptable(sourceFile: SourceFile): Promise<boolean> {
        return isXml(sourceFile);
    }

    protected async visitDocument(document: Xml.Document, p: P): Promise<Xml | undefined> {
        return updateIfChanged(document, {
            markers: await this.visitMarkers(document.markers, p),
            prolog: await this.visitDefined<Xml.Prolog>(document.prolog, p),
            root: document.root && await this.visitDefined<Xml.Tag>(document.root, p)
        });
    }

    protected async visitProlog(prolog: Xml.Prolog, p: P): Promise<Xml | undefined> {
        return updateIfChanged(prolog, {
            markers: await this.visitMarkers(prolog.markers, p),
            xmlDecl: prolog.xmlDecl && await this.visit<Xml.XmlDecl>(prolog.xmlDecl, p),
            misc: await mapAsync(prolog.misc, m => this.visit<Xml.Misc>(m, p)),
            jspDirectives: await mapAsync(prolog.jspDirectives, j => this.visit<Xml.JspDirective>(j, p))
        });
    }

    protected async visitXmlDecl(xmlDecl: Xml.XmlDecl, p: P): Promise<Xml | undefined> {
        return updateIfChanged(xmlDecl, {
            markers: await this.visitMarkers(xmlDecl.markers, p),
            attributes: await mapAsync(xmlDecl.attributes, a => this.visit<Xml.Attribute>(a, p))
        });
    }

    protected async visitProcessingInstruction(pi: Xml.ProcessingInstruction, p: P): Promise<Xml | undefined> {
        return updateIfChanged(pi, {
            markers: await this.visitMarkers(pi.markers, p),
            processingInstructions: pi.processingInstructions &&
                await this.visit<Xml.CharData>(pi.processingInstructions, p)
        });
    }

    protected async visitTag(tag: Xml.Tag, p: P): Promise<Xml | undefined> {
        return updateIfChanged(tag, {
            markers: await this.visitMarkers(tag.markers, p),
            attributes: await mapAsync(tag.attributes, a => this.visit<Xml.Attribute>(a, p)),
            content: tag.content && await mapAsync(tag.content, c => this.visit<Xml.Content>(c, p)),
            closing: tag.closing && await this.visit<Xml.TagClosing>(tag.closing, p)
        });
    }

    protected async visitTagClosing(closing: Xml.TagClosing, p: P): Promise<Xml | undefined> {
        return updateIfChanged(closing, {
            markers: await this.visitMarkers(closing.markers, p)
        });
    }

    protected async visitAttribute(attribute: Xml.Attribute, p: P): Promise<Xml | undefined> {
        return updateIfChanged(attribute, {
            markers: await this.visitMarkers(attribute.markers, p),
            value: await this.visitDefined<Xml.AttributeValue>(attribute.value, p)
        });
    }

    protected async visitAttributeValue(value: Xml.AttributeValue, p: P): Promise<Xml | undefined> {
        return updateIfChanged(value, {
            markers: await this.visitMarkers(value.markers, p)
        });
    }

    protected async visitCharData(charData: Xml.CharData, p: P): Promise<Xml | undefined> {
        return updateIfChanged(charData, {
            markers: await this.visitMarkers(charData.markers, p)
        });
    }

    protected async visitComment(comment: Xml.Comment, p: P): Promise<Xml | undefined> {
        return updateIfChanged(comment, {
            markers: await this.visitMarkers(comment.markers, p)
        });
    }

    protected async visitDocTypeDecl(docTypeDecl: Xml.DocTypeDecl, p: P): Promise<Xml | undefined> {
        return updateIfChanged(docTypeDecl, {
            markers: await this.visitMarkers(docTypeDecl.markers, p),
            internalSubset: await mapAsync(docTypeDecl.internalSubset, i => this.visit<Xml.Ident>(i, p)),
            externalSubsets: docTypeDecl.externalSubsets &&
                await this.visit<Xml.DocTypeDeclExternalSubsets>(docTypeDecl.externalSubsets, p)
        });
    }

    protected async visitDocTypeDeclExternalSubsets(externalSubsets: Xml.DocTypeDeclExternalSubsets, p: P): Promise<Xml | undefined> {
        return updateIfChanged(externalSubsets, {
            markers: await this.visitMarkers(externalSubsets.markers, p),
            elements: await mapAsync(externalSubsets.elements, e => this.visit<Xml.Element>(e, p))
        });
    }

    protected async visitElement(element: Xml.Element, p: P): Promise<Xml | undefined> {
        return updateIfChanged(element, {
            markers: await this.visitMarkers(element.markers, p),
            subset: await mapAsync(element.subset, i => this.visit<Xml.Ident>(i, p))
        });
    }

    protected async visitIdent(ident: Xml.Ident, p: P): Promise<Xml | undefined> {
        return updateIfChanged(ident, {
            markers: await this.visitMarkers(ident.markers, p)
        });
    }

    protected async visitJspDirective(jspDirective: Xml.JspDirective, p: P): Promise<Xml | undefined> {
        return updateIfChanged(jspDirective, {
            markers: await this.visitMarkers(jspDirective.markers, p),
            attributes: await mapAsync(jspDirective.attributes, a => this.visit<Xml.Attribute>(a, p))
        });
    }

    protected async visitJspScriptlet(jspScriptlet: Xml.JspScriptlet, p: P): Promise<Xml | undefined> {
        return updateIfChanged(jspScriptlet, {
            markers: await this.visitMarkers(jspScriptlet.markers, p)
        });
    }

    protected async visitJspExpression(jspExpression: Xml.JspExpression, p: P): Promise<Xml | undefined> {
        return updateIfChanged(jspExpression, {
            markers: await this.visitMarkers(jspExpression.markers, p)
        });
    }

    protected async visitJspDeclaration(jspDeclaration: Xml.JspDeclaration, p: P): Promise<Xml | undefined> {
        return updateIfChanged(jspDeclaration, {
            markers: await this.visitMarkers(jspDeclaration.markers, p)
        });
    }

    protected async visitJspComment(jspComment: Xml.JspComment, p: P): Promise<Xml | undefined> {
        return updateIfChanged(jspComment, {
            markers: await this.visitMarkers(jspComment.markers, p)
        });
    }

    /**
     * Edits an element as a draft, with its markers visited first.
     */
    protected async produceXml<X extends Xml>(
        before: Xml | undefined,
        p: P,
        recipe?: (draft: Draft<X>) =>
            ValidRecipeReturnType<Draft<X>> |
            PromiseLike<ValidRecipeReturnType<Draft<X>>>
    ): Promise<X | undefined> {
        if (before === undefined) {
            return undefined;
        }
        const [draft, finishDraft] = create(before as X);
        (draft as Draft<Xml>).markers = await this.visitMarkers(before.markers, p);
        if (recipe) {
            await recipe(draft);
        }
        return finishDraft() as X;
    }

    protected accept(t: Xml, p: P): Promise<Xml | undefined> {
        switch (t.kind) {
            case Xml.Kind.Document:
                return this.visitDocument(t as Xml.Document, p);
            case Xml.Kind.Prolog:
                return this.visitProlog(t as Xml.Prolog, p);
            case Xml.Kind.XmlDecl:
                return this.visitXmlDecl(t as Xml.XmlDecl, p);
            case Xml.Kind.ProcessingInstruction:
                return this.visitProcessingInstruction(t as Xml.ProcessingInstruction, p);
            case Xml.Kind.Tag:
                return this.visitTag(t as Xml.Tag, p);
            case Xml.Kind.TagClosing:
                return this.visitTagClosing(t as Xml.TagClosing, p);
            case Xml.Kind.Attribute:
                return this.visitAttribute(t as Xml.Attribute, p);
            case Xml.Kind.AttributeValue:
                return this.visitAttributeValue(t as Xml.AttributeValue, p);
            case Xml.Kind.CharData:
                return this.visitCharData(t as Xml.CharData, p);
            case Xml.Kind.Comment:
                return this.visitComment(t as Xml.Comment, p);
            case Xml.Kind.DocTypeDecl:
                return this.visitDocTypeDecl(t as Xml.DocTypeDecl, p);
            case Xml.Kind.DocTypeDeclExternalSubsets:
                return this.visitDocTypeDeclExternalSubsets(t as Xml.DocTypeDeclExternalSubsets, p);
            case Xml.Kind.Element:
                return this.visitElement(t as Xml.Element, p);
            case Xml.Kind.Ident:
                return this.visitIdent(t as Xml.Ident, p);
            case Xml.Kind.JspDirective:
                return this.visitJspDirective(t as Xml.JspDirective, p);
            case Xml.Kind.JspScriptlet:
                return this.visitJspScriptlet(t as Xml.JspScriptlet, p);
            case Xml.Kind.JspExpression:
                return this.visitJspExpression(t as Xml.JspExpression, p);
            case Xml.Kind.JspDeclaration:
                return this.visitJspDeclaration(t as Xml.JspDeclaration, p);
            case Xml.Kind.JspComment:
                return this.visitJspComment(t as Xml.JspComment, p);
            default:
                throw new Error(`Unexpected XML kind ${t.kind}`);
        }
    }
}
