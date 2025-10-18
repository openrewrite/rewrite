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

public class UpgradeToModelVersion410 extends Recipe {

    private static final XPathMatcher MODEL_VERSION_MATCHER = new XPathMatcher("/project/modelVersion");
    private static final String MODEL_VERSION_410 = "4.1.0";
    private static final String NAMESPACE_410 = "http://maven.apache.org/POM/4.1.0";

    @Override
    public String getDisplayName() {
        return "Upgrade to Maven model version 4.1.0";
    }

    @Override
    public String getDescription() {
        return "Upgrades Maven POMs from model version 4.0.0 to 4.1.0, enabling new Maven 4 features " +
               "like `<subprojects>`, `bom` packaging, and automatic version inference. " +
               "This recipe only updates the `<modelVersion>` element. Namespace updates should be done separately if needed.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("**/pom.xml"), new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.@Nullable Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                // Update modelVersion from 4.0.0 to 4.1.0
                if (MODEL_VERSION_MATCHER.matches(getCursor())) {
                    if (t.getValue().isPresent() && "4.0.0".equals(t.getValue().get())) {
                        t = t.withValue(MODEL_VERSION_410);
                    }
                }

                return t;
            }
        });
    }
}
