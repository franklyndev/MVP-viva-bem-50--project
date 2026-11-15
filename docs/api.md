# API do VivaBem 50+

Contrato da Release 1. Com a API rodando, a versão interativa fica em <http://localhost:8080/swagger-ui.html>: faça login, clique em **Authorize** e cole o token para testar as rotas protegidas.

## Regras gerais

- **Endereço:** `http://localhost:8080`. No emulador Android, use `http://10.0.2.2:8080`.
- **Formato:** JSON, com `Content-Type: application/json`.
- **Autenticação:** cadastro e login devolvem um `token`. Envie-o em toda rota protegida no header `Authorization: Bearer <token>`. O token vale 12 horas; depois disso a API responde `401` e o usuário precisa entrar de novo.
- **Datas:** `birthDate` no formato `AAAA-MM-DD`. Datas com horário, como `startsAt`, devem ser enviadas **com fuso**: `2026-11-20T09:00:00-03:00`. A API devolve em UTC (`2026-11-20T12:00:00Z`); converta para o horário local ao exibir.
- **Valores fixos:** tipos e status usam os códigos em maiúsculas listados abaixo. Mostre ao usuário um texto amigável, mas envie o código.

## Erros

Todos os erros têm o mesmo formato. Mostre `detail` ao usuário e, em erros de validação, a mensagem de cada campo em `errors`.

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "Alguns campos estão inválidos.",
  "errors": {
    "title": "Informe o título.",
    "startsAt": "Informe a data e o horário."
  }
}
```

| Status | Quando acontece |
|---|---|
| `400` | Campo inválido, JSON malformado ou regra de negócio quebrada |
| `401` | Sem token, token inválido ou vencido, ou e-mail e senha incorretos no login. Sem token, a resposta vem sem corpo |
| `404` | Registro não existe ou pertence a outro usuário |
| `409` | E-mail já cadastrado |

## Rotas

| Método | Rota | Token | Resposta |
|---|---|---|---|
| `GET` | `/api/health` | Não | `200` com o estado da API e do banco |
| `POST` | `/api/auth/register` | Não | `201` com token |
| `POST` | `/api/auth/login` | Não | `200` com token |
| `GET` | `/api/goals` | Não | `200` com a lista de objetivos |
| `GET` | `/api/me` | Sim | `200` com o perfil |
| `PUT` | `/api/me` | Sim | `200` com o perfil |
| `PUT` | `/api/me/health-profile` | Sim | `200` com o perfil |
| `PUT` | `/api/me/goals` | Sim | `200` com o perfil |
| `DELETE` | `/api/me` | Sim | `204`, conta e dados apagados |
| `GET` | `/api/appointments` | Sim | `200` com a lista |
| `GET` | `/api/appointments/{id}` | Sim | `200` com o compromisso |
| `POST` | `/api/appointments` | Sim | `201` com o compromisso |
| `PUT` | `/api/appointments/{id}` | Sim | `200` com o compromisso |
| `DELETE` | `/api/appointments/{id}` | Sim | `204` |
| `GET` | `/api/agenda` | Sim | `200` com a linha do tempo do dia |
| `POST` | `/api/agenda/complete` | Sim | `204` |
| `GET` | `/api/wellbeing` | Sim | `200` com o bem-estar do dia |
| `PUT` | `/api/wellbeing` | Sim | `200` com o bem-estar do dia |
| `POST` | `/api/ai/chat` | Sim | `200` com a resposta do assistente |

## Autenticação

### Cadastro: `POST /api/auth/register`

```json
{
  "name": "Maria Souza",
  "email": "maria@example.com",
  "password": "senha-segura-123",
  "birthDate": "1960-05-10",
  "city": "Aracaju",
  "consent": true
}
```

- `password`: de 8 a 72 caracteres.
- `consent` (ou `dataConsent`): precisa ser `true`. Mostre na tela um texto claro sobre o uso dos dados antes da caixa de aceite. Se faltar, o erro vem em `errors.dataConsent`.
- `city`: opcional, até 80 caracteres.
- O e-mail não diferencia maiúsculas de minúsculas.

Resposta `201` (a mesma estrutura do login):

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2026-11-09T03:00:00Z",
  "user": {
    "id": 1,
    "name": "Maria Souza",
    "email": "maria@example.com",
    "birthDate": "1960-05-10",
    "age": 66,
    "city": "Aracaju"
  }
}
```

