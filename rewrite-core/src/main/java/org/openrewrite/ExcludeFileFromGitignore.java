package org.openrewrite;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.jgit.ignore.FastIgnoreRule;
import org.openrewrite.jgit.ignore.IgnoreNode;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.openrewrite.ExcludeFileFromGitignore.Repository;
import static org.openrewrite.jgit.ignore.IgnoreNode.MatchResult.CHECK_PARENT;
import static org.openrewrite.jgit.ignore.IgnoreNode.MatchResult.IGNORED;
import static org.openrewrite.jgit.ignore.IgnoreNode.MatchResult.NOT_IGNORED;

@Value
@EqualsAndHashCode(callSuper = false)
public class ExcludeFileFromGitignore extends ScanningRecipe<Repository> {

    @Override
    public String getDisplayName() {
        return "Remove ignoral of files or directories in .gitignore";
    }

    @Override
    public String getDescription() {
        return "This recipe will remove a file or directory to the .gitignore file. " + "If the file or directory is already in the .gitignore file, it will be removed." + "If the file or directory is not in the .gitignore file, no action will be taken.";
    }

    @Option(displayName = "Paths", description = "The paths to find and remove from the gitignore files.", example = "blacklist")
    List<String> paths;

    @Override
    public Repository getInitialValue(ExecutionContext ctx) {
        return new Repository();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Repository acc) {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext executionContext) {
                return super.isAcceptable(sourceFile, executionContext) && sourceFile.getSourcePath().endsWith(".gitignore");
            }

            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                try {
                    String gitignoreFileName = text.getSourcePath().toString();
                    gitignoreFileName = gitignoreFileName.startsWith("/") ? gitignoreFileName : "/" + gitignoreFileName;
                    IgnoreNode ignoreNode = new IgnoreNode();
                    ignoreNode.parse(gitignoreFileName, new ByteArrayInputStream(text.getText().getBytes()));
                    acc.getRules().put(gitignoreFileName.substring(0, gitignoreFileName.lastIndexOf("/") + 1), ignoreNode);
                    acc.issue3Directories(gitignoreFileName.substring(0, gitignoreFileName.lastIndexOf("/") + 1), text.getText());
                } catch (IOException e) {
                    throw new RuntimeException("Unknown error udring recipe run", e);
                }
                return super.visitText(text, executionContext);
            }
        };
    }

    @Data
    public static class Repository {
        private final Map<String, IgnoreNode> rules = new HashMap<>();
        private final Map<String, List<String>> issue3IgnoredDirectories = new HashMap<>();
        private final Map<String, List<String>> issue3NegatedDirectories = new HashMap<>();

        public void exclude(String path) {
            String normalizedPath = path.startsWith("/") ? path : "/" + path;
            List<String> impactingFiles = rules.keySet()
                                               .stream()
                                               .filter(k -> normalizedPath.toLowerCase().startsWith(k.toLowerCase()))
                                               .sorted(Comparator.comparingInt(String::length).reversed())
                                               .collect(Collectors.toList());

            IgnoreNode.MatchResult isIgnored;
            boolean isDirectory = path.endsWith("/");
            for (String impactingFile : impactingFiles) {
                IgnoreNode ignoreNode = rules.get(impactingFile);
                String nestedPath = normalizedPath.substring(impactingFile.length() - 1);
                isIgnored = ignoreNode.isIgnored(nestedPath, isDirectory);
                // Overcoming issue3 in jgit repo for the pathMatch being false
                if (CHECK_PARENT.equals(isIgnored) && isDirectory) {
                    List<FastIgnoreRule> rules = ignoreNode.getRules();
                    for (int i = rules.size() - 1; i > -1; i--) {
                        FastIgnoreRule rule = rules.get(i);
                        if (rule.isMatch(nestedPath, true, false)) {
                            if (rule.getResult()) {
                                isIgnored = IGNORED;
                            } else {
                                isIgnored = NOT_IGNORED;
                            }
                            break;
                        }
                    }
                }
                // Overcoming issue3 in jgit repo for the single directory rules
                if (CHECK_PARENT.equals(isIgnored)) {
                    Boolean ignored = isIgnoredByDirectoryIssue3Bypass(impactingFile, nestedPath);
                    if (ignored != null) {
                        if (ignored) {
                            isIgnored = IGNORED;
                        } else {
                            isIgnored = NOT_IGNORED;
                        }
                    }
                }
                if (CHECK_PARENT.equals(isIgnored)) {
                    continue;
                }
                if (IGNORED.equals(isIgnored)) {
                    List<FastIgnoreRule> remainingRules = new ArrayList<>();
                    for (FastIgnoreRule rule : ignoreNode.getRules()) {
                        // First 2 if clauses are for the jgit issue (#3) opened at openrewrite/jgit
                        if (issue3IgnoredDirectories.getOrDefault(impactingFile, new ArrayList<>()).stream().anyMatch(nestedPath::equalsIgnoreCase)) {
                            continue;
                        } else if (issue3IgnoredDirectories.getOrDefault(impactingFile, new ArrayList<>()).stream().anyMatch(nestedPath::startsWith)) {
                            remainingRules.add(rule);
                            remainingRules.add(new FastIgnoreRule("!" + nestedPath));
                            continue;
                        } else if (!rule.isMatch(nestedPath, isDirectory, true) || !rule.getResult()) {
                            remainingRules.add(rule);
                            continue;
                        } else if (rule.toString().equals(nestedPath)) {
                            continue;
                        } else if (rule.getNameOnly() || rule.dirOnly()) {
                            remainingRules.add(rule);
                            remainingRules.add(new FastIgnoreRule("!" + nestedPath));
                            continue;
                        }
                        remainingRules.add(rule);
                    }
                    IgnoreNode replacedNode = new IgnoreNode(remainingRules);
                    rules.put(impactingFile, replacedNode);
                    if (CHECK_PARENT.equals(replacedNode.isIgnored(nestedPath, isDirectory))) {
                        continue;
                    }
                }
                break;
            }
        }

        public void issue3Directories(final String gitignoreFileName, final String ignoreFile) {
            Arrays.stream(ignoreFile.split("\\r?\\n")).forEach(line -> {
                if (line.startsWith("#") || StringUtils.isBlank(line)) {
                    return;
                }
                Map<String, List<String>> directories = issue3IgnoredDirectories;
                if (line.startsWith("!")) {
                    line = line.substring(1);
                    directories = issue3NegatedDirectories;
                }
                if (!line.startsWith("/")) {
                    // lines not starting with / are not exact directory matches but equivalent to **/line/
                    return;
                }
                if (line.endsWith("/")) {
                    directories.computeIfAbsent(gitignoreFileName, k -> new ArrayList<>()).add(line);
                }
            });
        }

        @Nullable
        private Boolean isIgnoredByDirectoryIssue3Bypass(String gitignoreFileName, String path) {
            List<String> ignoredDirectories = issue3IgnoredDirectories.get(gitignoreFileName);
            List<String> negatedDirectories = issue3NegatedDirectories.get(gitignoreFileName);
            if (negatedDirectories != null) {
                for (String negatedDirectory : negatedDirectories) {
                    if (path.startsWith(negatedDirectory)) {
                        return false;
                    }
                }
            }
            if (ignoredDirectories != null) {
                for (String ignoredDirectory : ignoredDirectories) {
                    if (path.startsWith(ignoredDirectory)) {
                        return true;
                    }
                }
            }
            return null;
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Repository acc) {

        for (String path : paths) {
            acc.exclude(path);
        }

        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext executionContext) {
                return super.isAcceptable(sourceFile, executionContext) && sourceFile.getSourcePath().endsWith(".gitignore");
            }

            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                String gitignoreFileName = text.getSourcePath().toString();
                gitignoreFileName = gitignoreFileName.startsWith("/") ? gitignoreFileName : "/" + gitignoreFileName;
                IgnoreNode ignoreNode = acc.getRules().get(gitignoreFileName.substring(0, gitignoreFileName.lastIndexOf("/") + 1));
                if (ignoreNode != null) {
                    String separator = text.getText().contains("\r\n") ? "\r\n" : "\n";
                    List<FastIgnoreRule> rules = ignoreNode.getRules();
                    List<String> newRules = Arrays.stream(text.getText().split(separator)).filter(line -> {
                        if (line.startsWith("#") || StringUtils.isBlank(line)) {
                            return true;
                        }
                        if (rules.stream().anyMatch(rule -> line.equalsIgnoreCase(rule.toString()))) {
                            return true;
                        }
                        return false;
                    }).collect(Collectors.toList());
                    for (int i = 0; i < rules.size(); i++) {
                        FastIgnoreRule ignoreRule = rules.get(i);
                        if (newRules.stream().noneMatch(rule -> rule.equalsIgnoreCase(ignoreRule.toString()))) {
                            //Can we not find the position to insert the rule using surrounding rules as best as possible?
                            newRules.add(ignoreRule.toString());
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < newRules.size(); i++) {
                        if (i > 0) {
                            sb.append(separator);
                        }
                        sb.append(newRules.get(i));
                    }
                    return text.withText(sb.toString());
                }
                return super.visitText(text, executionContext);
            }
        };
    }
}
