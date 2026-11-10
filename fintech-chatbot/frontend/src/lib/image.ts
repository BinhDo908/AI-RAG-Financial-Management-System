/**
 * Thu nhỏ ảnh hóa đơn trước khi gửi: cạnh dài tối đa {@code maxSide}px, JPEG chất lượng 0,85.
 * Ảnh chụp điện thoại 4–8 MB còn ~200–500 KB, vẫn đủ nét để đọc giá tiền, gửi nhanh hơn và rẻ hơn khi gọi AI.
 * Trình duyệt không giải mã được (ví dụ HEIC ngoài Safari) thì gửi nguyên ảnh để server báo lỗi định dạng.
 */
export async function prepareReceiptImage(file: File, maxSide = 1600): Promise<Blob> {
  try {
    const bitmap = await createImageBitmap(file)
    const scale = Math.min(1, maxSide / Math.max(bitmap.width, bitmap.height))
    const canvas = document.createElement('canvas')
    canvas.width = Math.round(bitmap.width * scale)
    canvas.height = Math.round(bitmap.height * scale)
    canvas.getContext('2d')?.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    bitmap.close()
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.85))
    return blob ?? file
  } catch {
    return file
  }
}
