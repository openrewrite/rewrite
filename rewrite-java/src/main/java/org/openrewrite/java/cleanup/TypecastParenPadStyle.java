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
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

@Value
@With
public class TypecastParenPadStyle implements Style {
    /**
     * Whether to add spacing between the typecast identifier and both the left and right parenthesis.
     * <p>
     * When true:
     * <p>
     * {@code ( int ) m;}
     * <p>
     * When false:
     * <p>
     * {@code (int) m;}
     */
    Boolean space;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.typecastParenPadStyle(), this);
    }
}
