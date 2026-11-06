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
package org.openrewrite.coldfusion.tree;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class CfRightPadded {

    @RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    @Getter
    public enum Location {
        INVOCATION_ARGUMENT_SUFFIX(CfSpace.Location.INVOCATION_ARGUMENT_SUFFIX),
        STRUCT_LITERAL_ENTRY_SUFFIX(CfSpace.Location.STRUCT_LITERAL_ENTRY_SUFFIX);

        private final CfSpace.Location afterLocation;
    }
}
