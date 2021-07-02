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
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.style.StyleHelper;
import org.openrewrite.style.Style;

@Value
public class EqualsAvoidsNullStyle implements Style {
    Boolean ignoreEqualsIgnoreCase;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(IntelliJ.equalsAvoidsNull(), this);
    }
}
