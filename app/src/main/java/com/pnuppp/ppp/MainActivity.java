package com.pnuppp.ppp;

import android.Manifest;
import android.os.Build;
import android.provider.Settings;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Calendar;

import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;
    private static final int REQUEST_CODE_SCHEDULE_EXACT_ALARM = 2;
    private MaterialCalendarView calendarView;
    private DataBase db;
    private EventDao eventDao;
    private Map<String, String> events = new HashMap<>();
    private Button recentSetNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        calendarView = findViewById(R.id.calendarView);
        db = Room.databaseBuilder(getApplicationContext(), DataBase.class, "event-database")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
        eventDao = db.eventDao();

        initializeDatabase();

        // POST_NOTIFICATIONS 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
        }
        else { // 권한이 이미 부여된 경우
            loadEvents();
        }
        // 정확한 알람 권한 요청 (API 31 이상에서만 가능)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivityForResult(intent, REQUEST_CODE_SCHEDULE_EXACT_ALARM);
            }
        }

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            String selectedDate = formatDateForMap(date);
            String eventDescription = events.get(selectedDate);
            showEventDialog(selectedDate, eventDescription);
        });

        //버튼
        Button buttonInfoEdit = findViewById(R.id.button_info_edit);
        buttonInfoEdit.setOnClickListener(v -> {
            Intent newIntent = new Intent(MainActivity.this, InfoEditActivity.class);
            startActivity(newIntent);
        });

        Button buttonnewnew = findViewById(R.id.button5);
        buttonnewnew.setOnClickListener(v -> {
            Intent newIntent = new Intent(MainActivity.this, newnewActivity.class);
            startActivity(newIntent);
        });


        Button buttonexpect = findViewById(R.id.button7);
        buttonexpect.setOnClickListener(v -> {
            Intent newIntent = new Intent(MainActivity.this, newnew2Activity.class);
            startActivity(newIntent);
        });

        Button buttonMyGPA = findViewById(R.id.button3);
        buttonMyGPA.setOnClickListener(v -> {
            Intent myIntent = new Intent(MainActivity.this, GPACalculatorActivity.class);
            startActivity(myIntent);
        });

        Button extraCurricularButton = findViewById(R.id.button_extra);
        extraCurricularButton.setOnClickListener(v->{
            Intent intent = new Intent(MainActivity.this, ExtraCurricularActivity.class);
            startActivity(intent);
        });

        // 공지사항 관련 버튼 설정
        Button noticeButton = findViewById(R.id.noticeButton);
        noticeButton.setOnClickListener(v -> openDepartmentNotice());

        TextView textView8 = findViewById(R.id.textView8);
        TextView textView9 = findViewById(R.id.textView9);

        Intent intent = getIntent();
        String major = intent.getStringExtra("major");
        String studentID = intent.getStringExtra("student_id");
        String name = intent.getStringExtra("name");

        if (major != null && studentID != null && name != null) {
            textView8.setText(major);
            textView9.setText(studentID + " " + name);
        } else {
            SharedPreferences sharedPref = getSharedPreferences("com.pnuppp.pplusplus.user_info", MODE_PRIVATE);
            if (sharedPref.contains("major") && sharedPref.contains("student_id") && sharedPref.contains("name")) {
                String savedMajor = sharedPref.getString("major", "");
                String savedStudentID = sharedPref.getString("student_id", "");
                String savedName = sharedPref.getString("name", "");

                textView8.setText(savedMajor);
                textView9.setText(savedStudentID + " " + savedName);
            }
        }
    }

    private void openDepartmentNotice() {
        String userDepartment = getUserDepartment();

        if (isRssDepartment(userDepartment)) {
            Intent intent = new Intent(MainActivity.this, RSSDepartmentNoticeActivity.class);
            intent.putExtra("major", userDepartment);
            startActivity(intent);
        } else if (isNonRssDepartment(userDepartment)) {
            Intent intent = new Intent(MainActivity.this, HtmlDepartmentNoticeActivity.class);
            intent.putExtra("major", userDepartment);
            startActivity(intent);
        } else {
            Log.e("MainActivity", "해당 학과의 공지사항 형식을 찾을 수 없습니다.");
        }
    }

    // 사용자의 학과 정보를 가져오는 메소드
    private String getUserDepartment() {
        SharedPreferences sharedPref = getSharedPreferences("com.pnuppp.pplusplus.user_info", MODE_PRIVATE);
        return sharedPref.getString("major", "정보컴퓨터공학부 컴퓨터공학전공"); // 기본값은 빈 문자열
    }

    private boolean isRssDepartment(String department) {
        for (String rssDepartment : rssDepartments) {
            if (rssDepartment.equals(department)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNonRssDepartment(String department) {
        for (String nonRssDepartment : nonRssDepartments) {
            if (nonRssDepartment.equals(department)) {
                return true;
            }
        }
        return false;
    }

    private String[] rssDepartments = new String[]{
            // RSS인 학과 리스트
            "국어국문학과", "일어일문학과", "불어불문학과", "노어노문학과",
            "중어중문학과", "영어영문학과", "독어독문학과", "언어정보학과", "사학과",
            // 사회과학대학
            "행정학과", "정치외교학과", "사회학과", "심리학과", "문헌정보학과", "미디어커뮤니케이션학과",
            // 자연과학대학
            "수학과", "통계학과", "화학과", "생명과학과", "미생물학과", "분자생물학과", "지질환경과학과",
            "대기환경과학과", "해양학과",
            // 공과대학
            "고분자공학과", "유기소재시스템공학과", "전기전자공학부 전자공학전공", "전기전자공학부 전기공학전공",
            "조선해양공학과", "재료공학부", "항공우주공학과", "건축공학과", "건축학과", "도시공학과", "사회기반시스템공학과",
            // 사범대학
            "국어교육과", "영어교육과", "독어교육과", "불어교육과", "교육학과", "유아교육과", "특수교육과", "일반사회교육과",
            "역사교육과", "지리교육과", "윤리교육과", "수학교육과", "물리교육과", "화학교육과", "생물교육과", "지구과학교육과", "체육교육과",
            // 경제통상대학
            "무역학부", "경제학부", "관광컨벤션학과", "공공정책학부",
            // 경영대학
            "경영학과",
            // 약학대학
            "약학전공", "제약학전공",
            // 생활과학대학
            "아동가족학과", "의류학과", "식품영양학과",
            // 예술대학
            "음악학과", "한국음악학과", "미술학과", "조형학과", "디자인학과", "무용학과", "예술문화영상학과",
            // 나노과학기술대학
            "나노메카트로닉스공학과", "나노에너지공학과", "광메카트로닉스공학과",
            // 생명자원과학대학
            "식물생명과학과", "원예생명과학과", "동물생명자원과학과", "식품공학과", "생명환경화학과", "바이오소재과학과",
            "바이오산업기계공학과", "조경학과", "식품자원경제학과", "IT응용공학과", "바이오환경에너지학과",
            // 간호대학
            "간호학과",
            // 의과대학
            "의예과", "의학과", "의과학과",
            // 정보의생명공학대학
            "정보컴퓨터공학부 컴퓨터공학전공", "정보컴퓨터공학부 인공지능전공", "의생명융합공학부",
            // 첨단융합학부
            "첨단융합학부 나노자율전공", "첨단융합학부 정보의생명공학자율전공"
    };

    private String[] nonRssDepartments = new String[]{
            // HTML인 학과 리스트
            "한문학과", "철학과", "고고학과", "사회복지학과", "물리학과",
            "기계공학부", "화공생명환경공학부 화공생명공학전공",
            "화공생명환경공학부 환경공학전공", "전기전자공학부 반도체공학전공",
            "산업공학과", "국제학부", "실내환경디자인학과", "스포츠과학과", "첨단융합학부 공학자율전공"
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadEvents();
            }
            else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("권한 거부됨")
                        .setMessage("이 앱이 정상적으로 작동하려면 알림 권한이 필요합니다.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }
    }

    private void initializeDatabase() {
        String[][] eventData = {
                {"2024-03-04", "1학기 개강\n1학기 1차 수강 정정 시작\n(학부, 타대생, 대학원 08:00~)"},
                {"2024-03-05", "1학기 휴·복학 신청 기간 시작"},
                {"2024-03-07", "1학기 휴·복학 신청 기간 끝"},
                {"2024-03-08", "1학기 1차 수강 정정 마감\n(학부, 타대생 ~17:00)\n(대학원 ~23:50)"},
                {"2024-03-11", "1학기 후기 학위 청구 자격 종합 시험 시작"},
                {"2024-03-15", "1학기 2차 폐강 강좌 공고\n1학기 후기 학위 청구 자격 종합 시험 끝"},
                {"2024-03-18", "1학기 2차 수강 정정 시작\n(학부, 타대생, 대학원 10:00~)"},
                {"2024-03-19", "1학기 2차 수강 정정 마감\n(학부, 타대생, 대학원 ~17:00)\n1학기 휴·복학 신청 시작\n1학기 수료후 연구생 등록금 납부 시작\n1학기 재학생 차등 납부 등록 시작"},
                {"2024-03-20", "1학기 확정 출석부 출력"},
                {"2024-03-21", "1학기 휴·복학 신청 마감\n1학기 수료후 연구생 등록금 납부 마감\n1학기 재학생 차등 납부 등록 마감"},

                {"2024-04-02", "1학기 수강 취소 시작"},
                {"2024-04-04", "1학기 후기 학위 청구 심사용 논문 제출"},
                {"2024-04-08", "1학기 수강 취소 마감\n1학기 수업일수 1/3선"},
                {"2024-04-22", "1학기 중간고사 시작"},
                {"2024-04-26", "1학기 수업일수 1/2선"},
                {"2024-04-27", "1학기 중간고사 끝"},

                {"2024-05-01", "여름 계절 및 도약 수업 복학 신청 시작"},
                {"2024-05-07", "여름 계절 및 도약 수업 복학 신청 마감"},
                {"2024-05-09", "여름 계절 수업 희망 과목 담기 시작"},
                {"2024-05-10", "여름 계절 수업 희망 과목 담기 마감"},
                {"2024-05-11", "여름 계절 수업 자동 신청 결과 확인"},
                {"2024-05-16", "1학기 수업일수 2/3선\n여름 계절 및 도약 수업 복학 신청 시작\n여름 계절 수업 수강 신청 시작(학부, 타대생, 대학원 08:00~)"},
                {"2024-05-20", "여름 계절 및 도약 수업 복학 신청 마감\n여름 계절 수업 수강 신청 마감(학부, 타대생, 대학원 ~17:00)"},
                {"2024-05-24", "여름 계절 수업 1차 폐강 강좌 공고"},
                {"2024-05-27", "2학기 재입학 신청 시작(학부)\n여름 계절 수업 1차 수강 정정 시작(학부, 타대생, 대학원 10:00~)\n여름 계절 및 도약 수업 복학 신청 시작"},
                {"2024-05-28", "여름 계절 수업 1차 수강 정정 마감(학부, 타대생, 대학원 ~17:00)\n여름 계절 및 도약 수업 복학 신청 마감"},

                {"2024-06-03", "여름 계절 수업 2차 폐강 강좌 공고"},
                {"2024-06-04", "여름 계절 수업 2차 수강 정정 시작(학부, 타대생, 대학원 10:00~)\n여름 계절 및 도약 수업 복학 신청 기간"},
                {"2024-06-05", "여름 계절 수업 2차 수강 정정 마감(학부, 타대생, 대학원 ~17:00)\n여름 계절 및 도약 수업 복학 신청 기간"},
                {"2024-06-07", "2학기 재입학 신청 마감(학부)"},
                {"2024-06-13", "여름 계절 수업 등록금 납부 시작"},
                {"2024-06-15", "1학기 기말고사 시작"},
                {"2024-06-17", "여름 계절 수업 등록금 납부 마감"},
                {"2024-06-21", "1학기 종강\n1학기 기말고사 끝"},
                {"2024-06-24", "여름 계절 수업 개강"},

                {"2024-07-01", "1학기 성적 입력 마감"},
                {"2024-07-05", "1학기 후위 학위 논문 심사 결과 보고서 및 최종 논문 제출"},
                {"2024-07-15", "2학기 교수 계획표 입력 시작"},
                {"2024-07-19", "여름 계절 수업 종강"},
                {"2024-07-26", "2학기 교수 계획표 입력 마감\n2학기 휴·복학 신청 기간 시작"},

                {"2024-08-02", "2학기 휴·복학 신청 기간 마감"},
                {"2024-08-06", "2학기 희망 과목 담기 시작"},
                {"2024-08-07", "2학기 희망 과목 담기 마감"},
                {"2024-08-08", "2학기 자동 신청 결과 확인"},
                {"2024-08-12", "2학기 1차 수강 신청 시작\n(학부, 타대생, 대학원 08:00~)"},
                {"2024-08-14", "2학기 1차 수강 신청 마감\n(학부, 타대생, 대학원 ~17:00)"},
                {"2024-08-19", "2학기 2차 수강 신청 시작\n(학부, 타대생, 대학원 10:00~)"},
                {"2024-08-20", "2학기 2차 수강 신청 마감\n(학부, 타대생, 대학원 ~17:00)\n2학기 등록금 납부 시작"},
                {"2024-08-23", "2학기 등록금 납부 마감"},
                {"2024-08-27", "2학기 1차 폐강 강좌 공고"},
                {"2024-08-30", "1학기 후기 학위 수여식"},

                {"2024-09-02", "2학기 개강\n2학기 1차 수강 정정 시작\n(학부, 타대생, 대학원 08:00~)"},
                {"2024-09-06", "2학기 1차 수강 정정 마감\n(학부, 타대생 ~17:00)\n(대학원 ~23:50)"},
                {"2024-09-13", "2학기 2차 폐강 강좌 공고"},
                {"2024-09-19", "2학기 2차 수강 정정 시작\n(학부, 타대생, 대학원 10:00~)"},
                {"2024-09-20", "2학기 2차 수강 정정 마감\n(학부, 타대생, 대학원 ~17:00)"},
                {"2024-09-23", "2학기 확정 출석부 출력\n2학기 전기 학위 청구 자격 종합 시험 시작"},
                {"2024-09-27", "2학기 전기 학위 청구 자격 종합 시험 끝"},

                {"2024-10-04", "2학기 수강 취소 기간 시작"},
                {"2024-10-11", "2학기 수강 취소 기간 끝\n2학기 수업일수 1/3선\n2학기 전기 학위 청구 심사용 논문 제출"},
                {"2024-10-21", "2학기 중간고사 시작"},
                {"2024-10-26", "2학기 중간고사 끝"},
                {"2024-10-31", "2학기 수업일수 1/2선"},

                {"2024-11-11", "겨울 계절 수업 희망 과목 담기 시작"},
                {"2024-11-12", "겨울 계절 수업 희망 과목 담기 마감"},
                {"2024-11-13", "겨울 계절 수업 자동 신청 결과 확인"},
                {"2024-11-15", "겨울 계절 수업 수강 신청 시작\n(학부, 타대생, 대학원)\n2학기 수업일수 2/3선"},
                {"2024-11-19", "겨울 계절 수업 수강 신청 마감\n(학부, 타대생, 대학원)"},
                {"2024-11-25", "겨울 계절 수업 1차 폐강 강좌 공고"},
                {"2024-11-26", "겨울 계절 수업 1차 수강 정정 시작\n(학부, 타대생, 대학원 10:00~)"},
                {"2024-11-27", "겨울 계절 수업 1차 수강 정정 마감\n(학부, 타대생, 대학원 ~17:00)"},

                {"2024-12-03", "겨울 계절 수업 2차 폐강 강좌 공고"},
                {"2024-12-04", "겨울 계절 수업 2차 수강 정정 시작\n(학부, 타대생, 대학원 10:00~)"},
                {"2024-12-05", "겨울 계절 수업 2차 수강 정정 마감\n(학부, 타대생, 대학원 ~17:00)"},
                {"2024-12-12", "겨울 계절 수업 등록금 납부 시작"},
                {"2024-12-14", "2학기 기말고사 시작"},
                {"2024-12-16", "겨울 계절 수업 등록금 납부 마감"},
                {"2024-12-20", "2학기 기말고사 끝\n2학기 종강"},
                {"2024-12-21", "겨울 계절 개강"},

                {"2025-01-17", "겨울 계절 종강"},
                {"2025-01-24", "1학기 휴·복학 신청 시작"},

                {"2025-02-04", "1학기 휴·복학 신청 마감"},
                {"2025-02-06", "1학기 희망 과목 담기 시작"},
                {"2025-02-07", "1학기 희망 과목 담기 마감"},
                {"2025-02-12", "1학기 1차 수강 신청 시작\n(학부, 타대생, 대학원 08:00~)"},
                {"2025-02-14", "1학기 1차 수강 신청 마감\n(학부, 타대생 ~17:00)\n(대학원 ~23:50)"},
                {"2025-02-17", "1학기 1차 수강 신청 시작(신입생)"},
                {"2025-02-18", "1학기 등록금 납부 시작\n1학기 1차 수강 신청 마감(신입생)"},
                {"2025-02-19", "1학기 2차 수강 신청 시작\n(학부, 타대생, 대학원 10:00~)"},
                {"2025-02-20", "1학기 2차 수강 신청 마감\n(학부, 타대생, 대학원 ~17:00)"},
                {"2025-02-21", "1학기 등록금 납부 마감"},
                {"2025-02-27", "1학기 1차 폐강 강좌 공고"},
                {"2025-02-28", "1학기 전기 학위 수여식"}
        };

        for (String[] data : eventData) {
            Event event = new Event();
            event.setDate(data[0]);
            event.setDescription(data[1]);
            eventDao.insert(event);
        }
    }

    private void loadEvents() {
        List<Event> eventList = eventDao.getAllEvents();
        Set<CalendarDay> eventDays = new HashSet<>();

        for (Event event : eventList) {
            String date = event.getDate();
            CalendarDay day = parseDateToCalendarDay(date);
            eventDays.add(day);
            events.put(date, event.getDescription());

            if (event.getDescription() != null && !event.getDescription().isEmpty() && isTodayDate(day)) {
                scheduleDailyNotification(day, event.getDescription());
            }
        }

        calendarView.addDecorator(new EventDecorator(Color.BLUE, eventDays));
        calendarView.addDecorator(new TodayDecorator());
    }

    private boolean isTodayDate(CalendarDay day) {
        Calendar today = Calendar.getInstance();
        Calendar eventDate = Calendar.getInstance();
        eventDate.set(day.getYear(), day.getMonth()-1, day.getDay());
        return !eventDate.before(today);
    }

    private void scheduleDailyNotification(CalendarDay day, String eventDescription) {
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(day.getYear(), day.getMonth() - 1, day.getDay(), 9, 0, 0);
//
//        Intent intent = new Intent(this, AlarmReceiver.class);
//        intent.putExtra("eventDescription", eventDescription);
//        intent.putExtra("eventDate", day.getYear() + "-" + String.format("%02d", day.getMonth()) + "-" + String.format("%02d", day.getDay()));
//
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, day.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE);
//
//        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//        if (alarmManager != null) {
//            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
//        }

        SharedPreferences prefs = getSharedPreferences("my_prefs", MODE_PRIVATE);
        boolean isNotificationScheduled = prefs.getBoolean("isNotificationScheduled", false);

        if (!isNotificationScheduled) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(day.getYear(), day.getMonth() - 1, day.getDay(), 9, 0, 0);

            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("eventDescription", eventDescription);
            intent.putExtra("eventDate", day.getYear() + "-" + String.format("%02d", day.getMonth()) + "-" + String.format("%02d", day.getDay()));

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, day.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }

            // 알림이 예약되었음을 기록
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isNotificationScheduled", true);
            editor.apply();
        }
    }

    private CalendarDay parseDateToCalendarDay(String date) {
        String[] dateParts = date.split("-");
        int year = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]);
        int day = Integer.parseInt(dateParts[2]);
        return CalendarDay.from(year, month, day);
    }

    private String formatDateForMap(CalendarDay date) {
        return date.getYear() + "-" + String.format("%02d", date.getMonth()) + "-" + String.format("%02d", date.getDay());
    }

    private void showEventDialog(String date, String eventDescription) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(date);
        builder.setMessage(eventDescription != null && !eventDescription.isEmpty() ? eventDescription : "일정이 존재하지 않습니다.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}