package com.chat_server.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * packageName    : com.chat_server.user.controller
 * fileName       : TestController
 * author         : parkminsu
 * date           : 25. 5. 30.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 25. 5. 30.        parkminsu       최초 생성
 */
@Controller
public class TestController {
    @GetMapping("/test")
    public String handlePost() {

        return "test.html"; // 혹은 JSON 응답 등
    }

    @PostMapping("/post/write")
    public String handlePost(@ModelAttribute PostForm form) {
        System.out.println("제목: " + form.getTitle());
        System.out.println("내용: " + form.getContent());
        // 저장 로직 수행
        return "redirect:/post/success"; // 혹은 JSON 응답 등
    }
}
