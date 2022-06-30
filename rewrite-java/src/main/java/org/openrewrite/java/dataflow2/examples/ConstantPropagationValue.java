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

import lombok.Value;
import org.openrewrite.java.dataflow2.Joiner;

import java.util.Collection;

@Value
public class ConstantPropagationValue {
    Understanding understanding;
    Object value;

    public enum Understanding {
        Unknown, Known, Conflict
    }

    public static final ConstantPropagationValue UNKNOWN = new ConstantPropagationValue(Understanding.Unknown, null);
    public static final ConstantPropagationValue CONFLICT = new ConstantPropagationValue(Understanding.Conflict, null);

    public static final Joiner<ConstantPropagationValue> JOINER = new Joiner<ConstantPropagationValue>() {
        @Override
        public ConstantPropagationValue join(Collection<ConstantPropagationValue> values) {
            ConstantPropagationValue result = UNKNOWN;
            for (ConstantPropagationValue value : values) {
                if(result == UNKNOWN) {
                    result = value;
                } else if (value == UNKNOWN) {
                    result = value;
                } else if(value == CONFLICT) {
                    return CONFLICT;
                } else if(value.getUnderstanding() == Understanding.Known) {
                    if(result.getUnderstanding() != Understanding.Known || !result.value.equals(value.value)) {
                        return CONFLICT;
                    }
                    result = value;
                }
            }
            return result;
        }

        @Override
        public ConstantPropagationValue lowerBound() {
            return UNKNOWN;
        }

        @Override
        public ConstantPropagationValue defaultInitialization() {
            return UNKNOWN;
        }
    };
}
