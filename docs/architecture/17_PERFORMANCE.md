# Performance

Budgets: useful shell under 2.5 s on typical broadband; p95 cached reads under 250 ms, ordinary API reads under 500 ms, writes under 800 ms excluding async work; realtime delivery under 1 s; no unbounded lists.

Use cursor pagination, query/index analysis, batched permission checks, Redis only where invalidation is understood, CDN/object caching, image optimization, code splitting, virtualization for long boards/lists, debounced search, and asynchronous ingestion/analytics.

Add representative load scripts for login, task list, message feed, WebSocket fan-out, search, and file initiation. Record machine/environment with results. Fix N+1 queries and define cache keys with tenant scope.

