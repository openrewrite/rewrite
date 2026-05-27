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

package parser

import (
	"go/build"
	"go/build/constraint"
	"strings"
)

// MatchBuildContext returns true when (name, content) should be parsed
// under buildCtx — i.e. the filename suffix doesn't conflict with
// GOOS/GOARCH and any `//go:build` / `// +build` constraint lines
// evaluate to true. Mirrors build.Context.MatchFile for callers that
// have file content in-memory rather than on disk.
//
// The constraint evaluator recognizes:
//   - GOOS/GOARCH name tags (matches when ctx.GOOS == tag etc.)
//   - "cgo" if buildCtx.CgoEnabled is true
//   - language version tags (e.g. "go1.21") if at or below the configured
//     release; falls back to true to avoid spurious exclusions
//   - any tag listed in buildCtx.BuildTags or buildCtx.ToolTags
//   - the special "ignore" tag (always false — Go convention for
//     manually excluding a file)
//
// Unknown tags evaluate to false. This matches build.Context's behavior
// for tags it doesn't recognize.
func MatchBuildContext(buildCtx build.Context, name, content string) bool {
	if !matchOSArchFilename(buildCtx, name) {
		return false
	}
	for _, line := range buildConstraintLines(content) {
		expr, err := constraint.Parse(line)
		if err != nil {
			// Malformed constraint — skip the line rather than the whole
			// file. Mirrors `build` package's tolerance for legacy tags.
			continue
		}
		if !expr.Eval(func(tag string) bool { return matchTag(buildCtx, tag) }) {
			return false
		}
	}
	return true
}

// matchOSArchFilename implements Go's filename build constraints:
//
//	*_GOOS.go
//	*_GOARCH.go
//	*_GOOS_GOARCH.go
//
// where GOOS and GOARCH are known operating systems or architectures.
// `_test.go` is intentionally NOT excluded here (callers handle that
// upstream).
func matchOSArchFilename(buildCtx build.Context, name string) bool {
	if !strings.HasSuffix(name, ".go") {
		return true
	}
	stem := strings.TrimSuffix(name, ".go")
	stem = strings.TrimSuffix(stem, "_test")
	parts := strings.Split(stem, "_")
	n := len(parts)
	if n < 2 {
		return true
	}

	last := parts[n-1]
	prev := ""
	if n >= 3 {
		prev = parts[n-2]
	}

	if knownOS(prev) && knownArch(last) {
		return prev == buildCtx.GOOS && last == buildCtx.GOARCH
	}
	if knownOS(last) {
		return last == buildCtx.GOOS
	}
	if knownArch(last) {
		return last == buildCtx.GOARCH
	}
	return true
}

// buildConstraintLines extracts each `//go:build` or `// +build` line
// from the file's leading comment block (everything up to the first
// blank line after a comment, per Go's build constraint placement rules).
//
// We're conservative: scan up to the first non-comment, non-blank line
// after we've seen any meaningful content, then stop. This catches both
// new-style and legacy constraints in practice.
func buildConstraintLines(content string) []string {
	var out []string
	for _, raw := range strings.Split(content, "\n") {
		line := strings.TrimSpace(raw)
		if line == "" {
			continue
		}
		// Stop at the package declaration — constraints must appear
		// before package per Go convention.
		if strings.HasPrefix(line, "package ") || line == "package" {
			break
		}
		if !strings.HasPrefix(line, "//") {
			continue
		}
		if constraint.IsGoBuild(line) || constraint.IsPlusBuild(line) {
			out = append(out, line)
		}
	}
	return out
}

// matchTag evaluates a single build tag against the build context.
func matchTag(buildCtx build.Context, tag string) bool {
	switch tag {
	case "ignore":
		return false
	case buildCtx.GOOS:
		return true
	case buildCtx.GOARCH:
		return true
	case "cgo":
		return buildCtx.CgoEnabled
	case "unix":
		return knownUnixOS(buildCtx.GOOS)
	}
	for _, t := range buildCtx.BuildTags {
		if t == tag {
			return true
		}
	}
	for _, t := range buildCtx.ToolTags {
		if t == tag {
			return true
		}
	}
	for _, t := range buildCtx.ReleaseTags {
		if t == tag {
			return true
		}
	}
	return false
}

// knownOS / knownArch / knownUnixOS mirror the lists Go's build package
// uses to identify GOOS/GOARCH filename suffixes. Limited to the
// recognized values so unrelated underscore-separated stems
// (`server_handler.go`) don't trigger filtering.
func knownOS(s string) bool { _, ok := knownOSes[s]; return ok }
func knownArch(s string) bool { _, ok := knownArches[s]; return ok }
func knownUnixOS(s string) bool { _, ok := unixOSes[s]; return ok }

// knownOSes lists every Go-recognized GOOS value as of Go 1.22. New
// values can be added without breaking existing files since unknown
// suffixes already fall through to "match".
var knownOSes = map[string]bool{
	"aix": true, "android": true, "darwin": true, "dragonfly": true,
	"freebsd": true, "hurd": true, "illumos": true, "ios": true,
	"js": true, "linux": true, "nacl": true, "netbsd": true,
	"openbsd": true, "plan9": true, "solaris": true, "wasip1": true,
	"windows": true, "zos": true,
}

var unixOSes = map[string]bool{
	"aix": true, "android": true, "darwin": true, "dragonfly": true,
	"freebsd": true, "hurd": true, "illumos": true, "ios": true,
	"linux": true, "netbsd": true, "openbsd": true, "solaris": true,
}

var knownArches = map[string]bool{
	"386": true, "amd64": true, "amd64p32": true, "arm": true,
	"arm64": true, "arm64be": true, "armbe": true, "loong64": true,
	"mips": true, "mips64": true, "mips64le": true, "mips64p32": true,
	"mips64p32le": true, "mipsle": true, "ppc": true, "ppc64": true,
	"ppc64le": true, "riscv": true, "riscv64": true, "s390": true,
	"s390x": true, "sparc": true, "sparc64": true, "wasm": true,
}
