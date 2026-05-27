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

package recipe

import (
	"reflect"
	"sync"
)

// Mirrors org.openrewrite.SourceFile.service(Class<S>): an extension
// point for languages to expose helper services keyed by service type.
// Recipes call Service[T](sourceFile) (or use the package-specific
// helper in pkg/recipe/golang/service) to compose follow-up visitors
// without coupling to specific recipe constructors.
//
// This file holds the Go-side mechanism: a process-wide registry of
// service factories keyed by the service type's reflect.Type. Languages
// register their factories at init() time; callers look them up via
// Service[T](anything) — the lookup ignores the source-file argument
// for now, since Go-side services are stateless. Future stateful
// services (like a ModuleResolutionService keyed by go.mod) can keep
// the same surface and route via the source-file's markers.

var (
	registryMu sync.RWMutex
	registry   = map[reflect.Type]func() any{}
)

// RegisterService installs a factory that produces a service of type T.
// Languages call this from their package init().
//
//	func init() {
//	    recipe.RegisterService[ImportService](func() any { return &importService{} })
//	}
func RegisterService[T any](factory func() any) {
	var zero T
	registryMu.Lock()
	defer registryMu.Unlock()
	registry[reflect.TypeOf(&zero).Elem()] = factory
}

// Service returns the registered service of type T. Panics with a
// descriptive error when no factory is registered — recipes that depend
// on a service should let the panic surface (it indicates a missing
// language activation).
//
//	svc := recipe.Service[golangservice.ImportService](cu)
//	v.DoAfterVisit(svc.AddImportVisitor("fmt", nil, false))
//
// `sourceFile` is unused today; the parameter exists so the call site
// reads naturally and so future stateful services can route on it
// without breaking callers.
func Service[T any](sourceFile any) T {
	_ = sourceFile
	var zero T
	t := reflect.TypeOf(&zero).Elem()
	registryMu.RLock()
	factory, ok := registry[t]
	registryMu.RUnlock()
	if !ok {
		panic("recipe: no service registered for " + t.String() +
			" — did the language's package init() run? " +
			"Import the language package (e.g. pkg/recipe/golang) to register services.")
	}
	v, ok := factory().(T)
	if !ok {
		panic("recipe: service factory for " + t.String() +
			" produced an instance that doesn't satisfy the requested type")
	}
	return v
}
