/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.rpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.python.PythonIsoVisitor;
import org.openrewrite.python.tree.Py;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "No remote client/server available")
class PythonRewriteRpcParseTest {

    /**
     * Parse an explicit list of two files where {@code derived.py} imports a class from
     * {@code base.py}, with {@code relativeTo} pointing at their common root so that
     * {@code ty} resolves the first-party import. Both must come back as attributed
     * {@link Py.CompilationUnit}s (not {@code ParseError}), and the cross-file supertype
     * must resolve — i.e. {@code self} inside {@code Derived} is assignable to {@code Base}.
     */
    @Test
    void parseExplicitFilesWithCrossFileTypeResolution(@TempDir Path root) throws Exception {
        Path base = root.resolve("base.py");
        Path derived = root.resolve("derived.py");
        Files.writeString(base,
          """
            class Base:
                def hello(self):
                    return 1
            """);
        Files.writeString(derived,
          """
            from base import Base


            class Derived(Base):
                def go(self):
                    return self.hello()
            """);

        ExecutionContext ctx = new InMemoryExecutionContext();
        List<SourceFile> parsed = PythonRewriteRpc.getOrStart()
          .parse(List.of(base, derived), root, null, ctx)
          .collect(toList());

        assertThat(parsed).hasSize(2);
        assertThat(parsed).allSatisfy(sf -> assertThat(sf).isInstanceOf(Py.CompilationUnit.class));

        Py.CompilationUnit derivedCu = parsed.stream()
          .filter(sf -> sf.getSourcePath().getFileName().toString().equals("derived.py"))
          .map(Py.CompilationUnit.class::cast)
          .findFirst()
          .orElseThrow();

        List<JavaType> selfTypes = new ArrayList<>();
        new PythonIsoVisitor<Integer>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                if ("self".equals(identifier.getSimpleName()) && identifier.getType() != null) {
                    selfTypes.add(identifier.getType());
                }
                return super.visitIdentifier(identifier, p);
            }
        }.visit(derivedCu, 0);

        assertThat(selfTypes)
          .as("`self` identifiers in Derived must carry a resolved type")
          .isNotEmpty();
        assertThat(selfTypes)
          .as("the `self` receiver's type must resolve as a subclass of the first-party `Base` "
              + "defined in the sibling file, proving `ty` was rooted at the common workspace root")
          .anySatisfy(t -> assertThat(hasSupertypeSimpleName(t, "Base")).isTrue());
    }

    /**
     * Walks the supertype/interface graph of {@code type} looking for a class whose
     * simple name (last dot-separated segment) equals {@code simpleName}, unwrapping
     * generic type variables (e.g. the {@code Self} bound on a {@code self} receiver)
     * along the way. This avoids depending on the exact module qualification {@code ty}
     * assigns to first-party types.
     */
    private static boolean hasSupertypeSimpleName(JavaType type, String simpleName) {
        Set<JavaType> seen = new HashSet<>();
        List<JavaType> queue = new ArrayList<>();
        queue.add(type);
        while (!queue.isEmpty()) {
            JavaType t = queue.remove(queue.size() - 1);
            if (!seen.add(t)) {
                continue;
            }
            if (t instanceof JavaType.GenericTypeVariable) {
                queue.addAll(((JavaType.GenericTypeVariable) t).getBounds());
            } else if (t instanceof JavaType.FullyQualified) {
                JavaType.FullyQualified fq = (JavaType.FullyQualified) t;
                String fqn = fq.getFullyQualifiedName();
                String lastSegment = fqn.substring(fqn.lastIndexOf('.') + 1);
                if (simpleName.equals(lastSegment)) {
                    return true;
                }
                if (fq.getSupertype() != null) {
                    queue.add(fq.getSupertype());
                }
                queue.addAll(fq.getInterfaces());
            }
        }
        return false;
    }
}
