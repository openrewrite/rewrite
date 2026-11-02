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
package org.openrewrite.python.tree;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.python.Python3Only;
import org.openrewrite.python.PythonParser;
import org.openrewrite.python.PythonVisitor;
import org.openrewrite.python.tree.Py.FormattedString.Value.Conversion;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.Assertions.python;

@Python3Only
class FormattedStringTest implements RewriteTest {

    @Test
    void spaceAfterConversion() {
        rewriteRun(
          python(
            """
              a = f"{x!s  }" f"{y!r  :10.10}" f"{z = !a }"
              """
          )
        );
    }

    @Test
    void changeConversion() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new PythonVisitor<>() {
              @Override
              public J visitFormattedStringValue(Py.FormattedString.Value value, ExecutionContext ctx) {
                  if (!(value.getExpression() instanceof J.Identifier name)) {
                      return value;
                  }
                  return switch (name.getSimpleName()) {
                      case "x" -> value.withConversion(Conversion.REPR);
                      case "y" -> value.withConversion(null);
                      default -> value.withConversion(Conversion.STR);
                  };
              }
          })),
          python(
            """
              a = f"{x!s  }" f"{y!a }" f"{z}"
              """,
            """
              a = f"{x!r  }" f"{y}" f"{z!s}"
              """
          )
        );
    }

    @Test
    void conversionOfAnOlderLst() throws Exception {
        ObjectMapper mapper = lstMapper();
        Py.FormattedString.Value value = values("a = f\"{x!r  }\"\n").get(0);
        ObjectNode json = (ObjectNode) mapper.readTree(mapper.writeValueAsString(value));

        Py.FormattedString.Value read = mapper.treeToValue(json, Py.FormattedString.Value.class);
        assertThat(requireNonNull(read.getPadding().getConversion()).getAfter().getWhitespace()).isEqualTo("  ");

        // before the conversion kept the space after it, it was written as the bare constant
        json.put("conversion", "REPR");
        Py.FormattedString.Value older = mapper.treeToValue(json, Py.FormattedString.Value.class);
        assertThat(older.getConversion()).isEqualTo(Conversion.REPR);
        assertThat(requireNonNull(older.getPadding().getConversion()).getAfter()).isEqualTo(Space.EMPTY);

        json.remove("conversion");
        assertThat(mapper.treeToValue(json, Py.FormattedString.Value.class).getConversion()).isNull();
    }

    private static List<Py.FormattedString.Value> values(String source) {
        SourceFile cu = PythonParser.builder().build().parse(source).collect(toList()).get(0);
        List<Py.FormattedString.Value> values = new ArrayList<>();
        new PythonVisitor<Integer>() {
            @Override
            public J visitFormattedStringValue(Py.FormattedString.Value value, Integer p) {
                values.add(value);
                return super.visitFormattedStringValue(value, p);
            }
        }.visit(cu, 0);
        return values;
    }

    /**
     * Mirrors the mapper configuration that serializes LSTs.
     */
    private static ObjectMapper lstMapper() {
        ObjectMapper m = JsonMapper.builder()
          .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
          .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
          .disable(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES)
          .build();
        m.registerModule(new ParameterNamesModule());
        m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        m.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
          .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
          .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
          .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
          .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
        return m;
    }
}
