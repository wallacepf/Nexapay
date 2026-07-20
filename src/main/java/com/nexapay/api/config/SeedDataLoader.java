package com.nexapay.api.config;

import com.nexapay.api.dto.AmountRequest;
import com.nexapay.api.dto.CreateCustomerRequest;
import com.nexapay.api.dto.OpenAccountRequest;
import com.nexapay.api.model.Account;
import com.nexapay.api.model.AccountType;
import com.nexapay.api.model.Customer;
import com.nexapay.api.service.BankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * Populates the in-memory store with demo data on startup so the API returns
 * something useful before any requests are made. Disable with
 * {@code nexapay.seed.enabled=false}.
 *
 * <p>All data is created through {@link BankService} rather than written to the
 * repository directly, so seeded balances always agree with the seeded
 * transaction history.
 */
@Component
@ConditionalOnProperty(name = "nexapay.seed.enabled", havingValue = "true", matchIfMissing = true)
public class SeedDataLoader implements ApplicationRunner {

    public static final int TRANSACTIONS_PER_ACCOUNT = 10;

    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);

    // Fixed seed so every restart produces the same data, which keeps the
    // README examples and any manual testing predictable.
    private static final long RANDOM_SEED = 20260720L;

    private static final String CURRENCY = "USD";

    private static final List<String> CUSTOMER_NAMES = List.of(
            "Ava Carter",
            "Noah Bennett",
            "Mia Alvarez",
            "Liam Fitzgerald",
            "Sofia Nakamura",
            "Ethan Brooks",
            "Isabella Rossi",
            "Mason Okafor",
            "Charlotte Dubois",
            "Lucas Meyer"
    );

    private static final List<String> CREDIT_DESCRIPTIONS = List.of(
            "Payroll", "Refund", "Interest", "Cheque deposit", "Reimbursement"
    );

    private static final List<String> DEBIT_DESCRIPTIONS = List.of(
            "Groceries", "ATM cash", "Utilities", "Rent", "Fuel", "Subscription"
    );

    private final BankService bankService;

    public SeedDataLoader(BankService bankService) {
        this.bankService = bankService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Random random = new Random(RANDOM_SEED);

        for (int i = 0; i < CUSTOMER_NAMES.size(); i++) {
            String name = CUSTOMER_NAMES.get(i);
            Customer customer = bankService.createCustomer(new CreateCustomerRequest(name, emailFor(name)));

            // The opening deposit is always positive, so it records the first
            // of this account's transactions.
            Account account = bankService.openAccount(new OpenAccountRequest(
                    customer.getId(),
                    i % 2 == 0 ? AccountType.CHECKING : AccountType.SAVINGS,
                    amountBetween(random, 25_000, 500_000),
                    CURRENCY
            ));

            addRemainingTransactions(account, random);
        }

        log.info("Seeded {} customers, each with 1 account and {} transactions",
                CUSTOMER_NAMES.size(), TRANSACTIONS_PER_ACCOUNT);
    }

    private void addRemainingTransactions(Account account, Random random) {
        for (int i = 1; i < TRANSACTIONS_PER_ACCOUNT; i++) {
            BigDecimal amount = amountBetween(random, 1_000, 40_000);

            // Only withdraw what the account can cover, so seeding never trips
            // the insufficient-funds guard.
            if (random.nextBoolean() && account.getBalance().compareTo(amount) >= 0) {
                bankService.withdraw(account.getId(), new AmountRequest(amount, pick(random, DEBIT_DESCRIPTIONS)));
            } else {
                bankService.deposit(account.getId(), new AmountRequest(amount, pick(random, CREDIT_DESCRIPTIONS)));
            }
        }
    }

    private BigDecimal amountBetween(Random random, int minCents, int maxCents) {
        return BigDecimal.valueOf(minCents + random.nextInt(maxCents - minCents + 1), 2);
    }

    private String pick(Random random, List<String> values) {
        return values.get(random.nextInt(values.size()));
    }

    private String emailFor(String fullName) {
        return fullName.toLowerCase().replace(' ', '.') + "@example.com";
    }
}
