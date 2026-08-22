(() => {
  "use strict";

  const KEY = "datacenter-data-chips-v1";
  const START_CHIPS = 200;
  const START_RESETS = 3;
  const COST = 1;
  const DECAY = 3;
  const TWO_DAYS = 2 * 24 * 60 * 60 * 1000;
  const FIVE_WEEKS = 5 * 7 * 24 * 60 * 60 * 1000;

  function fresh() {
    const now = Date.now();
    return {
      version: 1,
      chips: START_CHIPS,
      resetsLeft: START_RESETS,
      lastTick: now,
      pauseUntil: null,
      frozen: false,
      purchases: []
    };
  }

  function load() {
    try {
      const raw = localStorage.getItem(KEY);
      if (!raw) return fresh();
      return Object.assign(fresh(), JSON.parse(raw));
    } catch (_) {
      return fresh();
    }
  }

  function save(state) {
    localStorage.setItem(KEY, JSON.stringify(state));
  }

  function settle(state) {
    const now = Date.now();

    if (state.chips <= 0) {
      state.chips = 0;
      state.frozen = true;
      return state;
    }

    if (state.pauseUntil && now < state.pauseUntil) {
      state.frozen = false;
      return state;
    }

    if (state.pauseUntil && state.lastTick < state.pauseUntil) {
      state.lastTick = state.pauseUntil;
      state.pauseUntil = null;
    }

    const periods = Math.floor((now - state.lastTick) / TWO_DAYS);

    if (periods > 0) {
      state.chips = Math.max(0, state.chips - periods * DECAY);
      state.lastTick += periods * TWO_DAYS;
    }

    state.frozen = state.chips === 0;
    return state;
  }

  function nextLoss(state) {
    if (state.chips <= 0) return null;
    if (state.pauseUntil && Date.now() < state.pauseUntil) {
      return state.pauseUntil + TWO_DAYS;
    }
    return state.lastTick + TWO_DAYS;
  }

  function fmt(ms) {
    return new Date(ms).toLocaleString();
  }

  function buy(appName) {
    const state = settle(load());

    if (state.chips <= 0) {
      save(state);
      alert(
        state.resetsLeft > 0
          ? "You have 0 Data Chips. Reset before buying another app."
          : "You have 0 Data Chips and no resets remain."
      );
      return false;
    }

    state.chips -= COST;
    const now = Date.now();
    state.pauseUntil = now + FIVE_WEEKS;
    state.lastTick = state.pauseUntil;
    state.frozen = state.chips === 0;
    state.purchases.push({
      app: appName,
      cost: COST,
      time: now,
      protectionUntil: state.pauseUntil
    });
    state.purchases = state.purchases.slice(-100);
    save(state);
    render(state);
    return true;
  }

  function useReset() {
    const state = settle(load());

    if (state.chips > 0) {
      alert("Resets can only be used after your chips reach 0.");
      return;
    }

    if (state.resetsLeft <= 0) {
      alert("No Data Chip resets remain.");
      return;
    }

    state.resetsLeft -= 1;
    state.chips = START_CHIPS;
    state.lastTick = Date.now();
    state.pauseUntil = null;
    state.frozen = false;
    save(state);
    render(state);
  }

  function addNavBadge(state) {
    const nav = document.querySelector(".site-nav");
    if (!nav || document.getElementById("data-chip-nav")) return;

    const badge = document.createElement("a");
    badge.id = "data-chip-nav";
    badge.className = "chip-pill";
    badge.href = "data-chips.html";
    badge.innerHTML = `<strong>${state.chips}</strong> Data Chips`;
    nav.appendChild(badge);
  }

  function render(input) {
    const state = settle(input || load());
    save(state);

    const badge = document.getElementById("data-chip-nav");
    if (badge) badge.innerHTML = `<strong>${state.chips}</strong> Data Chips`;
    else addNavBadge(state);

    const chips = document.getElementById("chips-balance");
    const resets = document.getElementById("chips-resets");
    const status = document.getElementById("chips-status");
    const next = document.getElementById("chips-next-loss");

    if (chips) chips.textContent = state.chips;
    if (resets) resets.textContent = state.resetsLeft;

    if (status) {
      if (state.chips === 0) {
        status.textContent =
          state.resetsLeft > 0
            ? "Frozen at 0 — use a reset"
            : "Empty — no resets remain";
      } else if (state.pauseUntil && Date.now() < state.pauseUntil) {
        status.textContent = `Protected until ${fmt(state.pauseUntil)}`;
      } else {
        status.textContent = "Active";
      }
    }

    const loss = nextLoss(state);
    if (next) {
      next.textContent = loss ? `${fmt(loss)} (-${DECAY})` : "Paused at 0";
    }
  }

  function bindDownloads() {
    document.querySelectorAll('a[href*=".zip"]').forEach((link) => {
      if (link.dataset.dataChipsBound === "1") return;
      link.dataset.dataChipsBound = "1";

      link.addEventListener("click", (event) => {
        const card = link.closest(".app-card");
        const title = card?.querySelector("h3")?.textContent?.trim();
        const appName = title || link.textContent.trim() || "Datacenter app";

        if (!buy(appName)) {
          event.preventDefault();
        }
      });
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    const state = settle(load());
    save(state);
    render(state);
    bindDownloads();

    const resetButton = document.getElementById("chips-reset-button");
    if (resetButton) resetButton.addEventListener("click", useReset);
  });

  window.DatacenterChips = {
    status: () => settle(load()),
    buy,
    reset: useReset
  };
})();
