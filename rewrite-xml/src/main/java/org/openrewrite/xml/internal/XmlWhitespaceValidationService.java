/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.xml.internal;

import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.WhitespaceValidationService;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

public class XmlWhitespaceValidationService implements WhitespaceValidationService {
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Attribute visitAttribute(Xml.Attribute attribute, ExecutionContext executionContext) {
                Xml.Attribute a = super.visitAttribute(attribute, executionContext);
                a = a.withPrefix(withNonWhitespaceWarning(a.getPrefix()));
                return a;
            }

            @Override
            public Xml.CharData visitCharData(Xml.CharData charData, ExecutionContext executionContext) {
                Xml.CharData c = super.visitCharData(charData, executionContext);
                c = c.withPrefix(withNonWhitespaceWarning(c.getPrefix()));
                c = c.withAfterText(withNonWhitespaceWarning(c.getAfterText()));
                return c;
            }

            @Override
            public Xml.Comment visitComment(Xml.Comment comment, ExecutionContext executionContext) {
                Xml.Comment c = super.visitComment(comment, executionContext);
                c = c.withPrefix(withNonWhitespaceWarning(c.getPrefix()));
                return c;
            }

            @Override
            public Xml.DocTypeDecl visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, ExecutionContext executionContext) {
                Xml.DocTypeDecl d = super.visitDocTypeDecl(docTypeDecl, executionContext);
                d = d.withPrefix(withNonWhitespaceWarning(d.getPrefix()));
                return d;
            }

            // Document can start with a byte order marker, which is not technically whitespace
            // There should probably be some special handling of this, but it hasn't caused a problem so ignoring for now
//            @Override
//            public Xml.Document visitDocument(Xml.Document document, ExecutionContext executionContext) {
//                Xml.Document d = super.visitDocument(document, executionContext);
//                d = d.withPrefix(withNonWhitespaceWarning(d.getPrefix()));
//                return d;
//            }

            @Override
            public Xml.Element visitElement(Xml.Element element, ExecutionContext executionContext) {
                Xml.Element e = super.visitElement(element, executionContext);
                e = e.withPrefix(withNonWhitespaceWarning(e.getPrefix()));
                return e;
            }

            @Override
            public Xml.Ident visitIdent(Xml.Ident ident, ExecutionContext executionContext) {
                Xml.Ident i = super.visitIdent(ident, executionContext);
                i = i.withPrefix(withNonWhitespaceWarning(i.getPrefix()));
                return i;
            }

            @Override
            public Xml.XmlDecl visitXmlDecl(Xml.XmlDecl xmlDecl, ExecutionContext executionContext) {
                Xml.XmlDecl x = super.visitXmlDecl(xmlDecl, executionContext);
                x = x.withPrefix(withNonWhitespaceWarning(x.getPrefix()));
                return x;
            }

            @Override
            public Xml.ProcessingInstruction visitProcessingInstruction(Xml.ProcessingInstruction processingInstruction, ExecutionContext executionContext) {
                Xml.ProcessingInstruction p = super.visitProcessingInstruction(processingInstruction, executionContext);
                p = p.withPrefix(withNonWhitespaceWarning(p.getPrefix()));
                return p;
            }

            @Override
            public Xml.Prolog visitProlog(Xml.Prolog prolog, ExecutionContext executionContext) {
                Xml.Prolog p = super.visitProlog(prolog, executionContext);
                p = p.withPrefix(withNonWhitespaceWarning(p.getPrefix()));
                return p;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext executionContext) {
                Xml.Tag t = super.visitTag(tag, executionContext);
                t = t.withPrefix(withNonWhitespaceWarning(t.getPrefix()));
                return t;
            }
        };
    }

    private static String withNonWhitespaceWarning(String s) {
        if(StringUtils.isBlank(s)) {
            return s;
        }
        return "~~(non-whitespace)~~>" + s + "<~~";
    }
}
