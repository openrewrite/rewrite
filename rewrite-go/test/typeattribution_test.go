//go:build parityaudit

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

package test

import (
	"fmt"
	"go/types"
	"os"
	"path/filepath"
	"reflect"
	"runtime"
	"sort"
	"strings"
	"sync"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// TestTypeAttribution counts how many of a corpus's type slots came back
// resolved, per (node type, field): `java.Identifier.Type` and
// `java.MethodInvocation.MethodType` are separate populations, because a
// regression usually shows up in one of them rather than in the total.
//
//	GO_CORPUS=/tmp/go-corpus go test -tags parityaudit ./test/ -run TestTypeAttribution -timeout 60m
func TestTypeAttribution(t *testing.T) {
	root := os.Getenv("GO_CORPUS")
	if root == "" {
		t.Skip("GO_CORPUS not set")
	}
	files := stride(collectGoFiles(t, root), envInt("GO_TYPES_FILES", 2000))
	if os.Getenv("GO_TYPES_PACKAGE") != "" {
		files = packageMatesOf(t, root, files)
	}

	var mu sync.Mutex
	totals := map[string]*slotCount{}
	emptySites := map[string]int{}

	var wg sync.WaitGroup
	work := make(chan string)
	for w := 0; w < runtime.NumCPU(); w++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for path := range work {
				counts, empty := attributionOf(root, path)
				mu.Lock()
				for k, n := range empty {
					emptySites[k] += n
				}
				for k, v := range counts {
					c := totals[k]
					if c == nil {
						c = &slotCount{}
						totals[k] = c
					}
					c.resolved += v.resolved
					c.unknown += v.unknown
					c.nil_ += v.nil_
				}
				mu.Unlock()
			}
		}()
	}
	for _, f := range files {
		work <- f
	}
	close(work)
	wg.Wait()

	keys := make([]string, 0, len(totals))
	var all slotCount
	for k, v := range totals {
		keys = append(keys, k)
		all.resolved += v.resolved
		all.unknown += v.unknown
		all.nil_ += v.nil_
	}
	sort.Slice(keys, func(i, j int) bool { return totals[keys[i]].total() > totals[keys[j]].total() })

	var sb strings.Builder
	fmt.Fprintf(&sb, "files: %d\n", len(files))
	fmt.Fprintf(&sb, "%-52s %10s %10s %10s %8s\n", "slot", "resolved", "unknown", "nil", "resolved%")
	fmt.Fprintf(&sb, "%-52s %10d %10d %10d %7.2f%%\n", "TOTAL", all.resolved, all.unknown, all.nil_, all.pct())
	for _, k := range keys {
		c := totals[k]
		fmt.Fprintf(&sb, "%-52s %10d %10d %10d %7.2f%%\n", k, c.resolved, c.unknown, c.nil_, c.pct())
	}
	sites := make([]string, 0, len(emptySites))
	for k := range emptySites {
		sites = append(sites, k)
	}
	sort.Slice(sites, func(i, j int) bool { return emptySites[sites[i]] > emptySites[sites[j]] })
	fmt.Fprintf(&sb, "\nempty slots by where the node sits\n")
	for _, k := range sites[:min(len(sites), envInt("GO_TYPES_SITES", 40))] {
		fmt.Fprintf(&sb, "%10d  %s\n", emptySites[k], k)
	}
	t.Logf("\n%s", sb.String())
}

type slotCount struct{ resolved, unknown, nil_ int }

func (c *slotCount) total() int { return c.resolved + c.unknown + c.nil_ }
func (c *slotCount) pct() float64 {
	if c.total() == 0 {
		return 0
	}
	return 100 * float64(c.resolved) / float64(c.total())
}

// packageMatesOf expands a file sample to whole directories, so each
// file is parsed alongside the siblings that define what it references.
func packageMatesOf(t *testing.T, root string, sample []string) []string {
	dirs := map[string]bool{}
	for _, f := range sample {
		dirs[filepath.Dir(f)] = true
	}
	var out []string
	for d := range dirs {
		out = append(out, d)
	}
	sort.Strings(out)
	return out
}

// moduleRootOf finds the directory owning `path`'s go.mod, which is what
// an importer resolves against. A corpus root holds many repositories,
// and sources from one cannot satisfy another's imports.
func moduleRootOf(corpusRoot, path string) string {
	for d := path; strings.HasPrefix(d, corpusRoot) && d != corpusRoot; d = filepath.Dir(d) {
		if _, err := os.Stat(filepath.Join(d, "go.mod")); err == nil {
			return d
		}
	}
	// No go.mod above it: treat the repository directory as the module.
	rel, err := filepath.Rel(corpusRoot, path)
	if err != nil || rel == "." {
		return corpusRoot
	}
	return filepath.Join(corpusRoot, strings.SplitN(rel, string(filepath.Separator), 2)[0])
}

// lockedImporter serializes a ProjectImporter, which caches the packages
// it resolves and so cannot be shared between workers as it stands.
type lockedImporter struct {
	mu    sync.Mutex
	inner *parser.ProjectImporter
}

func (l *lockedImporter) Import(path string) (*types.Package, error) {
	l.mu.Lock()
	defer l.mu.Unlock()
	return l.inner.Import(path)
}

var (
	importerMu    sync.Mutex
	importerCache = map[string]*lockedImporter{}
)

