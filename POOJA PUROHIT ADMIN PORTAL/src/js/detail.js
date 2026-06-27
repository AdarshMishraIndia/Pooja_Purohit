/**
 * detail.js
 * Purohit full-screen detail view with inline field editing.
 *
 * Editable:  name, phone, city, locality, experience, proficiency, isVerified
 * Read-only: purohitId, email, fcmTokens, isAvailable, serviceIds,
 *            rating, totalBookings, createdAt, updatedAt
 *
 * proficiency → tickable checklist built from allServices (svc.name).
 * serviceIds  → auto-derived on save: svc.id for every svc whose name
 *               is in the selected proficiency set.
 *
 * Save pattern mirrors services.js offline-resilient race logic.
 *
 * Depends on: firebase.js, utils.js, services.js (allServices),
 *             purohits.js (allPurohits, updateStats, renderList)
 */

/** @type {string|null} */
let activePurohitId = null;

/** @type {boolean} */
let detailEditMode = false;

// ── Screen navigation ───────────────────────────────────────

function openPurohitDetail(id) {
  const p = allPurohits.find(x => x.id === id);
  if (!p) return;

  activePurohitId = id;
  detailEditMode  = false;

  _ensureServicesLoaded().then(() => {
    _setEditMode(false);
    _renderDetail(p);
    document.getElementById("dashboard-screen").style.display      = "none";
    document.getElementById("purohit-detail-screen").style.display = "flex";
    document.getElementById("detail-topbar-name").textContent      = p.name || "Purohit Profile";
    document.getElementById("detail-save-feedback").innerHTML      = "";
  });
}

function closePurohitDetail() {
  document.getElementById("purohit-detail-screen").style.display = "none";
  document.getElementById("dashboard-screen").style.display      = "block";
  activePurohitId = null;
  detailEditMode  = false;
  _setEditMode(false);
}

// ── Services loader (lazy — use cache if already populated) ─

async function _ensureServicesLoaded() {
  if (allServices.length > 0) return;
  try {
    const snap = await db.collection("services").orderBy("displayOrder", "asc").get();
    allServices = snap.docs.map(d => ({ id: d.id, ...d.data() }));
  } catch (e) {
    // Non-fatal — proficiency list will be empty; warn via toast
    showToast("Could not load services list.");
  }
}

// ── Edit mode toggle ────────────────────────────────────────

function _setEditMode(on) {
  detailEditMode = on;

  const editBtn   = document.getElementById("detail-edit-btn");
  const cancelBtn = document.getElementById("detail-cancel-btn");
  const saveBtn   = document.getElementById("detail-save-btn");

  editBtn.disabled        = on;
  editBtn.style.opacity   = on ? "0.4" : "";
  editBtn.style.cursor    = on ? "not-allowed" : "";
  cancelBtn.style.display = on ? "" : "none";
  saveBtn.style.display   = on ? "" : "none";
}

// ── Render (view mode) ──────────────────────────────────────

