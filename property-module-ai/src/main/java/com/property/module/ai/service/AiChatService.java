package com.property.module.ai.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.property.framework.service.SysConfigService;
import com.property.module.ai.dto.ChatHistoryVO;
import com.property.module.ai.dto.ChatRequest;
import com.property.module.ai.dto.ChatSessionVO;
import com.property.module.ai.entity.ChatHistory;
import com.property.module.ai.entity.ChatSession;
import com.property.module.ai.repository.ChatHistoryMapper;
import com.property.module.ai.repository.ChatSessionMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * AI 客服服务
 *
 * 封装对话逻辑：系统提示词加载、流式对话、聊天历史存储、会话管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    /** 系统提示词配置 Key */
    private static final String PROMPT_CONFIG_KEY = "ai.chat.system_prompt";
    /** 默认系统提示词 */
    private static final String DEFAULT_SYSTEM_PROMPT = """
        你是AI智慧社区服务平台的智能客服助手，为用户提供物业管理相关咨询服务。
        你的服务范围包括：
        1. 缴费咨询：物业费、水费、电费、燃气费等账单查询与缴费方式
        2. 业务咨询：报修流程、车位租赁、访客登记等
        3. 社区公告：社区活动、停水停电通知等
        请用友好、专业的语气回答，回答简洁明了，每次不超过150字。
        回复中不要使用任何引号（包括中文引号"和英文引号"）。
        如果你不知道答案，请引导用户联系物业服务中心。
        当用户询问账单、房屋、公告等具体信息时，请主动调用相关工具查询最新数据。""";

    private final ChatClient chatClient;
    private final SysConfigService sysConfigService;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ChatSessionMapper chatSessionMapper;

    // ==================== 会话管理 ====================

    /**
     * 创建新会话
     */
    @Transactional
    public ChatSession createSession(Long userId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle("新对话");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return session;
    }

    /**
     * 创建新会话并返回 VO（供 Controller 直接使用）
     */
    @Transactional
    public ChatSessionVO createSessionAndReturnVO(Long userId) {
        ChatSession session = createSession(userId);
        ChatSessionVO vo = new ChatSessionVO();
        BeanUtils.copyProperties(session, vo);
        return vo;
    }

    /**
     * 查询用户的所有会话列表
     */
    public List<ChatSessionVO> listSessions(Long userId) {
        List<ChatSession> sessions = chatSessionMapper.selectByUserId(userId);
        return sessions.stream().map(s -> {
            ChatSessionVO vo = new ChatSessionVO();
            BeanUtils.copyProperties(s, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 删除会话及其所有消息
     */
    @Transactional
    public void deleteSession(Long sessionId) {
        // 删除会话下的所有消息
        List<ChatHistory> histories = chatHistoryMapper.selectBySessionId(sessionId);
        for (ChatHistory h : histories) {
            chatHistoryMapper.deleteById(h.getId());
        }
        // 删除会话本身
        chatSessionMapper.deleteById(sessionId);
    }

    // ==================== 对话 ====================

    /**
     * 流式对话（SSE）
     *
     * @param sessionId 会话ID（null 则自动创建）
     * @param request   对话请求（含 userId、message）
     * @return 流式响应 Flux<String>
     */
    @Transactional
    public Flux<String> chatStream(Long sessionId, ChatRequest request) {
        // 如果没有会话ID，自动创建新会话
        if (sessionId == null) {
            ChatSession session = createSession(request.getUserId());
            sessionId = session.getId();
            request.setSessionId(sessionId); // 回写，便于 Controller 回调使用
        } else {
            // 更新会话的 updateTime
            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session != null) {
                session.setUpdateTime(LocalDateTime.now());
                chatSessionMapper.updateById(session);
            }
        }

        final Long sid = sessionId;
        String systemPrompt = getSystemPrompt();
        String userMessage = request.getMessage();

        // 自动更新会话标题（取首条用户消息前20字）
        updateSessionTitle(sid);

        // 保存用户消息到历史
        saveHistory(request.getUserId(), sid, "user", request.getMessage());

        // 构建 Prompt 并流式调用
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                // 按 sessionId 作为 conversationId 隔离各会话记忆
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sid))
                .stream()
                .content()
                .doOnError(e -> log.error("AI对话异常【userId={}, sessionId={}】", request.getUserId(), sid, e));
    }

    /**
     * 保存 AI 回复到聊天历史（由 Controller 在流结束后调用）
     */
    public void saveAssistantMessage(Long userId, Long sessionId, String content) {
        saveHistory(userId, sessionId, "assistant", content);
    }

    // ==================== 历史查询 ====================

    /**
     * 查询指定会话的聊天历史（按时间正序）
     */
    public List<ChatHistoryVO> getHistoryBySession(Long sessionId) {
        List<ChatHistory> list = chatHistoryMapper.selectBySessionId(sessionId);
        return list.stream()
                .map(history -> {
                    ChatHistoryVO vo = new ChatHistoryVO();
                    BeanUtils.copyProperties(history, vo);
                    return vo;
                }).collect(Collectors.toList());
    }

    // ==================== 内部方法 ====================

    /**
     * 获取系统提示词：优先从 sys_config 读取，未配置则用默认值
     */
    private String getSystemPrompt() {
        try {
            String custom = sysConfigService.getString(PROMPT_CONFIG_KEY, null);
            if (custom != null && !custom.isBlank()) {
                return custom;
            }
        } catch (Exception e) {
            log.warn("读取系统提示词配置失败，使用默认提示词");
        }
        return DEFAULT_SYSTEM_PROMPT;
    }

    /**
     * 保存聊天历史
     */
    private void saveHistory(Long userId, Long sessionId, String role, String content) {
        ChatHistory history = new ChatHistory();
        history.setUserId(userId);
        history.setSessionId(sessionId);
        history.setRole(role);
        history.setContent(content);
        history.setCreateTime(LocalDateTime.now());
        chatHistoryMapper.insert(history);
    }

    /**
     * 自动更新会话标题（取首条用户消息的前20字）
     */
    private void updateSessionTitle(Long sessionId) {
        try {
            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session == null || !"新对话".equals(session.getTitle())) {
                return;
            }
            List<ChatHistory> histories = chatHistoryMapper.selectBySessionId(sessionId);
            for (ChatHistory h : histories) {
                if ("user".equals(h.getRole())) {
                    String title = h.getContent();
                    if (title.length() > 20) {
                        title = title.substring(0, 20) + "...";
                    }
                    session.setTitle(title);
                    chatSessionMapper.updateById(session);
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("更新会话标题失败[sessionId={}]", sessionId, e);
        }
    }
}