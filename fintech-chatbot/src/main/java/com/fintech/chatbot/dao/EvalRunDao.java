package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.EvalRun;

import java.util.List;

/**
 * DAO cho kết quả chạy bộ đánh giá chatbot (UC-C03 AF-1).
 */
public interface EvalRunDao extends GenericDao<EvalRun, Long> {

    List<EvalRun> findRecent(int limit);
}
