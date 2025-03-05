package com.ytycc.initializer;

import com.ytycc.dispatch.ChannelWay;
import com.ytycc.utils.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStarter implements ApplicationRunner, ApplicationContextAware {


    @Value("${receivePort}")
    private Integer receivePort;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringUtil.initContext(applicationContext);
    }

    @Override
    public void run(ApplicationArguments args) {
        new ChannelWay(receivePort).openWay();
    }


}
