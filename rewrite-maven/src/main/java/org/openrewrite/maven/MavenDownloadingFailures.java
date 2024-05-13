package org.openrewrite.maven;

import lombok.Value;
import lombok.With;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;

import java.util.Collections;
import java.util.List;

@Value
@With
public class MavenDownloadingFailures {

    List<MavenDownloadingFailure> failures;

    public static MavenDownloadingFailures append(
            @Nullable MavenDownloadingFailures current,
            MavenDownloadingFailure failure) {
        if (current == null) {
            current = new MavenDownloadingFailures(Collections.emptyList());
        }

        return current.withFailures(ListUtils.concat(current.getFailures(), failure));
    }

    public static MavenDownloadingFailure append(
            @Nullable MavenDownloadingFailures current,
            @Nullable MavenDownloadingFailures exceptions) {
        if (current == null) {
            if (exceptions == null) {
                return new MavenDownloadingFailures();
            }
            current = new MavenDownloadingFailures();
        }
        if (exceptions == null) {
            return current;
        }
        exceptions.getExceptions().forEach(current::addSuppressed);
        current.exceptions.addAll(exceptions.getExceptions());
        return current;
    }
}
