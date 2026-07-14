package com.test.seckill.mapper;

import com.test.seckill.entity.LocalMessage;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

/**
 * 本地消息Mapper接口
 */
@Mapper
public interface LocalMessageMapper {
    /**
     * 根据ID查询消息
     * @param id 消息ID
     * @return 消息对象
     */
    @Select("SELECT * FROM local_message WHERE id = #{id}")
    LocalMessage selectById(Long id);
    
    /**
     * 查询待确认消息（用于补偿扫描）
     * @param status 状态
     * @param currentTime 当前时间
     * @return 消息列表
     */
    @Select("SELECT * FROM local_message WHERE status = #{status} AND next_retry_time <= #{currentTime}")
    List<LocalMessage> selectPendingMessages(Integer status, Date currentTime);
    
    /**
     * 插入消息
     * @param message 消息对象
     * @return 影响行数
     */
    @Insert("INSERT INTO local_message (id, business_id, business_type, message_content, status, retry_count, max_retry_count, next_retry_time, create_time, update_time) VALUES (#{id}, #{businessId}, #{businessType}, #{messageContent}, #{status}, #{retryCount}, #{maxRetryCount}, #{nextRetryTime}, #{createTime}, #{updateTime})")
    int insert(LocalMessage message);
    
    /**
     * 更新消息状态
     * @param id 消息ID
     * @param status 状态
     * @return 影响行数
     */
    @Update("UPDATE local_message SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(Long id, Integer status);
    
    /**
     * 更新重试信息
     * @param id 消息ID
     * @param retryCount 重试次数
     * @param nextRetryTime 下次重试时间
     * @return 影响行数
     */
    @Update("UPDATE local_message SET retry_count = #{retryCount}, next_retry_time = #{nextRetryTime}, update_time = NOW() WHERE id = #{id}")
    int updateRetryInfo(Long id, Integer retryCount, Date nextRetryTime);
    
    /**
     * 根据业务ID查询消息
     * @param businessId 业务ID
     * @return 消息对象
     */
    @Select("SELECT * FROM local_message WHERE business_id = #{businessId} AND status = 0")
    LocalMessage selectByBusinessId(String businessId);
}