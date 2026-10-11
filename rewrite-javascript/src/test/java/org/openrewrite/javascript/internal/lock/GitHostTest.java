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
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.GitHost.Opts;
import org.openrewrite.javascript.internal.lock.UpstreamCalls.Call;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * hosted-git-info 9.0.3's and 4.1.0's own tests ({@code upstream/hosted-git-info-*}), and 9.0.3's URL tables
 * ({@code npm-arborist/generate/hosted-git-tables.js}), invalid.js, parse-url.js and string-method cases.
 */
class GitHostTest {

    private static final List<String> METHODS = Arrays.asList("GitHost#hash", "GitHost#ssh", "GitHost#sshurl",
      "GitHost#https", "GitHost#git", "GitHost#shortcut", "GitHost#path", "GitHost#getDefaultRepresentation",
      "GitHost#toString");

    static Stream<Arguments> calls() {
        List<Arguments> out = new ArrayList<>();
        for (GitHost.Release release : GitHost.Release.values()) {
            for (Call call : UpstreamCalls.read(resource(release))) {
                if (notReplayed(call) == null) {
                    out.add(Arguments.of(release, call));
                }
            }
        }
        return out.stream();
    }

    private static String resource(GitHost.Release release) {
        return "hosted-git-info-" + release.name().substring(1).replace('_', '.') + ".jsonl.gz";
    }

