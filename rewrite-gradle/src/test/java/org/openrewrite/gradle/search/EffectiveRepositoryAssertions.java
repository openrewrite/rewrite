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
package org.openrewrite.gradle.search;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

final class EffectiveRepositoryAssertions {

    private static final Pattern COMMENT = Pattern.compile("/\\*~~\\(([^)]*)\\)~~>\\*/");

    private EffectiveRepositoryAssertions() {
    }

    /**
     * Builds the expected output for tests of recipes that annotate Gradle scripts with the list
     * of effective repository URLs. Extracts the leading {@code /*~~(URL[\nURL...])~~>*\/} comment
     * from {@code actual}, asserts that every URL listed in {@code mustInclude} is present, and
     * returns the extracted comment concatenated with {@code body}. Allows mirror entries
     * prepended at runtime (e.g. an Artifactory cache configured via init script in CI) to coexist
     * with the URLs the test cares about.
     *
     * <p>If {@code mustInclude} is empty, the comment is optional: when the actual output has no
     * comment (e.g. local run with no mirror configured and no repositories declared in the
     * source), {@code body} is returned unchanged.
     */
    static String expectedWithUrls(String actual, List<String> mustInclude, String body) {
        Matcher m = COMMENT.matcher(actual);
        if (!m.find()) {
            assertThat(mustInclude)
                    .as("expected URLs in effective-repositories comment, but no comment present in:%n%s", actual)
                    .isEmpty();
            return body;
        }
        String urlsBlock = m.group(1);
        for (String url : mustInclude) {
            assertThat(urlsBlock)
                    .as("URL %s missing from effective-repositories comment %s", url, m.group())
                    .contains(url);
        }
        return m.group() + body;
    }
}
