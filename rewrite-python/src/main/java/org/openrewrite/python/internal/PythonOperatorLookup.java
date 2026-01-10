/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal;

import org.jspecify.annotations.Nullable;

public abstract class PythonOperatorLookup {
    private PythonOperatorLookup() {}

    public static @Nullable String operatorForMagicMethod(String method) {
        switch (method) {
            case "__eq__":
                return "==";
            case "__ne__":
                return "!=";
            case "__contains__":
                return "in";
            case "__floordiv__":
                return "//";
            case "__pow__":
                return "**";
            case "__matmul__":
                return "@";
            default:
                return null;
        }
    }

    public static boolean doesMagicMethodReverseOperands(String method) {
        return "__contains__".equals(method);
    }
}
