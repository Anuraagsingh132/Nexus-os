const { Database } = require('@hocuspocus/extension-database');
const Y = require('yjs');

let API_URL = process.env.INTERNAL_API_URL || 'http://api:8080';
if (!API_URL.startsWith('http://') && !API_URL.startsWith('https://')) {
  API_URL = `https://${API_URL}`;
}
const INTERNAL_API_SECRET = process.env.INTERNAL_API_SECRET || 'dev-internal-secret-change-me';

const internalHeaders = (extra = {}) => ({
  'X-Internal-Secret': INTERNAL_API_SECRET,
  ...extra,
});

// Debounce map for plain-text extraction and backend PATCH writes per document
// Prevents HTTP PATCH & DB write event storms on every keystroke
const patchDebounceTimers = new Map();
const pendingPatches = new Map();
const DEBOUNCE_MS = 3000;

function extractPlainText(ydoc) {
  try {
    const xmlFragment = ydoc.getXmlFragment('prosemirror');
    const fragmentStr = xmlFragment.toString();
    if (fragmentStr) {
      return fragmentStr.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
    }
    const defaultFragment = ydoc.getXmlFragment('default');
    const defaultStr = defaultFragment.toString();
    if (defaultStr) {
      return defaultStr.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
    }
    const textNode = ydoc.getText('default');
    return textNode.toString();
  } catch (e) {
    console.error('Error extracting text from Ydoc:', e);
    return '';
  }
}

async function flushPlainTextPatch(documentName) {
  const plainText = pendingPatches.get(documentName);
  if (plainText === undefined) return;

  if (patchDebounceTimers.has(documentName)) {
    clearTimeout(patchDebounceTimers.get(documentName));
    patchDebounceTimers.delete(documentName);
  }

  try {
    const patchRes = await fetch(`${API_URL}/api/v1/internal/documents/${documentName}/content`, {
      method: 'PATCH',
      headers: {
        ...internalHeaders(),
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ content: plainText }),
    });
    if (!patchRes.ok) {
      console.error(`Failed to patch document content ${documentName}: ${patchRes.status} ${patchRes.statusText}`);
    } else {
      pendingPatches.delete(documentName);
    }
  } catch (error) {
    console.error(`Error patching plain text for document ${documentName}:`, error);
  }
}

function scheduleDebouncedPatch(documentName, plainText) {
  pendingPatches.set(documentName, plainText);

  if (!patchDebounceTimers.has(documentName)) {
    const timer = setTimeout(() => {
      flushPlainTextPatch(documentName);
    }, DEBOUNCE_MS);
    patchDebounceTimers.set(documentName, timer);
  }
}

module.exports = {
  port: process.env.PORT ? parseInt(process.env.PORT, 10) : 1234,
  onAuthenticate: async ({ token, documentName }) => {
    if (!token || !documentName) {
      throw new Error('Missing websocket token');
    }

    const response = await fetch(`${API_URL}/api/v1/internal/ws/validate-ticket`, {
      method: 'POST',
      headers: internalHeaders({
        'Content-Type': 'application/json',
      }),
      body: JSON.stringify({ token, documentName }),
    });

    if (!response.ok) {
      throw new Error(`Websocket authentication failed: ${response.status}`);
    }

    return { user: { id: 'authenticated' } };
  },
  onDisconnect: async ({ documentName }) => {
    await flushPlainTextPatch(documentName);
  },
  onDestroy: async ({ documentName }) => {
    await flushPlainTextPatch(documentName);
  },
  extensions: [
    new Database({
      fetch: async ({ documentName }) => {
        const response = await fetch(`${API_URL}/api/v1/internal/documents/${documentName}/yjs`, {
          headers: internalHeaders(),
        });
        if (response.status === 204) {
          return null;
        }
        if (!response.ok) {
          throw new Error(`Failed to fetch document ${documentName}: ${response.statusText}`);
        }
        const arrayBuffer = await response.arrayBuffer();
        return new Uint8Array(arrayBuffer);
      },
      store: async ({ documentName, state }) => {
        // Direct Yjs binary update to DB
        const response = await fetch(`${API_URL}/api/v1/internal/documents/${documentName}/yjs`, {
          method: 'PUT',
          headers: {
            ...internalHeaders(),
            'Content-Type': 'application/octet-stream',
          },
          body: state,
        });
        if (!response.ok) {
          throw new Error(`Failed to store document ${documentName}: ${response.statusText}`);
        }

        // Extract plain text and schedule debounced PATCH
        const ydoc = new Y.Doc();
        try {
          Y.applyUpdate(ydoc, state);
          const plainText = extractPlainText(ydoc);
          scheduleDebouncedPatch(documentName, plainText);
        } catch (error) {
          console.error(`Error parsing Yjs state for ${documentName}:`, error);
        } finally {
          ydoc.destroy();
        }
      },
    }),
  ],
};