### Usuária de demonstração

Com `DEMO_DATA_ENABLED=true` no `.env`, a API cria ao iniciar a usuária **Maria Souza** (`DEMO_USER_EMAIL` e `DEMO_USER_PASSWORD`), com caminhada diária às 9:30, três compromissos no dia e um registro de bem-estar. Use só em desenvolvimento e na apresentação.

### Login: `POST /api/auth/login`

```json
{ "email": "maria@example.com", "password": "senha-segura-123" }
```

E-mail ou senha errados respondem `401` com `"detail": "E-mail ou senha incorretos."`.

## Perfil

### `GET /api/me`

```json
{
  "id": 1,
  "name": "Maria Souza",
  "email": "maria@example.com",
  "birthDate": "1960-05-10",
  "age": 66,
  "city": "Aracaju",
  "goals": [{ "code": "ATIVIDADE_FISICA", "label": "Atividade física" }],
  "healthProfile": { "activityLevel": "LEVE", "notes": "Prefiro atividades pela manhã" }
}
```

`city` e `healthProfile` vêm `null` até o usuário preenchê-los. A idade é calculada pela API, no fuso de Sergipe.

### `PUT /api/me`

```json
{ "name": "Maria S. Lima", "birthDate": "1960-05-10" }
```

### `PUT /api/me/health-profile`

Os dois campos são opcionais; o que não for enviado fica vazio.

```json
{ "activityLevel": "LEVE", "notes": "Prefiro atividades pela manhã" }
```

`activityLevel`: `SEDENTARIO`, `LEVE`, `MODERADO` ou `ATIVO`.

### `PUT /api/me/goals`

Substitui todos os objetivos. Lista vazia remove todos. Use os códigos de `GET /api/goals`.

```json
{ "goalCodes": ["ATIVIDADE_FISICA", "ACOMPANHAMENTO_PREVENTIVO"] }
```

### `GET /api/goals`

```json
[
  { "code": "ATIVIDADE_FISICA", "label": "Atividade física" },
  { "code": "ACOMPANHAMENTO_PREVENTIVO", "label": "Acompanhamento preventivo" },
  { "code": "ORGANIZAR_COMPROMISSOS", "label": "Organizar consultas e exames" }
]
```

## Compromissos

### Criar ou editar: `POST /api/appointments` e `PUT /api/appointments/{id}`

O corpo é o mesmo nas duas rotas. Na edição, envie todos os campos: o que faltar fica vazio.

```json
{
  "type": "CONSULTA",
  "title": "Cardiologista",
  "location": "Clínica Centro",
  "notes": "Levar exames anteriores",
  "startsAt": "2026-11-20T09:00:00-03:00",
  "durationMinutes": 30
}
```

| Campo | Obrigatório | Regra |
|---|---|---|
| `type` | Sim | `CONSULTA`, `EXAME` ou `OUTRO` |
| `title` | Sim | Até 120 caracteres |
| `location` | Não | Até 160 caracteres |
| `notes` | Não | Até 2000 caracteres |
| `startsAt` | Sim | Data e horário com fuso |
| `durationMinutes` | Não | De 1 a 1440 |

Resposta (também usada no detalhe e em cada item da lista):

```json
{
  "id": 1,
  "type": "CONSULTA",
  "title": "Cardiologista",
  "location": "Clínica Centro",
  "notes": "Levar exames anteriores",
  "startsAt": "2026-11-20T12:00:00Z",
  "durationMinutes": 30,
  "status": "AGENDADO"
}
```

