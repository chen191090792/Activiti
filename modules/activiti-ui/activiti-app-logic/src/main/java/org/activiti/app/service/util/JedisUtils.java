package org.activiti.app.service.util;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by chx on 2019/4/11.
 */

public class JedisUtils {
    public static  JedisCluster getJedisCluser(){
        Set<HostAndPort> nodes = new HashSet<HostAndPort>();
        nodes.add(new HostAndPort("172.16.0.32", 7000));
        nodes.add(new HostAndPort("172.16.0.32", 7001));
        nodes.add(new HostAndPort("172.16.0.32", 7002));
        nodes.add(new HostAndPort("172.16.0.32", 7003));
        nodes.add(new HostAndPort("172.16.0.32", 7004));
        nodes.add(new HostAndPort("172.16.0.32", 7005));
        JedisCluster jedis = new JedisCluster(nodes);
        return jedis;
    }
}