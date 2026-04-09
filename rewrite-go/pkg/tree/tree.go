/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://docs.moderne.io/licensing/moderne-source-available-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tree

// Tree is the root interface for all LST nodes.
type Tree interface{ isTree() }

// J is the interface for all Java-like AST nodes that carry a prefix space.
type J interface {
	Tree
	isJ()
}

// Expression is a J node that evaluates to a value.
type Expression interface {
	J
	isExpression()
}

// Statement is a J node that can appear at statement level.
type Statement interface {
	J
	isStatement()
}

// SourceFile is a J node representing an entire source file.
type SourceFile interface {
	J
	isSourceFile()
}
