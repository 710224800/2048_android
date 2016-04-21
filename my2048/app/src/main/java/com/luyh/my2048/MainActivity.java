package com.luyh.my2048;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.Image;
import android.nfc.Tag;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.kyview.AdViewStream;
import com.kyview.AdViewTargeting;
import com.kyview.interfaces.AdInstlInterface;
import com.kyview.interfaces.AdViewInterface;
import com.kyview.screen.interstitial.AdInstlManager;
import com.phkg.b.BManager;
import com.phkg.b.MyBMDevListner;

import net.youmi.android.AdManager;
import net.youmi.android.banner.AdSize;
import net.youmi.android.banner.AdView;
import net.youmi.android.banner.AdViewListener;

public class MainActivity extends Activity implements AdViewInterface, AdInstlInterface {

    protected final static String TAG = "MainActivity";

    private MainView view;
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String SCORE = "score";
    public static final String HIGH_SCORE = "high score temp";
    public static final String UNDO_SCORE = "undo score";
    public static final String CAN_UNDO = "can undo";
    public static final String UNDO_GRID = "undo";
    public static final String GAME_STATE = "game state";
    public static final String UNDO_GAME_STATE = "undo game state";

    private AdInstlManager adInstlManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

//        AdViewTargeting.setUpdateMode(AdViewTargeting.UpdateMode.EVERYTIME); // 功能为每次都从服务器获取最新的设置，方便您的调试，当调 试结束后，一定要去掉这句话，因为系统已经为您优化成最佳时间，同时不影响应用本身的性能。
        AdViewTargeting.setAdSize(AdViewTargeting.AdSize.BANNER_AUTO_FILL);
        // 设置横幅可关闭
        AdViewTargeting.setBannerSwitcherMode(AdViewTargeting.BannerSwitcher.CANCLOSED);
        // 设置插屏可关闭
        AdViewTargeting.setInstlSwitcherMode(AdViewTargeting.InstlSwitcher.CANCLOSED);
        // 设置插屏模式
        AdViewTargeting
                .setInstlDisplayMode(AdViewTargeting.InstlDisplayMode.DIALOG_MODE);

        adInstlManager = new AdInstlManager(this,
                "SDK20161021100413hvp059mhx8l3i2p");
        adInstlManager.setAdInstlInterface(this);

