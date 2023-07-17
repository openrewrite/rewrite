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
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

import static org.openrewrite.java.style.Checkstyle.hiddenFieldStyle;

@Value
@With
public class HiddenFieldStyle implements Style {
    /**
     * Whether to ignore constructor parameters.
     */
    Boolean ignoreConstructorParameter;

    /**
     * Whether to ignore the parameter of a property setter method.
     */
    Boolean ignoreSetter;

    /**
     * Whether to expand the definition of a setter method to include methods that return the class' instance.
     * This only has an effect if {@link HiddenFieldStyle#ignoreSetter} is set to true.
     */
    Boolean setterCanReturnItsClass;

    /**
     * Whether to ignore parameters of abstract methods.
     */
    Boolean ignoreAbstractMethods;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(hiddenFieldStyle(), this);
    }

}
