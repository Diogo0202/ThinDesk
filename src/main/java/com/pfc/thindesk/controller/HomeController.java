package com.pfc.thindesk.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pfc.thindesk.entity.Cliente;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.HorarioAtendimento;
import com.pfc.thindesk.service.ClienteService;
import com.pfc.thindesk.service.ChamadoService;
import com.pfc.thindesk.service.HorarioAtendimentoService;
import java.util.List;
import java.util.Comparator;
import java.security.Principal;
import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private ChamadoService chamadoService;
    @Autowired
    private ClienteService clienteService;
    @Autowired
    private HorarioAtendimentoService horarioAtendimentoService;

    @Autowired
    private BuildProperties buildProperties;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/chamados"})
    public String painel(Model model, Authentication principal) {
        boolean tecnico = principal.getAuthorities()
                .stream().anyMatch(role -> "ROLE_TECHNICIAN".equals(role.getAuthority()));
        List<Chamado> chamados = tecnico ? chamadoService.listarChamados()
                : chamadoService.buscarPorUsuario(principal.getName());
        long resolvidos = chamados.stream().filter(chamado -> "RESOLVIDO".equalsIgnoreCase(chamado.getStatus())).count();
        model.addAttribute("chamados", chamados.stream().sorted(Comparator.comparing(
                Chamado::getDataAbertura, Comparator.nullsLast(Comparator.reverseOrder()))).toList());
        model.addAttribute("pendentes", chamados.size() - resolvidos);
        model.addAttribute("resolvidos", resolvidos);
        model.addAttribute("usuario", principal.getName());
        model.addAttribute("tecnico", tecnico);
        model.addAttribute("versao", buildProperties.getVersion());
        return "home";
    }

    @GetMapping("/clientes")
    public String clientes(Model model) {
        List<Cliente> clientes = clienteService.listarClientes();
        model.addAttribute("clientes", clientes);
        String fragment = "clientes :: content";
        log.info("Carregando fragmento: {}", fragment); // Log para depuração
        model.addAttribute("content", fragment);
        return "clientes";
    }

    @GetMapping("/ajustes-horarios")
    public String horarios(Model model) {
        List<HorarioAtendimento> horarios = horarioAtendimentoService.listarTodos();
        model.addAttribute("ajustes-horarios", horarios);
        String fragment = "ajustes-horarios :: content";
        log.info("Carregando fragmento: {}", fragment); // Log para depuração
        model.addAttribute("content", fragment);
        return "ajustes-horarios";
    }

}
