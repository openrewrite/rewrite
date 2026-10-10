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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port of bun's test/cli/install/semver.test.ts (bun-v1.3.10) and a sample of the scenarios' ranges, with bun's
 * answers ({@code semver-cases.tsv}); {@code BUN_SEMVER_CASES} names more rows. "expected errors" has no Java analog.
 */
class SemverTest {

    // SemverObject.zig satisfies
    static boolean satisfies(String left, String right) {
        if (!isAllAscii(left) || !isAllAscii(right)) {
            return false;
        }
        SemverVersion.ParseResult leftResult = SemverVersion.parse(left);
        if (leftResult.wildcard != SemverQuery.Wildcard.NONE) {
            return false;
        }
        SemverVersion leftVersion = leftResult.version.min();
        SemverQuery.Group rightGroup = SemverQuery.parse(right);
        SemverVersion rightVersion = exactVersion(rightGroup);
        if (rightVersion != null) {
            return leftVersion.eql(rightVersion);
        }
        return rightGroup.satisfies(leftVersion);
    }

    // SemverQuery.zig Group.getExactVersion
    private static @Nullable SemverVersion exactVersion(SemverQuery.Group group) {
        SemverRange range = group.head.head.range;
        if (group.head.next == null && group.head.head.next == null && range.hasLeft() &&
            range.left.op == SemverRange.Op.EQL && !range.hasRight()) {
            return range.left.version;
        }
        return null;
    }

    // SemverObject.zig order
    static int order(String left, String right) {
        if (!isAllAscii(left) || !isAllAscii(right)) {
            return 0;
        }
        SemverVersion.ParseResult leftResult = SemverVersion.parse(left);
        SemverVersion.ParseResult rightResult = SemverVersion.parse(right);
        if (!leftResult.valid) {
            throw new IllegalArgumentException("Invalid SemVer: " + left);
        }
        if (!rightResult.valid) {
            throw new IllegalArgumentException("Invalid SemVer: " + right);
        }
        return leftResult.version.max().orderWithoutBuild(rightResult.version.max());
    }

    private static boolean isAllAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    private static List<String> check(List<String[]> rows) {
        List<String> failures = new ArrayList<>();
        for (String[] row : rows) {
            String left = decode(row[1]);
            String right = decode(row[2]);
            String actual;
            try {
                actual = "satisfies".equals(row[0]) ? String.valueOf(satisfies(left, right)) :
                        String.valueOf(order(left, right));
            } catch (IllegalArgumentException e) {
                actual = e.getMessage();
            }
            if (!row[3].equals(actual)) {
                failures.add(row[0] + "(\"" + row[1] + "\", \"" + row[2] + "\") is " + actual + ", bun says " + row[3]);
            }
        }
        return failures;
    }

    // the recorder writes a tab, newline, carriage return or backslash in an argument as \t, \n, \r, \\
    private static String decode(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                out.append(next == 't' ? '\t' : next == 'n' ? '\n' : next == 'r' ? '\r' : next);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    @Test
    void answersAsBunDoes() {
        List<String[]> rows = BunTestData.rows("semver-cases.tsv");
        assertThat(rows).hasSizeGreaterThan(3000);
        assertThat(check(rows)).isEmpty();
    }

    @Test
    void answersAsBunDoesOverFurtherCases() throws IOException {
        String file = System.getenv("BUN_SEMVER_CASES");
        if (file == null || file.isEmpty()) {
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (String line : new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8).split("\n")) {
            if (!line.isEmpty()) {
                rows.add(line.split("\t", -1));
            }
        }
        assertThat(check(rows)).isEmpty();
    }

    @Test
    void preReleaseSnapshot() {
        List<String> unsorted = BunTestData.lines("semver-prereleases.txt");
        List<String> sorted = new ArrayList<>(unsorted);
        sorted.sort(SemverTest::order);
        assertThat(sorted).isEqualTo(BunTestData.lines("semver-prereleases-sorted.txt"));
    }
}
