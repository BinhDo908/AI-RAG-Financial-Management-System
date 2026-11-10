/** idle: đang chờ · thinking: AI đang nghĩ · writing: đang ra chữ · done: vừa trả lời xong. */
export type MascotMood = 'idle' | 'thinking' | 'writing' | 'done'

/** Muốn đổi ảnh (kể cả sang .gif): đặt file vào `public/mascot/` rồi sửa đường dẫn ở đây. */
const MASCOT_IMAGES: Record<MascotMood, string> = {
  idle: '/mascot/idle.webp',
  thinking: '/mascot/thinking.webp',
  writing: '/mascot/writing.webp',
  done: '/mascot/done.webp',
}

const MOODS = Object.keys(MASCOT_IMAGES) as MascotMood[]

/** Nhân vật đại diện: tải sẵn cả 4 ảnh rồi chỉ hiện ảnh đúng trạng thái, nên đổi trạng thái không bị nháy. */
export default function Mascot({ mood, message, size = 'lg' }: { mood: MascotMood; message?: string; size?: 'sm' | 'lg' }) {
  return (
    <div className="flex flex-col items-center gap-2">
      {message && (
        <p key={message} role="status" className="mascot-bubble max-w-[260px] rounded-2xl border border-stone-200 bg-white px-3.5 py-1.5 text-center text-sm text-stone-700">
          {message}
        </p>
      )}
      <div className={`relative ${size === 'lg' ? 'size-32 lg:size-40' : 'size-14'}`}>
        {MOODS.map((m) => (
          <img key={m} src={MASCOT_IMAGES[m]} alt="" draggable={false} className={`absolute inset-0 size-full object-contain ${m === mood ? '' : 'invisible'}`} />
        ))}
      </div>
    </div>
  )
}
