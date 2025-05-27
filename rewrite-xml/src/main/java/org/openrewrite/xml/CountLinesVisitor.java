/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.Tree;
import org.openrewrite.xml.tree.Xml;

import java.util.concurrent.atomic.AtomicInteger;

public class CountLinesVisitor extends XmlVisitor<AtomicInteger> {

    @Override
    public Xml visitDocument(Xml.Document document, AtomicInteger count) {
        count.incrementAndGet();
        return super.visitDocument(document, count);
    }

    @Override
    public Xml visitXmlDecl(Xml.XmlDecl xmlDecl, AtomicInteger count) {
        if(xmlDecl.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitXmlDecl(xmlDecl, count);
    }

    @Override
    public Xml visitProcessingInstruction(Xml.ProcessingInstruction processingInstruction, AtomicInteger count) {
        if(processingInstruction.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitProcessingInstruction(processingInstruction, count);
    }


    @Override
    public Xml visitElement(Xml.Element element, AtomicInteger count) {
        if(element.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitElement(element, count);
    }

    @Override
    public Xml visitAttribute(Xml.Attribute attribute, AtomicInteger count) {
        if(attribute.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitAttribute(attribute, count);
    }

    @Override
    public Xml visitTag(Xml.Tag tag, AtomicInteger count) {
        if(tag.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }

        return super.visitTag(tag, count);
    }

    @Override
    public Xml visitTagClosing(Xml.Tag.Closing closing, AtomicInteger count) {
        if (closing.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }

        return super.visitTagClosing(closing, count);
    }

    @Override
    public Xml visitComment(Xml.Comment comment, AtomicInteger count) {
        if(comment.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitComment(comment, count);
    }

    @Override
    public Xml visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, AtomicInteger count) {
        if(docTypeDecl.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitDocTypeDecl(docTypeDecl, count);
    }

    @Override
    public Xml visitProlog(Xml.Prolog prolog, AtomicInteger count) {
        if(prolog.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitProlog(prolog, count);
    }

    @Override
    public Xml visitIdent(Xml.Ident ident, AtomicInteger count) {
        if(ident.getPrefix().contains("\n")) {
            count.incrementAndGet();
        }
        return super.visitIdent(ident, count);
    }

    public static int countLines(Tree tree) {
        return new CountLinesVisitor().reduce(tree, new AtomicInteger()).get();
    }
}
