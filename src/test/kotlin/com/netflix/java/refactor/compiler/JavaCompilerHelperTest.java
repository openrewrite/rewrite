/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.java.refactor.compiler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JavaCompilerHelperTest {

	@Test
	public void compile() throws Exception {
		JavaCompilerHelper java = new JavaCompilerHelper();
		java.compile("public class A {}");
		java.compile("public class B { A a = new A(); }");
	}

	@Test
	public void fullyQualifiedName() throws Exception {
		assertEquals("myorg.a.A", JavaCompilerHelper.fullyQualifiedName("package myorg.a; public class A {}"));
		assertEquals("myorg.a.A", JavaCompilerHelper.fullyQualifiedName("package myorg.a; public interface A {}"));
	}
}