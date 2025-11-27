package org.example.service.acaocorretiva;

import org.example.model.AcaoCorretiva;

import java.sql.SQLException;

public interface AcaoCorretivaService{
    AcaoCorretiva registrarConclusaoDeAcao(AcaoCorretiva acao) throws SQLException;
}
