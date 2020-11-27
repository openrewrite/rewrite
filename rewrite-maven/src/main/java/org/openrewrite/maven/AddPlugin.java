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

import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.AddToTag;
import org.openrewrite.xml.ChangeTagValue;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

public class AddPlugin extends MavenRefactorVisitor {
    private static final XPathMatcher BUILD_MATCHER = new XPathMatcher("/project/build");

    private String groupId;
    private String artifactId;
    private String version;

    public AddPlugin() {
        setCursoringOn();
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
    public Maven visitMaven(Maven maven) {
        Xml.Tag root = maven.getRoot();
        if (!root.getChild("build").isPresent()) {
            andThen(new AddToTag.Scoped(root, Xml.Tag.build("<build/>"),
                    new MavenTagInsertionComparator(root.getChildren())));
        }

        return super.visitMaven(maven);
    }

    @Override
    public Xml visitTag(Xml.Tag tag) {
        if (BUILD_MATCHER.matches(getCursor())) {
            Optional<Xml.Tag> maybePlugins = tag.getChild("plugins");
            if (!maybePlugins.isPresent()) {
                andThen(new AddToTag.Scoped(tag, Xml.Tag.build("<plugins/>")));
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
                        andThen(new ChangeTagValue.Scoped(plugin.getChild("version").get(), version));
                    }
                } else {
                    andThen(new AddToTag.Scoped(plugins, Xml.Tag.build("<plugin>\n" +
                            "<groupId>" + groupId + "</groupId>\n" +
                            "<artifactId>" + artifactId + "</artifactId>\n" +
                            "<version>" + version + "</version>\n" +
                            "</plugin>")));
                }
            }

            return tag;
        }

        return super.visitTag(tag);
    }
}
