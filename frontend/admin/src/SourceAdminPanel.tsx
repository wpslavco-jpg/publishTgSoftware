import { useEffect, useState } from 'react'

type SourceType = 'HTML' | 'RSS'

export type SourceItem = {
  id: number
  code: string
  name: string
  type: SourceType
  baseUrl: string
  listingUrl: string
  rssUrl?: string
  active: boolean
  builtIn: boolean
  articleUrlPatterns: string
  bodySelectors?: string
  titleSelectors?: string
  publishedAtSelectors?: string
}

type SourceForm = {
  code: string
  name: string
  type: SourceType
  baseUrl: string
  listingUrl: string
  rssUrl: string
  active: boolean
  articleUrlPatterns: string
  bodySelectors: string
  titleSelectors: string
  publishedAtSelectors: string
}

const DEFAULT_BODY = 'article\n.article\n.entry-content\nmain'
const DEFAULT_TITLE = 'h1'
const DEFAULT_PUBLISHED_AT = 'meta[property=article:published_time]\ntime[datetime]\nmeta[name=date]'

const emptyForm: SourceForm = {
  code: '',
  name: '',
  type: 'HTML',
  baseUrl: '',
  listingUrl: '',
  rssUrl: '',
  active: true,
  articleUrlPatterns: '',
  bodySelectors: DEFAULT_BODY,
  titleSelectors: DEFAULT_TITLE,
  publishedAtSelectors: DEFAULT_PUBLISHED_AT,
}

type SourceAdminPanelProps = {
  loading: boolean
  setLoading: (value: boolean) => void
  setMessage: (value: string) => void
  onChanged: () => Promise<void>
  request: <T>(url: string, init?: RequestInit) => Promise<T>
}

