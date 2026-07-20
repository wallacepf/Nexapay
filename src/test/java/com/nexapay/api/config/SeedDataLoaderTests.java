package com.nexapay.api.config;

import com.nexapay.api.model.Account;
import com.nexapay.api.model.Customer;
import com.nexapay.api.model.Transaction;
import com.nexapay.api.model.TransactionType;
import com.nexapay.api.service.BankService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SeedDataLoaderTests {

    @Autowired
    private BankService bankService;

    @Test
    void seedsTenCustomersEachWithOneAccount() {
        List<Customer> customers = bankService.listCustomers();

        assertThat(customers).hasSize(10);
        assertThat(customers).extracting(Customer::getEmail).doesNotHaveDuplicates();

        for (Customer customer : customers) {
            assertThat(bankService.listAccountsByCustomer(customer.getId())).hasSize(1);
        }
    }

    @Test
    void seedsTenTransactionsForEveryAccount() {
        for (Account account : seededAccounts()) {
            assertThat(bankService.listTransactions(account.getId()))
                    .as("transactions for account %d", account.getId())
                    .hasSize(SeedDataLoader.TRANSACTIONS_PER_ACCOUNT);
        }
    }

    @Test
    void seededBalancesMatchTransactionHistory() {
        for (Account account : seededAccounts()) {
            BigDecimal expected = BigDecimal.ZERO.setScale(2);

            for (Transaction transaction : bankService.listTransactions(account.getId())) {
                boolean credit = transaction.getType() == TransactionType.DEPOSIT
                        || transaction.getType() == TransactionType.TRANSFER_IN;
                expected = credit
                        ? expected.add(transaction.getAmount())
                        : expected.subtract(transaction.getAmount());
            }

            assertThat(account.getBalance())
                    .as("balance for account %d", account.getId())
                    .isEqualByComparingTo(expected);
        }
    }

    @Test
    void seededBalancesAreNeverNegative() {
        for (Account account : seededAccounts()) {
            assertThat(account.getBalance())
                    .as("balance for account %d", account.getId())
                    .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
    }

    private List<Account> seededAccounts() {
        return bankService.listCustomers().stream()
                .flatMap(customer -> bankService.listAccountsByCustomer(customer.getId()).stream())
                .toList();
    }
}
