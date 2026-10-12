package com.genfer.pae;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** Catálogo local y modelo del borrador. Sin transmisión a servidores. */
public final class PlanRepository {
    public final JSONObject catalog;
    public PlanRepository(Context context) throws Exception {
        try (InputStream in = context.getAssets().open("catalogo.json")) {
            catalog = new JSONObject(read(in));
        }
    }
    public static String read(InputStream in) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) != -1) {
            bytes.write(buffer, 0, count);
            if (bytes.size() > 4 * 1024 * 1024) throw new IllegalStateException("Archivo demasiado grande");
        }
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }
    public static JSONObject obj(Object... pairs) {
        JSONObject result = new JSONObject();
        for (int i = 0; i < pairs.length; i += 2) put(result, (String) pairs[i], pairs[i + 1]);
        return result;
    }
    public static void put(JSONObject o, String key, Object value) {
        try { o.put(key, value); } catch (JSONException e) { throw new IllegalArgumentException(e); }
    }
    public static JSONArray array(JSONObject o, String key) {
        JSONArray a = o.optJSONArray(key);
        if (a == null) { a = new JSONArray(); put(o, key, a); }
        return a;
    }
    public static JSONObject map(JSONObject o, String key) {
        JSONObject value = o.optJSONObject(key);
        if (value == null) { value = new JSONObject(); put(o, key, value); }
        return value;
    }
    public static List<String> strings(JSONArray a) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) out.add(a.optString(i));
        return out;
    }
    public static List<String> keys(JSONObject o) {
        List<String> result = new ArrayList<>();
        Iterator<String> it = o.keys();
        while (it.hasNext()) result.add(it.next());
        return result;
    }
    public static String join(JSONArray a) { return String.join(", ", strings(a)); }
    public static boolean contains(JSONArray a, String value) { return strings(a).contains(value); }
    public static void toggle(JSONObject o, String key, String value, boolean checked) {
        List<String> list = strings(array(o, key));
        if (checked && !list.contains(value)) list.add(value);
        if (!checked) list.remove(value);
        put(o, key, new JSONArray(list));
    }
    public static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss XXX", Locale.US).format(new Date());
    }
    public static JSONObject newState() {
        return obj("version", 2, "caseId", "SIM-001", "date",
                new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()),
                "shift", "Mañana", "plans", new JSONObject());
    }
    public JSONObject condition(String id) {
        JSONArray cs = catalog.optJSONArray("conditions");
        for (int i = 0; i < cs.length(); i++) if (cs.optJSONObject(i).optString("id").equals(id)) return cs.optJSONObject(i);
        throw new IllegalArgumentException("Condición desconocida: " + id);
    }
    public JSONObject diagnosis(String id) {
        JSONArray cs = catalog.optJSONArray("conditions");
        for (int i = 0; i < cs.length(); i++) {
            JSONArray ds = cs.optJSONObject(i).optJSONArray("diagnoses");
            for (int j = 0; j < ds.length(); j++) if (ds.optJSONObject(j).optString("id").equals(id)) return ds.optJSONObject(j);
        }
        throw new IllegalArgumentException("Diagnóstico desconocido: " + id);
    }
    public JSONObject newPlan(String conditionId, String diagnosisId) {
        return obj("id", diagnosisId, "conditionId", conditionId, "factor", "", "moment", "",
                "sources", new JSONArray(), "evidence", new JSONArray(), "reviewed", false,
                "assessedAt", "", "priority", "Media", "note", "", "followup", "",
                "nocs", new JSONObject(), "nics", new JSONObject());
    }
    public JSONObject newOutcome(String name) {
        JSONObject definition = catalog.optJSONObject("outcomes").optJSONObject(name);
        String profile = definition == null ? "generic" : definition.optString("profile", "generic");
        return obj("profile", profile, "levels", 5, "scaleVersion", "GENFER-simple-5-v2",
                "baseline", 0, "target", 0, "actual", 0, "evaluationState", "Pendiente",
                "response", "", "evaluatedAt", "");
    }
    public static JSONObject newIntervention() {
        return obj("activities", new JSONArray(), "schedule", "", "responsible", "",
                "state", "Pendiente", "note", "", "recordedAt", "");
    }
    public static void resetEvaluation(JSONObject r) {
        put(r, "actual", 0); put(r, "evaluationState", "Pendiente");
        put(r, "response", ""); put(r, "evaluatedAt", "");
    }
    public static void resetExecution(JSONObject r) {
        put(r, "state", "Pendiente"); put(r, "note", ""); put(r, "recordedAt", "");
    }
    public static void invalidate(JSONObject p) { put(p, "reviewed", false); put(p, "assessedAt", ""); }
    public String formulation(JSONObject p) {
        JSONObject d = diagnosis(p.optString("id"));
        return CareEngine.formulation(d.optString("label"), "risk".equals(d.optString("type")),
                p.optString("factor"), join(array(p, "evidence")));
    }
    public String scoreText(JSONObject r, String key) {
        int v = r.optInt(key);
        if (!CareEngine.validScore(v)) return "Sin evaluar";
        return v + "/5 · " + anchors(r).optString(v - 1);
    }
    public JSONArray anchors(JSONObject r) { return catalog.optJSONObject("scales").optJSONArray(r.optString("profile")); }
    public static String comparison(JSONObject r) {
        return CareEngine.comparison(r.optInt("baseline"), r.optInt("target"), r.optInt("actual"), r.optString("evaluationState"));
    }
    public String goal(JSONObject r) {
        if (!CareEngine.validScore(r.optInt("baseline")) || !CareEngine.validScore(r.optInt("target")))
            return "Selecciona cómo recibes al paciente y a qué nivel quieres llevarlo.";
        return "Nivel al recibir: " + scoreText(r, "baseline") + "  →  Nivel esperado: " + scoreText(r, "target");
    }
    public List<String> diagnosisIssues(JSONObject p) {
        List<String> issues = new ArrayList<>();
        if (p.optString("factor").isEmpty()) issues.add("Selecciona el factor pertinente.");
        if (p.optString("moment").isEmpty() || array(p, "sources").length() == 0) issues.add("Selecciona momento y fuente de valoración.");
        if (!"risk".equals(diagnosis(p.optString("id")).optString("type")) && array(p, "evidence").length() == 0) issues.add("Selecciona hallazgos que sustenten el diagnóstico.");
        if (!p.optBoolean("reviewed")) issues.add("Revisa la formulación del ejercicio.");
        return issues;
    }
    public List<String> planIssues(JSONObject p) {
        List<String> issues = diagnosisIssues(p);
        JSONObject outcomes = map(p, "nocs"), interventions = map(p, "nics");
        if (outcomes.length() == 0) issues.add("Selecciona al menos un resultado NOC propuesto.");
        if (interventions.length() == 0) issues.add("Selecciona al menos una intervención NIC propuesta.");
        for (String name : keys(outcomes)) {
            JSONObject r = outcomes.optJSONObject(name);
            if (!CareEngine.validScore(r.optInt("baseline")) || !CareEngine.validScore(r.optInt("target")))
                issues.add(name + ": selecciona nivel al recibir y nivel esperado.");
            if (r.optInt("target") > 0 && r.optInt("baseline") > 0 && r.optInt("target") < r.optInt("baseline"))
                issues.add(name + ": el nivel esperado debe mantener o mejorar el estado inicial.");
        }
        return issues;
    }
    public static List<String> executionIssues(JSONObject p) {
        List<String> issues = new ArrayList<>();
        for (String name : keys(map(p, "nics"))) {
            JSONObject r = map(p, "nics").optJSONObject(name);
            if (!"Pendiente".equals(r.optString("state")) && (r.optString("note").isEmpty() || r.optString("recordedAt").isEmpty())) issues.add(name + ": falta registro de ejecución o motivo.");
        }
        for (String name : keys(map(p, "nocs"))) {
            JSONObject r = map(p, "nocs").optJSONObject(name);
            if (!"Pendiente".equals(r.optString("evaluationState")) && (r.optString("response").isEmpty() || r.optString("evaluatedAt").isEmpty())) issues.add(name + ": falta fuente o motivo de reevaluación.");
            if ("Evaluado".equals(r.optString("evaluationState")) && !CareEngine.validScore(r.optInt("actual"))) issues.add(name + ": puntuación de reevaluación inválida.");
        }
        return issues;
    }
    public String exportJson(JSONObject state) {
        JSONObject export = obj("warning", "SIMULACIÓN; no es un registro clínico firmado. Escalas y enlaces no validados.",
                "exportedAt", now(), "catalogVersion", catalog.optString("version"), "scaleDefinitions", catalog.optJSONObject("scales"), "state", state);
        try { return export.toString(2); } catch (JSONException e) { return export.toString(); }
    }
    public String summary(JSONObject state) {
        StringBuilder s = new StringBuilder("GENFER · PLAN DE CUIDADOS DE HOSPITALIZACIÓN\n");
        s.append("PROTOTIPO PARA VALIDACIÓN INSTITUCIONAL — NO SUSTITUYE EL JUICIO CLÍNICO\n");
        s.append(state.optString("caseId", "SIM-001")).append(" · ").append(state.optString("date")).append(" · ").append(state.optString("shift"));
        s.append("\nEscala ordinal simplificada 1–5: 1 = mayor compromiso; 5 = mejor estado.\n");
        JSONObject plans = map(state, "plans");
        if (plans.length() == 0) s.append("\nSin diagnósticos añadidos.\n");
        for (String id : keys(plans)) {
            JSONObject p = plans.optJSONObject(id);
            s.append("\n\nDIAGNÓSTICO DE ENFERMERÍA: ").append(diagnosis(id).optString("label"));
            s.append("\nDiagnóstico médico asociado: ").append(condition(p.optString("conditionId")).optString("label"));
            s.append("\nPrioridad: ").append(p.optString("priority"));
            s.append("\nPES: ").append(formulation(p));
            for (String name : keys(map(p, "nocs"))) {
                JSONObject r = map(p, "nocs").optJSONObject(name);
                s.append("\nNOC: ").append(name);
                s.append(" | Recibo: ").append(scoreText(r, "baseline"));
                s.append(" | Quiero: ").append(scoreText(r, "target"));
            }
            if (map(p, "nics").length() > 0) s.append("\nNIC seleccionados: ").append(String.join(", ", keys(map(p, "nics"))));
            if (!p.optString("note").isEmpty()) s.append("\nObservación: ").append(p.optString("note"));
        }
        return s.toString();
    }

}
