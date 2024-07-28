/*
 * Copyright 2023 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddProfile extends Recipe {
    private static final XPathMatcher PROJECT_MATCHER = new XPathMatcher("/project");

    @Option(displayName = "id",
            description = "The profile id.",
            example = "default")
    String id;

    @Option(displayName = "Activation",
            description = "activation details of a maven profile, provided as raw XML.",
            example = "<activation><foo>foo</foo></activation>",
            required = false)
    @Nullable
    String activation;

    @Option(displayName = "Properties",
            description = "properties of a maven profile, provided as raw XML.",
            example = "<properties><foo>foo</foo><bar>bar</bar></properties>",
            required = false)
    @Nullable
    String properties;

    @Option(displayName = "build",
            description = "build details of a maven profile, provided as raw XML.",
            example = "<build><foo>foo</foo></build>",
            required = false)
    @Nullable
    String build;

    @Override
    public String getDisplayName() {
        return "Add Maven profile";
    }

    @Override
    public String getDescription() {
        return "Add a maven profile to a `pom.xml` file.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddProfile.AddProfileVisitor();
    }

    private class AddProfileVisitor extends MavenIsoVisitor<ExecutionContext> {

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);

            if (PROJECT_MATCHER.matches(getCursor())) {
                Optional<Xml.Tag> maybeProfiles = t.getChild("profiles");
                Xml.Tag profiles;
                if (maybeProfiles.isPresent()) {
                    profiles = maybeProfiles.get();
                } else {
                    t = (Xml.Tag) new AddToTagVisitor<>(t, Xml.Tag.build("<profiles/>")).visitNonNull(t, ctx, getCursor().getParentOrThrow());
                    //noinspection OptionalGetWithoutIsPresent
                    profiles = t.getChild("profiles").get();
                }

                Optional<Xml.Tag> maybeProfile = profiles.getChildren().stream()
                        .filter(profile ->
                                profile.getChildValue("id").get().equals(id)
                        )
                        .findAny();

                if (maybeProfile.isPresent()) {
                    Xml.Tag profile = maybeProfile.get();

                    t = (Xml.Tag) new RemoveContentVisitor(profile, false).visitNonNull(t, ctx, getCursor().getParentOrThrow());

                }
                Xml.Tag profileTag = Xml.Tag.build("<profile>\n" +
                                                   "<id>" + id + "</id>\n" +
                                                   (activation != null ? activation.trim() + "\n" : "") +
                                                   (properties != null ? properties.trim() + "\n" : "") +
                                                   (build != null ? build.trim() + "\n" : "") +
                                                   "</profile>");
                t = (Xml.Tag) new AddToTagVisitor<>(profiles, profileTag).visitNonNull(t, ctx, getCursor().getParentOrThrow());

            }

            return t;
        }
    }
}
