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

package template

import (
	"reflect"
	"sync"

	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// fieldRule is how one field of a node takes part in a comparison.
type fieldRule int

const (
	ruleSkip     fieldRule = iota
	ruleNode               // implements java.J, or its address does
	ruleTypeSlot           // implements java.JavaType
	rulePadded             // RightPadded / LeftPadded / Container
	ruleSlice
	ruleScalar
)

// fieldStep is a field's rule, resolved once. elem carries the rule for a
// slice's element type, so the walk resolves nothing per element.
type fieldStep struct {
	index int
	rule  fieldRule
	elem  *fieldStep
}

type typePlan struct{ steps []fieldStep }

var (
	planCache    sync.Map // reflect.Type -> *typePlan
	uuidType     = reflect.TypeOf(uuid.UUID{})
	spaceType    = reflect.TypeOf(java.Space{})
	markersType  = reflect.TypeOf(java.Markers{})
	jType        = reflect.TypeOf((*java.J)(nil)).Elem()
	javaTypeType = reflect.TypeOf((*java.JavaType)(nil)).Elem()
)

// fileFields describe the file a compilation unit came from rather than the
// tree in it, so a pattern parsed from a scaffold can still match source.
var fileFields = map[string]bool{
	"SourcePath": true, "CharsetBomMarked": true, "EOF": true,
}

func planFor(t reflect.Type) *typePlan {
	if cached, ok := planCache.Load(t); ok {
		return cached.(*typePlan)
	}
	plan := &typePlan{}
	for i := 0; i < t.NumField(); i++ {
		f := t.Field(i)
		if f.PkgPath != "" || fileFields[f.Name] {
			continue
		}
		step := fieldStep{index: i, rule: ruleFor(f.Type)}
		if step.rule == ruleSkip {
			continue
		}
		if step.rule == ruleSlice {
			elem := fieldStep{rule: ruleFor(f.Type.Elem())}
			step.elem = &elem
		}
		plan.steps = append(plan.steps, step)
	}
	planCache.Store(t, plan)
	return plan
}

func ruleFor(t reflect.Type) fieldRule {
	switch t {
	case uuidType, spaceType, markersType:
		return ruleSkip
	}
	if t.Implements(javaTypeType) {
		return ruleTypeSlot
	}
	if t.Implements(jType) || reflect.PointerTo(t).Implements(jType) {
		return ruleNode
	}
	deref := t
	if deref.Kind() == reflect.Pointer {
		deref = deref.Elem()
	}
	if deref.Kind() == reflect.Struct && wrapsElement(deref) {
		return rulePadded
	}
	if t.Kind() == reflect.Slice {
		return ruleSlice
	}
	return ruleScalar
}

// A padded wrapper is recognised by the field holding what it wraps, so one
// rule covers RightPadded, LeftPadded and Container at every instantiation of
// their element type.
func wrapsElement(t reflect.Type) bool {
	_, single := t.FieldByName("Element")
	_, many := t.FieldByName("Elements")
	return single || many
}

// matchFields compares two nodes of the same concrete type field by field.
func (c *patternComparator) matchFields(pattern, candidate java.J) bool {
	pv, cv := reflect.ValueOf(pattern), reflect.ValueOf(candidate)
	if pv.Kind() != reflect.Pointer || pv.IsNil() || cv.IsNil() {
		return pv.IsNil() && cv.IsNil()
	}
	pv, cv = pv.Elem(), cv.Elem()
	for _, step := range planFor(pv.Type()).steps {
		if !c.matchValue(step, pv.Field(step.index), cv.Field(step.index)) {
			return false
		}
	}
	return true
}

func (c *patternComparator) matchValue(step fieldStep, pv, cv reflect.Value) bool {
	switch step.rule {
	case ruleSkip, ruleTypeSlot:
		return true
	case ruleNode:
		return c.matchNode(asJ(pv), asJ(cv))
	case rulePadded:
		return c.matchPadded(pv, cv)
	case ruleSlice:
		return c.matchSlice(*step.elem, pv, cv)
	default:
		return pv.Equal(cv)
	}
}

// asJ reads a field as a J. A node held by value has its address taken, since
// the interface is satisfied by pointer receivers, and a nil pointer is
// returned as a nil J rather than an interface holding one.
func asJ(v reflect.Value) java.J {
	if v.Kind() == reflect.Interface {
		if v.IsNil() {
			return nil
		}
		v = v.Elem()
	}
	if v.Kind() != reflect.Pointer && v.CanAddr() {
		v = v.Addr()
	}
	if v.Kind() == reflect.Pointer && v.IsNil() {
		return nil
	}
	j, _ := v.Interface().(java.J)
	return j
}

func (c *patternComparator) matchPadded(pv, cv reflect.Value) bool {
	if pv.Kind() == reflect.Pointer {
		if pv.IsNil() || cv.IsNil() {
			return pv.IsNil() && cv.IsNil()
		}
		pv, cv = pv.Elem(), cv.Elem()
	}
	if elements := pv.FieldByName("Elements"); elements.IsValid() {
		elem := fieldStep{rule: ruleFor(elements.Type().Elem())}
		return c.matchSlice(elem, elements, cv.FieldByName("Elements"))
	}
	inner := pv.FieldByName("Element")
	return c.matchValue(fieldStep{rule: ruleFor(inner.Type())}, inner, cv.FieldByName("Element"))
}

// matchSlice compares two slices, letting a variadic capture absorb a run
// where the elements are subtrees. Anything else compares one to one.
func (c *patternComparator) matchSlice(elem fieldStep, pv, cv reflect.Value) bool {
	if elem.rule == ruleNode || elem.rule == rulePadded {
		return c.matchRun(elem, pv, cv)
	}
	if pv.Len() != cv.Len() {
		return false
	}
	for i := range pv.Len() {
		if !c.matchValue(elem, pv.Index(i), cv.Index(i)) {
			return false
		}
	}
	return true
}

// matchRun compares two slices of subtrees, letting a variadic capture in the
// pattern absorb a run of candidate elements. One variadic makes the run's
// length arithmetic — every other pattern element takes one — so the elements
// after it match positionally. Two leave the split undetermined, and fail.
func (c *patternComparator) matchRun(elem fieldStep, pv, cv reflect.Value) bool {
	at, ok := c.reflectVariadicIndex(elem, pv)
	if !ok {
		return false
	}
	if at < 0 {
		return pv.Len() == cv.Len() && c.matchSpan(elem, pv, cv, 0, 0, pv.Len())
	}

	run := cv.Len() - (pv.Len() - 1)
	if run < 0 {
		return false
	}
	name, _ := placeholderName(elemJ(elem, pv.Index(at)))
	if !c.captures[name].allowsCount(run) {
		return false
	}
	if !c.matchSpan(elem, pv, cv, 0, 0, at) {
		return false
	}
	if !c.matchSpan(elem, pv, cv, at+1, at+run, pv.Len()-at-1) {
		return false
	}
	values := make([]java.J, run)
	for i := range run {
		values[i] = elemJ(elem, cv.Index(at+i))
	}
	return c.bindRun(name, values)
}

func (c *patternComparator) matchSpan(elem fieldStep, pv, cv reflect.Value, from, to, n int) bool {
	for i := range n {
		if !c.matchValue(elem, pv.Index(from+i), cv.Index(to+i)) {
			return false
		}
	}
	return true
}

func (c *patternComparator) reflectVariadicIndex(elem fieldStep, pv reflect.Value) (int, bool) {
	at := -1
	for i := range pv.Len() {
		name, ok := placeholderName(elemJ(elem, pv.Index(i)))
		if !ok || !c.isVariadic(name) {
			continue
		}
		if at >= 0 {
			return 0, false
		}
		at = i
	}
	return at, true
}

// elemJ reads the subtree a slice element stands for, reaching through a
// padded wrapper when the slice holds one.
func elemJ(elem fieldStep, v reflect.Value) java.J {
	if elem.rule == rulePadded {
		if v.Kind() == reflect.Pointer {
			if v.IsNil() {
				return nil
			}
			v = v.Elem()
		}
		inner := v.FieldByName("Element")
		if !inner.IsValid() {
			return nil
		}
		return asJ(inner)
	}
	return asJ(v)
}
