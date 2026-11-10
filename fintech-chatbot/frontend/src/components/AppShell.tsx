import { NavLink, Outlet } from 'react-router-dom'
import { ArrowLeftRight, BookOpen, Gauge, LayoutGrid, MessageCircle, PiggyBank, Wallet, type LucideIcon } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import AccountMenu from './AccountMenu'

interface NavItem {
  to: string
  label: string
  Icon: LucideIcon
  end?: boolean
  /** Nút nổi ở giữa thanh dưới (trên điện thoại). */
  primary?: boolean
}

const USER_NAV: NavItem[] = [
  { to: '/overview', label: 'Tổng quan', Icon: LayoutGrid },
  { to: '/transactions', label: 'Giao dịch', Icon: ArrowLeftRight },
  { to: '/chat', label: 'Trợ lý', Icon: MessageCircle, primary: true },
  { to: '/budgets', label: 'Ngân sách', Icon: PiggyBank },
  { to: '/wallets', label: 'Ví', Icon: Wallet },
]

const ADMIN_NAV: NavItem[] = [
  { to: '/admin', label: 'Tổng quan', Icon: Gauge, end: true },
  { to: '/chat', label: 'Thử chatbot', Icon: MessageCircle, primary: true },
  { to: '/admin/knowledge', label: 'Kho tri thức', Icon: BookOpen },
]

/**
 * Khung sau khi đăng nhập, bố trí như ứng dụng di động:
 * máy tính — thanh dọc hẹp bên trái (biểu tượng + nhãn ngắn, tài khoản ở đáy);
 * điện thoại — thanh dưới cùng, Trợ lý là nút nổi ở giữa, tài khoản nằm cạnh tiêu đề trang (PageHeader).
 */
export default function AppShell() {
  const { user } = useAuth()
  const items = user?.role === 'ADMIN' ? ADMIN_NAV : USER_NAV

  return (
    <div className="flex h-full flex-col md:flex-row">
      <nav aria-label="Điều hướng chính" className="hidden w-[84px] shrink-0 flex-col items-center border-r border-stone-200 bg-white py-3 md:flex">
        <span className="mb-4 grid size-9 place-items-center rounded-xl bg-ink text-sm font-semibold text-white" aria-hidden>
          ₫
        </span>
        <ul className="flex flex-1 flex-col gap-1">
          {items.map((item) => (
            <li key={item.to}>
              <RailLink item={item} />
            </li>
          ))}
        </ul>
        <AccountMenu placement="rail" />
      </nav>

      <main className="min-h-0 flex-1">
        <Outlet />
      </main>

      <nav
        aria-label="Điều hướng chính"
        className="shrink-0 border-t border-stone-200 bg-white/95 pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden"
      >
        <ul className="mx-auto flex h-16 max-w-md items-stretch justify-around px-2">
          {items.map((item) => (
            <li key={item.to} className="flex flex-1 justify-center">
              <TabLink item={item} />
            </li>
          ))}
        </ul>
      </nav>
    </div>
  )
}

/** Thanh dọc (máy tính): ô vuông bo góc, trang hiện tại có nền xám. */
function RailLink({ item: { to, label, Icon, end } }: { item: NavItem }) {
  return (
    <NavLink
      to={to}
      end={end}
      title={label}
      className={({ isActive }) =>
        `flex w-[70px] flex-col items-center gap-1 rounded-xl px-1 py-2 text-[11px] leading-tight transition-colors ${
          isActive ? 'bg-stone-100 font-medium text-ink' : 'text-stone-500 hover:bg-stone-50 hover:text-ink'
        }`
      }
    >
      <Icon size={20} strokeWidth={1.75} aria-hidden />
      <span className="w-full text-center text-balance">{label}</span>
    </NavLink>
  )
}

/** Thanh dưới (điện thoại): biểu tượng + nhãn; Trợ lý là nút tròn nổi. */
function TabLink({ item: { to, label, Icon, end, primary } }: { item: NavItem }) {
  if (primary) {
    return (
      <NavLink to={to} end={end} aria-label={label} className="flex flex-col items-center justify-center">
        {({ isActive }) => (
          <span
            className={`-mt-5 grid size-13 place-items-center rounded-full bg-ink text-white shadow-lg ring-4 ring-white transition-transform duration-150 active:scale-95 ${
              isActive ? '' : 'opacity-95'
            }`}
          >
            <Icon size={22} strokeWidth={1.9} aria-hidden />
          </span>
        )}
      </NavLink>
    )
  }
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        `flex w-full flex-col items-center justify-center gap-0.5 text-[11px] transition-colors ${
          isActive ? 'font-medium text-ink' : 'text-stone-400'
        }`
      }
    >
      <Icon size={21} strokeWidth={1.75} aria-hidden />
      {label}
    </NavLink>
  )
}
