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

import org.openrewrite.AbstractSourceVisitor;
import org.openrewrite.xml.tree.Xml;

public abstract class AbstractXmlSourceVisitor<R> extends AbstractSourceVisitor<R>
        implements XmlSourceVisitor<R> {

    public Xml.Tag enclosingTag() {
        return getCursor().firstEnclosing(Xml.Tag.class);
    }

    public R visitDocument(Xml.Document document) {
        return reduce(
                defaultTo(document),
                reduce(
                        visit(document.getProlog()),
                        visit(document.getRoot())
                )
        );
    }

    public R visitProcessingInstruction(Xml.ProcessingInstruction pi) {
        return reduce(
                defaultTo(pi),
                visit(pi.getAttributes())
        );
    }

    public R visitTag(Xml.Tag tag) {
        return reduce(
                defaultTo(tag),
                reduce(
                        visit(tag.getAttributes()),
                        visit(tag.getContent())
                )
        );
    }

    public R visitAttribute(Xml.Attribute attribute) {
        return defaultTo(attribute);
    }

    public R visitCharData(Xml.CharData charData) {
        return defaultTo(charData);
    }

    public R visitComment(Xml.Comment comment) {
        return defaultTo(comment);
    }

    public R visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl) {
        return reduce(
                defaultTo(docTypeDecl),
                reduce(
                        visit(docTypeDecl.getInternalSubset()),
                        visit(docTypeDecl.getExternalSubsets())
                )
        );
    }

    public R visitProlog(Xml.Prolog prolog) {
        return reduce(
                defaultTo(prolog),
                reduce(
                        visit(prolog.getXmlDecls()),
                        visit(prolog.getMisc())
                )
        );
    }

    public R visitIdent(Xml.Ident ident) {
        return defaultTo(ident);
    }

    public R visitElement(Xml.DocTypeDecl.Element element) {
        return reduce(
                defaultTo(element),
                visit(element.getSubset())
        );
    }
}
