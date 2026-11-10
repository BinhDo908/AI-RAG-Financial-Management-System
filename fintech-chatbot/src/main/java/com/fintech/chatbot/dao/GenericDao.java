package com.fintech.chatbot.dao;

import java.util.List;
import java.util.Optional;

/**
 * Thao tác CRUD chung cho mọi DAO.
 *
 * @param <T>  kiểu entity
 * @param <ID> kiểu khóa chính
 */
public interface GenericDao<T, ID> {

    Optional<T> findById(ID id);

    /** @param page bắt đầu từ 0 */
    List<T> findAll(int page, int size);

    long count();

    /** Thêm mới nếu chưa có id, ngược lại cập nhật. Trả về entity được quản lý. */
    T save(T entity);

    void delete(T entity);

    boolean deleteById(ID id);

    void flush();
}
