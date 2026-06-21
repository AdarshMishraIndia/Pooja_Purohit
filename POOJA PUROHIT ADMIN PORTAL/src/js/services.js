/**
 * services.js
 * Services overlay: load from Firestore, render grid,
 * add new service, inline edit with offline-resilient save.
 * Depends on: firebase.js, utils.js
 */

/** @type {Array<Object>} */
let allServices = [];

/** @type {string|null} Enforces one-edit-at-a-time */
let activeEditSlug = null;

// ── Overlay open / close ────────────────────────────────────

function openServicesOverlay() {
  document.getElementById("services-overlay").classList.add("open");
  document.body.style.overflow = "hidden";
  loadServices();
}

function closeServicesOverlay() {
  document.getElementById("services-overlay").classList.remove("open");
  document.body.style.overflow = "";
  document.getElementById("svc-add-form").classList.remove("visible");
  document.getElementById("svc-add-input").value = "";
  document.getElementById("svc-add-price-input").value = "";
}

// ── Load ────────────────────────────────────────────────────

async function loadServices() {
  document.getElementById("svc-grid").innerHTML =
    '<div class="svc-loading"><div class="spinner"></div>Loading services…</div>';
  document.getElementById("svc-count-label").textContent = "Loading…";

  try {
    const snap = await db.collection("services").orderBy("displayOrder", "asc").get();
    allServices = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    renderServicesGrid();
  } catch (e) {
    document.getElementById("svc-grid").innerHTML =
      `<div class="svc-loading">⚠ Failed to load services.<br><small>${escHtml(e.message)}</small></div>`;
    document.getElementById("svc-count-label").textContent = "Error";
  }
}

// ── Render ──────────────────────────────────────────────────

function renderServicesGrid() {
  const active = allServices.filter(s => s.isActive !== false);
  document.getElementById("svc-count-label").textContent =
    `${active.length} active service${active.length !== 1 ? "s" : ""}`;

  if (!allServices.length) {
    document.getElementById("svc-grid").innerHTML =
      '<div class="svc-loading">No services found. Add one above.</div>';
    return;
  }

  const isEditing = activeEditSlug !== null;

  document.getElementById("svc-grid").innerHTML = allServices.map(svc => {
    const thisEditing = activeEditSlug === svc.id;
    if (thisEditing) return ""; // managed by enterServiceEditMode; skip re-render

    return `
      <div class="svc-card" id="svc-card-${svc.id}">
        <div class="svc-order-badge">${svc.displayOrder ?? "?"}</div>
        <div class="svc-card-name">
          ${escHtml(svc.name)}
          <span class="svc-card-slug">${escHtml(svc.id)}</span>
          <span class="svc-card-price">${
            typeof svc.price === "number"
              ? `₹${svc.price.toLocaleString("en-IN")}`
              : '<span style="color:var(--text-muted);font-weight:400">No price set</span>'
          }</span>
        </div>
        <button class="btn-edit-icon"
          onclick="enterServiceEditMode('${svc.id}')"
          title="${isEditing ? "Finish the current edit first" : "Edit service"}"
          ${isEditing ? 'disabled style="opacity:0.35;cursor:not-allowed;"' : ""}
        >
          <svg width="13" height="13" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M11.5 2.5a2.121 2.121 0 013 3L5 15H1v-4L11.5 2.5z"/>
          </svg>
        </button>
      </div>
    `;
  }).join("");
}

// ── Inline edit ─────────────────────────────────────────────

