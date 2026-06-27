/**
 * purohits.js
 * Manages the purohit list: loading from Firestore, filtering,
 * rendering list cards, and toggling verification status.
 * Depends on: firebase.js, utils.js
 */

/** @type {Array<Object>} */
let allPurohits = [];

/** @type {string} */
let activeFilter = "all";

// ── Internal helpers ────────────────────────────────────────

function showLoading() {
  document.getElementById("purohit-list").innerHTML =
    '<div class="loading-state"><div class="spinner"></div>Loading purohits…</div>';
}

function updateStats() {
  const total      = allPurohits.length;
  const verified   = allPurohits.filter(p => p.isVerified === true).length;
  const unverified = total - verified;
  document.getElementById("stat-total").textContent      = total;
  document.getElementById("stat-verified").textContent   = verified;
  document.getElementById("stat-unverified").textContent = unverified;
}

function getFiltered() {
  const q = document.getElementById("search-input").value.toLowerCase();
  return allPurohits.filter(p => {
    const matchFilter =
      activeFilter === "all" ||
      (activeFilter === "verified"   && p.isVerified === true) ||
      (activeFilter === "unverified" && p.isVerified !== true);
    const matchSearch = !q ||
      (p.name  || "").toLowerCase().includes(q) ||
      (p.city  || "").toLowerCase().includes(q) ||
      (p.email || "").toLowerCase().includes(q);
    return matchFilter && matchSearch;
  });
}

// ── Public API ──────────────────────────────────────────────

async function loadPurohits() {
  showLoading();
  try {
    const snap = await db.collection("purohits").get();
    allPurohits = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    updateStats();
    renderList();
  } catch (e) {
    document.getElementById("purohit-list").innerHTML =
      `<div class="empty-state">⚠ Failed to load data.<br><small>${escHtml(e.message)}</small></div>`;
  }
}

function renderList() {
  const list     = document.getElementById("purohit-list");
  const filtered = getFiltered();

  if (filtered.length === 0) {
    list.innerHTML = '<div class="empty-state">No purohits found matching your filter.</div>';
    return;
  }

  list.innerHTML = filtered.map(p => {
    const initials   = (p.name || "?").split(" ").map(w => w[0]).join("").substring(0, 2).toUpperCase();
    const isVerified = p.isVerified === true;
    const profSnip   = Array.isArray(p.proficiency)
      ? p.proficiency.slice(0, 2).join(", ")
      : (p.proficiency || "—");
    const rating = (typeof p.rating === "number" && p.rating > 0) ? `★ ${p.rating.toFixed(1)}` : "";

    return `
      <div class="purohit-card" id="card-${p.id}" onclick="openPurohitDetail('${p.id}')">
        <div class="avatar">${escHtml(initials)}</div>
        <div class="purohit-info">
          <div class="purohit-name">${escHtml(p.name || "Unnamed")}</div>
          <div class="purohit-meta">
            ${p.city   ? `<span>📍 ${escHtml(p.city)}</span>` : ""}
            ${p.phone  ? `<span>${escHtml(p.phone)}</span>` : ""}
            ${profSnip ? `<span>${escHtml(profSnip)}</span>` : ""}
            ${rating   ? `<span>${rating}</span>` : ""}
            ${typeof p.experience === "number" ? `<span>${p.experience} yrs exp</span>` : ""}
          </div>
        </div>
        <span class="badge ${isVerified ? "badge-verified" : "badge-unverified"}">
          ${isVerified ? "Verified" : "Unverified"}
        </span>
        ${isVerified
          ? `<button class="action-btn btn-unverify" onclick="event.stopPropagation(); quickToggleVerify('${p.id}', false)">Unverify</button>`
          : `<button class="action-btn btn-verify"   onclick="event.stopPropagation(); quickToggleVerify('${p.id}', true)">Verify</button>`
        }
      </div>
    `;
  }).join("");
}

// Quick verify toggle from list card (no detail screen involved)
async function quickToggleVerify(id, value) {
  const listBtn = document.querySelector(`#card-${id} .action-btn`);
  if (listBtn) { listBtn.disabled = true; listBtn.textContent = "Saving…"; }

  try {
    await db.collection("purohits").doc(id).update({
      isVerified:  value,
      isAvailable: value,
      updatedAt:   firebase.firestore.FieldValue.serverTimestamp()
    });
    const p = allPurohits.find(x => x.id === id);
    if (p) { p.isVerified = value; p.isAvailable = value; }
    updateStats();
    renderList();
    showToast(value ? "Purohit verified ✓" : "Marked as unverified");
  } catch (e) {
    showToast("Error: " + e.message);
    if (listBtn) { listBtn.disabled = false; listBtn.textContent = value ? "Verify" : "Unverify"; }
  }
}

// ── Event wiring ────────────────────────────────────────────

document.getElementById("refresh-btn").addEventListener("click", loadPurohits);

document.querySelectorAll(".tab-btn").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    activeFilter = btn.dataset.filter;
    renderList();
  });
});

document.getElementById("search-input").addEventListener("input", renderList);