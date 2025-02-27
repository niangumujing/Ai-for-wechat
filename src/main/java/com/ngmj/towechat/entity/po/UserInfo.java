package com.ngmj.towechat.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("userinfo")
public class UserInfo {
    @TableId
    private String openId;
    private String apiInfo;

    private String platform;
}
