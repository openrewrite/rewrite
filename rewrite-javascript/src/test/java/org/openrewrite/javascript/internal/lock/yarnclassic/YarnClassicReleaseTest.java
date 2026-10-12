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

    // an uncompressed ustar archive of one file, or of one symbolic link to a target
    private static byte[] tar(String name, @Nullable String content, @Nullable String target) {
        byte[] data = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        byte[] header = new byte[512];
        put(header, 0, name);
        put(header, 100, target == null ? "0000644" : "0000777");
        put(header, 108, "0000000");
        put(header, 116, "0000000");
        put(header, 124, String.format("%011o", data.length));
        put(header, 136, "00000000000");
        header[156] = (byte) (target == null ? '0' : '2');
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
        out.write(new byte[(512 - data.length % 512) % 512 + 1024], 0, (512 - data.length % 512) % 512 + 1024);
        return out.toByteArray();
    }

    private static void put(byte[] header, int offset, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, header, offset, bytes.length);
    }
}
