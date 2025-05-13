package com.example.a2025termproject;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import okhttp3.*;


import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    String apiKey = "Bearer " + BuildConfig.OPENAI_API_KEY;
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    Button Btn_send;
    TextInputEditText userInputField;

    /// 프롬프트 + 모든 메시지(사용자와 챗봇의 대화)
    List<Map<String, Object>> messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Log.d("API_KEY_CHECK", apiKey);

        //Initialize UI
        Btn_send = findViewById(R.id.sendButton);
        userInputField = findViewById(R.id.userMessageInputField);


        Btn_send.setOnClickListener(v -> {
            Toast.makeText(this, "답장을 기다려줘요.", Toast.LENGTH_SHORT).show();
            createUserTextView();
        });

        // 시스템 메시지 추가
        {
            String history = loadChatHistory(); // 채팅 요약본 모두 불러오기
            String prompt = buildPrompt();  // 프롬프트 작성
            ChatMessage system = new ChatMessage("system", history + prompt);
            messages.add(system.message);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private String extractContentFromResponse(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray choices = obj.getJSONArray("choices");
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            return message.getString("content");
        } catch (Exception e) {
            return "[content 파싱 오류]";
        }
    }

    //사용자 입력 값으로 textView생성.
    //userInputField text 들고 와서 위젯 추가하는 방식.
    protected  void createUserTextView()
    {
        TextView textView = new TextView(this);
        textView.setText(userInputField.getText().toString());
        textView.setTextSize(14);
        textView.setBackgroundColor(Color.parseColor("#b6bffe"));
        textView.setGravity(Gravity.RIGHT);

        textView.setAutoLinkMask(0);
        textView.setPaintFlags(textView.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        Btn_send.postDelayed(() -> Btn_send.setEnabled(true), 3000);
        sendChatMessage(userInputField.getText().toString());
        userInputField.setText("");
        LinearLayout layout = findViewById(R.id.chatContainer);
        layout.addView(textView);
    }

    //AI 응답 받고 Textview 위젯 생성
    protected  void createResponseTextView(String Message)
    {
        TextView textView = new TextView(this);
        textView.setText(Message.toString());
        textView.setTextSize(14);
        textView.setBackgroundColor(Color.parseColor("#D0CDCD"));
        textView.setGravity(Gravity.LEFT);

        textView.setAutoLinkMask(0);
        textView.setPaintFlags(textView.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        Btn_send.postDelayed(() -> Btn_send.setEnabled(true), 3000);

        userInputField.setText("");
        LinearLayout layout = findViewById(R.id.chatContainer);
        layout.addView(textView);
    }

    private void sendChatMessage(String userInput) {
        
        OkHttpClient client = new OkHttpClient();

        // 사용자 응답 추가
        ChatMessage user = new ChatMessage("user", userInput);
        messages.add(user.message);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-3.5-turbo");
        body.put("messages", messages);
        body.put("max_tokens", 100);

        // JSON 만들기
        Gson gson = new Gson();
        String jsonBody = gson.toJson(body);

        // Request 만들기
        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .addHeader("Authorization", apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        // 비동기 요청
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("ChatGPT", "요청 실패: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    Log.e("ChatGPT", "응답 실패 - 상태 코드: " + response.code());
                    Log.e("ChatGPT", "에러 메시지 본문: " + responseBody); // 📌 여기서( 에러 메시지 출력
                    return;
                }

                Log.d("ChatGPT", "응답 성공: " + responseBody);
                runOnUiThread(() -> {
                    String content = extractContentFromResponse(responseBody);

                    // 챗봇 응답 추가
                    ChatMessage assistant = new ChatMessage("assistant", content);
                    messages.add(assistant.message);

                    createResponseTextView(content);
                });
            }
        });
    }

    /// 챗봇 프롬프트를 작성한다.
    private String buildPrompt()
    {
        return "당신은 예의 바르고 침착한 상담가입니다. 사용자의 감정을 존중하며 공감하는 말투를 사용하세요.\n" +
                "※ 응답은 50자 이내로 간결하게 말해 주세요.\n" +
                "※ 이전 대화 내용을 기억하고, 그 맥락을 반영하여 자연스럽게 대화를 이어가 주세요.";

        // TODO: 프롬프트 추가 요청 => 채팅 요약
        // 아래는 프롬프트 예시이며 참고만 해주세요.

        /*
        당신은 예의 바르고 침착한 상담가입니다. 사용자의 감정을 존중하며 공감하는 말투를 사용하세요.
        ※ 응답은 50자 이내로 간결하게 말해 주세요.
        ※ 이전 대화 내용을 기억하고, 그 맥락을 반영하여 자연스럽게 대화를 이어가 주세요.

        또한, 당신은 상담을 마친 뒤 상담 내용을 분석하여 감정 흐름과 사용자의 심리 상태를 요약하는 역할도 맡고 있습니다.

        [1] 사용자에게 응답할 메시지를 먼저 생성하고,
        [2] 이어서 개발자에게 전달할 요약 정보를 아래 양식으로 생성하세요.

        출력 형식:
        ---
        응답: [여기에 사용자에게 보여줄 공감 기반 메시지를 작성]

        요약:
        - 주요 감정 상태: [예: 불안, 스트레스, 혼란 등]
        - 감정의 흐름: [예: 초반엔 불안했으나 점차 안정됨]
        - 주요 이슈/주제: [예: 직장 스트레스, 대인관계 문제 등]
        - 관찰된 행동/사고 패턴: [예: 자기비난 경향, 해결 의지 있음 등]
        - 상담사 메모: [개발자가 DB에 저장할 수 있도록 핵심 정리]
        ---
        */
    }

    /// DB에 저장된 채팅 요약본을 모두 불러온다.
    private String loadChatHistory()
    {
        ChatHistoryDatabaseHelper db = new ChatHistoryDatabaseHelper(this);
        List<String> history = db.getAllSummaries();

        if (history.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        sb.append("이전 상담 요약:\n");

        for (String summary : history)
            sb.append("- ").append(summary).append("\n");
        sb.append("\n");

        return sb.toString();
    }

    /// DB에 채팅 요약본을 저장한다.
    /// TODO: 앱을 끄면 채팅 요약본을 DB에 저장할 수 있게 호출한다. 단, 요약 프롬프트가 정상적으로 작동한다는 가정 하에 해야한다.
    private void saveChatHistory(String summary)
    {
        // 비어 있으면 저장 안 한다
        if (summary.isEmpty())
            return;

        ChatHistoryDatabaseHelper db = new ChatHistoryDatabaseHelper(this);
        db.insert(summary);
    }
}