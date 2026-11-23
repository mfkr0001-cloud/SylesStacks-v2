export function showToast(message, element = document.querySelector('#toast')) {
  if (!element) return;
  element.textContent = String(message);
  element.hidden = false;
}
