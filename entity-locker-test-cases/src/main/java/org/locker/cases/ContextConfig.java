package org.locker.cases;

import org.locker.BasicEntityLocker;
import org.locker.EntityLocker;
import org.locker.EscalationEntityLocker;
import org.locker.GlobalEntityLocker;
import org.locker.NoDeadLockEntityLocker;
import org.locker.cases.entity.BookId;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@EnableJpaRepositories
@EntityScan
public class ContextConfig {

    public static final int GLOBAL_LOCK_THRESHOLD = 5;

    @Bean
    public EntityLocker<BookId> basicBookLocker() {
        return new BasicEntityLocker<>();
    }

    @Bean
    public EntityLocker<BookId> noDeadLockBookLocker() {
        return new NoDeadLockEntityLocker<>(new BasicEntityLocker<>());
    }

    @Bean
    public EntityLocker<BookId> escalationBookLocker() {
        return new EscalationEntityLocker<>(new NoDeadLockEntityLocker<>(new BasicEntityLocker<>()), GLOBAL_LOCK_THRESHOLD);
        // OR return new EscalationEntityLocker<>(new BasicEntityLocker<>(), GLOBAL_LOCK_THRESHOLD);
    }

    @Bean
    public GlobalEntityLocker<BookId> globalBookLocker() {
        return new GlobalEntityLocker<>(new EscalationEntityLocker<>(new NoDeadLockEntityLocker<>(new BasicEntityLocker<>()), GLOBAL_LOCK_THRESHOLD));
    }
}
