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

package rpc

import (
	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
)

type GoResolutionInternPool struct {
	byIdent map[uuid.UUID]golang.GoResolutionResult
}

func NewGoResolutionInternPool() *GoResolutionInternPool {
	return &GoResolutionInternPool{byIdent: make(map[uuid.UUID]golang.GoResolutionResult)}
}

func (p *GoResolutionInternPool) intern(r golang.GoResolutionResult) golang.GoResolutionResult {
	if p == nil || r.Ident == uuid.Nil {
		return r
	}
	if c, ok := p.byIdent[r.Ident]; ok {
		return c
	}
	p.byIdent[r.Ident] = r
	return r
}

// sendGoResolutionResult mirrors Java's
// org.openrewrite.golang.marker.GoResolutionResult#rpcSend.
//
// Field order MUST match the Java side exactly, otherwise the cross-
// language round-trip queue desyncs:
//
//  1. id (UUID string)
//  2. modulePath (String)
//  3. goVersion (String, nullable)
//  4. toolchain (String, nullable)
//  5. path (String)
//  6. requires (List<Require>, ref-by-key)
//  7. replaces (List<Replace>, ref-by-key)
//  8. excludes (List<Exclude>, ref-by-key)
//  9. retracts (List<Retract>, ref-by-key)
//
// 10. resolvedDependencies (List<ResolvedDependency>, ref-by-key)
// 11. packageModules (List<PackageModule>, ref-by-key)
//
// Each ResolvedDependency element sends, after goModHash: indirect, main,
// replacePath, replaceVersion, moduleGoVersion, then deps (List<ModuleRef>).
//
// Each list element invokes its own rpcSend on the Java side; we mirror
// the same field order in the per-element onChange callback.
func sendGoResolutionResult(m golang.GoResolutionResult, q *SendQueue) {
	q.GetAndSend(m, func(x any) any { return x.(golang.GoResolutionResult).Ident.String() }, nil)
	q.GetAndSend(m, func(x any) any { return x.(golang.GoResolutionResult).ModulePath }, nil)
	q.GetAndSend(m, func(x any) any { return emptyAsNil(x.(golang.GoResolutionResult).GoVersion) }, nil)
	q.GetAndSend(m, func(x any) any { return emptyAsNil(x.(golang.GoResolutionResult).Toolchain) }, nil)
	q.GetAndSend(m, func(x any) any { return x.(golang.GoResolutionResult).Path }, nil)

	q.GetAndSendListAsRef(m,
		func(x any) []any { return requireSlice(x.(golang.GoResolutionResult).Requires) },
		func(x any) any {
			r := x.(golang.GoRequire)
			return r.ModulePath + "@" + r.Version
		},
		func(x any) {
			r := x.(golang.GoRequire)
			q.GetAndSend(r, func(y any) any { return y.(golang.GoRequire).ModulePath }, nil)
			q.GetAndSend(r, func(y any) any { return y.(golang.GoRequire).Version }, nil)
			q.GetAndSend(r, func(y any) any { return y.(golang.GoRequire).Indirect }, nil)
		})

	q.GetAndSendListAsRef(m,
		func(x any) []any { return replaceSlice(x.(golang.GoResolutionResult).Replaces) },
		func(x any) any {
			r := x.(golang.GoReplace)
			return r.OldPath + "@" + r.OldVersion + "=>" + r.NewPath + "@" + r.NewVersion
		},
		func(x any) {
			r := x.(golang.GoReplace)
			q.GetAndSend(r, func(y any) any { return y.(golang.GoReplace).OldPath }, nil)
			q.GetAndSend(r, func(y any) any { return emptyAsNil(y.(golang.GoReplace).OldVersion) }, nil)
			q.GetAndSend(r, func(y any) any { return y.(golang.GoReplace).NewPath }, nil)
			q.GetAndSend(r, func(y any) any { return emptyAsNil(y.(golang.GoReplace).NewVersion) }, nil)
		})

	q.GetAndSendListAsRef(m,
		func(x any) []any { return excludeSlice(x.(golang.GoResolutionResult).Excludes) },
		func(x any) any {
			e := x.(golang.GoExclude)
			return e.ModulePath + "@" + e.Version
		},
		func(x any) {
			e := x.(golang.GoExclude)
			q.GetAndSend(e, func(y any) any { return y.(golang.GoExclude).ModulePath }, nil)
			q.GetAndSend(e, func(y any) any { return y.(golang.GoExclude).Version }, nil)
		})

	q.GetAndSendListAsRef(m,
		func(x any) []any { return retractSlice(x.(golang.GoResolutionResult).Retracts) },
		func(x any) any { return x.(golang.GoRetract).VersionRange },
		func(x any) {
			r := x.(golang.GoRetract)
			q.GetAndSend(r, func(y any) any { return y.(golang.GoRetract).VersionRange }, nil)
			q.GetAndSend(r, func(y any) any { return emptyAsNil(y.(golang.GoRetract).Rationale) }, nil)
		})

	q.GetAndSendListAsRef(m,
		func(x any) []any { return resolvedSlice(x.(golang.GoResolutionResult).ResolvedDependencies) },
		func(x any) any {
			d := x.(golang.GoResolvedDependency)
			return d.ModulePath + "@" + d.Version
		},
		func(x any) {
			d := x.(golang.GoResolvedDependency)
			q.GetAndSend(d, func(y any) any { return y.(golang.GoResolvedDependency).ModulePath }, nil)
			q.GetAndSend(d, func(y any) any { return y.(golang.GoResolvedDependency).Version }, nil)
			q.GetAndSend(d, func(y any) any { return emptyAsNil(y.(golang.GoResolvedDependency).ModuleHash) }, nil)
			q.GetAndSend(d, func(y any) any { return emptyAsNil(y.(golang.GoResolvedDependency).GoModHash) }, nil)
			q.GetAndSend(d, func(y any) any { return y.(golang.GoResolvedDependency).Indirect }, nil)
			q.GetAndSend(d, func(y any) any { return y.(golang.GoResolvedDependency).Main }, nil)
			q.GetAndSend(d, func(y any) any { return emptyAsNil(y.(golang.GoResolvedDependency).ReplacePath) }, nil)
			q.GetAndSend(d, func(y any) any { return emptyAsNil(y.(golang.GoResolvedDependency).ReplaceVersion) }, nil)
			q.GetAndSend(d, func(y any) any { return emptyAsNil(y.(golang.GoResolvedDependency).ModuleGoVersion) }, nil)
			q.GetAndSendListAsRef(d,
				func(y any) []any { return moduleRefSlice(y.(golang.GoResolvedDependency).Deps) },
				func(y any) any {
					r := y.(golang.GoModuleRef)
					return r.ModulePath + "@" + r.Version
				},
				func(y any) {
					r := y.(golang.GoModuleRef)
					q.GetAndSend(r, func(z any) any { return z.(golang.GoModuleRef).ModulePath }, nil)
					q.GetAndSend(r, func(z any) any { return z.(golang.GoModuleRef).Version }, nil)
				})
		})

	q.GetAndSendListAsRef(m,
		func(x any) []any { return packageModuleSlice(x.(golang.GoResolutionResult).PackageModules) },
		func(x any) any { return x.(golang.GoPackageModule).ImportPath },
		func(x any) {
			p := x.(golang.GoPackageModule)
			q.GetAndSend(p, func(y any) any { return y.(golang.GoPackageModule).ImportPath }, nil)
			q.GetAndSend(p, func(y any) any { return emptyAsNil(y.(golang.GoPackageModule).ModulePath) }, nil)
			q.GetAndSend(p, func(y any) any { return emptyAsNil(y.(golang.GoPackageModule).Version) }, nil)
			q.GetAndSend(p, func(y any) any { return y.(golang.GoPackageModule).Standard }, nil)
		})
}

