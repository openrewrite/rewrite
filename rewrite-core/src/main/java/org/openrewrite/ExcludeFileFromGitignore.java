package org.openrewrite;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.jgit.ignore.FastIgnoreRule;
import org.openrewrite.jgit.ignore.IgnoreNode;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.openrewrite.ExcludeFileFromGitignore.Repository;
import static org.openrewrite.jgit.ignore.IgnoreNode.MatchResult.CHECK_PARENT;
import static org.openrewrite.jgit.ignore.IgnoreNode.MatchResult.IGNORED;

@Value
@EqualsAndHashCode(callSuper = false)
public class ExcludeFileFromGitignore extends ScanningRecipe<Repository> {

    @Override
    public String getDisplayName() {
        return "Remove ignoral of files or directories in .gitignore";
    }

    @Override
    public String getDescription() {
        return "This recipe will remove a file or directory to the .gitignore file. " +
                "If the file or directory is already in the .gitignore file, it will be removed." +
                "If the file or directory is not in the .gitignore file, no action will be taken.";
    }

    @Option(displayName = "Paths",
            description = "The paths to find and remove from the gitignore files.",
            example = "blacklist")
    List<String> paths;

    @Override
    public Repository getInitialValue(ExecutionContext ctx) {
        return new Repository();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Repository acc) {
        return new PlainTextVisitor<ExecutionContext>(){
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

        public void exclude(String path) {
            String normalizedPath = path.startsWith("/") ? path : "/" + path;
            List<String> impactingFiles = rules.keySet().stream()
                    .filter(k -> normalizedPath.toLowerCase().startsWith(k.toLowerCase()))
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .collect(Collectors.toList());

            IgnoreNode.MatchResult isIgnored;
            boolean isDirectory = path.endsWith("/");
            for (String impactingFile : impactingFiles) {
                IgnoreNode ignoreNode = rules.get(impactingFile);
                String nestedPath = normalizedPath.substring(impactingFile.length() - 1);
                isIgnored = ignoreNode.isIgnored(nestedPath, isDirectory);
                if (CHECK_PARENT.equals(isIgnored)) {
                    continue;
                }
                if (IGNORED.equals(isIgnored)) {
                    List<FastIgnoreRule> remainingRules = new ArrayList<>();
                    for (FastIgnoreRule rule : ignoreNode.getRules()) {
                        if (!rule.isMatch(nestedPath, isDirectory, true) || !rule.getResult()) {
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
                            //TODO find the position to insert the rule using surrounding rules as best as possible
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
