package com.notificacao.controller;

import com.notificacao.config.JwtUtil;
import com.notificacao.Dto.AuthRequest;
import com.notificacao.Dto.AuthResponse;
import com.notificacao.model.Usuario;
import com.notificacao.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
// @CrossOrigin(origins = "*") // pode remover se já tiver CORS global no SecurityConfig
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Usuario usuario) {
        // 409 se o e-mail já existe
        if (usuarioService.emailExiste(usuario.getEmail())) {
            return ResponseEntity.status(409).body("E-mail já cadastrado");
        }
        Usuario salvo = usuarioService.cadastrar(usuario);
        // não retornar senha
        salvo.setSenha(null);
        return ResponseEntity.status(201).body(salvo);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
    return usuarioService.autenticar(request.getEmail(), request.getSenha())
        .map(user -> {
        String token = jwtUtil.generateToken(user.getEmail());
        AuthResponse resp = new AuthResponse(token, user.getEmail(), "ROLE_USER");
        return ResponseEntity.ok(resp);
        })
        .orElseGet(() -> ResponseEntity.status(401).build()); // sem body para manter o tipo
}
}
