import {SnowflakeId} from '@akashrajpurohit/snowflake-id';

export class IdentityMap {
    private objectMap = new WeakMap<object, string>();
    private primitiveMap = new Map<any, string>();

    set(key: any, value: string): void {
        if (typeof key === 'object' && key !== null) {
            this.objectMap.set(key, value);
        } else {
            this.primitiveMap.set(key, value);
        }
    }

    get(key: any): string | undefined {
        if (typeof key === 'object' && key !== null) {
            return this.objectMap.get(key);
        } else {
            return this.primitiveMap.get(key);
        }
    }

    has(key: any): boolean {
        if (typeof key === 'object' && key !== null) {
            return this.objectMap.has(key);
        } else {
            return this.primitiveMap.has(key);
        }
    }

    delete(key: any): boolean {
        if (typeof key === 'object' && key !== null) {
            return this.objectMap.delete(key);
        } else {
            return this.primitiveMap.delete(key);
        }
    }
}

export class ObjectStore {
    private readonly objects: Map<string, any>;
    private readonly objectIds: IdentityMap;
    private readonly objectIdToVersion: Map<string, string>; // objectId -> currentVersionId
    private readonly snowflake: ReturnType<typeof SnowflakeId>;

    constructor(snowflake: ReturnType<typeof SnowflakeId>) {
        this.objects = new Map<string, any>();
        this.objectIds = new IdentityMap();
        this.objectIdToVersion = new Map<string, string>();
        this.snowflake = snowflake;
    }

    /**
     * Check if an object has an intrinsic ID property.
     */
    private hasIntrinsicId(obj: any): boolean {
        return obj && typeof obj === 'object' && typeof obj.id === 'string';
    }

    /**
     * Get the intrinsic ID from an object.
     */
    private getIntrinsicId(obj: any): string | undefined {
        return this.hasIntrinsicId(obj) ? obj.id : undefined;
    }

    /**
     * Create a composite ID from object ID and version ID.
     */
    private createCompositeId(objectId: string, versionId: string): string {
        return `${objectId}@${versionId}`;
    }

    /**
     * Parse a composite ID into object ID and version ID.
     * Returns null if not a composite ID.
     */
    private parseCompositeId(compositeId: string): { objectId: string, versionId: string } | null {
        const atIndex = compositeId.indexOf('@');
        if (atIndex === -1) {
            return null;
        }
        return {
            objectId: compositeId.substring(0, atIndex),
            versionId: compositeId.substring(atIndex + 1)
        };
    }

    /**
     * Get the current version ID for an object ID.
     */
    getCurrentVersion(objectId: string): string | undefined {
        return this.objectIdToVersion.get(objectId);
    }

    /**
     * Store an object with an optional ID. If no ID is provided, a new one will be generated.
     * For objects with intrinsic IDs, creates composite IDs with versioning.
     * Returns the ID used to store the object.
     */
    store<T>(obj: T, id?: string): string {
        const currentId = this.objectIds.get(obj);
        const intrinsicId = this.getIntrinsicId(obj);
        
        if (!currentId) {
            // Object not yet stored
            let actualId: string;
            
            if (intrinsicId && !id) {
                // Object has intrinsic ID, create composite ID with new version
                const versionId = this.snowflake.generate();
                actualId = this.createCompositeId(intrinsicId, versionId);
                this.objectIdToVersion.set(intrinsicId, versionId);
            } else if (intrinsicId && id) {
                // Object has intrinsic ID and specific ID provided
                const parsed = this.parseCompositeId(id);
                if (parsed && parsed.objectId === intrinsicId) {
                    actualId = id;
                    this.objectIdToVersion.set(intrinsicId, parsed.versionId);
                } else {
                    // Provided ID doesn't match intrinsic ID, create new composite
                    const versionId = this.snowflake.generate();
                    actualId = this.createCompositeId(intrinsicId, versionId);
                    this.objectIdToVersion.set(intrinsicId, versionId);
                }
            } else {
                // Object has no intrinsic ID, use simple Snowflake ID
                actualId = id ?? this.snowflake.generate();
            }
            
            this.objects.set(actualId, obj);
            this.objectIds.set(obj, actualId);
            return actualId;
        } else if (currentId !== id && id !== undefined) {
            // Object already stored with different ID, update it
            this.objects.delete(currentId);
            this.objects.set(id, obj);
            this.objectIds.set(obj, id);
            
            // Update version mapping if object has intrinsic ID
            if (intrinsicId) {
                const parsed = this.parseCompositeId(id);
                if (parsed && parsed.objectId === intrinsicId) {
                    this.objectIdToVersion.set(intrinsicId, parsed.versionId);
                }
            }
            
            return id;
        } else {
            // Object already stored with same ID or no new ID provided
            return currentId;
        }
    }

    /**
     * Get an object by its ID.
     */
    get<T>(id: string): T | undefined {
        return this.objects.get(id) as T | undefined;
    }

    /**
     * Get the ID associated with an object.
     */
    getId(obj: any): string | undefined {
        return this.objectIds.get(obj);
    }

    /**
     * Check if an object is stored.
     */
    has(obj: any): boolean {
        return this.objectIds.has(obj);
    }

    /**
     * Check if an ID exists in the store.
     */
    hasId(id: string): boolean {
        return this.objects.has(id);
    }

    /**
     * Remove an object by its ID.
     */
    remove(id: string): boolean {
        const obj = this.objects.get(id);
        if (obj !== undefined) {
            this.objects.delete(id);
            this.objectIds.delete(obj);
            return true;
        }
        return false;
    }

    /**
     * Clear all stored objects.
     */
    clear(): void {
        this.objects.clear();
        this.objectIdToVersion.clear();
        // Note: WeakMap in objectIds will be garbage collected automatically
    }

    /**
     * Get the number of stored objects.
     */
    get size(): number {
        return this.objects.size;
    }
}