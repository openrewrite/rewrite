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
	"encoding/json"
	"fmt"
	"go/build"
	"os"
	"path/filepath"
	"regexp"
	"runtime"
	"sort"
	"strconv"
	"strings"
	"sync"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	recipes "github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// TestSweep classifies every .go file under $GO_CORPUS as sound,
// unsound, or a parse error, and writes a bucketed report plus a JSONL
// row per file to $GO_CORPUS_OUT (default /tmp/go-sweep).
//
// Buckets are keyed on normalized signatures: error text with source
// offsets stripped, hidden Space content with identifiers and literals
// collapsed. Un-normalized keys fragment a single cause across hundreds
// of apparent singletons.
//
//	GO_CORPUS=/tmp/go-corpus go test -tags parityaudit ./test/ -run TestSweep -timeout 60m
func TestSweep(t *testing.T) {
	root := os.Getenv("GO_CORPUS")
	if root == "" {
		t.Skip("GO_CORPUS not set")
	}
	outDir := os.Getenv("GO_CORPUS_OUT")
	if outDir == "" {
		outDir = "/tmp/go-sweep"
	}
	if err := os.MkdirAll(outDir, 0o755); err != nil {
		t.Fatalf("mkdir out: %v", err)
	}

	files := collectGoFiles(t, root)

	// A stack overflow in the parser is a fatal runtime error: recover
	// cannot catch it and the process dies mid-sweep. Results are
	// therefore journalled as they complete and each worker publishes
	// the file it is on, so a rerun resumes and attributes the crash.
	j, err := openJournal(outDir)
	if err != nil {
		t.Fatalf("journal: %v", err)
	}
	defer j.close()

	suspects, pending := j.partition(root, files)
	if len(suspects) == 1 {
		j.record(sweepResult{Path: suspects[0], Class: "parse_error", Bucket: "fatal: process died (stack overflow or OOM)"})
		suspects = nil
	}
	t.Logf("sweeping %d files under %s (%d done, %d suspect)", len(files), root, len(files)-len(pending)-len(suspects), len(suspects))

	// Suspects run alone so the next rerun sees exactly one in-flight
	// path and can name the culprit.
	for _, s := range suspects {
		j.markInflight(0, s)
		j.record(sweepOne(root, filepath.Join(root, s)))
		j.clearInflight(0)
	}

	var wg sync.WaitGroup
	work := make(chan int)
	workers := runtime.NumCPU()
	for w := 0; w < workers; w++ {
		wg.Add(1)
		go func(w int) {
			defer wg.Done()
			for i := range work {
				rel, _ := filepath.Rel(root, pending[i])
				j.markInflight(w, rel)
				j.record(sweepOne(root, pending[i]))
				j.clearInflight(w)
			}
		}(w)
	}
	for i := range pending {
		work <- i
	}
	close(work)
	wg.Wait()

	writeReport(t, outDir, j.all())
}

// journal is the crash-resumable record of a sweep: an append-only
// JSONL of finished files plus one marker file per worker naming the
// file it is currently on.
type journal struct {
	mu       sync.Mutex
	dir      string
	f        *os.File
	enc      *json.Encoder
	done     map[string]bool
	results  []sweepResult
	inflight string
}

func openJournal(dir string) (*journal, error) {
	j := &journal{dir: dir, done: map[string]bool{}, inflight: filepath.Join(dir, "inflight")}
	if err := os.MkdirAll(j.inflight, 0o755); err != nil {
		return nil, err
	}
	path := filepath.Join(dir, "results.jsonl")
	if b, err := os.ReadFile(path); err == nil {
		for _, line := range strings.Split(string(b), "\n") {
			if strings.TrimSpace(line) == "" {
				continue
			}
			var r sweepResult
			if json.Unmarshal([]byte(line), &r) == nil {
				j.done[r.Path] = true
				j.results = append(j.results, r)
			}
		}
	}
	f, err := os.OpenFile(path, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o644)
	if err != nil {
		return nil, err
	}
	j.f, j.enc = f, json.NewEncoder(f)
	return j, nil
}

