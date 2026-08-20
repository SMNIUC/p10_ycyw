package com.ycyw.poc.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Vérifie que les règles d'architecture détectent ce qu'elles prétendent détecter.
 *
 * <p><b>Pourquoi ce test existe.</b> « Aucune violation » peut signifier deux choses opposées : que
 * le code est sain, ou que le contrôle ne contrôle rien — un paquet mal orthographie, un prédicat
 * qui ne rencontre jamais d'élément. Un garde-fou dont personne n'a vérifie qu'il retient quelque
 * chose donne une fausse assurance, ce qui est pire que pas de garde-fou du tout.
 *
 * <p>Chaque règle est donc confrontee à une classe volontairement fautive, et doit échouer.
 */
@DisplayName("Les règles d'architecture sont effectives")
class ArchitectureRulesAreEffectiveTest {

    private static final JavaClasses DOMAINE_FAUTIF =
            new ClassFileImporter().importPackages("com.ycyw.poc.assistance.domain.fixture");

    private static final JavaClasses MODULE_FAUTIF =
            new ClassFileImporter().importPackages("com.ycyw.poc.assistance.fixture");

    @Test
    @DisplayName("les classes fautives sont bien présentes — sans quoi le test ne prouverait rien")
    void lesFixturesExistent() {
        assertThat(DOMAINE_FAUTIF).isNotEmpty();
        assertThat(MODULE_FAUTIF).isNotEmpty();
    }

    @Test
    @DisplayName("refuse une dépendance du domaine vers le framework")
    void dependanceVersLeFramework() {
        assertRuleRejects(ArchitectureRulesTest.le_domaine_ignore_le_framework, DOMAINE_FAUTIF);
    }

    @Test
    @DisplayName("refuse une dépendance du domaine vers un adaptateur")
    void dependanceVersUnAdaptateur() {
        assertRuleRejects(ArchitectureRulesTest.le_domaine_ignore_les_adaptateurs, DOMAINE_FAUTIF);
    }

    @Test
    @DisplayName("refuse une annotation de framework sur une classe du domaine")
    void annotationDeFramework() {
        assertRuleRejects(
                ArchitectureRulesTest.aucune_annotation_de_framework_dans_le_domaine,
                DOMAINE_FAUTIF);
    }

    @Test
    @DisplayName("refuse un accesseur en écriture public dans le domaine")
    void accesseurEnEcriture() {
        assertRuleRejects(
                ArchitectureRulesTest.aucun_accesseur_en_ecriture_public_dans_le_domaine,
                DOMAINE_FAUTIF);
    }

    @Test
    @DisplayName("refuse qu'un module atteigne l'intérieur d'un autre")
    void traverseeDeModule() {
        assertRuleRejects(ArchitectureRulesTest.les_modules_ne_se_traversent_pas, MODULE_FAUTIF);
    }

    @Test
    @DisplayName("refuse une table qui ne déclare pas le schéma de son module")
    void schemaNonDeclare() {
        assertRuleRejects(
                ArchitectureRulesTest.chaque_entite_declare_le_schema_de_son_module, MODULE_FAUTIF);
    }

    private static void assertRuleRejects(ArchRule rule, JavaClasses classesFautives) {
        assertThatThrownBy(() -> rule.check(classesFautives))
                .as("la règle « %s » doit refuser la classe fautive", rule.getDescription())
                .isInstanceOf(AssertionError.class);
    }
}
