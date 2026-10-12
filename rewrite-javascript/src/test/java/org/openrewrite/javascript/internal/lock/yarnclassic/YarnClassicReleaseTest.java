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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.LockFileRegeneration;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.RegistrySource;
import org.openrewrite.javascript.internal.lock.Rejected;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What each Yarn 1 release does otherwise than 1.22.22, as {@link YarnClassicRelease} answers it, each case from the
 * release's source at its tag {@code v1.x.y} of yarnpkg/yarn.
 */
class YarnClassicReleaseTest {

    private static final String ROOT = "{\"name\":\"p\",\"version\":\"1.0.0\"}";

    private static YarnClassicRelease release(String version) {
        return YarnClassicRelease.of(version);
    }

    @Test
    void theReleasesAreEveryStableYarn1TheRegistryPublishes() {
        List<YarnClassicRelease> all = YarnClassicReleases.all();
        assertThat(all).hasSize(61).startsWith(release("1.0.0")).endsWith(YarnClassicRelease.PORTED)
                .contains(release("1.22.12")).doesNotContain(release("1.20.0"));
    }

    @Test
    void installsAsAReleaseTheEngineReproducesOnly() {
        assertThat(YarnClassicReleases.installedAs("1.22.22")).isEqualTo(YarnClassicRelease.PORTED);
        assertThatThrownBy(() -> YarnClassicReleases.installedAs("0.28.4"))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("what Yarn 0.28.4 writes, since the engine reproduces Yarn " +
                                      YarnClassicReleases.ranges(YarnClassicReleases.proven()));
        assertThatThrownBy(() -> YarnClassicReleases.installedAs("1.22.12"))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("what Yarn 1.22.12 writes");
    }

    /**
     * rc.js {@code loadRcFile} and util/rc.js {@code parseRcPaths}: before 1.22.1 a .yarnrc.yml is parsed as a .yarnrc
     * and keeps its yarnPath alone, whose absence throws in path.resolve and empties the file; 1.22.1 parses it as YAML
     * and keeps its settings, and throws what a file throws; 1.22.2 reads a document of no object as an empty one.
     */
    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(delimiter = '|', value = {
            "1.22.0|registry: x|{}",
            "1.22.1|registry: x|{registry=x}",
            "1.22.0|yarnPath: y.js|{yarn-path=y.js}",
            "1.22.1|yarnPath: y.js|{yarn-path=y.js}",
            "1.22.1|registry: x\\nyarnPath: y.js|{yarn-path=y.js}",
            "1.22.0||{}",
            "1.22.1||TypeError: Cannot read properties of undefined (reading 'yarnPath')",
            "1.22.2||{}",
            "1.22.1|abc|{0=a, 1=b, 2=c}",
            "1.22.2|abc|{}",
            "1.22.0|'foo: ['|{}",
    })
    void readsAYarnrcYmlAsItsReleaseDoes(String version, @Nullable String yml, String rc) {
        String text = yml == null ? "" : yml.replace("\\n", "\n");
        assertThat(rc(version, Collections.singletonMap(".yarnrc.yml", text))).isEqualTo(rc);
    }

    /**
     * util/rc.js {@code parseRcPaths}: a .yarnrc neither parser reads counts as empty before 1.22.1, and its error ends
     * yarn from 1.22.1.
     */
    @Test
    void throwsWhatAnRcFileThrowsFrom1221() {
        Map<String, String> files = Collections.singletonMap(".yarnrc", "foo: [\n");
        assertThat(rc("1.22.0", files)).isEqualTo("{}");
        assertThat(rc("1.22.1", files)).startsWith("SyntaxError: ");
    }

    private static String rc(String version, Map<String, String> files) {
        try {
            return Rc.getRcConfigForCwd(files, Collections.emptyMap(), release(version)).toString();
        } catch (JsError e) {
            return e.toString();
        }
    }

    /**
     * cli/index.js {@code main}: no check before 1.22.20; 1.22.20 reads the manifest where yarn runs, whose error ends
     * yarn; 1.22.21 finds the field from there up, past a manifest it cannot parse, unless
     * {@code SKIP_YARN_COREPACK_CHECK}.
     */
    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(delimiter = '|', value = {
            "1.22.19|root|-|installs",
            "1.22.20|root|-|This project's package.json defines \"packageManager\": \"yarn@pnpm@9.0.0\". However the " +
            "current global version of Yarn is 1.22.20.",
            "1.22.20|parent|-|installs",
            "1.22.21|parent|-|This project's package.json defines \"packageManager\": \"yarn@pnpm@9.0.0\". However " +
            "the current global version of Yarn is 1.22.21.",
            "1.22.20|root|SKIP_YARN_COREPACK_CHECK|This project's package.json defines \"packageManager\": " +
            "\"yarn@pnpm@9.0.0\". However the current global version of Yarn is 1.22.20.",
            "1.22.21|root|SKIP_YARN_COREPACK_CHECK|installs",
            "1.22.21|root|COREPACK_ROOT|installs",
    })
    void checksThePackageManagerAsItsReleaseDoes(String version, String where, @Nullable String variable,
                                                 String outcome) {
        String manifest = "{\"name\":\"p\",\"version\":\"1.0.0\",\"packageManager\":\"pnpm@9.0.0\"}";
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .lock("# yarn lockfile v1\n\n\n")
                .env(variable == null ? Collections.emptyMap() : Collections.singletonMap(variable, "1"))
                .directory("app");
        if ("root".equals(where)) {
            inputs.manifest("", manifest);
        } else {
            inputs.manifest("", ROOT).configFile("../package.json", manifest);
        }
        assertThat(start(inputs.build())).isEqualTo(outcome);
    }

    /**
     * 1.22.20 reads the manifest where yarn runs before its install: a manifest JSON.parse rejects ends yarn there,
     * and its stack is what yarn prints; 1.22.21 passes over it.
     */
    @Test
    void readsTheManifestWhereYarnRunsFirstIn12220() {
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .manifest("", "{\"name\":\"p\",}")
                .lock("# yarn lockfile v1\n\n\n")
                .env(Collections.emptyMap());
        assertThat(start(inputs.release(release("1.22.20")).build())).startsWith("crash SyntaxError: package.json:");
        assertThat(start(inputs.release(release("1.22.21")).build())).isEqualTo("installs");
        assertThat(start(inputs.release(release("1.22.19")).build())).isEqualTo("installs");
    }

    /**
     * cli/index.js {@code start}: a file named as a release is the release that built the LST, whatever the version
     * its name gives; without that release, the port only where it is 1.22.22's; a script is no release.
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', nullValues = "-", value = {
            "1.19.1|.yarn/releases/yarn-1.19.1.cjs|installs",
            "1.19.1|.yarn/releases/yarn-1.22.22.cjs|installs",
            "-|.yarn/releases/yarn-1.22.22.cjs|installs",
            "-|.yarn/releases/yarn-1.19.1.cjs|cannot-know what the release yarn-path names " +
            "(.yarn/releases/yarn-1.19.1.cjs) writes: yarn 1.22.22 hands the install to it",
            "1.19.1|.yarn/releases/yarn-wrapper.cjs|cannot-know what the release yarn-path names " +
            "(.yarn/releases/yarn-wrapper.cjs) writes: yarn 1.19.1 hands the install to it",
    })
    void handsTheInstallToTheReleaseThatBuiltTheLst(@Nullable String version, String yarnPath, String outcome) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(version == null ? null : release(version))
                .manifest("", ROOT)
                .lock("# yarn lockfile v1\n\n\n")
                .configFile(".yarnrc", "yarn-path \"" + yarnPath + "\"\n")
                .paths(new LinkedHashSet<>(Arrays.asList("package.json", "yarn.lock", ".yarnrc", yarnPath)))
                .env(Collections.emptyMap())
                .build();
        assertThat(start(inputs)).isEqualTo(outcome);
    }

    private static String start(YarnClassicLockRegenerator.Inputs inputs) {
        try {
            YarnClassicLockRegenerator.start(inputs);
            return "installs";
        } catch (Rejected e) {
            return e.getMessage();
        } catch (Cli.Crash e) {
            return "crash " + e;
        } catch (CannotKnow e) {
            return "cannot-know " + e.getSubject();
        }
    }

    /**
     * cli/index.js {@code main}'s report of an error that is no MessageError: onUnexpectedError's words before
     * 1.22.20, the stack from 1.22.21 (1.22.20 prints none).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "1.22.19|An unexpected error occurred: \"Cannot convert object to primitive value\".",
            "1.22.20|TypeError: Cannot convert object to primitive value",
            "1.22.21|TypeError: Cannot convert object to primitive value",
    })
    void reportsAnUnexpectedErrorAsItsReleaseDoes(String version, String reported) {
        assertThat(YarnClassicLockRegenerator.unexpectedError(new JsError(JsError.TYPE_ERROR,
                "Cannot convert object to primitive value"), release(version))).isEqualTo(reported);
    }

    /**
     * The release's own version: in the header yarn-enable-lockfile-versions writes, and as {@code engines.yarn}
     * holds it.
     */
    @Test
    void namesItsOwnVersion() {
        assertThat(LockfileStringifier.stringify(new LinkedHashMap<>(), false, true, "v24.19.0", release("1.19.1")))
                .contains("# yarn v1.19.1\n# node v24.19.0\n");
        Map<String, Object> engines = Collections.singletonMap("yarn", ">=1.22.20");
        Manifest manifest = new Manifest();
        manifest.put("name", "x");
        manifest.put("version", "1.0.0");
        manifest.put("engines", engines);
        List<String> errors = new ArrayList<>();
        new PackageCompatibility(config("1.22.20"), Host.UNKNOWN, errors).checkOne(manifest, false);
        assertThatThrownBy(() -> new PackageCompatibility(config("1.22.19"), Host.UNKNOWN, errors)
                .checkOne(manifest, false)).isInstanceOf(MessageError.class);
        assertThat(errors).containsExactly("x@1.0.0: The engine \"yarn\" is incompatible with this module. " +
                                           "Expected version \">=1.22.20\". Got \"1.22.19\"");
    }

