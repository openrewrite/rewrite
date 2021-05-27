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
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.search.FindPlugin;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.search.FindTags;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

public class MigrateQuarkusMavenPluginNativeImageGoal extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate `quarkus-maven-plugin` `native-image` goal";
    }

    @Override
    public String getDescription() {
        return "Migrates the `quarkus-maven-plugin` deprecated `native-image` goal. " +
                "If the `native-image` goal needs to be removed, this adds `<quarkus.package.type>native</quarkus.package.type>` " +
                "to the `native` profile `properties` section, given the `native` profile exists in the `pom.xml`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MigrateQuarkusMavenPluginNativeImageGoalVisitor();
    }

    private static class MigrateQuarkusMavenPluginNativeImageGoalVisitor extends MavenVisitor {
        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            FindPlugin.find(maven, "io.quarkus", "quarkus-maven-plugin").forEach(plugin -> {
                FindTags.find(plugin, "//executions/execution/goals/goal").forEach(goal -> {
                    if (goal.getContent() != null && goal.getContent().size() == 1 && goal.getContent().get(0) instanceof Xml.CharData) {
                        Xml.CharData existingValue = (Xml.CharData) goal.getContent().get(0);
                        if (existingValue.getText().equalsIgnoreCase("native-image")) {
                            doAfterVisit(new RemoveContentVisitor<>(goal, true));
                            doAfterVisit(new AddQuarkusPackageTypePropertyToNativeProfile());
                        }
                    }
                });
            });

            return super.visitMaven(maven, ctx);
        }

    }

    private static class AddQuarkusPackageTypePropertyToNativeProfile extends MavenVisitor {
        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            FindTags.find(maven, "/project/profiles/profile").forEach(profile -> {
                Optional<Xml.Tag> maybeId = profile.getChild("id");
                if (maybeId.isPresent()) {
                    String profileId = maybeId.get().getValue().orElse(null);
                    if (profileId != null && profileId.equals("native")) {
                        // we're now in the correct profile location; time to look at properties
                        Optional<Xml.Tag> maybeProperties = profile.getChild("properties");
                        if (!maybeProperties.isPresent()) {
                            // properties tag was missing; we'll add it to this profile and re-run the visitor
                            Xml.Tag propertiesTag = Xml.Tag.build("<properties/>");
                            doAfterVisit(new AddToTagVisitor<>(profile, propertiesTag));
                            doAfterVisit(new AddQuarkusPackageTypePropertyToNativeProfile());
                        } else {
                            Xml.Tag profileProperties = maybeProperties.get();
                            Optional<Xml.Tag> maybePackagingProperty = profileProperties.getChildren().stream()
                                    .filter(prop -> prop.getName().equals("quarkus.package.type"))
                                    .findAny();

                            if (!maybePackagingProperty.isPresent()) {
                                Xml.Tag newVersionTag = Xml.Tag.build("<quarkus.package.type>native</quarkus.package.type>");
                                doAfterVisit(new AddToTagVisitor<>(profileProperties, newVersionTag));
                            }
                        }
                    }
                }
            });

            return super.visitMaven(maven, ctx);
        }

    }

}

