import { useMemo, useRef, useState } from 'react'
import {
  EDITOR_EMOJIS,
  buildTelegramPostPreview,
  countSentences,
  insertAtCursor,
  stripHtml,
  wrapSelection,
} from './telegramFormat'

type TelegramEditorProps = {
  title: string
  value: string
  onChange: (value: string) => void
  maxSentences?: number
  maxChars?: number
}

export function TelegramEditor({
  title,
  value,
  onChange,
  maxSentences = 10,
  maxChars = 4096,
}: TelegramEditorProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const [showEmoji, setShowEmoji] = useState(false)

  const plainLength = stripHtml(value).length
  const sentenceCount = countSentences(value)
  const sentenceOver = sentenceCount > maxSentences
  const charOver = plainLength > maxChars

  const previewHtml = useMemo(() => buildTelegramPostPreview(title, value), [title, value])

  function applyWrap(openTag: string, closeTag: string) {
    const el = textareaRef.current
    if (!el) {
      return
    }
    const { value: next, selectionStart, selectionEnd } = wrapSelection(
      value,
      el.selectionStart,
      el.selectionEnd,
      openTag,
      closeTag,
    )
    onChange(next)
    requestAnimationFrame(() => {
      el.focus()
      el.setSelectionRange(selectionStart, selectionEnd)
    })
  }

  function applyLink() {
    const url = window.prompt('URL ссылки (https://...)', 'https://')
    if (!url || !url.startsWith('http')) {
      return
    }
    applyWrap(`<a href="${url}">`, '</a>')
  }

  function applyEmoji(emoji: string) {
    const el = textareaRef.current
    const cursor = el?.selectionStart ?? value.length
    const { value: next, cursor: nextCursor } = insertAtCursor(value, cursor, emoji)
    onChange(next)
    requestAnimationFrame(() => {
      if (el) {
        el.focus()
        el.setSelectionRange(nextCursor, nextCursor)
      }
    })
    setShowEmoji(false)
  }

  async function copyPost() {
    const post = title.trim() ? `${title.trim()}\n\n${stripHtml(value)}` : stripHtml(value)
    await navigator.clipboard.writeText(post)
  }

  return (
    <div className="telegram-editor">
      <div className="editor-toolbar">
        <button type="button" title="Жирный" onClick={() => applyWrap('<b>', '</b>')}>
          <strong>B</strong>
        </button>
        <button type="button" title="Курсив" onClick={() => applyWrap('<i>', '</i>')}>
          <em>I</em>
        </button>
        <button type="button" title="Зачёркнутый" onClick={() => applyWrap('<s>', '</s>')}>
          <s>S</s>
        </button>
        <button type="button" title="Код" onClick={() => applyWrap('<code>', '</code>')}>
          {'</>'}
        </button>
        <button type="button" title="Ссылка" onClick={() => applyLink()}>
          🔗
        </button>
        <button type="button" title="Эмодзи" onClick={() => setShowEmoji((v) => !v)}>
          😀
        </button>
        <button type="button" className="toolbar-secondary" onClick={() => void copyPost()}>
          Копировать
        </button>
      </div>

      {showEmoji ? (
        <div className="emoji-picker">
          {EDITOR_EMOJIS.map((emoji) => (
            <button key={emoji} type="button" className="emoji-btn" onClick={() => applyEmoji(emoji)}>
              {emoji}
            </button>
          ))}
        </div>
      ) : null}

      <textarea
        ref={textareaRef}
        rows={10}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="Текст поста для Telegram. Выделите фрагмент и нажмите B/I/🔗."
      />

      <div className="editor-stats">
        <span className={sentenceOver ? 'stat-warn' : ''}>
          Предложений: {sentenceCount} / {maxSentences}
        </span>
        <span className={charOver ? 'stat-warn' : ''}>
          Символов: {plainLength} / {maxChars}
        </span>
      </div>

      <div className="telegram-preview">
        <p className="preview-label">Превью в Telegram</p>
        <div className="telegram-bubble">
          <div className="telegram-bubble-inner" dangerouslySetInnerHTML={{ __html: previewHtml }} />
        </div>
        <p className="subtle preview-hint">
          Поддерживаются HTML-теги Telegram: b, i, s, code, a. Премиум-эмодзи Telegram доступны только
          в клиенте — здесь обычные Unicode-эмодзи.
        </p>
      </div>
    </div>
  )
}
