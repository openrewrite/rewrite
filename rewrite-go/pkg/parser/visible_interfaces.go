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
	"go/token"
	"go/types"
	"sort"
)

// visibleInterfaces are the interfaces a package under parse can see: the predeclared error, its own, those of its
// module's packages it reaches through imports, and those exported by the packages that it or those import by name. Go
// satisfies an interface by method set alone, so the interfaces a type of the package satisfies are those of these the
// type checker says it implements.
type visibleInterfaces struct {
	pkg *types.Package
	// byMethod files each interface under its first method's name, which a type has to have to satisfy it.
	byMethod map[string][]*types.Named
}

func (m *typeMapper) interfacesVisibleFrom(pkg *types.Package, inModule func(path string) bool) *visibleInterfaces {
	visible := &visibleInterfaces{pkg: pkg, byMethod: make(map[string][]*types.Named)}
	if predeclared, ok := types.Universe.Lookup("error").Type().(*types.Named); ok {
		visible.byMethod["Error"] = append(visible.byMethod["Error"], predeclared)
	}
	seen := make(map[*types.Package]bool)
	var visit func(p *types.Package)
	visit = func(p *types.Package) {
		if p == nil || seen[p] {
			return
		}
		seen[p] = true
		within := p == pkg || inModule(p.Path())
		for _, named := range m.interfacesDeclaredIn(p, within) {
			first := named.Underlying().(*types.Interface).Method(0).Name()
			visible.byMethod[first] = append(visible.byMethod[first], named)
		}
		// Only a package imported by name is sure to hold all it declares. One that only another's export data
		// names holds just what that names, so which packages were parsed first would decide what it holds.
		if within {
			for _, imported := range p.Imports() {
				visit(imported)
			}
		}
	}
	visit(pkg)
	return visible
}

// interfacesDeclaredIn lists the package-level interfaces of p that have methods and no type parameters. The unexported
// ones are left out of a package outside the module under parse: a type may satisfy them, but only that package's own
// code can put it to use as one.
func (m *typeMapper) interfacesDeclaredIn(p *types.Package, unexported bool) []*types.Named {
	if declared, ok := m.declaredInterfaces[p]; ok {
		return declared
	}
	var declared []*types.Named
	scope := p.Scope()
	for _, name := range scope.Names() {
		if !unexported && !token.IsExported(name) {
			continue
		}
		typeName, ok := scope.Lookup(name).(*types.TypeName)
		if !ok || typeName.IsAlias() {
			continue
		}
		named, ok := typeName.Type().(*types.Named)
		if !ok || named.TypeParams().Len() > 0 {
			continue
		}
		if iface, ok := named.Underlying().(*types.Interface); ok && iface.IsMethodSet() && iface.NumMethods() > 0 {
			declared = append(declared, named)
		}
	}
	// A package that is still incomplete may gain interfaces once something imports it.
	if p.Complete() {
		m.declaredInterfaces[p] = declared
	}
	return declared
}

// satisfiedBy lists, in name order, the interfaces named satisfies, or a pointer to it does when named is no
// interface itself.
func (v *visibleInterfaces) satisfiedBy(named *types.Named) (satisfied []*types.Named) {
	// A type the checker left broken can trip its assertions. It then names no interfaces rather than losing its type.
	defer func() {
		if recover() != nil {
			satisfied = nil
		}
	}()
	_, isInterface := named.Underlying().(*types.Interface)
	var pointer types.Type
	methods := types.NewMethodSet(named)
	if !isInterface {
		pointer = types.NewPointer(named)
		methods = types.NewMethodSet(pointer)
	}
	seen := map[*types.Named]bool{named: true}
	for i := 0; i < methods.Len(); i++ {
		for _, candidate := range v.byMethod[methods.At(i).Obj().Name()] {
			if seen[candidate] {
				continue
			}
			seen[candidate] = true
			iface := candidate.Underlying().(*types.Interface)
			if types.Implements(named, iface) || pointer != nil && types.Implements(pointer, iface) {
				satisfied = append(satisfied, candidate)
			}
		}
	}
	sort.Slice(satisfied, func(i, j int) bool {
		return fullyQualifiedName(satisfied[i].Obj()) < fullyQualifiedName(satisfied[j].Obj())
	})
	return satisfied
}
