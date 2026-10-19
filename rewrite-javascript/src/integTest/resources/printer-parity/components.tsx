import * as React from 'react';

interface ListProps<T> {
    items: T[];
    render: (item: T) => React.ReactNode;
    title?: string;
}

function List<T>({ items, render, title = "List" }: ListProps<T>): JSX.Element {
    return (
        <section aria-label={title}>
            <h1>{title!}</h1>
            <ul>{items.map((item, index) => <li key={index}>{render(item)}</li>)}</ul>
        </section>
    );
}

const identity = <T, >(value: T): T => value;

export const App: React.FC<{ names: string[] }> = ({ names }) => {
    const [selected, setSelected] = React.useState<string | null>(null);
    return (
        <>
            <List<string> items={names} render={name => <b onClick={() => setSelected(name)}>{name}</b>}/>
            <p className="selected">{(selected as string) ?? identity("none")}</p>
        </>
    );
};
