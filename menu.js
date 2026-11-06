export function setMenuOpen(element, open) {
  if (!element) return;
  element.classList.toggle('open', Boolean(open));
  element.setAttribute('aria-expanded', String(Boolean(open)));
}
