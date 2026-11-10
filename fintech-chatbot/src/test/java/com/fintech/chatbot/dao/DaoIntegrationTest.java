package com.fintech.chatbot.dao;

import com.fintech.chatbot.dao.impl.CategoryDaoImpl;
import com.fintech.chatbot.dao.impl.UserDaoImpl;
import com.fintech.chatbot.dao.impl.WalletDaoImpl;
import com.fintech.chatbot.entity.Category;
import com.fintech.chatbot.entity.User;
import com.fintech.chatbot.entity.Wallet;
import com.fintech.chatbot.entity.enums.CategoryType;
import com.fintech.chatbot.entity.enums.Role;
import com.fintech.chatbot.entity.enums.UserStatus;
import com.fintech.chatbot.entity.enums.WalletType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra DAO Hibernate trên H2 + migration Flyway thật.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UserDaoImpl.class, WalletDaoImpl.class, CategoryDaoImpl.class})
class DaoIntegrationTest {

    @Autowired
    private UserDao userDao;
    @Autowired
    private WalletDao walletDao;
    @Autowired
    private CategoryDao categoryDao;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = userDao.save(user("alice@example.com", "Alice Nguyen"));
        bob = userDao.save(user("bob@example.com", "Bob Tran"));
    }

    @Test
    void findByEmailIsCaseInsensitive() {
        assertThat(userDao.findByEmail("ALICE@example.com")).map(User::getId).contains(alice.getId());
        assertThat(userDao.existsByEmail(" Bob@Example.com ")).isTrue();
        assertThat(userDao.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void searchFiltersByKeywordAndStatus() {
        bob.setStatus(UserStatus.LOCKED);
        userDao.flush();

        var byName = userDao.search(new UserDao.SearchCriteria("nguyen", null, null), 0, 10);
        assertThat(byName).extracting(User::getEmail).containsExactly("alice@example.com");

        var locked = new UserDao.SearchCriteria(null, UserStatus.LOCKED, null);
        assertThat(userDao.search(locked, 0, 10)).extracting(User::getEmail).containsExactly("bob@example.com");
        assertThat(userDao.countSearch(locked)).isEqualTo(1);
    }

    @Test
    void userOwnedQueriesNeverReturnOtherUsersData() {
        Wallet aliceWallet = walletDao.save(wallet(alice.getId(), "Tiền mặt"));
        walletDao.save(wallet(bob.getId(), "Tiền mặt"));

        assertThat(walletDao.findByIdAndUserId(aliceWallet.getId(), alice.getId())).isPresent();
        assertThat(walletDao.findByIdAndUserId(aliceWallet.getId(), bob.getId())).isEmpty();
        assertThat(walletDao.findAllByUserId(alice.getId())).hasSize(1);
        assertThat(walletDao.countByUserId(bob.getId())).isEqualTo(1);
        assertThat(walletDao.existsByUserIdAndName(alice.getId(), "tiền MẶT", null)).isTrue();
        assertThat(walletDao.existsByUserIdAndName(alice.getId(), "Tiền mặt", aliceWallet.getId())).isFalse();
    }

    @Test
    void defaultCategoriesAreSeededAndVisibleWithOwnCategories() {
        List<Category> defaults = categoryDao.findDefaults(CategoryType.EXPENSE, false);
        assertThat(defaults).extracting(Category::getName).contains("Ăn uống", "Di chuyển");

        Category own = new Category();
        own.setUserId(alice.getId());
        own.setName("Cà phê");
        own.setType(CategoryType.EXPENSE);
        categoryDao.save(own);

        assertThat(categoryDao.findVisibleForUser(alice.getId(), CategoryType.EXPENSE))
                .extracting(Category::getName).contains("Ăn uống", "Cà phê");
        assertThat(categoryDao.findVisibleForUser(bob.getId(), CategoryType.EXPENSE))
                .extracting(Category::getName).doesNotContain("Cà phê");

        assertThat(categoryDao.existsByName(null, CategoryType.EXPENSE, "ăn UỐNG", null)).isTrue();
        assertThat(categoryDao.existsByName(alice.getId(), CategoryType.EXPENSE, "cà phê", null)).isTrue();
        assertThat(categoryDao.existsByName(bob.getId(), CategoryType.EXPENSE, "cà phê", null)).isFalse();
        assertThat(categoryDao.isInUse(own.getId())).isFalse();
    }

    private static User user(String email, String name) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setPasswordHash("hash");
        u.setRole(Role.USER);
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }

    private static Wallet wallet(Long userId, String name) {
        Wallet w = new Wallet();
        w.setUserId(userId);
        w.setName(name);
        w.setType(WalletType.CASH);
        w.setInitialBalance(BigDecimal.ZERO);
        w.setCurrentBalance(BigDecimal.ZERO);
        return w;
    }
}