    // why the port does not answer a row, or null when it does
    private static @Nullable String notReplayed(Call call) {
        if (call.getTest().startsWith("test/localhost.js")) {
            return "a host the test adds to hosted-git-info's own table";
        }
        if ("fromUrl".equals(call.getFn())) {
            return null;
        }
        if (!METHODS.contains(call.getFn())) {
            return call.getFn() + ": npm-package-arg never calls it";
        }
        Object opts = call.arg(0);
        return opts instanceof Map && !((Map<?, ?>) opts).keySet().stream()
          .allMatch(k -> "noGitPlus".equals(k) || "noCommittish".equals(k)) ?
          "options npm-package-arg never passes (committish)" : null;
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("calls")
    void answersAsTheReleaseDoes(GitHost.Release release, Call call) {
        if ("fromUrl".equals(call.getFn())) {
            Object url = call.arg(0);
            GitHost parsed = GitHost.fromUrl(url instanceof String ? (String) url : null, Opts.NONE, release);
            if (!(call.getOut() instanceof Map)) {
                assertThat(parsed).isNull();
                return;
            }
            assertThat(parsed).isNotNull();
            Map<?, ?> out = (Map<?, ?>) call.getOut();
            for (String key : new String[]{"type", "user", "auth", "project", "committish", "default"}) {
                Object want = out.get(key);
                assertThat(field(parsed, key)).as(key).isEqualTo(want == EcmaScript.UNDEFINED ? null : want);
            }
            return;
        }
        List<?> from = (List<?>) ((Map<?, ?>) Objects.requireNonNull(call.getSelf())).get("from");
        GitHost host = Objects.requireNonNull(GitHost.fromUrl((String) from.get(0), Opts.NONE, release));
        Object args = call.arg(0);
        Opts opts = args instanceof Map ? Opts.of((Boolean) ((Map<?, ?>) args).get("noGitPlus"),
          (Boolean) ((Map<?, ?>) args).get("noCommittish")) : Opts.NONE;
        assertThat(method(host, call.getFn(), opts)).isEqualTo(call.getOut());
    }

    private static @Nullable String method(GitHost host, String fn, Opts opts) {
        switch (fn) {
            case "GitHost#hash":
                return host.hash();
            case "GitHost#ssh":
                return host.ssh(opts);
            case "GitHost#sshurl":
                return host.sshurl(opts);
            case "GitHost#https":
                return host.https(opts);
            case "GitHost#git":
                return host.git(opts);
            case "GitHost#shortcut":
                return host.shortcut(opts);
            case "GitHost#path":
                return host.path(opts);
            case "GitHost#getDefaultRepresentation":
                return host.getDefaultRepresentation();
            default:
                return host.toString(opts);
        }
    }

    @Test
    void namesEveryUpstreamCallItDoesNotReplay() {
        Map<String, Integer> notReplayed = new TreeMap<>();
        for (GitHost.Release release : GitHost.Release.values()) {
            for (Call call : UpstreamCalls.read(resource(release))) {
                String reason = notReplayed(call);
                if (reason != null) {
                    notReplayed.merge(release + " " + reason, 1, Integer::sum);
                }
            }
        }
        Map<String, Integer> expected = new TreeMap<>();
        for (GitHost.Release release : GitHost.Release.values()) {
            boolean v9 = release == GitHost.Release.V9_0_3;
            String never = ": npm-package-arg never calls it";
            expected.put(release + " GitHost#browse" + never, v9 ? 36 : 30);
            expected.put(release + " GitHost#bugs" + never, 5);
            expected.put(release + " GitHost#docs" + never, 14);
            expected.put(release + " GitHost#file" + never, 20);
            expected.put(release + " GitHost#tarball" + never, v9 ? 9 : 8);
            if (v9) {
                expected.put(release + " GitHost#browseFile" + never, 4);
                expected.put(release + " GitHost#edit" + never, 11);
            }
            expected.put(release + " a host the test adds to hosted-git-info's own table", 2);
            expected.put(release + " options npm-package-arg never passes (committish)", 1);
        }
        assertThat(notReplayed).isEqualTo(expected);
    }

    static Stream<Arguments> tables() {
        try (InputStream is = GitHostTest.class.getResourceAsStream("/npm-arborist/hosted-git-tables")) {
            List<Arguments> rows = new ArrayList<>();
            for (JsonNode row : new ObjectMapper().readTree(is)) {
                rows.add(Arguments.of(row.get("file").asText(), row.get("url").asText(), row.get("expected")));
            }
            return rows.stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("tables")
    void table(String file, String url, JsonNode expected) {
        GitHost parsed = GitHost.fromUrl(url);
        if (expected.isNull()) {
            assertThat(parsed).as(url + " returns undefined").isNull();
            return;
        }
        assertThat(parsed).as(url + " parses").isNotNull();
        for (Iterator<Map.Entry<String, JsonNode>> it = expected.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> field = it.next();
            JsonNode want = field.getValue();
            assertThat(field(parsed, field.getKey())).as(url + " (" + field.getKey() + ")")
              .isEqualTo(want.isNull() ? null : want.asText());
        }
    }

    private static @Nullable String field(GitHost host, String key) {
        switch (key) {
            case "type":
                return host.getType();
            case "user":
                return host.getUser();
            case "project":
                return host.getProject();
            case "default":
                return host.getDefaultRepresentation();
            case "committish":
                return host.getCommittish();
            case "auth":
                return host.getAuth();
            default:
                throw new IllegalArgumentException(key);
        }
    }

    @Test
    void invalidResultsParseToUndefined() {
        for (String url : new String[]{"https://google.com", "git+ssh://git@nothosted.com/abc/def",
            "git://nothosted.com",
          "git+file:///foo/bar", "git+ssh://git@git.unlucky.com:RND/electron-tools/some-tool#2.0.1", "::", "", null}) {
            assertThat(GitHost.fromUrl(url)).as(url + " returns undefined").isNull();
        }
    }

    @Test
    void parseUrl() {
        assertThat(GitHost.parseUrl("git+ssh://git@abc:frontend/utils.git#6d45447e0c5eb6cd2e3edf05a8c5a9bb81950c79",
            false))
          .as("git+ssh url").isNotNull();
        assertThat(GitHost.parseUrl("file:../../../global-prefix/lib/node_modules/@myscope/bar", false))
          .as("file url").isNotNull();
        WhatwgUrl custom = GitHost.parseUrl("foobar://user:host@path", false);
        assertThat(custom).isNotNull();
        assertThat(custom.getProtocol()).isEqualTo("foobar:");
    }

    @Test
    void toStringRespectsDefaults() {
        assertDefault("git+ssh://github.com/foo/bar", "sshurl", "sshurl");
        assertDefault("https://github.com/foo/bar", "https", "https");
        assertDefault("http://github.com/foo/bar", "http", "sshurl");
        assertDefault("git://github.com/foo/bar", "git", "git");
        assertDefault("github:foo/bar", "shortcut", "shortcut");
        for (String host : new String[]{"gitlab.com", "bitbucket.org"}) {
            assertDefault("git+ssh://" + host + "/foo/bar", "sshurl", "sshurl");
            assertDefault("https://" + host + "/foo/bar", "https", "https");
        }
        assertDefault("gitlab:foo/bar", "shortcut", "shortcut");
        assertDefault("bitbucket:foo/bar", "shortcut", "shortcut");
        assertDefault("git+ssh://gist.github.com/foo/feedbeef", "sshurl", "sshurl");
        assertDefault("https://gist.github.com/foo/feedbeef", "https", "https");
        assertDefault("gist:feedbeef", "shortcut", "shortcut");
        assertDefault("git+ssh://git.sr.ht/~foo/bar", "sshurl", "sshurl");
        assertDefault("https://git.sr.ht/~foo/bar", "https", "https");
        assertDefault("sourcehut:~foo/bar", "shortcut", "shortcut");
    }

    private static void assertDefault(String url, String expectedDefault, String representation) {
        GitHost parsed = GitHost.fromUrl(url);
        assertThat(parsed).as(url).isNotNull();
        assertThat(parsed.getDefaultRepresentation()).as(url).isEqualTo(expectedDefault);
        String expected;
        switch (representation) {
            case "sshurl":
                expected = parsed.sshurl(Opts.NONE);
                break;
            case "https":
                expected = parsed.https(Opts.NONE);
                break;
            case "git":
                expected = parsed.git(Opts.NONE);
                break;
            default:
                expected = parsed.shortcut(Opts.NONE);
                break;
        }
        assertThat(parsed.toString(Opts.NONE)).as(url).isEqualTo(expected);
    }

    @Test
    void stringMethodsGithub() {
        GitHost parsed = GitHost.fromUrl("git+ssh://github.com/foo/bar");
        assertThat(parsed.hash()).isEmpty();
        assertThat(parsed.ssh(Opts.NONE)).isEqualTo("git@github.com:foo/bar.git");
        assertThat(parsed.sshurl(Opts.NONE)).isEqualTo("git+ssh://git@github.com/foo/bar.git");
        assertThat(parsed.https(Opts.NONE)).isEqualTo("git+https://github.com/foo/bar.git");
        assertThat(parsed.shortcut(Opts.NONE)).isEqualTo("github:foo/bar");
        assertThat(parsed.path(Opts.NONE)).isEqualTo("foo/bar");
        assertThat(parsed.git(Opts.NONE)).isEqualTo("git://github.com/foo/bar.git");

        GitHost extra = GitHost.fromUrl("https://user@github.com/foo/bar#fix/bug");
        assertThat(extra.hash()).isEqualTo("#fix/bug");
        assertThat(extra.https(Opts.NONE)).isEqualTo("git+https://user@github.com/foo/bar.git#fix/bug");
        assertThat(extra.shortcut(Opts.NONE)).isEqualTo("github:foo/bar#fix/bug");
        assertThat(extra.ssh(Opts.NONE)).isEqualTo("git@github.com:foo/bar.git#fix/bug");
        assertThat(extra.sshurl(Opts.NONE)).isEqualTo("git+ssh://git@github.com/foo/bar.git#fix/bug");
        assertThat(extra.sshurl(Opts.of(null, true))).as("noCommittish drops committish from urls")
          .isEqualTo("git+ssh://git@github.com/foo/bar.git");
        assertThat(extra.sshurl(Opts.of(true, null))).as("noGitPlus drops git+ prefix from urls")
          .isEqualTo("ssh://git@github.com/foo/bar.git#fix/bug");
    }

    @Test
    void stringMethodsGitlabAndBitbucket() {
        for (String domain : new String[]{"gitlab.com", "bitbucket.org"}) {
            String type = domain.substring(0, domain.indexOf('.'));
            GitHost parsed = GitHost.fromUrl("git+ssh://" + domain + "/foo/bar");
            assertThat(parsed.hash()).isEmpty();
            assertThat(parsed.ssh(Opts.NONE)).isEqualTo("git@" + domain + ":foo/bar.git");
            assertThat(parsed.sshurl(Opts.NONE)).isEqualTo("git+ssh://git@" + domain + "/foo/bar.git");
            assertThat(parsed.https(Opts.NONE)).isEqualTo("git+https://" + domain + "/foo/bar.git");
            assertThat(parsed.shortcut(Opts.NONE)).isEqualTo(type + ":foo/bar");
            assertThat(parsed.path(Opts.NONE)).isEqualTo("foo/bar");
            assertThat(parsed.git(Opts.NONE)).as("git() returns null").isNull();

            GitHost extra = GitHost.fromUrl("https://user@" + domain + "/foo/bar#fix/bug");
            assertThat(extra.hash()).isEqualTo("#fix/bug");
            assertThat(extra.https(Opts.NONE)).isEqualTo("git+https://user@" + domain + "/foo/bar.git#fix/bug");
            assertThat(extra.shortcut(Opts.NONE)).isEqualTo(type + ":foo/bar#fix/bug");
            assertThat(extra.ssh(Opts.NONE)).isEqualTo("git@" + domain + ":foo/bar.git#fix/bug");
            assertThat(extra.sshurl(Opts.NONE)).isEqualTo("git+ssh://git@" + domain + "/foo/bar.git#fix/bug");
            assertThat(extra.sshurl(Opts.of(null, true))).isEqualTo("git+ssh://git@" + domain + "/foo/bar.git");
            assertThat(extra.sshurl(Opts.of(true, null))).isEqualTo("ssh://git@" + domain + "/foo/bar.git#fix/bug");
        }
    }

    @Test
    void stringMethodsGist() {
        GitHost parsed = GitHost.fromUrl("git+ssh://gist.github.com/foo/feedbeef");
        assertThat(parsed.hash()).isEmpty();
        assertThat(parsed.ssh(Opts.NONE)).isEqualTo("git@gist.github.com:feedbeef.git");
        assertThat(parsed.sshurl(Opts.NONE)).isEqualTo("git+ssh://git@gist.github.com/feedbeef.git");
        assertThat(parsed.https(Opts.NONE)).isEqualTo("git+https://gist.github.com/feedbeef.git");
        assertThat(parsed.shortcut(Opts.NONE)).isEqualTo("gist:feedbeef");
        assertThat(parsed.path(Opts.NONE)).isEqualTo("feedbeef");
        assertThat(parsed.git(Opts.NONE)).isEqualTo("git://gist.github.com/feedbeef.git");

        GitHost extra = GitHost.fromUrl("https://user@gist.github.com/foo/feedbeef#fix/bug");
        assertThat(extra.hash()).isEqualTo("#fix/bug");
        assertThat(extra.https(Opts.NONE)).isEqualTo("git+https://gist.github.com/feedbeef.git#fix/bug");
        assertThat(extra.shortcut(Opts.NONE)).isEqualTo("gist:feedbeef#fix/bug");
        assertThat(extra.ssh(Opts.NONE)).isEqualTo("git@gist.github.com:feedbeef.git#fix/bug");
        assertThat(extra.sshurl(Opts.NONE)).isEqualTo("git+ssh://git@gist.github.com/feedbeef.git#fix/bug");
        assertThat(extra.sshurl(Opts.of(null, true))).isEqualTo("git+ssh://git@gist.github.com/feedbeef.git");
        assertThat(extra.sshurl(Opts.of(true, null))).isEqualTo("ssh://git@gist.github.com/feedbeef.git#fix/bug");
    }

    @Test
    void stringMethodsSourcehut() {
        GitHost parsed = GitHost.fromUrl("git+ssh://git.sr.ht/~foo/bar");
        assertThat(parsed.hash()).isEmpty();
        assertThat(parsed.ssh(Opts.NONE)).isEqualTo("git@git.sr.ht:~foo/bar.git");
        assertThat(parsed.sshurl(Opts.NONE)).isEqualTo("git+ssh://git@git.sr.ht/~foo/bar.git");
        assertThat(parsed.https(Opts.NONE)).isEqualTo("https://git.sr.ht/~foo/bar");
        assertThat(parsed.shortcut(Opts.NONE)).isEqualTo("sourcehut:~foo/bar");
        assertThat(parsed.path(Opts.NONE)).isEqualTo("~foo/bar");
        assertThat(parsed.git(Opts.NONE)).as("git() returns null").isNull();

        GitHost extra = GitHost.fromUrl("https://@git.sr.ht/~foo/bar#fix/bug");
        assertThat(extra.hash()).isEqualTo("#fix/bug");
        assertThat(extra.https(Opts.NONE)).isEqualTo("https://git.sr.ht/~foo/bar#fix/bug");
        assertThat(extra.shortcut(Opts.NONE)).isEqualTo("sourcehut:~foo/bar#fix/bug");
        assertThat(extra.ssh(Opts.NONE)).isEqualTo("git@git.sr.ht:~foo/bar.git#fix/bug");
        assertThat(extra.sshurl(Opts.NONE)).isEqualTo("git+ssh://git@git.sr.ht/~foo/bar.git#fix/bug");
        assertThat(extra.sshurl(Opts.of(null, true))).isEqualTo("git+ssh://git@git.sr.ht/~foo/bar.git");
        assertThat(extra.sshurl(Opts.of(true, null))).isEqualTo("ssh://git@git.sr.ht/~foo/bar.git#fix/bug");
    }
}
