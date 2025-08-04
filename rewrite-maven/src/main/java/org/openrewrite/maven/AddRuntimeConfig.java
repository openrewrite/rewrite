/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.maven;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddRuntimeConfig extends ScanningRecipe<AddRuntimeConfig.Accumulator> {
    static final String POM_FILENAME = "pom.xml";
    static final String MVN_CONFIG_DIR = ".mvn";
    static final String MAVEN_CONFIG_FILENAME = "maven.config";
    static final String MAVEN_CONFIG_PATH = MVN_CONFIG_DIR + "/" + MAVEN_CONFIG_FILENAME;
    static final String JVM_CONFIG_FILENAME = "jvm.config";
    static final String JVM_CONFIG_PATH = MVN_CONFIG_DIR + "/" + JVM_CONFIG_FILENAME;

    @Option(displayName = "Config file",
            description = "The file name for setting the runtime configuration.",
            valid = {MAVEN_CONFIG_FILENAME, JVM_CONFIG_FILENAME},
            example = "maven.config")
    String relativeConfigFileName;

    @Option(displayName = "Runtime flag",
            description = "The runtime flag name to be set.",
            example = "-T")
    String flag;

    @Option(displayName = "Runtime flag argument",
            description = "The argument to set for the runtime flag. Some flags do not need to provide a value.",
            required = false,
            example = "3")
    @Nullable
    String argument;

    @Option(displayName = "Separator between runtime flag and argument",
            description = "The separator to use if flag and argument have been provided.",
            valid = {"", " ", "="},
            example = "=")
    Separator separator;

    @Getter
    public enum Separator {
        @SuppressWarnings("DefaultAnnotationParam")
        @JsonProperty("")
        NONE(""),

        @JsonProperty(" ")
        SPACE(" "),

        @JsonProperty("=")
        EQUALS("=");

        private final String notation;

        Separator(String notation) {
            this.notation = notation;
        }
    }

    @Override
    public String getDisplayName() {
        return "Add a configuration option for the Maven runtime";
    }

    @Override
    public String getDescription() {
        return "Add a new configuration option for the Maven runtime if not already present.";
    }

    @Data
    @RequiredArgsConstructor
    public static class Accumulator {
        final String targetRepresentation;
        boolean mavenProject;

        @Nullable
        Path matchingRuntimeConfigFile;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        String targetRepresentation = argument == null ? flag : flag + separator.getNotation() + argument;
        return new Accumulator(targetRepresentation);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof SourceFile) {
                    Path sourcePath = ((SourceFile) tree).getSourcePath();
                    switch (PathUtils.separatorsToUnix(sourcePath.toString())) {
                        case POM_FILENAME:
                            acc.setMavenProject(true);
                            break;
                        case MAVEN_CONFIG_PATH:
                        case JVM_CONFIG_PATH:
                            acc.setMatchingRuntimeConfigFile(sourcePath);
                            break;
                        default:
                            break;
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        if (acc.isMavenProject() && acc.getMatchingRuntimeConfigFile() == null) {
            return singletonList(PlainText.builder()
                    .text(acc.getTargetRepresentation())
                    .sourcePath(Paths.get(MVN_CONFIG_DIR, relativeConfigFileName))
                    .build());
        }
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return Preconditions.check(acc.isMavenProject() && acc.getMatchingRuntimeConfigFile() != null,
                new PlainTextVisitor<ExecutionContext>() {
                    @Override
                    public PlainText visitText(PlainText plainText, ExecutionContext ctx) {
                        if (plainText.getSourcePath().equals(acc.getMatchingRuntimeConfigFile())) {
                            return addOrReplaceConfig(plainText, acc);
                        }
                        return plainText;
                    }

                    private PlainText addOrReplaceConfig(PlainText plainText, Accumulator acc) {
                        String existingContent = plainText.getText();
                        Matcher matcher = Pattern.compile(Pattern.quote(flag) + "[=\\s]?[a-zA-Z0-9]*").matcher(existingContent);
                        if (matcher.find()) {
                            return plainText.withText(matcher.replaceAll(acc.getTargetRepresentation()));
                        }

                        String newText = StringUtils.isBlank(existingContent) ? existingContent : existingContent + determineConfigSeparator(plainText);
                        return plainText.withText(newText + acc.getTargetRepresentation());
                    }

                    private String determineConfigSeparator(PlainText plainText) {
                        // Use new line for maven.config, space for jvm.config
                        if (Paths.get(JVM_CONFIG_PATH).equals(plainText.getSourcePath())) {
                            return " ";
                        }
                        return plainText.getText().contains("\r\n") ? "\r\n" : "\n";
                    }
                });
    }
}
