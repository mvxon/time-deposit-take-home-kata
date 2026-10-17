package org.ikigaidigital.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@Import({PostgresContainerConfiguration.class, TimeDepositFixtures.class})
@TestPropertySource(properties = "time-deposit.balance-update.page-size=2")
public @interface IntegrationTest {}
