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

import lombok.Value;
import lombok.With;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

@Value
@With
public class MethodParamPadStyle implements Style {
    /**
     * Whether to add spacing between the method declarations (or method invocations) identifier and the start of the left parenthesis.
     * <p>
     * When true:
     * <p>
     * {@code method (a, b);}
     * <p>
     * When false:
     * <p>
     * {@code method(a, b);}
     */
    Boolean space;

    /**
     * Whether to allow a linebreak between the method declaration (or method invocation) identifier and the start of the left parenthesis.
     * Note this does not add a linebreak if one is missing. It only controls whether a linebreak is permitted.
     * <p>
     * When true:
     * <pre>{@code method // acceptable
     *      (a, b);
     * }</pre>
     * <p>
     * When false, the linebreak would be removed, becoming:
     * <p>
     * {@code method(a, b);}
     * <p>
     */
    Boolean allowLineBreaks;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.methodParamPadStyle(), this);
    }

}
