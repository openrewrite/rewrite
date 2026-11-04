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
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.LegacyUrl;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.regex.Pattern;

import static org.openrewrite.javascript.semver.NodeSemver.Options.DEFAULT;

/**
 * Port of @npmcli/arborist lib/version-from-tgz.js (npm 11.17.0), and of the form npm before 10.5.0 bundles, which reads
 * the tarball URL with Node's legacy {@code url.parse}.
 */
final class VersionFromTgz {

    private static final Pattern SLASH = Pattern.compile("/|%2f", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTTP_PROTOCOL = Pattern.compile("^https?:");

    @Value
    static class NameVersion {
        String name;
        String version;
    }

    private VersionFromTgz() {
    }

    static @Nullable NameVersion versionFromTgz(String name, String tgz) {
        return versionFromTgz(name, tgz, NpmRelease.PORTED);
    }

    /**
     * @throws JsError where {@code url.parse} throws, or reads a URL with an http protocol and no path, whose
     *                 {@code u.path.slice} is a TypeError (npm before 10.5.0)
     */
    static @Nullable NameVersion versionFromTgz(String name, String tgz, NpmRelease release) {
        String base = PosixPath.basename(tgz);
        if (!base.endsWith(".tgz")) {
            return null;
        }

        String path = null;
        if (release.readsTarballUrlWithUrlParse()) {
            // const u = parse(tgz); if (/^https?:/.test(u.protocol)) ... u.path
            LegacyUrl u = LegacyUrl.parse(tgz);
            if (HTTP_PROTOCOL.matcher(String.valueOf(u.getProtocol())).find()) {
                path = u.getPath();
                if (path == null) {
                    throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of null (reading 'slice')");
                }
            }
        } else if (tgz.startsWith("http:/") || tgz.startsWith("https:/")) {
            path = WhatwgUrl.parse(tgz).getPathname();
        }
        if (path != null) {
            String[] tfsplit = path.substring(Math.min(1, path.length())).split("/-/", -1);
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
