/*
 * Copyright 2026 the original author or authors.
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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.ChangeTagValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class UpdateMavenProfilePropertiesTest implements RewriteTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void refreshesPropertiesOfModeledProfiles(boolean active) {
        String before = "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>profiles</artifactId><version>1</version>" +
                "<properties><level>31</level></properties><profiles><profile><id>selected</id><activation><jdk>" +
                (active ? "[9,)" : "[99,)") + "</jdk></activation><properties><level>8</level></properties></profile></profiles></project>";
        Recipe refresh = new Recipe() {
            @Override
            public String getDisplayName() { return "Refresh Maven model"; }
            @Override
            public String getDescription() { return "Resolve the current Maven declarations after an XML edit."; }
            @Override
            public TreeVisitor<?, ExecutionContext> getVisitor() { return new UpdateMavenModel<>(); }
        };
        rewriteRun(spec -> spec.recipes(new ChangeTagValue("/project/profiles/profile/properties/level", null, "25", null), refresh)
                        .cycles(1).expectedCyclesThatMakeChanges(1),
                pomXml(before, before.replace("<level>8</level>", "<level>25</level>"), spec -> spec.afterRecipe(document -> {
                    var pom = document.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom();
                    assertThat(pom.getRequested().getProfiles().get(0).getProperties()).containsEntry("level", "25");
                    assertThat(pom.getValue("${level}")).isEqualTo(active ? "25" : "31");
                })));
    }
}
