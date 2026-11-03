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

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.java.Assertions.mavenProject;

class UpdateMavenProjectPropertyJavaVersionAliasTest implements RewriteTest {
    @Override public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateMavenProjectPropertyJavaVersion(25)).cycles(1).expectedCyclesThatMakeChanges(1);
    }
    private String pom(String properties) {
        return "<project><modelVersion>4.0.0</modelVersion><groupId>peer</groupId><artifactId>consumer</artifactId><version>1</version><properties>" + properties + "</properties></project>";
    }
    @Test void sharedLeafAndTransitiveAliasStayShared() {
        String before = pom("<shared.level>8</shared.level><middle>${shared.level}</middle><unrelated.consumer>${shared.level}</unrelated.consumer><maven.compiler.release>${middle}</maven.compiler.release>");
        rewriteRun(pomXml(before, spec -> spec.after(after -> {
            assertThat(after).contains("<shared.level>8</shared.level>", "<middle>${shared.level}</middle>", "<unrelated.consumer>${shared.level}</unrelated.consumer>", "<maven.compiler.release>25</maven.compiler.release>");
            return after;
        }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getValue("${maven.compiler.release}")).isEqualTo("25"))));
    }
    @Test void actualCommons107InheritedJdkProfileGetsLocalStandardPin() {
        String before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <parent><groupId>org.apache.commons</groupId><artifactId>commons-parent</artifactId><version>107</version><relativePath/></parent>
              <artifactId>cora-profile-peer</artifactId><version>1</version>
              <properties><maven.compiler.source>25</maven.compiler.source><maven.compiler.target>25</maven.compiler.target></properties>
            </project>
            """;
        rewriteRun(pomXml(before, spec -> spec.beforeRecipe(doc -> {
            var pom = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom();
            assertThat(pom.getValue("${maven.compiler.release}")).isEqualTo("8");
            assertThat(pom.getValue("${commons.compiler.release}")).isEqualTo("8");
        }).after(after -> {
            assertThat(after).contains("<maven.compiler.release>25</maven.compiler.release>");
            assertThat(after).doesNotContain("<commons.compiler.release>");
            return after;
        }).afterRecipe(doc -> {
            var pom = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom();
            assertThat(pom.getValue("${maven.compiler.release}")).isEqualTo("25");
            assertThat(pom.getValue("${commons.compiler.release}")).isEqualTo("8");
        })));
    }
    @Test void futureAliasIsNotDowngraded() {
        rewriteRun(spec -> spec.expectedCyclesThatMakeChanges(0), pomXml(pom("<shared.level>31</shared.level><maven.compiler.release>${shared.level}</maven.compiler.release>")));
    }
    @Test void unresolvedAliasIsNotInvented() {
        rewriteRun(spec -> spec.expectedCyclesThatMakeChanges(0), pomXml(pom("<maven.compiler.release>${unknown.level}</maven.compiler.release>")));
    }
    @Test void cyclicAliasIsNotInvented() {
        rewriteRun(spec -> spec.expectedCyclesThatMakeChanges(0), pomXml(pom("<one>${two}</one><two>${one}</two><maven.compiler.release>${one}</maven.compiler.release>")));
    }
    @Test void reactorChildAliasShadowsFutureParentWithoutChangingSharedLeaf() {
        String parent = """
            <project><modelVersion>4.0.0</modelVersion><groupId>peer</groupId><artifactId>parent</artifactId><version>1</version><packaging>pom</packaging><modules><module>child</module></modules><properties><maven.compiler.release>31</maven.compiler.release><shared.level>8</shared.level></properties></project>
            """;
        String child = """
            <project><modelVersion>4.0.0</modelVersion><parent><groupId>peer</groupId><artifactId>parent</artifactId><version>1</version></parent><artifactId>child</artifactId><properties><maven.compiler.release>${shared.level}</maven.compiler.release></properties></project>
            """;
        rewriteRun(pomXml(parent), mavenProject("child", pomXml(child, spec -> spec.after(after -> {
            assertThat(after).contains("<maven.compiler.release>25</maven.compiler.release>"); return after;
        }).afterRecipe(doc -> {
            var pom=doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom();
            assertThat(pom.getValue("${maven.compiler.release}")).isEqualTo("25");
            assertThat(pom.getValue("${shared.level}")).isEqualTo("8");
        }))));
    }
    @Test void activeLocalProfileAliasOverridesAlreadyUpgradedRoot() {
        String before = pom("<shared.level>8</shared.level><maven.compiler.release>25</maven.compiler.release>")
            .replace("</project>", "<profiles><profile><id>java-9-up</id><activation><jdk>[9,)</jdk></activation><properties><maven.compiler.release>${shared.level}</maven.compiler.release></properties></profile></profiles></project>");
        rewriteRun(pomXml(before, spec -> spec.beforeRecipe(doc -> assertThat(doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getValue("${maven.compiler.release}")).isEqualTo("8"))
            .after(after -> {
                assertThat(after).contains("<shared.level>8</shared.level>");
                assertThat(after).doesNotContain("<maven.compiler.release>${shared.level}</maven.compiler.release>");
                assertThat(after.split("<maven.compiler.release>25</maven.compiler.release>", -1)).hasSize(3);
                return after;
            }).afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getValue("${maven.compiler.release}")).isEqualTo("25"))));
    }
    @Test void inactiveProfileAliasIsPreserved() {
        String before = pom("<shared.level>8</shared.level><maven.compiler.release>25</maven.compiler.release>")
            .replace("</project>", "<profiles><profile><id>future-jdk</id><activation><jdk>[99,)</jdk></activation><properties><maven.compiler.release>${shared.level}</maven.compiler.release></properties></profile></profiles></project>");
        rewriteRun(spec -> spec.expectedCyclesThatMakeChanges(0), pomXml(before));
    }
    @Test void activeFutureProfileAliasIsPreserved() {
        String before = pom("<shared.level>31</shared.level><maven.compiler.release>25</maven.compiler.release>")
            .replace("</project>", "<profiles><profile><id>java-9-up</id><activation><jdk>[9,)</jdk></activation><properties><maven.compiler.release>${shared.level}</maven.compiler.release></properties></profile></profiles></project>");
        rewriteRun(spec -> spec.expectedCyclesThatMakeChanges(0), pomXml(before));
    }

}
