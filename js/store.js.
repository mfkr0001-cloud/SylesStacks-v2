
'use strict';

const STORE_ITEMS = [
  {
    id: 'brawl-stars', name: 'Brawl Stars', dev: 'Supercell',
    category: 'games', rating: '4.3', label: 'Экшен', icon: '⭐',
    color: 'linear-gradient(145deg,#ffb52e,#f04c35)',
    pkg: 'com.supercell.brawlstars',
    description: 'Динамичные командные сражения и разные игровые режимы.'
  },
  {
    id: 'standoff-2', name: 'Standoff 2', dev: 'AXLEBOLT LTD',
    category: 'games', rating: '4.2', label: 'Шутер', icon: '🎯',
    color: 'linear-gradient(145deg,#5b91c9,#1e3559)',
    pkg: 'com.axlebolt.standoff2',
    description: 'Многопользовательский тактический шутер.'
  },
  {
    id: 'geometry-dash', name: 'Geometry Dash', dev: 'RobTop Games',
    category: 'games', rating: '4.6', label: 'Аркада', icon: '🟩',
    color: 'linear-gradient(145deg,#36d58a,#087c66)',
    pkg: 'com.robtopx.geometryjump',
    description: 'Ритмичный платформер с редактором уровней.'
  },
  {
    id: 'clash-royale', name: 'Clash Royale', dev: 'Supercell',
    category: 'games', rating: '4.2', label: 'Стратегия', icon: '👑',
    color: 'linear-gradient(145deg,#64c8f2,#4160bc)',
    pkg: 'com.supercell.clashroyale',
    description: 'Карточные тактические битвы.'
  },
  {
    id: 'youtube', name: 'YouTube', dev: 'Google LLC',
    category: 'apps', rating: '4.1', label: 'Видео', icon: '▶',
    color: 'linear-gradient(145deg,#ff5555,#c90020)',
    pkg: 'com.google.android.youtube',
    description: 'Видео, каналы и трансляции.'
  },
  {
    id: 'telegram', name: 'Telegram', dev: 'Telegram FZ-LLC',
    category: 'apps', rating: '4.2', label: 'Общение', icon: '➤',
    color: 'linear-gradient(145deg,#49c8f4,#1687c9)',
    pkg: 'org.telegram.messenger',
    description: 'Мессенджер для сообщений и групп.'
  },
  {
    id: 'spotify', name: 'Spotify', dev: 'Spotify AB',
    category: 'apps', rating: '4.4', label: 'Музыка', icon: '♫',
    color: 'linear-gradient(145deg,#40d879,#087e45)',
    pkg: 'com.spotify.music',
    description: 'Музыка и подкасты.'
  },
  {
    id: 'google-photos', name: 'Google Фото', dev: 'Google LLC',
    category: 'apps', rating: '4.4', label: 'Фото', icon: '✿',
    color: 'linear-gradient(145deg,#f4c64e,#4b9ef1)',
    pkg: 'com.google.android.apps.photos',
    description: 'Фотографии и инструменты организации.'
  }
];

let storeCategory = 'all';
let storeQuery = '';
let storeSelected = null;
let storeInitialized = false;

