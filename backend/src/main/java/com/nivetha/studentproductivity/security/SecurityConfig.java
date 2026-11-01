package com.nivetha.studentproductivity.security;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Value("${app.security.secure-cookies:false}")
	private boolean secureCookies;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		tokenRepository.setCookieCustomizer(cookie -> cookie.secure(secureCookies));
		SpaCsrfTokenRequestHandler tokenRequestHandler = new SpaCsrfTokenRequestHandler();

		http
			.securityContext(securityContext -> securityContext
				.securityContextRepository(securityContextRepository())
			)
			.csrf(csrf -> csrf
				.csrfTokenRepository(tokenRepository)
				.csrfTokenRequestHandler(tokenRequestHandler)
				.ignoringRequestMatchers("/h2-console/**")
			)
			.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
			.headers(headers -> headers
				.frameOptions(frame -> frame.sameOrigin())
			)
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/api/auth/register",
					"/api/auth/login",
					"/api/auth/logout",
					"/api/auth/csrf"
				).permitAll()
				.requestMatchers(
					"/login.html",
					"/register.html",
					"/auth.css",
					"/style.css",
					"/script.js",
					"/auth.js",
					"/favicon.ico",
					"/h2-console/**"
				).permitAll()
				.requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
				.requestMatchers("/", "/index.html").authenticated()
				.requestMatchers("/api/auth/me").authenticated()
				.requestMatchers("/api/tasks/**").authenticated()
				.requestMatchers("/api/study-sessions/**").authenticated()
				.requestMatchers("/api/notifications/**").authenticated()
				.requestMatchers("/api/push/**").authenticated()
				.requestMatchers("/api/user/**").authenticated()
				.anyRequest().authenticated()
			)
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint((request, response, authException) -> {
					String uri = request.getRequestURI();
					if (uri.startsWith("/api/")) {
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						response.setContentType(MediaType.APPLICATION_JSON_VALUE);
						response.getWriter().write("""
							{"status":401,"error":"Unauthorized","message":"Authentication required"}
							""");
					} else {
						response.sendRedirect("/login.html");
					}
				})
				.accessDeniedHandler((request, response, accessDeniedException) -> {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					response.setContentType(MediaType.APPLICATION_JSON_VALUE);
					response.getWriter().write("""
						{"status":403,"error":"Forbidden","message":"Access denied"}
						""");
				})
			);

		return http.build();
	}

	static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
		private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
			this.delegate.handle(request, response, csrfToken);
		}

		@Override
		public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
			String headerValue = request.getHeader(csrfToken.getHeaderName());
			if (!StringUtils.hasText(headerValue)) {
				headerValue = request.getHeader("X-XSRF-TOKEN");
			}
			return (StringUtils.hasText(headerValue)) ? headerValue : this.delegate.resolveCsrfTokenValue(request, csrfToken);
		}
	}

	static final class CsrfCookieFilter extends OncePerRequestFilter {
		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
				throws ServletException, IOException {
			CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
			if (csrfToken != null) {
				csrfToken.getToken();
			}
			filterChain.doFilter(request, response);
		}
	}
}
