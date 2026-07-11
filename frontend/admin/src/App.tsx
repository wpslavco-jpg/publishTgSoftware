import { useEffect, useMemo, useState } from 'react'
import { TelegramEditor } from './TelegramEditor'
import { SourceAdminPanel } from './SourceAdminPanel'
import { countSentences } from './telegramFormat'
import './App.css'

type ArticleStatus =
  | 'DISCOVERED'
  | 'RAW_SAVED'
  | 'AI_PROCESSING'
  | 'READY_FOR_REVIEW'
  | 'READY_TO_PUBLISH'
  | 'SCHEDULED'
  | 'PUBLISHED'
  | 'FAILED'

type ArticleSummary = {
  preparedArticleId: number
  rawArticleId: number
  sourceCode: string
  title: string
  canonicalUrl: string
  summaryBody: string
  status: ArticleStatus
  needsManualReview: boolean
  publishedAt: string
  updatedAt: string
}

type ArticleDetails = ArticleSummary & {
  markdownPath: string
  rawExcerpt: string
  translatedBody: string
  editorialNotes: string
}

type Publication = {
  id: number
  preparedArticleId: number
  title: string
  scheduledAt: string
  status: 'SCHEDULED' | 'REGISTERED_IN_TELEGRAM' | 'PUBLISHING' | 'PUBLISHED' | 'FAILED'
  attemptCount: number
  telegramMessageId?: string
  lastError?: string
}

type TelegramConfig = {
  id: number
  active: boolean
  chatId: string
  botUsername: string
  validated: boolean
  lastValidatedAt?: string
  configuredViaEnv?: boolean
  draftChatConfigured?: boolean
  draftChatIdMasked?: string
}

type DiscoveredChat = {
  chatId: string
  username?: string
  firstName?: string
  lastName?: string
  lastMessageText?: string
}

const emptyTelegram = { botToken: '', chatId: '', active: true }

