package com.skillnet.web.dto.response.analytics;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentKpiDTO {

    /** Total de cursos comprados (matriculados), sin filtro de periodo. */
    private long purchasedCourses;

    /** Compras (matriculas) en el mes/año seleccionado. */
    private long purchasedInPeriod;

    /** Cursos completados (total acumulado). */
    private long completedCourses;

    /** Certificados obtenidos (completados acumulados en periodo). */
    private long certificates;

    /** Cursos activos (matriculados sin completar). */
    private long activeCourses;
}
