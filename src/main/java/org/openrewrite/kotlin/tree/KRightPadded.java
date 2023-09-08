/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.kotlin.tree;

public class KRightPadded {
    public enum Location {
        DESTRUCT_SUFFIX(KSpace.Location.DESTRUCT_SUFFIX),
        FUNCTION_TYPE_PARAMETER_SUFFIX(KSpace.Location.FUNCTION_TYPE_PARAMETER_SUFFIX),
        FUNCTION_TYPE_RECEIVER(KSpace.Location.FUNCTION_TYPE_RECEIVER),
        LIST_LITERAL_ELEMENT_SUFFIX(KSpace.Location.LIST_LITERAL_ELEMENT_SUFFIX),
        TOP_LEVEL_STATEMENT_SUFFIX(KSpace.Location.TOP_LEVEL_STATEMENT),
        WHEN_BRANCH_EXPRESSION(KSpace.Location.WHEN_BRANCH_EXPRESSION);

        private final KSpace.Location afterLocation;

        Location(KSpace.Location afterLocation) {
            this.afterLocation = afterLocation;
        }

        public KSpace.Location getAfterLocation() {
            return afterLocation;
        }
    }
}
