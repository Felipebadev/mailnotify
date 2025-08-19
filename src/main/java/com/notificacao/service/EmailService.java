package com.notificacao.service;

import com.notificacao.model.EmailLog;
import java.util.List;

public interface EmailService {
    /** Dispara o e-mail e registra o histórico (com retry). */
    EmailLog enviarEmail(String to, String assunto, String conteudo);

    /** Lista o histórico; se status for null/vazio, retorna todos. */
    List<EmailLog> listar(String status);

    /** Reenvia manualmente com base em um log existente (idempotente). */
    EmailLog reenviar(Long logId);
}
