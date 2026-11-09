package org.test.djhaicode.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工具调用事件 实体类。
 * 一行对应一次已执行完成的工具调用；summary 只存紧凑摘要，不存完整参数/结果。
 *
 * @author <a href="https://github.com/012hui">djh</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_event")
public class ChatEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 应用id
     */
    @Column("appId")
    private Long appId;

    /**
     * 轮次ID，对应 chat_history.turnId；同一轮的事件共享同一个值
     */
    @Column("turnId")
    private Long turnId;

    /**
     * 轮内顺序，从1递增；查询时按它升序
     */
    private Integer seq;

    /**
     * 工具名
     */
    @Column("toolName")
    private String toolName;

    /**
     * 工具执行摘要，用于压缩后替代完整工具结果，最长1024字符
     */
    private String summary;

    /**
     * 工具是否成功：1成功 0失败
     */
    private Integer success;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
