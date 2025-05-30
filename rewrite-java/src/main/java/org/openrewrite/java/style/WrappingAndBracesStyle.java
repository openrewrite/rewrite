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
package org.openrewrite.java.style;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaStyle;

@Value
@With
public class WrappingAndBracesStyle implements JavaStyle {

    IfStatement ifStatement;
    Annotations classAnnotations;
    Annotations methodAnnotations;
    Annotations fieldAnnotations;
    Annotations parameterAnnotations;
    Annotations localVariableAnnotations;
    Annotations enumFieldAnnotations;

    public IfStatement getIfStatement() {
        //noinspection ConstantConditions
        return ifStatement == null ? new IfStatement(false) : ifStatement;
    }

    @Value
    @With
    public static class IfStatement {
        Boolean elseOnNewLine;
    }
    public enum Wrap {
        DO_NOT_WRAP,
        // TODO implement hard wrap limits before we can implement the `IF_LONG` options
        WRAP_IF_LONG,
        CHOP_DOWN_IF_LONG,
        WRAP_ALWAYS
    }

    @Value
    @With
    @AllArgsConstructor
    public static class Annotations {
        Wrap wrap;
        @Nullable
        Boolean doNotWrapAfterSingleAnnotation;

        public Annotations(Wrap wrap) {
            this.wrap = wrap;
            this.doNotWrapAfterSingleAnnotation = null;
        }
    }
}
