/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Comparator;
import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddProperty<P> extends XmlVisitor<P> {
    String key;
    String value;

    @Override
    public Xml visitDocument(Xml.Document document, P p) {
        Optional<Xml.Tag> props = document.getRoot().getChild("properties");
        if (!props.isPresent()) {
            Xml.Tag prop = new XmlParser().parse("" +
                    "<properties>\n" +
                    "  <" + key + ">" + value + "</" + key + ">\n" +
                    "</properties>").get(0).getRoot();
            document = (Xml.Document) new AddToTagVisitor<P>(
                    document.getRoot(),
                    prop,
                    new MavenTagInsertionComparator(document.getRoot().getChildren())
            ).visitNonNull(document, p);
        } else {
            if(!props.get().getChild(key).isPresent()) {
                Xml.Tag prop = new XmlParser().parse("<" + key + ">" + value + "</" + key + ">")
                        .get(0).getRoot();
                document = (Xml.Document) new AddToTagVisitor<P>(
                        props.get(),
                        prop,
                        Comparator.comparing(Xml.Tag::getName)
                ).visitNonNull(document, p);
            } else {
                document = (Xml.Document) new ChangeTagValueVisitor<P>(props.get().getChild(key).get(), value)
                        .visitNonNull(document, p);
            }
        }
        return document;
    }
}
