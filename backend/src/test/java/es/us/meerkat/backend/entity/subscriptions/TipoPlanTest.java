package es.us.meerkat.backend.entity.subscriptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Jerarquía de planes individuales (TipoPlan)")
class TipoPlanTest {

    @Test
    @DisplayName("Los niveles siguen la jerarquía FREE < PREMIUM < PRO")
    void nivelesSiguenLaJerarquiaFreePremiumPro() {
        assertThat(TipoPlan.FREE.getNivel()).isLessThan(TipoPlan.PREMIUM.getNivel());
        assertThat(TipoPlan.PREMIUM.getNivel()).isLessThan(TipoPlan.PRO.getNivel());
    }

    @Test
    @DisplayName("esSuperiorA compara correctamente la jerarquía entre planes")
    void esSuperiorAComparaJerarquiaCorrectamente() {
        assertThat(TipoPlan.PRO.esSuperiorA(TipoPlan.PREMIUM)).isTrue();
        assertThat(TipoPlan.PRO.esSuperiorA(TipoPlan.FREE)).isTrue();
        assertThat(TipoPlan.PREMIUM.esSuperiorA(TipoPlan.FREE)).isTrue();

        assertThat(TipoPlan.PREMIUM.esSuperiorA(TipoPlan.PRO)).isFalse();
        assertThat(TipoPlan.FREE.esSuperiorA(TipoPlan.FREE)).isFalse();

        assertThat(TipoPlan.PREMIUM.esSuperiorA(null)).isTrue();
    }
}
