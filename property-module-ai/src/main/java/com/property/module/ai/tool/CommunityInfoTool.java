package com.property.module.ai.tool;


import com.property.framework.web.security.SecurityUtil;
import com.property.module.notification.dto.NoticeVO;
import com.property.module.notification.service.NoticeService;
import com.property.module.bill.dto.request.BillPageQuery;
import com.property.module.bill.dto.response.BillVO;
import com.property.module.bill.service.BillService;
import com.property.module.owner.dto.response.OwnerRoomVO;
import com.property.module.owner.service.OwnerRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityInfoTool {

    private final BillService billService;
    private final OwnerRoomService ownerRoomService;
    private final NoticeService noticeService;
    //查询当前业主的的待缴费账单
    @Tool(description = "查看当前业主的待教费账单列表，返回账单周期，金额，截止日期等")
    public List<BillVO> queryMyBills(){
        Long userId= SecurityUtil.requireUser().getUserId();
        log.info("AI工具调用：queryMyBills[userId={}]",userId);
        BillPageQuery query=new BillPageQuery();
        query.setOwnerId(userId);
        query.setStatus(0);
        query.setCurrent(1);
        query.setSize(20);
        return billService.page(query).getRecords();
    }
    //查询当前业主的房屋信息
    @Tool(description = "查询当前业主的房屋信息，返回楼栋，房号，业主关系信息等")
    public List<OwnerRoomVO> queryMyRooms(){
        Long userId=SecurityUtil.requireUser().getUserId();
        log.info("AI工具调用：queryMyRooms[userId={}]",userId);
        return ownerRoomService.listByOwnerId(userId);
    }
    //查询小区最新公告
    @Tool(description = "查询小区最新公告与通知，返回公告标题，内容，类型，发布时间，")
    public List<NoticeVO> queryLatestNotices(@ToolParam(description = "查询数量。默认5条")Integer limit)
    {
        int n=(limit !=null&&limit>0&&limit<=20)?limit:5;
        log.info("AI工具调用：queryLatestNotices[limit={}]",n);
        return noticeService.listLatest(n);
    }
    //查询小区当前状况简报（实时生成）
    @Tool(description = " 查询小区当前的总和状况简报，包括缴费率，车位余量，最新公告")
    public String queryCommunityBrief()
    {
        log.info("AI工具调用：queryCommunityBrief");
        StringBuilder stringBuilder=new StringBuilder("[智慧社区实时简报]");
        //最新公告
        List<NoticeVO> noticeVOS=noticeService.listLatest(3);
        if (!noticeVOS.isEmpty())
        {
            stringBuilder.append("△最新公告：\n");
            for (NoticeVO vo:noticeVOS)
            {
                stringBuilder.append(vo);
            }
        }
        return stringBuilder.toString();
    }
}
