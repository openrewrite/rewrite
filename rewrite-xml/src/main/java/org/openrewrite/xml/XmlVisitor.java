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
package org.openrewrite.xml;

import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.xml.tree.Xml;

public class XmlVisitor<P> extends TreeVisitor<Xml, P> {

    @Override
    public String getLanguage() {
        return "xml";
    }

    public Xml visitDocument(Xml.Document document, P p) {
        Xml.Document d = document;
        d = d.withProlog(visitAndCast(d.getProlog(), p));
        return d.withRoot(visitAndCast(d.getRoot(), p));
    }

    public Xml visitProcessingInstruction(Xml.ProcessingInstruction pi, P p) {
        return pi.withAttributes(ListUtils.map(pi.getAttributes(), a -> visitAndCast(a, p)));
    }

    public Xml visitTag(Xml.Tag tag, P p) {
        Xml.Tag t = tag;
        t = t.withAttributes(ListUtils.map(t.getAttributes(), a -> visitAndCast(a, p)));
        if(t.getContent() != null) {
            t = t.withContent(ListUtils.map(t.getContent(), c -> visitAndCast(c, p)));
        }
        return t.withClosing(visitAndCast(t.getClosing(), p));
    }

    public Xml visitAttribute(Xml.Attribute attribute, P p) {
        return attribute;
    }

    public Xml visitCharData(Xml.CharData charData, P p) {
        return charData;
    }

    public Xml visitComment(Xml.Comment comment, P p) {
        return comment;
    }

    public Xml visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, P p) {
        Xml.DocTypeDecl d = docTypeDecl;
        d = d.withInternalSubset(ListUtils.map(d.getInternalSubset(), i -> visitAndCast(i, p)));
        return d.withExternalSubsets(visitAndCast(d.getExternalSubsets(), p));
    }

    public Xml visitProlog(Xml.Prolog prolog, P p) {
        Xml.Prolog pl = prolog;
        pl = pl.withXmlDecls(ListUtils.map(pl.getXmlDecls(), d -> visitAndCast(d, p)));
        return pl.withMisc(ListUtils.map(pl.getMisc(), m -> visitAndCast(m, p)));
    }

    public Xml visitIdent(Xml.Ident ident, P p) {
        return ident;
    }

    public Xml visitElement(Xml.Element element, P p) {
        return element.withSubset(ListUtils.map(element.getSubset(), i -> visitAndCast(i, p)));
    }
}
