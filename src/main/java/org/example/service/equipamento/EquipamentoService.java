package org.example.service.equipamento;

import org.example.model.Equipamento;

import java.sql.SQLException;

public interface EquipamentoService {
    Equipamento criarEquipamento(Equipamento equipamento) throws SQLException;

    Equipamento buscarEquipamentoPorId(Long id) throws SQLException;
}
