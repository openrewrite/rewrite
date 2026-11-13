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
	"reflect"
	"regexp"
	"sort"
	"strconv"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

const (
	// maxAccumulatorObjects is how many objects of one accumulator the walk
	// reaches before it stops descending.
	maxAccumulatorObjects = 100_000
	maxAccumulatorPaths   = 5
)

var (
	heavyweightType = reflect.TypeOf((*recipe.Heavyweight)(nil)).Elem()
	recipeType      = reflect.TypeOf((*recipe.Recipe)(nil)).Elem()
	keyOrIndex      = regexp.MustCompile(`\[[^\]]*]`)
	// templatePackage holds GoPattern and GoTemplate, which the harness cannot
	// import, as the tests of that package use the harness.
	templatePackage = strings.TrimSuffix(visitorPackage, "/visitor") + "/template"
)

// accumulatorContents finds the recipe.Heavyweight objects, such as trees, that
// the accumulators of scanning recipes keep. The walk reads fields, slices,
// arrays and maps by reflection, and calls no method of the recipe's own.
type accumulatorContents struct {
	recipes  []string
	findings map[string][]accumulatorFinding
	shapes   map[string]bool
}

type accumulatorFinding struct {
	path, what, when string
}

// checkAccumulators walks the accumulator of each scanning recipe in recipes,
// as it is when.
func (c *runChecks) checkAccumulators(recipes []recipe.Recipe, accs []any, when string) {
	if c == nil || !c.lightweightAccumulators {
		return
	}
	for i, rc := range recipes {
		if _, ok := rc.(recipe.ScanningRecipe); ok && accs[i] != nil {
			c.accumulators.walk(rc.Name(), accs[i], when)
		}
	}
}

func (a *accumulatorContents) walk(recipeName string, acc any, when string) {
	// an accumulator the walk cannot read is left alone
	defer func() { _ = recover() }()
	w := &accumulatorWalk{contents: a, recipe: recipeName, when: when, visited: map[identity]bool{},
		budget: maxAccumulatorObjects}
	root := reflect.ValueOf(acc)
	w.visit(root, nil, rootName(root.Type()))
	// breadth first, so that the path to what the accumulator keeps is a short one
	for len(w.queue) > 0 {
		n := w.queue[0]
		w.queue = w.queue[1:]
		w.expand(n)
	}
}

type accumulatorWalk struct {
	contents     *accumulatorContents
	recipe, when string
	visited      map[identity]bool
	queue        []*accumulatorNode
	budget       int
}

// accumulatorNode is a value the walk reached and the step its parent took to it.
type accumulatorNode struct {
	value  reflect.Value
	parent *accumulatorNode
	step   string
}

type identity struct {
	t   reflect.Type
	ptr uintptr
	len int
}

func (w *accumulatorWalk) visit(v reflect.Value, parent *accumulatorNode, step string) {
	for v.IsValid() {
		if (v.Kind() == reflect.Interface || v.Kind() == reflect.Pointer) && v.IsNil() {
			return
		}
		if heavyweight(v.Type()) {
			w.found(&accumulatorNode{value: v, parent: parent, step: step})
			return
		}
		if v.Kind() == reflect.Pointer {
			id := identity{t: v.Type(), ptr: v.Pointer()}
			if w.visited[id] {
				return
			}
			w.visited[id] = true
		} else if v.Kind() != reflect.Interface {
			break
		}
		// what an interface or pointer holds is reached in the same step
		v = v.Elem()
	}
	if !v.IsValid() || accumulatorLeaf(v.Type()) {
		return
	}
	if v.Kind() == reflect.Map || v.Kind() == reflect.Slice {
		if v.Len() == 0 {
			return
		}
		id := identity{t: v.Type(), ptr: v.Pointer(), len: v.Len()}
		if w.visited[id] {
			return
		}
		w.visited[id] = true
	}
	if w.budget > 0 {
		w.budget--
		w.queue = append(w.queue, &accumulatorNode{value: v, parent: parent, step: step})
	}
}

func (w *accumulatorWalk) expand(n *accumulatorNode) {
	v := n.value
	switch v.Kind() {
	case reflect.Struct:
		for i := 0; i < v.NumField(); i++ {
			w.visit(v.Field(i), n, "."+v.Type().Field(i).Name)
		}
	case reflect.Slice, reflect.Array:
		for i := 0; i < v.Len(); i++ {
			w.visit(v.Index(i), n, "["+strconv.Itoa(i)+"]")
		}
	case reflect.Map:
		type entry struct {
			key, value reflect.Value
			step       string
		}
		var entries []entry
		for it := v.MapRange(); it.Next(); {
			entries = append(entries, entry{it.Key(), it.Value(), "[" + accumulatorKey(it.Key()) + "]"})
		}
		sort.SliceStable(entries, func(i, j int) bool { return entries[i].step < entries[j].step })
		for _, e := range entries {
			w.visit(e.key, n, e.step)
			w.visit(e.value, n, e.step)
		}
	}
}

