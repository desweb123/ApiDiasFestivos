package apidiafestivo.apidiafestivo.aplicacion.seguridad;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FiltroSeguridad extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIJO = "Bearer ";

    @Autowired
    private SeguridadServicio servicioSeguridad;

    @Autowired
    private UsuarioDetallesServicio servicioUsuario;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, 
            @NonNull HttpServletResponse response, 
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader(HEADER);
            String token = null;
            String nombreUsuario = null;

            if (header != null && header.startsWith(PREFIJO) && header.length() > PREFIJO.length()) {
                token = header.substring(PREFIJO.length());
                nombreUsuario = servicioSeguridad.extraeNombreUsuario(token);
            }

            if (nombreUsuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = servicioUsuario.loadUserByUsername(nombreUsuario);
                if (servicioSeguridad.validarToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken autenticacionToken = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    autenticacionToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacionToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }
}
