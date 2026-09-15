/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.tree;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScopeTest {

    @Test
    void isInClasspathOf() {
        assertThat(Scope.Runtime.isInClasspathOf(Scope.Test)).isTrue();
        assertThat(Scope.Runtime.isInClasspathOf(Scope.Compile)).isTrue();
        assertThat(Scope.Test.isInClasspathOf(Scope.Compile)).isFalse();
    }

    @Test
    void isDirectlyIncludedIn() {
        // compile is available on the compile, runtime, and test classpaths
        assertThat(Scope.Compile.isDirectlyIncludedIn(Scope.Compile)).isTrue();
        assertThat(Scope.Compile.isDirectlyIncludedIn(Scope.Provided)).isFalse();
        assertThat(Scope.Compile.isDirectlyIncludedIn(Scope.Runtime)).isTrue();
        assertThat(Scope.Compile.isDirectlyIncludedIn(Scope.Test)).isTrue();

        // provided is available on the compile and test classpaths, but withheld from runtime
        assertThat(Scope.Provided.isDirectlyIncludedIn(Scope.Compile)).isTrue();
        assertThat(Scope.Provided.isDirectlyIncludedIn(Scope.Provided)).isTrue();
        assertThat(Scope.Provided.isDirectlyIncludedIn(Scope.Runtime)).isFalse();
        assertThat(Scope.Provided.isDirectlyIncludedIn(Scope.Test)).isTrue();

        // runtime is available at runtime and test, but not compile or provided
        assertThat(Scope.Runtime.isDirectlyIncludedIn(Scope.Compile)).isFalse();
        assertThat(Scope.Runtime.isDirectlyIncludedIn(Scope.Provided)).isFalse();
        assertThat(Scope.Runtime.isDirectlyIncludedIn(Scope.Runtime)).isTrue();
        assertThat(Scope.Runtime.isDirectlyIncludedIn(Scope.Test)).isTrue();

        // test is only available in its own bucket
        assertThat(Scope.Test.isDirectlyIncludedIn(Scope.Compile)).isFalse();
        assertThat(Scope.Test.isDirectlyIncludedIn(Scope.Provided)).isFalse();
        assertThat(Scope.Test.isDirectlyIncludedIn(Scope.Runtime)).isFalse();
        assertThat(Scope.Test.isDirectlyIncludedIn(Scope.Test)).isTrue();
    }
}
