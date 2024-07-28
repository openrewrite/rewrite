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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.PathUtils;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public enum Separator {
        NONE(""),
        SPACE(" "),
        EQUALS("=");

        private final static Map<String, Separator> all = new HashMap<>();
        private final String notation;

        static {
            for (Separator separator : values()) {
                all.put(separator.notation, separator);
            }
        }

        static Separator forNotation(String notation) {
            if (!all.containsKey(notation)) {
                throw new IllegalArgumentException("Unknown notation for separator: " + notation);
            }

            return all.get(notation);
        }

        Separator(String notation) {
            this.notation = notation;
        }

        public String getNotation() {
            return notation;
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
    static class Accumulator {
        boolean mavenProject;
        Path matchingRuntimeConfigFile;
        RealizedConfig realizedConfig;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }

                SourceFile sourceFile = (SourceFile) tree;
                String sourcePath = PathUtils.separatorsToUnix(sourceFile.getSourcePath().toString());
                acc.setRealizedConfig(new RealizedConfig(flag, argument, separator));

                switch (sourcePath) {
                    case POM_FILENAME:
                        acc.setMavenProject(true);
                        break;
                    case MAVEN_CONFIG_PATH:
                    case JVM_CONFIG_PATH:
                        acc.setMatchingRuntimeConfigFile(sourceFile.getSourcePath());
                        break;
                    default:
                        break;
                }

                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        if (!acc.isMavenProject() || acc.getMatchingRuntimeConfigFile() != null) {
            return Collections.emptyList();
        }

        List<SourceFile> sources = new ArrayList<>();

        Path path = Paths.get(MVN_CONFIG_DIR, relativeConfigFileName);

        if (!Files.exists(path)) {
            PlainText newConfigFile = PlainText.builder()
                    .text(acc.getRealizedConfig().getTargetRepresentation())
                    .sourcePath(path)
                    .build();
            sources.add(newConfigFile);
        }

        return sources;
    }

    private static class RealizedConfig {
        private final String targetRepresentation;

        RealizedConfig(String flag, String argument, Separator separator) {
            String flagAndArg = flag;

            if (argument != null) {
                flagAndArg += separator.getNotation() + argument;
            }

            targetRepresentation = flagAndArg;
        }

        String getTargetRepresentation() {
            return targetRepresentation;
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (!acc.isMavenProject() || acc.getMatchingRuntimeConfigFile() == null) {
            return TreeVisitor.noop();
        }

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }

                SourceFile sourceFile = (SourceFile) tree;

                if (sourceFile.getSourcePath().equals(acc.getMatchingRuntimeConfigFile())) {
                    return addOrReplaceConfig(sourceFile, acc);
                }

                return tree;
            }
        };
    }

    private PlainText addOrReplaceConfig(SourceFile sourceFile, Accumulator acc) {
        PlainText plainText = PlainTextParser.convert(sourceFile);
        String existingContent = plainText.getText();
        String escapedFlag = Pattern.quote(flag);
        Pattern pattern = Pattern.compile(escapedFlag + "[=\\s]?[a-zA-Z0-9]*");
        Matcher matcher = pattern.matcher(existingContent);

        if (!matcher.find()) {
            String separator = determineConfigSeparator(acc.getMatchingRuntimeConfigFile());
            String newText = existingContent.isEmpty() ? existingContent : existingContent + separator;
            newText += acc.getRealizedConfig().getTargetRepresentation();
            return plainText.withText(newText);
        } else {
            String newText = matcher.replaceAll(acc.getRealizedConfig().getTargetRepresentation());
            return plainText.withText(newText);
        }
    }

    private String determineConfigSeparator(Path matchingRuntimeConfigFile) {
        // Use new line for maven.config, space for jvm.config
        return Paths.get(MAVEN_CONFIG_PATH).equals(matchingRuntimeConfigFile) ? System.lineSeparator() : " ";
    }
}