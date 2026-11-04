package com.dangmod.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "DangMod";
    static final String PKG_FF = "com.dts.freefireth";
    static final String PKG_FFMAX = "com.dts.freefiremax";

    static final int MATCH = LayoutParams.MATCH_PARENT;
    static final int WRAP = LayoutParams.WRAP_CONTENT;

    static final int C_BG = 0xFF121212;
    static final int C_CARD = 0xFF1E1E1E;
    static final int C_ACCENT = 0xFF00E676;
    static final int C_TEXT = 0xFFFFFFFF;
    static final int C_SUB = 0xFFB0B0B0;

    static final Pattern PKG_RE =
            Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$");

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final JSONObject settings = new JSONObject();

    private ViewFlipper flipper;
    private EditText keyInput;
    private SwitchCompat swAimbot, swEsp, swNoRecoil;
    private TextView optFF, optFFMax;
    private TextView loadingPct, loadingStatus;
    private View loadingBarFill;
    private String selectedPkg = null;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().setStatusBarColor(C_BG);
        getWindow().setNavigationBarColor(C_BG);

        try { settings.put("aimbot", false).put("esp", false).put("no_recoil", false); }
        catch (Exception ignored) {}

        flipper = new ViewFlipper(this);
        flipper.setBackgroundColor(C_BG);
        flipper.setInAnimation(this, android.R.anim.fade_in);
        flipper.setOutAnimation(this, android.R.anim.fade_out);
        setContentView(flipper);

        flipper.addView(buildSplash());   // 0
        flipper.addView(buildKey());      // 1
        flipper.addView(buildLoading());  // 2
        flipper.addView(buildMain());     // 3
        flipper.addView(buildApply());    // 4

        flipper.setDisplayedChild(0);
        ui.postDelayed(() -> flipper.setDisplayedChild(1), 1800);

        loadSettingsFromDisk();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private GradientDrawable round(int radiusDp, int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private GradientDrawable roundStroke(int radiusDp, int color, int strokeDp, int strokeColor) {
        GradientDrawable g = round(radiusDp, color);
        g.setStroke(dp(strokeDp), strokeColor);
        return g;
    }

    private LinearLayout fullCenter() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        ll.setLayoutParams(new LayoutParams(MATCH, MATCH));
        return ll;
    }

    private TextView primaryBtn(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.BLACK);
        tv.setTextSize(15);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(round(25, C_ACCENT));
        tv.setPadding(dp(35), dp(13), dp(35), dp(13));
        tv.setClickable(true);
        return tv;
    }

    private SwitchCompat makeSwitch() {
        SwitchCompat sw = new SwitchCompat(this);
        sw.setThumbTintList(new ColorStateList(
                new int[][]{ {android.R.attr.state_checked}, {} },
                new int[]{ Color.WHITE, Color.WHITE }));
        sw.setTrackTintList(new ColorStateList(
                new int[][]{ {android.R.attr.state_checked}, {} },
                new int[]{ C_ACCENT, 0xFF333333 }));
        return sw;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private View buildSplash() {
        LinearLayout ll = fullCenter();
        TextView logo = new TextView(this);
        logo.setText("DANG MOD");
        logo.setTextSize(40);
        logo.setTextColor(C_ACCENT);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setLetterSpacing(0.15f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
        lp.bottomMargin = dp(20);
        ll.addView(logo, lp);
        TextView sub = new TextView(this);
        sub.setText("Đang khởi động...");
        sub.setTextColor(C_SUB);
        sub.setTextSize(14);
        ll.addView(sub);
        return ll;
    }

    private View buildKey() {
        LinearLayout ll = fullCenter();
        ll.setPadding(dp(24), 0, dp(24), 0);
        keyInput = new EditText(this);
        keyInput.setHint("Nhập key của bạn...");
        keyInput.setHintTextColor(0xFF808080);
        keyInput.setTextColor(Color.WHITE);
        keyInput.setTextSize(18);
        keyInput.setGravity(Gravity.CENTER);
        keyInput.setSingleLine(true);
        keyInput.setBackground(roundStroke(20, C_BG, 2, C_CARD));
        keyInput.setPadding(dp(16), dp(15), dp(16), dp(15));
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(dp(320), WRAP);
        l1.bottomMargin = dp(20);
        ll.addView(keyInput, l1);
        TextView btn = primaryBtn("XÁC NHẬN");
        btn.setOnClickListener(v -> onKeyConfirm());
        ll.addView(btn, new LinearLayout.LayoutParams(WRAP, WRAP));
        return ll;
    }

    private void onKeyConfirm() {
        String key = keyInput.getText().toString().trim();
        if (key.isEmpty()) { toast("Vui lòng nhập key!"); return; }
        flipper.setDisplayedChild(2);
        runLoading();
    }

    private View buildLoading() {
        LinearLayout ll = fullCenter();
        ProgressBar circle = new ProgressBar(this);
        circle.setIndeterminate(true);
        circle.setIndeterminateTintList(ColorStateList.valueOf(C_ACCENT));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(dp(50), dp(50));
        plp.bottomMargin = dp(15);
        ll.addView(circle, plp);

        FrameLayout bar = new FrameLayout(this);
        bar.setBackground(round(10, 0xFF333333));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(260), dp(6));
        blp.bottomMargin = dp(10);
        ll.addView(bar, blp);

        loadingBarFill = new View(this);
        loadingBarFill.setBackground(round(10, C_ACCENT));
        bar.addView(loadingBarFill, new FrameLayout.LayoutParams(0, MATCH));

        loadingPct = new TextView(this);
        loadingPct.setText("0%");
        loadingPct.setTextColor(C_TEXT);
        loadingPct.setTextSize(14);
        loadingPct.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(WRAP, WRAP);
        p2.bottomMargin = dp(6);
        ll.addView(loadingPct, p2);

        loadingStatus = new TextView(this);
        loadingStatus.setText("Đang kết nối...");
        loadingStatus.setTextColor(C_SUB);
        loadingStatus.setTextSize(12);
        ll.addView(loadingStatus);

        return ll;
    }

    private void runLoading() {
        loadingBarFill.setLayoutParams(new FrameLayout.LayoutParams(0, MATCH));
        loadingPct.setText("0%");
        loadingStatus.setText("Đang kết nối máy chủ...");
        String[] msgs = {
                "Đang kết nối máy chủ...", "Đang kiểm tra key...",
                "Đang tải dữ liệu...", "Hoàn tất!" };
        final int barW = dp(260);
        final float[] p = {0f};
        final Runnable[] tick = new Runnable[1];
        tick[0] = () -> {
            p[0] = Math.min(100f, p[0] + (float)(Math.random() * 9 + 2));
            int fillW = (int)(barW * p[0] / 100f);
            loadingBarFill.setLayoutParams(new FrameLayout.LayoutParams(fillW, MATCH));
            int pi = (int) p[0];
            loadingPct.setText(pi + "%");
            loadingStatus.setText(msgs[Math.min(3, pi / 26)]);
            if (p[0] >= 100f) {
                ui.postDelayed(() -> flipper.setDisplayedChild(3), 400);
            } else {
                ui.postDelayed(tick[0], 130);
            }
        };
        ui.postDelayed(tick[0], 130);
    }

    private View buildMain() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LayoutParams(MATCH, MATCH));
        root.setPadding(dp(20), dp(24), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("BẢNG ĐIỀU KHIỂN");
        title.setTextColor(C_ACCENT);
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(WRAP, WRAP);
        tlp.bottomMargin = dp(10);
        root.addView(title, tlp);

        swAimbot = makeSwitch();
        swEsp = makeSwitch();
        swNoRecoil = makeSwitch();

        root.addView(toggleRow("Aimbot", swAimbot));
        root.addView(toggleRow("ESP (Xuyên tường)", swEsp));
        root.addView(toggleRow("Chống giật tâm", swNoRecoil));

        swAimbot.setOnCheckedChangeListener((v, c) -> onToggle("aimbot", "Aimbot", c));
        swEsp.setOnCheckedChangeListener((v, c) -> onToggle("esp", "ESP", c));
        swNoRecoil.setOnCheckedChangeListener((v, c) -> onToggle("no_recoil", "Chống giật tâm", c));

        root.addView(new View(this), new LinearLayout.LayoutParams(MATCH, 0, 1f));

        LinearLayout cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.HORIZONTAL);
        cards.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(100)));

        TextView cardReset = makeCard("RESET");
        TextView cardApply = makeCard("ÁP DỤNG\nVÀO GAME");
        TextView cardInfo  = makeCard("INFO");

        LinearLayout.LayoutParams c1 = new LinearLayout.LayoutParams(0, MATCH, 1f);
        c1.rightMargin = dp(15);
        cards.addView(cardReset, c1);

        LinearLayout.LayoutParams c2 = new LinearLayout.LayoutParams(0, MATCH, 1f);
        c2.rightMargin = dp(15);
        cards.addView(cardApply, c2);

        cards.addView(cardInfo, new LinearLayout.LayoutParams(0, MATCH, 1f));
        root.addView(cards);

        cardReset.setOnClickListener(v -> resetSettings());
        cardInfo.setOnClickListener(v -> toast("DANG MOD — native UI demo"));
        cardApply.setOnClickListener(v -> {
            if (optFF != null) optFF.setSelected(false);
            if (optFFMax != null) optFFMax.setSelected(false);
            selectedPkg = null;
            flipper.setDisplayedChild(4);
        });

        return root;
    }

    private LinearLayout toggleRow(String label, SwitchCompat sw) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.setClickable(true);
        row.setLayoutParams(new LinearLayout.LayoutParams(MATCH, WRAP));
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xFFDDDDDD);
        tv.setTextSize(16);
        row.addView(tv, new LinearLayout.LayoutParams(0, WRAP, 1f));
        row.addView(sw);
        row.setOnClickListener(v -> sw.toggle());
        return row;
    }

    private TextView makeCard(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(C_TEXT);
        tv.setTextSize(15);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(round(15, C_CARD));
        tv.setClickable(true);
        return tv;
    }

    private View buildApply() {
        LinearLayout ll = fullCenter();
        ll.setPadding(dp(20), 0, dp(20), 0);
        TextView title = new TextView(this);
        title.setText("ÁP DỤNG VÀO GAME");
        title.setTextColor(C_ACCENT);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(WRAP, WRAP);
        tlp.bottomMargin = dp(25);
        ll.addView(title, tlp);

        optFF = makeGameOption("Free Fire", PKG_FF);
        optFFMax = makeGameOption("Free Fire Max", PKG_FFMAX);
        ll.addView(optFF);
        ll.addView(optFFMax);

        TextView btnLaunch = primaryBtn("XÁC NHẬN");
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(WRAP, WRAP);
        blp.topMargin = dp(25);
        ll.addView(btnLaunch, blp);
        btnLaunch.setOnClickListener(v -> launchGame());

        TextView back = new TextView(this);
        back.setText("← Quay lại");
        back.setTextColor(C_SUB);
        back.setTextSize(14);
        back.setGravity(Gravity.CENTER);
        back.setPadding(0, dp(12), 0, 0);
        back.setClickable(true);
        back.setOnClickListener(v -> flipper.setDisplayedChild(3));
        ll.addView(back, new LinearLayout.LayoutParams(WRAP, WRAP));
        return ll;
    }

    private TextView makeGameOption(String name, String pkg) {
        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setTextColor(C_TEXT);
        tv.setTextSize(17);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(18), dp(18), dp(18), dp(18));
        tv.setTag(pkg);
        tv.setClickable(true);
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{ android.R.attr.state_selected }, optionBg(true));
        sld.addState(new int[]{}, optionBg(false));
        tv.setBackground(sld);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(320), WRAP);
        lp.bottomMargin = dp(10);
        tv.setLayoutParams(lp);
        tv.setOnClickListener(v -> {
            optFF.setSelected(false);
            optFFMax.setSelected(false);
            v.setSelected(true);
            selectedPkg = (String) v.getTag();
        });
        return tv;
    }

    private GradientDrawable optionBg(boolean selected) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(selected ? 0x1500E676 : C_CARD);
        g.setCornerRadius(dp(18));
        g.setStroke(dp(2), selected ? C_ACCENT : C_CARD);
        return g;
    }

    private void onToggle(String key, String name, boolean val) {
        try { settings.put(key, val); } catch (Exception ignored) {}
        saveSettingsAsync();
        toast(name + ": " + (val ? "BẬT" : "TẮT"));
    }

    private void resetSettings() {
        swAimbot.setChecked(false);
        swEsp.setChecked(false);
        swNoRecoil.setChecked(false);
        settings.remove("target_game");
        saveSettingsAsync();
        toast("Đã reset");
    }

    private void saveSettingsAsync() {
        final String json = settings.toString();
        io.execute(() -> {
            try {
                ConfigFile.write(this, json);
                NativeLib.setConfig(json);
            } catch (Throwable t) { Log.e(TAG, "save", t); }
        });
    }

    private void loadSettingsFromDisk() {
        io.execute(() -> {
            try {
                String raw = ConfigFile.read(this);
                final JSONObject obj = new JSONObject(raw);
                ui.post(() -> {
                    swAimbot.setChecked(obj.optBoolean("aimbot", false));
                    swEsp.setChecked(obj.optBoolean("esp", false));
                    swNoRecoil.setChecked(obj.optBoolean("no_recoil", false));
                });
            } catch (Exception e) { Log.e(TAG, "load", e); }
        });
    }

    private void launchGame() {
        if (selectedPkg == null) { toast("Vui lòng chọn game!"); return; }
        final String pkg = selectedPkg;
        final String gameName = pkg.equals(PKG_FF) ? "Free Fire" : "Free Fire Max";
        if (!isInstalled(pkg)) { toast("Chưa cài " + gameName + "!"); return; }
        try { settings.put("target_game", pkg); } catch (Exception ignored) {}
        toast("Đang áp dụng mod & vào " + gameName + "...");
        final String json = settings.toString();
        io.execute(() -> {
            try {
                ConfigFile.write(this, json);
                if (!NativeLib.inject(pkg)) {
                    ui.post(() -> toast("Inject thất bại"));
                    return;
                }
                final Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
                if (i == null) {
                    ui.post(() -> toast("Không mở được " + gameName));
                    return;
                }
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ui.post(() -> {
                    try {
                        startActivity(i);
                        toast("Đã vào " + gameName);
                    } catch (Exception e) {
                        toast("Lỗi mở game: " + e.getMessage());
                    }
                });
            } catch (Throwable t) {
                Log.e(TAG, "launch", t);
                ui.post(() -> toast("Lỗi: " + t.getMessage()));
            }
        });
    }

    private boolean isInstalled(String pkg) {
        if (pkg == null || !PKG_RE.matcher(pkg).matches()) return false;
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static class ConfigFile {
        private static final Object LOCK = new Object();
        private static File file(android.content.Context ctx) {
            return new File(ctx.getFilesDir(), "dangmod.cfg");
        }
        static void write(android.content.Context ctx, String json) throws IOException {
            File f = file(ctx);
            File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
            synchronized (LOCK) {
                try (FileOutputStream out = new FileOutputStream(tmp)) {
                    out.write(json.getBytes(StandardCharsets.UTF_8));
                    out.getFD().sync();
                }
                if (!tmp.renameTo(f)) throw new IOException("rename failed");
            }
        }
        static String read(android.content.Context ctx) throws IOException {
            File f = file(ctx);
            if (!f.exists()) return "{}";
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(
                    new FileInputStream(f), StandardCharsets.UTF_8))) {
                char[] buf = new char[4096];
                int n;
                while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
            }
            return sb.toString();
        }
    }

    static class NativeLib {
        static final boolean DRY_RUN = true;
        static boolean inject(String pkg) {
            if (DRY_RUN) { Log.i(TAG, "[dry-run] inject " + pkg); return true; }
            return false;
        }
        static void setConfig(String json) {
            if (DRY_RUN) { Log.i(TAG, "[dry-run] setConfig " + json); return; }
        }
    }
}
