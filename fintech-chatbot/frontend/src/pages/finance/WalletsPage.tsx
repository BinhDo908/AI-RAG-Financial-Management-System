import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Archive, ArchiveRestore, Banknote, Landmark, Plus, Smartphone, SlidersHorizontal, Trash2 } from 'lucide-react'
import { ApiError } from '../../api/client'
import { financeApi, type WalletType } from '../../api/finance'
import type { Wallet, WalletList } from '../../api/types'
import { formatMoney, formatSigned } from '../../lib/format'
import { EmptyState, ErrorNote, FinancePage, MoneyInput, PageHeader, Skeleton } from '../../components/finance/common'
import Dialog from '../../components/ui/Dialog'
import Segmented from '../../components/ui/Segmented'

const WALLET_TYPES: Record<WalletType, { label: string; Icon: typeof Banknote }> = {
  CASH: { label: 'Tiền mặt', Icon: Banknote },
  BANK: { label: 'Ngân hàng', Icon: Landmark },
  EWALLET: { label: 'Ví điện tử', Icon: Smartphone },
}

type DialogState = { kind: 'create' } | { kind: 'edit'; wallet: Wallet } | { kind: 'adjust'; wallet: Wallet } | null

/** UC-A01: quản lý ví — thêm, sửa, điều chỉnh số dư, lưu trữ, xóa. */
export default function WalletsPage() {
  const [data, setData] = useState<WalletList | null>(null)
  const [showArchived, setShowArchived] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [dialog, setDialog] = useState<DialogState>(null)

  const load = useCallback(async () => {
    try {
      setData(await financeApi.wallets(true))
      setError(null)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không tải được danh sách ví')
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const active = data?.wallets.filter((w) => !w.archived) ?? []
  const archived = data?.wallets.filter((w) => w.archived) ?? []
  const activeTotal = active.reduce((s, w) => s + w.currentBalance, 0)

  const done = () => {
    setDialog(null)
    void load()
  }

  return (
    <FinancePage>
      <PageHeader title="Ví">
        <button className="btn btn-primary" onClick={() => setDialog({ kind: 'create' })}>
          <Plus size={16} />
          Thêm ví
        </button>
      </PageHeader>

      {error && <ErrorNote message={error} />}

      <section className="rounded-xl border border-stone-200 bg-white p-5 sm:p-6">
        <p className="text-sm text-stone-500">Tổng số dư</p>
        {data ? (
          <p className={`mt-1 truncate text-[clamp(2.25rem,11vw,3rem)] leading-none font-semibold tracking-[-0.03em] ${activeTotal < 0 ? 'text-negative' : 'text-ink'}`}>
            {formatMoney(activeTotal)}
          </p>
        ) : (
          <Skeleton className="mt-2 h-12 w-56" />
        )}
      </section>

      {!data ? (
        <Skeleton className="h-48" />
      ) : active.length === 0 ? (
        <EmptyState
          title="Chưa có ví nào"
          action={
            <button className="btn btn-primary" onClick={() => setDialog({ kind: 'create' })}>
              <Plus size={16} /> Thêm ví
            </button>
          }
        />
      ) : (
        <WalletTable wallets={active} total={activeTotal} onEdit={(w) => setDialog({ kind: 'edit', wallet: w })} onAdjust={(w) => setDialog({ kind: 'adjust', wallet: w })} />
      )}

      {archived.length > 0 && (
        <div>
          <button className="btn btn-ghost btn-sm -ml-2" onClick={() => setShowArchived((v) => !v)} aria-expanded={showArchived}>
            <Archive size={14} />
            {showArchived ? 'Ẩn' : 'Hiện'} {archived.length} ví đã lưu trữ
          </button>
          {showArchived && (
            <div className="enter mt-2">
              <WalletTable wallets={archived} onEdit={(w) => setDialog({ kind: 'edit', wallet: w })} />
            </div>
          )}
        </div>
      )}

      {dialog?.kind === 'create' && <WalletFormDialog onClose={() => setDialog(null)} onDone={done} />}
      {dialog?.kind === 'edit' && <WalletFormDialog wallet={dialog.wallet} onClose={() => setDialog(null)} onDone={done} />}
      {dialog?.kind === 'adjust' && <AdjustDialog wallet={dialog.wallet} onClose={() => setDialog(null)} onDone={done} />}
    </FinancePage>
  )
}

function WalletTable({ wallets, total, onEdit, onAdjust }: { wallets: Wallet[]; total?: number; onEdit: (w: Wallet) => void; onAdjust?: (w: Wallet) => void }) {
  return (
    <ul className="divide-y divide-stone-100 rounded-xl border border-stone-200 bg-white">
      {wallets.map((w) => {
        const { Icon, label } = WALLET_TYPES[w.type]
        const change = w.currentBalance - w.initialBalance
        return (
          <li key={w.id} className="flex items-center gap-3 py-3 pr-2 pl-3 sm:px-4">
            <span className={`grid size-9 shrink-0 place-items-center rounded-lg ${w.archived ? 'bg-stone-50 text-stone-400' : 'bg-stone-100 text-stone-600'}`}>
              <Icon size={17} />
            </span>
            <button onClick={() => onEdit(w)} className="min-w-0 flex-1 text-left">
              <span className="flex items-center gap-2">
                <span className={`truncate text-sm font-medium ${w.archived ? 'text-stone-500' : 'text-ink'}`}>{w.name}</span>
              </span>
              <span className="block truncate text-xs text-stone-500">
                {label}
                {w.defaultWallet && <span className="font-medium text-stone-700"> · Mặc định</span>}
                {change !== 0 && <span className="hidden sm:inline"> · {formatSigned(change)} so với ban đầu</span>}
              </span>
            </button>
            <div className="shrink-0 text-right">
              <div className={`tabular text-sm font-semibold ${w.currentBalance < 0 ? 'text-negative' : 'text-ink'}`}>{formatMoney(w.currentBalance)}</div>
              {total !== undefined && total > 0 && w.currentBalance > 0 && (
                <div className="tabular hidden text-xs text-stone-500 sm:block">{Math.round((w.currentBalance / total) * 100)}% tổng</div>
              )}
            </div>
            {onAdjust && (
              <button onClick={() => onAdjust(w)} className="btn btn-ghost btn-icon btn-sm shrink-0" title="Điều chỉnh số dư" aria-label={`Điều chỉnh số dư ${w.name}`}>
                <SlidersHorizontal size={15} />
              </button>
            )}
          </li>
        )
      })}
    </ul>
  )
}

function WalletFormDialog({ wallet, onClose, onDone }: { wallet?: Wallet; onClose: () => void; onDone: () => void }) {
  const editing = !!wallet
  const [name, setName] = useState(wallet?.name ?? '')
  const [type, setType] = useState<WalletType>(wallet?.type ?? 'CASH')
  const [initialBalance, setInitialBalance] = useState<number | null>(wallet ? wallet.initialBalance : 0)
  const [defaultWallet, setDefaultWallet] = useState(wallet?.defaultWallet ?? false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [confirmDelete, setConfirmDelete] = useState(false)

  const run = async (action: () => Promise<unknown>, fallback: string) => {
    setBusy(true)
    setError(null)
    try {
      await action()
      onDone()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : fallback)
      setConfirmDelete(false)
    } finally {
      setBusy(false)
    }
  }

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return setError('Nhập tên ví')
    void run(
      () =>
        editing
          ? financeApi.updateWallet(wallet!.id, { name: name.trim(), type, defaultWallet })
          : financeApi.createWallet({ name: name.trim(), type, initialBalance: initialBalance ?? 0, defaultWallet }),
      'Không lưu được ví',
    )
  }

  return (
    <Dialog
      title={editing ? 'Sửa ví' : 'Thêm ví'}
      onClose={onClose}
      footer={
        <>
          {editing && (
            <div className="mr-auto flex gap-1">
              <button
                type="button"
                className="btn btn-ghost"
                disabled={busy}
                onClick={() => void run(() => financeApi.archiveWallet(wallet!.id, !wallet!.archived), 'Không cập nhật được ví')}
                title={wallet!.archived ? 'Dùng lại ví' : 'Ẩn ví khỏi danh sách, giữ lịch sử giao dịch'}
              >
                {wallet!.archived ? <ArchiveRestore size={15} /> : <Archive size={15} />}
                <span className="hidden sm:inline">{wallet!.archived ? 'Bỏ lưu trữ' : 'Lưu trữ'}</span>
              </button>
              <button
                type="button"
                className={`btn ${confirmDelete ? 'btn-danger' : 'btn-ghost text-negative'}`}
                disabled={busy}
                onClick={() => (confirmDelete ? void run(() => financeApi.deleteWallet(wallet!.id), 'Không xóa được ví') : setConfirmDelete(true))}
              >
                <Trash2 size={15} />
                {confirmDelete ? 'Xác nhận xóa' : <span className="hidden sm:inline">Xóa</span>}
              </button>
            </div>
          )}
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Hủy
          </button>
          <button type="submit" form="wallet-form" className="btn btn-primary" disabled={busy}>
            {busy ? 'Đang lưu…' : editing ? 'Lưu' : 'Thêm ví'}
          </button>
        </>
      }
    >
      <form id="wallet-form" onSubmit={submit} className="grid gap-4" noValidate>
        <div>
          <label htmlFor="wallet-name" className="label">
            Tên ví
          </label>
          <input id="wallet-name" className="input" maxLength={100} value={name} onChange={(e) => setName(e.target.value)} placeholder="VD: Vietcombank, Ví MoMo" />
        </div>
        <div>
          <span className="label">Loại</span>
          <Segmented
            value={type}
            onChange={setType}
            label="Loại ví"
            options={(Object.keys(WALLET_TYPES) as WalletType[]).map((t) => ({ value: t, label: WALLET_TYPES[t].label }))}
          />
        </div>
        {!editing && (
          <div>
            <label htmlFor="wallet-initial" className="label">
              Số dư hiện có
            </label>
            <MoneyInput id="wallet-initial" value={initialBalance} onChange={setInitialBalance} />
          </div>
        )}
        <label className="flex items-center gap-2 text-sm text-stone-700">
          <input type="checkbox" className="size-4 accent-[#1c1917]" checked={defaultWallet} onChange={(e) => setDefaultWallet(e.target.checked)} />
          Dùng làm ví mặc định khi ghi giao dịch
        </label>
        {error && <ErrorNote message={error} />}
      </form>
    </Dialog>
  )
}

/** UC-A01 AF-1: nhập số dư thực tế → hệ thống tạo giao dịch ADJUST cho phần chênh lệch (BR-07). */
function AdjustDialog({ wallet, onClose, onDone }: { wallet: Wallet; onClose: () => void; onDone: () => void }) {
  const [balance, setBalance] = useState<number | null>(wallet.currentBalance >= 0 ? wallet.currentBalance : null)
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const diff = balance === null ? 0 : balance - wallet.currentBalance

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (balance === null) return setError('Nhập số dư thực tế')
    setBusy(true)
    setError(null)
    try {
      await financeApi.adjustBalance(wallet.id, balance, note.trim() || undefined)
      onDone()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Không điều chỉnh được số dư')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog
      title={`Điều chỉnh số dư · ${wallet.name}`}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Hủy
          </button>
          <button type="submit" form="adjust-form" className="btn btn-primary" disabled={busy || diff === 0}>
            {busy ? 'Đang lưu…' : 'Điều chỉnh'}
          </button>
        </>
      }
    >
      <form id="adjust-form" onSubmit={submit} className="grid gap-4" noValidate>
        <div>
          <label htmlFor="adjust-balance" className="label">
            Số dư thực tế
          </label>
          <MoneyInput id="adjust-balance" value={balance} onChange={setBalance} large />
          <p className="tabular mt-1.5 text-xs text-stone-500">
            Hiện tại {formatMoney(wallet.currentBalance)}
            {diff !== 0 && (
              <>
                {' · '}chênh lệch <span className="font-medium text-ink">{formatSigned(diff)}</span>
              </>
            )}
          </p>
        </div>
        <div>
          <label htmlFor="adjust-note" className="label">
            Ghi chú <span className="font-normal text-stone-400">(không bắt buộc)</span>
          </label>
          <input id="adjust-note" className="input" maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} placeholder="VD: kiểm kê cuối tháng" />
        </div>
        {error && <ErrorNote message={error} />}
      </form>
    </Dialog>
  )
}
