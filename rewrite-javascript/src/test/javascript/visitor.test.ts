import {produceAsync} from "../../main/javascript";

describe('produceAsync', () => {
    interface State {
        readonly a: number;
    }

    const before: State = {a: 1};

    test('async update', async () => {
        const updateA = async () => 2;
        const after = produceAsync<State>(before, async (draft) => {
            draft.a = await updateA();
            return draft;
        });
        await expect(after).resolves.toEqual({a: 2})
    });

    test('sync update', async () => {
        const after = produceAsync(before, (draft) => {
            draft.a = 2;
            return draft;
        });
        await expect(after).resolves.toEqual({a: 2})
    });
});
