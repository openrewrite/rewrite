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
import lombok.With;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

@Value
@With
public class NoWhitespaceAfterStyle implements Style {
    /**
     * Whether whitespace is allowed if the token is at a linebreak.
     */
    Boolean allowLineBreaks;

    /**
     * A type-cast. For example: {@code (String) itr.next()}
     */
    Boolean typecast;

    /**
     * A :: reference to a method or constructor.
     */
    Boolean methodRef;

    /**
     * An array declaration.
     */
    Boolean arrayDeclarator;

    /**
     * An @ annotation symbol.
     */
    Boolean annotation;

    /**
     * An array initialization.
     */
    Boolean arrayInitializer;

    /**
     * The array index operator.
     */
    Boolean indexOperation;

    /**
     * The . (dot) operator.
     */
    Boolean dot;

    /**
     * The ++ (prefix increment) operator.
     */
    Boolean inc;

    /**
     * The -- (prefix decrement) operator.
     */
    Boolean dec;

    /**
     * The ~ (bitwise complement) operator.
     */
    Boolean bnoc;

    /**
     * The ! (logical complement) operator.
     */
    Boolean lnot;

    /**
     * The - (unary minus) operator.
     */
    Boolean unaryPlus;

    /**
     * The + (unary plus) operator.
     */
    Boolean unaryMinus;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.noWhitespaceAfterStyle(), this);
    }

}
