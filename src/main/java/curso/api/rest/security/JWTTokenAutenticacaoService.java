package curso.api.rest.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import curso.api.rest.ApplicationContextLoad;
import curso.api.rest.model.Usuario;
import curso.api.rest.repository.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Service
@Component
public class JWTTokenAutenticacaoService {

	
	private static final long EXPIRATION_TIME = 172800000;
	
	private static String SECRET = "SenhaExtremamenteSecreta";
	
	private static String TOKEN_PREFIX = "Bearer";
	
	private static String HEADER_STRING = "Authorization";
	
	
	public void addAuthentication(HttpServletResponse response, 
			String username) throws IOException {
		
		String JWT = Jwts.builder().setSubject(username)
				.setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
				.signWith(SignatureAlgorithm.HS512, SECRET).compact();
		
		String token = TOKEN_PREFIX + " " + JWT;
		
		
		
		response.addHeader(HEADER_STRING, token);
		
		ApplicationContextLoad.getApplicationContext()
		.getBean(UsuarioRepository.class).atualizaTokenUser(JWT, username);
		
		
		response.addHeader("Access-Control-Allow-Origin", "*");


		
		liberarCors(response);
		
		response.getWriter().write("{\"Authorization\": \""+token+"\"}");
		
	}
	
	public Authentication getAuthentication(HttpServletRequest request, HttpServletResponse response) {
		String token  = request.getHeader(HEADER_STRING);
		
		try {
		if(token != null) {
			
			String tokenLimpo = token.replace(TOKEN_PREFIX, "").trim();
			
			String user = Jwts.parser().setSigningKey(SECRET)
					.parseClaimsJws(tokenLimpo)
					.getBody().getSubject();
			
			if(user != null) {
				
				Usuario usuario = ApplicationContextLoad.getApplicationContext()
						.getBean(UsuarioRepository.class).findUserByLogin(user);
				
				if(usuario!= null ) {
					
					if(tokenLimpo.equalsIgnoreCase(usuario.getToken())) {
						
						return new UsernamePasswordAuthenticationToken(usuario.getLogin(), 
								usuario.getSenha(),
								usuario.getAuthorities());
					}
				}
				
			}
			
		}
		
		}catch (io.jsonwebtoken.ExpiredJwtException e) {
			try {
				response.getOutputStream().println("Seu  token esta expirado, faca o login  ou  informe um  novo token");
			} catch (IOException e1) {}
				
		}
		liberarCors(response);
		
		return null;
	}

	private void liberarCors(HttpServletResponse response) {
		if (response.getHeader("Access-Control-Allow-Origin") == null) {
			response.addHeader("Access-Control-Allow-Origin", "*");		
		}
		
		if (response.getHeader("Access-Control-Allow-Headers") == null) {
			response.addHeader("Access-Control-Allow-Headers", "*");		
		}
		
		if (response.getHeader("Access-Control-Request-Headers") == null) {
			response.addHeader("Access-Control-Request-Headers", "*");		
		}
		
		if (response.getHeader("Access-Control-Allow-Methods") == null) {
			response.addHeader("Access-Control-Allow-Methods", "*");		
		}
	}
 	
	@Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
	
	
}
