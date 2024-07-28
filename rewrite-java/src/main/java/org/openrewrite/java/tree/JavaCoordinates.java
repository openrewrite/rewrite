/*
 * Copyright 2020 the original author or authors.
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

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.template.Coordinates;

import java.util.Comparator;

@Data
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class JavaCoordinates implements Coordinates {
    J tree;
    Space.Location spaceLocation;
    Mode mode;

    @Nullable
    Comparator<? extends J> comparator;

    public boolean isReplacement() {
        return Mode.REPLACEMENT.equals(mode);
    }

    /**
     * Determines whether we are replacing a whole tree element, and not either
     * (1) replacing just a piece of a method, class, or variable declaration signature or
     * (2) inserting a new element
     */
    public boolean isReplaceWholeCursorValue() {
        return isReplacement() && spaceLocation == null;
    }

    public enum Mode {
        AFTER,
        BEFORE,
        REPLACEMENT
    }

    public <J2 extends J> @Nullable Comparator<J2> getComparator() {
        //noinspection unchecked
        return (Comparator<J2>) comparator;
    }
}