// importerFor builds one importer per module and hands it out again on
// later calls: reading a repository's sources is the expensive part, and
// every directory in it resolves against the same set.
func importerFor(moduleRoot string) *lockedImporter {
	importerMu.Lock()
	defer importerMu.Unlock()
	if li, ok := importerCache[moduleRoot]; ok {
		return li
	}
	pi := parser.NewProjectImporter(filepath.Base(moduleRoot), nil)
	pi.SetProjectRoot(moduleRoot)
	_ = filepath.Walk(moduleRoot, func(p string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() || !strings.HasSuffix(p, ".go") {
			return nil
		}
		if b, err := os.ReadFile(p); err == nil {
			rel, _ := filepath.Rel(moduleRoot, p)
			pi.AddSource(rel, string(b))
		}
		return nil
	})
	li := &lockedImporter{inner: pi}
	importerCache[moduleRoot] = li
	return li
}

// parseGroup parses one directory as a package when GO_TYPES_PACKAGE is
// set, and a single file otherwise.
func parseGroup(root, path string) []*golang.CompilationUnit {
	gp := parser.NewGoParser()
	if os.Getenv("GO_TYPES_PACKAGE") == "" {
		src, err := os.ReadFile(path)
		if err != nil || !parser.MatchBuildContext(gp.BuildContext, filepath.Base(path), string(src)) {
			return nil
		}
		cu, err := gp.Parse(filepath.Base(path), string(src))
		if err != nil {
			return nil
		}
		return []*golang.CompilationUnit{cu}
	}

	gp.Importer = importerFor(moduleRootOf(root, path))

	entries, err := os.ReadDir(path)
	if err != nil {
		return nil
	}
	byPkg := map[string][]parser.FileInput{}
	for _, e := range entries {
		if e.IsDir() || !strings.HasSuffix(e.Name(), ".go") {
			continue
		}
		full := filepath.Join(path, e.Name())
		b, err := os.ReadFile(full)
		if err != nil || !parser.MatchBuildContext(gp.BuildContext, e.Name(), string(b)) {
			continue
		}
		pkg := parser.PackageNameOf(full, string(b))
		byPkg[pkg] = append(byPkg[pkg], parser.FileInput{Path: e.Name(), Content: string(b)})
	}
	var out []*golang.CompilationUnit
	for _, inputs := range byPkg {
		if cus, err := gp.ParsePackage(inputs); err == nil {
			out = append(out, cus...)
		}
	}
	return out
}

func attributionOf(root, path string) (counts map[string]*slotCount, empty map[string]int) {
	counts, empty = map[string]*slotCount{}, map[string]int{}
	defer func() { recover() }()

	for _, cu := range parseGroup(root, path) {
		w := &typeWalker{counts: counts, empty: empty, seen: map[uintptr]bool{}}
		w.walk(reflect.ValueOf(cu), "", "", "<root>")
	}
	return
}

var javaTypeIface = reflect.TypeOf((*java.JavaType)(nil)).Elem()

type typeWalker struct {
	counts map[string]*slotCount
	// empty is keyed by the field the node sits in as well as its own,
	// because an identifier's role decides whether Go has a type for it.
	empty map[string]int
	seen  map[uintptr]bool
}

func (w *typeWalker) count(slot, site string, v reflect.Value) {
	c := w.counts[slot]
	if c == nil {
		c = &slotCount{}
		w.counts[slot] = c
	}
	switch {
	case v.IsNil():
		c.nil_++
		w.empty[site+" [nil]"]++
	case v.Interface() == java.UnknownType:
		c.unknown++
		w.empty[site+" [unknown]"]++
	default:
		c.resolved++
	}
}

// walk threads two things: `owner`, the type of the node a field belongs
// to, and `site`, the field of the enclosing node that this one sits in.
func (w *typeWalker) walk(v reflect.Value, owner, path, site string) {
	if !v.IsValid() {
		return
	}
	switch v.Kind() {
	case reflect.Ptr, reflect.Interface:
		if v.IsNil() {
			return
		}
		if v.Kind() == reflect.Ptr {
			if v.Type().Implements(javaTypeIface) {
				return // inside the type graph, not a slot on a node
			}
			if w.seen[v.Pointer()] {
				return
			}
			w.seen[v.Pointer()] = true
			owner, site = strings.TrimPrefix(v.Type().String(), "*"), path
		}
		w.walk(v.Elem(), owner, path, site)
	case reflect.Slice, reflect.Array:
		for i := 0; i < v.Len(); i++ {
			w.walk(v.Index(i), owner, path, site)
		}
	case reflect.Map:
		for _, k := range v.MapKeys() {
			w.walk(v.MapIndex(k), owner, path, site)
		}
	case reflect.Struct:
		if v.Type().Implements(javaTypeIface) {
			return
		}
		if n := v.Type().Name(); n != "" && !strings.HasPrefix(n, "Container") &&
			!strings.HasPrefix(n, "RightPadded") && !strings.HasPrefix(n, "LeftPadded") {
			owner, site = v.Type().String(), path
		}
		for i := 0; i < v.NumField(); i++ {
			f := v.Type().Field(i)
			if f.PkgPath != "" {
				continue
			}
			fv := v.Field(i)
			if isTypeSlot(f.Type) {
				w.count(owner+"."+f.Name, site+" -> "+owner+"."+f.Name, fv)
				continue
			}
			w.walk(fv, owner, owner+"."+f.Name, site)
		}
	}
}

// isTypeSlot reports whether a field holds attribution rather than tree
// structure: the JavaType interface itself, or a pointer to one of the
// concrete types a node names directly.
func isTypeSlot(t reflect.Type) bool {
	if t.Kind() == reflect.Interface {
		return t == javaTypeIface
	}
	return t.Kind() == reflect.Ptr && t.Implements(javaTypeIface)
}
