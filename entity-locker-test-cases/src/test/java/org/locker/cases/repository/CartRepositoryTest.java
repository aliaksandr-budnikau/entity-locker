package org.locker.cases.repository;

import org.junit.jupiter.api.Test;
import org.locker.cases.ContextConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;


@DataJpaTest
@ContextConfiguration(classes = ContextConfig.class)
public class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Test
    public void test() {
        cartRepository.findAll();
    }
}
