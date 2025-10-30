package com.wolfhouse.answerme.model.dto.postfavour;

import com.wolfhouse.answerme.common.PageRequest;
import com.wolfhouse.answerme.model.dto.post.PostQueryRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 帖子收藏查询请求
 *
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostFavourQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 帖子查询请求
     */
    private PostQueryRequest postQueryRequest;
    /**
     * 用户 id
     */
    private Long userId;
}