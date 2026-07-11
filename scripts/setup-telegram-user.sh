#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

VENV_DIR="$ROOT_DIR/.venv-telegram-user"
if [[ ! -d "$VENV_DIR" ]]; then
  python3 -m venv "$VENV_DIR"
fi
# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"
pip install -q -r "$ROOT_DIR/services/telegram-user-client/requirements.txt"

if [[ -z "${TELEGRAM_API_ID:-}" || -z "${TELEGRAM_API_HASH:-}" ]]; then
  cat <<'EOF'
Нужны TELEGRAM_API_ID и TELEGRAM_API_HASH.

1. Откройте https://my.telegram.org
2. API development tools
3. Создайте приложение
4. Добавьте в .env:

TELEGRAM_API_ID=12345678
TELEGRAM_API_HASH=your_api_hash

EOF
  exit 1
fi

python3 - <<'PY'
import asyncio
import os

from telethon import TelegramClient
from telethon.sessions import StringSession


async def main() -> None:
    api_id = int(os.environ["TELEGRAM_API_ID"])
    api_hash = os.environ["TELEGRAM_API_HASH"]
    existing = os.environ.get("TELEGRAM_USER_SESSION", "").strip()

    client = TelegramClient(StringSession(existing), api_id, api_hash)
    await client.connect()

    if not await client.is_user_authorized():
        phone = input("Telegram phone (+7999...): ").strip()
        await client.send_code_request(phone)
        code = input("Code from Telegram: ").strip()
        try:
            await client.sign_in(phone=phone, code=code)
        except Exception as exc:
            if "Two-steps verification" in str(exc) or "password" in str(exc).lower():
                password = input("2FA password: ")
                await client.sign_in(password=password)
            else:
                raise

    session = client.session.save()
    print("\nAdd to .env:\n")
    print(f"TELEGRAM_USER_SESSION={session}")
    await client.disconnect()


asyncio.run(main())
PY

echo
echo "Session generated. Start user client:"
echo "  source .venv-telegram-user/bin/activate"
echo "  uvicorn app.main:app --host 0.0.0.0 --port 8090 --app-dir services/telegram-user-client"
