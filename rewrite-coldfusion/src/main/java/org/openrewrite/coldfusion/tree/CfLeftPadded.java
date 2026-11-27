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

public class CfLeftPadded {

    @RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    @Getter
    public enum Location {
        ASSIGNMENT_OPERATION_OPERATOR(CfSpace.Location.ASSIGNMENT_OPERATION_OPERATOR),
        BINARY_OPERATOR(CfSpace.Location.BINARY_OPERATOR),
        KEY_VALUE_SEPARATOR(CfSpace.Location.KEY_VALUE_SEPARATOR),
        TAG_ATTRIBUTE_VALUE(CfSpace.Location.TAG_ATTRIBUTE_VALUE);

        private final CfSpace.Location beforeLocation;
    }
}
