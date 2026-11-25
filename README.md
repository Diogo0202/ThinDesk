# ThinDesk: chamados técnicos

Aplicação Spring Boot com login por formulário, API REST de chamados e persistência no MongoDB. O mesmo login atende as páginas existentes e os quatro endpoints da API. Para testar a API no Postman ou Insomnia, selecione **Basic Auth**.

## Versão beta

A versão `1.0.0-beta.1` aparece no painel e é gerada a partir do `pom.xml`. A tag Git correspondente é `v1.0.0-beta.1`. O painel abre chamados, mostra os detalhes e permite registrar a solução com uma conta técnica. O solicitante vê os próprios chamados e o técnico vê a fila completa.

O ThinDesk segue o [SemVer 2.0.0](https://semver.org/lang/pt-BR/). As revisões desta fase avançam como `1.0.0-beta.2`, `1.0.0-beta.3` e assim por diante. Depois de `1.0.0`, a versão de correção indica ajustes compatíveis, a menor indica novos recursos compatíveis e a maior indica mudanças incompatíveis na API. As mudanças ficam no [CHANGELOG](CHANGELOG.md).

## Objetivos da entrega

- [x] Abrir chamado com título e descrição, registrando o usuário autenticado, a data e o status `ABERTO`.
- [x] Listar chamados por usuário e consultar um chamado por ID.
- [x] Registrar solução, técnico e data ao mudar o status para `RESOLVIDO`.
- [x] Restringir a resolução ao técnico e impedir nova alteração de um chamado resolvido.
- [x] Gravar os chamados na coleção `chamados` do MongoDB.
- [x] Passar os testes automatizados de serviço e API.
- [x] Publicar o projeto no [GitHub](https://github.com/Diogo0202/ThinDesk).
- [x] Gravar e enviar um [vídeo da tela de 50 segundos](docs/demo-thindesk.mp4), mostrando os endpoints e o documento gravado no MongoDB.

## Executar

Requisitos: Java 21, MongoDB e acesso às dependências Maven. No PowerShell:

```powershell
$env:MONGODB_URI = 'mongodb://localhost:27017/thindesk'
$env:THINDESK_ADMIN_PASSWORD = '<senha-do-tecnico>'
$env:THINDESK_USER_USERNAME = 'ana'
$env:THINDESK_USER_PASSWORD = '<senha-do-solicitante>'
.\mvnw.cmd spring-boot:run
```

`THINDESK_ADMIN_PASSWORD` é obrigatória. A conta técnica usa o nome `admin`. As duas variáveis `THINDESK_USER_*` criam uma conta comum para testar abertura e acompanhamento. A URI do MongoDB assume `mongodb://localhost:27017/thindesk` quando a variável não está definida. As senhas devem ficar fora do repositório.

## API

| Método | Rota | Permissão | Corpo |
| --- | --- | --- | --- |
| POST | `/api/chamados` | Usuário autenticado | `{"titulo":"Impressora sem rede","descricao":"Não responde ao ping"}` |
| GET | `/api/chamados/usuario/{id}` | O próprio usuário ou técnico | Nenhum |
| GET | `/api/chamados/{id}` | Solicitante ou técnico | Nenhum |
| PUT | `/api/chamados/{id}/resolver` | Técnico | `{"solucao":"Cabo de rede substituído"}` |

Na rota de listagem, `{id}` é o **nome de login** do solicitante, como `ana`. O corpo de abertura não define solicitante, status ou datas. A API extrai o solicitante da autenticação. A conta `admin` atua como técnico. O campo `tipo` continua opcional por compatibilidade com os registros antigos.

### Roteiro de demonstração

1. Inicie o MongoDB e a aplicação. No Postman, configure Basic Auth com a conta comum e envie o `POST`. Guarde o `id` retornado e confira `ABERTO`, `usuario` e `dataAbertura`.
2. Com a mesma conta, chame `GET /api/chamados/usuario/ana` e `GET /api/chamados/{id}`. Consulte um nome de outro usuário para mostrar o bloqueio de acesso.
3. Tente resolver com a conta comum. Depois use a conta técnica e envie o `PUT` com a solução. Confira `RESOLVIDO`, `tecnico` e `dataResolucao`. Repita o `PUT` para mostrar o bloqueio.
4. No `mongosh`, execute `use thindesk` e `db.chamados.findOne({ _id: ObjectId("<id>") })` para exibir o documento persistido. Grave diretamente a tela do computador e mantenha o vídeo abaixo de 5 minutos.

O [vídeo entregue](docs/demo-thindesk.mp4) mostra as chamadas por um cliente HTTP em PowerShell, ferramenta equivalente ao Postman, e uma leitura direta do documento no MongoDB local. Para repetir as chamadas, execute `scripts/demo-api.ps1` com a aplicação em execução e as variáveis de senha configuradas.

## Testes

```powershell
.\mvnw.cmd test
```

Os testes automatizados validam as regras e as respostas HTTP. A prova de persistência real exige MongoDB ativo; o roteiro acima cobre essa etapa.

## Vercel

O [runtime de contêiner da Vercel](https://vercel.com/docs/functions/runtimes/container) detecta `Dockerfile.vercel` na raiz. A imagem empacota o Spring Boot com Java 21. O perfil `vercel` grava as sessões no mesmo MongoDB dos chamados para que uma nova instância reconheça o login.

Defina estas variáveis no projeto antes de publicar:

| Variável | Valor |
| --- | --- |
| `MONGODB_URI` | URI de um MongoDB remoto acessível pela Vercel, com permissão de leitura e escrita no banco da aplicação |
| `MONGODB_DATABASE` | Nome do banco, com padrão `thindesk` |
| `THINDESK_ADMIN_PASSWORD` | Senha da conta técnica `admin` |
| `THINDESK_USER_USERNAME` e `THINDESK_USER_PASSWORD` | Credenciais da conta solicitante. Configure as duas para validar os dois perfis |

Cadastre a URI e as senhas como variáveis sensíveis. Depois do deploy, entre como solicitante, abra um chamado e confirme que a conta técnica consegue resolvê-lo. Consulte novamente o chamado na conta solicitante para conferir o estado persistido.
