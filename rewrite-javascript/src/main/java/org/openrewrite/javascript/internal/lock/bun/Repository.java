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

import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Port of bun's src/install/repository.zig {@code Repository} (bun 1.3.10), without the git operations, with
 * repository.rs and src/install_types/resolver_hooks.rs (1.4.0) beside it.
 */
final class Repository {

    /**
     * {@code Repository.Hosts}: the git hosts with the top-level domain of each.
     */
    static final Map<String, String> HOSTS;

    static {
        Map<String, String> hosts = new HashMap<>();
        hosts.put("bitbucket", ".org");
        hosts.put("github", ".com");
        hosts.put("gitlab", ".com");
        HOSTS = Collections.unmodifiableMap(hosts);
    }

    final String owner;
    final String repo;
    final String committish;
    final String resolved;
    final String packageName;

    Repository(String owner, String repo, String committish, String resolved, String packageName) {
        this.owner = owner;
        this.repo = repo;
        this.committish = committish;
        this.resolved = resolved;
        this.packageName = packageName;
    }

    Repository withResolved(String resolved) {
        return new Repository(owner, repo, committish, resolved, packageName);
    }

    /**
     * {@code is_safe_resolved_tag} (src/install/repository.rs, from 1.4.0): a lock's {@code .bun-tag} is one path
     * component of at most 256 letters, digits, {@code -}, {@code _} and {@code .}, neither {@code .} nor {@code ..},
     * that does not start with {@code -}.
     */
    static boolean isSafeResolvedTag(String resolved) {
        if (resolved.isEmpty() || resolved.length() > 256 || resolved.charAt(0) == '-' || ".".equals(resolved) ||
            "..".equals(resolved)) {
            return false;
        }
        for (int i = 0; i < resolved.length(); i++) {
            char c = resolved.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '_' ||
                  c == '.')) {
                return false;
            }
        }
        return true;
    }

    static Repository parseAppendGit(String input) {
        String remain = input;
        if (remain.startsWith("git+")) {
            remain = remain.substring("git+".length());
        }
        int hash = remain.lastIndexOf('#');
        if (hash >= 0) {
            return new Repository("", remain.substring(0, hash), remain.substring(hash + 1), "", "");
        }
        return new Repository("", remain, "", "", "");
    }

    /**
     * {@code parseAppendGithub}: the owner precedes the last {@code /}, the repo follows it up to the last {@code #}
     * (one at the start counting as none), and the committish follows that; from 1.4.0 the {@code /} is the last
     * before the {@code #}, and without one the owner is empty
     * ({@link BunRelease#splitsAGitHubResolutionAtTheSlashBeforeItsHash()}).
     *
     * @throws CannotKnow before 1.4.0 for a {@code /} after the {@code #}, which has bun slice the repo from past its
     *                    end, a length that wraps around in its release build
     */
    static Repository parseAppendGithub(String input, BunRelease release) {
        String remain = input;
        if (remain.startsWith("github:")) {
            remain = remain.substring("github:".length());
        }
        if (release.splitsAGitHubResolutionAtTheSlashBeforeItsHash()) {
            int hashIndex = remain.lastIndexOf('#');
            String beforeHash = hashIndex >= 0 ? remain.substring(0, hashIndex) : remain;
            String committish = hashIndex >= 0 ? remain.substring(hashIndex + 1) : "";
            int slashIndex = beforeHash.lastIndexOf('/');
            return new Repository(slashIndex >= 0 ? beforeHash.substring(0, slashIndex) : "",
                    beforeHash.substring(slashIndex + 1), committish, "", "");
        }
        int hash = 0;
        int slash = 0;
        for (int i = 0; i < remain.length(); i++) {
            char c = remain.charAt(i);
            if (c == '/') {
                slash = i;
            } else if (c == '#') {
                hash = i;
            }
        }
        if (hash != 0 && slash + 1 > hash) {
            throw new CannotKnow("what bun " + release + " reads of bun.lock's GitHub resolution " + input +
                                 ", whose repo it slices from past its end (a / after the #)");
        }
        String repo = hash == 0 ? remain.substring(slash + 1) : remain.substring(slash + 1, hash);
        return new Repository(remain.substring(0, slash), repo, hash != 0 ? remain.substring(hash + 1) : "", "", "");
    }

    int order(Repository rhs) {
        return order(rhs, BunRelease.PORTED);
    }

    /**
     * {@code order}: by owner, repo and committish; from 1.4.0 then by the resolved tag
     * ({@link BunRelease#ordersRepositoriesByTheirResolvedTag()}).
     */
    int order(Repository rhs, BunRelease release) {
        int o = Strings.order(owner, rhs.owner);
        if (o != 0) {
            return o;
        }
        o = Strings.order(repo, rhs.repo);
        if (o != 0) {
            return o;
        }
        o = Strings.order(committish, rhs.committish);
        if (o != 0 || !release.ordersRepositoriesByTheirResolvedTag()) {
            return o;
        }
        return Strings.order(resolved, rhs.resolved);
    }

    boolean eql(Repository rhs) {
        if (!owner.equals(rhs.owner) || !repo.equals(rhs.repo)) {
            return false;
        }
        if (resolved.isEmpty() || rhs.resolved.isEmpty()) {
            return committish.equals(rhs.committish);
        }
        return resolved.equals(rhs.resolved);
    }

    String format(String label) {
        StringBuilder out = new StringBuilder(label);
        if (!owner.isEmpty()) {
            out.append(owner).append('/');
        } else if (Dependency.isSCPLikePath(repo)) {
            out.append("ssh://");
        }
        out.append(repo);
        if (!resolved.isEmpty()) {
            out.append('#');
            int dash = resolved.lastIndexOf('-');
            out.append(dash >= 0 ? resolved.substring(dash + 1) : resolved);
        } else if (!committish.isEmpty()) {
            out.append('#').append(committish);
        }
        return out.toString();
    }
}
