package org.openrewrite.marker;

import lombok.Getter;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

@Incubating(since = "7.31.0")
public abstract class Markup extends SearchResult {
    @Getter
    @Nullable
    private final String detail;

    public Markup(UUID id, String message, @Nullable String detail) {
        super(id, message);
        this.detail = detail;
    }

    public enum Level {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }

    public static class Error extends Markup {
        @Getter
        private final RecipeRunException exception;

        public Error(UUID id, String message, RecipeRunException exception) {
            super(id, message, exception.getSanitizedStackTrace());
            this.exception = exception;
        }
    }

    public static class Warn extends Markup {
        @Getter
        @Nullable
        private final RecipeRunException exception;

        public Warn(UUID id, String message, @Nullable RecipeRunException exception) {
            super(
                    id,
                    message,
                    exception == null ? null : exception.getSanitizedStackTrace()
            );
            this.exception = exception;
        }
    }

    public static class Info extends Markup {
        public Info(UUID id, String message) {
            super(id, message, null);
        }
    }

    public static class Debug extends Markup {
        public Debug(UUID id, String message) {
            super(id, message, null);
        }
    }

    public static <T extends Tree> T error(T t, String message, Throwable throwable) {
        return markup(t, Level.ERROR, message, throwable);
    }

    public static <T extends Tree> T warn(T t, String message, Throwable throwable) {
        return markup(t, Level.WARNING, message, throwable);
    }

    public static <T extends Tree> T info(T t, String message, Throwable throwable) {
        return markup(t, Level.INFO, message, throwable);
    }

    public static <T extends Tree> T debug(T t, String message, Throwable throwable) {
        return markup(t, Level.DEBUG, message, throwable);
    }

    private static <T extends Tree> T markup(T t, Level level, String message, @Nullable Throwable throwable) {
        RecipeRunException rre = null;
        if (throwable instanceof RecipeRunException) {
            rre = (RecipeRunException) throwable;
        } else if(throwable != null) {
            rre = new RecipeRunException(throwable);
        }

        try {
            Method getMarkers = t.getClass().getDeclaredMethod("getMarkers");
            Method withMarkers = t.getClass().getDeclaredMethod("withMarkers", Markers.class);
            Markers markers = (Markers) getMarkers.invoke(t);

            Markup markup;
            switch(level) {
                case DEBUG:
                    markup = new Markup.Debug(randomId(), message);
                    break;
                case INFO:
                    markup = new Markup.Info(randomId(), message);
                    break;
                case WARNING:
                    markup = new Markup.Warn(randomId(), message, rre);
                    break;
                case ERROR:
                default:
                    markup = new Markup.Error(randomId(), message, requireNonNull(rre));
                    break;
            }

            //noinspection unchecked
            return (T) withMarkers.invoke(t, markers
                    .computeByType(markup, (s1, s2) -> s1 == null ? s2 : s1));
        } catch (Throwable ignored) {
            return t;
        }
    }
}
