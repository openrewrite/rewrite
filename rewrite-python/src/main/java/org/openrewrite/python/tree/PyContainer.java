/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.python.tree;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class PyContainer {
    @Getter
    @RequiredArgsConstructor
    public enum Location {
        COLLECTION_LITERAL_ELEMENTS(PySpace.Location.COLLECTION_LITERAL_PREFIX, PyRightPadded.Location.COLLECTION_LITERAL_ELEMENT),
        DICT_LITERAL_ELEMENTS(PySpace.Location.DICT_LITERAL_PREFIX, PyRightPadded.Location.DICT_LITERAL_ELEMENT),
        MATCH_CASE_PATTERN_CHILDREN(PySpace.Location.MATCH_CASE_PATTERN_CHILD_PREFIX, PyRightPadded.Location.MATCH_CASE_PATTERN_CHILD),
        MULTI_IMPORT_NAMES(PySpace.Location.MULTI_IMPORT_NAME_PREFIX, PyRightPadded.Location.MULTI_IMPORT_NAME),
        ;

        private final PySpace.Location beforeLocation;
        private final PyRightPadded.Location elementLocation;
    }
}
