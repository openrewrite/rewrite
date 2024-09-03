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
import org.jspecify.annotations.NonNull;
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
     * The ? (conditional) operator.
     */
    Boolean question;

    /**
     * The : (colon) operator.
     */
    Boolean colon;

    /**
     * The == (equal) operator.
     */
    Boolean equal;

    /**
     * The != (not equal) operator.
     */
    Boolean notEqual;

    /**
     * The / (division) operator.
     */
    Boolean div;

    /**
     * The + (addition) operator.
     */
    Boolean plus;

    /**
     * The - (subtraction) operator.
     */
    Boolean minus;

    /**
     * The * (multiplication or wildcard) operator.
     */
    Boolean star;

    /**
     * The % (remainder) operator.
     */
    Boolean mod;

    /**
     * The >> (signed shift right) operator.
     */
    Boolean sr;

    /**
     * The >>> (unsigned shift right) operator.
     */
    Boolean bsr;

    /**
     * The >= (greater than or equal) operator.
     */
    Boolean ge;

    /**
     * The > (greater than) operator.
     */
    Boolean gt;

    /**
     * The << (shift left) operator.
     */
    Boolean sl;

    /**
     * The <= (less than or equal) operator.
     */
    Boolean le;

    /**
     * The < (less than) operator.
     */
    Boolean lt;

    /**
     * The ^ (bitwise exclusive OR) operator.
     */
    Boolean bxor;

    /**
     * The | (bitwise OR) operator.
     */
    Boolean bor;

    /**
     * The || (conditional OR) operator.
     */
    Boolean lor;

    /**
     * The & (bitwise AND) operator.
     */
    Boolean band;

    /**
     * The && (conditional AND) operator.
     */
    Boolean land;

    /**
     * The & symbol when used to extend a generic upper or lower bounds constrain or a type cast expression with an additional interface.
     */
    Boolean typeExtensionAnd;

    /**
     * The instanceof operator.
     */
    Boolean literalInstanceof;

    /**
     * A :: reference to a method or constructor without arguments.
     */
    Boolean methodRef;

    /**
     * The = (assignment) operator.
     */
    Boolean assign;

    /**
     * The += (addition assignment) operator.
     */
    Boolean plusAssign;

    /**
     * The -= (subtraction assignment) operator.
     */
    Boolean minusAssign;

    /**
     * The *= (multiplication assignment) operator.
     */
    Boolean starAssign;

    /**
     * The /= (division assignment) operator.
     */
    Boolean divAssign;

    /**
     * The %= (remainder assignment) operator.
     */
    Boolean modAssign;

    /**
     * The >>= (signed right shift assignment) operator.
     */
    Boolean srAssign;

    /**
     * The >>>= (unsigned right shift assignment) operator.
     */
    Boolean bsrAssign;

    /**
     * The <<= (left shift assignment) operator.
     */
    Boolean slAssign;

    /**
     * The &= (bitwise AND assignment) operator.
     */
    Boolean bandAssign;

    /**
     * The ^= (bitwise exclusive OR assignment) operator.
     */
    Boolean bxorAssign;

    /**
     * The |= (bitwise OR assignment) operator.
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
