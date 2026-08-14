package com.ycyw.poc.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifie que les regles d'architecture detectent ce qu'elles pretendent detecter.
 *
 * <p><b>Pourquoi ce test existe.</b> « Aucune violation » peut signifier deux choses opposees : que
 * le code est sain, ou que le controle ne controle rien — un paquet mal orthographie, un predicat
 * qui ne rencontre jamais d'element. Un garde-fou dont personne n'a verifie qu'il retient quelque
 * chose donne une fausse assurance, ce qui est pire que pas de garde-fou du tout.
 *
 * <p>Chaque regle est donc confrontee a une classe volontairement fautive, et doit echouer.
 */
@DisplayName("Les regles d'architecture sont effectives")
class ArchitectureRulesAreEffectiveTest {

    private static final JavaClasses DOMAINE_FAUTIF =
            new ClassFileImporter().importPackages("com.ycyw.poc.assistance.domain.fixture");

    private static final JavaClasses MODULE_FAUTIF =
            new ClassFileImporter().importPackages("com.ycyw.poc.assistance.fixture");

    @Test
    @DisplayName("les classes fautives sont bien presentes — sans quoi le test ne prouverait rien")
    void lesFixturesExistent() {
        assertThat(DOMAINE_FAUTIF).isNotEmpty();
        assertThat(MODULE_FAUTIF).isNotEmpty();
    }

    @Test
    @DisplayName("refuse une dependance du domaine vers le framework")
    void dependanceVersLeFramework() {
        assertRuleRejects(ArchitectureRulesTest.le_domaine_ignore_le_framework, DOMAINE_FAUTIF);
    }

    @Test
    @DisplayName("refuse une annotation de framework sur une classe du domaine")
    void annotationDeFramework() {
        assertRuleRejects(
                ArchitectureRulesTest.aucune_annotation_de_framework_dans_le_domaine,
                DOMAINE_FAUTIF);
    }

    @Test
    @DisplayName("refuse un accesseur en ecriture public dans le domaine")
    void accesseurEnEcriture() {
        assertRuleRejects(
                ArchitectureRulesTest.aucun_accesseur_en_ecriture_public_dans_le_domaine,
                DOMAINE_FAUTIF);
    }

    @Test
    @DisplayName("refuse qu'un module atteigne l'interieur d'un autre")
    void traverseeDeModule() {
        assertRuleRejects(ArchitectureRulesTest.les_modules_ne_se_traversent_pas, MODULE_FAUTIF);
    }

    @Test
    @DisplayName("refuse une table qui ne declare pas le schema de son module")
    void schemaNonDeclare() {
        assertRuleRejects(
                ArchitectureRulesTest.chaque_entite_declare_le_schema_de_son_module, MODULE_FAUTIF);
    }

    private static void assertRuleRejects(ArchRule rule, JavaClasses classesFautives) {
        assertThatThrownBy(() -> rule.check(classesFautives))
                .as("la regle « %s » doit refuser la classe fautive", rule.getDescription())
                .isInstanceOf(AssertionError.class);
    }
}
