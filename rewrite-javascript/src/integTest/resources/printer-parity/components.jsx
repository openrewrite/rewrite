import React from 'react';

const props = { className: 'button', disabled: false };

export function Simple() {
    return <div/>;
}

export function Attributes({ label, onClick }) {
    return (
        <button
            type="button"
            data-testid='attributes'
            aria-label={label}
            onClick={() => onClick(label)}
            disabled
            {...props}
            { ...{ extra: true } }
        >
            {label}
            {/* a comment */}
            {}
        </button>
    );
}

export const Fragment = () => (
    <>
        <React.Fragment key="a">text &amp; entity</React.Fragment>
        <svg:rect xlink:href="#a" width={10} />
        <Member.Access.Deep value={<span>nested</span>}></Member.Access.Deep>
        < Spaced   attr = "1"   >   padded   </ Spaced >
    </>
);

export const Conditional = ({ items }) => (
    <ul>
        {items.length > 0 && items.map(item => <li key={item.id}>{item.name}</li>)}
        {items.length === 0 ? <li>empty</li> : null}
    </ul>
);
