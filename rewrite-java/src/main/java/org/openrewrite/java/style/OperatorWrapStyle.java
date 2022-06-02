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
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.NullFields;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

@Value
@With
@NullFields
public class OperatorWrapStyle implements Style {
    /**
     * Whether to add a newline for the token at the end of the line.
     */
    @NonNull
    WrapOption wrapOption;

    /**
     * The {@code ?} (conditional) operator.
     */
    Boolean question;

    /**
     * The {@code :} (colon) operator.
     */
    Boolean colon;

    /**
     * The {@code ==} (equal) operator.
     */
    Boolean equal;

    /**
     * The {@code !=} (not equal) operator.
     */
    Boolean notEqual;

    /**
     * The {@code /} (division) operator.
     */
    Boolean div;

    /**
     * The {@code +} (addition) operator.
     */
    Boolean plus;

    /**
     * The {@code -} (subtraction) operator.
     */
    Boolean minus;

    /**
     * The {@code *} (multiplication or wildcard) operator.
     */
    Boolean star;

    /**
     * The {@code %} (remainder) operator.
     */
    Boolean mod;

    /**
     * The {@code >>} (signed shift right) operator.
     */
    Boolean sr;

    /**
     * The {@code >>>} (unsigned shift right) operator.
     */
    Boolean bsr;

    /**
     * The {@code >=} (greater than or equal) operator.
     */
    Boolean ge;

    /**
     * The {@code >} (greater than) operator.
     */
    Boolean gt;

    /**
     * The {@code <<} (shift left) operator.
     */
    Boolean sl;

    /**
     * The {@code <=} (less than or equal) operator.
     */
    Boolean le;

    /**
     * The {@code <} (less than) operator.
     */
    Boolean lt;

    /**
     * The {@code ^} (bitwise exclusive OR) operator.
     */
    Boolean bxor;

    /**
     * The {@code |} (bitwise OR) operator.
     */
    Boolean bor;

    /**
     * The {@code ||} (conditional OR) operator.
     */
    Boolean lor;

    /**
     * The {@code &} (bitwise AND) operator.
     */
    Boolean band;

    /**
     * The {@code &&} (conditional AND) operator.
     */
    Boolean land;

    /**
     * The {@code &} symbol when used to extend a generic upper or lower bounds constrain or a type cast expression with an additional interface.
     */
    Boolean typeExtensionAnd;

    /**
     * The {@code instanceof} operator.
     */
    Boolean literalInstanceof;

    /**
     * A {@code ::} reference to a method or constructor without arguments.
     */
    Boolean methodRef;

    /**
     * The {@code =} (assignment) operator.
     */
    Boolean assign;

    /**
     * The {@code +=} (addition assignment) operator.
     */
    Boolean plusAssign;

    /**
     * The {@code -=} (subtraction assignment) operator.
     */
    Boolean minusAssign;

    /**
     * The {@code *=} (multiplication assignment) operator.
     */
    Boolean starAssign;

    /**
     * The {@code /=} (division assignment) operator.
     */
    Boolean divAssign;

    /**
     * The {@code %=} (remainder assignment) operator.
     */
    Boolean modAssign;

    /**
     * The {@code >>=} (signed right shift assignment) operator.
     */
    Boolean srAssign;

    /**
     * The {@code >>>=} (unsigned right shift assignment) operator.
     */
    Boolean bsrAssign;

    /**
     * The {@code <<=} (left shift assignment) operator.
     */
    Boolean slAssign;

    /**
     * The {@code &=} (bitwise AND assignment) operator.
     */
    Boolean bandAssign;

    /**
     * The {@code ^=} (bitwise exclusive OR assignment) operator.
     */
    Boolean bxorAssign;

    /**
     * The {@code |=} (bitwise OR assignment) operator.
     */
    Boolean borAssign;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.operatorWrapStyle(), this);
    }

    public enum WrapOption {
        /**
         * Require that the token is at the end of the line.
         */
        EOL,

        /**
         * Require that the token is on a new line.
         */
        NL
    }

}
