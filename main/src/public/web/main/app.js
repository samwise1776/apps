(() => {
  const currentYear = new Date().getFullYear();
  document.querySelectorAll(".copyright").forEach((element) => {
    element.textContent = element.textContent.replace(/©\s+\d{4}/, `© ${currentYear}`);
  });
})();
