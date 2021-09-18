package org.locker.cases.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
@Getter
@EqualsAndHashCode(of = "bookId")
@AllArgsConstructor
@NoArgsConstructor
public class Book {
    @EmbeddedId
    private BookId bookId;
    private String description;
}
