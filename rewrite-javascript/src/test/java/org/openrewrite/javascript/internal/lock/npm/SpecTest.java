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
package org.openrewrite.javascript.internal.lock.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * npm-package-arg 13.0.2's tests: the table rows of test/basic.js, github.js, gitlab.js and
 * bitbucket.js (dumped by .lockwork/bin/dump-npa-tables.js) and basic.js's assertions around them.
 */
class SpecTest {

    static Stream<Arguments> tables() {
        try (InputStream is = SpecTest.class.getResourceAsStream("/npm-arborist/npa-tables")) {
            List<Arguments> rows = new ArrayList<>();
            for (JsonNode row : new ObjectMapper().readTree(is)) {
                rows.add(Arguments.of(row.get("file").asText(), row.get("arg").asText(),
                  row.get("where").isNull() ? null : row.get("where").asText(), row.get("expected")));
            }
            return rows.stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("tables")
    void table(String file, String arg, @Nullable String where, JsonNode expected) {
        assertMatches(Spec.npa(arg, where), expected, arg, "basic.js".equals(file));
    }

    // basic.js asserts with t.match, where a string expectation need only be contained in the value
    private static void assertMatches(Spec spec, JsonNode expected, String label, boolean substring) {
        for (Iterator<Map.Entry<String, JsonNode>> it = expected.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> field = it.next();
            String key = field.getKey();
            JsonNode want = field.getValue();
            if ("subSpec".equals(key)) {
                assertThat(spec.getSubSpec()).as(label + " [subSpec]").isNotNull();
                assertMatches(spec.getSubSpec(), want, label + " subSpec", substring);
                continue;
            }
            if ("hosted".equals(key)) {
                assertThat(spec.getHosted()).as(label + " [hosted]").isNull();
                continue;
            }
            Object actual = field(spec, key);
            if (want.isNull()) {
                assertThat(actual).as(label + " [" + key + "]").isNull();
            } else if (want.isBoolean()) {
                assertThat(actual).as(label + " [" + key + "]").isEqualTo(want.asBoolean());
            } else {
                String text = want.asText();
                if (text.startsWith("{HOME}")) {
                    text = PosixPath.join(Spec.HOME, text.substring("{HOME}".length()));
                }
                String value = actual == null ? null : actual.toString();
                if (substring) {
                    assertThat(value).as(label + " [" + key + "]").contains(text);
                } else {
                    assertThat(value).as(label + " [" + key + "]").isEqualTo(text);
                }
            }
        }
    }

    private static @Nullable Object field(Spec spec, String key) {
        switch (key) {
            case "name":
                return spec.getName();
            case "escapedName":
                return spec.getEscapedName();
            case "scope":
                return spec.getScope();
            case "type":
                return spec.getType();
            case "saveSpec":
                return spec.getSaveSpec();
            case "fetchSpec":
                return spec.getFetchSpec();
            case "raw":
                return spec.getRaw();
            case "rawSpec":
                return spec.getRawSpec();
            case "registry":
                return spec.getRegistry();
            case "gitCommittish":
                return spec.getGitCommittish();
            case "gitRange":
                return spec.getGitRange();
            case "gitSubdir":
                return spec.getGitSubdir();
            default:
                throw new IllegalArgumentException("unexpected key " + key);
        }
    }

    @Test
    void basicAssertions() {
        assertThat(Spec.resolve("foo", "1.2.3", "/whatnot").toString()).as("parsed object").isEqualTo("foo@1.2.3");
        assertThat(Spec.npa("git+http://foo.com/bar").toString()).as("parsed git toString").isEqualTo("git+http://foo.com/bar");
        assertThat(Spec.npa("./foo/bar", "/here").getFetchSpec()).as("`where` is reused").isEqualTo("/here/foo/bar");
        assertThat(Spec.npa(Spec.npa("./foo/bar", "/here"), "/whatnot").getFetchSpec())
          .as("`where` arg overrides the one in the spec object").isEqualTo("/whatnot/foo/bar");
        assertThat(Spec.npa(Spec.npa("foo@1.2.3"), null).toString()).as("spec is passthrough").isEqualTo("foo@1.2.3");

        Spec parsedSpec = Spec.npa("./foo", "./here");
        assertThat(Spec.npa(parsedSpec, null)).as("reused if no where").isSameAs(parsedSpec);
        assertThat(Spec.npa(parsedSpec, "./here")).as("reused if where matches").isSameAs(parsedSpec);
        assertThat(Spec.npa(parsedSpec, "./there")).as("new instance if where does not match").isNotSameAs(parsedSpec);

        assertThatThrownBy(() -> Spec.npa("this is not a \0 valid package name or url")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Spec.npa("gopher://yea right")).hasMessage("Invalid URL");
        assertThatThrownBy(() -> Spec.resolve("invalid/name", "1.0.0", null)).as("Invalid names throw errrors")
          .isInstanceOf(Spec.InvalidSpecException.class);
        assertThatThrownBy(() -> Spec.npa("foo@npm:bar@npm:baz")).hasMessage("nested aliases not supported");
        assertThatThrownBy(() -> Spec.npa("foo@npm:")).hasMessage("aliases must have a name");
        assertThatThrownBy(() -> Spec.npa("foo@npm:foo/bar")).hasMessage("aliases only work for registry deps");

        assertThat(Spec.resolve("foo", "^1.2.3", "/test/a/b").getType()).isEqualTo(Spec.Type.RANGE);
        Spec fileFoo = Spec.resolve("foo", "file:foo", "/test/a/b");
        assertThat(fileFoo.getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(fileFoo.getFetchSpec()).isEqualTo("/test/a/b/foo");
        assertThat(Spec.resolve("foo", "../foo/bar", "/test/a/b").getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(Spec.resolve("foo", "file:../foo/bar", "/test/a/b").getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(Spec.resolve("foo", "file:../foo/bar.tgz", "/test/a/b").getType()).isEqualTo(Spec.Type.FILE);
        Spec noName = Spec.resolve(null, "4.0.0", "/test/a/b");
        assertThat(noName.getType()).isEqualTo(Spec.Type.VERSION);
        assertThat(noName.getName()).isNull();
        Spec fileAbc = Spec.resolve("foo", "file:abc", null);
        assertThat(fileAbc.getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(fileAbc.getRaw()).isEqualTo("foo@file:abc");
        Spec withAt = Spec.npa("./path/to/thing/package@1.2.3/");
        assertThat(withAt.getName()).isNull();
        assertThat(withAt.getType()).isEqualTo(Spec.Type.DIRECTORY);
        Spec noSlash = Spec.npa("path/to/thing/package@1.2.3");
        assertThat(noSlash.getName()).isNull();
        assertThat(noSlash.getType()).isEqualTo(Spec.Type.DIRECTORY);
    }

    @Test
    void directoryWithNonUriCompatibleComponents() {
        Spec spec = Spec.npa("/test%dir");
        assertThat(spec.getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(spec.getFetchSpec()).isEqualTo("/test%dir");
        assertThat(spec.getSaveSpec()).isEqualTo("file:/test%dir");
    }

    @Test
    void fileSpecWithNonUriCompatibleComponents() {
        Spec spec = Spec.npa("file:/test%dir");
        assertThat(spec.getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(spec.getFetchSpec()).isEqualTo("/test%dir");
        assertThat(spec.getSaveSpec()).isEqualTo("file:/test%dir");
    }

    // upstream sets process.cwd(), which only supplies the default `where`
    @Test
    void directoryCwdHasNonUriCompatibleComponents() {
        String where = "/tmp/ !\"$%&'()*+,-.0123456789:;<=>@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
        Spec spec = Spec.npa("./", where);
        assertThat(spec.getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(spec.getWhere()).isEqualTo(where);
        assertThat(spec.getName()).isNull();
        assertThat(spec.getRawSpec()).isEqualTo("./");
        assertThat(spec.getFetchSpec()).isEqualTo(where);
    }

    @Test
    void errorMessage() {
        assertThatThrownBy(() -> Spec.npa("lodash.has@>=^4")).hasMessage("Invalid tag name \">=^4\" of package " +
          "\"lodash.has@>=^4\": Tags may not have any characters that encodeURIComponent encodes.");
        assertThatThrownBy(() -> Spec.npa("lodash.has @^4")).hasMessage("Invalid package name \"lodash.has \" of package " +
          "\"lodash.has @^4\": name cannot contain leading or trailing spaces; name can only contain URL-friendly characters.");
        assertThatThrownBy(() -> Spec.npa("user/foo#1234::semver:^1.2.3")).hasMessage("cannot override existing committish with a semver range");
        assertThatThrownBy(() -> Spec.npa("user/foo#semver:^1.2.3::1234")).hasMessage("cannot override existing semver range with a committish");
        assertThatThrownBy(() -> Spec.npa("user/foo#path:skipped::path:dist")).hasMessage("cannot override existing path with a second path");
        assertThatThrownBy(() -> Spec.npa("user/foo#1234::5678")).hasMessage("cannot override existing committish with a second committish");
        assertThatThrownBy(() -> Spec.npa("user/foo#semver:^1.0.0::semver:^2.0.0")).hasMessage("cannot override existing semver range with a second semver range");
    }

    @Test
    void tarballRegexShouldOnlyMatchLiteralDots() {
        assertThat(Spec.npa("/path/to/package.tar.gz").getType()).isEqualTo(Spec.Type.FILE);
        assertThat(Spec.npa("/path/to/package.tgz").getType()).isEqualTo(Spec.Type.FILE);
        assertThat(Spec.npa("/path/to/package.tar").getType()).isEqualTo(Spec.Type.FILE);
        assertThat(Spec.npa("/path/to/package.tarXgz").getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(Spec.npa("/path/to/package.tar_gz").getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(Spec.npa("./package.tar gz").getType()).isEqualTo(Spec.Type.DIRECTORY);
    }

    @Test
    void posixFileSpecs() {
        Spec local = Spec.npa("./local");
        assertThat(local.getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(local.getRaw()).isEqualTo("./local");
        assertThat(Spec.npa("./path\\backslash").getRawSpec()).isEqualTo("./path\\backslash");
    }
}
