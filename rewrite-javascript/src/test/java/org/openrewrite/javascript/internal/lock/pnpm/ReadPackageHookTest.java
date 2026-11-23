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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.sortedJson;

/**
 * pnpm 11.2.2's hooks/read-package-hook tests, less the overrider's absolute-path cases: the port works on paths
 * relative to the lock's directory.
 */
class ReadPackageHookTest {

    private static String sorted(ReadPackageHook hook, String manifest, @Nullable String dir) {
        return json(KeySorting.sortDeepKeys(hook.apply(obj(manifest), dir)));
    }

    private static final ReadPackageHook PACKAGE_EXTENDER = ReadPackageHook.packageExtender(obj(
      "{'foo':{'dependencies':{'a':'1'},'optionalDependencies':{'b':'2'},'peerDependencies':{'c':'3'}," +
      "'peerDependenciesMeta':{'c':{'optional':true}}},'bar@1':{'dependencies':{'d':'1'}}," +
      "'qar':{'dependencies':{'e':'1','f':'1'}}}"));

    @Test
    void createPackageExtenderExtendsTheSupportedFields() {
        assertThat(sorted(PACKAGE_EXTENDER, "{'name':'foo','dependencies':{'bar':'^1.0.0'}}", null))
          .isEqualTo(sortedJson("{'name':'foo','dependencies':{'bar':'^1.0.0','a':'1'}," +
                                "'optionalDependencies':{'b':'2'},'peerDependencies':{'c':'3'}," +
                                "'peerDependenciesMeta':{'c':{'optional':true}}}"));
    }

    @Test
    void createPackageExtenderDoesNotChangePackagesThatShouldNotBeExtended() {
        assertThat(sorted(PACKAGE_EXTENDER, "{'name':'ignore'}", null)).isEqualTo(sortedJson("{'name':'ignore'}"));
    }

    @Test
    void createPackageExtenderMatchesByVersion() {
        assertThat(sorted(PACKAGE_EXTENDER, "{'name':'bar','version':'1.0.0'}", null))
          .isEqualTo(sortedJson("{'name':'bar','version':'1.0.0','dependencies':{'d':'1'}}"));
    }

    @Test
    void createPackageExtenderDoesNotOverrideExistingFields() {
        assertThat(sorted(PACKAGE_EXTENDER, "{'name':'qar','version':'1.0.0','dependencies':{'e':'100'}}", null))
          .isEqualTo(sortedJson("{'name':'qar','version':'1.0.0','dependencies':{'e':'100','f':'1'}}"));
    }

    @Test
    void createPackageExtenderShouldWorkForOddNames() {
        ReadPackageHook extender = ReadPackageHook.packageExtender(obj("{'constructor':{'dependencies':{'foo':'1'}}}"));
        assertThat(sorted(extender, "{'name':'constructor','dependencies':{}}", null))
          .isEqualTo(sortedJson("{'name':'constructor','dependencies':{'foo':'1'}}"));
    }

    private static final String ALL = "{'dependencies':{'foo':'0.1.2','bar':'2.1.0','baz':'1.0.0','qux':'2.0.0'}," +
                                      "'optionalDependencies':{'foo':'0.1.2','bar':'2.1.0','baz':'1.0.0'}}";

    @Test
    void createOptionalDependenciesRemover() {
        assertThat(sorted(ReadPackageHook.optionalDependenciesRemover(Arrays.asList("foo", "bar")), ALL, null))
          .isEqualTo(sortedJson("{'dependencies':{'baz':'1.0.0','qux':'2.0.0'}," +
                                "'optionalDependencies':{'baz':'1.0.0'}}"));
        assertThat(sorted(ReadPackageHook.optionalDependenciesRemover(Arrays.asList("foo", "bar")),
          "{'dependencies':{'foo':'0.1.2','bar':'2.1.0','baz':'1.0.0','qux':'2.0.0'}," +
          "'optionalDependencies':{'foo':'0.1.2','baz':'1.0.0'}}", null))
          .isEqualTo(sortedJson("{'dependencies':{'bar':'2.1.0','baz':'1.0.0','qux':'2.0.0'}," +
                                "'optionalDependencies':{'baz':'1.0.0'}}"));
        assertThat(sorted(ReadPackageHook.optionalDependenciesRemover(Collections.singletonList("*")), ALL, null))
          .isEqualTo(sortedJson("{'dependencies':{'qux':'2.0.0'},'optionalDependencies':{}}"));
        assertThat(sorted(ReadPackageHook.optionalDependenciesRemover(Arrays.asList("@foo/*", "@bar/*")),
          "{'dependencies':{'@foo/abc':'0.0.0','@foo/def':'0.0.0','@foo/not-optional':'0.0.0','@bar/ghi':'0.0.0'," +
          "'@bar/required':'0.0.0','@baz/jkl':'0.0.0'}," +
          "'optionalDependencies':{'@foo/abc':'0.0.0','@foo/def':'0.0.0','@bar/ghi':'0.0.0'," +
          "'@baz/jkl':'0.0.0'}}", null))
          .isEqualTo(sortedJson("{'dependencies':{'@foo/not-optional':'0.0.0','@bar/required':'0.0.0'," +
                                "'@baz/jkl':'0.0.0'},'optionalDependencies':{'@baz/jkl':'0.0.0'}}"));
    }

