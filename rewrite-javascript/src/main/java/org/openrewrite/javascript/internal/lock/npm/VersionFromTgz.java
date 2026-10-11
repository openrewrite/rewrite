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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.regex.Pattern;

import static org.openrewrite.javascript.semver.NodeSemver.Options.DEFAULT;

/**
 * Port of @npmcli/arborist lib/version-from-tgz.js (npm 11.17.0).
 */
final class VersionFromTgz {

    private static final Pattern SLASH = Pattern.compile("/|%2f", Pattern.CASE_INSENSITIVE);

    @Value
    static class NameVersion {
        String name;
        String version;
    }

    private VersionFromTgz() {
    }

    static @Nullable NameVersion versionFromTgz(String name, String tgz) {
        String base = PosixPath.basename(tgz);
        if (!base.endsWith(".tgz")) {
            return null;
        }

        if (tgz.startsWith("http:/") || tgz.startsWith("https:/")) {
            WhatwgUrl u = WhatwgUrl.parse(tgz);
            String[] tfsplit = u.getPathname().substring(1).split("/-/", -1);
            if (tfsplit.length > 1) {
                String afterTF = tfsplit[tfsplit.length - 1];
                if (afterTF.equals(base)) {
                    String[] preSplit = SLASH.split(tfsplit[tfsplit.length - 2], -1);
                    String project = preSplit[preSplit.length - 1];
                    String scope = preSplit.length > 1 ? preSplit[preSplit.length - 2] : null;
                    return versionFromBaseScopeName(base, scope, project);
                }
            }
        }

        String[] split = SLASH.split(name, -1);
        String project = split[split.length - 1];
        String scope = split.length > 1 ? split[split.length - 2] : null;
        return versionFromBaseScopeName(base, scope, project);
    }

    private static @Nullable NameVersion versionFromBaseScopeName(String base, @Nullable String scope, String name) {
        if (!base.startsWith(name + "-")) {
            return null;
        }
        String version = NodeSemver.valid(base.substring(name.length() + 1, base.length() - 4), DEFAULT);
        return version == null ? null :
                new NameVersion(scope != null && scope.startsWith("@") ? scope + "/" + name : name, version);
    }
}
