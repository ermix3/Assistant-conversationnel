"use strict";
const $ = id => document.getElementById(id);
const copy = {
    fr: {
        title: "Parlons de votre prochaine visite",
        privacy: "Évitez les données personnelles et médicales dans le chat. Historique temporaire de 30 minutes.",
        question: "Votre question",
        send: "Envoyer",
        callback: "Être rappelé",
        note: "Une demande de rappel ne confirme aucun rendez-vous.",
        name: "Nom",
        phone: "Téléphone marocain",
        consent: "J’accepte que le club conserve mon nom et téléphone pour me rappeler. Ne partagez aucune information médicale.",
        request: "Demander un rappel",
        saved: "Demande de rappel enregistrée. Aucun rendez-vous n’est confirmé.",
        error: "Le service est indisponible. Réessayez plus tard.",
        busy: "Trop de demandes. Réessayez dans une minute.",
        footer: "Descriptions générales uniquement. Pour une question de santé, contactez le personnel qualifié."
    },
    en: {
        title: "Let's talk about your visit",
        privacy: "Keep personal and medical details out of chat. Temporary history lasts 30 minutes.",
        question: "Your question",
        send: "Send",
        callback: "Request a callback",
        note: "A callback request does not confirm an appointment.",
        name: "Name",
        phone: "Moroccan phone number",
        consent: "I agree that the club may store my name and phone number to call me back. Do not share medical information.",
        request: "Request a callback",
        saved: "Callback request saved. No appointment is confirmed.",
        error: "The service is unavailable. Please try again later.",
        busy: "Too many requests. Please try again in one minute.",
        footer: "General descriptions only. Contact qualified staff for health questions."
    },
    ar: {
        title: "كيف نقدر نعاونك فزيارتك؟",
        privacy: "ما تشاركش معلومات شخصية أو طبية فالدردشة. المحادثة مؤقتة لمدة 30 دقيقة.",
        question: "السؤال ديالك",
        send: "إرسال",
        callback: "طلب اتصال",
        note: "طلب الاتصال ما كيأكد حتى موعد.",
        name: "الاسم",
        phone: "رقم الهاتف المغربي",
        consent: "كنوافق أن النادي يحتافظ باسمي ورقم هاتفي باش يتاصل بيا. ما تشاركش معلومات طبية.",
        request: "طلب اتصال",
        saved: "تسجل طلب الاتصال. ما تأكد حتى موعد.",
        error: "الخدمة ما متوفراش دابا. عاود حاول من بعد.",
        busy: "طلبات كثيرة. عاود حاول بعد دقيقة.",
        footer: "معلومات عامة فقط. للأسئلة الصحية تواصل مع الطاقم المختص."
    }
};
let language = 'fr';
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
        'callback-title': 'callback',
        'callback-note': 'note',
        'name-label': 'name',
        'phone-label': 'phone',
        'consent-text': 'consent',
        'callback-send': 'request',
        footer: 'footer'
    })) $(id).textContent = t[key];
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
        $('message').focus();
    }
});
$('callback-form').addEventListener('submit', async e => {
    e.preventDefault();
    $('callback-send').disabled = true;
    try {
        await post('/api/callbacks', {
            name: $('name').value.trim(),
            phone: $('phone').value,
            language,
            consent: $('consent').checked
        });
        $('callback-form').reset();
        $('callback-status').textContent = copy[language].saved;
    } catch (error) {
        $('callback-status').textContent = copy[language].error;
    } finally {
        $('callback-send').disabled = false;
    }
});
