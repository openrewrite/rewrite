import {Type} from "../java";
import FullyQualified = Type.FullyQualified;

export class MethodMatcher {
    private readonly packagePattern: string;
    private readonly typePattern: string;
    private readonly methodPattern: string;
    private readonly argumentPatterns: string[];

    constructor(pattern: string) {
        // Find the last space before the method spec (which contains parentheses)
        const firstParenIndex = pattern.indexOf('(');
        if (firstParenIndex === -1) {
            throw new Error(`Invalid pattern format: ${pattern} - missing method arguments`);
        }

        // Find the last space before the opening parenthesis
        const lastSpaceBeforeParen = pattern.lastIndexOf(' ', firstParenIndex);
        if (lastSpaceBeforeParen === -1) {
            throw new Error(`Invalid pattern format: ${pattern}`);
        }

        const typeSpec = pattern.substring(0, lastSpaceBeforeParen).trim();
        const methodSpec = pattern.substring(lastSpaceBeforeParen + 1).trim();

        // Parse type specification (package.Type or just Type)
        // Special case: *..* pattern (any package, any type)
        if (typeSpec === '*..*') {
            this.packagePattern = '*..';
            this.typePattern = '*';
        } else {
            const lastDotIndex = typeSpec.lastIndexOf('.');
            if (lastDotIndex === -1) {
                this.packagePattern = '*';
                this.typePattern = typeSpec;
            } else {
                // Check if we're splitting a *.. pattern incorrectly
                const potentialPackage = typeSpec.substring(0, lastDotIndex);
                if (potentialPackage.endsWith('*..')) {
                    // Don't split *.. pattern - it should stay together
                    this.packagePattern = potentialPackage;
                    this.typePattern = typeSpec.substring(lastDotIndex + 1);
                } else {
                    this.packagePattern = potentialPackage;
                    this.typePattern = typeSpec.substring(lastDotIndex + 1);
                }
            }
        }

        // Parse method specification methodName(args)
        const methodParenIndex = methodSpec.indexOf('(');
        if (methodParenIndex === -1 || !methodSpec.endsWith(')')) {
            throw new Error(`Invalid method specification: ${methodSpec}`);
        }

        this.methodPattern = methodSpec.substring(0, methodParenIndex);
        const argsString = methodSpec.substring(methodParenIndex + 1, methodSpec.length - 1);

        // Parse arguments
        if (argsString.trim() === '..') {
            this.argumentPatterns = ['..'];
        } else if (argsString.trim() === '') {
            this.argumentPatterns = [];
        } else {
            this.argumentPatterns = argsString.split(',').map(arg => arg.trim());
        }
    }

    matches(method?: Type.Method): boolean {
        if (!method) {
            return false;
        }

        // Extract fully qualified name from declaringType
        const fullyQualifiedName = FullyQualified.getFullyQualifiedName(method.declaringType);

        // Split fully qualified name into package and type
        const lastDotIndex = fullyQualifiedName.lastIndexOf('.');
        const packageName = lastDotIndex === -1 ? '' : fullyQualifiedName.substring(0, lastDotIndex);
        const typeName = lastDotIndex === -1 ? fullyQualifiedName : fullyQualifiedName.substring(lastDotIndex + 1);

        // Match package
        if (!this.matchesPackage(packageName)) {
            return false;
        }

        // Match type (normalize primitives for matching)
        const normalizedTypePattern = this.normalizePrimitiveType(this.typePattern);
        const normalizedTypeName = this.normalizePrimitiveType(typeName);
        if (!this.matchesPattern(normalizedTypePattern, normalizedTypeName)) {
            return false;
        }

        // Match method name
        if (!this.matchesPattern(this.methodPattern, method.name)) {
            return false;
        }

        // Match arguments - convert Type[] to string representations
        const argStrings = method.parameterTypes.map(type => this.typeToString(type));
        return this.matchesArguments(argStrings);
    }

