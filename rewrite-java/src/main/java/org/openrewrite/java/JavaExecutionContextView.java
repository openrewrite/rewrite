/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java;

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

public class JavaExecutionContextView extends DelegatingExecutionContext {
    public static final String EVENT_SOURCE_FILE_PARSED = "org.openrewrite.java.parsing.parsed";
    public static final String EVENT_TYPE_ATTRIBUTION_COMPLETE = "org.openrewrite.java.parsing.attributed";
    public static final String EVENT_SOURCE_FILE_MAPPED = "org.openrewrite.java.parsing.mapped";

    public JavaExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public void increment(String key) {
        int value = getMessage(key, 0) + 1;
        putMessage(key, value);
    }
}
