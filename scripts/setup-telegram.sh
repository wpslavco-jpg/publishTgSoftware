#!/usr/bin/env bash

set -euo pipefail

echo "Geek Parser Telegram setup"
echo
read -r -p "Создать нового бота через BotFather? [Y/n]: " CREATE_BOT

if [[ -z "${CREATE_BOT}" || "${CREATE_BOT}" =~ ^[Yy]$ ]]; then
  cat <<'EOF'

1. Откройте Telegram и найдите @BotFather
2. Выполните /newbot
3. Задайте name и username
4. Скопируйте bot token
5. Добавьте бота в вашу группу или канал
6. Выдайте права на публикацию
7. Узнайте chat ID группы и вставьте его ниже

EOF
else
  echo "Будет использован существующий бот."
fi

read -r -p "Введите bot token: " BOT_TOKEN
read -r -p "Введите chat ID: " CHAT_ID
read -r -p "URL backend API [http://localhost:8080]: " API_URL

API_URL="${API_URL:-http://localhost:8080}"

echo
echo "Проверяю токен и chat ID..."
curl -fsS -X POST "${API_URL}/api/admin/telegram/validate" \
  -H 'Content-Type: application/json' \
  -d "{\"botToken\":\"${BOT_TOKEN}\",\"chatId\":\"${CHAT_ID}\",\"active\":true}"

echo
echo "Отправляю тестовый пинг..."
curl -fsS -X POST "${API_URL}/api/admin/telegram/test-message" \
  -H 'Content-Type: application/json' \
  -d "{\"botToken\":\"${BOT_TOKEN}\",\"chatId\":\"${CHAT_ID}\",\"active\":true}"

echo
echo "Telegram bot успешно привязан к Geek Parser."
echo
echo "Рекомендуется также сохранить значения в .env:"
echo "APP_TELEGRAM_BOT_TOKEN=${BOT_TOKEN}"
echo "APP_TELEGRAM_CHAT_ID=${CHAT_ID}"
