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

import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;

public class AddToTag extends XmlRefactorVisitor {
    private final Xml.Tag scope;
    private final Xml.Tag tagToAdd;

    public AddToTag(Xml.Tag scope, String tagSource) {
        super("xml.AddToTag");
        this.scope = scope;
        this.tagToAdd = new XmlParser().parseTag(tagSource);
        setCursoringOn();
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public Xml visitTag(Xml.Tag tag) {
        Xml.Tag t = refactor(tag, super::visitTag);
        if (scope.isScope(tag)) {
            List<Content> content = t.getContent() == null ? new ArrayList<>() : new ArrayList<>(t.getContent());
            content.add(tagToAdd.withPrefix(formatter.findIndent(enclosingTag().getFormatting().getIndent(), tag).getPrefix()));
            t = t.withContent(content);
        }
        return t;
    }
}
