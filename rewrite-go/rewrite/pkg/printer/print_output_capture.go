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

package printer

import (
	"strings"

	"github.com/openrewrite/rewrite/pkg/tree"
)

// PrintOutputCapture accumulates printed source code text.
type PrintOutputCapture struct {
	buf           strings.Builder
	markerPrinter MarkerPrinter
}

// NewPrintOutputCapture creates a PrintOutputCapture with no marker printer.
func NewPrintOutputCapture() *PrintOutputCapture {
	return &PrintOutputCapture{}
}

// NewPrintOutputCaptureWithMarkers creates a PrintOutputCapture with the given MarkerPrinter.
func NewPrintOutputCaptureWithMarkers(mp MarkerPrinter) *PrintOutputCapture {
	return &PrintOutputCapture{markerPrinter: mp}
}

func (p *PrintOutputCapture) Append(s string) {
	p.buf.WriteString(s)
}

func (p *PrintOutputCapture) String() string {
	return p.buf.String()
}

// BeforePrefix emits marker output before a node's prefix space.
func (p *PrintOutputCapture) BeforePrefix(markers tree.Markers) {
	if p.markerPrinter == nil {
		return
	}
	for _, m := range markers.Entries {
		if s := p.markerPrinter.BeforePrefix(m, GoCommentWrapper); s != "" {
			p.buf.WriteString(s)
		}
	}
}

// BeforeSyntax emits marker output after a node's prefix but before its syntax.
func (p *PrintOutputCapture) BeforeSyntax(markers tree.Markers) {
	if p.markerPrinter == nil {
		return
	}
	for _, m := range markers.Entries {
		if s := p.markerPrinter.BeforeSyntax(m, GoCommentWrapper); s != "" {
			p.buf.WriteString(s)
		}
	}
}

// AfterSyntax emits marker output after a node's syntax.
func (p *PrintOutputCapture) AfterSyntax(markers tree.Markers) {
	if p.markerPrinter == nil {
		return
	}
	for _, m := range markers.Entries {
		if s := p.markerPrinter.AfterSyntax(m, GoCommentWrapper); s != "" {
			p.buf.WriteString(s)
		}
	}
}
