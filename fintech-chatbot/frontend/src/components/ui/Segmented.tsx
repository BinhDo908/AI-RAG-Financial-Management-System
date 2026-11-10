interface Props<T extends string> {
  value: T
  options: { value: T; label: string }[]
  onChange: (value: T) => void
  size?: 'sm' | 'md'
  label?: string
}

/** Nhóm nút chọn một (Chi / Thu / Chuyển tiền, khoảng thời gian…). */
export default function Segmented<T extends string>({ value, options, onChange, size = 'md', label }: Props<T>) {
  return (
    <div className="inline-flex shrink-0 rounded-lg bg-stone-100 p-0.5" role="radiogroup" aria-label={label}>
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          role="radio"
          aria-checked={value === o.value}
          onClick={() => onChange(o.value)}
          className={`rounded-md whitespace-nowrap transition-colors ${size === 'sm' ? 'px-2.5 py-1 text-xs' : 'px-3 py-1.5 text-sm'} ${
            value === o.value ? 'bg-white font-medium text-ink shadow-sm' : 'text-stone-500 hover:text-ink'
          }`}
        >
          {o.label}
        </button>
      ))}
    </div>
  )
}
