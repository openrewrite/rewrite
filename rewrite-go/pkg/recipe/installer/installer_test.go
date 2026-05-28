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
	"os"
	"path/filepath"
	"testing"
)

func writeGoMod(t *testing.T, contents string) string {
	t.Helper()
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "go.mod"), []byte(contents), 0o644); err != nil {
		t.Fatalf("write go.mod: %v", err)
	}
	return dir
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
