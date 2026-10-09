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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.Spec;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.link;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.node;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * Port of @npmcli/arborist test/dep-valid.js; its "unsupported dependency type" case fakes a spec type
 * {@link Spec.Type} cannot hold.
 */
class DepValidTest {

    private final Node emptyRequestor = node(opts().path("/requestor"));

    private static Node child(@Nullable String name, @Nullable String version, @Nullable String resolved) {
        Map<String, Object> pkg = new LinkedHashMap<>();
        if (version != null) {
            pkg.put("version", version);
        }
        Node.Options.OptionsBuilder options = opts().pkg(pkg).resolved(resolved);
        return node(name == null ? options.path("/") : options.name(name));
    }

    private boolean depValid(Node child, @Nullable String requested, @Nullable String accept) {
        return DepValid.depValid(child, requested, accept, emptyRequestor);
    }

    @Test
    void basic() {
        assertThat(depValid(child(null, null, null), "", null)).as("* is always ok").isTrue();
        assertThat(depValid(child("kid", "1.2.3", null), "1.x", null)).as("range that is satisfied").isTrue();
        assertThat(depValid(child("kid", "2.2.3", null), "1.x", "2.x")).as("range that is acceptable").isTrue();

        Link linkToSomePath = link(opts().name("kid").realpath("/some/path"));
        assertThat(DepValid.depValid(linkToSomePath, Spec.npa("file:/some/path"), null, emptyRequestor))
          .as("links must point at intended target").isTrue();
        Link linkElsewhere = link(opts().name("kid").realpath("/some/other/path"));
        assertThat(depValid(linkElsewhere, "file:/some/path", null)).as("links must point at intended target")
          .isFalse();
        assertThat(depValid(node(opts().path("/some/path")), "file:/some/path", null)).as("file:// must be a link")
          .isFalse();

        assertThat(depValid(child("foo", "1.2.3", "git://host/repo#somebranch"), "git://host/repo#semver:1.x", null))
          .as("git url with semver range").isTrue();
        assertThat(depValid(node(opts().name("foo").pkg(obj("name", "bar", "version", "1.2.3"))), "npm:bar@1.2.3",
          null))
          .as("alias is ok").isTrue();
        assertThat(depValid(child("abbrev", null, "https://registry/abbrev-1.1.1.tgz"),
          "https://registry/abbrev-1.1.1.tgz", null))
          .as("remote url match").isTrue();
        assertThat(depValid(child("bar", null, "git+ssh://git@github.com/foo/bar"),
          "git+ssh://git@github.com/foo/bar.git", null))
          .as("matching _from saveSpec").isTrue();
        assertThat(depValid(child("bar", null, "git+ssh://git@github.com/foo/bar"),
          "git+ssh://git@github.com/bar/foo.git", null))
          .as("different repo").isFalse();
        assertThat(depValid(child("bar", null, null), "git+ssh://git@github.com/bar/foo.git", null))
          .as("missing repo").isFalse();
        assertThat(depValid(child("tarball", null, "file:/path/to/tarball.tgz"), "/path/to/tarball.tgz", null))
          .as("same tarball").isTrue();
        assertThat(depValid(child("tarball", null, "file:/path/to/other/tarball.tgz"), "/path/to/tarball.tgz", null))
          .as("different tarball").isFalse();
        assertThat(depValid(link(opts().name("tarball").realpath("/some/link/target")), "/path/to/tarball.tgz", null))
          .as("links are not tarballs").isFalse();
        assertThat(depValid(node(opts().name("tarball").pkg(obj("_requested", obj("saveSpec", "file:tarball.tgz")))),
          "./tarball.tgz", null)).as("probably the same-ish, hopefully").isTrue();
        assertThat(depValid(child("tarball", null, null), "./tarball.tgz", null)).as("too uncertain, nope").isFalse();
        assertThat(depValid(child("foo", null, "https://registry.npmjs.org/foo/foo-1.2.3.tgz"), "latest", null))
          .as("tagged registry version needs remote tarball").isTrue();
        assertThat(depValid(child("foo", null, "git+https://registry.npmjs.org/foo/foo-1.2.3.git"), "latest", null))
          .as("tagged registry version needs remote tarball, not git").isFalse();
        assertThat(depValid(child("foo", null, null), "latest", null))
          .as("tagged registry version needs remote tarball resolution").isFalse();
    }

    @Test
    void invalidTagName() {
        Node requestor = node(opts().path("/requestor"));
        assertThat(DepValid.depValid(child("kid", null, null), "!!@#$%!#@$!", null, requestor)).isFalse();
        assertThat(requestor.getErrors()).hasSize(1);
        DepValid.DependencyError error = (DepValid.DependencyError) requestor.getErrors().get(0);
        assertThat(error.getMessage()).startsWith("Invalid tag name \"!!@#$%!#@$!\"");
        assertThat(error.getDependency()).isEqualTo("kid");
        assertThat(error.getRequested()).isEqualTo("!!@#$%!#@$!");
    }

