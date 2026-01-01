# ADR 0001: Modular Monolith Architecture

## Status
Accepted

## Context
Nexus OS requires a scalable architecture that allows independent domains to evolve and potentially be extracted later, while maintaining developer velocity and deployment simplicity for the initial versions.

## Decision
We will build Nexus OS as a Modular Monolith using Spring Boot. Bounded contexts (Identity, Workspace, Projects, etc.) will be separated by Java packages. Modules will communicate via explicit APIs and domain events, and will not directly access each other's databases/repositories.

## Consequences
- Easier local development and deployment.
- Clear boundaries facilitate future microservice extraction.
- Requires strict discipline to avoid accidental coupling across module boundaries.
