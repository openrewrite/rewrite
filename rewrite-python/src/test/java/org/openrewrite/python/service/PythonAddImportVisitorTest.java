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
import org.openrewrite.python.tree.Py;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.service.PythonImports.*;

class PythonAddImportVisitorTest {

    private static boolean mightChange(String module, String name, String alias,
                                       boolean onlyIfReferenced, Py.CompilationUnit cu) {
        return new PythonAddImportVisitor<>(module, name, alias, onlyIfReferenced).mightChange(cu);
    }

    @Test
    void skipsBuiltinsWithoutAnAlias() {
        assertThat(mightChange("builtins", "list", null, false, cu())).isFalse();
    }

    @Test
    void dispatchesForAnAliasedBuiltinsImport() {
        assertThat(mightChange("builtins", "list", "_list", false, cu())).isTrue();
    }

    @Test
    void skipsABareBuiltinName() {
        assertThat(mightChange("list", null, null, false, cu())).isFalse();
    }

    @Test
    void dispatchesForADottedModuleThatStartsWithABuiltinName() {
        assertThat(mightChange("list.sub", null, null, false, cu())).isTrue();
    }

    @Test
    void skipsWhenTheFromImportAlreadyExists() {
        assertThat(mightChange("typing", "List", null, false,
          cu(fromImport("typing", member("List"))))).isFalse();
    }

    @Test
    void dispatchesWhenOnlyAnotherMemberExists() {
        assertThat(mightChange("typing", "List", null, false,
          cu(fromImport("typing", member("Iterable"))))).isTrue();
    }

    @Test
    void dispatchesWhenTheExistingImportIsAliased() {
        assertThat(mightChange("typing", "List", null, false,
          cu(fromImport("typing", member("List", "L", null))))).isTrue();
    }

    @Test
    void skipsWhenTheDirectImportAlreadyExists() {
        assertThat(mightChange("os", null, null, false, cu(directImport(member("os"))))).isFalse();
    }

    @Test
    void skipsWhenAnExistingImportCanonicallyBindsTheSameName() {
        assertThat(mightChange("posixpath", "join", null, false,
          cu(fromImport("os.path", member("join", null, methodType("posixpath", "join")))))).isFalse();
    }

    @Test
    void dispatchesWhenTheCanonicalMatchBindsAnotherName() {
        assertThat(mightChange("posixpath", "join", null, false,
          cu(fromImport("os.path", member("join", "j", methodType("posixpath", "join")))))).isTrue();
    }

    @Test
    void skipsAnUnreferencedNameWhenOnlyIfReferenced() {
        assertThat(mightChange("os.path", "join", null, true, cu())).isFalse();
    }

    @Test
    void doesNotCountAnIdentifierBoundByAnImportAsAReference() {
        assertThat(mightChange("pathlib", null, null, true,
          cu(directImport(member("pathlib", "o", null))))).isFalse();
    }
}
