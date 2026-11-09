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
package org.openrewrite.ruby;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.PotentialCreator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ObjectMappers;
import org.openrewrite.ruby.tree.Rb;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A Ruby tree read back from a serialized LST prints as the source it was parsed from.
 */
class RubySerializationTest {

    private static final ObjectMapper MAPPER = lstMapper();

    @ParameterizedTest
    @ValueSource(strings = {
      "class Box < Base\n  def initialize(x, y = 1, *rest, k:, **opts)\n    @x = x.call(y)\n  end\nend\n",
      "def each(&block)\n  @items.each(&block)\nend\n",
      "def strict(&nil)\nend\n",
      "def visit(node)\n  yield node\nend\n",
      "def pairs\n  yield [1, 2], :a, \"#{x}\", *rest, k: 1\nend\n",
      "def chained\n  yield(a + b)\nend\n"
    })
    void survivesSerialization(String source) throws Exception {
        SourceFile parsed = RubyParser.builder().build().parse(source).findFirst().orElseThrow();
        assertThat(parsed).isInstanceOf(Rb.CompilationUnit.class);

        SourceFile read = MAPPER.readValue(MAPPER.writeValueAsBytes(parsed), SourceFile.class);

        assertThat(read.printAll()).isEqualTo(source);
    }

    /**
     * Writes a tree field by field and reads it through its widest public constructor, as an LST is written and read.
     */
    private static ObjectMapper lstMapper() {
        ObjectMapper mapper = ObjectMappers.propertyBasedMapper(null);
        mapper.registerModule(new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.insertAnnotationIntrospector(new NopAnnotationIntrospector() {
                    @Override
                    public PotentialCreator findDefaultCreator(MapperConfig<?> config, AnnotatedClass valueClass,
                                                               List<PotentialCreator> declaredConstructors,
                                                               List<PotentialCreator> declaredFactories) {
                        PotentialCreator widest = null;
                        int count = 0;
                        for (PotentialCreator creator : declaredConstructors) {
                            Constructor<?> constructor = (Constructor<?>) creator.creator().getAnnotated();
                            if (Modifier.isPublic(constructor.getModifiers()) && constructor.getParameterCount() > count &&
                                constructor.getParameters()[0].isNamePresent()) {
                                widest = creator;
                                count = constructor.getParameterCount();
                            }
                        }
                        return widest;
                    }
                });
            }
        });
        return mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
          .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
          .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
          .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
          .withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    }
}
