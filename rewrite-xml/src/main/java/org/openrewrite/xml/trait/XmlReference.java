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
package org.openrewrite.xml.trait;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.trait.Reference;
import org.openrewrite.xml.tree.Xml;

public abstract class XmlReference implements Reference {

    @Override
    public Tree getTree() {
        return Reference.super.getTree();
    }

    @Override
    public String getValue() {
        if (getTree() instanceof Xml.Attribute) {
            Xml.Attribute attribute = (Xml.Attribute) getTree();
            return attribute.getValueAsString();
        }
        if (getTree() instanceof Xml.Tag) {
            Xml.Tag tag = (Xml.Tag) getTree();
            if (tag.getValue().isPresent()) {
                return tag.getValue().get();
            }
        }
        throw new IllegalArgumentException("getTree() must be an Xml.Attribute or Xml.Tag: " + getTree().getClass());
    }

    @Override
    public boolean supportsRename() {
        return true;
    }

    @Override
    public Tree rename(Renamer renamer, Cursor cursor, ExecutionContext ctx) {
        Tree tree = cursor.getValue();
        if (tree instanceof Xml.Attribute) {
            Xml.Attribute attribute = (Xml.Attribute) tree;
            String renamed = renamer.rename(this);
            return attribute.withValue(attribute.getValue().withValue(renamed));
        }
        if (tree instanceof Xml.Tag && ((Xml.Tag) tree).getValue().isPresent()) {
            String renamed = renamer.rename(this);
            return ((Xml.Tag) tree).withValue(renamed);
        }
        return tree;
    }

}
