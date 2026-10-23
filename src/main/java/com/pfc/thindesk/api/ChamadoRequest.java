package com.pfc.thindesk.api;

public class ChamadoRequest {
    private String descricao;
    private String titulo;
    private String tipo;

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void validar() {
        if (vazio(titulo) || titulo.trim().length() > 160) {
            throw new IllegalArgumentException("Título obrigatório, com até 160 caracteres.");
        }
        if (vazio(descricao) || descricao.trim().length() > 4000) {
            throw new IllegalArgumentException("Descrição obrigatória, com até 4000 caracteres.");
        }
        if (tipo != null && tipo.trim().length() > 120) {
            throw new IllegalArgumentException("Tipo deve ter até 120 caracteres.");
        }
    }

    private boolean vazio(String valor) { return valor == null || valor.trim().isEmpty(); }
}
