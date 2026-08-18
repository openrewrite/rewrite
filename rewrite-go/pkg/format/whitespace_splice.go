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

package format

import (
	"reflect"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

var (
	spaceType    = reflect.TypeOf(java.Space{})
	markersType  = reflect.TypeOf(java.Markers{})
	javaTypeType = reflect.TypeOf((*java.JavaType)(nil)).Elem()
	treeType     = reflect.TypeOf((*java.Tree)(nil)).Elem()
)

// SpliceWhitespace returns original with every Space replaced by its
// counterpart in formatted, a tree parsed from a re-laid-out rendering of
// original. Node ids, type attribution and everything else come from original,
// which is rebuilt only along the paths that changed. When target is non-nil,
// only Spaces inside that subtree are replaced.
//
// A node the two trees shape differently keeps its original whitespace while
// its siblings are spliced; the second return value reports whether every node
// was reached, distinguishing a complete splice from a partial one.
func SpliceWhitespace(original, formatted java.Tree, target java.Tree) (java.Tree, bool) {
	s := &splicer{target: target, active: target == nil}
	out, changed := s.value(reflect.ValueOf(original), reflect.ValueOf(formatted))
	if !changed {
		return original, !s.diverged
	}
	return out.Interface().(java.Tree), !s.diverged
}

type splicer struct {
	target   java.Tree
	active   bool
	diverged bool
}

// value walks orig and fmtd in lockstep, returning the reconciled value and
// whether it differs from orig.
func (s *splicer) value(orig, fmtd reflect.Value) (reflect.Value, bool) {
	t := orig.Type()
	d := shapeOf(t)
	if t != fmtd.Type() {
		// A payload the trees disagree on (the `any` behind Literal.Value, say)
		// carries no layout, so only a node counts as a divergence.
		if d.isTree || shapeOf(fmtd.Type()).isTree {
			s.diverged = true
		}
		return orig, false
	}
	if !d.carriesSpace {
		return orig, false
	}

	switch {
	case d.isSpace:
		if !s.active || spaceContentEqual(orig.Interface().(java.Space), fmtd.Interface().(java.Space)) {
			return orig, false
		}
		return fmtd, true
	case d.isMarkers:
		if !s.active {
			return orig, false
		}
		return s.markers(orig, fmtd)
	}

	switch d.kind {
	case reflect.Interface:
		if orig.IsNil() != fmtd.IsNil() {
			s.diverged = true
			return orig, false
		}
		if orig.IsNil() {
			return orig, false
		}
		elem, changed := s.value(orig.Elem(), fmtd.Elem())
		if !changed {
			return orig, false
		}
		boxed := reflect.New(t).Elem()
		boxed.Set(elem)
		return boxed, true

	case reflect.Pointer:
		if orig.IsNil() != fmtd.IsNil() {
			s.diverged = true
			return orig, false
		}
		if orig.IsNil() {
			return orig, false
		}
		outer := s.active
		if !outer && d.isTree && orig.Interface() == s.target {
			s.active = true
		}
		elem, changed := s.value(orig.Elem(), fmtd.Elem())
		s.active = outer
		if !changed {
			return orig, false
		}
		p := reflect.New(elem.Type())
		p.Elem().Set(elem)
		return p, true

	case reflect.Struct:
		var out reflect.Value
		for _, i := range d.spaceFields {
			field, changed := s.value(orig.Field(i), fmtd.Field(i))
			if !changed {
				continue
			}
			if !out.IsValid() {
				out = reflect.New(t).Elem()
				out.Set(orig)
			}
			out.Field(i).Set(field)
		}
		if !out.IsValid() {
			return orig, false
		}
		return out, true

	case reflect.Slice:
		if orig.Len() != fmtd.Len() {
			if elem := shapeOf(t.Elem()); elem.isTree || elem.kind == reflect.Struct {
				s.diverged = true
			}
			return orig, false
		}
		var out reflect.Value
		for i := 0; i < orig.Len(); i++ {
			elem, changed := s.value(orig.Index(i), fmtd.Index(i))
			if !changed {
				continue
			}
			if !out.IsValid() {
				out = reflect.MakeSlice(t, orig.Len(), orig.Len())
				reflect.Copy(out, orig)
			}
			out.Index(i).Set(elem)
		}
		if !out.IsValid() {
			return orig, false
		}
		return out, true

	default:
		return orig, false
	}
}

// markers splices the layout a marker carries: some whitespace has no Space of
// its own and rides on a marker instead, as golang.TrailingComma holds the
// space around a composite literal's trailing comma. Semicolon is itself
// layout — go/printer writes a newline where an explicit `;` separated two
// statements — so it is added or dropped to match the formatted tree.
func (s *splicer) markers(orig, fmtd reflect.Value) (reflect.Value, bool) {
	om := orig.Interface().(java.Markers)
	fm := fmtd.Interface().(java.Markers)

	entries := om.Entries
	changed := false
	copyOnWrite := func() {
		if !changed {
			entries = append([]java.Marker(nil), om.Entries...)
			changed = true
		}
	}

	oIdx, fIdx := layoutPeers(om.Entries), layoutPeers(fm.Entries)
	if len(oIdx) == len(fIdx) {
		for k := range oIdx {
			ov := reflect.ValueOf(om.Entries[oIdx[k]])
			fv := reflect.ValueOf(fm.Entries[fIdx[k]])
			if ov.Type() != fv.Type() {
				continue
			}
			spliced, ch := s.value(ov, fv)
			if !ch {
				continue
			}
			copyOnWrite()
			entries[oIdx[k]] = spliced.Interface().(java.Marker)
		}
	}

	if has := java.HasMarker[golang.Semicolon](om); has != java.HasMarker[golang.Semicolon](fm) {
		copyOnWrite()
		if has {
			kept := entries[:0:0]
			for _, e := range entries {
				if _, isSemicolon := e.(golang.Semicolon); !isSemicolon {
					kept = append(kept, e)
				}
			}
			entries = kept
		} else {
			entries = append(entries, golang.NewSemicolon())
		}
	}

	if !changed {
		return orig, false
	}
	return reflect.ValueOf(java.Markers{ID: om.ID, Entries: entries}), true
}

// layoutPeers indexes the entries that pair up positionally between the two
// trees. Semicolons are excluded because they are added and dropped by
// formatting, which would misalign every marker after them.
func layoutPeers(entries []java.Marker) []int {
	var idx []int
	for i, e := range entries {
		if _, isSemicolon := e.(golang.Semicolon); !isSemicolon {
			idx = append(idx, i)
		}
	}
	return idx
}

// spaceContentEqual compares Spaces by content. java.SpaceEqual is identity
// based on the comments slice, which would report every commented Space of a
// freshly parsed tree as different.
func spaceContentEqual(a, b java.Space) bool {
	if a.Whitespace != b.Whitespace || len(a.Comments) != len(b.Comments) {
		return false
	}
	for i := range a.Comments {
		x, y := a.Comments[i], b.Comments[i]
		if x.Kind != y.Kind || x.Text != y.Text || x.Suffix != y.Suffix || x.Multiline != y.Multiline {
			return false
		}
	}
	return true
}
