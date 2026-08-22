/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.docker.internal;

import org.openrewrite.SourceFile;
import org.openrewrite.docker.DockerIsoVisitor;
import org.openrewrite.docker.tree.Comment;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The build stages a repository asks for by name. A Dockerfile says which of its stages feed the image
 * it ends with, and nothing else; that a stage is built on its own is said elsewhere.
 */
public class BuildTargets {

    /// `docker build --target x`, a bake `target = "x"` or `target "x" {`, a compose `target: x`, a Go `Target: "x"`.
    private static final Pattern[] NAMED = {
            Pattern.compile("--target[= \\t]+\"?([A-Za-z0-9][A-Za-z0-9._-]*)"),
            Pattern.compile("target\\s*=\\s*\"([A-Za-z0-9][A-Za-z0-9._-]*)\""),
            Pattern.compile("target\\s+\"([A-Za-z0-9][A-Za-z0-9._-]*)\"\\s*\\{"),
            Pattern.compile("(?m)^[ \\t]*target:[ \\t]*\"?([A-Za-z0-9][A-Za-z0-9._-]*)"),
            Pattern.compile("Target:\\s*\"([A-Za-z0-9][A-Za-z0-9._-]*)\"")
    };

    /// Too loose to mean "target" on its own, so read only from files that exist to orchestrate builds,
    /// where a word matching a stage name is unlikely to be a coincidence.
    private static final Pattern QUOTED_WORD = Pattern.compile("\"([A-Za-z0-9][A-Za-z0-9._-]{0,63})\"");

    private static final Pattern BUILD_SCRIPT = Pattern.compile("(?i)" +
            ".*\\.(ya?ml|sh|bash|ps1|bat|py|hcl|mk)|.*_test\\.go|" +
            "(gnu)?makefile(\\..*)?|jenkinsfile(\\..*)?|docker-bake\\..*");

    private static final Pattern ORCHESTRATION = Pattern.compile("(?i)" +
            "docker-bake\\..*|.*compose.*\\.ya?ml|(gnu)?makefile(\\..*)?|.*\\.mk");

    private BuildTargets() {
    }

    public static boolean isBuildScript(Path path) {
        return BUILD_SCRIPT.matcher(path.getFileName().toString()).matches() || isWorkflow(path);
    }

    public static void collect(Path path, String source, Set<String> into) {
        for (Pattern pattern : NAMED) {
            Matcher matcher = pattern.matcher(source);
            while (matcher.find()) {
                into.add(matcher.group(1).toLowerCase(Locale.ROOT));
            }
        }
        if (isWorkflow(path) || ORCHESTRATION.matcher(path.getFileName().toString()).matches()) {
            Matcher matcher = QUOTED_WORD.matcher(source);
            while (matcher.find()) {
                into.add(matcher.group(1).toLowerCase(Locale.ROOT));
            }
        }
    }

    public static Set<String> inComments(Docker.File file) {
        StringBuilder comments = new StringBuilder();
        new DockerIsoVisitor<StringBuilder>() {
            @Override
            public Space visitSpace(Space space, StringBuilder out) {
                for (Comment comment : space.getComments()) {
                    out.append(comment.getText()).append('\n');
                }
                return super.visitSpace(space, out);
            }
        }.reduce(file, comments);

        Set<String> targets = new LinkedHashSet<>();
        collect(file.getSourcePath(), comments.toString(), targets);
        return targets;
    }

    public static void scan(SourceFile sourceFile, Set<String> into) {
        if (isBuildScript(sourceFile.getSourcePath())) {
            collect(sourceFile.getSourcePath(), sourceFile.printAll(), into);
        }
    }

    private static boolean isWorkflow(Path path) {
        return path.toString().replace('\\', '/').contains(".github/workflows/");
    }
}
