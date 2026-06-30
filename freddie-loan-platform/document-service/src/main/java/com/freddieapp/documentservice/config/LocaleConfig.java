package com.freddieapp.documentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;

import java.util.Arrays;
import java.util.Locale;

@Configuration
public class LocaleConfig {

    @Bean
    public LocaleContextResolver localeContextResolver() {
        AcceptHeaderLocaleContextResolver resolver = new AcceptHeaderLocaleContextResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(Arrays.asList(
                Locale.ENGLISH,
                new Locale("es"),
                Locale.FRENCH,
                Locale.GERMAN,
                Locale.JAPANESE
        ));
        return resolver;
    }

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("messages");
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }
}