// partition splits files into those a previous run was mid-way through
// when it died, and those never attempted.
func (j *journal) partition(root string, files []string) (suspects, pending []string) {
	entries, _ := os.ReadDir(j.inflight)
	seen := map[string]bool{}
	for _, e := range entries {
		b, err := os.ReadFile(filepath.Join(j.inflight, e.Name()))
		if err != nil {
			continue
		}
		p := strings.TrimSpace(string(b))
		if p != "" && !j.done[p] && !seen[p] {
			seen[p] = true
			suspects = append(suspects, p)
		}
		os.Remove(filepath.Join(j.inflight, e.Name()))
	}
	for _, f := range files {
		rel, _ := filepath.Rel(root, f)
		if !j.done[rel] && !seen[rel] {
			pending = append(pending, f)
		}
	}
	sort.Strings(suspects)
	return
}

func (j *journal) markInflight(worker int, rel string) {
	os.WriteFile(filepath.Join(j.inflight, fmt.Sprintf("w%d", worker)), []byte(rel), 0o644)
}

func (j *journal) clearInflight(worker int) {
	os.Remove(filepath.Join(j.inflight, fmt.Sprintf("w%d", worker)))
}

func (j *journal) record(r sweepResult) {
	j.mu.Lock()
	defer j.mu.Unlock()
	j.done[r.Path] = true
	j.results = append(j.results, r)
	j.enc.Encode(r)
}

func (j *journal) all() []sweepResult {
	j.mu.Lock()
	defer j.mu.Unlock()
	return append([]sweepResult{}, j.results...)
}

func (j *journal) close() { j.f.Close() }

type sweepResult struct {
	Path   string `json:"path"`
	Class  string `json:"class"` // sound | unsound | parse_error | skipped
	Bucket string `json:"bucket,omitempty"`
	Detail string `json:"detail,omitempty"`
}

// sweepOne runs the full pipeline on one file. A file is a parse error
// if the parser rejects it, panics, or prints back something other than
// its input; unsound if it round-trips but hides source text in a Space.
func sweepOne(root, path string) (res sweepResult) {
	rel, _ := filepath.Rel(root, path)
	res = sweepResult{Path: rel}

	defer func() {
		if r := recover(); r != nil {
			res.Class = "parse_error"
			res.Bucket = "panic: " + normalizeErr(fmt.Sprint(r))
			res.Detail = fmt.Sprint(r)
		}
	}()

	src, err := os.ReadFile(path)
	if err != nil {
		res.Class = "skipped"
		res.Bucket = "unreadable"
		return
	}
	// A file guarded by `//go:build` reaches the parser only under a
	// context that selects it, and the host's is one of many. Grading it
	// under the first context that takes it covers the platform-specific
	// halves of the standard library, which no single host sees.
	gp := parserSelecting(filepath.Base(path), string(src))
	if gp == nil {
		res.Class = "skipped"
		res.Bucket = "build-constrained"
		return
	}

	cu, err := gp.Parse(filepath.Base(path), string(src))
	if err != nil {
		res.Class = "parse_error"
		res.Bucket = "error: " + normalizeErr(err.Error())
		res.Detail = err.Error()
		return
	}

	printed := printer.Print(cu)
	if printed != string(src) {
		res.Class = "parse_error"
		b, d := diffSignature(string(src), printed)
		res.Bucket = "roundtrip: " + b
		res.Detail = d
		return
	}

	if sites := locateHidden(cu); len(sites) > 0 {
		res.Class = "unsound"
		res.Bucket = "hidden in " + sites[0].owner + ": " + normalizeHidden(sites[0].msg)
		var ds []string
		for _, s := range sites[:min(len(sites), 3)] {
			ds = append(ds, s.owner+" "+s.msg)
		}
		res.Detail = strings.Join(ds, " | ")
		return
	}

	res.Class = "sound"
	return
}

