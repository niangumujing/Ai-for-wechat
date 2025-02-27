package com.ngmj.towechat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ngmj.towechat.entity.po.UserInfo;
import com.ngmj.towechat.service.UserinfoService;
import com.ngmj.towechat.mapper.UserinfoMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【UserInfo】的数据库操作Service实现
* @createDate 2025-02-24 18:54:28
*/
@Service
public class UserinfoServiceImpl extends ServiceImpl<UserinfoMapper, UserInfo>
    implements UserinfoService{
}




