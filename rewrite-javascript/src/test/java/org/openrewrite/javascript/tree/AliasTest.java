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
package org.openrewrite.javascript.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class AliasTest {

    @Test
    void typeOfStringLiteralModuleExportNameIsTheBoundNamesType() {
        // given
        JavaType.Class linkType = JavaType.ShallowClass.build("styles.LinkA");
        JS.Alias alias = new JS.Alias(randomId(), Space.EMPTY, Markers.EMPTY,
          JRightPadded.build(new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "link-a", "\"link-a\"", null, JavaType.Primitive.String)),
          new J.Identifier(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, emptyList(), "linkA", linkType, null));

        // when
        JavaType type = alias.getType();

        // then
        assertThat(type).isSameAs(linkType);
    }

    @Test
    void withTypeOnStringLiteralModuleExportNameRetypesTheBoundName() {
        // given
        JavaType.Class linkType = JavaType.ShallowClass.build("styles.LinkB");
        JS.Alias alias = new JS.Alias(randomId(), Space.EMPTY, Markers.EMPTY,
          JRightPadded.build(new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "link-b", "\"link-b\"", null, JavaType.Primitive.String)),
          new J.Identifier(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, emptyList(), "linkB", null, null));

        // when
        JS.Alias retyped = alias.withType(linkType);

        // then
        assertThat(retyped.getType()).isSameAs(linkType);
        assertThat(retyped.getPropertyName().getType()).isEqualTo(JavaType.Primitive.String);
    }
}
