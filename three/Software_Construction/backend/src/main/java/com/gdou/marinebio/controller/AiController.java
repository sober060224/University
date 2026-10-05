package com.gdou.marinebio.controller;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.common.PageResult;
import com.gdou.marinebio.common.Result;
import com.gdou.marinebio.dto.AuthForms;
import com.gdou.marinebio.entity.AiRecord;
import com.gdou.marinebio.entity.ChatMessage;
import com.gdou.marinebio.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 模块五：智能服务接口。
 * 大模型调用全部在服务端完成，API Key 不出现在任何响应里。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // ========== 图像智能识别与物种鉴定 ==========
    @PostMapping("/identify")
    public Result<Map<String, Object>> identify(@Valid @RequestBody AuthForms.Identify form,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(aiService.identify(form, loginUser));
    }

    // ========== 文本辅助分类与补全 ==========
    @PostMapping("/complete")
    public Result<Map<String, Object>> complete(@Valid @RequestBody AuthForms.Complete form,
                                                @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(aiService.complete(form, loginUser));
    }

    // ========== 观测记录智能标签与异常检测（选做） ==========
    @PostMapping("/analyze")
    public Result<Map<String, Object>> analyze(@Valid @RequestBody AuthForms.Analyze form,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(aiService.analyze(form, loginUser));
    }

    // ========== 智能问答与科研助手 ==========
    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@Valid @RequestBody AuthForms.Ask form,
                                           @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(aiService.ask(form, loginUser));
    }

    @GetMapping("/history")
    public Result<List<ChatMessage>> history(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(aiService.history(loginUser.getId()));
    }

    @DeleteMapping("/history")
    public Result<Void> clearHistory(@AuthenticationPrincipal LoginUser loginUser) {
        aiService.clearHistory(loginUser.getId());
        return Result.ok("对话记录已清空", null);
    }

    // ========== 物种描述多语言支持 ==========
    @PostMapping("/translate")
    public Result<Map<String, Object>> translate(@Valid @RequestBody AuthForms.Translate form,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(aiService.translate(form, loginUser));
    }

    // ========== 调用记录，便于追溯与质量分析 ==========
    @GetMapping("/records")
    public Result<PageResult<AiRecord>> records(@AuthenticationPrincipal LoginUser loginUser,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return Result.ok(PageResult.of(
                aiService.records(loginUser.getId(), PageResult.of(page, size, 100, Sort.by(Sort.Direction.DESC, "id")))));
    }
}
