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
package org.openrewrite.coldfusion.style;

import lombok.Value;
import lombok.With;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

/**
 * The blank lines between statements and declarations, in script and between tags.
 */
@Value
@With
public class BlankLinesStyle implements Style {
    KeepMaximum keepMaximum;
    Minimum minimum;

    @Value
    @With
    public static class KeepMaximum {
        /**
         * Between the functions and properties of a component.
         */
        Integer inDeclarations;

        /**
         * Between statements.
         */
        Integer inCode;

        /**
         * Before the <code>}</code> or end tag that closes a body.
         */
        Integer beforeEndOfBlock;
    }

    @Value
    @With
    public static class Minimum {
        /**
         * Around a function declared among a component's other declarations.
         */
        Integer aroundFunction;
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(CFFormat.blankLines(), this);
    }
}
