// Навигационный помощник для будущего модульного интерфейса.
export function showPage(id, root = document) {
  const pages = [...root.querySelectorAll('.page')];
  if (!pages.some(page => page.id === id)) return false;
  pages.forEach(page => page.classList.toggle('active', page.id === id));
  root.querySelectorAll('[data-go]').forEach(button => button.classList.toggle('active', button.dataset.go === id));
  return true;
}
