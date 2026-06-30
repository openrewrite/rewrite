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

package installer

import (
	"errors"
	"os"
	"path/filepath"
	"testing"
)

func TestIsProxyFetchError(t *testing.T) {
	for _, tc := range []struct {
		name string
		err  error
		want bool
	}{
		{"nil", nil, false},
		{
			"proxy 403 reading zip",
			errors.New("get github.com/moderneinc/recipes-go@v0.4.0: reading https://proxy.golang.org/github.com/moderneinc/recipes-go/@v/v0.4.0.zip: 403 Forbidden"),
			true,
		},
		{
			"proxy 410 gone",
			errors.New("reading https://proxy.golang.org/github.com/foo/bar/@v/v1.0.0.info: 410 Gone"),
			true,
		},
		{
			"unrelated compile error",
			errors.New("build helper: ./main.go:5: undefined: recipes.Activate"),
			false,
		},
		{
			"genuine not-found should not be masked by proxy mention alone",
			errors.New("go: github.com/foo/bar@v9.9.9: invalid version: unknown revision"),
			false,
		},
	} {
		// when
		got := isProxyFetchError(tc.err)

		// then
		if got != tc.want {
			t.Errorf("isProxyFetchError(%q) = %v, want %v", tc.err, got, tc.want)
		}
	}
}

func writeGoMod(t *testing.T, contents string) string {
	t.Helper()
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "go.mod"), []byte(contents), 0o644); err != nil {
		t.Fatalf("write go.mod: %v", err)
	}
	return dir
}

func TestHelperBinaryPath(t *testing.T) {
	for _, tc := range []struct{ goos, want string }{
		{"windows", filepath.Join("ws", "helper") + ".exe"},
		{"linux", filepath.Join("ws", "helper")},
		{"darwin", filepath.Join("ws", "helper")},
	} {
		if got := helperBinaryPath("ws", tc.goos); got != tc.want {
			t.Errorf("helperBinaryPath(ws, %q) = %q, want %q", tc.goos, got, tc.want)
		}
	}
}

func TestReadResolvedVersion_DirectRequireInBlock(t *testing.T) {
	// given
	goMod := `module example.com/workspace

go 1.25

require (
	github.com/foo/bar v1.2.3
)
	`
	inst := &Installer{WorkspaceDir: writeGoMod(t, goMod)}

	// when
	got := inst.readResolvedVersion("github.com/foo/bar")

	// then
	if got != "v1.2.3" {
		t.Errorf("expected v1.2.3, got %q", got)
	}
}

func TestReadResolvedVersion_IndirectRequireInBlock(t *testing.T) {
	// given
	goMod := `module example.com/workspace

go 1.25

require (
	github.com/foo/bar v1.2.3 // indirect
)
	`
	inst := &Installer{WorkspaceDir: writeGoMod(t, goMod)}

	// when
	got := inst.readResolvedVersion("github.com/foo/bar")

	// then
	if got != "v1.2.3" {
		t.Errorf("expected v1.2.3, got %q", got)
	}
}

func TestReadResolvedVersion_SingleLineRequire(t *testing.T) {
	// given
	goMod := `module example.com/workspace

go 1.25

require github.com/foo/bar v1.2.3
	`
	inst := &Installer{WorkspaceDir: writeGoMod(t, goMod)}

	// when
	got := inst.readResolvedVersion("github.com/foo/bar")

	// then
	if got != "v1.2.3" {
		t.Errorf("expected v1.2.3, got %q", got)
	}
}

func TestReadResolvedVersion_PrefixCollision(t *testing.T) {
	// given - looking up github.com/foo/bar must not match github.com/foo/barbaz
	goMod := `module example.com/workspace

go 1.25

require (
	github.com/foo/barbaz v0.9.9
	github.com/foo/bar v1.2.3
)
	`
	inst := &Installer{WorkspaceDir: writeGoMod(t, goMod)}

	// when
	got := inst.readResolvedVersion("github.com/foo/bar")

	// then
	if got != "v1.2.3" {
		t.Errorf("expected v1.2.3 (the exact match, not the prefix-collision sibling), got %q", got)
	}
}
