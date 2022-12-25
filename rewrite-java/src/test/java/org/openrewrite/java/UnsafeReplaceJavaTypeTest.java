/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

class UnsafeReplaceJavaTypeTest {

    @Test
    void unsafeReplace() {
        var t1t = TypeUtils.asClass(TypeTree.<J.FieldAccess>build("org.openrewrite.Test.Inner").getType());
        var t2t = TypeUtils.asClass(TypeTree.<J.FieldAccess>build("org.openrewrite.Test.Inner").getType());
        assertThat(t1t).isNotNull();
        assertThat(t2t).isNotNull();

        var replaced = new UnsafeReplaceJavaType(t1t.getOwningClass(), t2t.getOwningClass()).visit(t1t, 0);
        assertThat(requireNonNull(TypeUtils.asClass(replaced)).getOwningClass()).isEqualTo(t2t.getOwningClass());
    }
}
