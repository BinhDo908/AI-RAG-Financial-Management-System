import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import type { ReactElement } from 'react'
import { useAuth } from './auth/AuthContext'
import AppShell from './components/AppShell'
import LoginPage from './pages/LoginPage'
import ChatPage from './pages/ChatPage'
import OverviewPage from './pages/finance/OverviewPage'
import TransactionsPage from './pages/finance/TransactionsPage'
import WalletsPage from './pages/finance/WalletsPage'
import BudgetsPage from './pages/finance/BudgetsPage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import KnowledgePage from './pages/admin/KnowledgePage'

function RequireAuth({ children }: { children: ReactElement }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) {
    return <div className="flex h-full items-center justify-center text-stone-400">Đang tải…</div>
  }
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return children
}

/** Trang quản trị: chỉ role ADMIN (backend cũng chặn /api/admin/** — đây chỉ là điều hướng giao diện). */
function RequireAdmin({ children }: { children: ReactElement }) {
  const { user } = useAuth()
  if (user?.role !== 'ADMIN') return <Navigate to="/overview" replace />
  return children
}

/** Trang quản lý tài chính cá nhân: Admin có khu vực riêng nên chuyển về bảng điều khiển. */
function RequireUser({ children }: { children: ReactElement }) {
  const { user } = useAuth()
  if (user?.role === 'ADMIN') return <Navigate to="/admin" replace />
  return children
}

/** Trang chủ theo role: User → Tổng quan, Admin → Bảng điều khiển. */
function Home() {
  const { user } = useAuth()
  return <Navigate to={user?.role === 'ADMIN' ? '/admin' : '/overview'} replace />
}

const userOnly = (page: ReactElement) => <RequireUser>{page}</RequireUser>
const adminOnly = (page: ReactElement) => <RequireAdmin>{page}</RequireAdmin>

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        <Route path="/" element={<Home />} />
        <Route path="/overview" element={userOnly(<OverviewPage />)} />
        <Route path="/transactions" element={userOnly(<TransactionsPage />)} />
        <Route path="/wallets" element={userOnly(<WalletsPage />)} />
        <Route path="/budgets" element={userOnly(<BudgetsPage />)} />
        {/* Trò chuyện dùng chung: Admin dùng để tự kiểm tra câu trả lời của chatbot */}
        <Route path="/chat" element={<ChatPage />} />
        <Route path="/chat/:conversationId" element={<ChatPage />} />
        <Route path="/admin" element={adminOnly(<AdminDashboardPage />)} />
        <Route path="/admin/knowledge" element={adminOnly(<KnowledgePage />)} />
        <Route path="*" element={<Home />} />
      </Route>
    </Routes>
  )
}
