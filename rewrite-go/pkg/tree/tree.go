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

import "github.com/google/uuid"

// Tree is the root interface for all LST nodes.
type Tree interface{ isTree() }

// J is the interface for all Java-like AST nodes that carry a prefix
// space. Mirrors org.openrewrite.java.tree.J — `getId`, `getPrefix`,
// `getMarkers` are polymorphic accessors so RPC senders, receivers,
// and other framework code can read the cross-cutting fields without
// per-type switches.
//
// Concrete impls live in j_methods.go (keep in sync with the J types
// in j.go and go.go).
//
// Mutation: the J interface is read-only. To produce a modified node,
// use the typed `WithPrefix(Space) *T` / `WithMarkers(Markers) *T`
// per-type methods — they return a *new* instance so the visitor
// framework's change-detection (pointer identity) works correctly.
// Framework code that needs to invoke them polymorphically (e.g. the
// RPC receiver's PreVisit) goes through reflection — see the
// `withPrefixViaReflection` helper in pkg/rpc.
//
// In-place mutation of Prefix / Markers / ID is never safe because it
// would silently bypass RecipeScheduler's "did this recipe change the
// tree?" check.
type J interface {
	Tree
	isJ()
	GetID() uuid.UUID
	GetPrefix() Space
	GetMarkers() Markers
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
