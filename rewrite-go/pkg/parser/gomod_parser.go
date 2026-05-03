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
	"log"
	"regexp"
	"strings"

	"golang.org/x/mod/modfile"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// ParseGoMod parses go.mod content into a tree.GoResolutionResult.
// Mirrors org.openrewrite.golang.GoModParser on the Java side.
func ParseGoMod(path, content string) (*tree.GoResolutionResult, error) {
	f, err := modfile.Parse(path, []byte(content), nil)
	if err != nil {
		return nil, err
	}

	mrr := tree.NewGoResolutionResult("", "", "", path)

	if f.Module != nil {
		mrr.ModulePath = f.Module.Mod.Path
	}
	if f.Go != nil {
		mrr.GoVersion = f.Go.Version
	}
	if f.Toolchain != nil {
		mrr.Toolchain = f.Toolchain.Name
	}

	for _, r := range f.Require {
		mrr.Requires = append(mrr.Requires, tree.GoRequire{
			ModulePath: r.Mod.Path,
			Version:    r.Mod.Version,
			Indirect:   r.Indirect,
		})
	}

	for _, r := range f.Replace {
		mrr.Replaces = append(mrr.Replaces, tree.GoReplace{
			OldPath:    r.Old.Path,
			OldVersion: r.Old.Version,
			NewPath:    r.New.Path,
			NewVersion: r.New.Version,
		})
	}

	for _, e := range f.Exclude {
		mrr.Excludes = append(mrr.Excludes, tree.GoExclude{
			ModulePath: e.Mod.Path,
			Version:    e.Mod.Version,
		})
	}

	for _, r := range f.Retract {
		// modfile.Retract gives Low and High; if equal, it's a single
		// version, otherwise it's a range. Format the range expression to
		// match what the Java side stores.
		var rng string
		if r.Low == r.High {
			rng = r.Low
		} else {
			rng = "[" + r.Low + ", " + r.High + "]"
		}
		mrr.Retracts = append(mrr.Retracts, tree.GoRetract{
			VersionRange: rng,
			Rationale:    strings.TrimSpace(r.Rationale),
		})
	}

	return &mrr, nil
}

// goSumLine matches one line of a go.sum file:
//
//	<module> <version>[/go.mod] h1:<hash>
//
// Each module version appears on two lines — one for the module zip, one
// for its go.mod. Mirrors the Java GO_SUM_LINE pattern.
var goSumLine = regexp.MustCompile(`^\s*(\S+)\s+(\S+?)(/go\.mod)?\s+h1:(\S+)\s*$`)

// ParseGoSum parses go.sum content into a slice of GoResolvedDependency,
// one per (module, version) pair. Bad lines are logged and skipped — go.sum
// is best-effort metadata, not an authoritative spec; a single malformed
// line should never tank a parse.
//
// Mirrors org.openrewrite.golang.GoModParser#parseSumSibling. The Go side
// is content-based (not filesystem-based) because the parser is invoked via
// RPC where sources are passed as strings.
func ParseGoSum(content string) []tree.GoResolvedDependency {
	if content == "" {
		return nil
	}
	type slot struct{ module, gomod string }
	order := []string{}
	byKey := map[string]*slot{}
	for i, line := range strings.Split(content, "\n") {
		if strings.TrimSpace(line) == "" {
			continue
		}
		m := goSumLine.FindStringSubmatch(line)
		if m == nil {
			log.Printf("go.sum line %d: skipping malformed entry: %q", i+1, line)
			continue
		}
		module, version, isGoMod, hash := m[1], m[2], m[3] != "", "h1:"+m[4]
		key := module + "@" + version
		s, ok := byKey[key]
		if !ok {
			s = &slot{}
			byKey[key] = s
			order = append(order, key)
		}
		if isGoMod {
			s.gomod = hash
		} else {
			s.module = hash
		}
	}
	out := make([]tree.GoResolvedDependency, 0, len(order))
	for _, key := range order {
		s := byKey[key]
		parts := strings.SplitN(key, "@", 2)
		out = append(out, tree.GoResolvedDependency{
			ModulePath: parts[0],
			Version:    parts[1],
			ModuleHash: s.module,
			GoModHash:  s.gomod,
		})
	}
	return out
}
