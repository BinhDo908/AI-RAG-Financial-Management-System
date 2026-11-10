import { useState, type FormEvent } from 'react'
import { Trash2 } from 'lucide-react'
import { ApiError } from '../../api/client'
import { financeApi, type Transaction, type TransactionResult } from '../../api/finance'
import type { Category, TransactionType, Wallet } from '../../api/types'
import { todayIso } from '../../lib/format'
import Dialog from '../ui/Dialog'
import Segmented from '../ui/Segmented'
import { ErrorNote, MoneyInput } from './common'

type EditableType = Exclude<TransactionType, 'ADJUST'>

interface Props {
  wallets: Wallet[]
  categories: Category[]
  /** Có giá trị → sửa giao dịch; không có → thêm mới. */
  transaction?: Transaction | null
  onClose: () => void
  onSaved: (result: TransactionResult | null) => void
}

const TYPE_OPTIONS: { value: EditableType; label: string }[] = [
  { value: 'EXPENSE', label: 'Chi' },
  { value: 'INCOME', label: 'Thu' },
  { value: 'TRANSFER', label: 'Chuyển tiền' },
]

/** UC-A02: thêm / sửa / xóa giao dịch thủ công. */
export default function TransactionDialog({ wallets, categories, transaction, onClose, onSaved }: Props) {
  const editing = !!transaction
  const activeWallets = wallets.filter((w) => !w.archived)
  const defaultWallet = activeWallets.find((w) => w.defaultWallet) ?? activeWallets[0]

  const [type, setType] = useState<EditableType>((transaction?.type as EditableType) ?? 'EXPENSE')
  const [amount, setAmount] = useState<number | null>(transaction?.amount ?? null)
  const [walletId, setWalletId] = useState<number | ''>(transaction?.walletId ?? defaultWallet?.id ?? '')
  const [toWalletId, setToWalletId] = useState<number | ''>(transaction?.toWalletId ?? '')
  const [categoryId, setCategoryId] = useState<number | ''>(transaction?.categoryId ?? '')
  const [txnDate, setTxnDate] = useState(transaction?.txnDate ?? todayIso())
  const [note, setNote] = useState(transaction?.note ?? '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [confirmDelete, setConfirmDelete] = useState(false)

  const typeCategories = categories.filter((c) => !c.hidden && c.type === type)

  const changeType = (t: EditableType) => {
    setType(t)
    // Danh mục thu và chi khác nhau → bỏ chọn khi đổi loại
    if (categories.find((c) => c.id === categoryId)?.type !== t) setCategoryId('')
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const errs: Record<string, string> = {}
    if (!amount) errs.amount = 'Nhập số tiền lớn hơn 0'
    if (!walletId) errs.walletId = 'Chọn ví'
    if (type === 'TRANSFER' && !toWalletId) errs.toWalletId = 'Chọn ví nhận'
    if (type === 'TRANSFER' && toWalletId && toWalletId === walletId) errs.toWalletId = 'Ví nhận phải khác ví nguồn'
    if (type !== 'TRANSFER' && !categoryId) errs.categoryId = 'Chọn danh mục'
    setFieldErrors(errs)
    if (Object.keys(errs).length) return

    const body = {
      type,
      amount: amount!,
      walletId: Number(walletId),
      toWalletId: type === 'TRANSFER' ? Number(toWalletId) : null,
      categoryId: type === 'TRANSFER' ? null : Number(categoryId),
      txnDate,
      note: note.trim() || null,
    }
    setBusy(true)
    setError(null)
    try {
      const result = editing ? await financeApi.updateTransaction(transaction!.id, body) : await financeApi.createTransaction(body)
      onSaved(result)
    } catch (err) {
      if (err instanceof ApiError && err.details) setFieldErrors(err.details)
      setError(err instanceof ApiError ? err.message : 'Không lưu được giao dịch')
    } finally {
      setBusy(false)
    }
  }

  const remove = async () => {
    setBusy(true)
    setError(null)
    try {
      await financeApi.deleteTransaction(transaction!.id)
      onSaved(null)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không xóa được giao dịch')
      setConfirmDelete(false)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog
      title={editing ? 'Sửa giao dịch' : 'Thêm giao dịch'}
      onClose={onClose}
      footer={
        <>
          {editing && (
            <button
              type="button"
              onClick={() => (confirmDelete ? void remove() : setConfirmDelete(true))}
              disabled={busy}
              className={`btn mr-auto ${confirmDelete ? 'btn-danger' : 'btn-ghost text-negative'}`}
            >
              <Trash2 size={15} />
              {confirmDelete ? 'Bấm lần nữa để xóa' : 'Xóa'}
            </button>
          )}
          <button type="button" onClick={onClose} className="btn btn-secondary">
            Hủy
          </button>
          <button type="submit" form="txn-form" disabled={busy} className="btn btn-primary">
            {busy ? 'Đang lưu…' : editing ? 'Lưu thay đổi' : 'Thêm giao dịch'}
          </button>
        </>
      }
    >
      <form id="txn-form" onSubmit={submit} className="grid gap-4" noValidate>
        <Segmented value={type} options={TYPE_OPTIONS} onChange={changeType} label="Loại giao dịch" />

        <div>
          <label htmlFor="txn-amount" className="label">
            Số tiền
          </label>
          <MoneyInput id="txn-amount" value={amount} onChange={setAmount} large />
          <FieldError message={fieldErrors.amount} />
        </div>

        <div className={`grid gap-4 ${type === 'TRANSFER' ? 'sm:grid-cols-2' : ''}`}>
          <div>
            <label htmlFor="txn-wallet" className="label">
              {type === 'TRANSFER' ? 'Từ ví' : 'Ví'}
            </label>
            <select id="txn-wallet" className="input" value={walletId} onChange={(e) => setWalletId(e.target.value ? Number(e.target.value) : '')}>
              <option value="">Chọn ví</option>
              {activeWallets.map((w) => (
                <option key={w.id} value={w.id}>
                  {w.name}
                </option>
              ))}
            </select>
            <FieldError message={fieldErrors.walletId} />
          </div>
          {type === 'TRANSFER' && (
            <div>
              <label htmlFor="txn-to-wallet" className="label">
                Đến ví
              </label>
              <select
                id="txn-to-wallet"
                className="input"
                value={toWalletId}
                onChange={(e) => setToWalletId(e.target.value ? Number(e.target.value) : '')}
              >
                <option value="">Chọn ví nhận</option>
                {activeWallets
                  .filter((w) => w.id !== walletId)
                  .map((w) => (
                    <option key={w.id} value={w.id}>
                      {w.name}
                    </option>
                  ))}
              </select>
              <FieldError message={fieldErrors.toWalletId} />
            </div>
          )}
        </div>

        {type !== 'TRANSFER' && (
          <div>
            <span className="label">Danh mục</span>
            <div className="flex flex-wrap gap-1.5" role="radiogroup" aria-label="Danh mục">
              {typeCategories.map((c) => (
                <button
                  key={c.id}
                  type="button"
                  role="radio"
                  aria-checked={categoryId === c.id}
                  onClick={() => setCategoryId(c.id)}
                  className={`h-8 rounded-full border px-3 text-sm transition-colors ${
                    categoryId === c.id ? 'border-ink bg-ink text-white' : 'border-stone-200 bg-white text-stone-700 hover:border-stone-300'
                  }`}
                >
                  {c.name}
                </button>
              ))}
            </div>
            <FieldError message={fieldErrors.categoryId} />
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-[10rem_minmax(0,1fr)]">
          <div>
            <label htmlFor="txn-date" className="label">
              Ngày
            </label>
            <input id="txn-date" type="date" className="input" value={txnDate} max={todayIso()} onChange={(e) => setTxnDate(e.target.value)} />
            <FieldError message={fieldErrors.txnDate} />
          </div>
          <div>
            <label htmlFor="txn-note" className="label">
              Ghi chú <span className="font-normal text-stone-400">(không bắt buộc)</span>
            </label>
            <input id="txn-note" className="input" maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} placeholder="VD: ăn trưa với nhóm" />
          </div>
        </div>

        {error && <ErrorNote message={error} />}
      </form>
    </Dialog>
  )
}

function FieldError({ message }: { message?: string }) {
  return message ? <p className="mt-1 text-xs text-negative">{message}</p> : null
}
