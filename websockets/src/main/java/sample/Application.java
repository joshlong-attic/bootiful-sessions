/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package sample;

import org.h2.server.web.WebServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;
import org.springframework.session.ExpiringSession;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.socket.config.annotation.AbstractSessionWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import sample.data.ActiveWebSocketUserRepository;
import sample.websocket.WebSocketConnectHandler;
import sample.websocket.WebSocketDisconnectHandler;

/**
 * @author Rob Winch
 */
@SpringBootApplication
public class Application {

	@Configuration
	static class H2Config {

		@Bean
		public ServletRegistrationBean h2Servlet() {
			ServletRegistrationBean servletBean = new ServletRegistrationBean();
			servletBean.addUrlMappings("/h2/*");
			servletBean.setServlet(new WebServlet());
			return servletBean;
		}
	}

	@Configuration
	@EnableWebSecurity
	@EnableGlobalMethodSecurity(prePostEnabled = true)
	@EnableRedisHttpSession
	static class WebSecurityConfig extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {

			http
					.authorizeRequests()
					.anyRequest().authenticated()
					.and()
					.formLogin()
					.and()
					.logout()
					.permitAll();
		}

		@Autowired
		public void configureGlobal(AuthenticationManagerBuilder auth, UserDetailsService userDetailsService) throws Exception {
			auth
					.userDetailsService(userDetailsService)
					.passwordEncoder(new BCryptPasswordEncoder());
		}

		@Bean
		public SecurityEvaluationContextExtension securityEvaluationContextExtension() {
			return new SecurityEvaluationContextExtension();
		}
	}

	@Configuration
	static class WebSocketHandlersConfig<S extends ExpiringSession> {

		@Bean
		public WebSocketConnectHandler<S> webSocketConnectHandler(SimpMessageSendingOperations messagingTemplate,
		                                                          ActiveWebSocketUserRepository repository) {
			return new WebSocketConnectHandler<S>(messagingTemplate, repository);
		}

		@Bean
		public WebSocketDisconnectHandler<S> webSocketDisconnectHandler(SimpMessageSendingOperations messagingTemplate,
		                                                                ActiveWebSocketUserRepository repository) {
			return new WebSocketDisconnectHandler<S>(messagingTemplate, repository);
		}
	}

	@Configuration
	@EnableScheduling
	@EnableWebSocketMessageBroker
	static class WebSocketConfig extends AbstractSessionWebSocketMessageBrokerConfigurer<ExpiringSession> {

		protected void configureStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/messages").withSockJS();
		}

		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.enableSimpleBroker("/queue/", "/topic/");
			registry.setApplicationDestinationPrefixes("/app");
		}
	}

	@Configuration
	static class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

		@Override
		protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
			messages
					.simpMessageDestMatchers("/queue/**", "/topic/**").denyAll()
					.simpSubscribeDestMatchers("/queue/**/*-user*", "/topic/**/*-user*").denyAll()
					.anyMessage().authenticated();
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
