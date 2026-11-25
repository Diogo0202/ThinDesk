package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ChamadoService {

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

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
        return atualizarPendente(id, new Update()
                .set("tecnico", tecnico.trim())
                .set("solucao", solucao.trim())
                .set("status", "RESOLVIDO")
                .set("dataResolucao", LocalDateTime.now()));
    }

    public Chamado atualizarChamado(String id, Chamado chamadoAtualizado) {
        if (chamadoAtualizado == null) {
            throw new IllegalArgumentException("Os dados do chamado são obrigatórios.");
        }
        validarTexto(chamadoAtualizado.getDescricao(), "Descrição");
        Update alteracoes = new Update().set("descricao", chamadoAtualizado.getDescricao().trim());
        if (chamadoAtualizado.getTitulo() != null && !chamadoAtualizado.getTitulo().isBlank()) {
            alteracoes.set("titulo", chamadoAtualizado.getTitulo().trim());
        }
        if (chamadoAtualizado.getTipo() != null && !chamadoAtualizado.getTipo().isBlank()) {
            alteracoes.set("tipo", chamadoAtualizado.getTipo().trim());
        }
        return atualizarPendente(id, alteracoes);
    }

    private Chamado atualizarPendente(String id, Update alteracoes) {
        validarTexto(id, "Id");
        String chave = id.trim();
        // O MongoDB confere o status no mesmo passo da gravação para bloquear pedidos simultâneos.
        Query filtro = Query.query(Criteria.where("_id").is(chave)
                .and("status").not().regex("^RESOLVIDO$", "i"));
        Chamado atualizado = mongoTemplate.findAndModify(
                filtro, alteracoes, FindAndModifyOptions.options().returnNew(true), Chamado.class);
        if (atualizado != null) {
            return atualizado;
        }
        buscarPorId(chave);
        throw new IllegalStateException("Chamados resolvidos não podem ser alterados.");
    }

    private static void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
    }
}
