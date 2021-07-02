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
package org.openrewrite.java.cleanup;

import lombok.Value;
import lombok.With;
import org.openrewrite.style.StyleHelper;
import org.openrewrite.style.Style;

import java.util.regex.Pattern;

@Value
@With
public class FallThroughStyle implements Style {
    /**
     * Control whether the last case group should be checked.
     */
    Boolean checkLastCaseGroup;

    /**
     * Ignores any fall-through commented with a text matching the regex pattern.
     * This is currently non-user-configurable, though held within {@link FallThroughStyle}.
     */
    static final Pattern RELIEF_PATTERN = Pattern.compile("falls?[ -]?thr(u|ough)");

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(fallThroughStyle(), this);
    }

    /**
     * The default of what would be part of {@link org.openrewrite.java.style.IntelliJ} (as of the time of writing) is to not
     * include this at all. Therefore, it is not included as a default there.
     *
     * @return instantiation of {@link FallThroughStyle} with default settings.
     */
    public static FallThroughStyle fallThroughStyle() {
        return new FallThroughStyle(false);
    }
}
