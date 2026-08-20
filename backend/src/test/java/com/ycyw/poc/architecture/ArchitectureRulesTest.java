package com.ycyw.poc.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import jakarta.persistence.Table;

/**
 * Frontières vérifiées au build (DA-04).
 *
 * <p><b>Pourquoi ce fichier existe.</b> L'audit de l'existant ne constate pas une divergence
 * décidée : il constate une divergence <i>progressive</i>, installée sans que personne ne la
 * choisisse (constat C-02). Une frontière seulement documentée subit exactement le même sort — elle
 * s'erode à chaque échéance serrée, un raccourci à la fois.
 *
 * <p>Chaque règle ci-dessous échoué au build, avec un message qui renvoie à la décision
 * d'architecture qu'elle protege. C'est la différence entre une convention — qui se contourne un
 * vendredi soir — et une contrainte.
 */
@AnalyzeClasses(
        packages = "com.ycyw.poc",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    private static final String DOMAINE = "..assistance.domain..";
    private static final String SERVICES = "..assistance.application..";

    // ---------------------------------------------------------------------------------------
    // Règle 1 — le domaine ne dépend d'aucun élément d'infrastructure (DA-05)
    // ---------------------------------------------------------------------------------------

    @ArchTest
    static final ArchRule le_domaine_ignore_le_framework =
            noClasses()
                    .that()
                    .resideInAnyPackage(DOMAINE, SERVICES)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.servlet..",
                            "org.hibernate..",
                            "com.fasterxml.jackson..")
                    .because(
                            "DA-05 : le domaine et les cas d'usage doivent rester testables sans"
                                    + " infrastructure et survivre à un changement de framework");

    @ArchTest
    static final ArchRule le_domaine_ignore_les_adaptateurs =
            noClasses()
                    .that()
                    .resideInAnyPackage(DOMAINE, SERVICES)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..assistance.adapter..")
                    .because(
                            "DA-05 : les dépendances pointent vers le domaine, jamais l'inverse."
                                    + " Le domaine appelle la persistance à travers un port, et"
                                    + " c'est la persistance qui dépend de lui");

    // ---------------------------------------------------------------------------------------
    // Règle 2 — aucune classe du domaine n'expose d'accesseur en écriture public (§ 5.4)
    // ---------------------------------------------------------------------------------------

    /**
     * Le contrôle porte sur la forme la plus répandue de fuite d'invariant : l'accesseur en
     * écriture. Il est le garde-fou mécanique de la règle « aucun service ne modifie l'état d'un
     * agrégat autrement qu'en appelant une de ses méthodes » — un service ne peut pas contourner
     * l'agrégat s'il n'existe aucun moyen de lui imposer un état.
     */
    @ArchTest
    static final ArchRule aucun_accesseur_en_ecriture_public_dans_le_domaine =
            methods()
                    .that()
                    .areDeclaredInClassesThat()
                    .resideInAPackage(DOMAINE)
                    .and()
                    .haveNameMatching("set[A-Z].*")
                    .should()
                    .notBePublic()
                    .because(
                            "§ 5.4 : les invariants sont portés par les méthodes de l'agrégat ;"
                                    + " un accesseur en écriture public permettrait de les"
                                    + " contourner");

    // ---------------------------------------------------------------------------------------
    // Règle 3 — aucune classe du domaine ne porte d'annotation de framework (§ 5.4)
    // ---------------------------------------------------------------------------------------

    @ArchTest
    static final ArchRule aucune_annotation_de_framework_dans_le_domaine =
            noClasses()
                    .that()
                    .resideInAnyPackage(DOMAINE, SERVICES)
                    .should(porterUneAnnotationDeFramework())
                    .because(
                            "§ 5.4 : une annotation de framework dans le domaine y fait entrer une"
                                    + " dépendance technique par la porte de derrière");

    // ---------------------------------------------------------------------------------------
    // Règle 4 — aucun module n'atteint l'intérieur d'un autre (DA-02, DA-04)
    // ---------------------------------------------------------------------------------------

    @ArchTest
    static final ArchRule les_modules_ne_se_traversent_pas =
            noClasses()
                    .that()
                    .resideInAPackage("..assistance..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..identity.internal..")
                    .because(
                            "DA-02 : un module n'accède qu'au contrat publié d'un autre — ici"
                                    + " IdentityApi —, jamais à ses classes internes");

    // ---------------------------------------------------------------------------------------
    // Règle 5 — aucune requête ne franchit une frontière de schéma (§ 7.1)
    // ---------------------------------------------------------------------------------------

    /**
     * Le cloisonnement par schéma n'a d'effet que si chaque table declare le sien. Une entité sans
     * schéma explicite atterrirait dans le schéma par défaut de la connexion, et la frontière
     * deviendrait invisible — donc infranchissable à personne.
     */
    @ArchTest
    static final ArchRule chaque_entite_declare_le_schema_de_son_module =
            ArchRuleDefinition.classes()
                    .that(sontDesEntitesPersistantes())
                    .should(declarerLeSchemaDeLeurModule())
                    .because(
                            "§ 7.1 : le cloisonnement par schéma matérialise la frontière de"
                                    + " module et permet de la vérifier");

    // ---------------------------------------------------------------------------------------
    // Conditions personnalisées
    // ---------------------------------------------------------------------------------------

    private static ArchCondition<JavaClass> porterUneAnnotationDeFramework() {
        return new ArchCondition<>("porter une annotation de framework") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getAnnotations().stream()
                        .filter(
                                annotation -> {
                                    String name = annotation.getRawType().getName();
                                    return name.startsWith("org.springframework")
                                            || name.startsWith("jakarta.persistence")
                                            || name.startsWith("com.fasterxml.jackson");
                                })
                        .forEach(
                                annotation ->
                                        events.add(
                                                SimpleConditionEvent.satisfied(
                                                        item,
                                                        item.getName()
                                                                + " porte "
                                                                + annotation
                                                                        .getRawType()
                                                                        .getName())));
            }
        };
    }

    private static DescribedPredicate<JavaClass> sontDesEntitesPersistantes() {
        return new DescribedPredicate<>("sont des entités persistantes") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.isAnnotatedWith(Table.class);
            }
        };
    }

    private static ArchCondition<JavaClass> declarerLeSchemaDeLeurModule() {
        return new ArchCondition<>("déclarer le schéma de leur module") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String schema = item.getAnnotationOfType(Table.class).schema();
                String attendu = item.getPackageName().contains(".assistance.") ? "assistance" : "identity";
                if (!attendu.equals(schema)) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    item,
                                    item.getName()
                                            + " déclare le schéma « "
                                            + schema
                                            + " » alors que son module impose « "
                                            + attendu
                                            + " »"));
                }
            }
        };
    }
}
