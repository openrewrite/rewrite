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
public class EmptyBlockStyle implements Style {
    @NonNull
    BlockPolicy blockPolicy;

    Boolean instanceInit;
    Boolean literalCatch;
    Boolean literalDo;
    Boolean literalElse;
    Boolean literalFinally;
    Boolean literalFor;
    Boolean literalIf;
    Boolean literalSwitch;
    Boolean literalSynchronized;
    Boolean literalTry;
    Boolean literalWhile;
    Boolean staticInit;

    public enum BlockPolicy {
        /**
         * Require that there is some text in the block. For example:
         *     catch (Exception ex) {
         *         // This is a bad coding practice
         *     }
         */
        TEXT,

        /**
         * Require that there is a statement in the block. For example:
         *     finally {
         *         lock.release();
         *     }
         */
        STATEMENT
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.emptyBlock(), this);
    }
}