// auditContexts span the GOOS/GOARCH combinations that between them
// select nearly every build-constrained file; one excluded from all of
// them is genuinely unreachable.
var auditContexts = []struct{ goos, goarch string }{
	{"linux", "amd64"},
	{"linux", "arm64"},
	{"darwin", "arm64"},
	{"windows", "amd64"},
	{"freebsd", "amd64"},
	{"plan9", "386"},
	{"js", "wasm"},
}

// parserSelecting returns a parser whose build context takes the file,
// or nil when none does.
func parserSelecting(name, content string) *parser.GoParser {
	if parser.MatchBuildContext(build.Default, name, content) {
		return parser.NewGoParser()
	}
	for _, c := range auditContexts {
		bc := build.Default
		bc.GOOS, bc.GOARCH = c.goos, c.goarch
		if parser.MatchBuildContext(bc, name, content) {
			return parser.NewGoParserWithBuildContext(bc)
		}
	}
	return nil
}

// hiddenSite names the LST field that owns an offending Space. Without
// the owner, every bucket is just the stowed text, which rarely points
// at the parser code path that produced it.
type hiddenSite struct {
	owner string
	msg   string
}

func locateHidden(root java.Tree) []hiddenSite {
	var sites []hiddenSite
	for _, e := range (&recipes.WhitespaceValidationService{}).Validate(root) {
		owner, msg, _ := strings.Cut(e, ": ")
		sites = append(sites, hiddenSite{owner, msg})
	}
	return sites
}

func collectGoFiles(t *testing.T, root string) []string {
	var out []string
	err := filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return nil // unreadable subtree: skip rather than abort the sweep
		}
		if info.IsDir() {
			if info.Name() == ".git" {
				return filepath.SkipDir
			}
			return nil
		}
		if strings.HasSuffix(path, ".go") {
			out = append(out, path)
		}
		return nil
	})
	if err != nil {
		t.Fatalf("walk: %v", err)
	}
	sort.Strings(out)
	return out
}

var (
	reOffset  = regexp.MustCompile(`\b\d+:\d+\b`)
	reNum     = regexp.MustCompile(`\b\d+\b`)
	rePath    = regexp.MustCompile(`[\w./-]+\.go`)
	reQuoted  = regexp.MustCompile(`"[^"]*"`)
	reBacktik = regexp.MustCompile("`[^`]*`")
	reIdent   = regexp.MustCompile(`[A-Za-z_][A-Za-z0-9_]*`)
)

// normalizeErr collapses the parts of an error that vary per file —
// paths, source offsets, quoted names — so one cause lands in one
// bucket instead of one bucket per occurrence.
func normalizeErr(s string) string {
	s = rePath.ReplaceAllString(s, "F.go")
	s = reOffset.ReplaceAllString(s, "L:C")
	s = reQuoted.ReplaceAllString(s, `"X"`)
	s = reNum.ReplaceAllString(s, "N")
	return truncate(strings.TrimSpace(s), 120)
}

// normalizeHidden reduces a validator message to the shape of the text
// that got stowed in a Space: identifiers and literals collapse to
// placeholders so `foo.Bar` and `baz.Qux` share a bucket, while the
// punctuation that identifies the construct survives.
func normalizeHidden(msg string) string {
	i := strings.Index(msg, `: "`)
	if i < 0 {
		return truncate(msg, 120)
	}
	kind := msg[:i]
	body := msg[i+2:]
	if u, err := strconv.Unquote(body); err == nil {
		body = u
	}
	body = strings.TrimSpace(body)
	body = reQuoted.ReplaceAllString(body, `"S"`)
	body = reBacktik.ReplaceAllString(body, "`S`")
	body = reIdent.ReplaceAllString(body, "X")
	body = regexp.MustCompile(`\s+`).ReplaceAllString(body, " ")
	body = reNum.ReplaceAllString(body, "N")
	return kind + ": " + truncate(body, 60)
}

