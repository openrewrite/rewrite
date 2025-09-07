# Enhanced Type Mapping Implementation Plan

## Overview
This document tracks the implementation of enhanced type mapping for the JavaScript/TypeScript parser in OpenRewrite. The goal is to leverage TypeScript's type checker to provide rich type information for AST transformations, even for plain JavaScript files.

## Current State
- ✅ Basic TypeChecker integration via `ts.createProgram()` and `program.getTypeChecker()`
- ✅ Primitive type mapping (boolean, number, string, void, null, undefined, bigint)
- ✅ RegExp type mapping (mapped to String)
- ✅ Proper numeric literal parsing (integers, floats, bigint)
- ✅ Search result marker integration for type testing
- ⚠️ Incomplete `variableType()` method
- ❌ No class/interface type support
- ❌ No method/function type support
- ❌ No array/tuple type support
- ❌ No union/intersection type support (basic test passes but full support not implemented)
- ❌ No generic type support
- ❌ No JSX/React component type support

## Implementation Steps

### Phase 1: Foundation Enhancements ✅
- [x] **1.1 Enable `checkJs` for Better Type Inference**
  - ✅ Already enabled in parser.ts (line 80)
  - TypeChecker now provides better type inference for JavaScript files
  - Enables type checking and richer type information for .js files
  
- [x] **1.2 Improve Type Cache Key Generation**
  - ✅ Implemented fallback strategy for types without `id`
  - ✅ Cache now accepts both string and number keys
  - ✅ Fallback uses file path + position + type string for uniqueness
  - ✅ Handles synthetic types with "synthetic:" prefix

- [x] **1.3 Add Type Testing Infrastructure**
  - ✅ Created `/rewrite-javascript/rewrite/test/javascript/type-mapping.test.ts`
  - ✅ Added tests for primitive types and type annotations
  - ✅ Added cache behavior tests
  - ✅ Set up placeholder tests for future phases

- [x] **1.4 Implement Fully Qualified Name Resolution**
  - ✅ Implemented `getFullyQualifiedName()` method
  - ✅ Format: `"module-specifier.TypeName"` (e.g., `"@mui/material.Button"`, `"src/components/Button.Button"`)
  - ✅ Handles different module contexts:
    - NPM packages: Extracts package name from node_modules path
    - Local files: Uses relative path from project root
    - Built-in types: Uses "lib" prefix (e.g., `"lib.Array"`)
    - Ambient/global types: Uses type name without module prefix
  - ✅ Added helper methods:
    - `extractPackageName()` - Extracts package names including scoped packages
    - `getRelativeModulePath()` - Normalizes paths relative to project root
    - `isBuiltInType()` - Identifies built-in JavaScript/TypeScript types
  - ✅ Integrated project root passing from parser to type mapping

### Phase 2: Object and Class Types
- [ ] **2.1 Implement Class Type Mapping**
  - Map TypeScript classes to `JavaType.Class`
  - Handle class kinds (Class, Interface, Enum)
  - Extract class members and methods
  - Handle inheritance chain (`supertype`)
  - Support nested/inner classes (`owningClass`)
  ```typescript
  // In createType method
  if (type.symbol && type.symbol.flags & ts.SymbolFlags.Class) {
    // Create JavaType.Class
  }
  ```

- [ ] **2.2 Implement Interface Type Mapping**
  - Map interfaces to `JavaType.Class` with `Kind.Interface`
  - Handle extended interfaces
  - Extract interface members

- [ ] **2.3 Handle Object Literal Types**
  - Map object literals to appropriate JavaType
  - Handle index signatures
  - Support mapped types

### Phase 3: Function and Method Types
- [ ] **3.1 Complete `methodType()` Implementation**
  - Map function/method signatures to `JavaType.Method`
  - Extract parameter names and types
  - Handle return types
  - Support overloaded methods
  ```typescript
  methodType(node: ts.Node): JavaType.Method | undefined {
    // Get signature from type checker
    // Map to JavaType.Method
  }
  ```

- [ ] **3.2 Handle Constructor Types**
  - Map constructor signatures
  - Handle constructor overloads

- [ ] **3.3 Support Arrow Functions and Function Expressions**
  - Ensure all function forms are properly typed

### Phase 4: Complex Types
- [ ] **4.1 Implement Array Type Support**
  - Map arrays to `JavaType.Array`
  - Handle tuple types
  - Support readonly arrays
  ```typescript
  if (checker.isArrayType(type)) {
    const elemType = checker.getTypeArguments(type)[0];
    // Create JavaType.Array
  }
  ```

- [ ] **4.2 Implement Union Type Support**
  - Map union types to `JavaType.Union`
  - Handle discriminated unions
  - Support literal type unions

- [ ] **4.3 Implement Intersection Type Support**
  - Map intersection types to `JavaType.Intersection`
  - Handle complex intersections

### Phase 5: Generic Types
- [ ] **5.1 Implement Generic Type Variables**
  - Map type parameters to `JavaType.GenericTypeVariable`
  - Handle variance (covariant, contravariant, invariant)
  - Support type parameter constraints

- [ ] **5.2 Implement Parameterized Types**
  - Map generic instantiations to `JavaType.Parameterized`
  - Handle partial type argument inference
  - Support default type parameters

