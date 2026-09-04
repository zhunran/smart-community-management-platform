package com.property.framework.util;

import com.property.framework.service.SysConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 敏感词过滤器（DFA 前缀树实现）
 * 从 sys_config(key=sensitive.words) 加载词库，支持热更新
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveWordFilter {

    private static final String CONFIG_KEY = "sensitive.words";
    private static final String WORD_SEPARATOR = ",";
    private static final String END_FLAG = "isEnd";
    private static final int END_FLAG_LEN = END_FLAG.length();

    private final SysConfigService sysConfigService;

    @SuppressWarnings("rawtypes")
    private volatile Map sensitiveWordMap = new HashMap<>();

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 热更新词库（从数据库重新加载）
     */
    public synchronized void reload() {
        String configValue = sysConfigService.getString(CONFIG_KEY, "");
        sysConfigService.refreshCache(CONFIG_KEY);
        if (configValue == null || configValue.isBlank()) {
            log.warn("敏感词库为空，key={}", CONFIG_KEY);
            this.sensitiveWordMap = new HashMap<>();
            return;
        }
        String[] words = configValue.split(WORD_SEPARATOR);
        @SuppressWarnings("rawtypes")
        Map<String, Map> newMap = new HashMap<>(words.length);
        for (String word : words) {
            String trimmed = word.trim();
            if (trimmed.isEmpty()) continue;
            addWord(newMap, trimmed);
        }
        this.sensitiveWordMap = newMap;
        log.info("敏感词库加载完成，共 {} 个词", words.length);
    }

    /**
     * 过滤文本，返回命中的敏感词列表
     */
    @SuppressWarnings("unchecked")
    public List<String> filter(String text) {
        if (text == null || text.isEmpty() || sensitiveWordMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        int len = text.length();
        for (int i = 0; i < len; i++) {
            int matchLen = checkWord(text, i);
            if (matchLen > 0) {
                result.add(text.substring(i, i + matchLen));
                i += matchLen - 1;
            }
        }
        return result;
    }

    /**
     * 检查文本中是否包含敏感词
     */
    public boolean containsSensitiveWord(String text) {
        List<String> words = filter(text);
        return !words.isEmpty();
    }

    /**
     * 将敏感词替换为指定字符
     */
    public String replace(String text, char replacement) {
        List<String> words = filter(text);
        if (words.isEmpty()) return text;
        String result = text;
        for (String word : words) {
            result = result.replace(word, String.valueOf(replacement).repeat(word.length()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void addWord(Map<String, Map> map, String word) {
        Map<String, Map> current = map;
        for (int i = 0; i < word.length(); i++) {
            String key = String.valueOf(word.charAt(i));
            Map<String, Map> child = current.get(key);
            if (child == null) {
                child = new HashMap<>();
                current.put(key, child);
            }
            current = child;
            if (i == word.length() - 1) {
                current.put(END_FLAG, null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private int checkWord(String text, int beginIndex) {
        Map<String, Map> current = sensitiveWordMap;
        int matchLen = 0;
        for (int i = beginIndex; i < text.length(); i++) {
            String key = String.valueOf(text.charAt(i));
            Map<String, Map> child = current.get(key);
            if (child == null) break;
            current = child;
            matchLen++;
            if (current.containsKey(END_FLAG)) {
                return matchLen;
            }
        }
        return 0;
    }
}