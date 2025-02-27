package com.ngmj.towechat.entity.po;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AliyunBaiLian implements BaseAi{
    private String accessKeyId;
    private String model;
}
