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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator.Inputs;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static org.openrewrite.javascript.internal.lock.V8RegExp.DOT;

/**
 * Port of yarn src/cli/index.js (yarn 1.22.22): {@code start}'s yarn-path hand-off and {@code main}'s packageManager
 * check, without corepack.
 */
final class Cli {

    private static final Pattern RELEASE = Pattern.compile("yarn-1\\.22\\.22\\.[cm]?js");
    private static final Pattern IN_A_DIRECTORY_ABOVE = Pattern.compile("(\\.\\./)+[^/]+");
    private static final Pattern YARN_0_OR_1 = Pattern.compile("^yarn@[01]\\.");
    private static final Pattern BUILD_METADATA = Pattern.compile("\\+" + DOT + "*");

    private final Inputs inputs;

    Cli(Inputs inputs) {
        this.inputs = inputs;
    }

    /**
     * {@code start}: a yarn-path in the rc chain hands the install to the release it names, which is this port only
     * when it is a yarn 1.22.22 release the project holds.
     */
    void start(Map<String, @Nullable Object> rc) {
        Object yarnPath = Js.truthy(rc.get("yarn-path")) ? rc.get("yarn-path") : rc.get("yarnPath");
        Map<String, String> env = inputs.getEnv();
        if (!Js.truthy(yarnPath) ||
            Config.boolifyWithDefault(env == null ? null : env.get("YARN_IGNORE_PATH"), false)) {
            return;
        }
        String release = PosixPath.normalize(Js.string(yarnPath));
        if (!PosixPath.isAbsolute(release)) {
            release = PosixPath.normalize(PosixPath.join(inputs.getCwd(), release));
        }
        Set<String> paths = inputs.getPaths();
        if (yarnPath instanceof String && RELEASE.matcher(PosixPath.basename(release)).matches()) {
            if (paths == null || paths.contains(release)) {
                return;
            }
            // the host's paths cover the project and the files directly in each directory above it they reach
            if (!PosixPath.isAbsolute(release) && (!release.startsWith("../") ||
                IN_A_DIRECTORY_ABOVE.matcher(release).matches() &&
                Config.depth(singletonList(release)) <= inputs.reach())) {
                throw new JsError(JsError.ERROR, "Cannot find module '" + release + "'");
            }
        }
        throw new CannotKnow("what the release yarn-path names (" + release + ") writes: yarn 1.22.22 hands the " +
                             "install to it");
    }

    /**
     * {@code main}: the nearest package.json from where yarn runs up with a packageManager field stops yarn when that
     * field names another package manager than yarn 0 or 1.
     */
    void main() {
        Map<String, String> env = inputs.getEnv();
        if (env != null && (Js.truthy(env.get("COREPACK_ROOT")) || Js.truthy(env.get("SKIP_YARN_COREPACK_CHECK")))) {
            return;
        }
        String packageManager = findPackageManager();
        if (packageManager != null && !YARN_0_OR_1.matcher(packageManager).find()) {
            String version = BUILD_METADATA.matcher(packageManager.replaceFirst("^yarn@", "")).replaceFirst("");
            throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, "This project's package.json defines " +
                                                           "\"packageManager\": \"yarn@" + version + "\". However " +
                                                           "the current global version of Yarn is 1.22.22.");
        }
    }

    // findPackageManager from where yarn runs, which passes over a package.json JSON.parse rejects
    private @Nullable String findPackageManager() {
        Set<String> paths = inputs.getPaths();
        for (String dir = inputs.getCwd(); !dir.isEmpty(); dir = dir.substring(0, Math.max(dir.lastIndexOf('/'), 0))) {
            String text = inputs.getManifests().containsKey(dir) ? inputs.getManifests().get(dir) :
                    inputs.getConfigFiles().get(dir + "/package.json");
            CannotKnow.requireRead(text, paths, dir + "/package.json");
            String packageManager = text == null ? null : packageManager(text);
            if (packageManager != null) {
                return packageManager;
            }
        }
        String up = "";
        for (int depth = 0; depth <= inputs.reach(); depth++, up += "../") {
            String file = up + "package.json";
            String text = depth == 0 ? inputs.getManifests().get("") : inputs.getConfigFiles().get(file);
            if (text == null) {
                if (depth > 0 && paths != null && paths.contains(file)) {
                    throw CannotKnow.unread(file);
                }
                continue;
            }
            String packageManager = packageManager(text);
            if (packageManager != null) {
                return packageManager;
            }
        }
        return null;
    }

    private static @Nullable String packageManager(String text) {
        Object data;
        try {
            data = Js.jsonParse(text);
        } catch (JsError e) {
            return null;
        }
        Object packageManager = Js.truthy(data) ? Js.get(data, "packageManager") : null;
        return packageManager instanceof String ? (String) packageManager : null;
    }
}
