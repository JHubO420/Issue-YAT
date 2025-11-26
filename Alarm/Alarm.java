package com.example.issueyat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

    // 레이아웃 덩어리들
    ViewGroup layoutMainList;       // 목록 화면
    ViewGroup layoutSettingContainer; // 설정 화면
    ViewGroup layoutAlarmRinging;     // [신규] 알람 울림 화면

    // 알람 울림 화면의 뷰들
    TextView tvRingingTime, tvRingingDate;
    CardView btnGo;

    // 기존 뷰들
    LinearLayout alarmContainer;
    ImageView btnAdd;
    Button btnSave;
    TimePicker timePicker;

    // 소리 및 진동 제어용 변수
    private static Ringtone ringtone;
    private static Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 뷰 연결 (바인딩)
        layoutMainList = findViewById(R.id.layout_main_list);
        layoutSettingContainer = findViewById(R.id.layout_setting_container);
        layoutAlarmRinging = findViewById(R.id.layout_alarm_ringing);

        tvRingingTime = findViewById(R.id.tvRingingTime);
        tvRingingDate = findViewById(R.id.tvRingingDate);
        btnGo = findViewById(R.id.btnGo);

        alarmContainer = findViewById(R.id.alarmContainer);
        btnAdd = findViewById(R.id.btnAdd);
        btnSave = findViewById(R.id.btnSave);
        timePicker = findViewById(R.id.timePicker);

        // 2. 버튼 리스너 설정
        btnAdd.setOnClickListener(v -> layoutSettingContainer.setVisibility(View.VISIBLE));

        btnSave.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            calendar.set(Calendar.MINUTE, timePicker.getMinute());
            calendar.set(Calendar.SECOND, 0);
            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DATE, 1);
            }
            setAlarm(calendar);
            addAlarmView(calendar);
            layoutSettingContainer.setVisibility(View.GONE);
        });

        // [신규] GO 버튼 클릭 시 알람 해제
        btnGo.setOnClickListener(v -> stopAlarm());

        // 3. 알람 때문에 앱이 켜진 것인지 확인
        checkIfAlarmIsRinging(getIntent());
    }

    // 앱이 이미 켜져있을 때 알람 신호가 오면 이 함수가 실행됨
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // 새 인텐트로 교체
        checkIfAlarmIsRinging(intent);
    }

    // 인텐트를 확인해서 알람 화면을 띄울지 결정
    private void checkIfAlarmIsRinging(Intent intent) {
        if (intent != null && intent.getBooleanExtra("ALARM_RINGING", false)) {
            showAlarmScreen();
        }
    }

    // [신규] 알람 화면 보여주기 & 소리/진동 시작
    private void showAlarmScreen() {
        // 다른 화면 숨기고 알람 화면 표시
        layoutMainList.setVisibility(View.GONE);
        layoutSettingContainer.setVisibility(View.GONE);
        layoutAlarmRinging.setVisibility(View.VISIBLE);

        // 현재 시간과 날짜 표시
        Calendar now = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("H:mm", Locale.KOREA);
        SimpleDateFormat dateFormat = new SimpleDateFormat("M월 d일 EEEE", Locale.KOREA);
        tvRingingTime.setText(timeFormat.format(now.getTime()));
        tvRingingDate.setText(dateFormat.format(now.getTime()));

        // 화면 깨우기 (잠겨있어도 보이게)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // 소리 및 진동 시작
        startAlarmSoundAndVibration();
    }

    // [신규] 소리와 진동 시작
    private void startAlarmSoundAndVibration() {
        try {
            // 소리 재생
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
            ringtone.play();

            // 진동 시작
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                // 1초 진동, 1초 쉼 반복
                vibrator.vibrate(new long[]{0, 1000, 1000}, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // [신규] 알람 해제 (GO 버튼 클릭 시)
    private void stopAlarm() {
        // 소리와 진동 멈춤
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }

        // 알람 화면 숨기고 원래 목록 화면 표시
        layoutAlarmRinging.setVisibility(View.GONE);
        layoutMainList.setVisibility(View.VISIBLE);

        // 화면 깨우기 플래그 해제 (구버전 호환)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    // 뒤로가기 처리
    @Override
    public void onBackPressed() {
        // 알람이 울리는 중이면 뒤로가기 막음 (GO 버튼을 눌러야 함)
        if (layoutAlarmRinging.getVisibility() == View.VISIBLE) {
            return;
        }
        if (layoutSettingContainer.getVisibility() == View.VISIBLE) {
            layoutSettingContainer.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    // 목록에 카드 추가 (기존 기능)
    private void addAlarmView(Calendar calendar) {
        View alarmView = LayoutInflater.from(this).inflate(R.layout.item_alarm, alarmContainer, false);
        TextView tvDate = alarmView.findViewById(R.id.tvDate);
        TextView tvTime = alarmView.findViewById(R.id.tvTime);
        tvDate.setText("매일");
        SimpleDateFormat timeFormat = new SimpleDateFormat("a h:mm", Locale.KOREA);
        tvTime.setText(timeFormat.format(calendar.getTime()));
        alarmContainer.addView(alarmView, 0);
    }

    // 알람 등록 (기존 기능)
    private void setAlarm(Calendar calendar) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);

        // [중요 1] 알람마다 고유한 ID 만들기 (설정된 시간을 ID로 사용)
        // 이렇게 해야 알람들이 서로 덮어씌워지지 않고 모두 살아있습니다.
        int requestCode = (int) calendar.getTimeInMillis();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode, // 0 대신 고유 ID를 넣습니다.
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            // 권한 체크 (안드로이드 12 이상)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Intent permissionIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(permissionIntent);
                    return;
                }
            }

            try {
                // [중요 2] 정확한 시간에 울리게 하기 위해 setRepeating 대신 setExactAndAllowWhileIdle 사용
                // (참고: setRepeating은 안드로이드 정책상 시간이 정확하지 않습니다)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAlarmClock(
                            new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent),
                            pendingIntent
                    );
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }

                Toast.makeText(this, "알람이 설정되었습니다.", Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                Toast.makeText(this, "권한 오류 발생", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // =================================================
    // [수정됨] 알람 리시버 (이제 소리 재생 안 함)
    // =================================================
    public static class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 알람 시간이 되면 MainActivity를 깨우는 신호를 보냄
            Intent activityIntent = new Intent(context, MainActivity.class);
            // "알람이 울려서 실행된 거야!" 라는 표시를 남김
            activityIntent.putExtra("ALARM_RINGING", true);
            // 앱이 꺼져있어도 실행되도록 플래그 설정
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(activityIntent);
        }
    }
}
