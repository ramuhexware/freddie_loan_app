package com.freddieapp.legacyadapter.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.SimpleWsdl11Definition;

@EnableWs
@Configuration
public class SoapServerConfig extends WsConfigurerAdapter {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "LoanApprovalService")
    public SimpleWsdl11Definition loanApprovalWsdl() {
        return new SimpleWsdl11Definition(new ClassPathResource("wsdl/LoanApprovalProcess.wsdl"));
    }

    @Bean(name = "LegacySoapService")
    public SimpleWsdl11Definition legacySoapWsdl() {
        return new SimpleWsdl11Definition(new ClassPathResource("wsdl/LegacySoapService.wsdl"));
    }
}
