package org.example.service.acaocorretiva;

import org.example.database.Conexao;
import org.example.model.AcaoCorretiva;
import java.sql.*;

public class AcaoCorretivaServiceImpl implements AcaoCorretivaService {

    @Override
    public AcaoCorretiva registrarConclusaoDeAcao(AcaoCorretiva acao) throws SQLException {
        Long idFalha = acao.getFalhaId();
        Connection conn = null;

        if (idFalha == null) {
            throw new RuntimeException("ID da Falha não pode ser nulo para registrar a Ação Corretiva.");
        }

        try {
            conn = Conexao.conectar();
            conn.setAutoCommit(false);

            String criticidadeFalha = null;
            Long idEquipamento = null;

            String checkFalhaQuery = "SELECT equipamentoId, criticidade FROM Falha WHERE id = ?";
            try (PreparedStatement stmtCheck = conn.prepareStatement(checkFalhaQuery)) {
                stmtCheck.setLong(1, idFalha);
                try (ResultSet rs = stmtCheck.executeQuery()) {
                    if (!rs.next()) {
                        throw new RuntimeException("Falha não encontrada!");
                    }
                    idEquipamento = rs.getLong("equipamentoId");
                    criticidadeFalha = rs.getString("criticidade");
                }
            }

            String insertAcaoQuery =
                    """
                            INSERT INTO AcaoCorretiva (falhaId, dataHoraInicio, dataHoraFim, responsavel, descricaoAcao)
                            VALUES (?, ?, ?, ?, ?)
                            """;

            try (PreparedStatement stmtInsert = conn.prepareStatement(insertAcaoQuery, Statement.RETURN_GENERATED_KEYS)) {
                stmtInsert.setLong(1, idFalha);
                stmtInsert.setObject(2, acao.getDataHoraInicio());
                stmtInsert.setObject(3, acao.getDataHoraFim());
                stmtInsert.setString(4, acao.getResponsavel());
                stmtInsert.setString(5, acao.getDescricaoArea());

                stmtInsert.executeUpdate();

                try (ResultSet rs = stmtInsert.getGeneratedKeys()) {
                    if (rs.next()) {
                        acao.setId(rs.getLong(1));
                    }
                }
            }

            String updateFalhaQuery = "UPDATE Falha SET status = 'RESOLVIDA' WHERE id = ?";
            try (PreparedStatement stmtUpdateFalha = conn.prepareStatement(updateFalhaQuery)) {
                stmtUpdateFalha.setLong(1, idFalha);
                stmtUpdateFalha.executeUpdate();
            }

            if ("CRITICA".equalsIgnoreCase(criticidadeFalha) && idEquipamento != null) {
                String updateEquipamentoQuery =
                        "UPDATE Equipamento SET statusOperacional = 'OPERACIONAL' WHERE id = ?";

                try (PreparedStatement stmtUpdateEquip = conn.prepareStatement(updateEquipamentoQuery)) {
                    stmtUpdateEquip.setLong(1, idEquipamento);
                    stmtUpdateEquip.executeUpdate();
                }
            }

            conn.commit();

            return acao;

        } catch (SQLException | RuntimeException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }
}