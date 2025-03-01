export async function mapAsync<T>(arr: T[], fn: (t: T) => Promise<T | undefined>): Promise<T[]> {
    return (await Promise.all(arr.map(fn))).filter(v => v !== undefined);
}
