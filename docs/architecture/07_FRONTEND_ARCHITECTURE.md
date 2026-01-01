# Frontend Architecture

Use Next.js App Router route groups for public auth, onboarding, and authenticated workspace routes. Server Components render shells and read-heavy initial data; Client Components own interactive islands. A typed generated API client consumes OpenAPI.

Feature folders include route UI, components, query keys, mutations, schemas, and tests. Shared primitives contain no business rules. TanStack Query owns remote state; Zustand owns only transient UI such as command palette, pane layout, and optimistic drag state.

Required shell: organization/workspace switcher, project navigation, recent/favorites, global search, create menu, notifications, profile, responsive sidebar, breadcrumbs, and command palette. Use error boundaries, loading skeletons, meaningful empty states, retry actions, toast announcements, and unsaved-change protection.

Never leak access tokens to localStorage. Avoid waterfalls, giant client bundles, duplicated fetch state, and direct calls to storage/AI providers.

