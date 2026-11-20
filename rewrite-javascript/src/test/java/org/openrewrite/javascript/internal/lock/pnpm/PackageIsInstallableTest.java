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
import org.openrewrite.javascript.internal.lock.Host;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * config/package-is-installable/test/checkEngine.ts and checkPlatform.ts of pnpm 11.2.2, the latter under its
 * {@code familySync: () => 'musl'} mock, on a darwin x64 host; and cli/utils' packageIsInstallable over a project.
 */
class PackageIsInstallableTest {

    private static final String PACKAGE_ID = "registry.npmjs.org/foo/1.0.0";
    private static final Host HOST = Host.builder().os("darwin").cpu("x64").libc("musl").build();

    private static PackageIsInstallable.@Nullable UnsupportedEngineError checkEngine(String wanted, String node,
                                                                                    @Nullable String pnpm) {
        return PackageIsInstallable.checkEngine(PACKAGE_ID, obj(wanted), node, pnpm);
    }

    private static PackageIsInstallable.@Nullable UnsupportedPlatformError checkPlatform(String wanted) {
        return checkPlatform(wanted, null, HOST);
    }

    private static PackageIsInstallable.@Nullable UnsupportedPlatformError checkPlatform(
            String wanted, @Nullable String supportedArchitectures, Host host) {
        Map<String, @Nullable Object> supported = supportedArchitectures == null ? null : obj(supportedArchitectures);
        return PackageIsInstallable.checkPlatform(PACKAGE_ID, obj(wanted), supported, host);
    }

    @Test
    void noEngineDefined() {
        assertThat(checkEngine("{}", "0.2.1", "1.1.2")).isNull();
    }

    @Test
    void prereleaseNodeVersion() {
        assertThat(checkEngine("{'node':'^14.18.0 || >=16.0.0'}", "v21.0.0-nightly20230429c968361829", null)).isNull();
    }

    @Test
    void nodeVersionTooOld() {
        PackageIsInstallable.UnsupportedEngineError err = checkEngine("{'node':'0.10.24'}", "0.10.18", "1.1.2");
        assertThat(err).isNotNull();
        assertThat(err.getWanted().get("node")).isEqualTo("0.10.24");
    }

    @Test
    void nodeRangePassedInsteadOfVersion() {
        assertThatThrownBy(() -> checkEngine("{'node':'21.0.0'}", ">=20.0.0", null))
          .hasMessageContaining("The nodeVersion setting is");
    }

    @Test
    void pnpmVersionTooOld() {
        PackageIsInstallable.UnsupportedEngineError err = checkEngine("{'pnpm':'^1.4.6'}", "0.2.1", "1.3.2");
        assertThat(err).isNotNull();
        assertThat(err.getWanted().get("pnpm")).isEqualTo("^1.4.6");
    }

    @Test
    void pnpmIsAPrereleaseVersion() {
        assertThat(checkEngine("{'pnpm':'9'}", "0.2.1", "9.0.0-alpha.1")).isNull();
        assertThat(checkEngine("{'pnpm':'>=9'}", "0.2.1", "9.0.0-alpha.1")).isNull();
        assertThat(checkEngine("{'pnpm':'>=9.0.0'}", "0.2.1", "9.0.0-alpha.1")).isNotNull();
    }

    @Test
    void engineIsSupported() {
        assertThat(checkEngine("{'pnpm':'1','node':'10'}", "10.2.1", "1.3.2")).isNull();
    }

    @Test
    void targetCpuWrong() {
        PnpmError err = checkPlatform("{'cpu':'enten-cpu','os':'any','libc':'any'}");
        assertThat(err).isNotNull();
        assertThat(err.getCode()).isEqualTo("ERR_PNPM_UNSUPPORTED_PLATFORM");
    }

    @Test
    void osWrong() {
        PnpmError err = checkPlatform("{'cpu':'any','os':'enten-os','libc':'any'}");
        assertThat(err).isNotNull();
        assertThat(err.getCode()).isEqualTo("ERR_PNPM_UNSUPPORTED_PLATFORM");
    }

    @Test
    void libcWrong() {
        PnpmError err = checkPlatform("{'cpu':'any','os':'any','libc':'enten-libc'}");
        assertThat(err).isNotNull();
        assertThat(err.getCode()).isEqualTo("ERR_PNPM_UNSUPPORTED_PLATFORM");
    }

    @Test
    void cpuNull() {
        assertThat(checkPlatform("{'cpu':[null],'os':'any','libc':'any'}")).isNull();
    }

    @Test
    void osNull() {
        assertThat(checkPlatform("{'cpu':'any','os':[null],'libc':'any'}")).isNull();
    }

    @Test
    void libcNull() {
        assertThat(checkPlatform("{'cpu':'any','os':'any','libc':[null]}")).isNull();
    }

    @Test
    void nothingWrong() {
        assertThat(checkPlatform("{'cpu':'any','os':'any','libc':'any'}")).isNull();
    }

    @Test
    void onlyTargetCpuWrong() {
        PnpmError err = checkPlatform("{'cpu':'enten-cpu','os':'any','libc':'any'}");
        assertThat(err).isNotNull();
        assertThat(err.getCode()).isEqualTo("ERR_PNPM_UNSUPPORTED_PLATFORM");
    }

    @Test
    void onlyOsWrong() {
        PnpmError err = checkPlatform("{'cpu':'any','os':'enten-os','libc':'any'}");
        assertThat(err).isNotNull();
        assertThat(err.getCode()).isEqualTo("ERR_PNPM_UNSUPPORTED_PLATFORM");
    }