    @Test
    void invalidRequestAllTogether() {
        Node requestor = node(opts().path("/requestor"));
        assertThat(DepValid.depValid(child("kid", null, null), (String) null, null, requestor)).isFalse();
        assertThat(requestor.getErrors()).hasSize(1);
        DepValid.DependencyError error = (DepValid.DependencyError) requestor.getErrors().get(0);
        assertThat(error.getMessage()).isEqualTo("Invalid dependency specifier");
        assertThat(error.getRequested()).isNull();
        assertThat(error.getDependency()).isEqualTo("kid");
    }

    @Test
    void installLinksMakesLinkNodesInvalid() {
        Node requestor = node(opts().path("/requestor").installLinks(true));
        Link child = link(opts().name("kid").realpath("/some/path"));
        assertThat(child.isWorkspace()).isFalse();
        assertThat(DepValid.depValid(child, Spec.npa("file:/some/path"), null, requestor)).isFalse();
    }

    @Test
    void installLinksDoesNotMakeWorkspaceNodesInvalid() {
        Node root = node(opts().path("/some").installLinks(true).pkg(obj("name", "root")));
        Node target = node(opts().path("/some/path").pkg(obj("name", "kid", "version", "1.0.0")).root(root));
        Link child = link(opts().name("kid").parent(root).target(target));
        Map<String, String> workspaces = new LinkedHashMap<>();
        workspaces.put("kid", "/some/path");
        root.setWorkspaces(workspaces);
        assertThat(child.isWorkspace()).isTrue();
        assertThat(DepValid.depValid(child, Spec.npa("file:/some/path"), null, root)).isTrue();
    }

    @Test
    void sha1AndSha256() {
        String sha1 = "0d7bd85a85fa2571fa532d2fc842ed099b236ad2";
        String sha256 = "8e3a9b3579ab330238c06b761e7f1b5dc5b4ac6e5a96da4dd2fb3b7411009df8";
        assertThat(depValid(child("foo", "1.2.3", "npm/repo#" + sha1), "npm/repo#" + sha1, null))
          .as("git url with full sha-1 hash match").isTrue();
        assertThat(depValid(child("foo", "1.2.3", "npm/repo#" + sha1),
          "npm/repo#1d7bd85a85fa2571fa532d2fc842ed099b236ad2", null))
          .as("git url with full sha-1 hash mismatch").isFalse();
        assertThat(depValid(child("foo", "1.2.3", "npm/repo#" + sha256), "npm/repo#" + sha256, null))
          .as("git url with full sha-256 hash match").isTrue();
        assertThat(depValid(child("foo", "1.2.3", "npm/repo#" + sha256),
          "npm/repo#9e3a9b3579ab330238c06b761e7f1b5dc5b4ac6e5a96da4dd2fb3b7411009df8", null))
          .as("git url with full sha-256 hash mismatch").isFalse();
    }

    // the requestor is a root whose lock metadata holds the recorded entry, attached as a loaded lock is
    private static Node requestor(String depType, String recordedSpec) {
        Node requestor = node(opts().path("/some/path"));
        Shrinkwrap meta = new Shrinkwrap("/some/path", NpmOptions.DEFAULT);
        meta.setData(obj("packages", obj("", obj(depType, obj("repo", recordedSpec)))));
        requestor.setMeta(meta);
        return requestor;
    }

    @Test
    void gitTagOrBranchChangeDetectedViaLockfileCommittish() {
        Node child = child("repo", "1.0.0",
          "git+ssh://git@github.com/npm/repo.git#0d7bd85a85fa2571fa532d2fc842ed099b236ad2");

        assertThat(DepValid.depValid(child, "npm/repo#v1.0.0", null, requestor("dependencies", "npm/repo#v1.0.0")))
          .as("unchanged tag is valid").isTrue();
        assertThat(DepValid.depValid(child, "npm/repo#v2.0.0", null, requestor("dependencies", "npm/repo#v1.0.0")))
          .as("changed tag must be re-resolved").isFalse();
        assertThat(DepValid.depValid(child, "npm/repo#other", null, requestor("devDependencies", "npm/repo#main")))
          .as("changed branch in devDependencies must be re-resolved").isFalse();
        assertThat(DepValid.depValid(child, "npm/repo#v2.0.0", null,
          requestor("optionalDependencies", "npm/repo#v1.0.0")))
          .as("changed tag in optionalDependencies must be re-resolved").isFalse();
        assertThat(DepValid.depValid(child, "npm/repo#other", null, requestor("peerDependencies", "npm/repo#main")))
          .as("changed branch in peerDependencies must be re-resolved").isFalse();
        assertThat(DepValid.depValid(child, "npm/repo#v2.0.0", null, requestor("dependencies", "npm/repo")))
          .as("lockfile git spec without a committish differs from a named ref").isFalse();
        assertThat(DepValid.depValid(child, "npm/repo#v2.0.0", null, requestor("dependencies", "^1.0.0")))
          .as("non-git lockfile spec is ignored, falling back to the repo-only check").isTrue();
        assertThat(depValid(child, "npm/repo#v2.0.0", null))
          .as("without lockfile data, fall back to the repo-only check").isTrue();
        assertThat(DepValid.depValid(child, "npm/repo#v2.0.0", null,
          requestor("dependencies", "invalid spec with spaces")))
          .as("unparseable lockfile spec is ignored, falling back to the repo-only check").isTrue();
    }
}
