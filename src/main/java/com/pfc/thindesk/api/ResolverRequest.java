package com.pfc.thindesk.api;

public class ResolverRequest {
    private String solucao;

    public String getSolucao() { return solucao; }
    public void setSolucao(String solucao) { this.solucao = solucao; }

    public void validar() {
        if (solucao == null || solucao.trim().isEmpty() || solucao.trim().length() > 4000) {
            throw new IllegalArgumentException("Solução obrigatória, com até 4000 caracteres.");
        }
    }
}
