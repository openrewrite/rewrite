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

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.HashMap;
import java.util.Map;

public class ReplaceDeprecatedLifecyclePhases extends Recipe {

    private static final Map<String, String> PHASE_REPLACEMENTS = new HashMap<>();
    static {
        PHASE_REPLACEMENTS.put("pre-integration-test", "before:integration-test");
        PHASE_REPLACEMENTS.put("post-integration-test", "after:integration-test");
    }

    private static final XPathMatcher PLUGIN_PHASE_MATCHER = new XPathMatcher("//project/build/plugins/plugin/executions/execution/phase");
    private static final XPathMatcher PLUGIN_MANAGEMENT_PHASE_MATCHER = new XPathMatcher("//project/build/pluginManagement/plugins/plugin/executions/execution/phase");

    @Override
    public String getDisplayName() {
        return "Replace deprecated lifecycle phases";
    }

    @Override
    public String getDescription() {
        return "Maven 4 deprecated the `pre-integration-test` and `post-integration-test` lifecycle phases " +
               "in favor of `before:integration-test` and `after:integration-test`. " +
               "This recipe updates plugin phase declarations to use the new syntax.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("**/pom.xml"), new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.@Nullable Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if ((PLUGIN_PHASE_MATCHER.matches(getCursor()) || PLUGIN_MANAGEMENT_PHASE_MATCHER.matches(getCursor()))
                    && t.getValue().isPresent()) {
                    String currentPhase = t.getValue().get();
                    String newPhase = PHASE_REPLACEMENTS.get(currentPhase);
                    if (newPhase != null) {
                        t = t.withValue(newPhase);
                    }
                }

                return t;
            }
        });
    }
}
