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
package org.openrewrite.python.internal.index;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link EnvExpansion#expandUrl} to pipenv's {@code expand_url_credentials}
 * ({@code pipenv/utils/shell.py}) semantics.
 */
class EnvExpansionTest {

    private static Environment env(Map<String, String> vars) {
        return new Environment() {
            @Override
            public String getenv(String name) {
                return vars.get(name);
            }

            @Override
            public Path userHome() {
                return Paths.get("/unused");
            }

            @Override
            public String osName() {
                return "Linux";
            }
        };
    }

    @Test
    void urlWithoutDollarPassesThroughUntouched() {
        String url = "https://alice:p@ss%2Fword@corp.example.com/simple";
        assertThat(EnvExpansion.expandUrl(url, env(Map.of("PASS", "x")))).isEqualTo(url);
    }

    @Test
    void urlWithoutUserinfoGetsPlainExpandvars() {
        assertThat(EnvExpansion.expandUrl("https://host/$SEG/simple", env(Map.of("SEG", "corp"))))
          .isEqualTo("https://host/corp/simple");
    }

    @Test
    void unchangedUserinfoKeptVerbatim() {
        String url = "https://${UNSET}:${ALSO_UNSET}@host/simple";
        assertThat(EnvExpansion.expandUrl(url, env(Map.of()))).isEqualTo(url);
    }

    @Test
    void expandedUserinfoSplitOnFirstColonAndQuoted() {
        assertThat(EnvExpansion.expandUrl("https://${USER}:${PASS}@host/simple",
          env(Map.of("USER", "alice", "PASS", "p@ss:w0rd"))))
          .isEqualTo("https://alice:p%40ss%3Aw0rd@host/simple");
    }

    @Test
    void preExistingPercentEscapesAreReEncoded() {
        assertThat(EnvExpansion.expandUrl("https://bob:${PASS}@host/simple",
          env(Map.of("PASS", "a%2Fb"))))
          .isEqualTo("https://bob:a%252Fb@host/simple");
    }

    @Test
    void singleQuotedBraceFormStripsQuotes() {
        assertThat(EnvExpansion.expandUrl("https://'${TOKEN}'@host/simple",
          env(Map.of("TOKEN", "sekret"))))
          .isEqualTo("https://sekret@host/simple");
    }

    @Test
    void bareVariableFormExpands() {
        assertThat(EnvExpansion.expandUrl("https://$USER@host/simple",
          env(Map.of("USER", "alice"))))
          .isEqualTo("https://alice@host/simple");
    }

    @Test
    void partiallyExpandedUserinfoEncodesRemainingPlaceholder() {
        // pipenv percent-encodes whatever expansion produced, unresolved placeholders included
        assertThat(EnvExpansion.expandUrl("https://${USER}:${PASS}@host/simple",
          env(Map.of("USER", "alice"))))
          .isEqualTo("https://alice:%24%7BPASS%7D@host/simple");
    }

    @Test
    void hostAndPathGetPlainExpandvarsWithoutEncoding() {
        assertThat(EnvExpansion.expandUrl("https://${USER}@${HOST}/simple",
          env(Map.of("USER", "alice", "HOST", "corp.example.com"))))
          .isEqualTo("https://alice@corp.example.com/simple");
    }

    @Test
    void partiallyResolvedUserinfoIsFlaggedUnresolved() {
        // percent-encoding turns ${PASS} into %24%7BPASS%7D; the flag is judged before that
        EnvExpansion.Expansion expansion = EnvExpansion.expand("https://${USER}:${PASS}@host/simple",
          env(Map.of("USER", "alice")));
        assertThat(expansion.url).isEqualTo("https://alice:%24%7BPASS%7D@host/simple");
        assertThat(expansion.unresolvedPlaceholders).isTrue();
    }

    @Test
    void fullyResolvedUserinfoIsNotFlagged() {
        EnvExpansion.Expansion expansion = EnvExpansion.expand("https://${USER}:${PASS}@host/simple",
          env(Map.of("USER", "alice", "PASS", "sekret")));
        assertThat(expansion.url).isEqualTo("https://alice:sekret@host/simple");
        assertThat(expansion.unresolvedPlaceholders).isFalse();
    }
}