func (w *accumulatorWalk) found(n *accumulatorNode) {
	var steps []string
	for x := n; x != nil; x = x.parent {
		steps = append(steps, x.step)
	}
	var path strings.Builder
	for i := len(steps) - 1; i >= 0; i-- {
		path.WriteString(steps[i])
	}
	a := w.contents
	what := accumulatorObject(n.value)
	shape := w.recipe + " " + keyOrIndex.ReplaceAllString(path.String(), "[]") + " -> " + what
	if a.shapes[shape] {
		return
	}
	if a.findings == nil {
		a.findings = map[string][]accumulatorFinding{}
		a.shapes = map[string]bool{}
	}
	a.shapes[shape] = true
	if _, ok := a.findings[w.recipe]; !ok {
		a.recipes = append(a.recipes, w.recipe)
	}
	a.findings[w.recipe] = append(a.findings[w.recipe], accumulatorFinding{path: path.String(), what: what, when: w.when})
}

// heavyweight reports whether a value of type t is too costly to keep for a
// whole cycle, as a recipe.Heavyweight is.
func heavyweight(t reflect.Type) bool {
	return implementedBy(t, heavyweightType)
}

// accumulatorLeaf reports whether the walk leaves a value of type t alone: one
// that holds no references, type attribution, which refers to no tree, a recipe,
// which lives for the whole run anyway, or a pattern or template, which keeps
// the tree of its own code only.
func accumulatorLeaf(t reflect.Type) bool {
	switch t.Kind() {
	case reflect.Slice, reflect.Array:
		return holdsNoReference(t.Elem())
	case reflect.Map:
		return holdsNoReference(t.Key()) && holdsNoReference(t.Elem())
	case reflect.Struct, reflect.Pointer, reflect.Interface:
	default:
		return true
	}
	if t.Kind() == reflect.Pointer {
		t = t.Elem()
	}
	return implementedBy(t, javaTypeIface) || implementedBy(t, recipeType) ||
		t.PkgPath() == templatePackage && (t.Name() == "GoPattern" || t.Name() == "GoTemplate")
}

func holdsNoReference(t reflect.Type) bool {
	switch t.Kind() {
	case reflect.Struct, reflect.Pointer, reflect.Interface, reflect.Slice, reflect.Array, reflect.Map:
		return false
	}
	return true
}

// implementedBy reports whether t or a pointer to it implements iface.
func implementedBy(t, iface reflect.Type) bool {
	return t.Implements(iface) || t.Kind() != reflect.Interface && reflect.PointerTo(t).Implements(iface)
}

// rootName names an accumulator by its type, spelled out when it has no name.
func rootName(t reflect.Type) string {
	for t.Kind() == reflect.Pointer {
		t = t.Elem()
	}
	if t.Name() == "" {
		return t.String()
	}
	return typeName(t)
}

// accumulatorObject names what the walk found, with its path for a source file.
func accumulatorObject(v reflect.Value) string {
	for v.Kind() == reflect.Interface {
		v = v.Elem()
	}
	name := typeName(v.Type())
	if path := sourcePathField(v); path != "" {
		return name + " " + path
	}
	return name
}

func sourcePathField(v reflect.Value) string {
	for v.Kind() == reflect.Pointer || v.Kind() == reflect.Interface {
		if v.IsNil() {
			return ""
		}
		v = v.Elem()
	}
	if v.Kind() != reflect.Struct {
		return ""
	}
	if f := v.FieldByName("SourcePath"); f.IsValid() && f.Kind() == reflect.String {
		return f.String()
	}
	return ""
}

// accumulatorKey prints a map key without calling a method of its own.
func accumulatorKey(k reflect.Value) string {
	for k.Kind() == reflect.Interface || k.Kind() == reflect.Pointer {
		if k.IsNil() {
			return "nil"
		}
		if path := sourcePathField(k); path != "" && heavyweight(k.Type()) {
			return path
		}
		k = k.Elem()
	}
	var s string
	switch k.Kind() {
	case reflect.String:
		s = k.String()
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
		s = strconv.FormatInt(k.Int(), 10)
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64, reflect.Uintptr:
		s = strconv.FormatUint(k.Uint(), 10)
	case reflect.Bool:
		s = strconv.FormatBool(k.Bool())
	case reflect.Float32, reflect.Float64:
		s = strconv.FormatFloat(k.Float(), 'g', -1, 64)
	default:
		if path := sourcePathField(k); path != "" {
			return path
		}
		return typeName(k.Type())
	}
	if r := []rune(s); len(r) > 60 {
		s = string(r[:60]) + "..."
	}
	return s
}

func (a *accumulatorContents) failure() string {
	if len(a.recipes) == 0 {
		return ""
	}
	var b strings.Builder
	b.WriteString("A ScanningRecipe's accumulator should keep the values the recipe needs, such as names, paths, " +
		"positions and ids, and no recipe.Heavyweight objects, such as trees, cursors, visitors, execution contexts " +
		"and module graphs:\n")
	for _, r := range a.recipes {
		fmt.Fprintf(&b, "  %s keeps\n", r)
		for i, f := range a.findings[r] {
			if i == maxAccumulatorPaths {
				fmt.Fprintf(&b, "    and %d more\n", len(a.findings[r])-maxAccumulatorPaths)
				break
			}
			fmt.Fprintf(&b, "    %s -> %s, %s\n", f.path, f.what, f.when)
		}
	}
	b.WriteString("An accumulator stays in memory from the first source file scanned until the cycle ends, so " +
		"what it keeps for each source file adds up over a large repository. A GoResolutionResult can hold the " +
		"module graph of a whole build, while small markers such as GoProject are fine to keep. A recipe that " +
		"must keep a Heavyweight object opts out with spec.CheckLightweightAccumulators = false.")
	return b.String()
}