    @Test
    void everythingWrongWithArrays() {
        PnpmError err = checkPlatform("{'cpu':['enten-cpu'],'os':['enten-os'],'libc':['enten-libc']}");
        assertThat(err).isNotNull();
        assertThat(err.getCode()).isEqualTo("ERR_PNPM_UNSUPPORTED_PLATFORM");
    }

    @Test
    void osWrongNegation() {
        PnpmError err = checkPlatform("{'cpu':'any','os':'!darwin','libc':'any'}");
        assertThat(err).isNotNull();
        assertThat(err.getCode()).isEqualTo("ERR_PNPM_UNSUPPORTED_PLATFORM");
    }

    @Test
    void nothingWrongNegation() {
        assertThat(checkPlatform("{'cpu':'!enten-cpu','os':'!enten-os','libc':'!enten-libc'}")).isNull();
    }

    @Test
    void overrideOs() {
        assertThat(checkPlatform("{'cpu':'any','os':'win32','libc':'any'}",
          "{'os':['win32'],'cpu':['current'],'libc':['current']}", HOST)).isNull();
    }

    @Test
    void acceptAnotherCpu() {
        assertThat(checkPlatform("{'cpu':'x64','os':'any','libc':'any'}",
          "{'os':['current'],'cpu':['current','x64'],'libc':['current']}", HOST)).isNull();
    }

    @Test
    void failWhenCpuIsDifferent() {
        PnpmError err = checkPlatform("{'cpu':'x64','os':'any','libc':'any'}",
          "{'os':['current'],'cpu':['arm64'],'libc':['current']}", HOST);
        assertThat(err).isNotNull();
        assertThat(err.getCode()).isEqualTo("ERR_PNPM_UNSUPPORTED_PLATFORM");
    }

    @Test
    void overrideLibc() {
        assertThat(checkPlatform("{'cpu':'any','os':'any','libc':'glibc'}",
          "{'os':['current'],'cpu':['current'],'libc':['glibc']}", HOST)).isNull();
    }

    @Test
    void acceptAnotherLibc() {
        assertThat(checkPlatform("{'cpu':'any','os':'any','libc':'glibc'}",
          "{'os':['current'],'cpu':['current'],'libc':['current','glibc']}", HOST)).isNull();
    }

    @Test
    void acceptNegatedOsWithMultiValuedSupportedArchitectures() {
        Host linux = HOST.toBuilder().os("linux").build();
        assertThat(checkPlatform("{'cpu':'any','os':['!win32'],'libc':'any'}",
          "{'os':['linux','current'],'cpu':['current'],'libc':['current']}", linux)).isNull();
    }

    @Test
    void acceptNegatedCpuWithMultiValuedSupportedArchitectures() {
        Host x64 = HOST.toBuilder().cpu("x64").build();
        assertThat(checkPlatform("{'cpu':['!ia32'],'os':'any','libc':'any'}",
          "{'os':['current'],'cpu':['x64','current'],'libc':['current']}", x64)).isNull();
    }

    @Test
    void rejectNegatedOsWhenAnySupportedValueMatchesTheNegation() {
        Host darwin = HOST.toBuilder().os("darwin").build();
        PnpmError err = checkPlatform("{'cpu':'any','os':['!win32'],'libc':'any'}",
          "{'os':['win32','current'],'cpu':['current'],'libc':['current']}", darwin);
        assertThat(err).isNotNull();
        assertThat(err.getCode()).isEqualTo("ERR_PNPM_UNSUPPORTED_PLATFORM");
    }

    @Test
    void aDependencysEnginesPnpmIsNotChecked() {
        assertThat(PackageIsInstallable.packageIsInstallable(PACKAGE_ID, obj("{'engines':{'pnpm':'^9'}}"), true,
          "v24.19.0", false, null, HOST)).isTrue();
    }

    @Test
    void aProjectsEnginesPnpmThrowsWithoutEngineStrict() {
        assertThatThrownBy(() -> PackageIsInstallable.projectIsInstallable(".", obj("{'engines':{'pnpm':'^9'}}"),
          false, "v24.19.0", null, HOST))
          .isInstanceOf(PackageIsInstallable.UnsupportedEngineError.class)
          .hasMessage("Unsupported engine for .: wanted: {\"pnpm\":\"^9\"} (current: {\"node\":\"v24.19.0\"," +
                      "\"pnpm\":\"11.2.2\"})");
    }

    @Test
    void aProjectsPlatformOnlyWarns() {
        PackageIsInstallable.projectIsInstallable(".", obj("{'os':['win32'],'engines':{'pnpm':'^9'}}"), true,
          "v24.19.0", null, HOST);
    }

    @Test
    void aProjectsEnginesNodeThrowsUnderEngineStrict() {
        Map<String, @Nullable Object> manifest = obj("{'engines':{'node':'<10'}}");
        PackageIsInstallable.projectIsInstallable(".", manifest, false, "v24.19.0", null, HOST);
        assertThatThrownBy(() -> PackageIsInstallable.projectIsInstallable(".", manifest, true, "v24.19.0", null,
          HOST)).isInstanceOf(PackageIsInstallable.UnsupportedEngineError.class);
    }

    @Test
    void aNodeVersionTheHostDoesNotReportIsSatisfied() {
        PackageIsInstallable.projectIsInstallable(".", obj("{'engines':{'node':'<10'}}"), true, null, null,
          Host.UNKNOWN);
    }
}
