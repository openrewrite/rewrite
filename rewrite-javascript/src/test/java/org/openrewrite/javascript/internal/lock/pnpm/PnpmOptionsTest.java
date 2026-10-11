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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * pnpm 11.2.2's config/reader tests getOptionsFromRootManifest, env, parseCreds, getNetworkConfigs and the project
 * config cases of index.ts, and @pnpm/config.env-replace 4.1.0's env-replace.spec.ts.
 */
class PnpmOptionsTest {

    private static Map<String, String> env(String... pairs) {
        Map<String, String> env = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            env.put(pairs[i], pairs[i + 1]);
        }
        return env;
    }

    // getOptionsFromRootManifest.test.ts

    @Test
    void getOptionsFromPnpmSettingsReplacesEnvVariablesInSettings() {
        Map<String, @Nullable Object> options = PnpmOptions.getOptionsFromPnpmSettings(
          obj("{'${PNPM_TEST_KEY}':'${PNPM_TEST_VALUE}'}"), null,
          env("PNPM_TEST_KEY", "foo", "PNPM_TEST_VALUE", "bar"));
        assertThat(options.get("foo")).isEqualTo("bar");
    }

    @Test
    void getOptionsFromPnpmSettingsExpandsEnvVariablesInsideRegistriesValues() {
        Map<String, @Nullable Object> options = PnpmOptions.getOptionsFromPnpmSettings(
          obj("{'registries':{'default':'https://registry.npmjs.org/'," +
              "'@scope':'https://registry.example.com/${PNPM_TEST_TOKEN}/'}}"), null, env("PNPM_TEST_TOKEN", "secret"));
        assertThat(((Map<?, ?>) options.get("registries")).get("@scope"))
          .isEqualTo("https://registry.example.com/secret/");
    }

    @Test
    void getOptionsFromPnpmSettingsExpandsEnvVariablesInsideNamedRegistriesValues() {
        Map<String, @Nullable Object> options = PnpmOptions.getOptionsFromPnpmSettings(
          obj("{'namedRegistries':{'work':'https://${PNPM_TEST_HOST}/npm/'}}"), null,
          env("PNPM_TEST_HOST", "work.example.com"));
        assertThat(((Map<?, ?>) options.get("namedRegistries")).get("work")).isEqualTo("https://work.example.com/npm/");
    }

    @Test
    void getOptionsFromPnpmSettingsConvertsAllowBuilds() {
        assertThat(json(PnpmOptions.getOptionsFromPnpmSettings(
          obj("{'allowBuilds':{'foo':true,'bar':false,'qar':'warn'}}"), null, env())))
          .isEqualTo("{'allowBuilds':{'foo':true,'bar':false,'qar':'warn'}}");
    }

    @Test
    void getOptionsFromPnpmSettingsRejectsNonStringOverridesValues() {
        assertThatThrownBy(() -> PnpmOptions.getOptionsFromPnpmSettings(obj("{'overrides':{'foo':null}}"), null,
          env()))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_OVERRIDES"))
          .hasMessage("The value of overrides.foo should be a string, but got null");
    }

    @Test
    void getOptionsFromPnpmSettingsRejectsArrayOverridesValues() {
        assertThatThrownBy(() -> PnpmOptions.getOptionsFromPnpmSettings(obj("{'overrides':{'foo':[]}}"), null,
          env()))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_OVERRIDES"))
          .hasMessage("The value of overrides.foo should be a string, but got array");
    }

    @Test
    void getOptionsFromPnpmSettingsRejectsNonObjectOverridesValues() {
        assertThatThrownBy(() -> PnpmOptions.getOptionsFromPnpmSettings(obj("{'overrides':[]}"), null, env()))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_OVERRIDES"))
          .hasMessage("The overrides field should be an object, but got array");
    }

    // env.test.ts

    private static final String HOME = "/srv/fake-user";
    private static final String PATH =
      "/bin:/usr/bin:/usr/local/bin:/srv/fake-user/.bin:/srv/fake-user/share/local/bin";

    private static Function<String, @Nullable Object> alwaysSchema(Object schema) {
        return key -> {
            assertThat(key).matches("[a-z0-9]+(-[a-z0-9]+)*");
            return schema;
        };
    }

    // the pairs as an object, a value no variant parses printed as undefined
    private static String pairsToObject(List<PnpmOptions.ConfigPair> pairs) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        for (PnpmOptions.ConfigPair pair : pairs) {
            out.put(pair.key, pair.value == Js.UNDEFINED ? "<undefined>" : pair.value);
        }
        return json(out);
    }

    @Test
    void parseEnvVarsWorksWithStrings() {
        Map<String, String> env = env("HOME", HOME, "PATH", PATH, "pnpm_config_abc_def_ghi", "value of abcDefGhi",
          "pnpm_config_foo", "value of foo", "pnpm_config_bar", "value of bar");
        env.put("pnpm_config_undefined_somehow", null);
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(ConfigTypes.Type.STRING), env)))
          .isEqualTo("{'abcDefGhi':'value of abcDefGhi','foo':'value of foo','bar':'value of bar'}");
    }

    @Test
    void parseEnvVarsWorksWithNumbers() {
        Map<String, String> env = env("HOME", HOME, "PATH", PATH, "pnpm_config_abc_def_ghi", "123",
          "pnpm_config_foo", "456", "pnpm_config_bar", "789");
        env.put("pnpm_config_undefined_somehow", null);
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(ConfigTypes.Type.NUMBER), env)))
          .isEqualTo("{'abcDefGhi':123,'foo':456,'bar':789}");
    }

    @Test
    void parseEnvVarsWorksWithBooleans() {
        Map<String, String> env = env("HOME", HOME, "PATH", PATH, "pnpm_config_foo", "false",
          "pnpm_config_bar", "true", "pnpm_config_baz", "not a boolean");
        env.put("pnpm_config_undefined_somehow", null);
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(ConfigTypes.Type.BOOLEAN), env)))
          .isEqualTo("{'foo':false,'bar':true,'baz':'<undefined>'}");
    }

    @Test
    void parseEnvVarsWorksWithArrays() {
        Map<String, String> env = env("HOME", HOME, "PATH", PATH, "pnpm_config_foo", "[0, 1, 2]",
          "pnpm_config_bar", "[\"a\", \"b\"]", "pnpm_config_baz", "not an array");
        env.put("pnpm_config_undefined_somehow", null);
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(ConfigTypes.Type.ARRAY), env)))
          .isEqualTo("{'foo':[0,1,2],'bar':['a','b'],'baz':'<undefined>'}");
    }

    @Test
    void parseEnvVarsWorksWithPaths() {
        Map<String, String> env = env("HOME", HOME, "PATH", PATH, "pnpm_config_foo", "abc/def/ghi",
          "pnpm_config_bar", "~/abc/def/ghi", "pnpm_config_baz", "~\\abc\\def\\ghi");
        env.put("pnpm_config_undefined_somehow", null);
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(ConfigTypes.Type.PATH), env)))
          .isEqualTo("{'foo':'abc/def/ghi','bar':'/srv/fake-user/abc/def/ghi','baz':'/srv/fake-user/abc\\def\\ghi'}");

        env = env("pnpm_config_foo", "abc/def/ghi", "pnpm_config_bar", "~/abc/def/ghi",
          "pnpm_config_baz", "~\\abc\\def\\ghi");
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(ConfigTypes.Type.PATH), env)))
          .isEqualTo("{'foo':'abc/def/ghi','bar':'~/abc/def/ghi','baz':'~\\abc\\def\\ghi'}");
    }

    // Node 24.19.0: path.join('/srv/fake-user', '') is '/srv/fake-user'; with 'a/' it keeps the slash
    @Test
    void aPathSettingOfTheHomeDirectoryIsJoinedAsNodeJoinsIt() {
        Map<String, String> env = env("HOME", HOME, "pnpm_config_foo", "~/", "pnpm_config_bar", "~/a/");
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(ConfigTypes.Type.PATH), env)))
          .isEqualTo("{'foo':'/srv/fake-user','bar':'/srv/fake-user/a/'}");
    }

    @Test
    void parseEnvVarsWorksWithUrls() {
        Map<String, String> env = env("HOME", HOME, "PATH", PATH, "pnpm_config_foo", "https://registry.npmjs.com",
          "pnpm_config_bar", "http://example.org", "pnpm_config_baz", "file:///path/to/some/local/file");
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(ConfigTypes.Type.URL), env)))
          .isEqualTo("{'foo':'https://registry.npmjs.com/','bar':'http://example.org/'," +
                     "'baz':'file:///path/to/some/local/file'}");
    }

    @Test
    void parseEnvVarsWorksWithLiterals() {
        Map<String, String> env = env("HOME", HOME, "PATH", PATH, "pnpm_config_a", "foo", "pnpm_config_b", "bar",
          "pnpm_config_c", "baz", "pnpm_config_d", "false", "pnpm_config_e", "true", "pnpm_config_f", "null");
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(asList("foo", "bar", true, ConfigTypes.NULL)),
          env)))
          .isEqualTo("{'a':'foo','b':'bar','c':'<undefined>','d':'<undefined>','e':true,'f':null}");
    }

    @Test
    void parseEnvVarsWorksWithUnion() {
        Map<String, String> env = env("HOME", HOME, "PATH", PATH, "pnpm_config_a", "foo", "pnpm_config_b", "bar",
          "pnpm_config_c", "baz", "pnpm_config_d", "123", "pnpm_config_e", "456", "pnpm_config_f", "[0, 1, \"abc\"]");
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(asList("foo", "bar", ConfigTypes.Type.NUMBER,
          ConfigTypes.Type.ARRAY)), env)))
          .isEqualTo("{'a':'foo','b':'bar','c':'<undefined>','d':123,'e':456,'f':[0,1,'abc']}");
    }

    @Test
    void parseEnvVarsPrioritizesParsingNonStrings() {
        Map<String, String> env = env("HOME", HOME, "PATH", PATH, "pnpm_config_a", "foo", "pnpm_config_b", "bar",
          "pnpm_config_c", "baz", "pnpm_config_d", "123", "pnpm_config_e", "456", "pnpm_config_f", "[0, 1, \"abc\"]");
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(asList(ConfigTypes.Type.STRING,
          ConfigTypes.Type.NUMBER, ConfigTypes.Type.ARRAY, ConfigTypes.Type.BOOLEAN)), env)))
          .isEqualTo("{'a':'foo','b':'bar','c':'baz','d':123,'e':456,'f':[0,1,'abc']}");
    }

    @Test
    void parseEnvVarsSkipsUndefinedSchema() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("foo", ConfigTypes.Type.STRING);
        dict.put("bar", ConfigTypes.Type.STRING);
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(dict::get, env("pnpm_config_foo", "from foo",
          "pnpm_config_bar", "from bar", "pnpm_config_baz", "from baz"))))
          .isEqualTo("{'foo':'from foo','bar':'from bar'}");
    }

    @Test
    void parseEnvVarsSkipsNpmConfig() {
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(ConfigTypes.Type.STRING),
          env("npm_config_abc_def_ghi", "value of abcDefGhi", "npm_config_foo", "value of foo",
            "npm_config_bar", "value of bar"))))
          .isEqualTo("{}");
    }

    @Test
    void parseEnvVarsReadsFullyLowercaseOrFullyUppercaseSnakeCaseKeys() {
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(ConfigTypes.Type.STRING), env(
          "PNPM_CONFIG_UPPER_SNAKE_CASE_KEY", "whole key in upper snake case",
          "pnpmConfigCamelCaseKey", "whole key in snake case",
          "pnpm-config-kebab-case", "whole key in kebab case",
          "pnpm_config_UPPER_SNAKE_CASE_SUFFIX", "mixed case suffix",
          "PNPM_CONFIG_lower_snake_case_suffix", "mixed case suffix",
          "pnpm_config_camelCaseSuffix", "suffix in camel case",
          "pnpm_config_kebab-case-suffix", "suffix in kebab case",
          "pnpm_config_lower_snake_case_key", "whole key in lower snake case"))))
          .isEqualTo("{'upperSnakeCaseKey':'whole key in upper snake case'," +
                     "'lowerSnakeCaseKey':'whole key in lower snake case'}");
    }

    @Test
    void parseEnvVarsSkipsKeysThatContainMultipleConsecutiveUnderscoreCharacters() {
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(ConfigTypes.Type.STRING),
          env("pnpm_config_foo__bar", "foo bar", "pnpm_config_abc_def__ghi___jkl", "abc def ghi jkl"))))
          .isEqualTo("{}");
    }

    @Test
    void parseEnvVarsSkipsKeysThatEndWithUnderscoreCharacter() {
        assertThat(pairsToObject(PnpmOptions.parseEnvVars(alwaysSchema(ConfigTypes.Type.STRING),
          env("pnpm_config_foo_bar_", "foo bar"))))
          .isEqualTo("{}");
    }

    // index.ts

    // upstream reads these from a userconfig; the project's .npmrc gives the same registries
    @Test
    void nodeVersionHasNoDefault() {
        assertThat(PnpmOptions.load(null, null, obj("{}"), env()).get("nodeVersion")).isNull();
    }

    @Test
    void nodeVersionIsTheMinimumVersionOfTheNodeRuntime() {
        String[][] cases = {
          {"devEngines", "22.20.0", "download", "22.20.0"},
          {"devEngines", "22.20.0", "error", "22.20.0"},
          {"devEngines", "^22.0.0", "download", "22.0.0"},
          {"engines", "22.20.0", "download", "22.20.0"},
        };
        for (String[] c : cases) {
            Map<String, @Nullable Object> manifest = obj("{'" + c[0] + "':{'runtime':{'name':'node','version':'" +
                                                         c[1] + "','onFail':'" + c[2] + "'}}}");
            assertThat(PnpmOptions.load(null, null, manifest, env()).get("nodeVersion"))
              .as("%s %s %s", (Object[]) c).isEqualTo(c[3]);
        }
    }

    // upstream sets it on the command line; pnpm-workspace.yaml is read after the manifest too, and the environment
    // never sets it (envPnpmTypes omits node-version)
    @Test
    void nodeVersionFromConfigTakesPriorityOverDevEnginesRuntime() {
        Map<String, @Nullable Object> manifest = obj("{'devEngines':{'runtime':{'name':'node','version':'22.20.0'," +
                                                     "'onFail':'download'}}}");
        assertThat(PnpmOptions.load("nodeVersion: 20.0.0\n", null, manifest, env()).get("nodeVersion"))
          .isEqualTo("20.0.0");
        assertThat(PnpmOptions.load(null, null, manifest, env("pnpm_config_node_version", "20.0.0"))
          .get("nodeVersion")).isEqualTo("22.20.0");
    }

    // upstream sets runtime-on-fail on the command line; pnpm-workspace.yaml sets it too, and the install applies it to
    // every project's manifest (PnpmLockRegenerator.install) where config/reader applies it to the root's
    @Test
    void runtimeOnFailDownloadOverridesDevEnginesRuntimeOnFailAndAddsNodeToDevDependencies() {
        Map<String, @Nullable Object> manifest = obj("{'devEngines':{'runtime':{'name':'node','version':'22.20.0'}}}");
        PnpmOptions options = PnpmOptions.load("runtimeOnFail: download\n", null, manifest, env());
        assertThat(options.get("runtimeOnFail")).isEqualTo("download");
        PkgManifestUtils.applyRuntimeOnFailOverride(manifest, options.get("runtimeOnFail"));
        assertThat(json(manifest)).isEqualTo("{'devEngines':{'runtime':{'name':'node','version':'22.20.0'," +
                                             "'onFail':'download'}},'devDependencies':{'node':'runtime:22.20.0'}}");
    }

    @Test
    void runtimeOnFailIgnoreOverridesAnExistingOnFailDownloadAndRemovesNodeFromDevDependencies() {
        Map<String, @Nullable Object> manifest = obj("{'devEngines':{'runtime':{'name':'node','version':'22.20.0'," +
                                                     "'onFail':'download'}}}");
        PnpmOptions options = PnpmOptions.load("runtimeOnFail: ignore\n", null, manifest, env());
        assertThat(options.get("runtimeOnFail")).isEqualTo("ignore");
        PkgManifestUtils.applyRuntimeOnFailOverride(manifest, options.get("runtimeOnFail"));
        assertThat(json(manifest)).isEqualTo("{'devEngines':{'runtime':{'name':'node','version':'22.20.0'," +
                                             "'onFail':'ignore'}},'devDependencies':{}}");
    }

    @Test
    void devEnginesPackageManagerWithoutOnFailResolvesToTheDocumentedPmOnFailDefaultDownload() {
        PnpmOptions.WantedPackageManager pm = PnpmOptions.load(null, null, obj("{'devEngines':{'packageManager':" +
          "{'name':'pnpm','version':'11.0.0'}}}"), env()).getWantedPackageManager();
        assertThat(pm).isNotNull();
        assertThat(pm.getName()).isEqualTo("pnpm");
        assertThat(pm.getVersion()).isEqualTo("11.0.0");
        assertThat(pm.getOnFail()).isEqualTo("download");
    }

    @Test
    void devEnginesPackageManagerWithExplicitOnFailIsRespected() {
        PnpmOptions.WantedPackageManager pm = PnpmOptions.load(null, null, obj("{'devEngines':{'packageManager':" +
          "{'name':'pnpm','version':'11.0.0','onFail':'error'}}}"), env()).getWantedPackageManager();
        assertThat(pm).isNotNull();
        assertThat(pm.getOnFail()).isEqualTo("error");
    }

    @Test
    void registriesOfScopedPackagesAreReadAndNormalized() {
        String npmrc = "@foo:registry=https://foo.com\n@bar:registry=https://bar.com\n" +
                       "@qar:registry=https://qar.com/qar\nregistry=https://default.com\n";
        PnpmOptions options = PnpmOptions.load(null, npmrc, null, env());
        assertThat(json(options.getRegistries())).isEqualTo("{'default':'https://default.com/'," +
                                                           "'@jsr':'https://npm.jsr.io/','@foo':'https://foo.com/'," +
                                                           "'@bar':'https://bar.com/','@qar':'https://qar.com/qar'}");
    }

    @Test
    void registriesInCurrentDirectorysNpmrcHaveBiggerPriorityThenGlobalConfigSettings() {
        String userConfig = "@foo:registry=https://foo.com\n@bar:registry=https://bar.com\n" +
                            "@qar:registry=https://qar.com/qar\nregistry=https://default.com\n";
        PnpmOptions options = PnpmOptions.load(null, "registry=https://pnpm.io/", userConfig, null, env());
        assertThat(json(options.getRegistries())).isEqualTo("{'default':'https://pnpm.io/'," +
                                                           "'@jsr':'https://npm.jsr.io/','@foo':'https://foo.com/'," +
                                                           "'@bar':'https://bar.com/','@qar':'https://qar.com/qar'}");
    }

    @Test
    void theUsersNpmrcAloneNamesTheRegistries() {
        PnpmOptions options = PnpmOptions.load(null, null, "registry=https://registry.npmmirror.com/\n", null, env());
        assertThat(options.getRegistries().get("default")).isEqualTo("https://registry.npmmirror.com/");
    }

    @Test
    void aTokenHelperIsReadFromTheUsersNpmrcOnly() {
        String userConfig = "//r.example.com/:tokenHelper=/srv/helper\n";
        PnpmOptions.load(null, null, userConfig, null, env());
        assertThatThrownBy(() -> PnpmOptions.load(null, "//r.example.com/:tokenHelper=/srv/other\n", userConfig, null,
          env()))
          .isInstanceOf(PnpmError.class)
          .hasMessageContaining("tokenHelper must not be configured in project-level .npmrc");
    }

    @Test
    void pnpmWorkspaceYamlRegistriesOverrideTheSameScopeFromNpmrc() {
        PnpmOptions options = PnpmOptions.load("registries:\n  '@my-org': https://from-workspace-yaml.example.com/\n",
          "@my-org:registry=https://from-npmrc.example.com/", null, env());
        assertThat(options.getRegistries().get("@my-org")).isEqualTo("https://from-workspace-yaml.example.com/");
    }

    @Test
    void pnpmWorkspaceYamlRegistriesDefaultIsReflectedInConfigRegistry() {
        PnpmOptions options = PnpmOptions.load("registries:\n  default: https://private.example.com/\n", null, null,
          env());
        assertThat(options.get("registry")).isEqualTo("https://private.example.com/");
        assertThat(options.getRegistries().get("default")).isEqualTo("https://private.example.com/");
    }

    @Test
    void npmrcDoesNotLoadPnpmSettings() {
        PnpmOptions options = PnpmOptions.load(null, String.join("\n",
          "//my-org.registry.example.com:username=some-employee",
          "//my-org.registry.example.com:_authToken=some-employee-token",
          "@my-org:registry=https://my-org.registry.example.com",
          "@jsr:registry=https://not-actually-jsr.example.com",
          "username=example-user-name",
          "_authToken=example-auth-token",
          "dlx-cache-max-age=1234",
          "trust-policy-exclude[]=foo",
          "trust-policy-exclude[]=bar",
          "packages[]=baz",
          "packages[]=qux"), null, env());
        assertThat(options.getRegistries().get("@my-org")).isEqualTo("https://my-org.registry.example.com/");
        assertThat(options.getRegistries().get("@jsr")).isEqualTo("https://not-actually-jsr.example.com/");
        assertThat(options.get("dlxCacheMaxAge")).isNull();
        assertThat(options.get("trustPolicyExclude")).isNull();
        assertThat(options.get("packages")).isNull();
    }

    /**
     * A token pnpm substitutes from the environment reaches no lock; a registry it substitutes does.
     */
    @Test
    void anEnvironmentTheHostDoesNotKnow() {
        PnpmOptions options = PnpmOptions.load(null, "//registry.npmjs.org/:_authToken=${NPM_TOKEN}\n", null, null);
        assertThat(options.getRegistries().get("default")).isEqualTo("https://registry.npmjs.org/");
        assertThatThrownBy(() -> PnpmOptions.load(null, "registry=${REGISTRY}\n", null, null))
          .isInstanceOf(CannotKnow.class);
        assertThat(PnpmOptions.load(null, "registry=${REGISTRY}\n", null, env("REGISTRY", "https://r.example.com"))
          .getRegistries().get("default")).isEqualTo("https://r.example.com/");
    }

    @Test
    void minimumReleaseAgeStrictDefaultsToTrueWhenMinimumReleaseAgeIsSetInPnpmWorkspaceYaml() {
        PnpmOptions options = PnpmOptions.load("minimumReleaseAge: 60\n", null, null, env());
        assertThat(options.get("minimumReleaseAge")).isEqualTo(60.0);
        assertThat(options.get("minimumReleaseAgeStrict")).isEqualTo(true);
    }

    @Test
    void minimumReleaseAgeStrictDefaultsToTrueWhenMinimumReleaseAgeIsSetViaEnvVar() {
        PnpmOptions options = PnpmOptions.load(null, null, null, env("pnpm_config_minimum_release_age", "60"));
        assertThat(options.get("minimumReleaseAge")).isEqualTo(60.0);
        assertThat(options.get("minimumReleaseAgeStrict")).isEqualTo(true);
    }

    @Test
    void minimumReleaseAgeStrictRespectsAnExplicitFalseFromPnpmWorkspaceYaml() {
        PnpmOptions options = PnpmOptions.load("minimumReleaseAge: 60\nminimumReleaseAgeStrict: false\n", null,
          null, env());
        assertThat(options.get("minimumReleaseAge")).isEqualTo(60.0);
        assertThat(options.get("minimumReleaseAgeStrict")).isEqualTo(false);
    }

    @Test
    void minimumReleaseAgeStrictIsUnsetWhenOnlyTheBuiltInDefaultApplies() {
        PnpmOptions options = PnpmOptions.load("{}\n", null, null, env());
        assertThat(options.get("minimumReleaseAge")).isEqualTo(1440.0);
        assertThat(options.get("minimumReleaseAgeStrict")).isNull();
    }

    // env-replace.spec.ts

    private static final Map<String, String> ENV = env("foo", "foo_value", "bar", "bar_value", "zoo", "");

    @Test
    void envReplaceSucceeds() {
        String[][] rows = {
          {"-${foo}-${bar}", "-foo_value-bar_value"},
          {"\\${foo}", "${foo}"},
          {"\\${zoo}", "${zoo}"},
          {"\\\\${foo}", "\\foo_value"},
          {"-${foo-fallback-value}-${bar:-fallback-value}", "-foo_value-bar_value"},
          {"-${qar-fallback-value}-${zoo-fallback-value}", "-fallback-value-"},
          {"-${qar-fallback-value}-${zoo:-fallback-for-empty-value}", "-fallback-value-fallback-for-empty-value"},
        };
        for (String[] row : rows) {
            assertThat(PnpmOptions.envReplace(row[0], ENV)).as(row[0]).isEqualTo(row[1]);
        }
    }

    @Test
    void envReplaceFailsWhenTheEnvVariableIsNotFound() {
        assertThatThrownBy(() -> PnpmOptions.envReplace("${baz}", ENV))
          .hasMessage("Failed to replace env in config: ${baz}");
        assertThatThrownBy(() -> PnpmOptions.envReplace("${foo-}", ENV))
          .hasMessage("Failed to replace env in config: ${foo-}");
        assertThatThrownBy(() -> PnpmOptions.envReplace("${foo:-}", ENV))
          .hasMessage("Failed to replace env in config: ${foo:-}");
    }

    @Test
    void envReplaceTreatsAPresentButUndefinedValueAsUnset() {
        Map<String, String> env = new LinkedHashMap<>(ENV);
        env.put("qux", null);
        assertThat(PnpmOptions.envReplace("${qux-fallback}", env)).isEqualTo("fallback");
        assertThat(PnpmOptions.envReplace("${qux:-fallback}", env)).isEqualTo("fallback");
        assertThatThrownBy(() -> PnpmOptions.envReplace("${qux}", env))
          .hasMessage("Failed to replace env in config: ${qux}");
    }

    @Test
    void envReplaceLossy() {
        assertThat(PnpmOptions.envReplaceLossy("${baz}", ENV)).isEmpty();
        assertThat(PnpmOptions.envReplaceLossy("${foo}-${baz}-${qux-fallback}", ENV)).isEqualTo("foo_value--fallback");
        assertThat(PnpmOptions.envReplaceLossy("${a}-${b}-${a}", env())).isEqualTo("--");
        assertThat(PnpmOptions.envReplaceLossy("-${foo}-${bar}-", ENV)).isEqualTo("-foo_value-bar_value-");
        assertThat(PnpmOptions.envReplaceLossy("\\${baz}", ENV)).isEqualTo("${baz}");
        assertThat(PnpmOptions.envReplaceLossy("\\\\${foo}", ENV)).isEqualTo("\\foo_value");
    }

    // parseCreds.test.ts: the port keeps what parseCreds throws, not the creds it returns, which reach no lock

    private static String btoa(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    void parseCredsOfUsableCredentials() {
        assertThatCode(() -> {
            PnpmOptions.parseCreds(env());
            PnpmOptions.parseCreds(env("authToken", "example auth token"));
            PnpmOptions.parseCreds(env("authToken", ""));
            PnpmOptions.parseCreds(env("authPairBase64", btoa("foo:bar")));
            PnpmOptions.parseCreds(env("authPairBase64", btoa("foo:bar:baz")));
            PnpmOptions.parseCreds(env("authPairBase64", btoa("foo:bar") + "="));
            PnpmOptions.parseCreds(env("authUsername", "foo", "authPassword", btoa("bar")));
            PnpmOptions.parseCreds(env("authUsername", "foo"));
            PnpmOptions.parseCreds(env("authPassword", "bar"));
            PnpmOptions.parseCreds(env("authUsername", "foo", "authPassword", btoa("bar") + "="));
            PnpmOptions.parseCreds(env("tokenHelper", "example-token-helper --foo --bar baz"));
            PnpmOptions.parseCreds(env("tokenHelper", "./example-token-helper.sh --foo --bar baz"));
            PnpmOptions.parseCreds(env("tokenHelper", "node ./example-token-helper.js --foo --bar baz"));
            PnpmOptions.parseCreds(env("tokenHelper", "./example-token-helper.sh"));
        }).doesNotThrowAnyException();
    }

    @Test
    void authPairBase64MustHaveASeparator() {
        assertThatThrownBy(() -> PnpmOptions.parseCreds(env("authPairBase64", btoa("foo"))))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_AUTH_MISSING_SEPARATOR"));
    }

    @Test
    void authPairBase64AndAuthPasswordMustBeBase64() {
        assertThatThrownBy(() -> PnpmOptions.parseCreds(env("authPairBase64", "foo*bar")))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_AUTH_INVALID_BASE64"))
          .hasMessage("Failed to decode _auth as base64");
        assertThatThrownBy(() -> PnpmOptions.parseCreds(env("authUsername", "foo", "authPassword", "bar*baz")))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_AUTH_INVALID_BASE64"))
          .hasMessage("Failed to decode _password as base64");
    }

    @Test
    void tokenHelperDoesNotSupportEnvironmentVariablesOrQuotations() {
        for (String[] helper : new String[][]{{"example-token-helper $MY_VAR", "$"},
          {"example-token-helper \"hello world\"", "\""}, {"example-token-helper 'hello world'", "'"}}) {
            assertThatThrownBy(() -> PnpmOptions.parseCreds(env("tokenHelper", helper[0])))
              .isInstanceOfSatisfying(PnpmError.class,
                e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_TOKEN_HELPER_UNSUPPORTED_CHARACTER"))
              .hasMessage("Unexpected character " + Js.jsonStringify(helper[1]));
        }
    }

    // getNetworkConfigs.test.ts: the registries; configByUri (TLS and creds) reaches no lock

    private static final String FOO = "'@foo:registry':'https://example.com/foo'";

    private static Map<String, String> networkRegistries(String... keys) {
        return PnpmOptions.getNetworkConfigs(obj("{" + String.join(",", keys) + "}"));
    }

    @Test
    void getNetworkConfigsWithoutFiles() {
        assertThat(networkRegistries()).isEmpty();
        assertThat(networkRegistries(FOO)).containsExactly(entry("@foo", "https://example.com/foo"));
        assertThat(networkRegistries("'//example.com/foo:ca':'some-ca'")).isEmpty();
        assertThat(networkRegistries("'//example.com/foo:cert':'some-cert'")).isEmpty();
        assertThat(networkRegistries(FOO, "'//example.com/foo:ca':'some-ca'", "'//example.com/foo:cert':'some-cert'"))
          .containsExactly(entry("@foo", "https://example.com/foo"));
    }

    /**
     * Upstream reads the files; the port does not have the host's file system.
     */
    @Test
    void getNetworkConfigsWithFiles() {
        assertThatThrownBy(() -> networkRegistries(FOO, "'//example.com/foo:cafile':'cafile'",
          "'//example.com/foo:certfile':'certfile'"))
          .isInstanceOf(CannotKnow.class);
    }

    @Test
    void getNetworkConfigsAuthAndTlsCombined() {
        for (String creds : new String[]{"'//example.com/foo:_authToken':'example auth token'",
          "'//example.com/foo:_auth':'" + btoa("foo:bar") + "'",
          "'//example.com/foo:username':'foo','//example.com/foo:_password':'" + btoa("bar") + "'",
          "'//example.com/foo:tokenHelper':'node ./my-token-helper.cjs'"}) {
            assertThat(networkRegistries(FOO, creds)).containsExactly(entry("@foo", "https://example.com/foo"));
        }
        assertThat(networkRegistries("'//example.com/foo:_authToken':'token'", "'//example.com/foo:cert':'some-cert'",
          "'//example.com/foo:key':'some-key'")).isEmpty();
    }

    @Test
    void getNetworkConfigsUnsupportedKey() {
        assertThat(networkRegistries(FOO, "'//example.com/foo:someUnsupportedKey':'hello world'"))
          .containsExactly(entry("@foo", "https://example.com/foo"));
    }
}
