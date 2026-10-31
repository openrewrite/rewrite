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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;

class UseUtf8FileEncodingTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.gradle.UseUtf8FileEncoding");
    }

    @DocumentExample("Pin the daemon's file encoding so the build does not depend on the platform default")
    @Test
    void appendsEncodingToExistingJvmArgs() {
        rewriteRun(
          properties(
            """
              org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
              org.gradle.caching=true
              """,
            """
              org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
              org.gradle.caching=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void noChangeWhenEncodingAlreadyUtf8() {
        rewriteRun(
          properties(
            """
              org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    /**
     * A second `-Dfile.encoding` would quietly win over the one already chosen.
     */
    @Test
    void noChangeWhenADifferentEncodingIsSet() {
        rewriteRun(
          properties(
            """
              org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-16
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    /**
     * Setting `org.gradle.jvmargs` replaces Gradle's default daemon arguments rather than adding to them, so
     * creating it here would drop the default heap and metaspace settings.
     */
    @Test
    void noChangeWhenJvmArgsAbsent() {
        rewriteRun(
          properties(
            """
              org.gradle.caching=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void noChangeToOtherPropertiesFiles() {
        rewriteRun(
          properties(
            """
              org.gradle.jvmargs=-Xmx2g
              """,
            spec -> spec.path("src/main/resources/application.properties")
          )
        );
    }
}