        view = new MainView(getBaseContext());

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        view.hasSaveState = settings.getBoolean("save_state", false);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("hasState")) {
                load();
            }
        }
        setContentView(view);
        btnCode2();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            //Do nothing
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            view.game.move(2);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            view.game.move(0);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            view.game.move(3);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            view.game.move(1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private AdViewStream adStream;
    private LinearLayout layout, layoutXml, layoutCustom;

    public void btnCode2() {
        if (adStream != null)
            adStream.setClosed(true);
        if (null != adStream) {
            ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
            for (int i = 0; i < rootView.getChildCount(); i++) {
                if (rootView.getChildAt(i) == adStream) {
                    rootView.removeView(adStream);
                }
            }
        }
        if (layout != null)
            layout.removeAllViews();
        adStream = new AdViewStream(this, "SDK20161021100413hvp059mhx8l3i2p");
        adStream.setAdViewInterface(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM;
        this.addContentView(adStream, params);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("hasState", true);
        save();
    }

    protected void onPause() {
        super.onPause();
        save();
    }

    private void save() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        Tile[][] field = view.game.grid.field;
        Tile[][] undoField = view.game.grid.undoField;
        editor.putInt(WIDTH, field.length);
        editor.putInt(HEIGHT, field.length);
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field[0].length; yy++) {
                if (field[xx][yy] != null) {
                    editor.putInt(xx + " " + yy, field[xx][yy].getValue());
                } else {
                    editor.putInt(xx + " " + yy, 0);
                }

                if (undoField[xx][yy] != null) {
                    editor.putInt(UNDO_GRID + xx + " " + yy, undoField[xx][yy].getValue());
                } else {
                    editor.putInt(UNDO_GRID + xx + " " + yy, 0);
                }
            }
        }
        editor.putLong(SCORE, view.game.score);
        editor.putLong(HIGH_SCORE, view.game.highScore);
        editor.putLong(UNDO_SCORE, view.game.lastScore);
        editor.putBoolean(CAN_UNDO, view.game.canUndo);
        editor.putInt(GAME_STATE, view.game.gameState);
        editor.putInt(UNDO_GAME_STATE, view.game.lastGameState);
        editor.commit();
    }

    private boolean isFirstResume = true;

    protected void onResume() {
        super.onResume();
        load();
        if (isFirstResume) {
            isFirstResume = false;
        } else {
            adInstlManager.requestAndshow();
        }
    }

    private void load() {
        //Stopping all animations
        view.game.aGrid.cancelAnimations();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        for (int xx = 0; xx < view.game.grid.field.length; xx++) {
            for (int yy = 0; yy < view.game.grid.field[0].length; yy++) {
                int value = settings.getInt(xx + " " + yy, -1);
                if (value > 0) {
                    view.game.grid.field[xx][yy] = new Tile(xx, yy, value);
                } else if (value == 0) {
                    view.game.grid.field[xx][yy] = null;
                }

                int undoValue = settings.getInt(UNDO_GRID + xx + " " + yy, -1);
                if (undoValue > 0) {
                    view.game.grid.undoField[xx][yy] = new Tile(xx, yy, undoValue);
                } else if (value == 0) {
                    view.game.grid.undoField[xx][yy] = null;
                }
            }
        }

        view.game.score = settings.getLong(SCORE, view.game.score);
        view.game.highScore = settings.getLong(HIGH_SCORE, view.game.highScore);
        view.game.lastScore = settings.getLong(UNDO_SCORE, view.game.lastScore);
        view.game.canUndo = settings.getBoolean(CAN_UNDO, view.game.canUndo);
        view.game.gameState = settings.getInt(GAME_STATE, view.game.gameState);
        view.game.lastGameState = settings.getInt(UNDO_GAME_STATE, view.game.lastGameState);
    }

    @Override
    public void onClickAd() {
        // TODO Auto-generated method stub
        Log.i("AdBannerActivity", "onClickAd");
    }

    @Override
    public void onClosedAd() {
        // TODO Auto-generated method stub
        // 如果想立即关闭直接调用：
        // adStream.setClosed(true);

        // 弹出对话框，要求二次确认
        Dialog dialog = new AlertDialog.Builder(this).setTitle("确定要关闭广告？")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 无论是否关闭广告，请务必调用下一行方法，否则广告将停止切换
                        // 传入false，广告将不会关闭
                        adStream.setClosed(false);
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 无论是否关闭广告，请务必调用下一行方法，否则广告将停止切换
                        // 传入true，广告将关闭
                        adStream.setClosed(true);
                    }
                }).show();
        // 防止误点击关闭对话框，可能使 adStream.setClosed(boolean);不被调用
        dialog.setCanceledOnTouchOutside(false);
    }

    @Override
    public void onDisplayAd() {
        // TODO Auto-generated method stub
        Log.i("AdBannerActivity", "onDisplayAd");
    }

    @Override
    public void onAdDismiss() {

    }

    @Override
    public void onReceivedAd(int arg0, View arg1) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        Log.i("AdInstlActivity", "onReceivedAd");
//                Toast.makeText(MainActivity.this, "ReceivedAd",
//                        Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    @Override
    public void onReceivedAdFailed(final String error) {
//        runOnUiThread(new Runnable() {
//
//            @Override
//            public void run() {
        Log.i("AdInstlActivity", "onReceivedAd" + error);
//                Toast.makeText(MainActivity.this, "onReceiveAdFailed" + error,
//                        Toast.LENGTH_SHORT).show();
//            }
//        });
    }
}
