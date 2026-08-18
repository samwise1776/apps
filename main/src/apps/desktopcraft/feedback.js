const form = document.getElementById("feedbackForm");
const nameInput = document.getElementById("feedbackName");
const emailInput = document.getElementById("feedbackEmail");
const messageInput = document.getElementById("feedbackMessage");
const websiteInput = document.getElementById("feedbackWebsite");
const count = document.getElementById("feedbackCount");
const submitButton = document.getElementById("feedbackSubmit");
const status = document.getElementById("feedbackStatus");

function updateCount() {
  count.textContent = String(messageInput.value.length);
}

function showStatus(message, kind = "success") {
  status.textContent = message;
  status.className = `feedback-status${kind === "success" ? "" : ` is-${kind}`}`;
  status.hidden = false;
}

messageInput.addEventListener("input", updateCount);

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!form.reportValidity()) return;

  submitButton.disabled = true;
  submitButton.textContent = "Sending…";
  status.hidden = true;

  try {
    const response = await fetch("/api/feedback", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name: nameInput.value,
        email: emailInput.value,
        message: messageInput.value,
        website: websiteInput.value,
      }),
    });

    let payload = {};
    try {
      payload = await response.json();
    } catch {
      // A non-JSON response normally means the static preview is running without the API.
    }

    if (payload.saved) {
      form.reset();
      updateCount();
      if (payload.emailed) {
        showStatus(payload.message || "Thank you—your feedback was saved and emailed.");
      } else {
        showStatus(payload.warning || "Your message was saved, but email delivery is not configured yet.", "warning");
      }
      return;
    }

    if (!response.ok) {
      throw new Error(payload.error || "The feedback server is unavailable. Run the site with npm start and try again.");
    }

    showStatus(payload.message || "Thank you for the feedback.");
  } catch (error) {
    showStatus(error.message || "Feedback could not be sent. Please try again.", "error");
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = "Send feedback";
  }
});

updateCount();
