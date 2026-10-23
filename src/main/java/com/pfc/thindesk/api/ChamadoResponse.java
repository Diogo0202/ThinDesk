package com.pfc.thindesk.api;

import com.pfc.thindesk.entity.Chamado;
import java.time.LocalDateTime;

public class ChamadoResponse {
    private String id;
    private String titulo;
    private String descricao;
    private String status;
    private String tipo;
    private String tecnico;
    private String usuario;
    private LocalDateTime dataAbertura;
    private String solucao;
    private LocalDateTime dataResolucao;

    public static ChamadoResponse de(Chamado chamado) {
        ChamadoResponse response = new ChamadoResponse();
        response.id = chamado.getId();
        response.titulo = chamado.getTitulo();
        response.descricao = chamado.getDescricao();
        response.status = chamado.getStatus();
        response.tipo = chamado.getTipo();
        response.tecnico = chamado.getTecnico();
        response.usuario = chamado.getUsuario();
        response.dataAbertura = chamado.getDataAbertura();
        response.solucao = chamado.getSolucao();
        response.dataResolucao = chamado.getDataResolucao();
        return response;
    }

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public String getStatus() { return status; }
    public String getTipo() { return tipo; }
    public String getTecnico() { return tecnico; }
    public String getUsuario() { return usuario; }
    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public String getSolucao() { return solucao; }
    public LocalDateTime getDataResolucao() { return dataResolucao; }
}
