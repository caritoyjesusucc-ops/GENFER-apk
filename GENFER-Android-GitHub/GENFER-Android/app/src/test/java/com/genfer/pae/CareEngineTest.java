package com.genfer.pae;

import org.junit.Test;
import static org.junit.Assert.*;

public class CareEngineTest {
    @Test public void scoreRangeIsOneToFive() {
        assertFalse(CareEngine.validScore(0));
        assertTrue(CareEngine.validScore(1));
        assertTrue(CareEngine.validScore(5));
        assertFalse(CareEngine.validScore(6));
    }
    @Test public void absentScoresArePendingNotZero() {
        assertEquals("Pendiente de evaluación",CareEngine.comparison(2,5,0,"Pendiente"));
        assertEquals("Pendiente de evaluación",CareEngine.comparison(2,5,0,"Evaluado"));
        assertEquals("Meta pendiente de definir",CareEngine.comparison(0,5,3,"Evaluado"));
    }
    @Test public void comparesGoalAndTrend() {
        assertEquals("Meta alcanzada",CareEngine.comparison(2,5,5,"Evaluado"));
        assertEquals("Meta alcanzada",CareEngine.comparison(2,4,5,"Evaluado"));
        assertEquals("En progreso · meta aún no alcanzada",CareEngine.comparison(2,5,4,"Evaluado"));
        assertEquals("Sin cambio · meta aún no alcanzada",CareEngine.comparison(2,5,2,"Evaluado"));
        assertEquals("Retroceso · revisar el plan",CareEngine.comparison(2,5,1,"Evaluado"));
    }
    @Test public void nonEvaluableNeverBecomesAchieved() {
        assertEquals("No evaluable",CareEngine.comparison(2,5,5,"No evaluable"));
    }
    @Test public void riskDoesNotIncludeManifestations() {
        String risk=CareEngine.formulation("Riesgo de shock",true,"Factor de simulación","dato residual");
        assertFalse(risk.contains("manifestado por"));assertFalse(risk.contains("dato residual"));
        assertTrue(risk.contains("Factor de simulación"));
    }
    @Test public void actualDiagnosisIncludesSelectedEvidence() {
        String pes=CareEngine.formulation("Deterioro de la movilidad física",false,"factor propuesto","Hemiparesia derecha");
        assertEquals("Deterioro de la movilidad física relacionado con factor propuesto, manifestado por Hemiparesia derecha.",pes);
    }
    @Test public void missingScoreHasNeutralColor() {
        assertNotEquals(CareEngine.scoreColor(0),CareEngine.scoreColor(1));
        assertNotEquals(CareEngine.scoreColor(0),CareEngine.scoreColor(5));
        assertNotEquals(CareEngine.scoreColor(4),CareEngine.scoreColor(5));
    }
}
