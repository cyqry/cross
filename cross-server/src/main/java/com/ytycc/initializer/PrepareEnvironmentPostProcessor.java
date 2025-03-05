package com.ytycc.initializer;

import com.ytycc.constant.DefaultConst;
import com.ytycc.utils.AuthUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PrepareEnvironmentPostProcessor implements EnvironmentPostProcessor {


    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Binder binder = Binder.get(environment);
        Bindable<List<String>> listBindable = Bindable.listOf(String.class);
        List<String> forwardPorts = binder
                .bind("forward-port", listBindable)
                .orElse(List.of());
        List<String> receivePorts = binder
                .bind("receive-port", listBindable)
                .orElse(List.of());
        List<String> users = binder
                .bind("user", listBindable)
                .orElse(List.of());
        List<String> passwords = binder
                .bind("password", listBindable)
                .orElse(List.of());


        int forwardPort = DefaultConst.DefaultForwardPort;
        int receivePort = DefaultConst.DefaultReceivePort;

        String user = null, password = null, idToken = null;


        if (!ObjectUtils.isEmpty(users)) {
            user = users.get(0);
        }
        if (!ObjectUtils.isEmpty(passwords)) {
            password = passwords.get(0);
        }

        if ((StringUtils.hasText(user) != StringUtils.hasText(password))) {
            System.out.println("请输入预设的账号和密码");
            System.exit(0);
        }

        if (StringUtils.hasText(user)) {
            Optional<String> token = AuthUtil.finalToken(AuthUtil.preToken(user, password));
            if (token.isEmpty()) {
                System.out.println("账号或密码过于简单");
                System.exit(0);
            }
            idToken = token.get();
        }

        try {
            if (receivePorts != null && !receivePorts.isEmpty()) {
                receivePort = Integer.parseInt(receivePorts.get(0));
            }
        } catch (NumberFormatException ignored) {
        }

        try {
            if (forwardPorts != null && !forwardPorts.isEmpty()) {
                forwardPort = Integer.parseInt(forwardPorts.get(0));
            }
        } catch (NumberFormatException ignored) {
        }

        try {
            if (receivePorts != null && !receivePorts.isEmpty()) {
                receivePort = Integer.parseInt(receivePorts.get(0));
            }
        } catch (NumberFormatException ignored) {
        }

        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("forwardPort", forwardPort);
        serverInfo.put("receivePort", receivePort);
        serverInfo.put("idToken", idToken);
        environment.getPropertySources().addLast(new MapPropertySource("serverInfo", serverInfo));
    }
}