    private static ParseOverrides.VersionOverride override(@Nullable String parent, @Nullable String parentSpec,
                                                           String target, @Nullable String targetSpec, String newSpec) {
        return new ParseOverrides.VersionOverride("",
          parent == null ? null : new ParseOverrides.PackageSelector(parent, parentSpec),
          new ParseOverrides.PackageSelector(target, targetSpec), newSpec);
    }

    private static ReadPackageHook overrider(ParseOverrides.VersionOverride... overrides) {
        return ReadPackageHook.versionsOverrider(new ArrayList<>(Arrays.asList(overrides)), "");
    }

    @Test
    void createVersionsOverriderMatchesSubRanges() {
        ReadPackageHook overrider = overrider(override(null, null, "foo", "2", "2.12.0"),
          override(null, null, "qar", ">2", "1.0.0"));
        assertThat(sorted(overrider, "{'dependencies':{'foo':'^2.10.0'},'optionalDependencies':{'qar':'^4.0.0'}}",
          null)).isEqualTo(sortedJson("{'dependencies':{'foo':'2.12.0'},'optionalDependencies':{'qar':'1.0.0'}}"));
    }

    @Test
    void createVersionsOverriderDoesNotFailOnNonRangeSelectors() {
        ReadPackageHook overrider = overrider(override(null, null, "foo", "2", "2.12.0"),
          override(null, null, "bar", "github:org/bar", "2.12.0"));
        assertThat(sorted(overrider, "{'dependencies':{'foo':'github:org/foo','bar':'github:org/bar'}}", null))
          .isEqualTo(sortedJson("{'dependencies':{'foo':'github:org/foo','bar':'2.12.0'}}"));
    }

    @Test
    void createVersionsOverriderOverridesDependenciesOfSpecifiedPackagesOnly() {
        ReadPackageHook overrider = overrider(override("foo", "1", "bar", "^1.2.0", "3.0.0"),
          override("qar", "1", "bar", ">4", "3.0.0"));
        assertThat(sorted(overrider, "{'name':'foo','version':'1.2.0','dependencies':{'bar':'^1.2.0'}}", null))
          .isEqualTo(sortedJson("{'name':'foo','version':'1.2.0','dependencies':{'bar':'3.0.0'}}"));
        assertThat(sorted(overrider, "{'name':'foo','version':'2.0.0','dependencies':{'bar':'^1.2.0'}}", null))
          .isEqualTo(sortedJson("{'name':'foo','version':'2.0.0','dependencies':{'bar':'^1.2.0'}}"));
        assertThat(sorted(overrider, "{'name':'qar','version':'1.0.0','dependencies':{'bar':'^10.0.0'}}", null))
          .isEqualTo(sortedJson("{'name':'qar','version':'1.0.0','dependencies':{'bar':'3.0.0'}}"));
        assertThat(sorted(overrider, "{'name':'qar','version':'1.0.0','dependencies':{'bar':'^4.0.0'}}", null))
          .isEqualTo(sortedJson("{'name':'qar','version':'1.0.0','dependencies':{'bar':'^4.0.0'}}"));
    }

    @Test
    void createVersionsOverriderOverridesAllTypesOfDependencies() {
        ReadPackageHook overrider = overrider(override(null, null, "foo", null, "3.0.0"),
          override(null, null, "bar", null, "3.0.0"), override(null, null, "qar", null, "3.0.0"));
        assertThat(sorted(overrider, "{'name':'foo','version':'1.2.0','dependencies':{'foo':'^1.2.0'}," +
                                     "'optionalDependencies':{'bar':'^1.2.0'}," +
                                     "'devDependencies':{'qar':'^1.2.0'}}", null))
          .isEqualTo(sortedJson("{'name':'foo','version':'1.2.0','dependencies':{'foo':'3.0.0'}," +
                                "'optionalDependencies':{'bar':'3.0.0'},'devDependencies':{'qar':'3.0.0'}}"));
    }

    @Test
    void createVersionsOverriderOverridesDependenciesWithLinks() {
        assertThat(sorted(overrider(override(null, null, "qar", null, "link:../qar")),
          "{'name':'foo','version':'1.2.0','dependencies':{'qar':'3.0.0'}}", "pkg"))
          .isEqualTo(sortedJson("{'name':'foo','version':'1.2.0','dependencies':{'qar':'link:../../qar'}}"));
    }

