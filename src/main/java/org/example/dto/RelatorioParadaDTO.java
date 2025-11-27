package org.example.dto;

public class RelatorioParadaDTO {
    private Long equipamentoId;
    private String nomeEquipamento;
    private double totalHorasParadas;

    public RelatorioParadaDTO(Long equipamentoId, String nomeEquipamento, double totalHorasParadas) {
        this.equipamentoId = equipamentoId;
        this.nomeEquipamento = nomeEquipamento;
        this.totalHorasParadas = totalHorasParadas;
    }

    public Long getEquipamentoId() { return equipamentoId; }
    public String getNomeEquipamento() { return nomeEquipamento; }
    public double getTotalHorasParadas() { return totalHorasParadas; }
}
