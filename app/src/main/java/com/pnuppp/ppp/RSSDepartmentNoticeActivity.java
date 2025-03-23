package com.pnuppp.ppp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import android.content.SharedPreferences;

public class RSSDepartmentNoticeActivity extends AppCompatActivity {

    private Spinner categorySpinner;
    private RecyclerView recyclerView;
    private NoticeAdapter noticeAdapter;
    private List<RSSItem> rssItems = new ArrayList<>();
    private Map<String, Map<String, String>> departmentRSSMap; // 학과별로 여러 카테고리 RSS 링크를 저장

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department_notice);

        categorySpinner = findViewById(R.id.categorySpinner);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        noticeAdapter = new NoticeAdapter(rssItems, this);
        recyclerView.setAdapter(noticeAdapter);

        departmentRSSMap = new HashMap<>();
        setupRSSMapping();

        // SharedPreferences에서 학과 정보 가져오기
        SharedPreferences sharedPref = getSharedPreferences("com.pnuppp.pplusplus.user_info", MODE_PRIVATE);
        String tempSelectedDepartment = getIntent().getStringExtra("major");
        if (tempSelectedDepartment == null) {
            tempSelectedDepartment = sharedPref.getString("major", null);
        }
        final String selectedDepartment = tempSelectedDepartment;
        Log.d("RSSDepartmentNotice", "Selected Department: " + selectedDepartment);

        // 학과가 존재하는지 확인
        if (departmentRSSMap.containsKey(selectedDepartment)) {
            Map<String, String> noticeCategories = departmentRSSMap.get(selectedDepartment);
            List<String> categories = new ArrayList<>(noticeCategories.keySet());

            // 카테고리를 알파벳 또는 한글 순서로 정렬
            Collections.sort(categories);

            // Spinner에 카테고리 세팅
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            categorySpinner.setAdapter(adapter);

            categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedCategory = categories.get(position);
                    fetchRSSFeed(noticeCategories.get(selectedCategory));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Do nothing
                }
            });

            // 기본적으로 첫 번째 공지를 로드
            if (!categories.isEmpty()) {
                fetchRSSFeed(noticeCategories.get(categories.get(0)));
            }
        } else {
            Log.e("RSSDepartmentNotice", "Invalid department selected or no RSS URL available.");
        }
    }

    // 학과별로 카테고리를 매핑하는 메소드
    private void setupRSSMapping() {
        departmentRSSMap = new HashMap<>();
        // 인문대학
        // 국어국문학과
        Map<String, String> koreanLangCategories = new HashMap<>();
        koreanLangCategories.put("학부 공지", "https://bkorea.pusan.ac.kr/bbs/bkorea/3375/rssList.do?row=50");
        koreanLangCategories.put("대학원 공지", "https://bkorea.pusan.ac.kr/bbs/bkorea/3376/rssList.do?row=50");
        koreanLangCategories.put("취업게시판", "https://bkorea.pusan.ac.kr/bbs/bkorea/3379/rssList.do?row=50");
        departmentRSSMap.put("국어국문학과", koreanLangCategories);

        // 일어일문학과
        Map<String, String> japaneseLangCategories = new HashMap<>();
        japaneseLangCategories.put("공지사항", "https://japan.pusan.ac.kr/bbs/japan/3422/rssList.do?row=50");
        japaneseLangCategories.put("취업정보", "https://japan.pusan.ac.kr/bbs/japan/3428/rssList.do?row=50");
        japaneseLangCategories.put("대학원", "https://japan.pusan.ac.kr/bbs/japan/3423/rssList.do?row=50");
        departmentRSSMap.put("일어일문학과", japaneseLangCategories);

        // 불어불문학과
        Map<String, String> frenchLangCategories = new HashMap<>();
        frenchLangCategories.put("학부 공지", "https://french.pusan.ac.kr/bbs/french/4295/rssList.do?row=50");
        departmentRSSMap.put("불어불문학과", frenchLangCategories);

        // 노어노문학과
        Map<String, String> russianLangCategories = new HashMap<>();
        russianLangCategories.put("학부 공지사항", "https://russia.pusan.ac.kr/bbs/russia/16061/rssList.do?row=50");
        russianLangCategories.put("대학원 공지사항", "https://russia.pusan.ac.kr/bbs/russia/16062/rssList.do?row=50");
        russianLangCategories.put("교직 공지사항", "https://russia.pusan.ac.kr/bbs/russia/17947/rssList.do?row=50");
        russianLangCategories.put("취업정보", "https://russia.pusan.ac.kr/bbs/russia/16063/rssList.do?row=50");
        departmentRSSMap.put("노어노문학과", russianLangCategories);

        // 중어중문학과
        Map<String, String> chineseLangCategories = new HashMap<>();
        chineseLangCategories.put("학부 공지", "https://china.pusan.ac.kr/bbs/china/5363/rssList.do?row=50");
        chineseLangCategories.put("대학원 공지", "https://china.pusan.ac.kr/bbs/china/5364/rssList.do?row=50");
        chineseLangCategories.put("장학", "https://china.pusan.ac.kr/bbs/china/5366/rssList.do?row=50");
        departmentRSSMap.put("중어중문학과", chineseLangCategories);

        // 영어영문학과
        Map<String, String> englishLangCategories = new HashMap<>();
        englishLangCategories.put("학부 공지", "https://pnuenglish.pusan.ac.kr/bbs/pnuenglish/3108/rssList.do?row=50");
        englishLangCategories.put("대학원 공지", "https://pnuenglish.pusan.ac.kr/bbs/pnuenglish/3107/rssList.do?row=50");
        englishLangCategories.put("취업정보, 대외활동", "https://pnuenglish.pusan.ac.kr/bbs/pnuenglish/3102/rssList.do?row=50");
        englishLangCategories.put("아르바이트", "https://pnuenglish.pusan.ac.kr/bbs/pnuenglish/3106/rssList.do?row=50");
        departmentRSSMap.put("영어영문학과", englishLangCategories);

        // 독어독문학과
        Map<String, String> germanLangCategories = new HashMap<>();
        germanLangCategories.put("학부 공지", "https://german.pusan.ac.kr/bbs/german/5032/rssList.do?row=50");
        germanLangCategories.put("대학원 공지", "https://german.pusan.ac.kr/bbs/german/5033/rssList.do?row=50");
        germanLangCategories.put("부,복수전공 공지", "https://german.pusan.ac.kr/bbs/german/11768/rssList.do?row=50");
        germanLangCategories.put("취업정보", "https://german.pusan.ac.kr/bbs/german/5049/rssList.do?row=50");
        germanLangCategories.put("대학생활", "https://german.pusan.ac.kr/bbs/german/6860/rssList.do?row=50");
        departmentRSSMap.put("독어독문학과", germanLangCategories);

        // 언어정보학과
        Map<String, String> linguisticsCategories = new HashMap<>();
        linguisticsCategories.put("공지사항", "https://linguistics.pusan.ac.kr/bbs/linguistics/4133/rssList.do?row=50");
        linguisticsCategories.put("취업정보", "https://linguistics.pusan.ac.kr/bbs/linguistics/4135/rssList.do?row=50");
        linguisticsCategories.put("학부 공지", "https://linguistics.pusan.ac.kr/bbs/linguistics/4133/rssList.do?row=50");
        departmentRSSMap.put("언어정보학과", linguisticsCategories);

        // 사학과
        Map<String, String> historyCategories = new HashMap<>();
        historyCategories.put("학부 공지", "https://history.pusan.ac.kr/bbs/history/3466/rssList.do?row=50");
        historyCategories.put("대학원 공지", "https://history.pusan.ac.kr/bbs/history/11701/rssList.do?row=50");
        departmentRSSMap.put("사학과", historyCategories);


        // 사회과학대학
        // 행정학과
        Map<String, String> adminDept = new HashMap<>();
        adminDept.put("학부공지사항", "https://pub-adm.pusan.ac.kr/bbs/pub-adm/2744/rssList.do?row=50");
        adminDept.put("대학원공지사항", "https://pub-adm.pusan.ac.kr/bbs/pub-adm/2743/rssList.do?row=50");
        departmentRSSMap.put("행정학과", adminDept);

        // 정치외교학과
        Map<String, String> polsciDept = new HashMap<>();
        polsciDept.put("공지사항", "https://polsci.pusan.ac.kr/bbs/polsci/2837/rssList.do?row=50");
        departmentRSSMap.put("정치외교학과", polsciDept); // 대학원 공지 없음

        // 사회학과
        Map<String, String> sociologyDept = new HashMap<>();
        sociologyDept.put("학사안내", "https://soc.pusan.ac.kr/bbs/soc/7527/rssList.do?row=50");
        sociologyDept.put("대학원게시판", "https://soc.pusan.ac.kr/bbs/soc/9673/rssList.do?row=50");
        sociologyDept.put("취업 및 장학", "https://soc.pusan.ac.kr/bbs/soc/7525/rssList.do?row=50");
        departmentRSSMap.put("사회학과", sociologyDept);

        // 심리학과
        Map<String, String> psychologyDept = new HashMap<>();
        psychologyDept.put("학부공지사항", "https://psy.pusan.ac.kr/bbs/psy/2798/rssList.do?row=50");
        psychologyDept.put("대학원공지사항", "https://psy.pusan.ac.kr/bbs/psy/2797/rssList.do?row=50");
        psychologyDept.put("취업및학술정보", "https://psy.pusan.ac.kr/bbs/psy/2794/rssList.do?row=50");

        departmentRSSMap.put("심리학과", psychologyDept);

        // 문헌정보학과
        Map<String, String> libraryInfoDept = new HashMap<>();
        libraryInfoDept.put("학부 공지사항", "https://lais.pusan.ac.kr/bbs/lais/15732/rssList.do?row=50");
        libraryInfoDept.put("대학원 공지사항", "https://lais.pusan.ac.kr/bbs/lais/15733/rssList.do?row=50");
        libraryInfoDept.put("학과 소식", "https://lais.pusan.ac.kr/bbs/lais/15734/rssList.do?row=50");
        libraryInfoDept.put("취업공고", "https://lais.pusan.ac.kr/bbs/lais/15740/rssList.do?row=50");
        departmentRSSMap.put("문헌정보학과", libraryInfoDept);

        // 미디어커뮤니케이션학과 (RSS 피드 없음)
        Map<String, String> mediaCommDept = new HashMap<>();
        mediaCommDept.put("공지 없음", null); // RSS 링크가 없어서 빈 값을 처리
        departmentRSSMap.put("미디어커뮤니케이션학과", mediaCommDept);


        // 자연과학대학
        // 수학과
        Map<String, String> mathDept = new HashMap<>();
        mathDept.put("학부공지", "https://math.pusan.ac.kr/bbs/math/2818/rssList.do?row=50");
        mathDept.put("대학원공지", "https://math.pusan.ac.kr/bbs/math/16237/rssList.do?row=50");
        departmentRSSMap.put("수학과", mathDept);

        // 통계학과
        Map<String, String> statDept = new HashMap<>();
        statDept.put("학과공지사항", "https://stat.pusan.ac.kr/bbs/stat/2705/rssList.do?row=50");
        statDept.put("취업공지사항", "https://stat.pusan.ac.kr/bbs/stat/2707/rssList.do?row=50");
        statDept.put("초청특강", "https://stat.pusan.ac.kr/bbs/stat/2708/rssList.do?row=50");
        departmentRSSMap.put("통계학과", statDept); // 대학원 공지 없음

        // 화학과
        Map<String, String> chemDept = new HashMap<>();
        chemDept.put("공지사항", "https://chem.pusan.ac.kr/bbs/chem/2734/rssList.do?row=50");
        chemDept.put("취업", "https://chem.pusan.ac.kr/bbs/chem/2739/rssList.do?row=50");
        departmentRSSMap.put("통계학과", chemDept);

        // 생명과학과
        Map<String, String> bioDept = new HashMap<>();
        bioDept.put("학부 공지", "https://biology.pusan.ac.kr/bbs/biology/3143/rssList.do?row=50");
        bioDept.put("대학원 공지", "https://biology.pusan.ac.kr/bbs/biology/3144/rssList.do?row=50");
        bioDept.put("자유게시판", "https://biology.pusan.ac.kr/bbs/biology/3148/rssList.do?row=50");
        bioDept.put("취업게시판", "https://biology.pusan.ac.kr/bbs/biology/3151/rssList.do?row=50");
        departmentRSSMap.put("생명과학과", bioDept);

        // 미생물학과
        Map<String, String> microbioDept = new HashMap<>();
        microbioDept.put("학부 공지", "https://microbio.pusan.ac.kr/bbs/microbio/3085/rssList.do?row=50");
        microbioDept.put("대학원 공지", "https://microbio.pusan.ac.kr/bbs/microbio/3086/rssList.do?row=50");
        microbioDept.put("취업정보", "https://microbio.pusan.ac.kr/bbs/microbio/3084/rssList.do?row=50");
        microbioDept.put("초청 세미나", "https://microbio.pusan.ac.kr/bbs/microbio/3093/rssList.do?row=50");
        departmentRSSMap.put("미생물학과", microbioDept);

        // 분자생물학과
        Map<String, String> molbioDept = new HashMap<>();
        molbioDept.put("공지사항", "https://molbiology.pusan.ac.kr/bbs/molbiology/3918/rssList.do?row=50");
        departmentRSSMap.put("분자생물학과", molbioDept); // 대학원 공지 없음

        // 지질환경과학과
        Map<String, String> geologyDept = new HashMap<>();
        geologyDept.put("공지사항", "https://geology.pusan.ac.kr/bbs/geology/2800/rssList.do?row=50");
        geologyDept.put("학과게시판", "https://geology.pusan.ac.kr/bbs/geology/2813/rssList.do?row=50");
        geologyDept.put("취업정보", "https://geology.pusan.ac.kr/bbs/geology/2799/rssList.do?row=50");
        departmentRSSMap.put("지질환경과학과", geologyDept); // 대학원 공지 없음

        // 대기환경과학과
        Map<String, String> atmosDept = new HashMap<>();
        atmosDept.put("학과공지사항", "https://atmos.pusan.ac.kr/bbs/atmos/3096/rssList.do?row=50");
        departmentRSSMap.put("대기환경과학과", atmosDept); // 대학원 공지 없음

        // 해양학과
        Map<String, String> oceanDept = new HashMap<>();
        oceanDept.put("학과공지사항", "https://ocean.pusan.ac.kr/bbs/ocean/2877/rssList.do?row=50");
        oceanDept.put("취업공지사항", "https://ocean.pusan.ac.kr/bbs/ocean/2876/rssList.do?row=50");
        oceanDept.put("소식게시판", "https://ocean.pusan.ac.kr/bbs/ocean/2879/rssList.do?row=50");
        oceanDept.put("장학정보", "https://ocean.pusan.ac.kr/bbs/ocean/2880/rssList.do?row=50");
        departmentRSSMap.put("해양학과", oceanDept); // 대학원 공지 없음


        // 공과대학
        // 고분자공학과
        Map<String, String> polymerDept = new HashMap<>();
        polymerDept.put("공지사항", "https://polymer.pusan.ac.kr/bbs/polymer/16257/rssList.do?row=50");
        polymerDept.put("장학", "https://polymer.pusan.ac.kr/bbs/polymer/3283/rssList.do?row=50");
        polymerDept.put("취업", "https://polymer.pusan.ac.kr/bbs/polymer/3284/rssList.do?row=50");
        departmentRSSMap.put("고분자공학과", polymerDept); // 대학원 공지 없음

        // 유기소재시스템공학과
        Map<String, String> omseDept = new HashMap<>();
        omseDept.put("공지사항", "https://omse.pusan.ac.kr/bbs/omse/3203/rssList.do?row=50");
        omseDept.put("취업게시판", "https://omse.pusan.ac.kr/bbs/omse/3205/rssList.do?row=50");
        omseDept.put("대학원 학사공지", "https://omse.pusan.ac.kr/bbs/omse/12392/rssList.do?row=50");
        departmentRSSMap.put("유기소재시스템공학과", omseDept);

        // 전기전자공학부 전자공학전공
        Map<String, String> eceDept = new HashMap<>();
        eceDept.put("공지사항(학부)", "https://ee.pusan.ac.kr/bbs/ee/2635/rssList.do?row=50");
        eceDept.put("공지사항(대학원)", "https://ee.pusan.ac.kr/bbs/ee/2642/rssList.do?row=50");
        departmentRSSMap.put("전기전자공학부 전자공학전공", eceDept);

        // 전기전자공학부 전기공학전공
        Map<String, String> eeDept = new HashMap<>();
        eeDept.put("공지사항(학과)", "https://eec.pusan.ac.kr/bbs/eehome/2650/rssList.do?row=50");
        eeDept.put("공지사항(대학원)", "https://eec.pusan.ac.kr/bbs/eehome/2651/rssList.do?row=50");
        departmentRSSMap.put("전기전자공학부 전기공학전공", eeDept);

        // 조선해양공학과
        Map<String, String> naoeDept = new HashMap<>();
        naoeDept.put("학부공지사항", "https://naoe.pusan.ac.kr/bbs/naoe/2754/rssList.do?row=50");
        naoeDept.put("취업게시판", "https://naoe.pusan.ac.kr/bbs/naoe/2759/rssList.do?row=50");
        naoeDept.put("대학원공지사항", "https://naoe.pusan.ac.kr/bbs/naoe/2756/rssList.do?row=50");
        departmentRSSMap.put("조선해양공학과", naoeDept);

        // 재료공학부
        Map<String, String> mseDept = new HashMap<>();
        mseDept.put("학부공지", "https://mse.pusan.ac.kr/bbs/mse/8972/rssList.do?row=50");
        mseDept.put("대학원(입학 및 학사)", "https://mse.pusan.ac.kr/bbs/mse/12265/rssList.do?row=50");
        mseDept.put("대학원(졸업)", "https://mse.pusan.ac.kr/bbs/mse/12266/rssList.do?row=50");
        mseDept.put("대학원(수업)", "https://mse.pusan.ac.kr/bbs/mse/12267/rssList.do?row=50");
        mseDept.put("대학원(연구)", "https://mse.pusan.ac.kr/bbs/mse/12268/rssList.do?row=50");
        mseDept.put("대학원(기타)", "https://mse.pusan.ac.kr/bbs/mse/12269/rssList.do?row=50");
        mseDept.put("취업공지", "https://mse.pusan.ac.kr/bbs/mse/8974/rssList.do?row=50");
        mseDept.put("행사 및 세미나 공지", "https://mse.pusan.ac.kr/bbs/mse/8975/rssList.do?row=50");
        departmentRSSMap.put("재료공학부", mseDept);

        // 항공우주공학과
        Map<String, String> aerospaceDept = new HashMap<>();
        aerospaceDept.put("학과공지", "https://aerospace.pusan.ac.kr/bbs/aerospace/3213/rssList.do?row=50");
        aerospaceDept.put("FLT&SIM공지", "https://aerospace.pusan.ac.kr/bbs/aerospace/3215/rssList.do?row=50");
        departmentRSSMap.put("항공우주공학과", aerospaceDept); // 대학원 공지 없음

        // 건축공학과
        Map<String, String> archEngDept = new HashMap<>();
        archEngDept.put("학부 공지사항", "https://archieng.pusan.ac.kr/bbs/_archieng/3964/rssList.do?row=50");
        archEngDept.put("대학원 공지사항", "https://archieng.pusan.ac.kr/bbs/_archieng/14096/rssList.do?row=50");
        archEngDept.put("취업 및 공모전", "https://archieng.pusan.ac.kr/bbs/_archieng/3965/rssList.do?row=50");
        departmentRSSMap.put("건축공학과", archEngDept);

        // 건축학과
        Map<String, String> archDept = new HashMap<>();
        archDept.put("학부 공지사항", "https://archi.pusan.ac.kr/bbs/archi/11920/rssList.do?row=50");
        archDept.put("대학원 공지사항", "https://archi.pusan.ac.kr/bbs/archi/11921/rssList.do?row=50");
        archDept.put("취업게시판", "https://archi.pusan.ac.kr/bbs/archi/11412/rssList.do?row=50");
        departmentRSSMap.put("건축학과", archDept);

        // 도시공학과
        Map<String, String> urbanDept = new HashMap<>();
        urbanDept.put("공지사항", "https://urban.pusan.ac.kr/bbs/urban/3413/rssList.do?row=50");
        urbanDept.put("취업게시판", "https://urban.pusan.ac.kr/bbs/urban/3415/rssList.do?row=50");
        departmentRSSMap.put("도시공학과", urbanDept); // 대학원 공지 없음

        // 사회기반시스템공학과
        Map<String, String> civilDept = new HashMap<>();
        civilDept.put("공지사항(학부)", "https://civil.pusan.ac.kr/bbs/civil/3207/rssList.do?row=50");
        civilDept.put("공지사항(일반대학원)", "https://civil.pusan.ac.kr/bbs/civil/3206/rssList.do?row=50");
        civilDept.put("공지사항(산업대학원)", "https://civil.pusan.ac.kr/bbs/civil/3208/rssList.do?row=50");
        civilDept.put("취업정보(학부)", "https://civil.pusan.ac.kr/bbs/civil/16152/rssList.do?row=50");
        civilDept.put("취업정보(석,박사)", "https://civil.pusan.ac.kr/bbs/civil/16153/rssList.do?row=50");
        departmentRSSMap.put("사회기반시스템공학과", civilDept);


        // 사범대학
        // 국어교육과
        Map<String, String> koreduDept = new HashMap<>();
        koreduDept.put("학부", "https://koredu.pusan.ac.kr/bbs/koredu/5262/rssList.do?row=50");
        koreduDept.put("일반대학원", "https://koredu.pusan.ac.kr/bbs/koredu/5264/rssList.do?row=50");
        koreduDept.put("교육대학원", "https://koredu.pusan.ac.kr/bbs/koredu/5265/rssList.do?row=50");
        departmentRSSMap.put("국어교육과", koreduDept);

        // 영어교육과
        Map<String, String> englisheduDept = new HashMap<>();
        englisheduDept.put("학부게시판", "https://englishedu.pusan.ac.kr/bbs/englishedupnu/8789/rssList.do?row=50");
        englisheduDept.put("대학원게시판", "https://englishedu.pusan.ac.kr/bbs/englishedupnu/8790/rssList.do?row=50");
        englisheduDept.put("구인구직", "https://englishedu.pusan.ac.kr/bbs/englishedupnu/8792/rssList.do?row=50");
        englisheduDept.put("교직과정안내", "https://englishedu.pusan.ac.kr/bbs/englishedupnu/9039/rssList.do?row=50");
        departmentRSSMap.put("영어교육과", englisheduDept);

        // 독어교육과
        Map<String, String> gereduDept = new HashMap<>();
        gereduDept.put("공지사항", "https://geredu.pusan.ac.kr/bbs/geredu/4381/rssList.do?row=50");
        departmentRSSMap.put("독어교육과", gereduDept);

        // 불어교육과
        Map<String, String> freduDept = new HashMap<>();
        freduDept.put("공지사항", "https://fredu.pusan.ac.kr/bbs/fredu/4398/rssList.do?row=50");
        departmentRSSMap.put("불어교육과", freduDept);

        // 교육학과
        Map<String, String> edDept = new HashMap<>();
        edDept.put("전체공지사항", "https://ed.pusan.ac.kr/bbs/ed/2768/rssList.do?row=50");
        edDept.put("취업정보", "https://ed.pusan.ac.kr/bbs/ed/2772/rssList.do?row=50");
        departmentRSSMap.put("교육학과", edDept);

        // 유아교육과
        Map<String, String> childDept = new HashMap<>();
        childDept.put("학부공지사항", "https://child.pusan.ac.kr/bbs/child/3129/rssList.do?row=50");
        childDept.put("대학원공지사항", "https://child.pusan.ac.kr/bbs/child/3131/rssList.do?row=50");
        childDept.put("취업게시판", "https://child.pusan.ac.kr/bbs/child/3132/rssList.do?row=50");
        departmentRSSMap.put("유아교육과", childDept);

        // 특수교육과
        Map<String, String> specialDept = new HashMap<>();
        specialDept.put("공지사항", "https://special.pusan.ac.kr/bbs/special/3470/rssList.do?row=50");
        specialDept.put("일대원", "https://special.pusan.ac.kr/bbs/special/3795/rssList.do?row=50");
        specialDept.put("교대원", "https://special.pusan.ac.kr/bbs/special/3796/rssList.do?row=50");
        specialDept.put("취업게시판", "https://special.pusan.ac.kr/bbs/special/3797/rssList.do?row=50");
        departmentRSSMap.put("특수교육과", specialDept);

        // 일반사회교육과
        Map<String, String> socialeduDept = new HashMap<>();
        socialeduDept.put("학과 공지사항", "https://socialedu.pusan.ac.kr/bbs/socialedu/4102/rssList.do?row=50");
        socialeduDept.put("학생회 공지사항", "https://socialedu.pusan.ac.kr/bbs/socialedu/4103/rssList.do?row=50");
        socialeduDept.put("채용공고", "https://socialedu.pusan.ac.kr/bbs/socialedu/17668/rssList.do?row=50");
        departmentRSSMap.put("일반사회교육과", socialeduDept);

        // 역사교육과
        Map<String, String> hiseduDept = new HashMap<>();
        hiseduDept.put("공지사항", "https://hisedu.pusan.ac.kr/bbs/hisedu/3361/rssList.do?row=50");
        hiseduDept.put("채용정보", "https://hisedu.pusan.ac.kr/bbs/hisedu/3366/rssList.do?row=50");
        departmentRSSMap.put("역사교육과", hiseduDept);

        // 지리교육과
        Map<String, String> geoeduDept = new HashMap<>();
        geoeduDept.put("공지사항", "https://geoedu.pusan.ac.kr/bbs/geoedu/4310/rssList.do?row=50");
        departmentRSSMap.put("지리교육과", geoeduDept);

        // 윤리교육과
        Map<String, String> ethicsDept = new HashMap<>();
        ethicsDept.put("얼바람말글장", "https://ethics.pusan.ac.kr/bbs/ethics/4351/rssList.do?row=50");
        ethicsDept.put("채용공고", "https://ethics.pusan.ac.kr/bbs/ethics/12455/rssList.do?row=50");
        departmentRSSMap.put("윤리교육과", ethicsDept);

        // 수학교육과
        Map<String, String> matheduDept = new HashMap<>();
        matheduDept.put("공지사항", "https://mathedu.pusan.ac.kr/bbs/mathedu/2696/rssList.do?row=50");
        matheduDept.put("대학원 공지사항", "https://mathedu.pusan.ac.kr/bbs/mathedu/2698/rssList.do?row=50");
        matheduDept.put("교육대학원 공지사항", "https://mathedu.pusan.ac.kr/bbs/mathedu/2697/rssList.do?row=50");
        matheduDept.put("취업,과외게시판", "https://mathedu.pusan.ac.kr/bbs/mathedu/2700/rssList.do?row=50");
        departmentRSSMap.put("수학교육과", matheduDept);

        // 물리교육과
        Map<String, String> physeduDept = new HashMap<>();
        physeduDept.put("공지사항", "https://physedu.pusan.ac.kr/bbs/physedu/3403/rssList.do?row=50");
        physeduDept.put("구인/구직", "https://physedu.pusan.ac.kr/bbs/physedu/4338/rssList.do?row=50");
        departmentRSSMap.put("물리교육과", physeduDept);

        // 화학교육과
        Map<String, String> chemeduDept = new HashMap<>();
        chemeduDept.put("공지사항", "https://chemedu.pusan.ac.kr/bbs/chemedu/4094/rssList.do?row=50");
        chemeduDept.put("일반대학원 공지", "https://chemedu.pusan.ac.kr/bbs/chemedu/4089/rssList.do?row=50");
        chemeduDept.put("교육대학원 공지", "https://chemedu.pusan.ac.kr/bbs/chemedu/4057/rssList.do?row=50");
        departmentRSSMap.put("화학교육과", chemeduDept);

        // 생물교육과
        Map<String, String> edubioDept = new HashMap<>();
        edubioDept.put("학부 공지사항", "https://edubio.pusan.ac.kr/bbs/edubio/4231/rssList.do?row=50");
        edubioDept.put("교육대학원 공지사항", "https://edubio.pusan.ac.kr/bbs/edubio/4243/rssList.do?row=50");
        edubioDept.put("대학원 공지사항", "https://edubio.pusan.ac.kr/bbs/edubio/4240/rssList.do?row=50");
        edubioDept.put("임용게시판", "https://edubio.pusan.ac.kr/bbs/edubio/4233/rssList.do?row=50");
        departmentRSSMap.put("생물교육과", edubioDept);

        // 지구과학교육과
        Map<String, String> earthDept = new HashMap<>();
        earthDept.put("학과 공지", "https://earth.pusan.ac.kr/bbs/earth/4654/rssList.do?row=50");
        earthDept.put("대학원 공지", "https://earth.pusan.ac.kr/bbs/earth/4707/rssList.do?row=50");
        earthDept.put("교육대학원 지구과학교육전공 공지사항", "https://earth.pusan.ac.kr/bbs/earth/4708/rssList.do?row=50");
        earthDept.put("교육대학원 환경교육전공 공지사항", "https://earth.pusan.ac.kr/bbs/earth/4709/rssList.do?row=50");
        earthDept.put("취업정보게시판", "https://earth.pusan.ac.kr/bbs/earth/4690/rssList.do?row=50");
        departmentRSSMap.put("지구과학교육과", earthDept);

        // 체육교육과
        Map<String, String> physicaleduDept = new HashMap<>();
        physicaleduDept.put("학과 공지사항", "https://physicaledu.pusan.ac.kr/bbs/physicaledu/4358/rssList.do?row=50");
        physicaleduDept.put("일반대학원 공지사항", "https://physicaledu.pusan.ac.kr/bbs/physicaledu/4357/rssList.do?row=50");
        physicaleduDept.put("교육대학원 공지사항", "https://physicaledu.pusan.ac.kr/bbs/physicaledu/4369/rssList.do?row=50");
        departmentRSSMap.put("체육교육과", physicaleduDept);


        // 경제통상대학
        // 무역학부
        Map<String, String> tradeDept = new HashMap<>();
        tradeDept.put("학부 공지", "https://pnutrade.pusan.ac.kr/bbs/pnutrade/3390/rssList.do?row=50");
        tradeDept.put("대학원 공지", "https://pnutrade.pusan.ac.kr/bbs/pnutrade/3391/rssList.do?row=50");
        tradeDept.put("무역학부 소식", "https://pnutrade.pusan.ac.kr/bbs/pnutrade/3392/rssList.do?row=50");
        tradeDept.put("채용 및 공모전", "https://pnutrade.pusan.ac.kr/bbs/pnutrade/3393/rssList.do?row=50");
        tradeDept.put("장학", "https://pnutrade.pusan.ac.kr/bbs/pnutrade/3394/rssList.do?row=50");
        tradeDept.put("기타", "https://pnutrade.pusan.ac.kr/bbs/pnutrade/3395/rssList.do?row=50");
        departmentRSSMap.put("무역학부", tradeDept);

        // 경제학부
        Map<String, String> econDept = new HashMap<>();
        econDept.put("학부 공지", "https://pnuecon.pusan.ac.kr/bbs/pnuecon/3210/rssList.do?row=50");
        econDept.put("대학원 공지", "https://pnuecon.pusan.ac.kr/bbs/pnuecon/17205/rssList.do?row=50");
        econDept.put("학부 취업", "https://pnuecon.pusan.ac.kr/bbs/pnuecon/7319/rssList.do?row=50");
        departmentRSSMap.put("경제학부", econDept);

        // 관광컨벤션학과
        Map<String, String> conventionDept = new HashMap<>();
        conventionDept.put("학부 공지", "https://convention.pusan.ac.kr/bbs/convention/3346/rssList.do?row=50");
        conventionDept.put("대학원 공지", "https://convention.pusan.ac.kr/bbs/convention/9230/rssList.do?row=50");
        conventionDept.put("채용공고", "https://convention.pusan.ac.kr/bbs/convention/3351/rssList.do?row=50");
        conventionDept.put("장학안내", "https://convention.pusan.ac.kr/bbs/convention/3348/rssList.do?row=50");
        departmentRSSMap.put("관광컨벤션학과", conventionDept);

        // 공공정책학부
        Map<String, String> ppmDept = new HashMap<>();
        ppmDept.put("학부 공지", "https://ppm.pusan.ac.kr/bbs/ppm/3498/rssList.do?row=50");
        departmentRSSMap.put("공공정책학부", ppmDept);


        // 경영대학
        // 경영학과
        Map<String, String> bizDept = new HashMap<>();
        bizDept.put("학부 공지", "https://biz.pusan.ac.kr/bbs/biz/2557/rssList.do?row=50");
        bizDept.put("대학원 공지", "https://biz.pusan.ac.kr/bbs/biz/2556/rssList.do?row=50");
        bizDept.put("취업", "https://biz.pusan.ac.kr/bbs/biz/11448/rssList.do?row=50");
        bizDept.put("장학", "https://biz.pusan.ac.kr/bbs/biz/2558/rssList.do?row=50");
        departmentRSSMap.put("경영학과", bizDept);


        // 약학대학
        // 약학대학 약학전공
        Map<String, String> pharmacyMajor = new HashMap<>();
        pharmacyMajor.put("학부 공지", "https://pharmacy.pusan.ac.kr/bbs/pharmacy/2420/rssList.do?row=50");
        pharmacyMajor.put("대학원 공지", "https://pharmacy.pusan.ac.kr/bbs/pharmacy/11649/rssList.do?row=50");
        departmentRSSMap.put("약학대학 약학전공", pharmacyMajor);

        // 약학대학 제약학전공
        Map<String, String> pharmScienceMajor = new HashMap<>();
        pharmScienceMajor.put("학부 공지", "https://pharmacy.pusan.ac.kr/bbs/pharmacy/2420/rssList.do?row=50");
        pharmScienceMajor.put("대학원 공지", "https://pharmacy.pusan.ac.kr/bbs/pharmacy/11649/rssList.do?row=50");
        departmentRSSMap.put("약학대학 제약학전공", pharmScienceMajor);


        // 생활과학대학
        // 아동가족학과
        Map<String, String> childFamilyStudies = new HashMap<>();
        childFamilyStudies.put("학부 공지", "https://cdfs.pusan.ac.kr/bbs/cdfs/3449/rssList.do?row=50");
        departmentRSSMap.put("아동가족학과", childFamilyStudies);

        // 의류학과
        Map<String, String> fashionStudies = new HashMap<>();
        fashionStudies.put("학부 공지", "https://fashion.pusan.ac.kr/bbs/fashion/3442/rssList.do?row=50");
        fashionStudies.put("대학원 공지", "https://fashion.pusan.ac.kr/bbs/fashion/11724/rssList.do?row=50");
        departmentRSSMap.put("의류학과", fashionStudies);

        // 식품영양학과
        Map<String, String> foodNutrition = new HashMap<>();
        foodNutrition.put("학부 공지", "https://fsn.pusan.ac.kr/bbs/fsn/2783/rssList.do?row=50");
        departmentRSSMap.put("식품영양학과", foodNutrition);


        // 예술대학
        // 음악학과
        Map<String, String> musicDept = new HashMap<>();
        musicDept.put("공지사항", "https://music.pusan.ac.kr/bbs/music/3192/rssList.do?row=50");
        musicDept.put("공연안내", "https://music.pusan.ac.kr/bbs/music/3195/rssList.do?row=50");
        departmentRSSMap.put("음악학과", musicDept);

        // 한국음악학과
        Map<String, String> koreanMusicDept = new HashMap<>();
        koreanMusicDept.put("공지사항", "https://gukak.pusan.ac.kr/bbs/gukak/3978/rssList.do?row=50");
        koreanMusicDept.put("공연안내", "https://gukak.pusan.ac.kr/bbs/gukak/3983/rssList.do?row=50");
        departmentRSSMap.put("한국음악학과", koreanMusicDept);

        // 미술학과
        Map<String, String> artDept = new HashMap<>();
        artDept.put("학부 공지", "https://pnuart.pusan.ac.kr/bbs/pnuart/3941/rssList.do?row=50");
        artDept.put("대학원 공지", "https://pnuart.pusan.ac.kr/bbs/pnuart/3946/rssList.do?row=50");
        departmentRSSMap.put("미술학과", artDept);

        // 조형학과
        Map<String, String> sculptureDept = new HashMap<>();
        sculptureDept.put("공지사항", "https://plarts.pusan.ac.kr/bbs/plarts/4434/rssList.do?row=50");
        departmentRSSMap.put("조형학과", sculptureDept);

        // 디자인학과
        Map<String, String> designDept = new HashMap<>();
        designDept.put("공지사항", "https://design.pusan.ac.kr/bbs/design/3353/rssList.do?row=50");
        designDept.put("학과소식", "https://design.pusan.ac.kr/bbs/design/18284/rssList.do?row=50");
        departmentRSSMap.put("디자인학과", designDept);

        // 무용학과
        Map<String, String> danceDept = new HashMap<>();
        danceDept.put("학부공지", "https://dance.pusan.ac.kr/bbs/dance/4164/rssList.do?row=50");
        danceDept.put("콩쿠르요강", "https://dance.pusan.ac.kr/bbs/dance/4154/rssList.do?row=50");
        departmentRSSMap.put("무용학과", danceDept);

        // 예술문화영상학과
        Map<String, String> artCultureDept = new HashMap<>();
        artCultureDept.put("공지사항", "https://artimage.pusan.ac.kr/bbs/artimage/3257/rssList.do?row=50");
        artCultureDept.put("자유게시판", "https://artimage.pusan.ac.kr/bbs/artimage/3258/rssList.do?row=50");
        departmentRSSMap.put("예술문화영상학과", artCultureDept);


        // 나노과학기술대학
        // 나노메카트로닉스공학과
        Map<String, String> nanomechaDept = new HashMap<>();
        nanomechaDept.put("학부 공지", "https://nanomecha.pusan.ac.kr/bbs/nanomecha/3264/rssList.do?row=50");
        nanomechaDept.put("대학원 공지", "https://nanomecha.pusan.ac.kr/bbs/nanomecha/14888/rssList.do?row=50");
        nanomechaDept.put("장학정보", "https://nanomecha.pusan.ac.kr/bbs/nanomecha/3274/rssList.do?row=50");
        nanomechaDept.put("취업정보", "https://nanomecha.pusan.ac.kr/bbs/nanomecha/3275/rssList.do?row=50");
        departmentRSSMap.put("나노메카트로닉스공학과", nanomechaDept);

        // 나노에너지공학과
        Map<String, String> nanoEnergyDept = new HashMap<>();
        nanoEnergyDept.put("학부 공지", "https://energy.pusan.ac.kr/bbs/energy/2829/rssList.do?row=50");
        nanoEnergyDept.put("장학정보", "https://energy.pusan.ac.kr/bbs/energy/2831/rssList.do?row=50");
        nanoEnergyDept.put("취업정보", "https://energy.pusan.ac.kr/bbs/energy/2832/rssList.do?row=50");
        nanoEnergyDept.put("대학원 공지", "https://energy.pusan.ac.kr/bbs/energy/7496/rssList.do?row=50");
        departmentRSSMap.put("나노에너지공학과", nanoEnergyDept);

        // 광메카트로닉스공학과
        Map<String, String> optomechaDept = new HashMap<>();
        optomechaDept.put("학부 공지", "https://ome.pusan.ac.kr/bbs/ome/3307/rssList.do?row=50");
        optomechaDept.put("장학정보", "https://ome.pusan.ac.kr/bbs/ome/3308/rssList.do?row=50");
        optomechaDept.put("취업정보", "https://ome.pusan.ac.kr/bbs/ome/3309/rssList.do?row=50");
        departmentRSSMap.put("광메카트로닉스공학과", optomechaDept);


        // 생명자원과학대학
        // 식물생명과학과
        Map<String, String> plantDept = new HashMap<>();
        plantDept.put("공지사항", "https://plant.pusan.ac.kr/bbs/plant/4252/rssList.do?row=50");
        plantDept.put("취업정보", "https://plant.pusan.ac.kr/bbs/plant/4253/rssList.do?row=50");
        departmentRSSMap.put("식물생명과학과", plantDept);

        // 원예생명과학과
        Map<String, String> horticultureDept = new HashMap<>();
        horticultureDept.put("공지사항", "https://his.pusan.ac.kr/horticulture/21127/subview.do");
        horticultureDept.put("장학공지", "https://his.pusan.ac.kr/bbs/horticulture/4182/rssList.do?row=50");
        horticultureDept.put("자유게시판", "https://his.pusan.ac.kr/horticulture/21131/subview.do");
        horticultureDept.put("취업공지", "https://his.pusan.ac.kr/bbs/horticulture/4192/rssList.do?row=50");
        horticultureDept.put("학생지원", "https://his.pusan.ac.kr/bbs/horticulture/4196/rssList.do?row=50");
        departmentRSSMap.put("원예생명과학과", horticultureDept);

        // 동물생명자원과학과
        Map<String, String> animalDept = new HashMap<>();
        animalDept.put("공지사항", "https://animal.pusan.ac.kr/bbs/animal/3293/rssList.do?row=50");
        animalDept.put("취업게시판", "https://animal.pusan.ac.kr/bbs/animal/3297/rssList.do?row=50");
        departmentRSSMap.put("동물생명자원과학과", animalDept);

        // 식품공학과
        Map<String, String> foodScienceDept = new HashMap<>();
        foodScienceDept.put("공지사항", "https://fst.pusan.ac.kr/bbs/fst/3950/rssList.do?row=50");
        foodScienceDept.put("취업게시판", "https://fst.pusan.ac.kr/bbs/fst/3960/rssList.do?row=50");
        foodScienceDept.put("자유게시판", "https://fst.pusan.ac.kr/bbs/fst/3962/rssList.do?row=50");
        departmentRSSMap.put("식품공학과", foodScienceDept);

        // 생명환경화학과
        Map<String, String> lifeEnvChemDept = new HashMap<>();
        lifeEnvChemDept.put("학부 공지", "https://pnu-lseb.pusan.ac.kr/bbs/pnu-lseb/4324/rssList.do?row=50");
        lifeEnvChemDept.put("대학원 공지", "https://pnu-lseb.pusan.ac.kr/bbs/pnu-lseb/12479/rssList.do?row=50");
        lifeEnvChemDept.put("취업게시판", "https://pnu-lseb.pusan.ac.kr/bbs/pnu-lseb/4326/rssList.do?row=50");
        lifeEnvChemDept.put("학과소식지", "https://pnu-lseb.pusan.ac.kr/bbs/pnu-lseb/17901/rssList.do?row=50");
        lifeEnvChemDept.put("자유게시판", "https://pnu-lseb.pusan.ac.kr/bbs/pnu-lseb/4327/rssList.do?row=50");
        departmentRSSMap.put("생명환경화학과", lifeEnvChemDept);

        // 바이오소재과학과
        Map<String, String> bioMaterialDept = new HashMap<>();
        bioMaterialDept.put("공지사항", "https://bm.pusan.ac.kr/bbs/bm/3234/rssList.do?row=50");
        bioMaterialDept.put("취업게시판", "https://bm.pusan.ac.kr/bbs/bm/3235/rssList.do?row=50");
        departmentRSSMap.put("바이오소재과학과", bioMaterialDept);

        // 바이오산업기계공학과
        Map<String, String> bioIndustryDept = new HashMap<>();
        bioIndustryDept.put("학부 공지", "https://bime.pusan.ac.kr/bbs/bime/3835/rssList.do?row=50");
        bioIndustryDept.put("취업알림", "https://bime.pusan.ac.kr/bbs/bime/3836/rssList.do?row=50");
        bioIndustryDept.put("수상소식", "https://bime.pusan.ac.kr/bbs/bime/18283/rssList.do?row=50");
        departmentRSSMap.put("바이오산업기계공학과", bioIndustryDept);

        // 조경학과
        Map<String, String> landscapeDept = new HashMap<>();
        landscapeDept.put("공지사항", "https://la.pusan.ac.kr/bbs/la/4041/rssList.do?row=50");
        departmentRSSMap.put("조경학과", landscapeDept);

        // 식품자원경제학과
        Map<String, String> foodResourceEconDept = new HashMap<>();
        foodResourceEconDept.put("공지사항", "https://agecon.pusan.ac.kr/bbs/agecon/4116/rssList.do?row=50");
        foodResourceEconDept.put("학과알림", "https://agecon.pusan.ac.kr/bbs/agecon/4116/rssList.do?row=50");
        foodResourceEconDept.put("취업자료실", "https://agecon.pusan.ac.kr/bbs/agecon/4129/rssList.do?row=50");
        departmentRSSMap.put("식품자원경제학과", foodResourceEconDept);

        // IT응용공학과
        Map<String, String> itAppliedEngDept = new HashMap<>();
        itAppliedEngDept.put("공지사항", "https://ite.pusan.ac.kr/bbs/ite/3139/rssList.do?row=50");
        itAppliedEngDept.put("취업정보", "https://ite.pusan.ac.kr/bbs/ite/3140/rssList.do?row=50");
        departmentRSSMap.put("IT응용공학과", itAppliedEngDept);

        // 바이오환경에너지학과
        Map<String, String> bioEnvEnergyDept = new HashMap<>();
        bioEnvEnergyDept.put("공지사항", "https://bee.pusan.ac.kr/bbs/bee/3905/rssList.do?row=50");
        bioEnvEnergyDept.put("채용공고", "https://bee.pusan.ac.kr/bbs/bee/7159/rssList.do?row=50");
        departmentRSSMap.put("바이오환경에너지학과", bioEnvEnergyDept);


        // 간호대학
        // 간호학과
        Map<String, String> nursingDept = new HashMap<>();
        nursingDept.put("학부 공지", "https://nursing.pusan.ac.kr/bbs/nursing/2584/rssList.do?row=50");
        nursingDept.put("대학원 공지", "https://nursing.pusan.ac.kr/bbs/nursing/2585/rssList.do?row=50");
        nursingDept.put("채용공고", "https://nursing.pusan.ac.kr/bbs/nursing/11712/rssList.do?row=50");
        departmentRSSMap.put("간호학과", nursingDept);


        // 의과대학
        // 의예과
        Map<String, String> medicalPreDept = new HashMap<>();
        medicalPreDept.put("학부 공지", "https://medicine.pusan.ac.kr/bbs/medicine/2270/rssList.do?row=50");
        medicalPreDept.put("특정 공지", "https://medicine.pusan.ac.kr/bbs/medicine/2257/rssList.do?row=50");
        medicalPreDept.put("대학원 공지", "https://medicine.pusan.ac.kr/bbs/medicine/2267/rssList.do?row=50");
        departmentRSSMap.put("의예과", medicalPreDept);

        // 의학과
        Map<String, String> medicalDept = new HashMap<>();
        medicalDept.put("학부 공지", "https://medicine.pusan.ac.kr/bbs/medicine/2270/rssList.do?row=50");
        medicalDept.put("특정 공지", "https://medicine.pusan.ac.kr/bbs/medicine/2260/rssList.do?row=50");
        medicalDept.put("대학원 공지", "https://medicine.pusan.ac.kr/bbs/medicine/2267/rssList.do?row=50");
        departmentRSSMap.put("의학과", medicalDept);

        // 의과학과
        Map<String, String> biomedicalDept = new HashMap<>();
        biomedicalDept.put("학부 공지", "https://medicine.pusan.ac.kr/bbs/medicine/2270/rssList.do?row=50");
        biomedicalDept.put("특정 공지", "https://medicine.pusan.ac.kr/bbs/medicine/2265/rssList.do?row=50");
        biomedicalDept.put("대학원 공지", "https://medicine.pusan.ac.kr/bbs/medicine/2267/rssList.do?row=50");
        departmentRSSMap.put("의과학과", biomedicalDept);


        // 정보의생명공학대학
        // 정보컴퓨터공학부 컴퓨터공학전공
        Map<String, String> cseDept = new HashMap<>();
        cseDept.put("학부 공지", "https://cse.pusan.ac.kr/bbs/cse/2605/rssList.do?row=50");
        cseDept.put("대학원 공지", "https://cse.pusan.ac.kr/bbs/cse/2611/rssList.do?row=50");
        cseDept.put("졸업과제 공지사항", "https://cse.pusan.ac.kr/bbs/cse/2606/rssList.do?row=50");
        cseDept.put("수업게시판", "https://cse.pusan.ac.kr/bbs/cse/2615/rssList.do?row=50");
        cseDept.put("채용게시판", "https://cse.pusan.ac.kr/bbs/cse/2616/rssList.do?row=50");
        cseDept.put("경진대회안내", "https://cse.pusan.ac.kr/bbs/cse/12278/rssList.do?row=50");
        cseDept.put("기타행사안내", "https://cse.pusan.ac.kr/bbs/cse/2617/rssList.do?row=50");
        departmentRSSMap.put("정보컴퓨터공학부 컴퓨터공학전공", cseDept);

        // 정보컴퓨터공학부 인공지능전공
        Map<String, String> aiDept = new HashMap<>();
        aiDept.put("학부 공지", "https://cse.pusan.ac.kr/bbs/cse/2605/rssList.do?row=50");
        aiDept.put("대학원 공지", "https://cse.pusan.ac.kr/bbs/cse/2611/rssList.do?row=50");
        aiDept.put("졸업과제 공지사항", "https://cse.pusan.ac.kr/bbs/cse/2606/rssList.do?row=50");
        aiDept.put("수업게시판", "https://cse.pusan.ac.kr/bbs/cse/2615/rssList.do?row=50");
        aiDept.put("채용게시판", "https://cse.pusan.ac.kr/bbs/cse/2616/rssList.do?row=50");
        aiDept.put("경진대회안내", "https://cse.pusan.ac.kr/bbs/cse/12278/rssList.do?row=50");
        aiDept.put("기타행사안내", "https://cse.pusan.ac.kr/bbs/cse/2617/rssList.do?row=50");
        departmentRSSMap.put("정보컴퓨터공학부 인공지능전공", aiDept);

        // 의생명융합공학부
        Map<String, String> bioconvergenceDept = new HashMap<>();
        bioconvergenceDept.put("학부 공지", "https://bce.pusan.ac.kr/bbs/bce/12005/rssList.do?row=50");
        bioconvergenceDept.put("채용게시판", "https://bce.pusan.ac.kr/bbs/bce/18019/rssList.do?row=50");
        departmentRSSMap.put("의생명융합공학부", bioconvergenceDept); // 대학원 공지 RSS가 없기 때문에 학부 공지만 추가


        // 첨단융합학부(2025 신설)
        // 첨단융합학부 나노자율전공
        Map<String, String> nanoMajor = new HashMap<>();
        nanoMajor.put("공지사항", "https://u-nano.pusan.ac.kr/bbs/u-nano/18379/rssList.do?row=50");
        departmentRSSMap.put("첨단융합학부 나노자율전공", nanoMajor);

        Log.d("RSSDepartmentNotice", "RSS Mapping Setup Complete");
    }

    // 단일 URL을 처리하는 메소드
    private void fetchRSSFeed(String rssUrl) {
        if (rssUrl == null || rssUrl.isEmpty()) return;
        rssItems.clear();
        new Thread(() -> {
            try {
                URL url = new URL(rssUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                InputStream inputStream = connection.getInputStream();
                String content = readStream(inputStream);
                Log.d("RSSFeed", content);

                parseRSS(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                Log.e("RSSFetch", "Error fetching RSS feed", e);
            }

            new Handler(Looper.getMainLooper()).post(() -> noticeAdapter.notifyDataSetChanged());
        }).start();
    }

    private String readStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        return out.toString();
    }

    private void parseRSS(InputStream inputStream) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, null);

            int eventType = parser.getEventType();
            RSSItem currentItem = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName;
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        tagName = parser.getName();
                        if (tagName.equals("item")) {
                            currentItem = new RSSItem();
                        } else if (currentItem != null) {
                            switch (tagName) {
                                case "title":
                                    currentItem.setTitle(parser.nextText());
                                    break;
                                case "link":
                                    currentItem.setLink(parser.nextText());
                                    break;
                                case "pubDate":
                                    currentItem.setPubDate(parser.nextText());
                                    break;
                                case "author":
                                    currentItem.setAuthor(parser.nextText());
                                    break;
                                case "category":
                                    currentItem.setCategory(parser.nextText());
                                    break;
                                case "description":
                                    currentItem.setDescription(parser.nextText());
                                    break;
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        tagName = parser.getName();
                        if (tagName.equalsIgnoreCase("item") && currentItem != null) {
                            rssItems.add(currentItem);
                            currentItem = null;
                        }
                        break;
                }
                eventType = parser.next();
            }

            new Handler(Looper.getMainLooper()).post(() -> noticeAdapter.notifyDataSetChanged());

        } catch (Exception e) {
            Log.e("RSSParsing", "Error parsing RSS feed", e);
        }
    }
}