    @Test
    void createVersionsOverriderOverridesDependencyOfPkgMatchedByNameAndVersion() {
        assertThat(sorted(overrider(override("yargs", "^7.1.0", "yargs-parser", null, "^20.0.0")),
          "{'name':'yargs','version':'7.1.0','dependencies':{'yargs-parser':'19'}}", null))
          .isEqualTo(sortedJson("{'name':'yargs','version':'7.1.0','dependencies':{'yargs-parser':'^20.0.0'}}"));
        assertThat(sorted(overrider(override("yargs", "^8.1.0", "yargs-parser", null, "^20.0.0")),
          "{'name':'yargs','version':'7.1.0','dependencies':{'yargs-parser':'19'}}", null))
          .isEqualTo(sortedJson("{'name':'yargs','version':'7.1.0','dependencies':{'yargs-parser':'19'}}"));
    }

    @Test
    void createVersionsOverriderWorksForScopedAndUnscopedNames() {
        assertThat(sorted(overrider(override("@scoped/package", null, "unscoped-package", null, "workspace:*")),
          "{'name':'@scoped/package','version':'1.0.0','dependencies':{'unscoped-package':'1.0.0'}}", null))
          .isEqualTo(sortedJson("{'name':'@scoped/package','version':'1.0.0','dependencies':" +
                                "{'unscoped-package':'workspace:*'}}"));
        assertThat(sorted(overrider(override("unscoped-package", null, "@scoped/package", null, "workspace:*")),
          "{'name':'unscoped-package','version':'1.0.0','dependencies':{'@scoped/package':'1.0.0'}}", null))
          .isEqualTo(sortedJson("{'name':'unscoped-package','version':'1.0.0','dependencies':" +
                                "{'@scoped/package':'workspace:*'}}"));
        assertThat(sorted(overrider(override("@scoped/package", null, "@scoped/package2", null, "workspace:*")),
          "{'name':'@scoped/package','version':'1.0.0','dependencies':{'@scoped/package2':'1.0.0'}}", null))
          .isEqualTo(sortedJson("{'name':'@scoped/package','version':'1.0.0','dependencies':" +
                                "{'@scoped/package2':'workspace:*'}}"));
    }

    @Test
    void createVersionsOverriderOverridesDependenciesWithFileWithRelativePath() {
        ReadPackageHook overrider = overrider(override(null, null, "qar", null, "file:../qar"));
        assertThat(sorted(overrider, "{'name':'foo','version':'1.2.0','dependencies':{'qar':'3.0.0'}}", ""))
          .isEqualTo(sortedJson("{'name':'foo','version':'1.2.0','dependencies':{'qar':'file:../qar'}}"));
        assertThat(sorted(overrider, "{'name':'foo','version':'1.2.0','dependencies':{'qar':'3.0.0'}}", "packages/pkg"))
          .isEqualTo(sortedJson("{'name':'foo','version':'1.2.0','dependencies':{'qar':'file:../../../qar'}}"));
    }

    @Test
    void createVersionOverrideShouldUseTheMostSpecificRule() {
        ReadPackageHook overrider = overrider(override(null, null, "foo", null, "3.0.0"),
          override(null, null, "foo", "3", "4.0.0"), override(null, null, "foo", "2", "2.12.0"),
          override("bar", null, "foo", "2", "github:org/foo"), override("bar", null, "foo", "3", "5.0.0"));
        assertThat(sorted(overrider, "{'dependencies':{'foo':'^3.0.0'}}", null))
          .isEqualTo(sortedJson("{'dependencies':{'foo':'4.0.0'}}"));
        assertThat(sorted(overrider, "{'dependencies':{'foo':'^4.0.0'}}", null))
          .isEqualTo(sortedJson("{'dependencies':{'foo':'3.0.0'}}"));
        assertThat(sorted(overrider, "{'dependencies':{'foo':'^2.0.0'}}", null))
          .isEqualTo(sortedJson("{'dependencies':{'foo':'2.12.0'}}"));
        assertThat(sorted(overrider, "{'name':'bar','version':'1.0.0','dependencies':{'foo':'^2.0.0'}}", null))
          .isEqualTo(sortedJson("{'name':'bar','version':'1.0.0','dependencies':{'foo':'github:org/foo'}}"));
        assertThat(sorted(overrider, "{'name':'bar','version':'1.0.0','dependencies':{'foo':'^3.0.0'}}", null))
          .isEqualTo(sortedJson("{'name':'bar','version':'1.0.0','dependencies':{'foo':'5.0.0'}}"));
    }

