/*
 * Copyright 2025 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.Set;

public class RemoveDuplicatePluginDeclarations extends Recipe {

    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("//project/build/plugins");
    private static final XPathMatcher PLUGIN_MANAGEMENT_PLUGINS_MATCHER = new XPathMatcher("//project/build/pluginManagement/plugins");

    @Override
    public String getDisplayName() {
        return "Remove duplicate plugin declarations";
    }

    @Override
    public String getDescription() {
        return "Maven 4 rejects duplicate plugin declarations (same groupId and artifactId) with an error. " +
                "This recipe removes duplicate plugin declarations, keeping only the first occurrence.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (PLUGINS_MATCHER.matches(getCursor()) || PLUGIN_MANAGEMENT_PLUGINS_MATCHER.matches(getCursor())) {
                    Set<String> seenPlugins = new HashSet<>();
                    return t.withContent(ListUtils.filter(t.getContent(), content -> {
                        if (content instanceof Xml.Tag) {
                            Xml.Tag pluginTag = (Xml.Tag) content;
                            if ("plugin".equals(pluginTag.getName())) {
                                String groupId = pluginTag.getChildValue("groupId").orElse("org.apache.maven.plugins");
                                String artifactId = pluginTag.getChildValue("artifactId").orElse(null);
                                if (artifactId != null) {
                                    return seenPlugins.add(groupId + ":" + artifactId); // Keep only the first occurrence
                                }
                            }
                        }
                        return true; // Keep non-plugin tags
                    }));
                }

                return t;
            }
        };
    }
}
