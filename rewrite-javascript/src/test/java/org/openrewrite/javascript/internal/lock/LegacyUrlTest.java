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
package org.openrewrite.javascript.internal.lock;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LegacyUrl against Node 24.21.0's url.parse over git specs, tarball URLs and the edges of its parser (rows written by
 * generate/legacy-url.js).
 */
class LegacyUrlTest {

    private static final String[] FIELDS = {"protocol", "slashes", "auth", "host", "port", "hostname", "hash",
      "search", "query", "pathname", "path"};

    static Stream<UpstreamCalls.Call> calls() {
        return UpstreamCalls.read("node-24.21.0-url-parse.jsonl.gz").stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("calls")
    void parsesAsNodeDoes(UpstreamCalls.Call call) {
        String input = (String) call.arg(0);
        if (call.getThrew() != null) {
            assertThatThrownBy(() -> LegacyUrl.parse(input)).isInstanceOf(JsError.class)
              .hasMessage((String) call.getThrew().get("message"));
            return;
        }
        LegacyUrl url = LegacyUrl.parse(input);
        Map<?, ?> out = (Map<?, ?>) call.getOut();
        Object[] got = {url.getProtocol(), url.getSlashes(), url.getAuth(), url.getHost(), url.getPort(),
          url.getHostname(), url.getHash(), url.getSearch(), url.getQuery(), url.getPathname(), url.getPath()};
        for (int i = 0; i < FIELDS.length; i++) {
            assertThat(got[i]).as(FIELDS[i]).isEqualTo(out.get(FIELDS[i]));
        }
    }
}
