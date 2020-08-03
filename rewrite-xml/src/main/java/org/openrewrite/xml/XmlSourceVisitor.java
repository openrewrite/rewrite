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

import org.openrewrite.SourceVisitor;
import org.openrewrite.xml.tree.Xml;

public interface XmlSourceVisitor<R> extends SourceVisitor<R> {

    Xml.Tag enclosingTag();
    R visitDocument(Xml.Document document);
    R visitProcessingInstruction(Xml.ProcessingInstruction pi);
    R visitTag(Xml.Tag tag);
    R visitAttribute(Xml.Attribute attribute);
    R visitCharData(Xml.CharData charData);
    R visitComment(Xml.Comment comment);
    R visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl);
    R visitProlog(Xml.Prolog prolog);
    R visitIdent(Xml.Ident ident);
    R visitElement(Xml.DocTypeDecl.Element element);
}
