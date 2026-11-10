package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.Category;
import com.fintech.chatbot.entity.enums.CategoryType;

import java.util.List;
import java.util.Optional;

/**
 * DAO cho E-03 Category. Danh mục mặc định có {@code userId = null}.
 */
public interface CategoryDao extends GenericDao<Category, Long> {

    /** UC-C06: danh mục mặc định; {@code type = null} lấy cả hai loại. */
    List<Category> findDefaults(CategoryType type, boolean includeHidden);

    Optional<Category> findDefaultById(Long id);

    /** Danh mục user được dùng: mặc định hoặc của chính user (kể cả đã ẩn). */
    Optional<Category> findAccessible(Long id, Long userId);

    /** Id danh mục con trực tiếp (ngân sách danh mục cha tính cả chi của danh mục con). */
    List<Long> findChildIds(Long parentId);

    /** UC-A03: danh mục user thấy được = mặc định chưa ẩn + danh mục riêng chưa ẩn. */
    List<Category> findVisibleForUser(Long userId, CategoryType type);

    /** Danh mục riêng của user (kể cả đã ẩn). */
    List<Category> findOwnedByUser(Long userId);

    /**
     * Kiểm tra trùng tên trong cùng phạm vi (userId, kể cả null) và cùng loại.
     *
     * @param excludeId bỏ qua chính danh mục đang sửa; null khi tạo mới
     */
    boolean existsByName(Long userId, CategoryType type, String name, Long excludeId);

    /** Danh mục đã được giao dịch, ngân sách, quy tắc định kỳ hoặc danh mục con tham chiếu (BR-10). */
    boolean isInUse(Long categoryId);
}
