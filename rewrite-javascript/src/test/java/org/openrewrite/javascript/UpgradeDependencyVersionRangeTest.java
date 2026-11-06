/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;

/**
 * Which ranges an upgrade to {@code ^5.9.3} replaces: one that admits a version below 5.9.3. One that already starts
 * at or above it is left (mapbox/pixelmatch's {@code ^6.0.3}), and so is a peer range that admits every version the
 * new one does (0no-co/gql.tada's union of majors), which a published package keeps as it declared it.
 */
class UpgradeDependencyVersionRangeTest implements RewriteTest {

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = ';', value = {
      "devDependencies      ; ^6.0.3                                  ;",
      "devDependencies      ; ^5.8.3                                  ; ^5.9.3",
      "dependencies         ; >=5.9.3                                 ;",
      "dependencies         ; ~5.9.5                                  ;",
      "dependencies         ; 5.9.3                                   ;",
      "dependencies         ; 5.9.2                                   ; ^5.9.3",
      "dependencies         ; *                                       ; ^5.9.3",
      "dependencies         ; latest                                  ; ^5.9.3",
      "dependencies         ; ^5.0.0 || ^6.0.0                        ; ^5.9.3",
      "optionalDependencies ; ^5.0.0                                  ; ^5.9.3",
      "peerDependencies     ; ^5.0.0 || ^6.0.0 || ^7.0.0 || ^8.0.0    ;",
      "peerDependencies     ; >=5                                     ;",
      "peerDependencies     ; ^5.9.5                                  ;",
      "peerDependencies     ; ^4.0.0                                  ; ^5.9.3",
      "peerDependencies     ; ~5.8.0                                  ; ^5.9.3"
    })
    void replacesARangeThatAdmitsAVersionBelowTheNewOnesLowest(String scope, String range, @Nullable String replaced) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("typescript", null, "^5.9.3")),
          packageJson(manifest(scope, range), replaced == null ? null : manifest(scope, replaced),
            nodeResolutionResult(PackageManager.Npm))
        );
    }

    private static String manifest(String scope, String range) {
        return "{\n" +
               "  \"name\": \"app\",\n" +
               "  \"version\": \"1.0.0\",\n" +
               "  \"" + scope + "\": {\n" +
               "    \"typescript\": \"" + range + "\"\n" +
               "  }\n" +
               "}\n";
    }
}
