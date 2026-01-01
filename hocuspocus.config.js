const { Database } = require('@hocuspocus/extension-database');
const Y = require('yjs');

const API_URL = process.env.INTERNAL_API_URL || 'http://api:8080';
const INTERNAL_API_SECRET = process.env.INTERNAL_API_SECRET || 'dev-internal-secret-change-me';

const internalHeaders = (extra = {}) => ({
  'X-Internal-Secret': INTERNAL_API_SECRET,
  ...extra,
});

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
        
        try {
          const ydoc = new Y.Doc();
          Y.applyUpdate(ydoc, state);
          const xmlFragment = ydoc.getXmlFragment('default');
          const plainText = xmlFragment.toString();
          
          if (plainText) {
            const patchRes = await fetch(`${API_URL}/api/v1/internal/documents/${documentName}/content`, {
              method: 'PATCH',
              headers: {
                ...internalHeaders(),
                'Content-Type': 'application/json',
              },
              body: JSON.stringify({ content: plainText }),
            });
            if (!patchRes.ok) {
              console.error(`Failed to patch document content ${documentName}: ${patchRes.statusText}`);
            }
          }
        } catch (error) {
          console.error(`Error parsing yjs state or patching content:`, error);
        }
      },
    }),
  ],
};
