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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.semver.NodeSemver;

/**
 * The Node releases a package manager runs on, which stand for the host when its own version is unknown, and the
 * node-semver options its engine check reads an engines.node range with.
 */
public enum NodeHosts {

    /**
     * Below its own engines npm 11.17.0 warns and runs on (lib/cli/entry.js), as it does on node 18.20.8. No older
     * release was tried.
     */
    NPM(">=18.0.0", NodeSemver.Options.INCLUDE_PRERELEASE),

    /**
     * pnpm 11.2.2's bin/pnpm.mjs exits below Node 22.13. On Node 20 it warns, then fails to load its bundle.
     */
    PNPM(">=22.13.0", NodeSemver.Options.of(false, true, NodeSemver.Release.V7_8_0)),

    /**
     * Node 18 and up is our assumption of the releases still in use. Yarn 1.22.22 itself runs from Node 4
     * (bin/yarn.js exits below that). Its semver is loose unless strict-semver is set.
     */
    YARN_CLASSIC(">=18.0.0", NodeSemver.Options.V5_5_0_LOOSE);

    private final String releases;
    private final NodeSemver.Options semver;

    NodeHosts(String releases, NodeSemver.Options semver) {
        this.releases = releases;
        this.semver = semver;
    }

    public String releases() {
        return releases;
    }

    /**
     * TRUE when every host release satisfies {@code range}, FALSE when none does, null when some do. A range that is
     * not valid satisfies none.
     */
    public @Nullable Boolean satisfiedBy(@Nullable Object range) {
        String valid = range instanceof String ?
                NodeSemver.validRange((String) range, semver) : null;
        if (valid == null) {
            return Boolean.FALSE;
        }
        if (NodeSemver.subset(releases, valid, semver)) {
            return Boolean.TRUE;
        }
        return NodeSemver.intersects(releases, valid, semver) ? null : Boolean.FALSE;
    }
}
