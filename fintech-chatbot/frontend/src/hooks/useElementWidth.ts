import { useEffect, useRef, useState } from 'react'

/** Chiều rộng thực của một phần tử (biểu đồ SVG co giãn theo khung chứa). */
export function useElementWidth<T extends HTMLElement>() {
  const ref = useRef<T>(null)
  const [width, setWidth] = useState(0)
  useEffect(() => {
    const el = ref.current
    if (!el) return
    const observer = new ResizeObserver(([entry]) => setWidth(Math.floor(entry.contentRect.width)))
    observer.observe(el)
    return () => observer.disconnect()
  }, [])
  return { ref, width }
}
