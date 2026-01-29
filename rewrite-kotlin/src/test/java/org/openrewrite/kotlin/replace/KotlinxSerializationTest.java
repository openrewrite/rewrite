/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin.replace;

import org.junit.jupiter.api.Test;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.kotlin.Assertions.kotlin;

class KotlinxSerializationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResources("org.jetbrains.kotlinx.ReplaceDeprecatedKotlinxSerializationCore1Methods")
          .parser(KotlinParser.builder().classpath("kotlinx-serialization-core-jvm"))
          .afterTypeValidationOptions(TypeValidation.all().methodInvocations(false));
    }

    @Test
    void replaceDefaultWithDefaultDeserializer() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.serialization.modules.PolymorphicModuleBuilder
              
              fun <T : Any> configure(builder: PolymorphicModuleBuilder<T>) {
                  builder.default { className -> null }
              }
              """,
            // TODO: KotlinTemplate adds empty () before trailing lambda - cosmetic issue
            """
              import kotlinx.serialization.modules.PolymorphicModuleBuilder
              
              fun <T : Any> configure(builder: PolymorphicModuleBuilder<T>) {
                  builder.defaultDeserializer(){ className -> null }
              }
              """
          )
        );
    }

    @Test
    void replacePolymorphicDefaultWithPolymorphicDefaultDeserializer() {
        rewriteRun(
          kotlin(
            """
              import kotlinx.serialization.modules.SerializersModuleBuilder
              
              fun configure(builder: SerializersModuleBuilder) {
                  builder.polymorphicDefault(Any::class) { className -> null }
              }
              """,
            """
              import kotlinx.serialization.modules.SerializersModuleBuilder
              
              fun configure(builder: SerializersModuleBuilder) {
                  builder.polymorphicDefaultDeserializer(Any::class) { className -> null }
              }
              """
          )
        );
    }
}
