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
 *
 */
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.internal.StringUtils.matchesGlob;
import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterTagChildren;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangePackaging extends Recipe {
    private static final XPathMatcher PROJECT_MATCHER = new XPathMatcher("/project");

    @Option(displayName = "Group",
            description = "The groupId of the project whose packaging should be changed. Accepts glob patterns.",
            example = "org.openrewrite.*"
    )
    String groupId;

    @Option(displayName = "Group",
            description = "The artifactId of the project whose packaging should be changed. Accepts glob patterns.",
            example = "rewrite-*"
    )
    String artifactId;

    @Option(displayName = "Packaging",
            description = "The type of packaging to set. If `null` specified the packaging tag will be removed",
            example = "jar")
    @Nullable
    String packaging;

    @Override
    public String getDisplayName() {
        return "Set Maven project packaging";
    }

    public String getDescription() {
        return "Sets the packaging type of Maven projects. Either adds the packaging tag if it is missing or changes its context if present.";
    }

    public MavenVisitor getVisitor() {
        return new MavenVisitor() {
            @Override
            public Maven visitMaven(Maven maven, ExecutionContext ctx) {
                Pom pom = maven.getModel();
                if(!(matchesGlob(pom.getGroupId(), groupId) && matchesGlob(pom.getArtifactId(), artifactId))) {
                    return maven;
                }
                maven = maven.withModel(pom.withPackaging(packaging));
                return super.visitMaven(maven, ctx);
            }

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext context) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, context);
                if(PROJECT_MATCHER.matches(getCursor())) {
                    if(packaging == null) {
                        t = filterTagChildren(t, it -> !"packaging".equals(it.getName()));
                    } else {
                        t = addOrUpdateChild(t, Xml.Tag.build("\n<packaging>" + packaging + "</packaging>"), getCursor().getParentOrThrow());
                    }
                }
                return t;
            }
        };
    }
}
