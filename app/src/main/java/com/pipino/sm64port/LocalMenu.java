package com.pipino.sm64port;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicInteger;

/** Native, touchable lobby; UI animations follow display refresh, not game FPS. */
final class LocalMenu extends FrameLayout {
    private static final int ACCENT = 0xff8db6ff, MUTED = 0xffabb5ca;
    private final Activity activity;
    private final GameHaptics haptics;
    private final AtomicInteger event = new AtomicInteger();
    private LinearLayout panel;
    private final TextView[] rows = new TextView[2];
    private final ValueAnimator[] highlights = new ValueAnimator[2];
    private final int[] rowColors = new int[2];
    private int page = -1, selection = -1, status = -1, ready = -1;
    private String detail = "", submitted = "";

    LocalMenu(Activity activity, ViewGroup parent, GameHaptics haptics) {
        super(activity);
        this.activity = activity;
        this.haptics = haptics;
        setClipChildren(false);
        setVisibility(GONE);
        parent.addView(this, new ViewGroup.LayoutParams(-1, -1));
    }

    // Called by SDL. Each command is tagged with its source page, so a double
    // tap cannot accidentally confirm an option on the following screen.
    int update(int nextPage, int selected, int nextStatus, int nextReady, String text) {
        String key = nextPage + ":" + selected + ":" + nextStatus + ":" + nextReady + ":" + text;
        if (!key.equals(submitted)) {
            submitted = key;
            activity.runOnUiThread(() -> render(nextPage, selected, nextStatus, nextReady, text));
        }
        return event.getAndSet(0);
    }

    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable background(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView label(String text, int size, int color) {
        TextView v = new TextView(activity);
        v.setText(text); v.setTextSize(size); v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setFontFeatureSettings("kern");
        return v;
    }
    private void send(int value) { event.compareAndSet(0, (page << 8) | value); }

    private void render(int nextPage, int selected, int nextStatus, int nextReady, String text) {
        if (nextPage == 0) {
            page = 0;
            animate().cancel();
            setVisibility(GONE);
            return;
        }
        boolean rebuild = page != nextPage || status != nextStatus || ready != nextReady || !detail.equals(text);
        boolean newPage = page != nextPage;
        page = nextPage; status = nextStatus; ready = nextReady; detail = text;
        if (rebuild) {
            for (ValueAnimator a : highlights) if (a != null) a.cancel();
            removeAllViews();
            panel = new LinearLayout(activity);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setPadding(dp(16), dp(12), dp(16), dp(10));
            GradientDrawable bg = background(0xf51a2130, 20);
            bg.setStroke(dp(1), 0xff354156);
            panel.setBackground(bg); panel.setElevation(dp(12));
            int width = page == 5 ? 220 : 400;
            width = Math.min(dp(width), getResources().getDisplayMetrics().widthPixels - dp(32));
            FrameLayout.LayoutParams placement = new FrameLayout.LayoutParams(width, -2,
                    page == 5 ? Gravity.TOP | Gravity.RIGHT : Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            placement.setMargins(dp(16), dp(16), dp(16), dp(12));
            addView(panel, placement);
            if (page != 5) {
                String title = page == 1 ? "Come vuoi giocare?" : page == 4 ? "Connessione locale"
                        : page == 2 ? "La tua partita" : status == 2 ? "Giocatori connessi"
                        : status == 3 ? "Connessione interrotta" : status == 4 ? "Riconnessione…" : "Trova il tuo compagno";
                TextView heading = label(title, 17, Color.WHITE);
                heading.setTypeface(null, Typeface.BOLD);
                LinearLayout header = new LinearLayout(activity);
                header.setGravity(Gravity.CENTER_VERTICAL);
                if (page == 2 || page == 4) {
                    TextView back = label("‹", 28, ACCENT);
                    back.setGravity(Gravity.CENTER);
                    back.setContentDescription("Indietro");
                    back.setOnClickListener(v -> send(3));
                    header.addView(back, new LinearLayout.LayoutParams(dp(36), dp(36)));
                }
                header.addView(heading, new LinearLayout.LayoutParams(0, -1, 1));
                panel.addView(header, new LinearLayout.LayoutParams(-1, dp(32)));
                String caption = page == 1 ? "Super Mario 64 · Gioco locale" : page == 4 ? "Scegli come collegare i due telefoni"
                        : page == 2 ? "Mario ospita · Luigi si unisce"
                        : status == 2 ? ((ready & 1) != 0 ? "Tu sei pronto · In attesa del compagno" : "Confermate entrambi per iniziare")
                        : translate(text);
                TextView subtitle = label(caption, 12, MUTED);
                subtitle.setMaxLines(2);
                panel.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(32)));
                if (page == 3 && (status == 1 || status == 4)) {
                    ProgressBar progress = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
                    progress.setIndeterminate(true);
                    progress.setIndeterminateTintList(ColorStateList.valueOf(ACCENT));
                    panel.addView(progress, new LinearLayout.LayoutParams(-1, dp(4)));
                }
                String[] choices = page == 1 ? new String[] { "Giocatore singolo", "Multiplayer" }
                        : page == 4 ? new String[] { "Wi-Fi / Hotspot", "Wi-Fi Direct · Nelle vicinanze" }
                        : page == 2 ? new String[] { "Crea partita", "Cerca partita" }
                        : new String[] { status == 2 ? ((ready & 1) != 0 ? "Pronto ✓" : "Sono pronto")
                                : status == 3 ? "Riprova" : "Ricerca in corso…", "Indietro" };
                for (int i = 0; i < 2; ++i) {
                    final int index = i;
                    TextView row = label(choices[i], 14, Color.WHITE);
                    row.setPadding(dp(14), 0, dp(14), 0);
                    row.setTypeface(null, Typeface.BOLD);
                    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(44));
                    rp.topMargin = dp(5);
                    panel.addView(row, rp); rows[i] = row;
                    rowColors[i] = 0xff242e40;
                    row.setBackground(background(rowColors[i], 12));
                    boolean enabled = page != 3 || i == 1 || status == 2 || status == 3;
                    row.setEnabled(enabled); row.setAlpha(enabled ? 1f : .6f);
                    row.setOnClickListener(v -> send(16 + index));
                    row.setOnTouchListener((v, e) -> {
                        if (e.getActionMasked() == 0) v.animate().scaleX(.98f).scaleY(.98f).setDuration(80).start();
                        else if (e.getActionMasked() == 1 || e.getActionMasked() == 3)
                            v.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                        return false;
                    });
                }
            }
            Switch vibration = new Switch(activity);
            vibration.setText("Vibrazione discreta"); vibration.setTextSize(12); vibration.setTextColor(MUTED);
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                vibration.setThumbTintList(ColorStateList.valueOf(ACCENT));
                vibration.setTrackTintList(ColorStateList.valueOf(0xff465d80));
            }
            vibration.setChecked(haptics.enabled);
            vibration.setOnCheckedChangeListener((button, checked) -> haptics.setEnabled(checked));
            panel.addView(vibration, new LinearLayout.LayoutParams(-1, dp(42)));
            if (page != 5) {
                TextView footer = label(page == 1 ? "A / START  Seleziona" : "A / START  Seleziona     B  Indietro", 11, MUTED);
                footer.setGravity(Gravity.CENTER);
                panel.addView(footer, new LinearLayout.LayoutParams(-1, dp(24)));
            }
            selection = -1;
        }
        setVisibility(VISIBLE);
        if (page != 5 && selected != selection) {
            selection = selected;
            for (int i = 0; i < 2; ++i) {
                final int index = i;
                if (highlights[i] != null) highlights[i].cancel();
                ValueAnimator a = ValueAnimator.ofArgb(rowColors[i], i == selected ? 0xff3c5278 : 0xff242e40);
                highlights[i] = a;
                a.addUpdateListener(animation -> {
                    rowColors[index] = (Integer)animation.getAnimatedValue();
                    rows[index].setBackground(background(rowColors[index], 12));
                });
                a.setDuration(160); a.start();
            }
        }
        if (newPage) {
            panel.animate().cancel(); panel.setAlpha(0); panel.setTranslationY(dp(12));
            panel.animate().alpha(1).translationY(0).setDuration(200)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    static String translate(String text) {
        if (text.contains("PERMISSION DENIED")) return "Permesso negato. Consenti l’accesso e riprova.";
        if (text.contains("PERMISSION")) return "Consenti il permesso Wi-Fi richiesto da Android.";
        if (text.contains("LOCATION")) return "Accendi Wi-Fi e localizzazione per la ricerca.";
        if (text.contains("ACCEPT")) return "Accetta la richiesta sull’altro telefono.";
        if (text.contains("NOT SUPPORTED")) return "Wi-Fi Direct non disponibile: usa Wi-Fi / Hotspot.";
        if (text.contains("TIMEOUT")) return "Nessuna risposta. Avvicina i telefoni e riprova.";
        if (text.contains("CREATING")) return "Preparazione della partita nelle vicinanze…";
        if (text.contains("WAITING")) return "In attesa del secondo giocatore…";
        if (text.contains("SAME WIFI")) return "Collega entrambi alla stessa rete Wi-Fi o hotspot.";
        if (text.contains("ENABLE WIFI")) return "Accendi il Wi-Fi nelle impostazioni del telefono.";
        if (text.contains("CANCELLED")) return "Ricerca annullata. Puoi riprovare o tornare indietro.";
        if (text.contains("RECONNECTING")) return "Attendo di ritrovare il compagno della sessione…";
        if (text.contains("ERROR") || text.contains("DISCONNECTED")) return "Collegamento non riuscito. Controlla il Wi-Fi e riprova.";
        return text;
    }
}