// diffSignature describes how a round-trip diverged. It reports the
// first differing byte in context, with identifiers collapsed, plus
// whether the two sides agree once all whitespace is removed — a
// whitespace-only divergence points at Space handling, anything else at
// a dropped or duplicated node.
func diffSignature(want, got string) (bucket, detail string) {
	i := 0
	for i < len(want) && i < len(got) && want[i] == got[i] {
		i++
	}
	lo := max(0, i-30)
	wHi := min(len(want), i+30)
	gHi := min(len(got), i+30)
	detail = fmt.Sprintf("at %d\nwant: %q\ngot:  %q", i, want[lo:wHi], got[lo:gHi])

	kind := "text"
	if stripWS(want) == stripWS(got) {
		kind = "whitespace"
	}
	shape := func(s string) string {
		s = reIdent.ReplaceAllString(s, "X")
		s = reNum.ReplaceAllString(s, "N")
		return s
	}
	return fmt.Sprintf("%s | want %q got %q", kind,
		truncate(shape(want[i:wHi]), 24), truncate(shape(got[i:gHi]), 24)), detail
}

func stripWS(s string) string {
	var b strings.Builder
	for _, c := range s {
		if c != ' ' && c != '\t' && c != '\n' && c != '\r' {
			b.WriteRune(c)
		}
	}
	return b.String()
}

func truncate(s string, n int) string {
	if len(s) <= n {
		return s
	}
	return s[:n] + "…"
}

func writeReport(t *testing.T, outDir string, results []sweepResult) {
	counts := map[string]int{}
	buckets := map[string]map[string]int{} // class -> bucket -> count
	examples := map[string]sweepResult{}
	for _, r := range results {
		counts[r.Class]++
		if r.Bucket != "" {
			if buckets[r.Class] == nil {
				buckets[r.Class] = map[string]int{}
			}
			buckets[r.Class][r.Bucket]++
			if _, ok := examples[r.Class+"\x00"+r.Bucket]; !ok {
				examples[r.Class+"\x00"+r.Bucket] = r
			}
		}
	}

	var sb strings.Builder
	total := len(results)
	graded := counts["sound"] + counts["unsound"] + counts["parse_error"]
	fmt.Fprintf(&sb, "files walked : %d\n", total)
	fmt.Fprintf(&sb, "graded       : %d (skipped %d)\n", graded, counts["skipped"])
	fmt.Fprintf(&sb, "  sound      : %d\n", counts["sound"])
	fmt.Fprintf(&sb, "  unsound    : %d\n", counts["unsound"])
	fmt.Fprintf(&sb, "  parse error: %d\n\n", counts["parse_error"])

	for _, class := range []string{"parse_error", "unsound", "skipped"} {
		bs := buckets[class]
		if len(bs) == 0 {
			continue
		}
		keys := make([]string, 0, len(bs))
		for k := range bs {
			keys = append(keys, k)
		}
		sort.Slice(keys, func(i, j int) bool {
			if bs[keys[i]] != bs[keys[j]] {
				return bs[keys[i]] > bs[keys[j]]
			}
			return keys[i] < keys[j]
		})
		fmt.Fprintf(&sb, "=== %s buckets (%d distinct) ===\n", class, len(keys))
		for _, k := range keys {
			ex := examples[class+"\x00"+k]
			fmt.Fprintf(&sb, "%6d  %s\n          e.g. %s\n", bs[k], k, ex.Path)
		}
		sb.WriteString("\n")
	}

	reportPath := filepath.Join(outDir, "report.txt")
	if err := os.WriteFile(reportPath, []byte(sb.String()), 0o644); err != nil {
		t.Fatalf("write report: %v", err)
	}
	t.Logf("\n%s\nreport: %s", sb.String(), reportPath)
}
