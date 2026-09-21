package com.genfer.pae;

/** Operaciones ordinales; no calcula diagnósticos ni decisiones clínicas. */
public final class CareEngine {
    private CareEngine() { }
    public static boolean validScore(int score) { return score >= 1 && score <= 5; }
    public static String comparison(int initial, int target, int actual, String status) {
        if ("No evaluable".equals(status)) return "No evaluable";
        if (!"Evaluado".equals(status) || !validScore(actual)) return "Pendiente de evaluación";
        if (!validScore(initial) || !validScore(target)) return "Meta pendiente de definir";
        if (actual >= target) return "Meta alcanzada";
        if (actual < initial) return "Retroceso · revisar el plan";
        if (actual > initial) return "En progreso · meta aún no alcanzada";
        return "Sin cambio · meta aún no alcanzada";
    }
    public static String formulation(String label, boolean risk, String factor, String evidence) {
        String f = factor.trim().isEmpty() ? "[seleccionar factor]" : factor;
        if (risk) return label + ". Factores de riesgo propuestos: " + f + ".";
        return label + " relacionado con " + f + ", manifestado por "
                + (evidence.trim().isEmpty() ? "[seleccionar hallazgos]" : evidence) + ".";
    }
    public static int scoreColor(int score) {
        switch (score) {
            case 1: return 0xFFF9E6E4;
            case 2: return 0xFFFAECDD;
            case 3: return 0xFFF8F2D1;
            case 4: return 0xFFEDF1D9;
            case 5: return 0xFFDEEEE3;
            default: return 0xFFEDF0F0;
        }
    }
}