// receiveGoResolutionResult mirrors Java's
// org.openrewrite.golang.marker.GoResolutionResult#rpcReceive.
func receiveGoResolutionResult(before golang.GoResolutionResult, q *ReceiveQueue) golang.GoResolutionResult {
	idStr := receiveScalar[string](q, before.Ident.String())
	if idStr != "" {
		if parsed, err := uuid.Parse(idStr); err == nil {
			before.Ident = parsed
		}
	}
	before.ModulePath = receiveScalar[string](q, before.ModulePath)
	before.GoVersion = receiveNullableString(q, before.GoVersion)
	before.Toolchain = receiveNullableString(q, before.Toolchain)
	before.Path = receiveScalar[string](q, before.Path)

	before.Requires = recvRequires(q, before.Requires)
	before.Replaces = recvReplaces(q, before.Replaces)
	before.Excludes = recvExcludes(q, before.Excludes)
	before.Retracts = recvRetracts(q, before.Retracts)
	before.ResolvedDependencies = recvResolvedDeps(q, before.ResolvedDependencies)
	before.PackageModules = recvPackageModules(q, before.PackageModules)
	return q.goResolutionIntern.intern(before)
}

func recvRequires(q *ReceiveQueue, before []golang.GoRequire) []golang.GoRequire {
	beforeAny := requireSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		r := v.(golang.GoRequire)
		r.ModulePath = receiveScalar[string](q, r.ModulePath)
		r.Version = receiveScalar[string](q, r.Version)
		r.Indirect = receiveScalar[bool](q, r.Indirect)
		return r
	})
	if afterAny == nil {
		return nil
	}
	out := make([]golang.GoRequire, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(golang.GoRequire)
	}
	return out
}

