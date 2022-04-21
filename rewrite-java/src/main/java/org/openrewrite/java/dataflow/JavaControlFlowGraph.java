/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.dataflow;

import org.openrewrite.Incubating;
import org.openrewrite.dataflow.BasicBlock;
import org.openrewrite.dataflow.ControlFlowGraph;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Incubating(since = "7.22.0")
public class JavaControlFlowGraph extends ControlFlowGraph<Statement> {
    protected JavaControlFlowGraph(BasicBlock<Statement> bb, List<JavaControlFlowGraph> children,
                                   List<JavaControlFlowGraph> loops) {
        super(bb, children, loops);
    }

    /**
     * @param statement A local control flow statement is built from either a block
     *                  or a single statement of a conditional or loop "block".
     * @return A local control flow graph.
     */
    public JavaControlFlowGraph buildLocal(Statement statement) {
        return null;
//        return cfg;
    }

    public static void main(String[] args) {
        System.out.println(args);
//        Map<String, String> env = Collections.singletonMap("java.home", javaHome.toString());
//        Path jrtfsJar = javaHome.resolve("lib").resolve("jrt-fs.jar");
//        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jrt:/"), env, classloader)) {
//            Path modulesRoot = fs.getPath("modules");
//            return Files.list(modulesRoot);
//        }
    }

    private static class ControlFlowGraphGenerator extends JavaIsoVisitor<JavaControlFlowGraph> {
        List<Statement> basicBlockStatements = new ArrayList<>();

        public JavaControlFlowGraph buildLocal(Statement statement) {
            JavaControlFlowGraph cfg = new JavaControlFlowGraph(new BasicBlock<>(singletonList(statement)),
                    emptyList(), emptyList());
            return cfg;
        }

        @Override
        public J.If visitIf(J.If iff, JavaControlFlowGraph cfg) {
            cfg.getBasicBlock().unsafeSetStatements(basicBlockStatements);
            basicBlockStatements = new ArrayList<>();
            JavaControlFlowGraph header = new JavaControlFlowGraph(new BasicBlock<>(singletonList(iff)),
                    emptyList(), emptyList());
            cfg = header;

            visit(iff.getThenPart(), cfg);

            visit(iff.getElsePart(), cfg);

            cfg.unsafeSetChildren(singletonList(header));
            return iff;
        }

        @Override
        public Statement visitStatement(Statement statement, JavaControlFlowGraph cfg) {
            basicBlockStatements.add(statement);
            return statement;
        }
    }
}
