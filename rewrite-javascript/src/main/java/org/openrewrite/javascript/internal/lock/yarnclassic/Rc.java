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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        // rc.js reads the file raw: the tokenizer reads its \r (an invalid token before 1.1.0), and an error names the
        // raw file's lines and columns
        Object values = LockfileParser.parseLockfile(text, fileLoc, release).getObject();
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
     * commander drops with the value after it. Before 1.1.0, the args by their values, parsed before the command line
     * with what they leave of it; the arguments no option takes are under {@link #ARGS}.
     */
    static Map<String, @Nullable Object> getRcArgs(Map<String, @Nullable Object> rc, YarnClassicRelease release) {
        if (!release.buildsRcArgsByTheirOption()) {
            return parseArgs(buildRcArgsByTheirValue(rc), release);
        }
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
                        args.put("production", Config.boolify(value, release));
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
        switch (arg) {
            case "focus":
                return release.declaresFocus();
            case "registry":
                return release.declaresRegistry();
            case "update-checksums":
                return release.declaresUpdateChecksums();
            default:
                return true;
        }
    }

    /**
     * {@code buildRcArgs} before 1.1.0: each {@code --<arg>} of the chain, the wildcard's then install's, by its
     * value's type.
     */
    private static List<String> buildRcArgsByTheirValue(Map<String, @Nullable Object> rc) {
        List<String> argv = new ArrayList<>();
        for (String command : Arrays.asList("*", "install")) {
            for (String key : Js.keys(rc)) {
                Matcher keyMatch = RC_ARG.matcher(key);
                if (!keyMatch.find() || !command.equals(keyMatch.group(1) == null ? "*" : keyMatch.group(1))) {
                    continue;
                }
                String arg = keyMatch.group(2);
                Object value = rc.get(key);
                if (value instanceof String) {
                    argv.add("--" + arg);
                    argv.add((String) value);
                } else if (Boolean.TRUE.equals(value)) {
                    argv.add("--" + arg);
                } else if (Boolean.FALSE.equals(value)) {
                    argv.add("--no-" + arg);
                }
            }
        }
        return argv;
    }

    // the options of cli/index.js and install.js setFlags before 1.1.0, by what commander 2.9.0 reads after them
    private static final Set<String> FLAGS_BEFORE_1_1_0 = new HashSet<>(Arrays.asList("--verbose", "--offline",
            "--prefer-offline", "--strict-semver", "--json", "--ignore-scripts", "--har", "--ignore-platform",
            "--ignore-engines", "--ignore-optional", "--force", "--skip-integrity-check", "--check-files",
            "--no-bin-links", "--flat", "--no-lockfile", "--pure-lockfile", "--frozen-lockfile", "--link-duplicates",
            "-s", "--silent", "--no-progress", "--non-interactive", "-g", "--global", "-S", "--save", "-D",
            "--save-dev", "-P", "--save-peer", "-O", "--save-optional", "-E", "--save-exact", "-T", "--save-tilde"));
    private static final Set<String> OPTIONAL_VALUES_BEFORE_1_1_0 = new HashSet<>(Arrays.asList("--prod",
            "--production", "--emoji", "--scripts-prepend-node-path"));
    private static final Set<String> REQUIRED_VALUES_BEFORE_1_1_0 = new HashSet<>(Arrays.asList("--link-folder",
            "--global-folder", "--modules-folder", "--preferred-cache-folder", "--cache-folder", "--mutex", "--cwd",
            "--proxy", "--https-proxy", "--network-concurrency", "--network-timeout"));
    private static final Map<String, String> SHORT_FLAGS = new HashMap<>();

    static {
        String[][] shorts = {{"-s", "silent"}, {"-g", "global"}, {"-S", "save"}, {"-D", "save-dev"},
                {"-P", "save-peer"}, {"-O", "save-optional"}, {"-E", "save-exact"}, {"-T", "save-tilde"}};
        for (String[] s : shorts) {
            SHORT_FLAGS.put(s[0], s[1]);
        }
    }

    // the command line the engine stands for, after its command: yarn install --ignore-scripts ...
    private static final List<String> COMMAND_LINE = Arrays.asList("--ignore-scripts", "--non-interactive",
            "--no-progress");

    /**
     * commander 2.9.0's {@code normalize} and {@code parseOptions} of the rc args before the command line, as the
     * options before 1.1.0 read them: each option's value, and the arguments no option takes under {@code args},
     * which install refuses.
     */
    private static Map<String, @Nullable Object> parseArgs(List<String> rcArgs, YarnClassicRelease release) {
        List<String> given = new ArrayList<>(rcArgs);
        given.addAll(COMMAND_LINE);
        List<String> argv = new ArrayList<>();
        for (int i = 0; i < given.size(); i++) {
            String arg = given.get(i);
            boolean afterRequired = i > 0 && REQUIRED_VALUES_BEFORE_1_1_0.contains(given.get(i - 1));
            int equals = arg.indexOf('=');
            if ("--".equals(arg)) {
                argv.addAll(given.subList(i, given.size()));
                break;
            } else if (afterRequired) {
                argv.add(arg);
            } else if (arg.length() > 1 && arg.charAt(0) == '-' && arg.charAt(1) != '-') {
                for (char c : arg.substring(1).toCharArray()) {
                    argv.add("-" + c);
                }
            } else if (arg.startsWith("--") && equals >= 0) {
                argv.add(arg.substring(0, equals));
                argv.add(arg.substring(equals + 1));
            } else {
                argv.add(arg);
            }
        }

        Map<String, @Nullable Object> flags = new LinkedHashMap<>();
        List<String> args = new ArrayList<>();
        boolean literal = false;
        for (int i = 0; i < argv.size(); i++) {
            String arg = argv.get(i);
            if (literal) {
                args.add(arg);
            } else if ("--".equals(arg)) {
                literal = true;
            } else if (FLAGS_BEFORE_1_1_0.contains(arg)) {
                String name = SHORT_FLAGS.containsKey(arg) ? SHORT_FLAGS.get(arg) : arg.substring(2);
                flags.put(camelCase(name.startsWith("no-") ? name.substring(3) : name), !name.startsWith("no-"));
            } else if (OPTIONAL_VALUES_BEFORE_1_1_0.contains(arg)) {
                String next = i + 1 < argv.size() ? argv.get(i + 1) : null;
                boolean takesNext = next != null && !(next.startsWith("-") && !"-".equals(next));
                String name = "--prod".equals(arg) ? "production" : camelCase(arg.substring(2));
                // the bare option sets true where no value was given before
                if (takesNext || !(flags.get(name) instanceof String)) {
                    Object value = takesNext ? argv.get(++i) : Boolean.TRUE;
                    flags.put(name, takesNext && "production".equals(name) &&
                                    release.boolifiesTheProductionFlag() ? Config.boolify(value, release) : value);
                }
            } else if (REQUIRED_VALUES_BEFORE_1_1_0.contains(arg)) {
                // the command line follows the rc args, so a value always does
                flags.put(camelCase(arg.substring(2)), argv.get(++i));
            } else if (arg.length() > 1 && arg.charAt(0) == '-') {
                // an option commander does not know takes what follows it unless that looks like an option
                if (i + 1 < argv.size() && !argv.get(i + 1).isEmpty() && argv.get(i + 1).charAt(0) != '-') {
                    i++;
                }
            } else {
                args.add(arg);
            }
        }
        if (flags.containsKey("cwd")) {
            throw new CannotKnow("the project yarn installs in the directory the .yarnrc's --cwd " +
                                 Js.string(flags.get("cwd")) + " names");
        }
        if (!args.isEmpty()) {
            flags.put(ARGS, args);
        }
        return flags;
    }

    /**
     * The arguments of install the rc args leave, which install refuses.
     */
    static final String ARGS = "args";

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
