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
package org.openrewrite.python.internal.uvlock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class UvLockReachabilityTest {

    @Test
    void edgeTheParentsReachabilityContradictsLeavesTheLock() {
        UvLockPackage root = UvLockPackage.builder().name("root").version("0.1.0").source(UvLockSource.virtual("."))
          .dependencies(singletonList(new UvLockDependency("a", null, null, null, "python_full_version < '3.11'")))
          .build();
        UvLockPackage a = UvLockPackage.builder().name("a").version("1.0").source(UvLockSource.registry("https://index.example/simple/"))
          .dependencies(List.of(
            new UvLockDependency("b", null, null, null, "python_full_version >= '3.11'"),
            new UvLockDependency("c", null, null, null, "sys_platform == 'win32'")))
          .build();
        UvLockPackage b = UvLockPackage.builder().name("b").version("1.0").source(UvLockSource.registry("https://index.example/simple/")).build();
        UvLockPackage c = UvLockPackage.builder().name("c").version("1.0").source(UvLockSource.registry("https://index.example/simple/")).build();

        List<UvLockPackage> simplified = UvLockReachability.simplify(List.of(root, a, b, c), 0, PyRange.parse(">=3.10"), null);

        // b can never be reached through a, while c's platform clause is undecided and stays
        assertThat(simplified.get(1).getDependencies())
          .containsExactly(new UvLockDependency("c", null, null, null, "sys_platform == 'win32'"));
    }

    @Test
    void anEdgePairingValuesThatNeverHoldTogetherKeepsItsRecordedMarker() {
        UvLockPackage root = UvLockPackage.builder().name("root").version("0.1.0").source(UvLockSource.virtual("."))
          .dependencies(singletonList(new UvLockDependency("a", null, null, null, null)))
          .build();
        UvLockPackage a = UvLockPackage.builder().name("a").version("1.0").source(UvLockSource.registry("https://index.example/simple/"))
          .dependencies(List.of(
            new UvLockDependency("b", null, null, null, "(os_name == 'nt' and platform_machine != 'aarch64' and sys_platform == 'linux') or (os_name == 'nt' and sys_platform != 'darwin' and sys_platform != 'linux')"),
            new UvLockDependency("c", null, null, null, "python_full_version >= '3.9' and os_name == 'nt'")))
          .build();
        UvLockPackage b = UvLockPackage.builder().name("b").version("1.0").source(UvLockSource.registry("https://index.example/simple/")).build();
        UvLockPackage c = UvLockPackage.builder().name("c").version("1.0").source(UvLockSource.registry("https://index.example/simple/")).build();

        List<UvLockPackage> simplified = UvLockReachability.simplify(List.of(root, a, b, c), 0, PyRange.parse(">=3.10"), null);

        // b's marker reads as uv recorded it, since only the pruning of nt with linux would change it
        assertThat(simplified.get(1).getDependencies()).containsExactly(
          new UvLockDependency("b", null, null, null, "(os_name == 'nt' and platform_machine != 'aarch64' and sys_platform == 'linux') or (os_name == 'nt' and sys_platform != 'darwin' and sys_platform != 'linux')"),
          new UvLockDependency("c", null, null, null, "os_name == 'nt'"));
    }

    @Test
    void parentEqualityImpliesEdgeInequalityAndRootGroupsAreSimplified() {
        UvLockPackage root = UvLockPackage.builder().name("root").version("0.1.0").source(UvLockSource.virtual("."))
          .dependencies(singletonList(new UvLockDependency("a", null, null, null, "sys_platform == 'win32'")))
          .optionalDependencies(Map.of("fast", singletonList(
            new UvLockDependency("c", null, null, null, "python_full_version >= '3.10'"))))
          .build();
        UvLockPackage a = UvLockPackage.builder().name("a").version("1.0").source(UvLockSource.registry("https://index.example/simple/"))
          .dependencies(singletonList(new UvLockDependency("b", null, null, null, "sys_platform != 'darwin'")))
          .build();
        UvLockPackage b = UvLockPackage.builder().name("b").version("1.0").source(UvLockSource.registry("https://index.example/simple/")).build();
        UvLockPackage c = UvLockPackage.builder().name("c").version("1.0").source(UvLockSource.registry("https://index.example/simple/")).build();

        List<UvLockPackage> simplified = UvLockReachability.simplify(List.of(root, a, b, c), 0, PyRange.parse(">=3.10"), null);

        // a is only reached on win32, which already rules darwin out
        assertThat(simplified.get(1).getDependencies()).containsExactly(new UvLockDependency("b", null, null, null, null));

        // the root's own groups are reachable with the root, so requires-python decides their markers too
        assertThat(simplified.get(0).getOptionalDependencies().get("fast"))
          .containsExactly(new UvLockDependency("c", null, null, null, null));
    }

    // rows follow uv-lock's is_wheel_unreachable_for_marker and uv-platform-tags' classification
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "sys_platform == 'win32'                                  | x-1.0-py3-none-win_amd64.whl                          | false",
      "sys_platform == 'win32'                                  | x-1.0-py3-none-manylinux_2_17_x86_64.whl              | true",
      "sys_platform == 'win32'                                  | x-1.0-py3-none-macosx_11_0_arm64.whl                  | true",
      "sys_platform == 'win32'                                  | x-1.0-py3-none-any.whl                                | false",
      "platform_machine == 'x86_64' and sys_platform == 'linux' | x-1.0-cp312-cp312-manylinux_2_17_aarch64.whl          | true",
      "platform_machine == 'x86_64' and sys_platform == 'linux' | x-1.0-cp312-cp312-manylinux_2_17_x86_64.manylinux2014_x86_64.whl | false",
      "platform_machine == 'ARM64' and sys_platform == 'win32'  | x-1.0-cp312-cp312-win_amd64.whl                       | false",
      "sys_platform == 'darwin'                                 | x-1.0-cp312-cp312-macosx_10_13_universal2.whl         | false",
      "sys_platform == 'linux'                                  | x-1.0-cp312-cp312-freebsd_14_0_amd64.whl              | false",
      "platform_machine == 'x86_64' and sys_platform == 'linux' or platform_machine == 'aarch64' and sys_platform == 'darwin' | x-1.0-cp312-cp312-manylinux_2_17_aarch64.whl | true",
    })
    void wheelsTheReachabilityRulesOut(String marker, String wheel, boolean unreachable) {
        UvLockPackage root = UvLockPackage.builder().name("root").version("0.1.0").source(UvLockSource.virtual("."))
          .dependencies(singletonList(new UvLockDependency("x", null, null, null, marker)))
          .build();
        UvLockPackage x = UvLockPackage.builder().name("x").version("1.0").source(UvLockSource.registry("https://index.example/simple/")).build();

        UvLockReachability reachability = UvLockReachability.of(List.of(root, x), 0, PyRange.parse(">=3.10"), null);

        assertThat(reachability.wheelUnreachable(1, wheel)).isEqualTo(unreachable);
    }
}
