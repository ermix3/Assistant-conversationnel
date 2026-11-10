const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {runInNewContext} = require('node:vm');

function widget() {
    const elements = new Map();
    let focused;
    const document = {
        documentElement: {},
        getElementById(id) {
            if (!elements.has(id)) elements.set(id, {
                hidden: id === 'chat-panel',
                listeners: {}, attributes: {},
                addEventListener(name, handler) { this.listeners[name] = handler; },
                setAttribute(name, value) { this.attributes[name] = value; },
                focus() { focused = id; }
            });
            return elements.get(id);
        }
    };
    runInNewContext(readFileSync('src/main/resources/static/widget.js', 'utf8'), {document});
    return {get: id => document.getElementById(id), document, focused: () => focused};
}

test('launcher opens the chat and the close button restores keyboard focus', () => {
    const w = widget();
    w.get('chat-launcher').listeners.click();
    assert.equal(w.get('chat-panel').hidden, false);
    assert.equal(w.get('chat-launcher').attributes['aria-expanded'], 'true');
    assert.equal(w.focused(), 'message');
    w.get('chat-close').listeners.click();
    assert.equal(w.get('chat-panel').hidden, true);
    assert.equal(w.get('chat-launcher').attributes['aria-expanded'], 'false');
    assert.equal(w.focused(), 'chat-launcher');
});

test('Escape closes the chat while preserving the draft', () => {
    const w = widget();
    w.get('chat-launcher').listeners.click();
    w.get('message').value = 'Bonjour';
    w.get('chat-panel').listeners.keydown({key: 'Escape'});
    assert.equal(w.get('chat-panel').hidden, true);
    w.get('chat-launcher').listeners.click();
    assert.equal(w.get('message').value, 'Bonjour');
});

test('Arabic translates widget controls and keeps the language when reopening', () => {
    const w = widget();
    w.get('language').value = 'ar';
    w.get('language').listeners.change();
    assert.equal(w.document.documentElement.dir, 'rtl');
    assert.equal(w.get('chat-launcher').attributes['aria-label'], 'فتح الدردشة');
    assert.equal(w.get('chat-close').attributes['aria-label'], 'إغلاق الدردشة');
    w.get('chat-launcher').listeners.click();
    w.get('chat-close').listeners.click();
    w.get('chat-launcher').listeners.click();
    assert.equal(w.get('language').value, 'ar');
    assert.equal(w.get('message').placeholder, 'السؤال ديالك');
});

test('welcome invitation opens the conversation and focuses the composer', () => {
    const w = widget();
    w.get('welcome-chat').listeners.click();
    assert.equal(w.get('chat-panel').hidden, false);
    assert.equal(w.get('chat-launcher').attributes['aria-expanded'], 'true');
    assert.equal(w.focused(), 'message');
});
