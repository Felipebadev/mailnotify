package com.notificacao.service.EmailServiceImpl;

import com.notificacao.model.EmailLog;
import com.notificacao.repository.EmailLogRepository;
import com.notificacao.service.EmailService;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmailServiceImpl implements EmailService {

    private static final String PENDING  = "PENDING";
    private static final String RETRYING = "RETRYING";
    private static final String SUCCESS  = "SUCCESS";
    private static final String FAILED   = "FAILED";

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;

    public EmailServiceImpl(JavaMailSender mailSender, EmailLogRepository emailLogRepository) {
        this.mailSender = mailSender;
        this.emailLogRepository = emailLogRepository;
    }

    @Override
    @Transactional
    public EmailLog enviarEmail(String to, String assunto, String conteudo) {
        // 1) cria o log inicial
        EmailLog log = new EmailLog();
        log.setDestinatario(to);
        log.setAssunto(assunto);
        log.setConteudo(conteudo);
        log.setStatus(PENDING);
        log = emailLogRepository.save(log);

        // 2) tenta enviar com retry
        enviarComRetry(log.getId());

        // 3) devolve o estado atualizado
        return emailLogRepository.findById(log.getId()).orElseThrow();
    }

    /** Tenta enviar: 1 tentativa + 1 retry após 5s (atende ao PDF). */
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 5000))
    public void enviarComRetry(Long logId) {
        EmailLog log = emailLogRepository.findById(logId).orElseThrow();

        log.setStatus(RETRYING);
        emailLogRepository.save(log);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(log.getDestinatario());
        msg.setSubject(log.getAssunto());
        msg.setText(log.getConteudo());

        try {
            mailSender.send(msg);

            log.setStatus(SUCCESS);
            log.setErro(null);
            emailLogRepository.save(log);

        } catch (Exception ex) {
            // marca falha desta tentativa; o @Retryable fará nova tentativa
            log.setStatus(FAILED);
            log.setErro(ex.getMessage());
            emailLogRepository.save(log);
            throw ex; // re-lança para acionar o retry
        }
    }

    /** Chamado quando todas as tentativas falham. */
    @Recover
    public void recover(Exception ex, Long logId) {
        emailLogRepository.findById(logId).ifPresent(log -> {
            log.setStatus(FAILED);
            log.setErro(ex.getMessage());
            emailLogRepository.save(log);
        });
    }

    @Override
    public List<EmailLog> listar(String status) {
        if (status == null || status.isBlank()) {
            return emailLogRepository.findAllByOrderByDataHoraDesc();
        }
        return emailLogRepository.findAllByStatusIgnoreCaseOrderByDataHoraDesc(status);
    }

    @Override
    @Transactional
    public EmailLog reenviar(Long logId) {
        EmailLog original = emailLogRepository.findById(logId).orElseThrow();

        // Idempotência simples: se já foi SUCCESS, não dispara novo envio
        if (SUCCESS.equalsIgnoreCase(original.getStatus())) {
            return original;
        }

        // Cria um NOVO registro para a nova tentativa (mantém histórico)
        return enviarEmail(original.getDestinatario(), original.getAssunto(), original.getConteudo());
    }
}