    private static Config config(String version) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version)).manifest("", ROOT).env(Collections.emptyMap()).build();
        return new Config(inputs, YarnClassicLockRegenerator.start(inputs));
    }

    /**
     * util/rc.js {@code getRcPaths} and rc.js {@code loadRcFile}: a .yarnrc.yml is read from 1.17.1, before its
     * directory's .yarnrc and keeping its yarnPath unresolved; from 1.17.2 after it, as {@code yarn-path} resolved
     * against the file's directory.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "1.17.0|{yarn-path=../a.js}",
            "1.17.1|{yarnPath=b.js, yarn-path=../a.js}",
            "1.17.2|{yarn-path=../b.js}",
    })
    void readsAYarnrcYmlBesideTheYarnrcFrom1171(String version, String rc) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("../.yarnrc", "yarn-path \"a.js\"\n");
        files.put("../.yarnrc.yml", "yarnPath: b.js\n");
        assertThat(rc(version, files)).isEqualTo(rc);
    }

    /**
     * parse.js {@code parse}: what the lockfile parser cannot read is read with js-yaml's failsafe schema from 1.17.0;
     * lockfile/index.js {@code fromDirectory} sets a Yarn Berry lock aside from 1.19.0.
     */
    @Test
    void readsAJsonOrBerryLockAsItsReleaseDoes() {
        String json = "{\"a@^1.0.0\": {\"version\": \"1.0.0\"}}";
        assertThatThrownBy(() -> LockfileParser.parseLockfile(json, "yarn.lock", release("1.16.0")))
                .isInstanceOf(JsError.class)
                .hasMessageStartingWith("Unknown token: { line: 1, col: 0, type: 'INVALID', value: undefined } 1:0");
        assertThat(LockfileParser.parseLockfile(json, "yarn.lock", release("1.17.0")).getObject())
                .isEqualTo(Collections.singletonMap("a@^1.0.0", Collections.singletonMap("version", "1.0.0")));
        String berry = "__metadata:\n  version: 6\n\n\"a@npm:^1.0.0\":\n  version: 1.0.0\n";
        assertThat(Lockfile.fromDirectory(berry, release("1.18.0")).getLockfilev2()).isNull();
        assertThat(Lockfile.fromDirectory(berry, release("1.19.0")).getLockfilev2()).isNotNull();
    }

    /**
     * config.js: {@code unsafe-disable-integrity-migration} disables the migration where it holds a truthy value
     * before 1.12.0, and is a boolean defaulting to true from 1.12.0.
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', nullValues = "-", value = {
            "1.11.1|-|true",
            "1.11.1|true|false",
            "1.12.0|-|false",
            "1.12.0|false|true",
    })
    void migratesIntegrityAsItsReleaseDoes(String version, @Nullable String disable, boolean migrates) {
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version)).manifest("", ROOT).env(Collections.emptyMap());
        if (disable != null) {
            inputs.configFile(".yarnrc", "unsafe-disable-integrity-migration " + disable + "\n");
        }
        YarnClassicLockRegenerator.Inputs built = inputs.build();
        assertThat(new Config(built, YarnClassicLockRegenerator.start(built)).isAutoAddIntegrity())
                .isEqualTo(migrates);
    }

    /**
     * link-resolver.js {@code resolve}: a link to a directory without a package.json is a package of its name from
     * 1.17.0; before, yarn reads the directory's manifest and fails.
     */
    @Test
    void linksADirectoryWithoutAManifestFrom1170() {
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"x\":\"link:./lib\"}}")
                .lock("# yarn lockfile v1\n\n\n")
                .paths(new LinkedHashSet<>(Arrays.asList("package.json", "yarn.lock", "lib/index.js")))
                .env(Collections.emptyMap());
        assertThat(new YarnClassicLockRegenerator(inputs.release(release("1.16.0")).build(),
                YarnClassicScenarios.NO_REGISTRY).regenerate().getErrorMessage())
                .contains("Couldn't find a package.json file in \"lib\"");
        assertThat(new YarnClassicLockRegenerator(inputs.release(release("1.17.0")).build(),
                YarnClassicScenarios.NO_REGISTRY).regenerate().isSuccess()).isTrue();
    }

    /**
     * npm-resolver.js {@code findVersionInRegistryResponse}: a malformed response names the package asked for from
     * 1.12.0, the one the response names before.
     */
    @Test
    void namesThePackageOfAMalformedResponse() {
        Map<String, Object> body = Collections.singletonMap("name", "y");
        assertThatThrownBy(() -> NpmResolver.findVersionInRegistryResponse(config("1.11.1"), "x", "^1.0.0", body))
                .hasMessage("Received malformed response from registry for \"y\". The registry may be down.");
        assertThatThrownBy(() -> NpmResolver.findVersionInRegistryResponse(config("1.12.0"), "x", "^1.0.0", body))
                .hasMessage("Received malformed response from registry for \"x\". The registry may be down.");
    }

    /**
     * npm-resolver.js {@code cleanRegistry} (1.10.0) rewrites an http registry.npmjs.org url too; before, an https one
     * only.
     */
    @Test
    void cleansHttpNpmRegistryUrlsFrom1100() {
        String tarball = "http://registry.npmjs.org/a/-/a-1.0.0.tgz";
        assertThat(NpmResolver.cleanRegistry(config("1.9.4"), tarball)).isEqualTo(tarball);
        assertThat(NpmResolver.cleanRegistry(config("1.10.0"), tarball))
                .isEqualTo("https://registry.yarnpkg.com/a/-/a-1.0.0.tgz");
    }

    /**
     * npm-registry.js {@code getRequestUrl}: registry.npmjs.org and registry.yarnpkg.com over https from 1.17.3.
     */
    @Test
    void asksThePublicRegistriesOverHttpsFrom1173() {
        assertThat(NpmRegistry.getRequestUrl("http://registry.npmjs.org", "x", release("1.17.2")))
                .isEqualTo("http://registry.npmjs.org/x");
        assertThat(NpmRegistry.getRequestUrl("http://registry.npmjs.org", "x", release("1.17.3")))
                .isEqualTo("https://registry.npmjs.org/x");
    }

    /**
     * resolve-relative.js: a {@code file:./} dependency on the lockfile's directory is written {@code file:.} from
     * 1.18.0, {@code file:./.} before.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({"1.17.3,file:./.", "1.18.0,file:."})
    void writesTheLockfileFolderAsItsReleaseDoes(String version, String written) {
        Manifest info = new Manifest();
        Map<String, Object> dependencies = new LinkedHashMap<>();
        dependencies.put("self", "file:./");
        info.put("dependencies", dependencies);
        NormalizeManifest.resolveRelative(info, "", "", release(version));
        assertThat(dependencies.get("self")).isEqualTo(written);
    }

    /**
     * tarball-fetcher.js {@code createExtractor}: the sha1 hash a url names is checked beside the integrity from
     * 1.19.0; before, the integrity alone.
     */
    @Test
    void checksTheHashBesideTheIntegrityFrom1190() throws NoSuchAlgorithmException {
        byte[] tar = tar("package/package.json", "{\"name\":\"x\",\"version\":\"1.0.0\"}", null);
        PackageRemote remote = new PackageRemote();
        remote.type = "tarball";
        remote.registry = "npm";
        remote.reference = "https://registry.npmjs.org/x/-/x-1.0.0.tgz";
        remote.hash = "0000000000000000000000000000000000000000";
        remote.integrity = "sha512-" + Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-512").digest(tar));
        RegistrySource tarballs = new RegistrySource() {
            @Override
            public @Nullable String packument(String r, String name, boolean full, @Nullable String authorization) {
                return null;
            }

            @Override
            public byte[] tarball(String url, String name, String version, @Nullable String authorization) {
                return tar;
            }
        };
        assertThat(new TarballFetcher(remote, "x", "1.0.0", "dest", tarballs, config("1.18.0")).download())
                .isEqualTo(tar);
        assertThatThrownBy(() -> new TarballFetcher(remote, "x", "1.0.0", "dest", tarballs, config("1.19.0"))
                .download())
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("does not match the integrity the registry gives it");
    }

    /**
     * tarball-fetcher.js {@code createExtractor}'s map (1.22.0) keeps a link of the archive inside the package's
     * directory; before, a link out of it reaches the disk around yarn's cache.
     */
    @Test
    void keepsTheLinksOfAnArchiveInsideFrom1220() {
        byte[] tar = tar("package/package.json", null, "../../outside/package.json");
        Object jailed = TarballFetcher.manifest(TarballFetcher.untar("<url>", tar, release("1.22.0")), "", "npm",
                "fallback", "9.9.9", true);
        assertThat(((Manifest) jailed).getName()).isEqualTo("fallback");
        assertThatThrownBy(() -> TarballFetcher.manifest(TarballFetcher.untar("<url>", tar, release("1.21.1")), "",
                "npm", "fallback", "9.9.9", true))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("a link of the archive leads to");
    }

    /**
     * parse.js {@code tokenise}: 1.12.0 reads a comment or a string past the end of the input, until V8 refuses the
     * token's length (what yarn 1.12.0 printed: {@code An unexpected error occurred: "Invalid string length".}); 1.12.1
     * ends them there.
     */
    @Test
    void endsTokensAtTheEndOfInputFrom1121() {
        String comment = "# yarn lockfile v1\n\n# trailing";
        assertThatThrownBy(() -> LockfileParser.parseLockfile(comment, "yarn.lock", release("1.12.0")))
                .isInstanceOfSatisfying(JsError.class, e -> assertThat(e).hasToString(
                        "RangeError: Invalid string length"));
        assertThat(LockfileParser.parseLockfile(comment, "yarn.lock", release("1.12.1")).getObject())
                .isEqualTo(Collections.emptyMap());
        String string = "# yarn lockfile v1\n\n\"a@1\":\n  version \"1.0.0\n";
        assertThatThrownBy(() -> LockfileParser.parseLockfile(string, "yarn.lock", release("1.12.0")))
                .hasMessage("Invalid string length");
        assertThatThrownBy(() -> LockfileParser.parseLockfile(string, "yarn.lock", release("1.12.1")))
                .isInstanceOfSatisfying(JsError.class, e -> assertThat(e.isSyntaxError()).isTrue());
    }

    /**
     * parse.js: a colon opens an object whatever follows it before 1.16.0, and a word that starts with a dot is no
     * token (what yarn 1.15.2 printed of {@code registry: "x"}: {@code Invalid value type 2:0 in <.yarnrc>}).
     */
    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(delimiter = '|', value = {
            "1.15.2|registry: \"x\"|SyntaxError: Invalid value type 2:0 in .yarnrc",
            "1.16.0|registry: \"x\"|{registry=x}",
            "1.15.2|yarn-offline-mirror ./m|SyntaxError: Invalid value type 1:20 in .yarnrc",
            "1.16.0|yarn-offline-mirror ./m|{yarn-offline-mirror=./m}",
    })
    void readsAValueAfterAColonFrom1160(String version, String line, String parsed) {
        assertThat(parse(line + "\n", version)).isEqualTo(parsed);
    }

    private static String parse(String text, String version) {
        try {
            return String.valueOf(LockfileParser.parseLockfile(text, ".yarnrc", release(version)).getObject());
        } catch (JsError e) {
            return e.toString();
        }
    }

    /**
     * yarn-registry.js {@code loadConfig} parses each .yarnrc as its release does: what only js-yaml reads ends the
     * install before 1.17.0 (yarn 1.16.0 printed {@code Unknown token: { line: 1, col: 5, type: 'INVALID', value:
     * undefined } 1:5 in <.yarnrc>}).
     */
    @Test
    void readsTheYarnrcAsItsReleaseParsesIt() {
        Map<String, String> yarnrc = Collections.singletonMap(".yarnrc", "foo: [bar]\n");
        assertThat(load("1.16.0", yarnrc, Collections.emptyMap())).isEqualTo("SyntaxError: Unknown token: { line: " +
                                                                           "1, col: 5, type: 'INVALID', value: " +
                                                                           "undefined } 1:5 in .yarnrc");
        assertThat(load("1.17.0", yarnrc, Collections.emptyMap())).isEqualTo("reads");
    }

    /**
     * yarn-registry.js {@code loadConfig} resolves the paths of {@code RELATIVE_KEYS} where no file before or
     * variable set them, and {@code path.resolve} throws for a value that is no string: {@code cache-folder} from
     * 1.9.0, {@code offline-cache-folder} from 1.11.0, {@code yarn-path} from 1.13.0, {@code global-folder} from
     * 1.15.0, {@code yarn-offline-mirror} in every release (as each release printed these, without
     * {@code YARN_CACHE_FOLDER} for {@code cache-folder}).
     */
    @ParameterizedTest(name = "{0}: {1}")
    @CsvSource(delimiter = '|', nullValues = "-", value = {
            "1.8.0|cache-folder true|-|reads",
            "1.9.1|cache-folder true|-|TypeError [ERR_INVALID_ARG_TYPE]: The \"paths[1]\" argument must be of type " +
            "string. Received type boolean (true)",
            "1.22.0|cache-folder true|/cache|reads",
            "1.10.1|offline-cache-folder 1|-|reads",
            "1.11.0|offline-cache-folder 1|-|TypeError [ERR_INVALID_ARG_TYPE]: The \"paths[1]\" argument must be of " +
            "type string. Received type number (1)",
            "1.12.3|yarn-path true|-|reads",
            "1.13.0|yarn-path true|-|TypeError [ERR_INVALID_ARG_TYPE]: The \"paths[1]\" argument must be of type " +
            "string. Received type boolean (true)",
            "1.14.0|global-folder true|-|reads",
            "1.15.0|global-folder true|-|TypeError [ERR_INVALID_ARG_TYPE]: The \"paths[1]\" argument must be of " +
            "type string. Received type boolean (true)",
            "1.22.22|yarn-offline-mirror 5|-|TypeError [ERR_INVALID_ARG_TYPE]: The \"paths[1]\" argument must be of " +
            "type string. Received type number (5)",
    })
    void resolvesTheYarnrcPathsOfItsRelease(String version, String line, @Nullable String cacheFolder,
                                            String outcome) {
        assertThat(load(version, Collections.singletonMap(".yarnrc", line + "\n"), cacheFolder == null ?
                Collections.emptyMap() : Collections.singletonMap("YARN_CACHE_FOLDER", cacheFolder)))
                .isEqualTo(outcome);
    }

    private static String load(String version, Map<String, String> configFiles, Map<String, String> env) {
        try {
            config(version, configFiles, env);
            return "reads";
        } catch (JsError e) {
            return e.toString();
        }
    }

    private static Config config(String version, Map<String, String> configFiles, Map<String, String> env) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version)).manifest("", ROOT).configFiles(configFiles).env(env).build();
        return new Config(inputs, YarnClassicLockRegenerator.start(inputs));
    }

    /**
     * rc.js {@code PATH_KEYS}: an rc file's {@code offline-cache-folder} is resolved against its directory from
     * 1.11.0.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "1.10.1|{offline-cache-folder=m}",
            "1.11.0|{offline-cache-folder=../m}",
    })
    void resolvesAnRcOfflineCacheFolderFrom1110(String version, String rc) {
        assertThat(rc(version, Collections.singletonMap("../.yarnrc", "offline-cache-folder \"m\"\n"))).isEqualTo(rc);
    }

    /**
     * The ini an .npmrc is read with: 1.3.4 before 1.10.0 keeps the whitespace before an inline comment.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "1.9.4|'https://r.example/ '",
            "1.10.0|https://r.example/",
    })
    void readsAnNpmrcWithTheIniOfItsRelease(String version, String registry) {
        assertThat(config(version, Collections.singletonMap(".npmrc", "registry=https://r.example/ # mirror\n"),
                Collections.emptyMap()).getRegistry("x")).isEqualTo(registry);
    }

    /**
     * base-registry.js {@code mergeEnv}: a variable whose name holds the prefix past its start sets a key of its whole
     * name before 1.12.1; a leading underscore becomes a dash before 1.15.0.
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', nullValues = "-", value = {
            "1.12.0|FOO_YARN_REGISTRY|foo-yarn-registry|x",
            "1.12.1|FOO_YARN_REGISTRY|foo-yarn-registry|-",
            "1.14.0|NPM_CONFIG__AUTH|-auth|x",
            "1.14.0|NPM_CONFIG__AUTH|_auth|-",
            "1.15.0|NPM_CONFIG__AUTH|_auth|x",
    })
    void mergesTheEnvironmentAsItsReleaseDoes(String version, String variable, String key, @Nullable String value) {
        assertThat(config(version, Collections.emptyMap(), Collections.singletonMap(variable, "x")).getOption(key))
                .isEqualTo(value);
    }

    /**
     * npm-registry.js {@code requestNeedsAuth}: a request to a host an .npmrc gives a {@code _password} carries the
     * package's registry's credentials from 1.12.1.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', nullValues = "-", value = {
            "1.12.0|-",
            "1.12.1|Bearer t",
    })
    void authenticatesToARegistryOfAPasswordFrom1121(String version, @Nullable String authorization) {
        String npmrc = "@s:registry=https://r.example/\n//r.example/:_authToken=t\n//o.example/:_password=cA==\n";
        assertThat(config(version, Collections.singletonMap(".npmrc", npmrc), Collections.emptyMap())
                .getAuthorization("@s/x", "https://o.example/x/-/x-1.0.0.tgz")).isEqualTo(authorization);
    }

    /**
     * cli/index.js {@code start}: a .yarnrc's {@code yarnPath} hands the install over from 1.16.0.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "1.15.2|installs",
            "1.16.0|cannot-know what the release yarn-path names (.yarn/releases/yarn-wrapper.cjs) writes: yarn " +
            "1.16.0 hands the install to it",
    })
    void followsAYarnrcsYarnPathFrom1160(String version, String outcome) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", ROOT)
                .lock("# yarn lockfile v1\n\n\n")
                .configFile(".yarnrc", "yarnPath \".yarn/releases/yarn-wrapper.cjs\"\n")
                .paths(new LinkedHashSet<>(Arrays.asList("package.json", "yarn.lock", ".yarnrc",
                        ".yarn/releases/yarn-wrapper.cjs")))
                .env(Collections.emptyMap())
                .build();
        assertThat(start(inputs)).isEqualTo(outcome);
    }

    /**
     * package-compatibility.js and en.js: the engine's message names the version it has from 1.10.0, and the failure
     * ends with a period from 1.16.0.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "1.9.4|.|Found incompatible module",
            "1.15.2|. Got \"1.15.2\"|Found incompatible module",
            "1.16.0|. Got \"1.16.0\"|Found incompatible module.",
    })
    void reportsAnIncompatibleEngineAsItsReleaseDoes(String version, String got, String found) {
        Manifest manifest = new Manifest();
        manifest.put("name", "x");
        manifest.put("version", "1.0.0");
        manifest.put("engines", Collections.singletonMap("yarn", ">=1.22.20"));
        List<String> errors = new ArrayList<>();
        assertThatThrownBy(() -> new PackageCompatibility(config(version), Host.UNKNOWN, errors)
                .checkOne(manifest, false)).isInstanceOf(MessageError.class).hasMessage(found);
        assertThat(errors).containsExactly("x@1.0.0: The engine \"yarn\" is incompatible with this module. " +
                                           "Expected version \">=1.22.20\"" + got);
    }

    /**
     * install.js {@code init}: the root's os is checked without an engines object from 1.15.0; before, where its
     * engines is an object or null (as yarn 1.14.0 and 1.15.0 installed or refused these on darwin).
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "1.14.0|{\"name\":\"p\",\"version\":\"1.0.0\",\"os\":[\"win32\"]}|installs",
            "1.15.0|{\"name\":\"p\",\"version\":\"1.0.0\",\"os\":[\"win32\"]}|UNSUPPORTED_ENTRY_TYPE: yarn rejects " +
            "this edit: p@1.0.0: The platform \"darwin\" is incompatible with this module. Found incompatible module",
            "1.14.0|{\"name\":\"p\",\"version\":\"1.0.0\",\"os\":[\"win32\"],\"engines\":null}|" +
            "UNSUPPORTED_ENTRY_TYPE: yarn rejects this edit: p@1.0.0: The platform \"darwin\" is incompatible with " +
            "this module. Found incompatible module",
    })
    void checksTheRootsPlatformAsItsReleaseDoes(String version, String manifest, String outcome) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", manifest)
                .lock("# yarn lockfile v1\n\n\n")
                .host(Host.builder().os("darwin").cpu("arm64").build())
                .env(Collections.emptyMap())
                .build();
        LockFileRegeneration.Result result = new YarnClassicLockRegenerator(inputs, YarnClassicScenarios.NO_REGISTRY)
                .regenerate();
        assertThat(result.isSuccess() ? "installs" : result.getErrorMessage()).isEqualTo(outcome);
    }

    /**
     * install.js {@code fetchRequestFromCwd}: each workspace is asked for from the root beside the aggregator from
     * 1.11.0.
     */
    @Test
    void requestsTheWorkspacesFromTheRootFrom1110() {
        assertThat(workspacePatterns("1.10.1")).noneMatch(p -> p.startsWith("a@"));
        assertThat(workspacePatterns("1.11.0")).contains("a@1.0.0");
    }

    private static List<String> workspacePatterns(String version) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", "{\"name\":\"r\",\"version\":\"1.0.0\",\"private\":true,\"workspaces\":[\"a\"]}")
                .manifest("a", "{\"name\":\"a\",\"version\":\"1.0.0\"}")
                .lock("# yarn lockfile v1\n\n\n")
                .paths(new LinkedHashSet<>(Arrays.asList("package.json", "yarn.lock", "a/package.json")))
                .env(Collections.emptyMap())
                .build();
        Map<String, @Nullable Object> flags = YarnClassicLockRegenerator.start(inputs);
        Config config = new Config(inputs, flags);
        return new Install(inputs, config, flags, YarnClassicScenarios.NO_REGISTRY)
                .fetchRequestFromCwd(Lockfile.fromDirectory(inputs.getLock(), config.getRelease()),
                        new ResolutionMap(config)).getPatterns();
    }

    /**
     * config.js {@code generateModuleCachePath}: from 1.11.0 a package's cache folder is a slug of its name, whose
     * {@code replace} a lock entry's name of no string does not have.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({"1.10.1,false", "1.11.0,true"})
    void slugsTheCacheFolderFrom1110(String version, boolean throwsOnTheName) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"x\":\"1.0.0\"}}")
                .lock("# yarn lockfile v1\n\n\nx@1.0.0:\n  name 123\n  version \"1.0.0\"\n  resolved " +
                      "\"https://registry.yarnpkg.com/x/-/x-1.0.0.tgz#0000000000000000000000000000000000000000\"\n")
                .configFile(".yarnrc", "unsafe-disable-integrity-migration true\n")
                .env(Collections.emptyMap())
                .build();
        LockFileRegeneration.Result result = new YarnClassicLockRegenerator(inputs, YarnClassicScenarios.NO_REGISTRY)
                .regenerate();
        assertThat(String.valueOf(result.getErrorMessage()).contains("slug.replace is not a function"))
                .as(String.valueOf(result.getErrorMessage())).isEqualTo(throwsOnTheName);
    }

    /**
     * fix.js: from 1.21.1 a bin whose name or target is invalid is dropped, and {@code path.isAbsolute} throws for a
     * target that is no string; 1.22.0 takes names with dots.
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "1.21.0|{\"x\": 1}|normalizes",
            "1.21.1|{\"x\": 1}|TypeError [ERR_INVALID_ARG_TYPE]: The \"path\" argument must be of type string. " +
            "Received type number (1)",
            "1.21.1|{\"a.b\": 1}|normalizes",
            "1.22.0|{\"a.b\": 1}|TypeError [ERR_INVALID_ARG_TYPE]: The \"path\" argument must be of type string. " +
            "Received type number (1)",
            "1.22.22|{\"a/b\": 1}|normalizes",
            "1.22.22|[\"cli.js\", true]|TypeError [ERR_INVALID_ARG_TYPE]: The \"path\" argument must be of type " +
            "string. Received type boolean (true)",
    })
    void validatesTheBinsAsItsReleaseDoes(String version, String bin, String outcome) {
        Manifest info = new Manifest(object("{\"name\": \"x\", \"version\": \"1.0.0\", \"bin\": " + bin + "}"));
        String normalized;
        try {
            NormalizeManifest.normalizeManifest(info, "", "", false, true, release(version));
            normalized = "normalizes";
        } catch (JsError e) {
            normalized = e.toString();
        }
        assertThat(normalized).isEqualTo(outcome);
    }

    /**
     * A directory dependency's bin of no string ends the install in every release, where each release links it: the
     * link step before 1.11.0, the fetch (copy-fetcher.js is a base-fetcher.js) from 1.11.0, fix.js from 1.21.1 (as
     * each printed it).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "1.10.1|An unexpected error occurred: \"The \\\"path\\\" argument must be of type string. Received type " +
            "number (1)\".",
            "1.11.0|An unexpected error occurred: \"The \\\"paths[1]\\\" argument must be of type string. Received " +
            "type number (1)\".",
            "1.21.0|An unexpected error occurred: \"The \\\"paths[1]\\\" argument must be of type string. Received " +
            "type number (1)\".",
            "1.21.1|An unexpected error occurred: \"The \\\"path\\\" argument must be of type string. Received type " +
            "number (1)\".",
    })
    void linksTheBinsAsItsReleaseDoes(String version, String error) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"dep\":\"file:./dep\"}}")
                .manifest("dep", "{\"name\":\"dep\",\"version\":\"1.0.0\",\"bin\":{\"x\":1}}")
                .lock("# yarn lockfile v1\n\n\n")
                .paths(new LinkedHashSet<>(Arrays.asList("package.json", "yarn.lock", "dep/package.json")))
                .env(Collections.emptyMap())
                .build();
        LockFileRegeneration.Result result = new YarnClassicLockRegenerator(inputs, YarnClassicScenarios.NO_REGISTRY)
                .regenerate();
        assertThat(result.getErrorMessage()).endsWith(": yarn fails on this edit: " + error);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> object(String json) {
        return (Map<String, @Nullable Object>) Js.jsonParse(json);
    }

    /**
     * The tar-fs of each release: 1.16.0 (before 1.10.0) writes through a directory that is a link of the archive,
     * 1.16.3 refuses it ({@code validate}), which yarn reports as a corrupt archive.
     */
    @Test
    void writesThroughALinkedDirectoryBefore1100() {
        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        byte[][] entries = {
                entry("package/package.json", "{\"name\":\"x\",\"version\":\"1.0.0\"}", null),
                entry("package/src/", null, null),
                entry("package/lib", null, "src"),
                entry("package/lib/x.js", "x", null),
                new byte[1024]
        };
        for (byte[] entry : entries) {
            archive.write(entry, 0, entry.length);
        }
        byte[] tar = archive.toByteArray();
        assertThat(TarballFetcher.untar("<url>", tar, release("1.9.4")).get("src/x.js")).isNotNull();
        assertThatThrownBy(() -> TarballFetcher.untar("<url>", tar, release("1.10.0")))
                .isInstanceOf(MessageError.class)
                .hasMessageEndingWith("the file appears to be corrupt: \"lib is not a valid path\"");
    }

    /**
     * install.js {@code setFlags} declares {@code --focus} from 1.7.0; before, a .yarnrc's {@code --focus} is an
     * option commander drops, and the install at the workspace root runs as any other (the sweep's
     * open-opt-focus-standalone).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "1.6.0|installs",
            "1.7.0|RESOLUTION_REQUIRED: yarn rejects this edit: This command can only be run inside an individual " +
            "workspace.",
    })
    void declaresFocusFrom170(String version, String outcome) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", "{\"name\":\"r\",\"version\":\"1.0.0\",\"private\":true,\"workspaces\":[\"a\"]}")
                .manifest("a", "{\"name\":\"a\",\"version\":\"1.0.0\"}")
                .lock("# yarn lockfile v1\n\n\n")
                .configFile(".yarnrc", "--focus true\n")
                .paths(new LinkedHashSet<>(Arrays.asList("package.json", "yarn.lock", ".yarnrc", "a/package.json")))
                .env(Collections.emptyMap())
                .build();
        LockFileRegeneration.Result result = new YarnClassicLockRegenerator(inputs, YarnClassicScenarios.NO_REGISTRY)
                .regenerate();
        assertThat(result.isSuccess() ? "installs" : result.getErrorMessage()).isEqualTo(outcome);
    }

    /**
     * package-resolver.js {@code resolveToResolution}: the lock entry of a pattern a resolution takes is removed
     * before 1.3.1, kept for the root's own patterns from 1.3.1, kept always from 1.6.0, removed where the resolution's
     * tarball is another from 1.7.0. A later request of the pattern then resolves anew and joins the resolution, or reads
     * the entry and makes a second reference, whose fetch races the resolution's for the pattern (as yarn 1.2.1, 1.3.1,
     * 1.5.1, 1.6.0 and 1.7.0 installed these: the races wrote both entries over repeated runs).
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "1.2.1|root|shares",
            "1.3.1|root|races",
            "1.6.0|root|races",
            "1.7.0|root|shares",
            "1.2.1|nested|shares",
            "1.3.1|nested|shares",
            "1.6.0|nested|races",
            "1.7.0|nested|shares",
    })
    void keepsTheLockEntryOfAResolvedPatternAsItsReleaseDoes(String version, String where, String outcome) {
        boolean root = "root".equals(where);
        String lock = "# yarn lockfile v1\n\n\n" +
                      "a@^1.0.0:\n  version \"1.0.0\"\n  resolved \"https://registry.yarnpkg.com/a/-/a-1.0.0.tgz#a\"\n" +
                      "  dependencies:\n    b \"^1.0.0\"\n\n" +
                      "b@1.0.1:\n  version \"1.0.1\"\n  resolved \"https://registry.yarnpkg.com/b/-/b-1.0.1.tgz#b1\"\n\n" +
                      "b@^1.0.0:\n  version \"1.0.0\"\n  resolved \"https://registry.yarnpkg.com/b/-/b-1.0.0.tgz#b0\"\n" +
                      (root ? "" : "\nc@^1.0.0:\n  version \"1.0.0\"\n  resolved \"https://registry.yarnpkg.com/c/-/" +
                                   "c-1.0.0.tgz#c\"\n  dependencies:\n    b \"^1.0.0\"\n");
        String manifest = root ?
                "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"^1.0.0\",\"b\":\"^1.0.0\"}," +
                "\"resolutions\":{\"b\":\"1.0.1\"}}" :
                "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"^1.0.0\",\"c\":\"^1.0.0\"}," +
                "\"resolutions\":{\"a/b\":\"1.0.1\"}}";
        String b = "{\"name\":\"b\",\"dist-tags\":{\"latest\":\"1.0.1\"},\"versions\":{" +
                   "\"1.0.0\":{\"name\":\"b\",\"version\":\"1.0.0\",\"dist\":{\"tarball\":" +
                   "\"https://registry.yarnpkg.com/b/-/b-1.0.0.tgz\",\"shasum\":\"b0\"}}," +
                   "\"1.0.1\":{\"name\":\"b\",\"version\":\"1.0.1\",\"dist\":{\"tarball\":" +
                   "\"https://registry.yarnpkg.com/b/-/b-1.0.1.tgz\",\"shasum\":\"b1\"}}}}";
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", manifest)
                .lock(lock)
                .env(Collections.emptyMap())
                .build();
        LockFileRegeneration.Result result = new YarnClassicLockRegenerator(inputs,
                YarnClassicScenarios.packuments((registry, name) -> "b".equals(name) ? b : null)).regenerate();
        if ("shares".equals(outcome)) {
            assertThat(result.getLockFileContent()).contains("b@1.0.1, b@^1.0.0:\n  version \"1.0.1\"\n");
        } else {
            assertThat(result.getErrorMessage()).contains("yarn finishes last: both their references hold b@^1.0.0");
        }
    }

    /**
     * .babelrc: from 1.4.0 to 1.5.1 no error of yarn's own is an {@code instanceof MessageError}, so that cli/index.js
     * reports it as unexpected (the sweep's small-version-not-found and its kin).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "1.3.2|Couldn't find any versions for \"x\" that matches \"^9.0.0\"",
            "1.4.0|An unexpected error occurred: \"Couldn't find any versions for \\\"x\\\" that matches " +
            "\\\"^9.0.0\\\"\".",
            "1.5.1|An unexpected error occurred: \"Couldn't find any versions for \\\"x\\\" that matches " +
            "\\\"^9.0.0\\\"\".",
            "1.6.0|Couldn't find any versions for \"x\" that matches \"^9.0.0\"",
    })
    void reportsItsOwnErrorsAsItsReleaseDoes(String version, String reported) {
        assertThat(YarnClassicLockRegenerator.report(new MessageError(
                "Couldn't find any versions for \"x\" that matches \"^9.0.0\""), release(version))).isEqualTo(reported);
    }

    /**
     * npm-resolver.js {@code findVersionInRegistryResponse}, as yarn 1.2.1, 1.3.2 and 1.5.0 answered a packument whose
     * versions are empty or missing: the highest satisfying version before 1.3.1, the latest tag from 1.3.1 (a version
     * of empty versions is none: yarn then names the package not found), the response refused from 1.5.0.
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "1.2.1|{}|Couldn't find any versions for \"x\" that matches \"^1.0.0\"",
            "1.3.2|{}|none",
            "1.5.0|{}|No valid versions found for \"x\". The package may be unpublished.",
            "1.2.1|-|TypeError: Cannot convert undefined or null to object",
            "1.3.2|-|TypeError: Cannot read properties of undefined (reading '1.0.0')",
            "1.5.0|-|Received malformed response from registry for \"x\". The registry may be down.",
    })
    void readsAResponseWithoutVersionsAsItsReleaseDoes(String version, String versions, String outcome) {
        Object body = object("{\"name\": \"x\", \"dist-tags\": {\"latest\": \"1.0.0\"}" +
                             ("-".equals(versions) ? "" : ", \"versions\": " + versions) + "}");
        String answer;
        try {
            Object found = NpmResolver.findVersionInRegistryResponse(config(version), "x", "^1.0.0", body);
            answer = found == null ? "none" : String.valueOf(found);
        } catch (MessageError e) {
            answer = e.getMessage();
        } catch (JsError e) {
            answer = e.toString();
        }
        assertThat(answer).isEqualTo(outcome);
    }

    /**
     * config.js, as yarn 1.3.2, 1.4.0, 1.5.0, 1.5.1 and 1.6.0 installed these roots: workspaces that are a string fail
     * in {@code patterns.map} before 1.4.0, are refused from 1.4.0, make no workspace root from 1.5.0
     * ({@code extractWorkspaces}); a nohoist the config disables is thrown in 1.5.x, a warning from 1.6.0.
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "1.3.2|\"packages/*\"||RESOLUTION_REQUIRED: yarn fails on this edit: An unexpected error occurred: " +
            "\"patterns.map is not a function\".",
            "1.4.0|\"packages/*\"||MALFORMED_MANIFEST: yarn rejects this edit: An unexpected error occurred: \"The " +
            "workspaces field in package.json must be an array.\".",
            "1.5.0|\"packages/*\"||installs",
            "1.5.1|{\"packages\": [\"packages/*\"], \"nohoist\": [\"**/x\"]}|workspaces-nohoist-experimental false|" +
            "MALFORMED_MANIFEST: yarn rejects this edit: An unexpected error occurred: \"\\\"r\\\" defines nohoist but " +
            "the feature is disabled in your Yarn config. Please check \\\"workspaces-nohoist-experimental\\\" in " +
            "your .yarnrc file.\".",
            "1.6.0|{\"packages\": [\"packages/*\"], \"nohoist\": [\"**/x\"]}|workspaces-nohoist-experimental false|" +
            "installs",
    })
    void readsTheWorkspacesAsItsReleaseDoes(String version, String workspaces, @Nullable String yarnrc,
                                            String outcome) {
        YarnClassicLockRegenerator.Inputs.InputsBuilder inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", "{\"name\":\"r\",\"version\":\"1.0.0\",\"private\":true,\"workspaces\":" + workspaces +
                              "}")
                .manifest("packages/a", "{\"name\":\"a\",\"version\":\"1.0.0\"}")
                .lock("# yarn lockfile v1\n\n\n")
                .paths(new LinkedHashSet<>(Arrays.asList("package.json", "yarn.lock", "packages/a/package.json")))
                .env(Collections.emptyMap());
        if (yarnrc != null) {
            inputs.configFile(".yarnrc", yarnrc + "\n");
        }
        LockFileRegeneration.Result result = new YarnClassicLockRegenerator(inputs.build(),
                YarnClassicScenarios.NO_REGISTRY).regenerate();
        assertThat(result.isSuccess() ? "installs" : result.getErrorMessage()).isEqualTo(outcome);
    }

    /**
     * en.js {@code workspacesRequirePrivateProjects} ends with a period from 1.4.0, as yarn 1.3.2, 1.4.0 and 1.6.0
     * refused the sweep's open-ws-not-private (1.4.0 to 1.5.1 report it as unexpected).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "1.3.2|Workspaces can only be enabled in private projects",
            "1.4.0|An unexpected error occurred: \"Workspaces can only be enabled in private projects.\".",
            "1.6.0|Workspaces can only be enabled in private projects.",
    })
    void endsTheWorkspacesPrivacyMessageAsItsReleaseDoes(String version, String reported) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", "{\"name\":\"r\",\"version\":\"1.0.0\",\"workspaces\":[\"packages/*\"]}")
                .manifest("packages/a", "{\"name\":\"a\",\"version\":\"1.0.0\"}")
                .lock("# yarn lockfile v1\n\n\n")
                .paths(new LinkedHashSet<>(Arrays.asList("package.json", "yarn.lock", "packages/a/package.json")))
                .env(Collections.emptyMap())
                .build();
        LockFileRegeneration.Result result = new YarnClassicLockRegenerator(inputs, YarnClassicScenarios.NO_REGISTRY)
                .regenerate();
        assertThat(result.getErrorMessage()).isEqualTo("MALFORMED_MANIFEST: yarn rejects this edit: " + reported);
    }

    /**
     * config.js {@code resolveWorkspaces}: a manifest two levels under a member's node_modules is a member before
     * 1.6.0, which ignores one level down only (as yarn 1.5.1 and 1.6.0 installed a {@code packages/**} root).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {"1.5.1|[@s/b, a]", "1.6.0|[a]"})
    void findsTheMembersUnderNodeModulesAsItsReleaseDoes(String version, String members) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", "{\"name\":\"r\",\"version\":\"1.0.0\",\"private\":true,\"workspaces\":[\"packages/**\"]}")
                .manifest("packages/a", "{\"name\":\"a\",\"version\":\"1.0.0\"}")
                .manifest("packages/a/node_modules/@s/b", "{\"name\":\"@s/b\",\"version\":\"1.0.0\"}")
                .env(Collections.emptyMap())
                .build();
        Config config = new Config(inputs, YarnClassicLockRegenerator.start(inputs));
        Manifest root = Config.tryManifest("", inputs.getManifests().get(""), true, true, "", release(version));
        assertThat(config.resolveWorkspaces(root).keySet()).asString().isEqualTo(members);
    }

    /**
     * util/conversion.js {@code boolify} from 1.6.0: a .yarnrc's {@code --production 0} is not production; before,
     * cli/index.js's boolify makes the number 0 true (as yarn 1.5.1 and 1.6.0 installed a member's devDependencies).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({"1.5.1,true", "1.6.0,false"})
    void boolifiesZeroAsItsReleaseDoes(String version, boolean production) {
        Map<String, @Nullable Object> rc = new LinkedHashMap<>();
        rc.put("--production", 0.0);
        assertThat(Rc.getRcArgs(rc, release(version)).get("production")).isEqualTo(production);
    }

    /**
     * git-resolver.js {@code isVersion} from 1.6.0 matches git's url patterns; before, url.parse's protocol and a
     * pathname ending in {@code .git} make a pattern git, which is the first exotic resolver (as yarn 1.5.1 ran git for
     * {@code file:./x.git} and 1.6.0 copied it).
     */
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', nullValues = "-", value = {
            "1.5.1|file:./x.git|GIT",
            "1.6.0|file:./x.git|FILE",
            "1.5.1|github:a/b.git|GIT",
            "1.6.0|github:a/b.git|GITHUB",
            "1.5.1|https://h/x.git?q|GIT",
            "1.6.0|https://h/x.git?q|TARBALL",
            "1.5.1|link:x.git|LINK",
            "1.5.1|^1.0.0|-",
    })
    void tellsGitAsItsReleaseDoes(String version, String pattern, @Nullable String kind) {
        ExoticResolvers.Kind resolver = ExoticResolvers.getExoticResolver(pattern, release(version));
        assertThat(resolver == null ? null : resolver.name()).isEqualTo(kind);
    }

    /**
     * registry-resolver.js from 1.5.0 keeps the {@code @} of a scoped alias's name; before, its name is {@code ""}
     * (yarn 1.4.0 asked the registry's root for it).
     */
    @Test
    void readsAScopedAliasFrom150() {
        assertThat(ExoticResolvers.parseRegistryFragment("npm:@s/p@1.0.0", release("1.4.0")))
                .isEqualTo(new ExoticResolvers.RegistryFragment("npm", "", "s/p@1.0.0"));
        assertThat(ExoticResolvers.parseRegistryFragment("npm:@s/p@1.0.0", release("1.5.0")))
                .isEqualTo(new ExoticResolvers.RegistryFragment("npm", "@s/p", "1.0.0"));
    }

    /**
     * file-resolver.js from 1.0.2 takes dots only before a file pattern's slash; before, any one or two characters, so
     * that a package of a one-character scope is a file (yarn 1.0.1 found no {@code @z/zz-probe-sc@1.0.0} directory).
     */
    @Test
    void takesAOneCharacterScopeForAPackageFrom102() {
        assertThat(ExoticResolvers.getExoticResolver("@z/x@1.0.0", release("1.0.1")))
                .isEqualTo(ExoticResolvers.Kind.FILE);
        assertThat(ExoticResolvers.getExoticResolver("@z/x@1.0.0", release("1.0.2"))).isNull();
    }

    /**
     * prebuiltVariants: a lock entry's variants are written from 1.5.0; before, every lock yarn writes drops them (as
     * yarn 1.4.0 and 1.5.0 rewrote such a lock).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({"1.4.0,false", "1.5.0,true"})
    void keepsPrebuiltVariantsFrom150(String version, boolean kept) throws NoSuchAlgorithmException {
        String entry = "a@1.0.0:\n  version \"1.0.0\"\n  resolved \"https://registry.yarnpkg.com/a/-/a-1.0.0.tgz#a\"\n";
        String variants = "  prebuiltVariants:\n    a-v1.0.0-x abc\n";
        byte[] tar = tar("package/package.json", "{\"name\":\"b\",\"version\":\"1.0.0\"}", null);
        StringBuilder shasum = new StringBuilder();
        for (byte digit : MessageDigest.getInstance("SHA-1").digest(tar)) {
            shasum.append(String.format("%02x", digit));
        }
        String b = "{\"name\":\"b\",\"dist-tags\":{\"latest\":\"1.0.0\"},\"versions\":{\"1.0.0\":{\"name\":\"b\"," +
                   "\"version\":\"1.0.0\",\"dist\":{\"tarball\":\"https://registry.yarnpkg.com/b/-/b-1.0.0.tgz\"," +
                   "\"shasum\":\"" + shasum + "\"}}}}";
        RegistrySource registry = new RegistrySource() {
            @Override
            public @Nullable String packument(String r, String name, boolean full, @Nullable String authorization) {
                return "b".equals(name) ? b : null;
            }

            @Override
            public byte @Nullable [] tarball(String url, String name, String v, @Nullable String authorization) {
                return "b".equals(name) ? tar : null;
            }
        };
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"1.0.0\",\"b\":\"1.0.0\"}}")
                .lock("# yarn lockfile v1\n\n\n" + entry + variants)
                .env(Collections.emptyMap())
                .build();
        LockFileRegeneration.Result result = new YarnClassicLockRegenerator(inputs, registry).regenerate();
        assertThat(result.getLockFileContent()).as(String.valueOf(result.getErrorMessage())).contains(entry)
                .contains("\nb@1.0.0:\n");
        assertThat(result.getLockFileContent().contains(entry + variants)).isEqualTo(kept);
    }

    /**
     * package-request.js {@code find} rejects an invalid version from 1.5.0; before, the missing version reaches
     * {@code getExactVersionMatch}, whose exotic check throws in url.parse (the sweep's
     * small-lock-entry-without-version).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "1.4.0|RESOLUTION_REQUIRED: yarn fails on this edit: An unexpected error occurred: \"The \\\"url\\\" " +
            "argument must be of type string. Received undefined\".",
            "1.5.0|MALFORMED_LOCK: yarn rejects this edit: An unexpected error occurred: \"Can't add \\\"a\\\": " +
            "invalid package version undefined.\".",
    })
    void rejectsAnInvalidVersionFrom150(String version, String outcome) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"1.0.0\",\"b\":\"1.0.0\"}}")
                .lock("# yarn lockfile v1\n\n\n" +
                      "a@1.0.0:\n  version \"1.0.0\"\n  resolved \"https://registry.yarnpkg.com/a/-/a-1.0.0.tgz#a\"\n\n" +
                      "a@^1.0.0:\n  resolved \"https://registry.yarnpkg.com/a/-/a-1.0.0.tgz#a\"\n\n" +
                      "b@1.0.0:\n  version \"1.0.0\"\n  resolved \"https://registry.yarnpkg.com/b/-/b-1.0.0.tgz#b\"\n" +
                      "  dependencies:\n    a \"^1.0.0\"\n")
                .env(Collections.emptyMap())
                .build();
        LockFileRegeneration.Result result = new YarnClassicLockRegenerator(inputs, YarnClassicScenarios.NO_REGISTRY)
                .regenerate();
        assertThat(result.getErrorMessage()).isEqualTo(outcome);
    }

    /**
     * package-request.js {@code findVersionOnRegistry} from 1.5.0 names the parent of a package the registry does not
     * have; before, the registry's own error goes out (the sweep's open-member-cwd-implicit-file-root-dir).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "1.4.0|An unexpected error occurred: \"https://registry.yarnpkg.com/b: Not found\".",
            "1.5.0|An unexpected error occurred: \"Couldn't find package \\\"b@^1.0.0\\\" required by \\\"a@1.0.0\\\" " +
            "on the \\\"npm\\\" registry.\".",
            "1.6.0|Couldn't find package \"b@^1.0.0\" required by \"a@1.0.0\" on the \"npm\" registry.",
    })
    void namesTheParentOfAPackageNotFoundFrom150(String version, String reported) {
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"1.0.0\"}}")
                .lock("# yarn lockfile v1\n\n\na@1.0.0:\n  version \"1.0.0\"\n  resolved " +
                      "\"https://registry.yarnpkg.com/a/-/a-1.0.0.tgz#a\"\n  dependencies:\n    b \"^1.0.0\"\n")
                .env(Collections.emptyMap())
                .build();
        LockFileRegeneration.Result result = new YarnClassicLockRegenerator(inputs, YarnClassicScenarios.NO_REGISTRY)
                .regenerate();
        assertThat(result.getErrorMessage()).endsWith(": " + reported);
    }

    /**
     * base-reporter.js {@code stringifyLangArgs}, as yarn 1.4.0, 1.5.0, 1.6.0 and 1.7.0 reported a range that holds a
     * backslash followed by an {@code n}, and one that holds a line break: the escapes of the JSON before 1.5.0, a line
     * break for each from 1.5.0, the backslash's {@code n} kept from 1.7.0; a message reported as unexpected (1.4.0 to
     * 1.5.1) goes through it twice. ({@code ^} stands for a backslash.)
     */
    @Test
    void breaksTheLinesOfAnArgumentAsItsReleaseDoes() {
        String escaped = "a\\nb";
        String broken = "a\nb";
        assertThat(versionNotFound("1.4.0", escaped)).isEqualTo(backslashes(
                "An unexpected error occurred: \"Couldn't find any versions for ^\"x^\" that matches ^\"a^^^^nb^\"\"."));
        assertThat(versionNotFound("1.5.0", escaped)).isEqualTo(backslashes(
                "An unexpected error occurred: \"Couldn't find any versions for ^\"x^\" that matches ^\"a^^\nb^\"\"."));
        assertThat(versionNotFound("1.6.0", escaped)).isEqualTo(backslashes(
                "Couldn't find any versions for \"x\" that matches \"a^\nb\""));
        assertThat(versionNotFound("1.7.0", escaped)).isEqualTo(backslashes(
                "Couldn't find any versions for \"x\" that matches \"a^^nb\""));
        assertThat(versionNotFound("1.4.0", broken)).isEqualTo(backslashes(
                "An unexpected error occurred: \"Couldn't find any versions for ^\"x^\" that matches ^\"a^^nb^\"\"."));
        assertThat(versionNotFound("1.5.0", broken)).isEqualTo(backslashes(
                "An unexpected error occurred: \"Couldn't find any versions for ^\"x^\" that matches ^\"a\nb^\"\"."));
        assertThat(versionNotFound("1.6.0", broken)).isEqualTo("Couldn't find any versions for \"x\" that matches \"a\nb\"");
        assertThat(versionNotFound("1.7.0", broken)).isEqualTo("Couldn't find any versions for \"x\" that matches \"a\nb\"");
    }

    // what yarn reports for a root dependency on x, whose only version is 1.0.0, of a range no version satisfies
    private static String versionNotFound(String version, String range) {
        Map<String, @Nullable Object> manifest = new LinkedHashMap<>();
        manifest.put("name", "p");
        manifest.put("version", "1.0.0");
        manifest.put("dependencies", Collections.singletonMap("x", range));
        YarnClassicLockRegenerator.Inputs inputs = YarnClassicLockRegenerator.Inputs.builder()
                .release(release(version))
                .manifest("", Js.jsonStringify(manifest))
                .lock("# yarn lockfile v1\n\n\n")
                .env(Collections.emptyMap())
                .build();
        LockFileRegeneration.Result result = new YarnClassicLockRegenerator(inputs, packuments(Collections.singletonMap(
                "x", "{\"name\":\"x\",\"dist-tags\":{\"latest\":\"1.0.0\"},\"versions\":{\"1.0.0\":{\"name\":\"x\"," +
                     "\"version\":\"1.0.0\",\"dist\":{\"tarball\":\"https://registry.yarnpkg.com/x/-/x-1.0.0.tgz\"}}}}")))
                .regenerate();
        String error = String.valueOf(result.getErrorMessage());
        return error.substring(error.indexOf(" this edit: ") + " this edit: ".length());
    }

    private static String backslashes(String text) {
        return text.replace('^', '\\');
    }

    // a registry that has these packuments, by name, and no tarball
    private static RegistrySource packuments(Map<String, String> byName) {
        return new RegistrySource() {
            @Override
            public @Nullable String packument(String registry, String name, boolean full,
                                              @Nullable String authorization) {
                return byName.get(name);
            }

            @Override
            public byte @Nullable [] tarball(String url, String name, String version, @Nullable String authorization) {
                return null;
            }
        };
    }

    /**
     * tar-stream and readable-stream as each release bundles them, as yarn 1.6.0, 1.7.0 and 1.8.0 installed these
     * tarballs from a registry: an archive cut inside a file after package.json extracts before 1.8.0 (readable-stream
     * 2.2.9 calls no {@code _final}) and is corrupt from 1.8.0; cut inside package.json, what yarn reads of it follows
     * its write (1.6.0 installed, 1.7.0 read ten bytes); a package.json whose size a pax header gives is read from
     * 1.7.0 (tar-stream 1.6.0), and before, its content is the next header, which tar-stream rejects.
     */
    @Test
    void extractsAnArchiveAsItsTarStreamDoes() {
        byte[] manifest = entry("package/package.json", "{\"name\":\"x\",\"version\":\"1.0.0\"}", null);
        byte[] later = entry("package/later.txt", repeat('x', 2000), null);
        byte[] cutLater = concat(manifest, Arrays.copyOfRange(later, 0, 512 + 100));
        assertThat(new String(TarballFetcher.untar("<url>", cutLater, release("1.7.0")).get("package.json")
                .content(), StandardCharsets.UTF_8)).isEqualTo("{\"name\":\"x\",\"version\":\"1.0.0\"}");
        assertThatThrownBy(() -> TarballFetcher.untar("<url>", cutLater, release("1.8.0")))
                .isInstanceOf(MessageError.class)
                .hasMessageEndingWith("the file appears to be corrupt: \"Unexpected end of data\"");

        byte[] cutManifest = Arrays.copyOfRange(manifest, 0, 512 + 10);
        TarFs extracted = TarballFetcher.untar("<url>", cutManifest, release("1.7.0"));
        assertThatThrownBy(() -> extracted.get("package.json").content())
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("which the tarball's archive ends inside");

        String text = "{\"name\":\"x\",\"version\":\"1.0.0\"}";
        String record = " size=" + text.length() + "\n";
        record = (record.length() + 2) + record;
        byte[] paxSize = concat(header("package/PaxHeader", record.length(), 'x'), block(record),
                header("package/package.json", 0, '0'), block(text), new byte[1024]);
        assertThatThrownBy(() -> TarballFetcher.untar("<url>", paxSize, release("1.6.0")))
                .isInstanceOf(JsError.class)
                .hasMessage("<url>: Invalid tar header. Maybe the tar is corrupted or it needs to be gunzipped?");
        assertThat(new String(TarballFetcher.untar("<url>", paxSize, release("1.7.0")).get("package.json").content(),
                StandardCharsets.UTF_8)).isEqualTo(text);
    }

    private static String repeat(char c, int times) {
        char[] chars = new char[times];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    // a ustar header of a size and type, its content to follow
    private static byte[] header(String name, int size, char type) {
        byte[] header = new byte[512];
        put(header, 0, name);
        put(header, 100, "0000644");
        put(header, 108, "0000000");
        put(header, 116, "0000000");
        put(header, 124, String.format("%011o", size));
        put(header, 136, "00000000000");
        header[156] = (byte) type;
        put(header, 257, "ustar");
        put(header, 263, "00");
        Arrays.fill(header, 148, 156, (byte) ' ');
        int sum = 0;
        for (byte b : header) {
            sum += b & 0xff;
        }
        put(header, 148, String.format("%06o", sum));
        header[154] = 0;
        return header;
    }

    // text padded to whole blocks
    private static byte[] block(String text) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        return Arrays.copyOf(data, (data.length + 511) / 512 * 512);
    }

    // an uncompressed ustar archive of one file, or of one symbolic link to a target
    private static byte[] tar(String name, @Nullable String content, @Nullable String target) {
        byte[] entry = entry(name, content, target);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(entry, 0, entry.length);
        out.write(new byte[1024], 0, 1024);
        return out.toByteArray();
    }

    // a ustar entry: a directory where the name ends with a slash, else a file or a symbolic link to a target
    private static byte[] entry(String name, @Nullable String content, @Nullable String target) {
        byte[] data = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        boolean directory = name.endsWith("/");
        byte[] header = new byte[512];
        put(header, 0, name);
        put(header, 100, target == null && !directory ? "0000644" : "0000777");
        put(header, 108, "0000000");
        put(header, 116, "0000000");
        put(header, 124, String.format("%011o", data.length));
        put(header, 136, "00000000000");
        header[156] = (byte) (directory ? '5' : target == null ? '0' : '2');
        if (target != null) {
            put(header, 157, target);
        }
        put(header, 257, "ustar");
        put(header, 263, "00");
        Arrays.fill(header, 148, 156, (byte) ' ');
        int sum = 0;
        for (byte b : header) {
            sum += b & 0xff;
        }
        put(header, 148, String.format("%06o", sum));
        header[154] = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(header, 0, header.length);
        out.write(data, 0, data.length);
        out.write(new byte[(512 - data.length % 512) % 512], 0, (512 - data.length % 512) % 512);
        return out.toByteArray();
    }

    private static void put(byte[] header, int offset, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, header, offset, bytes.length);
    }
}