function _renderDetail(p) {
  const isVerified  = p.isVerified === true;
  const isAvailable = p.isAvailable === true;
  const initials    = (p.name || "?").split(" ").map(w => w[0]).join("").substring(0, 2).toUpperCase();

  const profTags = Array.isArray(p.proficiency) && p.proficiency.length
    ? p.proficiency.map(t => `<span class="dtag">${escHtml(t)}</span>`).join("")
    : '<span class="dvalue muted">—</span>';

  const svcTags = Array.isArray(p.serviceIds) && p.serviceIds.length
    ? p.serviceIds.map(slug => {
        const svc = allServices.find(s => s.id === slug);
        return `<span class="dtag svc">${escHtml(svc ? svc.name : slug)}</span>`;
      }).join("")
    : '<span class="dvalue muted">—</span>';

  document.getElementById("purohit-detail-content").innerHTML = `

    <!-- ── Hero ── -->
    <div class="detail-hero">
      <div class="detail-avatar">${escHtml(initials)}</div>
      <div class="detail-hero-info">
        <div class="detail-hero-name">${escHtml(p.name || "Unnamed")}</div>
        <div class="detail-hero-badges">
          <span class="badge ${isVerified ? "badge-verified" : "badge-unverified"}">${isVerified ? "Verified" : "Unverified"}</span>
          <span class="badge dbadge-neutral">${isAvailable ? "🟢 Available" : "⚫ Unavailable"}</span>
        </div>
        <div class="detail-hero-id">${escHtml(p.purohitId || p.id)}</div>
      </div>
    </div>

    <!-- ── Editable section ── -->
    <div class="dsection">
      <div class="dsection-label">Profile</div>
      <div class="dfields-grid">

        <div class="dfield" id="dfield-name">
          <div class="dfield-label">Name</div>
          <div class="dfield-view dvalue">${escHtml(p.name || "—")}</div>
          <input class="dfield-input" id="dedit-name" type="text" value="${escHtml(p.name || "")}" maxlength="120" placeholder="Full name" style="display:none"/>
        </div>

        <div class="dfield" id="dfield-phone">
          <div class="dfield-label">Phone</div>
          <div class="dfield-view dvalue ${p.phone ? "" : "muted"}">${escHtml(p.phone || "—")}</div>
          <input class="dfield-input" id="dedit-phone" type="tel" value="${escHtml(p.phone || "")}" maxlength="20" placeholder="+91 XXXXX XXXXX" style="display:none"/>
        </div>

        <div class="dfield" id="dfield-city">
          <div class="dfield-label">City</div>
          <div class="dfield-view dvalue ${p.city ? "" : "muted"}">${escHtml(p.city || "—")}</div>
          <input class="dfield-input" id="dedit-city" type="text" value="${escHtml(p.city || "")}" maxlength="80" placeholder="City" style="display:none"/>
        </div>

        <div class="dfield" id="dfield-locality">
          <div class="dfield-label">Locality</div>
          <div class="dfield-view dvalue ${p.locality ? "" : "muted"}">${escHtml(p.locality || "—")}</div>
          <input class="dfield-input" id="dedit-locality" type="text" value="${escHtml(p.locality || "")}" maxlength="120" placeholder="Locality / area" style="display:none"/>
        </div>

        <div class="dfield" id="dfield-experience">
          <div class="dfield-label">Experience (years)</div>
          <div class="dfield-view dvalue ${typeof p.experience === "number" ? "" : "muted"}">
            ${typeof p.experience === "number" ? `${p.experience} years` : "—"}
          </div>
          <input class="dfield-input" id="dedit-experience" type="number" min="0" max="80" step="1"
            value="${typeof p.experience === "number" ? p.experience : ""}" placeholder="Years" style="display:none"/>
        </div>

        <div class="dfield" id="dfield-verified">
          <div class="dfield-label">Verified</div>
          <div class="dfield-view dvalue">${isVerified ? "✓ Yes" : "✗ No"}</div>
          <label class="dtoggle-wrap" id="dedit-verified-wrap" style="display:none">
            <input type="checkbox" id="dedit-verified" ${isVerified ? "checked" : ""}/>
            <span class="dtoggle-label" id="dedit-verified-label">${isVerified ? "Verified" : "Unverified"}</span>
          </label>
        </div>

      </div>
    </div>

    <!-- ── Proficiency (editable via checklist) ── -->
    <div class="dsection" id="dsection-proficiency">
      <div class="dsection-label">Proficiency</div>

      <!-- View mode tags -->
      <div id="dprof-view" class="dtags-row">${profTags}</div>

      <!-- Edit mode checklist -->
      <div id="dprof-edit" style="display:none">
        ${_buildProficiencyChecklist(p.proficiency)}
      </div>
    </div>

    <!-- ── Read-only section ── -->
    <div class="dsection">
      <div class="dsection-label">Services (auto-derived from proficiency)</div>
      <div class="dtags-row">${svcTags}</div>
    </div>

    <div class="dsection">
      <div class="dsection-label">Performance</div>
      <div class="dfields-grid">
        <div class="dfield readonly">
          <div class="dfield-label">Rating</div>
          <div class="dvalue">${typeof p.rating === "number" ? p.rating.toFixed(1) : "—"}</div>
        </div>
        <div class="dfield readonly">
          <div class="dfield-label">Total Bookings</div>
          <div class="dvalue">${typeof p.totalBookings === "number" ? p.totalBookings : "—"}</div>
        </div>
      </div>
    </div>

    <div class="dsection">
      <div class="dsection-label">Read-only / System</div>
      <div class="dfields-grid">
        <div class="dfield readonly full">
          <div class="dfield-label">Email</div>
          <div class="dvalue ${p.email ? "" : "muted"}">${escHtml(p.email || "—")}</div>
        </div>
        <div class="dfield readonly full">
          <div class="dfield-label">Purohit ID</div>
          <div class="dvalue mono">${escHtml(p.purohitId || p.id)}</div>
        </div>
        <div class="dfield readonly">
          <div class="dfield-label">Availability</div>
          <div class="dvalue">${isAvailable ? "Available" : "Unavailable"}</div>
        </div>
        <div class="dfield readonly">
          <div class="dfield-label">FCM Tokens</div>
          <div class="dvalue muted">${Array.isArray(p.fcmTokens) ? `${p.fcmTokens.length} token(s)` : "—"}</div>
        </div>
        <div class="dfield readonly">
          <div class="dfield-label">Registered</div>
          <div class="dvalue" style="font-size:12.5px">${escHtml(formatTs(p.createdAt) || "—")}</div>
        </div>
        <div class="dfield readonly">
          <div class="dfield-label">Last Updated</div>
          <div class="dvalue" style="font-size:12.5px">${escHtml(formatTs(p.updatedAt) || "—")}</div>
        </div>
      </div>
    </div>
  `;
}