Todo compromisso novo começa como `AGENDADO`. Confirmar e marcar como realizado entram na Release 2.

### Listar: `GET /api/appointments`

Sem filtro, devolve todos em ordem de data. Para uma semana, envie `from` e `to` juntos, com fuso; o intervalo inclui `from` e exclui `to`:

```
GET /api/appointments?from=2026-11-23T00:00:00-03:00&to=2026-11-30T00:00:00-03:00
```

Na URL, o `+` de fusos positivos precisa ser codificado como `%2B`; fusos negativos, como o `-03:00` do Brasil, não precisam.

## Agenda do dia

### `GET /api/agenda?date=2026-11-20`

`date` é opcional; sem ele, a API usa o dia de hoje. Junta os compromissos do dia e as atividades da semana daquele dia, em ordem de horário.

```json
{
  "date": "2026-11-20",
  "user": { "id": 1, "name": "Maria Souza", "email": "maria@vivabem50.com", "birthDate": "1964-03-15", "age": 62, "city": "Aracaju" },
  "nextAppointment": { "id": "APPOINTMENT:3", "source": "APPOINTMENT", "sourceId": 3, "time": "15:00", "title": "Consulta – Dra. Helena", "kind": "Consulta", "detail": "Clínica Vida · Cardiologista", "done": false },
  "wellbeing": { "date": "2026-11-20", "waterLiters": 1.00, "waterGoalLiters": 2.00, "activityMinutes": 20, "activityGoal": 30, "mood": null },
  "items": [
    { "id": "ACTIVITY:5", "source": "ACTIVITY", "sourceId": 5, "time": "09:30", "title": "Caminhada no parque", "kind": "Atividade", "detail": "Caminhada, 30 minutos", "done": false }
  ]
}
```

- `time` já vem no horário local, no formato `HH:mm`.
- `nextAppointment` é o primeiro compromisso do dia ainda não feito, ou `null`.
- `kind`: `Consulta`, `Exame`, `Lembrete` (compromisso do tipo `OUTRO`) ou `Atividade`.

### `POST /api/agenda/complete`

Marca (`done: true`) ou desmarca (`done: false`) um item como feito. Use `source` e `sourceId` do item.

```json
{ "source": "ACTIVITY", "sourceId": 5, "date": "2026-11-20", "done": true }
```

Um compromisso feito passa a ter status `REALIZADO`. Responde `204`; item de outro usuário responde `404`.

## Bem-estar

### `GET /api/wellbeing?date=2026-11-20`

`date` é opcional. Sem registro no dia, os valores vêm zerados.

```json
{ "date": "2026-11-20", "waterLiters": 1.00, "waterGoalLiters": 2.00, "activityMinutes": 20, "activityGoal": 30, "mood": "BEM" }
```

### `PUT /api/wellbeing`

Só os campos enviados mudam. `date` é obrigatório.

```json
{ "date": "2026-11-20", "waterLiters": 1.25, "activityGoal": 45, "mood": "OTIMO" }
```

| Campo | Regra |
|---|---|
| `waterLiters` | De 0 a 20 litros, com até duas casas decimais |
| `activityMinutes` | De 0 a 600 |
| `activityGoal` | De 1 a 600 minutos; a meta fica salva para os próximos dias do usuário |
| `mood` | `OTIMO`, `BEM`, `NORMAL` ou `TRISTE` |

As respostas incluem `waterLiters` e `waterGoalLiters`. Os campos `waterCups` e `waterGoal` ainda são enviados temporariamente para compatibilidade com clientes antigos.

## Assistente

### `POST /api/ai/chat`

```json
{ "message": "Quero marcar uma consulta" }
```

Resposta:

```json
{ "reply": "Olá, Maria! Você já tem Consulta – Dra. Helena às 15:00. Para marcar outro compromisso, abra Minha Agenda e toque em Adicionar lembrete." }
```

`message`: até 500 caracteres. Hoje as respostas seguem regras sobre a agenda do dia; a integração com o LLM entra na Release 2 sem mudar este contrato.
