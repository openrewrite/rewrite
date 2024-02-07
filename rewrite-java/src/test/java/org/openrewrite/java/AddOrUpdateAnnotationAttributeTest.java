/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.recipes;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.AddOrUpdateAnnotationAttribute;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;


class AddOrUpdateAnnotationAttributeTest implements RewriteTest
{
	@Override
	public void defaults(final RecipeSpec spec)
	{
		spec.recipe(new AddOrUpdateAnnotationAttribute(
			"java.lang.annotation.Repeatable",
			"value",
			"Foo3.class",
			null
		  ))
		  .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
	}

	@Test
	@DocumentExample
	void changeAnnotationValueFromClassToDifferentClass()
	{
		this.rewriteRun(
		  java(
			"""
							   import java.lang.annotation.Repeatable;
							   import java.lang.annotation.Retention;
							   import java.lang.annotation.RetentionPolicy;
									
				@Retention(RetentionPolicy.RUNTIME)
							   @interface Foo2
							   {
								   Foo[] value();
							   }
									
				@Retention(RetentionPolicy.RUNTIME)
							   @interface Foo3
							   {
								   Foo[] value();
							   }
									
				@Repeatable(Foo2.class)
				public @interface Foo {
					String bar();
				}
				""",
			"""
							   import java.lang.annotation.Repeatable;
							   import java.lang.annotation.Retention;
							   import java.lang.annotation.RetentionPolicy;
									
				@Retention(RetentionPolicy.RUNTIME)
							   @interface Foo2
							   {
								   Foo[] value();
							   }
									
				@Retention(RetentionPolicy.RUNTIME)
							   @interface Foo3
							   {
								   Foo[] value();
							   }
									
				@Repeatable(Foo3.class)
				public @interface Foo {
					String bar();
				}
				"""
		  )
		);
	}
}
