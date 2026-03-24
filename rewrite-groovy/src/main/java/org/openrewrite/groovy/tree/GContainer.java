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

public class GContainer {
    public enum Location {
        LIST_LITERAL_ELEMENTS(GSpace.Location.LIST_LITERAL_ELEMENTS, GRightPadded.Location.LIST_LITERAL_ELEMENT_SUFFIX),
        MAP_LITERAL_ELEMENTS(GSpace.Location.MAP_LITERAL_ELEMENTS, GRightPadded.Location.MAP_LITERAL_ELEMENT_SUFFIX),
        TUPLE_ELEMENTS(GSpace.Location.TUPLE_ELEMENTS, GRightPadded.Location.TUPLE_ELEMENT_SUFFIX);

        private final GSpace.Location beforeLocation;
        private final GRightPadded.Location elementLocation;

        Location(GSpace.Location beforeLocation, GRightPadded.Location elementLocation) {
            this.beforeLocation = beforeLocation;
            this.elementLocation = elementLocation;
        }

        public GSpace.Location getBeforeLocation() {
            return beforeLocation;
        }

        public GRightPadded.Location getElementLocation() {
            return elementLocation;
        }
    }
}
