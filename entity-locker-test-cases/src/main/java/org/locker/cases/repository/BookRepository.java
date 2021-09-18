package org.locker.cases.repository;

import org.locker.cases.entity.Book;
import org.locker.cases.entity.BookId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, BookId> {
}
