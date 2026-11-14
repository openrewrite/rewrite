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
package org.openrewrite.javascript.internal.lock.bun;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port of bun's test/cli/install/hosted-git-info/from-url.test.ts (bun-v1.3.10) "valid urls"; bun itself skips its
 * "invalid urls" cases.
 */
class HostedGitInfoTest {

    @Test
    void parsesBunsValidUrls() {
        Json.Expr cases = Json.parse(BunTestData.resource("hosted-git-info-valid"));
        int count = 0;
        List<String> failures = new ArrayList<>();
        for (Json.Property provider : cases.properties()) {
            for (Json.Property urlCase : provider.value.properties()) {
                count++;
                String url = urlCase.key.asString();
                Json.Expr expected = urlCase.value;
                HostedGitInfo info = HostedGitInfo.fromUrl(url);
                if (info == null) {
                    failures.add(url + ": not parsed");
                    continue;
                }
                check(failures, url, "type", expected, info.hostProvider.name().toLowerCase(Locale.ROOT));
                check(failures, url, "domain", expected, info.hostProvider.domain);
                check(failures, url, "user", expected, info.user);
                check(failures, url, "project", expected, info.project);
                check(failures, url, "committish", expected, info.committish);
                check(failures, url, "default", expected, info.defaultRepresentation.name().toLowerCase(Locale.ROOT));
            }
        }
        assertThat(count).isGreaterThan(300);
        assertThat(failures).isEmpty();
    }

    // toMatchObject only checks the fields the case gives a truthy value
    private static void check(List<String> failures, String url, String field, Json.Expr expected, String actual) {
        Json.Expr value = expected.get(field);
        String want = value == null ? null : value.asString();
        if (want == null || want.isEmpty()) {
            return;
        }
        if (!want.equals(actual)) {
            failures.add(url + ": " + field + " is " + actual + ", expected " + want);
        }
    }
}
