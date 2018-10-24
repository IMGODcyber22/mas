package com.letv.mas.caller.iptv.tvproxy.common.dao;

import com.letv.mas.caller.iptv.tvproxy.common.dao.sql.ChannelDataMysqlDao;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * MysqlDao门面类
 */
@Component(value = "facadeMysqlDao")
public class FacadeMysqlDao {

    @Resource
    private ChannelDataMysqlDao channelDataMysqlDao;

    public ChannelDataMysqlDao getChannelDataMysqlDao() {
        return channelDataMysqlDao;
    }
}
