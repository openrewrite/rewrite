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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.lock.UpstreamCalls.Call;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * npm-package-arg 13.0.2's tests: the table rows of test/basic.js, github.js, gitlab.js and
 * bitbucket.js (dumped by npm-arborist/generate/npa-tables.js) and basic.js's assertions around them.
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
            case "where":
                return spec.getWhere();
            case "alias":
                return spec.getAlias();
            default:
                throw new IllegalArgumentException("unexpected key " + key);
        }
    }

    // not an upstream case: npm-package-arg 13.0.2's own answers for an empty first committish segment
    @Test
    void emptyCommittishSegment() {
        Spec ranged = Spec.npa("foo@github:u/r#::semver:^1", "/");
        assertThat(ranged.getType()).isEqualTo(Spec.Type.GIT);
        assertThat(ranged.getGitRange()).isEqualTo("^1");
        assertThat(ranged.getGitCommittish()).isEmpty();
        assertThat(ranged.getSaveSpec()).isEqualTo("github:u/r#::semver:^1");
        assertThat(ranged.getFetchSpec()).isNull();

        assertThat(Spec.npa("foo@github:u/r#::a", "/").getGitCommittish()).isEqualTo("a");

        Spec subdir = Spec.npa("foo@github:u/r#::path:sub", "/");
        assertThat(subdir.getGitCommittish()).isEmpty();
        assertThat(subdir.getGitSubdir()).isEqualTo("/sub");

        Spec scp = Spec.npa("foo@git+ssh://git@example.com:u/r#::semver:^1", "/");
        assertThat(scp.getGitRange()).isEqualTo("^1");
        assertThat(scp.getGitCommittish()).isEmpty();
        assertThat(scp.getFetchSpec()).isEqualTo("git@example.com:u/r");
    }

    @Test
    void basicAssertions() {
        assertThat(Spec.resolve("foo", "1.2.3", "/whatnot").toString()).as("parsed object").isEqualTo("foo@1.2.3");
        assertThat(Spec.npa("git+http://foo.com/bar").toString()).as("parsed git toString")
            .isEqualTo("git+http://foo.com/bar");
        assertThat(Spec.npa("./foo/bar", "/here").getFetchSpec()).as("`where` is reused").isEqualTo("/here/foo/bar");
        assertThat(Spec.npa(Spec.npa("./foo/bar", "/here"), "/whatnot").getFetchSpec())
          .as("`where` arg overrides the one in the spec object").isEqualTo("/whatnot/foo/bar");
        assertThat(Spec.npa(Spec.npa("foo@1.2.3"), null).toString()).as("spec is passthrough").isEqualTo("foo@1.2.3");

        Spec parsedSpec = Spec.npa("./foo", "./here");
        assertThat(Spec.npa(parsedSpec, null)).as("reused if no where").isSameAs(parsedSpec);
        assertThat(Spec.npa(parsedSpec, "./here")).as("reused if where matches").isSameAs(parsedSpec);
        assertThat(Spec.npa(parsedSpec, "./there")).as("new instance if where does not match").isNotSameAs(parsedSpec);

        assertThatThrownBy(() -> Spec.npa("this is not a \0 valid package name or url"))
            .isInstanceOf(IllegalArgumentException.class);
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
        String where = "/tmp/ " +
                       "!\"$%&'()*+,-.0123456789:;<=>@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
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
        assertThatThrownBy(() -> Spec.npa("lodash.has @^4"))
            .hasMessage("Invalid package name \"lodash.has \" of package " +
          "\"lodash.has @^4\": name cannot contain leading or trailing spaces; name can only contain URL-friendly " +
          "characters.");
        assertThatThrownBy(() -> Spec.npa("user/foo#1234::semver:^1.2.3"))
            .hasMessage("cannot override existing committish with a semver range");
        assertThatThrownBy(() -> Spec.npa("user/foo#semver:^1.2.3::1234"))
            .hasMessage("cannot override existing semver range with a committish");
        assertThatThrownBy(() -> Spec.npa("user/foo#path:skipped::path:dist"))
            .hasMessage("cannot override existing path with a second path");
        assertThatThrownBy(() -> Spec.npa("user/foo#1234::5678"))
            .hasMessage("cannot override existing committish with a second committish");
        assertThatThrownBy(() -> Spec.npa("user/foo#semver:^1.0.0::semver:^2.0.0"))
            .hasMessage("cannot override existing semver range with a second semver range");
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

    // test/invalid-url.js
    @Test
    void unsupportedProtocol() {
        assertThatThrownBy(() -> Spec.npa("foo@gopher://goodluckwiththat"))
          .isInstanceOf(Spec.InvalidSpecException.class)
          .satisfies(e -> assertThat(((Spec.InvalidSpecException) e).getCode()).isEqualTo("EUNSUPPORTEDPROTOCOL"));
    }

    // test/realize-package-specifier.js
    @Test
    void realizePackageSpecifier() {
        assertThat(Spec.npa("a.tar.gz", "/test/a/b").getType()).as("local tarball").isEqualTo(Spec.Type.FILE);
        assertThat(Spec.npa("d", "/test/a/b").getType()).as("remote package").isEqualTo(Spec.Type.RANGE);
        assertThat(Spec.npa("file:./a.tar.gz", "/test/a/b").getType()).as("local tarball").isEqualTo(Spec.Type.FILE);
        assertThat(Spec.npa("file:./b", "/test/a/b").getType()).as("local package directory")
            .isEqualTo(Spec.Type.DIRECTORY);
        assertThat(Spec.npa("file:./c", "/test/a/b").getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(Spec.npa("file:./d", "/test/a/b").getType()).isEqualTo(Spec.Type.DIRECTORY);
    }

    @Test
    void namedRealizePackageSpecifier() {
        assertThat(Spec.npa("a@a.tar.gz", "/test/a/b").getType()).as("named local tarball").isEqualTo(Spec.Type.FILE);
        assertThat(Spec.npa("d@d", "/test/a/b").getType()).as("remote package").isEqualTo(Spec.Type.TAG);
        assertThat(Spec.npa("a@file:./a.tar.gz", "/test/a/b").getType()).isEqualTo(Spec.Type.FILE);
        assertThat(Spec.npa("b@file:./b", "/test/a/b").getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(Spec.npa("c@file:./c", "/test/a/b").getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(Spec.npa("d@file:./d", "/test/a/b").getType()).isEqualTo(Spec.Type.DIRECTORY);
        Spec hosted = Spec.npa("e@e/2", "test/a/b");
        assertThat(hosted.getType()).as("hosted package dependency is git").isEqualTo(Spec.Type.GIT);
        assertThat(hosted.getHosted()).isNotNull();
        assertThat(hosted.getHosted().getType()).as("github package dependency").isEqualTo("github");
        assertThat(Spec.npa("e@1", "/test/a/b").getType()).as("range like specifier is never a local file")
            .isEqualTo(Spec.Type.RANGE);
        assertThat(Spec.npa("e@1.0.0", "/test/a/b").getType()).as("version like specifier is never a local file")
            .isEqualTo(Spec.Type.VERSION);
    }

    // npa.resolve(name, value)'s type, or the pnpm, Yarn or Bun protocol it rejects with EUNSUPPORTEDPROTOCOL; this
    // test's rows are printed by npm-arborist/generate/dependency-values.js
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
      catalog:                          | catalog          | catalog:
      catalog:react17                   | catalog          | catalog:
      workspace:^                       | workspace        | workspace:
      workspace:1.4.1                   | workspace        | workspace:
      npm:@acme/logger@^1.4.1           | alias            | npm:
      patch:acme-logger@1.4.1#fix.patch | patch            | patch:
      portal:../acme-logger             | portal           | portal:
      file:../acme-logger               | directory        | file:
      link:../acme-logger               | link             | link:
      jsr:@std/path@^1                  | jsr              | jsr:
      exec:./gen.js                     | exec             | exec:
      github:acme/logger                | git              | github:
      https://example.com/acme.tgz      | remote           | https:
      http://example.com/a.tgz          | remote           | http:
      git+ssh://example.com/acme.git    | git              | git+ssh:
      git+https://example.com/a.git     | git              | git+https:
      git+file:///tmp/a                 | git              | git+file:
      git:x                             | git              | git:
      user/repo                         | git              | github:
      mochajs/mocha#4727d357ea          | git              | github:
      user/repo#semver:^1.0.0           | git              | github:
      user/repo#semver:>=1 <2           | git              | github:
      git@github.com:user/repo.git      | git              | github:
      gitlab:a/b                        | git              | gitlab:
      bitbucket:a/b                     | git              | bitbucket:
      gist:abc                          | git              | gist:
      sourcehut:a/b                     | git              | sourcehut:
      ../pkg                            | directory        | file:
      ./pkg                             | directory        | file:
      /abs/pkg                          | directory        | file:
      ~/pkg                             | directory        | file:
      .local                            | directory        | file:
      C:\\pkgs\\foo                     | directory        | file:
      c:/pkgs/foo                       | directory        | file:
      foo-1.0.0.tgz                     | file             | file:
      team/sub/repo                     | directory        | file:
      @scope/pkg                        | directory        | file:
      Catalog:                          | unknown-protocol | catalog:
      gitmoji:x                         | unknown-protocol | gitmoji:
      future:x                          | unknown-protocol | future:
      ^1.4.1                            | range            | null
      ~1.4.1                            | range            | null
      1.4.1                             | version          | null
      1.x                               | range            | null
      *                                 | range            | null
      ''                                | range            | null
      latest                            | tag              | null
      next                              | tag              | null
      >=1.0.0 <2.0.0                    | range            | null
      1.2.3 - 2.3.4                     | range            | null
      1.2.3-beta.1                      | version          | null
      1.2.3+build.4                     | version          | null
      """)
    void dependencyValue(String value, String type, @Nullable String protocol) {
        Spec spec = Spec.ofDependency("acme-logger", value);
        assertThat(spec.getType()).hasToString(type);
        assertThat(spec.getProtocol()).isEqualTo(protocol);
        assertThat(spec.isVersionSpec()).isEqualTo(protocol == null);
    }

    // on POSIX a backslash is no separator, and a scheme is letters only: npa takes each for an invalid tag
    @ParameterizedTest
    @ValueSource(strings = {"foo\\bar", "git-lfs:x", "x+y:abc", "-bad:"})
    void dependencyValueNpaRejects(String value) {
        assertThatThrownBy(() -> Spec.ofDependency("acme-logger", value))
          .isInstanceOf(Spec.InvalidSpecException.class)
          .satisfies(e -> assertThat(((Spec.InvalidSpecException) e).getCode()).isEqualTo("EINVALIDTAGNAME"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
      github:me/old-fork          | true
      git+https://example.com/a   | true
      https://example.com/a.tgz   | true
      file:../a                   | true
      ../a                        | true
      foo-1.0.0.tgz               | true
      link:../a                   | true
      portal:../a                 | true
      sourcehut:a/b               | true
      catalog:                    | false
      workspace:*                 | false
      patch:a@1.0.0#./p.patch     | false
      npm:other@^1                | false
      jsr:@std/path@^1            | false
      exec:./gen.js               | false
      future:x                    | false
      ^1.0.0                      | false
      """)
    void location(String value, boolean location) {
        assertThat(Spec.ofDependency("acme-logger", value).isLocation()).isEqualTo(location);
    }

    @Test
    void catalogName() {
    }

    static Stream<Arguments> calls() {
        List<Arguments> out = new ArrayList<>();
        for (Spec.Release release : Spec.Release.values()) {
            for (Call call : UpstreamCalls.read(resource(release))) {
                if (!"toPurl".equals(call.getFn()) && !madeByTheTest(call)) {
                    out.add(Arguments.of(release, call));
                }
            }
        }
        return out.stream();
    }

    private static String resource(Spec.Release release) {
        return "npm-package-arg-" + (release == Spec.Release.PNPM_2_0_0 ? "pnpm-2.0.0" : "13.0.2") + ".jsonl.gz";
    }

    private static final String[] FIELDS = {"type", "registry", "where", "raw", "name", "escapedName", "scope",
      "rawSpec", "saveSpec", "fetchSpec", "gitRange", "gitCommittish", "gitSubdir", "alias"};

    // each release's own tests: the module loads once, before windows.js asks for Windows paths, so its rows hold
    // the POSIX answers; process.cwd() (the test directory) is the port's root
    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("calls")
    void answersAsTheReleaseDoes(Spec.Release release, Call call) {
        Map<String, @Nullable Object> threw = call.getThrew();
        if (threw != null) {
            assertThatThrownBy(() -> answer(release, call)).isInstanceOf(IllegalArgumentException.class)
              .hasMessage((String) threw.get("message"));
            return;
        }
        Object answer = answer(release, call);
        if ("Result#toString".equals(call.getFn())) {
            assertThat(answer).isEqualTo(call.getOut());
            return;
        }
        Map<?, ?> out = (Map<?, ?>) Objects.requireNonNull(call.getOut());
        Spec spec = (Spec) answer;
        for (String key : FIELDS) {
            Object want = out.containsKey(key) ? out.get(key) : EcmaScript.UNDEFINED;
            Object got = field(spec, key);
            assertThat(got == null ? null : got.toString()).as(key).isEqualTo(cwd(want));
        }
        Object hosted = out.get("hosted");
        if (hosted instanceof Map) {
            GitHost host = Objects.requireNonNull(spec.getHosted());
            assertThat(host.getType()).isEqualTo(((Map<?, ?>) hosted).get("type"));
            assertThat(host.getUser()).isEqualTo(((Map<?, ?>) hosted).get("user"));
            assertThat(host.getProject()).isEqualTo(((Map<?, ?>) hosted).get("project"));
            assertThat(host.getCommittish()).isEqualTo(cwd(((Map<?, ?>) hosted).get("committish")));
            assertThat(host.getDefaultRepresentation()).isEqualTo(((Map<?, ?>) hosted).get("default"));
        } else {
            assertThat(spec.getHosted()).isNull();
        }
    }

    private static Object answer(Spec.Release release, Call call) {
        switch (call.getFn()) {
            case "npa":
                return Spec.npa(call.arg(0), where(call, 1), release);
            case "resolve":
                return Spec.resolve(text(call.arg(0)), text(call.arg(1)), where(call, 2), release);
            default:
                return Spec.npa(call.getSelf(), null, release).toString();
        }
    }

    // the where a call passes, else the process.cwd() its test had, which a file spec's result names
    private static @Nullable String where(Call call, int i) {
        if (call.arg(i) instanceof String || !(call.getOut() instanceof Map)) {
            return where(call.arg(i));
        }
        return where(((Map<?, ?>) call.getOut()).get("where"));
    }

    // a Result the test makes with new Result(...), which no npa call returns
    private static boolean madeByTheTest(Call call) {
        return "Result#toString".equals(call.getFn()) &&
               ((Map<?, ?>) Objects.requireNonNull(call.getSelf())).get("type") == EcmaScript.UNDEFINED;
    }

    @Test
    void namesEveryUpstreamCallItDoesNotReplay() {
        assertThat(UpstreamCalls.read(resource(Spec.Release.V13_0_2)).stream()
          .filter(call -> "toPurl".equals(call.getFn()))).as("toPurl: npm never calls it on a lock").hasSize(7);
        assertThat(UpstreamCalls.read(resource(Spec.Release.V13_0_2)).stream().filter(SpecTest::madeByTheTest))
          .as("toString of a Result the test constructs").hasSize(3);
        assertThat(UpstreamCalls.read(resource(Spec.Release.PNPM_2_0_0)).stream()
          .filter(call -> "toPurl".equals(call.getFn()) || madeByTheTest(call))).isEmpty();
    }

    private static @Nullable String text(@Nullable Object value) {
        return value instanceof String ? (String) value : null;
    }

    private static @Nullable String where(@Nullable Object value) {
        return value instanceof String ? cwd(value) : null;
    }

    // a value under the test directory or the home directory as the port, whose cwd and home are its root, has it
    private static @Nullable String cwd(@Nullable Object value) {
        if (value == null || value == EcmaScript.UNDEFINED) {
            return null;
        }
        String s = value.toString();
        for (String dir : new String[]{"<checkout>/test", "<home>"}) {
            if (s.equals(dir) || s.startsWith(dir + "/")) {
                return "/" + s.substring(Math.min(s.length(), dir.length() + 1));
            }
        }
        return s;
    }

    @Test
    void posixFileSpecs() {
        Spec local = Spec.npa("./local");
        assertThat(local.getType()).isEqualTo(Spec.Type.DIRECTORY);
        assertThat(local.getRaw()).isEqualTo("./local");
        assertThat(Spec.npa("./path\\backslash").getRawSpec()).isEqualTo("./path\\backslash");
    }
}
