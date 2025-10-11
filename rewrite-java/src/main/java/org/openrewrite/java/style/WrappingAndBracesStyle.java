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

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaStyle;
import org.openrewrite.style.LineWrapSetting;

import java.util.List;

import static java.util.Collections.emptyList;

@Value
@With
public class WrappingAndBracesStyle implements JavaStyle {

    IfStatement ifStatement;
    ChainedMethodCalls chainedMethodCalls;
    @Nullable Annotations classAnnotations;
    @Nullable Annotations methodAnnotations;
    @Nullable Annotations fieldAnnotations;
    @Nullable Annotations parameterAnnotations;
    @Nullable Annotations localVariableAnnotations;
    @Nullable Annotations enumFieldAnnotations;

    public IfStatement getIfStatement() {
        //noinspection ConstantConditions
        return ifStatement == null ? new IfStatement(false) : ifStatement;
    }

    public ChainedMethodCalls getChainedMethodCalls() {
        //noinspection ConstantConditions
        return chainedMethodCalls == null ? new ChainedMethodCalls(LineWrapSetting.DoNotWrap, emptyList()) : chainedMethodCalls;
    }

    @Value
    @With
    public static class IfStatement {
        Boolean elseOnNewLine;
    }

    @Value
    @With
    public static class Annotations {
        LineWrapSetting wrap;
    }

    @Value
    @With
    public static class ChainedMethodCalls {
        LineWrapSetting wrap;
        List<String> builderMethods;

        public LineWrapSetting getWrap() {
            //noinspection ConstantConditions
            return wrap == null ? LineWrapSetting.DoNotWrap : wrap;
        }

        public List<String> getBuilderMethods() {
            //noinspection ConstantConditions
            return builderMethods == null ? emptyList() : builderMethods;
        }
    }
}
