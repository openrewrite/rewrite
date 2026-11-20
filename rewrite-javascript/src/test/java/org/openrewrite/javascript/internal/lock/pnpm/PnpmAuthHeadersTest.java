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
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PnpmAuthHeadersTest {
    private static final String URL = "https://private.example/";
    private static final String KEY = "//private.example/:_authToken=";

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.34.9", "11.0.3", "11.1.2", "11.1.3", "11.5.2",
      "11.5.3", "11.10.0", "11.16.0"})
    void exactNativeSourcePolicyAndPrecedence(String version) {
        boolean dropsProject = version.equals("10.34.9") || version.equals("11.5.3") ||
          version.equals("11.10.0") || version.equals("11.16.0");
        PnpmRelease release = PnpmRelease.of(version);
        Map<String, String> env = Map.of("TOKEN", "synthetic-env");
        assertThat(options(KEY + "${TOKEN}", null, env, release).authHeaders().authorization(URL, "@s/p"))
          .isEqualTo(dropsProject ? null : "Bearer synthetic-env");
        assertThat(options(KEY + "${TOKEN}", KEY + "synthetic-user", env, release).authHeaders()
          .authorization(URL, "@s/p")).isEqualTo(dropsProject ? "Bearer synthetic-user" : "Bearer synthetic-env");
        assertThat(options(KEY + "synthetic-project", KEY + "${TOKEN}", env, release).authHeaders()
          .authorization(URL, "@s/p")).isEqualTo("Bearer synthetic-project");
        assertThat(options(null, KEY + "${TOKEN}", env, release).authHeaders().authorization(URL, "@s/p"))
          .isEqualTo("Bearer synthetic-env");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.34.9", "11.5.3", "11.10.0", "11.16.0"})
    void noEnvironmentExpansionInRepositoryAuthKeysOrValues(String version) {
        PnpmRelease release = PnpmRelease.of(version);
        for (String config : new String[]{KEY + "${TOKEN}", "//${HOST}/:_authToken=${TOKEN}",
          "//private.example/:${ATTR}=${TOKEN}", "${KEY}=${TOKEN}",
          "//private.example/:_auth=${PAIR}", "//private.example/:username=${USER}\n//private.example/:_password=${PASSWORD}"}) {
            assertThat(options(config, null, Map.of("TOKEN", "synthetic-env", "HOST", "private.example",
              "ATTR", "_authToken", "KEY", "//private.example/:_authToken", "PAIR", "dTpw", "USER", "u",
              "PASSWORD", "cA=="), release).authHeaders().authorization(URL, "@s/p")).isNull();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.34.9", "11.1.3", "11.5.3", "11.10.0", "11.16.0"})
    void unsetVariablesNeverBecomeAuthorization(String version) {
        PnpmRelease release = PnpmRelease.of(version);
        assertThat(options(KEY + "${MISSING}", null, Map.of(), release).authHeaders().authorization(URL, "p"))
          .isNull();
        assertThat(options(null, KEY + "${MISSING}", Map.of(), release).authHeaders().authorization(URL, "p"))
          .isNull();
    }

    @Test
    void oldNativePlaceholderBehaviorIsAnExplicitRefusal() {
        assertThatThrownBy(() -> options(KEY + "${MISSING}", null, Map.of(), PnpmRelease.of("11.0.3"))
          .authHeaders()).isInstanceOf(CannotKnow.class);
    }

    @Test
    void valuesAreExpandedOnceAndLiteralBasicCredentialsStillWork() {
        PnpmRelease release = PnpmRelease.of("11.10.0");
        assertThat(options(null, KEY + "${TOKEN}", Map.of("TOKEN", "synthetic-token"), release)
          .authHeaders().authorization(URL, "p")).isEqualTo("Bearer synthetic-token");
        assertThat(options("//private.example/:_auth=dTpw", null, Map.of(), release)
          .authHeaders().authorization(URL, "p")).isEqualTo("Basic dTpw");
        assertThat(options("//private.example/:username=u\n//private.example/:_password=cA==", null, Map.of(), release)
          .authHeaders().authorization(URL, "p")).isEqualTo("Basic dTpw");
    }

    @Test
    void scopesAndPathsRestrictWhichCredentialIsSent() {
        PnpmAuthHeaders auth = options(KEY + "base\n//private.example/team/:_authToken=path\n" +
          "//private.example/:@s:_authToken=scoped", null, Map.of(), PnpmRelease.of("11.10.0")).authHeaders();
        assertThat(auth.authorization(URL + "team/", "p")).isEqualTo("Bearer path");
        assertThat(auth.authorization(URL + "team/", "@s/p")).isEqualTo("Bearer scoped");
        assertThat(auth.authorization("https://other.example/", "@s/p")).isNull();
        assertThat(auth.authorization("https://private.example.evil/", "p")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.0.0", "10.11.0", "10.34.9", "11.0.3", "11.1.2", "11.1.3", "11.5.2",
      "11.5.3", "11.10.0", "11.16.0"})
    void defaultUnscopedCredentialsHaveTheNativesPrecedence(String version) {
        boolean rescoped = java.util.List.of("10.34.9", "11.5.2", "11.5.3", "11.10.0", "11.16.0").contains(version);
        PnpmAuthHeaders auth = options("registry=" + URL + "\n" + KEY + "synthetic-scoped\n" +
          "_authToken=synthetic-default", null, Map.of(), PnpmRelease.of(version)).authHeaders();
        assertThat(auth.authorization(URL, "p"))
          .isEqualTo(rescoped ? "Bearer synthetic-scoped" : "Bearer synthetic-default");
    }

    @Test
    void bootstrapUsesTrustedCredentialsEvenWhenRegistryRoutesAreTheSame() {
        PnpmOptions options = options(KEY + "project", KEY + "user", Map.of(), PnpmRelease.of("11.10.0"));
        assertThat(options.authHeaders().authorization(URL, "p")).isEqualTo("Bearer project");
        assertThat(options.packageManagerBootstrap(PnpmRelease.of("11.10.0")).authHeaders().authorization(URL, "p"))
          .isEqualTo("Bearer user");
    }

    private static PnpmOptions options(String project, String user, Map<String, String> env, PnpmRelease release) {
        return PnpmOptions.load(null, project, user, null, env, release);
    }
}
