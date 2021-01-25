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
import org.openrewrite.Validated;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Optional;

import static org.openrewrite.Validated.required;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExcludeDependency extends Recipe {
    private final String groupId;
    private final String artifactId;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ExcludeDependencyVisitor();
    }

    private class ExcludeDependencyVisitor extends MavenVisitor<ExecutionContext> {

        public ExcludeDependencyVisitor() {
            setCursoringOn();
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isDependencyTag()) {
                Pom.Dependency dependency = findDependency(tag);
                if (dependency != null && !dependency.findDependencies(groupId, artifactId).isEmpty()) {
                    Optional<Xml.Tag> maybeExclusions = tag.getChild("exclusions");
                    if (maybeExclusions.isPresent()) {
                        Xml.Tag exclusions = maybeExclusions.get();

                        List<Xml.Tag> individualExclusions = exclusions.getChildren("exclusion");
                        if (individualExclusions.stream().noneMatch(exclusion ->
                                groupId.equals(exclusion.getChildValue("groupId").orElse(null)) &&
                                        artifactId.equals(exclusion.getChildValue("artifactId").orElse(null)))) {
                            doAfterVisit(new AddToTagVisitor<>(exclusions, Xml.Tag.build("<exclusion>\n" +
                                    "<groupId>" + groupId + "</groupId>\n" +
                                    "<artifactId>" + artifactId + "</artifactId>\n" +
                                    "</exclusion>")));
                        }

                    } else {
                        doAfterVisit(new AddToTagVisitor<>(tag, Xml.Tag.build("<exclusions>\n" +
                                "<exclusion>\n" +
                                "<groupId>" + groupId + "</groupId>\n" +
                                "<artifactId>" + artifactId + "</artifactId>\n" +
                                "</exclusion>\n" +
                                "</exclusions>")));
                    }
                }
            }

            return super.visitTag(tag, ctx);
        }
    }
}
