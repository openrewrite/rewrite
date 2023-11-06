/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.ruby.tree;

public class RubyRightPadded {
    public enum Location {
        KEY_VALUE_SUFFIX(RubySpace.Location.KEY_VALUE_SUFFIX),
        LIST_LITERAL_SUFFIX(RubySpace.Location.LIST_LITERAL_SUFFIX),
        MULTIPLE_ASSIGNMENT_SUFFIX(RubySpace.Location.MULTIPLE_ASSIGNMENT_SUFFIX),
        MULTIPLE_ASSIGNMENT_INITIALIZERS_SUFFIX(RubySpace.Location.MULTIPLE_ASSIGNMENT_INITIALIZERS_SUFFIX),
        YIELD_DATA_SUFFIX(RubySpace.Location.YIELD_DATA_SUFFIX);

        private final RubySpace.Location afterLocation;

        Location(RubySpace.Location afterLocation) {
            this.afterLocation = afterLocation;
        }

        public RubySpace.Location getAfterLocation() {
            return afterLocation;
        }
    }
}