func recvReplaces(q *ReceiveQueue, before []golang.GoReplace) []golang.GoReplace {
	beforeAny := replaceSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		r := v.(golang.GoReplace)
		r.OldPath = receiveScalar[string](q, r.OldPath)
		r.OldVersion = receiveNullableString(q, r.OldVersion)
		r.NewPath = receiveScalar[string](q, r.NewPath)
		r.NewVersion = receiveNullableString(q, r.NewVersion)
		return r
	})
	if afterAny == nil {
		return nil
	}
	out := make([]golang.GoReplace, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(golang.GoReplace)
	}
	return out
}

func recvExcludes(q *ReceiveQueue, before []golang.GoExclude) []golang.GoExclude {
	beforeAny := excludeSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		e := v.(golang.GoExclude)
		e.ModulePath = receiveScalar[string](q, e.ModulePath)
		e.Version = receiveScalar[string](q, e.Version)
		return e
	})
	if afterAny == nil {
		return nil
	}
	out := make([]golang.GoExclude, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(golang.GoExclude)
	}
	return out
}

func recvRetracts(q *ReceiveQueue, before []golang.GoRetract) []golang.GoRetract {
	beforeAny := retractSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		r := v.(golang.GoRetract)
		r.VersionRange = receiveScalar[string](q, r.VersionRange)
		r.Rationale = receiveNullableString(q, r.Rationale)
		return r
	})
	if afterAny == nil {
		return nil
	}
	out := make([]golang.GoRetract, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(golang.GoRetract)
	}
	return out
}

func recvResolvedDeps(q *ReceiveQueue, before []golang.GoResolvedDependency) []golang.GoResolvedDependency {
	beforeAny := resolvedSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		d := v.(golang.GoResolvedDependency)
		d.ModulePath = receiveScalar[string](q, d.ModulePath)
		d.Version = receiveScalar[string](q, d.Version)
		d.ModuleHash = receiveNullableString(q, d.ModuleHash)
		d.GoModHash = receiveNullableString(q, d.GoModHash)
		d.Indirect = receiveScalar[bool](q, d.Indirect)
		d.Main = receiveScalar[bool](q, d.Main)
		d.ReplacePath = receiveNullableString(q, d.ReplacePath)
		d.ReplaceVersion = receiveNullableString(q, d.ReplaceVersion)
		d.ModuleGoVersion = receiveNullableString(q, d.ModuleGoVersion)
		d.Deps = recvModuleRefs(q, d.Deps)
		return d
	})
	if afterAny == nil {
		return nil
	}
	out := make([]golang.GoResolvedDependency, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(golang.GoResolvedDependency)
	}
	return out
}

func recvModuleRefs(q *ReceiveQueue, before []golang.GoModuleRef) []golang.GoModuleRef {
	beforeAny := moduleRefSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		r := v.(golang.GoModuleRef)
		r.ModulePath = receiveScalar[string](q, r.ModulePath)
		r.Version = receiveScalar[string](q, r.Version)
		return r
	})
	if afterAny == nil {
		return nil
	}
	out := make([]golang.GoModuleRef, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(golang.GoModuleRef)
	}
	return out
}

func recvPackageModules(q *ReceiveQueue, before []golang.GoPackageModule) []golang.GoPackageModule {
	beforeAny := packageModuleSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		p := v.(golang.GoPackageModule)
		p.ImportPath = receiveScalar[string](q, p.ImportPath)
		p.ModulePath = receiveNullableString(q, p.ModulePath)
		p.Version = receiveNullableString(q, p.Version)
		p.Standard = receiveScalar[bool](q, p.Standard)
		return p
	})
	if afterAny == nil {
		return nil
	}
	out := make([]golang.GoPackageModule, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(golang.GoPackageModule)
	}
	return out
}

func requireSlice(s []golang.GoRequire) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

func replaceSlice(s []golang.GoReplace) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

func excludeSlice(s []golang.GoExclude) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

func retractSlice(s []golang.GoRetract) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

func resolvedSlice(s []golang.GoResolvedDependency) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

func moduleRefSlice(s []golang.GoModuleRef) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

func packageModuleSlice(s []golang.GoPackageModule) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

// receiveNullableString reads a value that may be null on the wire and
// returns "" if so. Mirrors how emptyAsNil is sent on the send side.
func receiveNullableString(q *ReceiveQueue, before string) string {
	v := q.Receive(emptyAsNil(before), nil)
	if v == nil {
		return ""
	}
	return v.(string)
}
