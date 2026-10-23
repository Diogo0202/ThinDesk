package com.pfc.thindesk.controller;

import com.pfc.SecurityConfig;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.service.ChamadoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChamadoController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "spring.security.user.name=admin-test",
    "spring.security.user.password=test-password",
    "THINDESK_USER_USERNAME=ana",
    "THINDESK_USER_PASSWORD=user-test-password"
})
class ChamadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChamadoService chamadoService;

    @Test
    void endpointExigeAutenticacao() throws Exception {
        mockMvc.perform(get("/api/chamados/ch-1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(chamadoService);
    }

    @Test
    void credencialTecnicoConfiguradaAutenticaViaHttpBasic() throws Exception {
        when(chamadoService.buscarPorId("ch-1")).thenReturn(chamado("ch-1", "ana", "ABERTO"));

        mockMvc.perform(get("/api/chamados/ch-1")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .httpBasic("admin-test", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ch-1"));
    }

    @Test
    @WithMockUser(username = "ana", roles = "USER")
    void solicitanteAbreChamadoComIdentidadeDaSessao() throws Exception {
        when(chamadoService.criarChamado(any(Chamado.class))).thenAnswer(invocation -> {
            Chamado salvo = invocation.getArgument(0);
            salvo.setId("ch-1");
            salvo.setDataAbertura(LocalDateTime.of(2026, 10, 22, 10, 0));
            return salvo;
        });

        mockMvc.perform(post("/api/chamados")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Impressora indisponível","descricao":"Impressora sem conexão","tipo":"Hardware"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ch-1"))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.usuario").value("ana"));

        verify(chamadoService).criarChamado(argThat(ch -> "ana".equals(ch.getUsuario())));
    }

    @Test
    @WithMockUser(username = "ana", roles = "USER")
    void solicitanteNaoPodeSubstituirSuaIdentidadeNoPayload() throws Exception {
        when(chamadoService.criarChamado(any(Chamado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Erro\",\"descricao\":\"Falha\",\"tipo\":\"Software\",\"usuario\":\"bia\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").value("ana"));

        verify(chamadoService).criarChamado(argThat(ch -> "ana".equals(ch.getUsuario())));
    }

    @Test
    @WithMockUser(username = "ana", roles = "USER")
    void solicitanteConsultaApenasSuaLista() throws Exception {
        Chamado chamado = chamado("ch-1", "ana", "ABERTO");
        when(chamadoService.buscarPorUsuario("ana")).thenReturn(List.of(chamado));

        mockMvc.perform(get("/api/chamados/usuario/ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ch-1"))
                .andExpect(jsonPath("$[0].usuario").value("ana"));

        mockMvc.perform(get("/api/chamados/usuario/bia"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin-test", roles = "TECHNICIAN")
    void tecnicoConsultaChamadoDeOutroSolicitante() throws Exception {
        when(chamadoService.buscarPorId("ch-1")).thenReturn(chamado("ch-1", "ana", "ABERTO"));

        mockMvc.perform(get("/api/chamados/ch-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ch-1"));
    }

    @Test
    @WithMockUser(username = "ana", roles = "USER")
    void solicitanteNaoResolveChamado() throws Exception {
        mockMvc.perform(put("/api/chamados/ch-1/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solucao\":\"Cabo substituído\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(chamadoService);
    }

    @Test
    @WithMockUser(username = "admin-test", roles = "TECHNICIAN")
    void tecnicoResolveERespostaExpõeSolucaoSemCredenciais() throws Exception {
        Chamado resolvido = chamado("ch-1", "ana", "RESOLVIDO");
        resolvido.setTecnico("admin-test");
        resolvido.setSolucao("Cabo substituído");
        resolvido.setDataResolucao(LocalDateTime.of(2026, 10, 22, 10, 15));
        when(chamadoService.resolverChamado("ch-1", "admin-test", "Cabo substituído"))
                .thenReturn(resolvido);

        mockMvc.perform(put("/api/chamados/ch-1/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solucao\":\"Cabo substituído\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDO"))
                .andExpect(jsonPath("$.solucao").value("Cabo substituído"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(chamadoService).resolverChamado("ch-1", "admin-test", "Cabo substituído");
    }

    @Test
    @WithMockUser(username = "admin-test", roles = "TECHNICIAN")
    void chamadoResolvidoRetornaConflitoAoTentarResolverDeNovo() throws Exception {
        when(chamadoService.resolverChamado("ch-1", "admin-test", "Outra solução"))
                .thenThrow(new IllegalStateException("Chamados resolvidos não podem ser alterados."));

        mockMvc.perform(put("/api/chamados/ch-1/resolver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solucao\":\"Outra solução\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin-test", roles = "TECHNICIAN")
    void aberturaRejeitaDescricaoVazia() throws Exception {
        mockMvc.perform(post("/api/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Impressora indisponível\",\"descricao\":\"  \",\"tipo\":\"Hardware\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chamadoService);
    }

    private static Chamado chamado(String id, String usuario, String status) {
        Chamado chamado = new Chamado();
        chamado.setId(id);
        chamado.setTitulo("Impressora indisponível");
        chamado.setDescricao("Impressora sem conexão");
        chamado.setTipo("Hardware");
        chamado.setUsuario(usuario);
        chamado.setStatus(status);
        chamado.setDataAbertura(LocalDateTime.of(2026, 10, 22, 9, 0));
        return chamado;
    }
}
