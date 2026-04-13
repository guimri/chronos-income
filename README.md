# chronos-income
# Chronos Income

Sistema de gerenciamento de receitas, projetos e clientes para freelancers.
 
---

## Tecnologias

- Java 21
- Spring Boot 3.5
- Spring Security + JWT
- PostgreSQL 16
- Docker
- Maven

---

## Pré-requisitos

Antes de rodar o projeto, certifique-se de ter instalado:

- [Java 21]
- [Docker]
- [Git]

---

## Como rodar o projeto

### 1. Clonar o repositório

```bash
git clone https://github.com/guimri/chronos-income
cd chronos-income
```

### 2. Configurar as variáveis de ambiente

Copie o arquivo de exemplo e preencha com suas configurações locais:

```bash
cp src/main/resources/application-dev.properties.example \
   src/main/resources/application-dev.properties
```

Edite o `application-dev.properties` com seu editor. Os valores padrão já são compatíveis com o container do passo seguinte.

### 3. Subir o banco de dados

```bash
docker run --name chronos-db \
  -e POSTGRES_DB=chronos_income_dev \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:16
```

Para verificar se está rodando:

```bash
docker ps
```

> Nas próximas vezes que precisar iniciar o banco, basta rodar `docker start chronos-db` (sem precisar recriar o container).

### 4. Definir o JWT Secret

**Linux/macOS:**
```bash
export JWT_SECRET=sua-chave-secreta-longa-aqui
```

**Windows (CMD):**
```cmd
set JWT_SECRET=sua-chave-secreta-longa-aqui
```

**Windows (PowerShell):**
```powershell
$env:JWT_SECRET="sua-chave-secreta-longa-aqui"
```

> O JWT secret pode ser qualquer string longa e aleatória. Em produção, use uma chave gerada de forma segura.

### 5. Rodar a aplicação

**Linux/macOS:**
```bash
./mvnw spring-boot:run
```

**Windows:**
```cmd
mvnw.cmd spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080`.
 
---

## Parar e iniciar o banco

```bash
docker stop chronos-db   # para o container
docker start chronos-db  # inicia novamente (dados preservados)
```

Para remover o container completamente:

```bash
docker rm -f chronos-db
```
 
---

## Estrutura do projeto

```
src/
├── main/
│   ├── java/com/chronosincome/
│   │   ├── config/         # Configurações (Security, JWT, etc.)
│   │   ├── controller/     # Endpoints REST
│   │   ├── dto/            # Objetos de transferência de dados
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── entity/         # Entidades JPA
│   │   ├── exception/      # Tratamento de erros
│   │   ├── repository/     # Repositórios Spring Data
│   │   ├── service/        # Regras de negócio
│   │   └── util/           # Utilitários
│   └── resources/
│       ├── application.properties
│       ├── application-dev.properties        # (ignorado pelo Git)
│       ├── application-dev.properties.example
│       └── application-prod.properties
└── test/
```
