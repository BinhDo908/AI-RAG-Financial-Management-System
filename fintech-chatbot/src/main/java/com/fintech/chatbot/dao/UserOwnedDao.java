package com.fintech.chatbot.dao;

import java.util.List;
import java.util.Optional;

/**
 * DAO cho entity thuộc về một user. Service nghiệp vụ phải dùng các phương thức có {@code userId}
 * thay vì {@link #findById} để không bao giờ đọc nhầm dữ liệu của người khác (BR-05, NFR-04).
 */
public interface UserOwnedDao<T, ID> extends GenericDao<T, ID> {

    Optional<T> findByIdAndUserId(ID id, Long userId);

    List<T> findAllByUserId(Long userId);

    long countByUserId(Long userId);
}
