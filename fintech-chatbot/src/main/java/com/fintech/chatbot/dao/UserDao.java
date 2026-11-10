package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.User;
import com.fintech.chatbot.entity.enums.Role;
import com.fintech.chatbot.entity.enums.UserStatus;

import java.util.List;
import java.util.Optional;

/**
 * DAO cho E-01 User.
 */
public interface UserDao extends GenericDao<User, Long> {

    /** So khớp email không phân biệt hoa thường. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** UC-C05: tìm theo email / họ tên, lọc theo trạng thái và vai trò (null = bỏ qua). */
    List<User> search(SearchCriteria criteria, int page, int size);

    long countSearch(SearchCriteria criteria);

    /** Xóa user; dữ liệu liên quan bị xóa theo ON DELETE CASCADE ở DB (UC-D03 AF-2). */
    void hardDelete(Long userId);

    record SearchCriteria(String keyword, UserStatus status, Role role) {
    }
}
