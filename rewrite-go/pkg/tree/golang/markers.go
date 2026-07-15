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

package golang

import "github.com/google/uuid"

// GoProject identifies the Go project (logical grouping of go.mod + .go
// files) a source belongs to. Mirrors org.openrewrite.golang.marker.GoProject
// on the Java side.
type GoProject struct {
	Ident       uuid.UUID
	ProjectName string
	ModulePath  string
}

func (m GoProject) ID() uuid.UUID { return m.Ident }

// Semicolon marks a RightPadded element that is followed by an explicit
// `;` separator in the source — i.e. multiple statements on one line:
// `_ = 1; _ = 2`. Go inserts implicit semicolons at end-of-line so most
// files don't need this marker; it's only emitted when the source
// literally has a `;` between statements that the printer must
// reproduce.
//
// Mirrors org.openrewrite.java.marker.Semicolon on the Java side.
type Semicolon struct {
	Ident uuid.UUID
}

func (m Semicolon) ID() uuid.UUID { return m.Ident }

func NewSemicolon() Semicolon {
	return Semicolon{Ident: uuid.New()}
}

func NewGoProject(projectName, modulePath string) GoProject {
	return GoProject{Ident: uuid.New(), ProjectName: projectName, ModulePath: modulePath}
}