### Phase 6: Variable Types
- [ ] **6.1 Complete `variableType()` Implementation**
  - Map variable declarations to `JavaType.Variable`
  - Include owner type information
  - Handle variable annotations
  ```typescript
  variableType(node: ts.NamedDeclaration): JavaType.Variable | undefined {
    if (ts.isVariableDeclaration(node)) {
      const symbol = this.checker.getSymbolAtLocation(node.name);
      if (symbol) {
        const type = this.checker.getTypeOfSymbolAtLocation(symbol, node);
        // Create JavaType.Variable with proper type mapping
      }
    }
  }
  ```

- [ ] **6.2 Handle Property Declarations**
  - Map class properties
  - Handle property modifiers (readonly, optional)

### Phase 7: JSX and React Types
- [ ] **7.1 Map JSX Element Types**
  - Recognize imported React components
  - Map component prop types
  - Handle generic components
  ```typescript
  if (ts.isJsxElement(node) || ts.isJsxSelfClosingElement(node)) {
    const jsxType = checker.getTypeAtLocation(node);
    // Map JSX element type
  }
  ```

- [ ] **7.2 Handle Intrinsic Elements**
  - Map HTML elements to appropriate types
  - Support JSX.IntrinsicElements

- [ ] **7.3 Support JSX Fragments and Expressions**
  - Map fragment types
  - Handle JSX expression containers

### Phase 8: Module and Import Types
- [ ] **8.1 Handle Module Types**
  - Track module exports
  - Resolve import types
  - Support namespace imports

- [ ] **8.2 Support Type-Only Imports**
  - Handle `import type` declarations
  - Map to appropriate JavaType

### Phase 9: Advanced Features
- [ ] **9.1 Handle Conditional Types**
  - Map conditional type expressions
  - Support type inference in conditionals

- [ ] **9.2 Support Template Literal Types**
  - Map template literal types
  - Handle string manipulation types

- [ ] **9.3 Handle Enum Types**
  - Map enum declarations to `JavaType.Class` with `Kind.Enum`
  - Handle const enums

### Phase 10: Optimization and Testing
- [ ] **10.1 Performance Optimization**
  - Profile type mapping performance
  - Optimize cache usage
  - Consider lazy type resolution

- [ ] **10.2 Comprehensive Testing**
  - Test with real-world JavaScript projects
  - Test with React/TypeScript projects
  - Ensure backward compatibility

- [ ] **10.3 Documentation**
  - Document type mapping behavior
  - Add examples for each type category
  - Create troubleshooting guide

## Learnings and Notes

### Phase 1 Learnings
- **Type ID Fallback**: Not all TypeScript types have the internal `id` property, especially synthetic or fresh types
- **Cache Key Strategy**: Using file path + position + type string as fallback ensures uniqueness
- **Project Root**: Parser's `relativeTo` property can be used as project root for fully qualified names
- **Module Resolution**: TypeScript's `sourceFile.isDeclarationFile` helps identify external modules
- **Package Name Extraction**: Regex pattern `/node_modules\/(@[^\/]+\/[^\/]+|[^\/]+)/` handles both scoped and unscoped packages

### TypeChecker Capabilities Discovered
- TypeChecker can infer types even from plain JavaScript through control flow analysis
- The `type.id` property is a private field but reliable for caching when available
- JSDoc comments in JavaScript files provide explicit type information
- Import resolution works across module boundaries
- `checkJs: true` is already enabled, providing enhanced type inference
- TypeScript provides `getSymbol()` and symbol declarations for type location tracking

### Implementation Considerations
- Type cache keys need to include module context for accurate class identification
- Some types may not have an `id` property and need alternative signature generation
- Java implementation uses `JavaTypeCache` for efficient type caching
- Java separates class types from their parameterized versions (important for generics)
- TypeScript's module system differs from Java's package system - need module-aware fully qualified names

### Key Insights from Java Implementation
- **Type Cache Strategy**: Java uses signature-based caching with different signatures for:
  - Base class types (by fully qualified name)
  - Parameterized types (includes type arguments in signature)
  - Methods (includes declaring type, name, and parameter types)
  - Variables (includes owner and name)
- **Lazy Loading**: Java uses `unsafeSet()` to avoid circular dependencies during type construction
- **Method Handling**: Separate handling for constructors vs regular methods
- **Variance**: Generic type variables track variance (covariant, contravariant, invariant)

### Performance Notes
- (To be filled as we implement and profile)

### Edge Cases to Handle
- Circular type references
- Types from external modules without type definitions
- Dynamic property access patterns
- Async/Promise types
- Error types and exception handling

## Testing Strategy

### Unit Tests
- Test each type mapping method individually
- Mock TypeChecker for isolated testing
- Verify cache behavior

### Integration Tests
- Parse real JavaScript/TypeScript files
- Verify complete type trees
- Test cross-module type resolution

### Test Cases Needed
- [ ] Plain JavaScript with inferred types
- [ ] TypeScript with explicit types
- [ ] React components (class and functional)
- [ ] Node.js modules
- [ ] Mixed JS/TS projects
- [ ] Projects with @types packages
- [ ] JSDoc annotated JavaScript

## Success Metrics
- All primitive types correctly mapped ✅
- Complex types (classes, interfaces, functions) mapped accurately
- JSX/React component types resolved
- Type information available for plain JavaScript files
- Performance impact < 10% on parsing time
- No regression in existing functionality

## References
- [TypeScript Compiler API](https://github.com/microsoft/TypeScript/wiki/Using-the-Compiler-API)
- [TypeScript Type Checker Internals](https://github.com/microsoft/TypeScript/blob/main/src/compiler/checker.ts)
- [OpenRewrite JavaType Model](https://docs.openrewrite.org/concepts-explanations/lossless-semantic-trees)