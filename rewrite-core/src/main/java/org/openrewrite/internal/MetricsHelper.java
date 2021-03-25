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
package org.openrewrite.internal;

import io.micrometer.core.instrument.Timer;

public class MetricsHelper {
    public static Timer.Builder successTags(Timer.Builder timer, String detailedOutcome) {
        return timer
                .tag("outcome", detailedOutcome)
                .tag("exception", "none")
                .tag("exception.line", "none")
                .tag("exception.declaring.class", "none")
                .tag("step", "none");
    }

    public static Timer.Builder successTags(Timer.Builder timer) {
        return successTags(timer, "success");
    }

    public static Timer.Builder errorTags(Timer.Builder timer, Throwable t) {
        StackTraceElement stackTraceElement = null;
        if (t.getStackTrace().length > 0) {
            stackTraceElement = t.getStackTrace()[0];
        }

        Timer.Builder tag = timer
                .tag("outcome", "error")
                .tag("exception", t.getClass().getSimpleName())
                .tag("step", "none");
        if (stackTraceElement != null) {
            tag = tag.tag("exception.line", Integer.toString(stackTraceElement.getLineNumber()))
                    .tag("exception.declaring.class", stackTraceElement.getClassName());
        }
        return tag;
    }
}
