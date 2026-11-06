// Делегирование событий без привязки к конкретной странице.
export function on(root, eventName, selector, handler) {
  root.addEventListener(eventName, event => {
    const target = event.target.closest(selector);
    if (target && root.contains(target)) handler(event, target);
  });
}
