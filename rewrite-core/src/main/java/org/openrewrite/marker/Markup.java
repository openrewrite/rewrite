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
package org.openrewrite.marker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.RecipeScheduler;
import org.openrewrite.Tree;
import org.openrewrite.internal.ExceptionUtils;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.31.0")
public interface Markup extends Marker {

    String getMessage();

    @Nullable
    String getDetail();

    @Override
    default String print(Cursor cursor, UnaryOperator<String> commentWrapper, boolean verbose) {
        if (verbose) {
            return commentWrapper.apply("(" + getDetail() + ")");
        }
        return commentWrapper.apply("(" + getMessage() + ")");
    }

    static <T extends Tree> T error(T t, Throwable throwable) {
        return markup(t, new Markup.Error(randomId(), throwable));
    }

    static <T extends Tree> T warn(T t, Throwable throwable) {
        return markup(t, new Markup.Warn(randomId(), throwable));
    }

    static <T extends Tree> T info(T t, String message) {
        return info(t, message, null);
    }

    static <T extends Tree> T info(T t, String message, @Nullable String detail) {
        return markup(t, new Markup.Info(randomId(), message, detail));
    }

    static <T extends Tree> T debug(T t, String message) {
        return debug(t, message, null);
    }

    static <T extends Tree> T debug(T t, String message, @Nullable String detail) {
        return markup(t, new Markup.Debug(randomId(), message, detail));
    }

    static <T extends Tree> T markup(T t, Markup markup) {
        return t.withMarkers(t.getMarkers().compute(markup, (s1, s2) -> s1 == null ? s2 : s1));
    }

    @Value
    @With
    class Error implements Markup {
        UUID id;
        Throwable exception;

        @Override
        public String getMessage() {
            return getCause().getMessage();
        }

        @Override
        @NonNull
        public String getDetail() {
            return ExceptionUtils.sanitizeStackTrace(getCause(), RecipeScheduler.class);
        }

        private Throwable getCause() {
            return exception instanceof RecipeRunException ? exception.getCause() : exception;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Error error = (Error) o;
            return getDetail().equals(error.getDetail());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getDetail());
        }
    }

    @Value
    @With
    class Warn implements Markup {
        UUID id;
        Throwable exception;

        @Override
        public String getMessage() {
            return getCause().getMessage();
        }

        @Override
        @NonNull
        public String getDetail() {
            return ExceptionUtils.sanitizeStackTrace(getCause(), RecipeScheduler.class);
        }

        private Throwable getCause() {
            return exception instanceof RecipeRunException ? exception.getCause() : exception;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Warn error = (Warn) o;
            return getDetail().equals(error.getDetail());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getDetail());
        }
    }

    @Value
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @With
    class Info implements Markup {
        UUID id;

        @EqualsAndHashCode.Include
        String message;

        @EqualsAndHashCode.Include
        @Nullable
        String detail;


        @Override
        public String getMessage() {
            return message;
        }

        @Nullable
        @Override
        public String getDetail() {
            return detail;
        }
    }

    @Value
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @With
    class Debug implements Markup {
        UUID id;

        @EqualsAndHashCode.Include
        String message;

        @EqualsAndHashCode.Include
        @Nullable
        String detail;

        public String getMessage() {
            return message;
        }

        @Nullable
        @Override
        public String getDetail() {
            return detail;
        }
    }
}
