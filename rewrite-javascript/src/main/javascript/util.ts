export async function mapAsync<T, U>(arr: T[], fn: (t: T, i: number) => Promise<U | undefined>): Promise<U[]> {
    return (await Promise.all(arr.map(fn))).filter(v => v !== undefined);
}
