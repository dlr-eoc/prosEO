package de.dlr.proseo.ui.gui;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class SpringAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	private String usernameParameter = SPRING_SECURITY_FORM_USERNAME_KEY;
	private String missionParameter = "mission";
	private boolean postOnly = true;
	
	@Override
    public Authentication attemptAuthentication(
  	      HttpServletRequest request, 
  	      HttpServletResponse response) 
  	        throws AuthenticationException {
		if (postOnly && !request.getMethod().equals("POST")) {
			throw new AuthenticationServiceException(
					"Authentication method not supported: " + request.getMethod());
		}
  	 
  	        UsernamePasswordAuthenticationToken authRequest
  	          = getAuthRequest(request);
  	        setDetails(request, authRequest);
  	        
  	        return this.getAuthenticationManager()
  	          .authenticate(authRequest);
  	    }
  	 
  	    private UsernamePasswordAuthenticationToken getAuthRequest(
  	      HttpServletRequest request) {
  	 
  	        String username = obtainUsername(request);
  	        String password = obtainPassword(request);
  	        String mission = request.getParameter(missionParameter);
  	      
  	        String usernameDomain = String.format("%s%s%s", mission, 
  	          "/", username.trim());
  	        return new UsernamePasswordAuthenticationToken(
  	          usernameDomain, password);
  	    }
  	    
  		protected boolean requiresAuthentication(HttpServletRequest request,
  				HttpServletResponse response) {
  			
  			return (request.getMethod().equals("POST") && super.requiresAuthentication(request, response));
  		}
}
