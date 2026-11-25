
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
      "'": "&#039;"
    })[char]);
  }

  function formatSize(bytes) {
    const size = Number(bytes || 0);

    if (!size) return "Размер неизвестен";
    if (size < 1024 * 1024) {
      return Math.round(size / 1024) + " КБ";
    }

    return (size / (1024 * 1024)).toFixed(1) + " МБ";
  }

  function getDownloadUrl(app) {
    const url = String(app.downloadUrl || app.url || "").trim();

    if (/^https:\/\/[^\s]+$/i.test(url)) {
      return url;
    }

    if (app.packageId) {
      return "https://play.google.com/store/apps/details?id=" +
        encodeURIComponent(app.packageId);
    }

    return "";
  }

  function showMessage(message) {
    if (typeof window.toast === "function") {
      window.toast(message);
      return;
    }

    console.log("[SylesStacks Store]", message);
  }

  function getCategory(app) {
    const text = (
      String(app.category || "") + " " +
      String(app.type || "") + " " +
      String(app.name || "") + " " +
      String(app.description || "")
    ).toLowerCase();

    if (
      text.includes("game") ||
      text.includes("игр") ||
      text.includes("standoff") ||
      text.includes("brawl") ||
      text.includes("minecraft")
    ) {
      return "games";
    }

    return "apps";
  }

  function installStoreStyles() {
    if ($("sylesstacks-store-download-styles")) return;

    const style = document.createElement("style");
    style.id = "sylesstacks-store-download-styles";

    style.textContent = [
      "#storeGrid .store-app-row {",
      "display:flex;align-items:center;gap:14px;",
      "padding:14px;margin-bottom:12px;",
      "border:1px solid rgba(255,255,255,.08);",
      "border-radius:16px;",
      "background:rgba(255,255,255,.035);",
      "}",
      "#storeGrid .store-app-icon {",
      "width:58px;height:58px;flex:0 0 58px;",
      "display:grid;place-items:center;",
      "border-radius:14px;overflow:hidden;",
      "background:#20263a;font-size:25px;",
      "}",
      "#storeGrid .store-app-icon img {",
      "width:100%;height:100%;object-fit:cover;",
      "}",
      "#storeGrid .store-app-info {",
      "flex:1;min-width:0;",
      "}",
      "#storeGrid .store-app-info h3 {",
      "margin:0 0 5px;font-size:15px;",
      "}",
      "#storeGrid .store-app-info p {",
      "margin:0;font-size:12px;opacity:.7;",
      "}",
      "#storeGrid .store-download {",
      "border:0;border-radius:11px;padding:10px 15px;",
      "background:#9b79ff;color:white;",
      "font-weight:700;cursor:pointer;",
      "text-decoration:none;white-space:nowrap;",
      "}",
      "#storeGrid .store-download:active {",
      "transform:scale(.96);",
      "}",
      ".store-detail-content { padding:18px 0; }",
      ".store-detail-icon {",
      "width:86px;height:86px;object-fit:cover;",
      "border-radius:20px;background:#20263a;",
      "}",
      ".store-detail-download {",
      "display:inline-block;margin-top:15px;",
      "padding:13px 22px;border-radius:12px;",
      "background:#9b79ff;color:white;",
      "text-decoration:none;font-weight:700;",
      "}",
      ".store-error {",
      "padding:18px;border-radius:14px;",
      "background:rgba(255,90,90,.08);",
      "color:#ffaaaa;",
      "}"
    ].join("\n");

    document.head.appendChild(style);
  }

  function getFilteredApps() {
    let result = [...catalog];

    if (activeCategory === "games") {
      result = result.filter(app => getCategory(app) === "games");
    }

    if (activeCategory === "apps") {
      result = result.filter(app => getCategory(app) === "apps");
    }

    if (activeCategory === "popular") {
      result = result.filter(app =>
        Number(app.downloads || 0) > 0 ||
        app.popular === true ||
        app.featured === true
      );
    }

    if (searchQuery) {
      const query = searchQuery.toLowerCase();

      result = result.filter(app =>
        String(app.name || "").toLowerCase().includes(query) ||
        String(app.description || "").toLowerCase().includes(query) ||
        String(app.category || "").toLowerCase().includes(query)
      );
    }

    return result;
  }

  function createDownloadButton(app) {
    const url = getDownloadUrl(app);

    if (!url) {
      return '<span class="store-download">Нет ссылки</span>';
    }

    return '<a class="store-download" href="' +
      escapeHTML(url) +
      '" data-native-download="true">Скачать</a>';
  }

  function renderCatalog() {
    const grid = $("storeGrid");
    const empty = $("storeEmpty");

    if (!grid) return;

    const apps = getFilteredApps();

    if (!apps.length) {
      grid.innerHTML = "";
      if (empty) empty.hidden = false;
      return;
    }

    if (empty) empty.hidden = true;

    grid.innerHTML = apps.map((app, index) => {
      const name = escapeHTML(app.name || "Без названия");
      const description = escapeHTML(
        app.description || "Описание отсутствует"
      );
      const version = escapeHTML(app.version || "Неизвестно");
      const size = formatSize(app.size);
      const image = app.icon || app.image || "";

      return `
        <article class="store-app-row" data-app-index="${index}">
          <div class="store-app-icon">
            ${
              image
                ? '<img src="' + escapeHTML(image) + '" alt="">'
                : "📦"
            }
          </div>
          <div class="store-app-info">
            <h3>${name}</h3>
            <p>${description}</p>
            <p>Версия ${version} · ${size}</p>
          </div>
          ${createDownloadButton(app)}
          <button class="secondary tiny store-details-button"
            type="button" data-details-index="${index}">
            Подробнее
          </button>
        </article>
      `;
    }).join("");

    grid.querySelectorAll("[data-details-index]").forEach(button => {
      button.addEventListener("click", () => {
        const index = Number(button.dataset.detailsIndex);
        const app = apps[index];
        if (app) openDetails(app);
      });
    });
  }

  function openDetails(app) {
    selectedApp = app;

    const detail = $("storeDetail");
    const content = $("storeDetailContent");

    if (!detail || !content) return;

    const image = app.icon || app.image || "";
    const url = getDownloadUrl(app);

    content.innerHTML = `
      <div class="store-detail-content">
        ${
          image
            ? '<img class="store-detail-icon" src="' +
              escapeHTML(image) + '" alt="">'
            : '<div class="store-detail-icon">📦</div>'
        }
        <h2>${escapeHTML(app.name || "Без названия")}</h2>
        <p>${escapeHTML(app.description || "Описание отсутствует")}</p>
        <p>Версия: ${escapeHTML(app.version || "Неизвестно")}</p>
        <p>Размер: ${formatSize(app.size)}</p>
        <p>Загрузок: ${Number(app.downloads || 0)}</p>
        ${
          url
            ? '<a class="store-detail-download" href="' +
              escapeHTML(url) +
              '" data-native-download="true">Скачать приложение</a>'
            : "<p>Ссылка на загрузку отсутствует.</p>"
        }
      </div>
    `;

    detail.hidden = false;

    const catalogPage = $("storeCatalogPage");
    const myAppsPage = $("storeMyAppsPage");

    if (catalogPage) catalogPage.hidden = true;
    if (myAppsPage) myAppsPage.hidden = true;
  }

  function closeDetails() {
    const detail = $("storeDetail");
    if (detail) detail.hidden = true;

    if (activeTab === "apps") {
      const page = $("storeMyAppsPage");
      if (page) page.hidden = false;
    } else {
      const page = $("storeCatalogPage");
      if (page) page.hidden = false;
    }

    selectedApp = null;
  }

  function showTab(tab) {
    activeTab = tab;

    const catalogPage = $("storeCatalogPage");
    const myAppsPage = $("storeMyAppsPage");
    const detail = $("storeDetail");

    if (detail) detail.hidden = true;
    if (catalogPage) catalogPage.hidden = tab !== "store";
    if (myAppsPage) myAppsPage.hidden = tab !== "apps";

    document.querySelectorAll("[data-store-main]").forEach(button => {
      button.classList.toggle(
        "active",
        button.dataset.storeMain === tab
      );
    });

    if (tab === "apps") renderMyApps();
  }

  function getLocalGames() {
    try {
      const data = JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");

      if (Array.isArray(data.games)) return data.games;
      if (Array.isArray(data.apps)) return data.apps;

      return [];
    } catch (error) {
      console.warn("Не удалось прочитать библиотеку:", error);
      return [];
    }
  }

  function renderMyApps() {
    const list = $("storeMyAppsList");
    if (!list) return;

    const games = getLocalGames();

    if (!games.length) {
      list.innerHTML = `
        <div class="store-empty">
          <span>📱</span>
          <h3>Пока нет приложений</h3>
          <p>Добавь игру в библиотеку SylesStacks.</p>
        </div>
      `;
      return;
    }

    list.innerHTML = games.map(game => `
      <div class="store-app-row">
        <div class="store-app-icon">🎮</div>
        <div class="store-app-info">
          <h3>${escapeHTML(game.name || "Без названия")}</h3>
          <p>${escapeHTML(game.packageId || game.package || "")}</p>
        </div>
      </div>
    `).join("");
  }

  async function loadCatalog() {
    const grid = $("storeGrid");
    if (!grid) return;

    grid.innerHTML = `
      <div class="store-empty">
        <span>⌛</span>
        <h3>Загрузка каталога...</h3>
        <p>Подключаемся к серверу.</p>
      </div>
    `;

    try {
      const response = await fetch(API_URL, {
        method: "GET",
        headers: { "Accept": "application/json" },
        cache: "no-store"
      });

      if (!response.ok) {
        throw new Error("Сервер ответил: HTTP " + response.status);
      }

      const data = await response.json();

      if (!data || data.ok !== true || !Array.isArray(data.apps)) {
        throw new Error("Сервер вернул неверный формат каталога");
      }

      catalog = data.apps;
      renderCatalog();

      console.log("SylesStacks Store: загружено приложений:", catalog.length);

    } catch (error) {
      console.error("Ошибка магазина:", error);

      grid.innerHTML = `
        <div class="store-error">
          <h3>Не удалось загрузить магазин</h3>
          <p>${escapeHTML(error.message)}</p>
          <button class="secondary" id="storeRetry" type="button">
            Повторить
          </button>
        </div>
      `;

      const retry = $("storeRetry");
      if (retry) retry.addEventListener("click", loadCatalog);
    }
  }

  function setupEvents() {
    document.addEventListener("click", event => {
      const link = event.target.closest("a[data-native-download]");
      if (!link) return;

      const nativeBridge = window.SylesStacksNative;
      if (!nativeBridge || typeof nativeBridge.downloadFile !== "function") {
        return; // Browser fallback: let the normal download handling run.
      }

      // A Play Store URL is a web page, not an APK file.
      try {
        if (new URL(link.href).hostname === "play.google.com") return;
      } catch (_) {
        return;
      }

      event.preventDefault();
      nativeBridge.downloadFile(link.href, link.textContent.trim() || "SylesStacks download");
    });

    document.querySelectorAll("[data-store-main]").forEach(button => {
      button.addEventListener("click", () => {
        showTab(button.dataset.storeMain);
      });
    });

    document.querySelectorAll("[data-store-category]").forEach(button => {
      button.addEventListener("click", () => {
        activeCategory = button.dataset.storeCategory;

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

    const back = document.querySelector(".store-back");

    if (back) {
      back.addEventListener("click", closeDetails);
    }
  }

  function init() {
    if (!$("storeGrid")) return;

    installStoreStyles();
    setupEvents();
    loadCatalog();

    console.log("SylesStacks Store: initialized");
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
