package apidiafestivo.apidiafestivo.aplicacion.seguridad;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class SeguridadServicio {

    @Value("${jwt.secret}")
    private String secreto;

    @Value("${jwt.expiration}")
    private Long expiracion;

    public String generarToken(String nombreUsuario) {
        Map<String, Object> declaraciones = new HashMap<>();
        return crearToken(declaraciones, nombreUsuario);
    }

    private String crearToken(Map<String, Object> declaraciones, String nombreUsuario) {
        return Jwts.builder()
                .setClaims(declaraciones)
                .setSubject(nombreUsuario)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiracion * 1000))
                .signWith(getClaveFirma(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getClaveFirma() {
        byte[] keyBytes = Decoders.BASE64.decode(secreto);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extraeNombreUsuario(String token) {
        return extraerDeclaracion(token, Claims::getSubject);
    }

    public Date extraerExpiracion(String token) {
        return extraerDeclaracion(token, Claims::getExpiration);
    }

    public <T> T extraerDeclaracion(String token, Function<Claims, T> declaracionesResolver) {
        final Claims declaraciones = extraerDeclaraciones(token);
        return declaracionesResolver.apply(declaraciones);
    }

    private Claims extraerDeclaraciones(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getClaveFirma())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean tokenExpirado(String token) {
        return extraerExpiracion(token).before(new Date());
    }

    public Boolean validarToken(String token, UserDetails userDetails) {
        final String nombreUsuario = extraeNombreUsuario(token);
        return (nombreUsuario.equals(userDetails.getUsername()) && !tokenExpirado(token));
    }
}
