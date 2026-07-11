# ADR 0001: Modular Monolith for Content Platform

## Status

Accepted

## Context

The system needs article ingestion, AI processing, editorial workflow, and Telegram publishing. These are tightly coupled in a single editorial pipeline owned by one operator.

## Decision

Build as a modular monolith (`content-platform`) with bounded contexts:
- `ingestion` — source scraping
- `article` — raw/prepared articles, LLM processing
- `publishing` — Telegram config, scheduled jobs
- `admin` — REST API for React UI

## Consequences

- Fast first release with one deployable unit
- Clear package boundaries allow future extraction to microservices
- Single PostgreSQL database with schema migrations via Flyway
