/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlagTest {

    // ACC_PUBLIC | ACC_STATIC | ACC_VARARGS, e.g. java.util.Arrays.asList
    private static final long PUBLIC_STATIC_VARARGS = 0x0001 | 0x0008 | 0x0080;

    @Test
    void mapsAccVarargsToVarargsFlag() {
        long bitMap = Flag.mapBytecodeAccessFlagsToBitMap(PUBLIC_STATIC_VARARGS);
        assertThat(Flag.bitMapToFlags(bitMap))
          .contains(Flag.Public, Flag.Static, Flag.Varargs)
          .doesNotContain(Flag.Transient);
    }

    @Test
    void leavesNonVarargsFlagsUntouched() {
        long publicStatic = 0x0001 | 0x0008;
        assertThat(Flag.mapBytecodeAccessFlagsToBitMap(publicStatic)).isEqualTo(publicStatic);
    }
}
