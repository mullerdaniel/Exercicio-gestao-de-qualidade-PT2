package org.example.service.falha;

import org.example.database.Conexao;
import org.example.model.Falha;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FalhaServiceImpl implements FalhaService{
    @Override
    public Falha registrarNovaFalha(Falha falha) throws SQLException {

        Long idEquipamento = falha.getEquipamentoId();
        Connection conn = null;

        if (idEquipamento == null) {
            throw new IllegalArgumentException("ID do Equipamento não pode ser nulo para registrar a falha!");
        }

        try {
            conn = Conexao.conectar();
            conn.setAutoCommit(false);

            if (!equals(conn)) {
                throw new IllegalArgumentException("Equipamento não encontrado!");
            }

            String insertFalhaQuery =
                    """
                            INSERT INTO Falha (equipamentoId, dataHoraOcorrencia, descricao, criticidade, status, tempoParadaHoras)
                            VALUES (?, ?, ?, ?, 'ABERTA', ?)
                            """;

            try (PreparedStatement stmtInsert = conn.prepareStatement(insertFalhaQuery, Statement.RETURN_GENERATED_KEYS)) {

                stmtInsert.setLong(1, idEquipamento);
                stmtInsert.setObject(2, falha.getDataHoraOcorrencia());
                stmtInsert.setString(3, falha.getDescricao());
                stmtInsert.setString(4, falha.getCriticidade());

                stmtInsert.executeUpdate();

                try (ResultSet rs = stmtInsert.getGeneratedKeys()) {
                    if (rs.next()) {
                        falha.setId(rs.getLong(1));
                    }
                }

                falha.setStatus("ABERTA");
            }

            if ("CRITICA".equalsIgnoreCase(falha.getCriticidade())) {

                String updateEquipamentoQuery =
                        "UPDATE Equipamento SET statusOperacional = 'EM_MANUTENCAO' WHERE id = ?";

                try (PreparedStatement stmtUpdate = conn.prepareStatement(updateEquipamentoQuery)) {
                    stmtUpdate.setLong(1, idEquipamento);
                    stmtUpdate.executeUpdate();
                }
            }

            return falha;

        } catch (SQLException | IllegalArgumentException e) {
            if (conn != null) {
            }
        }
        return falha;
    }

    @Override
    public List<Falha> buscarFalhasCriticasAbertas() throws SQLException {
        String query = """
        SELECT id, equipamentoId, dataHoraOcorrencia, descricao, criticidade, status, tempoParadaHoras
        FROM Falha
        WHERE status = 'ABERTA' AND criticidade = 'CRITICA'
        """;

        List<Falha> falhas = new ArrayList<>();

        try (Connection conn = Conexao.conectar();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Falha falha = new Falha();
                falha.setId(rs.getLong("id"));
                falha.setEquipamentoId(rs.getLong("equipamentoId"));

                falha.setDataHoraOcorrencia(rs.getObject("dataHoraOcorrencia", LocalDateTime.class));

                falha.setDescricao(rs.getString("descricao"));
                falha.setCriticidade(rs.getString("criticidade"));
                falha.setStatus(rs.getString("status"));

                falha.setTempoParadaHoras(BigDecimal.valueOf(rs.getObject("tempoParadaHoras", Double.class)));

                falhas.add(falha);
            }
        }
        return falhas;
    }
}