// ── Build proficiency checklist from allServices ────────────

function _buildProficiencyChecklist(currentProficiency) {
  const active = allServices.filter(s => s.isActive !== false);
  if (!active.length) {
    return '<div class="dvalue muted" style="padding:4px 0">No services available to select from.</div>';
  }
  const selected = new Set(Array.isArray(currentProficiency) ? currentProficiency : []);
  return `
    <div class="dprof-checklist" id="dprof-checklist">
      ${active.map(svc => `
        <label class="dprof-item ${selected.has(svc.name) ? "checked" : ""}">
          <input type="checkbox" value="${escHtml(svc.name)}" ${selected.has(svc.name) ? "checked" : ""}
            onchange="this.closest('label').classList.toggle('checked', this.checked)"/>
          <span class="dprof-item-name">${escHtml(svc.name)}</span>
        </label>
      `).join("")}
    </div>
  `;
}

// ── Enter / exit edit mode ──────────────────────────────────

function _enterEditMode() {
  _setEditMode(true);

  // Switch each editable field: hide view div, show input
  ["name","phone","city","locality","experience"].forEach(key => {
    document.querySelector(`#dfield-${key} .dfield-view`).style.display  = "none";
    document.querySelector(`#dfield-${key} .dfield-input`).style.display = "";
  });

  // Verified toggle
  document.querySelector("#dfield-verified .dfield-view").style.display   = "none";
  document.getElementById("dedit-verified-wrap").style.display             = "";

  // isVerified checkbox label sync
  const cb = document.getElementById("dedit-verified");
  const lbl = document.getElementById("dedit-verified-label");
  cb.onchange = () => { lbl.textContent = cb.checked ? "Verified" : "Unverified"; };

  // Proficiency: hide tag view, show checklist
  document.getElementById("dprof-view").style.display = "none";
  document.getElementById("dprof-edit").style.display = "";
}

function _exitEditMode(p) {
  // Re-render completely from data object (discards any DOM edits)
  _renderDetail(p);
  _setEditMode(false);
  document.getElementById("detail-save-feedback").innerHTML = "";
}

// ── Read current edit values from DOM ───────────────────────

function _readEditValues() {
  const name       = document.getElementById("dedit-name").value.trim();
  const phone      = document.getElementById("dedit-phone").value.trim();
  const city       = document.getElementById("dedit-city").value.trim();
  const locality   = document.getElementById("dedit-locality").value.trim();
  const expRaw     = document.getElementById("dedit-experience").value.trim();
  const experience = expRaw !== "" ? parseInt(expRaw, 10) : null;
  const isVerified = document.getElementById("dedit-verified").checked;

  // Collect checked proficiencies
  const proficiency = [];
  document.querySelectorAll("#dprof-checklist input[type=checkbox]:checked").forEach(cb => {
    proficiency.push(cb.value);
  });

  // Derive serviceIds: for every selected proficiency name, find matching service id
  const serviceIds = proficiency
    .map(profName => {
      const svc = allServices.find(s => s.name === profName && s.isActive !== false);
      return svc ? svc.id : null;
    })
    .filter(Boolean);

  return { name, phone, city, locality, experience, isVerified, proficiency, serviceIds };
}

// ── Save ─────────────────────────────────────────────────────

