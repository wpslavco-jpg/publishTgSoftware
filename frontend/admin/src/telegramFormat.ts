/** Utilities for Telegram HTML formatting (Bot API parse_mode=HTML subset). */

const SENTENCE_SPLIT = /(?<=[.!?…])\s+/u

export function countSentences(text: string): number {
  const plain = stripHtml(text).trim()
  if (!plain) {
    return 0
  }
  return plain.split(SENTENCE_SPLIT).filter((part) => part.trim().length > 0).length
}

export function stripHtml(text: string): string {
  return text.replace(/<[^>]+>/g, '')
}

export function looksLikeTelegramHtml(text: string): boolean {
  return /<(b|i|u|s|code|pre|a)\b/i.test(text)
}

export function wrapSelection(
  value: string,
  selectionStart: number,
  selectionEnd: number,
  openTag: string,
  closeTag: string,
): { value: string; selectionStart: number; selectionEnd: number } {
  const selected = value.slice(selectionStart, selectionEnd)
  const wrapped = selected || 'текст'
  const next =
    value.slice(0, selectionStart) + openTag + wrapped + closeTag + value.slice(selectionEnd)
  const start = selectionStart + openTag.length
  const end = start + wrapped.length
  return { value: next, selectionStart: start, selectionEnd: end }
}

export function insertAtCursor(value: string, cursor: number, insertion: string): {
  value: string
  cursor: number
} {
  const next = value.slice(0, cursor) + insertion + value.slice(cursor)
  return { value: next, cursor: cursor + insertion.length }
}

/** Allow only Telegram-safe HTML tags for preview. */
export function sanitizeTelegramHtml(html: string): string {
  return html
    .replace(/<(?!(\/?(b|i|u|s|code|pre|a\b[^>]*|br\s*\/?))>)[^>]+>/gi, '')
    .replace(/<a\b([^>]*)>/gi, (_, attrs: string) => {
      const href = attrs.match(/href\s*=\s*["']([^"']+)["']/i)?.[1]
      if (!href || !/^https?:\/\//i.test(href)) {
        return ''
      }
      return `<a href="${href}" target="_blank" rel="noreferrer">`
    })
}

export function buildTelegramPostPreview(title: string, body: string): string {
  const safeBody = sanitizeTelegramHtml(body).replace(/\n/g, '<br />')
  const safeTitle = stripHtml(title)
  if (safeTitle) {
    return `<strong>${escapeHtml(safeTitle)}</strong><br /><br />${safeBody}`
  }
  return safeBody
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

export const EDITOR_EMOJIS = [
  '📱', '💻', '🎮', '⌚', '🔥', '⚡', '✨', '🚀', '🤖', '🎯',
  '📰', '👀', '💡', '🛠️', '📸', '🎧', '🔋', '⭐', '❗', '✅',
  '❌', '🆕', '🆓', '💰', '📈', '📉', '🌍', '🇷🇺', '❤️', '😎',
]
