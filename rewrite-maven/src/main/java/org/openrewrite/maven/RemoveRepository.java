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
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveRepository extends Recipe {
    private static final XPathMatcher REPOS_MATCHER = new XPathMatcher("/project/repositories/repository");
    private static final XPathMatcher PLUGIN_REPOS_MATCHER = new XPathMatcher("/project/pluginRepositories/pluginRepository");

    @Option(displayName = "Repository ID",
            description = "A unique repository ID.",
            required = false)
    @Nullable
    String id;

    @Option(displayName = "Repository URL",
            description = "The URL of the repository.")
    String url;

    @Override
    public String getDisplayName() {
        return "Remove repository";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s`", url);
    }

    @Override
    public String getDescription() {
        return "Removes a matching Maven repository.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag repo = super.visitTag(tag, ctx);

                if (REPOS_MATCHER.matches(getCursor()) || PLUGIN_REPOS_MATCHER.matches(getCursor())) {
                    if (isSameUrlAndID(repo)) {
                        return null;
                    }
                }
                return repo;
            }
        };
    }

    private boolean isSameUrlAndID(Xml.Tag repo) {
        boolean sameURL = StringUtils.isBlank(this.url) || StringUtils.equals(this.url, repo.getChildValue("url").orElse(null));
        boolean sameID = StringUtils.isBlank(this.id) || StringUtils.equals(this.id, repo.getChildValue("id").orElse(null));

        return sameURL && sameID;
    }
}
