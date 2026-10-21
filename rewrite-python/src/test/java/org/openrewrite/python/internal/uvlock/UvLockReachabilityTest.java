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

        List<UvLockPackage> simplified = UvLockReachability.simplify(List.of(root, a, b, c), 0, PyRange.parse(">=3.10"));

        // b can never be reached through a, while c's platform clause is undecided and stays
        assertThat(simplified.get(1).getDependencies())
          .containsExactly(new UvLockDependency("c", null, null, null, "sys_platform == 'win32'"));
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

        List<UvLockPackage> simplified = UvLockReachability.simplify(List.of(root, a, b, c), 0, PyRange.parse(">=3.10"));

        // a is only reached on win32, which already rules darwin out
        assertThat(simplified.get(1).getDependencies()).containsExactly(new UvLockDependency("b", null, null, null, null));

        // the root's own groups are reachable with the root, so requires-python decides their markers too
        assertThat(simplified.get(0).getOptionalDependencies().get("fast"))
          .containsExactly(new UvLockDependency("c", null, null, null, null));
    }
}
