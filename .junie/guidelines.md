# OpenRewrite Project Guidelines

## Overview
This document outlines the coding standards, patterns, and best practices for the OpenRewrite project. The project consists of multiple modules, with code written in both Java and TypeScript. These guidelines should be followed when contributing to the project.

## General Guidelines

### Code Organization
- Code should be organized into modules based on functionality
- Each module should have a clear responsibility
- Follow the package structure conventions established in the project
- Keep files focused on a single responsibility

### Documentation
- Public APIs should be documented with a brief summary at the type level and method level documentation for interfaces
- Include examples in documentation where appropriate
- Document complex algorithms and non-obvious code
- Keep documentation up-to-date with code changes

### Testing
- Write tests for all new features and bug fixes
- Aim for high test coverage
- Tests should be comprehensive and cover edge cases
- Use appropriate testing frameworks (JUnit for Java, Jest for TypeScript)

### Error Handling
- Handle errors appropriately and provide meaningful error messages
- Use exceptions for exceptional conditions, not for control flow
- Validate inputs and fail fast
- Log errors with appropriate context

### Task Management
- Always check tasks in the task list when they are completed
- Update task lists in `docs/tasks.md` to reflect current progress
- Mark tasks as completed by changing `- [ ]` to `- [x]`
- Keep task lists up-to-date to help track project progress
- Ensure all subtasks are checked before marking a parent task as complete

## Java Guidelines

### Code Style
- Follow standard Java naming conventions
  - Classes: PascalCase
  - Methods and variables: camelCase
  - Constants: UPPER_SNAKE_CASE
- Use 4 spaces for indentation
- Keep lines under 120 characters
- Use meaningful variable and method names

### Java Patterns
- Use Lombok annotations to reduce boilerplate (@Getter, @RequiredArgsConstructor, etc.)
- Use interfaces with default methods where appropriate
- Use @Nullable annotations for null safety
- Use functional programming style with streams and lambdas where it improves readability
- Prefer immutable objects where possible
- Use the Builder pattern for complex object creation
- Use the Visitor pattern for tree traversal and transformation

### Java Best Practices
- Favor composition over inheritance
- Use dependency injection for better testability
- Write small, focused methods
- When testing OpenRewrite parsers or recipes use `RewriteTest` and its `rewriteRun()` methods with "before" and "after" state to indicate what changes are expected and not expected to be made to source files
- Avoid mutable state where possible
- Use appropriate data structures for the task
- Follow the principle of least surprise

## TypeScript Guidelines

### Organization
- The Node project root for the TypeScript code is `rewrite-javascript/rewrite`. So when running `npm`, make sure to add `--prefix rewrite-javascript/rewrite` to the command or change into that directory before running the command.
- The TypeScript code represents an implementation of OpenRewrite Java in TypeScript
  - The modules directly inside `src`, `src/rpc`, `src/text` roughly correspond to the repo-level Gradle project `rewrite-core`
  - The modules in `src/java` correspond to the Java code in the Gradle project `rewrite-java`
  - The modules in `src/javascript` correspond to the Java code in the Gradle project `rewrite-javascript`
  - The modules in `src/json` correspond to the Java code in the Gradle project `rewrite-json`
  - The modules in `src/test` correspond to the Java code in the Gradle project `rewrite-test`
- Specifically, there are a lot of types which have the exact same names and structures in both the Java and the TypeScript code (e.g. `JavaVisitor` or `Markers`).
  - A lot of types (specifically those in `tree.ts` and `markers.ts` files) represent data types which need to have matching definitions in Java and TypeScript to support a custom serialization mechanism
  - The serialization mechanism is generally referred to as RPC and implemented in `src/rpc` (and inside the Java package `org.openrewrite.rpc` of `rewrite-core`)
  - Further, the serialization mechanism is visitor-based and thus for each of the supported languages there is a "sender" and a "receiver" (e.g. `JavaSender` and `JavaReceiver`) which each needs an implementation in both Java and TypeScript and at the same time this must be fully aligned with the corresponding model (e.g. `src/java/tree.ts`)

### Code Style
- Follow standard TypeScript naming conventions
  - Classes and interfaces: PascalCase
  - Methods, properties, and variables: camelCase
  - Constants: UPPER_SNAKE_CASE
- Use 4 spaces for indentation
- Keep lines under 120 characters
- Use meaningful variable and method names

### TypeScript Patterns
- Use TypeScript interfaces and classes for type safety
- Use generics for reusable code
- Use async/await for asynchronous operations (including the visitor which is async)
- Use Immer for immutable state management
- Use the visitor pattern for tree traversal and transformation
- Use optional chaining for null/undefined handling

### TypeScript Best Practices
- Explicitly type function parameters and return values
- Use readonly for immutable properties
- Use union types instead of inheritance where appropriate
- Avoid any type where possible
- Use type guards for runtime type checking
- Use async/await instead of raw promises

## Version Control Guidelines

### Commits
- Write clear, concise commit messages
- Each commit should represent a logical change
- Keep commits focused on a single task
- Reference issue numbers in commit messages where applicable

### Pull Requests
- Write a clear description of the changes
- Include tests for new features and bug fixes
- Ensure all tests pass before submitting
- Address review comments promptly

## Conclusion
Following these guidelines will help maintain code quality and consistency across the OpenRewrite project. These guidelines are not exhaustive, and common sense should be applied when making decisions not covered here.
