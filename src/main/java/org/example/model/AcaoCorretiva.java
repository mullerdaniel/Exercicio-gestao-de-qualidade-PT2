package org.example.model;


/*CREATE TABLE IF NOT EXISTS AcaoCorretiva (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        falhaId BIGINT NOT NULL,
        dataHoraInicio DATETIME NOT NULL,
        dataHoraFim DATETIME NOT NULL,
        responsavel VARCHAR(255) NOT NULL,
descricaoAcao TEXT NOT NULL,

    -- Chave estrangeira ligando a Ação à Falha
CONSTRAINT fk_acao_falha
FOREIGN KEY (falhaId)
REFERENCES Falha(id)
ON DELETE RESTRICT -- Impede excluir uma falha se ela tiver ações corretivas
);*/

import java.time.LocalDateTime;

public class AcaoCorretiva {
    private Long id;

    private Long falhaId;

    private LocalDateTime dataHoraInicio;

    private LocalDateTime dataHoraFim;

    private String responsavel;

    private String descricaoArea;

    public AcaoCorretiva(Long id, Long falhaId, LocalDateTime dataHoraInicio, LocalDateTime dataHoraFim, String responsavel, String descricaoArea) {
        this.id = id;
        this.falhaId = falhaId;
        this.dataHoraInicio = dataHoraInicio;
        this.dataHoraFim = dataHoraFim;
        this.responsavel = responsavel;
        this.descricaoArea = descricaoArea;
    }

    public AcaoCorretiva(Long falhaId, LocalDateTime dataHoraInicio, LocalDateTime dataHoraFim, String responsavel, String descricaoArea) {
        this.falhaId = falhaId;
        this.dataHoraInicio = dataHoraInicio;
        this.dataHoraFim = dataHoraFim;
        this.responsavel = responsavel;
        this.descricaoArea = descricaoArea;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFalhaId() {
        return falhaId;
    }

    public void setFalhaId(Long falhaId) {
        this.falhaId = falhaId;
    }

    public LocalDateTime getDataHoraInicio() {
        return dataHoraInicio;
    }

    public void setDataHoraInicio(LocalDateTime dataHoraInicio) {
        this.dataHoraInicio = dataHoraInicio;
    }

    public LocalDateTime getDataHoraFim() {
        return dataHoraFim;
    }

    public void setDataHoraFim(LocalDateTime dataHoraFim) {
        this.dataHoraFim = dataHoraFim;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public String getDescricaoArea() {
        return descricaoArea;
    }

    public void setDescricaoArea(String descricaoArea) {
        this.descricaoArea = descricaoArea;
    }
}
