package com.property.module.statistic.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.property.module.statistic.vo.AuditModuleStatVO;
import com.property.module.statistic.vo.AuditUserStatVO;
import com.property.module.statistic.vo.FeeTrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface FeeTrendMapper {
    @Select("""
        SELECT DATE(payment_time)              AS date,
               COUNT(DISTINCT owner_id)        AS payerCount,
               COALESCE(SUM(payment_amount),0) AS amount
        FROM t_payment
        WHERE payment_status = 2
          AND del_flag = 0
          AND payment_time >= #{start}                        -- 含 start 当天
          AND payment_time < DATE_ADD(#{end}, INTERVAL 1 DAY)  -- 含 end 当天（取到次日0点）
        GROUP BY DATE(payment_time)
        ORDER BY DATE(payment_time)
        """)
    List<FeeTrendPointVO> selectDailyFeeTrend(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Select("<script>SELECT module, COUNT(*) AS count\n" +
            "FROM t_sys_operation_log\n" +
            "WHERE create_time >= #{start} AND create_time &lt; #{end}\n" +
            "<if test='status != null'>\n" +
            "    AND status = #{status}\n" +
            "</if>\n" +
            "<if test='username != null and username != \"\"'>\n" +
            "    AND user_name = #{username}\n" +
            "</if>\n" +
            "GROUP BY module\n" +
            "ORDER BY count DESC\n" +
            "LIMIT 10</script>")
    List<AuditModuleStatVO> selectModuleCount(@Param("start")LocalDateTime start,
                                              @Param("end")LocalDateTime end,
                                              @Param("status")Integer status,
                                              @Param("username")String username);
    @Select("SELECT user_name, real_name, COUNT(*) AS count\n" +
            "FROM t_sys_operation_log\n" +
            "WHERE create_time >= #{start} AND create_time < #{end}\n" +
            "GROUP BY user_name, real_name\n" +
            "ORDER BY count DESC\n" +
            "LIMIT 10")
    List<AuditUserStatVO> selectUserStat(@Param("start")LocalDateTime start,
                                         @Param("end")LocalDateTime end);
    @Select("SELECT action, COUNT(*) AS count\n" +
            "FROM t_sys_operation_log\n" +
            "WHERE create_time >= #{start} AND create_time < #{end}\n" +
            "GROUP BY action")
    List<Map<String, Object>> selectAction(@Param("start")LocalDateTime start,
                                            @Param("end")LocalDateTime end);
}
