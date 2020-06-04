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
package org.openrewrite.xml.refactor;

import org.openrewrite.RefactorVisitorSupport;
import org.openrewrite.Tree;
import org.openrewrite.refactor.Formatter;
import org.openrewrite.xml.XmlSourceVisitor;
import org.openrewrite.xml.tree.Xml;

public abstract class XmlRefactorVisitor extends XmlSourceVisitor<Xml> implements RefactorVisitorSupport {
    protected Formatter formatter;

    @Override
    public Xml defaultTo(Tree t) {
        return (Xml) t;
    }

    @Override
    public Xml visitDocument(Xml.Document document) {
        formatter = new Formatter(document);
        Xml.Document d = refactor(document, super::visitDocument);
        d = d.withProlog(refactor(d.getProlog()));
        return d.withRoot(refactor(d.getRoot()));
    }

    @Override
    public Xml visitProlog(Xml.Prolog prolog) {
        Xml.Prolog p = refactor(prolog, super::visitProlog);
        p = p.withXmlDecl(refactor(p.getXmlDecl()));
        return p.withMisc(refactor(p.getMisc()));
    }

    @Override
    public Xml visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl) {
        Xml.DocTypeDecl d = refactor(docTypeDecl, super::visitDocTypeDecl);
        d = d.withName(refactor(d.getName()));
        d = d.withExternalId(refactor(d.getExternalId()));
        d = d.withExternalSubsets(refactor(d.getExternalSubsets()));
        return d.withInternalSubset(refactor(d.getInternalSubset()));
    }

    @Override
    public Xml visitElement(Xml.Element element) {
        Xml.Element e = refactor(element, super::visitElement);
        return e.withSubset(e.getSubset());
    }

    @Override
    public Xml visitTag(Xml.Tag tag) {
        Xml.Tag t = refactor(tag, super::visitTag);
        t = t.withAttributes(refactor(t.getAttributes()));
        t = t.withContent(refactor(t.getContent()));
        return t.withClosing(refactor(t.getClosing()));
    }

    @Override
    public Xml visitAttribute(Xml.Attribute attribute) {
        Xml.Attribute a = refactor(attribute, super::visitAttribute);
        a = a.withKey(refactor(a.getKey()));
        return a.withValue(refactor(a.getValue()));
    }

    @Override
    public Xml visitCharData(Xml.CharData charData) {
        return refactor(charData, super::visitCharData);
    }

    @Override
    public Xml visitProcessingInstruction(Xml.ProcessingInstruction pi) {
        Xml.ProcessingInstruction p = refactor(pi, super::visitProcessingInstruction);
        return p.withAttributes(refactor(p.getAttributes()));
    }

    @Override
    public Xml visitComment(Xml.Comment comment) {
        return refactor(comment, super::visitComment);
    }

    @Override
    public Xml visitIdent(Xml.Ident ident) {
        return refactor(ident, super::visitIdent);
    }

    public Xml.Tag enclosingTag() {
        return getCursor().firstEnclosing(Xml.Tag.class);
    }

    public Xml.Tag enclosingRootTag() {
        return getCursor().getPathAsStream()
                .filter(t -> t instanceof Xml.Tag)
                .map(Xml.Tag.class::cast)
                .reduce((t1, t2) -> t2)
                .orElseThrow(() -> new IllegalStateException("No root tag. This operation should be called from a cursor scope that is inside of the root tag."));
    }
}
