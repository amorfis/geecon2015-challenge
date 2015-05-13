package pl.allegro.promo.geecon2015.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.promo.geecon2015.domain.stats.FinancialStatisticsRepository;
import pl.allegro.promo.geecon2015.domain.stats.FinancialStats;
import pl.allegro.promo.geecon2015.domain.transaction.TransactionRepository;
import pl.allegro.promo.geecon2015.domain.transaction.UserTransaction;
import pl.allegro.promo.geecon2015.domain.transaction.UserTransactions;
import pl.allegro.promo.geecon2015.domain.user.User;
import pl.allegro.promo.geecon2015.domain.user.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class ReportGenerator {
    
    private final FinancialStatisticsRepository financialStatisticsRepository;
    
    private final UserRepository userRepository;
    
    private final TransactionRepository transactionRepository;

    @Autowired
    public ReportGenerator(FinancialStatisticsRepository financialStatisticsRepository,
                           UserRepository userRepository,
                           TransactionRepository transactionRepository) {
        this.financialStatisticsRepository = financialStatisticsRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public Report generate(ReportRequest request) {
        FinancialStats stats = financialStatisticsRepository
                .listUsersWithMinimalIncome(request.getMinimalIncome(), request.getUsersToCheck());

        Stream<ReportedUser> reportedUsers = stats.getUserIds().stream().map(
                uuid -> {
                    Optional<User> details = getUserDetails(uuid);

                    Optional<UserTransactions> transactions = getUserTransactions(uuid);
                    BigDecimal transAmount = transactions.isPresent() ? transactions.get()
                            .getTransactions()
                            .stream()
                            .map(UserTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            : null;

                    return new ReportedUser(
                            uuid,
                            details.orElseGet(() -> new User(uuid, "<failed>")).getName(),
                            transAmount);
                });

        Report report = new Report();
        reportedUsers.forEach(report::add);

        return report;
    }

    private Optional<UserTransactions> getUserTransactions(UUID uuid) {
        try {
            return Optional.of(transactionRepository.transactionsOf(uuid));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<User> getUserDetails(UUID uuid) {
        try {
            return Optional.of(userRepository.detailsOf(uuid));
        } catch(Exception e) {
            return Optional.empty();
        }
    }

}
