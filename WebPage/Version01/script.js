document.addEventListener("DOMContentLoaded", () => {
  const urlInput = document.getElementById("urlInput");
  const checkButton = document.getElementById("checkButton");
  const modal = document.getElementById("modal");
  const modalHeader = document.getElementById("modalHeader");
  const closeButton = document.getElementById("closeButton");

  let isSafe;

  // Show the modal with the appropriate message
  function showModal() {
    if (isSafe) {
      // Show safe message (no additional message)
      modalHeader.innerHTML = `<p style="color: green;">✔️ There's a ${Math.floor(Math.random() * 20) + 80}% chance that it's safe.</p>`;
      closeButton.style.backgroundColor = "#22c55e"; // Green for safe
      closeButton.removeEventListener("click", showBlockedMessage); // Remove event listener if safe
      closeButton.addEventListener("click", closeModal); // Add event listener for closing the modal
    } else {
      // Show malicious message
      modalHeader.innerHTML = `<p style="color: red;">🚨 There's a ${Math.floor(Math.random() * 20) + 80}% chance that it's malicious.</p>`;
      closeButton.style.backgroundColor = "#dc2626"; // Red for malicious

      // Add event listener to show "Access is blocked" when close button is clicked
      closeButton.removeEventListener("click", closeModal); // Remove event listener for closing the modal
      closeButton.addEventListener("click", showBlockedMessage); // Add event listener to show blocked message
    }

    modal.classList.remove("hidden");
  }

  // Show the "Access is blocked" message after closing modal for malicious URL
  function showBlockedMessage() {
    modalHeader.innerHTML = `</p><p style="color: red;">🚨 Access is blocked.</p>`;
    
    // Handle closing the modal after showing blocked message
    closeButton.removeEventListener("click", showBlockedMessage);
    closeButton.addEventListener("click", closeModal); // Add event listener for closing the modal after blocked message
  }

  // Close the modal
  function closeModal() {
    modal.classList.add("hidden");
  }

  // Handle button click
  checkButton.addEventListener("click", () => {
    const url = urlInput.value.trim();
    if (!url) {
      alert("Please enter a URL to check.");
      return;
    }

    // Mock URL check logic
    isSafe = Math.random() > 0.5;
    showModal();
  });

  // Handle close button for closing the modal initially
  closeButton.addEventListener("click", closeModal);
});
