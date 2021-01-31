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
package org.openrewrite.maven;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Data
@EqualsAndHashCode(callSuper = true)
public class AddPlugin extends Recipe {

    private static final XPathMatcher BUILD_MATCHER = new XPathMatcher("/project/build");

    private final String groupId;
    private final String artifactId;
    private final String version;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddPluginVisitor();
    }

    private class AddPluginVisitor extends MavenVisitor<ExecutionContext> {

        public AddPluginVisitor() {
            setCursoringOn();
        }

        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            Xml.Tag root = maven.getRoot();
            if (!root.getChild("build").isPresent()) {
                doAfterVisit(new AddToTagVisitor<>(root, Xml.Tag.build("<build>\n" +
                        "<plugins>\n" +
                        "<plugin>\n" +
                        "<groupId>" + groupId + "</groupId>\n" +
                        "<artifactId>" + artifactId + "</artifactId>\n" +
                        "<version>" + version + "</version>\n" +
                        "</plugin>\n" +
                        "</plugins>\n" +
                        "</build>"),
                        new MavenTagInsertionComparator(root.getChildren())));
            }

            return super.visitMaven(maven, ctx);
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);

            if (BUILD_MATCHER.matches(getCursor())) {
                Optional<Xml.Tag> maybePlugins = t.getChild("plugins");
                if (!maybePlugins.isPresent()) {
                    doAfterVisit(new AddToTagVisitor<>(t, Xml.Tag.build("<plugins/>")));
                } else {
                    Xml.Tag plugins = maybePlugins.get();

                    Optional<Xml.Tag> maybePlugin = plugins.getChildren().stream()
                            .filter(plugin ->
                                    plugin.getName().equals("plugin") &&
                                            groupId.equals(plugin.getChildValue("groupId").orElse(null)) &&
                                            artifactId.equals(plugin.getChildValue("artifactId").orElse(null))
                            )
                            .findAny();

                    if (maybePlugin.isPresent()) {
                        Xml.Tag plugin = maybePlugin.get();
                        if (!version.equals(plugin.getChildValue("version").orElse(null))) {
                            //noinspection OptionalGetWithoutIsPresent
                            doAfterVisit(new ChangeTagValueVisitor<>(plugin.getChild("version").get(), version));
                        }
                    } else {
                        doAfterVisit(new AddToTagVisitor<>(plugins, Xml.Tag.build("<plugin>\n" +
                                "<groupId>" + groupId + "</groupId>\n" +
                                "<artifactId>" + artifactId + "</artifactId>\n" +
                                "<version>" + version + "</version>\n" +
                                "</plugin>")));
                    }
                }
            }

            return t;
        }
    }
}
