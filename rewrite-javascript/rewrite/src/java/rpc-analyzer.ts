import * as fs from 'fs';
import * as path from 'path';

interface PropertyExtractor {
    pattern: RegExp;
    extract: (match: RegExpExecArray, seenProps: Set<string>) => string | null;
}

const senderExtractor: PropertyExtractor = {
    // Match property accesses in getAndSend/getAndSendList calls (first arrow function only)
    // Pattern: q.getAndSend[List]?(object, variable => variable.property, ...)
    pattern: /q\.getAndSend(?:List)?\([^,]+,\s*[a-z]+\s*=>\s*(?:asRef\()?\s*([a-z]+)\.(\w+)/g,
    extract: (match, seenProps) => {
        const identifier = match[1];
        const prop = match[2];

        // Exclude common base properties (but NOT 'element' as it can be a real property)
        const excludedProps = new Set(['id', 'prefix', 'markers', 'before', 'after', 'whitespace', 'comments', 'kind', 'suffix']);

        // Skip if calling methods on 'this'
        if (identifier === 'this') {
            return null;
        }

        if (!seenProps.has(prop) && !excludedProps.has(prop)) {
            return prop;
        }
        return null;
    }
};

const receiverExtractor: PropertyExtractor = {
    // Match properties in updates object: propertyName: await q.receive... or propertyName: (await q.receive...
    pattern: /(\w+):\s*\(?await\s+q\.receive/g,
    extract: (match, seenProps) => {
        const prop = match[1];
        if (!seenProps.has(prop)) {
            return prop;
        }
        return null;
    }
};

function analyzeClass(content: string, queueType: 'RpcSendQueue' | 'RpcReceiveQueue', extractor: PropertyExtractor): Map<string, string[]> {
    const results = new Map<string, string[]>();

    // Match method signatures and their bodies
    const terminatorPattern = queueType === 'RpcSendQueue'
        ? /(?=protected async visit|\s*private typeVisitor|\s*public override async visit)/
        : /(?=protected async visit|\s*public override async visit)/;

    const methodPattern = new RegExp(
        `protected async visit(\\w+)\\([^:]+:\\s*J\\.(\\S+?),\\s*q:\\s*${queueType}\\)[^{]*\\{([\\s\\S]*?)${terminatorPattern.source}`,
        'g'
    );

    let match;
    while ((match = methodPattern.exec(content)) !== null) {
        const typeName = match[2];
        const methodBody = match[3];
        const properties: string[] = [];
        const seenProps = new Set<string>();

        let propMatch;
        while ((propMatch = extractor.pattern.exec(methodBody)) !== null) {
            const prop = extractor.extract(propMatch, seenProps);
            if (prop) {
                seenProps.add(prop);
                properties.push(prop);
            }
        }

        if (properties.length > 0) {
            // Remove generic type parameters from type name
            const cleanTypeName = typeName.replace(/<.*>/, '');
            results.set(cleanTypeName, properties);
        }
    }

    return results;
}

function generateReport() {
    const filePath = path.join(__dirname, 'rpc.ts');
    const content = fs.readFileSync(filePath, 'utf-8');

    // Split content into JavaSender and JavaReceiver sections
    const senderMatch = content.match(/export class JavaSender[\s\S]*?(?=export class JavaReceiver)/);
    const receiverMatch = content.match(/export class JavaReceiver[\s\S]*?(?=export function registerJLanguageCodecs)/);

    if (!senderMatch || !receiverMatch) {
        console.error('Could not find JavaSender or JavaReceiver classes');
        return;
    }

    const senderContent = senderMatch[0];
    const receiverContent = receiverMatch[0];

    const senderData = analyzeClass(senderContent, 'RpcSendQueue', senderExtractor);
    const receiverData = analyzeClass(receiverContent, 'RpcReceiveQueue', receiverExtractor);

    // Build JavaSender output
    let senderOutput = 'JavaSender\n';
    senderOutput += '='.repeat(80) + '\n';

    const sortedSenderKeys = Array.from(senderData.keys()).sort();
    for (const key of sortedSenderKeys) {
        const props = senderData.get(key)!;
        senderOutput += `- ${key} (${props.join(', ')})\n`;
    }

    // Build JavaReceiver output
    let receiverOutput = 'JavaReceiver\n';
    receiverOutput += '='.repeat(80) + '\n';

    const sortedReceiverKeys = Array.from(receiverData.keys()).sort();
    for (const key of sortedReceiverKeys) {
        const props = receiverData.get(key)!;
        receiverOutput += `- ${key} (${props.join(', ')})\n`;
    }

    // Write to files
    const senderFile = path.join(__dirname, 'rpc-sender-summary.txt');
    const receiverFile = path.join(__dirname, 'rpc-receiver-summary.txt');

    fs.writeFileSync(senderFile, senderOutput, 'utf-8');
    fs.writeFileSync(receiverFile, receiverOutput, 'utf-8');

    console.log(`✓ Generated ${senderFile}`);
    console.log(`✓ Generated ${receiverFile}`);
}

// Run the analysis
generateReport();
