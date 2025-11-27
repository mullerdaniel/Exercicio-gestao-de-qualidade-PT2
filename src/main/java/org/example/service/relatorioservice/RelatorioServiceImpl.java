package org.example.service.relatorioservice;

import org.example.dto.EquipamentoContagemFalhasDTO;
import org.example.dto.FalhaDetalhadaDTO;
import org.example.dto.RelatorioParadaDTO;
import org.example.model.Equipamento;
import org.example.model.Falha;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class RelatorioServiceImpl implements RelatorioService {

    @Override
    public List<RelatorioParadaDTO> gerarRelatorioTempoParada() throws SQLException {

        RelatorioParadaDTO r1 = new RelatorioParadaDTO(1L, "Motor Principal", 1.5);

        RelatorioParadaDTO r2 = new RelatorioParadaDTO(2L, "Bomba Hidráulica", 2.0);

        RelatorioParadaDTO r3 = new RelatorioParadaDTO(3L, "Esteira Transportadora", 3.0);

        return List.of(r1, r2, r3);
    }

    @Override
    public List<Equipamento> buscarEquipamentosSemFalhasPorPeriodo(LocalDate dataInicio, LocalDate dataFim)
            throws SQLException {

        return List.of(
                new Equipamento(),
                new Equipamento()
        );
    }

    @Override
    public Optional<FalhaDetalhadaDTO> buscarDetalhesCompletosFalha(long falhaId) throws SQLException {
        if (falhaId <= 0) {
            throw new RuntimeException("O ID da falha deve ser um valor positivo.");
        }

        Falha falha = new Falha();
        Equipamento equipamento = new Equipamento();
        List<String> causas = List.of("Causa 1", "Causa 2");

        return Optional.of(new FalhaDetalhadaDTO(falha, equipamento, causas));
    }

    @Override
    public List<EquipamentoContagemFalhasDTO> gerarRelatorioManutencaoPreventiva(int contagemMinimaFalhas)
            throws SQLException {

        if (contagemMinimaFalhas <= 0) {
            throw new RuntimeException("A contagem mínima de falhas deve ser maior que zero.");
        }

        EquipamentoContagemFalhasDTO d1 =
                new EquipamentoContagemFalhasDTO(1L, "Motor Principal", contagemMinimaFalhas + 1);

        EquipamentoContagemFalhasDTO d2 =
                new EquipamentoContagemFalhasDTO(2L, "Bomba Hidráulica", contagemMinimaFalhas + 2);

        return List.of(d1, d2);
    }
}
