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

    /**
     * An error that ends yarn before {@code main} reports what failed: {@code start}'s catch prints its stack. The
     * hand-off to a yarn-path that does not load, a .yarnrc yarn cannot read, the manifest 1.22.20 reads first.
     */
    static final class Crash extends JsError {
        Crash(JsError error) {
            super(error.getName(), String.valueOf(error.getMessage()));
        }
    }

    /**
     * The end of a yarn that handed the install to a yarn-path whose failure it does not pass on (before 1.2.0): the
     * project is as the yarn-path left it, which node could not load.
     */
    static final class HandedOver extends RuntimeException {
        HandedOver() {
            super("yarn ends with 0 after the yarn-path it hands the install to fails to load", null, false, false);
        }
    }

    private static final Pattern RELEASE_FILE = Pattern.compile("yarn-\\d+\\.\\d+\\.\\d+\\.[cm]?js");
    private static final Pattern IN_A_DIRECTORY_ABOVE = Pattern.compile("(\\.\\./)+[^/]+");
    private static final Pattern YARN_0_OR_1 = Pattern.compile("^yarn@[01]\\.");
    private static final Pattern BUILD_METADATA = Pattern.compile("\\+" + DOT + "*");

    private final Inputs inputs;
    private final YarnClassicRelease release;

    Cli(Inputs inputs) {
        this.inputs = inputs;
        this.release = inputs.installsAs();
    }

    /**
     * {@code start}: a yarn-path in the rc chain hands the install to the file it names, which the project must hold.
     * A file named as a release ({@code yarn-1.22.19.js}, {@code .cjs}, {@code .mjs}, as {@code yarn policies
     * set-version} saves one) is the release that built the LST, whose {@code --version} answered through the same
     * hand-off; without that release, it is this port only when it is 1.22.22's. Any other file is a script whose
     * install the engine cannot know.
     */
    void start(Map<String, @Nullable Object> rc) {
        Object yarnPath = Js.truthy(rc.get("yarn-path")) || !release.followsAYarnrcsYarnPath() ? rc.get("yarn-path") :
                rc.get("yarnPath");
        Map<String, String> env = inputs.getEnv();
        String ignorePath = env == null ? null : env.get("YARN_IGNORE_PATH");
        if (!Js.truthy(yarnPath) || (release.boolifiesYarnIgnorePath() ?
                Config.boolifyWithDefault(ignorePath, false) : "1".equals(ignorePath))) {
            return;
        }
        String path = PosixPath.normalize(Js.string(yarnPath));
        if (!PosixPath.isAbsolute(path)) {
            path = PosixPath.normalize(PosixPath.join(inputs.getCwd(), path));
        }
        Set<String> paths = inputs.getPaths();
        boolean held = paths == null || paths.contains(path);
        // the host's paths cover the project and the files directly in each directory above it they reach
        if (yarnPath instanceof String && !held && !PosixPath.isAbsolute(path) && (!path.startsWith("../") ||
            IN_A_DIRECTORY_ABOVE.matcher(path).matches() && Config.depth(singletonList(path)) <= inputs.reach())) {
            if (!release.exitsWithTheYarnPathsCode()) {
                throw new HandedOver();
            }
            throw new Crash(new JsError(JsError.ERROR, "Cannot find module '" + path + "'"));
        }
        String file = PosixPath.basename(path);
        if (yarnPath instanceof String && held && (inputs.getRelease() != null ? RELEASE_FILE.matcher(file).matches() :
                file.matches("yarn-" + Pattern.quote(release.toString()) + "\\.[cm]?js"))) {
            return;
        }
        throw new CannotKnow("what the release yarn-path names (" + path + ") writes: yarn " + release + " hands " +
                             "the install to it");
    }

    /**
     * {@code main}: the nearest package.json from where yarn runs up with a packageManager field stops yarn when that
     * field names another package manager than yarn 0 or 1; 1.22.20 asks the manifest where yarn runs, before 1.22.20
     * no manifest.
     */
    void main() {
        if (!release.checksPackageManager()) {
            return;
        }
        Map<String, String> env = inputs.getEnv();
        if (env != null && (Js.truthy(env.get("COREPACK_ROOT")) ||
                            release.findsPackageManagerAbove() && Js.truthy(env.get("SKIP_YARN_COREPACK_CHECK")))) {
            return;
        }
        String packageManager = release.findsPackageManagerAbove() ? findPackageManager() :
                rootManifestPackageManager();
        if (packageManager != null && !YARN_0_OR_1.matcher(packageManager).find()) {
            String version = BUILD_METADATA.matcher(packageManager.replaceFirst("^yarn@", "")).replaceFirst("");
            throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, "This project's package.json defines " +
                                                           "\"packageManager\": \"yarn@" + version + "\". However " +
                                                           "the current global version of Yarn is " + release + ".");
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

    // 1.22.20's config.readRootManifest(): the manifest of the directory yarn runs in, read and normalized before
    // Config.init, whose error ends yarn
    private @Nullable String rootManifestPackageManager() {
        String cwd = inputs.getCwd();
        String text = inputs.getManifests().get(cwd);
        if (text == null) {
            String file = cwd.isEmpty() ? "package.json" : cwd + "/package.json";
            if (inputs.getPaths() == null || inputs.getPaths().contains(file)) {
                throw CannotKnow.unread(file);
            }
            throw new Crash(new JsError(JsError.ERROR, "Couldn't find a package.json file in " +
                                                       Js.jsonStringify(cwd.isEmpty() ? "." : cwd)));
        }
        Manifest manifest;
        try {
            manifest = Config.tryManifest(cwd, text, true, true, null);
        } catch (MessageError e) {
            throw new Crash(new JsError(JsError.ERROR, String.valueOf(e.getMessage())));
        } catch (Crash e) {
            throw e;
        } catch (JsError e) {
            throw new Crash(e);
        }
        Object packageManager = manifest.get("packageManager");
        return packageManager instanceof String ? (String) packageManager : null;
    }
}
