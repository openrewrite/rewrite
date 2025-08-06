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
package org.openrewrite.maven.plugin;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("ALL")
public class DependencyPluginGoalResolveSources extends Recipe {
    @Override
    public String getDisplayName() {
        return "Migrate to `maven-dependency-plugin` goal `resolve-sources`";
    }

    @Override
    public String getDescription() {
        return "Migrate from `sources` to `resolve-sources` for the `maven-dependency-plugin`.";
    }

    private static final XPathMatcher xPathMatcher = new XPathMatcher("//plugin[artifactId='maven-dependency-plugin']/executions/execution/goals[goal='sources']/goal");
    private static final String minimumVersion = "3.7.0";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator comparator = requireNonNull(Semver.validate(minimumVersion, null).getValue());
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (xPathMatcher.matches(getCursor()) && isPlugInVersionInRange()) {
                    return tag.withValue("resolve-sources");
                }
                return t;
            }

            private boolean isPlugInVersionInRange() {
                Cursor pluginCursor = getCursor().dropParentUntil(i -> i instanceof Xml.Tag && "plugin".equals(((Xml.Tag) i).getName()));
                Xml.Tag pluginTag = pluginCursor.getValue();
                Plugin plugin = findPlugin(pluginTag);
                if (plugin == null || plugin.getVersion() == null) {
                    plugin = findManagedPlugin(pluginTag);
                    if (plugin == null || plugin.getVersion() == null) {
                        return false;
                    }
                }
                return comparator.compare(null, plugin.getVersion(), minimumVersion) >= 0;
            }
        };
    }
}
