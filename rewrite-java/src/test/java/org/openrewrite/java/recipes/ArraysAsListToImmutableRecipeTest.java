package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ArraysAsListToImmutableRecipeTest implements RewriteTest {
	@Test
	void wrapArrayListToBeUnmodifiableWhenStrings() {
		rewriteRun(
				recipeSpec -> recipeSpec.recipe(new ArraysAsListToImmutableRecipe()),
				java(
						"""
								package my.test;
										
								import java.util.Arrays;
											
								class A
								{
								   public static final List<String> entries = Arrays.asList("A", "B");
								}
								""",
						"""
								package my.test;
											
								import java.util.Arrays;
								import java.util.Collections;
           								   
								class A
								{
								   public static final List<String> entries = Collections.unmodifiableList(Arrays.asList("A", "B"));
								}
								"""
				)
		);
	}

	@Test
	void wrapArrayListToBeUnmodifiableWhenNulls() {
		rewriteRun(
				recipeSpec -> recipeSpec.recipe(new ArraysAsListToImmutableRecipe()),
				java(
						"""
								package my.test;
										
								import java.util.Arrays;
											
								class A
								{
								   public static final List<String> entries = Arrays.asList("A", null);
								}
								""",
						"""
								package my.test;
											
								import java.util.Arrays;
								import java.util.Collections;
           								   
								class A
								{
								   public static final List<String> entries = Collections.unmodifiableList(Arrays.asList("A", null));
								}
								"""
				)
		);
	}


	@Test
	void wrapArrayListToBeUnmodifiableWhenIntegers() {
		rewriteRun(
				recipeSpec -> recipeSpec.recipe(new ArraysAsListToImmutableRecipe()),
				java(
						"""
								package my.test;
										
								import java.util.Arrays;
											
								class A
								{
								   public static final List<Integer> entries = Arrays.asList(1, 2);
								}
								""",
						"""
								package my.test;
											
								import java.util.Arrays;
								import java.util.Collections;
           								   
								class A
								{
								   public static final List<Integer> entries = Collections.unmodifiableList(Arrays.asList(1, 2));
								}
								"""
				)
		);
	}
}