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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.AddToTagProcessor;
import org.openrewrite.xml.ChangeTagValueProcessor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static org.openrewrite.Validated.required;

public class AddPlugin extends Recipe {
    private String groupId;
    private String artifactId;
    private String version;

    public AddPlugin() {
        this.processor = () -> new AddPluginProcessor(groupId, artifactId, version);
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId)
                .and(required("version", version)));
    }

    private static class AddPluginProcessor extends MavenProcessor<ExecutionContext> {
        private static final XPathMatcher BUILD_MATCHER = new XPathMatcher("/project/build");

        private final String groupId;
        private final String artifactId;
        private final String version;

        public AddPluginProcessor(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            setCursoringOn();
        }

        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            Xml.Tag root = maven.getRoot();
            if (!root.getChild("build").isPresent()) {
                doAfterVisit(new AddToTagProcessor<>(root, Xml.Tag.build("<build/>"),
                        new MavenTagInsertionComparator(root.getChildren())));
            }

            return super.visitMaven(maven, ctx);
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (BUILD_MATCHER.matches(getCursor())) {
                Optional<Xml.Tag> maybePlugins = tag.getChild("plugins");
                if (!maybePlugins.isPresent()) {
                    doAfterVisit(new AddToTagProcessor<>(tag, Xml.Tag.build("<plugins/>")));
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
                            doAfterVisit(new ChangeTagValueProcessor<>(plugin.getChild("version").get(), version));
                        }
                    } else {
                        doAfterVisit(new AddToTagProcessor<>(plugins, Xml.Tag.build("<plugin>\n" +
                                "<groupId>" + groupId + "</groupId>\n" +
                                "<artifactId>" + artifactId + "</artifactId>\n" +
                                "<version>" + version + "</version>\n" +
                                "</plugin>")));
                    }
                }

                return tag;
            }

            return super.visitTag(tag, ctx);
        }
    }
}
