'use strict';

(() => {
  function refreshApps() {
    const root = document.getElementById('launcher-icons');
    if (!root) return;

    root.querySelectorAll('.native-game-app').forEach(el => el.remove());

    const apps = window.SylesStacksInstalledApps || [];

    const games = apps
      .filter(app => app && app.isGame && app.packageName)
      .sort((a, b) => a.name.localeCompare(b.name, 'ru'));

    games.forEach(app => {
      const button = document.createElement('button');
      button.className = 'launcher-app native-game-app';
      button.type = 'button';

      const art = document.createElement('span');
      art.className = 'launcher-art native-game-art';

      const img = document.createElement('img');
      img.src = app.icon || '';
      img.alt = '';

      art.appendChild(img);

      const label = document.createElement('span');
      label.className = 'native-game-label';
      label.textContent = app.name;

      button.append(art, label);

      button.addEventListener('click', () => {
        if (window.SylesStacksNative) {
          window.SylesStacksNative.launchPackage(app.packageName);
        }
      });

      root.appendChild(button);
    });

    const hint = document.querySelector('.launcher-hint');
    if (hint) {
      hint.textContent = games.length
        ? 'Нажми на значок, чтобы запустить игру'
        : 'Установленные игры появятся здесь автоматически';
    }
  }

  window.SylesStacksRefreshApps = refreshApps;

  document.addEventListener('DOMContentLoaded', refreshApps);

  if (document.readyState !== 'loading') {
    refreshApps();
  }
})();
