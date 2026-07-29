const paymentMethod = document.getElementById('paymentMethod');
const cardFields = document.getElementById('cardFields');
const splitCheckbox = document.getElementById('splitPayment');
const splitFields = document.getElementById('splitFields');
const form = document.getElementById('paymentForm');
const statusMessage = document.getElementById('statusMessage');

// Show/hide card fields based on selected payment method
paymentMethod.addEventListener('change', () => {
  cardFields.classList.toggle('hidden', paymentMethod.value !== 'card');
});

// Toggle split payment fields (Alt Flow 2.0: Split payment)
splitCheckbox.addEventListener('change', () => {
  splitFields.classList.toggle('hidden', !splitCheckbox.checked);
});

function showStatus(type, text) {
  statusMessage.classList.remove('hidden', 'success', 'error', 'pending');
  statusMessage.classList.add(type);
  statusMessage.textContent = text;
}

// Basic client-side validation mirroring Exception 1.0: Invalid payment details
function validateCardDetails() {
  const cardNumber = document.getElementById('cardNumber').value.replace(/\s/g, '');
  const expiry = document.getElementById('expiry').value;
  const cvv = document.getElementById('cvv').value;
  const cardHolder = document.getElementById('cardHolder').value.trim();

  if (paymentMethod.value !== 'card') return true;

  if (!/^\d{13,19}$/.test(cardNumber)) return false;
  if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(expiry)) return false;
  if (!/^\d{3,4}$/.test(cvv)) return false;
  if (cardHolder.length === 0) return false;

  return true;
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();

  if (!validateCardDetails()) {
    showStatus('error', 'Invalid payment details. Please check card number, expiry, and CVV.');
    return;
  }

  showStatus('pending', 'Authorizing transaction...');

  const payload = {
    pnr: document.getElementById('pnrValue').textContent,
    method: paymentMethod.value,
    cardNumber: document.getElementById('cardNumber').value,
    expiry: document.getElementById('expiry').value,
    cvv: document.getElementById('cvv').value,
    cardHolder: document.getElementById('cardHolder').value,
    isSplit: splitCheckbox.checked,
    splitAmount: splitCheckbox.checked ? document.getElementById('splitAmount').value : null
  };

  try {
    // Replace with your actual backend endpoint, e.g. /api/payment/process
    const response = await fetch('/api/payment/process', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const result = await response.json();

    if (result.status === 'APPROVED') {
      showStatus('success', `Payment successful. Transaction ref: ${result.transactionRef}`);
    } else if (result.status === 'PENDING') {
      showStatus('pending', 'Payment gateway timed out. We will confirm status shortly.');
    } else if (result.status === 'FRAUD_REVIEW') {
      showStatus('pending', 'Transaction flagged for manual review. Ticket will not be issued until cleared.');
    } else {
      showStatus('error', result.message || 'Payment declined. Please try another method.');
    }
  } catch (err) {
    showStatus('error', 'Unable to reach payment service. Please try again.');
  }
});
