package com.fintech.chatbot.mapper;

import com.fintech.chatbot.dto.response.AiDataPermissionResponse;
import com.fintech.chatbot.dto.response.ConversationResponse;
import com.fintech.chatbot.dto.response.MessageResponse;
import com.fintech.chatbot.dto.response.NotificationResponse;
import com.fintech.chatbot.dto.response.RecurringRuleResponse;
import com.fintech.chatbot.dto.response.TransactionResponse;
import com.fintech.chatbot.dto.response.WalletResponse;
import com.fintech.chatbot.entity.AiDataPermission;
import com.fintech.chatbot.entity.Conversation;
import com.fintech.chatbot.entity.Message;
import com.fintech.chatbot.entity.Notification;
import com.fintech.chatbot.entity.RecurringRule;
import com.fintech.chatbot.entity.Transaction;
import com.fintech.chatbot.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Entity → DTO cho các module tài chính. Tên ví / danh mục lấy qua quan hệ (cần nằm trong transaction).
 */
@Mapper
public interface FinanceMapper {

    WalletResponse toResponse(Wallet wallet);

    List<WalletResponse> toWalletResponses(List<Wallet> wallets);

    @Mapping(target = "walletName", source = "wallet.name")
    @Mapping(target = "toWalletName", source = "toWallet.name")
    @Mapping(target = "categoryName", source = "category.name")
    TransactionResponse toResponse(Transaction transaction);

    List<TransactionResponse> toTransactionResponses(List<Transaction> transactions);

    @Mapping(target = "walletName", source = "wallet.name")
    @Mapping(target = "categoryName", source = "category.name")
    RecurringRuleResponse toResponse(RecurringRule rule);

    NotificationResponse toResponse(Notification notification);

    ConversationResponse toResponse(Conversation conversation);

    MessageResponse toResponse(Message message);

    AiDataPermissionResponse toResponse(AiDataPermission permission);
}
