```javascript
"use strict";

(() => {
  const API_URL = "https://server-5aky.onrender.com/api/store/apps";
  const STORAGE_KEY = "sylesstacks_v23_data";

  let catalog = [];
  let activeCategory = "all";
  let activeTab = "store";
  let searchQuery = "";
  let selectedApp = null;

  const $ = id => document.getElementById(id);

  function escapeHTML(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;"
    }[char]));
  }

  function formatSize(bytes) {
    const size = Number(bytes) || 0;

    if (size >= 1024 * 1024 * 1024) {
      return (size / (1024 * 1024 * 1024)).toFixed(2) + " GB";
    }

    if (size >= 1024 * 1024) {
      return (size / (1024 * 1024)).toFixed(1) + " MB";
    }

    return Math.round(size / 1024) + " KB";
  }

  function getDownloadUrl(app) {
    const rawUrl = app.downloadUrl || app.url || "";

    try {
      const url = new URL(rawUrl);

      if (url.protocol !== "https:") return "";

      return url.href;
    } catch {
      return "";
    }
  }

  function getCategory(app) {
    const text = (
      (app.name || "") + " " +
      (app.description || "") + " " +
      (app.fileName || "")
    ).toLowerCase();

    const gameWords = [
      "game", "игра", "standoff", "brawl",
      "minecraft", "roblox", "pubg", "mobile legends"
    ];

    return gameWords.some(word => text.includes(word))
      ? "games"
      : "apps";
  }

  function showMessage(message, isError = false) {
    const root = $("storeGrid");
    if (!root) return;

    root.innerHTML =
      '<div class="store-empty">' +
      '<h3>' + escapeHTML(isError ? "Не удалось загрузить каталог" : "Загрузка") + '</h3>' +
      '<p>' + escapeHTML(message) + '</p>' +
      '</div>';
  }

  async function loadCatalog() {
    showMessage("Подключаемся к серверу...");

    try {
      const response = await fetch(API_URL, {
        method: "GET",
        headers: {
          "Accept": "application/json"
        },
        cache: "no-store"
      });

      if (!response.ok) {
        throw new Error("Ошибка сервера: " + response.status);
      }

      const result = await response.json();

      if (!result || result.ok !== true || !Array.isArray(result.apps)) {
        throw new Error("Сервер вернул неверный формат каталога.");
      }

      catalog = result.apps.map(app => ({
        ...app,
        category: getCategory(app)
      }));

      renderCatalog();
    } catch (error) {
      console.error("SylesStacks Store:", error);

      showMessage(
        "Проверь подключение к интернету и доступность Render. " +
        "Попробуй обновить каталог.",
        true
      );

      const root = $("storeGrid");

      if (root) {
        const button = document.createElement("button");
        button.className = "store-back";
        button.type = "button";
        button.textContent = "Повторить загрузку";
        button.addEventListener("click", loadCatalog);
        root.appendChild(button);
      }
    }
  }

  function getFilteredApps() {
    let result = [...catalog];

    if (activeCategory === "games") {
      result = result.filter(app => app.category === "games");
    } else if (activeCategory === "apps") {
      result = result.filter(app => app.category === "apps");
    } else if (activeCategory === "popular") {
      result.sort((a, b) =>
        (Number(b.downloads) || 0) - (Number(a.downloads) || 0)
      );
    }

    if (searchQuery) {
      const query = searchQuery.toLowerCase();

      result = result.filter(app => {
        const text = [
          app.name,
          app.description,
          app.version,
          app.fileName
        ].join(" ").toLowerCase();

        return text.includes(query);
      });
    }

    return result;
  }

  function renderCatalog() {
    const root = $("storeGrid");
    const empty = $("storeEmpty");

    if (!root) return;

    root.innerHTML = "";

    const apps = getFilteredApps();

    if (empty) {
      empty.hidden = apps.length > 0;
    }

    if (!apps.length) {
      root.innerHTML =
        '<div class="store-empty">' +
        '<h3>Приложений пока нет</h3>' +
        '<p>Попробуй изменить поиск или категорию.</p>' +
        '</div>';
      return;
    }

    apps.forEach(app => {
      const card = document.createElement("button");
      card.type = "button";
      card.className = "store-card";

      const icon = document.createElement("span");
      icon.className = "store-app-icon";
      icon.textContent = app.category === "games" ? "🎮" : "📦";

      const info = document.createElement("span");
      info.className = "store-card-info";

      const name = document.createElement("b");
      name.textContent = app.name || "Без названия";

      const version = document.createElement("small");
      version.textContent = "Версия " + (app.version || "latest");

      const rating = document.createElement("span");
      rating.className = "store-rating";
      rating.textContent =
        formatSize(app.size) + " · " +
        (Number(app.downloads) || 0) + " загрузок";

      info.append(name, version, rating);

      const arrow = document.createElement("span");
      arrow.className = "store-card-arrow";
      arrow.textContent = "›";

      card.append(icon, info, arrow);
      card.addEventListener("click", () => openDetails(app));

      root.appendChild(card);
    });
  }

  function openDetails(app) {
    selectedApp = app;

    const detail = $("storeDetail");
    const content = $("storeDetailContent");

    if (!detail || !content) return;

    const icon = app.category === "games" ? "🎮" : "📦";
    const downloadUrl = getDownloadUrl(app);

    const downloadButton = downloadUrl
      ? `
        <a
          class="primary store-download-button"
          href="${escapeHTML(downloadUrl)}"
          target="_blank"
          rel="noopener noreferrer"
          style="
            display:flex;
            align-items:center;
            justify-content:center;
            width:100%;
            box-sizing:border-box;
            padding:15px 20px;
            margin:20px 0;
            border-radius:12px;
            background:#7657ed;
            color:#ffffff;
            font-size:16px;
            font-weight:700;
            text-decoration:none;
            cursor:pointer;
          "
        >
          ⬇ Скачать APK
        </a>
      `
      : `
        <button
          type="button"
          disabled
          style="
            display:block;
            width:100%;
            padding:15px;
            margin:20px 0;
            border:0;
            border-radius:12px;
            background:#555;
            color:#ddd;
            font-size:16px;
            font-weight:700;
            opacity:0.7;
          "
        >
          Ссылка на скачивание отсутствует
        </button>
      `;

    content.innerHTML = `
      <div class="detail-hero">
        <div class="store-app-icon large">${icon}</div>
        <div>
          <h1>${escapeHTML(app.name || "Без названия")}</h1>
          <p>Версия ${escapeHTML(app.version || "latest")}</p>
          <p>${escapeHTML(formatSize(app.size))}</p>
        </div>
      </div>

      <p class="detail-description">
        ${escapeHTML(app.description || "Описание отсутствует.")}
      </p>

      <div class="detail-meta">
        <span>Имя файла</span>
        <b>${escapeHTML(app.fileName || "Не указано")}</b>

        <span>Загрузки</span>
        <b>${Number(app.downloads) || 0}</b>

        <span>Репозиторий</span>
        <b>${escapeHTML(app.repository || "Не указан")}</b>
      </div>

      ${downloadButton}

      <p class="detail-note">
        Скачивай APK только из источников, которым доверяешь.
      </p>

      <button
        type="button"
        class="store-back"
        id="storeDetailBack"
      >
        ← Назад к каталогу
      </button>
    `;

    detail.hidden = false;

    const back = $("storeDetailBack");
    if (back) {
      back.addEventListener("click", closeDetails);
    }
  }

  function closeDetails() {
    const detail = $("storeDetail");

    if (detail) {
      detail.hidden = true;
    }

    selectedApp = null;
  }

  function showTab(tab) {
    activeTab = tab;

    const catalogPage = $("storeCatalogPage");
    const myAppsPage = $("storeMyAppsPage");
    const detail = $("storeDetail");

    if (catalogPage) catalogPage.hidden = tab !== "store";
    if (myAppsPage) myAppsPage.hidden = tab !== "apps";

    if (detail) detail.hidden = true;

    document.querySelectorAll("[data-store-main]").forEach(button => {
      button.classList.toggle(
        "active",
        button.dataset.storeMain === tab
      );
    });

    if (tab === "apps") {
      renderMyApps();
    }
  }

  function getLocalGames() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return [];

      const data = JSON.parse(raw);

      return Array.isArray(data.games) ? data.games : [];
    } catch (error) {
      console.warn("Не удалось прочитать библиотеку игр:", error);
      return [];
    }
  }

  function renderMyApps() {
    const root = $("storeMyAppsList");
    if (!root) return;

    root.innerHTML = "";

    const games = getLocalGames();

    if (!games.length) {
      root.innerHTML =
        '<div class="store-empty">' +
        '<h3>Библиотека пуста</h3>' +
        '<p>Добавленные тобой игры появятся здесь.</p>' +
        '</div>';
      return;
    }

    games.forEach(game => {
      const card = document.createElement("div");
      card.className = "store-library-card";

      const icon = document.createElement("span");
      icon.className = "store-app-icon";
      icon.textContent = "🎮";

      const meta = document.createElement("div");
      meta.className = "store-library-meta";

      const name = document.createElement("b");
      name.textContent = game.name || "Без названия";

      const packageId = document.createElement("small");
      packageId.textContent = game.packageId || "Package ID не указан";

      meta.append(name, packageId);
      card.append(icon, meta);

      root.appendChild(card);
    });
  }

  function setupEvents() {
    document.querySelectorAll("[data-store-main]").forEach(button => {
      button.addEventListener("click", () => {
        showTab(button.dataset.storeMain);
      });
    });

    document.querySelectorAll("[data-store-category]").forEach(button => {
      button.addEventListener("click", () => {
        activeCategory = button.dataset.storeCategory || "all";

        document.querySelectorAll("[data-store-category]").forEach(item => {
          item.classList.toggle("active", item === button);
        });

        renderCatalog();
      });
    });

    const search = $("storeSearch");

    if (search) {
      search.addEventListener("input", () => {
        searchQuery = search.value.trim();
        renderCatalog();
      });
    }

    window.addEventListener("storage", event => {
      if (event.key === STORAGE_KEY && activeTab === "apps") {
        renderMyApps();
      }
    });
  }

  function init() {
    if (!$("storeGrid")) return;

    setupEvents();
    loadCatalog();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
```
