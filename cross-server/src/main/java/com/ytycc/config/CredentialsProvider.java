package com.ytycc.config;

import com.ytycc.utils.AuthUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;


@Component
public class CredentialsProvider {


    private final String idToken;

    public CredentialsProvider(@Value("${idToken:}") String idToken) {
        if (StringUtils.hasText(idToken))
            this.idToken = idToken;
        else {
            this.idToken = AuthUtil.finalToken(AuthUtil.preToken("user", "123456")).orElseThrow();
        }
    }


    public String idToken() {
        return idToken;
    }
}
