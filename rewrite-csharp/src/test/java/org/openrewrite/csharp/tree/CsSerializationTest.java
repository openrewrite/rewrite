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
package org.openrewrite.csharp.tree;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;
import org.openrewrite.csharp.CSharpPrinter;
import org.openrewrite.csharp.CSharpVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

/**
 * Trees serialized before a field was added or removed still have to load, and to print as
 * they did.
 */
class CsSerializationTest {

    private final ObjectMapper mapper = newMapper();

    private static ObjectMapper newMapper() {
        ObjectMapper m = JsonMapper.builder()
          .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
          .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
          .build()
          .registerModule(new ParameterNamesModule())
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
          .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
          .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
          .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
          .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    }

    @Test
    void usingDirectiveSerializedBeforeUnsafe() throws Exception {
        Cs.UsingDirective using = new Cs.UsingDirective(randomId(), Space.EMPTY, Markers.EMPTY,
          JRightPadded.build(false),
          JLeftPadded.build(false),
          JLeftPadded.build(true).withBefore(Space.SINGLE_SPACE),
          JRightPadded.build(identifier("Ptr").withPrefix(Space.SINGLE_SPACE)).withAfter(Space.SINGLE_SPACE),
          identifier("Target").withPrefix(Space.SINGLE_SPACE));

        ObjectNode json = mapper.valueToTree(using);
        assertThat(print(mapper.treeToValue(json, Cs.UsingDirective.class))).isEqualTo("using unsafe Ptr = Target");

        json.remove("unsafe");
        Cs.UsingDirective old = mapper.treeToValue(json, Cs.UsingDirective.class);
        assertThat(old.isUnsafe()).isFalse();
        assertThat(print(old)).isEqualTo("using Ptr = Target");
        assertThat(new CSharpVisitor<Integer>().visit(old, 0)).isSameAs(old);
    }

    @Test
    void interpolationSerializedBeforeSpaceBeforeAlignmentAndFormat() throws Exception {
        Cs.Interpolation interpolation = new Cs.Interpolation(randomId(), Space.EMPTY, Markers.EMPTY,
          JRightPadded.<Expression>build(identifier("x")),
          Space.SINGLE_SPACE,
          JRightPadded.<Expression>build(identifier("width")),
          Space.SINGLE_SPACE,
          JRightPadded.<Expression>build(identifier("F2")));

        ObjectNode json = mapper.valueToTree(interpolation);
        assertThat(print(mapper.treeToValue(json, Cs.Interpolation.class))).isEqualTo("{x ,width :F2}");

        json.remove("alignmentBefore");
        json.remove("formatBefore");
        Cs.Interpolation old = mapper.treeToValue(json, Cs.Interpolation.class);
        assertThat(old.getAlignmentBefore()).isSameAs(Space.EMPTY);
        assertThat(old.getFormatBefore()).isSameAs(Space.EMPTY);
        assertThat(print(old)).isEqualTo("{x,width:F2}");
        assertThat(new CSharpVisitor<Integer>().visit(old, 0)).isSameAs(old);
    }

    @Test
    void expressionStatementSerializedWithAPrefixAndMarkersOfItsOwn() throws Exception {
        Cs.ExpressionStatement statement = new Cs.ExpressionStatement(randomId(),
          JRightPadded.build(identifier("run").withPrefix(Space.SINGLE_SPACE)));

        // these were copies of the expression's, and never printed
        ObjectNode json = mapper.valueToTree(statement);
        json.set("prefix", mapper.valueToTree(Space.SINGLE_SPACE));
        json.set("markers", mapper.valueToTree(Markers.build(emptyList())));

        Cs.ExpressionStatement old = mapper.treeToValue(json, Cs.ExpressionStatement.class);
        assertThat(print(old)).isEqualTo(" run");
        assertThat(old.getPrefix()).isEqualTo(Space.SINGLE_SPACE);
        assertThat(print(old.withPrefix(Space.EMPTY))).isEqualTo("run");
    }

    private static J.Identifier identifier(String name) {
        return new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), name, null, null);
    }

    private static String print(J tree) {
        return tree.print(new CSharpPrinter<>());
    }
}
