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
package org.openrewrite.internal;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.StringJoiner;

public class ExceptionUtils {
    /**
     * Shorten a stack trace to the first invocation of a particular class.
     *
     * @param t The original exception
     * @param until Cut the stack trace when it reaches a method in this class.
     * @return The sanitized stack trace
     */
    public static String sanitizeStackTrace(Throwable t, Class<?> until) {
        StringJoiner sanitized = new StringJoiner("\n");
        Throwable cause = t instanceof RecipeRunException ? t.getCause() : t;
        sanitized.add(cause.getClass().getName() + ": " + cause.getLocalizedMessage());

        int i = 0;
        for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
            if (stackTraceElement.getClassName().equals(until.getName())) {
                break;
            }
            if (i++ >= 16) {
                sanitized.add("  ...");
                break;
            }
            sanitized.add("  " + stackTraceElement);
        }
        return sanitized.toString();
    }

    public static boolean containsCircularReferences(Throwable exception) {
        Set<Throwable> causes = Collections.newSetFromMap(new IdentityHashMap<>());
        causes.add(exception);
        boolean containsACircularReference = false;
        while (exception != null && exception.getCause() != null) {
            Throwable exceptionToFind = exception.getCause();
            if (exceptionToFind != null) {

                if (!causes.add(exceptionToFind)) {
                    containsACircularReference = true;
                    break;
                } else {
                    exception = exceptionToFind;
                }
            } else {
                exception = null;
            }
        }
        return containsACircularReference;
    }
}
