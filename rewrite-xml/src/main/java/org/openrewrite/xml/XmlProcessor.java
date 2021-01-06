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

import org.openrewrite.TreeProcessor;
import org.openrewrite.xml.tree.Xml;

public class XmlProcessor<P> extends TreeProcessor<Xml, P> implements XmlVisitor<Xml, P> {

    @Override
    public Xml visitDocument(Xml.Document document, P p) {
        Xml.Document d = call(document, p, this::visitEach);
        d = d.withProlog(call(d.getProlog(), p));
        return d.withRoot(call(d.getRoot(), p));
    }

    @Override
    public Xml visitProcessingInstruction(Xml.ProcessingInstruction pi, P p) {
        Xml.ProcessingInstruction procInstr = call(pi, p, this::visitEach);
        return procInstr.withAttributes(call(procInstr.getAttributes(), p));
    }

    @Override
    public Xml visitTag(Xml.Tag tag, P p) {
        Xml.Tag t = call(tag, p, this::visitEach);
        t = t.withAttributes(call(t.getAttributes(), p));
        return t.withContent(call(t.getContent(), p));
    }

    @Override
    public Xml visitAttribute(Xml.Attribute attribute, P p) {
        return call(attribute, p, this::visitEach);
    }

    @Override
    public Xml visitCharData(Xml.CharData charData, P p) {
        return call(charData, p, this::visitEach);
    }

    @Override
    public Xml visitComment(Xml.Comment comment, P p) {
        return call(comment, p, this::visitEach);
    }

    @Override
    public Xml visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, P p) {
        Xml.DocTypeDecl d = call(docTypeDecl, p, this::visitEach);
        d = d.withInternalSubset(call(d.getInternalSubset(), p));
        return d.withExternalSubsets(call(d.getExternalSubsets(), p));
    }

    @Override
    public Xml visitProlog(Xml.Prolog prolog, P p) {
        Xml.Prolog pl = call(prolog, p, this::visitEach);
        pl = pl.withXmlDecls(call(pl.getXmlDecls(), p));
        return pl.withMisc(call(pl.getMisc(), p));
    }

    @Override
    public Xml visitIdent(Xml.Ident ident, P p) {
        return call(ident, p, this::visitEach);
    }

    @Override
    public Xml visitElement(Xml.Element element, P p) {
        Xml.Element e = call(element, p, this::visitEach);
        return e.withSubset(call(e.getSubset(), p));
    }
}
