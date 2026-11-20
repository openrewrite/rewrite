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
	"path"
	"reflect"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// probeTimeCap is how long a recipe's probes may take in the process before it
// stops being probed.
const probeTimeCap = 20 * time.Second

// probes is what the process has probed and how long each recipe's probes took.
var probes = newProbeLog()

type probeLog struct {
	mu    sync.Mutex
	ran   map[probeKey]bool
	spent map[string]time.Duration
}

type probeKey struct {
	recipe string
	source reflect.Type
	kind   string
}

func newProbeLog() *probeLog {
	return &probeLog{ran: map[probeKey]bool{}, spent: map[string]time.Duration{}}
}

// once returns the failure of try, which probes r on a source of type source in
// the way kind names, unless the process has run that probe before, whether it
// passed or failed, or r's probes have taken probeTimeCap.
func (l *probeLog) once(r recipe.Recipe, source reflect.Type, kind string, try func() string) string {
	key := probeKey{recipe: recipeWithOptions(r), source: source, kind: kind}
	l.mu.Lock()
	if l.ran[key] || l.spent[probeBudget(r)] > probeTimeCap {
		l.mu.Unlock()
		return ""
	}
	l.ran[key] = true
	l.mu.Unlock()
	start := time.Now()
	defer func() { l.charge(r, time.Since(start)) }()
	return try()
}

func (l *probeLog) charge(r recipe.Recipe, d time.Duration) {
	l.mu.Lock()
	defer l.mu.Unlock()
	l.spent[probeBudget(r)] += d
}

// recipeWithOptions names r by its type, name and option values, and those of
// the recipes it runs.
func recipeWithOptions(r recipe.Recipe) string {
	var b strings.Builder
	fmt.Fprintf(&b, "%T %s", r, r.Name())
	for _, o := range r.Options() {
		fmt.Fprintf(&b, " %s=%#v", o.Name, o.Value)
	}
	if list := r.RecipeList(); len(list) > 0 {
		b.WriteString(" [")
		for i, sub := range list {
			if i > 0 {
				b.WriteString(", ")
			}
			b.WriteString(recipeWithOptions(sub))
		}
		b.WriteString("]")
	}
	return b.String()
}

// probeBudget names what r's probes share a time cap by: its name, or for a
// recipe that runs others, theirs.
func probeBudget(r recipe.Recipe) string {
	list := r.RecipeList()
	if len(list) == 0 {
		return r.Name()
	}
	names := make([]string, len(list))
	for i, sub := range list {
		names[i] = probeBudget(sub)
	}
	return "[" + strings.Join(names, ",") + "]"
}

// probePanic is what a recipe panicked with when the harness ran it on sources
// the test did not give it.
type probePanic struct {
	value  any
	frames []runtime.Frame
}

// probe runs r over sources for up to cycles cycles, each on the output of the
// one before, with the messages start holds, and returns what r panicked with.
func probe(r recipe.Recipe, sources []java.Tree, start *recipe.ExecutionContext, cycles int) (p *probePanic) {
	defer func() {
		if v := recover(); v != nil {
			p = &probePanic{value: v, frames: panicFrames()}
		}
	}()
	ctx := recipe.NewExecutionContext()
	if start != nil {
		for k, v := range messages(start) {
			ctx.PutMessage(k, v)
		}
	}
	ctx.PutMessage(recipe.DataTableStoreKey, discardingStore{})
	for cycle := 1; cycle <= cycles; cycle++ {
		results, generated := runCycle(r, sources, ctx, nil)
		if len(changesOf(sources, results, generated)) == 0 {
			break
		}
		var next []java.Tree
		for _, t := range append(results, generated...) {
			if t != nil {
				next = append(next, t)
			}
		}
		sources = next
	}
	return nil
}

// panicFrames is the stack of the goroutine as a deferred function recovering
// from a panic sees it, from the panic down.
func panicFrames() []runtime.Frame {
	pcs := make([]uintptr, 128)
	frames := runtime.CallersFrames(pcs[:runtime.Callers(1, pcs)])
	var out []runtime.Frame
	panicking := false
	for {
		f, more := frames.Next()
		if panicking {
			out = append(out, f)
		} else {
			panicking = f.Function == "runtime.gopanic"
		}
		if !more {
			return out
		}
	}
}

// describe is the first line of what the recipe panicked with, the frame that
// panicked and, when that frame is not of the recipe's package, the first one
// that is.
func (p *probePanic) describe(r recipe.Recipe, indent string) string {
	var b strings.Builder
	b.WriteString(indent + firstLine(panicMessage(p.value)))
	packages := map[string]bool{}
	for _, rc := range recipeStack(r) {
		t := reflect.TypeOf(rc)
		for t.Kind() == reflect.Pointer {
			t = t.Elem()
		}
		packages[t.PkgPath()] = true
	}
	outsideRuntime := false
	for _, f := range p.frames {
		pkg := funcPackage(f.Function)
		if !outsideRuntime && pkg != "runtime" {
			outsideRuntime = true
			b.WriteString("\n" + indent + "    at " + frameString(f))
			if packages[pkg] {
				break
			}
		} else if outsideRuntime && packages[pkg] {
			b.WriteString("\n" + indent + "    at " + frameString(f))
			break
		}
	}
	return b.String()
}

func panicMessage(v any) string {
	switch x := v.(type) {
	case error:
		return x.Error()
	case string:
		return x
	}
	return fmt.Sprint(v)
}

func firstLine(s string) string {
	line, _, _ := strings.Cut(strings.TrimSpace(s), "\n")
	if r := []rune(line); len(r) > 200 {
		line = string(r[:200]) + "..."
	}
	return line
}

// funcPackage is the import path of the package a function of a stack frame,
// such as example.com/pkg.(*T).Method, belongs to.
func funcPackage(function string) string {
	slash := strings.LastIndex(function, "/")
	if dot := strings.Index(function[slash+1:], "."); dot >= 0 {
		return function[:slash+1+dot]
	}
	return function
}

func frameString(f runtime.Frame) string {
	return f.Function[strings.LastIndex(f.Function, "/")+1:] + "(" + path.Base(f.File) + ":" + strconv.Itoa(f.Line) + ")"
}

// carriedMarkerTypes names the types of the markers a probe's source has from the
// test's source, or "" when it has none.
func carriedMarkerTypes(markers []java.Marker) string {
	var names []string
	seen := map[string]bool{}
	for _, m := range markers {
		if n := markerTypeName(m); !seen[n] {
			seen[n] = true
			names = append(names, n)
		}
	}
	return strings.Join(names, ", ")
}