    private typeToString(type: Type): string {
        switch (type.kind) {
            case Type.Kind.Primitive:
                return (type as Type.Primitive).keyword;
            case Type.Kind.Class:
                return (type as Type.Class).fullyQualifiedName;
            case Type.Kind.Parameterized:
                return FullyQualified.getFullyQualifiedName((type as Type.Parameterized).type);
            case Type.Kind.Array:
                const arrayType = type as Type.Array;
                return this.typeToString(arrayType.elemType) + '[]';
            case Type.Kind.GenericTypeVariable:
                return (type as Type.GenericTypeVariable).name;
            default:
                return 'unknown';
        }
    }

    private matchesPackage(packageName?: string): boolean {
        const pkg = packageName || '';

        // Handle *..*  pattern (any package including nested)
        if (this.packagePattern === '*..') {
            return true;
        }

        // Handle * pattern (no package or any single-level package)
        if (this.packagePattern === '*') {
            return true;
        }

        // Handle lib.* pattern (lib and any subpackage)
        if (this.packagePattern.endsWith('.*')) {
            const prefix = this.packagePattern.slice(0, -2);
            return pkg === prefix || pkg.startsWith(prefix + '.');
        }

        // Exact match
        return pkg === this.packagePattern;
    }

    private matchesPattern(pattern: string, value: string): boolean {
        if (pattern === '*') {
            return true;
        }

        // Handle patterns with wildcards
        if (pattern.includes('*')) {
            // Convert pattern to regex
            const regexPattern = pattern
                .replace(/[.+?^${}()|[\]\\]/g, '\\$&') // Escape special chars except *
                .replace(/\*/g, '.*'); // Replace * with .*
            const regex = new RegExp(`^${regexPattern}$`);
            return regex.test(value);
        }

        return pattern === value;
    }

    private matchesArguments(args: string[]): boolean {
        // Handle .. pattern (any arguments)
        if (this.argumentPatterns.length === 1 && this.argumentPatterns[0] === '..') {
            return true;
        }

        // Handle patterns with .. in arguments
        let patternIndex = 0;
        let argIndex = 0;

        while (patternIndex < this.argumentPatterns.length && argIndex < args.length) {
            const pattern = this.argumentPatterns[patternIndex];

            if (pattern === '..') {
                // If .. is the last pattern, it matches all remaining args
                if (patternIndex === this.argumentPatterns.length - 1) {
                    return true;
                }

                // Otherwise, try to match the next pattern with remaining args
                const nextPattern = this.argumentPatterns[patternIndex + 1];
                let matched = false;
                for (let i = argIndex; i < args.length; i++) {
                    if (this.matchesArgumentPattern(nextPattern, args[i])) {
                        argIndex = i + 1;
                        patternIndex += 2;
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return false;
                }
            } else {
                if (!this.matchesArgumentPattern(pattern, args[argIndex])) {
                    return false;
                }
                patternIndex++;
                argIndex++;
            }
        }

        // Check if all patterns and arguments are consumed
        if (patternIndex < this.argumentPatterns.length) {
            // Allow trailing .. to match zero arguments
            return patternIndex === this.argumentPatterns.length - 1 &&
                this.argumentPatterns[patternIndex] === '..';
        }

        return argIndex === args.length;
    }

    private matchesArgumentPattern(pattern: string, arg: string): boolean {
        // Handle type patterns like lib.Array
        if (pattern.includes('.')) {
            // Also check if the pattern is a TypeScript primitive class (lib.Number, lib.String, lib.Boolean)
            const normalizedPattern = this.normalizePrimitiveType(pattern);
            const normalizedArg = this.normalizePrimitiveType(arg);
            return normalizedPattern === normalizedArg;
        }

        // Normalize TypeScript primitive names to match both primitive and class representations
        const normalizedPattern = this.normalizePrimitiveType(pattern);
        const normalizedArg = this.normalizePrimitiveType(arg);

        return normalizedPattern === normalizedArg;
    }

    private normalizePrimitiveType(type: string): string {
        switch (type) {
            case 'number':
            case 'Number':
            case 'lib.Number':
            case 'double':
                return 'Number';
            case 'string':
            case 'String':
            case 'lib.String':
                return 'String';
            case 'boolean':
            case 'Boolean':
            case 'lib.Boolean':
                return 'Boolean';
            default:
                return type;
        }
    }
}
