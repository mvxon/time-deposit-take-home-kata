package org.ikigaidigital.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "org.ikigaidigital", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String[] DOMAIN = {"org.ikigaidigital", "org.ikigaidigital.domain.."};

    @ArchTest
    static final ArchRule layersDependInwards = onionArchitecture()
            .domainModels(DOMAIN)
            .applicationServices("org.ikigaidigital.application..")
            .adapter("web", "org.ikigaidigital.adapter.web..")
            .adapter("persistence", "org.ikigaidigital.adapter.persistence..")
            .ignoreDependency(resideInAPackage("org.ikigaidigital.config.."), alwaysTrue())
            .withOptionalLayers(true);

    @ArchTest
    static final ArchRule packagesAreFreeOfCycles =
            slices().matching("org.ikigaidigital.(**)").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule interestStrategiesDoNotDependOnSharedKataClasses = noClasses()
            .that()
            .resideInAPackage("org.ikigaidigital.domain.interest..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.ikigaidigital")
            .as(
                    "interest strategies do not depend on the root package, so the calculator can use them without a cycle");

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that()
            .resideInAnyPackage(DOMAIN)
            .and()
            .doNotHaveSimpleName("TimeDepositApplication")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.validation..", "org.hibernate..")
            .as("domain does not depend on Spring, JPA or validation frameworks");

    @ArchTest
    static final ArchRule persistenceDetailsStayInsideAdapter = noClasses()
            .that()
            .resideOutsideOfPackage("org.ikigaidigital.adapter.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.ikigaidigital.adapter.persistence..")
            .as("only the persistence adapter knows its JPA entities and repositories");

    @ArchTest
    static final ArchRule webDetailsStayInsideAdapter = noClasses()
            .that()
            .resideOutsideOfPackage("org.ikigaidigital.adapter.web..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.ikigaidigital.adapter.web..")
            .as("only the web adapter knows the generated API and DTOs");
}
