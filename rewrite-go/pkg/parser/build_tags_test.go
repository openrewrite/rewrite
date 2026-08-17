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

package parser_test

import (
	"go/build"
	"os"
	"path/filepath"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
)

// gcContext pins GOOS/GOARCH so that on any host the cases below turn on
// Compiler alone.
func gcContext() build.Context {
	ctx := build.Default
	ctx.GOOS = "linux"
	ctx.GOARCH = "amd64"
	ctx.Compiler = "gc"
	return ctx
}

func TestMatchBuildContextCompilerTag(t *testing.T) {
	gccgo := gcContext()
	gccgo.Compiler = "gccgo"

	tests := []struct {
		name    string
		ctx     build.Context
		content string
		want    bool
	}{
		{
			name:    "gc constraint under gc compiler",
			ctx:     gcContext(),
			content: "//go:build gc\n\npackage build\n",
			want:    true,
		},
		{
			name:    "gccgo constraint under gc compiler",
			ctx:     gcContext(),
			content: "//go:build gccgo\n\npackage build\n",
			want:    false,
		},
		{
			name:    "gccgo constraint under gccgo compiler",
			ctx:     gccgo,
			content: "//go:build gccgo\n\npackage build\n",
			want:    true,
		},
		{
			name:    "gc constraint under gccgo compiler",
			ctx:     gccgo,
			content: "//go:build gc\n\npackage build\n",
			want:    false,
		},
		{
			name:    "gc and not purego",
			ctx:     gcContext(),
			content: "//go:build gc && !purego\n\npackage chacha20\n",
			want:    true,
		},
		{
			name:    "linux and gc",
			ctx:     gcContext(),
			content: "//go:build linux && gc\n\npackage issue9400\n",
			want:    true,
		},
		{
			name:    "legacy plus-build gc",
			ctx:     gcContext(),
			content: "// +build gc\n\npackage build\n",
			want:    true,
		},
		{
			name:    "zero-value context does not satisfy gc",
			ctx:     build.Context{},
			content: "//go:build gc\n\npackage build\n",
			want:    false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := parser.MatchBuildContext(tt.ctx, "constrained.go", tt.content); got != tt.want {
				t.Errorf("MatchBuildContext() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestMatchBuildContextAgreesWithGoBuild(t *testing.T) {
	ctx := gcContext()
	for _, content := range []string{
		"//go:build gc\n\npackage p\n",
		"//go:build gccgo\n\npackage p\n",
		"//go:build gc && !purego\n\npackage p\n",
		"// +build gc\n\npackage p\n",
	} {
		dir := t.TempDir()
		const name = "constrained.go"
		if err := os.WriteFile(filepath.Join(dir, name), []byte(content), 0o600); err != nil {
			t.Fatalf("write fixture: %v", err)
		}
		want, err := ctx.MatchFile(dir, name)
		if err != nil {
			t.Fatalf("MatchFile: %v", err)
		}
		if got := parser.MatchBuildContext(ctx, name, content); got != want {
			t.Errorf("MatchBuildContext(%q) = %v, go/build says %v", content, got, want)
		}
	}
}
