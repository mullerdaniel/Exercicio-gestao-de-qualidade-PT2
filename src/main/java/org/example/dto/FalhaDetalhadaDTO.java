package org.example.dto;

import org.example.model.Equipamento;
import org.example.model.Falha;

import java.util.List;

public class FalhaDetalhadaDTO {
    private Falha falha;
    private Equipamento equipamento;
    private List<String> acoesCorretivas;

    public FalhaDetalhadaDTO(Falha falha, Equipamento equipamento, List<String> acoesCorretivas) {
        this.falha = falha;
        this.equipamento = equipamento;
        this.acoesCorretivas = acoesCorretivas;
    }

    public Falha getFalha() { return falha; }
    public Equipamento getEquipamento() { return equipamento; }
    public List<String> getAcoesCorretivas() { return acoesCorretivas; }
}
