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
 * Frontieres verifiees au build (DA-04).
 *
 * <p><b>Pourquoi ce fichier existe.</b> L'audit de l'existant ne constate pas une divergence
 * decidee : il constate une divergence <i>progressive</i>, installee sans que personne ne la
 * choisisse (constat C-02). Une frontiere seulement documentee subit exactement le meme sort — elle
 * s'erode a chaque echeance serree, un raccourci a la fois.
 *
 * <p>Chaque regle ci-dessous echoue au build, avec un message qui renvoie a la decision
 * d'architecture qu'elle protege. C'est la difference entre une convention — qui se contourne un
 * vendredi soir — et une contrainte.
 */
@AnalyzeClasses(
        packages = "com.ycyw.poc",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    private static final String DOMAINE = "..assistance.domain..";
    private static final String SERVICES = "..assistance.application..";

    // ---------------------------------------------------------------------------------------
    // Regle 1 — le domaine ne depend d'aucun element d'infrastructure (DA-05)
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
                                    + " infrastructure et survivre a un changement de framework");

    @ArchTest
    static final ArchRule le_domaine_ignore_les_adaptateurs =
            noClasses()
                    .that()
                    .resideInAnyPackage(DOMAINE, SERVICES)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..assistance.adapter..")
                    .because(
                            "DA-05 : les dependances pointent vers le domaine, jamais l'inverse."
                                    + " Le domaine appelle la persistance a travers un port, et"
                                    + " c'est la persistance qui depend de lui");

    // ---------------------------------------------------------------------------------------
    // Regle 2 — aucune classe du domaine n'expose d'accesseur en ecriture public (§ 4.5)
    // ---------------------------------------------------------------------------------------

    /**
     * Le controle porte sur la forme la plus repandue de fuite d'invariant : l'accesseur en
     * ecriture. Il est le garde-fou mecanique de la regle « aucun service ne modifie l'etat d'un
     * agregat autrement qu'en appelant une de ses methodes » — un service ne peut pas contourner
     * l'agregat s'il n'existe aucun moyen de lui imposer un etat.
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
                            "§ 4.5 : les invariants sont portes par les methodes de l'agregat ;"
                                    + " un accesseur en ecriture public permettrait de les"
                                    + " contourner");

    // ---------------------------------------------------------------------------------------
    // Regle 3 — aucune classe du domaine ne porte d'annotation de framework (§ 4.5)
    // ---------------------------------------------------------------------------------------

    @ArchTest
    static final ArchRule aucune_annotation_de_framework_dans_le_domaine =
            noClasses()
                    .that()
                    .resideInAnyPackage(DOMAINE, SERVICES)
                    .should(porterUneAnnotationDeFramework())
                    .because(
                            "§ 4.5 : une annotation de framework dans le domaine y fait entrer une"
                                    + " dependance technique par la porte de derriere");

    // ---------------------------------------------------------------------------------------
    // Regle 4 — aucun module n'atteint l'interieur d'un autre (DA-02, DA-04)
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
                            "DA-02 : un module n'accede qu'au contrat publie d'un autre — ici"
                                    + " IdentityApi —, jamais a ses classes internes");

    // ---------------------------------------------------------------------------------------
    // Regle 5 — aucune requete ne franchit une frontiere de schema (§ 6.1)
    // ---------------------------------------------------------------------------------------

    /**
     * Le cloisonnement par schema n'a d'effet que si chaque table declare le sien. Une entite sans
     * schema explicite atterrirait dans le schema par defaut de la connexion, et la frontiere
     * deviendrait invisible — donc infranchissable a personne.
     */
    @ArchTest
    static final ArchRule chaque_entite_declare_le_schema_de_son_module =
            ArchRuleDefinition.classes()
                    .that(sontDesEntitesPersistantes())
                    .should(declarerLeSchemaDeLeurModule())
                    .because(
                            "§ 6.1 : le cloisonnement par schema materialise la frontiere de"
                                    + " module et permet de la verifier");

    // ---------------------------------------------------------------------------------------
    // Conditions personnalisees
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
        return new DescribedPredicate<>("sont des entites persistantes") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.isAnnotatedWith(Table.class);
            }
        };
    }

    private static ArchCondition<JavaClass> declarerLeSchemaDeLeurModule() {
        return new ArchCondition<>("declarer le schema de leur module") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String schema = item.getAnnotationOfType(Table.class).schema();
                String attendu = item.getPackageName().contains(".assistance.") ? "assistance" : "identity";
                if (!attendu.equals(schema)) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    item,
                                    item.getName()
                                            + " declare le schema « "
                                            + schema
                                            + " » alors que son module impose « "
                                            + attendu
                                            + " »"));
                }
            }
        };
    }
}
