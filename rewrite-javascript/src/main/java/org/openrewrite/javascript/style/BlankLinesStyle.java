/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.javascript.style;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.JavaScriptStyle;

@Value
@With
public class BlankLinesStyle implements JavaScriptStyle {
    KeepMaximum keepMaximum;
    Minimum minimum;

    @Value
    @With
    public static class KeepMaximum {
        Integer inCode;
    }

    @Value
    @With
    public static class Minimum {
        Integer afterImports;
        Integer aroundClass;

        @Nullable
        Integer aroundFieldInInterface;

        Integer aroundField;

        @Nullable
        Integer aroundMethodInInterface;

        Integer aroundMethod;
        Integer aroundFunction;
    }
}
