const moneyFormat = new Intl.NumberFormat('vi-VN')

/** 1234567 → "1.234.567đ" (khớp MoneyFormatter ở backend). */
export function formatMoney(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '—'
  return moneyFormat.format(Math.round(value) || 0) + 'đ'
}

/** Số tiền có dấu: thu "+1.000đ", chi "−1.000đ" (dấu trừ thật, không phải gạch nối). */
export function formatSigned(value: number): string {
  if (value === 0) return formatMoney(0)
  return (value > 0 ? '+' : '−') + formatMoney(Math.abs(value))
}

/** Rút gọn cho trục biểu đồ: 1.500.000 → "1,5tr"; 25.000 → "25k"; 2.300.000.000 → "2,3 tỷ". */
export function formatCompact(value: number): string {
  const abs = Math.abs(value)
  const sign = value < 0 ? '−' : ''
  const fmt = (n: number) => n.toLocaleString('vi-VN', { maximumFractionDigits: n < 10 ? 1 : 0 })
  if (abs >= 1e9) return `${sign}${fmt(abs / 1e9)} tỷ`
  if (abs >= 1e6) return `${sign}${fmt(abs / 1e6)}tr`
  if (abs >= 1e3) return `${sign}${fmt(abs / 1e3)}k`
  return `${sign}${abs}`
}

export function formatDate(iso: string): string {
  const [y, m, d] = iso.slice(0, 10).split('-')
  return `${d}/${m}/${y}`
}

const WEEKDAYS = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy']

/** Tiêu đề nhóm ngày trong danh sách giao dịch: "Hôm nay", "Hôm qua", "Thứ ba, 21/10". */
export function formatDayHeading(iso: string): string {
  const today = todayIso()
  if (iso === today) return 'Hôm nay'
  if (iso === addDays(today, -1)) return 'Hôm qua'
  const d = new Date(iso + 'T00:00:00')
  const [y, m, day] = iso.split('-')
  const sameYear = y === today.slice(0, 4)
  return `${WEEKDAYS[d.getDay()]}, ${day}/${m}${sameYear ? '' : '/' + y}`
}

/** "2026-10" → "Tháng 10/2026"; dạng ngắn "T10". */
export function formatMonth(month: string, short = false): string {
  const [y, m] = month.split('-')
  return short ? `T${Number(m)}` : `Tháng ${Number(m)}/${y}`
}

/** Thời điểm tương đối ngắn gọn. */
export function formatRelative(iso: string): string {
  const date = new Date(iso)
  const diff = (Date.now() - date.getTime()) / 1000
  if (diff < 60) return 'vừa xong'
  if (diff < 3600) return `${Math.floor(diff / 60)} phút trước`
  if (diff < 86400) return `${Math.floor(diff / 3600)} giờ trước`
  if (diff < 7 * 86400) return `${Math.floor(diff / 86400)} ngày trước`
  return date.toLocaleDateString('vi-VN')
}

export function todayIso(): string {
  const d = new Date()
  const offset = d.getTimezoneOffset() * 60000
  return new Date(d.getTime() - offset).toISOString().slice(0, 10)
}

export function addDays(iso: string, days: number): string {
  const d = new Date(iso + 'T00:00:00Z')
  d.setUTCDate(d.getUTCDate() + days)
  return d.toISOString().slice(0, 10)
}

export function currentMonth(): string {
  return todayIso().slice(0, 7)
}

export function shiftMonth(month: string, delta: number): string {
  const [y, m] = month.split('-').map(Number)
  const d = new Date(Date.UTC(y, m - 1 + delta, 1))
  return d.toISOString().slice(0, 7)
}

export function monthRange(month: string): { from: string; to: string } {
  const from = `${month}-01`
  const to = addDays(`${shiftMonth(month, 1)}-01`, -1)
  return { from, to }
}
