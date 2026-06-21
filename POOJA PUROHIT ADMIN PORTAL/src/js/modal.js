/**
 * modal.js
 * Purohit detail modal — open, close, render.
 * Depends on: utils.js, purohits.js (allPurohits, toggleVerify),
 *             services.js (allServices)
 */

/** @type {string|null} */
let activePurohitId = null;

// ── Public API ──────────────────────────────────────────────

function openPurohitModal(id) {
  const p = allPurohits.find(x => x.id === id);
  if (!p) return;
  activePurohitId = id;
  renderPurohitModal(p);
  document.getElementById("purohit-modal-backdrop").classList.add("open");
  document.body.style.overflow = "hidden";
}

function closePurohitModal() {
  document.getElementById("purohit-modal-backdrop").classList.remove("open");
  document.body.style.overflow = "";
  activePurohitId = null;
}

function renderPurohitModal(p) {
  if (!p) return;

  const isVerified  = p.isVerified === true;
  const isAvailable = p.isAvailable === true;
  const initials    = (p.name || "?").split(" ").map(w => w[0]).join("").substring(0, 2).toUpperCase();
  const photoURL    = p.photoURL || null;

  const profTags = Array.isArray(p.proficiency) && p.proficiency.length
    ? p.proficiency.map(t => `<span class="modal-tag">${escHtml(t)}</span>`).join("")
    : '<span class="modal-tag" style="opacity:0.5">—</span>';

  const svcTags = Array.isArray(p.serviceIds) && p.serviceIds.length
    ? p.serviceIds.map(slug => {
        const svc = allServices.find(s => s.id === slug);
        return `<span class="modal-tag service">${escHtml(svc ? svc.name : slug)}</span>`;
      }).join("")
    : '<span style="font-size:13px;color:var(--text-muted);font-style:italic">None assigned</span>';

  // Footer
  document.getElementById("modal-footer-info").textContent = `ID: ${p.id}`;

  const modalVerifyBtn = document.getElementById("modal-verify-btn");
  modalVerifyBtn.disabled    = false;
  modalVerifyBtn.className   = "btn-modal-verify " + (isVerified ? "unverify" : "verify");
  modalVerifyBtn.textContent = isVerified ? "Unverify" : "Verify";
  modalVerifyBtn.onclick     = () => toggleVerify(p.id, !isVerified);

  document.getElementById("purohit-modal-body").innerHTML = `
    <div class="modal-profile-hero">
      <div class="modal-avatar-wrap">
        <div class="modal-avatar" id="modal-avatar-el">
          ${photoURL
            ? `<img src="${escHtml(photoURL)}" alt="${escHtml(p.name || "")}" onerror="this.parentElement.textContent='${escHtml(initials)}'">` 
            : escHtml(initials)
          }
        </div>
        <div class="modal-avail-dot ${isAvailable ? "available" : "unavailable"}" title="${isAvailable ? "Available" : "Unavailable"}"></div>
        ${!photoURL ? `<div style="font-size:9.5px;color:var(--text-muted);text-align:center;margin-top:5px;line-height:1.35;max-width:72px;">No photo —<br>write photoURL<br>to Firestore</div>` : ""}
      </div>
      <div class="modal-profile-info">
        <div class="modal-profile-name" id="modal-purohit-name">${escHtml(p.name || "Unnamed")}</div>
        <div class="modal-badges">
          <span class="badge ${isVerified ? "badge-verified" : "badge-unverified"}">${isVerified ? "Verified" : "Unverified"}</span>
          <span class="badge" style="background:var(--bg);border:1px solid var(--border);color:var(--text-secondary);">
            ${isAvailable ? "🟢 Available" : "⚫ Unavailable"}
          </span>
        </div>
        <div class="modal-id">${escHtml(p.purohitId || p.id)}</div>
      </div>
    </div>

    <div class="modal-section">
      <div class="modal-section-label">Contact</div>
      <div class="modal-fields-grid">
        <div class="modal-field">
          <div class="modal-field-label">Phone</div>
          <div class="modal-field-value ${p.phone ? "" : "muted"}">${escHtml(p.phone || "—")}</div>
        </div>
        <div class="modal-field">
          <div class="modal-field-label">Email</div>
          <div class="modal-field-value ${p.email ? "" : "muted"}">${escHtml(p.email || "—")}</div>
        </div>
      </div>
    </div>

    <div class="modal-section">
      <div class="modal-section-label">Location</div>
      <div class="modal-fields-grid">
        <div class="modal-field">
          <div class="modal-field-label">City</div>
          <div class="modal-field-value ${p.city ? "" : "muted"}">${escHtml(p.city || "—")}</div>
        </div>
        <div class="modal-field">
          <div class="modal-field-label">Locality</div>
          <div class="modal-field-value ${p.locality ? "" : "muted"}">${escHtml(p.locality || "—")}</div>
        </div>
      </div>
    </div>

    <div class="modal-section">
      <div class="modal-section-label">Expertise</div>
      <div class="modal-fields-grid">
        <div class="modal-field">
          <div class="modal-field-label">Experience</div>
          <div class="modal-field-value">
            ${typeof p.experience === "number" ? `${p.experience} years` : '<span class="muted">—</span>'}
          </div>
        </div>
        <div class="modal-field">
          <div class="modal-field-label">Proficiency</div>
          <div class="modal-tags">${profTags}</div>
        </div>
      </div>
    </div>

    <div class="modal-section">
      <div class="modal-section-label">Services Offered</div>
      <div class="modal-tags">${svcTags}</div>
    </div>

    <div class="modal-section">
      <div class="modal-section-label">Performance Metrics</div>
      <div class="modal-metrics">
        <div class="modal-metric">
          <div class="modal-metric-value">${typeof p.rating === "number" ? p.rating.toFixed(1) : "—"}</div>
          <div class="modal-metric-label">Rating</div>
        </div>
        <div class="modal-metric">
          <div class="modal-metric-value">${typeof p.totalBookings === "number" ? p.totalBookings : "—"}</div>
          <div class="modal-metric-label">Bookings</div>
        </div>
        <div class="modal-metric">
          <div class="modal-metric-value">${typeof p.trustIndex === "number" ? p.trustIndex.toFixed(1) : "—"}</div>
          <div class="modal-metric-label">Trust Index</div>
        </div>
      </div>
    </div>

    <div class="modal-section">
      <div class="modal-section-label">Timestamps</div>
      <div class="modal-fields-grid">
        <div class="modal-field">
          <div class="modal-field-label">Registered</div>
          <div class="modal-field-value" style="font-size:12.5px">${escHtml(formatTs(p.createdAt) || "—")}</div>
        </div>
        <div class="modal-field">
          <div class="modal-field-label">Last Updated</div>
          <div class="modal-field-value" style="font-size:12.5px">${escHtml(formatTs(p.updatedAt) || "—")}</div>
        </div>
      </div>
    </div>
  `;
}

// ── Event wiring ────────────────────────────────────────────

document.getElementById("close-purohit-modal-btn").addEventListener("click", closePurohitModal);

document.getElementById("purohit-modal-backdrop").addEventListener("click", function(e) {
  if (e.target === this) closePurohitModal();
});

document.addEventListener("keydown", e => {
  if (e.key === "Escape") {
    if (document.getElementById("purohit-modal-backdrop").classList.contains("open")) closePurohitModal();
    if (document.getElementById("services-overlay").classList.contains("open"))       closeServicesOverlay();
  }
});
