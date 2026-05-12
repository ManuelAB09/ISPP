package es.us.meerkat.backend.entity.subscriptions;

/**
 * Planes individuales disponibles para un usuario.
 *
 * <p>Cada plan tiene un nivel de jerarquía: {@code FREE < PREMIUM < PRO}. Esta jerarquía se usa
 * para garantizar que un usuario solo tenga un único plan activo y para resolver conflictos cuando
 * coexisten varias suscripciones (se conserva siempre la de mayor nivel).
 */
public enum TipoPlan {
    FREE(0),
    PREMIUM(1),
    PRO(2);

    private final int nivel;

    TipoPlan(int nivel) {
        this.nivel = nivel;
    }

    /**
     * Devuelve el nivel de jerarquía del plan.
     *
     * @return nivel (0 = FREE, 1 = PREMIUM, 2 = PRO)
     */
    public int getNivel() {
        return nivel;
    }

    /**
     * Indica si este plan tiene una jerarquía estrictamente superior a {@code otro}.
     *
     * @param otro plan con el que comparar (puede ser {@code null}, en cuyo caso se considera el
     *     nivel más bajo)
     * @return {@code true} si este plan es de mayor nivel que {@code otro}
     */
    public boolean esSuperiorA(TipoPlan otro) {
        return otro == null || this.nivel > otro.nivel;
    }
}