    @Test
    void createVersionsOverriderMatchesIntersections() {
        assertThat(sorted(overrider(override(null, null, "foo", "<1.2.4", ">=1.2.4")),
          "{'dependencies':{'foo':'^1.2.3'}}", null)).isEqualTo(sortedJson("{'dependencies':{'foo':'>=1.2.4'}}"));
    }

    @Test
    void createVersionsOverriderOverridesPeerDependenciesOfAnotherDependency() {
        assertThat(sorted(overrider(override("react-dom", null, "react", null, "18.1.0")),
          "{'name':'react-dom','version':'18.2.0','peerDependencies':{'react':'18.2.0'}}", null))
          .isEqualTo(sortedJson("{'name':'react-dom','version':'18.2.0','dependencies':{}," +
                                "'peerDependencies':{'react':'18.1.0'}}"));
    }

    @Test
    void createVersionsOverriderRemovesDependencies() {
        ReadPackageHook overrider = overrider(override(null, null, "foo", null, "-"),
          override("bar", null, "baz", null, "-"), override(null, null, "qux", "2", "-"));
        assertThat(sorted(overrider, "{'dependencies':{'foo':'0.1.2','bar':'1.2.3','baz':'1.0.0','qux':'2.1.0'}}",
          null)).isEqualTo(sortedJson("{'dependencies':{'bar':'1.2.3','baz':'1.0.0'}}"));
        assertThat(sorted(overrider, "{'name':'bar','dependencies':{'foo':'0.1.2','bar':'1.2.3','baz':'1.0.0'," +
                                     "'qux':'2.1.0'}}", null))
          .isEqualTo(sortedJson("{'name':'bar','dependencies':{'bar':'1.2.3'}}"));
        assertThat(sorted(overrider, "{'dependencies':{'foo':'0.1.2','bar':'1.2.3','baz':'1.0.0','qux':'3.2.1'}}",
          null)).isEqualTo(sortedJson("{'dependencies':{'bar':'1.2.3','baz':'1.0.0','qux':'3.2.1'}}"));
    }

    @Test
    void createVersionsOverriderMovesInvalidVersionsFromPeerDependenciesToDependencies() {
        ReadPackageHook overrider = overrider(override(null, null, "foo", null, "link:foo"),
          override(null, null, "bar", null, "file:bar"), override(null, null, "baz", null, "7.7.7"));
        assertThat(sorted(overrider, "{'peerDependencies':{'foo':'^1.0.0 || ^2.0.0','bar':'^1.0.0 || ^2.0.0'," +
                                     "'baz':'^1.0.0 || ^2.0.0','qux':'^1.0.0 || ^2.0.0'}}", null))
          .isEqualTo(sortedJson("{'dependencies':{'foo':'link:foo','bar':'file:bar'},'peerDependencies':{" +
                                "'bar':'^1.0.0 || ^2.0.0','baz':'7.7.7','foo':'^1.0.0 || ^2.0.0'," +
                                "'qux':'^1.0.0 || ^2.0.0'}}"));
        assertThat(sorted(overrider, "{'dependencies':{'foo':'^1.0.0','bar':'^2.0.0','baz':'^1.2.3','qux':'^2.1.0'}," +
                                     "'peerDependencies':{'foo':'^1.0.0 || ^2.0.0','bar':'^1.0.0 || ^2.0.0'," +
                                     "'baz':'^1.0.0 || ^2.0.0','qux':'^1.0.0 || ^2.0.0'}}", null))
          .isEqualTo(sortedJson("{'dependencies':{'foo':'link:foo','bar':'file:bar','baz':'7.7.7','qux':'^2.1.0'}," +
                                "'peerDependencies':{'bar':'^1.0.0 || ^2.0.0','baz':'7.7.7','foo':'^1.0.0 || ^2.0.0'," +
                                "'qux':'^1.0.0 || ^2.0.0'}}"));
    }

    /**
     * The compatibility database is applied unless ignored: {@code debug@<4.2.0} gains {@code supports-color} as an
     * optional peer, as pnpm 11.2.2 installed it (recorded).
     */
    @Test
    void createReadPackageHookAppliesTheCompatibilityDatabase() {
        List<ParseOverrides.VersionOverride> none = new ArrayList<>();
        ReadPackageHook hook = ReadPackageHook.createReadPackageHook(false, "", none, null, null);
        assertThat(sorted(hook, "{'name':'debug','version':'2.6.9','dependencies':{'ms':'2.0.0'}}", null))
          .isEqualTo(sortedJson("{'name':'debug','version':'2.6.9','dependencies':{'ms':'2.0.0'}," +
                                "'peerDependenciesMeta':{'supports-color':{'optional':true}}}"));
        assertThat(ReadPackageHook.createReadPackageHook(true, "", none, null, null)).isNull();
    }
}
