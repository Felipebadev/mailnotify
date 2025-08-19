package com.notificacao.controller;

import com.notificacao.Dto.SendEmailRequest;
import com.notificacao.model.EmailLog;
import com.notificacao.model.Usuario;
import com.notificacao.repository.UsuarioRepository;
import com.notificacao.service.EmailService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email") // mantém o caminho singular conforme seu projeto
public class EmailController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // === AÇÃO QUE DISPARA E-MAIL (USER autenticado) ===
    @PostMapping("/reset-senha")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> resetSenha(@RequestParam String email) {
        return usuarioRepository.findByEmail(email)
            .map((Usuario usuario) -> {
                String nome = usuario.getNome() != null ? usuario.getNome() : "usuário";
                String conteudo = "Olá " + nome
                        + ",\n\nClique no link para redefinir sua senha: "
                        + "https://seu-frontend/reset?email=" + usuario.getEmail();

                EmailLog log = emailService.enviarEmail(
                        usuario.getEmail(),
                        "Redefinição de Senha",
                        conteudo
                );

                return ResponseEntity.ok(Map.of(
                        "id", log.getId(),
                        "status", log.getStatus(),
                        "mensagem", "E-mail enviado (ou tentativa registrada)"
                ));
            })
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error","usuário não encontrado")));
    }

    // === HISTÓRICO (ADMIN) ===
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmailLog>> listar(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(emailService.listar(status));
    }

    // === REENVIO MANUAL (ADMIN) ===
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmailLog> reenviar(@PathVariable Long id) {
        return ResponseEntity.ok(emailService.reenviar(id));
    }

        @PostMapping("/send")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> send(@Valid @RequestBody SendEmailRequest req) {
        EmailLog log = emailService.enviarEmail(
                req.getDestinatario(),
                req.getAssunto(),
                req.getConteudo()
        );
        return ResponseEntity.ok(Map.of(
                "id", log.getId(),
                "status", log.getStatus(),
                "mensagem", "E-mail enviado (ou tentativa registrada)"
        ));
    }
}
