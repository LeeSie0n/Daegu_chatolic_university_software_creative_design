// <Array of URLs and corresponding QR images>
const urls = [
    { src: 'Nor_URL.png', url: 'https://www.instagram.com/p/DDCS30lybA2IBHrYefgjgCauMlaEknc8mcoYKw0/' },
    { src: 'Mal_URL.png', url: 'https://www.instagram.com/p/DDCS89eyD32qzpM4LjpwclgDhj8_FxUIYn0SOc0/' }
  ];
  
  // Duplicate URLs to ensure 4 total
  const allUrls = [...urls, ...urls];
  
  // <Shuffle function to randomize the QR codes>
  function shuffle(array) {
    for (let i = array.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [array[i], array[j]] = [array[j], array[i]];
    }
  }
  
  // <Initialize grid on page load>
  document.addEventListener("DOMContentLoaded", function () {
    shuffle(allUrls);
    
    // <QR Grid Element>
    const qrGrid = document.getElementById("qr-grid");
    
    // Populate grid with QR cards
    qrGrid.innerHTML = `
      <div class="grid">
        ${allUrls.map((url, index) => `
          <!-- <QR Code Card> -->
          <div class="card">
            <div class="quote">"</div>
            <p class="note-title">QR Code #${index + 1}</p>
            <div class="qr-container">
              <!-- <QR Code Image> -->
              <img src="${url.src}" alt="QR Code ${index + 1}" class="qr-image" onclick="window.location.href='${url.url}'">
            </div>
            <div class="quote reverse">"</div>
          </div>
        `).join("")}
      </div>
    `;
  });
  