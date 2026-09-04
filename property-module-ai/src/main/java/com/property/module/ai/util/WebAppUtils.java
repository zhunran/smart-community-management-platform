package com.property.module.ai.util;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.time.LocalDateTime;

public class WebAppUtils {
    @Tool(description = "两数求和")
    public int sum(@ToolParam(description = "加数") int a1,@ToolParam(description = "加数") int a2)
    {
        return a1+a2;
    }
    @Tool(description = "定时删除某个目录下的某些内容")
    public void removeFile(@ToolParam(description = "要删除的文件")File file,
                           @ToolParam(description = "执行时间")LocalDateTime time)
    {
//        ScheduledExecutorService executorService=Excu
    }
}
