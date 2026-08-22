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
package org.openrewrite.docker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.tree.Docker;

import java.util.Locale;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpperCaseInstructions extends Recipe {

    @Option(displayName = "Casing",
            description = "The casing every instruction keyword is written in. Defaults to `uppercase`, " +
                    "the convention Docker's own documentation follows.",
            valid = {"uppercase", "lowercase"},
            required = false,
            example = "uppercase")
    @Nullable
    String casing;

    @Override
    public String getDisplayName() {
        return "Write Dockerfile instruction keywords in one casing";
    }

    @Override
    public String getDescription() {
        return "BuildKit's `ConsistentInstructionCasing` check reports a Dockerfile that writes its instruction " +
                "keywords in more than one casing, since `FROM` and `from` name the same instruction. This writes " +
                "every keyword, and the `AS` of a `FROM`, in the configured casing so that the file is consistent.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean upper = !"lowercase".equals(casing);

        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable Docker visit(@Nullable Tree tree, ExecutionContext ctx) {
                Docker docker = super.visit(tree, ctx);
                if (docker instanceof Docker.Instruction) {
                    Docker.Instruction instruction = (Docker.Instruction) docker;
                    return withKeyword(instruction, cased(instruction.getKeyword()));
                }
                return docker;
            }

            @Override
            public Docker.From.@Nullable As visitFromAs(Docker.From.As as, ExecutionContext ctx) {
                Docker.From.As a = super.visitFromAs(as, ctx);
                return a == null ? null : a.withKeyword(cased(a.getKeyword()));
            }

            private String cased(String keyword) {
                return upper ? keyword.toUpperCase(Locale.ROOT) : keyword.toLowerCase(Locale.ROOT);
            }
        };
    }

    private static Docker.Instruction withKeyword(Docker.Instruction instruction, String keyword) {
        if (instruction.getKeyword().equals(keyword)) {
            return instruction;
        }
        if (instruction instanceof Docker.From) {
            return ((Docker.From) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Run) {
            return ((Docker.Run) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Add) {
            return ((Docker.Add) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Copy) {
            return ((Docker.Copy) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Arg) {
            return ((Docker.Arg) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Env) {
            return ((Docker.Env) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Label) {
            return ((Docker.Label) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Cmd) {
            return ((Docker.Cmd) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Entrypoint) {
            return ((Docker.Entrypoint) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Expose) {
            return ((Docker.Expose) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Volume) {
            return ((Docker.Volume) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Shell) {
            return ((Docker.Shell) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Workdir) {
            return ((Docker.Workdir) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.User) {
            return ((Docker.User) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Stopsignal) {
            return ((Docker.Stopsignal) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Onbuild) {
            return ((Docker.Onbuild) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Healthcheck) {
            return ((Docker.Healthcheck) instruction).withKeyword(keyword);
        }
        if (instruction instanceof Docker.Maintainer) {
            return ((Docker.Maintainer) instruction).withKeyword(keyword);
        }
        return instruction;
    }
}
