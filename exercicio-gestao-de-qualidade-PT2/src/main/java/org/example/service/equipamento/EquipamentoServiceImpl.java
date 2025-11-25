package org.example.service.equipamento;

import org.example.database.Conexao;
import org.example.model.Equipamento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EquipamentoServiceImpl implements EquipamentoService{
    @Override
    public Equipamento criarEquipamento(Equipamento equipamento) throws SQLException {
        String query = "INSERT INTO Equipamento (nome, numeroDeSerie, areaSetor, statusOperacional) VALUES (?,?,?,?)";

        try(Connection conn = Conexao.conectar();
            PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, equipamento.getNome());
            stmt.setString(2, equipamento.getAreaSetor());
            stmt.setString(3, equipamento.getAreaSetor());
            stmt.setString(4, equipamento.getStatusOperacional());
            stmt.executeUpdate();
        }
        return equipamento;
    }



    @Override
    public Equipamento buscarEquipamentoPorId(Long id) throws SQLException {
        String query = "SELECT * FROM Equipamento WHERE id = ?";

        Equipamento equipamentoEncontrado = null;

        try(Connection conn = Conexao.conectar();
            PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    equipamentoEncontrado = new Equipamento();

                    equipamentoEncontrado.setId(rs.getLong("id"));
                    equipamentoEncontrado.setNome(rs.getString("nome"));
                    equipamentoEncontrado.setNumeroDeSerie(rs.getString("numeroDeSerie"));

                }
            }

        }

        if (equipamentoEncontrado == null) {
            throw new RuntimeException("Equipamento não encontrado!");
        }

        return equipamentoEncontrado;
    }
}
