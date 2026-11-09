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

import org.openrewrite.maven.search.EffectiveMavenRepositoriesTable.Row;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.internal.StringUtils.isNullOrEmpty;

final class EffectiveRepositoryAssertions {

    private static final Pattern COMMENT = Pattern.compile("/\\*~~\\(([^)]*)\\)~~>\\*/");

    private EffectiveRepositoryAssertions() {
    }

    /**
     * Maven Central's URL as the model builds see it: with MAVEN_CENTRAL_MIRROR_* set, they use the mirror in its
     * place.
     */
    static String mavenCentral() {
        return mirrorOr("https://repo.maven.apache.org/maven2/");
    }

    /**
     * The Gradle plugin portal remains separate from Maven Central and its mirror.
     */
    static String gradlePluginPortal() {
        return "https://plugins.gradle.org/m2";
    }

    static List<Row> withoutTrailingSlashes(List<Row> rows) {
        return rows.stream()
                .map(row -> new Row(row.getPomPath(), withoutTrailingSlash(row.getRepositoryUri())))
                .collect(toList());
    }

    private static String withoutTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String mirrorOr(String url) {
        String mirror = System.getenv("MAVEN_CENTRAL_MIRROR_URL");
        if (isNullOrEmpty(mirror) || isNullOrEmpty(System.getenv("MAVEN_CENTRAL_MIRROR_USERNAME")) ||
            isNullOrEmpty(System.getenv("MAVEN_CENTRAL_MIRROR_PASSWORD"))) {
            return url;
        }
        return mirror;
    }

    /**
     * Builds the expected output for tests of recipes that annotate Gradle scripts with the list
     * of effective repository URLs. Extracts the leading {@code /*~~(URL[\nURL...])~~>*\/} comment
     * from {@code actual}, asserts that every URL listed in {@code mustInclude} is present, and
     * returns the extracted comment concatenated with {@code body}. Other repositories in the
     * comment, such as ones an init script adds, don't fail the test.
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
