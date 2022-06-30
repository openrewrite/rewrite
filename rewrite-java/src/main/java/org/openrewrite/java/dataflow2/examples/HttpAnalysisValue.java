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
import org.openrewrite.java.tree.Expression;

import java.util.Collection;

@Value
public class HttpAnalysisValue {
    Understanding name;
    Expression literal;

    public enum Understanding {
        UNKNOWN, SECURE, NOT_SECURE, CONFLICT
    }

    public static final HttpAnalysisValue UNKNOWN = new HttpAnalysisValue(Understanding.UNKNOWN, null);
    public static final HttpAnalysisValue SECURE = new HttpAnalysisValue(Understanding.SECURE, null);

    public static final Joiner<HttpAnalysisValue> JOINER = new Joiner<HttpAnalysisValue>() {
        @Override
        public HttpAnalysisValue join(Collection<HttpAnalysisValue> values) {
            HttpAnalysisValue result = UNKNOWN;
            for (HttpAnalysisValue value : values) {
                if (value == UNKNOWN) {
                    result = value;
                } else {
                    return UNKNOWN;
                }
            }
            return result;
        }

        @Override
        public HttpAnalysisValue lowerBound() {
            return UNKNOWN;
        }

        @Override
        public HttpAnalysisValue defaultInitialization() {
            return SECURE;
        }
    };
}
