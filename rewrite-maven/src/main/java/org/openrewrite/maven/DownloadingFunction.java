package org.openrewrite.maven;

import org.jspecify.annotations.Nullable;

/**
 * A function that is allowed to throw MavenDownloadingExceptions
 * @param <T>
 * @param <R>
 */
@FunctionalInterface
public interface DownloadingFunction<T, R> {
    R apply(T t) throws MavenDownloadingException;
}
