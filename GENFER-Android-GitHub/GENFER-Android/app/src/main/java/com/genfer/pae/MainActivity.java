package com.genfer.pae;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AtomicFile;
import android.view.View;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import static com.genfer.pae.PlanRepository.*;

/** Interfaz Android nativa, escrita en Java. No utiliza WebView ni un servidor. */
public final class MainActivity extends Activity {
    private static final int INK = 0xFF183D3B, TEAL = 0xFF0D6B5E, BG = 0xFFF4F6F2;
    private static final int EXPORT_REQUEST = 41;
    private PlanRepository repo;
    private JSONObject state;
    private String conditionId = "c1", activeId = "", pendingExport;
    private int step;
    private ScrollView scroll;
    private LinearLayout content;
    private interface Choice { void accept(String value); }
    private interface Checked { void accept(boolean value); }

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        try {
            repo = new PlanRepository(this);
            state = saved != null ? new JSONObject(saved.getString("state", "{}")) : newState();
            if (saved != null) {
                conditionId = saved.getString("condition", "c1");
                activeId = saved.getString("active", "");
                step = saved.getInt("step", 0);
                pendingExport = saved.getString("export");
            }
            validateState(state);
        } catch (Exception e) {
            TextView error = new TextView(this);
            error.setPadding(32, 64, 32, 32);
            error.setText("No se pudo abrir GENFER. Revisa assets/catalogo.json.\n" + e.getMessage());
            setContentView(error); return;
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
            });
        } else root.setFitsSystemWindows(true);
        scroll = new ScrollView(this); scroll.setFillViewport(true);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(30));
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        setContentView(root); render(false);
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        if (state != null) out.putString("state", state.toString());
        out.putString("condition", conditionId); out.putString("active", activeId);
        out.putInt("step", step); out.putString("export", pendingExport);
        super.onSaveInstanceState(out);
    }
    private JSONObject plan() { return map(state, "plans").optJSONObject(activeId); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable background(int color, boolean selected) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color);
        shape.setCornerRadius(dp(12)); shape.setStroke(dp(selected ? 2 : 1), selected ? TEAL : 0xFFDCE6DF); return shape;
    }
    private TextView text(LinearLayout parent, String value, int size, boolean bold) {
        TextView label = new TextView(this); label.setText(value); label.setTextColor(INK);
        label.setTextSize(size); label.setPadding(0, dp(6), 0, dp(8));
        if (bold) label.setTypeface(null, Typeface.BOLD);
        label.setTextIsSelectable(true); parent.addView(label); return label;
    }
    private LinearLayout card(String title) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(12), dp(14), dp(14)); box.setBackground(background(Color.WHITE, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(12), 0, dp(6));
        content.addView(box, lp); if (!title.isEmpty()) text(box, title, 19, true); return box;
    }
    private Button button(LinearLayout parent, String title, boolean primary, Runnable action) {
        Button b = new Button(this); b.setText(title); b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : TEAL); b.setTextSize(14);
        b.setPadding(dp(12), dp(8), dp(12), dp(8)); b.setMinHeight(dp(48));
        b.setBackground(background(primary ? TEAL : Color.WHITE, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(7), 0, dp(4));
        parent.addView(b, lp); b.setOnClickListener(v -> action.run()); return b;
    }
    private void select(LinearLayout parent, String title, List<String> options, String selected, boolean blank, Choice callback) {
        text(parent, title, 13, true);
        List<String> values = new ArrayList<>(); if (blank) values.add(""); values.addAll(options);
        List<String> labels = new ArrayList<>(); for (String s : values) labels.add(s.isEmpty() ? "Seleccionar…" : s);
        Spinner spinner = new Spinner(this); spinner.setContentDescription(title);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels) {
            private View readable(View view) {
                TextView label = (TextView) view;
                label.setSingleLine(false); label.setMaxLines(6); label.setTextSize(14);
                label.setMinHeight(dp(48)); label.setPadding(dp(8), dp(8), dp(8), dp(8));
                return label;
            }
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                return readable(super.getView(position, convertView, parent));
            }
            @Override public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                return readable(super.getDropDownView(position, convertView, parent));
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter);
        int position = values.indexOf(selected); spinner.setSelection(Math.max(0, position));
        spinner.setMinimumHeight(dp(54)); parent.addView(spinner, new LinearLayout.LayoutParams(-1, -2));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private String last = selected;
            @Override public void onItemSelected(AdapterView<?> p, View view, int pos, long id) {
                String value = values.get(pos);
                if (!value.equals(last)) { last = value; callback.accept(value); }
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });
    }
    private List<String> options(String key) { return strings(repo.catalog.optJSONArray(key)); }
    private void check(LinearLayout parent, String title, boolean selected, Checked action) {
        CheckBox box = new CheckBox(this); box.setText(title); box.setTextColor(INK); box.setMinHeight(dp(48));
        box.setButtonTintList(ColorStateList.valueOf(TEAL)); box.setChecked(selected);
        parent.addView(box); box.setOnCheckedChangeListener((b, checked) -> action.accept(checked));
    }
    private void message(String title, String body) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("Entendido", null).show();
    }
    private void confirm(String title, String body, Runnable action) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(body)
                .setPositiveButton("Continuar", (d,w) -> action.run()).setNegativeButton("Cancelar", (d,w) -> render(true)).setOnCancelListener(d -> render(true)).show();
    }
    private void render(boolean keepScroll) {
        int previous = keepScroll ? scroll.getScrollY() : 0;
        content.removeAllViews();
        text(content, "✚ GENFER", 28, true);
        text(content, "Hospitalización · Simulación · Java nativo", 13, false);
        text(content, "Solo datos ficticios. Catálogo, enlaces y anclajes pendientes de validación institucional.", 12, false);
        LinearLayout context = card("Caso SIM-001");
        button(context, "Fecha: " + state.optString("date"), false, this::chooseDate);
        select(context, "Turno", Arrays.asList("Mañana", "Tarde", "Noche"), state.optString("shift"), false,
                value -> { put(state, "shift", value); if (step == 4) render(true); });
        JSONArray cs = repo.catalog.optJSONArray("conditions"); List<String> names = new ArrayList<>();
        for (int i = 0; i < cs.length(); i++) names.add(cs.optJSONObject(i).optString("label"));
        select(context, "Patología / condición de ingreso", names, repo.condition(conditionId).optString("label"), false, value -> {
            conditionId = cs.optJSONObject(names.indexOf(value)).optString("id"); activeId = ""; step = 0; render(false);
        });
        JSONObject plans = map(state, "plans");
        if (plans.length() > 0) {
            LinearLayout selected = card("Plan del caso · " + plans.length());
            for (String id : keys(plans)) button(selected, (id.equals(activeId) ? "✓ " : "") + repo.diagnosis(id).optString("label"), false, () -> {
                activeId = id; conditionId = plans.optJSONObject(id).optString("conditionId"); step = 1; render(false);
            });
        }
        HorizontalScrollView tabs = new HorizontalScrollView(this);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); tabs.addView(row); content.addView(tabs);
        String[] labels = {"1. Selección", "2. PES", "3. NOC / NIC", "4. Turno", "5. Resumen"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i; Button b = new Button(this); b.setText(labels[i]); b.setAllCaps(false);
            b.setTextColor(i == step ? Color.WHITE : TEAL); b.setBackground(background(i == step ? TEAL : Color.WHITE, false));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(125), dp(48)); lp.setMargins(dp(3), dp(10), dp(3), dp(10)); row.addView(b, lp);
            b.setOnClickListener(v -> go(index));
        }
        if (step == 0) selection();
        else if (step == 4) summaryView();
        else if (plan() == null) text(card("Selecciona un diagnóstico"), "Añádelo desde Selección o abre uno del plan del caso.", 15, false);
        else if (step == 1) diagnosisView();
        else if (step == 2) planningView();
        else turnView();
        LinearLayout files = card("Borrador local de simulación");
        button(files, "Guardar borrador en este celular", false, this::saveDraft);
        button(files, "Abrir borrador guardado", false, () -> confirm("Abrir borrador", "Reemplazará las selecciones en pantalla por el último borrador guardado.", this::loadDraft));
        button(files, "Reiniciar pantalla", false, () -> confirm("Reiniciar", "Se borrarán las selecciones actuales. El archivo guardado no cambia.", () -> {
            state = newState(); activeId = ""; conditionId = "c1"; step = 0; render(false);
        }));
        button(files, "Eliminar borrador guardado", false, () -> confirm("Eliminar guardado", "Se eliminará el archivo local de simulación. La pantalla actual no cambia.", () -> {
            draftFile().delete(); Toast.makeText(this, "Borrador guardado eliminado", Toast.LENGTH_SHORT).show();
        }));
        text(content, "Escala 1–5 ilustrativa. El color no es triaje. Sin inicio de sesión, firma clínica ni historia de turnos.", 12, false);
        scroll.post(() -> scroll.scrollTo(0, previous));
    }
    private void go(int next) {
        JSONObject p = plan();
        if (p != null && (next == 2 || next == 3)) {
            List<String> issues = next == 2 ? repo.diagnosisIssues(p) : repo.planIssues(p);
            if (!issues.isEmpty()) {
                step = repo.diagnosisIssues(p).isEmpty() ? 2 : 1; render(false);
                message("Antes de continuar", String.join("\n", issues)); return;
            }
        }
        step = next; render(false);
    }
    private void chooseDate() {
        String[] value = state.optString("date").split("-"); Calendar c = Calendar.getInstance();
        try { c.set(Integer.parseInt(value[0]), Integer.parseInt(value[1]) - 1, Integer.parseInt(value[2])); } catch (Exception ignored) { }
        new DatePickerDialog(this, (v,y,m,d) -> { put(state,"date",String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d)); render(true); },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void selection() {
        JSONObject c = repo.condition(conditionId);
        text(card("Hallazgos de referencia"), join(c.optJSONArray("findings")) + ". No se incorporan automáticamente al PES.", 14, false);
        JSONArray ds = c.optJSONArray("diagnoses");
        for (int i = 0; i < ds.length(); i++) {
            JSONObject d = ds.optJSONObject(i); String id = d.optString("id"); LinearLayout box = card(d.optString("label"));
            text(box, "risk".equals(d.optString("type")) ? "Diagnóstico de riesgo · sin manifestaciones" : "Problema actual · formulación PES", 13, false);
            text(box, d.optJSONArray("noc").length() + " resultados NOC y " + d.optJSONArray("nic").length() + " intervenciones NIC propuestos", 13, false);
            button(box, map(state,"plans").has(id) ? "Abrir formulación" : "Añadir al plan", true, () -> {
                if (!map(state,"plans").has(id)) put(map(state,"plans"),id,repo.newPlan(conditionId,id));
                activeId = id; go(1);
            });
        }
    }
    private void diagnosisView() {
        JSONObject p = plan(), d = repo.diagnosis(activeId); boolean risk = "risk".equals(d.optString("type"));
        LinearLayout box = card(d.optString("label"));
        select(box, risk ? "Factor de riesgo propuesto" : "Factor relacionado propuesto", Arrays.asList(d.optString("proposedFactor")), p.optString("factor"), true,
                value -> { put(p,"factor",value); invalidate(p); render(true); });
        text(box, "Texto de la matriz por cotejar. Si no corresponde, no fuerces una selección. Contexto asociado: " + repo.condition(p.optString("conditionId")).optString("label"), 12, false);
        select(box, "Momento de valoración", options("moments"), p.optString("moment"), true,
                value -> { put(p,"moment",value); invalidate(p); render(true); });
        text(box,"Fuentes de valoración",16,true);
        for (String value : options("sources")) check(box,value,contains(array(p,"sources"),value),checked -> {
            toggle(p,"sources",value,checked); invalidate(p); render(true);
        });
        if (risk) text(box,"Este diagnóstico no incluye signos, síntomas ni «manifestado por».",14,true);
        else {
            text(box,"Signos y síntomas pertinentes",16,true);
            for (String value : strings(d.optJSONArray("evidenceOptions"))) check(box,value,contains(array(p,"evidence"),value),checked -> {
                toggle(p,"evidence",value,checked); invalidate(p); render(true);
            });
        }
        select(box,"Prioridad",Arrays.asList("Alta","Media","Baja"),p.optString("priority"),false,value -> put(p,"priority",value));
        text(box,repo.formulation(p),16,true);
        check(box,"He revisado la coherencia de las selecciones en este ejercicio",p.optBoolean("reviewed"),checked -> {
            put(p,"reviewed",checked); put(p,"assessedAt",checked ? now() : "");
        });
        text(box,"Observación libre opcional",13,true);
        EditText note = new EditText(this); note.setText(p.optString("note")); note.setHint("No es obligatorio redactar"); note.setMinLines(2); note.setGravity(android.view.Gravity.TOP); box.addView(note);
        note.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) { put(p,"note",s.toString()); }
            @Override public void afterTextChanged(Editable e) { }
        });
        button(box,"Seleccionar NOC y NIC",true,() -> go(2));
        button(box,"Retirar diagnóstico del plan",false,() -> confirm("Retirar diagnóstico","Se eliminarán sus metas, actividades y evaluación de la pantalla.",() -> {
            map(state,"plans").remove(activeId); activeId=""; step=0; render(false);
        }));
    }
    private void score(LinearLayout box, JSONObject r, String key, String title) {
        text(box,title,15,true); JSONArray anchors = repo.anchors(r);
        for (int i=1;i<=5;i++) {
            final int value=i; boolean chosen=r.optInt(key)==i;
            Button b=button(box,(chosen?"✓ ":"")+i+" · "+anchors.optString(i-1),false,() -> {
                if ("actual".equals(key)) {
                    if (!"Evaluado".equals(r.optString("evaluationState"))) put(r,"response","");
                    put(r,"actual",value); put(r,"evaluationState","Evaluado"); put(r,"evaluatedAt",now());
                } else if (r.optInt(key)!=value) { resetEvaluation(r); put(r,key,value); }
                render(true);
            });
            b.setTextColor(INK); b.setBackground(background(CareEngine.scoreColor(i),chosen)); b.setSelected(chosen);
            b.setContentDescription(title+": "+i+" de 5, "+anchors.optString(i-1)+(chosen?", seleccionado":""));
        }
    }
    private void planningView() {
        JSONObject p=plan(),d=repo.diagnosis(activeId);
        LinearLayout header=card(d.optString("label"));
        text(header,"Escala ordinal ilustrativa de cinco puntos: mayor número = mejor estado. No es una escala NOC oficial. Cada indicador tiene sus descripciones.",13,false);
        LinearLayout choices=card("Resultados NOC propuestos");
        JSONObject outcomes=map(p,"nocs");
        for(String name:strings(d.optJSONArray("noc"))) check(choices,name,outcomes.has(name),checked -> {
            if(checked){put(outcomes,name,repo.newOutcome(name));render(true);}
            else confirm("Retirar resultado","Se borrarán sus selecciones y evaluación de la pantalla.",() -> {outcomes.remove(name);render(true);});
        });
        for(String name:keys(outcomes)) {
            JSONObject r=outcomes.optJSONObject(name); LinearLayout box=card(name);
            select(box,"Indicador predeterminado",strings(repo.catalog.optJSONObject("outcomes").optJSONObject(name).optJSONArray("indicators")),r.optString("indicator"),true,value -> {
                put(r,"indicator",value);put(r,"baseline",0);put(r,"target",0);resetEvaluation(r);render(true);
            });
            score(box,r,"baseline","Estado inicial"); score(box,r,"target","Meta esperada");
            select(box,"Plazo de evaluación",options("deadlines"),r.optString("deadline"),true,value -> {
                put(r,"deadline",value);resetEvaluation(r);render(true);
            });
            text(box,repo.goal(r),15,true);
        }
        LinearLayout nics=card("Intervenciones NIC propuestas"); JSONObject interventions=map(p,"nics");
        for(String name:strings(d.optJSONArray("nic"))) check(nics,name,interventions.has(name),checked -> {
            if(checked){put(interventions,name,newIntervention());render(true);}
            else confirm("Retirar intervención","Se borrarán sus actividades y registros de la pantalla.",() -> {interventions.remove(name);render(true);});
        });
        for(String name:keys(interventions)) {
            JSONObject r=interventions.optJSONObject(name); LinearLayout box=card(name);
            text(box,"Selecciona actividades aplicables según competencia, indicación y protocolo.",12,false);
            for(String value:strings(repo.catalog.optJSONObject("interventions").optJSONArray(name))) check(box,value,contains(array(r,"activities"),value),checked -> {
                toggle(r,"activities",value,checked);resetExecution(r);
            });
            select(box,"Programación",options("schedules"),r.optString("schedule"),true,value -> {put(r,"schedule",value);resetExecution(r);});
            select(box,"Responsable / rol",options("responsibles"),r.optString("responsible"),true,value -> {put(r,"responsible",value);resetExecution(r);});
        }
        button(content,"Registrar turno y reevaluar",true,() -> go(3));
    }
    private void progress(LinearLayout box,JSONObject r) {
        text(box,"Inicial: "+repo.scoreText(r,"baseline")+"\nMeta: "+repo.scoreText(r,"target"),14,false);
        String status=r.optString("evaluationState");
        TextView current=text(box,"Reevaluación: "+("No evaluable".equals(status)?"No evaluable":repo.scoreText(r,"actual")),16,true);
        current.setPadding(dp(12),dp(12),dp(12),dp(12));
        current.setBackground(background(CareEngine.scoreColor("Evaluado".equals(status)?r.optInt("actual"):0),false));
        text(box,comparison(r),15,true);
    }
    private void turnView() {
        JSONObject p=plan();
        text(card(repo.diagnosis(activeId).optString("label")),"Cada estado debe corresponder a lo observado. La hora automática es la hora del registro, no una hora de ejecución inferida.",13,false);
        for(String name:keys(map(p,"nics"))) {
            JSONObject r=map(p,"nics").optJSONObject(name);LinearLayout box=card(name);
            text(box,join(array(r,"activities")),14,false);text(box,r.optString("schedule")+" · "+r.optString("responsible"),13,false);
            select(box,"Estado de las actividades seleccionadas",Arrays.asList("Pendiente","Realizada","No realizada"),r.optString("state"),false,value -> {
                put(r,"state",value);put(r,"note","");put(r,"recordedAt","Pendiente".equals(value)?"":now());render(true);
            });
            if(!"Pendiente".equals(r.optString("state"))) {
                List<String> all=options("notes"); boolean notDone="No realizada".equals(r.optString("state"));
                select(box,notDone?"Motivo de no realización":"Registro de ejecución",notDone?all.subList(3,all.size()):all.subList(0,3),r.optString("note"),true,value -> put(r,"note",value));
            }
            text(box,"Hora de registro: "+r.optString("recordedAt"),12,false);
        }
        for(String name:keys(map(p,"nocs"))) {
            JSONObject r=map(p,"nocs").optJSONObject(name);LinearLayout box=card(name);
            text(box,r.optString("indicator")+"\n"+repo.goal(r),13,false);progress(box,r);
            score(box,r,"actual","Reevaluación observada");
            button(box,"Dejar pendiente",false,() -> {resetEvaluation(r);render(true);});
            button(box,"No evaluable",false,() -> {resetEvaluation(r);put(r,"evaluationState","No evaluable");put(r,"evaluatedAt",now());render(true);});
            boolean no="No evaluable".equals(r.optString("evaluationState"));
            if(!"Pendiente".equals(r.optString("evaluationState"))) select(box,no?"Motivo de no evaluabilidad":"Fuente de reevaluación",options(no?"nonEvaluable":"responses"),r.optString("response"),true,value -> put(r,"response",value));
            text(box,"Hora de registro: "+r.optString("evaluatedAt"),12,false);
        }
        LinearLayout box=card("Continuidad");
        select(box,"Conducta para el siguiente turno",options("followups"),p.optString("followup"),true,value -> put(p,"followup",value));
        button(box,"Ver todas las metas y el resumen",true,() -> go(4));
    }
    private void summaryView() {
        JSONObject plans=map(state,"plans");int total=0,met=0,pending=0;
        for(String id:keys(plans)) for(String name:keys(map(plans.optJSONObject(id),"nocs"))) {
            JSONObject r=map(plans.optJSONObject(id),"nocs").optJSONObject(name);total++;
            if("Meta alcanzada".equals(comparison(r)))met++;
            if("Pendiente".equals(r.optString("evaluationState")))pending++;
        }
        LinearLayout header=card("Panel de metas");
        text(header,met+" / "+total+" metas alcanzadas según puntuación seleccionada\n"+pending+" pendientes de evaluar",20,true);
        text(header,"Sin promedios entre escalas. El color describe el indicador, no el riesgo clínico global.",13,false);
        for(String id:keys(plans)) for(String name:keys(map(plans.optJSONObject(id),"nocs"))) {
            LinearLayout box=card(name);text(box,repo.diagnosis(id).optString("label"),12,false);
            progress(box,map(plans.optJSONObject(id),"nocs").optJSONObject(name));
        }
        button(header,"Exportar resumen TXT",true,() -> export(repo.summary(state),"text/plain","GENFER-SIM-001.txt"));
        button(header,"Exportar datos JSON",false,() -> export(repo.exportJson(state),"application/json","GENFER-SIM-001.json"));
        text(card("Resumen para revisión"),repo.summary(state),13,false);
    }
    private AtomicFile draftFile() { return new AtomicFile(new File(getFilesDir(),"genfer-simulacion.json")); }
    private boolean saveDraft() {
        FileOutputStream output=null;AtomicFile file=draftFile();
        try {
            output=file.startWrite();output.write(state.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(output);
            Toast.makeText(this,"Borrador guardado en este celular",Toast.LENGTH_SHORT).show();return true;
        } catch(Exception e) {if(output!=null)file.failWrite(output);message("No se pudo guardar",e.getMessage());return false;}
    }
    private void validateState(JSONObject candidate) {
        if(candidate.optInt("version")!=1 || candidate.optJSONObject("plans")==null) throw new IllegalArgumentException("Versión o estructura de borrador no compatible");
        JSONObject plans=map(candidate,"plans");
        for(String id:keys(plans)) {
            JSONObject p=plans.optJSONObject(id);if(p==null)throw new IllegalArgumentException("Plan inválido");
            JSONObject d=repo.diagnosis(id);JSONObject condition=repo.condition(p.optString("conditionId"));
            if(!p.optString("id").equals(id))throw new IllegalArgumentException("Identificador de plan inválido");
            boolean member=false;JSONArray ds=condition.optJSONArray("diagnoses");
            for(int i=0;i<ds.length();i++)if(id.equals(ds.optJSONObject(i).optString("id")))member=true;
            if(!member)throw new IllegalArgumentException("Diagnóstico ajeno a la condición");
            for(String name:keys(map(p,"nocs"))) {
                if(!strings(d.optJSONArray("noc")).contains(name))throw new IllegalArgumentException("Resultado no compatible");
                JSONObject r=map(p,"nocs").optJSONObject(name);
                if(r==null||r.optInt("levels")!=5||!repo.catalog.optJSONObject("outcomes").optJSONObject(name).optString("profile").equals(r.optString("profile")))throw new IllegalArgumentException("Escala no compatible");
                for(String k:Arrays.asList("baseline","target","actual"))if(r.optInt(k,-1)<0||r.optInt(k)>5)throw new IllegalArgumentException("Puntuación inválida");
            }
            for(String name:keys(map(p,"nics")))if(!strings(d.optJSONArray("nic")).contains(name))throw new IllegalArgumentException("Intervención no compatible");
        }
    }
    private void loadDraft() {
        try(InputStream in=draftFile().openRead()) {
            JSONObject loaded=new JSONObject(read(in));validateState(loaded);state=loaded;
            activeId="";conditionId="c1";step=4;render(false);
        } catch(Exception e) {message("No se pudo abrir el borrador","Guarda primero una simulación.\n"+e.getMessage());}
    }
    private void export(String data,String mime,String filename) {
        pendingExport=data;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);intent.putExtra(Intent.EXTRA_TITLE,filename);
        try {startActivityForResult(intent,EXPORT_REQUEST);}catch(Exception e){pendingExport=null;message("No se pudo exportar",e.getMessage());}
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=EXPORT_REQUEST)return;
        if(result==RESULT_OK&&data!=null&&data.getData()!=null&&pendingExport!=null) {
            try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")) {
                if(out==null)throw new IllegalStateException("No se pudo abrir el destino");
                out.write(pendingExport.getBytes(StandardCharsets.UTF_8));Toast.makeText(this,"Archivo exportado",Toast.LENGTH_SHORT).show();
            }catch(Exception e){message("No se pudo escribir el archivo",e.getMessage());}
        }
        pendingExport=null;
    }
    @Override public void onBackPressed() {
        if(state==null){super.onBackPressed();return;}
        if(step>0){step--;render(false);return;}
        new AlertDialog.Builder(this).setTitle("Salir de GENFER").setMessage("¿Guardar las selecciones actuales antes de salir?")
                .setPositiveButton("Guardar y salir",(d,w)->{if(saveDraft())finish();})
                .setNegativeButton("Salir sin guardar",(d,w)->finish()).setNeutralButton("Cancelar",null).show();
    }
}
