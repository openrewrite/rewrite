/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.json.format.LineWrapSetting;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

@Value
@With
public class WrappingAndBracesStyle implements JsonStyle {
    public static final WrappingAndBracesStyle DEFAULT = new WrappingAndBracesStyle(LineWrapSetting.WrapAlways, LineWrapSetting.WrapAlways);
    public static final WrappingAndBracesStyle OBJECTS_WRAP_ARRAYS_DONT = new WrappingAndBracesStyle(LineWrapSetting.WrapAlways, LineWrapSetting.DoNotWrap);

    LineWrapSetting wrapObjects;
    LineWrapSetting wrapArrays;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(DEFAULT, this);
    }
}
