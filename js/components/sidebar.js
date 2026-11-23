export function toggleSidebar(element) {
  if (!element) return false;
  const open = element.classList.toggle('open');
  element.setAttribute('aria-hidden', String(!open));
  return open;
}