function enterServiceEditMode(slug) {
  if (activeEditSlug !== null) {
    if (activeEditSlug === slug) return;
    showToast("Finish editing the current service first.");
    const activeCard = document.getElementById("svc-card-" + activeEditSlug);
    if (activeCard) {
      activeCard.style.transition = "box-shadow 0.15s";
      activeCard.style.boxShadow  = "0 0 0 3px rgba(232,115,10,0.45)";
      setTimeout(() => { activeCard.style.boxShadow = ""; }, 900);
    }
    return;
  }

  const svc  = allServices.find(s => s.id === slug);
  const card = document.getElementById("svc-card-" + slug);
  if (!svc || !card) return;

  activeEditSlug = slug;
  card.classList.add("editing");
  card.innerHTML = `
    <div class="svc-edit-row">
      <div class="svc-edit-name-row">
        <div class="svc-order-badge">${svc.displayOrder ?? "?"}</div>
        <input
          class="svc-edit-input"
          id="svc-edit-input-${slug}"
          value="${escHtml(svc.name)}"
          maxlength="120"
          autocomplete="off"
          placeholder="Service name"
        />
      </div>
      <input
        class="svc-price-input"
        id="svc-price-input-${slug}"
        type="number"
        min="0"
        step="1"
        value="${typeof svc.price === "number" ? svc.price : ""}"
        placeholder="Price (₹) — optional"
      />
    </div>
    <div id="svc-save-feedback-${slug}"></div>
    <div style="display:flex; gap:8px; justify-content:flex-end;">
      <button class="btn-svc-cancel" id="svc-cancel-${slug}" onclick="cancelServiceEdit('${slug}')">Cancel</button>
      <button class="btn-svc-save"   id="svc-save-${slug}"   onclick="saveServiceEdit('${slug}')">Save</button>
    </div>
  `;

  const input = document.getElementById("svc-edit-input-" + slug);
  input.focus();
  input.select();
  input.addEventListener("keydown", e => {
    if (e.key === "Enter")  saveServiceEdit(slug);
    if (e.key === "Escape") cancelServiceEdit(slug);
  });

  const priceInput = document.getElementById("svc-price-input-" + slug);
  priceInput.addEventListener("keydown", e => {
    if (e.key === "Enter")  saveServiceEdit(slug);
    if (e.key === "Escape") cancelServiceEdit(slug);
  });
}

function cancelServiceEdit(slug) {
  activeEditSlug = null;
  renderServicesGrid();
}

async function saveServiceEdit(slug) {
  const input      = document.getElementById("svc-edit-input-" + slug);
  const priceInput = document.getElementById("svc-price-input-" + slug);
  const saveBtn    = document.getElementById("svc-save-" + slug);
  const cancelBtn  = document.getElementById("svc-cancel-" + slug);
  const feedback   = document.getElementById("svc-save-feedback-" + slug);
  if (!input) return;

  const newName  = input.value.trim();
  const priceRaw = priceInput ? priceInput.value.trim() : "";
  const newPrice = priceRaw !== "" ? parseInt(priceRaw, 10) : null;

  if (!newName) { showToast("Name cannot be empty."); input.focus(); return; }
  if (newPrice !== null && (isNaN(newPrice) || newPrice < 0)) {
    showToast("Enter a valid price (≥ 0)."); priceInput.focus(); return;
  }

  const svc = allServices.find(s => s.id === slug);
  const nameUnchanged  = newName === svc?.name;
  const priceUnchanged =
    newPrice === (typeof svc?.price === "number" ? svc.price : null) ||
    (newPrice === null && svc?.price === undefined);
  if (nameUnchanged && priceUnchanged) { cancelServiceEdit(slug); return; }

  const lockAll = (locked, saveBtnLabel = "Saving…") => {
    [input, priceInput, saveBtn, cancelBtn].forEach(el => { if (el) el.disabled = locked; });
    if (saveBtn) saveBtn.textContent = locked ? saveBtnLabel : "Save";
  };
  lockAll(true);
  if (feedback) feedback.innerHTML = "";

  const updates = { name: newName };
  if (newPrice !== null) updates.price = newPrice;
  else updates.price = firebase.firestore.FieldValue.delete();

  const writePromise = db.collection("services").doc(slug).update(updates);

  const OFFLINE_THRESHOLD_MS = 5000;
  let offlineTimerId = null;
  const offlineSentinel = new Promise(resolve => {
    offlineTimerId = setTimeout(() => resolve("__OFFLINE__"), OFFLINE_THRESHOLD_MS);
  });

  const raceResult = await Promise.race([
    writePromise.then(() => "__SUCCESS__").catch(e => e),
    offlineSentinel
  ]);

  // Resolved within threshold
  if (raceResult !== "__OFFLINE__") {
    clearTimeout(offlineTimerId);
    if (raceResult === "__SUCCESS__") {
      if (svc) {
        svc.name = newName;
        if (newPrice !== null) svc.price = newPrice; else delete svc.price;
      }
      activeEditSlug = null;
      renderServicesGrid();
      showToast("Service updated ✓");
    } else {
      lockAll(false);
      const err = raceResult;
      if (feedback) feedback.innerHTML = `
        <div class="svc-save-error">
          <span class="svc-save-error-icon">⚠</span>
          <div class="svc-save-error-body">
            <div class="svc-save-error-title">Update failed</div>
            <div class="svc-save-error-desc">${escHtml(err?.message ?? "Unexpected error. Try again or reload.")}</div>
          </div>
          <button class="btn-retry-inline" onclick="saveServiceEdit('${slug}')">Retry</button>
        </div>`;
    }
    return;
  }

  // Timed out — write buffered offline
  if (saveBtn) { saveBtn.disabled = true; saveBtn.textContent = "Queued…"; }
  if (feedback) feedback.innerHTML = `
    <div class="svc-save-waiting">
      <div class="spinner-sm"></div>
      <div class="svc-save-waiting-body">
        <div class="svc-save-waiting-title">No connection — change queued</div>
        <div class="svc-save-waiting-desc">Your edit is saved locally and will sync automatically when the connection is restored.</div>
      </div>
    </div>`;

  try {
    await writePromise;
    clearTimeout(offlineTimerId);
    if (svc) {
      svc.name = newName;
      if (newPrice !== null) svc.price = newPrice; else delete svc.price;
    }
    activeEditSlug = null;
    renderServicesGrid();
    showToast("Service synced ✓");
  } catch (e) {
    lockAll(false);
    if (feedback) feedback.innerHTML = `
      <div class="svc-save-error">
        <span class="svc-save-error-icon">⚠</span>
        <div class="svc-save-error-body">
          <div class="svc-save-error-title">Sync failed</div>
          <div class="svc-save-error-desc">${escHtml(e?.message ?? "The queued write was rejected by the server.")}</div>
        </div>
        <button class="btn-retry-inline" onclick="saveServiceEdit('${slug}')">Retry</button>
      </div>`;
  }
}

