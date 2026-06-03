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
package org.openrewrite.java.internal.rpc;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;

import static org.assertj.core.api.Assertions.assertThat;

class JavaTypeRpcCodecTest {

    /**
     * Stand-in for a runtime-generated proxy: a {@code JavaType} subtype living outside the
     * canonical {@code org.openrewrite.java.tree} package (this test class's package is foreign).
     */
    static class LazyShallowClass extends JavaType.ShallowClass {
        LazyShallowClass() {
            super(null, 0, "java.lang.String", JavaType.FullyQualified.Kind.Class,
                    null, null, null, null, null, null, null);
        }
    }

    @Test
    void codecResolvesForJavaTypeValues() {
        JavaType type = JavaType.ShallowClass.build("java.lang.String");
        // JavaType is cross-language and is never the enclosing source file type, so it resolves
        // through the assignability fallback rather than the source-file-keyed bucket.
        assertThat(DynamicDispatchRpcCodec.getCodec(type, J.CompilationUnit.class.getName()))
                .isInstanceOf(JavaTypeRpcCodec.class);
    }

    @Test
    void valueTypeDoesNotCollapseRealSubtypes() {
        // JavaType is a real class hierarchy (ShallowClass -> Class -> FullyQualified). The
        // canonicalization must NOT walk up it (that would lose the concrete type); a value
        // already in the canonical package is returned unchanged.
        assertThat(new JavaTypeRpcCodec().valueType(JavaType.ShallowClass.build("java.lang.String")))
                .isEqualTo("org.openrewrite.java.tree.JavaType$ShallowClass");
    }

    @Test
    void valueTypeStripsSyntheticSubclassLayer() {
        // A proxy/subclass outside the canonical package canonicalizes back to the real type.
        assertThat(new JavaTypeRpcCodec().valueType(new LazyShallowClass()))
                .isEqualTo("org.openrewrite.java.tree.JavaType$ShallowClass");
    }
}
