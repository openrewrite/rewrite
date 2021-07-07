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
package org.openrewrite.java.cleanup;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Value;
import lombok.With;
import org.openrewrite.java.JavaStyle;
import org.openrewrite.java.style.PadPolicy;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

@Value
@With
public class EmptyForInitializerStyle implements JavaStyle {
    private static final EmptyForInitializerStyle INSTANCE =
            new EmptyForInitializerStyle(
                   PadPolicy.NOSPACE
            );

    @JsonCreator
    public static EmptyForInitializerStyle defaults() {
        return INSTANCE;
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(INSTANCE, this);
    }

    PadPolicy option;
}
