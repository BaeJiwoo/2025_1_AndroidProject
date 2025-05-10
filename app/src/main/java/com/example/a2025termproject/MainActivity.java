package com.example.a2025termproject;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Map;

import okhttp3.*;

import com.example.a2025termproject.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    String apiKey = "Bearer " + BuildConfig.OPENAI_API_KEY;
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    Button Btn_send;
    TextInputEditText userInputField;
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

        // JSON 만들기
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "당신은 예의 바르고 침착한 상담가입니다. 사용자의 감정을 존중하며 공감하는 말투를 사용하세요.");

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", userInput);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-3.5-turbo");
        body.put("messages", Collections.singletonList(message));
        body.put("max_tokens", 100);
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
                    createResponseTextView(extractContentFromResponse(responseBody));
                });
            }
        });
    }
}