// =====================================================================
// api.js -- compartido por todas las pantallas de Mapuescuela
// =====================================================================
// URL del backend real (mapuescuela-backend). Si corres el backend en
// otro puerto/host, cambia solo esta línea.
const API_BASE_URL = 'http://localhost:8081';

// ---------- Llamadas reales a la API ----------

async function crearPedido(pedido) {
  const res = await fetch(`${API_BASE_URL}/pedidos`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(pedido)
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.mensaje || 'No se pudo crear el pedido.');
  }
  return data;
}

async function obtenerPedido(idPedido) {
  const res = await fetch(`${API_BASE_URL}/pedidos/${encodeURIComponent(idPedido)}`);
  if (!res.ok) return null;
  return res.json();
}

async function obtenerNotificaciones(idPedido) {
  const res = await fetch(`${API_BASE_URL}/notificaciones/pedido/${encodeURIComponent(idPedido)}`);
  if (!res.ok) return [];
  return res.json();
}

async function revisarComprobante(idPedido, decision, observaciones) {
  const res = await fetch(`${API_BASE_URL}/pedidos/${encodeURIComponent(idPedido)}/revision`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ decision, observaciones })
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.mensaje || 'No se completó la revisión en Flowable.');
  return data;
}

// Sube el archivo real del comprobante (multipart/form-data). Sirve
// tanto para la primera subida como para el reenvío tras un error.
async function subirComprobante(idPedido, archivo) {
  const formData = new FormData();
  formData.append('archivo', archivo);

  const res = await fetch(`${API_BASE_URL}/pedidos/${encodeURIComponent(idPedido)}/comprobante`, {
    method: 'POST',
    body: formData
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.mensaje || 'No se pudo subir el comprobante.');
  }
  return data;
}

// URL para mostrar/descargar el comprobante real (usada por pantalla3).
function urlComprobante(idPedido) {
  return `${API_BASE_URL}/pedidos/${encodeURIComponent(idPedido)}/comprobante`;
}

// ---------- Carrito (una sola línea, porque el backend maneja un
// producto + una cantidad por pedido, no una lista de ítems) ----------

const CARRITO_KEY = 'mapuescuela_carrito';
const PEDIDO_ACTUAL_KEY = 'mapuescuela_pedido_actual';

function getCarrito() {
  const raw = sessionStorage.getItem(CARRITO_KEY);
  return raw ? JSON.parse(raw) : null;
}

function setCarrito(item) {
  sessionStorage.setItem(CARRITO_KEY, JSON.stringify(item));
}

function limpiarCarrito() {
  sessionStorage.removeItem(CARRITO_KEY);
}

function setPedidoActual(idPedido) {
  sessionStorage.setItem(PEDIDO_ACTUAL_KEY, idPedido);
}

function getPedidoActual() {
  return sessionStorage.getItem(PEDIDO_ACTUAL_KEY);
}

// Precios referenciales, compartidos entre catálogo, carrito y resumen.
const PRECIOS = {
  'Cuaderno Mapuche Artesanal': 4500,
  'Juego Didáctico Mapudungun': 8990,
  'Set de Lápices Ilustrados': 3200,
  'Mochila Étnica Infantil': 12500
};

Object.assign(PRECIOS, JSON.parse(sessionStorage.getItem('mapuescuela_precios') || '{}'));

async function listarProductos() {
  const res = await fetch(`${API_BASE_URL}/inventario/productos`);
  if (!res.ok) throw new Error('No se pudo cargar el catálogo.');
  const productos = await res.json();
  for (const p of productos) if (p.precio > 0) PRECIOS[p.producto] = p.precio;
  sessionStorage.setItem('mapuescuela_precios', JSON.stringify(PRECIOS));
  return productos;
}

function formatearCLP(valor) {
  return `$${valor.toLocaleString('es-CL')}`;
}

// Navegación común para volver al inicio desde cualquier pantalla.
document.addEventListener('DOMContentLoaded', () => {
  const nav = document.querySelector('nav.navbar');
  if (!nav || location.pathname.endsWith('/index.html')) return;
  // Las pantallas de voluntariado ya incluyen Inicio en su propia barra.
  if (nav.querySelector('a[href="index.html"]')) return;
  const home = document.createElement('a');
  home.href = 'index.html';
  home.textContent = 'Inicio';
  home.setAttribute('aria-label', 'Volver al inicio de Mapuescuela');
  home.style.cssText = 'position:absolute;right:18px;top:12px;padding:7px 12px;border-radius:8px;background:#fff;color:#174b65;text-decoration:none;font-weight:600;box-shadow:0 1px 5px #0002;z-index:10';
  nav.appendChild(home);
});
