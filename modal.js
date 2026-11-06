export function closeModal(element) {
  if (!element) return;
  if (typeof element.close === 'function') element.close();
  else element.hidden = true;
}
