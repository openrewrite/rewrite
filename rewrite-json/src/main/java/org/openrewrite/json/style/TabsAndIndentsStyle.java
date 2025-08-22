/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.json.style;

import lombok.Value;
import lombok.With;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

@Value
@With
public class TabsAndIndentsStyle implements JsonStyle {
    public static final TabsAndIndentsStyle DEFAULT = new TabsAndIndentsStyle(false, 2, 2);

    Boolean useTabCharacter;
    Integer tabSize;
    Integer indentSize;
    // TODO Decide whether something like continuation indent in JSON makes sense. Currently not used.
    // Although IntelliJ settings panel does mention this notion.
    // Integer continuationIndentSize;

    public String singleIndent() {
        if (getUseTabCharacter()) {
            return "\t";
        }
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < getIndentSize(); j++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(DEFAULT, this);
    }
}
