package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ChamadoService {

    @Autowired
    private ChamadoRepository chamadoRepository;

    public Chamado criarChamado(Chamado chamado) {
        if (chamado == null) {
            throw new IllegalArgumentException("Os dados do chamado são obrigatórios.");
        }
        validarTexto(chamado.getTitulo(), "Título");
        validarTexto(chamado.getDescricao(), "Descrição");
        validarTexto(chamado.getUsuario(), "Solicitante");
        chamado.setId(null);
        chamado.setStatus("ABERTO");
        chamado.setDataAbertura(LocalDateTime.now());
        chamado.setDataResolucao(null);
        chamado.setSolucao(null);
        chamado.setTecnico(null);
        return chamadoRepository.save(chamado);
    }

    public List<Chamado> listarChamados() {
        return chamadoRepository.findAll();
    }

    public List<Chamado> buscarPorUsuario(String usuario) {
        validarTexto(usuario, "Usuário");
        return chamadoRepository.findByUsuario(usuario.trim());
    }

    public Chamado buscarPorId(String id) {
        validarTexto(id, "Id");
        return chamadoRepository.findById(id.trim())
                .orElseThrow(() -> new NoSuchElementException("Chamado não encontrado com id: " + id));
    }

    public Chamado resolverChamado(String id, String tecnico, String solucao) {
        validarTexto(tecnico, "Técnico");
        validarTexto(solucao, "Solução");
        Chamado chamado = buscarPorId(id);
        assegurarAberto(chamado);
        chamado.setTecnico(tecnico.trim());
        chamado.setSolucao(solucao.trim());
        chamado.setStatus("RESOLVIDO");
        chamado.setDataResolucao(LocalDateTime.now());
        return chamadoRepository.save(chamado);
    }

    public Chamado atualizarChamado(String id, Chamado chamadoAtualizado) {
        if (chamadoAtualizado == null) {
            throw new IllegalArgumentException("Os dados do chamado são obrigatórios.");
        }
        Chamado chamado = buscarPorId(id);
        assegurarAberto(chamado);
        if (chamadoAtualizado.getTitulo() != null && !chamadoAtualizado.getTitulo().isBlank()) {
            chamado.setTitulo(chamadoAtualizado.getTitulo().trim());
        }
        validarTexto(chamadoAtualizado.getDescricao(), "Descrição");
        chamado.setDescricao(chamadoAtualizado.getDescricao().trim());
        if (chamadoAtualizado.getTipo() != null && !chamadoAtualizado.getTipo().isBlank()) {
            chamado.setTipo(chamadoAtualizado.getTipo().trim());
        }
        return chamadoRepository.save(chamado);
    }

    private static void assegurarAberto(Chamado chamado) {
        if ("RESOLVIDO".equalsIgnoreCase(chamado.getStatus())) {
            throw new IllegalStateException("Chamados resolvidos não podem ser alterados.");
        }
    }

    private static void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
    }
}
