'use strict';

(() => {
  const STORAGE_KEY = 'sylesstacks.launcher.folders.v1';

  let root;
  let staticItems = [];
  let nativeItems = [];
  let folders = [];
  let dragState = null;
  let suppressClick = false;

  function readFolders() {
    try {
      const data = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
      return Array.isArray(data) ? data : [];
    } catch {
      return [];
    }
  }

  function saveFolders() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(folders));
  }

  function hint(message) {
    const el = document.querySelector('.launcher-hint');
    if (el) el.textContent = message;
  }

  function allItems() {
    return [...staticItems, ...nativeItems];
  }

  function findItem(id) {
    return allItems().find(item => item.id === id);
  }

  function collectStaticItems() {
    const elements = root.querySelectorAll(
      ':scope > .launcher-app:not(.native-game-app):not(.launcher-folder)'
    );

    staticItems = Array.from(elements).map((element, index) => {
      const label = element.querySelector(':scope > span:last-child');
      const name = label?.textContent.trim() || 'Приложение';

      const id = element.dataset.launcherId ||
        'system:item-' + index;

      element.dataset.launcherId = id;

      return { id, name, element };
    });
  }

  function createNativeElement(app) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'launcher-app native-game-app';
    button.dataset.launcherId = 'pkg:' + app.packageName;

    const art = document.createElement('span');
    art.className = 'launcher-art native-game-art';

    if (app.icon) {
      const img = document.createElement('img');
      img.src = app.icon;
      img.alt = '';
      art.appendChild(img);
    } else {
      art.textContent = 'APP';
    }

    const label = document.createElement('span');
    label.className = 'native-game-label';
    label.textContent = app.name || 'Приложение';

    button.append(art, label);

    button.addEventListener('click', () => {
      if (suppressClick) return;

      const bridge = window.SylesStacksNative;

      if (bridge?.launchPackage) {
        bridge.launchPackage(app.packageName);
      }
    });

    return button;
  }

  function createFolderTile(folder) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'launcher-app launcher-folder';
    button.dataset.folderId = folder.id;

    const art = document.createElement('span');
    art.className = 'launcher-art folder-art';

    const preview = document.createElement('span');
    preview.className = 'folder-preview';

    folder.items.slice(0, 4).forEach(id => {
      const item = findItem(id);
      const cell = document.createElement('span');
      cell.className = 'folder-preview-cell';

      const source = item?.element.querySelector('.launcher-art');
      const img = source?.querySelector('img');

      if (img) {
        const copy = document.createElement('img');
        copy.src = img.src;
        cell.appendChild(copy);
      } else {
        cell.textContent = item ? '•' : '';
      }

      preview.appendChild(cell);
    });

    art.appendChild(preview);

    const label = document.createElement('span');
    label.className = 'launcher-app-label';
    label.textContent = folder.name;

    button.append(art, label);

    button.addEventListener('click', () => {
      if (!suppressClick) openFolder(folder.id);
    });

    return button;
  }

  function render() {
    if (!root) return;

    root.replaceChildren();

    const placed = new Set();

    folders.forEach(folder => {
      folder.items.forEach(id => placed.add(id));
    });

    folders.forEach(folder => {
      root.appendChild(createFolderTile(folder));
    });

    allItems().forEach(item => {
      if (!placed.has(item.id)) {
        root.appendChild(item.element);
      }
    });

    bindDrag();
    saveFolders();
  }

  function openFolder(id) {
    const folder = folders.find(f => f.id === id);
    if (!folder) return;

    closeFolder();

    const overlay = document.createElement('div');
    overlay.className = 'launcher-folder-overlay';

    const dialog = document.createElement('div');
    dialog.className = 'launcher-folder-dialog';

    const header = document.createElement('div');
    header.className = 'launcher-folder-header';

    const title = document.createElement('strong');
    title.textContent = folder.name;

    const rename = document.createElement('button');
    rename.textContent = '✎';
    rename.type = 'button';

    const close = document.createElement('button');
    close.textContent = '✕';
    close.type = 'button';

    const actions = document.createElement('div');
    actions.className = 'launcher-folder-actions';
    actions.append(rename, close);

    header.append(title, actions);

    const list = document.createElement('div');
    list.className = 'launcher-folder-items';

    folder.items.forEach(id => {
      const item = findItem(id);
      if (!item) return;

      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'launcher-app folder-item';

      const art = item.element.querySelector('.launcher-art');
      if (art) button.appendChild(art.cloneNode(true));

      const label = document.createElement('span');
      label.className = 'launcher-app-label';
      label.textContent = item.name;

      button.appendChild(label);

      button.addEventListener('click', () => {
        closeFolder();
        item.element.click();
      });

      list.appendChild(button);
    });

    dialog.append(header, list);
    overlay.appendChild(dialog);
    document.body.appendChild(overlay);

    close.addEventListener('click', closeFolder);

    overlay.addEventListener('click', event => {
      if (event.target === overlay) closeFolder();
    });

    rename.addEventListener('click', () => {
      const name = prompt('Название папки:', folder.name);

      if (name?.trim()) {
        folder.name = name.trim().slice(0, 28);
        saveFolders();
        closeFolder();
        render();
      }
    });
  }

  function closeFolder() {
    document.querySelector('.launcher-folder-overlay')?.remove();
  }

  function createFolder(sourceId, targetId) {
    if (!sourceId || !targetId || sourceId === targetId) return;

    const targetFolder = folders.find(f => f.id === targetId);

    if (targetFolder) {
      if (!targetFolder.items.includes(sourceId)) {
        targetFolder.items.push(sourceId);
      }
    } else {
      if (!findItem(sourceId) || !findItem(targetId)) return;

      folders.push({
        id: 'folder:' + Date.now(),
        name: 'Новая папка',
        items: [targetId, sourceId]
      });
    }

    saveFolders();
    render();
    hint('Папка создана');
  }

  function bindDrag() {
    root.querySelectorAll(':scope > .launcher-app').forEach(element => {
      element.addEventListener('pointerdown', event => {
        dragState = {
          id: element.dataset.launcherId || element.dataset.folderId,
          x: event.clientX,
          y: event.clientY,
          moved: false,
          element
        };
      });

      element.addEventListener('pointermove', event => {
        if (!dragState || dragState.element !== element) return;

        if (Math.hypot(
          event.clientX - dragState.x,
          event.clientY - dragState.y
        ) > 12) {
          dragState.moved = true;
          element.classList.add('launcher-dragging');
        }
      });

      element.addEventListener('pointerup', event => {
        if (!dragState || dragState.element !== element) return;

        const state = dragState;
        dragState = null;
        element.classList.remove('launcher-dragging');

        if (!state.moved) return;

        suppressClick = true;

        const target = document
          .elementFromPoint(event.clientX, event.clientY)
          ?.closest('.launcher-app');

        const targetId =
          target?.dataset.launcherId || target?.dataset.folderId;

        if (target && target !== element) {
          createFolder(state.id, targetId);
        }

        setTimeout(() => {
          suppressClick = false;
        }, 300);
      });

      element.addEventListener('pointercancel', () => {
        dragState = null;
      });
    });
  }

  function refreshApps() {
    root = document.querySelector('.launcher-icons, #launcher-icons');
    if (!root) return;

    if (!staticItems.length) collectStaticItems();

    const apps = Array.isArray(window.SylesStacksInstalledApps)
      ? window.SylesStacksInstalledApps
      : [];

    const unique = new Map();

    apps.forEach(app => {
      if (app?.packageName) {
        unique.set(app.packageName, app);
      }
    });

    nativeItems = Array.from(unique.values()).map(app => ({
      id: 'pkg:' + app.packageName,
      name: app.name || 'Приложение',
      element: createNativeElement(app)
    }));

    folders = readFolders();

    render();

    hint('Перетащи одну иконку на другую, чтобы создать папку');
  }

  window.SylesStacksRefreshApps = refreshApps;
  window.SylesStacksCloseFolder = closeFolder;

  document.addEventListener('DOMContentLoaded', refreshApps);

  if (document.readyState !== 'loading') {
    refreshApps();
  }
})();
