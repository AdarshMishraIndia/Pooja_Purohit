/**
 * auth.js
 * Handles login / logout UI transitions.
 * Depends on: utils.js
 */

const ADMIN_USERNAME = "admin";
const ADMIN_PASSWORD = "admin#123";

function handleLogin() {
  const u   = document.getElementById("username").value.trim();
  const p   = document.getElementById("password").value;
  const err = document.getElementById("login-error");

  if (u === ADMIN_USERNAME && p === ADMIN_PASSWORD) {
    err.style.display = "none";
    document.getElementById("login-screen").style.display     = "none";
    document.getElementById("dashboard-screen").style.display = "block";
    loadPurohits();
  } else {
    err.style.display = "block";
    document.getElementById("password").value = "";
  }
}

function handleLogout() {
  document.getElementById("dashboard-screen").style.display      = "none";
  document.getElementById("purohit-detail-screen").style.display = "none";
  document.getElementById("login-screen").style.display          = "flex";
  document.getElementById("username").value = "";
  document.getElementById("password").value = "";

  // Reset shared state
  allPurohits = [];
  allServices = [];
  closePurohitDetail();
  closeServicesOverlay();
}

// ── Event wiring ────────────────────────────────────────────
document.getElementById("login-btn").addEventListener("click", handleLogin);

["username", "password"].forEach(id => {
  document.getElementById(id).addEventListener("keydown", e => {
    if (e.key === "Enter") handleLogin();
  });
});

document.getElementById("logout-btn").addEventListener("click", handleLogout);
