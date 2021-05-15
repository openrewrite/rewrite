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
import org.openrewrite.java.style.StyleHelper;
import org.openrewrite.style.Style;

@Value
@With
public class DefaultComesLastStyle implements Style {
    /**
     * Whether to allow the default label to be not last if it's evaluation is shared with a case.
     */
    Boolean skipIfLastAndSharedWithCase;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(defaultComesLastStyle(), this);
    }

    /**
     * The default of what would be part of {@link org.openrewrite.java.style.IntelliJ} (as the time of writing) is to not
     * inspect on whether the "default" case comes last in a switch block at all. Therefore, it is not included as a default there.
     *
     * @return instantiation of DefaultComesLastStyle with default settings.
     */
    public static DefaultComesLastStyle defaultComesLastStyle() {
        return new DefaultComesLastStyle(false);
    }
}
