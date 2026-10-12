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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PosixPath;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.V8RegExp.DOT;

/**
 * Port of yarn src/rc.js and util/rc.js (yarn 1.22.22): the .yarnrc chain and the command line it adds.
 */
final class Rc {

    static final String HOME = "~/";

    private static final List<String> RC_FILES = Arrays.asList(".yarnrc", ".yarnrc.yml");
    private static final List<String> PATH_KEYS = Arrays.asList("yarn-path", "cache-folder", "global-folder",
            "modules-folder", "cwd", "offline-cache-folder");
    private static final Pattern PATH_KEY_PREFIX = Pattern.compile("^(--)?([^.]+\\.)*");
    private static final Pattern RC_ARG = Pattern.compile("^--(?:([^.]+)\\.)?(" + DOT + "*)\\z");

    private Rc() {
    }

    /**
     * {@code getRcConfigForCwd}: each directory's .yarnrc then .yarnrc.yml from the topmost down, then the home
     * directory's ({@code home}, by name), read before anything else yarn reads.
     */
    static Map<String, @Nullable Object> getRcConfigForCwd(Map<String, String> configFiles,
                                                           Map<String, String> home) {
        return getRcConfigForCwd(configFiles, home, YarnClassicRelease.PORTED);
    }

    /**
     * {@code getRcConfigForCwd} as {@code release} reads the chain.
     */
    static Map<String, @Nullable Object> getRcConfigForCwd(Map<String, String> configFiles,
                                                           Map<String, String> home, YarnClassicRelease release) {
        Map<String, @Nullable Object> rc = new LinkedHashMap<>();
        List<String> inEachDirectory = !release.readsYarnrcYml() ? RC_FILES.subList(0, 1) :
                release.readsYarnrcYmlAfterYarnrc() ? RC_FILES : Arrays.asList(RC_FILES.get(1), RC_FILES.get(0));
        for (int depth = Config.depth(configFiles.keySet()); depth >= 0; depth--) {
            for (String name : inEachDirectory) {
                String text = configFiles.get(Config.up(depth) + name);
                if (text != null) {
                    Js.assign(rc, parseRcPath(text, Config.up(depth) + name, release));
                }
            }
        }
        for (String name : release.readsYarnrcYml() ? RC_FILES : RC_FILES.subList(0, 1)) {
            String text = home.get(name);
            if (text != null) {
                Js.assign(rc, parseRcPath(text, HOME + name, release));
            }
        }
        return rc;
    }

    // util/rc.js parseRcPaths: what reading a file throws, from 1.22.1; before, a file that throws counts as empty
    private static @Nullable Object parseRcPath(String text, String filePath, YarnClassicRelease release) {
        try {
            return loadRcFile(text, filePath, release);
        } catch (JsError | MessageError e) {
            if (release.throwsRcFileErrors()) {
                throw e;
            }
            return new LinkedHashMap<String, Object>();
        }
    }

    /**
     * {@code loadRcFile}, its directory keys resolved against the file's directory as paths relative to the
     * project's root, or to the home directory ({@code ~/}), whose files the engine does not see.
     */
    static @Nullable Object loadRcFile(String text, String filePath) {
        return loadRcFile(text, filePath, YarnClassicRelease.PORTED);
    }

