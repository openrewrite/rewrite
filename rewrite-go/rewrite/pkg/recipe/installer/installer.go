/*
 * Copyright 2025 the original author or authors.
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

package installer

import (
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"text/template"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
)

// RecipeModuleInfo holds information about an installed recipe module.
type RecipeModuleInfo struct {
	ImportPath string
	Version    string
	Recipes    []recipe.RecipeDescriptor
}

// Installer manages the Go recipe workspace and installs recipe modules.
type Installer struct {
	WorkspaceDir string            // e.g., ~/.rewrite/go-recipes/
	Logger       func(string, ...any) // optional logger
}

// NewInstaller creates an Installer with the default workspace directory.
func NewInstaller() *Installer {
	home, _ := os.UserHomeDir()
	return &Installer{
		WorkspaceDir: filepath.Join(home, ".rewrite", "go-recipes"),
	}
}

// ensureWorkspace creates the workspace directory and initializes go.mod if needed.
func (inst *Installer) ensureWorkspace() error {
	if err := os.MkdirAll(inst.WorkspaceDir, 0755); err != nil {
		return fmt.Errorf("create workspace: %w", err)
	}
	goMod := filepath.Join(inst.WorkspaceDir, "go.mod")
	if _, err := os.Stat(goMod); os.IsNotExist(err) {
		content := "module rewrite-recipe-workspace\n\ngo 1.23\n"
		if err := os.WriteFile(goMod, []byte(content), 0644); err != nil {
			return fmt.Errorf("write go.mod: %w", err)
		}
	}
	return nil
}

// InstallFromPath installs recipes from a local Go module path.
func (inst *Installer) InstallFromPath(localPath string, registry *recipe.Registry) (*RecipeModuleInfo, error) {
	if err := inst.ensureWorkspace(); err != nil {
		return nil, err
	}

	// Resolve absolute path
	absPath, err := filepath.Abs(localPath)
	if err != nil {
		return nil, fmt.Errorf("resolve path: %w", err)
	}

	// Read the module path from the local module's go.mod
	modulePath, err := readModulePath(absPath)
	if err != nil {
		return nil, fmt.Errorf("read module path from %s: %w", absPath, err)
	}

	// Add replace directive to workspace go.mod
	if err := inst.addReplace(modulePath, absPath); err != nil {
		return nil, err
	}

	// Propagate replace directives from the installed module's go.mod
	// so transitive local dependencies (e.g., rewrite-go) are resolved.
	if err := inst.propagateReplaces(absPath); err != nil {
		return nil, fmt.Errorf("propagate replaces from %s: %w", absPath, err)
	}

	// Add require directive
	if err := inst.addRequire(modulePath, "v0.0.0"); err != nil {
		return nil, err
	}

	// Discover the import path of the package containing Activate.
	activatePkg, err := findActivatePackage(absPath, modulePath)
	if err != nil {
		return nil, fmt.Errorf("find Activate package: %w", err)
	}

	// Generate the helper source BEFORE tidy, so tidy sees the
	// imports and keeps the require/replace directives.
	helperDir := filepath.Join(inst.WorkspaceDir, "helper")
	if err := os.MkdirAll(helperDir, 0755); err != nil {
		return nil, fmt.Errorf("create helper dir: %w", err)
	}
	helperSrc := filepath.Join(helperDir, "main.go")
	if err := generateHelper(helperSrc, activatePkg); err != nil {
		return nil, fmt.Errorf("generate helper: %w", err)
	}

	// Run go mod tidy
	if err := inst.goCmd("mod", "tidy"); err != nil {
		return nil, fmt.Errorf("go mod tidy: %w", err)
	}

	// Build and run the helper to discover recipes, then build a custom
	// RPC binary that includes the recipe module for actual execution.
	return inst.loadRecipes(modulePath, activatePkg, registry)
}

// InstallFromPackage installs recipes from a remote Go module.
func (inst *Installer) InstallFromPackage(packageName string, version *string, registry *recipe.Registry) (*RecipeModuleInfo, error) {
	if err := inst.ensureWorkspace(); err != nil {
		return nil, err
	}

	// Determine version spec
	versionSpec := "latest"
	if version != nil && *version != "" {
		versionSpec = *version
	}

	// Run go get to fetch the module
	getArg := packageName + "@" + versionSpec
	if err := inst.goCmd("get", "-d", getArg); err != nil {
		return nil, fmt.Errorf("go get %s: %w", getArg, err)
	}

	// Read resolved version from go.mod
	resolvedVersion := inst.readResolvedVersion(packageName)

	// Load recipes (for remote packages, activatePkg is the package name itself)
	info, err := inst.loadRecipes(packageName, packageName, registry)
	if err != nil {
		return nil, err
	}
	if resolvedVersion != "" {
		info.Version = resolvedVersion
	}
	return info, nil
}

// loadRecipes builds a custom RPC binary that includes the recipe module,
// registers recipe descriptors in the current registry, and stores the custom
// binary path for the Java side to use on subsequent RPC sessions.
//
// Go plugins have type identity issues (types from a plugin are not identical
// to types in the host binary), so instead we rebuild the entire RPC binary
// with the recipe module linked in. The custom binary is stored at
// ~/.rewrite/go-recipes/rewrite-go-rpc and used by subsequent mod run calls.
func (inst *Installer) loadRecipes(modulePath, activatePkg string, registry *recipe.Registry) (*RecipeModuleInfo, error) {
	helperDir := filepath.Join(inst.WorkspaceDir, "helper")
	helperSrc := filepath.Join(helperDir, "main.go")

	// Generate the helper source if it doesn't already exist.
	// This program both outputs recipe descriptors AND serves as a
	// recipe-enabled RPC server.
	if _, err := os.Stat(helperSrc); os.IsNotExist(err) {
		if err := os.MkdirAll(helperDir, 0755); err != nil {
			return nil, fmt.Errorf("create helper dir: %w", err)
		}
		if err := generateHelper(helperSrc, modulePath); err != nil {
			return nil, fmt.Errorf("generate helper: %w", err)
		}
	}

	// Build the helper binary
	helperBin := filepath.Join(helperDir, "helper")
	cmd := exec.Command("go", "build", "-o", helperBin, ".")
	cmd.Dir = helperDir
	cmd.Env = append(os.Environ(), "GO111MODULE=on")
	if output, err := cmd.CombinedOutput(); err != nil {
		return nil, fmt.Errorf("build helper: %s: %w", string(output), err)
	}

	// Run the helper to discover recipe descriptors
	cmd = exec.Command(helperBin)
	cmd.Dir = helperDir
	output, err := cmd.Output()
	if err != nil {
		return nil, fmt.Errorf("run helper: %w", err)
	}

	var entries []helperOutput
	if err := json.Unmarshal(output, &entries); err != nil {
		return nil, fmt.Errorf("parse helper output: %w", err)
	}

	// Register recipes in the current session's registry (descriptors only)
	var descriptors []recipe.RecipeDescriptor
	for _, entry := range entries {
		desc := recipe.RecipeDescriptor{
			Name:        entry.Descriptor.Name,
			DisplayName: entry.Descriptor.DisplayName,
			Description: entry.Descriptor.Description,
		}
		var cats []recipe.CategoryDescriptor
		for _, c := range entry.Categories {
			cats = append(cats, recipe.CategoryDescriptor{
				DisplayName: c.DisplayName,
				Description: c.Description,
			})
		}
		registry.RegisterWithCategories(desc, cats)
		descriptors = append(descriptors, desc)
	}

	// Now build a custom RPC binary that includes the recipe module.
	// This binary will be used by subsequent mod run calls.
	customBin := filepath.Join(inst.WorkspaceDir, "rewrite-go-rpc")
	if err := inst.buildCustomRpc(activatePkg, customBin); err != nil {
		if inst.Logger != nil {
			inst.Logger("warning: failed to build custom RPC binary: %v", err)
		}
		// Non-fatal: recipes will be discovered but not executable
	}

	os.RemoveAll(helperDir)
	return &RecipeModuleInfo{
		ImportPath: modulePath,
		Recipes:    descriptors,
	}, nil
}

// buildCustomRpc copies the original RPC server source and adds recipe
// module activation. The resulting binary can execute installed recipes.
func (inst *Installer) buildCustomRpc(activatePkg, outputPath string) error {
	rpcDir := filepath.Join(inst.WorkspaceDir, "rpc")
	if err := os.MkdirAll(rpcDir, 0755); err != nil {
		return fmt.Errorf("create rpc dir: %w", err)
	}

	// Find the original RPC source
	rpcSrcDir := inst.findRpcSource()
	if rpcSrcDir == "" {
		return fmt.Errorf("could not find rewrite-go-rpc source directory")
	}

	// Copy main.go and inject recipe activation
	origSrc, err := os.ReadFile(filepath.Join(rpcSrcDir, "main.go"))
	if err != nil {
		return fmt.Errorf("read original main.go: %w", err)
	}

	// Inject the recipe import and activation call
	modified := injectRecipeActivation(string(origSrc), activatePkg)
	if err := os.WriteFile(filepath.Join(rpcDir, "main.go"), []byte(modified), 0644); err != nil {
		return fmt.Errorf("write custom main.go: %w", err)
	}

	cmd := exec.Command("go", "build", "-o", outputPath, ".")
	cmd.Dir = rpcDir
	cmd.Env = append(os.Environ(), "GO111MODULE=on")
	if output, err := cmd.CombinedOutput(); err != nil {
		return fmt.Errorf("build custom rpc: %s: %w", string(output), err)
	}

	os.RemoveAll(rpcDir)
	return nil
}

// findRpcSource looks for the rewrite-go-rpc source directory.
func (inst *Installer) findRpcSource() string {
	// Try relative to the current binary
	exe, err := os.Executable()
	if err == nil {
		dir := filepath.Dir(exe)
		// The source is typically at rewrite-go/rewrite/cmd/rpc relative to the build
		candidates := []string{
			filepath.Join(dir, "..", "rewrite", "cmd", "rpc"),
			filepath.Join(dir, "..", "..", "rewrite", "cmd", "rpc"),
		}
		for _, c := range candidates {
			if _, err := os.Stat(filepath.Join(c, "main.go")); err == nil {
				return c
			}
		}
	}

	// Try via go/build to find the module
	gopath := os.Getenv("GOPATH")
	if gopath == "" {
		home, _ := os.UserHomeDir()
		gopath = filepath.Join(home, "go")
	}

	_ = filepath.Join(gopath, "pkg", "mod") // for future GOMODCACHE lookup
	if runtime.GOOS == "darwin" || runtime.GOOS == "linux" {
		// For local development, check the workspace's replace directive
		data, err := os.ReadFile(filepath.Join(inst.WorkspaceDir, "go.mod"))
		if err == nil {
			for _, line := range strings.Split(string(data), "\n") {
				if strings.Contains(line, "openrewrite/rewrite/rewrite-go") && strings.Contains(line, "=>") {
					parts := strings.SplitN(line, "=>", 2)
					if len(parts) == 2 {
						localPath := strings.TrimSpace(parts[1])
						candidate := filepath.Join(localPath, "cmd", "rpc")
						if _, err := os.Stat(filepath.Join(candidate, "main.go")); err == nil {
							return candidate
						}
					}
				}
			}
		}
	}

	return ""
}

// injectRecipeActivation modifies the RPC server source to import and activate
// the recipe module. It adds an import and inserts an Activate call in newServer.
func injectRecipeActivation(src, activatePkg string) string {
	// Add import
	importLine := fmt.Sprintf("\trecipes %q\n", activatePkg)
	src = strings.Replace(src,
		"\"github.com/openrewrite/rewrite/rewrite-go/pkg/rpc\"\n",
		"\"github.com/openrewrite/rewrite/rewrite-go/pkg/rpc\"\n"+importLine,
		1)

	// Add Activate call after the built-in golang.Activate
	src = strings.Replace(src,
		"reg.Activate(golang.Activate)\n",
		"reg.Activate(golang.Activate)\n\treg.Activate(recipes.Activate)\n",
		1)

	return src
}

// helperOutput is the JSON format output by the helper program.
type helperOutput struct {
	Descriptor RecipeDescriptorJSON    `json:"descriptor"`
	Categories []CategoryDescriptorJSON `json:"categories"`
}

// RecipeDescriptorJSON is the JSON form of recipe.RecipeDescriptor.
type RecipeDescriptorJSON struct {
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
	Description string `json:"description"`
}

// CategoryDescriptorJSON is the JSON form of recipe.CategoryDescriptor.
type CategoryDescriptorJSON struct {
	DisplayName string `json:"displayName"`
	Description string `json:"description"`
}


// helperTemplate is the Go source template for the recipe discovery helper program.
var helperTemplate = template.Must(template.New("helper").Parse(`package main

import (
	"encoding/json"
	"fmt"
	"os"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	recipes "{{.ModulePath}}"
)

type output struct {
	Descriptor struct {
		Name        string ` + "`" + `json:"name"` + "`" + `
		DisplayName string ` + "`" + `json:"displayName"` + "`" + `
		Description string ` + "`" + `json:"description"` + "`" + `
	} ` + "`" + `json:"descriptor"` + "`" + `
	Categories []struct {
		DisplayName string ` + "`" + `json:"displayName"` + "`" + `
		Description string ` + "`" + `json:"description"` + "`" + `
	} ` + "`" + `json:"categories"` + "`" + `
}

func main() {
	reg := recipe.NewRegistry()
	recipes.Activate(reg)

	var results []output
	for _, r := range reg.AllRegistrations() {
		o := output{}
		o.Descriptor.Name = r.Descriptor.Name
		o.Descriptor.DisplayName = r.Descriptor.DisplayName
		o.Descriptor.Description = r.Descriptor.Description
		for _, c := range r.Categories {
			o.Categories = append(o.Categories, struct {
				DisplayName string ` + "`" + `json:"displayName"` + "`" + `
				Description string ` + "`" + `json:"description"` + "`" + `
			}{DisplayName: c.DisplayName, Description: c.Description})
		}
		results = append(results, o)
	}
	data, err := json.Marshal(results)
	if err != nil {
		fmt.Fprintf(os.Stderr, "marshal error: %v\n", err)
		os.Exit(1)
	}
	fmt.Print(string(data))
}
`))

// generateHelper creates a Go program that imports the recipe module,
// calls Activate, and outputs recipe descriptors as JSON.
func generateHelper(path string, modulePath string) error {
	f, err := os.Create(path)
	if err != nil {
		return err
	}
	defer f.Close()

	return helperTemplate.Execute(f, map[string]string{
		"ModulePath": modulePath,
	})
}

// goCmd runs a go command in the workspace directory.
func (inst *Installer) goCmd(args ...string) error {
	cmd := exec.Command("go", args...)
	cmd.Dir = inst.WorkspaceDir
	cmd.Env = append(os.Environ(), "GO111MODULE=on")
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("%s: %s", strings.Join(args, " "), string(output))
	}
	if inst.Logger != nil {
		inst.Logger("go %s: %s", strings.Join(args, " "), string(output))
	}
	return nil
}

// addReplace adds a replace directive to the workspace go.mod.
func (inst *Installer) addReplace(modulePath, localPath string) error {
	return inst.goCmd("mod", "edit", "-replace="+modulePath+"="+localPath)
}

// addRequire adds a require directive to the workspace go.mod.
func (inst *Installer) addRequire(modulePath, version string) error {
	return inst.goCmd("mod", "edit", "-require="+modulePath+"@"+version)
}

// findActivatePackage discovers the Go import path of the package that contains
// func Activate. It walks the module directory looking for a .go file with
// "func Activate(". Returns the full import path (modulePath + relative subdir).
func findActivatePackage(moduleDir, modulePath string) (string, error) {
	var activatePkg string
	err := filepath.Walk(moduleDir, func(path string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() || !strings.HasSuffix(path, ".go") {
			return nil
		}
		data, readErr := os.ReadFile(path)
		if readErr != nil {
			return nil
		}
		if strings.Contains(string(data), "func Activate(") {
			rel, err := filepath.Rel(moduleDir, filepath.Dir(path))
			if err != nil {
				return nil
			}
			if rel == "." {
				activatePkg = modulePath
			} else {
				activatePkg = modulePath + "/" + filepath.ToSlash(rel)
			}
			return filepath.SkipAll
		}
		return nil
	})
	if err != nil {
		return "", err
	}
	if activatePkg == "" {
		// Fall back to the module path itself
		return modulePath, nil
	}
	return activatePkg, nil
}

// readModulePath reads the module path from a go.mod file in the given directory.
func readModulePath(dir string) (string, error) {
	data, err := os.ReadFile(filepath.Join(dir, "go.mod"))
	if err != nil {
		return "", err
	}
	for _, line := range strings.Split(string(data), "\n") {
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "module ") {
			return strings.TrimSpace(strings.TrimPrefix(line, "module ")), nil
		}
	}
	return "", fmt.Errorf("no module directive found in go.mod")
}

// propagateReplaces reads replace directives from the module's go.mod and adds
// them to the workspace go.mod, resolving relative paths against the module dir.
func (inst *Installer) propagateReplaces(moduleDir string) error {
	data, err := os.ReadFile(filepath.Join(moduleDir, "go.mod"))
	if err != nil {
		return nil // No go.mod to read replaces from
	}

	lines := strings.Split(string(data), "\n")
	inReplaceBlock := false
	for _, line := range lines {
		trimmed := strings.TrimSpace(line)

		if trimmed == "replace (" {
			inReplaceBlock = true
			continue
		}
		if inReplaceBlock && trimmed == ")" {
			inReplaceBlock = false
			continue
		}

		var replaceLine string
		if strings.HasPrefix(trimmed, "replace ") && !strings.HasSuffix(trimmed, "(") {
			replaceLine = strings.TrimPrefix(trimmed, "replace ")
		} else if inReplaceBlock && trimmed != "" {
			replaceLine = trimmed
		}

		if replaceLine == "" {
			continue
		}

		parts := strings.SplitN(replaceLine, " => ", 2)
		if len(parts) != 2 {
			continue
		}

		target := strings.TrimSpace(parts[1])
		// Resolve relative paths against the module directory
		if !strings.Contains(target, "@") && (strings.HasPrefix(target, ".") || strings.HasPrefix(target, "/")) {
			absTarget := target
			if !filepath.IsAbs(target) {
				absTarget = filepath.Join(moduleDir, target)
			}
			absTarget, err = filepath.Abs(absTarget)
			if err != nil {
				continue
			}
			target = absTarget
		}

		source := strings.TrimSpace(parts[0])
		// Skip if we already have a replace for this module
		if err := inst.addReplace(source, target); err != nil {
			// Non-fatal: workspace might already have a conflicting replace
			continue
		}
	}
	return nil
}

// readResolvedVersion reads the resolved version of a module from the workspace go.mod.
func (inst *Installer) readResolvedVersion(modulePath string) string {
	data, _ := os.ReadFile(filepath.Join(inst.WorkspaceDir, "go.mod"))
	for _, line := range strings.Split(string(data), "\n") {
		line = strings.TrimSpace(line)
		if strings.Contains(line, modulePath) && !strings.HasPrefix(line, "module") {
			parts := strings.Fields(line)
			if len(parts) >= 2 {
				return parts[len(parts)-1]
			}
		}
	}
	return ""
}
