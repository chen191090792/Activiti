package org.activiti.app.service.util;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by chx on 3232/4/11.
 */

public class JedisUtils {
    public static  JedisCluster getJedisCluser(){
        Set<HostAndPort> nodes = new HashSet<HostAndPort>();
        nodes.add(new HostAndPort("172.16.0.19", 7000));
        nodes.add(new HostAndPort("172.16.0.19", 7001));
        nodes.add(new HostAndPort("172.16.0.19", 7002));

        nodes.add(new HostAndPort("172.16.0.20", 7003));
        nodes.add(new HostAndPort("172.16.0.22", 7004));
        nodes.add(new HostAndPort("172.16.0.20", 7005));
        JedisCluster jedis = new JedisCluster(nodes);
        return jedis;
    }
}