function storeEscape(value) {
  return String(value ?? '').replace(/[&<>"']/g, ch => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[ch]));
}

function setStoreCategory(category) {
  const allowed = ['all', 'games', 'apps', 'popular'];
  if (!allowed.includes(category)) return;

  storeCategory = category;

  document.querySelectorAll('[data-store-category]').forEach(button => {
    const active = button.dataset.storeCategory === category;
    button.classList.toggle('active', active);
    button.setAttribute('aria-pressed', String(active));
  });

  const heading = document.getElementById('storeHeading');

  if (heading) {
    const names = {
      all: 'Для вас',
      games: 'Игры',
      apps: 'Приложения',
      popular: 'Популярное'
    };

    heading.textContent = names[category];
  }

  closeStoreDetail();
  renderStore();
}

function renderStore() {
  const grid = document.getElementById('storeGrid');
  if (!grid) return;

  const query = storeQuery.trim().toLowerCase();

  let items = STORE_ITEMS.filter(item => {
    const categoryMatch =
      storeCategory === 'all' ||
      item.category === storeCategory ||
      (storeCategory === 'popular' && Number(item.rating) >= 4.3);

    const searchMatch =
      !query ||
      (item.name + ' ' + item.dev + ' ' + item.label + ' ' + item.description)
        .toLowerCase()
        .includes(query);

    return categoryMatch && searchMatch;
  });

  if (storeCategory === 'popular') {
    items = items.filter(item => Number(item.rating) >= 4.3);
  }

  grid.replaceChildren();

  items.forEach(item => {
    const card = document.createElement('button');
    card.className = 'store-card';
    card.type = 'button';
    card.dataset.storeItem = item.id;

    card.innerHTML =
      '<span class="store-app-icon" style="background:' +
      item.color + '">' + storeEscape(item.icon) + '</span>' +
      '<span class="store-card-info">' +
      '<b>' + storeEscape(item.name) + '</b>' +
      '<small>' + storeEscape(item.dev) + '</small>' +
      '<span class="store-rating">★ ' + item.rating +
      ' · ' + storeEscape(item.label) + '</span>' +
      '</span><span class="store-card-arrow">›</span>';

    grid.appendChild(card);
  });

  const empty = document.getElementById('storeEmpty');
  if (empty) empty.hidden = items.length !== 0;

  const feature = document.getElementById('storeFeature');
  if (feature) feature.hidden = !!query;
}

function showStoreDetail(id) {
  const item = STORE_ITEMS.find(app => app.id === id);
  if (!item) return;

  storeSelected = item;

  const root = document.getElementById('storeDetail');
  const content = document.getElementById('storeDetailContent');

  if (!root || !content) return;

  content.innerHTML =
    '<div class="detail-hero">' +
    '<span class="store-app-icon large" style="background:' +
    item.color + '">' + storeEscape(item.icon) + '</span>' +
    '<div><h1>' + storeEscape(item.name) + '</h1>' +
    '<p>' + storeEscape(item.dev) + '</p>' +
    '<p>★ ' + item.rating + ' · ' +
    storeEscape(item.label) + '</p></div></div>' +
    '<button class="store-install" type="button" onclick="openOfficialStore()">' +
    'Открыть в Google Play</button>' +
    '<div class="detail-screens">' +
    '<div>' + storeEscape(item.icon) + '</div>' +
    '<div>✦</div><div>▶</div></div>' +
    '<h3>Об этом приложении</h3>' +
    '<p class="detail-description">' +
    storeEscape(item.description) + '</p>' +
    '<div class="detail-meta">' +
    '<span>Категория</span><b>' + storeEscape(item.label) + '</b>' +
    '<span>Разработчик</span><b>' + storeEscape(item.dev) + '</b>' +
    '<span>Android package</span><b>' + storeEscape(item.pkg) + '</b>' +
    '</div>' +
    '<p class="detail-note">Установка выполняется на официальной странице Google Play.</p>';

  root.hidden = false;
  root.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function closeStoreDetail() {
  const root = document.getElementById('storeDetail');
  if (root) root.hidden = true;
  storeSelected = null;
}

function openOfficialStore() {
  if (!storeSelected) return;

  const url =
    'https://play.google.com/store/apps/details?id=' +
    encodeURIComponent(storeSelected.pkg);

  window.open(url, '_blank', 'noopener,noreferrer');
}

function initStore() {
  if (storeInitialized) return;

  const grid = document.getElementById('storeGrid');
  if (!grid) return;

  storeInitialized = true;

  const search = document.getElementById('storeSearch');

  if (search) {
    search.addEventListener('input', () => {
      storeQuery = search.value;
      closeStoreDetail();
      renderStore();
    });
  }

  document.addEventListener('click', event => {
    const tab = event.target.closest('[data-store-category]');

    if (tab) {
      event.preventDefault();
      setStoreCategory(tab.dataset.storeCategory);
      return;
    }

    const card = event.target.closest('[data-store-item]');

    if (card) {
      showStoreDetail(card.dataset.storeItem);
      return;
    }

    const back = event.target.closest('.store-back');

    if (back) {
      event.preventDefault();
      closeStoreDetail();
    }
  }, true);

  document.querySelectorAll('[data-store-category]').forEach(button => {
    button.type = 'button';
  });

  setStoreCategory('all');
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initStore, { once: true });
} else {
  initStore();
}
