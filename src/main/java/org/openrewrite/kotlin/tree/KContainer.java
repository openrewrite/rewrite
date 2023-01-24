/*
 * Copyright 2023 the original author or authors.
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

public class KContainer {
    public enum Location {
        LIST_LITERAL_ELEMENTS(KSpace.Location.INFIX_FUNCTION_DECLARATION_RECEIVER, KRightPadded.Location.INFIX_FUNCTION_DECLARATION_RECEIVER),
        WHEN_BRANCH_EXPRESSION(KSpace.Location.WHEN_BRANCH_EXPRESSION, KRightPadded.Location.WHEN_BRANCH_EXPRESSION);

        private final KSpace.Location beforeLocation;
        private final KRightPadded.Location elementLocation;

        Location(KSpace.Location beforeLocation, KRightPadded.Location elementLocation) {
            this.beforeLocation = beforeLocation;
            this.elementLocation = elementLocation;
        }

        public KSpace.Location getBeforeLocation() {
            return beforeLocation;
        }

        public KRightPadded.Location getElementLocation() {
            return elementLocation;
        }
    }
}
