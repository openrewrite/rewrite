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

import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;

public class RemoveContent {
    private RemoveContent() {
    }

    public static class Scoped extends XmlRefactorVisitor {
        private final Content scope;

        public Scoped(Content tag) {
            this.scope = tag;
        }

        @Override
        public Xml visitTag(Xml.Tag tag) {
            for (Content content : tag.getContent()) {
                if (scope.isScope(content)) {
                    List<Content> contents = new ArrayList<>(tag.getContent());
                    contents.remove(content);
                    return tag.withContent(contents);
                }
            }

            return super.visitTag(tag);
        }
    }
}
