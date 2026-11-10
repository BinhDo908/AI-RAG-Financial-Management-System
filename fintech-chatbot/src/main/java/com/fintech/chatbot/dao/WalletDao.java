package com.fintech.chatbot.dao;

import com.fintech.chatbot.entity.Wallet;

import java.util.Optional;

/**
 * DAO cho E-02 Wallet (UC-A01).
 */
public interface WalletDao extends UserOwnedDao<Wallet, Long> {

    boolean existsByUserIdAndName(Long userId, String name, Long excludeId);

    Optional<Wallet> findDefaultByUserId(Long userId);
}
