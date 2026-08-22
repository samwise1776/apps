const form = document.querySelector("#authForm");
const signInTab = document.querySelector("#signInTab");
const createTab = document.querySelector("#createTab");
const nameField = document.querySelector("#nameField");
const displayName = document.querySelector("#displayName");
const username = document.querySelector("#username");
const password = document.querySelector("#password");
const submitButton = document.querySelector("#authSubmit");
const errorBox = document.querySelector("#authError");
const formKicker = document.querySelector("#formKicker");
const formTitle = document.querySelector("#formTitle");
const formDescription = document.querySelector("#formDescription");

let mode = "sign-in";

const mainPage = "index.html";

function setMode(nextMode) {
  mode = nextMode;
  const creating = mode === "create";
  signInTab.classList.toggle("active", !creating);
  createTab.classList.toggle("active", creating);
  signInTab.setAttribute("aria-selected", String(!creating));
  createTab.setAttribute("aria-selected", String(creating));
  nameField.hidden = !creating;
  displayName.required = creating;
  password.autocomplete = creating ? "new-password" : "current-password";
  formKicker.textContent = creating ? "START YOUR PROFILE" : "WELCOME BACK";
  formTitle.textContent = creating ? "Join the builder league" : "Continue building";
  formDescription.textContent = creating
    ? "Create your Desktopcraft profile. Your sign-in is saved automatically."
    : "Sign in to your Desktopcraft profile.";
  submitButton.textContent = creating ? "Create account" : "Sign in";
  errorBox.hidden = true;
}

signInTab.addEventListener("click", () => setMode("sign-in"));
createTab.addEventListener("click", () => setMode("create"));

if (new URLSearchParams(window.location.search).get("mode") === "create") {
  setMode("create");
}

document.querySelector("#togglePassword").addEventListener("click", (event) => {
  const willShow = password.type === "password";
  password.type = willShow ? "text" : "password";
  event.currentTarget.textContent = willShow ? "Hide" : "Show";
  event.currentTarget.setAttribute("aria-label", willShow ? "Hide password" : "Show password");
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  errorBox.hidden = true;
  if (!form.checkValidity()) {
    form.reportValidity();
    return;
  }
  submitButton.disabled = true;
  submitButton.textContent = mode === "create" ? "Creating…" : "Signing in…";
  try {
    if (mode === "create") {
      await window.DesktopcraftAuth.signUp({ name: displayName.value, username: username.value, password: password.value });
    } else {
      await window.DesktopcraftAuth.signIn({ username: username.value, password: password.value });
    }
    window.location.assign(mainPage);
  } catch (error) {
    errorBox.textContent = error.message;
    errorBox.hidden = false;
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = mode === "create" ? "Create account" : "Sign in";
  }
});

void window.DesktopcraftAuth.ready().then((existingUser) => {
  if (!existingUser || new URLSearchParams(window.location.search).has("switch")) return;
  username.value = existingUser.username;
  formDescription.textContent = `Welcome back, ${existingUser.name}. Opening your account…`;
  window.location.replace(mainPage);
});
