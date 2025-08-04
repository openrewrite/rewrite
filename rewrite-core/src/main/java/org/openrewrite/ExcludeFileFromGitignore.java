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
package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.jgit.ignore.FastIgnoreRule;
import org.openrewrite.jgit.ignore.IgnoreNode;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import static java.util.Collections.*;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.join;
import static org.openrewrite.ExcludeFileFromGitignore.Repository;
import static org.openrewrite.PathUtils.separatorsToUnix;
import static org.openrewrite.jgit.ignore.IgnoreNode.MatchResult.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class ExcludeFileFromGitignore extends ScanningRecipe<Repository> {

    @Option(displayName = "Paths", description = "The paths to find and remove from the gitignore files.", example = "/folder/file.txt")
    List<String> paths;

    @Override
    public String getDisplayName() {
        return "Remove ignoral of files or directories from .gitignore";
    }

    @Override
    public String getDescription() {
        return "This recipe will remove a file or directory from the .gitignore file. " +
               "If the file or directory is already in the .gitignore file, it will be removed or negated. " +
               "If the file or directory is not in the .gitignore file, no action will be taken.";
    }

    @Override
    public Repository getInitialValue(ExecutionContext ctx) {
        return new Repository();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Repository acc) {
        return Preconditions.check(new FindSourceFiles("**/.gitignore"), new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                try {
                    acc.addGitignoreFile(text);
                } catch (IOException e) {
                    throw new RecipeException("Failed to parse the .gitignore file", e);
                }
                return super.visitText(text, ctx);
            }
        });
    }

    @Override
    public Collection<? extends SourceFile> generate(Repository acc, ExecutionContext ctx) {
        for (String path : paths) {
            acc.exclude(path);
        }
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Repository acc) {
        return Preconditions.check(new FindSourceFiles("**/.gitignore"), new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                CustomIgnoreNode ignoreNode = acc.rules.get(asGitignoreFileLocation(text));
                if (ignoreNode != null) {
                    String separator = text.getText().contains("\r\n") ? "\r\n" : "\n";
                    List<String> newRules = ignoreNode.getRules().stream().map(IgnoreRule::getText).collect(toList());
                    String[] currentContent = text.getText().split(separator);
                    text = text.withText(join(sortRules(currentContent, newRules), separator));
                }
                return text;
            }

            private List<String> sortRules(String[] originalRules, List<String> newRules) {
                LinkedList<String> results = new LinkedList<>();
                Arrays.stream(originalRules).filter(line -> {
                    if (StringUtils.isBlank(line) || line.startsWith("#")) {
                        return true;
                    }
                    return newRules.stream().anyMatch(line::equalsIgnoreCase);
                }).forEach(results::add);

                int resultsIndexCurrentlyAt = 0;
                for (String newRule : newRules) {
                    List<String> resultsSubList = results.subList(resultsIndexCurrentlyAt, results.size());
                    if (resultsSubList.stream().noneMatch(rule -> rule.equalsIgnoreCase(newRule))) {
                        if (resultsIndexCurrentlyAt >= results.size()) {
                            results.add(newRule);
                        } else {
                            results.add(resultsIndexCurrentlyAt, newRule);
                        }
                    } else {
                        resultsIndexCurrentlyAt += resultsSubList.indexOf(newRule);
                    }
                    resultsIndexCurrentlyAt++;
                }

                return distinctValuesStartingReversed(results);
            }

            private List<String> distinctValuesStartingReversed(List<String> list) {
                LinkedList<String> filteredList = new LinkedList<>();
                ListIterator<String> iterator = list.listIterator(list.size());

                while (iterator.hasPrevious()) {
                    String previous = iterator.previous();
                    if (StringUtils.isBlank(previous) || previous.startsWith("#") || !filteredList.contains(previous)) {
                        filteredList.addFirst(previous);
                    }
                }

                return filteredList;
            }
        });
    }

    public static class Repository {
        private final Map<String, CustomIgnoreNode> rules = new HashMap<>();

        public void exclude(String path) {
            path = separatorsToUnix(path);
            String normalizedPath = path.startsWith("/") ? path : "/" + path;
            List<String> impactingFiles = rules.keySet()
                    .stream()
                    .filter(k -> normalizedPath.toLowerCase().startsWith(k.toLowerCase()))
                    .sorted(comparingInt(String::length).reversed())
                    .collect(toList());

            for (String impactingFile : impactingFiles) {
                CustomIgnoreNode ignoreNode = rules.get(impactingFile);
                String nestedPath = normalizedPath.substring(impactingFile.length() - 1);

                while (IGNORED == ignoreNode.isIgnored(nestedPath)) {
                    List<IgnoreRule> existingRules = ignoreNode.getRules();
                    LinkedHashSet<FastIgnoreRule> remainingRules = new LinkedHashSet<>();
                    for (int i = existingRules.size() - 1; i > -1; i--) {
                        IgnoreRule rule = existingRules.get(i);
                        remainingRules.addAll(rule.negateIfNecessary(nestedPath));
                    }
                    ArrayList<FastIgnoreRule> ignoreRules = new ArrayList<>(remainingRules);
                    reverse(ignoreRules);
                    ignoreNode = new CustomIgnoreNode(ignoreRules, ignoreNode.getPath());
                    if (ignoreRules.size() == existingRules.size()) {
                        break;
                    }
                }
                rules.put(impactingFile, ignoreNode);

                if (CHECK_PARENT == ignoreNode.isIgnored(nestedPath)) {
                    continue;
                }
                // There is already an ignore rule for the path, so not needed to check parent rules.
                break;
            }
        }

        public void addGitignoreFile(PlainText text) throws IOException {
            CustomIgnoreNode ignoreNode = CustomIgnoreNode.of(text);
            rules.put(ignoreNode.path, ignoreNode);
        }
    }

    @Getter
    private static class CustomIgnoreNode {
        private final List<IgnoreRule> rules;
        private final String path;

        public CustomIgnoreNode(List<FastIgnoreRule> rules, String path) {
            this.rules = rules.stream().map(IgnoreRule::new).collect(toList());
            this.path = path;
        }

        static CustomIgnoreNode of(PlainText text) throws IOException {
            String gitignoreFileName = asGitignoreFileLocation(text);
            IgnoreNode ignoreNode = new IgnoreNode();
            ignoreNode.parse(gitignoreFileName, new ByteArrayInputStream(text.getText().getBytes()));

            return new CustomIgnoreNode(ignoreNode.getRules(), gitignoreFileName);
        }

        public IgnoreNode.MatchResult isIgnored(String path) {
            for (int i = rules.size() - 1; i > -1; i--) {
                IgnoreRule rule = rules.get(i);
                if (rule.isMatch(path)) {
                    if (rule.getResult()) {
                        return IGNORED;
                    } else {
                        return NOT_IGNORED;
                    }
                }
            }
            return CHECK_PARENT;
        }
    }

    private static class IgnoreRule {
        private final FastIgnoreRule rule;

        @Getter
        private final String text;

        public IgnoreRule(FastIgnoreRule rule) {
            this.rule = rule;
            this.text = rule.toString();
        }

        public boolean isMatch(String path) {
            return rule.isMatch(path, true, false) ||  rule.isMatch(path, true, true);
        }

        public boolean getResult() {
            return rule.getResult();
        }

        public List<FastIgnoreRule> negateIfNecessary(String nestedPath) {
            if (!isMatch(nestedPath) || !getResult()) {
                // If this rule has nothing to do with the path to remove, we keep it.
                // OR if this rule is a negation, we keep it.
                return singletonList(rule);
            } else if (text.equals(nestedPath)) {
                // If this rule is an exact match to the path to remove, we remove it.
                return emptyList();
            } else if (isMatch(nestedPath)) {
                if (text.contains("*")) {
                    return getWildcardRules(nestedPath);
                }
                if (("/" + text).equals(nestedPath)) {
                    // An entry not starting with a slash, but exact match otherwise needs to be negated using exact path as that leftover entry can match nested paths also.
                    return Arrays.asList(new FastIgnoreRule("!" + nestedPath), rule);
                }
                if (!rule.dirOnly()) {
                    // If the rule does not end with a slash, it's a matcher for both filenames and directories, so we must negate it with an exact path.
                    return Arrays.asList(new FastIgnoreRule("!" + nestedPath), rule);
                }
                return traversePaths(text, nestedPath, null, null);
            }
            // If we still have the rule, we keep it. --> not making changes to an unknown flow.
            return singletonList(rule);
        }

        @Override
        public String toString() {
            return text;
        }

        private List<FastIgnoreRule> getWildcardRules(String nestedPath) {
            if (!isMatch(nestedPath)) {
                return singletonList(rule);
            }
            if (text.startsWith("!")) {
                return singletonList(rule);
            }
            if (isWildcardedBetween(1, -1) || (splitRuleParts().length > 1 && isWildcardedBetween(0, 1) && isWildcardedBetween(-1, 0))) {
                // No support for wildcard in the middle of the path (yet?). So, we keep the rule.
                // No support for wildcards in both beginning and end.
                return singletonList(rule);
            }
            if (!hasOnlyOneWildcardGroup()) {
                // No support for multiple wildcard groups (yet?). So, we keep the rule.
                // No support for wildcards + text (yet?). So, we keep the rule.
                return singletonList(rule);
            }
            if (!isFullWildcard()) {
                return Arrays.asList(new FastIgnoreRule("!" + nestedPath), rule);
            }
            String wildcard = "*";
            if (text.contains("**")) {
                wildcard = "**";
            }
            if (isWildcardedBetween(0, 1)) {
                return traversePaths(text, nestedPath, null, (text.startsWith("/") ? "/" : "") + wildcard);
            }
            if (isWildcardedBetween(-1, 0)) {
                // If the wildcard is at the end of the path, we should negate the rule.
                return traversePaths(text, nestedPath, wildcard + (text.endsWith("/") ? "/" : ""), null);
            }
            // In any other case, we will keep the rule.
            return singletonList(rule);
        }

        private boolean isFullWildcard() {
            if (!text.contains("*")) {
                return false;
            }
            // only / or empty before and after the wildcard
            int begin = text.indexOf("*");
            int end = text.lastIndexOf("*");

            return (begin == 0 || text.charAt(begin - 1) == '/') && (end == text.length() - 1 || text.charAt(end + 1) == '/');
        }

        private boolean hasOnlyOneWildcardGroup() {
            if (!text.contains("*")) {
                return false;
            }
            int firstWildcard = text.indexOf("*");
            int lastWildcard = text.lastIndexOf("*");
            return firstWildcard == lastWildcard || lastWildcard - firstWildcard == 1;
        }

        private boolean isWildcardedBetween(int start, int end) {
            if (!text.contains("*")) {
                return false;
            }
            String[] parts = splitRuleParts();
            int startIdx = start;
            if (startIdx < 0) {
                startIdx = parts.length + start;
            }
            int endIdx = end;
            if (endIdx <= 0) {
                endIdx = parts.length + end;
            }
            for (int i = startIdx; i < endIdx; i++) {
                if (parts[i].contains("*")) {
                    return true;
                }
            }
            return false;
        }

        private String[] splitRuleParts() {
            String rulePath = text;
            if (rulePath.startsWith("!")) {
                rulePath = rulePath.substring(1);
            }
            if (rulePath.startsWith("/")) {
                rulePath = rulePath.substring(1);
            }
            if (rulePath.endsWith("/")) {
                rulePath = rulePath.substring(0, rulePath.length() - 1);
            }
            return rulePath.split("/");
        }

        private static List<FastIgnoreRule> traversePaths(String originalRule, String path, @Nullable String wildcardSuffix, @Nullable String wildcardPrefix) {
            String rule = originalRule;
            ArrayList<FastIgnoreRule> traversedRemainingRules = new ArrayList<>();
            if (wildcardSuffix != null && rule.endsWith(wildcardSuffix)) {
                rule = rule.substring(0, rule.length()-wildcardSuffix.length());
            }
            if (wildcardPrefix != null && rule.startsWith(wildcardPrefix)) {
                rule = path.substring(0, path.indexOf(rule.substring(wildcardPrefix.length()))) + rule.substring(wildcardPrefix.length());
                traversedRemainingRules.add(new FastIgnoreRule(originalRule + (originalRule.endsWith("/") ? "*" : "/*")));
                traversedRemainingRules.add(new FastIgnoreRule("!" + rule));
            }
            StringBuilder rulePath = new StringBuilder(rule);
            String pathToTraverse = path.substring(rule.length());

            if (originalRule.contains("*")) {
                if (pathToTraverse.isEmpty() && wildcardSuffix != null) {
                    return Arrays.asList(new FastIgnoreRule("!" + rule), new FastIgnoreRule(originalRule + (originalRule.endsWith("/") ? "*" : "/*")));
                } else if (pathToTraverse.isEmpty() && wildcardPrefix != null) {
                    return Arrays.asList(new FastIgnoreRule("!" + rule), new FastIgnoreRule(originalRule));
                }
            } else {
                if (pathToTraverse.replace("/", "").isEmpty()) {
                    return Arrays.asList(new FastIgnoreRule("!" + rule), new FastIgnoreRule(originalRule));
                }
            }
            String pathToSplit = pathToTraverse.startsWith("/") ? pathToTraverse.substring(1) : pathToTraverse;
            pathToSplit = pathToSplit.endsWith("/") ? pathToSplit.substring(0, pathToSplit.length() - 1) : pathToSplit;
            String[] splitPath = pathToSplit.split("/");
            for (int j = 0; j < splitPath.length; j++) {
                String s = splitPath[j];
                traversedRemainingRules.add(new FastIgnoreRule(rulePath + (wildcardSuffix != null ? wildcardSuffix : "*")));
                rulePath.append(s);
                traversedRemainingRules.add(new FastIgnoreRule("!" + rulePath + (j < splitPath.length - 1 || path.endsWith("/") ? "/" : "")));
                rulePath.append("/");
            }
            reverse(traversedRemainingRules);
            return traversedRemainingRules;
        }
    }

    private static String asGitignoreFileLocation(PlainText text) {
        String gitignoreFileName = separatorsToUnix(text.getSourcePath().toString());
        gitignoreFileName = gitignoreFileName.startsWith("/") ? gitignoreFileName : "/" + gitignoreFileName;
        return gitignoreFileName.substring(0, gitignoreFileName.lastIndexOf("/") + 1);
    }
}
