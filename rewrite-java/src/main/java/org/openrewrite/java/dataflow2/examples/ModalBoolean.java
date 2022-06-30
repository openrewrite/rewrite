/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.dataflow2.examples;

import org.openrewrite.Incubating;
import org.openrewrite.java.dataflow2.Joiner;

import java.util.Collection;

/**
 * A modal boolean is 'true', 'false', or one of 'NoIdea' (not enough knowledge) or 'Conflict' (contradicting knowledge).
 */
@Incubating(since = "7.25.0")
public enum ModalBoolean {

    // The lattice is ordered as:
    // NoIdea < True, False, Null < Conflict

    NoIdea, // The lower bound of the lattice (nothing is known about the value)
    True,
    False,
    Null, // in the case of reference type java.lang.Boolean
    Conflict; // The upper bound of the lattice (some paths lead to True, some paths leads to False)

    public static final Joiner<ModalBoolean> JOINER = new Joiner<ModalBoolean>() {

        @Override
        public ModalBoolean join(Collection<ModalBoolean> values) {
            return ModalBoolean.join(values);
        }

        @Override
        public ModalBoolean lowerBound() {
            return NoIdea;
        }

        @Override
        public ModalBoolean defaultInitialization() {
            return False;
        }
    };

    public static ModalBoolean join(Collection<ModalBoolean> outs) {
        ModalBoolean result = NoIdea;
        for (ModalBoolean out : outs) {
            if (out == NoIdea) {
                continue;
            }
            if ((result == True && out != True) ||
                    (result == False && out != False) ||
                    (result == Null && out != Null)) {
                return Conflict;
            } else if (result == Conflict) {
                return result;
            }
            result = out;
        }
        return result;
    }
}
