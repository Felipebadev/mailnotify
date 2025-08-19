package com.notificacao.repository;

import com.notificacao.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    // Lista tudo em ordem decrescente por data/hora
    List<EmailLog> findAllByOrderByDataHoraDesc();

    // Lista filtrando por status (SUCCESS/FAILED/RETRYING/PENDING), ignorando maiúsculas/minúsculas
    List<EmailLog> findAllByStatusIgnoreCaseOrderByDataHoraDesc(String status);
}
