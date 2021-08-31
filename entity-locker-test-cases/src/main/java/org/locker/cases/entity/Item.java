package org.locker.cases.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Getter
@Setter
public class Item {
    @Id
    private Integer id;

    private Integer productId;

    private Integer quantity;
}
