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
package org.openrewrite.groovy.tree;

public class GRightPadded {
    public enum Location {
        LIST_LITERAL_ELEMENT_SUFFIX(GSpace.Location.LIST_LITERAL_ELEMENT_SUFFIX),
        MAP_ENTRY_KEY(GSpace.Location.MAP_ENTRY_KEY_SUFFIX),
        MAP_LITERAL_ELEMENT_SUFFIX(GSpace.Location.MAP_LITERAL_ELEMENT_SUFFIX),
        TOP_LEVEL_STATEMENT_SUFFIX(GSpace.Location.TOP_LEVEL_STATEMENT),
        TUPLE_ELEMENT_SUFFIX(GSpace.Location.TUPLE_ELEMENT_SUFFIX);

        private final GSpace.Location afterLocation;

        Location(GSpace.Location afterLocation) {
            this.afterLocation = afterLocation;
        }

        public GSpace.Location getAfterLocation() {
            return afterLocation;
        }
    }
}