async function savePurohitDetail() {
  const p = allPurohits.find(x => x.id === activePurohitId);
  if (!p) return;

  const vals = _readEditValues();

  // Basic validation
  if (!vals.name) { showToast("Name cannot be empty."); document.getElementById("dedit-name").focus(); return; }
  if (vals.experience !== null && (isNaN(vals.experience) || vals.experience < 0)) {
    showToast("Experience must be a non-negative number.");
    document.getElementById("dedit-experience").focus();
    return;
  }

  const updates = {
    name:        vals.name,
    phone:       vals.phone       || firebase.firestore.FieldValue.delete(),
    city:        vals.city        || firebase.firestore.FieldValue.delete(),
    locality:    vals.locality    || firebase.firestore.FieldValue.delete(),
    isVerified:  vals.isVerified,
    isAvailable: vals.isVerified,
    proficiency: vals.proficiency,
    serviceIds:  vals.serviceIds,
    updatedAt:   firebase.firestore.FieldValue.serverTimestamp(),
  };
  if (vals.experience !== null) updates.experience = vals.experience;
  else updates.experience = firebase.firestore.FieldValue.delete();

  // Lock UI
  const saveBtn   = document.getElementById("detail-save-btn");
  const cancelBtn = document.getElementById("detail-cancel-btn");
  const feedback  = document.getElementById("detail-save-feedback");
  saveBtn.disabled   = true;  saveBtn.textContent   = "Saving…";
  cancelBtn.disabled = true;
  feedback.innerHTML = "";

  const writePromise = db.collection("purohits").doc(activePurohitId).update(updates);

  const OFFLINE_MS = 5000;
  let offlineTimer;
  const offlineSentinel = new Promise(resolve => {
    offlineTimer = setTimeout(() => resolve("__OFFLINE__"), OFFLINE_MS);
  });

  const race = await Promise.race([
    writePromise.then(() => "__SUCCESS__").catch(e => e),
    offlineSentinel,
  ]);

  if (race !== "__OFFLINE__") {
    clearTimeout(offlineTimer);

    if (race === "__SUCCESS__") {
      // Patch local cache
      Object.assign(p, {
        name:        vals.name,
        phone:       vals.phone       || undefined,
        city:        vals.city        || undefined,
        locality:    vals.locality    || undefined,
        isVerified:  vals.isVerified,
        isAvailable: vals.isVerified,
        proficiency: vals.proficiency,
        serviceIds:  vals.serviceIds,
        experience:  vals.experience !== null ? vals.experience : undefined,
        updatedAt:   new Date().toISOString(),
      });
      updateStats();
      renderList();
      document.getElementById("detail-topbar-name").textContent = vals.name || "Purohit Profile";
      _exitEditMode(p);
      showToast("Purohit updated ✓");
    } else {
      // Write rejected
      saveBtn.disabled = false; saveBtn.textContent = "Save Changes";
      cancelBtn.disabled = false;
      feedback.innerHTML = `
        <div class="detail-save-error">
          <span>⚠</span>
          <div>
            <div class="dse-title">Update failed</div>
            <div class="dse-desc">${escHtml(race?.message ?? "Unexpected error.")}</div>
          </div>
          <button class="btn-retry-inline" onclick="savePurohitDetail()">Retry</button>
        </div>`;
    }
    return;
  }

  // Timed-out — buffered offline
  saveBtn.textContent = "Queued…";
  feedback.innerHTML = `
    <div class="detail-save-waiting">
      <div class="spinner-sm"></div>
      <div>
        <div class="dse-title">No connection — change queued</div>
        <div class="dse-desc">Will sync automatically when connection is restored.</div>
      </div>
    </div>`;

  try {
    await writePromise;
    clearTimeout(offlineTimer);
    Object.assign(p, {
      name:        vals.name,
      phone:       vals.phone       || undefined,
      city:        vals.city        || undefined,
      locality:    vals.locality    || undefined,
      isVerified:  vals.isVerified,
      isAvailable: vals.isVerified,
      proficiency: vals.proficiency,
      serviceIds:  vals.serviceIds,
      experience:  vals.experience !== null ? vals.experience : undefined,
      updatedAt:   new Date().toISOString(),
    });
    updateStats();
    renderList();
    document.getElementById("detail-topbar-name").textContent = vals.name || "Purohit Profile";
    _exitEditMode(p);
    showToast("Purohit synced ✓");
  } catch (e) {
    saveBtn.disabled = false; saveBtn.textContent = "Save Changes";
    cancelBtn.disabled = false;
    feedback.innerHTML = `
      <div class="detail-save-error">
        <span>⚠</span>
        <div>
          <div class="dse-title">Sync failed</div>
          <div class="dse-desc">${escHtml(e?.message ?? "Queued write rejected by server.")}</div>
        </div>
        <button class="btn-retry-inline" onclick="savePurohitDetail()">Retry</button>
      </div>`;
  }
}

// ── Cancel ───────────────────────────────────────────────────

function cancelPurohitEdit() {
  const p = allPurohits.find(x => x.id === activePurohitId);
  if (p) _exitEditMode(p);
}

// ── Event wiring ────────────────────────────────────────────

document.getElementById("back-to-dashboard-btn").addEventListener("click", () => {
  if (detailEditMode) {
    // Discard edits silently, then go back
    cancelPurohitEdit();
  }
  closePurohitDetail();
});

document.getElementById("detail-edit-btn").addEventListener("click", () => {
  if (!detailEditMode) _enterEditMode();
});

document.getElementById("detail-cancel-btn").addEventListener("click", cancelPurohitEdit);

document.getElementById("detail-save-btn").addEventListener("click", savePurohitDetail);

document.addEventListener("keydown", e => {
  if (e.key === "Escape") {
    if (document.getElementById("services-overlay").classList.contains("open")) closeServicesOverlay();
    else if (document.getElementById("purohit-detail-screen").style.display !== "none") {
      if (detailEditMode) cancelPurohitEdit();
      else closePurohitDetail();
    }
  }
});