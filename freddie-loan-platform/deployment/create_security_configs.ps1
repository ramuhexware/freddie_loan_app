# api-gateway (Reactive WebFlux Security Config)
$gatewayDir = "c:\ramu\Project_Assignment\RapidX\FreddeMac_Project_RapidX\Freddie_Style_Application\freddie-loan-platform\api-gateway\src\main\java\com\freddieapp\apigateway\config"
if (!(Test-Path $gatewayDir)) { New-Item -ItemType Directory -Path $gatewayDir -Force }
$gatewayConfig = @"
package com.freddieapp.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
        return http.build();
    }
}
"@
Set-Content -Path "$gatewayDir\SecurityConfig.java" -Value $gatewayConfig -Encoding utf8
Write-Host "Created SecurityConfig for api-gateway"

# customer-service (Servlet Web Security Config)
$customerDir = "c:\ramu\Project_Assignment\RapidX\FreddeMac_Project_RapidX\Freddie_Style_Application\freddie-loan-platform\customer-service\src\main\java\com\freddieapp\customerservice\config"
if (!(Test-Path $customerDir)) { New-Item -ItemType Directory -Path $customerDir -Force }
$customerConfig = @"
package com.freddieapp.customerservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
"@
Set-Content -Path "$customerDir\SecurityConfig.java" -Value $customerConfig -Encoding utf8
Write-Host "Created SecurityConfig for customer-service"

# loan-origination-service (Servlet Web Security Config)
$loanDir = "c:\ramu\Project_Assignment\RapidX\FreddeMac_Project_RapidX\Freddie_Style_Application\freddie-loan-platform\loan-origination-service\src\main\java\com\freddieapp\loanorigination\config"
if (!(Test-Path $loanDir)) { New-Item -ItemType Directory -Path $loanDir -Force }
$loanConfig = @"
package com.freddieapp.loanorigination.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
"@
Set-Content -Path "$loanDir\SecurityConfig.java" -Value $loanConfig -Encoding utf8
Write-Host "Created SecurityConfig for loan-origination-service"

# underwriting-service (Servlet Web Security Config)
$underwritingDir = "c:\ramu\Project_Assignment\RapidX\FreddeMac_Project_RapidX\Freddie_Style_Application\freddie-loan-platform\underwriting-service\src\main\java\com\freddieapp\underwriting\config"
if (!(Test-Path $underwritingDir)) { New-Item -ItemType Directory -Path $underwritingDir -Force }
$underwritingConfig = @"
package com.freddieapp.underwriting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
"@
Set-Content -Path "$underwritingDir\SecurityConfig.java" -Value $underwritingConfig -Encoding utf8
Write-Host "Created SecurityConfig for underwriting-service"
