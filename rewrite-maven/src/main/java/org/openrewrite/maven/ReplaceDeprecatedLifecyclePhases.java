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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.HashMap;
import java.util.Map;

public class ReplaceDeprecatedLifecyclePhases extends Recipe {

    private static final Map<String, String> PHASE_REPLACEMENTS = new HashMap<>();

    static {
        PHASE_REPLACEMENTS.put("pre-clean", "before:clean");
        PHASE_REPLACEMENTS.put("post-clean", "after:clean");
        PHASE_REPLACEMENTS.put("pre-site", "before:site");
        PHASE_REPLACEMENTS.put("post-site", "after:site");
        PHASE_REPLACEMENTS.put("pre-integration-test", "before:integration-test");
        PHASE_REPLACEMENTS.put("post-integration-test", "after:integration-test");
    }

    private static final XPathMatcher PLUGIN_PHASE_MATCHER = new XPathMatcher("//plugins/plugin/executions/execution/phase");

    @Override
    public String getDisplayName() {
        return "Replace deprecated lifecycle phases";
    }

    @Override
    public String getDescription() {
        return "Maven 4 deprecated all `pre-*` and `post-*` lifecycle phases in favor of the `before:` and `after:` syntax. " +
                "This recipe updates plugin phase declarations to use the new syntax, including `pre-clean` → `before:clean`, " +
                "`pre-site` → `before:site`, `pre-integration-test` → `before:integration-test`, and their `post-*` equivalents.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if ((PLUGIN_PHASE_MATCHER.matches(getCursor())) && t.getValue().isPresent()) {
                    String newPhase = PHASE_REPLACEMENTS.get(t.getValue().get());
                    if (newPhase != null) {
                        return t.withValue(newPhase);
                    }
                }

                return t;
            }
        };
    }
}
