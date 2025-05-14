package com.example.a2025termproject;

import java.util.HashMap;
import java.util.Map;

public class ChatMessage
{
    public String role;
    public String content;
    public Map<String, Object> message = new HashMap<>();

    public ChatMessage(String role, String content)
    {
        this.role = role;
        this.content = content;

        message.put("role", role);
        message.put("content", content);
    }
}

