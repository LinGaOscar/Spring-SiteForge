package com.siteforge.web.config;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.ServletException;
import java.io.IOException;

@Configuration
public class TomcatRootRedirectConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> rootRedirect() {
        return factory -> factory.addEngineValves(new ValveBase() {
            @Override
            public void invoke(Request request, Response response) throws IOException, ServletException {
                if ("/".equals(request.getDecodedRequestURI())) {
                    response.sendRedirect("/web/");
                    return;
                }
                getNext().invoke(request, response);
            }
        });
    }
}
