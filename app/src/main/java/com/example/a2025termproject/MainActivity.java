package com.example.a2025termproject;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RemoteViews;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import okhttp3.*;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    String apiKey = "Bearer " + BuildConfig.OPENAI_API_KEY;
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    Button Btn_send;
    TextInputEditText userInputField;

    /// 프롬프트 + 모든 메시지(사용자와 챗봇의 대화)
    List<Map<String, Object>> messages = new ArrayList<>();

    /// 초기화 작업
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

        // 시스템 메시지 추가 (상담사 프롬프트 작성 + 요약본 모두 불러오기)
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

    /// 다른 화면으로 전환될 때 호출되는 함수이다.
    @Override
    protected void onStop() {
        super.onStop();
        Log.d("callBack", "onStop");

        OkHttpClient client = new OkHttpClient();

        // 시스템 메시지 추가 (요약 프롬프트)
        String prompt = buildSummaryPrompt();
        Map<String, Object> body = buildBody(prompt, true);

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
                runOnUiThread(() -> Btn_send.setEnabled(false));
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    Log.e("ChatGPT", "응답 실패 - 상태 코드: " + response.code());
                    Log.e("ChatGPT", "에러 메시지 본문: " + responseBody); // 📌 여기서( 에러 메시지 출력
                    return;
                }

                Log.d("ChatGPT", "응답 성공: " + responseBody);
                runOnUiThread(() -> {
                    String content = extractContentFromResponse(responseBody);
                    Log.d("GPT_RAW", content);

                    // json 형태로 gpt 응답
                    try {
                        JSONObject gptResponse = new JSONObject(content);
                        String summary = gptResponse.getString("summary");
                        saveChatHistory(summary);   // 요약 삽입
                    } catch (JSONException e) {
                        Log.e("JSON_PARSE_ERROR", "JSON 파싱 오류: " + e.getMessage());
                    }
                    runOnUiThread(() -> Btn_send.setEnabled(true));
                });
            }
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("callBack", "onRestart");

        // 시스템 메시지 추가 (상담사 프롬프트 작성 + 요약본 모두 불러오기)
        {
            String history = loadChatHistory(); // 채팅 요약본 모두 불러오기
            String prompt = buildPrompt();  // 프롬프트 작성

            ChatMessage system = new ChatMessage("system", history + prompt);
            messages.add(system.message);
        }
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
    protected void createUserTextView() {
        TextView textView = new TextView(this);
        textView.setText(userInputField.getText().toString());
        textView.setTextSize(16);
        textView.setBackgroundColor(Color.parseColor("#b6bffe"));
        textView.setBackgroundResource(R.drawable.chat_box_green);
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
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

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.END;
        params.setMargins(0, 25, 0, 25);

        // 파라미터 적용
        textView.setLayoutParams(params);
        layout.addView(textView);
    }

    //AI 응답 받고 Textview 위젯 생성
    protected void createResponseTextView(String Message) {
        TextView textView = new TextView(this);
        textView.setText(Message.toString());
        textView.setTextSize(16);
        textView.setBackgroundColor(Color.parseColor("#D0CDCD"));
        textView.setBackgroundResource(R.drawable.chat_box_gray);
        textView.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
        textView.setAutoLinkMask(0);
        textView.setPaintFlags(textView.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        Btn_send.postDelayed(() -> Btn_send.setEnabled(true), 3000);

        userInputField.setText("");
        LinearLayout layout = findViewById(R.id.chatContainer);


        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 25, 0, 25);

        // 파라미터 적용
        textView.setLayoutParams(params);
        layout.addView(textView);
    }

    private void sendChatMessage(String userInput) {
        OkHttpClient client = new OkHttpClient();
        Map<String, Object> body = buildBody(userInput);

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
                    Log.d("GPT_RAW", content);

                    // json 형태로 gpt 응답
                    try {
                        JSONObject gptResponse = new JSONObject(content);
                        String r = gptResponse.getString("response");

                        // 챗봇 응답 추가
                        ChatMessage assistant = new ChatMessage("assistant", r);
                        messages.add(assistant.message);

                        createResponseTextView(r);
                    } catch (JSONException e) {
                        Log.e("JSON_PARSE_ERROR", "JSON 파싱 오류: " + e.getMessage());

                        ChatMessage assistant = new ChatMessage("assistant", content);
                        messages.add(assistant.message);

                        createResponseTextView("⚠️ 응답을 JSON으로 파싱할 수 없습니다.\n" + content);
                    }
                });
            }
        });
    }

    /// 챗봇 프롬프트를 작성한다.
    private String buildPrompt() {
        return "You are a kind and professional counselor. Always reply in the user's language.\n" +
                "Your reply must have to under the 100letters" +
                "Response to following text and Respond in **strict JSON format only** without any explanation or prefix.\n" +
                "Use this exact format:" +
                "\n\n{\"response\": \"Full Response\"}";
    }

    /// 요약 프롬프트를 작성한다.
    private String buildSummaryPrompt() {
        return "You are a summarizing assistant. " +
                "Based on the following conversation history, generate a concise summary in JSON format only. " +
                "Use this format:\n\n{\"summary\": \"요약 내용\"}";
    }

    /// API를 요청하고 body를 생성한다.
    private Map<String, Object> buildBody(String userInput) {
        return buildBody(userInput, false);
    }

    /// API를 요청하고 body를 생성한다.
    private Map<String, Object> buildBody(String input, boolean systemMode) {
        Map<String, Object> body = new HashMap<>();

        // API 요청
        body.put("model", "gpt-3.5-turbo");
        body.put("max_tokens", 700);

        if (systemMode) {
            // 시스템 메시지 추가
            ChatMessage system = new ChatMessage("system", input);
            messages.add(system.message);
        } else {
            // 사용자 응답 추가
            ChatMessage user = new ChatMessage("user", input);
            messages.add(user.message);
        }

        body.put("messages", messages);
        return body;
    }

    /// DB에 저장된 채팅 요약본을 모두 불러온다.
    private String loadChatHistory() {
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
    private void saveChatHistory(String summary) {
        // 비어 있으면 저장 안 한다
        if (summary.isEmpty())
            return;
        ChatHistoryDatabaseHelper db = new ChatHistoryDatabaseHelper(this);
        Log.d("GPT_RAW", JSONObject.quote(summary));
        db.insert(summary);
    }
}