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

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

import java.util.function.Consumer;
import java.util.function.Function;

public class MetricsHelper {
    public static void record(String timerName, Consumer<Timer.Builder> f) {
        Timer.Builder timer = Timer.builder(timerName);
        Timer.Sample sample = Timer.start();
        try {
            f.accept(timer);
            sample.stop(MetricsHelper.successTags(timer).register(Metrics.globalRegistry));
        } catch (Throwable t) {
            sample.stop(MetricsHelper.errorTags(timer, t).register(Metrics.globalRegistry));
            throw t;
        }
    }

    public static <U> U record(String timerName, Function<Timer.Builder, U> f) {
        Timer.Builder timer = Timer.builder(timerName);
        Timer.Sample sample = Timer.start();
        try {
            U returnVal = f.apply(timer);
            sample.stop(MetricsHelper.successTags(timer).register(Metrics.globalRegistry));
            return returnVal;
        } catch (Throwable t) {
            sample.stop(MetricsHelper.errorTags(timer, t).register(Metrics.globalRegistry));
            throw t;
        }
    }

    public static Timer.Builder successTags(Timer.Builder timer) {
        return successTags(timer, "none");
    }

    public static Timer.Builder successTags(Timer.Builder timer, String reason) {
        return timer
                .tag("outcome", "success")
                .tag("reason", reason)
                .tag("exception", "none")
                .tag("exception.line", "none")
                .tag("exception.declaring.class", "none");
    }

    public static Timer.Builder errorTags(Timer.Builder timer, Throwable t) {
        return errorTags(timer, "none", t);
    }

    public static Timer.Builder errorTags(Timer.Builder timer, String reason, Throwable t) {
        StackTraceElement stackTraceElement = null;
        if (t.getStackTrace().length > 0) {
            stackTraceElement = t.getStackTrace()[0];
        }

        String exceptionLine = "none";
        String exceptionDeclaringClass = "none";
        if (stackTraceElement != null) {
            exceptionLine = Integer.toString(stackTraceElement.getLineNumber());
            exceptionDeclaringClass = stackTraceElement.getClassName();
        }

        return timer
                .tag("outcome", "error")
                .tag("reason", reason)
                .tag("exception", t.getClass().getSimpleName())
                .tag("exception.line", exceptionLine)
                .tag("exception.declaring.class", exceptionDeclaringClass);
    }
}
