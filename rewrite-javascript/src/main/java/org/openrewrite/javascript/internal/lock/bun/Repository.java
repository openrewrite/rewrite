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

/**
 * Port of bun's src/install/repository.zig {@code Repository} (bun 1.3.10), without the git operations.
 */
final class Repository {

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

    static Repository parseAppendGithub(String input) {
        String remain = input;
        if (remain.startsWith("github:")) {
            remain = remain.substring("github:".length());
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
        String repo = hash == 0 ? remain.substring(slash + 1) : remain.substring(slash + 1, hash);
        return new Repository(remain.substring(0, slash), repo, hash != 0 ? remain.substring(hash + 1) : "", "", "");
    }

    int order(Repository rhs) {
        int o = Strings.order(owner, rhs.owner);
        if (o != 0) {
            return o;
        }
        o = Strings.order(repo, rhs.repo);
        if (o != 0) {
            return o;
        }
        return Strings.order(committish, rhs.committish);
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
