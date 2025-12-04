/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleMultiDependency;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.semver.LatestIntegration;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveRedundantSecurityResolutionRules extends Recipe {

    private static final String DEFAULT_SECURITY_PATTERN = "(CVE-\\d|GHSA-[a-z0-9])";

    @Option(displayName = "Security pattern",
            description = "A regular expression pattern to identify security-related resolution rules by matching " +
                          "against the `because` clause. Rules matching this pattern will be considered for removal. " +
                          "Default pattern matches CVE identifiers (e.g., `CVE-2024-1234`) and GitHub Security " +
                          "Advisory identifiers (e.g., `GHSA-xxxx-xxxx-xxxx`).",
            example = "(CVE-\\d|GHSA-[a-z0-9])",
            required = false)
    @Nullable
    String securityPattern;

    @Override
    public String getDisplayName() {
        return "Remove redundant dependency resolution rules";
    }

    @Override
    public String getDescription() {
        return "Remove `resolutionStrategy.eachDependency` rules that pin dependencies to versions that are already " +
               "being managed by a platform/BOM to equal or newer versions. Only removes rules that have a security " +
               "advisory identifier (CVE or GHSA) in the `because` clause, unless a custom pattern is specified.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern compiledPattern = Pattern.compile(
                securityPattern != null ? securityPattern : DEFAULT_SECURITY_PATTERN,
                Pattern.CASE_INSENSITIVE
        );

        JavaIsoVisitor<ExecutionContext> removeRulesVisitor = new JavaIsoVisitor<ExecutionContext>() {
            @Nullable
            GradleProject gradleProject;
            final Map<String, List<ResolvedPom>> buildscriptPlatforms = new HashMap<>();
            final Map<String, List<ResolvedPom>> projectPlatforms = new HashMap<>();
            boolean initialized;
            boolean insideBuildscript;

            private void maybeInitialize(ExecutionContext ctx) {
                if (initialized) {
                    return;
                }
                initialized = true;

                Cursor rootCursor = getCursor();
                while (rootCursor.getParent() != null && rootCursor.getParent().getValue() != Cursor.ROOT_VALUE) {
                    rootCursor = rootCursor.getParent();
                }
                Tree tree = rootCursor.getValue();
                if (tree instanceof JavaSourceFile) {
                    Optional<GradleProject> maybeGp = tree.getMarkers().findFirst(GradleProject.class);
                    if (maybeGp.isPresent()) {
                        gradleProject = maybeGp.get();

                        // Find all platforms and download their POMs to get managed versions
                        // Use a custom visitor that properly tracks buildscript context
                        MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                        new JavaIsoVisitor<ExecutionContext>() {
                            boolean scannerInBuildscript;

                            @Override
                            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                                boolean wasInBuildscript = scannerInBuildscript;
                                if ("buildscript".equals(method.getSimpleName())) {
                                    scannerInBuildscript = true;
                                }

                                J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);

                                // Check if this is a dependency declaration containing platforms
                                GradleMultiDependency multiDep = GradleMultiDependency.matcher().get(getCursor()).orElse(null);
                                if (multiDep != null) {
                                    boolean inBuildscript = scannerInBuildscript;
                                    multiDep.forEach(gradleDependency -> {
                                        if (gradleDependency.isPlatform()) {
                                            try {
                                                ResolvedPom platformPom = mpd.download(
                                                                gradleDependency.getGav(), null, null, gradleProject.getMavenRepositories())
                                                        .resolve(emptyList(), mpd, executionContext);
                                                Map<String, List<ResolvedPom>> targetMap = inBuildscript ? buildscriptPlatforms : projectPlatforms;
                                                targetMap.computeIfAbsent(gradleDependency.getConfigurationName(), k -> new ArrayList<>())
                                                        .add(platformPom);
                                            } catch (MavenDownloadingException ignored) {
                                            }
                                        }
                                    });
                                }

                                scannerInBuildscript = wasInBuildscript;
                                return m;
                            }
                        }.visit(tree, ctx, getCursor());
                    }
                }
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                maybeInitialize(ctx);

                // Track when we enter/exit buildscript block
                boolean wasInsideBuildscript = insideBuildscript;
                if ("buildscript".equals(method.getSimpleName())) {
                    insideBuildscript = true;
                }

                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                // Restore state after visiting children
                boolean currentlyInBuildscript = insideBuildscript;
                insideBuildscript = wasInsideBuildscript;

                if (!isEachDependency(m, getCursor())) {
                    return m;
                }

                if (m.getArguments().isEmpty()) {
                    return m;
                }

                Expression maybeClosure = m.getArguments().get(0);
                if (!(maybeClosure instanceof J.Lambda) || !(((J.Lambda) maybeClosure).getBody() instanceof J.Block)) {
                    return m;
                }

                J.Lambda closure = (J.Lambda) maybeClosure;
                J.Block closureBody = (J.Block) closure.getBody();

                // Determine which platform map to use based on context
                Map<String, List<ResolvedPom>> platforms = currentlyInBuildscript ? buildscriptPlatforms : projectPlatforms;

                // Process each if statement to check if it should be removed
                List<Statement> newStatements = processStatements(closureBody.getStatements(), platforms);

                if (newStatements.equals(closureBody.getStatements())) {
                    return m;
                }

                // If all statements were removed, remove the entire eachDependency block
                if (newStatements.isEmpty() || allStatementsAreEmptyReturns(newStatements)) {
                    return null;
                }

                return m.withArguments(Collections.singletonList(
                        closure.withBody(closureBody.withStatements(newStatements))
                ));
            }

            private List<Statement> processStatements(List<Statement> statements, Map<String, List<ResolvedPom>> platforms) {
                return ListUtils.flatMap(statements, statement -> {
                    if (statement instanceof J.If) {
                        return processIfStatement((J.If) statement, platforms);
                    }
                    return statement;
                });
            }

            @SuppressWarnings("NullableProblems")
            private J.If processIfStatement(J.If ifStatement, Map<String, List<ResolvedPom>> platforms) {
                ResolutionRuleInfo ruleInfo = extractRuleInfo(ifStatement);
                if (ruleInfo != null && shouldRemoveRule(ruleInfo, platforms)) {
                    // If there's an else-if chain, promote the else part
                    if (ifStatement.getElsePart() != null) {
                        Statement elseBody = ifStatement.getElsePart().getBody();
                        if (elseBody instanceof J.If) {
                            return processIfStatement((J.If) elseBody, platforms);
                        } else if (elseBody instanceof J.Block) {
                            return null;
                        }
                    }
                    return null;
                }

                // Process else-if chain recursively
                if (ifStatement.getElsePart() != null) {
                    Statement elseBody = ifStatement.getElsePart().getBody();
                    if (elseBody instanceof J.If) {
                        J.If processedElseIf = processIfStatement((J.If) elseBody, platforms);
                        if (processedElseIf == null) {
                            J.If originalElseIf = (J.If) elseBody;
                            if (originalElseIf.getElsePart() != null && !(originalElseIf.getElsePart().getBody() instanceof J.If)) {
                                return ifStatement.withElsePart(originalElseIf.getElsePart());
                            }
                            return ifStatement.withElsePart(null);
                        }
                        return ifStatement.withElsePart(ifStatement.getElsePart().withBody(processedElseIf));
                    }
                }

                return ifStatement;
            }

            private boolean shouldRemoveRule(ResolutionRuleInfo ruleInfo, Map<String, List<ResolvedPom>> platforms) {
                // Only remove rules matching the security pattern
                if (!isSecurityRelated(ruleInfo.because)) {
                    return false;
                }

                // Find the managed version from platforms
                String managedVersion = findManagedVersion(ruleInfo.groupId, ruleInfo.artifactId, platforms);
                if (managedVersion == null) {
                    return false;
                }

                // Remove if managed version >= pinned version
                int comparison = new LatestIntegration(null).compare(null, managedVersion, ruleInfo.version);
                return comparison >= 0;
            }

            private boolean isSecurityRelated(@Nullable String because) {
                if (because == null || because.isEmpty()) {
                    return false;
                }
                return compiledPattern.matcher(because).find();
            }

            private @Nullable String findManagedVersion(String groupId, String artifactId, Map<String, List<ResolvedPom>> platforms) {
                // Look through all platforms for a managed version
                for (List<ResolvedPom> platformList : platforms.values()) {
                    for (ResolvedPom platform : platformList) {
                        String managedVersion = platform.getManagedVersion(groupId, artifactId, null, null);
                        if (managedVersion != null) {
                            return managedVersion;
                        }
                    }
                }
                return null;
            }

            private boolean allStatementsAreEmptyReturns(List<Statement> statements) {
                for (Statement s : statements) {
                    if (!(s instanceof J.Return) || ((J.Return) s).getExpression() != null) {
                        return false;
                    }
                }
                return true;
            }
        };

        // Combine both visitors: first remove redundant rules, then clean up empty blocks
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile)) {
                    return (J) tree;
                }
                // First pass: remove redundant resolution rules
                J result = removeRulesVisitor.visit(tree, ctx);
                // Second pass: remove empty configurations.all blocks
                return new MaybeRemoveEmptyConfigurationsAll().visit(result, ctx);
            }
        });
    }

    @Value
    static class ResolutionRuleInfo {
        String groupId;
        String artifactId;
        String version;
        @Nullable String because;
    }

    private static @Nullable ResolutionRuleInfo extractRuleInfo(J.If ifStatement) {
        // Extract group and artifact from the if condition
        Expression predicate = ifStatement.getIfCondition().getTree();
        if (!(predicate instanceof J.Binary)) {
            return null;
        }

        J.Binary and = (J.Binary) predicate;
        if (and.getOperator() != J.Binary.Type.And) {
            return null;
        }

        AtomicReference<String> groupId = new AtomicReference<>();
        AtomicReference<String> artifactId = new AtomicReference<>();

        new JavaIsoVisitor<Integer>() {
            @Override
            public J.Binary visitBinary(J.Binary binary, Integer integer) {
                J.Binary b = super.visitBinary(binary, integer);
                if (b.getOperator() != J.Binary.Type.Equal) {
                    return b;
                }

                J.FieldAccess access = null;
                J.Literal literal = null;

                if (b.getLeft() instanceof J.FieldAccess && b.getRight() instanceof J.Literal) {
                    access = (J.FieldAccess) b.getLeft();
                    literal = (J.Literal) b.getRight();
                } else if (b.getRight() instanceof J.FieldAccess && b.getLeft() instanceof J.Literal) {
                    access = (J.FieldAccess) b.getRight();
                    literal = (J.Literal) b.getLeft();
                }

                if (access == null || literal == null || literal.getValue() == null) {
                    return b;
                }

                String fieldName = access.getSimpleName();
                String value = literal.getValue().toString();

                if ("group".equals(fieldName)) {
                    groupId.set(value);
                } else if ("name".equals(fieldName)) {
                    artifactId.set(value);
                }

                return b;
            }
        }.visit(and, 0);

        if (groupId.get() == null || artifactId.get() == null) {
            return null;
        }

        // Extract version and because from the then block
        Statement thenPart = ifStatement.getThenPart();
        if (!(thenPart instanceof J.Block)) {
            return null;
        }

        AtomicReference<String> version = new AtomicReference<>();
        AtomicReference<String> because = new AtomicReference<>();

        new JavaIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                J.MethodInvocation m = super.visitMethodInvocation(method, integer);

                if ("useVersion".equals(m.getSimpleName()) && !m.getArguments().isEmpty()) {
                    Expression arg = m.getArguments().get(0);
                    if (arg instanceof J.Literal && ((J.Literal) arg).getValue() != null) {
                        version.set(((J.Literal) arg).getValue().toString());
                    }
                } else if ("because".equals(m.getSimpleName()) && !m.getArguments().isEmpty()) {
                    Expression arg = m.getArguments().get(0);
                    if (arg instanceof J.Literal && ((J.Literal) arg).getValue() != null) {
                        because.set(((J.Literal) arg).getValue().toString());
                    }
                }

                return m;
            }
        }.visit(thenPart, 0);

        if (version.get() == null) {
            return null;
        }

        return new ResolutionRuleInfo(groupId.get(), artifactId.get(), version.get(), because.get());
    }

    private static boolean isEachDependency(J.MethodInvocation m, Cursor cursor) {
        if (!"eachDependency".equals(m.getSimpleName())) {
            return false;
        }
        // Pattern 1: resolutionStrategy.eachDependency { }
        if (m.getSelect() instanceof J.Identifier &&
            "resolutionStrategy".equals(((J.Identifier) m.getSelect()).getSimpleName())) {
            return true;
        }
        // Pattern 2: resolutionStrategy { eachDependency { } } - no select, inside resolutionStrategy block
        if (m.getSelect() == null) {
            return isInsideResolutionStrategyBlock(cursor);
        }
        return false;
    }

    private static boolean isInsideResolutionStrategyBlock(Cursor cursor) {
        Cursor parent = cursor.dropParentUntil(value ->
                value == Cursor.ROOT_VALUE ||
                (value instanceof J.MethodInvocation &&
                 "resolutionStrategy".equals(((J.MethodInvocation) value).getSimpleName())));
        return parent.getValue() != Cursor.ROOT_VALUE;
    }

    static class MaybeRemoveEmptyConfigurationsAll extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.@Nullable MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            // Track original statement count for configurations { } blocks
            int originalSize = -1;
            if ("configurations".equals(method.getSimpleName()) &&
                method.getArguments().size() == 1 &&
                method.getArguments().get(0) instanceof J.Lambda) {

                J.Lambda lambda = (J.Lambda) method.getArguments().get(0);
                if (lambda.getBody() instanceof J.Block) {
                    originalSize = ((J.Block) lambda.getBody()).getStatements().size();
                }
            }

            // Visit children first
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

            // Handle removal of empty blocks:
            // 1. configurations.all { } or configurations.<configName> { }
            // 2. resolutionStrategy { } blocks
            // 3. Nested configuration blocks inside configurations { }
            // 4. Empty configurations { } block itself
            // 5. configurations.named("...") { } (Kotlin DSL)
            if (isEmptyConfigurationsAll(m, getCursor()) || isEmptyConfigurationsBlock(m) ||
                isEmptyResolutionStrategyBlock(m) || isEmptyNestedConfigurationBlock(m, getCursor()) ||
                isEmptyConfigurationsMethodBlock(m) || isEmptyNamedConfigurationBlock(m, getCursor())) {
                return null;
            }

            // For configurations { }, clean up whitespace if statements were removed
            if (originalSize > 0 &&
                m.getArguments().size() == 1 &&
                m.getArguments().get(0) instanceof J.Lambda) {

                J.Lambda newLambda = (J.Lambda) m.getArguments().get(0);
                if (newLambda.getBody() instanceof J.Block) {
                    J.Block newBlock = (J.Block) newLambda.getBody();
                    int newSize = newBlock.getStatements().size();

                    if (newSize < originalSize && newSize > 0) {
                        // Statements were removed - clean up whitespace
                        newBlock = cleanupBlockWhitespace(newBlock);
                        newLambda = newLambda.withBody(newBlock);
                        m = m.withArguments(Collections.singletonList(newLambda));
                    }
                }
            }

            return m;
        }

        private static J.Block cleanupBlockWhitespace(J.Block block) {
            // Clean up the block's end space
            Space endSpace = block.getEnd();
            String endWs = endSpace.getWhitespace();
            int endNewlineCount = endWs.length() - endWs.replace("\n", "").length();
            if (endNewlineCount > 1) {
                int lastNewline = endWs.lastIndexOf('\n');
                block = block.withEnd(endSpace.withWhitespace(endWs.substring(lastNewline)));
            }

            // Also clean up the after-space of the last remaining statement
            List<JRightPadded<Statement>> stmts = block.getPadding().getStatements();
            if (!stmts.isEmpty()) {
                int lastIdx = stmts.size() - 1;
                JRightPadded<Statement> lastStmt = stmts.get(lastIdx);
                Space afterSpace = lastStmt.getAfter();
                String afterWs = afterSpace.getWhitespace();
                int afterNewlineCount = afterWs.length() - afterWs.replace("\n", "").length();
                if (afterNewlineCount > 1) {
                    // Keep only the first newline
                    int firstNewline = afterWs.indexOf('\n');
                    List<JRightPadded<Statement>> newStmts = new ArrayList<>(stmts);
                    newStmts.set(lastIdx, lastStmt.withAfter(afterSpace.withWhitespace(afterWs.substring(0, firstNewline + 1))));
                    block = block.getPadding().withStatements(newStmts);
                }
            }

            return block;
        }

        private static boolean isEmptyAllBlock(J.MethodInvocation m) {
            if (!"all".equals(m.getSimpleName())) {
                return false;
            }
            if (m.getArguments().size() != 1 || !(m.getArguments().get(0) instanceof J.Lambda)) {
                return false;
            }
            J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
            if (!(lambda.getBody() instanceof J.Block)) {
                return false;
            }
            J.Block block = (J.Block) lambda.getBody();
            // Empty if no statements OR only contains an empty return statement
            // OR only contains resolutionStrategy statements (which means they were emptied)
            if (block.getStatements().isEmpty()) {
                return true;
            }
            for (Statement stmt : block.getStatements()) {
                if (stmt instanceof J.Return && ((J.Return) stmt).getExpression() == null) {
                    continue; // empty return is okay
                }
                if (stmt instanceof J.MethodInvocation) {
                    J.MethodInvocation mi = (J.MethodInvocation) stmt;
                    // Check if this is resolutionStrategy.eachDependency with empty closure
                    if (isEmptyResolutionStrategy(mi)) {
                        continue;
                    }
                }
                // Found a non-empty statement
                return false;
            }
            return true;
        }

        private static boolean isEmptyResolutionStrategy(J.MethodInvocation m) {
            // Check for resolutionStrategy.eachDependency { } or resolutionStrategy { }
            if ("eachDependency".equals(m.getSimpleName())) {
                if (m.getSelect() instanceof J.Identifier &&
                    "resolutionStrategy".equals(((J.Identifier) m.getSelect()).getSimpleName())) {
                    if (m.getArguments().size() == 1 && m.getArguments().get(0) instanceof J.Lambda) {
                        J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
                        if (lambda.getBody() instanceof J.Block) {
                            J.Block block = (J.Block) lambda.getBody();
                            // Empty or only empty returns
                            if (block.getStatements().isEmpty()) {
                                return true;
                            }
                            for (Statement stmt : block.getStatements()) {
                                if (!(stmt instanceof J.Return) || ((J.Return) stmt).getExpression() != null) {
                                    return false;
                                }
                            }
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * Checks if this is an empty resolutionStrategy { } block.
         */
        private static boolean isEmptyResolutionStrategyBlock(J.MethodInvocation m) {
            if (!"resolutionStrategy".equals(m.getSimpleName())) {
                return false;
            }
            if (m.getArguments().size() != 1 || !(m.getArguments().get(0) instanceof J.Lambda)) {
                return false;
            }
            J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
            if (!(lambda.getBody() instanceof J.Block)) {
                return false;
            }
            J.Block block = (J.Block) lambda.getBody();
            if (block.getStatements().isEmpty()) {
                return true;
            }
            if (block.getStatements().size() == 1) {
                Statement stmt = block.getStatements().get(0);
                return stmt instanceof J.Return && ((J.Return) stmt).getExpression() == null;
            }
            return false;
        }

        /**
         * Checks if this is an empty configuration block inside configurations { } (e.g., compileClasspath { }).
         */
        private static boolean isEmptyNestedConfigurationBlock(J.MethodInvocation m, Cursor cursor) {
            // Check if we're inside a configurations { } block
            if (!isInsideConfigurationsBlock(cursor)) {
                return false;
            }
            // Skip "all" - handled separately
            if ("all".equals(m.getSimpleName())) {
                return false;
            }
            if (m.getArguments().size() != 1 || !(m.getArguments().get(0) instanceof J.Lambda)) {
                return false;
            }
            J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
            if (!(lambda.getBody() instanceof J.Block)) {
                return false;
            }
            J.Block block = (J.Block) lambda.getBody();
            if (block.getStatements().isEmpty()) {
                return true;
            }
            if (block.getStatements().size() == 1) {
                Statement stmt = block.getStatements().get(0);
                return stmt instanceof J.Return && ((J.Return) stmt).getExpression() == null;
            }
            return false;
        }

        /**
         * Checks if this is an empty configurations.<configName> { } block (e.g., configurations.compileClasspath { }).
         * This handles cases where the resolution strategy was on a specific configuration rather than configurations.all.
         */
        private static boolean isEmptyConfigurationsBlock(J.MethodInvocation m) {
            // Check if select is "configurations" identifier (e.g., configurations.compileClasspath { })
            if (!(m.getSelect() instanceof J.Identifier) ||
                !"configurations".equals(((J.Identifier) m.getSelect()).getSimpleName())) {
                return false;
            }
            // Don't handle "all" here - that's handled by isEmptyConfigurationsAll
            if ("all".equals(m.getSimpleName())) {
                return false;
            }
            if (m.getArguments().size() != 1 || !(m.getArguments().get(0) instanceof J.Lambda)) {
                return false;
            }
            J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
            if (!(lambda.getBody() instanceof J.Block)) {
                return false;
            }
            J.Block block = (J.Block) lambda.getBody();
            // Empty if no statements OR only contains an empty return statement
            if (block.getStatements().isEmpty()) {
                return true;
            }
            if (block.getStatements().size() == 1) {
                Statement stmt = block.getStatements().get(0);
                return stmt instanceof J.Return && ((J.Return) stmt).getExpression() == null;
            }
            return false;
        }

        private static boolean isEmptyConfigurationsAll(J.MethodInvocation m, Cursor cursor) {
            if (!"all".equals(m.getSimpleName())) {
                return false;
            }
            // Check for two patterns:
            // 1. configurations.all { } - select is "configurations" identifier
            // 2. all { } inside configurations { } block - select is null
            boolean isConfigurationsAll = false;
            if (m.getSelect() instanceof J.Identifier &&
                "configurations".equals(((J.Identifier) m.getSelect()).getSimpleName())) {
                isConfigurationsAll = true;
            } else if (m.getSelect() == null) {
                // Check if we're inside a configurations { } block
                isConfigurationsAll = isInsideConfigurationsBlock(cursor);
            }
            if (!isConfigurationsAll) {
                return false;
            }
            if (m.getArguments().size() != 1 || !(m.getArguments().get(0) instanceof J.Lambda)) {
                return false;
            }
            J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
            if (!(lambda.getBody() instanceof J.Block)) {
                return false;
            }
            J.Block block = (J.Block) lambda.getBody();
            // Empty if no statements OR only contains an empty return statement
            if (block.getStatements().isEmpty()) {
                return true;
            }
            if (block.getStatements().size() == 1) {
                Statement stmt = block.getStatements().get(0);
                return stmt instanceof J.Return && ((J.Return) stmt).getExpression() == null;
            }
            return false;
        }

        private static boolean isInsideConfigurationsBlock(Cursor cursor) {
            Cursor parent = cursor.dropParentUntil(value ->
                    value == Cursor.ROOT_VALUE ||
                    (value instanceof J.MethodInvocation &&
                     "configurations".equals(((J.MethodInvocation) value).getSimpleName())));
            return parent.getValue() != Cursor.ROOT_VALUE;
        }

        /**
         * Checks if this is an empty configurations.named("...") { } block (Kotlin DSL pattern).
         * Also handles named("...") { } inside a configurations { } block.
         */
        private static boolean isEmptyNamedConfigurationBlock(J.MethodInvocation m, Cursor cursor) {
            if (!"named".equals(m.getSimpleName())) {
                return false;
            }
            // Check for two patterns:
            // 1. configurations.named("...") { } - select is "configurations" identifier
            // 2. named("...") { } inside configurations { } block - select is null
            boolean isConfigurationsNamed = false;
            if (m.getSelect() instanceof J.Identifier &&
                "configurations".equals(((J.Identifier) m.getSelect()).getSimpleName())) {
                isConfigurationsNamed = true;
            } else if (m.getSelect() == null) {
                isConfigurationsNamed = isInsideConfigurationsBlock(cursor);
            }
            if (!isConfigurationsNamed) {
                return false;
            }
            // named() takes a string argument and a lambda: named("configName") { }
            if (m.getArguments().size() != 2) {
                return false;
            }
            Expression lastArg = m.getArguments().get(1);
            if (!(lastArg instanceof J.Lambda)) {
                return false;
            }
            J.Lambda lambda = (J.Lambda) lastArg;
            if (!(lambda.getBody() instanceof J.Block)) {
                return false;
            }
            J.Block block = (J.Block) lambda.getBody();
            // Empty if no statements OR only contains an empty return statement
            if (block.getStatements().isEmpty()) {
                return true;
            }
            if (block.getStatements().size() == 1) {
                Statement stmt = block.getStatements().get(0);
                return stmt instanceof J.Return && ((J.Return) stmt).getExpression() == null;
            }
            return false;
        }

        /**
         * Checks if this is an empty configurations { } block (method name is "configurations" with no select).
         */
        private static boolean isEmptyConfigurationsMethodBlock(J.MethodInvocation m) {
            if (!"configurations".equals(m.getSimpleName())) {
                return false;
            }
            // Only for configurations { } block (no select)
            if (m.getSelect() != null) {
                return false;
            }
            if (m.getArguments().size() != 1 || !(m.getArguments().get(0) instanceof J.Lambda)) {
                return false;
            }
            J.Lambda lambda = (J.Lambda) m.getArguments().get(0);
            if (!(lambda.getBody() instanceof J.Block)) {
                return false;
            }
            J.Block block = (J.Block) lambda.getBody();
            // Empty if no statements OR only contains an empty return statement
            if (block.getStatements().isEmpty()) {
                return true;
            }
            if (block.getStatements().size() == 1) {
                Statement stmt = block.getStatements().get(0);
                return stmt instanceof J.Return && ((J.Return) stmt).getExpression() == null;
            }
            return false;
        }
    }

}
