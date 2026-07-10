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
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"os/exec"
	"strings"
	"time"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

// resolveTimeout bounds each `go` invocation so a hung proxy fetch can never
// block the parse indefinitely. On timeout the command errors and the caller
// falls back to go.sum-only.
const resolveTimeout = 120 * time.Second

// ResolveModuleGraph runs the Go toolchain in moduleDir to produce the resolved
// build list (with graph edges) and the imported-package -> module map that
// go.sum alone cannot provide. It is a pure function of the on-disk module: no
// marker coupling, no mutation of go.mod/go.sum (readonly + -e).
//
// It degrades gracefully: any hard toolchain/network failure returns an error
// so the caller can keep today's go.sum-only behavior. Partial output from an
// untidy module (surfaced via -e) is kept rather than discarded.
func ResolveModuleGraph(moduleDir string) (mods []golang.GoResolvedDependency, pkgs []golang.GoPackageModule, err error) {
	defer func() {
		if r := recover(); r != nil {
			mods, pkgs, err = nil, nil, fmt.Errorf("resolve panicked: %v", r)
		}
	}()

	// The build list is mandatory: without it there is nothing to enrich, so a
	// failure here propagates and the caller falls back to go.sum-only.
	mods, err = goListModules(moduleDir)
	if err != nil {
		return nil, nil, err
	}
	// Graph edges and the package map are best-effort: a partial build list is
	// still useful, so sub-failures are swallowed rather than discarding mods.
	if edges, gerr := goModGraph(moduleDir); gerr == nil {
		attachEdges(mods, edges)
	}
	pkgs, _ = goListPackages(moduleDir)
	return mods, pkgs, nil
}

// goModule is the subset of `go list -m -json` / `go list -deps -json` output we consume.
type goModule struct {
	Path      string
	Version   string
	Main      bool
	Indirect  bool
	GoVersion string
	Replace   *goModule
}

type goPackage struct {
	ImportPath string
	Standard   bool
	Module     *goModule
}

func goListModules(dir string) ([]golang.GoResolvedDependency, error) {
	stdout, err := runGo(dir, "list", "-mod=readonly", "-e", "-m", "-json", "all")
	if err != nil && len(stdout) == 0 {
		return nil, err
	}
	var out []golang.GoResolvedDependency
	dec := json.NewDecoder(bytes.NewReader(stdout))
	for {
		var m goModule
		if derr := dec.Decode(&m); derr == io.EOF {
			break
		} else if derr != nil {
			return out, fmt.Errorf("go list -m decode: %w", derr)
		}
		if m.Path == "" {
			continue
		}
		rd := golang.GoResolvedDependency{
			ModulePath:      m.Path,
			Version:         m.Version,
			Main:            m.Main,
			Indirect:        m.Indirect,
			ModuleGoVersion: m.GoVersion,
		}
		if m.Replace != nil {
			rd.ReplacePath = m.Replace.Path
			rd.ReplaceVersion = m.Replace.Version
		}
		out = append(out, rd)
	}
	return out, nil
}

func goListPackages(dir string) ([]golang.GoPackageModule, error) {
	stdout, err := runGo(dir, "list", "-mod=readonly", "-e", "-deps", "-test", "-json", "./...")
	if err != nil && len(stdout) == 0 {
		return nil, err
	}
	var out []golang.GoPackageModule
	dec := json.NewDecoder(bytes.NewReader(stdout))
	for {
		var p goPackage
		if derr := dec.Decode(&p); derr == io.EOF {
			break
		} else if derr != nil {
			return out, fmt.Errorf("go list -deps decode: %w", derr)
		}
		if p.ImportPath == "" {
			continue
		}
		pm := golang.GoPackageModule{ImportPath: p.ImportPath, Standard: p.Standard}
		if p.Module != nil {
			pm.ModulePath = p.Module.Path
			pm.Version = p.Module.Version
		}
		out = append(out, pm)
	}
	return out, nil
}

type moduleEdge struct {
	fromPath, toPath, toVersion string
}

func goModGraph(dir string) ([]moduleEdge, error) {
	stdout, err := runGo(dir, "mod", "graph")
	if err != nil && len(stdout) == 0 {
		return nil, err
	}
	var edges []moduleEdge
	for _, line := range strings.Split(string(stdout), "\n") {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}
		parts := strings.Fields(line)
		if len(parts) != 2 {
			continue
		}
		fromPath, _ := splitModuleVersion(parts[0])
		toPath, toVersion := splitModuleVersion(parts[1])
		edges = append(edges, moduleEdge{fromPath: fromPath, toPath: toPath, toVersion: toVersion})
	}
	return edges, nil
}

func splitModuleVersion(s string) (path, version string) {
	if i := strings.Index(s, "@"); i >= 0 {
		return s[:i], s[i+1:]
	}
	return s, ""
}

// attachEdges sets each build-list node's Deps from the module graph. Edges are
// keyed by the source module path (MVS selects one version per path), so a node
// receives the direct dependencies its selected version requires.
func attachEdges(mods []golang.GoResolvedDependency, edges []moduleEdge) {
	byPath := make(map[string]int, len(mods))
	for i := range mods {
		byPath[mods[i].ModulePath] = i
	}
	for _, e := range edges {
		i, ok := byPath[e.fromPath]
		if !ok {
			continue
		}
		mods[i].Deps = append(mods[i].Deps, golang.GoModuleRef{ModulePath: e.toPath, Version: e.toVersion})
	}
}

func runGo(dir string, args ...string) ([]byte, error) {
	ctx, cancel := context.WithTimeout(context.Background(), resolveTimeout)
	defer cancel()
	cmd := exec.CommandContext(ctx, "go", args...)
	cmd.Dir = dir
	cmd.Env = append(os.Environ(), "GO111MODULE=on", "GOFLAGS=-mod=readonly")
	var stdout, stderr bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr
	if err := cmd.Run(); err != nil {
		return stdout.Bytes(), fmt.Errorf("go %s: %v: %s", strings.Join(args, " "), err, strings.TrimSpace(stderr.String()))
	}
	return stdout.Bytes(), nil
}

// MergeResolvedDependencies overlays the toolchain-resolved build list onto the
// go.sum-derived hash rows, keyed by module@version. Build-list nodes are
// authoritative (they carry the selected version, indirect/main flags, replace
// info and graph edges) and inherit the go.sum content hashes for their version.
// go.sum rows whose version is not in the selected build list (stale/extra
// hashes) are preserved. Pure function — unit-testable without the toolchain.
func MergeResolvedDependencies(fromSum, fromList []golang.GoResolvedDependency) []golang.GoResolvedDependency {
	key := func(d golang.GoResolvedDependency) string { return d.ModulePath + "@" + d.Version }
	sumByKey := make(map[string]golang.GoResolvedDependency, len(fromSum))
	for _, d := range fromSum {
		sumByKey[key(d)] = d
	}
	out := make([]golang.GoResolvedDependency, 0, len(fromList)+len(fromSum))
	seen := make(map[string]bool, len(fromList))
	for _, m := range fromList {
		k := key(m)
		if s, ok := sumByKey[k]; ok {
			m.ModuleHash = s.ModuleHash
			m.GoModHash = s.GoModHash
		}
		out = append(out, m)
		seen[k] = true
	}
	for _, s := range fromSum {
		if !seen[key(s)] {
			out = append(out, s)
		}
	}
	return out
}
