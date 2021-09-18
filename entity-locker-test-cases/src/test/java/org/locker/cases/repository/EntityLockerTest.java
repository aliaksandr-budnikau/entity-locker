package org.locker.cases.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locker.EntityLocker;
import org.locker.GlobalEntityLocker;
import org.locker.cases.ContextConfig;
import org.locker.cases.entity.Book;
import org.locker.cases.entity.BookId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

@Slf4j
@DataJpaTest
@ContextConfiguration(classes = ContextConfig.class)
public class EntityLockerTest {

    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private GlobalEntityLocker<BookId> globalBookLocker;
    @Autowired
    @Qualifier("escalationBookLocker")
    private EntityLocker<BookId> bookLocker;

    private BookId bookId1;
    private BookId bookId2;

    @BeforeEach
    void setUp() {
        bookId1 = new BookId("Crime and Punishment", "en");
        bookId2 = new BookId("Schuld und Sühne", "ger");

        bookRepository.save(new Book(bookId1, bookId1.getTitle()));
        bookRepository.save(new Book(bookId2, bookId2.getTitle()));
    }

    @Test
    public void test() {
        bookRepository.findById(bookId1).ifPresent(book -> {
            bookLocker.lock(book.getBookId());
            log.info("locked {}", book.getBookId());
            try {
                log.info("Did some work");
            } finally {
                bookLocker.unlock(book.getBookId());
                log.info("unlocked {}", book.getBookId());
            }
        });

        bookRepository.findById(bookId2).ifPresent(book -> {
            try {
                globalBookLocker.lock();
                log.info("globally locked books");
                log.info("Did some work");
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            } finally {
                globalBookLocker.unlock();
                log.info("globally unlocked books");
            }
        });
    }
}
