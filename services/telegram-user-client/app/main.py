from __future__ import annotations

import os
from contextlib import asynccontextmanager
from datetime import datetime, timezone

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from telethon import TelegramClient
from telethon.sessions import StringSession


def _require_env(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


def _optional_env(name: str, default: str = "") -> str:
    return os.getenv(name, default).strip()


API_ID = int(_optional_env("TELEGRAM_API_ID", "0") or "0")
API_HASH = _optional_env("TELEGRAM_API_HASH")
SESSION_STRING = _optional_env("TELEGRAM_USER_SESSION")

client: TelegramClient | None = None


@asynccontextmanager
async def lifespan(_: FastAPI):
    global client
    if not API_ID or not API_HASH:
        yield
        return
    client = TelegramClient(StringSession(SESSION_STRING), API_ID, API_HASH)
    await client.connect()
    if not await client.is_user_authorized():
        await client.disconnect()
        client = None
        yield
        return
    yield
    if client is not None:
        await client.disconnect()
        client = None


app = FastAPI(title="Geek Parser Telegram User Client", lifespan=lifespan)


class ScheduleRequest(BaseModel):
    chat_id: str = Field(min_length=1)
    text: str = Field(min_length=1, max_length=4096)
    scheduled_at: datetime


class ScheduleResponse(BaseModel):
    scheduled_message_id: int
    scheduled_at: datetime
    chat_id: str


class HealthResponse(BaseModel):
    status: str
    authorized: bool
    configured: bool


def _ensure_ready() -> TelegramClient:
    if client is None:
        if not API_ID or not API_HASH:
            raise HTTPException(
                status_code=503,
                detail="Telegram user client is not configured (TELEGRAM_API_ID/TELEGRAM_API_HASH).",
            )
        raise HTTPException(
            status_code=503,
            detail="Telegram user session is not authorized. Run scripts/setup-telegram-user.sh.",
        )
    return client


def _parse_chat_id(chat_id: str) -> int | str:
    trimmed = chat_id.strip()
    if trimmed.lstrip("-").isdigit():
        return int(trimmed)
    return trimmed


def _normalize_schedule_time(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value.astimezone(timezone.utc)


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    configured = bool(API_ID and API_HASH)
    authorized = client is not None and await client.is_user_authorized()
    status = "UP" if authorized else ("CONFIGURED" if configured else "NOT_CONFIGURED")
    return HealthResponse(status=status, authorized=authorized, configured=configured)


@app.post("/api/schedule", response_model=ScheduleResponse)
async def schedule_message(request: ScheduleRequest) -> ScheduleResponse:
    tg = _ensure_ready()
    scheduled_at = _normalize_schedule_time(request.scheduled_at)
    now = datetime.now(timezone.utc)
    if scheduled_at <= now:
        raise HTTPException(status_code=400, detail="scheduled_at must be in the future")

    entity = await tg.get_entity(_parse_chat_id(request.chat_id))
    message = await tg.send_message(entity, request.text, schedule=scheduled_at)
    return ScheduleResponse(
        scheduled_message_id=message.id,
        scheduled_at=scheduled_at,
        chat_id=request.chat_id,
    )


@app.post("/api/test-schedule", response_model=ScheduleResponse)
async def test_schedule() -> ScheduleResponse:
    chat_id = _require_env("TELEGRAM_TEST_CHAT_ID") if os.getenv("TELEGRAM_TEST_CHAT_ID") else _optional_env("APP_TELEGRAM_CHAT_ID")
    if not chat_id:
        raise HTTPException(status_code=400, detail="Set TELEGRAM_TEST_CHAT_ID or APP_TELEGRAM_CHAT_ID for test")
    from datetime import timedelta

    scheduled_at = datetime.now(timezone.utc) + timedelta(minutes=2)
    request = ScheduleRequest(
        chat_id=chat_id,
        text="Geek Parser: тест нативной отложенной отправки. Откройте «Отложенная отправка» в канале.",
        scheduled_at=scheduled_at,
    )
    return await schedule_message(request)
