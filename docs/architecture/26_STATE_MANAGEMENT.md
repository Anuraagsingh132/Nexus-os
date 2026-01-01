# State Management

Server state belongs in TanStack Query with consistent keys, cancellation, invalidation, optimistic updates and rollback. URL state owns filters, sorting, selected tabs, dates, pagination cursors where shareable. React Hook Form owns forms; Zod mirrors API constraints for immediate UX only.

Zustand is limited to ephemeral shell state, command palette, local board drag state, editor panels, and draft preferences. Persist only harmless preferences. Realtime events patch or invalidate query caches; reconnect triggers authoritative refetch.

Never duplicate a server entity across Context, Zustand and Query. Never store credentials/tokens in persistent browser state.

