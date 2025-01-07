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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Validated;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.openrewrite.xml.Assertions.xml;

class EnableDevelocityBuildCacheTest implements RewriteTest {

    @DocumentExample
    @Test
    void addBuildCacheAllConfigWithOptions() {
        rewriteRun(
          spec -> spec.recipe(new EnableDevelocityBuildCache("true", "true", "#{isTrue(env['CI'])}")),
          xml(
            """
              <develocity>
              </develocity>
              """,
            """
              <develocity>
                <buildCache>
                  <local>
                    <enabled>true</enabled>
                  </local>
                  <remote>
                    <enabled>true</enabled>
                    <storeEnabled>#{isTrue(env['CI'])}</storeEnabled>
                  </remote>
                </buildCache>
              </develocity>
              """,
            spec -> spec.path(".mvn/develocity.xml")
          )
        );
    }

    @Test
    void requireAtLeastOneOption() {
        Validated<Object> validate = new EnableDevelocityBuildCache(null, null, null).validate(new InMemoryExecutionContext());
        assertThat(validate.isValid()).isFalse();
    }

    @Test
    void addBuildCacheConfigWithLocalOnly() {
        rewriteRun(
          spec -> spec.recipe(new EnableDevelocityBuildCache("true", null, null)),
          xml(
            """
              <develocity>
              </develocity>
              """,
            """
              <develocity>
                <buildCache>
                  <local>
                    <enabled>true</enabled>
                  </local>
                </buildCache>
              </develocity>
              """,
            spec -> spec.path(".mvn/develocity.xml")
          )
        );
    }

    @Test
    void addBuildCacheAllConfigWithRemoteOnly() {
        rewriteRun(
          spec -> spec.recipe(new EnableDevelocityBuildCache(null, "true", "#{isTrue(env['CI'])}")),
          xml(
            """
              <develocity>
              </develocity>
              """,
            """
              <develocity>
                <buildCache>
                  <remote>
                    <enabled>true</enabled>
                    <storeEnabled>#{isTrue(env['CI'])}</storeEnabled>
                  </remote>
                </buildCache>
              </develocity>
              """,
            spec -> spec.path(".mvn/develocity.xml")
          )
        );
    }

    @Test
    void shouldNotModifyExistingConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new EnableDevelocityBuildCache("true", "true", "true")),
          xml(
            """
              <develocity>
                <buildCache>
                  <local>
                    <enabled>false</enabled>
                  </local>
                  <remote>
                    <enabled>false</enabled>
                    <storeEnabled>false</storeEnabled>
                  </remote>
                </buildCache>
              </develocity>
              """,
            spec -> spec.path(".mvn/develocity.xml")
          )
        );
    }

    @Test
    void shouldNotModifyOtherLocations() {
        rewriteRun(
          spec -> spec.recipe(new EnableDevelocityBuildCache("true", "true", "true")),
          xml(
            """
              <develocity>
              </develocity>
              """,
            spec -> spec.path("src/test/resources/develocity.xml")
          )
        );
    }
}
