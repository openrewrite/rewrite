export async function mapAsync<T>(arr: T[], fn: (t: T, i: number) => Promise<T | undefined>): Promise<T[]> {
    return (await Promise.all(arr.map(fn))).filter(v => v !== undefined);
}
