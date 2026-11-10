"use strict";
const $ = id => document.getElementById(id);
const copy = {
    fr: {
        title: "Parlons de votre prochaine visite",
        privacy: "Évitez les données personnelles et médicales dans le chat. Historique temporaire de 30 minutes.",
        question: "Votre question",
        send: "Envoyer",
        open: "Ouvrir le chat",
        close: "Fermer le chat",
        error: "Le service est indisponible. Réessayez plus tard.",
        busy: "Trop de demandes. Réessayez dans une minute.",
        footer: "Descriptions générales uniquement. Pour une question de santé, contactez le personnel qualifié."
    },
    en: {
        title: "Let's talk about your visit",
        privacy: "Keep personal and medical details out of chat. Temporary history lasts 30 minutes.",
        question: "Your question",
        send: "Send",
        open: "Open chat",
        close: "Close chat",
        error: "The service is unavailable. Please try again later.",
        busy: "Too many requests. Please try again in one minute.",
        footer: "General descriptions only. Contact qualified staff for health questions."
    },
    ar: {
        title: "كيف نقدر نعاونك فزيارتك؟",
        privacy: "ما تشاركش معلومات شخصية أو طبية فالدردشة. المحادثة مؤقتة لمدة 30 دقيقة.",
        question: "السؤال ديالك",
        send: "إرسال",
        open: "فتح الدردشة",
        close: "إغلاق الدردشة",
        error: "الخدمة ما متوفراش دابا. عاود حاول من بعد.",
        busy: "طلبات كثيرة. عاود حاول بعد دقيقة.",
        footer: "معلومات عامة فقط. للأسئلة الصحية تواصل مع الطاقم المختص."
    }
};
let language = 'fr';
function setChatOpen(open) {
    $('chat-panel').hidden = !open;
    $('chat-launcher').setAttribute('aria-expanded', String(open));
    (open ? $('message') : $('chat-launcher')).focus();
}
$('chat-launcher').addEventListener('click', () => setChatOpen($('chat-panel').hidden));
$('chat-close').addEventListener('click', () => setChatOpen(false));
$('welcome-chat').addEventListener('click', () => setChatOpen(true));
$('chat-panel').addEventListener('keydown', event => {
    if (event.key === 'Escape') setChatOpen(false);
});
$('language').addEventListener('change', () => {
    language = $('language').value;
    document.documentElement.lang = language;
    document.documentElement.dir = language === 'ar' ? 'rtl' : 'ltr';
    const t = copy[language];
    for (const [id, key] of Object.entries({
        'chat-title': 'title',
        privacy: 'privacy',
        'message-label': 'question',
        send: 'send',
        footer: 'footer'
    })) $(id).textContent = t[key];
    $('message').placeholder = t.question;
    $('chat-launcher').setAttribute('aria-label', t.open);
    $('chat-close').setAttribute('aria-label', t.close);
});

function add(text, user = false) {
    const el = document.createElement('p');
    el.className = 'message' + (user ? ' user' : '');
    el.dir = 'auto';
    el.textContent = text;
    $('messages').append(el);
    while ($('messages').children.length > 40) $('messages').firstChild.remove();
    el.scrollIntoView({block: 'nearest'});
}

async function post(path, body) {
    const response = await fetch(path, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body),
        signal: AbortSignal.timeout(120000)
    });
    if (!response.ok) throw new Error(response.status === 429 ? copy[language].busy : copy[language].error);
    return response.json();
}

$('chat-form').addEventListener('submit', async e => {
    e.preventDefault();
    const message = $('message').value.trim();
    if (!message) return;
    add(message, true);
    $('send').disabled = true;
    $('message').value = '';
    try {
        const data = await post('/api/chat', {message});
        add(data.message);
    } catch (error) {
        add(error.message.startsWith('The operation') ? copy[language].error : error.message);
    } finally {
        $('send').disabled = false;
        if (!$('chat-panel').hidden) $('message').focus();
    }
});
