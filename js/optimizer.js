'use strict';

(() => {
  const STORAGE_KEY = 'sylesstacks_v23_data';

  const $ = id => document.getElementById(id);

  function notify(message) {
    if (typeof window.toast === 'function') {
      window.toast(message);
    } else {
      alert(message);
    }
  }

  function getBridge() {
    return window.SylesStacksNative || null;
  }

  function getSelectedGame() {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}');
      const games = Array.isArray(saved.games) ? saved.games : [];
      const select = $('profileGame');
      const selectedId = select?.value;

      return games.find(game => game.id === selectedId) ||
        games[0] ||
        null;
    } catch (_) {
      return null;
    }
  }

  function getSelectedPackage() {
    const game = getSelectedGame();
    return game?.packageId || '';
  }

  function openSelectedGame() {
    const packageName = getSelectedPackage();
    const bridge = getBridge();

    if (!packageName) {
      notify('Сначала добавь игру с Package ID в библиотеку.');
      return;
    }

    if (!bridge || typeof bridge.launchPackage !== 'function') {
      notify('Запуск игр доступен в Android APK SylesStacks.');
      return;
    }

    bridge.launchPackage(packageName);
  }

  function openSelectedGameSettings() {
    const packageName = getSelectedPackage();
    const bridge = getBridge();

    if (!packageName) {
      notify('Сначала выбери игру с Package ID.');
      return;
    }

    if (!bridge || typeof bridge.openPackageSettings !== 'function') {
      notify('Системные настройки доступны в Android APK.');
      return;
    }

    bridge.openPackageSettings(packageName);
  }

  function openBatterySettings() {
    const bridge = getBridge();

    if (!bridge || typeof bridge.openBatterySettings !== 'function') {
      notify('Настройки батареи доступны в Android APK.');
      return;
    }

    bridge.openBatterySettings();
  }

  function openDisplaySettings() {
    const bridge = getBridge();

    if (!bridge || typeof bridge.openDisplaySettings !== 'function') {
      notify('Настройки экрана доступны в Android APK.');
      return;
    }

    bridge.openDisplaySettings();
  }

  function addButton(parent, text, className, action) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = className;
    button.textContent = text;
    button.addEventListener('click', action);
    parent.appendChild(button);
    return button;
  }

  function createOptimizerPanel() {
    const performancePage = $('performance');
    if (!performancePage || $('optimizerNativePanel')) return;

    const firstCard = performancePage.querySelector(':scope > .card');

    const panel = document.createElement('div');
    panel.id = 'optimizerNativePanel';
    panel.className = 'section card';

    const heading = document.createElement('div');
    heading.className = 'head';

    const title = document.createElement('h2');
    title.textContent = 'Android Game Optimizer';

    const status = document.createElement('span');
    status.className = 'tag';
    status.id = 'optimizerBridgeStatus';
    status.textContent = getBridge() ? 'Android подключён' : 'Только веб';

    heading.append(title, status);

    const description = document.createElement('p');
    description.className = 'muted';
    description.textContent =
      'Запуск игр, системные настройки и диагностика устройства. ' +
      'Изменения системных параметров подтверждаются самим Android.';

    const gameActions = document.createElement('div');
    gameActions.className = 'row';
    gameActions.style.marginTop = '12px';

    addButton(
      gameActions,
      '▶ Запустить выбранную игру',
      'primary',
      openSelectedGame
    );

    addButton(
      gameActions,
      'Настройки игры',
      'secondary',
      openSelectedGameSettings
    );

    const systemActions = document.createElement('div');
    systemActions.className = 'row';
    systemActions.style.marginTop = '10px';

    addButton(
      systemActions,
      'Энергосбережение',
      'secondary',
      openBatterySettings
    );

    addButton(
      systemActions,
      'Настройки экрана',
      'secondary',
      openDisplaySettings
    );

    const statsHeading = document.createElement('div');
    statsHeading.className = 'head';
    statsHeading.style.marginTop = '18px';

    const statsTitle = document.createElement('h2');
    statsTitle.textContent = 'Состояние устройства';

    const refresh = addButton(
      statsHeading,
      'Обновить',
      'secondary tiny',
      refreshStats
    );

    const stats = document.createElement('div');
    stats.id = 'optimizerStats';

    panel.append(
      heading,
      description,
      gameActions,
      systemActions,
      statsHeading,
      stats
    );

    if (firstCard) {
      firstCard.insertAdjacentElement('afterend', panel);
    } else {
      performancePage.appendChild(panel);
    }

    refreshStats();
  }

  function addStat(root, label, value) {
    const row = document.createElement('div');
    row.className = 'setting';

    const info = document.createElement('div');

    const name = document.createElement('b');
    name.textContent = label;

    const detail = document.createElement('small');
    detail.textContent = value;

    info.append(name, detail);
    row.appendChild(info);
    root.appendChild(row);
  }

  function refreshStats() {
    const root = $('optimizerStats');
    const status = $('optimizerBridgeStatus');

    if (!root) return;

    root.replaceChildren();

    const bridge = getBridge();

    if (status) {
      status.textContent = bridge ? 'Android подключён' : 'Только веб';
    }

    if (!bridge || typeof bridge.getDeviceStats !== 'function') {
      addStat(root, 'Android-диагностика', 'Открой SylesStacks внутри APK.');
      addStat(root, 'Игровые настройки', 'Доступны сохранённые профили сайта.');
      return;
    }

    try {
      const raw = bridge.getDeviceStats();
      const stats = JSON.parse(raw);

      if (stats.error) {
        addStat(root, 'Ошибка диагностики', stats.error);
        return;
      }

      const total = Number(stats.totalRamMb);
      const available = Number(stats.availableRamMb);
      const used = total - available;

      addStat(
        root,
        'Оперативная память',
        Number.isFinite(total) && total > 0
          ? `${Math.round(used)} MB занято из ${Math.round(total)} MB`
          : 'Недоступно'
      );

      addStat(
        root,
        'Свободная память',
        Number.isFinite(available) && available >= 0
          ? `${Math.round(available)} MB`
          : 'Недоступно'
      );

      addStat(
        root,
        'Заряд батареи',
        Number(stats.batteryPercent) >= 0
          ? `${stats.batteryPercent}%`
          : 'Недоступно'
      );

      addStat(
        root,
        'Температурный статус',
        stats.thermalStatus || 'Недоступно'
      );

      addStat(
        root,
        'Устройство',
        [stats.deviceModel, stats.androidVersion ? `Android ${stats.androidVersion}` : '']
          .filter(Boolean)
          .join(' · ') || 'Неизвестно'
      );

      if (stats.lowMemory) {
        addStat(
          root,
          'Предупреждение',
          'Android сообщает о низком объёме свободной памяти.'
        );
      }
    } catch (_) {
      addStat(root, 'Диагностика', 'Не удалось получить данные Android.');
    }
  }

  function addGameProfileActions() {
    const page = $('gameprofiles');
    if (!page || $('optimizerGameActions')) return;

    const saveButton = page.querySelector('button[onclick="saveGameProfile()"]');
    if (!saveButton) return;

    const actions = document.createElement('div');
    actions.id = 'optimizerGameActions';
    actions.className = 'row';
    actions.style.marginTop = '10px';

    addButton(
      actions,
      '▶ Запустить эту игру',
      'primary',
      openSelectedGame
    );

    addButton(
      actions,
      'Открыть настройки Android',
      'secondary',
      openSelectedGameSettings
    );

    saveButton.insertAdjacentElement('afterend', actions);
  }

  function init() {
    createOptimizerPanel();
    addGameProfileActions();

    window.SylesStacksOptimizerRefresh = refreshStats;
  }

  init();
})();
