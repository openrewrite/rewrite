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
        Map<String, @Nullable Object> rc = new LinkedHashMap<>();
        for (int depth = Config.depth(configFiles.keySet()); depth >= 0; depth--) {
            for (String name : RC_FILES) {
                String text = configFiles.get(Config.up(depth) + name);
                if (text != null) {
                    Js.assign(rc, loadRcFile(text, Config.up(depth) + name));
                }
            }
        }
        for (String name : RC_FILES) {
            String text = home.get(name);
            if (text != null) {
                Js.assign(rc, loadRcFile(text, HOME + name));
            }
        }
        return rc;
    }

    /**
     * {@code loadRcFile}, its directory keys resolved against the file's directory as paths relative to the
     * project's root, or to the home directory ({@code ~/}), whose files the engine does not see.
     */
    @SuppressWarnings("unchecked")
    static @Nullable Object loadRcFile(String text, String filePath) {
        Object values = LockfileParser.parseLockfile(text.replace("\r\n", "\n"), PosixPath.basename(filePath))
                .getObject();
        Object yarnPath = Js.get(values, "yarnPath");
        if (filePath.endsWith(".yml") && Js.truthy(values) && yarnPath instanceof String) {
            Map<String, @Nullable Object> path = new LinkedHashMap<>();
            path.put("yarn-path", yarnPath);
            values = path;
        }
        if (values instanceof Map) {
            Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) values;
            for (String key : Js.keys(map)) {
                String name = PATH_KEY_PREFIX.matcher(key).replaceFirst("");
                if (!PATH_KEYS.contains(name)) {
                    continue;
                }
                if (filePath.startsWith(HOME) && "yarn-path".equals(name)) {
                    throw new CannotKnow("what the release " + Js.string(map.get(key)) + " that " + filePath +
                                         " names writes: yarn 1.22.22 hands the install to it");
                }
                map.put(key, resolve(PosixPath.dirname(filePath), map.get(key)));
            }
        }
        return values;
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
