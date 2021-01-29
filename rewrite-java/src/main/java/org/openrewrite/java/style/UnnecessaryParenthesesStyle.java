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

import lombok.AccessLevel;
import lombok.Data;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.Incubating;
import org.openrewrite.java.JavaStyle;

@Incubating(since = "7.0.0")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
@With
public class UnnecessaryParenthesesStyle implements JavaStyle {
    boolean expr;
    boolean ident;
    boolean numDouble;
    boolean numFloat;
    boolean numInt;
    boolean numLong;
    boolean stringLiteral;
    boolean literalNull;
    boolean literalFalse;
    boolean literalTrue;
    boolean assign;
    boolean bandAssign;
    boolean borAssign;
    boolean bsrAssign;
    boolean bxorAssign;
    boolean divAssign;
    boolean minusAssign;
    boolean modAssign;
    boolean plusAssign;
    boolean slAssign;
    boolean srAssign;
    boolean starAssign;
    boolean lambda;
}
