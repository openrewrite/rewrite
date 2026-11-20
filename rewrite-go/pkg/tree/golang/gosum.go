/*
 * Copyright 2026 the original author or authors.
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

import (
	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

type GoSum struct {
	Ident            uuid.UUID
	Prefix           java.Space
	Markers          java.Markers
	SourcePath       string
	Charset          string
	CharsetBomMarked bool
	Lines            []java.RightPadded[*GoSumLine]
	Eof              java.Space
}

func (*GoSum) IsTree()       {}
func (*GoSum) IsSourceFile() {}

func (n *GoSum) GetSourcePath() string { return n.SourcePath }

func (n *GoSum) WithPrefix(prefix java.Space) *GoSum {
	if java.SpaceEqual(n.Prefix, prefix) {
		return n
	}
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *GoSum) WithMarkers(markers java.Markers) *GoSum {
	if java.MarkersEqual(n.Markers, markers) {
		return n
	}
	c := *n
	c.Markers = markers
	return &c
}

func (n *GoSum) WithLines(lines []java.RightPadded[*GoSumLine]) *GoSum {
	if java.SameSlice(n.Lines, lines) {
		return n
	}
	c := *n
	c.Lines = lines
	return &c
}

func (n *GoSum) WithEof(eof java.Space) *GoSum {
	if java.SpaceEqual(n.Eof, eof) {
		return n
	}
	c := *n
	c.Eof = eof
	return &c
}

// GoSumLine is one go.sum entry: `module version[/go.mod] h1:hash`.
type GoSumLine struct {
	Ident      uuid.UUID
	Prefix     java.Space
	Markers    java.Markers
	ModulePath string
	Version    string
	GoMod      bool
	Hash       string
}

func (*GoSumLine) IsTree() {}

func (n *GoSumLine) WithPrefix(prefix java.Space) *GoSumLine {
	if java.SpaceEqual(n.Prefix, prefix) {
		return n
	}
	c := *n
	c.Prefix = prefix
	return &c
}

func (n *GoSumLine) WithMarkers(markers java.Markers) *GoSumLine {
	if java.MarkersEqual(n.Markers, markers) {
		return n
	}
	c := *n
	c.Markers = markers
	return &c
}

func (n *GoSumLine) WithModulePath(modulePath string) *GoSumLine {
	if n.ModulePath == modulePath {
		return n
	}
	c := *n
	c.ModulePath = modulePath
	return &c
}

func (n *GoSumLine) WithVersion(version string) *GoSumLine {
	if n.Version == version {
		return n
	}
	c := *n
	c.Version = version
	return &c
}

func (n *GoSumLine) WithGoMod(goMod bool) *GoSumLine {
	if n.GoMod == goMod {
		return n
	}
	c := *n
	c.GoMod = goMod
	return &c
}

func (n *GoSumLine) WithHash(hash string) *GoSumLine {
	if n.Hash == hash {
		return n
	}
	c := *n
	c.Hash = hash
	return &c
}