export function SourceAdminPanel({
  loading,
  setLoading,
  setMessage,
  onChanged,
  request,
}: SourceAdminPanelProps) {
  const [sources, setSources] = useState<SourceItem[]>([])
  const [form, setForm] = useState<SourceForm>(emptyForm)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [showForm, setShowForm] = useState(false)

  useEffect(() => {
    void loadSources()
  }, [])

  async function loadSources() {
    const list = await request<SourceItem[]>('/api/admin/sources')
    setSources(list)
  }

  function startCreate() {
    setEditingId(null)
    setForm(emptyForm)
    setShowForm(true)
  }

  function startEdit(source: SourceItem) {
    setEditingId(source.id)
    setForm({
      code: source.code,
      name: source.name,
      type: source.type,
      baseUrl: source.baseUrl,
      listingUrl: source.listingUrl,
      rssUrl: source.rssUrl ?? '',
      active: source.active,
      articleUrlPatterns: source.articleUrlPatterns ?? '',
      bodySelectors: source.bodySelectors ?? DEFAULT_BODY,
      titleSelectors: source.titleSelectors ?? DEFAULT_TITLE,
      publishedAtSelectors: source.publishedAtSelectors ?? DEFAULT_PUBLISHED_AT,
    })
    setShowForm(true)
  }

  async function saveSource() {
    setLoading(true)
    try {
      if (editingId == null) {
        await request<SourceItem>('/api/admin/sources', {
          method: 'POST',
          body: JSON.stringify(form),
        })
        setMessage('Источник добавлен.')
      } else {
        const { code: _code, ...updatePayload } = form
        await request<SourceItem>(`/api/admin/sources/${editingId}`, {
          method: 'PUT',
          body: JSON.stringify(updatePayload),
        })
        setMessage('Источник обновлён.')
      }
      setShowForm(false)
      setEditingId(null)
      setForm(emptyForm)
      await loadSources()
      await onChanged()
    } catch (error) {
      setMessage((error as Error).message)
    } finally {
      setLoading(false)
    }
  }

  async function deleteSource(source: SourceItem) {
    const deleteArticles = window.confirm(
      `Удалить источник «${source.name}»?\n\nOK — удалить вместе со статьями этого источника.\nCancel — отмена.`,
    )
    if (!deleteArticles) {
      return
    }
    setLoading(true)
    try {
      await request(`/api/admin/sources/${source.id}?deleteArticles=true`, { method: 'DELETE' })
      setMessage(`Источник «${source.name}» удалён.`)
      if (editingId === source.id) {
        setShowForm(false)
        setEditingId(null)
      }
      await loadSources()
      await onChanged()
    } catch (error) {
      setMessage((error as Error).message)
    } finally {
      setLoading(false)
    }
  }

  async function clearStorage() {
    if (
      !window.confirm(
        'Очистить ВСЕ статьи: база данных + markdown-файлы?\n\nИсточники парсинга останутся. Действие необратимо.',
      )
    ) {
      return
    }
    setLoading(true)
    try {
      const result = await request<{
        deletedRawArticles: number
        deletedPreparedArticles: number
        deletedPublications: number
        deletedMarkdownFiles: number
      }>('/api/admin/storage/clear-articles', { method: 'POST' })
      setMessage(
        `Хранилище очищено: raw=${result.deletedRawArticles}, prepared=${result.deletedPreparedArticles}, publications=${result.deletedPublications}, files=${result.deletedMarkdownFiles}.`,
      )
      await onChanged()
    } catch (error) {
      setMessage((error as Error).message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="panel sources-panel">
      <div className="panel-header-row">
        <div>
          <h2>Источники парсинга</h2>
          <p className="subtle">{sources.length} источников · HTML-листинг + regex URL статей.</p>
        </div>
        <div className="actions">
          <button type="button" onClick={() => void clearStorage()} disabled={loading}>
            Очистить статьи
          </button>
          <button type="button" onClick={startCreate} disabled={loading}>
            Добавить
          </button>
        </div>
      </div>

      <div className="source-list">
        {sources.map((source) => (
          <div key={source.id} className="source-card">
            <div className="source-card-main">
              <strong>{source.name}</strong>
              <span className="badge">{source.code}</span>
              <span className={`badge ${source.active ? 'badge-ok' : 'badge-off'}`}>
                {source.active ? 'active' : 'off'}
              </span>
              {source.builtIn ? <span className="badge">built-in</span> : null}
              <p className="subtle">{source.listingUrl}</p>
            </div>
            <div className="source-card-actions">
              <button type="button" onClick={() => startEdit(source)} disabled={loading}>
                Изменить
              </button>
              <button type="button" onClick={() => void deleteSource(source)} disabled={loading}>
                Удалить
              </button>
            </div>
          </div>
        ))}
      </div>

      {showForm ? (
        <div className="source-form">
          <h3>{editingId == null ? 'Новый источник' : `Редактирование: ${form.code}`}</h3>
          <div className="editor-grid">
            {editingId == null ? (
              <label>
                Code (slug)
                <input
                  value={form.code}
                  onChange={(event) => setForm({ ...form, code: event.target.value })}
                  placeholder="my-source"
                />
              </label>
            ) : (
              <label>
                Code
                <input value={form.code} disabled />
              </label>
            )}
            <label>
              Название
              <input
                value={form.name}
                onChange={(event) => setForm({ ...form, name: event.target.value })}
              />
            </label>
            <label>
              Тип
              <select
                value={form.type}
                onChange={(event) => setForm({ ...form, type: event.target.value as SourceType })}
              >
                <option value="HTML">HTML</option>
                <option value="RSS">RSS</option>
              </select>
            </label>
            <label>
              Active
              <select
                value={form.active ? 'true' : 'false'}
                onChange={(event) => setForm({ ...form, active: event.target.value === 'true' })}
              >
                <option value="true">Да</option>
                <option value="false">Нет</option>
              </select>
            </label>
            <label className="full-width">
              Base URL
              <input
                value={form.baseUrl}
                onChange={(event) => setForm({ ...form, baseUrl: event.target.value })}
              />
            </label>
            <label className="full-width">
              Listing URL (страница со списком статей)
              <input
                value={form.listingUrl}
                onChange={(event) => setForm({ ...form, listingUrl: event.target.value })}
              />
            </label>
            <label className="full-width">
              Regex URL статей (по одному на строку)
              <textarea
                rows={3}
                value={form.articleUrlPatterns}
                onChange={(event) => setForm({ ...form, articleUrlPatterns: event.target.value })}
                placeholder="https://example\\.com/news/.+"
              />
            </label>
            <label className="full-width">
              CSS body selectors
              <textarea
                rows={3}
                value={form.bodySelectors}
                onChange={(event) => setForm({ ...form, bodySelectors: event.target.value })}
              />
            </label>
            <label className="full-width">
              CSS title selectors
              <textarea
                rows={2}
                value={form.titleSelectors}
                onChange={(event) => setForm({ ...form, titleSelectors: event.target.value })}
              />
            </label>
            <label className="full-width">
              CSS/meta date selectors
              <textarea
                rows={2}
                value={form.publishedAtSelectors}
                onChange={(event) => setForm({ ...form, publishedAtSelectors: event.target.value })}
              />
            </label>
          </div>
          <div className="actions">
            <button type="button" onClick={() => void saveSource()} disabled={loading}>
              Сохранить
            </button>
            <button
              type="button"
              onClick={() => {
                setShowForm(false)
                setEditingId(null)
              }}
              disabled={loading}
            >
              Отмена
            </button>
          </div>
        </div>
      ) : null}
    </div>
  )
}