    /**
     * {@code loadRcFile} of {@code release}: a .yarnrc.yml is parsed as YAML and keeps its settings from 1.22.1;
     * before, it is parsed as a .yarnrc and keeps its {@code yarnPath} alone.
     */
    @SuppressWarnings("unchecked")
    static @Nullable Object loadRcFile(String text, String filePath, YarnClassicRelease release) {
        String fileLoc = release.parsesRcFilesByTheirPath() ? PosixPath.basename(filePath) : "yarnrc";
        Object values = LockfileParser.parseLockfile(text.replace("\r\n", "\n"), fileLoc, release).getObject();
        if (filePath.endsWith(".yml")) {
            if (!release.keepsYarnrcYmlSettings()) {
                Map<String, @Nullable Object> path = new LinkedHashMap<>();
                path.put(release.resolvesYarnrcYmlYarnPath() ? "yarn-path" : "yarnPath", yarnPathOf(values));
                values = path;
            } else if ((Js.truthy(values) || !release.readsYarnPathOfAValue()) &&
                       yarnPathOf(values) instanceof String) {
                Map<String, @Nullable Object> path = new LinkedHashMap<>();
                path.put("yarn-path", yarnPathOf(values));
                values = path;
            }
        }
        if (values instanceof Map) {
            Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) values;
            for (String key : Js.keys(map)) {
                String name = PATH_KEY_PREFIX.matcher(key).replaceFirst("");
                if (!PATH_KEYS.contains(name) ||
                    "offline-cache-folder".equals(name) && !release.resolvesOfflineCacheFolder()) {
                    continue;
                }
                if (filePath.startsWith(HOME) && "yarn-path".equals(name) && map.get(key) instanceof String) {
                    throw new CannotKnow("what the release " + Js.string(map.get(key)) + " that " + filePath +
                                         " names writes: yarn " + release + " hands the install to it");
                }
                map.put(key, resolve(PosixPath.dirname(filePath), map.get(key)));
            }
        }
        return values;
    }

    // values.yarnPath, which throws where the file's document is no value (undefined)
    private static @Nullable Object yarnPathOf(@Nullable Object values) {
        if (values == null) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of undefined (reading 'yarnPath')");
        }
        return Js.get(values, "yarnPath");
    }

    // path.resolve(dirname(filePath), value) relative to the project's root
    private static String resolve(String dir, @Nullable Object value) {
        if (!(value instanceof String)) {
            throw Js.invalidArgType("The \"paths[1]\" argument must be of type string", value);
        }
        String path = (String) value;
        String resolved = PosixPath.isAbsolute(path) ? PosixPath.normalize(path) : PosixPath.join(dir, path);
        return resolved.length() > 1 && resolved.endsWith("/") ? resolved.substring(0, resolved.length() - 1) :
                resolved;
    }

    /**
     * {@code getRcArgs} for install as commander sets its flags: a {@code --<option> <value>} line passes the value,
     * a flag passes itself when the value is true.
     */
    static Map<String, @Nullable Object> getRcArgs(Map<String, @Nullable Object> rc) {
        return getRcArgs(rc, YarnClassicRelease.PORTED);
    }

    /**
     * {@code getRcArgs} as {@code release}'s commander sets the flags: an option the release does not declare is one
     * commander drops with the value after it.
     */
    static Map<String, @Nullable Object> getRcArgs(Map<String, @Nullable Object> rc, YarnClassicRelease release) {
        Map<String, @Nullable Object> args = new LinkedHashMap<>();
        for (String command : Arrays.asList("*", "install")) {
            for (String key : Js.keys(rc)) {
                Matcher keyMatch = RC_ARG.matcher(key);
                if (!keyMatch.find()) {
                    continue;
                }
                String commandName = keyMatch.group(1) == null ? "*" : keyMatch.group(1);
                if (!commandName.equals(command)) {
                    continue;
                }
                String arg = keyMatch.group(2);
                Object value = rc.get(key);
                if (!declares(arg, release)) {
                    continue;
                }
                switch (arg) {
                    case "prod":
                    case "production":
                        args.put("production", Config.boolify(value));
                        break;
                    case "registry":
                    case "modules-folder":
                    case "cache-folder":
                        args.put(camelCase(arg), value);
                        break;
                    case "no-lockfile":
                        if (Boolean.TRUE.equals(value)) {
                            args.put("lockfile", false);
                        }
                        break;
                    case "cwd":
                        throw new CannotKnow("the project yarn installs in the directory the .yarnrc's --cwd " +
                                             Js.string(value) + " names");
                    default:
                        if (Boolean.TRUE.equals(value)) {
                            args.put(camelCase(arg), true);
                        }
                }
            }
        }
        return args;
    }

    // whether the release's commander knows an option the port reads: cli/index.js's and install.js setFlags'
    private static boolean declares(String arg, YarnClassicRelease release) {
        return !"focus".equals(arg) || release.declaresFocus();
    }

    private static String camelCase(String flag) {
        StringBuilder out = new StringBuilder();
        boolean upper = false;
        for (char c : flag.toCharArray()) {
            if (c == '-') {
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString();
    }
}
