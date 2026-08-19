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
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

// ParseVendorModules reads a vendor/modules.txt into the vendored build list and
// the package-to-module map. Its grammar is a `# module version [=> replacement]`
// line, an optional `## marker; marker` line, then one line per vendored package:
//
//	# golang.org/x/mod v0.35.0
//	## explicit; go 1.23
//	golang.org/x/mod/modfile
//
// A module without the `explicit` marker is not required by the main module
// directly, which is the same distinction `go list -m` reports as indirect.
func ParseVendorModules(content string) ([]golang.GoResolvedDependency, []golang.GoPackageModule) {
	mods := []golang.GoResolvedDependency{}
	pkgs := []golang.GoPackageModule{}

	current := -1
	for _, line := range strings.Split(content, "\n") {
		line = strings.TrimSpace(line)
		switch {
		case line == "":
			continue

		case strings.HasPrefix(line, "##"):
			if current < 0 {
				continue
			}
			for _, marker := range strings.Split(strings.TrimPrefix(line, "##"), ";") {
				marker = strings.TrimSpace(marker)
				if marker == "explicit" {
					mods[current].Indirect = false
				} else if goVersion := strings.TrimPrefix(marker, "go "); goVersion != marker {
					mods[current].ModuleGoVersion = strings.TrimSpace(goVersion)
				}
			}

		case strings.HasPrefix(line, "#"):
			mod, ok := parseVendorModuleLine(strings.TrimSpace(strings.TrimPrefix(line, "#")))
			if !ok {
				current = -1
				continue
			}
			mods = append(mods, mod)
			current = len(mods) - 1

		default:
			if current < 0 {
				continue
			}
			pkgs = append(pkgs, golang.GoPackageModule{
				ImportPath: line,
				ModulePath: mods[current].ModulePath,
				Version:    mods[current].Version,
			})
		}
	}
	return mods, pkgs
}

// parseVendorModuleLine reads `path version [=> path [version]]`. Everything in
// modules.txt is vendored into the build, hence Selected; Indirect starts true
// because only an `explicit` marker line establishes a direct requirement.
func parseVendorModuleLine(line string) (golang.GoResolvedDependency, bool) {
	spec, replacement, _ := strings.Cut(line, "=>")
	fields := strings.Fields(spec)
	if len(fields) < 2 {
		return golang.GoResolvedDependency{}, false
	}
	mod := golang.GoResolvedDependency{
		ModulePath: fields[0],
		Version:    fields[1],
		Indirect:   true,
		Selected:   true,
	}
	if replaced := strings.Fields(replacement); len(replaced) > 0 {
		mod.ReplacePath = replaced[0]
		if len(replaced) > 1 {
			mod.ReplaceVersion = replaced[1]
		}
	}
	return mod, true
}
