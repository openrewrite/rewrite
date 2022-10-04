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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.tree.Xml;

public class RemoveDeadRepos extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove dead repositories";
    }

    @Override
    public String getDescription() {
        return "Remove repos marked as dead in Maven POM files.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveDeadReposVisitor();
    }

    private static class RemoveDeadReposVisitor extends XmlIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            Xml.Tag newTag = t;
            if (t.getName().equals("repository")) {
                newTag = t.getChild("url").flatMap(Xml.Tag::getValue).map(url -> {
                    String urlKey = IdentifyUnreachableRepos.httpOrHttps(url);
                    if (IdentifyUnreachableRepos.KNOWN_DEFUNCT.containsKey(urlKey)) {
                        if (IdentifyUnreachableRepos.KNOWN_DEFUNCT.get(urlKey) == null) {
                            return null;
                        }
                        return t.withChildValue("url", "https://" + IdentifyUnreachableRepos.KNOWN_DEFUNCT.get(urlKey));
                    }
                    return t;
                }).orElse(t);
            }
            return newTag;
        }

    }
}