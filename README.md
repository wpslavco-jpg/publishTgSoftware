# Geek Parser

Система сбора статей, AI-адаптации и отложенной публикации в Telegram.

## Стек

- **Backend:** Java 21, Spring Boot 3, PostgreSQL, Flyway
- **Admin UI:** React + Vite
- **Infra:** Docker Compose

## Быстрый старт

### 1. Поднять PostgreSQL

```bash
docker compose up -d postgres
```

### 2. Настроить переменные окружения

```bash
cp .env.example .env
# Укажите APP_LLM_API_KEY и параметры Telegram:
# APP_TELEGRAM_BOT_TOKEN, APP_TELEGRAM_CHAT_ID
```

### 3. Запустить backend

```bash
cd backend
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export APP_LLM_API_KEY="your-key"
./mvnw spring-boot:run
```

Backend: http://localhost:8080

### 4. Запустить админку

```bash
cd frontend/admin
npm install
npm run dev
```

Админка: http://localhost:5173

### 5. Подключить Telegram-бота

Укажите в `.env`:

```bash
APP_TELEGRAM_BOT_TOKEN=your-bot-token
APP_TELEGRAM_CHAT_ID=-1001234567890
APP_TELEGRAM_BOT_USERNAME=your_bot   # опционально
```

После изменения `.env` перезапустите backend.

Альтернатива — мастер настройки:

```bash
./scripts/setup-telegram.sh
```

### 6. Нативная «Отложенная отправка» Telegram (редактирование в клиенте)

Bot API не поддерживает очередь отложенных постов. Для предредактирования (премиум-эмодзи и т.п.) используется **user client** (Telethon).

1. Получите `api_id` и `api_hash` на https://my.telegram.org
2. Добавьте в `.env`:

```bash
TELEGRAM_API_ID=12345678
TELEGRAM_API_HASH=your_api_hash
```

3. Создайте сессию администратора канала:

```bash
pip install -r services/telegram-user-client/requirements.txt
chmod +x scripts/setup-telegram-user.sh
./scripts/setup-telegram-user.sh
# Скопируйте TELEGRAM_USER_SESSION в .env
```

4. Запустите user client:

```bash
cd services/telegram-user-client
uvicorn app.main:app --host 0.0.0.0 --port 8090
```

5. Перезапустите backend. Кнопка **«Запланировать»** в админке положит пост в «Отложенную отправку» канала в Telegram.

## Полный запуск через Docker

```bash
export APP_LLM_API_KEY="your-key"
docker compose up --build
```

- Backend: http://localhost:8080
- Admin: http://localhost:4173
- Telegram user client: http://localhost:8090
- PostgreSQL: localhost:5432

## Основные API

| Метод | Endpoint | Описание |
|-------|----------|----------|
| POST | `/api/admin/sources/sync` | Запуск парсинга за 48 часов |
| GET | `/api/admin/articles` | Список статей |
| GET | `/api/admin/articles/{id}` | Детали статьи |
| PUT | `/api/admin/articles/{id}` | Редактирование |
| POST | `/api/admin/articles/{id}/schedule` | Отложенная публикация |
| POST | `/api/admin/articles/{id}/publish-now` | Публикация сейчас |
| POST | `/api/admin/telegram/validate` | Валидация бота |
| POST | `/api/admin/telegram/test-message` | Тестовый пинг |

## Источники

- rozetked.me
- wylsa.com/category/news
- ign.com/news
- macrumors.com
- 3dnews.ru

## Workflow

1. Парсер собирает статьи за последние 48 часов
2. Оригиналы сохраняются в `storage/articles/raw/` как `.md`
3. LLM переводит и сокращает до 10 предложений
4. В админке можно отредактировать текст и запланировать публикацию
5. Scheduler отправляет посты в Telegram в назначенное время
