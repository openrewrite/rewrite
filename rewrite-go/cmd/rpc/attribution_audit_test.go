//go:build attributionaudit

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

package main

import (
	"encoding/json"
	"fmt"

	"github.com/google/uuid"
	"os"
	"path/filepath"
	"reflect"
	"sort"
	"strings"
	"testing"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// TestAttributionAudit parses a real repository through the ParseProject RPC
// — the only path that resolves cross-file and third-party types — and counts,
// per construct kind, how many nodes came back with a type a recipe can match
// on. Buckets are syntactic, so an unattributed node stays in the bucket its
// source spelling puts it in.
//
//	GO_AUDIT_REPOS=/path/to/repo1,/path/to/repo2 \
//	  go test -tags attributionaudit ./cmd/rpc/ -run TestAttributionAudit -timeout 30m
func TestAttributionAudit(t *testing.T) {
	repos := os.Getenv("GO_AUDIT_REPOS")
	if repos == "" {
		t.Skip("GO_AUDIT_REPOS not set")
	}
	for _, repo := range strings.Split(repos, ",") {
		repo = strings.TrimSpace(repo)
		if repo == "" {
			continue
		}
		t.Run(filepath.Base(repo), func(t *testing.T) {
			t.Logf("\n%s", auditRepo(t, repo).report(envInt("GO_AUDIT_EXAMPLES", 6)))
		})
	}
}

func envInt(name string, dflt int) int {
	var n int
	if _, err := fmt.Sscanf(os.Getenv(name), "%d", &n); err != nil || n <= 0 {
		return dflt
	}
	return n
}

func auditRepo(t *testing.T, repo string) *census {
	t.Helper()
	s, _ := newTestServer(t)
	params, err := json.Marshal(parseProjectRequest{ProjectPath: repo, RelativeTo: &repo})
	if err != nil {
		t.Fatalf("marshal params: %v", err)
	}
	if rpcErr := mustParseProject(s, params); rpcErr != nil {
		t.Fatalf("handleParseProject: %v", rpcErr.Message)
	}

	c := newCensus()
	cus := make([]*golang.CompilationUnit, 0, len(s.localObjects))
	for _, obj := range s.localObjects {
		if cu, ok := obj.(*golang.CompilationUnit); ok {
			cus = append(cus, cu)
		}
	}
	sort.Slice(cus, func(i, j int) bool { return cus[i].SourcePath < cus[j].SourcePath })
	for _, cu := range cus {
		c.files++
		av := &auditVisitor{census: c, path: cu.SourcePath, packages: packageAliasesOf(cu)}
		// A package clause names the file's own package, which binds no value.
		if cu.PackageDecl != nil && cu.PackageDecl.Element != nil {
			av.packageClause = cu.PackageDecl.Element.ID
		}
		w := visitor.Init(av)
		w.Visit(cu, nil)
	}
	return c
}

func mustParseProject(s *server, params json.RawMessage) *rpcError {
	_, rpcErr := s.handleParseProject(params)
	return rpcErr
}

// packageAliasesOf names every identifier in the file that qualifies a
// package rather than a value, so `fmt.Println` is bucketed apart from
// `buf.WriteString` without consulting either one's attribution.
func packageAliasesOf(cu *golang.CompilationUnit) map[string]bool {
	aliases := map[string]bool{}
	if cu.Imports == nil {
		return aliases
	}
	for _, imp := range cu.Imports.Elements {
		e := imp.Element
		if e.Alias != nil && e.Alias.Element != nil {
			aliases[e.Alias.Element.Name] = true
			continue
		}
		lit, ok := e.Qualid.(*java.Literal)
		if !ok {
			continue
		}
		path := strings.Trim(lit.Source, "`\"")
		// A package's name usually matches the last path segment; a
		// `/v2` suffix names the major version, not the package.
		seg := path[strings.LastIndexByte(path, '/')+1:]
		if strings.HasPrefix(seg, "v") && strings.Trim(seg[1:], "0123456789") == "" {
			if i := strings.LastIndexByte(strings.TrimSuffix(path, "/"+seg), '/'); i >= 0 {
				seg = path[i+1 : len(path)-len(seg)-1]
			}
		}
		aliases[seg] = true
	}
	return aliases
}

// ---------------------------------------------------------------- census

type bucket struct {
	attributed   int
	unattributed int
	examples     []string
	seen         map[string]bool
}

type census struct {
	files   int
	order   []string
	buckets map[string]*bucket
}

func newCensus() *census { return &census{buckets: map[string]*bucket{}} }

func (c *census) get(name string) *bucket {
	b := c.buckets[name]
	if b == nil {
		b = &bucket{seen: map[string]bool{}}
		c.buckets[name] = b
		c.order = append(c.order, name)
	}
	return b
}

func (c *census) record(name string, ok bool, example string) {
	b := c.get(name)
	if ok {
		b.attributed++
		return
	}
	b.unattributed++
	if !b.seen[example] {
		b.seen[example] = true
		b.examples = append(b.examples, example)
	}
}

func (c *census) report(maxExamples int) string {
	names := append([]string(nil), c.order...)
	sort.Strings(names)
	var sb strings.Builder
	fmt.Fprintf(&sb, "compilation units: %d\n\n", c.files)
	fmt.Fprintf(&sb, "%-44s %11s %13s %9s\n", "construct", "attributed", "unattributed", "attr%")
	for _, n := range names {
		b := c.buckets[n]
		total := b.attributed + b.unattributed
		pct := 100.0
		if total > 0 {
			pct = 100 * float64(b.attributed) / float64(total)
		}
		fmt.Fprintf(&sb, "%-44s %11d %13d %8.1f%%\n", n, b.attributed, b.unattributed, pct)
	}
	fmt.Fprintf(&sb, "\nunattributed examples\n")
	for _, n := range names {
		b := c.buckets[n]
		if len(b.examples) == 0 {
			continue
		}
		sort.Strings(b.examples)
		fmt.Fprintf(&sb, "  %s\n", n)
		for _, e := range b.examples[:min(len(b.examples), maxExamples)] {
			fmt.Fprintf(&sb, "      %s\n", e)
		}
	}
	return sb.String()
}

// ---------------------------------------------------------------- predicates

// usable reports whether a type slot carries something a recipe can match on.
// A nil pointer boxed in the interface reads as present, so it counts as
// absent, as does a class with no name to match.
func usable(t java.JavaType) bool {
	if isAbsent(t) || java.IsUnknown(t) {
		return false
	}
	switch v := t.(type) {
	case *java.JavaTypeClass:
		return v.FullyQualifiedName != ""
	case *java.JavaTypeShallowClass:
		return v.FullyQualifiedName != ""
	case *java.JavaTypeParameterized:
		return usable(v.Type)
	case *java.JavaTypeArray:
		return usable(v.ElemType)
	case *java.JavaTypeMethod:
		// A func type is an unnamed signature and declares nothing; only a
		// call has to name what it resolved to.
		if v.Name == "" {
			return v.ReturnType != nil
		}
		return v.DeclaringType != nil && !java.IsUnknown(v.DeclaringType)
	case *java.JavaTypeVariable:
		return usable(v.Type)
	}
	return true
}

func isAbsent(t java.JavaType) bool {
	if t == nil {
		return true
	}
	rv := reflect.ValueOf(t)
	return rv.Kind() == reflect.Ptr && rv.IsNil()
}

func fqnOf(t java.JavaType) string {
	if isAbsent(t) {
		return "<nil>"
	}
	if fq, ok := t.(java.FullyQualified); ok {
		return java.FQNOf(fq)
	}
	if p, ok := t.(*java.JavaTypePrimitive); ok {
		return p.Keyword
	}
	return fmt.Sprintf("%T", t)
}

// ---------------------------------------------------------------- visitor

// goBuiltins are the predeclared functions, which Go resolves to a
// *types.Builtin rather than to a declared func.
var goBuiltins = map[string]bool{
	"append": true, "cap": true, "clear": true, "close": true, "complex": true,
	"copy": true, "delete": true, "imag": true, "len": true, "make": true,
	"max": true, "min": true, "new": true, "panic": true, "print": true,
	"println": true, "real": true, "recover": true,
}

type auditVisitor struct {
	visitor.GoVisitor
	census   *census
	path     string
	packages map[string]bool
	// roles names the type-expression contexts an identifier can sit in, so
	// `int` in `map[string]int` is counted apart from `int` in `var n int`.
	roles         []string
	packageClause uuid.UUID
}

func (v *auditVisitor) at(what string) string { return v.path + ": " + what }

func (v *auditVisitor) role() string {
	if len(v.roles) == 0 {
		return "expression"
	}
	return v.roles[len(v.roles)-1]
}

func (v *auditVisitor) inRole(role string, f func() java.J) java.J {
	v.roles = append(v.roles, role)
	defer func() { v.roles = v.roles[:len(v.roles)-1] }()
	return f()
}

func (v *auditVisitor) VisitMethodInvocation(mi *java.MethodInvocation, p any) java.J {
	name := "?"
	if mi.Name != nil {
		name = mi.Name.Name
	}
	kind, recv := v.calleeKind(mi, name)
	v.census.record(kind, usable(mi.MethodType), v.at(recv+name+"()"))
	if mi.TypeParameters != nil {
		v.census.record("generic call, explicit type args", usable(mi.MethodType), v.at(recv+name+"[...]()"))
	}
	if mi.MethodType != nil {
		lens := "call: receiver is a concrete type"
		if cls := asClass(mi.MethodType.DeclaringType); cls != nil && cls.Kind == "Interface" {
			lens = "call: receiver is an interface"
		}
		v.census.record(lens, usable(mi.MethodType), v.at(recv+name+"()"))
	}
	return v.GoVisitor.VisitMethodInvocation(mi, p)
}

// calleeKind buckets a call by how its source spells the callee, so an
// unresolved call still lands where its shape puts it.
func (v *auditVisitor) calleeKind(mi *java.MethodInvocation, name string) (kind, recvText string) {
	if mi.Select == nil {
		if goBuiltins[name] {
			return "call: builtin", ""
		}
		return "call: same-package function", ""
	}
	switch sel := mi.Select.Element.(type) {
	case *java.Identifier:
		if v.packages[sel.Name] {
			return "call: package function", sel.Name + "."
		}
		return "call: method via identifier", sel.Name + "."
	case *java.FieldAccess:
		return "call: method via field access", exprText(sel) + "."
	case *java.MethodInvocation:
		return "call: method via call result", exprText(sel) + "."
	default:
		return "call: method via other expression", exprText(sel) + "."
	}
}

func (v *auditVisitor) VisitComposite(comp *golang.Composite, p any) java.J {
	// An anonymous struct type has members but no name to match on, so it is
	// counted apart from a literal of a declared type.
	kind := "composite literal (named type)"
	if cls := asClass(elemOf(comp.Type)); cls != nil && cls.FullyQualifiedName == "" {
		kind = "composite literal (anonymous struct)"
	}
	v.census.record(kind, usable(comp.Type), v.at(exprText(comp.TypeExpr)+"{...}"))
	structFQN := fqnOf(comp.Type)
	for _, e := range comp.Elements.Elements {
		kv, ok := e.Element.(*golang.KeyValue)
		if !ok {
			continue
		}
		if key, ok := kv.Key.(*java.Identifier); ok {
			v.census.record("composite literal field key", usable(key.FieldType), v.at(structFQN+"{"+key.Name+": ...}"))
		}
	}
	return v.GoVisitor.VisitComposite(comp, p)
}

func (v *auditVisitor) VisitStructType(st *golang.StructType, p any) java.J {
	return v.inRole("struct field", func() java.J { return v.GoVisitor.VisitStructType(st, p) })
}

func (v *auditVisitor) VisitInterfaceType(it *golang.InterfaceType, p any) java.J {
	return v.inRole("interface member", func() java.J { return v.GoVisitor.VisitInterfaceType(it, p) })
}

// A func signature's parameters and results are VariableDeclarations too, so
// they must not read as the fields of an enclosing struct.
func (v *auditVisitor) VisitFuncType(ft *golang.FuncType, p any) java.J {
	return v.inRole("func signature", func() java.J { return v.GoVisitor.VisitFuncType(ft, p) })
}

func (v *auditVisitor) VisitTypeList(tl *golang.TypeList, p any) java.J {
	return v.inRole("func signature", func() java.J { return v.GoVisitor.VisitTypeList(tl, p) })
}

func (v *auditVisitor) VisitVariableDeclarations(vd *java.VariableDeclarations, p any) java.J {
	if v.role() == "struct field" {
		// An embedded field spells only its type, so the parser leaves the
		// declarator unnamed and the field it declares hangs off the type
		// expression's own identifier.
		if embedded(vd) {
			v.census.record("struct embedded field", usable(fieldTypeOfExpr(vd.TypeExpr)),
				v.at("embedded "+exprText(vd.TypeExpr)))
			return v.GoVisitor.VisitVariableDeclarations(vd, p)
		}
		for _, decl := range vd.Variables {
			if n := decl.Element.Name; n != nil {
				v.census.record("struct field declaration", usable(n.FieldType),
					v.at("field "+n.Name+" "+exprText(vd.TypeExpr)))
			}
		}
	}
	return v.GoVisitor.VisitVariableDeclarations(vd, p)
}

func (v *auditVisitor) VisitTypeAssertion(ta *golang.TypeAssertion, p any) java.J {
	asserted := ""
	if ta.AssertedType != nil {
		asserted = exprText(ta.AssertedType.Tree.Element)
	}
	// `x.(type)` heads a type switch, whose whole point is that x has no one
	// type here; each case clause names the type that holds inside it.
	kind := "type assertion"
	if asserted == "type" {
		kind = "type switch guard (no single type)"
	}
	v.census.record(kind, usable(ta.Type), v.at(exprText(ta.Left.Element)+".("+asserted+")"))
	return v.GoVisitor.VisitTypeAssertion(ta, p)
}

func (v *auditVisitor) VisitParameterizedType(pt *java.ParameterizedType, p any) java.J {
	v.census.record("generic instantiation (type)", usable(pt.Type), v.at(exprText(pt.Clazz)+"[...]"))
	return v.GoVisitor.VisitParameterizedType(pt, p)
}

// golang.MapType and golang.Channel model a type expression and hold no type
// slot at all, so what they would attribute is measured on the element
// identifiers instead.
func (v *auditVisitor) VisitMapType(mt *golang.MapType, p any) java.J {
	v.census.record("map type expression (no type slot)", false, v.at(exprText(mt)))
	return v.inRole("map key/element", func() java.J { return v.GoVisitor.VisitMapType(mt, p) })
}

func (v *auditVisitor) VisitChannel(ch *golang.Channel, p any) java.J {
	v.census.record("channel type expression (no type slot)", false, v.at(exprText(ch)))
	return v.inRole("channel element", func() java.J { return v.GoVisitor.VisitChannel(ch, p) })
}

func (v *auditVisitor) VisitMultiAssignment(ma *golang.MultiAssignment, p any) java.J {
	if len(ma.Values) == 1 && java.HasMarker[golang.ShortVarDecl](ma.Markers) {
		rhs := exprText(ma.Values[0].Element)
		for _, lhs := range ma.Variables {
			id, ok := lhs.Element.(*java.Identifier)
			if !ok || id.Name == "_" {
				continue
			}
			v.census.record(":= from a multi-value return", usable(id.Type), v.at(id.Name+" := "+rhs))
		}
	}
	return v.GoVisitor.VisitMultiAssignment(ma, p)
}

// Every remaining identifier, bucketed by the type expression it sits in, so
// the per-construct counts sit against a baseline.
func (v *auditVisitor) VisitIdentifier(id *java.Identifier, p any) java.J {
	if id.Name != "_" && id.Name != "" && id.ID != v.packageClause {
		v.census.record("identifier in "+v.role(), usable(id.Type), v.at(id.Name))
	}
	return id
}

func embedded(vd *java.VariableDeclarations) bool {
	return len(vd.Variables) == 0 ||
		(len(vd.Variables) == 1 && vd.Variables[0].Element.Name != nil && vd.Variables[0].Element.Name.Name == "")
}

// fieldTypeOfExpr is the field an embedded type expression declares, which
// hangs off the identifier naming the type.
func fieldTypeOfExpr(e java.Expression) java.JavaType {
	switch n := e.(type) {
	case *java.Identifier:
		return n.FieldType
	case *java.FieldAccess:
		return n.Name.Element.FieldType
	case *golang.PointerType:
		return fieldTypeOfExpr(n.Elem)
	case *java.ParameterizedType:
		return fieldTypeOfExpr(n.Clazz)
	}
	return nil
}

// ---------------------------------------------------------------- helpers

// elemOf unwraps a slice or array literal to the type of its elements, which is
// what `[]struct{...}{...}` names.
func elemOf(t java.JavaType) java.JavaType {
	if a, ok := t.(*java.JavaTypeArray); ok {
		return elemOf(a.ElemType)
	}
	return t
}

func asClass(t java.JavaType) *java.JavaTypeClass {
	switch c := t.(type) {
	case *java.JavaTypeClass:
		return c
	case *java.JavaTypeShallowClass:
		return &c.JavaTypeClass
	}
	return nil
}

// exprText renders just enough of an expression to tell one example from
// another in the report.
func exprText(e java.Expression) string {
	switch n := e.(type) {
	case nil:
		return "<untyped>"
	case *java.Identifier:
		return n.Name
	case *java.FieldAccess:
		return exprText(n.Target) + "." + n.Name.Element.Name
	case *java.MethodInvocation:
		if n.Name != nil {
			return exprText0(n.Select) + n.Name.Name + "()"
		}
	case *java.ParameterizedType:
		return exprText(n.Clazz) + "[...]"
	case *java.ArrayType:
		return "[]" + exprText(n.ElementType)
	case *golang.ArrayType:
		return "[N]" + exprText(n.ElementType)
	case *golang.PointerType:
		return "*" + exprText(n.Elem)
	case *golang.MapType:
		return "map[" + exprText(n.Key.Element) + "]" + exprText(n.Value)
	case *golang.Channel:
		return "chan " + exprText(n.Value)
	case *golang.StructType:
		return "struct{...}"
	case *golang.InterfaceType:
		return "interface{...}"
	case *golang.FuncType:
		return "func(...)"
	}
	return fmt.Sprintf("%T", e)
}

func exprText0(rp *java.RightPadded[java.Expression]) string {
	if rp == nil {
		return ""
	}
	return exprText(rp.Element) + "."
}
