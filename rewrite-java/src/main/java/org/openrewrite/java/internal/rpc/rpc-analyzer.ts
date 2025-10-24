import * as fs from 'fs';
import * as path from 'path';

interface PropertyExtractor {
    extract: (methodBody: string) => string[];
}

const senderExtractor: PropertyExtractor = {
    extract: (methodBody: string): string[] => {
        const properties: string[] = [];
        const seen = new Set<string>();
        const excludedProps = new Set(['id', 'prefix', 'markers', 'padding', 'element', 'valueSourceIndex', 'codePoint']);

        // Extract each q.getAndSend call separately to preserve order
        const sendCalls = methodBody.split(/(?=q\.getAndSend)/);

        for (const call of sendCalls) {
            if (!call.trim().startsWith('q.getAndSend')) continue;

            // Find all .get/.is method calls, take the last non-excluded one
            const getMatches = Array.from(call.matchAll(/::(get|is)([A-Z]\w+)|\.(get|is)([A-Z]\w+)\(\)/g));

            // Try from last to first to get the actual property (not intermediate calls like getPadding)
            for (let i = getMatches.length - 1; i >= 0; i--) {
                const match = getMatches[i];
                // match[2] is from ::get/is, match[4] is from .get/is()
                const propName = match[2] || match[4];
                if (propName && propName !== 'ValueNonNull') {
                    const prop = propName.charAt(0).toLowerCase() + propName.slice(1);
                    if (!excludedProps.has(prop) && !seen.has(prop)) {
                        properties.push(prop);
                        seen.add(prop);
                        break; // Found the property for this call
                    }
                }
            }
        }

        return properties;
    }
};

const receiverExtractor: PropertyExtractor = {
    extract: (methodBody: string): string[] => {
        const properties: string[] = [];
        const seen = new Set<string>();

        // Find .withPropertyName( calls in order they appear
        // Handle optional type parameters like .<Type>withPropertyName(
        const withPattern = /\.(?:<[^>]+>)?with([A-Z]\w+)\(/g;
        let match;
        while ((match = withPattern.exec(methodBody)) !== null) {
            const propName = match[1];
            const prop = propName.charAt(0).toLowerCase() + propName.slice(1);
            if (!seen.has(prop)) {
                properties.push(prop);
                seen.add(prop);
            }
        }

        return properties;
    }
};

function analyzeJavaClass(content: string, className: 'JavaSender' | 'JavaReceiver', extractor: PropertyExtractor): Map<string, string[]> {
    const results = new Map<string, string[]>();

    // Match visitor methods by finding the next @Override or end of class
    // This handles complex multiline chained methods better
    // Also match specific return types like J.TypeParameters (not just J)
    const methodSplitPattern = /@Override\s+public J(?:\.\w+)? visit(\w+)\(J\.(\w+(?:\.\w+)?)\s+\w+,\s*Rpc(?:Send|Receive)Queue\s+q\)/g;

    const matches: Array<{methodName: string, typeName: string, startIndex: number}> = [];
    let match;
    while ((match = methodSplitPattern.exec(content)) !== null) {
        matches.push({
            methodName: match[1],
            typeName: match[2],
            startIndex: match.index + match[0].length
        });
    }

    // Extract method bodies by taking content between method starts
    for (let i = 0; i < matches.length; i++) {
        const current = matches[i];
        const nextStart = i < matches.length - 1 ? matches[i + 1].startIndex : content.length;
        let methodBodyEnd = nextStart;

        // Find the actual end of this method by looking for the next method declaration (public/private/protected)
        const restOfContent = content.substring(current.startIndex, nextStart);
        const nextMethodMatch = restOfContent.match(/\n\s+(public|private|protected)\s+/);
        if (nextMethodMatch && nextMethodMatch.index !== undefined) {
            methodBodyEnd = current.startIndex + nextMethodMatch.index;
        }

        const methodBody = content.substring(current.startIndex, methodBodyEnd);

        const properties = extractor.extract(methodBody);

        if (properties.length > 0) {
            results.set(current.typeName, properties);
        }
    }

    return results;
}

function generateReport() {
    const currentDir = process.cwd();
    const senderPath = path.join(currentDir, 'JavaSender.java');
    const receiverPath = path.join(currentDir, 'JavaReceiver.java');

    const senderContent = fs.readFileSync(senderPath, 'utf-8');
    const receiverContent = fs.readFileSync(receiverPath, 'utf-8');

    const senderData = analyzeJavaClass(senderContent, 'JavaSender', senderExtractor);
    const receiverData = analyzeJavaClass(receiverContent, 'JavaReceiver', receiverExtractor);

    // Build JavaSender output
    let senderOutput = 'JavaSender (Java)\n';
    senderOutput += '='.repeat(80) + '\n';

    const sortedSenderKeys = Array.from(senderData.keys()).sort();
    for (const key of sortedSenderKeys) {
        const props = senderData.get(key)!;
        senderOutput += `- ${key} (${props.join(', ')})\n`;
    }

    // Build JavaReceiver output
    let receiverOutput = 'JavaReceiver (Java)\n';
    receiverOutput += '='.repeat(80) + '\n';

    const sortedReceiverKeys = Array.from(receiverData.keys()).sort();
    for (const key of sortedReceiverKeys) {
        const props = receiverData.get(key)!;
        receiverOutput += `- ${key} (${props.join(', ')})\n`;
    }

    // Write to files
    const senderFile = path.join(currentDir, 'java-rpc-sender-summary.txt');
    const receiverFile = path.join(currentDir, 'java-rpc-receiver-summary.txt');

    fs.writeFileSync(senderFile, senderOutput, 'utf-8');
    fs.writeFileSync(receiverFile, receiverOutput, 'utf-8');

    console.log(`✓ Generated ${senderFile}`);
    console.log(`✓ Generated ${receiverFile}`);
}

// Run the analysis
generateReport();
