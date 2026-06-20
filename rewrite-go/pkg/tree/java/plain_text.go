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

package java

import "github.com/google/uuid"

// PlainText mirrors org.openrewrite.text.PlainText — the LST fallback for a
// file no language parser claimed. The Go side only ever *receives* one (the
// Moderne CLI's Go build step backfills a PlainText for any `.go` file the
// parser doesn't return, e.g. a file excluded by the host build context), so
// this type carries just enough to read its source: a Go recipe that wants
// the imports of build-excluded files scans Text. It is never produced or
// sent by the Go side.
//
// Fields not needed Go-side (checksum, fileAttributes, snippets) are consumed
// and discarded by the receive codec rather than modeled here.
type PlainText struct {
	Ident            uuid.UUID
	Markers          Markers
	SourcePath       string
	CharsetName      string
	CharsetBomMarked bool
	Text             string
}

// PlainText flows through the same Tree-typed Visit pipeline as a SourceFile
// alternate. Like ParseError it isn't a J node; RPC receivers and visitors
// special-case it ahead of the J dispatch.
func (*PlainText) IsTree() {}
