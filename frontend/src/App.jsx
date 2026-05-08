import React, { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { v4 as uuidv4 } from 'uuid';

function App() {
  const [products, setProducts] = useState([]);
  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  
  const [user] = useState(() => {
    let savedUser = localStorage.getItem('shopping_user');
    if (!savedUser) {
      savedUser = JSON.stringify({
        id: uuidv4(),
        avatar: `https://api.dicebear.com/7.x/bottts/svg?seed=${Math.random()}`
      });
      localStorage.setItem('shopping_user', savedUser);
    }
    return JSON.parse(savedUser);
  });

  const clientRef = useRef(null);

  useEffect(() => {
    fetch('http://localhost:8080/api/products')
      .then(res => res.json())
      .then(data => setProducts(data))
      .catch(err => console.error("Failed to load products:", err));

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      onConnect: () => {
        client.subscribe('/topic/products', (message) => {
          setProducts(JSON.parse(message.body));
        });
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      },
    });
    client.activate();
    clientRef.current = client;

    return () => client.deactivate();
  }, []);

  const addProduct = (e) => {
    e.preventDefault();
    if (!name || !price) return;
    clientRef.current.publish({
      destination: '/app/addProduct',
      body: JSON.stringify({ name, price: parseFloat(price), addedByAvatar: user.avatar })
    });
    setName(''); setPrice('');
  };

  const vote = (productId) => {
    clientRef.current.publish({
      destination: '/app/voteProduct',
      body: JSON.stringify({ productId, userId: user.id })
    });
  };

  const toggleBought = (productId) => {
    clientRef.current.publish({ destination: '/app/toggleBought', body: productId });
  };

  const deleteProduct = (productId) => {
    clientRef.current.publish({ destination: '/app/deleteProduct', body: productId });
  };

  const approvedList = products.filter(p => p.approved);
  const suggestionsList = products.filter(p => !p.approved);

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto', fontFamily: 'sans-serif', padding: '20px' }}>
      <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
        <h2>Семейные Покупки</h2>
        <img src={user.avatar} alt="Мой аватар" width="50" style={{ borderRadius: '50%' }} />
      </header>

      <form onSubmit={addProduct} style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
        <input placeholder="Что купить?" value={name} onChange={e => setName(e.target.value)} required style={{ flex: 1, padding: '8px' }}/>
        <input type="number" placeholder="Цена (руб)" value={price} onChange={e => setPrice(e.target.value)} required style={{ width: '100px', padding: '8px' }}/>
        <button type="submit" style={{ padding: '8px 16px', cursor: 'pointer' }}>Добавить</button>
      </form>

      {suggestionsList.length > 0 && (
        <div style={{ background: '#fff3cd', padding: '15px', borderRadius: '8px', marginBottom: '20px' }}>
          <h3 style={{ marginTop: 0 }}>🤔 Предложения (нужно 3 голоса)</h3>
          {suggestionsList.map(p => (
            <div key={p.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
              <span>
                <img src={p.addedByAvatar} alt="аватар" width="24" style={{ verticalAlign: 'middle', marginRight: '5px' }}/>
                {p.name} - <b>{p.price} ₽</b>
              </span>
              <div>
                <span style={{ marginRight: '10px' }}>Голосов: {p.voters.length}/3</span>
                {!p.voters.includes(user.id) && (
                  <button onClick={() => vote(p.id)} style={{ cursor: 'pointer' }}>👍 За</button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <h3>🛒 Общий список</h3>
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {approvedList.map(p => (
          <li key={p.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '10px', borderBottom: '1px solid #eee', opacity: p.bought ? 0.5 : 1 }}>
            <span style={{ textDecoration: p.bought ? 'line-through' : 'none' }}>
              {p.name} — {p.price} ₽
            </span>
            <div>
              <button onClick={() => toggleBought(p.id)} style={{ marginRight: '5px', cursor: 'pointer' }}>
                {p.bought ? 'Вернуть' : 'Купили'}
              </button>
              <button onClick={() => deleteProduct(p.id)} style={{ cursor: 'pointer' }}>Удалить</button>
            </div>
          </li>
        ))}
      </ul>
      {approvedList.length === 0 && <p style={{ color: '#888' }}>Список пуст!</p>}
    </div>
  );
}

export default App;