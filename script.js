const menuItems = [
  {
    id: 1,
    name: "Doodh Patti Chai",
    category: "Drinks",
    price: 80,
    icon: "☕",
  },
  {
    id: 2,
    name: "Karak Chai",
    category: "Drinks",
    price: 100,
    icon: "☕",
  },
  {
    id: 3,
    name: "Qehwa",
    category: "Drinks",
    price: 90,
    icon: "🍵",
  },
  {
    id: 4,
    name: "Lassi",
    category: "Drinks",
    price: 140,
    icon: "🥛",
  },
  {
    id: 5,
    name: "Plain Paratha",
    category: "Breakfast",
    price: 120,
    icon: "🫓",
  },
  {
    id: 6,
    name: "Aloo Paratha",
    category: "Breakfast",
    price: 180,
    icon: "🥔",
  },
  {
    id: 7,
    name: "Anda Paratha",
    category: "Breakfast",
    price: 220,
    icon: "🍳",
  },
  {
    id: 8,
    name: "Halwa Poori",
    category: "Breakfast",
    price: 260,
    icon: "🍽️",
  }
];



function normalizeTable(table) {
  const status = (table.status || "").toLowerCase();

  return {
    ...table,
    status: status === "occupied" ? "occupied" : "available"
  };
}

// apply fix immediately

