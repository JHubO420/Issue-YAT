package com.example.issueyat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // [화면 1] 메인 목록 관련
    ViewGroup layoutMainList;
    LinearLayout layoutSettingContainer;
    LinearLayout alarmContainer;

    ImageView btnAdd, btnDelete;
    Button btnSave;
    TimePicker timePicker;

    // [화면 2] 알람 울림 관련
    ViewGroup layoutAlarmRinging;
    TextView tvRingingTime, tvRingingDate;
    CardView btnGo;

    // 상태 변수
    boolean isDeleteMode = false;
    private static Ringtone ringtone;
    private static Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 뷰 연결 (ID 확인 필수)
        layoutMainList = findViewById(R.id.layout_main_list); // xml에 이 ID가 없는 경우 ConstraintLayout(최상위)에 ID를 주거나 해야 하지만, 보통 activity_main.xml 구조상 루트가 아니라면 찾아서 할당.
        // *주의*: activity_main.xml 루트가 ConstraintLayout이면 findViewById로 못 찾을 수 있음.
        // 현재 구조상 layout_alarm_ringing과 겹치기 위해 FrameLayout이나 ConstraintLayout 안에 있을 것입니다.
        // 만약 에러나면 activity_main.xml의 가장 바깥 레이아웃이 아니라, 목록을 감싸는 레이아웃에 ID가 있는지 확인해야 합니다.
        // (아래에서 다시 설명)

        // 편의상 layoutMainList를 찾지 못할 경우를 대비해, 알람 화면만 VISIBLE/GONE 처리하는 방식으로 구현하겠습니다.
        layoutSettingContainer = findViewById(R.id.layout_setting_container);
        alarmContainer = findViewById(R.id.alarmContainer);

        btnAdd = findViewById(R.id.btnAdd);
        btnDelete = findViewById(R.id.btnDelete);
        btnSave = findViewById(R.id.btnSave);
        timePicker = findViewById(R.id.timePicker);

        layoutAlarmRinging = findViewById(R.id.layout_alarm_ringing);
        tvRingingTime = findViewById(R.id.tvRingingTime);
        tvRingingDate = findViewById(R.id.tvRingingDate);
        btnGo = findViewById(R.id.btnGo);

        // 2. 버튼 리스너
        btnAdd.setOnClickListener(v -> {
            if (isDeleteMode) toggleDeleteMode();
            layoutSettingContainer.setVisibility(View.VISIBLE);
        });

        btnDelete.setOnClickListener(v -> toggleDeleteMode());

        btnSave.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            calendar.set(Calendar.MINUTE, timePicker.getMinute());
            calendar.set(Calendar.SECOND, 0);

            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DATE, 1);
            }

            int requestCode = (int) calendar.getTimeInMillis(); // 고유 ID
            setAlarm(calendar, requestCode);
            addAlarmView(calendar, requestCode);
            layoutSettingContainer.setVisibility(View.GONE);
        });

        // [GO] 버튼: 알람 해제
        btnGo.setOnClickListener(v -> stopAlarm());

        // 3. 알람 신호 확인
        checkIfAlarmIsRinging(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkIfAlarmIsRinging(intent);
    }

    // 알람이 울려서 앱이 켜졌는지 확인
    private void checkIfAlarmIsRinging(Intent intent) {
        if (intent != null && intent.getBooleanExtra("ALARM_RINGING", false)) {
            showAlarmScreen();
        }
    }

    // [보라색 화면] 알람 화면 띄우기
    private void showAlarmScreen() {
        // 설정 화면 등은 숨기고 알람 화면 표시
        layoutSettingContainer.setVisibility(View.GONE);
        if (layoutAlarmRinging != null) {
            layoutAlarmRinging.setVisibility(View.VISIBLE);
        }

        // 현재 시간 표시
        Calendar now = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("H:mm", Locale.KOREA);
        SimpleDateFormat dateFormat = new SimpleDateFormat("M월 d일 EEEE", Locale.KOREA);

        if (tvRingingTime != null) tvRingingTime.setText(timeFormat.format(now.getTime()));
        if (tvRingingDate != null) tvRingingDate.setText(dateFormat.format(now.getTime()));

        // 화면 깨우기
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        startAlarmSoundAndVibration();
    }

    private void startAlarmSoundAndVibration() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
            ringtone.play();

            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(new long[]{0, 1000, 1000}, 0);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // [GO 버튼] 알람 끄기
    private void stopAlarm() {
        if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
        if (vibrator != null) vibrator.cancel();

        layoutAlarmRinging.setVisibility(View.GONE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    // [삭제 기능] 모드 전환
    private void toggleDeleteMode() {
        isDeleteMode = !isDeleteMode;
        if (isDeleteMode) {
            btnDelete.setColorFilter(Color.RED);
            Toast.makeText(this, "삭제할 알람을 선택하세요.", Toast.LENGTH_SHORT).show();
        } else {
            btnDelete.setColorFilter(Color.WHITE);
        }
    }

    // [삭제 기능] 알람 취소
    private void cancelAlarm(int requestCode) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    // 목록에 알람 추가
    private void addAlarmView(Calendar calendar, int requestCode) {
        View alarmView = LayoutInflater.from(this).inflate(R.layout.item_alarm, alarmContainer, false);
        TextView tvDate = alarmView.findViewById(R.id.tvDate);
        TextView tvTime = alarmView.findViewById(R.id.tvTime);

        tvDate.setText("매일");
        SimpleDateFormat timeFormat = new SimpleDateFormat("a h:mm", Locale.KOREA);
        tvTime.setText(timeFormat.format(calendar.getTime()));

        alarmView.setTag(requestCode); // ID 저장

        alarmView.setOnClickListener(v -> {
            if (isDeleteMode) {
                int id = (int) v.getTag();
                cancelAlarm(id);
                alarmContainer.removeView(v);
                Toast.makeText(this, "알람이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        alarmContainer.addView(alarmView, 0);
    }

    // 알람 등록 (정확한 시간 + 고유 ID)
    private void setAlarm(Calendar calendar, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                return;
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent), pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
                Toast.makeText(this, "알람이 설정되었습니다.", Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                Toast.makeText(this, "권한 오류 발생", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (layoutAlarmRinging.getVisibility() == View.VISIBLE) return; // 알람 중 뒤로가기 방지
        if (layoutSettingContainer.getVisibility() == View.VISIBLE) {
            layoutSettingContainer.setVisibility(View.GONE);
        } else if (isDeleteMode) {
            toggleDeleteMode();
        } else {
            super.onBackPressed();
        }
    }

    // ==========================================================
    // [중요] 알람 리시버: 토스트가 아니라 Activity를 실행해야 함!
    // ==========================================================
    public static class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // "일어나세요" 토스트는 삭제! 화면을 띄워야 합니다.
            Intent activityIntent = new Intent(context, MainActivity.class);
            activityIntent.putExtra("ALARM_RINGING", true); // 신호 보내기
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(activityIntent);
        }
    }
}
