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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port of pnpm 11.2.2's resolving/npm-resolver/test/whichVersionIsPinned.test.ts, workspacePrefToNpm.test.ts and
 * normalizeRegistryUrl.test.ts.
 */
class NpmResolverTest {

    @ParameterizedTest
    @CsvSource({
      "^1.0.0, major",
      "~1.0.0, minor",
      "1.0.0, patch",
      "*, none",
      "workspace:^1.0.0, major",
      "npm:foo@1.0.0, patch",
      "npm:@foo/foo@1.0.0, patch",
      "npm:foo@^1.0.0, major",
      "npm:@foo/foo@^1.0.0, major",
      "npm:@pnpm.e2e/qar@100.0.0, patch",
      "jsr:@foo/foo@1.0.0, patch",
      "jsr:foo@^1.0.0, major"
    })
    void whichVersionIsPinned(String spec, String expected) {
        assertThat(NpmResolver.whichVersionIsPinned(spec)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
      "https://registry.example.com:443/package.tgz, https://registry.example.com/package.tgz",
      "http://registry.example.com:80/package.tgz, http://registry.example.com/package.tgz",
      "https://registry.example.com:8443/package.tgz, https://registry.example.com:8443/package.tgz",
      "http://registry.example.com:8080/package.tgz, http://registry.example.com:8080/package.tgz",
      "https://registry.example.com/package.tgz, https://registry.example.com/package.tgz",
      "http://registry.example.com/package.tgz, http://registry.example.com/package.tgz",
      "https://artifactory:443/api/npm/npm-virtual/uuid/-/uuid-9.0.1.tgz, " +
      "https://artifactory/api/npm/npm-virtual/uuid/-/uuid-9.0.1.tgz",
      "invalid-url, invalid-url"
    })
    void normalizeRegistryUrl(String input, String expected) {
        assertThat(NpmResolver.normalizeRegistryUrl(input)).isEqualTo(expected);
    }

    @Test
    void workspacePrefToNpmResolvesWorkspaceOnlyVersionAliases() {
        assertThat(NpmResolver.workspacePrefToNpm("workspace:")).isEqualTo("*");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:*")).isEqualTo("*");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:^")).isEqualTo("*");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:~")).isEqualTo("*");
    }

    @Test
    void workspacePrefToNpmResolvesPackageNameAliases() {
        assertThat(NpmResolver.workspacePrefToNpm("workspace:is-positive@3.0.0")).isEqualTo("npm:is-positive@3.0.0");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:is-positive@*")).isEqualTo("npm:is-positive@*");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:is-positive@^")).isEqualTo("npm:is-positive@*");
    }

    @Test
    void workspacePrefToNpmResolvesScopedPackageNameAliases() {
        assertThat(NpmResolver.workspacePrefToNpm("workspace:@scope/is-positive@1.2.3"))
          .isEqualTo("npm:@scope/is-positive@1.2.3");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:@scope/is-positive@^1.2.3"))
          .isEqualTo("npm:@scope/is-positive@^1.2.3");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:@scope/is-positive@*"))
          .isEqualTo("npm:@scope/is-positive@*");
        assertThat(NpmResolver.workspacePrefToNpm("workspace:@scope/is-positive@~"))
          .isEqualTo("npm:@scope/is-positive@*");
    }
}
