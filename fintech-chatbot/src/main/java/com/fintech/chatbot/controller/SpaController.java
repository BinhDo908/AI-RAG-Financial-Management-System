package com.fintech.chatbot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Giao diện React là SPA: các đường dẫn phía client (/login, /chat/12…) đều trả về index.html,
 * React Router tự hiển thị trang tương ứng. File build nằm ở src/main/resources/static (frontend/vite.config.ts).
 */
@Controller
public class SpaController {

    @GetMapping({"/login", "/chat", "/chat/{id}", "/overview", "/transactions", "/wallets", "/budgets", "/admin", "/admin/{page}"})
    public String index() {
        return "forward:/index.html";
    }
}