function App() {
  const [articles, setArticles] = useState<ArticleSummary[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [selectedArticle, setSelectedArticle] = useState<ArticleDetails | null>(null)
  const [publications, setPublications] = useState<Publication[]>([])
  const [telegramConfig, setTelegramConfig] = useState<TelegramConfig | null>(null)
  const [telegramForm, setTelegramForm] = useState(emptyTelegram)
  const [scheduleAt, setScheduleAt] = useState('')
  const [discoveredChats, setDiscoveredChats] = useState<DiscoveredChat[]>([])
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('Готово к первой синхронизации.')

  const selectedIndex = useMemo(
    () => articles.findIndex((article) => article.preparedArticleId === selectedId),
    [articles, selectedId],
  )

  useEffect(() => {
    void refreshAll()
  }, [])

  useEffect(() => {
    if (selectedId == null) {
      return
    }

    void loadArticle(selectedId)
  }, [selectedId])

  async function request<T>(url: string, init?: RequestInit): Promise<T> {
    const response = await fetch(url, {
      headers: { 'Content-Type': 'application/json' },
      ...init,
    })

    if (!response.ok) {
      const payload = await response.json().catch(() => ({ message: 'Запрос не выполнен' }))
      throw new Error(payload.message ?? 'Запрос не выполнен')
    }

    if (response.status === 204) {
      return undefined as T
    }

    const text = await response.text()
    if (!text.trim()) {
      return undefined as T
    }

    return JSON.parse(text) as T
  }

  async function refreshAll() {
    setLoading(true)
    try {
      const [articleList, publicationList, tgConfig] = await Promise.all([
        request<ArticleSummary[]>('/api/admin/articles'),
        request<Publication[]>('/api/admin/publications'),
        request<TelegramConfig | null>('/api/admin/telegram/config'),
      ])
      setArticles(articleList)
      setPublications(publicationList)
      setTelegramConfig(tgConfig)
      if (articleList.length > 0) {
        setSelectedId((current) => current ?? articleList[0].preparedArticleId)
      }
    } catch (error) {
      setMessage((error as Error).message)
    } finally {
      setLoading(false)
    }
  }

  async function loadArticle(id: number) {
    try {
      const article = await request<ArticleDetails>(`/api/admin/articles/${id}`)
      setSelectedArticle(article)
    } catch (error) {
      setMessage((error as Error).message)
    }
  }

  async function syncSources() {
    setLoading(true)
    try {
      const result = await request<{ discovered: number; stored: number }>('/api/admin/sources/sync', {
        method: 'POST',
      })
      setMessage(`Синхронизация завершена: найдено ${result.discovered}, сохранено ${result.stored}.`)
      await refreshAll()
    } catch (error) {
      setMessage((error as Error).message)
    } finally {
      setLoading(false)
    }
  }

  function confirmEditorLimits(summaryBody: string): boolean {
    const sentences = countSentences(summaryBody)
    if (sentences > 10) {
      return window.confirm(
        `В тексте ${sentences} предложений (рекомендуемый лимит — 10). Всё равно продолжить?`,
      )
    }
    return true
  }

  async function saveArticle() {
    if (!selectedArticle) {
      return
    }
    if (!confirmEditorLimits(selectedArticle.summaryBody)) {
      return
    }
    setLoading(true)
    try {
      const updated = await request<ArticleDetails>(`/api/admin/articles/${selectedArticle.preparedArticleId}`, {
        method: 'PUT',
        body: JSON.stringify({
          title: selectedArticle.title,
          translatedBody: selectedArticle.translatedBody,
          summaryBody: selectedArticle.summaryBody,
          editorialNotes: selectedArticle.editorialNotes,
          readyToPublish: selectedArticle.status === 'READY_TO_PUBLISH',
        }),
      })
      setSelectedArticle(updated)
      setMessage('Статья сохранена.')
      await refreshAll()
    } catch (error) {
      setMessage((error as Error).message)
    } finally {
      setLoading(false)
    }
  }

  async function schedulePublication() {
    if (!selectedArticle || !scheduleAt) {
      setMessage('Выберите статью и дату публикации.')
      return
    }
    if (!confirmEditorLimits(selectedArticle.summaryBody)) {
      return
    }
    setLoading(true)
    try {
      await request(`/api/admin/articles/${selectedArticle.preparedArticleId}/schedule`, {
        method: 'POST',
        body: JSON.stringify({ scheduledAt: new Date(scheduleAt).toISOString() }),
      })
      const localTime = new Date(scheduleAt).toLocaleString('ru-RU')
      setMessage(
        `Публикация запланирована на ${localTime}. Пост уйдёт в канал с форматированием из редактора.`,
      )
      await refreshAll()
    } catch (error) {
      setMessage((error as Error).message)
    } finally {
      setLoading(false)
    }
  }

  async function publishNow() {
    if (!selectedArticle) {
      return
    }
    if (!confirmEditorLimits(selectedArticle.summaryBody)) {
      return
    }
    setLoading(true)
    try {
      await request(`/api/admin/articles/${selectedArticle.preparedArticleId}/publish-now`, {
        method: 'POST',
      })
      setMessage('Публикация поставлена в очередь на немедленную отправку.')
      await refreshAll()
    } catch (error) {
      setMessage((error as Error).message)
    } finally {
      setLoading(false)
    }
  }

  async function sendDraftToTelegram() {
    if (!selectedArticle) {
      return
    }
    if (!confirmEditorLimits(selectedArticle.summaryBody)) {
      return
    }
    setLoading(true)
    try {
      await request<ArticleDetails>(`/api/admin/articles/${selectedArticle.preparedArticleId}`, {
        method: 'PUT',
        body: JSON.stringify({
          title: selectedArticle.title,
          translatedBody: selectedArticle.translatedBody,
          summaryBody: selectedArticle.summaryBody,
          editorialNotes: selectedArticle.editorialNotes,
          readyToPublish: selectedArticle.status === 'READY_TO_PUBLISH',
        }),
      })
      const result = await request<{ messageId: string; draftChatId: string }>(
        `/api/admin/articles/${selectedArticle.preparedArticleId}/send-draft`,
        { method: 'POST' },
      )
      setMessage(
        `Черновик отправлен в личку Telegram (chat ${result.draftChatId}, message ${result.messageId}). Откройте бота и отредактируйте пост.`,
      )
    } catch (error) {
      setMessage((error as Error).message)
    } finally {
      setLoading(false)
    }
  }

  async function discoverDraftChats() {
    setLoading(true)
    try {
      const chats = await request<DiscoveredChat[]>('/api/admin/telegram/discover-chats', {
        method: 'POST',
        body: JSON.stringify(
          telegramForm.botToken ? { botToken: telegramForm.botToken } : {},
        ),
      })
      setDiscoveredChats(chats)
      if (chats.length === 0) {
        setMessage(
          'Личные чаты не найдены. Напишите боту /start в Telegram и повторите поиск.',
        )
      } else {
        setMessage(
          `Найдено чатов: ${chats.length}. Скопируйте chat ID в APP_TELEGRAM_DRAFT_CHAT_ID в .env и перезапустите backend.`,
        )
      }
    } catch (error) {
      setMessage((error as Error).message)
    } finally {
      setLoading(false)
    }
  }

  async function copyChatId(chatId: string) {
    await navigator.clipboard.writeText(chatId)
    setMessage(`Chat ID ${chatId} скопирован. Добавьте в .env: APP_TELEGRAM_DRAFT_CHAT_ID=${chatId}`)
  }

  async function validateTelegram() {
    setLoading(true)
    try {
      const result = await request<{ botUsername: string }>('/api/admin/telegram/validate', {
        method: 'POST',
        body: JSON.stringify(telegramForm),
      })
      setMessage(`Telegram валиден: @${result.botUsername}`)
      await refreshAll()
    } catch (error) {
      setMessage((error as Error).message)
    } finally {
      setLoading(false)
    }
  }

  async function sendTestMessage() {
    setLoading(true)
    try {
      await request('/api/admin/telegram/test-message', {
        method: 'POST',
        body: JSON.stringify(telegramForm),
      })
      setMessage('Тестовое сообщение отправлено.')
    } catch (error) {
      setMessage((error as Error).message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="layout">
      <header className="topbar">
        <div>
          <p className="eyebrow">Geek Parser</p>
          <h1>Редакторский пульт публикаций</h1>
          <p className="subtle">
            Парсинг за 48 часов, AI-адаптация, ручная редактура и отложенные публикации в Telegram.
          </p>
        </div>
        <div className="actions">
          <button type="button" onClick={() => void refreshAll()} disabled={loading}>
            Обновить
          </button>
          <button type="button" onClick={() => void syncSources()} disabled={loading}>
            Запустить парсинг
          </button>
        </div>
      </header>

      <div className="message">{loading ? 'Выполняется запрос...' : message}</div>

      <section className="dashboard">
        <aside className="panel list-panel">
          <h2>Статьи</h2>
          <p className="subtle">{articles.length} материалов в редакторском потоке.</p>
          <div className="article-list">
            {articles.map((article) => (
              <button
                key={article.preparedArticleId}
                type="button"
                className={`article-card ${article.preparedArticleId === selectedId ? 'selected' : ''}`}
                onClick={() => setSelectedId(article.preparedArticleId)}
              >
                <span className="badge">{article.status}</span>
                <strong>{article.title}</strong>
                <span>{article.sourceCode}</span>
                <span>{new Date(article.publishedAt).toLocaleString('ru-RU')}</span>
              </button>
            ))}
          </div>
        </aside>

        <main className="panel editor-panel">
          <h2>Редактор статьи</h2>
          {selectedArticle ? (
            <div className="editor-grid">
              <label>
                Заголовок
                <input
                  value={selectedArticle.title}
                  onChange={(event) =>
                    setSelectedArticle({ ...selectedArticle, title: event.target.value })
                  }
                />
              </label>
              <label>
                Статус
                <select
                  value={selectedArticle.status}
                  onChange={(event) =>
                    setSelectedArticle({
                      ...selectedArticle,
                      status: event.target.value as ArticleStatus,
                    })
                  }
                >
                  <option value="READY_FOR_REVIEW">READY_FOR_REVIEW</option>
                  <option value="READY_TO_PUBLISH">READY_TO_PUBLISH</option>
                  <option value="FAILED">FAILED</option>
                </select>
              </label>
              <div className="full-width">
                <p className="field-label">Готовый текст для Telegram</p>
                <TelegramEditor
                  title={selectedArticle.title}
                  value={selectedArticle.summaryBody}
                  onChange={(summaryBody) =>
                    setSelectedArticle({ ...selectedArticle, summaryBody })
                  }
                />
              </div>
              <label className="full-width">
                Полный перевод
                <textarea
                  rows={8}
                  value={selectedArticle.translatedBody}
                  onChange={(event) =>
                    setSelectedArticle({ ...selectedArticle, translatedBody: event.target.value })
                  }
                />
              </label>
              <label className="full-width">
                Комментарии редактора
                <textarea
                  rows={4}
                  value={selectedArticle.editorialNotes ?? ''}
                  onChange={(event) =>
                    setSelectedArticle({ ...selectedArticle, editorialNotes: event.target.value })
                  }
                />
              </label>

              <div className="meta full-width">
                <div>
                  <span>Источник</span>
                  <a href={selectedArticle.canonicalUrl} target="_blank" rel="noreferrer">
                    {selectedArticle.sourceCode}
                  </a>
                </div>
                <div>
                  <span>Markdown</span>
                  <code>{selectedArticle.markdownPath}</code>
                </div>
                <div>
                  <span>Оригинал</span>
                  <p>{selectedArticle.rawExcerpt}</p>
                </div>
              </div>

              <div className="actions full-width">
                <button type="button" onClick={() => void saveArticle()} disabled={loading}>
                  Сохранить
                </button>
                <button
                  type="button"
                  className="secondary-action"
                  onClick={() => void sendDraftToTelegram()}
                  disabled={loading || !telegramConfig?.draftChatConfigured}
                  title={
                    telegramConfig?.draftChatConfigured
                      ? 'Отправить черновик в личку с ботом'
                      : 'Настройте APP_TELEGRAM_DRAFT_CHAT_ID в .env'
                  }
                >
                  Черновик в Telegram
                </button>
                <button type="button" onClick={() => void publishNow()} disabled={loading}>
                  Опубликовать сейчас
                </button>
              </div>
              {!telegramConfig?.draftChatConfigured ? (
                <p className="subtle full-width">
                  Для черновиков: напишите боту /start, найдите chat ID в блоке Telegram ниже и добавьте{' '}
                  <code>APP_TELEGRAM_DRAFT_CHAT_ID</code> в .env.
                </p>
              ) : null}

              <div className="schedule-box full-width">
                <label>
                  Отложить публикацию
                  <input
                    type="datetime-local"
                    value={scheduleAt}
                    onChange={(event) => setScheduleAt(event.target.value)}
                  />
                </label>
                <p className="subtle">
                  Сервер отправит пост в канал в выбранное время (минимум через 1 минуту). Форматирование из
                  редактора сохранится при публикации.
                </p>
                <button type="button" onClick={() => void schedulePublication()} disabled={loading}>
                  Запланировать
                </button>
              </div>
            </div>
          ) : (
            <p className="subtle">Выберите материал слева.</p>
          )}
        </main>
      </section>

      <SourceAdminPanel
        loading={loading}
        setLoading={setLoading}
        setMessage={setMessage}
        onChanged={refreshAll}
        request={request}
      />

      <section className="bottom-grid">
        <div className="panel">
          <h2>Telegram</h2>
          <p className="subtle">
            Мастер подключения: токен, chat ID, валидация и тестовая отправка.
            {telegramConfig?.configuredViaEnv
              ? ' Сейчас используются значения из .env (APP_TELEGRAM_*).'
              : ''}
          </p>
          <div className="editor-grid">
            <label>
              Bot token
              <input
                value={telegramForm.botToken}
                onChange={(event) => setTelegramForm({ ...telegramForm, botToken: event.target.value })}
              />
            </label>
            <label>
              Chat ID
              <input
                value={telegramForm.chatId}
                onChange={(event) => setTelegramForm({ ...telegramForm, chatId: event.target.value })}
              />
            </label>
          </div>
          <div className="actions">
            <button type="button" onClick={() => void validateTelegram()} disabled={loading}>
              Валидировать
            </button>
            <button type="button" onClick={() => void sendTestMessage()} disabled={loading}>
              Тестовый пинг
            </button>
            <button type="button" onClick={() => void discoverDraftChats()} disabled={loading}>
              Найти chat ID
            </button>
          </div>
          {discoveredChats.length > 0 ? (
            <div className="discovered-chats full-width">
              <p className="preview-label">Личные чаты с ботом</p>
              {discoveredChats.map((chat) => (
                <div key={chat.chatId} className="discovered-chat-row">
                  <div>
                    <strong>{chat.firstName ?? chat.username ?? 'Пользователь'}</strong>
                    {chat.username ? <span> @{chat.username}</span> : null}
                    {chat.lastMessageText ? <p className="subtle">{chat.lastMessageText}</p> : null}
                  </div>
                  <button type="button" onClick={() => void copyChatId(chat.chatId)}>
                    {chat.chatId}
                  </button>
                </div>
              ))}
            </div>
          ) : null}
          {telegramConfig ? (
            <div className="meta">
              <div>
                <span>Текущий bot</span>
                <p>@{telegramConfig.botUsername || 'unknown'}</p>
              </div>
              <div>
                <span>Chat ID канала</span>
                <p>{telegramConfig.chatId}</p>
              </div>
              <div>
                <span>Черновики (личка)</span>
                <p>
                  {telegramConfig.draftChatConfigured
                    ? `настроено (${telegramConfig.draftChatIdMasked})`
                    : 'не настроено'}
                </p>
              </div>
              <div>
                <span>Статус</span>
                <p>{telegramConfig.validated ? 'validated' : 'not validated'}</p>
              </div>
            </div>
          ) : null}
        </div>

        <div className="panel">
          <h2>Публикации</h2>
          <p className="subtle">
            {selectedIndex >= 0 ? `Выбрана статья #${selectedIndex + 1}.` : 'Список задач публикации.'}
          </p>
          <div className="publication-list">
            {publications.map((publication) => (
              <div key={publication.id} className="publication-card">
                <strong>{publication.title}</strong>
                <span>{publication.status}</span>
                <span>{new Date(publication.scheduledAt).toLocaleString('ru-RU')}</span>
                <span>Попытки: {publication.attemptCount}</span>
                {publication.lastError ? <p>{publication.lastError}</p> : null}
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}

export default App