// ── Add new service ─────────────────────────────────────────

async function submitAddService() {
  const input      = document.getElementById("svc-add-input");
  const priceInput = document.getElementById("svc-add-price-input");
  const addBtn     = document.getElementById("svc-add-submit-btn");
  const name       = input.value.trim();
  const priceRaw   = priceInput ? priceInput.value.trim() : "";
  const price      = priceRaw !== "" ? parseInt(priceRaw, 10) : null;

  if (!name) { showToast("Enter a service name."); input.focus(); return; }
  if (price !== null && (isNaN(price) || price < 0)) {
    showToast("Enter a valid price (≥ 0)."); priceInput.focus(); return;
  }

  const slug = generateSlug(name);
  if (!slug) {
    showToast("Could not generate a valid slug from that name. Use alphanumeric characters.");
    return;
  }

  if (allServices.some(s => s.id === slug)) {
    showToast(`A service with slug "${slug}" already exists.`);
    return;
  }

  const maxOrder     = allServices.reduce((m, s) => Math.max(m, s.displayOrder ?? 0), 0);
  const displayOrder = maxOrder + 1;

  addBtn.disabled = true; addBtn.textContent = "Adding…";

  try {
    const docRef  = db.collection("services").doc(slug);
    const newData = {
      name,
      slug,
      displayOrder,
      isActive: true,
      createdAt: firebase.firestore.FieldValue.serverTimestamp()
    };
    if (price !== null) newData.price = price;
    await docRef.set(newData);

    allServices.push({ id: slug, ...newData, createdAt: new Date().toISOString() });
    allServices.sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));

    input.value = "";
    if (priceInput) priceInput.value = "";
    document.getElementById("svc-add-form").classList.remove("visible");
    renderServicesGrid();
    showToast(`Service "${name}" added ✓`);
  } catch (e) {
    showToast("Error: " + e.message);
  } finally {
    addBtn.disabled = false; addBtn.textContent = "Add";
  }
}

// ── Event wiring ────────────────────────────────────────────

document.getElementById("open-services-btn").addEventListener("click",  openServicesOverlay);
document.getElementById("close-services-btn").addEventListener("click", closeServicesOverlay);

document.getElementById("show-add-service-btn").addEventListener("click", () => {
  document.getElementById("svc-add-form").classList.add("visible");
  document.getElementById("svc-add-input").focus();
});

document.getElementById("svc-add-cancel-btn").addEventListener("click", () => {
  document.getElementById("svc-add-form").classList.remove("visible");
  document.getElementById("svc-add-input").value = "";
  document.getElementById("svc-add-price-input").value = "";
});

document.getElementById("svc-add-submit-btn").addEventListener("click", submitAddService);

document.getElementById("svc-add-input").addEventListener("keydown", e => {
  if (e.key === "Enter")  submitAddService();
  if (e.key === "Escape") {
    document.getElementById("svc-add-form").classList.remove("visible");
    document.getElementById("svc-add-input").value = "";
    document.getElementById("svc-add-price-input").value = "";
  }
});
