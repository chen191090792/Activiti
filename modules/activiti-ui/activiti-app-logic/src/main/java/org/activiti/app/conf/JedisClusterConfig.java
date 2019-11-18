package org.activiti.app.conf;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class JedisClusterConfig {
    @Bean
    public JedisCluster getJedisCluster() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(60000);//设置最大连接数
        config.setMaxIdle(1000); //设置最大空闲数
        config.setMaxWaitMillis(3000);//设置超时时间
        config.setTestOnBorrow(true);
        Set<HostAndPort> nodes = new HashSet<HostAndPort>();
        nodes.add(new HostAndPort("172.16.0.20", 7000));
        nodes.add(new HostAndPort("172.16.0.20", 7001));
        nodes.add(new HostAndPort("172.16.0.20", 7002));
        nodes.add(new HostAndPort("172.16.0.19", 7003));
        nodes.add(new HostAndPort("172.16.0.19", 7004));
        nodes.add(new HostAndPort("172.16.0.19", 7005));
        return new JedisCluster(nodes,config);
    }
}
