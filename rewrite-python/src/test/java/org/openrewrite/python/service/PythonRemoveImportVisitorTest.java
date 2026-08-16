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
package org.openrewrite.python.service;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.python.tree.Py;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.service.PythonImports.*;

class PythonRemoveImportVisitorTest {

    private static boolean mightChange(String module, String name, Py.CompilationUnit cu) {
        return new PythonRemoveImportVisitor<>(module, name).mightChange(cu);
    }

    @Test
    void skipsAFileWithNoImports() {
        assertThat(mightChange("typing", "List", cu())).isFalse();
    }

    @Test
    void skipsWhenNoImportBindsTheName() {
        assertThat(mightChange("typing", "List", cu(fromImport("typing", member("Iterable"))))).isFalse();
    }

    @Test
    void skipsWhenTheModuleDiffers() {
        assertThat(mightChange("typing", "List", cu(fromImport("collections.abc", member("List"))))).isFalse();
    }

    @Test
    void dispatchesForAMatchingFromImport() {
        assertThat(mightChange("typing", "List", cu(fromImport("typing", member("List"))))).isTrue();
    }

    @Test
    void dispatchesForAMatchingMemberAmongOthers() {
        assertThat(mightChange("typing", "List",
                cu(fromImport("typing", member("Iterable"), member("List"))))).isTrue();
    }

    @Test
    void dispatchesForACanonicalMatch() {
        assertThat(mightChange("posixpath", "join",
                cu(fromImport("os.path", member("join", null, methodType("posixpath", "join")))))).isTrue();
    }

    @Test
    void skipsWhenTheCanonicalNameDiffers() {
        assertThat(mightChange("posixpath", "exists",
                cu(fromImport("os.path", member("exists", null, methodType("genericpath", "exists")))))).isFalse();
    }

    @Test
    void dispatchesForAWholeModuleDirectImport() {
        assertThat(mightChange("os", null, cu(directImport(member("os"))))).isTrue();
    }

    @Test
    void skipsAWholeModuleRequestAgainstADifferentDirectImport() {
        assertThat(mightChange("os", null, cu(directImport(member("sys"))))).isFalse();
    }

    @Test
    void dispatchesForAWholeModuleRequestAgainstAFromImport() {
        assertThat(mightChange("typing", null, cu(fromImport("typing", member("List"))))).isTrue();
    }

    @Test
    void skipsANamedRequestAgainstADirectImport() {
        assertThat(mightChange("os", "path", cu(directImport(member("os"))))).isFalse();
    }
}
