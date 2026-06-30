package com.freddieapp.legacyadapter.config;

import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

@Configuration
public class SoapClientConfig {

    @Value("${legacy.soap.osb.endpoint.loan-eligibility}")
    private String loanEligibilityEndpoint;

    @Value("${legacy.soap.osb.endpoint.customer-verification}")
    private String customerVerificationEndpoint;

    @Value("${legacy.soap.wss.username}")
    private String wssUsername;

    @Value("${legacy.soap.wss.password}")
    private String wssPassword;

    @Bean
    public Wss4jSecurityInterceptor wssInterceptor() {
        Wss4jSecurityInterceptor interceptor = new Wss4jSecurityInterceptor();
        interceptor.setSecurementActions("UsernameToken");
        interceptor.setSecurementUsername(wssUsername);
        interceptor.setSecurementPassword(wssPassword);
        interceptor.setSecurementPasswordType("PasswordDigest");
        interceptor.setSecurementMustUnderstand(true);
        return interceptor;
    }

    @Bean
    public Jaxb2Marshaller loanEligibilityMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.freddieapp.legacyadapter.wsdl.loaneligibility");
        return marshaller;
    }

    @Bean
    public Jaxb2Marshaller customerVerificationMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.freddieapp.legacyadapter.wsdl.customerverification");
        return marshaller;
    }

    @Bean
    public WebServiceTemplate loanEligibilityTemplate() {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setDefaultUri(loanEligibilityEndpoint);
        template.setMarshaller(loanEligibilityMarshaller());
        template.setUnmarshaller(loanEligibilityMarshaller());
        template.setInterceptors(new org.springframework.ws.client.support.interceptor.ClientInterceptor[]{wssInterceptor()});
        template.setMessageSender(httpMessageSender());
        return template;
    }

    @Bean
    public WebServiceTemplate customerVerificationTemplate() {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setDefaultUri(customerVerificationEndpoint);
        template.setMarshaller(customerVerificationMarshaller());
        template.setUnmarshaller(customerVerificationMarshaller());
        template.setInterceptors(new org.springframework.ws.client.support.interceptor.ClientInterceptor[]{wssInterceptor()});
        template.setMessageSender(httpMessageSender());
        return template;
    }

    @Bean
    public HttpComponentsMessageSender httpMessageSender() {
        org.apache.http.client.config.RequestConfig config = org.apache.http.client.config.RequestConfig.custom()
                .setConnectTimeout(10000)
                .setSocketTimeout(30000)
                .build();

        return new HttpComponentsMessageSender(
                HttpClientBuilder.create()
                        .setDefaultRequestConfig(config)
                        .setConnectionTimeToLive(10, java.util.concurrent.TimeUnit.SECONDS)
                        .setMaxConnTotal(50)
                        .setMaxConnPerRoute(25)
                        .build()
        );
    }
}
