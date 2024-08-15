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
package org.openrewrite.hcl.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;
import org.openrewrite.template.Coordinates;

import java.util.Comparator;

@Data
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class HclCoordinates implements Coordinates {
    Hcl tree;
    Space.Location spaceLocation;
    Mode mode;

    @Nullable
    Comparator<? extends Hcl> comparator;

    public boolean isReplacement() {
        return Mode.REPLACEMENT.equals(mode);
    }

    public enum Mode {
        AFTER,
        BEFORE,
        REPLACEMENT
    }

    public <H extends Hcl> @Nullable Comparator<H> getComparator() {
        //noinspection unchecked
        return (Comparator<H>) comparator;
    }
}