function createMenuItemImage(item) {
  const categoryBackground = item.category === "Drinks" ? "#7C5B2E" : "#D79A42";
  const label = item.icon || item.name.charAt(0);
  const title = item.name.split(" ")[0];
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="140" height="140">
      <rect width="100%" height="100%" rx="28" fill="${categoryBackground}" />
      <text x="50%" y="48%" dominant-baseline="middle" text-anchor="middle" font-size="52" fill="#fff">${label}</text>
      <text x="50%" y="88%" dominant-baseline="middle" text-anchor="middle" font-size="14" fill="#fff" font-weight="700">${title}</text>
    </svg>`;
  return "data:image/svg+xml;charset=UTF-8," + encodeURIComponent(svg);
}

function normalizeMenuItem(item) {
  const normalized = Object.assign({}, item);
  if (!normalized.icon) {
    normalized.icon = (normalized.name || "").charAt(0) || "☕";
  }
  if (!normalized.image) {
    normalized.image = createMenuItemImage(normalized);
  }
  return normalized;
}

menuItems.forEach(function (item, index) {
  menuItems[index] = normalizeMenuItem(item);
});

let state = {
  page: localStorage.getItem("qh_page") || "home",
  filter: "All",
  cart: JSON.parse(localStorage.getItem("qh_cart") || "[]"),
  selectedTable: JSON.parse(localStorage.getItem("qh_table") || "null"),
  customer: JSON.parse(localStorage.getItem("qh_customer") || "null"),
  menu: menuItems.slice(),
  tables: []   // ✅ ADD THIS
};

const api = {
  getMenu: async function () {
    try {
      const res = await fetch("http://localhost:8080/menu");
      if (!res.ok) throw new Error("Network response was not ok");
      const data = await res.json();
      if (Array.isArray(data)) {
        return data.map(normalizeMenuItem);
      }
      return menuItems.slice();
    } catch (error) {
      console.warn("Menu API unavailable, using fallback menu:", error);
      return menuItems.slice();
    }
  },

getTables: async function () {
  try {
    const res = await fetch("http://localhost:8080/tables");
    if (!res.ok) throw new Error("Network error");

    const data = await res.json();

    if (Array.isArray(data)) {
      return data.map(function (table) {
        return normalizeTable(table); // ✅ FIX HERE
      });
    }

    return [];
  } catch (error) {
    console.warn("Tables API unavailable:", error);
    return [];
  }
},

  postOrder: async function (order) {
    try {
      const res = await fetch("http://localhost:8080/order", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(order)
      });

      const text = await res.text();
      let data = null;
      if (text) {
        try {
          data = JSON.parse(text);
        } catch (parseError) {
          data = { message: text };
        }
      }

      if (!res.ok) {
        const message = (data && data.message) || text || res.statusText || "Order submission failed";
        throw new Error(message);
      }

      return data || { success: true };
    } catch (error) {
      console.error("Order submission error:", error);
      return { error: true, message: error.message || "Unable to submit order", offline: true };
    }
  }
};

const app = document.getElementById("app");

function saveState() {
  localStorage.setItem("qh_cart", JSON.stringify(state.cart));
  localStorage.setItem("qh_table", JSON.stringify(state.selectedTable));
  localStorage.setItem("qh_customer", JSON.stringify(state.customer));
  localStorage.setItem("qh_page", state.page);
}

function navigate(page) {
  state.page = page;
  saveState();
  render();
}

function addToCart(itemId) {
  const menuSource = state.menu && state.menu.length ? state.menu : menuItems;
  const item = menuSource.find(function (entry) {
    return entry.id === itemId;
  });
  if (!item) return;
  const existing = state.cart.find(function (entry) {
    return entry.id === itemId;
  });
  if (existing) {
    existing.qty += 1;
  } else {
    state.cart.push({ id: item.id, name: item.name, category: item.category, price: item.price, icon: item.icon, qty: 1 });
  }
  saveState();
  render();
}

function updateQty(itemId, change) {
  const item = state.cart.find(function (entry) {
    return entry.id === itemId;
  });
  if (!item) return;
  item.qty += change;
  if (item.qty <= 0) {
    state.cart = state.cart.filter(function (entry) {
      return entry.id !== itemId;
    });
  }
  saveState();
  render();
}

function removeItem(itemId) {
  state.cart = state.cart.filter(function (entry) {
    return entry.id !== itemId;
  });
  saveState();
  render();
}

function cartTotal() {
  return state.cart.reduce(function (sum, item) {
    return sum + item.price * item.qty;
  }, 0);
}

function cartCount() {
  return state.cart.reduce(function (sum, item) {
    return sum + item.qty;
  }, 0);
}

const currencyFormatter = new Intl.NumberFormat("en-PK", {
  style: "currency",
  currency: "PKR",
  currencyDisplay: "narrowSymbol",
  maximumFractionDigits: 0
});

function money(value) {
  return currencyFormatter.format(value);
}

function buildReceiptData() {
  return {
    items: state.cart.map(function (item) {
      return {
        id: item.id,
        name: item.name,
        category: item.category,
        price: item.price,
        qty: item.qty
      };
    }),
    total: cartTotal(),
    table: state.selectedTable,
    customer: state.customer,
    date: new Date().toLocaleString()
  };
}

function nav() {
  if (state.page === "home" || state.page === "receipt") return "";
  return '<nav class="top-nav">' +
    '<button class="brand" data-page="home"><span>☕</span> Quetta Hotel</button>' +
    '<div class="nav-actions">' +
    '<button data-page="menu" class="' + (state.page === "menu" ? "active" : "") + '">Menu</button>' +
    '<button data-page="cart" class="' + (state.page === "cart" ? "active" : "") + '">Cart <b>' + cartCount() + '</b></button>' +
    '<button data-page="tables" class="' + (state.page === "tables" ? "active" : "") + '">Tables</button>' +
    '</div></nav>';
}

function homePage() {
  return '<main class="page home-page">' +
    '<div class="steam-wrap"><span></span><span></span><span></span></div>' +
    '<section class="hero-card">' +
    '<p class="eyebrow">Quetta Hotel Management System</p>' +
    '<h1 class="culture pashto">پخیر راغلې</h1>' +
    '<h2 class="culture urdu">چائے ہو جائے؟ ☕</h2>' +
    '<p class="tagline">Taste of Tradition</p>' +
    '<p class="hero-copy">Warm chai, crisp paratha, table service, and clean digital ordering inspired by the streetside hotels of Pakistan.</p>' +
    '<button class="primary-btn glow" data-page="menu">Start Order</button>' +
    '</section></main>';
}

function menuPage() {
  const categories = ["All", "Breakfast", "Drinks"];
  const menuSource = state.menu && state.menu.length ? state.menu : menuItems;
  const visibleItems = state.filter === "All" ? menuSource : menuSource.filter(function (item) {
    return item.category === state.filter;
  });
  let cards = "";
  visibleItems.forEach(function (item) {
    cards += '<article class="menu-card">' +
      '<div class="menu-icon">' +
        (item.image ? '<img class="menu-thumb" src="' + item.image + '" alt="' + item.name + '">' : item.icon) +
      '</div>' +
      '<div><p class="category-pill">' + item.category + '</p><h3>' + item.name + '</h3><p class="price">' + money(item.price) + '</p></div>' +
      '<button class="add-btn" data-add="' + item.id + '">Add</button>' +
      '</article>';
  });
  let filterButtons = "";
  categories.forEach(function (category) {
    filterButtons += '<button class="filter-btn ' + (state.filter === category ? "selected" : "") + '" data-filter="' + category + '">' + category + '</button>';
  });
  return '<main class="page scroll-page menu-page">' +
    '<header class="section-header"><p class="eyebrow">Fresh from the tandoor</p><h1>Menu</h1><p>Select chai, paratha, and breakfast favorites for your order.</p></header>' +
    '<div class="filters">' + filterButtons + '</div>' +
    '<section class="menu-grid">' + cards + '</section>' +
    '<section class="menu-proceed-panel"><div><strong>' + cartCount() + ' item' + (cartCount() === 1 ? '' : 's') + ' selected</strong><span>' + (cartCount() > 0 ? money(cartTotal()) + ' ready in your cart' : 'Add products to connect menu with the order process') + '</span></div><button class="primary-btn" data-page="cart" ' + (cartCount() === 0 ? 'disabled' : '') + '>Proceed Order</button></section>' +
    '</main>';
}

function cartPage() {
  if (state.cart.length === 0) {
    return '<main class="page scroll-page cart-page"><header class="section-header"><p class="eyebrow">Your order</p><h1>Cart</h1><p>Increase, decrease, remove items, and see the total update live.</p></header><section class="empty-box"><div>☕</div><h2>Your cart is empty</h2><p>Add a hot chai or paratha to begin.</p><button class="primary-btn" data-page="menu">Open Menu</button></section></main>';
  }
  let rows = "";
  state.cart.forEach(function (item) {
    rows += '<div class="cart-row"><span><b>' + item.name + '</b><small>' + item.category + '</small></span><span class="qty-controls"><button data-dec="' + item.id + '">−</button><b>' + item.qty + '</b><button data-inc="' + item.id + '">+</button></span><span>' + money(item.price) + '</span><span>' + money(item.price * item.qty) + '</span><button class="remove-btn" data-remove="' + item.id + '">×</button></div>';
  });
  return '<main class="page scroll-page cart-page"><header class="section-header"><p class="eyebrow">Your order</p><h1>Cart</h1><p>Increase, decrease, remove items, and see the total update live.</p></header><section class="cart-panel"><div class="cart-table"><div class="cart-row cart-head"><span>Item</span><span>Qty</span><span>Price</span><span>Total</span><span></span></div>' + rows + '</div><div class="total-bar"><span>Live Total</span><strong>' + money(cartTotal()) + '</strong></div><button class="primary-btn wide" data-page="tables">Choose Table</button></section></main>';
}

function tablesPage() {
  let tableCards = "";

  const tableSource = state.tables.length ? state.tables : [];

  tableSource.forEach(function (table) {
    const chosen = state.selectedTable && state.selectedTable.id === table.id;
    tableCards += '<button class="table-card ' + table.status + ' ' + (chosen ? 'chosen' : '') + '" data-table="' + table.id + '" ' + (table.status === 'occupied' ? 'disabled' : '') + '><span class="status-dot"></span><h3>' + table.name + '</h3><p>' + (table.status === 'available' ? 'Available' : 'Occupied') + '</p></button>';
  });
  return '<main class="page scroll-page tables-page"><header class="section-header"><p class="eyebrow">Seating</p><h1>Table Selection</h1><p>Choose an available table before confirming customer details.</p></header><section class="table-grid">' + tableCards + '</section><div class="next-panel"><p>' + (state.selectedTable ? state.selectedTable.name + ' selected' : 'Select a green table to continue') + '</p><button class="primary-btn" data-page="customer" ' + (!state.selectedTable ? 'disabled' : '') + '>Customer Details</button></div></main>';
}

function customerPage() {
  return '<main class="page scroll-page customer-page"><section class="form-card"><p class="eyebrow">Almost ready</p><h1>Customer Input</h1><p>Enter basic customer details for the receipt.</p><form id="customerForm"><label>Name<input name="name" type="text" placeholder="Customer name" value="' + (state.customer ? state.customer.name : '') + '" required></label><label>Phone number<input name="phone" type="tel" inputmode="tel" pattern="^03\\d{2}\\s?\\d{7}$" placeholder="03XX XXXXXXX" value="' + (state.customer ? state.customer.phone : '') + '" required></label><button class="primary-btn wide" type="submit">Generate Receipt</button></form></section></main>';
}

function receiptPage() {
  const receipt = buildReceiptData();
  let rows = "";
  receipt.items.forEach(function (item) {
    rows += '<div class="receipt-row"><span>' + item.name + '</span><span>' + item.qty + '</span><span>' + money(item.price) + '</span><span>' + money(item.price * item.qty) + '</span></div>';
  });
  return '<main class="page receipt-page"><section class="receipt-box"><div class="receipt-title"><span>☕</span><h1>QUETTA HOTEL</h1><p>Taste of Tradition</p></div><div class="receipt-meta"><span>Date: ' + receipt.date + '</span><span>Table: ' + (receipt.table ? receipt.table.name : 'Not selected') + '</span><span>Customer: ' + (receipt.customer ? receipt.customer.name : 'Guest') + '</span></div><div class="receipt-lines"><div class="receipt-row head"><span>Item</span><span>Qty</span><span>Price</span><span>Total</span></div>' + rows + '</div><div class="dash"></div><div class="receipt-total"><span>TOTAL:</span><strong>' + money(receipt.total) + '</strong></div><p class="thanks">Thank you! Visit again</p><div class="receipt-actions"><button class="ghost-btn" data-page="menu">New Items</button><button class="primary-btn" id="newOrder">New Order</button></div></section></main>';
}

function render() {
  document.body.classList.toggle("no-scroll", state.page === "home" || state.page === "receipt");
  const pages = {
    home: homePage,
    menu: menuPage,
    cart: cartPage,
    tables: tablesPage,
    customer: customerPage,
    receipt: receiptPage
  };
  app.innerHTML = nav() + (pages[state.page] ? pages[state.page]() : homePage());
  bindEvents();
}

function bindEvents() {
  document.querySelectorAll("[data-page]").forEach(function (button) {
    button.addEventListener("click", function () {
      navigate(button.dataset.page);
    });
  });
  document.querySelectorAll("[data-filter]").forEach(function (button) {
    button.addEventListener("click", function () {
      state.filter = button.dataset.filter;
      render();
    });
  });
  document.querySelectorAll("[data-add]").forEach(function (button) {
    button.addEventListener("click", function () {
      addToCart(Number(button.dataset.add));
    });
  });
  document.querySelectorAll("[data-inc]").forEach(function (button) {
    button.addEventListener("click", function () {
      updateQty(Number(button.dataset.inc), 1);
    });
  });
  document.querySelectorAll("[data-dec]").forEach(function (button) {
    button.addEventListener("click", function () {
      updateQty(Number(button.dataset.dec), -1);
    });
  });
  document.querySelectorAll("[data-remove]").forEach(function (button) {
    button.addEventListener("click", function () {
      removeItem(Number(button.dataset.remove));
    });
  });
  document.querySelectorAll("[data-table]").forEach(function (button) {
    button.addEventListener("click", function () {
     const table = state.tables.find(function (entry) {
        return entry.id === Number(button.dataset.table);
      });
      if (table.status === "available") state.selectedTable = table;
      saveState();
      render();
    });
  });
  const form = document.getElementById("customerForm");
  if (form) {
    form.addEventListener("submit", async function (event) {
      event.preventDefault();
      const data = new FormData(form);
      const name = String(data.get("name") || "").trim();
      const phone = String(data.get("phone") || "").trim();

      if (!name || !phone) {
        alert("Please enter both name and phone number.");
        return;
      }

      state.customer = { name, phone };

      if (!state.cart.length) {
        alert("Your cart is empty. Add something before generating a receipt.");
        navigate("menu");
        return;
      }

      const result = await api.postOrder(buildReceiptData());
      if (result.error) {
        const retry = confirm("Unable to submit order to the server. Continue with receipt anyway?\n\nError: " + result.message);
        if (!retry) return;
      }

      navigate("receipt");
    });
  }
  const newOrder = document.getElementById("newOrder");
  if (newOrder) {
    newOrder.addEventListener("click", function () {
      state.cart = [];
      state.selectedTable = null;
      state.customer = null;
      state.filter = "All";
      navigate("home");
    });
  }
}

Promise.all([
  api.getMenu(),
  api.getTables()
]).then(function ([menuData, tableData]) {
  state.menu = menuData;
  state.tables = tableData;
  render();
});