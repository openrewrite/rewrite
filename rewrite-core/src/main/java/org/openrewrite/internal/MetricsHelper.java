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
import org.jetbrains.annotations.NotNull;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.GitProvenance;

import java.util.Optional;

public class MetricsHelper {
    public static Timer.Builder successTags(Timer.Builder timer, String detailedOutcome) {
        return successTags(timer, null, detailedOutcome);
    }

    public static Timer.Builder successTags(Timer.Builder timer) {
        return successTags(timer, "success");
    }

    public static <S extends SourceFile> Timer.Builder successTags(Timer.Builder timer, @Nullable S sourceFile) {
        return successTags(timer, sourceFile, "success");
    }

    public static <S extends SourceFile> Timer.Builder successTags(Timer.Builder timer, @Nullable S sourceFile, String detailedOutcome) {
        String originRepository = getOriginRepository(sourceFile);
        return timer
            .tag("outcome", detailedOutcome)
            .tag("exception", "none")
            .tag("exception.line", "none")
            .tag("exception.declaring.class", "none")
            .tag("step", "none")
            .tag("repo.id", originRepository);
    }

    public static Timer.Builder errorTags(Timer.Builder timer, Throwable t) {
        return errorTags(timer, null, t);
    }

    public static <S extends SourceFile> Timer.Builder errorTags(Timer.Builder timer, @Nullable S sourceFile, Throwable t) {
        String originRepository = getOriginRepository(sourceFile);

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
            .tag("exception", t.getClass().getSimpleName())
            .tag("step", "none")
            .tag("repo.id", originRepository)
            .tag("exception.line", exceptionLine)
            .tag("exception.declaring.class", exceptionDeclaringClass);
    }

    @NotNull
    private static <S extends SourceFile> String getOriginRepository(@Nullable S sourceFile) {
        String originRepository = "none";
        if (sourceFile != null) {
            Optional<GitProvenance> maybeGitProvenance = sourceFile.getMarkers().findFirst(GitProvenance.class);
            if (maybeGitProvenance.isPresent()) {
                GitProvenance gitProvenance = maybeGitProvenance.get();
                String origin = gitProvenance.getOrigin();
                if (origin != null) {
                    originRepository = origin;
                }
            }
        }
        return originRepository;
    }
}
