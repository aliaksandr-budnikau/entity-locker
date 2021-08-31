package org.locker.cases.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.Set;

@Entity
@Getter
@Setter
public class Cart {
    @Id
    private int id;
    @OneToMany
    private Set<Item> items;
}
