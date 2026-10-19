/*
 * Copyright 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.TreeShapeValidationService;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

public class XmlTreeShapeValidationService implements TreeShapeValidationService {

    @Override
    public @Nullable Object value(Tree tree) {
        if (tree instanceof Xml.Tag) {
            return "the name `" + ((Xml.Tag) tree).getName() + "`";
        } else if (tree instanceof Xml.Ident) {
            return "the name `" + ((Xml.Ident) tree).getName() + "`";
        } else if (tree instanceof Xml.Attribute.Value) {
            return "the value `" + ((Xml.Attribute.Value) tree).getValue() + "`";
        } else if (tree instanceof Xml.CharData) {
            Xml.CharData charData = (Xml.CharData) tree;
            return (charData.isCdata() ? "the CDATA `" : "the text `") + charData.getText().trim() + "`";
        } else if (tree instanceof Xml.Comment) {
            return "the comment `" + ((Xml.Comment) tree).getText().trim() + "`";
        } else if (tree instanceof Xml.ProcessingInstruction) {
            return "the name `" + ((Xml.ProcessingInstruction) tree).getName() + "`";
        }
        return null;
    }

    @Override
    public Parser.Builder syntaxParser() {
        return XmlParser.builder();
    }

    @Override
    public List<String> invalid(SourceFile sourceFile) {
        List<String> invalid = new ArrayList<>();
        new XmlIsoVisitor<List<String>>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, List<String> found) {
                Set<String> keys = new HashSet<>();
                for (Xml.Attribute attribute : tag.getAttributes()) {
                    if (!keys.add(attribute.getKeyAsString())) {
                        found.add("the attribute `" + attribute.getKeyAsString() + "` appears twice on `<" +
                                  tag.getName() + ">`, which XML doesn't allow");
                    }
                }
                return super.visitTag(tag, found);
            }
        }.visit(sourceFile, invalid);
        return invalid;
    }
}
