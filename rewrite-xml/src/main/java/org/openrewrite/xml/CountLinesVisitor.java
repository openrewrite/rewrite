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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.xml.tree.Xml;

import java.util.concurrent.atomic.AtomicInteger;

public class CountLinesVisitor extends XmlVisitor<AtomicInteger> {

    @Override
    public Xml visitDocument(Xml.Document document, AtomicInteger count) {
        count.incrementAndGet();
        countLines(document.getPrefix(), count);
        return super.visitDocument(document, count);
    }

    @Override
    public Xml visitXmlDecl(Xml.XmlDecl xmlDecl, AtomicInteger count) {
        countLines(xmlDecl.getPrefix(), count);
        return super.visitXmlDecl(xmlDecl, count);
    }

    @Override
    public Xml visitProcessingInstruction(Xml.ProcessingInstruction processingInstruction, AtomicInteger count) {
        countLines(processingInstruction.getPrefix(), count);
        return super.visitProcessingInstruction(processingInstruction, count);
    }


    @Override
    public Xml visitElement(Xml.Element element, AtomicInteger count) {
        countLines(element.getPrefix(), count);
        return super.visitElement(element, count);
    }

    @Override
    public Xml visitAttribute(Xml.Attribute attribute, AtomicInteger count) {
        countLines(attribute.getPrefix(), count);
        return super.visitAttribute(attribute, count);
    }

    @Override
    public Xml visitTag(Xml.Tag tag, AtomicInteger count) {
        countLines(tag.getPrefix(), count);
        if (tag.getClosing() != null) {
            countLines(tag.getClosing().getPrefix(), count);
        }
        return super.visitTag(tag, count);
    }

    @Override
    public Xml visitComment(Xml.Comment comment, AtomicInteger count) {
        countLines(comment.getPrefix(), count);
        return super.visitComment(comment, count);
    }

    @Override
    public Xml visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, AtomicInteger count) {
        countLines(docTypeDecl.getPrefix(), count);
        return super.visitDocTypeDecl(docTypeDecl, count);
    }

    @Override
    public Xml visitProlog(Xml.Prolog prolog, AtomicInteger count) {
        countLines(prolog.getPrefix(), count);
        return super.visitProlog(prolog, count);
    }

    @Override
    public Xml visitIdent(Xml.Ident ident, AtomicInteger count) {
        countLines(ident.getPrefix(), count);
        return super.visitIdent(ident, count);
    }

    public static int countLines(Tree tree) {
        return new CountLinesVisitor().reduce(tree, new AtomicInteger()).get();
    }

    private void countLines(String whitespace, AtomicInteger count) {
        int lines = StringUtils.countOccurrences(whitespace, "\n");
        if(lines > 0) {
            count.addAndGet(lines);
        }
    }

}
