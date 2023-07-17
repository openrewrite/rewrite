/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.BuildToolFailure;
import org.openrewrite.marker.Markup;
import org.openrewrite.table.BuildToolFailures;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindBuildToolFailures extends Recipe {
    transient BuildToolFailures failures = new BuildToolFailures(this);

    @Option(displayName = "Suppress log output",
            description = "Default false. If true, the `logOutput` column will be empty in the output table.",
            required = false)
    @Nullable
    Boolean suppressLogOutput;

    @Override
    public String getDisplayName() {
        return "Find source files with `BuildToolFailure` markers";
    }

    @Override
    public String getDescription() {
        return "This recipe explores build tool failures after an LST is produced for classifying the types of " +
               "failures that can occur and prioritizing fixes according to the most common problems.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                return sourceFile.getMarkers().findFirst(BuildToolFailure.class)
                        .<Tree>map(failure -> {
                            String logFileContents = sourceFile.printAll();
                            String requiredJavaVersion = FailureLogAnalyzer.requiredJavaVersion(logFileContents);
                            if (suppressLogOutput != null && suppressLogOutput) {
                                logFileContents = "";
                            }
                            failures.insertRow(ctx, new BuildToolFailures.Row(
                                    failure.getType(),
                                    failure.getVersion(),
                                    failure.getCommand(),
                                    failure.getExitCode(),
                                    requiredJavaVersion,
                                    logFileContents
                            ));
                            return Markup.info(sourceFile, String.format("Exit code %d", failure.getExitCode()));
                        })
                        .orElse(sourceFile);
            }
        };
    }
}

class FailureLogAnalyzer {

    // Downgrade Java when necessary
    private static final Pattern UNSUPPORTED_CLASS_FILE_MAJOR_VERSION = Pattern.compile("Unsupported class file (?:major )?version (\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SOURCE_TARGET_OPTION = Pattern.compile("(?:Source|Target) option (\\d+) is no longer supported. Use \\d+ or later", Pattern.CASE_INSENSITIVE);

    // Upgrade Java when necessary
    private static final Pattern CLASS_FILE_MAJOR_VERSION = Pattern.compile("class file (?:major )?version (\\d+)");
    private static final String INVALID_FLAG_RELEASE = "invalid flag: --release";
    private static final String ADD_EXPORTS = "Unrecognized option: --add-exports";
    private static final String MODULE_PATH = "javac: invalid flag: --module-path";

    private static final Pattern BAD_OPTION_WAS_IGNORED = Pattern.compile("bad option '-target:(\\d+)' was ignored");
    private static final Pattern INCOMPATIBLE_COMPONENT = Pattern.compile("Incompatible because this component declares a component compatible with Java (\\d+)");
    private static final Pattern INVALID_SOURCE_TARGET_RELEASE = Pattern.compile("invalid (?:source|target) release: (?:1\\.)?(\\d+)");
    private static final Pattern RELEASE_VERSION_NOT_SUPPORTED = Pattern.compile("release version (\\d+) not supported");
    private static final Pattern SOURCE_TARGET_OBSOLETE = Pattern.compile("(?:source|target) value (?:1\\.)?(\\d+) is obsolete", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOOLCHAIN = Pattern.compile("\\[ERROR] jdk \\[ version='(?:1\\.)?(\\d+)' ]");
    private static final Pattern USE_SOURCE = Pattern.compile("use -source (\\d+) or higher to enable");

    @Nullable
    static String requiredJavaVersion(String logFileContents) {
        // Possibly downgrade Java when necessary
        Matcher matcher = UNSUPPORTED_CLASS_FILE_MAJOR_VERSION.matcher(logFileContents);
        if (matcher.find()) {
            // Oldest version we currently support, as message gives us no indication of what version is required
            return "8";
        }
        matcher = SOURCE_TARGET_OPTION.matcher(logFileContents);
        if (matcher.find()) {
            // Might return unsupported versions, which we will then have to skip
            return matcher.group(1);
        }

        // Possibly upgrade Java when necessary
        matcher = CLASS_FILE_MAJOR_VERSION.matcher(logFileContents);
        if (matcher.find()) {
            // https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-4.1-200-B.2
            return String.valueOf(Integer.parseInt(matcher.group(1)) - 44);
        }
        if (logFileContents.contains(INVALID_FLAG_RELEASE) ||
            logFileContents.contains(ADD_EXPORTS) ||
            logFileContents.contains(MODULE_PATH)) {
            return "11"; // Technically 9+, but we'll go for 11 as it's an LTS release
        }
        return Stream.of(
                        BAD_OPTION_WAS_IGNORED,
                        INCOMPATIBLE_COMPONENT,
                        INVALID_SOURCE_TARGET_RELEASE,
                        RELEASE_VERSION_NOT_SUPPORTED,
                        SOURCE_TARGET_OBSOLETE,
                        TOOLCHAIN,
                        USE_SOURCE)
                .map(pattern -> pattern.matcher(logFileContents))
                .filter(Matcher::find)
                .map(m -> m.group(1))
                .findFirst()
                .orElse(null);
    }
}
