package org.example.dto;

public class EquipamentoContagemFalhasDTO {
    private Long equipamentoId;
    private String nomeEquipamento;
    private int totalFalhas;

    public EquipamentoContagemFalhasDTO(Long equipamentoId, String nomeEquipamento, int totalFalhas) {
        this.equipamentoId = equipamentoId;
        this.nomeEquipamento = nomeEquipamento;
        this.totalFalhas = totalFalhas;
    }

    public Long getEquipamentoId() { return equipamentoId; }
    public String getNomeEquipamento() { return nomeEquipamento; }
    public int getTotalFalhas() { return totalFalhas; }
}
