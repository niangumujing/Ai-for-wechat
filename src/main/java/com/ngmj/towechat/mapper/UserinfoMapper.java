package com.ngmj.towechat.mapper;

import com.ngmj.towechat.entity.po.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;

/**
* @author Administrator
* @description 针对表【UserInfo】的数据库操作Mapper
* @createDate 2025-02-24 18:54:28
* @Entity com.ngmj.towechat.entity.Userinfo
*/
@Mapper
public interface UserinfoMapper extends BaseMapper<UserInfo> {

}




