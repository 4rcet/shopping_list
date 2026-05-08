// Инициализация пользователя
function getUser() {
    let savedUser = localStorage.getItem('shopping_user');
    if (!savedUser) {
        // Простая генерация ID, если crypto.randomUUID недоступен
        const uuid = (window.crypto && crypto.randomUUID) ? crypto.randomUUID() : Math.random().toString(36).substring(2);
        savedUser = JSON.stringify({
            id: uuid,
            avatar: `https://api.dicebear.com/7.x/bottts/svg?seed=${Math.random()}`
        });
        localStorage.setItem('shopping_user', savedUser);
    }
    return JSON.parse(savedUser);
}

const currentUser = getUser();
document.getElementById('my-avatar').src = currentUser.avatar;

let productsData = [];
let eventsData = [];
let stompClient = null;

// Инициализация WebSocket
function connectWebSocket() {
    stompClient = new window.StompJs.Client({
        webSocketFactory: () => new SockJS('/ws'),
        onConnect: () => {
            console.log('Connected to WebSocket');
            stompClient.subscribe('/topic/products', (msg) => {
                productsData = JSON.parse(msg.body);
                renderProducts();
            });
            stompClient.subscribe('/topic/events', (msg) => {
                eventsData = JSON.parse(msg.body);
                renderEvents();
            });
        }
    });
    stompClient.activate();
}

// Загрузка начальных данных по REST
async function fetchInitialData() {
    try {
        const prodRes = await fetch('/api/products');
        productsData = await prodRes.json();
        renderProducts();

        const evtRes = await fetch('/api/events');
        eventsData = await evtRes.json();
        renderEvents();
    } catch (err) {
        console.error("Ошибка загрузки данных:", err);
    }
}

// Отрисовка продуктов
function renderProducts() {
    const approvedListEl = document.getElementById('approved-list');
    const suggestionsListEl = document.getElementById('suggestions-list');
    const suggestionsContainer = document.getElementById('suggestions-container');
    const emptyMsg = document.getElementById('empty-list-msg');

    approvedListEl.innerHTML = '';
    suggestionsListEl.innerHTML = '';

    const approved = productsData.filter(p => p.approved);
    const suggestions = productsData.filter(p => !p.approved);

    // Предложения
    if (suggestions.length > 0) {
        suggestionsContainer.classList.remove('hidden');
        suggestions.forEach(p => {
            const hasVoted = p.voters.includes(currentUser.id);
            const voteBtnHTML = !hasVoted ? `<button onclick="vote('${p.id}')">👍 За</button>` : '';
            
            suggestionsListEl.innerHTML += `
                <div class="item-row">
                    <span>
                        <img src="${p.addedByAvatar}" width="24" style="vertical-align: middle; margin-right: 5px; border-radius: 50%;">
                        ${p.name} - <b>${p.price} ₽</b>
                    </span>
                    <div>
                        <span style="margin-right: 10px;">Голосов: ${p.voters.length}/3</span>
                        ${voteBtnHTML}
                    </div>
                </div>
            `;
        });
    } else {
        suggestionsContainer.classList.add('hidden');
    }

    // Общий список
    if (approved.length > 0) {
        emptyMsg.style.display = 'none';
        approved.forEach(p => {
            const rowClass = p.bought ? 'item-row bought' : 'item-row';
            const btnText = p.bought ? 'Вернуть' : 'Купили';
            
            approvedListEl.innerHTML += `
                <div class="${rowClass}">
                    <span class="name">${p.name} — ${p.price} ₽</span>
                    <div>
                        <button onclick="toggleBought('${p.id}')" style="margin-right: 5px;">${btnText}</button>
                        <button onclick="deleteProduct('${p.id}')">Удалить</button>
                    </div>
                </div>
            `;
        });
    } else {
        emptyMsg.style.display = 'block';
    }
}

// Отрисовка событий
function renderEvents() {
    const eventsListEl = document.getElementById('events-list');
    const emptyMsg = document.getElementById('empty-events-msg');
    
    eventsListEl.innerHTML = '';
    
    if (eventsData.length > 0) {
        emptyMsg.style.display = 'none';
        eventsData.forEach(ev => {
            eventsListEl.innerHTML += `
                <div class="event-row">
                    <img src="${ev.userAvatar}" width="30" alt="avatar">
                    <div style="flex: 1;">
                        <div><b>${ev.action}</b>: ${ev.productName}</div>
                        <div style="font-size: 12px; color: #888;">${ev.timestamp}</div>
                    </div>
                </div>
            `;
        });
    } else {
        emptyMsg.style.display = 'block';
    }
}

// Действия
document.getElementById('add-form').addEventListener('submit', (e) => {
    e.preventDefault();
    const nameInput = document.getElementById('prod-name');
    const priceInput = document.getElementById('prod-price');
    
    if (stompClient && stompClient.connected) {
        stompClient.publish({
            destination: '/app/addProduct',
            body: JSON.stringify({ 
                name: nameInput.value, 
                price: parseFloat(priceInput.value), 
                addedByAvatar: currentUser.avatar 
            })
        });
        nameInput.value = '';
        priceInput.value = '';
    }
});

function vote(productId) {
    if (stompClient && stompClient.connected) {
        stompClient.publish({
            destination: '/app/voteProduct',
            body: JSON.stringify({ productId, userId: currentUser.id })
        });
    }
}

function toggleBought(productId) {
    if (stompClient && stompClient.connected) {
        stompClient.publish({
            destination: '/app/toggleBought',
            body: JSON.stringify({ productId, userAvatar: currentUser.avatar })
        });
    }
}

function deleteProduct(productId) {
    if (stompClient && stompClient.connected) {
        stompClient.publish({
            destination: '/app/deleteProduct',
            body: JSON.stringify({ productId, userAvatar: currentUser.avatar })
        });
    }
}

// Переключение вкладок
function switchTab(tab) {
    const viewList = document.getElementById('view-list');
    const viewHistory = document.getElementById('view-history');
    const btnList = document.getElementById('tab-list');
    const btnHistory = document.getElementById('tab-history');

    if (tab === 'list') {
        viewList.classList.remove('hidden');
        viewHistory.classList.add('hidden');
        btnList.classList.add('active');
        btnHistory.classList.remove('active');
    } else {
        viewList.classList.add('hidden');
        viewHistory.classList.remove('hidden');
        btnList.classList.remove('active');
        btnHistory.classList.add('active');
    }
}

// Экспорт
function exportJSON() {
    const dataStr = JSON.stringify(productsData.filter(p => p.approved), null, 2);
    downloadFile(dataStr, 'application/json', 'shopping_list.json');
}

function exportTXT() {
    const text = productsData
        .filter(p => p.approved)
        .map(p => `${p.bought ? '[X]' : '[ ]'} ${p.name} - ${p.price} руб.`)
        .join('\n');
    downloadFile(text, 'text/plain', 'shopping_list.txt');
}

function downloadFile(content, mimeType, filename) {
    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
}

// Запуск
fetchInitialData();
connectWebSocket();