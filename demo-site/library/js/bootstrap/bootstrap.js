document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('[data-app]').forEach((el) => {
    const name = el.getAttribute('data-app');
    el.textContent = 'App mounted: ' + name;
    el.dataset.ready = 'true';
  });
});
